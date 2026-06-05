package id.naturalsmp.naturalSchool.profile;

import id.naturalsmp.naturalSchool.NaturalSchool;
import id.naturalsmp.naturalSchool.database.DatabaseManager;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class ProfileManager {

    private final NaturalSchool plugin;
    private final DatabaseManager databaseManager;
    private final Map<UUID, StudentProfile> profileCache;
    private final Map<UUID, CompletableFuture<Void>> pendingSaves;

    public ProfileManager(NaturalSchool plugin, DatabaseManager databaseManager) {
        this.plugin = plugin;
        this.databaseManager = databaseManager;
        this.profileCache = new ConcurrentHashMap<>();
        this.pendingSaves = new ConcurrentHashMap<>();
    }

    /**
     * Safe retrieval of a StudentProfile from cache.
     *
     * @param uuid Player UUID
     * @return Cached StudentProfile, or null if not cached
     */
    public StudentProfile getProfile(UUID uuid) {
        if (uuid == null) return null;
        return profileCache.get(uuid);
    }

    /**
     * Loads a profile from the database. If it doesn't exist, creates a default profile
     * and saves it to the database. Finally, stores the profile in the runtime cache.
     * This method contains database calls and must be executed asynchronously.
     * The username must be obtained on the main thread before calling this.
     *
     * @param uuid     Player UUID
     * @param username Player's current in-game name (captured on main thread before async call)
     */
    public void loadProfile(UUID uuid, String username) {
        if (uuid == null) return;

        CompletableFuture<Void> pendingSave = pendingSaves.get(uuid);
        if (pendingSave != null) {
            try {
                pendingSave.join();
            } catch (Exception ignored) {
            }
        }

        try {
            StudentProfile profile = databaseManager.loadProfile(uuid);
            if (profile == null) {
                Timestamp now = new Timestamp(System.currentTimeMillis());
                // username is safely passed in from the main thread — no Bukkit.getPlayer() call here
                profile = new StudentProfile(uuid, username, null, "NONE", 0, now, SchoolRank.NONE);
                databaseManager.saveProfile(profile);
            }
            profileCache.put(uuid, profile);
        } catch (SQLException e) {
            plugin.getLogger().log(java.util.logging.Level.SEVERE, "Database error loading profile for UUID: " + uuid + ". Player will be kicked.", e);
            Bukkit.getScheduler().runTask(plugin, () -> {
                Player player = Bukkit.getPlayer(uuid);
                if (player != null && player.isOnline()) {
                    player.kick(MiniMessage.miniMessage().deserialize("<red>Failed to load your academic profile due to database error. Please try reconnecting.</red>"));
                }
            });
        }
    }

    public CompletableFuture<Void> saveProfileAsync(StudentProfile profile) {
        if (profile == null) return CompletableFuture.completedFuture(null);
        UUID uuid = profile.getUuid();

        CompletableFuture<Void> saveFuture = new CompletableFuture<>();
        pendingSaves.put(uuid, saveFuture);

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                databaseManager.saveProfile(profile);
                saveFuture.complete(null);
            } catch (Throwable t) {
                saveFuture.completeExceptionally(t);
            } finally {
                pendingSaves.remove(uuid, saveFuture);
            }
        });
        return saveFuture;
    }

    public void saveProfile(UUID uuid) {
        if (uuid == null) return;

        StudentProfile profile = profileCache.get(uuid);
        if (profile != null) {
            databaseManager.saveProfile(profile);
        }
    }

    public void saveProfile(StudentProfile profile) {
        if (profile != null) {
            databaseManager.saveProfile(profile);
        }
    }

    /**
     * Strictly removes the StudentProfile from the runtime cache.
     *
     * @param uuid Player UUID
     */
    public void removeProfile(UUID uuid) {
        if (uuid == null) return;
        profileCache.remove(uuid);
    }

    /**
     * Gets a copy of the active cache map (primarily for shutdown flushing).
     *
     * @return Cache map
     */
    public Map<UUID, StudentProfile> getProfileCache() {
        return profileCache;
    }
    /**
     * Generates a unique NIS string based on the current registered count.
     * Format: 1 + 3-digit sequence + DDMMYY date
     *
     * @param registeredCount current count of registered NIS entries
     * @return generated NIS string
     */
    public static String generateNis(int registeredCount) {
        java.time.LocalDate now = java.time.LocalDate.now();
        java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("ddMMyy");
        String dateStr = now.format(formatter);
        int sequence = registeredCount + 1;
        return "1" + String.format("%03d", sequence) + dateStr;
    }
}
