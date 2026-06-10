package id.naturalsmp.naturalSchool.exam;

import com.google.gson.Gson;
import org.bukkit.Bukkit;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import com.sun.net.httpserver.HttpServer;
import id.naturalsmp.naturalSchool.NaturalSchool;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.logging.Level;

public class ExamManager {

    private final NaturalSchool plugin;
    private final Object cacheLock = new Object();
    private final Object syncLock = new Object();

    private final List<ExamQuestions.Question> cachedQuestions = new ArrayList<>();

    private HttpServer server;

    public ExamManager(NaturalSchool plugin) {
        this.plugin = plugin;
    }

    public synchronized void initialize() {
        // 1. Pull core state from database synchronously on boot
        String dbVersionStr = plugin.getDatabaseManager().getCoreState("exam_version");

        int dbVersion = 1;
        if (dbVersionStr != null) {
            try {
                dbVersion = Integer.parseInt(dbVersionStr);
            } catch (NumberFormatException e) {
                // Keep default 1
            }
        }

        // 2. Read local exams.json file
        File file = new File(plugin.getDataFolder(), "exams.json");
        boolean forceUpdate = false;
        int localVersion = -1;

        if (file.exists()) {
            try (FileReader reader = new FileReader(file, StandardCharsets.UTF_8)) {
                JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
                if (root.has("version")) {
                    localVersion = root.get("version").getAsInt();
                }
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Failed to parse local exams.json. Forcing update.", e);
                forceUpdate = true;
            }
        } else {
            forceUpdate = true;
        }

        // Check if outdated or file doesn't exist
        if (forceUpdate || localVersion < dbVersion) {
            plugin.getLogger().info("Local exams.json is outdated or missing. Synchronizing from DB asynchronously...");
            final int finalDbVersion = dbVersion;
            CompletableFuture.runAsync(() -> {
                synchronized (syncLock) {
                    try {
                        List<Map<String, Object>> rows = plugin.getDatabaseManager().getAllExamQuestions();
                        JsonObject root = new JsonObject();
                        root.addProperty("version", finalDbVersion);

                        JsonArray questionsArray = new JsonArray();
                        for (Map<String, Object> row : rows) {
                            JsonObject qObj = new JsonObject();
                            qObj.addProperty("packet_id", (String) row.get("packet_id"));
                            qObj.addProperty("academic_class", (Integer) row.get("academic_class"));
                            qObj.addProperty("question_number", (Integer) row.get("question_number"));
                            qObj.addProperty("question_type", (String) row.get("question_type"));
                            qObj.addProperty("question_text", (String) row.get("question_text"));

                            String optionsStr = (String) row.get("options");
                            if (optionsStr != null && !optionsStr.trim().isEmpty()) {
                                try {
                                    qObj.add("options", JsonParser.parseString(optionsStr));
                                } catch (Exception ex) {
                                    qObj.add("options", com.google.gson.JsonNull.INSTANCE);
                                }
                            } else {
                                qObj.add("options", com.google.gson.JsonNull.INSTANCE);
                            }

                            qObj.addProperty("correct_answer", (String) row.get("correct_answer"));

                            String indicesStr = (String) row.get("correct_indices");
                            if (indicesStr != null && !indicesStr.trim().isEmpty()) {
                                try {
                                    qObj.add("correct_indices", JsonParser.parseString(indicesStr));
                                } catch (Exception ex) {
                                    qObj.add("correct_indices", com.google.gson.JsonNull.INSTANCE);
                                }
                            } else {
                                qObj.add("correct_indices", com.google.gson.JsonNull.INSTANCE);
                            }

                            questionsArray.add(qObj);
                        }
                        root.add("questions", questionsArray);

                        // Overwrite file
                        try (FileWriter writer = new FileWriter(file, StandardCharsets.UTF_8)) {
                            new GsonBuilder().setPrettyPrinting().create().toJson(root, writer);
                        }

                        // Parse into memory
                        parseMemoryFromRoot(root);
                        plugin.getLogger().info("exams.json successfully populated from database with " + cachedQuestions.size() + " questions.");
                    } catch (Exception e) {
                        plugin.getLogger().log(Level.SEVERE, "Failed to synchronize exams from database asynchronously", e);
                    }
                }
            });
        } else {
            // Load questions from local JSON into memory
            plugin.getLogger().info("exams.json is up-to-date. Loading questions into memory...");
            try (FileReader reader = new FileReader(file, StandardCharsets.UTF_8)) {
                JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
                parseMemoryFromRoot(root);
                plugin.getLogger().info("exams.json successfully loaded with " + cachedQuestions.size() + " questions.");
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to load local exams.json", e);
            }
        }

        // Start Webhook Server
        startWebhookServer();
    }

