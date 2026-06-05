package id.naturalsmp.naturalSchool.profile;

import java.sql.Timestamp;
import java.util.UUID;

public class StudentProfile {

    private final UUID uuid;
    private String username;
    private String nis;
    private String academicStage;
    private int academicClass;
    private volatile String currentSemester;
    private Timestamp lastUpdated;
    private SchoolRank rank;

    public StudentProfile(UUID uuid, String username, String nis, String academicStage, int academicClass, String currentSemester, Timestamp lastUpdated, SchoolRank rank) {
        this.uuid = uuid;
        this.username = username;
        this.nis = nis;
        this.academicStage = academicStage;
        this.academicClass = academicClass;
        this.currentSemester = currentSemester != null ? currentSemester : "GANJIL";
        this.lastUpdated = lastUpdated;
        this.rank = rank;
    }

    public UUID getUuid() {
        return uuid;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
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

    public synchronized String getCurrentSemester() {
        return currentSemester;
    }

    public synchronized void setCurrentSemester(String currentSemester) {
        this.currentSemester = currentSemester;
    }

    public Timestamp getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(Timestamp lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    public SchoolRank getRank() {
        return rank;
    }

    public void setRank(SchoolRank rank) {
        this.rank = rank;
    }

    public boolean isStaff() {
        return rank != null && (rank.getType() == SchoolRank.RankType.STAFF 
                || rank.getType() == SchoolRank.RankType.HELPER 
                || rank.getType() == SchoolRank.RankType.MANAGEMENT);
    }

    public boolean isManagement() {
        return rank != null && rank.getType() == SchoolRank.RankType.MANAGEMENT;
    }

    public boolean hasHigherOrEqualRank(SchoolRank other) {
        if (this.rank == null) return false;
        if (other == null) return true;
        return this.rank.getPriority() >= other.getPriority();
    }
}
