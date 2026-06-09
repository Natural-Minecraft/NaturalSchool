package id.naturalsmp.naturalSchool.exam;

import com.google.gson.Gson;
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

    // Cache variables
    private volatile String portalStatus = "CLOSED";
    private volatile String portalMessage = "Portal Ujian Sedang ditutup!";
    private volatile int examVersion = 1;
    private final List<String> activePacketIds = new ArrayList<>();
    private final List<ExamQuestions.Question> cachedQuestions = new ArrayList<>();

    private HttpServer server;

    public ExamManager(NaturalSchool plugin) {
        this.plugin = plugin;
    }

    public void initialize() {
        // 1. Pull core state from database synchronously on boot
        String dbVersionStr = plugin.getDatabaseManager().getCoreState("exam_version");
        String dbStatus = plugin.getDatabaseManager().getCoreState("portal_status");
        String dbMessage = plugin.getDatabaseManager().getCoreState("portal_message");
        String dbActivePacketsStr = plugin.getDatabaseManager().getCoreState("active_packet_ids");

        int dbVersion = 1;
        if (dbVersionStr != null) {
            try {
                dbVersion = Integer.parseInt(dbVersionStr);
            } catch (NumberFormatException e) {
                // Keep default 1
            }
        }

        final int finalDbVersion = dbVersion;
        final String dbStatusVal = dbStatus != null ? dbStatus : "CLOSED";
        final String dbMessageVal = dbMessage != null ? dbMessage : "Portal Ujian Sedang ditutup!";
        final String dbActivePacketsVal = dbActivePacketsStr != null ? dbActivePacketsStr : "[]";

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
            CompletableFuture.runAsync(() -> {
                try {
                    List<Map<String, Object>> rows = plugin.getDatabaseManager().getAllExamQuestions();
                    JsonObject root = new JsonObject();
                    root.addProperty("version", finalDbVersion);
                    root.addProperty("portal_status", dbStatusVal);
                    root.addProperty("portal_message", dbMessageVal);
                    
                    try {
                        root.add("active_packet_ids", JsonParser.parseString(dbActivePacketsVal));
                    } catch (Exception e) {
                        root.add("active_packet_ids", new JsonArray());
                    }

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
                    List<ExamQuestions.Question> tempQuestions = parseQuestionsFromRoot(root);
                    List<String> tempActivePackets = parseActivePacketIdsFromRoot(root);
                    synchronized (cacheLock) {
                        this.examVersion = finalDbVersion;
                        this.portalStatus = dbStatusVal;
                        this.portalMessage = dbMessageVal;
                        this.activePacketIds.clear();
                        this.activePacketIds.addAll(tempActivePackets);
                        this.cachedQuestions.clear();
                        this.cachedQuestions.addAll(tempQuestions);
                    }
                    plugin.getLogger().info("exams.json successfully populated from database with " + tempQuestions.size() + " questions.");
                } catch (Exception e) {
                    plugin.getLogger().log(Level.SEVERE, "Failed to synchronize exams from database asynchronously", e);
                }
            });
        } else {
            // Load questions from local JSON into memory
            plugin.getLogger().info("exams.json is up-to-date. Loading questions into memory...");
            try (FileReader reader = new FileReader(file, StandardCharsets.UTF_8)) {
                JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
                int ver = root.has("version") ? root.get("version").getAsInt() : localVersion;
                String status = root.has("portal_status") ? root.get("portal_status").getAsString() : "CLOSED";
                String msg = root.has("portal_message") ? root.get("portal_message").getAsString() : "Portal Ujian Sedang ditutup!";
                
                List<ExamQuestions.Question> tempQuestions = parseQuestionsFromRoot(root);
                List<String> tempActivePackets = parseActivePacketIdsFromRoot(root);
                synchronized (cacheLock) {
                    this.examVersion = ver;
                    this.portalStatus = status;
                    this.portalMessage = msg;
                    this.activePacketIds.clear();
                    this.activePacketIds.addAll(tempActivePackets);
                    this.cachedQuestions.clear();
                    this.cachedQuestions.addAll(tempQuestions);
                }
                plugin.getLogger().info("exams.json successfully loaded with " + tempQuestions.size() + " questions.");
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

    private List<String> parseActivePacketIdsFromRoot(JsonObject root) {
        List<String> list = new ArrayList<>();
        if (root.has("active_packet_ids") && !root.get("active_packet_ids").isJsonNull()) {
            try {
                JsonArray arr = root.getAsJsonArray("active_packet_ids");
                for (JsonElement el : arr) {
                    list.add(el.getAsString());
                }
            } catch (Exception e) {
                // ignore
            }
        }
        return list;
    }

    public void startWebhookServer() {
        int port = plugin.getConfig().getInt("api.port", 8080);
        try {
            server = HttpServer.create(new java.net.InetSocketAddress(port), 0);
            server.createContext("/school/exam/update", exchange -> {
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
                    int version = root.get("version").getAsInt();
                    String status = root.get("portal_status").getAsString();
                    String msg = root.get("portal_message").getAsString();

                    // Overwrite local file
                    File file = new File(plugin.getDataFolder(), "exams.json");
                    try (FileWriter fw = new FileWriter(file, StandardCharsets.UTF_8)) {
                        fw.write(body);
                    }

                    // Update memory
                    List<ExamQuestions.Question> tempQuestions = parseQuestionsFromRoot(root);
                    List<String> tempActivePackets = parseActivePacketIdsFromRoot(root);
                    synchronized (cacheLock) {
                        this.examVersion = version;
                        this.portalStatus = status;
                        this.portalMessage = msg;
                        this.activePacketIds.clear();
                        this.activePacketIds.addAll(tempActivePackets);
                        this.cachedQuestions.clear();
                        this.cachedQuestions.addAll(tempQuestions);
                    }

                    plugin.getLogger().info("Exam Subsystem updated via webhook. Version: " + version + ", Status: " + status);

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

    public String getPortalStatus() {
        return portalStatus;
    }

    public String getPortalMessage() {
        return portalMessage;
    }

    public int getExamVersion() {
        return examVersion;
    }

    public List<String> getActivePacketIds() {
        synchronized (cacheLock) {
            return new ArrayList<>(activePacketIds);
        }
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
}

