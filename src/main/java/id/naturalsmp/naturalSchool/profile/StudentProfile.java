package id.naturalsmp.naturalSchool.profile;

import java.sql.Timestamp;
import java.util.UUID;

public class StudentProfile {

    private final UUID uuid;
    private String nis;
    private String academicStage;
    private int academicClass;
    private boolean practicalPassed;
    private int temporaryGrade;
    private Timestamp lastUpdated;

    public StudentProfile(UUID uuid, String nis, String academicStage, int academicClass, boolean practicalPassed, int temporaryGrade, Timestamp lastUpdated) {
        this.uuid = uuid;
        this.nis = nis;
        this.academicStage = academicStage;
        this.academicClass = academicClass;
        this.practicalPassed = practicalPassed;
        this.temporaryGrade = temporaryGrade;
        this.lastUpdated = lastUpdated;
    }

    public UUID getUuid() {
        return uuid;
    }

    public String getNis() {
        return nis;
    }

    public void setNis(String nis) {
        this.nis = nis;
    }

    public String getAcademicStage() {
        return academicStage;
    }

    public void setAcademicStage(String academicStage) {
        this.academicStage = academicStage;
    }

    public int getAcademicClass() {
        return academicClass;
    }

    public void setAcademicClass(int academicClass) {
        this.academicClass = academicClass;
    }

    public boolean isPracticalPassed() {
        return practicalPassed;
    }

    public void setPracticalPassed(boolean practicalPassed) {
        this.practicalPassed = practicalPassed;
    }

    public int getTemporaryGrade() {
        return temporaryGrade;
    }

    public void setTemporaryGrade(int temporaryGrade) {
        this.temporaryGrade = temporaryGrade;
    }

    public Timestamp getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(Timestamp lastUpdated) {
        this.lastUpdated = lastUpdated;
    }
}
