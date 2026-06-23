package id.naturalsmp.naturalSchool.semester;

import id.naturalsmp.naturalSchool.NaturalSchool;
import id.naturalsmp.naturalSchool.profile.StudentProfile;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.ZonedDateTime;
import java.time.ZoneId;
import java.time.LocalDate;
import java.time.DayOfWeek;
import java.time.temporal.TemporalAdjusters;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

public class SemesterManager {

    private final NaturalSchool plugin;
    private String currentAcademicYear;
    private String currentSemester;
    private int semesterDurationDays;

    private volatile boolean simulationEnabled = false;
    private volatile long simulationOffsetMs = 0L;

    public SemesterManager(NaturalSchool plugin) {
        this.plugin = plugin;
        loadConfigValues();
        loadSimulationState();
        syncSemesterStateLocal();
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

    private synchronized void loadSimulationState() {
        try {
            Map<String, Object> state = plugin.getDatabaseManager().loadSimulationState();
            this.simulationEnabled = (boolean) state.getOrDefault("enabled", false);
            this.simulationOffsetMs = (long) state.getOrDefault("offset", 0L);
        } catch (Exception e) {
            this.simulationEnabled = false;
            this.simulationOffsetMs = 0L;
            plugin.getLogger().log(Level.SEVERE, "Failed to load simulation state", e);
        }
    }

    private synchronized void syncSemesterStateLocal() {
        ZonedDateTime now = getCurrentTime();
        AcademicState state = getAcademicState(now);
        this.currentSemester = state.getSemester();
        this.currentAcademicYear = state.getAcademicYear();
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

    public ZonedDateTime getCurrentTime() {
        ZonedDateTime actualTime = ZonedDateTime.now(ZoneId.of("Asia/Jakarta"));
        if (simulationEnabled) {
            return actualTime.plus(java.time.Duration.ofMillis(simulationOffsetMs));
        }
        return actualTime;
    }

    public boolean isSimulationEnabled() {
        return simulationEnabled;
    }

    public long getSimulationOffsetMs() {
        return simulationOffsetMs;
    }

    public void setSimulationMode(boolean enabled, long offsetMs) {
        this.simulationEnabled = enabled;
        this.simulationOffsetMs = enabled ? offsetMs : 0;
        
        // Save state to database asynchronously
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            plugin.getDatabaseManager().saveSimulationState(enabled, this.simulationOffsetMs);
        });
        
        // Sync local caches immediately
        syncSemesterStateLocal();
    }

    public long calculateSimulationOffset(int day, Integer month) {
        ZonedDateTime actualTime = ZonedDateTime.now(ZoneId.of("Asia/Jakarta"));
        int targetYear = actualTime.getYear();
        int targetMonth = (month != null) ? month : actualTime.getMonthValue();
        
        LocalDate targetDate;
        try {
            targetDate = LocalDate.of(targetYear, targetMonth, day);
        } catch (java.time.DateTimeException e) {
            LocalDate firstOfTargetMonth = LocalDate.of(targetYear, targetMonth, 1);
            int lastDay = firstOfTargetMonth.lengthOfMonth();
            targetDate = LocalDate.of(targetYear, targetMonth, lastDay);
        }
        
        ZonedDateTime targetSimTime = ZonedDateTime.of(
            targetDate,
            actualTime.toLocalTime(),
            actualTime.getZone()
        );
        
        return targetSimTime.toInstant().toEpochMilli() - actualTime.toInstant().toEpochMilli();
    }

    public CompletableFuture<Integer> processSemesterEnd() {
        return syncSemesterState();
    }

    public CompletableFuture<Integer> resetSemesterState() {
        return syncSemesterState();
    }

    public CompletableFuture<Integer> syncSemesterState() {
        CompletableFuture<Integer> future = new CompletableFuture<>();

        // Sync local variables immediately
        final String nextSemester;
        final String nextYear;
        synchronized (this) {
            syncSemesterStateLocal();
            nextSemester = this.currentSemester;
            nextYear = this.currentAcademicYear;
        }

        // Force cache update for all online players immediately on the main thread
        final List<String> onlinePlayerUuidsStr = new ArrayList<>();
        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            String uuidStr = onlinePlayer.getUniqueId().toString();
            onlinePlayerUuidsStr.add(uuidStr);
            StudentProfile profile = plugin.getProfileManager().getProfile(onlinePlayer.getUniqueId());
            if (profile != null) {
                profile.setCurrentSemester(nextSemester);
                plugin.getLogger().info("Synchronized online player cache for " + onlinePlayer.getName() + " to " + nextSemester + " during semester sync.");
            }
        }

