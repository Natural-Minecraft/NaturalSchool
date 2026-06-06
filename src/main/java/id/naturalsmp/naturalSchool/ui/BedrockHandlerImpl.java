package id.naturalsmp.naturalSchool.ui;

import id.naturalsmp.naturalSchool.NaturalSchool;
import id.naturalsmp.naturalSchool.ui.gui.ExamGui;
import id.naturalsmp.naturalSchool.ui.gui.ExamVariantsGui;
import id.naturalsmp.naturalSchool.ui.gui.ProfileGui;
import id.naturalsmp.naturalSchool.ui.gui.RegistrationGui;
import id.naturalsmp.naturalSchool.ui.gui.StaffPanelGui;
import org.bukkit.entity.Player;
import org.geysermc.cumulus.form.SimpleForm;
import org.geysermc.floodgate.api.FloodgateApi;

import java.util.UUID;

/**
 * Implementasi BedrockHandler yang mendelegasikan setiap method ke GUI class mandiri masing-masing.
 * Setiap GUI class sudah berisi implementasi Bedrock (Cumulus) yang relevan.
 */
public class BedrockHandlerImpl implements BedrockHandler {

    private final RegistrationGui  registrationGui;
    private final ProfileGui       profileGui;
    private final StaffPanelGui    staffPanelGui;
    private final ExamVariantsGui  examVariantsGui;
    private final ExamGui          examGui;

    public BedrockHandlerImpl(NaturalSchool plugin) {
        this.registrationGui  = new RegistrationGui(plugin);
        this.profileGui       = new ProfileGui(plugin);
        this.staffPanelGui    = new StaffPanelGui(plugin);
        this.examVariantsGui  = new ExamVariantsGui(plugin);
        this.examGui          = new ExamGui(plugin);
    }

    @Override
    public boolean isBedrockPlayer(UUID uuid) {
        try {
            return FloodgateApi.getInstance().isFloodgatePlayer(uuid);
        } catch (Throwable t) {
            return false;
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // RegistrationGui — Step 1/2/3
    // ─────────────────────────────────────────────────────────────────────────

    @Override public void openStep1(Player player)                              { registrationGui.openStep1Bedrock(player); }
    @Override public void openStep2(Player player)                              { registrationGui.openStep2Bedrock(player); }
    @Override public void openStep3(Player player, boolean showWarning)         { registrationGui.openStep3Bedrock(player, showWarning); }

    // ─────────────────────────────────────────────────────────────────────────
    // openForm dispatcher (Profile & Staff Panel)
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public void openForm(Player player, SchoolMenuType menuType) {
        switch (menuType) {
            case PROFILE:      profileGui.openProfileBedrock(player);     break;
            case STAFF_PANEL:  staffPanelGui.openStaffPanelBedrock(player); break;
            default:
                throw new IllegalArgumentException("Unsupported menu type for Bedrock: " + menuType);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // ExamVariantsGui — /ns gui exam1–5 & testexam
    // ─────────────────────────────────────────────────────────────────────────


    @Override public void openExam1(Player player, boolean showWarning)         { examVariantsGui.openExam1Bedrock(player, showWarning); }
    @Override public void openExam2(Player player)                              { examVariantsGui.openExam2Bedrock(player); }
    @Override public void openExam3(Player player, boolean showWarning)         { examVariantsGui.openExam3Bedrock(player, showWarning); }
    @Override public void openExam4(Player player)                              { examVariantsGui.openExam4Bedrock(player); }
    @Override public void openExam5(Player player, boolean showWarning)         { examVariantsGui.openExam5Bedrock(player, showWarning); }

    // ─────────────────────────────────────────────────────────────────────────
    // ExamGui — /school exam (portal, pre-exam, questions 1-10, confirmation, closed)
    // ─────────────────────────────────────────────────────────────────────────

    @Override public void openExamPortal(Player player)                                         { examGui.openExamPortalBedrockDropdown(player); }
    @Override public void openExamPre(Player player, String subject)                            { examGui.openExamPreBedrock(player, subject); }
    @Override public void openExamQuestion(Player player, String subject, int questionNum)      { examGui.openExamQuestionBedrock(player, subject, questionNum); }
    @Override public void openExamConfirmation(Player player, String subject)                   { examGui.openExamConfirmationBedrock(player, subject); }
    @Override public void openExamClosed(Player player)                                         { examGui.openExamClosedBedrock(player); }
}
