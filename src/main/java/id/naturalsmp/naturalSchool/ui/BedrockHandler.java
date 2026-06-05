package id.naturalsmp.naturalSchool.ui;

import id.naturalsmp.naturalSchool.ui.SchoolMenuType;
import org.bukkit.entity.Player;
import java.util.UUID;

public interface BedrockHandler {
    boolean isBedrockPlayer(UUID uuid);
    void openStep1(Player player);
    void openStep2(Player player);
    void openStep3(Player player, boolean showWarning);
    void openForm(Player player, SchoolMenuType menuType);
    void openTestExam(Player player);
    void openExam1(Player player, boolean showWarning);
    void openExam2(Player player);
    void openExam3(Player player, boolean showWarning);
    void openExam4(Player player);
    void openExam5(Player player, boolean showWarning);
    void openExamPortal(Player player);
    void openExamQuestion1(Player player, String subject, boolean showWarning);
    void openExamQuestion2(Player player, String subject);
    void openExamQuestion3(Player player, String subject, boolean showWarning);
    void openExamConfirmation(Player player, String subject);
    void openExamClosed(Player player);
}