    public synchronized void reload() {
        plugin.getLogger().info("Reloading Exam Subsystem configuration...");
        stopWebhookServer();
        initialize();
    }

    private void parseMemoryFromRoot(JsonObject root) {
        synchronized (cacheLock) {
            this.cachedQuestions.clear();
            this.cachedQuestions.addAll(parseQuestionsFromRoot(root));
        }
    }

    private List<ExamQuestions.Question> parseQuestionsFromRoot(JsonObject root) {
        Gson gson = new Gson();
        List<ExamQuestions.Question> temp = new ArrayList<>();
        if (!root.has("questions") || root.get("questions").isJsonNull()) {
            return temp;
        }
        JsonArray qArray = root.getAsJsonArray("questions");
        for (JsonElement el : qArray) {
            try {
                JsonObject qObj = el.getAsJsonObject();
                String packetId = qObj.get("packet_id").getAsString();
                int cls = qObj.get("academic_class").getAsInt();
                int num = qObj.get("question_number").getAsInt();
                String type = qObj.get("question_type").getAsString();
                String text = qObj.get("question_text").getAsString();

                List<String> opts = null;
                if (qObj.has("options") && !qObj.get("options").isJsonNull()) {
                    opts = gson.fromJson(qObj.get("options"), new TypeToken<List<String>>(){}.getType());
                }

                String correctAns = null;
                if (qObj.has("correct_answer") && !qObj.get("correct_answer").isJsonNull()) {
                    correctAns = qObj.get("correct_answer").getAsString();
                }

                List<Integer> correctInds = null;
                if (qObj.has("correct_indices") && !qObj.get("correct_indices").isJsonNull()) {
                    correctInds = gson.fromJson(qObj.get("correct_indices"), new TypeToken<List<Integer>>(){}.getType());
                }

                temp.add(new ExamQuestions.Question(packetId, cls, num, type, text, opts, correctAns, correctInds));
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Failed to parse question object in JSON, skipping.", e);
            }
        }
        return temp;
    }

