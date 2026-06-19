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
                "<yellow>/school exam</yellow> - <gray>Membuka Portal Ujian sekolah.</gray>"
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
            return Arrays.asList("info", "class", "exam", "help").stream()
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

        return Collections.emptyList();
    }
}
