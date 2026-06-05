package id.naturalsmp.naturalSchool.semester;

import id.naturalsmp.naturalSchool.NaturalSchool;
import id.naturalsmp.naturalSchool.profile.StudentProfile;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

public class SemesterManager {

    private final NaturalSchool plugin;
    private String currentAcademicYear;
    private String currentSemester;
    private int semesterDurationDays;

    public SemesterManager(NaturalSchool plugin) {
        this.plugin = plugin;
        loadConfigValues();
    }

    public void loadConfigValues() {
        plugin.getConfig().addDefault("semester-settings.semester-duration-days", 14);
        plugin.getConfig().addDefault("semester-settings.current-academic-year", "JUNI 2026");
        plugin.getConfig().addDefault("semester-settings.current-semester", "GANJIL");
        plugin.saveConfig();

        this.semesterDurationDays = plugin.getConfig().getInt("semester-settings.semester-duration-days", 14);
        this.currentAcademicYear = plugin.getConfig().getString("semester-settings.current-academic-year", "JUNI 2026");
        this.currentSemester = plugin.getConfig().getString("semester-settings.current-semester", "GANJIL").toUpperCase();
    }

    public synchronized String getCurrentAcademicYear() {
        return currentAcademicYear;
    }

    public synchronized String getCurrentSemester() {
        return currentSemester;
    }

    public synchronized int getSemesterDurationDays() {
        return semesterDurationDays;
    }

