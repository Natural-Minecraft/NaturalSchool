package id.naturalsmp.naturalSchool.api;

import id.naturalsmp.naturalSchool.profile.SchoolRank;
import id.naturalsmp.naturalSchool.profile.StudentProfile;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface NaturalSchoolAPI {

    /**
     * Retrieves the cached StudentProfile for an online player.
     * This method runs in O(1) time and is safe to call on the main thread.
     *
     * @param uuid Player UUID
     * @return Optional containing the StudentProfile if online, or empty if offline
     */
    Optional<StudentProfile> getOnlineProfile(UUID uuid);

    /**
     * Fetches the StudentProfile for an offline player from the database.
     * This method runs asynchronously to prevent blocking the main server thread.
     *
     * @param uuid Player UUID
     * @return CompletableFuture containing Optional with StudentProfile if found
     */
    CompletableFuture<Optional<StudentProfile>> getOfflineProfile(UUID uuid);

    /**
     * Gets the current SchoolRank of a player.
     * If the player is offline/uncached, returns SchoolRank.NONE.
     *
     * @param uuid Player UUID
     * @return SchoolRank of the player
     */
    SchoolRank getPlayerRank(UUID uuid);

    /**
     * Sets the SchoolRank of a player.
     * If the player is online, it will fire StudentRankChangeEvent.
     * The profile is saved to the database asynchronously.
     *
     * @param uuid Player UUID
     * @param rank New SchoolRank
     */
    void setPlayerRank(UUID uuid, SchoolRank rank);

    /**
     * Gets the academic class level (1-12) of a player.
     * Returns 0 if uncached.
     *
     * @param uuid Player UUID
     * @return academic class level
     */
    int getPlayerClass(UUID uuid);

    /**
     * Sets the academic class level (1-12) of a player.
     * If the player is online, it will fire StudentClassChangeEvent.
     * The profile is saved to the database asynchronously.
     *
     * @param uuid          Player UUID
     * @param academicClass new academic class level (1-12)
     */
    void setPlayerClass(UUID uuid, int academicClass);

    /**
     * Gets the academic stage (SD, SMP, SMA) of a player.
     * Returns an empty string if uncached.
     *
     * @param uuid Player UUID
     * @return academic stage
     */
    String getPlayerStage(UUID uuid);

    /**
     * Checks if the player has passed the practical exam.
     * Returns false if uncached.
     *
     * @param uuid Player UUID
     * @return true if passed, false otherwise
     */
    boolean isPracticalPassed(UUID uuid);

    /**
     * Sets the practical exam status of a player.
     * If the player is online, it will fire StudentPracticalToggleEvent.
     * The profile is saved to the database asynchronously.
     *
     * @param uuid   Player UUID
     * @param passed new practical exam status
     */
    void setPracticalPassed(UUID uuid, boolean passed);
}