        // Save config immediately on the main thread
        plugin.getConfig().set("semester-settings.current-semester", nextSemester);
        plugin.getConfig().set("semester-settings.current-academic-year", nextYear);
        plugin.saveConfig();

        // Run database queries asynchronously
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            int totalAffected = 0;
            try (Connection conn = plugin.getDatabaseManager().getConnection()) {
                conn.setAutoCommit(false);
                try {
                    // Update all registered students who have an assigned NIS, excluding online players to prevent race conditions
                    StringBuilder queryBuilder = new StringBuilder("UPDATE nschool_students SET current_semester = ? WHERE nis IS NOT NULL");
                    if (!onlinePlayerUuidsStr.isEmpty()) {
                        queryBuilder.append(" AND uuid NOT IN (");
                        for (int i = 0; i < onlinePlayerUuidsStr.size(); i++) {
                            queryBuilder.append("?");
                            if (i < onlinePlayerUuidsStr.size() - 1) {
                                queryBuilder.append(",");
                            }
                        }
                        queryBuilder.append(")");
                    }
                    String updateSQL = queryBuilder.toString();

                    try (PreparedStatement ps = conn.prepareStatement(updateSQL)) {
                        ps.setString(1, nextSemester);
                        int paramIndex = 2;
                        for (String uuidStr : onlinePlayerUuidsStr) {
                            ps.setString(paramIndex++, uuidStr);
                        }
                        totalAffected = ps.executeUpdate();
                    }

                    // Total affected profiles includes both online players and database-updated players
                    int totalStudentsAffected = totalAffected + onlinePlayerUuidsStr.size();

                    // Insert sync log entry
                    String logSQL = "INSERT INTO nschool_semester_log (academic_year, semester, total_students_affected) VALUES (?, ?, ?);";
                    try (PreparedStatement ps = conn.prepareStatement(logSQL)) {
                        ps.setString(1, nextYear + " (SYNC)");
                        ps.setString(2, nextSemester);
                        ps.setInt(3, totalStudentsAffected);
                        ps.executeUpdate();
                    }

                    conn.commit();
                    
                    final int finalAffected = totalStudentsAffected;
                    // Run announcement safely back on the main thread
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        String broadcastMessage = "<gray>----------------------------------------</gray>\n" +
                                "<gold><bold>SINKRONISASI AKADEMIK</bold></gold>\n" +
                                "<yellow>Jadwal semester telah disinkronkan!</yellow>\n" +
                                "<gray>» Tahun Akademik: <green>" + nextYear + "</green>\n" +
                                "<gray>» Semester: <green>" + nextSemester + "</green></gray>\n" +
                                "<gray>Total Pelajar Terdampak: <aqua>" + finalAffected + "</aqua></gray>\n" +
                                "<gray>----------------------------------------</gray>";
                        Bukkit.broadcast(MiniMessage.miniMessage().deserialize(broadcastMessage));

                        future.complete(finalAffected);
                    });
                } catch (SQLException e) {
                    conn.rollback();
                    throw e;
                } finally {
                    conn.setAutoCommit(true);
                }
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Error during semester sync transaction!", e);
                future.completeExceptionally(e);
            }
        });

        return future;
    }

    public static class AcademicState {
        private final String semester;
        private final String academicYear;
        private final String phase; // UTS, US, UAS, TRANSISI
        private final int weekIndex;
        private final long daysElapsed;
        private final ZonedDateTime cycleStart;

        public AcademicState(String semester, String academicYear, String phase, int weekIndex, long daysElapsed, ZonedDateTime cycleStart) {
            this.semester = semester;
            this.academicYear = academicYear;
            this.phase = phase;
            this.weekIndex = weekIndex;
            this.daysElapsed = daysElapsed;
            this.cycleStart = cycleStart;
        }

        public String getSemester() { return semester; }
        public String getAcademicYear() { return academicYear; }
        public String getPhase() { return phase; }
        public int getWeekIndex() { return weekIndex; }
        public long getDaysElapsed() { return daysElapsed; }
        public ZonedDateTime getCycleStart() { return cycleStart; }
    }

    public ZonedDateTime getFirstMondayOfMonth(ZonedDateTime time) {
        LocalDate firstOfMonth = time.toLocalDate().withDayOfMonth(1);
        LocalDate firstMonday;
        if (firstOfMonth.getDayOfWeek() == DayOfWeek.MONDAY) {
            firstMonday = firstOfMonth;
        } else {
            firstMonday = firstOfMonth.with(TemporalAdjusters.next(DayOfWeek.MONDAY));
        }
        return firstMonday.atStartOfDay(time.getZone());
    }

    public ZonedDateTime getAcademicCycleStart(ZonedDateTime time) {
        ZonedDateTime firstMondayCurrent = getFirstMondayOfMonth(time);
        if (time.isBefore(firstMondayCurrent)) {
            ZonedDateTime previousMonthTime = time.minusMonths(1);
            return getFirstMondayOfMonth(previousMonthTime);
        }
        return firstMondayCurrent;
    }

    public String getAcademicYearName(ZonedDateTime cycleStart) {
        String[] months = {
            "JANUARI", "FEBRUARI", "MARET", "APRIL", "MEI", "JUNI",
            "JULI", "AGUSTUS", "SEPTEMBER", "OKTOBER", "NOVEMBER", "DESEMBER"
        };
        int monthIndex = cycleStart.getMonthValue() - 1;
        return months[monthIndex] + " " + cycleStart.getYear();
    }

    public AcademicState getAcademicState(ZonedDateTime time) {
        ZonedDateTime cycleStart = getAcademicCycleStart(time);
        long daysElapsed = ChronoUnit.DAYS.between(cycleStart.toLocalDate(), time.toLocalDate());
        int weekIndex = (int) (daysElapsed / 7);
        
        String semester = (weekIndex < 2) ? "GANJIL" : "GENAP";
        String academicYear = getAcademicYearName(cycleStart);
        
        String phase;
        if (weekIndex >= 4) {
            phase = "TRANSISI";
        } else {
            DayOfWeek dayOfWeek = time.getDayOfWeek();
            boolean isMondayOrTuesday = (dayOfWeek == DayOfWeek.MONDAY || dayOfWeek == DayOfWeek.TUESDAY);
            
            if (weekIndex == 0) {
                phase = isMondayOrTuesday ? "TRANSISI" : "UTS";
            } else if (weekIndex == 1) {
                phase = "US";
            } else if (weekIndex == 2) {
                phase = isMondayOrTuesday ? "TRANSISI" : "UTS";
            } else { // weekIndex == 3
                phase = "UAS";
            }
        }
        
        return new AcademicState(semester, academicYear, phase, weekIndex, daysElapsed, cycleStart);
    }

    public void checkAndAutoRotate() {
        ZonedDateTime nowWib = getCurrentTime();
        AcademicState expected = getAcademicState(nowWib);

        boolean needsUpdate;
        synchronized (this) {
            needsUpdate = !expected.getSemester().equalsIgnoreCase(this.currentSemester) 
                       || !expected.getAcademicYear().equalsIgnoreCase(this.currentAcademicYear);
        }

        if (needsUpdate) {
            plugin.getLogger().info("[SemesterManager] Mismatch detected. Auto-rotating semester to: " 
                + expected.getSemester() + " (" + expected.getAcademicYear() + ")...");
            
            syncSemesterState().thenAccept(affected -> {
                plugin.getLogger().info("[SemesterManager] Auto-rotation complete. Affected students: " + affected);
            }).exceptionally(ex -> {
                plugin.getLogger().log(Level.SEVERE, "[SemesterManager] Auto-rotation failed!", ex);
                return null;
            });
        }
    }

    public boolean isAllowedExamTime() {
        ZonedDateTime now = getCurrentTime();
        
        // 1. Check phase
        AcademicState state = getAcademicState(now);
        String phase = state.getPhase();
        if ("TRANSISI".equals(phase)) {
            return false;
        }
        
        // 2. Check day of week (must be Saturday or Sunday)
        DayOfWeek day = now.getDayOfWeek();
        if (day != DayOfWeek.SATURDAY && day != DayOfWeek.SUNDAY) {
            return false;
        }
        
        // 3. Check hours
        int startHour = plugin.getConfig().getInt("exam-schedule.start-hour", 10);
        int endHour = plugin.getConfig().getInt("exam-schedule.end-hour", 16);
        int currentHour = now.getHour();
        return currentHour >= startHour && currentHour < endHour;
    }

    public String getExamScheduleMessage() {
        int startHour = plugin.getConfig().getInt("exam-schedule.start-hour", 10);
        int endHour = plugin.getConfig().getInt("exam-schedule.end-hour", 16);

        return "<red><bold>Portal Ujian Ditutup!</bold></red>\n" +
               "<yellow>Ujian hanya dibuka pada hari Sabtu (Ujian Utama) & Minggu (Susulan) " +
               "pukul " + String.format("%02d:00", startHour) + " - " + String.format("%02d:00", endHour) + " WIB.</yellow>";
    }
}
