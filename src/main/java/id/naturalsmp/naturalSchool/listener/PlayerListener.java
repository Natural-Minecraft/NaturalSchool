package id.naturalsmp.naturalSchool.listener;

import id.naturalsmp.naturalSchool.NaturalSchool;
import id.naturalsmp.naturalSchool.profile.ProfileManager;
import id.naturalsmp.naturalSchool.profile.StudentProfile;
import id.naturalsmp.naturalSchool.ui.SchoolMenuType;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;

import java.util.UUID;

public class PlayerListener implements Listener {

    private final NaturalSchool plugin;
    private final ProfileManager profileManager;

    public PlayerListener(NaturalSchool plugin, ProfileManager profileManager) {
        this.plugin = plugin;
        this.profileManager = profileManager;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        // Capture name on main thread before going async — Bukkit API is not thread-safe
        String playerName = player.getName();

        // Asynchronously load the student profile from the database to the cache
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            profileManager.loadProfile(uuid, playerName);

            StudentProfile profile = profileManager.getProfile(uuid);
            if (profile != null && profile.getNis() == null) {
                // Freezing and onboarding trigger must be synchronous
                Bukkit.getScheduler().runTask(plugin, () -> {
                    if (player.isOnline()) {
                        // Freeze player immediately
                        plugin.getUiManager().freezePlayer(player);

                        // Schedule 20-tick (1 second) delayed synchronous task to call openMenu
                        Bukkit.getScheduler().runTaskLater(plugin, () -> {
                            if (player.isOnline()) {
                                plugin.getUiManager().openMenu(player, SchoolMenuType.REGISTRATION);
                            }
                        }, 20L);
                    }
                });
            }
        });
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        plugin.getUiManager().unfreezePlayer(event.getPlayer());
        handleDisconnect(uuid);
    }

    // NOTE: No separate onPlayerKick handler — on Paper, PlayerKickEvent also fires
    // PlayerQuitEvent, so onPlayerQuit handles both cases to prevent double-save.

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        if (plugin.getUiManager().isFrozen(event.getPlayer().getUniqueId())) {
            // Check if player changed blocks (allows looking around, cancels walking/teleporting)
            if (event.getFrom().getX() != event.getTo().getX()
                || event.getFrom().getY() != event.getTo().getY()
                || event.getFrom().getZ() != event.getTo().getZ()) {
                event.setTo(event.getFrom());
            }
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (plugin.getUiManager().isFrozen(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        if (plugin.getUiManager().isFrozen(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        if (plugin.getUiManager().isFrozen(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        if (plugin.getUiManager().isFrozen(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerPickupItem(EntityPickupItemEvent event) {
        if (event.getEntity() instanceof Player player) {
            if (plugin.getUiManager().isFrozen(player.getUniqueId())) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onInventoryClick(org.bukkit.event.inventory.InventoryClickEvent event) {
        if (event.getView().title().equals(net.kyori.adventure.text.Component.text("Ujian: Pilihan Ganda"))) {
            event.setCancelled(true);
            
            if (!(event.getWhoClicked() instanceof Player player)) {
                return;
            }
            
            int slot = event.getRawSlot();
            if (slot == 28 || slot == 30 || slot == 34) {
                // Incorrect Choice
                player.sendTitle("§c§lJAWABAN SALAH", "§7Coba belajar lagi ya!", 10, 70, 20);
                player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                player.closeInventory();
            } else if (slot == 32) {
                // Correct Choice (C)
                player.sendTitle("§a§lJAWABAN BENAR", "§7Selamat, kamu lulus!", 10, 70, 20);
                player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
                player.closeInventory();
            }
        }
    }

    private void handleDisconnect(UUID uuid) {
        StudentProfile profile = profileManager.getProfile(uuid);
        if (profile != null) {
            profileManager.removeProfile(uuid);
            profileManager.saveProfileAsync(profile);
        }
    }
}
