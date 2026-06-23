package id.naturalsmp.naturalSchool.command;

import id.naturalsmp.naturalSchool.NaturalSchool;
import id.naturalsmp.naturalSchool.profile.StudentProfile;
import id.naturalsmp.naturalSchool.ui.SchoolMenuType;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.bukkit.Bukkit;
import java.util.stream.Collectors;
import java.time.ZonedDateTime;
import id.naturalsmp.naturalSchool.semester.SemesterManager;

public class SchoolCommand implements CommandExecutor, TabCompleter {

    private final NaturalSchool plugin;

    public SchoolCommand(NaturalSchool plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>Hanya player yang dapat menggunakan command ini!</red>"));
            return true;
        }

        Player player = (Player) sender;
        StudentProfile profile = plugin.getProfileManager().getProfile(player.getUniqueId());

        // Khusus member: check if profile is null or NIS is null
        if (profile == null || profile.getNis() == null) {
            player.sendMessage(MiniMessage.miniMessage().deserialize("<red>Command ini khusus untuk member/pelajar yang terdaftar!</red>"));
            return true;
        }

        // Buat info dulu subcommandnya (if no args or args[0] is help)
        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            player.sendMessage(MiniMessage.miniMessage().deserialize(
                "<gold>=== School Commands ===</gold>\n" +
                "<yellow>/school info</yellow> - <gray>Menampilkan GUI dialog Informasi Pelajar Anda.</gray>\n" +
                "<yellow>/school class [class/player]</yellow> - <gray>Menampilkan informasi struktur organisasi kelas.</gray>\n" +
                "<yellow>/school semester</yellow> - <gray>Menampilkan kalender akademik dan sisa waktu semester.</gray>\n" +
                "<yellow>/school exam</yellow> - <gray>Membuka Portal Ujian sekolah.</gray>\n" +
                "<yellow>/school teacher salary [claim]</yellow> - <gray>Melihat dan mengklaim gaji guru.</gray>\n" +
                "<yellow>/school student violation</yellow> - <gray>Mencatat pelanggaran murid oleh Staf BK.</gray>"
            ));
            return true;
        }

        String subCommand = args[0].toLowerCase();
        if (subCommand.equals("info")) {
            plugin.getUiManager().openMenu(player, SchoolMenuType.PROFILE);
            return true;
        } else if (subCommand.equals("exam")) {
            if (!plugin.isExamOpen()) {
                plugin.getUiManager().openExamClosed(player);
                return true;
            }
            plugin.getUiManager().openExamPortal(player);
            return true;
        } else if (subCommand.equals("semester")) {
            ZonedDateTime now = plugin.getSemesterManager().getCurrentTime();
            SemesterManager.AcademicState state = plugin.getSemesterManager().getAcademicState(now);
            
            ZonedDateTime nextPhaseStart;
            int weekIndex = state.getWeekIndex();
            if (weekIndex == 0) {
                nextPhaseStart = state.getCycleStart().plusDays(7);
            } else if (weekIndex == 1) {
                nextPhaseStart = state.getCycleStart().plusDays(14);
            } else if (weekIndex == 2) {
                nextPhaseStart = state.getCycleStart().plusDays(21);
            } else if (weekIndex == 3) {
                nextPhaseStart = state.getCycleStart().plusDays(28);
            } else {
                nextPhaseStart = plugin.getSemesterManager().getFirstMondayOfMonth(state.getCycleStart().plusMonths(1));
            }
            
            long diffSeconds = java.time.Duration.between(now, nextPhaseStart).getSeconds();
            if (diffSeconds < 0) diffSeconds = 0;
            long days = diffSeconds / 86400;
            long hours = (diffSeconds % 86400) / 3600;
            long minutes = (diffSeconds % 3600) / 60;
            
            String countdown = String.format("%d hari, %d jam, %d menit", days, hours, minutes);
            
            java.time.format.DateTimeFormatter dayFormatter = java.time.format.DateTimeFormatter.ofPattern("dd MMM");
            ZonedDateTime mondayW1 = state.getCycleStart();
            ZonedDateTime mondayW2 = mondayW1.plusDays(7);
            ZonedDateTime mondayW3 = mondayW1.plusDays(14);
            ZonedDateTime mondayW4 = mondayW1.plusDays(21);
            ZonedDateTime transitionStart = mondayW1.plusDays(28);
            
            String dateW1 = mondayW1.format(dayFormatter) + " - " + mondayW1.plusDays(6).format(dayFormatter);
            String dateW2 = mondayW2.format(dayFormatter) + " - " + mondayW2.plusDays(6).format(dayFormatter);
            String dateW3 = mondayW3.format(dayFormatter) + " - " + mondayW3.plusDays(6).format(dayFormatter);
            String dateW4 = mondayW4.format(dayFormatter) + " - " + mondayW4.plusDays(6).format(dayFormatter);
            
            ZonedDateTime nextCycle = plugin.getSemesterManager().getFirstMondayOfMonth(mondayW1.plusMonths(1));
            String dateTrans = transitionStart.format(dayFormatter) + " - " + nextCycle.minusDays(1).format(dayFormatter);
            
            player.sendMessage(MiniMessage.miniMessage().deserialize(
                "<gold>=============================================</gold>\n" +
                "<yellow><bold>KALENDER AKADEMIK SEKOLAH</bold></yellow>\n" +
                "<gray>» Tahun Akademik: <white>" + state.getAcademicYear() + "</white>\n" +
                "<gray>» Semester: <white>" + state.getSemester() + "</white>\n" +
                "<gray>» Fase Aktif: <green>" + state.getPhase() + "</green>\n" +
                "<gray>» Sisa Waktu Fase: <aqua>" + countdown + "</aqua>\n" +
                "<gold>---------------------------------------------</gold>\n" +
                "<yellow><bold>JADWAL AKADEMIS BULAN INI:</bold></yellow>\n" +
                "<gray>• <white>Minggu 1 (" + dateW1 + ")</white>: Ganjil - UTS\n" +
                "  <gray>» Ujian: Sabtu, Susulan: Minggu. Senin-Selasa Libur.</gray>\n" +
                "<gray>• <white>Minggu 2 (" + dateW2 + ")</white>: Ganjil - US\n" +
                "  <gray>» Ujian: Sabtu, Susulan: Minggu. Senin-Selasa Normal.</gray>\n" +
                "<gray>• <white>Minggu 3 (" + dateW3 + ")</white>: Genap - UTS\n" +
                "  <gray>» Ujian: Sabtu, Susulan: Minggu. Senin-Selasa Libur.</gray>\n" +
                "<gray>• <white>Minggu 4 (" + dateW4 + ")</white>: Genap - UAS\n" +
                "  <gray>» Ujian: Sabtu, Susulan: Minggu. Senin-Selasa Normal.</gray>\n" +
                "<gray>• <white>Libur Akhir (" + dateTrans + ")</white>: Transisi/Libur semester.\n" +
                "<gold>=============================================</gold>"
            ));
            return true;
        } else if (subCommand.equals("class")) {
            int classNum = 0;
            if (args.length == 1) {
                classNum = profile.getAcademicClass();
                if (classNum == 0) {
                    player.sendMessage(MiniMessage.miniMessage().deserialize("<red>Anda tidak terdaftar di kelas mana pun.</red>"));
                    return true;
                }
            } else {
                String targetArg = args[1];
                try {
                    classNum = Integer.parseInt(targetArg.replaceAll("\\D+", ""));
                } catch (NumberFormatException ignored) {}

                if (classNum < 1 || classNum > 12) {
                    classNum = 0;
                    // Try looking up by player name
                    org.bukkit.OfflinePlayer targetOp = org.bukkit.Bukkit.getOfflinePlayer(targetArg);
                    if (targetOp.getUniqueId() != null) {
                        Player targetOnline = targetOp.getPlayer();
                        if (targetOnline != null) {
                            StudentProfile targetProfile = plugin.getProfileManager().getProfile(targetOnline.getUniqueId());
                            if (targetProfile != null) {
                                classNum = targetProfile.getAcademicClass();
                            }
                        }
                        if (classNum == 0) {
                            try {
                                StudentProfile targetProfile = plugin.getDatabaseManager().loadProfile(targetOp.getUniqueId());
                                if (targetProfile != null) {
                                    classNum = targetProfile.getAcademicClass();
                                }
                            } catch (Exception ignored) {}
                        }
                    }
                }
            }

            if (classNum < 1 || classNum > 12) {
                player.sendMessage(MiniMessage.miniMessage().deserialize("<red>Kelas atau Player tidak ditemukan!</red>"));
                return true;
            }

            displayClassroomInfo(player, classNum);
            return true;
        } else if (subCommand.equals("teacher")) {
            if (args.length >= 2 && args[1].equalsIgnoreCase("salary")) {
                if (args.length >= 3 && args[2].equalsIgnoreCase("claim")) {
                    id.naturalsmp.naturalSchool.teacher.TeacherManager.ClaimResult res = plugin.getTeacherManager().claimSalary(player);
                    if (res.isSuccess()) {
                        player.sendMessage(MiniMessage.miniMessage().deserialize("<green>" + res.getMessage() + "</green>"));
                    } else {
                        player.sendMessage(MiniMessage.miniMessage().deserialize("<red>" + res.getMessage() + "</red>"));
                    }
                    return true;
                }
                plugin.getUiManager().getTeacherSalaryGui().openSalaryGui(player);
                return true;
            }
            player.sendMessage(MiniMessage.miniMessage().deserialize("<red>Gunakan: /school teacher salary [claim]</red>"));
            return true;
        } else if (subCommand.equals("student")) {
            if (args.length >= 2 && args[1].equalsIgnoreCase("violation")) {
                id.naturalsmp.naturalSchool.profile.SchoolRank rank = profile.getRank();
                boolean isStaffBK = rank == id.naturalsmp.naturalSchool.profile.SchoolRank.GURU_BK || 
                                    rank == id.naturalsmp.naturalSchool.profile.SchoolRank.KOMISI_DISIPLIN || 
                                    player.hasPermission("naturalschool.admin") || 
                                    player.isOp() || 
                                    rank.getPriority() >= 50;
                if (!isStaffBK) {
                    player.sendMessage(MiniMessage.miniMessage().deserialize("<red>Hanya Staf BK, Komisi Disiplin, atau Guru yang dapat menggunakan command ini!</red>"));
                    return true;
                }
                plugin.getUiManager().getBkGui().openSearchGui(player);
                return true;
            }
            player.sendMessage(MiniMessage.miniMessage().deserialize("<red>Gunakan: /school student violation</red>"));
            return true;
        }


        player.sendMessage(MiniMessage.miniMessage().deserialize("<red>Subcommand tidak dikenal. Gunakan /school help untuk bantuan.</red>"));
        return true;
    }

    public void displayClassroomInfo(CommandSender sender, int classNum) {
        id.naturalsmp.naturalSchool.classes.ClassroomManager.ClassroomData data = plugin.getClassroomManager().getClassroom(classNum);
        String className = plugin.getRankPrefixConfig().getClassPrefix(classNum);
        if (className == null || className.isEmpty()) {
            className = "Kelas " + classNum;
        }

        String waliKelasName = "Tidak Ada";
        if (data.getWaliKelasUuid() != null) {
            org.bukkit.OfflinePlayer op = org.bukkit.Bukkit.getOfflinePlayer(data.getWaliKelasUuid());
            if (op.getName() != null) {
                waliKelasName = op.getName();
            }
        }

        Map<java.util.UUID, id.naturalsmp.naturalSchool.classes.ClassroomManager.OfficerInfo> officers = data.getOfficers();
        String ketua = "Tidak Ada";
        String wakil = "Tidak Ada";
        String sekretaris = "Tidak Ada";
        String bendahara = "Tidak Ada";

        for (Map.Entry<java.util.UUID, id.naturalsmp.naturalSchool.classes.ClassroomManager.OfficerInfo> entry : officers.entrySet()) {
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

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player)) {
            return Collections.emptyList();
        }

        Player player = (Player) sender;
        StudentProfile profile = plugin.getProfileManager().getProfile(player.getUniqueId());
        if (profile == null || profile.getNis() == null) {
            return Collections.emptyList();
        }

        if (args.length == 1) {
            return Arrays.asList("info", "class", "exam", "semester", "teacher", "student", "help").stream()
                    .filter(s -> s.toLowerCase().startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("class")) {
            List<String> suggestions = new java.util.ArrayList<>();
            for (int i = 1; i <= 12; i++) {
                suggestions.add("kelas" + i);
            }
            for (Player p : Bukkit.getOnlinePlayers()) {
                suggestions.add(p.getName());
            }
            return suggestions.stream()
                    .filter(s -> s.toLowerCase().startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("teacher")) {
            return Collections.singletonList("salary").stream()
                    .filter(s -> s.toLowerCase().startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("teacher") && args[1].equalsIgnoreCase("salary")) {
            return Collections.singletonList("claim").stream()
                    .filter(s -> s.toLowerCase().startsWith(args[2].toLowerCase()))
                    .collect(Collectors.toList());
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("student")) {
            return Collections.singletonList("violation").stream()
                    .filter(s -> s.toLowerCase().startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
        }

        return Collections.emptyList();
    }
}
