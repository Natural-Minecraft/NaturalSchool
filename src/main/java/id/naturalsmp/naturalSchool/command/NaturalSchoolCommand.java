package id.naturalsmp.naturalSchool.command;

import id.naturalsmp.naturalSchool.NaturalSchool;
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class NaturalSchoolCommand implements CommandExecutor, TabCompleter {

    private final NaturalSchool plugin;

    public NaturalSchoolCommand(NaturalSchool plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("naturalschool.admin")) {
            sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>No Permission!</red>"));
            return true;
        }

        if (args.length == 0) {
            sendHelpMessage(sender);
            return true;
        }

        String subCommand = args[0].toLowerCase();
        switch (subCommand) {
            case "reload":
                handleReload(sender);
                break;
            case "info":
                handleInfo(sender, args);
                break;
            case "setclass":
                handleSetClass(sender, args);
                break;
            case "setstage":
                handleSetStage(sender, args);
                break;
            case "setpractical":
                handleSetPractical(sender, args);
                break;
            default:
                sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>Unknown subcommand. Use /naturalschool for help.</red>"));
                break;
        }

        return true;
    }

    private void sendHelpMessage(CommandSender sender) {
        sender.sendMessage(MiniMessage.miniMessage().deserialize(
            "<gold>=== NaturalSchool Administrative Commands ===</gold>\n" +
            "<yellow>/naturalschool reload</yellow> - <gray>Reload configuration and database connections.</gray>\n" +
            "<yellow>/naturalschool info <player></yellow> - <gray>View player academic profile details.</gray>\n" +
            "<yellow>/naturalschool setclass <player> <1-12></yellow> - <gray>Set student academic class.</gray>\n" +
            "<yellow>/naturalschool setstage <player> <SD|SMP|SMA></yellow> - <gray>Set student academic stage.</gray>\n" +
            "<yellow>/naturalschool setpractical <player> <true|false></yellow> - <gray>Set practical exam status.</gray>"
        ));
    }

    private void handleReload(CommandSender sender) {
        sender.sendMessage(MiniMessage.miniMessage().deserialize("<yellow>Reloading configuration...</yellow>"));
        plugin.reloadConfig();
        plugin.getDatabaseManager().reload();
        sender.sendMessage(MiniMessage.miniMessage().deserialize("<green>NaturalSchool configuration and database pool reloaded successfully.</green>"));
    }

    private void handleInfo(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>Usage: /naturalschool info <player></red>"));
            return;
        }

        String targetName = args[1];
        Player targetPlayer = Bukkit.getPlayerExact(targetName);

        if (targetPlayer != null) {
            // Online player: fetch directly from active cache
            StudentProfile profile = plugin.getProfileManager().getProfile(targetPlayer.getUniqueId());
            if (profile != null) {
                displayProfile(sender, targetPlayer.getName(), profile, true);
            } else {
                sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>Profile cache not loaded for online player " + targetPlayer.getName() + ".</red>"));
            }
        } else {
            // Offline player: fetch asynchronously from database
            sender.sendMessage(MiniMessage.miniMessage().deserialize("<yellow>Player is offline. Fetching profile from database...</yellow>"));
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                OfflinePlayer offlineTarget = Bukkit.getOfflinePlayer(targetName);
                UUID uuid = offlineTarget.getUniqueId();
                StudentProfile profile = plugin.getDatabaseManager().loadProfile(uuid);

                if (profile != null) {
                    displayProfile(sender, offlineTarget.getName() != null ? offlineTarget.getName() : targetName, profile, false);
                } else {
                    sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>Player profile not found in cache or database.</red>"));
                }
            });
        }
    }

    private void displayProfile(CommandSender sender, String targetName, StudentProfile profile, boolean isOnline) {
        String status = isOnline ? "<green>ONLINE</green>" : "<gray>OFFLINE</gray>";
        sender.sendMessage(MiniMessage.miniMessage().deserialize(
            "<gold>=== Student Profile: " + targetName + " (" + status + ") ===</gold>\n" +
            "<yellow>NIS:</yellow> <white>" + (profile.getNis() != null ? profile.getNis() : "Not Assigned") + "</white>\n" +
            "<yellow>Academic Stage:</yellow> <white>" + profile.getAcademicStage() + "</white>\n" +
            "<yellow>Academic Class:</yellow> <white>" + profile.getAcademicClass() + "</white>\n" +
            "<yellow>Practical Status:</yellow> " + (profile.isPracticalPassed() ? "<green>Passed</green>" : "<red>Failed/Pending</red>") + "\n" +
            "<yellow>Temporary Grade:</yellow> <white>" + profile.getTemporaryGrade() + "</white>\n" +
            "<yellow>Last Updated:</yellow> <gray>" + profile.getLastUpdated() + "</gray>"
        ));
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
            // Online player: Update cache, and save to DB asynchronously
            StudentProfile profile = plugin.getProfileManager().getProfile(targetPlayer.getUniqueId());
            if (profile != null) {
                profile.setAcademicClass(academicClass);
                Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> plugin.getProfileManager().saveProfile(profile));
                sender.sendMessage(MiniMessage.miniMessage().deserialize("<green>Set class to " + academicClass + " for " + targetPlayer.getName() + ".</green>"));
            } else {
                sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>Profile cache not loaded for " + targetPlayer.getName() + ".</red>"));
            }
        } else {
            // Offline player: Load from DB, modify, and save back asynchronously
            sender.sendMessage(MiniMessage.miniMessage().deserialize("<yellow>Player is offline. Updating profile in database...</yellow>"));
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                OfflinePlayer offlineTarget = Bukkit.getOfflinePlayer(targetName);
                UUID uuid = offlineTarget.getUniqueId();
                StudentProfile profile = plugin.getDatabaseManager().loadProfile(uuid);

                if (profile != null) {
                    profile.setAcademicClass(academicClass);
                    plugin.getDatabaseManager().saveProfile(profile);
                    sender.sendMessage(MiniMessage.miniMessage().deserialize("<green>Set class to " + academicClass + " for offline player " + (offlineTarget.getName() != null ? offlineTarget.getName() : targetName) + ".</green>"));
                } else {
                    sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>Player profile not found in database.</red>"));
                }
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
            // Online player: Update cache, and save to DB asynchronously
            StudentProfile profile = plugin.getProfileManager().getProfile(targetPlayer.getUniqueId());
            if (profile != null) {
                profile.setAcademicStage(stage);
                Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> plugin.getProfileManager().saveProfile(profile));
                sender.sendMessage(MiniMessage.miniMessage().deserialize("<green>Set academic stage to " + stage + " for " + targetPlayer.getName() + ".</green>"));
            } else {
                sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>Profile cache not loaded for " + targetPlayer.getName() + ".</red>"));
            }
        } else {
            // Offline player: Load from DB, modify, and save back asynchronously
            sender.sendMessage(MiniMessage.miniMessage().deserialize("<yellow>Player is offline. Updating profile in database...</yellow>"));
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                OfflinePlayer offlineTarget = Bukkit.getOfflinePlayer(targetName);
                UUID uuid = offlineTarget.getUniqueId();
                StudentProfile profile = plugin.getDatabaseManager().loadProfile(uuid);

                if (profile != null) {
                    profile.setAcademicStage(stage);
                    plugin.getDatabaseManager().saveProfile(profile);
                    sender.sendMessage(MiniMessage.miniMessage().deserialize("<green>Set academic stage to " + stage + " for offline player " + (offlineTarget.getName() != null ? offlineTarget.getName() : targetName) + ".</green>"));
                } else {
                    sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>Player profile not found in database.</red>"));
                }
            });
        }
    }

    private void handleSetPractical(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>Usage: /naturalschool setpractical <player> <true|false></red>"));
            return;
        }

        String targetName = args[1];
        String rawPassed = args[2].toLowerCase();
        if (!rawPassed.equals("true") && !rawPassed.equals("false")) {
            sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>Practical status must be true or false.</red>"));
            return;
        }
        boolean passed = Boolean.parseBoolean(rawPassed);

        Player targetPlayer = Bukkit.getPlayerExact(targetName);
        if (targetPlayer != null) {
            // Online player: Update cache, and save to DB asynchronously
            StudentProfile profile = plugin.getProfileManager().getProfile(targetPlayer.getUniqueId());
            if (profile != null) {
                profile.setPracticalPassed(passed);
                Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> plugin.getProfileManager().saveProfile(profile));
                sender.sendMessage(MiniMessage.miniMessage().deserialize("<green>Set practical status to " + passed + " for " + targetPlayer.getName() + ".</green>"));
            } else {
                sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>Profile cache not loaded for " + targetPlayer.getName() + ".</red>"));
            }
        } else {
            // Offline player: Load from DB, modify, and save back asynchronously
            sender.sendMessage(MiniMessage.miniMessage().deserialize("<yellow>Player is offline. Updating profile in database...</yellow>"));
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                OfflinePlayer offlineTarget = Bukkit.getOfflinePlayer(targetName);
                UUID uuid = offlineTarget.getUniqueId();
                StudentProfile profile = plugin.getDatabaseManager().loadProfile(uuid);

                if (profile != null) {
                    profile.setPracticalPassed(passed);
                    plugin.getDatabaseManager().saveProfile(profile);
                    sender.sendMessage(MiniMessage.miniMessage().deserialize("<green>Set practical status to " + passed + " for offline player " + (offlineTarget.getName() != null ? offlineTarget.getName() : targetName) + ".</green>"));
                } else {
                    sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>Player profile not found in database.</red>"));
                }
            });
        }
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("naturalschool.admin")) {
            return Collections.emptyList();
        }

        if (args.length == 1) {
            List<String> subCommands = Arrays.asList("reload", "info", "setclass", "setstage", "setpractical");
            return filterList(subCommands, args[0]);
        }

        if (args.length == 2) {
            String subCommand = args[0].toLowerCase();
            if (Arrays.asList("info", "setclass", "setstage", "setpractical").contains(subCommand)) {
                List<String> players = Bukkit.getOnlinePlayers().stream()
                        .map(Player::getName)
                        .collect(Collectors.toList());
                return filterList(players, args[1]);
            }
        }

        if (args.length == 3) {
            String subCommand = args[0].toLowerCase();
            if ("setclass".equals(subCommand)) {
                List<String> classes = new ArrayList<>();
                for (int i = 1; i <= 12; i++) {
                    classes.add(String.valueOf(i));
                }
                return filterList(classes, args[2]);
            } else if ("setstage".equals(subCommand)) {
                return filterList(Arrays.asList("SD", "SMP", "SMA"), args[2]);
            } else if ("setpractical".equals(subCommand)) {
                return filterList(Arrays.asList("true", "false"), args[2]);
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
}
