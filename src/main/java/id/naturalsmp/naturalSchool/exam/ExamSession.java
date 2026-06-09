package id.naturalsmp.naturalSchool.exam;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ExamSession {

    private final UUID playerUuid;
    private final String packetId;
    private int currentQuestion; // 0 for Pre-Exam UI, 1-N for questions, N+1 for confirmation
    private boolean showWarning = false;
    private final Map<Integer, String> answers = new ConcurrentHashMap<>();

    public ExamSession(UUID playerUuid, String packetId) {
        this.playerUuid = playerUuid;
        this.packetId = packetId;
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

    public String getPacketId() {
        return packetId;
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

