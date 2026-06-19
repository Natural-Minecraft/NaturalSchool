package id.naturalsmp.naturalSchool.command;

import id.naturalsmp.naturalSchool.NaturalSchool;
import id.naturalsmp.naturalSchool.classes.ClassroomManager;
import id.naturalsmp.naturalSchool.classes.ClassSession;
import id.naturalsmp.naturalSchool.profile.StudentProfile;
import id.naturalsmp.naturalSchool.profile.SchoolRank;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
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
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

public class ClassCommand implements CommandExecutor, TabCompleter {

    private final NaturalSchool plugin;

    public ClassCommand(NaturalSchool plugin) {
        this.plugin = plugin;
    }

    private boolean checkStaffPermission(CommandSender sender) {
        if (sender.hasPermission("naturalschool.admin")) return true;
        if (!(sender instanceof Player)) return true; // console
        Player player = (Player) sender;
        StudentProfile profile = plugin.getProfileManager().getProfile(player.getUniqueId());
        if (profile != null) {
            if (profile.isStaff() || profile.isManagement()) return true;
            SchoolRank rank = profile.getRank();
            if (rank != null && rank.getPriority() >= SchoolRank.GURU_HONORER.getPriority()) {
                return true;
            }
        }
        for (ClassroomManager.ClassroomData data : plugin.getClassroomManager().getAllClassroomData()) {
            if (player.getUniqueId().equals(data.getWaliKelasUuid())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        // Special subcommands that can be run by students
        if (args.length >= 1) {
            String sub = args[0].toLowerCase();
            if (sub.equals("jawab")) {
                if (!(sender instanceof Player)) {
                    sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>Hanya player yang dapat menjawab kuis!</red>"));
                    return true;
                }
                handleJawab((Player) sender, args);
                return true;
            } else if (sub.equals("chat")) {
                if (!(sender instanceof Player)) {
                    sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>Hanya player yang dapat menggunakan chat kelas!</red>"));
                    return true;
                }
                handleChat((Player) sender, args);
                return true;
            } else if (sub.equals("info")) {
                handleInfo(sender, args);
                return true;
            } else if (sub.equals("fund") || sub.equals("bank") || sub.equals("gui")) {
                handleFund(sender, args);
                return true;
            } else if (sub.equals("help")) {
                sendHelpMessage(sender);
                return true;
            }
        } else {
            sendHelpMessage(sender);
            return true;
        }

        // Staff-only commands
        if (!checkStaffPermission(sender)) {
            sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>Anda tidak memiliki izin/rank Guru untuk menggunakan perintah ini!</red>"));
            return true;
        }

        String sub = args[0].toLowerCase();
        switch (sub) {
            case "start":
                handleStart(sender, args);
                break;
            case "selesai":
            case "stop":
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
            case "officer":
                handleOfficer(sender, args);
                break;
            default:
                sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>Perintah kelas tidak dikenal. Ketik /class help untuk bantuan.</red>"));
                break;
        }

        return true;
    }

    private void sendHelpMessage(CommandSender sender) {
        sender.sendMessage(MiniMessage.miniMessage().deserialize(
            "<gold>=== Perintah Kelas Siswa ===</gold>\n" +
            "<yellow>/class chat <pesan></yellow> - <gray>Mengirim pesan ke obrolan kelas</gray>\n" +
            "<yellow>/class chat</yellow> - <gray>Masuk/keluar saluran chat kelas (Toggle)</gray>\n" +
            "<yellow>/class chat norank</yellow> - <gray>Sembunyikan rank LuckPerms di chat kelas</gray>\n" +
            "<yellow>/class info</yellow> - <gray>Menampilkan kepengurusan kelas Anda saat ini</gray>\n" +
            "<yellow>/class gui</yellow> - <gray>Membuka GUI Hub Kelas & Kas</gray>\n" +
            "<yellow>/class fund bayar</yellow> - <gray>Membayar kas mingguan kelas</gray>\n" +
            "<yellow>/class fund history</yellow> - <gray>Melihat riwayat transaksi kas kelas</gray>"
        ));

        if (checkStaffPermission(sender)) {
            sender.sendMessage(MiniMessage.miniMessage().deserialize(
                "\n<gold>=== Perintah Kelas Staf ===</gold>\n" +
                "<yellow>/class start <id_kelas> <id_matapelajaran></yellow> - <gray>Memulai sesi pembelajaran kelas</gray>\n" +
                "<yellow>/class selesai [id_kelas]</yellow> - <gray>Mengakhiri sesi pembelajaran kelas</gray>\n" +
                "<yellow>/class selesaikan <player></yellow> - <gray>Pulang dini untuk murid tertentu</gray>\n" +
                "<yellow>/class pembelajaran <id_kelas> <namaFile></yellow> - <gray>Memuat materi papan tulis proyektor</gray>\n" +
                "<yellow>/class startsoal <namaFile></yellow> - <gray>Memulai kuis kelas</gray>\n" +
                "<yellow>/class rekap <id_kelas></yellow> - <gray>Merekap sesi pembelajaran kelas secara manual</gray>\n" +
                "<yellow>/class officer <add|remove|clear|list></yellow> - <gray>Manajemen pengurus kelas</gray>"
            ));
        }
    }

    private void handleChat(Player player, String[] args) {
        if (args.length == 1) {
            // Toggle chat channel
            boolean active = plugin.getClassChatManager().toggleClassChatChannel(player.getUniqueId());
            if (active) {
                player.sendMessage(MiniMessage.miniMessage().deserialize("<green>Anda masuk ke saluran chat kelas. Semua pesan chat biasa akan diarahkan ke kelas.</green>"));
            } else {
                player.sendMessage(MiniMessage.miniMessage().deserialize("<yellow>Anda keluar dari saluran chat kelas. Chat kembali normal.</yellow>"));
            }
            return;
        }

        if (args.length == 2 && args[1].equalsIgnoreCase("norank")) {
            boolean active = plugin.getClassChatManager().toggleNoRank(player.getUniqueId());
            if (active) {
                player.sendMessage(MiniMessage.miniMessage().deserialize("<green>Rank LuckPerms sekarang disembunyikan di chat kelas.</green>"));
            } else {
                player.sendMessage(MiniMessage.miniMessage().deserialize("<yellow>Rank LuckPerms kembali ditampilkan di chat kelas.</yellow>"));
            }
            return;
        }

        // Send a one-off class chat message
        StringBuilder sb = new StringBuilder();
        for (int i = 1; i < args.length; i++) {
            sb.append(args[i]).append(" ");
        }
        String msg = sb.toString().trim();
        plugin.getClassChatManager().sendClassChat(player, msg);
    }

    private void handleInfo(CommandSender sender, String[] args) {
        int classNum = 0;
        if (sender instanceof Player) {
            StudentProfile profile = plugin.getProfileManager().getProfile(((Player) sender).getUniqueId());
            if (profile != null) {
                classNum = profile.getAcademicClass();
            }
        }
        
        if (classNum == 0) {
            sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>Anda tidak berada di kelas mana pun. Gunakan /school class <kelas/player> untuk melihat kelas lain.</red>"));
            return;
        }

        displayClassroomInfo(sender, classNum);
    }

    public void displayClassroomInfo(CommandSender sender, int classNum) {
        ClassroomManager.ClassroomData data = plugin.getClassroomManager().getClassroom(classNum);
        String className = plugin.getRankPrefixConfig().getClassPrefix(classNum);
        if (className == null || className.isEmpty()) {
            className = "Kelas " + classNum;
        }

        String waliKelasName = "Tidak Ada";
        if (data.getWaliKelasUuid() != null) {
            OfflinePlayer op = Bukkit.getOfflinePlayer(data.getWaliKelasUuid());
            if (op.getName() != null) {
                waliKelasName = op.getName();
            }
        }

        Map<UUID, ClassroomManager.OfficerInfo> officers = data.getOfficers();
        String ketua = "Tidak Ada";
        String wakil = "Tidak Ada";
        String sekretaris = "Tidak Ada";
        String bendahara = "Tidak Ada";

        for (Map.Entry<UUID, ClassroomManager.OfficerInfo> entry : officers.entrySet()) {
            String role = entry.getValue().getRole();
            String name = entry.getValue().getUsername();
            if (name == null) name = "Unknown";
            
            if ("KETUA".equalsIgnoreCase(role)) ketua = name;
            else if ("WAKIL".equalsIgnoreCase(role)) wakil = name;
            else if ("SEKRETARIS".equalsIgnoreCase(role)) sekretaris = name;
            else if ("BENDAHARA".equalsIgnoreCase(role)) bendahara = name;
        }

        // Get student list
        List<Map<String, String>> students = plugin.getDatabaseManager().getStudentsInClass(classNum);
        String studentListStr = students.stream().map(m -> m.get("username")).collect(Collectors.joining(", "));
        if (studentListStr.isEmpty()) {
            studentListStr = "-";
        }

        sender.sendMessage(MiniMessage.miniMessage().deserialize(
            "<gold>=== Struktur Informasi Kelas " + className.trim() + " ===</gold>\n" +
            "<yellow>» Wali Kelas:</yellow> <white>" + waliKelasName + "</white>\n" +
            "<yellow>» Ketua Kelas:</yellow> <white>" + ketua + "</white>\n" +
            "<yellow>» Wakil Ketua:</yellow> <white>" + wakil + "</white>\n" +
            "<yellow>» Sekretaris:</yellow> <white>" + sekretaris + "</white>\n" +
            "<yellow>» Bendahara:</yellow> <white>" + bendahara + "</white>\n" +
            "<yellow>» Daftar Murid:</yellow> <gray>" + studentListStr + "</gray>"
        ));
    }

    private void handleStart(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>Format: /class start <id_kelas> [id_matapelajaran]</red>"));
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

        boolean success = plugin.getClassManager().startSession(idKelas, mapel, teacherUuid);
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
            Player p = (Player) sender;
            String detectedRegion = getPlayerRegion(p);
            if (detectedRegion != null && plugin.getClassManager().getSession(detectedRegion) != null) {
                idKelas = detectedRegion;
            } else {
                for (ClassSession cs : plugin.getClassManager().getActiveSessions().values()) {
                    if (p.getUniqueId().equals(cs.getTeacherUuid())) {
                        idKelas = cs.getIdKelas();
                        break;
                    }
                }
            }
        }

        if (idKelas == null) {
            sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>Tentukan kelas yang ingin diselesaikan! /class selesai <id_kelas></red>"));
            return;
        }

        boolean success = plugin.getClassManager().stopSession(idKelas);
        if (success) {
            sender.sendMessage(MiniMessage.miniMessage().deserialize("<green>Sesi kelas " + idKelas.toUpperCase() + " berhasil ditutup.</green>"));
        } else {
            sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>Gagal! Tidak ada sesi aktif untuk kelas " + idKelas.toUpperCase() + ".</red>"));
        }
    }

    private void handleSelesaikan(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>Format: /class selesaikan <player></red>"));
            return;
        }

        String targetName = args[1];
        Player targetPlayer = Bukkit.getPlayerExact(targetName);
        if (targetPlayer == null || !targetPlayer.isOnline()) {
            sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>Player " + targetName + " tidak online!</red>"));
            return;
        }

        String classId = null;
        StudentProfile targetProfile = plugin.getProfileManager().getProfile(targetPlayer.getUniqueId());
        if (targetProfile != null) {
            classId = "kelas" + targetProfile.getAcademicClass();
        }

        if (classId == null || plugin.getClassManager().getSession(classId) == null) {
            sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>Gagal! Murid " + targetName + " tidak berada dalam kelas dengan sesi aktif.</red>"));
            return;
        }

        boolean success = plugin.getClassManager().dismissEarly(classId, targetPlayer.getUniqueId());
        if (success) {
            sender.sendMessage(MiniMessage.miniMessage().deserialize("<green>Berhasil meloloskan " + targetPlayer.getName() + " pulang dini dari kelas " + classId.toUpperCase() + "!</green>"));
        } else {
            sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>Gagal memproses pulang dini.</red>"));
        }
    }

