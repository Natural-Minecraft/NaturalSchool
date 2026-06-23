package id.naturalsmp.naturalSchool.teacher;

import id.naturalsmp.naturalSchool.NaturalSchool;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.sql.Timestamp;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public class TeacherManager {

    private final NaturalSchool plugin;
    private final Map<UUID, Teacher> teachersCache;

    public TeacherManager(NaturalSchool plugin) {
        this.plugin = plugin;
        this.teachersCache = new ConcurrentHashMap<>();
    }

    public void initialize() {
        loadAllTeachers();
    }

    public void loadAllTeachers() {
        teachersCache.clear();
        List<Map<String, Object>> teachersList = plugin.getDatabaseManager().getAllTeachers();
        for (Map<String, Object> data : teachersList) {
            try {
                UUID uuid = UUID.fromString((String) data.get("teacher_uuid"));
                String name = (String) data.get("teacher_name");
                TeacherType type = TeacherType.valueOf(((String) data.get("teacher_type")).toUpperCase());
                TeacherRole role = TeacherRole.valueOf(((String) data.get("teacher_role")).toUpperCase());
                double rate = (Double) data.get("salary_rate");
                double unpaid = (Double) data.get("unpaid_salary_balance");
                Timestamp claimTime = (Timestamp) data.get("last_salary_claim_time");
                Timestamp created = (Timestamp) data.get("created_at");

                Teacher teacher = new Teacher(uuid, name, type, role, rate, unpaid, claimTime, created);
                teachersCache.put(uuid, teacher);
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to load teacher from database data: " + data, e);
            }
        }
        plugin.getLogger().info("Successfully loaded " + teachersCache.size() + " teachers into memory cache.");
    }

    public Collection<Teacher> getAllTeachers() {
        return teachersCache.values();
    }

    public Teacher getTeacher(UUID uuid) {
        if (uuid == null) return null;
        return teachersCache.get(uuid);
    }

    public Teacher getTeacherByName(String name) {
        if (name == null) return null;
        for (Teacher t : teachersCache.values()) {
            if (t.getName().equalsIgnoreCase(name)) {
                return t;
            }
        }
        // Try DB lookup
        Map<String, Object> data = plugin.getDatabaseManager().getTeacherByName(name);
        if (data != null) {
            try {
                UUID uuid = UUID.fromString((String) data.get("teacher_uuid"));
                // Load to cache if not present
                if (!teachersCache.containsKey(uuid)) {
                    TeacherType type = TeacherType.valueOf(((String) data.get("teacher_type")).toUpperCase());
                    TeacherRole role = TeacherRole.valueOf(((String) data.get("teacher_role")).toUpperCase());
                    double rate = (Double) data.get("salary_rate");
                    double unpaid = (Double) data.get("unpaid_salary_balance");
                    Timestamp claimTime = (Timestamp) data.get("last_salary_claim_time");
                    Timestamp created = (Timestamp) data.get("created_at");

                    Teacher teacher = new Teacher(uuid, name, type, role, rate, unpaid, claimTime, created);
                    teachersCache.put(uuid, teacher);
                    return teacher;
                }
                return teachersCache.get(uuid);
            } catch (Exception ignored) {}
        }
        return null;
    }

    public boolean isTeacher(UUID uuid) {
        return uuid != null && teachersCache.containsKey(uuid);
    }

    public void addTeacher(UUID uuid, String name, TeacherType type, TeacherRole role, double salaryRate) {
        Timestamp now = new Timestamp(System.currentTimeMillis());
        Teacher teacher = new Teacher(uuid, name, type, role, salaryRate, 0.0, now, now);
        teachersCache.put(uuid, teacher);
        
        plugin.getDatabaseManager().saveTeacher(uuid, name, type.name(), role.name(), salaryRate, 0.0, now);
    }

    public void removeTeacher(UUID uuid) {
        teachersCache.remove(uuid);
        plugin.getDatabaseManager().deleteTeacher(uuid);
    }

    public boolean editTeacher(UUID uuid, String field, String value) {
        Teacher teacher = teachersCache.get(uuid);
        if (teacher == null) return false;

        try {
            switch (field.toLowerCase()) {
                case "rate":
                case "salary_rate":
                    double rate = Double.parseDouble(value);
                    teacher.setSalaryRate(rate);
                    break;
                case "type":
                case "teacher_type":
                    TeacherType type = TeacherType.valueOf(value.toUpperCase());
                    teacher.setType(type);
                    break;
                case "role":
                case "teacher_role":
                    TeacherRole role = TeacherRole.valueOf(value.toUpperCase());
                    teacher.setRole(role);
                    break;
                default:
                    return false;
            }
            plugin.getDatabaseManager().saveTeacher(
                    teacher.getUuid(),
                    teacher.getName(),
                    teacher.getType().name(),
                    teacher.getRole().name(),
                    teacher.getSalaryRate(),
                    teacher.getUnpaidSalaryBalance(),
                    teacher.getLastSalaryClaimTime()
            );
            return true;
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Failed to edit teacher field " + field + " with value " + value, e);
            return false;
        }
    }

    public double calculatePendingSalary(UUID uuid) {
        Teacher teacher = teachersCache.get(uuid);
        if (teacher == null) return 0.0;

        if (teacher.getType() == TeacherType.TETAP) {
            long lastClaim = teacher.getLastSalaryClaimTime().getTime();
            long now = System.currentTimeMillis();
            long elapsedMs = now - lastClaim;
            
            // Weekly scale (7 days in ms)
            double weeksElapsed = (double) elapsedMs / (7.0 * 24.0 * 60.0 * 60.0 * 1000.0);
            if (weeksElapsed < 0) weeksElapsed = 0;
            
            double dynamicPay = weeksElapsed * teacher.getSalaryRate();
            return dynamicPay + teacher.getUnpaidSalaryBalance();
        } else {
            // Honorer is based purely on accumulated sessions (stored in unpaid_salary_balance)
            return teacher.getUnpaidSalaryBalance();
        }
    }

    public void rewardHonorerSession(UUID teacherUuid) {
        Teacher teacher = teachersCache.get(teacherUuid);
        if (teacher == null) return;

        if (teacher.getType() == TeacherType.HONORER) {
            double newBalance = teacher.getUnpaidSalaryBalance() + teacher.getSalaryRate();
            teacher.setUnpaidSalaryBalance(newBalance);
            
            plugin.getDatabaseManager().saveTeacher(
                    teacher.getUuid(),
                    teacher.getName(),
                    teacher.getType().name(),
                    teacher.getRole().name(),
                    teacher.getSalaryRate(),
                    newBalance,
                    teacher.getLastSalaryClaimTime()
            );
            plugin.getLogger().info("Incremented Honorer teacher " + teacher.getName() + " salary balance by " + teacher.getSalaryRate() + " for ending session.");
        }
    }

    public ClaimResult claimSalary(Player player) {
        Teacher teacher = teachersCache.get(player.getUniqueId());
        if (teacher == null) {
            return new ClaimResult(false, "Anda tidak terdaftar sebagai Staff Pengajar (Guru).");
        }

        double pending = calculatePendingSalary(player.getUniqueId());
        if (pending <= 0) {
            return new ClaimResult(false, "Anda tidak memiliki gaji tertunggak saat ini.");
        }

        Economy econ = plugin.getClassCashManager() != null ? plugin.getClassCashManager().getEconomy() : null;
        if (econ == null) {
            return new ClaimResult(false, "Sistem ekonomi server (Vault) tidak terhubung.");
        }

        // Process economic transaction
        econ.depositPlayer(player, pending);

        // Reset balance & last claim time
        Timestamp now = new Timestamp(System.currentTimeMillis());
        teacher.setUnpaidSalaryBalance(0.0);
        teacher.setLastSalaryClaimTime(now);

        plugin.getDatabaseManager().saveTeacher(
                teacher.getUuid(),
                teacher.getName(),
                teacher.getType().name(),
                teacher.getRole().name(),
                teacher.getSalaryRate(),
                0.0,
                now
        );

        return new ClaimResult(true, "Berhasil mencairkan gaji sebesar $" + String.format("%,.2f", pending) + " ke rekening Anda!");
    }

    public static class ClaimResult {
        private final boolean success;
        private final String message;

        public ClaimResult(boolean success, String message) {
            this.success = success;
            this.message = message;
        }

        public boolean isSuccess() {
            return success;
        }

        public String getMessage() {
            return message;
        }
    }
}
