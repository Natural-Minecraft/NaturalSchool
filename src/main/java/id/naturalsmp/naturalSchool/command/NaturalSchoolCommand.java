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

    private boolean checkPermission(CommandSender sender) {
        if (sender.hasPermission("naturalschool.admin")) {
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

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!checkPermission(sender)) {
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
            case "setrank":
                handleSetRank(sender, args);
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
            "<yellow>/naturalschool setrank <player> <rank></yellow> - <gray>Set player internal school rank.</gray>\n" +
            "<yellow>/naturalschool setclass <player> <1-12></yellow> - <gray>Set student academic class.</gray>\n" +
            "<yellow>/naturalschool setstage <player> <SD|SMP|SMA></yellow> - <gray>Set student academic stage.</gray>\n" +
            "<yellow>/naturalschool nis help</yellow> - <gray>View NIS Management System help.</gray>\n" +
            "<yellow>/naturalschool gui <welcome|exam1|exam2|exam3|exam4|exam5|version> [player]</yellow> - <gray>Manually trigger school GUI dialogs or view GUI version.</gray>\n" +
            "<yellow>/naturalschool semester <info|end></yellow> - <gray>Manage and rotate active school semesters.</gray>\n" +
            "<yellow>/naturalschool exam <open|close|message|sync> [msg]</yellow> - <gray>Manage exam portal status, messages, and synchronization.</gray>"
        ));
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

    private void handleSetRank(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>Usage: /naturalschool setrank <player> <RANK_ID></red>"));
            return;
        }

        String targetName = args[1];
        String rawRank = args[2].toUpperCase();
        SchoolRank newRank;
        try {
            newRank = SchoolRank.valueOf(rawRank);
        } catch (IllegalArgumentException e) {
            sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>Invalid rank ID. Use tab completion to see valid ranks.</red>"));
            return;
        }

        Player targetPlayer = Bukkit.getPlayerExact(targetName);
        if (targetPlayer != null) {
            StudentProfile profile = plugin.getProfileManager().getProfile(targetPlayer.getUniqueId());
            if (profile != null) {
                SchoolRank oldRank = profile.getRank();
                if (oldRank == newRank) {
                    sender.sendMessage(MiniMessage.miniMessage().deserialize("<yellow>" + targetPlayer.getName() + " is already " + newRank.getDisplayName() + ".</yellow>"));
                    return;
                }

                plugin.getNaturalSchoolAPI().setPlayerRank(targetPlayer.getUniqueId(), newRank);

                if (profile.getRank() != newRank) {
                    sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>Rank change for " + targetPlayer.getName() + " was cancelled by another plugin.</red>"));
                } else {
                    sender.sendMessage(MiniMessage.miniMessage().deserialize("<green>Set rank to </green>" + newRank.getDisplayName() + "<green> for " + targetPlayer.getName() + ".</green>"));
                }
            } else {
                sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>Profile cache not loaded for " + targetPlayer.getName() + ".</red>"));
            }
        } else {
            sender.sendMessage(MiniMessage.miniMessage().deserialize("<yellow>Player is offline. Updating rank in database...</yellow>"));
            OfflinePlayer offlineTarget = Bukkit.getOfflinePlayer(targetName);
            UUID uuid = offlineTarget.getUniqueId();

            plugin.getNaturalSchoolAPI().getOfflineProfile(uuid).thenAccept(optProfile -> {
                if (optProfile.isPresent()) {
                    plugin.getNaturalSchoolAPI().setPlayerRank(uuid, newRank);
                    sender.sendMessage(MiniMessage.miniMessage().deserialize("<green>Set rank to </green>" + newRank.getDisplayName() + "<green> for offline player " + (offlineTarget.getName() != null ? offlineTarget.getName() : targetName) + ".</green>"));
                } else {
                    sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>Player profile not found in database.</red>"));
                }
            }).exceptionally(ex -> {
                sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>Failed to update offline player rank: " + ex.getMessage() + "</red>"));
                return null;
            });
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
        if (!sender.hasPermission("naturalschool.admin")) {
            sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>No Permission!</red>"));
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
        if (!checkPermission(sender)) {
            return Collections.emptyList();
        }

        if (args.length == 1) {
            List<String> subCommands = Arrays.asList("reload", "info", "setrank", "setclass", "setstage", "nis", "gui", "semester", "exam");
            return filterList(subCommands, args[0]);
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
            } else if (Arrays.asList("info", "setrank", "setclass", "setstage").contains(subCommand)) {
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
            } else if ("setrank".equals(subCommand)) {
                List<String> ranks = Arrays.stream(SchoolRank.values())
                        .map(SchoolRank::name)
                        .collect(Collectors.toList());
                return filterList(ranks, args[2]);
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
