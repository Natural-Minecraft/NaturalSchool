package id.naturalsmp.naturalSchool.ui;

import id.naturalsmp.naturalSchool.NaturalSchool;
import id.naturalsmp.naturalSchool.profile.StudentProfile;
import id.naturalsmp.naturalSchool.profile.SchoolRank;
import id.naturalsmp.naturalSchool.ui.factory.BedrockFormFactory;
import id.naturalsmp.naturalSchool.ui.factory.JavaDialogFactory;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.geysermc.floodgate.api.FloodgateApi;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class UIManager {

    private final NaturalSchool plugin;
    private final JavaDialogFactory javaDialogFactory;
    private final BedrockFormFactory bedrockFormFactory;
    private final boolean floodgateEnabled;
    private final Set<UUID> frozenPlayers = ConcurrentHashMap.newKeySet();

    public UIManager(NaturalSchool plugin) {
        this.plugin = plugin;
        this.javaDialogFactory = new JavaDialogFactory(plugin);
        this.bedrockFormFactory = new BedrockFormFactory(plugin);
        this.floodgateEnabled = Bukkit.getPluginManager().isPluginEnabled("floodgate");
    }

    /**
     * Freeze player movement and start the onboarding flow.
     */
    public void startOnboarding(Player player) {
        freezePlayer(player);
        openStep1(player);
    }

    public void freezePlayer(Player player) {
        frozenPlayers.add(player.getUniqueId());
    }

    public void unfreezePlayer(Player player) {
        frozenPlayers.remove(player.getUniqueId());
    }

    public boolean isFrozen(UUID uuid) {
        return frozenPlayers.contains(uuid);
    }

    public void openStep1(Player player) {
        if (isBedrockPlayer(player)) {
            bedrockFormFactory.openStep1(player);
        } else {
            javaDialogFactory.openStep1(player);
        }
    }

    public void openStep2(Player player) {
        if (isBedrockPlayer(player)) {
            bedrockFormFactory.openStep2(player);
        } else {
            javaDialogFactory.openStep2(player);
        }
    }

    public void openStep3(Player player) {
        if (isBedrockPlayer(player)) {
            bedrockFormFactory.openStep3(player);
        } else {
            javaDialogFactory.openStep3(player);
        }
    }

    public void completeRegistration(Player player) {
        UUID uuid = player.getUniqueId();
        StudentProfile profile = plugin.getProfileManager().getProfile(uuid);
        if (profile == null) {
            player.kick(net.kyori.adventure.text.minimessage.MiniMessage.miniMessage()
                .deserialize("<red>Failed to load profile. Please reconnect!</red>"));
            return;
        }

        player.sendMessage(net.kyori.adventure.text.minimessage.MiniMessage.miniMessage()
            .deserialize("<yellow>Registering your profile...</yellow>"));

        CompletableFuture.supplyAsync(() -> {
            try {
                return plugin.getDatabaseManager().getRegisteredNisCount();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }, runnable -> Bukkit.getScheduler().runTaskAsynchronously(plugin, runnable))
        .thenAccept(count -> {
            java.time.LocalDate now = java.time.LocalDate.now();
            java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("ddMMyy");
            String dateStr = now.format(formatter);
            int sequence = count + 1;
            String generatedNis = "1" + String.format("%03d", sequence) + dateStr;

            profile.setNis(generatedNis);
            profile.setAcademicStage("SD");
            profile.setAcademicClass(1);
            profile.setRank(SchoolRank.SD_1);

            plugin.getProfileManager().saveProfileAsync(profile).thenRun(() -> {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    unfreezePlayer(player);
                    player.sendMessage(net.kyori.adventure.text.minimessage.MiniMessage.miniMessage()
                        .deserialize("<green>Registrasi Berhasil! Selamat datang di NaturalSchool! NIS Anda: " + generatedNis + " (SD Kelas 1).</green>"));
                });
            }).exceptionally(ex -> {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    player.sendMessage(net.kyori.adventure.text.minimessage.MiniMessage.miniMessage()
                        .deserialize("<red>Gagal menyimpan profile: " + ex.getMessage() + "</red>"));
                });
                return null;
            });
        }).exceptionally(ex -> {
            Bukkit.getScheduler().runTask(plugin, () -> {
                player.sendMessage(net.kyori.adventure.text.minimessage.MiniMessage.miniMessage()
                    .deserialize("<red>Gagal mengambil nomor urut NIS: " + ex.getMessage() + "</red>"));
            });
            return null;
        });
    }

    /**
     * Opens a specific school menu for the given player, routing based on connection type.
     *
     * @param player   the player opening the menu
     * @param menuType the type of menu to open
     */
    public void openMenu(Player player, SchoolMenuType menuType) {
        if (player == null || menuType == null) {
            return;
        }

        if (menuType == SchoolMenuType.REGISTRATION) {
            openStep1(player);
            return;
        }

        if (isBedrockPlayer(player)) {
            bedrockFormFactory.openForm(player, menuType);
        } else {
            javaDialogFactory.openDialog(player, menuType);
        }
    }

    /**
     * Check if the player is a Bedrock Edition player via Floodgate API.
     *
     * @param player the player to check
     * @return true if the player is connected via Floodgate, false otherwise
     */
    private boolean isBedrockPlayer(Player player) {
        if (!floodgateEnabled) {
            return false;
        }
        try {
            return FloodgateApi.getInstance().isFloodgatePlayer(player.getUniqueId());
        } catch (Throwable t) {
            return false;
        }
    }
}