    /**
     * Runs the asynchronous rotation processor method.
     * Transitions semester (GANJIL -> GENAP -> GANJIL with next Month/Year),
     * updates the database for all registered profiles with non-null NIS,
     * updates cache for online players, and broadcasts the transition.
     *
     * @return a CompletableFuture completed with the count of affected students.
     */
    public CompletableFuture<Integer> processSemesterEnd() {
        CompletableFuture<Integer> future = new CompletableFuture<>();
        
        // Capture current state
        final String previousYear = this.currentAcademicYear;
        final String previousSemester = this.currentSemester;

        // Perform State Mutation
        final String nextSemester;
        final String nextYear;
        if ("GANJIL".equals(previousSemester)) {
            nextSemester = "GENAP";
            nextYear = previousYear; // Year remains unchanged
        } else {
            nextSemester = "GANJIL";
            nextYear = advanceAcademicYear(previousYear); // Programmatically advance month
        }

        // Run asynchronously
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            int totalAffected = 0;
            try (Connection conn = plugin.getDatabaseManager().getConnection()) {
                conn.setAutoCommit(false);
                try {
                    // Update all registered students who have an assigned NIS
                    String updateSQL = "UPDATE nschool_students SET current_semester = ? WHERE nis IS NOT NULL;";
                    try (PreparedStatement ps = conn.prepareStatement(updateSQL)) {
                        ps.setString(1, nextSemester);
                        totalAffected = ps.executeUpdate();
                    }

                    // Insert rotation log entry
                    String logSQL = "INSERT INTO nschool_semester_log (academic_year, semester, total_students_affected) VALUES (?, ?, ?);";
                    try (PreparedStatement ps = conn.prepareStatement(logSQL)) {
                        ps.setString(1, previousYear);
                        ps.setString(2, previousSemester);
                        ps.setInt(3, totalAffected);
                        ps.executeUpdate();
                    }

                    conn.commit();
                } catch (SQLException e) {
                    conn.rollback();
                    throw e;
                } finally {
                    conn.setAutoCommit(true);
                }

                // Update configuration and runtime variables
                synchronized (this) {
                    this.currentSemester = nextSemester;
                    this.currentAcademicYear = nextYear;
                }

                final int finalAffected = totalAffected;
                // Run config save on the main thread safely
                Bukkit.getScheduler().runTask(plugin, () -> {
                    try {
                        plugin.getConfig().set("semester-settings.current-semester", nextSemester);
                        plugin.getConfig().set("semester-settings.current-academic-year", nextYear);
                        plugin.saveConfig();

                        // Force cache refresh for all online players
                        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                            StudentProfile profile = plugin.getProfileManager().getProfile(onlinePlayer.getUniqueId());
                            if (profile != null) {
                                profile.setCurrentSemester(nextSemester);
                            }
                        }

                        // Broadcast announcement message via MiniMessage detailing the transition
                        String broadcastMessage = "<gray>----------------------------------------</gray>\n" +
                                "<gold><bold>PENGUMUMAN AKADEMIK</bold></gold>\n" +
                                "<yellow>Semester baru telah dimulai!</yellow>\n" +
                                "<gray>» Dari: <white>" + previousSemester + " (" + previousYear + ")</white>\n" +
                                "<gray>» Ke: <green>" + nextSemester + " (TA " + nextYear + ")</green>\n" +
                                "<gray>Total Pelajar Terdampak: <aqua>" + finalAffected + "</aqua></gray>\n" +
                                "<gray>----------------------------------------</gray>";
                        Bukkit.broadcast(MiniMessage.miniMessage().deserialize(broadcastMessage));

                        future.complete(finalAffected);
                    } catch (Throwable t) {
                        future.completeExceptionally(t);
                    }
                });
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Error during semester rotation transaction!", e);
                future.completeExceptionally(e);
            }
        });

        return future;
    }

    /**
     * Resets the semester state back to match the real-life system clock calendar.
     * Week 1 & 2 (days 1-14) map to GANJIL, while days 15+ map to GENAP of the current real month.
     *
     * @return a CompletableFuture completed with the count of affected students.
     */
    public CompletableFuture<Integer> resetSemesterState() {
        CompletableFuture<Integer> future = new CompletableFuture<>();

        java.time.LocalDate now = java.time.LocalDate.now();
        int day = now.getDayOfMonth();
        final String nextSemester = (day <= 14) ? "GANJIL" : "GENAP";
        final String nextYear = getIndonesianMonthYear();

        // Run asynchronously
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            int totalAffected = 0;
            try (Connection conn = plugin.getDatabaseManager().getConnection()) {
                conn.setAutoCommit(false);
                try {
                    // Update all registered students who have an assigned NIS
                    String updateSQL = "UPDATE nschool_students SET current_semester = ? WHERE nis IS NOT NULL;";
                    try (PreparedStatement ps = conn.prepareStatement(updateSQL)) {
                        ps.setString(1, nextSemester);
                        totalAffected = ps.executeUpdate();
                    }

                    // Insert rotation log entry for RESET
                    String logSQL = "INSERT INTO nschool_semester_log (academic_year, semester, total_students_affected) VALUES (?, ?, ?);";
                    try (PreparedStatement ps = conn.prepareStatement(logSQL)) {
                        ps.setString(1, nextYear + " (RESET)");
                        ps.setString(2, nextSemester);
                        ps.setInt(3, totalAffected);
                        ps.executeUpdate();
                    }

                    conn.commit();
                } catch (SQLException e) {
                    conn.rollback();
                    throw e;
                } finally {
                    conn.setAutoCommit(true);
                }

                // Update configuration and runtime variables
                synchronized (this) {
                    this.currentSemester = nextSemester;
                    this.currentAcademicYear = nextYear;
                }

                final int finalAffected = totalAffected;
                // Run config save on the main thread safely
                Bukkit.getScheduler().runTask(plugin, () -> {
                    try {
                        plugin.getConfig().set("semester-settings.current-semester", nextSemester);
                        plugin.getConfig().set("semester-settings.current-academic-year", nextYear);
                        plugin.saveConfig();

                        // Force cache refresh for all online players
                        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                            StudentProfile profile = plugin.getProfileManager().getProfile(onlinePlayer.getUniqueId());
                            if (profile != null) {
                                profile.setCurrentSemester(nextSemester);
                            }
                        }

                        // Broadcast announcement message via MiniMessage detailing the transition
                        String broadcastMessage = "<gray>----------------------------------------</gray>\n" +
                                "<gold><bold>PENGUMUMAN AKADEMIK (RESET)</bold></gold>\n" +
                                "<yellow>Semester telah di-reset sesuai kalender real-life!</yellow>\n" +
                                "<gray>» Tahun Akademik: <green>" + nextYear + "</green>\n" +
                                "<gray>» Semester: <green>" + nextSemester + "</green></gray>\n" +
                                "<gray>Total Pelajar Terdampak: <aqua>" + finalAffected + "</aqua></gray>\n" +
                                "<gray>----------------------------------------</gray>";
                        Bukkit.broadcast(MiniMessage.miniMessage().deserialize(broadcastMessage));

                        future.complete(finalAffected);
                    } catch (Throwable t) {
                        future.completeExceptionally(t);
                    }
                });
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Error during semester reset transaction!", e);
                future.completeExceptionally(e);
            }
        });

        return future;
    }

    private String advanceAcademicYear(String currentYearStr) {
        if (currentYearStr == null || currentYearStr.trim().isEmpty()) {
            return getIndonesianMonthYear();
        }
        String[] parts = currentYearStr.split(" ");
        if (parts.length != 2) {
            return getIndonesianMonthYear();
        }
        String monthStr = parts[0].toUpperCase();
        int year;
        try {
            year = Integer.parseInt(parts[1]);
        } catch (NumberFormatException e) {
            year = java.time.LocalDate.now().getYear();
        }

        String[] months = {
            "JANUARI", "FEBRUARI", "MARET", "APRIL", "MEI", "JUNI",
            "JULI", "AGUSTUS", "SEPTEMBER", "OKTOBER", "NOVEMBER", "DESEMBER"
        };

        int monthIndex = -1;
        for (int i = 0; i < months.length; i++) {
            if (months[i].equals(monthStr)) {
                monthIndex = i;
                break;
            }
        }

        if (monthIndex == -1) {
            return getIndonesianMonthYear();
        }

        int nextMonthIndex = (monthIndex + 1) % 12;
        if (nextMonthIndex == 0) {
            year++; // Wrap to next year
        }

        return months[nextMonthIndex] + " " + year;
    }

    private String getIndonesianMonthYear() {
        java.time.LocalDate now = java.time.LocalDate.now();
        int month = now.getMonthValue();
        int year = now.getYear();
        String[] months = {
            "JANUARI", "FEBRUARI", "MARET", "APRIL", "MEI", "JUNI",
            "JULI", "AGUSTUS", "SEPTEMBER", "OKTOBER", "NOVEMBER", "DESEMBER"
        };
        return months[month - 1] + " " + year;
    }
}
