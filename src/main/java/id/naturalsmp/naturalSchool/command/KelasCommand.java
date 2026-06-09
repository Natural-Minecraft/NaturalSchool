package id.naturalsmp.naturalSchool.command;

import id.naturalsmp.naturalSchool.NaturalSchool;
import id.naturalsmp.naturalSchool.classsession.ClassSession;
import id.naturalsmp.naturalSchool.profile.StudentProfile;
import id.naturalsmp.naturalSchool.profile.SchoolRank;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public class KelasCommand implements CommandExecutor, TabCompleter {

    private final NaturalSchool plugin;

    public KelasCommand(NaturalSchool plugin) {
        this.plugin = plugin;
    }

    private boolean checkPermission(CommandSender sender) {
        if (sender.hasPermission("naturalschool.admin")) {
            return true;
        }
        if (sender instanceof Player) {
            Player player = (Player) sender;
            StudentProfile profile = plugin.getProfileManager().getProfile(player.getUniqueId());
            if (profile != null) {
                SchoolRank rank = profile.getRank();
                if (rank != null && (profile.isStaff() || rank.getPriority() >= SchoolRank.GURU_HONORER.getPriority())) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        // Special case: /kelas jawab can be run by anyone (students)
        if (args.length >= 1 && args[0].equalsIgnoreCase("jawab")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>Hanya player yang dapat menjawab kuis!</red>"));
                return true;
            }
            handleJawab((Player) sender, args);
            return true;
        }

        if (!checkPermission(sender)) {
            sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>Anda tidak memiliki izin/rank Guru untuk menggunakan perintah kelas!</red>"));
            return true;
        }

        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            sendHelpMessage(sender);
            return true;
        }

        String sub = args[0].toLowerCase();
        switch (sub) {
            case "start":
                handleStart(sender, args);
                break;
            case "selesai":
                handleSelesai(sender, args);
                break;
            case "selesaikan":
                handleSelesaikan(sender, args);
                break;
            case "pembelajaran":
                handlePembelajaran(sender, args);
                break;
            case "startsoal":
                handleStartSoal(sender, args);
                break;
            case "rekap":
                handleRekap(sender, args);
                break;
            default:
                sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>Perintah kelas tidak dikenal. Ketik /kelas help untuk bantuan.</red>"));
                break;
        }

        return true;
    }

    private void sendHelpMessage(CommandSender sender) {
        sender.sendMessage(MiniMessage.miniMessage().deserialize(
            "<gold>=== Perintah Manajemen Kelas Guru & Helper ===</gold>\n" +
            "<yellow>/kelas start <id_kelas> <id_matapelajaran></yellow> - <gray>Memulai sesi kelas.</gray>\n" +
            "<yellow>/kelas selesai [id_kelas]</yellow> - <gray>Mengakhiri sesi kelas dan rekap otomatis.</gray>\n" +
            "<yellow>/kelas selesaikan <player></yellow> - <gray>Pulang dini untuk murid tertentu.</gray>\n" +
            "<yellow>/kelas pembelajaran <id_kelas> <namaFile></yellow> - <gray>Memuat materi papan tulis dari website.</gray>\n" +
            "<yellow>/kelas startsoal <namaFile></yellow> - <gray>Membagikan kuis/ujian pilihan ganda.</gray>\n" +
            "<yellow>/kelas rekap <id_kelas></yellow> - <gray>1-Klik rekap nilai ke database manual.</gray>"
        ));
    }

    private void handleStart(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>Format: /kelas start <id_kelas> <id_matapelajaran></red>"));
            return;
        }

        String idKelas = args[1];
        String mapel = args.length >= 3 ? args[2] : "Materi Umum";
        UUID teacherUuid = (sender instanceof Player) ? ((Player) sender).getUniqueId() : null;

        // Auto detect class if sender is standing in region (if idKelas matches mapel check or is a mapel)
        if (sender instanceof Player && args.length == 2) {
            String detectedRegion = getPlayerRegion((Player) sender);
            if (detectedRegion != null) {
                mapel = args[1];
                idKelas = detectedRegion;
            }
        }

        boolean success = plugin.getNaturalSchoolAPI().getClassManager().startSession(idKelas, mapel, teacherUuid);
        if (success) {
            sender.sendMessage(MiniMessage.miniMessage().deserialize("<green>Sesi kelas untuk " + idKelas.toUpperCase() + " berhasil dibuka!</green>"));
        } else {
            sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>Gagal! Kelas " + idKelas.toUpperCase() + " sudah aktif saat ini.</red>"));
        }
    }

    private void handleSelesai(CommandSender sender, String[] args) {
        String idKelas = null;
        if (args.length >= 2) {
            idKelas = args[1];
        } else if (sender instanceof Player) {
            // Find active session taught by this player or where player is standing
            Player p = (Player) sender;
            String detectedRegion = getPlayerRegion(p);
            if (detectedRegion != null && plugin.getNaturalSchoolAPI().getClassManager().getSession(detectedRegion) != null) {
                idKelas = detectedRegion;
            } else {
                // Find first class taught by this helper
                for (ClassSession cs : plugin.getNaturalSchoolAPI().getClassManager().getActiveSessions().values()) {
                    if (p.getUniqueId().equals(cs.getTeacherUuid())) {
                        idKelas = cs.getIdKelas();
                        break;
                    }
                }
            }
        }

        if (idKelas == null) {
            sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>Tentukan kelas yang ingin diselesaikan! /kelas selesai <id_kelas></red>"));
            return;
        }

        boolean success = plugin.getNaturalSchoolAPI().getClassManager().stopSession(idKelas);
        if (success) {
            sender.sendMessage(MiniMessage.miniMessage().deserialize("<green>Sesi kelas " + idKelas.toUpperCase() + " berhasil ditutup.</green>"));
        } else {
            sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>Gagal! Tidak ada sesi aktif untuk kelas " + idKelas.toUpperCase() + ".</red>"));
        }
    }

    private void handleSelesaikan(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>Format: /kelas selesaikan <player></red>"));
            return;
        }

        String targetName = args[1];
        Player targetPlayer = Bukkit.getPlayerExact(targetName);
        if (targetPlayer == null || !targetPlayer.isOnline()) {
            sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>Player " + targetName + " tidak online!</red>"));
            return;
        }

        // Find which class the target player is currently in
        String classId = null;
        StudentProfile targetProfile = plugin.getProfileManager().getProfile(targetPlayer.getUniqueId());
        if (targetProfile != null) {
            classId = "kelas" + targetProfile.getAcademicClass();
        }

        if (classId == null || plugin.getNaturalSchoolAPI().getClassManager().getSession(classId) == null) {
            sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>Gagal! Murid " + targetName + " tidak berada dalam kelas dengan sesi aktif.</red>"));
            return;
        }

        boolean success = plugin.getNaturalSchoolAPI().getClassManager().dismissEarly(classId, targetPlayer.getUniqueId());
        if (success) {
            sender.sendMessage(MiniMessage.miniMessage().deserialize("<green>Berhasil meloloskan " + targetPlayer.getName() + " pulang dini dari kelas " + classId.toUpperCase() + "!</green>"));
        } else {
            sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>Gagal memproses pulang dini.</red>"));
        }
    }

    private void handlePembelajaran(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>Format: /kelas pembelajaran <id_kelas> <namaFile></red>"));
            return;
        }

        String idKelas = args[1];
        String fileName = args[2];

        boolean success = plugin.getNaturalSchoolAPI().getClassManager().loadLessonMaterial(idKelas, fileName);
        if (success) {
            sender.sendMessage(MiniMessage.miniMessage().deserialize("<green>Materi '" + fileName + "' berhasil dimuat di papan tulis " + idKelas.toUpperCase() + ".</green>"));
        } else {
            sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>Gagal! Berkas materi tidak ditemukan di database atau kelas tidak aktif.</red>"));
        }
    }

    private void handleStartSoal(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>Format: /kelas startsoal <namaFile></red>"));
            return;
        }

        String fileName = args[1];
        String idKelas = null;

        if (sender instanceof Player) {
            Player p = (Player) sender;
            // Detect class region
            String detectedRegion = getPlayerRegion(p);
            if (detectedRegion != null && plugin.getNaturalSchoolAPI().getClassManager().getSession(detectedRegion) != null) {
                idKelas = detectedRegion;
            } else {
                // Find first class taught by this helper
                for (ClassSession cs : plugin.getNaturalSchoolAPI().getClassManager().getActiveSessions().values()) {
                    if (p.getUniqueId().equals(cs.getTeacherUuid())) {
                        idKelas = cs.getIdKelas();
                        break;
                    }
                }
            }
        }

        if (idKelas == null) {
            sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>Anda harus berada di kelas yang aktif untuk menyebarkan soal kuis!</red>"));
            return;
        }

        boolean success = plugin.getNaturalSchoolAPI().getClassManager().startQuiz(idKelas, fileName);
        if (success) {
            sender.sendMessage(MiniMessage.miniMessage().deserialize("<green>Kuis '" + fileName + "' berhasil dibagikan ke siswa kelas " + idKelas.toUpperCase() + ".</green>"));
        } else {
            sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>Gagal! Berkas kuis tidak ditemukan atau kuis tidak aktif di kelas ini.</red>"));
        }
    }

    private void handleRekap(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>Format: /kelas rekap <id_kelas></red>"));
            return;
        }

        String idKelas = args[1];
        ClassSession cs = plugin.getNaturalSchoolAPI().getClassManager().getSession(idKelas);
        if (cs == null) {
            sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>Tidak ada sesi kelas aktif untuk kelas " + idKelas.toUpperCase() + ".</red>"));
            return;
        }

        plugin.getNaturalSchoolAPI().getClassManager().rekapSessionInternal(cs);
        sender.sendMessage(MiniMessage.miniMessage().deserialize("<green>Kompilasi rekap nilai/presensi untuk " + idKelas.toUpperCase() + " berhasil dipush ke database!</green>"));
    }

    private void handleJawab(Player player, String[] args) {
        if (args.length < 3) {
            player.sendMessage(MiniMessage.miniMessage().deserialize("<red>Format: /kelas jawab <index> <jawaban></red>"));
            return;
        }

        int index;
        try {
            index = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            player.sendMessage(MiniMessage.miniMessage().deserialize("<red>Nomor soal harus berupa angka!</red>"));
            return;
        }

        String jawaban = args[2];

        // Find which class session matches this student
        String idKelas = null;
        StudentProfile profile = plugin.getProfileManager().getProfile(player.getUniqueId());
        if (profile != null) {
            idKelas = "kelas" + profile.getAcademicClass();
        }

        if (idKelas == null || plugin.getNaturalSchoolAPI().getClassManager().getSession(idKelas) == null) {
            player.sendMessage(MiniMessage.miniMessage().deserialize("<red>Gagal! Anda sedang tidak berada dalam kelas dengan sesi aktif.</red>"));
            return;
        }

        boolean success = plugin.getNaturalSchoolAPI().getClassManager().submitAnswer(idKelas, player.getUniqueId(), index, jawaban);
        if (success) {
            player.sendMessage(MiniMessage.miniMessage().deserialize("<green>Jawaban untuk soal nomor " + (index + 1) + " berhasil direkam.</green>"));
        } else {
            player.sendMessage(MiniMessage.miniMessage().deserialize("<red>Gagal merekam jawaban. Kuis belum dimulai oleh guru.</red>"));
        }
    }

    private String getPlayerRegion(Player player) {
        try {
            if (Bukkit.getPluginManager().getPlugin("WorldGuard") == null) return null;
            Class<?> wgClass = Class.forName("com.sk89q.worldguard.WorldGuard");
            Object wgInstance = wgClass.getMethod("getInstance").invoke(null);
            Object platform = wgClass.getMethod("getPlatform").invoke(wgInstance);
            Object container = platform.getClass().getMethod("getRegionContainer").invoke(platform);

            Class<?> bukkitWorldClass = Class.forName("com.sk89q.worldedit.bukkit.BukkitWorld");
            Object editWorld = bukkitWorldClass.getConstructor(org.bukkit.World.class).newInstance(player.getWorld());
            
            Class<?> locationClass = Class.forName("com.sk89q.worldedit.util.Location");
            Object loc = locationClass.getConstructor(
                Class.forName("com.sk89q.worldedit.world.World"),
                Double.TYPE, Double.TYPE, Double.TYPE
            ).newInstance(editWorld, player.getLocation().getX(), player.getLocation().getY(), player.getLocation().getZ());

            Object manager = container.getClass().getMethod("get", Class.forName("com.sk89q.worldedit.world.World")).invoke(container, editWorld);
            if (manager != null) {
                Object set = manager.getClass().getMethod("getApplicableRegions", Class.forName("com.sk89q.worldedit.util.Location")).invoke(manager, loc);
                if (set != null) {
                    java.util.Collection<?> regions = (java.util.Collection<?>) set.getClass().getMethod("getRegions").invoke(set);
                    for (Object regionObj : regions) {
                        String id = (String) regionObj.getClass().getMethod("getId").invoke(regionObj);
                        if (id.toLowerCase().startsWith("kelas")) {
                            return id;
                        }
                    }
                }
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!checkPermission(sender)) {
            return Collections.emptyList();
        }

        if (args.length == 1) {
            return Arrays.asList("start", "selesai", "selesaikan", "pembelajaran", "startsoal", "rekap").stream()
                    .filter(s -> s.toLowerCase().startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }

        if (args.length == 2) {
            String sub = args[0].toLowerCase();
            if (Arrays.asList("start", "pembelajaran", "rekap", "selesai").contains(sub)) {
                // Return standard classes
                List<String> classes = new ArrayList<>();
                for (int i = 1; i <= 12; i++) {
                    classes.add("kelas" + i);
                }
                return classes.stream().filter(s -> s.toLowerCase().startsWith(args[1].toLowerCase())).collect(Collectors.toList());
            } else if ("selesaikan".equals(sub)) {
                // Complete online players
                return Bukkit.getOnlinePlayers().stream().map(Player::getName)
                        .filter(s -> s.toLowerCase().startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());
            } else if ("startsoal".equals(sub)) {
                // Complete with lesson files that are quizzes
                return getLessonFilesNames("SOAL_KUIS").stream()
                        .filter(s -> s.toLowerCase().startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());
            }
        }

        if (args.length == 3) {
            String sub = args[0].toLowerCase();
            if ("pembelajaran".equals(sub)) {
                // Complete with lesson files that are materials
                return getLessonFilesNames("MATERI_PROYEKTOR").stream()
                        .filter(s -> s.toLowerCase().startsWith(args[2].toLowerCase()))
                        .collect(Collectors.toList());
            } else if ("start".equals(sub)) {
                // Complete with some common subjects
                return Arrays.asList("Matematika", "Bahasa_Indonesia", "Bahasa_Inggris", "Fisika", "Kimia", "Biologi", "IPS", "PKN")
                        .stream().filter(s -> s.toLowerCase().startsWith(args[2].toLowerCase()))
                        .collect(Collectors.toList());
            }
        }

        return Collections.emptyList();
    }

    private List<String> getLessonFilesNames(String type) {
        List<String> list = new ArrayList<>();
        String query = "SELECT nama_file FROM natural_lesson_files WHERE tipe = ?;";
        try (Connection conn = plugin.getDatabaseManager().getConnection();
             PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setString(1, type);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(rs.getString("nama_file"));
                }
            }
        } catch (SQLException e) {
            // Ignore
        }
        return list;
    }
}
