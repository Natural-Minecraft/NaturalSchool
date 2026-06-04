package id.naturalsmp.naturalSchool.profile;

import id.naturalsmp.naturalSchool.NaturalSchool;
import id.naturalsmp.naturalSchool.database.DatabaseManager;

import java.sql.Timestamp;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ProfileManager {

    private final NaturalSchool plugin;
    private final DatabaseManager databaseManager;
    private final Map<UUID, StudentProfile> profileCache;

    public ProfileManager(NaturalSchool plugin, DatabaseManager databaseManager) {
        this.plugin = plugin;
        this.databaseManager = databaseManager;
        this.profileCache = new ConcurrentHashMap<>();
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
     *
     * @param uuid Player UUID
     */
    public void loadProfile(UUID uuid) {
        if (uuid == null) return;

        StudentProfile profile = databaseManager.loadProfile(uuid);
        if (profile == null) {
            int startClass = plugin.getConfig().getInt("academic-settings.default-start-class", 1);
            String startStage = plugin.getConfig().getString("academic-settings.default-start-stage", "SD");
            Timestamp now = new Timestamp(System.currentTimeMillis());

            profile = new StudentProfile(uuid, null, startStage, startClass, false, 0, now, SchoolRank.NONE);
            databaseManager.saveProfile(profile);
        }
        
        profileCache.put(uuid, profile);
    }

    /**
     * Saves the cached StudentProfile for a player to the database.
     * This method contains database calls and must be executed asynchronously.
     *
     * @param uuid Player UUID
     */
    public void saveProfile(UUID uuid) {
        if (uuid == null) return;

        StudentProfile profile = profileCache.get(uuid);
        if (profile != null) {
            databaseManager.saveProfile(profile);
        }
    }

    /**
     * Directly saves a given StudentProfile to the database.
     * This method contains database calls and must be executed asynchronously.
     *
     * @param profile The StudentProfile object
     */
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
}
