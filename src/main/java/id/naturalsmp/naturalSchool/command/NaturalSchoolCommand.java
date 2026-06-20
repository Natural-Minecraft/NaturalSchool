package id.naturalsmp.naturalSchool.command;

import id.naturalsmp.naturalSchool.NaturalSchool;
import id.naturalsmp.naturalSchool.exam.ExamManager;
import id.naturalsmp.naturalSchool.profile.SchoolRank;
import id.naturalsmp.naturalSchool.profile.StudentProfile;
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

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class NaturalSchoolCommand implements CommandExecutor, TabCompleter {

    private final NaturalSchool plugin;
    private final Map<String, Long> unregisterConfirmations = new ConcurrentHashMap<>();

    public NaturalSchoolCommand(NaturalSchool plugin) {
        this.plugin = plugin;
    }

    private static final String NO_PERMISSION = "<red><bold>NaturalSchool</bold> <gray>»</gray> <red>You don't have permission to perform this command!</red>";

    private boolean hasSubCommandPermission(CommandSender sender, String sub) {
        String node = "naturalschool." + sub;
        if (sender.hasPermission("naturalschool.admin") || sender.hasPermission("naturalschool.*") || sender.hasPermission(node)) {
            return true;
        }
        if (sender instanceof Player) {
            Player player = (Player) sender;
            StudentProfile profile = plugin.getProfileManager().getProfile(player.getUniqueId());
            if (profile != null) {
                SchoolRank rank = profile.getRank();
                return rank == SchoolRank.KETUA_YAYASAN || rank == SchoolRank.WAKIL_KETUA_YAYASAN;
            }
        }
        return false;
    }

    private boolean hasAnyAdminPermission(CommandSender sender) {
        if (sender.hasPermission("naturalschool.admin") || sender.hasPermission("naturalschool.*")) {
            return true;
        }
        for (String perm : Arrays.asList("general", "rank", "setclass", "setstage", "nis", "gui", "semester", "exam", "class")) {
            if (sender.hasPermission("naturalschool." + perm)) {
                return true;
            }
        }
        if (sender instanceof Player) {
            Player player = (Player) sender;
            StudentProfile profile = plugin.getProfileManager().getProfile(player.getUniqueId());
            if (profile != null) {
                SchoolRank rank = profile.getRank();
                return rank == SchoolRank.KETUA_YAYASAN || rank == SchoolRank.WAKIL_KETUA_YAYASAN;
            }
        }
        return false;
    }

    private String getPermissionGroup(String sub) {
        switch (sub.toLowerCase()) {
            case "reload":
            case "info":
                return "general";
            case "rank":
                return "rank";
            case "setclass":
                return "setclass";
            case "setstage":
                return "setstage";
            case "nis":
                return "nis";
            case "gui":
                return "gui";
            case "semester":
                return "semester";
            case "exam":
                return "exam";
            case "class":
                return "class";
            default:
                return null;
        }
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!hasAnyAdminPermission(sender)) {
            sender.sendMessage(MiniMessage.miniMessage().deserialize(NO_PERMISSION));
            return true;
        }

        if (args.length == 0) {
            sendHelpMessage(sender);
            return true;
        }

        String subCommand = args[0].toLowerCase();
        String group = getPermissionGroup(subCommand);
        if (group != null && !hasSubCommandPermission(sender, group)) {
            sender.sendMessage(MiniMessage.miniMessage().deserialize(NO_PERMISSION));
            return true;
        }

        switch (subCommand) {
            case "reload":
                handleReload(sender);
                break;
            case "info":
                handleInfo(sender, args);
                break;
            case "rank":
                handleRankCommand(sender, args);
                break;
            case "setclass":
                handleSetClass(sender, args);
                break;
            case "setstage":
                handleSetStage(sender, args);
                break;
            case "nis":
                handleNisCommand(sender, args);
                break;
            case "gui":
                handleGuiCommand(sender, args);
                break;
            case "semester":
                handleSemesterCommand(sender, args);
                break;
            case "exam":
                handleExamCommand(sender, args);
                break;
            case "class":
                handleClassCommand(sender, args);
                break;
            default:
                sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>Unknown subcommand. Use /naturalschool for help.</red>"));
                break;
        }

        return true;
    }

    private void sendHelpMessage(CommandSender sender) {
        StringBuilder sb = new StringBuilder("<gold>=== NaturalSchool Administrative Commands ===</gold>");
        if (hasSubCommandPermission(sender, "general")) {
            sb.append("\n<yellow>/naturalschool reload</yellow> - <gray>Reload configuration and database connections.</gray>");
            sb.append("\n<yellow>/naturalschool info <player></yellow> - <gray>View player academic profile details.</gray>");
        }
        if (hasSubCommandPermission(sender, "rank")) {
            sb.append("\n<yellow>/naturalschool rank ...</yellow> - <gray>Manage rank prefixes and assign rank to players. Type /ns rank for help.</gray>");
        }
        if (hasSubCommandPermission(sender, "setclass")) {
            sb.append("\n<yellow>/naturalschool setclass <player> <1-12></yellow> - <gray>Set student academic class.</gray>");
        }
        if (hasSubCommandPermission(sender, "setstage")) {
            sb.append("\n<yellow>/naturalschool setstage <player> <SD|SMP|SMA></yellow> - <gray>Set student academic stage.</gray>");
        }
        if (hasSubCommandPermission(sender, "nis")) {
            sb.append("\n<yellow>/naturalschool nis help</yellow> - <gray>View NIS Management System help.</gray>");
        }
        if (hasSubCommandPermission(sender, "gui")) {
            sb.append("\n<yellow>/naturalschool gui <welcome|exam1|exam2|exam3|exam4|exam5|version> [player]</yellow> - <gray>Manually trigger school GUI dialogs or view GUI version.</gray>");
        }
        if (hasSubCommandPermission(sender, "semester")) {
            sb.append("\n<yellow>/naturalschool semester <info|end></yellow> - <gray>Manage and rotate active school semesters.</gray>");
        }
        if (hasSubCommandPermission(sender, "exam")) {
            sb.append("\n<yellow>/naturalschool exam <open|close|message|sync> [msg]</yellow> - <gray>Manage exam portal status, messages, and synchronization.</gray>");
        }
        if (hasSubCommandPermission(sender, "class")) {
            sb.append("\n<yellow>/naturalschool class ...</yellow> - <gray>Manage classrooms, homerooms, areas, and doors. Type /ns class for sub-help.</gray>");
        }
        sender.sendMessage(MiniMessage.miniMessage().deserialize(sb.toString()));
    }

    private void handleClassCommand(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(MiniMessage.miniMessage().deserialize(
                "<gold>=== NaturalSchool Admin Class Commands ===</gold>\n" +
                "<yellow>/ns class list</yellow> - <gray>List all classrooms 1-12 and details.</gray>\n" +
                "<yellow>/ns class spy [player]</yellow> - <gray>Toggle class chat spy for player.</gray>\n" +
                "<yellow>/ns class setwali <1-12> <player></yellow> - <gray>Assign Wali Kelas.</gray>\n" +
                "<yellow>/ns class area <1-12> set</yellow> - <gray>Set classroom bounds from WorldEdit selection.</gray>\n" +
                "<yellow>/ns class area <1-12> remove</yellow> - <gray>Remove classroom bounds.</gray>\n" +
                "<yellow>/ns class door <1-12> <door_num> set</yellow> - <gray>Set door coordinates from WorldEdit selection.</gray>\n" +
                "<yellow>/ns class door <1-12> <door_num> remove</yellow> - <gray>Remove class door.</gray>\n" +
                "<yellow>/ns class panel [player]</yellow> - <gray>Open classroom manager GUI.</gray>"
            ));
            return;
        }

        String sub = args[1].toLowerCase();
        if (sub.equals("list")) {
            sender.sendMessage(MiniMessage.miniMessage().deserialize("<gold>=== List Kelas NaturalSchool ===</gold>"));
            for (int i = 1; i <= 12; i++) {
                id.naturalsmp.naturalSchool.classes.ClassroomManager.ClassroomData data = plugin.getClassroomManager().getClassroom(i);
                String prefix = plugin.getRankPrefixConfig().getClassPrefix(i);
                String bounds = data.hasBounds() ? data.getWorldName() + " (" + data.getX1() + "," + data.getY1() + "," + data.getZ1() + " to " + data.getX2() + "," + data.getY2() + "," + data.getZ2() + ")" : "Not Set";
                String wali = "None";
                if (data.getWaliKelasUuid() != null) {
                    wali = Bukkit.getOfflinePlayer(data.getWaliKelasUuid()).getName();
                    if (wali == null) wali = data.getWaliKelasUuid().toString();
                }
                sender.sendMessage(MiniMessage.miniMessage().deserialize(
                    "<yellow>Kelas " + i + " (" + prefix.trim() + "):</yellow>\n" +
                    "  » Wali Kelas: <white>" + wali + "</white>\n" +
                    "  » Bounds Area: <white>" + bounds + "</white>\n" +
                    "  » Officers: <white>" + data.getOfficers().size() + "</white>\n" +
                    "  » Doors: <white>" + plugin.getClassroomManager().getDoors(i).size() + "</white>"
                ));
            }
        } else if (sub.equals("spy")) {
            Player target = (sender instanceof Player) ? (Player) sender : null;
            if (args.length >= 3) {
                target = Bukkit.getPlayer(args[2]);
            }
            if (target == null) {
                sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>Player tidak ditemukan!</red>"));
                return;
            }
            boolean active = plugin.getClassChatManager().toggleChatSpy(target.getUniqueId());
            if (active) {
                sender.sendMessage(MiniMessage.miniMessage().deserialize("<green>Chat Spy diaktifkan untuk " + target.getName() + ".</green>"));
            } else {
                sender.sendMessage(MiniMessage.miniMessage().deserialize("<yellow>Chat Spy dinonaktifkan untuk " + target.getName() + ".</yellow>"));
            }
        } else if (sub.equals("setwali")) {
            if (args.length < 4) {
                sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>Format: /ns class setwali <1-12> <player></red>"));
                return;
            }
            int classNum;
            try {
                classNum = Integer.parseInt(args[2].replaceAll("\\D+", ""));
            } catch (Exception e) {
                sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>Kelas harus berupa angka 1-12!</red>"));
                return;
            }
            if (classNum < 1 || classNum > 12) {
                sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>Kelas harus bernilai 1-12!</red>"));
                return;
            }
            String targetName = args[3];
            OfflinePlayer op = Bukkit.getOfflinePlayer(targetName);
            plugin.getClassroomManager().updateWaliKelas(classNum, op.getUniqueId());
            sender.sendMessage(MiniMessage.miniMessage().deserialize("<green>Berhasil mengubah Wali Kelas " + classNum + " menjadi " + op.getName() + ".</green>"));
        } else if (sub.equals("panel")) {
            Player target = (sender instanceof Player) ? (Player) sender : null;
            if (args.length >= 3) {
                target = Bukkit.getPlayer(args[2]);
            }
            if (target == null) {
                sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>Player tidak ditemukan untuk membuka panel!</red>"));
                return;
            }
            boolean isBedrock = false;
            try {
                if (Bukkit.getPluginManager().isPluginEnabled("floodgate")) {
                    isBedrock = org.geysermc.floodgate.api.FloodgateApi.getInstance().isFloodgatePlayer(target.getUniqueId());
                }
            } catch (Throwable ignored) {}
            
            id.naturalsmp.naturalSchool.ui.gui.ClassroomManagerGui gui = new id.naturalsmp.naturalSchool.ui.gui.ClassroomManagerGui(plugin);
            if (isBedrock) {
                gui.openClassroomManagerBedrock(target);
            } else {
                gui.openClassroomManagerJava(target);
            }
            sender.sendMessage(MiniMessage.miniMessage().deserialize("<green>Membuka panel manajemen kelas untuk " + target.getName() + ".</green>"));
        } else if (sub.equals("area")) {
            if (args.length < 4) {
                sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>Format: /ns class area <1-12> <set/remove></red>"));
                return;
            }
            int classNum;
            try {
                classNum = Integer.parseInt(args[2].replaceAll("\\D+", ""));
            } catch (Exception e) {
                sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>Kelas harus berupa angka 1-12!</red>"));
                return;
            }
            if (classNum < 1 || classNum > 12) {
                sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>Kelas harus bernilai 1-12!</red>"));
                return;
            }
            String action = args[3].toLowerCase();
            if (action.equals("set")) {
                if (!(sender instanceof Player)) {
                    sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>Hanya player yang dapat menggunakan seleksi WorldEdit!</red>"));
                    return;
                }
                Player p = (Player) sender;
                try {
                    Integer[] sel = getWorldEditSelection(p);
                    if (sel != null) {
                        plugin.getClassroomManager().saveClassroomBounds(
                            classNum,
                            p.getWorld().getName(),
                            sel[0], sel[1], sel[2],
                            sel[3], sel[4], sel[5]
                        );
                        p.sendMessage(MiniMessage.miniMessage().deserialize("<green>Berhasil menyimpan batas koordinat area kelas " + classNum + " sesuai seleksi WorldEdit Anda!</green>"));
                    } else {
                        p.sendMessage(MiniMessage.miniMessage().deserialize("<red>Gagal! Anda belum membuat seleksi WorldEdit.</red>"));
                    }
                } catch (Throwable t) {
                    p.sendMessage(MiniMessage.miniMessage().deserialize("<red>Gagal mengambil seleksi: " + t.getMessage() + "</red>"));
                }
            } else if (action.equals("remove")) {
                plugin.getClassroomManager().saveClassroomBounds(classNum, null, null, null, null, null, null, null);
                sender.sendMessage(MiniMessage.miniMessage().deserialize("<green>Berhasil menghapus batas koordinat area kelas " + classNum + ".</green>"));
            }
        } else if (sub.equals("door")) {
            if (args.length < 5) {
                sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>Format: /ns class door <1-12> <door_num> <set/remove></red>"));
                return;
            }
            int classNum;
            int doorNum;
            try {
                classNum = Integer.parseInt(args[2].replaceAll("\\D+", ""));
                doorNum = Integer.parseInt(args[3].replaceAll("\\D+", ""));
            } catch (Exception e) {
                sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>Kelas dan nomor pintu harus berupa angka!</red>"));
                return;
            }
            if (classNum < 1 || classNum > 12) {
                sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>Kelas harus bernilai 1-12!</red>"));
                return;
            }
            String action = args[4].toLowerCase();
            if (action.equals("set")) {
                if (!(sender instanceof Player)) {
                    sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>Hanya player yang dapat menggunakan seleksi WorldEdit!</red>"));
                    return;
                }
                Player p = (Player) sender;
                try {
                    Integer[] sel = getWorldEditSelection(p);
                    if (sel != null) {
                        plugin.getClassroomManager().addDoor(
                            classNum,
                            doorNum,
                            p.getWorld().getName(),
                            sel[0], sel[1], sel[2],
                            sel[3], sel[4], sel[5]
                        );
                        p.sendMessage(MiniMessage.miniMessage().deserialize("<green>Berhasil menambahkan/mengatur koordinat Pintu " + doorNum + " untuk kelas " + classNum + " sesuai seleksi WorldEdit Anda!</green>"));
                    } else {
                        p.sendMessage(MiniMessage.miniMessage().deserialize("<red>Gagal! Anda belum membuat seleksi WorldEdit.</red>"));
                    }
                } catch (Throwable t) {
                    p.sendMessage(MiniMessage.miniMessage().deserialize("<red>Gagal mengambil seleksi: " + t.getMessage() + "</red>"));
                }
            } else if (action.equals("remove")) {
                plugin.getClassroomManager().removeDoor(classNum, doorNum);
                sender.sendMessage(MiniMessage.miniMessage().deserialize("<green>Berhasil menghapus Pintu " + doorNum + " untuk kelas " + classNum + ".</green>"));
            }
        }
    }

    private void handleReload(CommandSender sender) {
        sender.sendMessage(MiniMessage.miniMessage().deserialize("<yellow>Reloading configuration...</yellow>"));
        plugin.reloadConfig();
        plugin.getRankPrefixConfig().load();
        plugin.getDatabaseManager().reload();
        plugin.getExamManager().reload();
        sender.sendMessage(MiniMessage.miniMessage().deserialize("<green>NaturalSchool configuration and database pool reloaded successfully.</green>"));
    }

    private void handleSemesterCommand(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(MiniMessage.miniMessage().deserialize(
                "<gold>=== NaturalSchool Semester Management ===</gold>\n" +
                "<yellow>/naturalschool semester info</yellow> - <gray>Tampilkan status semester saat ini.</gray>\n" +
                "<yellow>/naturalschool semester end</yellow> - <gray>Paksa rotasi semester baru (asynchronous).</gray>\n" +
                "<yellow>/naturalschool semester reset</yellow> - <gray>Reset semester kembali ke kalender real-life.</gray>"
            ));
            return;
        }

        String sub = args[1].toLowerCase();
        if ("info".equals(sub)) {
            String academicYear = plugin.getSemesterManager().getCurrentAcademicYear();
            String semester = plugin.getSemesterManager().getCurrentSemester();
            sender.sendMessage(MiniMessage.miniMessage().deserialize(
                "<gold>=== Status Akademik Saat Ini ===</gold>\n" +
                "<yellow>Tahun Akademik:</yellow> <white>" + academicYear + "</white>\n" +
                "<yellow>Semester:</yellow> <white>" + semester + "</white>"
            ));
        } else if ("end".equals(sub)) {
            sender.sendMessage(MiniMessage.miniMessage().deserialize("<yellow>Memulai transaksi rotasi semester secara asynchronous...</yellow>"));
            plugin.getSemesterManager().processSemesterEnd().thenAccept(affected -> {
                sender.sendMessage(MiniMessage.miniMessage().deserialize(
                    "<green>Transaksi rotasi semester selesai! Total pelajar terdampak: </green><aqua>" + affected + "</aqua>"
                ));
            }).exceptionally(ex -> {
                sender.sendMessage(MiniMessage.miniMessage().deserialize(
                    "<red>Gagal melakukan rotasi semester: " + ex.getMessage() + "</red>"
                ));
                return null;
            });
        } else if ("reset".equals(sub)) {
            sender.sendMessage(MiniMessage.miniMessage().deserialize("<yellow>Memulai transaksi reset semester secara asynchronous...</yellow>"));
            plugin.getSemesterManager().resetSemesterState().thenAccept(affected -> {
                sender.sendMessage(MiniMessage.miniMessage().deserialize(
                    "<green>Transaksi reset semester selesai! Total pelajar terdampak: </green><aqua>" + affected + "</aqua>"
                ));
            }).exceptionally(ex -> {
                sender.sendMessage(MiniMessage.miniMessage().deserialize(
                    "<red>Gagal melakukan reset semester: " + ex.getMessage() + "</red>"
                ));
                return null;
            });
        } else {
            sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>Subcommand tidak dikenal. Gunakan info, end, atau reset.</red>"));
        }
    }

    private void handleExamCommand(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(MiniMessage.miniMessage().deserialize(
                "<gold>=== NaturalSchool Exam Management ===</gold>\n" +
                "<yellow>/naturalschool exam open</yellow> - <gray>Membuka akses portal ujian.</gray>\n" +
                "<yellow>/naturalschool exam close</yellow> - <gray>Menutup akses portal ujian.</gray>\n" +
                "<yellow>/naturalschool exam message <message></yellow> - <gray>Mengatur pesan portal ujian.</gray>\n" +
                "<yellow>/naturalschool exam sync</yellow> - <gray>Sinkronisasi semua soal dari database.</gray>"
            ));
            return;
        }

        String sub = args[1].toLowerCase();
        if ("open".equals(sub)) {
            plugin.setExamOpen(true);
            plugin.setExamForceOpen(true);
            sender.sendMessage(MiniMessage.miniMessage().deserialize("<green>Portal Ujian berhasil DIBUKA secara paksa (Bypass jadwal semester).</green>"));
        } else if ("close".equals(sub)) {
            plugin.setExamOpen(false);
            plugin.setExamForceOpen(false);
            sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>Portal Ujian berhasil DITUTUP.</red>"));
        } else if ("message".equals(sub)) {
            if (args.length < 3) {
                sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>Harap masukkan pesan! /ns exam message <message></red>"));
                return;
            }
            StringBuilder sb = new StringBuilder();
            for (int i = 2; i < args.length; i++) {
                sb.append(args[i]).append(" ");
            }
            String msg = sb.toString().trim();
            plugin.setExamMessage(msg);
            sender.sendMessage(MiniMessage.miniMessage().deserialize("<green>Pesan portal ujian berhasil diubah menjadi:</green>\n" + msg));
        } else if ("sync".equals(sub)) {
            sender.sendMessage(MiniMessage.miniMessage().deserialize("<yellow>Memulai sinkronisasi bank soal dari database...</yellow>"));
            plugin.getExamManager().forceSyncFromDatabase().thenAccept(success -> {
                if (success) {
                    sender.sendMessage(MiniMessage.miniMessage().deserialize("<green>Sinkronisasi bank soal berhasil! File cache local dan memori diperbarui.</green>"));
                } else {
                    sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>Gagal melakukan sinkronisasi bank soal! Periksa log konsol untuk detail.</red>"));
                }
            });
        } else {
            sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>Subcommand tidak dikenal. Gunakan open, close, message, atau sync.</red>"));
        }
    }

    private void handleInfo(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>Usage: /naturalschool info <player></red>"));
            return;
        }

        String targetName = args[1];
        Player targetPlayer = Bukkit.getPlayerExact(targetName);

        if (targetPlayer != null) {
            StudentProfile profile = plugin.getProfileManager().getProfile(targetPlayer.getUniqueId());
            if (profile != null) {
                displayProfile(sender, targetPlayer.getName(), profile, true);
            } else {
                sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>Profile cache not loaded for online player " + targetPlayer.getName() + ".</red>"));
            }
        } else {
            sender.sendMessage(MiniMessage.miniMessage().deserialize("<yellow>Player is offline. Fetching profile from database...</yellow>"));
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                OfflinePlayer offlineTarget = Bukkit.getOfflinePlayer(targetName);
                UUID uuid = offlineTarget.getUniqueId();
                try {
                    StudentProfile profile = plugin.getDatabaseManager().loadProfile(uuid);
                    if (profile != null) {
                        displayProfile(sender, offlineTarget.getName() != null ? offlineTarget.getName() : targetName, profile, false);
                    } else {
                        sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>Player profile not found in cache or database.</red>"));
                    }
                } catch (Exception e) {
                    sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>Failed to query database: " + e.getMessage() + "</red>"));
                }
            });
        }
    }

    private void displayProfile(CommandSender sender, String targetName, StudentProfile profile, boolean isOnline) {
        String status = isOnline ? "<green>ONLINE</green>" : "<gray>OFFLINE</gray>";
        sender.sendMessage(MiniMessage.miniMessage().deserialize(
            "<gold>=== Student Profile: " + targetName + " (" + status + ") ===</gold>\n" +
            "<yellow>UUID:</yellow> <white>" + profile.getUuid() + "</white>\n" +
            "<yellow>Username:</yellow> <white>" + (profile.getUsername() != null ? profile.getUsername() : "Unknown") + "</white>\n" +
            "<yellow>NIS:</yellow> <white>" + (profile.getNis() != null ? profile.getNis() : "Not Assigned") + "</white>\n" +
            "<yellow>School Rank:</yellow> " + profile.getRank().getDisplayName() + " <gray>(" + profile.getRank().name() + ")</gray>\n" +
            "<yellow>Academic Stage:</yellow> <white>" + profile.getAcademicStage() + "</white>\n" +
            "<yellow>Academic Class:</yellow> <white>" + profile.getAcademicClass() + "</white>\n" +
            "<yellow>Last Updated:</yellow> <gray>" + profile.getLastUpdated() + "</gray>"
        ));
    }

    private void handleRankCommand(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(MiniMessage.miniMessage().deserialize(
                "<gold>=== Perintah Rank NaturalSchool ===</gold>\n" +
                "<yellow>/ns rank set <player> <rank></yellow> - <gray>Mengatur pangkat internal sekolah pemain.</gray>\n" +
                "<yellow>/ns rank list</yellow> - <gray>Menampilkan daftar pangkat beserta prefix dari database.</gray>\n" +
                "<yellow>/ns rank update <RANK|CLASS|ROLE> <key> <prefix></yellow> - <gray>Mengubah prefix tertentu langsung di database.</gray>"
            ));
            return;
        }

        String sub = args[1].toLowerCase();
        if (sub.equals("set")) {
            if (args.length < 4) {
                sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>Format: /ns rank set <player> <rank></red>"));
                return;
            }

            String targetName = args[2];
            String rawRank = args[3].toUpperCase();
            SchoolRank newRank;
            try {
                newRank = SchoolRank.valueOf(rawRank);
            } catch (IllegalArgumentException e) {
                sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>Pangkat tidak dikenal!</red>"));
                return;
            }

            Player targetPlayer = Bukkit.getPlayerExact(targetName);
            if (targetPlayer != null) {
                StudentProfile profile = plugin.getProfileManager().getProfile(targetPlayer.getUniqueId());
                if (profile != null) {
                    SchoolRank oldRank = profile.getRank();
                    if (oldRank == newRank) {
                        sender.sendMessage(MiniMessage.miniMessage().deserialize("<yellow>" + targetPlayer.getName() + " sudah memiliki pangkat tersebut.</yellow>"));
                        return;
                    }

                    plugin.getNaturalSchoolAPI().setPlayerRank(targetPlayer.getUniqueId(), newRank);

                    if (profile.getRank() != newRank) {
                        sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>Perubahan pangkat dibatalkan oleh plugin lain.</red>"));
                    } else {
                        sender.sendMessage(MiniMessage.miniMessage().deserialize("<green>Berhasil mengatur pangkat </green>" + newRank.getDisplayName() + "<green> untuk " + targetPlayer.getName() + ".</green>"));
                    }
                } else {
                    sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>Gagal memuat profil cache untuk " + targetPlayer.getName() + ".</red>"));
                }
            } else {
                sender.sendMessage(MiniMessage.miniMessage().deserialize("<yellow>Pemain offline. Memperbarui di database...</yellow>"));
                OfflinePlayer offlineTarget = Bukkit.getOfflinePlayer(targetName);
                UUID uuid = offlineTarget.getUniqueId();

                plugin.getNaturalSchoolAPI().getOfflineProfile(uuid).thenAccept(optProfile -> {
                    if (optProfile.isPresent()) {
                        plugin.getNaturalSchoolAPI().setPlayerRank(uuid, newRank);
                        sender.sendMessage(MiniMessage.miniMessage().deserialize("<green>Berhasil mengatur pangkat </green>" + newRank.getDisplayName() + "<green> untuk pemain offline " + (offlineTarget.getName() != null ? offlineTarget.getName() : targetName) + ".</green>"));
                    } else {
                        sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>Profil pemain tidak ditemukan di database.</red>"));
                    }
                }).exceptionally(ex -> {
                    sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>Gagal memperbarui pangkat pemain offline: " + ex.getMessage() + "</red>"));
                    return null;
                });
            }
        } else if (sub.equals("list")) {
            sender.sendMessage(MiniMessage.miniMessage().deserialize("<gold>=== Daftar Prefix NaturalSchool (Database) ===</gold>"));
            sender.sendMessage(MiniMessage.miniMessage().deserialize("<yellow>» Pangkat (Ranks):</yellow>"));
            for (SchoolRank rank : SchoolRank.values()) {
                String prefix = plugin.getRankPrefixConfig().getFormattedPrefix(rank);
                sender.sendMessage(MiniMessage.miniMessage().deserialize("  - <white>" + rank.name() + ":</white> " + prefix));
            }
            sender.sendMessage(MiniMessage.miniMessage().deserialize("<yellow>» Tingkatan Kelas (Class Prefixes):</yellow>"));
            for (int i = 1; i <= 12; i++) {
                String prefix = plugin.getRankPrefixConfig().getClassPrefix(i);
                if (!prefix.isEmpty()) {
                    sender.sendMessage(MiniMessage.miniMessage().deserialize("  - <white>Kelas " + i + ":</white> " + prefix));
                }
            }
            sender.sendMessage(MiniMessage.miniMessage().deserialize("<yellow>» Peran Kelas (Class Roles):</yellow>"));
            for (String role : Arrays.asList("WALI_KELAS", "KETUA", "WAKIL", "SEKRETARIS", "BENDAHARA", "ANGGOTA")) {
                String prefix = plugin.getRankPrefixConfig().getClassRolePrefix(role);
                if (!prefix.isEmpty()) {
                    sender.sendMessage(MiniMessage.miniMessage().deserialize("  - <white>" + role + ":</white> " + prefix));
                }
            }
        } else if (sub.equals("update")) {
            if (args.length < 5) {
                sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>Format: /ns rank update <RANK|CLASS|ROLE> <key> <prefix></red>"));
                return;
            }
            String rawType = args[2].toUpperCase();
            String rawKey = args[3];
            
            // Rebuild multi-word prefix if it was split by space
            StringBuilder sb = new StringBuilder();
            for (int i = 4; i < args.length; i++) {
                sb.append(args[i]).append(" ");
            }
            String prefix = sb.toString().trim();

            if (!Arrays.asList("RANK", "CLASS", "ROLE").contains(rawType)) {
                sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>Tipe harus bernilai RANK, CLASS, atau ROLE!</red>"));
                return;
            }

            if ("RANK".equals(rawType)) {
                try {
                    SchoolRank.valueOf(rawKey.toUpperCase());
                } catch (IllegalArgumentException e) {
                    sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>Pangkat tidak dikenal!</red>"));
                    return;
                }
                rawKey = rawKey.toUpperCase();
            } else if ("ROLE".equals(rawType)) {
                rawKey = rawKey.toUpperCase();
            }

            final String type = rawType;
            final String key = rawKey;
            
            // Save asynchronously
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                plugin.getDatabaseManager().savePrefix(type, key, prefix);
                // Reload in config
                plugin.getRankPrefixConfig().load();
                sender.sendMessage(MiniMessage.miniMessage().deserialize("<green>Berhasil memperbarui prefix " + type + " [" + key + "] di database!</green>"));
            });
        } else {
            sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>Subcommand tidak dikenal. Gunakan set, list, atau update.</red>"));
        }
    }

    private void handleSetClass(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>Usage: /naturalschool setclass <player> <1-12></red>"));
            return;
        }

        String targetName = args[1];
        int academicClass;
        try {
            academicClass = Integer.parseInt(args[2]);
            if (academicClass < 1 || academicClass > 12) {
                sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>Class must be a number between 1 and 12.</red>"));
                return;
            }
        } catch (NumberFormatException e) {
            sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>Class must be a valid integer.</red>"));
            return;
        }

        Player targetPlayer = Bukkit.getPlayerExact(targetName);
        if (targetPlayer != null) {
            StudentProfile profile = plugin.getProfileManager().getProfile(targetPlayer.getUniqueId());
            if (profile != null) {
                int oldClass = profile.getAcademicClass();
                if (oldClass == academicClass) {
                    sender.sendMessage(MiniMessage.miniMessage().deserialize("<yellow>" + targetPlayer.getName() + " is already in class " + academicClass + ".</yellow>"));
                    return;
                }

                plugin.getNaturalSchoolAPI().setPlayerClass(targetPlayer.getUniqueId(), academicClass);

                if (profile.getAcademicClass() != academicClass) {
                    sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>Class change for " + targetPlayer.getName() + " was cancelled by another plugin.</red>"));
                } else {
                    sender.sendMessage(MiniMessage.miniMessage().deserialize("<green>Set class to " + academicClass + " for " + targetPlayer.getName() + ".</green>"));
                }
            } else {
                sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>Profile cache not loaded for " + targetPlayer.getName() + ".</red>"));
            }
        } else {
            sender.sendMessage(MiniMessage.miniMessage().deserialize("<yellow>Player is offline. Updating profile in database...</yellow>"));
            OfflinePlayer offlineTarget = Bukkit.getOfflinePlayer(targetName);
            UUID uuid = offlineTarget.getUniqueId();

            plugin.getNaturalSchoolAPI().getOfflineProfile(uuid).thenAccept(optProfile -> {
                if (optProfile.isPresent()) {
                    plugin.getNaturalSchoolAPI().setPlayerClass(uuid, academicClass);
                    sender.sendMessage(MiniMessage.miniMessage().deserialize("<green>Set class to " + academicClass + " for offline player " + (offlineTarget.getName() != null ? offlineTarget.getName() : targetName) + ".</green>"));
                } else {
                    sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>Player profile not found in database.</red>"));
                }
            }).exceptionally(ex -> {
                sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>Failed to update offline player class: " + ex.getMessage() + "</red>"));
                return null;
            });
        }
    }

    private void handleSetStage(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>Usage: /naturalschool setstage <player> <SD|SMP|SMA></red>"));
            return;
        }

        String targetName = args[1];
        String stage = args[2].toUpperCase();
        if (!stage.equals("SD") && !stage.equals("SMP") && !stage.equals("SMA")) {
            sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>Stage must be SD, SMP, or SMA.</red>"));
            return;
        }

        Player targetPlayer = Bukkit.getPlayerExact(targetName);
        if (targetPlayer != null) {
            StudentProfile profile = plugin.getProfileManager().getProfile(targetPlayer.getUniqueId());
            if (profile != null) {
                String oldStage = profile.getAcademicStage();
                if (stage.equals(oldStage)) {
                    sender.sendMessage(MiniMessage.miniMessage().deserialize("<yellow>" + targetPlayer.getName() + " is already in stage " + stage + ".</yellow>"));
                    return;
                }

                plugin.getNaturalSchoolAPI().setPlayerStage(targetPlayer.getUniqueId(), stage);

                if (!stage.equals(profile.getAcademicStage())) {
                    sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>Stage change for " + targetPlayer.getName() + " was cancelled by another plugin.</red>"));
                } else {
                    sender.sendMessage(MiniMessage.miniMessage().deserialize("<green>Set academic stage to " + stage + " for " + targetPlayer.getName() + ".</green>"));
                }
            } else {
                sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>Profile cache not loaded for " + targetPlayer.getName() + ".</red>"));
            }
        } else {
            sender.sendMessage(MiniMessage.miniMessage().deserialize("<yellow>Player is offline. Updating profile in database...</yellow>"));
            OfflinePlayer offlineTarget = Bukkit.getOfflinePlayer(targetName);
            UUID uuid = offlineTarget.getUniqueId();

            plugin.getNaturalSchoolAPI().getOfflineProfile(uuid).thenAccept(optProfile -> {
                if (optProfile.isPresent()) {
                    plugin.getNaturalSchoolAPI().setPlayerStage(uuid, stage);
                    sender.sendMessage(MiniMessage.miniMessage().deserialize("<green>Set academic stage to " + stage + " for offline player " + (offlineTarget.getName() != null ? offlineTarget.getName() : targetName) + ".</green>"));
                } else {
                    sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>Player profile not found in database.</red>"));
                }
            }).exceptionally(ex -> {
                sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>Failed to update offline player stage: " + ex.getMessage() + "</red>"));
                return null;
            });
        }
    }

    private void handleNisCommand(CommandSender sender, String[] args) {
        if (!hasSubCommandPermission(sender, "nis")) {
            sender.sendMessage(MiniMessage.miniMessage().deserialize(NO_PERMISSION));
            return;
        }

        if (args.length < 2) {
            sendNisHelp(sender);
            return;
        }

        String sub = args[1].toLowerCase();
        switch (sub) {
            case "register":
                handleNisRegister(sender, args);
                break;
            case "unregister":
                handleNisUnregister(sender, args);
                break;
            case "set":
                handleNisSet(sender, args);
                break;
            case "show":
                handleNisShow(sender, args);
                break;
            case "help":
            default:
                sendNisHelp(sender);
                break;
        }
    }

    private void sendNisHelp(CommandSender sender) {
        sender.sendMessage(MiniMessage.miniMessage().deserialize(
            "<gold>=== NaturalSchool NIS Subsystem Help ===</gold>\n" +
            "<yellow>/naturalschool nis register <player></yellow> - <gray>Daftarkan NIS resmi siswa.</gray>\n" +
            "<yellow>/naturalschool nis unregister <player></yellow> - <gray>Batalkan pendaftaran NIS siswa (memerlukan konfirmasi).</gray>\n" +
            "<yellow>/naturalschool nis set <player> <10-digit></yellow> - <gray>Atur NIS kustom untuk siswa.</gray>\n" +
            "<yellow>/naturalschool nis show [player]</yellow> - <gray>Lihat data NIS siswa.</gray>\n" +
            "<yellow>/naturalschool nis help</yellow> - <gray>Tampilkan menu bantuan NIS ini.</gray>"
        ));
    }

    private void handleNisRegister(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>Usage: /naturalschool nis register <player></red>"));
            return;
        }

        String targetName = args[2];
        Player targetPlayer = Bukkit.getPlayerExact(targetName);
        if (targetPlayer != null) {
            StudentProfile profile = plugin.getProfileManager().getProfile(targetPlayer.getUniqueId());
            if (profile != null) {
                if (profile.getNis() != null) {
                    sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>Player already has NIS: " + profile.getNis() + "</red>"));
                    return;
                }
                performNisRegistration(sender, targetPlayer.getUniqueId(), targetPlayer.getName(), profile);
            } else {
                sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>Profile cache not loaded for online player " + targetPlayer.getName() + ".</red>"));
            }
        } else {
            OfflinePlayer offlineTarget = Bukkit.getOfflinePlayer(targetName);
            UUID uuid = offlineTarget.getUniqueId();
            sender.sendMessage(MiniMessage.miniMessage().deserialize("<yellow>Player is offline. Fetching from database...</yellow>"));
            plugin.getNaturalSchoolAPI().getOfflineProfile(uuid).thenAccept(optProfile -> {
                if (optProfile.isPresent()) {
                    StudentProfile profile = optProfile.get();
                    if (profile.getNis() != null) {
                        sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>Player already has NIS: " + profile.getNis() + "</red>"));
                        return;
                    }
                    performNisRegistration(sender, uuid, offlineTarget.getName() != null ? offlineTarget.getName() : targetName, profile);
                } else {
                    sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>Player profile not found in database.</red>"));
                }
            }).exceptionally(ex -> {
                sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>Failed to load profile: " + ex.getMessage() + "</red>"));
                return null;
            });
        }
    }

    private void performNisRegistration(CommandSender sender, UUID uuid, String name, StudentProfile profile) {
        sender.sendMessage(MiniMessage.miniMessage().deserialize("<yellow>Registering NIS for " + name + "...</yellow>"));
        CompletableFuture.supplyAsync(() -> {
            try {
                return plugin.getDatabaseManager().getRegisteredNisCount();
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }, runnable -> Bukkit.getScheduler().runTaskAsynchronously(plugin, runnable))
        .thenAccept(count -> {
            String generatedNis = generateSpecialNis(count);
            
            profile.setNis(generatedNis);
            profile.setAcademicStage("SD");
            profile.setAcademicClass(1);
            profile.setRank(SchoolRank.SD_1);
            
            plugin.getProfileManager().saveProfileAsync(profile).thenRun(() -> {
                sender.sendMessage(MiniMessage.miniMessage().deserialize(
                    "<green>Successfully registered " + name + "! NIS: " + generatedNis + " (SD Class 1).</green>"
                ));
            }).exceptionally(ex -> {
                sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>Failed to save updated profile: " + ex.getMessage() + "</red>"));
                return null;
            });
        }).exceptionally(ex -> {
            sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>Failed to get database registration count: " + ex.getMessage() + "</red>"));
            return null;
        });
    }

    private String generateSpecialNis(int registeredCount) {
        java.time.LocalDate now = java.time.LocalDate.now();
        java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("ddMMyy");
        String dateStr = now.format(formatter);
        int sequence = registeredCount + 1;
        return "1" + String.format("%03d", sequence) + dateStr;
    }

    private void handleGuiCommand(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>Usage: /naturalschool gui <welcome|exam1|exam2|exam3|exam4|exam5|version> [player]</red>"));
            return;
        }

        String action = args[1].toLowerCase();
        if ("version".equals(action)) {
            sender.sendMessage(MiniMessage.miniMessage().deserialize(
                "<gold>NaturalSchool GUI Version: <white>" + id.naturalsmp.naturalSchool.ui.UIManager.GUI_VERSION + "</white></gold>"
            ));
            return;
        }

        if (args.length < 3) {
            sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>Usage: /naturalschool gui <welcome|exam1|exam2|exam3|exam4|exam5> <player></red>"));
            return;
        }

        List<String> validActions = Arrays.asList("welcome", "exam1", "exam2", "exam3", "exam4", "exam5");
        if (!validActions.contains(action)) {
            sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>Unknown action. Usage: /naturalschool gui <welcome|exam1|exam2|exam3|exam4|exam5> <player></red>"));
            return;
        }

        String targetName = args[2];
        Player targetPlayer = Bukkit.getPlayer(targetName);
        if (targetPlayer == null || !targetPlayer.isOnline()) {
            sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>Player " + targetName + " is not online!</red>"));
            return;
        }

        switch (action) {
            case "welcome":
                plugin.getUiManager().startOnboarding(targetPlayer);
                sender.sendMessage(MiniMessage.miniMessage().deserialize(
                    "<green>Successfully started onboarding welcome GUI for " + targetPlayer.getName() + ".</green>"
                ));
                break;
            case "exam1":
                plugin.getUiManager().openExam1(targetPlayer, false);
                sender.sendMessage(MiniMessage.miniMessage().deserialize(
                    "<green>Successfully opened Exam 1 GUI for " + targetPlayer.getName() + ".</green>"
                ));
                break;
            case "exam2":
                plugin.getUiManager().openExam2(targetPlayer);
                sender.sendMessage(MiniMessage.miniMessage().deserialize(
                    "<green>Successfully opened Exam 2 GUI for " + targetPlayer.getName() + ".</green>"
                ));
                break;
            case "exam3":
                plugin.getUiManager().openExam3(targetPlayer, false);
                sender.sendMessage(MiniMessage.miniMessage().deserialize(
                    "<green>Successfully opened Exam 3 GUI for " + targetPlayer.getName() + ".</green>"
                ));
                break;
            case "exam4":
                plugin.getUiManager().openExam4(targetPlayer);
                sender.sendMessage(MiniMessage.miniMessage().deserialize(
                    "<green>Successfully opened Exam 4 GUI for " + targetPlayer.getName() + ".</green>"
                ));
                break;
            case "exam5":
                plugin.getUiManager().openExam5(targetPlayer, false);
                sender.sendMessage(MiniMessage.miniMessage().deserialize(
                    "<green>Successfully opened Exam 5 GUI for " + targetPlayer.getName() + ".</green>"
                ));
                break;
        }
    }

    private void handleNisUnregister(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>Usage: /naturalschool nis unregister <player></red>"));
            return;
        }

        String targetName = args[2];
        UUID targetUuid;
        Player targetPlayer = Bukkit.getPlayerExact(targetName);
        if (targetPlayer != null) {
            targetUuid = targetPlayer.getUniqueId();
        } else {
            targetUuid = Bukkit.getOfflinePlayer(targetName).getUniqueId();
        }

        String key = sender.getName() + ":" + targetUuid.toString();
        long now = System.currentTimeMillis();

        if (unregisterConfirmations.containsKey(key) && unregisterConfirmations.get(key) > now) {
            unregisterConfirmations.remove(key);
            performNisUnregistration(sender, targetUuid, targetPlayer != null ? targetPlayer.getName() : targetName);
        } else {
            unregisterConfirmations.put(key, now + 15000);
            sender.sendMessage(MiniMessage.miniMessage().deserialize(
                "<yellow>WARNING: You are about to unregister NIS for " + targetName + ". This will reset their academic progress to NONE/0. Run the command again within 15 seconds to confirm.</yellow>"
            ));
        }
    }

    private void performNisUnregistration(CommandSender sender, UUID uuid, String name) {
        sender.sendMessage(MiniMessage.miniMessage().deserialize("<yellow>Unregistering NIS for " + name + "...</yellow>"));
        
        Player targetPlayer = Bukkit.getPlayer(uuid);
        if (targetPlayer != null) {
            StudentProfile profile = plugin.getProfileManager().getProfile(uuid);
            if (profile != null) {
                profile.setNis(null);
                profile.setAcademicStage("NONE");
                profile.setAcademicClass(0);
                profile.setRank(SchoolRank.NONE);
                plugin.getProfileManager().saveProfileAsync(profile).thenRun(() -> {
                    sender.sendMessage(MiniMessage.miniMessage().deserialize(
                        "<green>Successfully unregistered " + name + ". Progress reset.</green>"
                    ));
                });
            } else {
                sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>Profile cache not loaded for " + name + ".</red>"));
            }
        } else {
            plugin.getNaturalSchoolAPI().getOfflineProfile(uuid).thenAccept(optProfile -> {
                if (optProfile.isPresent()) {
                    StudentProfile profile = optProfile.get();
                    profile.setNis(null);
                    profile.setAcademicStage("NONE");
                    profile.setAcademicClass(0);
                    profile.setRank(SchoolRank.NONE);
                    plugin.getDatabaseManager().saveProfile(profile);
                    sender.sendMessage(MiniMessage.miniMessage().deserialize(
                        "<green>Successfully unregistered offline player " + name + ". Progress reset.</green>"
                    ));
                } else {
                    sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>Player profile not found in database.</red>"));
                }
            }).exceptionally(ex -> {
                sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>Failed to unregister: " + ex.getMessage() + "</red>"));
                return null;
            });
        }
    }

    private void handleNisSet(CommandSender sender, String[] args) {
        if (args.length < 4) {
            sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>Usage: /naturalschool nis set <player> <10-digit-nis></red>"));
            return;
        }

        String targetName = args[2];
        String customNis = args[3];

        if (!customNis.matches("^\\d{10}$")) {
            sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>NIS must be exactly a 10-digit numerical number.</red>"));
            return;
        }

        Player targetPlayer = Bukkit.getPlayerExact(targetName);
        if (targetPlayer != null) {
            StudentProfile profile = plugin.getProfileManager().getProfile(targetPlayer.getUniqueId());
            if (profile != null) {
                boolean wasUnregistered = (profile.getNis() == null);
                profile.setNis(customNis);
                if (wasUnregistered) {
                    profile.setAcademicStage("SD");
                    profile.setAcademicClass(1);
                    profile.setRank(SchoolRank.SD_1);
                }
                plugin.getProfileManager().saveProfileAsync(profile).thenRun(() -> {
                    sender.sendMessage(MiniMessage.miniMessage().deserialize(
                        "<green>Set NIS to " + customNis + " for " + targetPlayer.getName() + 
                        (wasUnregistered ? " (Registered to SD Class 1)" : "") + ".</green>"
                    ));
                });
            } else {
                sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>Profile cache not loaded for online player " + targetPlayer.getName() + ".</red>"));
            }
        } else {
            sender.sendMessage(MiniMessage.miniMessage().deserialize("<yellow>Player is offline. Fetching from database...</yellow>"));
            OfflinePlayer offlineTarget = Bukkit.getOfflinePlayer(targetName);
            UUID uuid = offlineTarget.getUniqueId();
            plugin.getNaturalSchoolAPI().getOfflineProfile(uuid).thenAccept(optProfile -> {
                if (optProfile.isPresent()) {
                    StudentProfile profile = optProfile.get();
                    boolean wasUnregistered = (profile.getNis() == null);
                    profile.setNis(customNis);
                    if (wasUnregistered) {
                        profile.setAcademicStage("SD");
                        profile.setAcademicClass(1);
                        profile.setRank(SchoolRank.SD_1);
                    }
                    plugin.getDatabaseManager().saveProfile(profile);
                    sender.sendMessage(MiniMessage.miniMessage().deserialize(
                        "<green>Set NIS to " + customNis + " for offline player " + (offlineTarget.getName() != null ? offlineTarget.getName() : targetName) +
                        (wasUnregistered ? " (Registered to SD Class 1)" : "") + ".</green>"
                    ));
                } else {
                    sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>Player profile not found in database.</red>"));
                }
            }).exceptionally(ex -> {
                sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>Failed to set custom NIS: " + ex.getMessage() + "</red>"));
                return null;
            });
        }
    }

    private void handleNisShow(CommandSender sender, String[] args) {
        String targetName;
        if (args.length < 3) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>Console must specify a player name: /ns nis show <player></red>"));
                return;
            }
            targetName = sender.getName();
        } else {
            targetName = args[2];
        }

        Player targetPlayer = Bukkit.getPlayerExact(targetName);
        if (targetPlayer != null) {
            StudentProfile profile = plugin.getProfileManager().getProfile(targetPlayer.getUniqueId());
            if (profile != null) {
                String nis = profile.getNis() != null ? profile.getNis() : "<red>Unregistered (NULL)</red>";
                sender.sendMessage(MiniMessage.miniMessage().deserialize(
                    "<gold>NIS for player <white>" + targetPlayer.getName() + "</white> is " + nis + "</gold>"
                ));
            } else {
                sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>Profile cache not loaded for online player " + targetPlayer.getName() + ".</red>"));
            }
        } else {
            OfflinePlayer offlineTarget = Bukkit.getOfflinePlayer(targetName);
            UUID uuid = offlineTarget.getUniqueId();
            plugin.getNaturalSchoolAPI().getOfflineProfile(uuid).thenAccept(optProfile -> {
                if (optProfile.isPresent()) {
                    StudentProfile profile = optProfile.get();
                    String nis = profile.getNis() != null ? profile.getNis() : "<red>Unregistered (NULL)</red>";
                    sender.sendMessage(MiniMessage.miniMessage().deserialize(
                        "<gold>NIS for offline player <white>" + (offlineTarget.getName() != null ? offlineTarget.getName() : targetName) + "</white> is " + nis + "</gold>"
                    ));
                } else {
                    sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>Player profile not found in database.</red>"));
                }
            }).exceptionally(ex -> {
                sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>Failed to query NIS: " + ex.getMessage() + "</red>"));
                return null;
            });
        }
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!hasAnyAdminPermission(sender)) {
            return Collections.emptyList();
        }

        if (args.length >= 2) {
            String subCommand = args[0].toLowerCase();
            String group = getPermissionGroup(subCommand);
            if (group != null && !hasSubCommandPermission(sender, group)) {
                return Collections.emptyList();
            }
        }

        if (args.length == 1) {
            List<String> subCommands = Arrays.asList("reload", "info", "rank", "setclass", "setstage", "nis", "gui", "semester", "exam", "class");
            List<String> allowed = new ArrayList<>();
            for (String sub : subCommands) {
                String group = getPermissionGroup(sub);
                if (group != null && hasSubCommandPermission(sender, group)) {
                    allowed.add(sub);
                }
            }
            return filterList(allowed, args[0]);
        }

        if (args.length == 2) {
            String subCommand = args[0].toLowerCase();
            if ("nis".equals(subCommand)) {
                return filterList(Arrays.asList("register", "unregister", "set", "show", "help"), args[1]);
            } else if ("semester".equals(subCommand)) {
                return filterList(Arrays.asList("info", "end", "reset"), args[1]);
            } else if ("exam".equals(subCommand)) {
                return filterList(Arrays.asList("open", "close", "message", "sync"), args[1]);
            } else if ("gui".equals(subCommand)) {
                return filterList(Arrays.asList("welcome", "exam1", "exam2", "exam3", "exam4", "exam5", "version"), args[1]);
            } else if ("class".equals(subCommand)) {
                return filterList(Arrays.asList("list", "spy", "setwali", "panel", "area", "door"), args[1]);
            } else if ("rank".equals(subCommand)) {
                return filterList(Arrays.asList("set", "list", "update"), args[1]);
            } else if (Arrays.asList("info", "setclass", "setstage").contains(subCommand)) {
                List<String> players = Bukkit.getOnlinePlayers().stream()
                        .map(Player::getName)
                        .collect(Collectors.toList());
                return filterList(players, args[1]);
            }
        }

        if (args.length == 3) {
            String subCommand = args[0].toLowerCase();
            if ("nis".equals(subCommand)) {
                String subNis = args[1].toLowerCase();
                if (Arrays.asList("register", "unregister", "set", "show").contains(subNis)) {
                    List<String> players = Bukkit.getOnlinePlayers().stream()
                            .map(Player::getName)
                            .collect(Collectors.toList());
                    return filterList(players, args[2]);
                }
            } else if ("gui".equals(subCommand)) {
                String subGui = args[1].toLowerCase();
                if (Arrays.asList("welcome", "exam1", "exam2", "exam3", "exam4", "exam5").contains(subGui)) {
                    List<String> players = Bukkit.getOnlinePlayers().stream()
                            .map(Player::getName)
                            .collect(Collectors.toList());
                    return filterList(players, args[2]);
                }
            } else if ("class".equals(subCommand)) {
                String subCls = args[1].toLowerCase();
                if (subCls.equals("spy") || subCls.equals("panel")) {
                    List<String> players = Bukkit.getOnlinePlayers().stream()
                            .map(Player::getName)
                            .collect(Collectors.toList());
                    return filterList(players, args[2]);
                } else if (Arrays.asList("setwali", "area", "door").contains(subCls)) {
                    List<String> classes = new ArrayList<>();
                    for (int i = 1; i <= 12; i++) {
                        classes.add("kelas" + i);
                    }
                    return filterList(classes, args[2]);
                }
            } else if ("rank".equals(subCommand)) {
                String subRank = args[1].toLowerCase();
                if ("set".equals(subRank)) {
                    List<String> players = Bukkit.getOnlinePlayers().stream()
                            .map(Player::getName)
                            .collect(Collectors.toList());
                    return filterList(players, args[2]);
                } else if ("update".equals(subRank)) {
                    return filterList(Arrays.asList("RANK", "CLASS", "ROLE"), args[2]);
                }
            } else if ("setclass".equals(subCommand)) {
                List<String> classes = new ArrayList<>();
                for (int i = 1; i <= 12; i++) {
                    classes.add(String.valueOf(i));
                }
                return filterList(classes, args[2]);
            } else if ("setstage".equals(subCommand)) {
                return filterList(Arrays.asList("SD", "SMP", "SMA"), args[2]);
            }
        }

        if (args.length == 4) {
            String subCommand = args[0].toLowerCase();
            if ("nis".equals(subCommand) && "set".equalsIgnoreCase(args[1])) {
                return filterList(Collections.singletonList("<10-digit-nis>"), args[3]);
            } else if ("class".equals(subCommand)) {
                String subCls = args[1].toLowerCase();
                if (subCls.equals("setwali")) {
                    List<String> players = Bukkit.getOnlinePlayers().stream()
                            .map(Player::getName)
                            .collect(Collectors.toList());
                    return filterList(players, args[3]);
                } else if (subCls.equals("area")) {
                    return filterList(Arrays.asList("set", "remove"), args[3]);
                } else if (subCls.equals("door")) {
                    List<String> doorNums = Arrays.asList("1", "2", "3", "4");
                    return filterList(doorNums, args[3]);
                }
            } else if ("rank".equals(subCommand)) {
                String subRank = args[1].toLowerCase();
                if ("set".equals(subRank)) {
                    List<String> ranks = Arrays.stream(SchoolRank.values())
                            .map(SchoolRank::name)
                            .collect(Collectors.toList());
                    return filterList(ranks, args[3]);
                } else if ("update".equals(subRank)) {
                    String type = args[2].toUpperCase();
                    if ("RANK".equals(type)) {
                        List<String> ranks = Arrays.stream(SchoolRank.values())
                                .map(SchoolRank::name)
                                .collect(Collectors.toList());
                        return filterList(ranks, args[3]);
                    } else if ("CLASS".equals(type)) {
                        List<String> classes = new ArrayList<>();
                        for (int i = 1; i <= 12; i++) {
                            classes.add(String.valueOf(i));
                        }
                        return filterList(classes, args[3]);
                    } else if ("ROLE".equals(type)) {
                        return filterList(Arrays.asList("WALI_KELAS", "KETUA", "WAKIL", "SEKRETARIS", "BENDAHARA", "ANGGOTA"), args[3]);
                    }
                }
            }
        }

        if (args.length == 5) {
            String subCommand = args[0].toLowerCase();
            if ("class".equals(subCommand) && "door".equalsIgnoreCase(args[1])) {
                return filterList(Arrays.asList("set", "remove"), args[4]);
            } else if ("rank".equals(subCommand) && "update".equalsIgnoreCase(args[1])) {
                return filterList(Collections.singletonList("<prefix>"), args[4]);
            }
        }

        return Collections.emptyList();
    }

    private List<String> filterList(List<String> list, String input) {
        String lowerInput = input.toLowerCase();
        return list.stream()
                .filter(s -> s.toLowerCase().startsWith(lowerInput))
                .collect(Collectors.toList());
    }

    private Integer[] getWorldEditSelection(Player player) {
        try {
            if (Bukkit.getPluginManager().getPlugin("WorldEdit") == null) {
                return null;
            }
            Class<?> bukkitAdapterClass = Class.forName("com.sk89q.worldedit.bukkit.BukkitAdapter");
            Object actor = bukkitAdapterClass.getMethod("adapt", Player.class).invoke(null, player);
            Class<?> worldEditClass = Class.forName("com.sk89q.worldedit.WorldEdit");
            Object worldEditInstance = worldEditClass.getMethod("getInstance").invoke(null);
            Object sessionManager = worldEditClass.getMethod("getSessionManager").invoke(worldEditInstance);
            Class<?> sessionOwnerClass = Class.forName("com.sk89q.worldedit.session.SessionOwner");
            Object localSession = sessionManager.getClass().getMethod("get", sessionOwnerClass).invoke(sessionManager, actor);
            Object world = actor.getClass().getMethod("getWorld").invoke(actor);
            Class<?> worldClass = Class.forName("com.sk89q.worldedit.world.World");
            Object region = localSession.getClass().getMethod("getSelection", worldClass).invoke(localSession, world);
            if (region == null) {
                return null;
            }
            Object minPoint = region.getClass().getMethod("getMinimumPoint").invoke(region);
            Object maxPoint = region.getClass().getMethod("getMaximumPoint").invoke(region);
            int minX, minY, minZ, maxX, maxY, maxZ;
            try {
                minX = (int) minPoint.getClass().getMethod("getBlockX").invoke(minPoint);
                minY = (int) minPoint.getClass().getMethod("getBlockY").invoke(minPoint);
                minZ = (int) minPoint.getClass().getMethod("getBlockZ").invoke(minPoint);
                maxX = (int) maxPoint.getClass().getMethod("getBlockX").invoke(maxPoint);
                maxY = (int) maxPoint.getClass().getMethod("getBlockY").invoke(maxPoint);
                maxZ = (int) maxPoint.getClass().getMethod("getBlockZ").invoke(maxPoint);
            } catch (NoSuchMethodException e) {
                minX = ((Double) minPoint.getClass().getMethod("getX").invoke(minPoint)).intValue();
                minY = ((Double) minPoint.getClass().getMethod("getY").invoke(minPoint)).intValue();
                minZ = ((Double) minPoint.getClass().getMethod("getZ").invoke(minPoint)).intValue();
                maxX = ((Double) maxPoint.getClass().getMethod("getX").invoke(maxPoint)).intValue();
                maxY = ((Double) maxPoint.getClass().getMethod("getY").invoke(maxPoint)).intValue();
                maxZ = ((Double) maxPoint.getClass().getMethod("getZ").invoke(maxPoint)).intValue();
            }
            return new Integer[]{minX, minY, minZ, maxX, maxY, maxZ};
        } catch (Throwable t) {
            plugin.getLogger().warning("Error getting WorldEdit selection: " + t.getMessage());
            return null;
        }
    }
}