    public void startWebhookServer() {
        int port = plugin.getConfig().getInt("api.port", 8080);
        try {
            server = HttpServer.create(new java.net.InetSocketAddress(port), 0);
            server.createContext("/school/exam/update", exchange -> {
                // IP Whitelist check
                java.net.InetAddress remoteAddress = exchange.getRemoteAddress().getAddress();
                String remoteIp = remoteAddress.getHostAddress();
                List<String> whitelist = plugin.getConfig().getStringList("api.whitelist-ips");
                if (whitelist != null && !whitelist.isEmpty()) {
                    boolean ipAllowed = false;
                    for (String ip : whitelist) {
                        if (ip.trim().equals(remoteIp)) {
                            ipAllowed = true;
                            break;
                        }
                    }
                    if (!ipAllowed) {
                        plugin.getLogger().warning("Blocked unauthorized webhook access attempt from IP: " + remoteIp);
                        exchange.sendResponseHeaders(401, -1);
                        return;
                    }
                }

                // API Key Verification
                String configApiKey = plugin.getConfig().getString("api.api-key");
                String requestApiKey = exchange.getRequestHeaders().getFirst("X-School-API-Key");
                if (configApiKey == null || configApiKey.trim().isEmpty() || !configApiKey.equals(requestApiKey)) {
                    plugin.getLogger().warning("Blocked unauthorized webhook access attempt: Invalid or missing X-School-API-Key from IP: " + remoteIp);
                    byte[] response = "{\"status\":\"error\",\"message\":\"Unauthorized\"}".getBytes(StandardCharsets.UTF_8);
                    exchange.getResponseHeaders().set("Content-Type", "application/json");
                    exchange.sendResponseHeaders(401, response.length);
                    try (java.io.OutputStream os = exchange.getResponseBody()) {
                        os.write(response);
                    }
                    return;
                }

                if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                    exchange.sendResponseHeaders(405, -1);
                    return;
                }

                try (java.io.InputStream is = exchange.getRequestBody();
                     java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream()) {
                    byte[] buffer = new byte[1024];
                    int len;
                    while ((len = is.read(buffer)) != -1) {
                        bos.write(buffer, 0, len);
                    }
                    String body = bos.toString(StandardCharsets.UTF_8);

                    // Parse & Validate
                    JsonObject root = JsonParser.parseString(body).getAsJsonObject();

                    // Upsert database configurations
                    if (root.has("portal_semester_status")) {
                        plugin.getDatabaseManager().setCoreState("portal_semester_status", root.get("portal_semester_status").getAsString());
                    } else if (root.has("portal_status")) {
                        plugin.getDatabaseManager().setCoreState("portal_semester_status", root.get("portal_status").getAsString());
                    }
                    if (root.has("portal_message")) {
                        plugin.getDatabaseManager().setCoreState("portal_message", root.get("portal_message").getAsString());
                    }
                    if (root.has("is_semester_break")) {
                        plugin.getDatabaseManager().setCoreState("is_semester_break", root.get("is_semester_break").getAsString());
                    }
                    if (root.has("active_uh_packets")) {
                        plugin.getDatabaseManager().setCoreState("active_uh_packets", root.get("active_uh_packets").toString());
                    } else if (root.has("active_packet_ids")) {
                        plugin.getDatabaseManager().setCoreState("active_uh_packets", root.get("active_packet_ids").toString());
                    }
                    if (root.has("current_active_semester_packets")) {
                        plugin.getDatabaseManager().setCoreState("current_active_semester_packets", root.get("current_active_semester_packets").toString());
                    }
                    if (root.has("version")) {
                        plugin.getDatabaseManager().setCoreState("exam_version", String.valueOf(root.get("version").getAsInt()));
                    }

                    // Prepare local JSON with ONLY version and questions
                    JsonObject localRoot = new JsonObject();
                    if (root.has("version")) {
                        localRoot.addProperty("version", root.get("version").getAsInt());
                    } else {
                        localRoot.addProperty("version", 1);
                    }
                    if (root.has("questions")) {
                        localRoot.add("questions", root.get("questions"));
                    } else {
                        localRoot.add("questions", new JsonArray());
                    }

                    // Overwrite local file with ONLY version and questions
                    File file = new File(plugin.getDataFolder(), "exams.json");
                    try (FileWriter fw = new FileWriter(file, StandardCharsets.UTF_8)) {
                        new GsonBuilder().setPrettyPrinting().create().toJson(localRoot, fw);
                    }

                    // Update memory
                    parseMemoryFromRoot(localRoot);

                    plugin.getLogger().info("Exam Subsystem updated via webhook. Version: " + getExamVersion() + ", Status: " + getPortalSemesterStatus());

                    // Response 200 OK
                    byte[] response = "{\"status\":\"success\",\"message\":\"Exam cache updated successfully\"}".getBytes(StandardCharsets.UTF_8);
                    exchange.getResponseHeaders().set("Content-Type", "application/json");
                    exchange.sendResponseHeaders(200, response.length);
                    try (java.io.OutputStream os = exchange.getResponseBody()) {
                        os.write(response);
                    }

                } catch (Exception e) {
                    plugin.getLogger().log(Level.SEVERE, "Failed to parse webhook JSON payload", e);
                    byte[] response = ("{\"status\":\"error\",\"message\":\"" + e.getMessage() + "\"}").getBytes(StandardCharsets.UTF_8);
                    exchange.getResponseHeaders().set("Content-Type", "application/json");
                    exchange.sendResponseHeaders(400, response.length);
                    try (java.io.OutputStream os = exchange.getResponseBody()) {
                        os.write(response);
                    }
                }
            });

            server.setExecutor(Executors.newFixedThreadPool(2));
            server.start();
            plugin.getLogger().info("Exam Webhook Server started successfully on port " + port);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to start Exam Webhook Server on port " + port, e);
        }
    }

    public void stopWebhookServer() {
        if (server != null) {
            server.stop(0);
            server = null;
            plugin.getLogger().info("Exam Webhook Server stopped.");
        }
    }

    public int getExamVersion() {
        String dbVersionStr = plugin.getDatabaseManager().getCoreState("exam_version");
        if (dbVersionStr != null) {
            try {
                return Integer.parseInt(dbVersionStr);
            } catch (NumberFormatException e) {
                // Ignore
            }
        }
        return 1;
    }

    public String getPortalSemesterStatus() {
        String status = plugin.getDatabaseManager().getCoreState("portal_semester_status");
        return status != null ? status : "CLOSED";
    }

    public String getPortalMessage() {
        String message = plugin.getDatabaseManager().getCoreState("portal_message");
        return message != null ? message : "Portal Ujian Sedang ditutup!";
    }

    public boolean isSemesterBreak() {
        String breakStr = plugin.getDatabaseManager().getCoreState("is_semester_break");
        return "true".equalsIgnoreCase(breakStr);
    }

    public List<String> getActiveUhPackets() {
        List<String> list = new ArrayList<>();
        String raw = plugin.getDatabaseManager().getCoreState("active_uh_packets");
        if (raw != null && !raw.trim().isEmpty() && !raw.equals("[]")) {
            try {
                JsonArray arr = JsonParser.parseString(raw).getAsJsonArray();
                for (JsonElement el : arr) {
                    list.add(el.getAsString());
                }
            } catch (Exception e) {
                // Ignore
            }
        }
        return list;
    }

    public List<String> getCurrentActiveSemesterPackets() {
        List<String> list = new ArrayList<>();
        String raw = plugin.getDatabaseManager().getCoreState("current_active_semester_packets");
        if (raw != null && !raw.trim().isEmpty() && !raw.equals("[]")) {
            try {
                JsonArray arr = JsonParser.parseString(raw).getAsJsonArray();
                for (JsonElement el : arr) {
                    list.add(el.getAsString());
                }
            } catch (Exception e) {
                // Ignore
            }
        }
        return list;
    }

    @Deprecated
    public String getPortalStatus() {
        return getPortalSemesterStatus();
    }

    @Deprecated
    public List<String> getActivePacketIds() {
        return getActiveUhPackets();
    }

    public List<ExamQuestions.Question> getQuestionsForPacket(String packetId) {
        List<ExamQuestions.Question> list = new ArrayList<>();
        synchronized (cacheLock) {
            for (ExamQuestions.Question q : cachedQuestions) {
                if (q.packetId != null && q.packetId.equalsIgnoreCase(packetId)) {
                    list.add(q);
                }
            }
        }
        return list;
    }

    public CompletableFuture<Boolean> forceSyncFromDatabase() {
        return CompletableFuture.supplyAsync(() -> {
            synchronized (syncLock) {
                try {
                    String dbVersionStr = plugin.getDatabaseManager().getCoreState("exam_version");

                    int dbVersion = 1;
                    if (dbVersionStr != null) {
                        try {
                            dbVersion = Integer.parseInt(dbVersionStr);
                        } catch (NumberFormatException e) {
                            // Keep default 1
                        }
                    }

                    // Increment version to mark it updated
                    dbVersion++;
                    plugin.getDatabaseManager().setCoreState("exam_version", String.valueOf(dbVersion));

                    List<Map<String, Object>> rows = plugin.getDatabaseManager().getAllExamQuestions();
                    JsonObject root = new JsonObject();
                    root.addProperty("version", dbVersion);

                    JsonArray questionsArray = new JsonArray();
                    for (Map<String, Object> row : rows) {
                        JsonObject qObj = new JsonObject();
                        qObj.addProperty("packet_id", (String) row.get("packet_id"));
                        qObj.addProperty("academic_class", (Integer) row.get("academic_class"));
                        qObj.addProperty("question_number", (Integer) row.get("question_number"));
                        qObj.addProperty("question_type", (String) row.get("question_type"));
                        qObj.addProperty("question_text", (String) row.get("question_text"));

                        String optionsStr = (String) row.get("options");
                        if (optionsStr != null && !optionsStr.trim().isEmpty()) {
                            try {
                                qObj.add("options", JsonParser.parseString(optionsStr));
                            } catch (Exception ex) {
                                qObj.add("options", com.google.gson.JsonNull.INSTANCE);
                            }
                        } else {
                            qObj.add("options", com.google.gson.JsonNull.INSTANCE);
                        }

                        qObj.addProperty("correct_answer", (String) row.get("correct_answer"));

                        String indicesStr = (String) row.get("correct_indices");
                        if (indicesStr != null && !indicesStr.trim().isEmpty()) {
                            try {
                                qObj.add("correct_indices", JsonParser.parseString(indicesStr));
                            } catch (Exception ex) {
                                qObj.add("correct_indices", com.google.gson.JsonNull.INSTANCE);
                            }
                        } else {
                            qObj.add("correct_indices", com.google.gson.JsonNull.INSTANCE);
                        }

                        questionsArray.add(qObj);
                    }
                    root.add("questions", questionsArray);

                    // Overwrite file
                    File file = new File(plugin.getDataFolder(), "exams.json");
                    try (FileWriter writer = new FileWriter(file, StandardCharsets.UTF_8)) {
                        new GsonBuilder().setPrettyPrinting().create().toJson(root, writer);
                    }

                    // Parse into memory
                    parseMemoryFromRoot(root);
                    return true;
                } catch (Exception e) {
                    plugin.getLogger().log(Level.SEVERE, "Failed to force sync exams from database", e);
                    return false;
                }
            }
        }, runnable -> Bukkit.getScheduler().runTaskAsynchronously(plugin, runnable));
    }
}
