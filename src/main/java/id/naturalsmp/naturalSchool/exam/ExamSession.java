package id.naturalsmp.naturalSchool.exam;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ExamSession {

    private final UUID playerUuid;
    private final String subject;
    private int currentQuestion; // 0 for Pre-Exam UI, 1-N for questions, N+1 for confirmation
    private boolean showWarning = false;
    private final Map<Integer, String> answers = new ConcurrentHashMap<>();

    public ExamSession(UUID playerUuid, String subject) {
        this.playerUuid = playerUuid;
        this.subject = subject;
        this.currentQuestion = 0; // Starts at Pre-Exam UI
        this.showWarning = false;
    }

    public boolean isShowWarning() {
        return showWarning;
    }

    public void setShowWarning(boolean showWarning) {
        this.showWarning = showWarning;
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

    public String getAnswer(int index) {
        return answers.get(index);
    }

    public void setAnswer(int index, String answer) {
        if (answer == null) {
            answers.remove(index);
        } else {
            answers.put(index, answer);
        }
    }
}
