package id.naturalsmp.naturalSchool.ui;

import java.util.UUID;

public class ExamSession {

    private final UUID playerUuid;
    private final String subject;
    private int currentQuestion; // 1, 2, 3, 4 (4 is confirmation/declaration screen)

    // Soal 1 (Multiple Choice Checkbox) State
    private boolean ansA;
    private boolean ansB;
    private boolean ansC;
    private boolean ansD;

    // Soal 2 (True/False Confirmation) State
    private Boolean trueOrFalse; // null if not answered yet

    // Soal 3 (Multiple Statement Checklist) State
    private boolean stmt1;
    private boolean stmt2;
    private boolean stmt3;

    public ExamSession(UUID playerUuid, String subject) {
        this.playerUuid = playerUuid;
        this.subject = subject;
        this.currentQuestion = 1;
    }

    public UUID getPlayerUuid() {
        return playerUuid;
    }

    public String getSubject() {
        return subject;
    }

    public int getCurrentQuestion() {
        return currentQuestion;
    }

    public void setCurrentQuestion(int currentQuestion) {
        this.currentQuestion = currentQuestion;
    }

    // Soal 1 Getters & Setters
    public boolean isAnsA() {
        return ansA;
    }

    public void setAnsA(boolean ansA) {
        this.ansA = ansA;
    }

    public boolean isAnsB() {
        return ansB;
    }

    public void setAnsB(boolean ansB) {
        this.ansB = ansB;
    }

    public boolean isAnsC() {
        return ansC;
    }

    public void setAnsC(boolean ansC) {
        this.ansC = ansC;
    }

    public boolean isAnsD() {
        return ansD;
    }

    public void setAnsD(boolean ansD) {
        this.ansD = ansD;
    }

    // Soal 2 Getters & Setters
    public Boolean getTrueOrFalse() {
        return trueOrFalse;
    }

    public void setTrueOrFalse(Boolean trueOrFalse) {
        this.trueOrFalse = trueOrFalse;
    }

    // Soal 3 Getters & Setters
    public boolean isStmt1() {
        return stmt1;
    }

    public void setStmt1(boolean stmt1) {
        this.stmt1 = stmt1;
    }

    public boolean isStmt2() {
        return stmt2;
    }

    public void setStmt2(boolean stmt2) {
        this.stmt2 = stmt2;
    }

    public boolean isStmt3() {
        return stmt3;
    }

    public void setStmt3(boolean stmt3) {
        this.stmt3 = stmt3;
    }
}
