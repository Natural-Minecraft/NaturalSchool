package id.naturalsmp.naturalSchool.classes;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import id.naturalsmp.naturalSchool.NaturalSchool;
import id.naturalsmp.naturalSchool.profile.StudentProfile;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.ZonedDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public class ClassManager {

    private final NaturalSchool plugin;
    private final Map<String, ClassSession> activeSessions = new ConcurrentHashMap<>();

    // Map to track student answers during an active quiz: idKelas -> (studentUuid -> (questionIndex -> answer))
    private final Map<String, Map<UUID, Map<Integer, String>>> studentAnswers = new ConcurrentHashMap<>();

    public ClassManager(NaturalSchool plugin) {
        this.plugin = plugin;
    }

    public Map<String, ClassSession> getActiveSessions() {
        return activeSessions;
    }

    public ClassSession getSession(String idKelas) {
        if (idKelas == null) return null;
        return activeSessions.get(idKelas.toLowerCase());
    }

    public boolean startSession(String idKelas, String subject, UUID teacherUuid) {
        String key = idKelas.toLowerCase();
        if (activeSessions.containsKey(key)) {
            return false;
        }

        ClassSession session = new ClassSession(idKelas, subject, teacherUuid);
        activeSessions.put(key, session);
        studentAnswers.put(key, new ConcurrentHashMap<>());

        // Attempt to lock class region via WorldGuard
        setWorldGuardRegionEntry(idKelas, false);

        // Toggle tinted glass doors to AIR
        plugin.getClassroomManager().toggleDoors(getClassNumber(idKelas), true);

        String teacherName = "System";
        if (teacherUuid != null) {
            Player p = Bukkit.getPlayer(teacherUuid);
            if (p != null) teacherName = p.getName();
        }

        // Broadcast to server / channel
        String msg = "<gray>[NaturalSchool]</gray> <green>Sesi Kelas untuk <yellow>" + idKelas.toUpperCase() + "</yellow> (Pelajaran: <yellow>" + subject + "</yellow>) telah dimulai oleh <aqua>" + teacherName + "</aqua>!</green>";
        Bukkit.broadcast(MiniMessage.miniMessage().deserialize(msg));

        // Send Discord log
        sendDiscordLog("Sesi Kelas Dimulai 🏫", 
            "**Kelas:** " + idKelas.toUpperCase() + "\n" +
            "**Mata Pelajaran:** " + subject + "\n" +
            "**Helper Pengajar:** " + teacherName, 
            3066993); // Green

        // Record initial attendance for online students of this class who are online at start
        int classNum = getClassNumber(idKelas);
        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            StudentProfile profile = plugin.getProfileManager().getProfile(onlinePlayer.getUniqueId());
            if (profile != null && isPlayerInClass(profile, idKelas)) {
                // Determine status (Hadir or Terlambat depending on time)
                String status = "HADIR";
                ZonedDateTime nowWib = ZonedDateTime.now(ZoneId.of("Asia/Jakarta"));
                if (nowWib.getHour() >= 20) {
                    status = "TERLAMBAT";
                }
                session.getAttendanceMap().put(onlinePlayer.getUniqueId(), status);
            }
        }

        return true;
    }

    public boolean stopSession(String idKelas) {
        String key = idKelas.toLowerCase();
        ClassSession session = activeSessions.remove(key);
        if (session == null) {
            return false;
        }

        session.setClosed(true);
        setWorldGuardRegionEntry(idKelas, true);

        // Toggle tinted glass doors to TINTED_GLASS
        plugin.getClassroomManager().toggleDoors(getClassNumber(idKelas), false);

        // Run rekapitulasi / save to DB
        rekapSessionInternal(session);
        studentAnswers.remove(key);

        String msg = "<gray>[NaturalSchool]</gray> <red>Sesi Kelas <yellow>" + idKelas.toUpperCase() + "</yellow> telah berakhir.</red>";
        Bukkit.broadcast(MiniMessage.miniMessage().deserialize(msg));

        sendDiscordLog("Sesi Kelas Berakhir 🔔", 
            "**Kelas:** " + idKelas.toUpperCase() + "\n" +
            "**Mata Pelajaran:** " + session.getSubject() + "\n" +
            "Sesi kelas telah direkapitulasi secara otomatis ke database.", 
            15158332); // Red

        return true;
    }

    public boolean loadLessonMaterial(String idKelas, String fileName) {
        ClassSession session = getSession(idKelas);
        if (session == null) return false;

        Map<String, Object> fileData = plugin.getDatabaseManager().loadLessonFileByName(fileName);
        if (fileData == null) return false;

        String content = (String) fileData.get("konten_json");
        session.setProjectorFileName(fileName);
        session.setProjectorContent(content);

        plugin.getDatabaseManager().updateLessonFileUsedTime(fileName);

        // Broadcast content to enrolled students
        String header = "<gold>=== MATERI PAPAN TULIS: " + fileName.toUpperCase() + " ===</gold>";
        broadcastToClass(idKelas, header);
        broadcastToClass(idKelas, "<white>" + content + "</white>");
        broadcastToClass(idKelas, "<gold>===========================================</gold>");

        return true;
    }

    public boolean startQuiz(String idKelas, String fileName) {
        ClassSession session = getSession(idKelas);
        if (session == null) return false;

        Map<String, Object> fileData = plugin.getDatabaseManager().loadLessonFileByName(fileName);
        if (fileData == null) return false;

        String contentJson = (String) fileData.get("konten_json");
        session.setQuizFileName(fileName);
        session.setQuizContentJson(contentJson);
        session.setQuizStarted(true);

        plugin.getDatabaseManager().updateLessonFileUsedTime(fileName);

        broadcastToClass(idKelas, "<green>⚡ Kuis/Ujian <b>" + fileName.toUpperCase() + "</b> telah dibagikan! Selesaikan menggunakan Custom UI.</green>");

        return true;
    }

    public boolean submitAnswer(String idKelas, UUID studentUuid, int questionIndex, String answer) {
        ClassSession session = getSession(idKelas);
        if (session == null || !session.isQuizStarted()) return false;

        String key = idKelas.toLowerCase();
        Map<UUID, Map<Integer, String>> classAnswers = studentAnswers.get(key);
        if (classAnswers == null) return false;

        classAnswers.computeIfAbsent(studentUuid, u -> new ConcurrentHashMap<>()).put(questionIndex, answer);

        // Calculate score on the fly for this student
        calculateAndSetGrade(session, studentUuid);
        return true;
    }

    public void calculateAndSetGrade(ClassSession session, UUID studentUuid) {
        String key = session.getIdKelas().toLowerCase();
        Map<UUID, Map<Integer, String>> classAnswers = studentAnswers.get(key);
        if (classAnswers == null) return;

        Map<Integer, String> answers = classAnswers.get(studentUuid);
        if (answers == null || session.getQuizContentJson() == null) return;

        try {
            JsonElement jsonElement = JsonParser.parseString(session.getQuizContentJson());
            if (!jsonElement.isJsonArray()) return;

            JsonArray questions = jsonElement.getAsJsonArray();
            int total = questions.size();
            if (total == 0) return;

            int correct = 0;
            for (int i = 0; i < total; i++) {
                JsonObject qObj = questions.get(i).getAsJsonObject();
                String correctAns = qObj.has("correct_answer") ? qObj.get("correct_answer").getAsString() : "";
                String studentAns = answers.getOrDefault(i, "");
                if (correctAns.equalsIgnoreCase(studentAns.trim())) {
                    correct++;
                }
            }

            int score = (int) Math.round((correct / (double) total) * 100.0);
            session.getGradesMap().put(studentUuid, score);
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error calculating quiz grade for student " + studentUuid, e);
        }
    }

    public boolean dismissEarly(String idKelas, UUID studentUuid) {
        ClassSession session = getSession(idKelas);
        if (session == null) return false;

        session.getEarlyDismissed().add(studentUuid);
        session.getAttendanceMap().put(studentUuid, "HADIR"); // Lock presence

        // Open WorldGuard region passage for them (usually handled dynamically by listener)
        Player p = Bukkit.getPlayer(studentUuid);
        if (p != null) {
            p.sendMessage(MiniMessage.miniMessage().deserialize("<green>Anda telah diperbolehkan pulang dini oleh Guru Piket! Status presensi Anda aman.</green>"));
        }
        return true;
    }

    public void rekapSessionInternal(ClassSession session) {
        String idKelas = session.getIdKelas();
        int classNum = getClassNumber(idKelas);
        String jenjang = getJenjangFromId(idKelas);
        String idHelper = session.getTeacherUuid() != null ? session.getTeacherUuid().toString() : "SYSTEM";

        // Query database to get all registered students in this class
        List<Map<String, String>> registeredStudents = plugin.getDatabaseManager().getStudentsInClass(classNum);

        // Process each registered student
        for (Map<String, String> student : registeredStudents) {
            UUID studentUuid = UUID.fromString(student.get("uuid"));
            String studentName = student.get("username");

            // Calculate final quiz grade if they answered
            calculateAndSetGrade(session, studentUuid);

            // 1. Attendance status
            String status = session.getAttendanceMap().get(studentUuid);
            if (status == null) {
                // If they are online, check if they were in region.
                Player p = Bukkit.getPlayer(studentUuid);
                if (p != null && p.isOnline()) {
                    status = "HADIR"; // fallback for online players
                } else {
                    status = "ALFA"; // membolos
                }
            }

            // Save to database
            plugin.getDatabaseManager().saveAttendance(studentUuid.toString(), studentName, idKelas, idHelper, session.getSubject(), status);

            // 2. Quiz Grade
            Integer score = session.getGradesMap().get(studentUuid);
            if (score != null) {
                plugin.getDatabaseManager().saveGrade(studentUuid.toString(), jenjang, session.getSubject(), score, "HARIAN", "NILAI_KUIS_KELAS");
            } else {
                // If attended but no quiz, set 0
                if ("HADIR".equals(status) || "TERLAMBAT".equals(status)) {
                    plugin.getDatabaseManager().saveGrade(studentUuid.toString(), jenjang, session.getSubject(), 0, "HARIAN", "TIDAK_MENGERJAKAN_KUIS");
                }
            }
        }
    }

    public void runAutoFallback(String idKelas) {
        String jenjang = getJenjangFromId(idKelas);
        int classNum = getClassNumber(idKelas);
        String subject = "Pelajaran Mandiri";

        // Look up latest projector file
        Map<String, Object> materialFile = plugin.getDatabaseManager().loadLatestLessonFile(jenjang, null, "MATERI_PROYEKTOR");
        Map<String, Object> quizFile = plugin.getDatabaseManager().loadLatestLessonFile(jenjang, null, "SOAL_KUIS");

        if (materialFile != null || quizFile != null) {
            // Case 1: Load latest files and run auto class
            String matName = materialFile != null ? (String) materialFile.get("nama_file") : "None";
            String matSubject = materialFile != null ? (String) materialFile.get("mata_pelajaran") : "Belajar";
            
            startSession(idKelas, matSubject, null);
            
            if (materialFile != null) {
                loadLessonMaterial(idKelas, (String) materialFile.get("nama_file"));
            }
            if (quizFile != null) {
                startQuiz(idKelas, (String) quizFile.get("nama_file"));
            }

            String msg = "<gray>[NaturalSchool]</gray> <yellow>Auto-Fallback: Membuka kelas mandiri untuk " + idKelas.toUpperCase() + " menggunakan materi '" + matName + "'.</yellow>";
            Bukkit.broadcast(MiniMessage.miniMessage().deserialize(msg));

            sendDiscordLog("Auto-Fallback Kelas Mandiri 🤖", 
                "**Kelas:** " + idKelas.toUpperCase() + "\n" +
                "**Status:** Helper piket absen. Sistem berhasil memuat berkas cadangan otomatis.\n" +
                "**Materi:** " + matName + "\n" +
                "**Kuis:** " + (quizFile != null ? (String) quizFile.get("nama_file") : "Tidak ada"), 
                16753920); // Yellow
        } else {
            // Case 2: No files exist. Auto inject grade 88 to all registered students
            List<Map<String, String>> registeredStudents = plugin.getDatabaseManager().getStudentsInClass(classNum);

            for (Map<String, String> student : registeredStudents) {
                UUID studentUuid = UUID.fromString(student.get("uuid"));
                plugin.getDatabaseManager().saveGrade(studentUuid.toString(), jenjang, "Pelajaran Umum", 88, "HARIAN", "NILAI_88_GURU_ABSEN");
                // Save attendance as IZIN/HADIR so they don't get ALFA penalty
                plugin.getDatabaseManager().saveAttendance(studentUuid.toString(), student.get("username"), idKelas, "SYSTEM", "Pelajaran Umum", "HADIR");
            }

            String msg = "<gray>[NaturalSchool]</gray> <red>Auto-Fallback: Guru absen dan tidak ada berkas cadangan. Seluruh murid kelas " + idKelas.toUpperCase() + " mendapatkan nilai 88 otomatis.</red>";
            Bukkit.broadcast(MiniMessage.miniMessage().deserialize(msg));

            sendDiscordLog("Guardian Policy: Injeksi Nilai 88 ⚠️", 
                "**Kelas:** " + idKelas.toUpperCase() + "\n" +
                "**Status:** Kelalaian Guru/Helper (Tidak ada file materi/kuis sama sekali di DB).\n" +
                "**Tindakan:** Seluruh murid terdaftar mendapatkan nilai **88** otomatis untuk hari ini tanpa penalti ALFA.", 
                15158332); // Red
        }
    }

    public void broadcastToClass(String idKelas, String message) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            StudentProfile profile = plugin.getProfileManager().getProfile(player.getUniqueId());
            if (profile != null && isPlayerInClass(profile, idKelas)) {
                player.sendMessage(MiniMessage.miniMessage().deserialize(message));
            }
        }
    }

    public boolean isPlayerInClass(StudentProfile profile, String idKelas) {
        if (profile == null) return false;
        if (profile.isStaff() || profile.isManagement()) return true;

        String stage = profile.getAcademicStage();
        int classNum = profile.getAcademicClass();
        String computedId = getKelasId(stage, classNum);
        return idKelas.equalsIgnoreCase(computedId);
    }

    public static String getKelasId(String stage, int classNum) {
        if (stage == null) return "NONE";
        return "kelas" + classNum; // simple standard binding
    }

    public static int getClassNumber(String idKelas) {
        if (idKelas == null) return 0;
        try {
            return Integer.parseInt(idKelas.replaceAll("\\D+", ""));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    public static String getJenjangFromId(String idKelas) {
        int classNum = getClassNumber(idKelas);
        if (classNum >= 1 && classNum <= 6) return "SD";
        if (classNum >= 7 && classNum <= 9) return "SMP";
        if (classNum >= 10 && classNum <= 12) return "SMA";
        return "SD";
    }

    private void setWorldGuardRegionEntry(String regionId, boolean allow) {
        // Query custom region name from DB if available, fallback to default regionId (e.g. "kelas10")
        String customRegionName = regionId;
        try {
            int classNum = getClassNumber(regionId);
            if (classNum > 0) {
                String dbRegion = plugin.getClassroomManager().getClassroomRegion(classNum);
                if (dbRegion != null && !dbRegion.isEmpty()) {
                    customRegionName = dbRegion;
                }
            }
        } catch (Exception ignored) {}

        try {
            if (Bukkit.getPluginManager().getPlugin("WorldGuard") == null) return;
            Class<?> wgClass = Class.forName("com.sk89q.worldguard.WorldGuard");
            Object wgInstance = wgClass.getMethod("getInstance").invoke(null);
            Object platform = wgClass.getMethod("getPlatform").invoke(wgInstance);
            Object container = platform.getClass().getMethod("getRegionContainer").invoke(platform);

            Class<?> bukkitWorldClass = Class.forName("com.sk89q.worldedit.bukkit.BukkitWorld");
            Class<?> stateFlagClass = Class.forName("com.sk89q.worldguard.protection.flags.StateFlag");
            Class<?> stateEnumClass = Class.forName("com.sk89q.worldguard.protection.flags.StateFlag$State");
            Class<?> flagsClass = Class.forName("com.sk89q.worldguard.protection.flags.Flags");

            Object entryFlag = flagsClass.getField("ENTRY").get(null);
            Object denyState = stateEnumClass.getField("DENY").get(null);

            for (org.bukkit.World w : Bukkit.getWorlds()) {
                Object editWorld = bukkitWorldClass.getConstructor(org.bukkit.World.class).newInstance(w);
                Object manager = container.getClass().getMethod("get", Class.forName("com.sk89q.worldedit.world.World")).invoke(container, editWorld);
                if (manager != null) {
                    Object region = manager.getClass().getMethod("getRegion", String.class).invoke(manager, customRegionName);
                    if (region != null) {
                        region.getClass().getMethod("setFlag", Class.forName("com.sk89q.worldguard.protection.flags.Flag"), Object.class)
                              .invoke(region, entryFlag, allow ? null : denyState);
                    }
                }
            }
        } catch (Throwable t) {
            // Silent fallback
        }
    }

    public void sendDiscordLog(String title, String description, int color) {
        String urlString = plugin.getConfig().getString("api.discord-webhook-url", "none");
        if (urlString == null || urlString.isEmpty() || urlString.equalsIgnoreCase("none")) {
            return;
        }

        CompletableFuture.runAsync(() -> {
            try {
                URL url = new URL(urlString);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json; utf-8");
                conn.setDoOutput(true);

                // Construct Simple Rich Embed Payload
                JsonObject payload = new JsonObject();
                payload.addProperty("username", "NaturalSchool Log");
                payload.addProperty("avatar_url", "https://naturalsmp.net/img/logo.png");

                JsonArray embeds = new JsonArray();
                JsonObject embed = new JsonObject();
                embed.addProperty("title", title);
                embed.addProperty("description", description);
                embed.addProperty("color", color);
                embed.addProperty("timestamp", ZonedDateTime.now(ZoneId.of("Asia/Jakarta")).toOffsetDateTime().toString());

                embeds.add(embed);
                payload.add("embeds", embeds);

                String json = new Gson().toJson(payload);

                try (OutputStream os = conn.getOutputStream()) {
                    byte[] input = json.getBytes(StandardCharsets.UTF_8);
                    os.write(input, 0, input.length);
                }

                int responseCode = conn.getResponseCode();
                if (responseCode < 200 || responseCode >= 300) {
                    plugin.getLogger().warning("Discord Webhook responded with code " + responseCode);
                }
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Failed to send Discord log webhook", e);
            }
        });
    }
}
