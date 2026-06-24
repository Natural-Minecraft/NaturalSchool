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
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;

import java.util.UUID;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerListener implements Listener {

    private final NaturalSchool plugin;
    private final ProfileManager profileManager;
    private final Map<UUID, Integer> insideClassrooms = new ConcurrentHashMap<>();

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
            if (profile != null) {
                if (profile.getNis() == null) {
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

                // Check for unread mails
                int unread = plugin.getDatabaseManager().getUnreadMailCount(uuid);
                if (unread > 0) {
                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        if (player.isOnline()) {
                            id.naturalsmp.naturalSchool.util.ToastUtil.sendToast(
                                plugin,
                                player,
                                "Pesan Belum Dibaca",
                                "Ada " + unread + " surat masuk baru.",
                                "minecraft:paper",
                                "task"
                            );
                        }
                    }, 40L);
                }
            }
        });
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        plugin.getUiManager().unfreezePlayer(player);
        plugin.getUiManager().clearExamSession(player);
        insideClassrooms.remove(uuid);
        handleDisconnect(uuid);
    }

    @EventHandler
    public void onPlayerKick(PlayerKickEvent event) {
        Player player = event.getPlayer();
        plugin.getUiManager().clearExamSession(player);
        insideClassrooms.remove(player.getUniqueId());
    }

    // NOTE: No separate onPlayerKick handler — on Paper, PlayerKickEvent also fires
    // PlayerQuitEvent, so onPlayerQuit handles both cases to prevent double-save.

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (plugin.getUiManager().isFrozen(player.getUniqueId())) {
            // Check if player changed blocks (allows looking around, cancels walking/teleporting)
            if (event.getFrom().getX() != event.getTo().getX()
                || event.getFrom().getY() != event.getTo().getY()
                || event.getFrom().getZ() != event.getTo().getZ()) {
                event.setTo(event.getFrom());
            }
            return;
        }

        // Optimize block coordinate checks
        if (event.getFrom().getBlockX() == event.getTo().getBlockX()
            && event.getFrom().getBlockY() == event.getTo().getBlockY()
            && event.getFrom().getBlockZ() == event.getTo().getBlockZ()
            && event.getFrom().getWorld().equals(event.getTo().getWorld())) {
            return;
        }

        int newClassNum = 0;
        for (id.naturalsmp.naturalSchool.classes.ClassroomManager.ClassroomData data : plugin.getClassroomManager().getAllClassroomData()) {
            if (data.isInside(event.getTo())) {
                newClassNum = data.getIdKelas();
                break;
            }
        }

        int oldClassNum = insideClassrooms.getOrDefault(player.getUniqueId(), 0);
        if (newClassNum != oldClassNum) {
            if (oldClassNum != 0) {
                String oldClassName = plugin.getRankPrefixConfig().getClassPrefix(oldClassNum);
                if (oldClassName == null || oldClassName.isEmpty()) {
                    oldClassName = "Kelas " + oldClassNum;
                }
                String leaveMsg = "§c§l" + oldClassName.trim() + " §cMeninggalkan ruang kelas §e" + oldClassName.trim();
                player.sendMessage(net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacySection().deserialize(leaveMsg));
            }
            if (newClassNum != 0) {
                String newClassName = plugin.getRankPrefixConfig().getClassPrefix(newClassNum);
                if (newClassName == null || newClassName.isEmpty()) {
                    newClassName = "Kelas " + newClassNum;
                }
                String enterMsg = "§a§l" + newClassName.trim() + " §aMemasuki ruang kelas §e" + newClassName.trim();
                player.sendMessage(net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacySection().deserialize(enterMsg));
                insideClassrooms.put(player.getUniqueId(), newClassNum);
            } else {
                insideClassrooms.remove(player.getUniqueId());
            }
        }
    }

    @EventHandler
    public void onPlayerChat(org.bukkit.event.player.AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        if (plugin.getClassChatManager().isInClassChatChannel(player.getUniqueId())) {
            event.setCancelled(true);
            Bukkit.getScheduler().runTask(plugin, () -> {
                plugin.getClassChatManager().sendClassChat(player, event.getMessage());
            });
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

    private void handleDisconnect(UUID uuid) {
        StudentProfile profile = profileManager.getProfile(uuid);
        if (profile != null) {
            profileManager.removeProfile(uuid);
            profileManager.saveProfileAsync(profile);
        }
    }
}
