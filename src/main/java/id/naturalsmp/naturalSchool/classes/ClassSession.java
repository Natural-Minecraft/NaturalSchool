package id.naturalsmp.naturalSchool.classes;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ClassSession {

    private final String idKelas; // Format: kelas1 to kelas12
    private final String subject;
    private final UUID teacherUuid;
    private final long startTime;
    private boolean closed = false;

    // Projector board material loaded
    private String projectorFileName;
    private String projectorContent;

    // Quiz loaded
    private String quizFileName;
    private String quizContentJson;
    private boolean quizStarted = false;

    // Attendance record: student UUID -> Status ('HADIR', 'TERLAMBAT', 'ALFA', 'IZIN', 'SAKIT')
    private final Map<UUID, String> attendanceMap = new ConcurrentHashMap<>();

    // Grades record: student UUID -> Quiz score (0-100)
    private final Map<UUID, Integer> gradesMap = new ConcurrentHashMap<>();

    // Set of students early dismissed
    private final Set<UUID> earlyDismissed = ConcurrentHashMap.newKeySet();

    public ClassSession(String idKelas, String subject, UUID teacherUuid) {
        this.idKelas = idKelas;
        this.subject = subject;
        this.teacherUuid = teacherUuid;
        this.startTime = System.currentTimeMillis();
    }

    public String getIdKelas() {
        return idKelas;
    }

    public String getSubject() {
        return subject;
    }

    public UUID getTeacherUuid() {
        return teacherUuid;
    }

    public long getStartTime() {
        return startTime;
    }

    public boolean isClosed() {
        return closed;
    }

    public void setClosed(boolean closed) {
        this.closed = closed;
    }

    public String getProjectorFileName() {
        return projectorFileName;
    }

    public void setProjectorFileName(String projectorFileName) {
        this.projectorFileName = projectorFileName;
    }

    public String getProjectorContent() {
        return projectorContent;
    }

    public void setProjectorContent(String projectorContent) {
        this.projectorContent = projectorContent;
    }

    public String getQuizFileName() {
        return quizFileName;
    }

    public void setQuizFileName(String quizFileName) {
        this.quizFileName = quizFileName;
    }

    public String getQuizContentJson() {
        return quizContentJson;
    }

    public void setQuizContentJson(String quizContentJson) {
        this.quizContentJson = quizContentJson;
    }

    public boolean isQuizStarted() {
        return quizStarted;
    }

    public void setQuizStarted(boolean quizStarted) {
        this.quizStarted = quizStarted;
    }

    public Map<UUID, String> getAttendanceMap() {
        return attendanceMap;
    }

    public Map<UUID, Integer> getGradesMap() {
        return gradesMap;
    }

    public Set<UUID> getEarlyDismissed() {
        return earlyDismissed;
    }
}