    private void handlePembelajaran(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>Format: /class pembelajaran <id_kelas> <namaFile></red>"));
            return;
        }

        String idKelas = args[1];
        String fileName = args[2];

        boolean success = plugin.getClassManager().loadLessonMaterial(idKelas, fileName);
        if (success) {
            sender.sendMessage(MiniMessage.miniMessage().deserialize("<green>Materi '" + fileName + "' berhasil dimuat di papan tulis " + idKelas.toUpperCase() + ".</green>"));
        } else {
            sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>Gagal! Berkas materi tidak ditemukan di database atau kelas tidak aktif.</red>"));
        }
    }

    private void handleStartSoal(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>Format: /class startsoal <namaFile></red>"));
            return;
        }

        String fileName = args[1];
        String idKelas = null;

        if (sender instanceof Player) {
            Player p = (Player) sender;
            String detectedRegion = getPlayerRegion(p);
            if (detectedRegion != null && plugin.getClassManager().getSession(detectedRegion) != null) {
                idKelas = detectedRegion;
            } else {
                for (ClassSession cs : plugin.getClassManager().getActiveSessions().values()) {
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

        boolean success = plugin.getClassManager().startQuiz(idKelas, fileName);
        if (success) {
            sender.sendMessage(MiniMessage.miniMessage().deserialize("<green>Kuis '" + fileName + "' berhasil dibagikan ke siswa kelas " + idKelas.toUpperCase() + ".</green>"));
        } else {
            sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>Gagal! Berkas kuis tidak ditemukan atau kuis tidak aktif di kelas ini.</red>"));
        }
    }

    private void handleRekap(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>Format: /class rekap <id_kelas></red>"));
            return;
        }

        String idKelas = args[1];
        ClassSession cs = plugin.getClassManager().getSession(idKelas);
        if (cs == null) {
            sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>Tidak ada sesi kelas aktif untuk kelas " + idKelas.toUpperCase() + ".</red>"));
            return;
        }

        plugin.getClassManager().rekapSessionInternal(cs);
        sender.sendMessage(MiniMessage.miniMessage().deserialize("<green>Kompilasi rekap nilai/presensi untuk " + idKelas.toUpperCase() + " berhasil dipush ke database!</green>"));
    }

    private void handleOfficer(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(MiniMessage.miniMessage().deserialize(
                "<gold>=== Manajemen Pengurus Kelas ===</gold>\n" +
                "<yellow>/class officer list [id_kelas]</yellow> - <gray>Lihat pengurus kelas</gray>\n" +
                "<yellow>/class officer add <player> <KETUA|WAKIL|SEKRETARIS|BENDAHARA></yellow> - <gray>Angkat pengurus baru</gray>\n" +
                "<yellow>/class officer remove <player></yellow> - <gray>Berhentikan pengurus</gray>\n" +
                "<yellow>/class officer clear [id_kelas]</yellow> - <gray>Hapus semua pengurus kelas</gray>"
            ));
            return;
        }

        String action = args[1].toLowerCase();
        if (action.equals("list")) {
            int classNum = 0;
            if (args.length >= 3) {
                try {
                    classNum = Integer.parseInt(args[2].replaceAll("\\D+", ""));
                } catch (NumberFormatException ignored) {}
            } else if (sender instanceof Player) {
                StudentProfile profile = plugin.getProfileManager().getProfile(((Player) sender).getUniqueId());
                if (profile != null) {
                    classNum = profile.getAcademicClass();
                }
            }
            if (classNum < 1 || classNum > 12) {
                sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>Tentukan kelas yang valid (1-12)!</red>"));
                return;
            }
            displayClassroomInfo(sender, classNum);
        } else if (action.equals("clear")) {
            int classNum = 0;
            if (args.length >= 3) {
                try {
                    classNum = Integer.parseInt(args[2].replaceAll("\\D+", ""));
                } catch (NumberFormatException ignored) {}
            } else if (sender instanceof Player) {
                StudentProfile profile = plugin.getProfileManager().getProfile(((Player) sender).getUniqueId());
                if (profile != null) {
                    classNum = profile.getAcademicClass();
                }
            }
            if (classNum < 1 || classNum > 12) {
                sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>Tentukan kelas yang valid (1-12)!</red>"));
                return;
            }
            plugin.getClassroomManager().clearOfficers(classNum);
            sender.sendMessage(MiniMessage.miniMessage().deserialize("<green>Semua pengurus untuk kelas " + classNum + " berhasil dihapus!</green>"));
        } else if (action.equals("add")) {
            if (args.length < 4) {
                sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>Format: /class officer add <player> <role></red>"));
                return;
            }
            String targetName = args[2];
            String roleStr = args[3].toUpperCase();
            List<String> validRoles = Arrays.asList("KETUA", "WAKIL", "SEKRETARIS", "BENDAHARA");
            if (!validRoles.contains(roleStr)) {
                sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>Role tidak valid! Pilih KETUA, WAKIL, SEKRETARIS, atau BENDAHARA.</red>"));
                return;
            }

            OfflinePlayer op = Bukkit.getOfflinePlayer(targetName);
            UUID uuid = op.getUniqueId();
            
            // Resolve player's class
            int classNum = 0;
            if (op.isOnline()) {
                StudentProfile profile = plugin.getProfileManager().getProfile(uuid);
                if (profile != null) {
                    classNum = profile.getAcademicClass();
                }
            } else {
                try {
                    StudentProfile profile = plugin.getDatabaseManager().loadProfile(uuid);
                    if (profile != null) {
                        classNum = profile.getAcademicClass();
                    }
                } catch (Exception ignored) {}
            }

            if (classNum == 0) {
                sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>Gagal! Murid " + targetName + " tidak terdaftar di kelas mana pun.</red>"));
                return;
            }

            String finalTargetName = op.getName() != null ? op.getName() : targetName;
            plugin.getClassroomManager().assignOfficer(classNum, uuid, finalTargetName, roleStr);
            sender.sendMessage(MiniMessage.miniMessage().deserialize("<green>Berhasil mengangkat " + (op.getName() != null ? op.getName() : targetName) + " sebagai " + roleStr + " di kelas " + classNum + ".</green>"));
        } else if (action.equals("remove")) {
            if (args.length < 3) {
                sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>Format: /class officer remove <player></red>"));
                return;
            }
            String targetName = args[2];
            OfflinePlayer op = Bukkit.getOfflinePlayer(targetName);
            plugin.getClassroomManager().removeOfficer(op.getUniqueId());
            sender.sendMessage(MiniMessage.miniMessage().deserialize("<green>Berhasil memberhentikan pengurus " + (op.getName() != null ? op.getName() : targetName) + ".</green>"));
        } else {
            sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>Tindakan officer tidak dikenal.</red>"));
        }
    }

    private void handleFund(CommandSender sender, String[] args) {
        String action = "gui";
        if (args.length >= 2 && (args[0].equalsIgnoreCase("fund") || args[0].equalsIgnoreCase("bank"))) {
            action = args[1].toLowerCase();
        } else if (args[0].equalsIgnoreCase("gui")) {
            action = "gui";
        }

        int classNum = 0;
        Player player = null;
        if (sender instanceof Player) {
            player = (Player) sender;
            StudentProfile profile = plugin.getProfileManager().getProfile(player.getUniqueId());
            if (profile != null) {
                classNum = profile.getAcademicClass();
            }
            if (classNum == 0) {
                for (ClassroomManager.ClassroomData data : plugin.getClassroomManager().getAllClassroomData()) {
                    if (player.getUniqueId().equals(data.getWaliKelasUuid())) {
                        classNum = data.getIdKelas();
                        break;
                    }
                }
            }
        }

        if (action.equals("gui")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>Hanya player yang dapat membuka GUI!</red>"));
                return;
            }
            int targetClass = classNum;
            int optClassIdx = (args[0].equalsIgnoreCase("gui")) ? 1 : 2;
            if (args.length > optClassIdx) {
                try {
                    targetClass = Integer.parseInt(args[optClassIdx].replaceAll("\\D+", ""));
                } catch (NumberFormatException ignored) {}
            }
            if (targetClass < 1 || targetClass > 12) {
                sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>Target kelas tidak valid! Gunakan angka 1-12.</red>"));
                return;
            }
            if (targetClass != classNum && !checkStaffPermission(player)) {
                sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>Anda tidak memiliki izin untuk melihat kas kelas lain!</red>"));
                return;
            }
            plugin.getUiManager().getClassCashGui().openClassGui(player, targetClass);
        } else if (action.equals("bayar")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>Hanya player yang dapat membayar kas!</red>"));
                return;
            }
            id.naturalsmp.naturalSchool.classes.ClassCashManager.CashOperationResult res = plugin.getClassCashManager().payWeeklyFee(player);
            if (res.isSuccess()) {
                player.sendMessage(MiniMessage.miniMessage().deserialize("<green>" + res.getMessage() + "</green>"));
            } else {
                player.sendMessage(MiniMessage.miniMessage().deserialize("<red>" + res.getMessage() + "</red>"));
            }
        } else if (action.equals("status")) {
            int targetClass = classNum;
            if (args.length > 2) {
                try {
                    targetClass = Integer.parseInt(args[2].replaceAll("\\D+", ""));
                } catch (NumberFormatException ignored) {}
            }
            if (targetClass < 1 || targetClass > 12) {
                sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>Target kelas tidak valid! Gunakan angka 1-12.</red>"));
                return;
            }
            if (player != null && targetClass != classNum && !checkStaffPermission(player)) {
                sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>Anda tidak memiliki izin untuk melihat status kas kelas lain!</red>"));
                return;
            }

            String week = plugin.getClassCashManager().getCurrentWeekIdentifier();
            if (args.length > 3) {
                week = args[3];
            }

            sender.sendMessage(MiniMessage.miniMessage().deserialize("<gold>=== Status Pembayaran Kas Kelas " + targetClass + " (Minggu: " + week + ") ===</gold>"));
            List<Map<String, String>> students = plugin.getDatabaseManager().getStudentsInClass(targetClass);
            List<Map<String, Object>> payments = plugin.getDatabaseManager().getClassCashPayments(targetClass, week);
            Set<String> paidUuids = payments.stream()
                .map(m -> (String) m.get("player_uuid"))
                .collect(Collectors.toSet());

            List<String> paidNames = new ArrayList<>();
            List<String> unpaidNames = new ArrayList<>();

            for (Map<String, String> student : students) {
                String uuid = student.get("uuid");
                String name = student.get("username");
                if (paidUuids.contains(uuid)) {
                    paidNames.add(name);
                } else {
                    unpaidNames.add(name);
                }
            }

            sender.sendMessage(MiniMessage.miniMessage().deserialize("<green>» Sudah Bayar (" + paidNames.size() + "):</green> <white>" + (paidNames.isEmpty() ? "-" : String.join(", ", paidNames)) + "</white>"));
            sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>» Belum Bayar (" + unpaidNames.size() + "):</red> <white>" + (unpaidNames.isEmpty() ? "-" : String.join(", ", unpaidNames)) + "</white>"));

        } else if (action.equals("withdraw")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>Hanya player yang dapat menarik saldo kas!</red>"));
                return;
            }
            if (classNum < 1 || classNum > 12) {
                sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>Anda tidak terdaftar di kelas mana pun.</red>"));
                return;
            }
            ClassroomManager.ClassroomData classData = plugin.getClassroomManager().getClassroom(classNum);
            if (!hasOfficerAccessFund(player, classData)) {
                sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>Hanya Wali Kelas, Ketua, Wakil, atau Bendahara yang dapat menarik saldo kas kelas!</red>"));
                return;
            }
            if (args.length < 3) {
                sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>Format: /class fund withdraw <jumlah> <alasan></red>"));
                return;
            }
            double amount;
            try {
                amount = Double.parseDouble(args[2].replaceAll("[^0-9.]", ""));
            } catch (NumberFormatException e) {
                sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>Jumlah penarikan harus berupa angka!</red>"));
                return;
            }
            if (amount <= 0) {
                sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>Jumlah penarikan harus lebih besar dari 0!</red>"));
                return;
            }
            String reason = "Penarikan manual via perintah";
            if (args.length > 3) {
                reason = String.join(" ", Arrays.copyOfRange(args, 3, args.length));
            }
            id.naturalsmp.naturalSchool.classes.ClassCashManager.CashOperationResult res = plugin.getClassCashManager().withdrawCash(player, classNum, amount, reason);
            if (res.isSuccess()) {
                player.sendMessage(MiniMessage.miniMessage().deserialize("<green>" + res.getMessage() + "</green>"));
            } else {
                player.sendMessage(MiniMessage.miniMessage().deserialize("<red>" + res.getMessage() + "</red>"));
            }
        } else if (action.equals("toggle")) {
            int targetClass = classNum;
            if (args.length > 2) {
                try {
                    targetClass = Integer.parseInt(args[2].replaceAll("\\D+", ""));
                } catch (NumberFormatException ignored) {}
            }
            if (targetClass < 1 || targetClass > 12) {
                sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>Target kelas tidak valid! Gunakan angka 1-12.</red>"));
                return;
            }
            ClassroomManager.ClassroomData classData = plugin.getClassroomManager().getClassroom(targetClass);
            if (player != null && !hasOfficerAccessFund(player, classData)) {
                sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>Hanya Wali Kelas, Ketua, atau Wakil yang dapat mengubah status kas kelas!</red>"));
                return;
            }
            boolean current = classData.isWeeklyFeeEnabled();
            plugin.getClassroomManager().updateClassCash(targetClass, classData.getCashBalance(), classData.getWeeklyFee(), !current);
            sender.sendMessage(MiniMessage.miniMessage().deserialize("<green>Berhasil mengubah status kas kelas " + targetClass + " menjadi: " + (!current ? "<bold>AKTIF</bold>" : "<bold>NONAKTIF</bold>") + ".</green>"));
        } else if (action.equals("setweekly")) {
            if (classNum < 1 || classNum > 12) {
                sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>Anda tidak terdaftar di kelas mana pun.</red>"));
                return;
            }
            ClassroomManager.ClassroomData classData = plugin.getClassroomManager().getClassroom(classNum);
            if (player != null && !hasOfficerAccessFund(player, classData)) {
                sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>Hanya Wali Kelas, Ketua, Wakil, atau Bendahara yang dapat mengatur biaya kas kelas!</red>"));
                return;
            }
            if (args.length < 3) {
                sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>Format: /class fund setweekly <jumlah></red>"));
                return;
            }
            double amount;
            try {
                amount = Double.parseDouble(args[2].replaceAll("[^0-9.]", ""));
            } catch (NumberFormatException e) {
                sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>Biaya kas harus berupa angka!</red>"));
                return;
            }
            if (amount < 0) {
                sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>Biaya kas tidak boleh negatif!</red>"));
                return;
            }
            plugin.getClassroomManager().updateClassCash(classNum, classData.getCashBalance(), amount, classData.isWeeklyFeeEnabled());
            sender.sendMessage(MiniMessage.miniMessage().deserialize("<green>Berhasil mengubah biaya kas mingguan kelas menjadi Rp" + String.format("%,.0f", amount) + ".</green>"));
        } else if (action.equals("denda")) {
            if (classNum < 1 || classNum > 12) {
                sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>Anda tidak terdaftar di kelas mana pun.</red>"));
                return;
            }
            ClassroomManager.ClassroomData classData = plugin.getClassroomManager().getClassroom(classNum);
            if (player != null && !hasOfficerAccessFund(player, classData)) {
                sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>Hanya Wali Kelas, Ketua, Wakil, atau Bendahara yang dapat memberikan denda kelas!</red>"));
                return;
            }
            if (args.length < 4) {
                sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>Format: /class fund denda <player> <jumlah> <alasan></red>"));
                return;
            }
            String targetName = args[2];
            double amount;
            try {
                amount = Double.parseDouble(args[3].replaceAll("[^0-9.]", ""));
            } catch (NumberFormatException e) {
                sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>Jumlah denda harus berupa angka!</red>"));
                return;
            }
            if (amount <= 0) {
                sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>Jumlah denda harus lebih besar dari 0!</red>"));
                return;
            }
            String reason = "Denda manual via perintah";
            if (args.length > 4) {
                reason = String.join(" ", Arrays.copyOfRange(args, 4, args.length));
            }
            OfflinePlayer targetPlayer = Bukkit.getOfflinePlayer(targetName);
            StudentProfile targetProfile = null;
            if (targetPlayer.isOnline()) {
                targetProfile = plugin.getProfileManager().getProfile(targetPlayer.getUniqueId());
            } else {
                try {
                    targetProfile = plugin.getDatabaseManager().loadProfile(targetPlayer.getUniqueId());
                } catch (Exception ignored) {}
            }
            if (targetProfile == null || targetProfile.getAcademicClass() != classNum) {
                sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>Pemain " + targetName + " tidak terdaftar di kelas Anda (" + classNum + ").</red>"));
                return;
            }
            id.naturalsmp.naturalSchool.classes.ClassCashManager.CashOperationResult res = plugin.getClassCashManager().applyFine(targetPlayer, player, classNum, amount, reason);
            if (res.isSuccess()) {
                sender.sendMessage(MiniMessage.miniMessage().deserialize("<green>" + res.getMessage() + "</green>"));
            } else {
                sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>" + res.getMessage() + "</red>"));
            }
        } else if (action.equals("history")) {
            int targetClass = classNum;
            int limit = 10;
            if (args.length > 2) {
                try {
                    targetClass = Integer.parseInt(args[2].replaceAll("\\D+", ""));
                } catch (NumberFormatException ignored) {}
            }
            if (args.length > 3) {
                try {
                    limit = Integer.parseInt(args[3]);
                } catch (NumberFormatException ignored) {}
            }
            if (targetClass < 1 || targetClass > 12) {
                sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>Target kelas tidak valid! Gunakan angka 1-12.</red>"));
                return;
            }
            if (player != null && targetClass != classNum && !checkStaffPermission(player)) {
                sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>Anda tidak memiliki izin untuk melihat riwayat kas kelas lain!</red>"));
                return;
            }
            sender.sendMessage(MiniMessage.miniMessage().deserialize("<gold>=== Riwayat Transaksi Kas Kelas " + targetClass + " (Limit: " + limit + ") ===</gold>"));
            List<Map<String, Object>> txs = plugin.getDatabaseManager().getClassCashTransactions(targetClass, limit);
            if (txs.isEmpty()) {
                sender.sendMessage(MiniMessage.miniMessage().deserialize("<gray>Belum ada riwayat transaksi.</gray>"));
            } else {
                SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm");
                for (Map<String, Object> tx : txs) {
                    String type = (String) tx.get("tx_type");
                    double amt = (double) tx.get("amount");
                    String desc = (String) tx.get("description");
                    Date date = (Date) tx.get("tx_date");
                    String dateStr = date != null ? sdf.format(date) : "-";
                    String color = type.equalsIgnoreCase("DEPOSIT") || type.equalsIgnoreCase("FINE") ? "<green>+" : "<red>-";
                    sender.sendMessage(MiniMessage.miniMessage().deserialize(
                        "<gray>[" + dateStr + "]</gray> " + color + "Rp" + String.format("%,.0f", amt) + "</green> <gray>- " + desc + "</gray>"
                    ));
                }
            }
        } else {
            sendHelpMessage(sender);
        }
    }

    private boolean hasOfficerAccessFund(Player player, ClassroomManager.ClassroomData data) {
        if (player.getUniqueId().equals(data.getWaliKelasUuid())) {
            return true;
        }
        if (player.hasPermission("naturalschool.admin")) {
            return true;
        }
        ClassroomManager.OfficerInfo officer = data.getOfficers().get(player.getUniqueId());
        if (officer != null) {
            String role = officer.getRole().toUpperCase();
            return role.equals("KETUA") || role.equals("WAKIL") || role.equals("BENDAHARA");
        }
        return false;
    }

    private void handleJawab(Player player, String[] args) {
        if (args.length < 3) {
            player.sendMessage(MiniMessage.miniMessage().deserialize("<red>Format: /class jawab <index> <jawaban></red>"));
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
        String idKelas = null;
        StudentProfile profile = plugin.getProfileManager().getProfile(player.getUniqueId());
        if (profile != null) {
            idKelas = "kelas" + profile.getAcademicClass();
        }

        if (idKelas == null || plugin.getClassManager().getSession(idKelas) == null) {
            player.sendMessage(MiniMessage.miniMessage().deserialize("<red>Gagal! Anda sedang tidak berada dalam kelas dengan sesi aktif.</red>"));
            return;
        }

        boolean success = plugin.getClassManager().submitAnswer(idKelas, player.getUniqueId(), index, jawaban);
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
        } catch (Throwable ignored) {}
        return null;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 1) {
            List<String> list = new ArrayList<>();
            list.add("chat");
            list.add("info");
            list.add("help");
            list.add("jawab");
            list.add("fund");
            list.add("bank");
            list.add("gui");
            if (checkStaffPermission(sender)) {
                list.add("start");
                list.add("selesai");
                list.add("stop");
                list.add("selesaikan");
                list.add("pembelajaran");
                list.add("startsoal");
                list.add("rekap");
                list.add("officer");
            }
            return list.stream().filter(s -> s.startsWith(args[0].toLowerCase())).collect(Collectors.toList());
        }

        if (args.length == 2) {
            String sub = args[0].toLowerCase();
            if (sub.equals("chat")) {
                return Collections.singletonList("norank").stream().filter(s -> s.startsWith(args[1].toLowerCase())).collect(Collectors.toList());
            } else if (sub.equals("fund") || sub.equals("bank")) {
                return Arrays.asList("gui", "bayar", "status", "withdraw", "toggle", "setweekly", "denda", "history").stream()
                        .filter(s -> s.startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());
            } else if (checkStaffPermission(sender)) {
                if (Arrays.asList("start", "pembelajaran", "rekap", "selesai", "stop").contains(sub)) {
                    List<String> classes = new ArrayList<>();
                    for (int i = 1; i <= 12; i++) {
                        classes.add("kelas" + i);
                    }
                    return classes.stream().filter(s -> s.startsWith(args[1].toLowerCase())).collect(Collectors.toList());
                } else if (sub.equals("selesaikan")) {
                    return Bukkit.getOnlinePlayers().stream().map(Player::getName)
                            .filter(s -> s.toLowerCase().startsWith(args[1].toLowerCase()))
                            .collect(Collectors.toList());
                } else if (sub.equals("startsoal")) {
                    return getLessonFilesNames("SOAL_KUIS").stream()
                            .filter(s -> s.toLowerCase().startsWith(args[1].toLowerCase()))
                            .collect(Collectors.toList());
                } else if (sub.equals("officer")) {
                    return Arrays.asList("list", "add", "remove", "clear").stream()
                            .filter(s -> s.startsWith(args[1].toLowerCase()))
                            .collect(Collectors.toList());
                }
            }
        }

        if (args.length == 3) {
            String sub = args[0].toLowerCase();
            if (sub.equals("fund") || sub.equals("bank")) {
                String action = args[1].toLowerCase();
                if (Arrays.asList("status", "toggle", "gui", "history").contains(action)) {
                    List<String> classes = new ArrayList<>();
                    for (int i = 1; i <= 12; i++) {
                        classes.add(String.valueOf(i));
                    }
                    return classes.stream().filter(s -> s.startsWith(args[2].toLowerCase())).collect(Collectors.toList());
                } else if (action.equals("denda")) {
                    return Bukkit.getOnlinePlayers().stream().map(Player::getName)
                            .filter(s -> s.toLowerCase().startsWith(args[2].toLowerCase()))
                            .collect(Collectors.toList());
                }
            }
            if (checkStaffPermission(sender)) {
                if (sub.equals("pembelajaran")) {
                    return getLessonFilesNames("MATERI_PROYEKTOR").stream()
                            .filter(s -> s.toLowerCase().startsWith(args[2].toLowerCase()))
                            .collect(Collectors.toList());
                } else if (sub.equals("start")) {
                    return Arrays.asList("Matematika", "Bahasa_Indonesia", "Bahasa_Inggris", "Fisika", "Kimia", "Biologi", "IPS", "PKN")
                            .stream().filter(s -> s.toLowerCase().startsWith(args[2].toLowerCase()))
                            .collect(Collectors.toList());
                } else if (sub.equals("officer")) {
                    String op = args[1].toLowerCase();
                    if (op.equals("add") || op.equals("remove")) {
                        return Bukkit.getOnlinePlayers().stream().map(Player::getName)
                                .filter(s -> s.toLowerCase().startsWith(args[2].toLowerCase()))
                                .collect(Collectors.toList());
                    } else if (op.equals("list") || op.equals("clear")) {
                        List<String> classes = new ArrayList<>();
                        for (int i = 1; i <= 12; i++) {
                            classes.add("kelas" + i);
                        }
                        return classes.stream().filter(s -> s.startsWith(args[2].toLowerCase())).collect(Collectors.toList());
                    }
                }
            }
        }

        if (args.length == 4) {
            String sub = args[0].toLowerCase();
            if (sub.equals("officer") && args[1].equalsIgnoreCase("add") && checkStaffPermission(sender)) {
                return Arrays.asList("KETUA", "WAKIL", "SEKRETARIS", "BENDAHARA").stream()
                        .filter(s -> s.startsWith(args[3].toUpperCase()))
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
        } catch (SQLException ignored) {}
        return list;
    }
}
