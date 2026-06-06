package id.naturalsmp.naturalSchool.ui;

import java.util.UUID;

public class ExamSession {

    private final UUID playerUuid;
    private final String subject;
    private int currentQuestion; // 0 for Pre-Exam UI, 1-10 for questions, 11 for confirmation

    // Answers for questions 1 to 6 (index 0 to 5)
    private final String[] mcAnswers = new String[6];

    // Answers for questions 7 to 8 (index 0 to 1)
    private final Boolean[] tfAnswers = new Boolean[2];

    // Answers for questions 9 to 10 (index 0 to 1, each has 3 statements)
    private final boolean[][] complexAnswers = new boolean[2][3];

    public ExamSession(UUID playerUuid, String subject) {
        this.playerUuid = playerUuid;
        this.subject = subject;
        this.currentQuestion = 0; // Starts at Pre-Exam UI
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

    public String getMcAnswer(int idx) {
        if (idx < 0 || idx >= mcAnswers.length) {
            return null;
        }
        return mcAnswers[idx];
    }

    public void setMcAnswer(int idx, String answer) {
        if (idx >= 0 && idx < mcAnswers.length) {
            mcAnswers[idx] = answer;
        }
    }

    public Boolean getTfAnswer(int idx) {
        if (idx < 0 || idx >= tfAnswers.length) {
            return null;
        }
        return tfAnswers[idx];
    }

    public void setTfAnswer(int idx, Boolean answer) {
        if (idx >= 0 && idx < tfAnswers.length) {
            tfAnswers[idx] = answer;
        }
    }

    public boolean[] getComplexAnswer(int idx) {
        if (idx < 0 || idx >= complexAnswers.length) {
            return null;
        }
        return complexAnswers[idx];
    }

    public boolean getComplexOption(int idx, int optionIdx) {
        if (idx < 0 || idx >= complexAnswers.length || optionIdx < 0 || optionIdx >= 3) {
            return false;
        }
        return complexAnswers[idx][optionIdx];
    }

    public void setComplexOption(int idx, int optionIdx, boolean val) {
        if (idx >= 0 && idx < complexAnswers.length && optionIdx >= 0 && optionIdx < 3) {
            complexAnswers[idx][optionIdx] = val;
        }
    }
}
