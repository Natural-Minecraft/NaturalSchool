package id.naturalsmp.naturalSchool.api;

import id.naturalsmp.naturalSchool.NaturalSchool;
import id.naturalsmp.naturalSchool.api.event.StudentClassChangeEvent;
import id.naturalsmp.naturalSchool.api.event.StudentPracticalToggleEvent;
import id.naturalsmp.naturalSchool.api.event.StudentRankChangeEvent;
import id.naturalsmp.naturalSchool.api.event.StudentStageChangeEvent;
import id.naturalsmp.naturalSchool.profile.SchoolRank;
import id.naturalsmp.naturalSchool.profile.StudentProfile;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class NaturalSchoolAPIImpl implements NaturalSchoolAPI {

    private final NaturalSchool plugin;

    public NaturalSchoolAPIImpl(NaturalSchool plugin) {
        this.plugin = plugin;
    }

    @Override
    public Optional<StudentProfile> getOnlineProfile(UUID uuid) {
        if (uuid == null) return Optional.empty();
        return Optional.ofNullable(plugin.getProfileManager().getProfile(uuid));
    }

    @Override
    public CompletableFuture<Optional<StudentProfile>> getOfflineProfile(UUID uuid) {
        if (uuid == null) {
            return CompletableFuture.completedFuture(Optional.empty());
        }
        CompletableFuture<Optional<StudentProfile>> future = new CompletableFuture<>();
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                StudentProfile profile = plugin.getDatabaseManager().loadProfile(uuid);
                future.complete(Optional.ofNullable(profile));
            } catch (Exception e) {
                future.completeExceptionally(e);
            }
        });
        return future;
    }

    @Override
    public SchoolRank getPlayerRank(UUID uuid) {
        if (uuid == null) return SchoolRank.NONE;
        StudentProfile profile = plugin.getProfileManager().getProfile(uuid);
        return profile != null ? profile.getRank() : SchoolRank.NONE;
    }

    @Override
    public void setPlayerRank(UUID uuid, SchoolRank rank) {
        if (uuid == null || rank == null) return;

        Player player = Bukkit.getPlayer(uuid);
        if (player != null) {
            // Online player: perform event check and cache update on the main thread
            if (!Bukkit.isPrimaryThread()) {
                Bukkit.getScheduler().runTask(plugin, () -> setPlayerRank(uuid, rank));
                return;
            }

            StudentProfile profile = plugin.getProfileManager().getProfile(uuid);
            if (profile != null) {
                SchoolRank oldRank = profile.getRank();
                if (oldRank == rank) return;

                StudentRankChangeEvent event = new StudentRankChangeEvent(player, oldRank, rank);
                Bukkit.getPluginManager().callEvent(event);
                if (event.isCancelled()) {
                    return;
                }

                profile.setRank(rank);
                plugin.getProfileManager().saveProfileAsync(profile);
            }
        } else {
            // Offline player: load, modify, and save to DB asynchronously
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                try {
                    StudentProfile profile = plugin.getDatabaseManager().loadProfile(uuid);
                    if (profile != null) {
                        profile.setRank(rank);
                        plugin.getDatabaseManager().saveProfile(profile);
                    }
                } catch (Exception e) {
                    plugin.getLogger().warning("Failed to update offline player rank: " + e.getMessage());
                }
            });
        }
    }

    @Override
    public int getPlayerClass(UUID uuid) {
        if (uuid == null) return 0;
        StudentProfile profile = plugin.getProfileManager().getProfile(uuid);
        return profile != null ? profile.getAcademicClass() : 0;
    }

    @Override
    public void setPlayerClass(UUID uuid, int academicClass) {
        if (uuid == null || academicClass < 1 || academicClass > 12) return;

        Player player = Bukkit.getPlayer(uuid);
        if (player != null) {
            // Online player: perform event check and cache update on the main thread
            if (!Bukkit.isPrimaryThread()) {
                Bukkit.getScheduler().runTask(plugin, () -> setPlayerClass(uuid, academicClass));
                return;
            }

            StudentProfile profile = plugin.getProfileManager().getProfile(uuid);
            if (profile != null) {
                int oldClass = profile.getAcademicClass();
                if (oldClass == academicClass) return;

                StudentClassChangeEvent event = new StudentClassChangeEvent(player, oldClass, academicClass);
                Bukkit.getPluginManager().callEvent(event);
                if (event.isCancelled()) {
                    return;
                }

                profile.setAcademicClass(academicClass);
                plugin.getProfileManager().saveProfileAsync(profile);
            }
        } else {
            // Offline player: load, modify, and save to DB asynchronously
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                try {
                    StudentProfile profile = plugin.getDatabaseManager().loadProfile(uuid);
                    if (profile != null) {
                        profile.setAcademicClass(academicClass);
                        plugin.getDatabaseManager().saveProfile(profile);
                    }
                } catch (Exception e) {
                    plugin.getLogger().warning("Failed to update offline player class: " + e.getMessage());
                }
            });
        }
    }

    @Override
    public String getPlayerStage(UUID uuid) {
        if (uuid == null) return "";
        StudentProfile profile = plugin.getProfileManager().getProfile(uuid);
        return profile != null && profile.getAcademicStage() != null ? profile.getAcademicStage() : "";
    }

    @Override
    public boolean isPracticalPassed(UUID uuid) {
        if (uuid == null) return false;
        StudentProfile profile = plugin.getProfileManager().getProfile(uuid);
        return profile != null && profile.isPracticalPassed();
    }

    @Override
    public void setPracticalPassed(UUID uuid, boolean passed) {
        if (uuid == null) return;

        Player player = Bukkit.getPlayer(uuid);
        if (player != null) {
            // Online player: perform event check and cache update on the main thread
            if (!Bukkit.isPrimaryThread()) {
                Bukkit.getScheduler().runTask(plugin, () -> setPracticalPassed(uuid, passed));
                return;
            }

            StudentProfile profile = plugin.getProfileManager().getProfile(uuid);
            if (profile != null) {
                if (profile.isPracticalPassed() == passed) return;

                StudentPracticalToggleEvent event = new StudentPracticalToggleEvent(player, passed);
                Bukkit.getPluginManager().callEvent(event);
                if (event.isCancelled()) {
                    return;
                }

                profile.setPracticalPassed(passed);
                plugin.getProfileManager().saveProfileAsync(profile);
            }
        } else {
            // Offline player: load, modify, and save to DB asynchronously
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                try {
                    StudentProfile profile = plugin.getDatabaseManager().loadProfile(uuid);
                    if (profile != null) {
                        profile.setPracticalPassed(passed);
                        plugin.getDatabaseManager().saveProfile(profile);
                    }
                } catch (Exception e) {
                    plugin.getLogger().warning("Failed to update offline player practical status: " + e.getMessage());
                }
            });
        }
    }

    @Override
    public void setPlayerStage(UUID uuid, String stage) {
        if (uuid == null || stage == null) return;

        Player player = Bukkit.getPlayer(uuid);
        if (player != null) {
            if (!Bukkit.isPrimaryThread()) {
                Bukkit.getScheduler().runTask(plugin, () -> setPlayerStage(uuid, stage));
                return;
            }

            StudentProfile profile = plugin.getProfileManager().getProfile(uuid);
            if (profile != null) {
                String oldStage = profile.getAcademicStage();
                if (stage.equals(oldStage)) return;

                StudentStageChangeEvent event = new StudentStageChangeEvent(player, oldStage != null ? oldStage : "", stage);
                Bukkit.getPluginManager().callEvent(event);
                if (event.isCancelled()) {
                    return;
                }

                profile.setAcademicStage(stage);
                plugin.getProfileManager().saveProfileAsync(profile);
            }
        } else {
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                try {
                    StudentProfile profile = plugin.getDatabaseManager().loadProfile(uuid);
                    if (profile != null) {
                        profile.setAcademicStage(stage);
                        plugin.getDatabaseManager().saveProfile(profile);
                    }
                } catch (Exception e) {
                    plugin.getLogger().warning("Failed to save offline player stage: " + e.getMessage());
                }
            });
        }
    }
}
