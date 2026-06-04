package id.naturalsmp.naturalSchool.listener;

import id.naturalsmp.naturalSchool.NaturalSchool;
import id.naturalsmp.naturalSchool.profile.ProfileManager;
import id.naturalsmp.naturalSchool.profile.StudentProfile;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerKickEvent;

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
        UUID uuid = event.getPlayer().getUniqueId();
        
        // Asynchronously load the student profile from the database to the cache
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            profileManager.loadProfile(uuid);
        });
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        handleDisconnect(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onPlayerKick(PlayerKickEvent event) {
        handleDisconnect(event.getPlayer().getUniqueId());
    }

    /**
     * Handles student profile saving and cache clearing strictly on disconnect.
     * Removes profile from cache immediately to prevent memory leaks, then saves asynchronously.
     */
    private void handleDisconnect(UUID uuid) {
        StudentProfile profile = profileManager.getProfile(uuid);
        if (profile != null) {
            // Strictly clear the cache entry first to avoid RAM bloat
            profileManager.removeProfile(uuid);
            
            // Asynchronously save the cached data back to the database
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                profileManager.saveProfile(profile);
            });
        }
    }
}
