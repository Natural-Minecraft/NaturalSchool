package id.naturalsmp.naturalSchool.ui.factory;

import id.naturalsmp.naturalSchool.NaturalSchool;
import id.naturalsmp.naturalSchool.profile.StudentProfile;
import id.naturalsmp.naturalSchool.ui.SchoolMenuType;
import org.bukkit.entity.Player;
import org.geysermc.cumulus.form.CustomForm;
import org.geysermc.cumulus.form.SimpleForm;
import org.geysermc.cumulus.response.CustomFormResponse;
import org.geysermc.cumulus.response.SimpleFormResponse;
import org.geysermc.floodgate.api.FloodgateApi;

import java.util.UUID;

public class BedrockFormFactory {

    private final NaturalSchool plugin;

    public BedrockFormFactory(NaturalSchool plugin) {
        this.plugin = plugin;
    }

    /**
     * Constructs and opens a Bedrock Form menu for Geyser/Floodgate players.
     *
     * @param player   the player opening the form
     * @param menuType the type of menu to display
     */
    public void openForm(Player player, SchoolMenuType menuType) {
        if (player == null || menuType == null) {
            return;
        }

        switch (menuType) {
            case REGISTRATION:
                openStep1(player);
                break;
            case PROFILE:
                openProfileForm(player);
                break;
            case STAFF_PANEL:
                openStaffPanelForm(player);
                break;
            default:
                throw new IllegalArgumentException("Unsupported menu type: " + menuType);
        }
    }

    /**
     * STEP 1: Welcome & Info CustomForm
     */
    public void openStep1(Player player) {
        StudentProfile profile = plugin.getProfileManager().getProfile(player.getUniqueId());
        String nis = (profile == null || profile.getNis() == null) ? "Unregistered" : profile.getNis();
        String status = (profile == null || profile.getNis() == null) ? "Belum Terdaftar" : "Terdaftar";

        CustomForm form = CustomForm.builder()
            .title("NaturalSchool Onboarding")
            .label("Welcome, " + player.getName() + "!\n\n" +
                "Username: " + player.getName() + "\n" +
                "NIS: " + nis + "\n" +
                "Status: " + status + "\n\n" +
                "Silakan klik Submit untuk melanjutkan.")
            .validResultHandler(response -> {
                plugin.getUiManager().openStep2(player);
            })
            .closedResultHandler(() -> {
                // Prevent escape: reopen Step 1
                openStep1(player);
            })
            .build();

        FloodgateApi.getInstance().sendForm(player.getUniqueId(), form);
    }

    /**
     * STEP 2: Cutscene cinematic offer CustomForm
     */
    public void openStep2(Player player) {
        CustomForm form = CustomForm.builder()
            .title("Tonton Cinematic?")
            .label("Apakah anda ingin menonton cinematic perkenalan NaturalSchool?")
            .toggle("Tonton Sinematik", false)
            .validResultHandler(response -> {
                boolean watchCinematic = response.asToggle(1);
                if (watchCinematic) {
                    player.sendMessage(net.kyori.adventure.text.minimessage.MiniMessage.miniMessage()
                        .deserialize("<yellow>Fitur cutscene mendatang!</yellow>"));
                }
                plugin.getUiManager().openStep3(player);
            })
            .closedResultHandler(() -> {
                // Prevent escape: reopen Step 2
                openStep2(player);
            })
            .build();

        FloodgateApi.getInstance().sendForm(player.getUniqueId(), form);
    }

    /**
     * STEP 3: ToS & Rules Agreement CustomForm
     */
    public void openStep3(Player player, boolean showWarning) {
        CustomForm.Builder builder = CustomForm.builder()
            .title("ToS & Rules Agreement");

        if (showWarning) {
            builder.label("§c§lAnda wajib menyetujui untuk bermain");
        }

        builder.label("Untuk mulai bermain, anda harus menyetujui ToS dan Rules kami.\nAturan di: https://naturalsmp.net")
            .toggle("Saya Menyetujui Terms Of Service", false)
            .toggle("Saya Menyetujui Rules Server", false);

        CustomForm form = builder.validResultHandler(response -> {
                // Cumulus asToggle(n) is 0-indexed by toggle elements only, not by all form elements.
                // The warning label prepended does NOT shift toggle indices.
                boolean acceptTos = response.asToggle(0);
                boolean acceptRules = response.asToggle(1);

                if (acceptTos && acceptRules) {
                    plugin.getUiManager().completeRegistration(player);
                } else {
                    plugin.getUiManager().openStep3(player, true);
                }
            })
            .closedResultHandler(() -> {
                // Prevent escape: reopen Step 3
                openStep3(player, showWarning);
            })
            .build();

        FloodgateApi.getInstance().sendForm(player.getUniqueId(), form);
    }

    private void openProfileForm(Player player) {
        StudentProfile profile = plugin.getProfileManager().getProfile(player.getUniqueId());
        String username = profile != null && profile.getUsername() != null ? profile.getUsername() : player.getName();
        String nis = (profile == null || profile.getNis() == null) ? "Belum Terdaftar" : profile.getNis();
        int academicClass = profile != null ? profile.getAcademicClass() : 0;
        String academicStage = profile != null ? profile.getAcademicStage() : "NONE";

        SimpleForm form = SimpleForm.builder()
            .title("Informasi Pelajar")
            .content("Username: " + username + "\n" +
                     "NIS: " + nis + "\n" +
                     "Kelas + Jenjang: " + academicClass + " (" + academicStage + ")")
            .button("Tutup")
            .build();

        FloodgateApi.getInstance().sendForm(player.getUniqueId(), form);
    }

    private void openStaffPanelForm(Player player) {
        SimpleForm form = SimpleForm.builder()
            .title("Staff Control Panel")
            .content("Administrator management options:")
            .button("View Student Database")
            .button("Clear Log Cache")
            .validResultHandler(response -> {
                int clickedId = response.clickedButtonId();
                player.sendMessage("§aStaff action performed: Option " + (clickedId + 1));
            })
            .build();

        FloodgateApi.getInstance().sendForm(player.getUniqueId(), form);
    }
}
