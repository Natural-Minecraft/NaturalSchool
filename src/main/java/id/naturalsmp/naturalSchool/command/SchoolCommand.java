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
                "<yellow>/school testexam</yellow> - <gray>Membuka visual prototype dialog Ujian sekolah.</gray>"
            ));
            return true;
        }

        String subCommand = args[0].toLowerCase();
        if (subCommand.equals("info")) {
            plugin.getUiManager().openMenu(player, SchoolMenuType.PROFILE);
            return true;
        } else if (subCommand.equals("testexam")) {
            plugin.getUiManager().openTestExam(player);
            return true;
        }

        player.sendMessage(MiniMessage.miniMessage().deserialize("<red>Subcommand tidak dikenal. Gunakan /school help untuk bantuan.</red>"));
        return true;
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
            return Arrays.asList("info", "testexam", "help").stream()
                    .filter(s -> s.toLowerCase().startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }

        return Collections.emptyList();
    }
}
