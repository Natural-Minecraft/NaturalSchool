package id.naturalsmp.naturalSchool.teacher;

import java.sql.Timestamp;
import java.util.UUID;

public class Teacher {
    private final UUID uuid;
    private final String name;
    private TeacherType type;
    private TeacherRole role;
    private double salaryRate;
    private double unpaidSalaryBalance;
    private Timestamp lastSalaryClaimTime;
    private final Timestamp createdAt;

    public Teacher(UUID uuid, String name, TeacherType type, TeacherRole role, double salaryRate, double unpaidSalaryBalance, Timestamp lastSalaryClaimTime, Timestamp createdAt) {
        this.uuid = uuid;
        this.name = name;
        this.type = type;
        this.role = role;
        this.salaryRate = salaryRate;
        this.unpaidSalaryBalance = unpaidSalaryBalance;
        this.lastSalaryClaimTime = lastSalaryClaimTime;
        this.createdAt = createdAt;
    }

    public UUID getUuid() {
        return uuid;
    }

    public String getName() {
        return name;
    }

    public TeacherType getType() {
        return type;
    }

    public void setType(TeacherType type) {
        this.type = type;
    }

    public TeacherRole getRole() {
        return role;
    }

    public void setRole(TeacherRole role) {
        this.role = role;
    }

    public double getSalaryRate() {
        return salaryRate;
    }

    public void setSalaryRate(double salaryRate) {
        this.salaryRate = salaryRate;
    }

    public double getUnpaidSalaryBalance() {
        return unpaidSalaryBalance;
    }

    public void setUnpaidSalaryBalance(double unpaidSalaryBalance) {
        this.unpaidSalaryBalance = unpaidSalaryBalance;
    }

    public Timestamp getLastSalaryClaimTime() {
        return lastSalaryClaimTime;
    }

    public void setLastSalaryClaimTime(Timestamp lastSalaryClaimTime) {
        this.lastSalaryClaimTime = lastSalaryClaimTime;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }
}
