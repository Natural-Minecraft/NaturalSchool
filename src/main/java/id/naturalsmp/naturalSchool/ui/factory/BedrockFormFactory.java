package id.naturalsmp.naturalSchool.ui.factory;

import id.naturalsmp.naturalSchool.NaturalSchool;
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
     * STEP 1: Welcome & Info SimpleForm
     */
    public void openStep1(Player player) {
        SimpleForm form = SimpleForm.builder()
            .title("NaturalSchool Onboarding")
            .content("Welcome, " + player.getName() + "!\n\n" +
                "Username: " + player.getName() + "\n" +
                "NIS: Unregistered\n" +
                "Status: Belum Terdaftar\n\n" +
                "Silakan klik Continue untuk melanjutkan.")
            .button("Continue")
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
     * STEP 2: Cutscene cinematic offer SimpleForm
     */
    public void openStep2(Player player) {
        SimpleForm form = SimpleForm.builder()
            .title("Tonton Cinematic?")
            .content("Apakah anda ingin menonton cinematic perkenalan NaturalSchool?")
            .button("Tonton Sinematik")
            .button("Lewati")
            .validResultHandler(response -> {
                int clickedId = response.clickedButtonId();
                if (clickedId == 0) {
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
    public void openStep3(Player player) {
        CustomForm form = CustomForm.builder()
            .title("ToS & Rules Agreement")
            .label("Untuk mulai bermain, anda harus menyetujui ToS dan Rules kami.\nAturan di: https://naturalsmp.net")
            .toggle("Saya Menyetujui Terms Of Service", false)
            .toggle("Saya Menyetujui Rules Server", false)
            .toggle("Tolak & Keluar dari Server", false)
            .validResultHandler(response -> {
                boolean acceptTos = response.asToggle(1);
                boolean acceptRules = response.asToggle(2);
                boolean decline = response.asToggle(3);

                if (decline) {
                    player.kick(net.kyori.adventure.text.minimessage.MiniMessage.miniMessage()
                        .deserialize("<red>Anda harus menyetujui ToS dan Rules untuk bermain di server ini!</red>"));
                    return;
                }

                if (acceptTos && acceptRules) {
                    plugin.getUiManager().completeRegistration(player);
                } else {
                    player.sendActionBar(net.kyori.adventure.text.minimessage.MiniMessage.miniMessage()
                        .deserialize("<red>Anda harus mencentang KEDUA persetujuan untuk dapat bermain!</red>"));
                    openStep3(player);
                }
            })
            .closedResultHandler(() -> {
                // Prevent escape: reopen Step 3
                openStep3(player);
            })
            .build();

        FloodgateApi.getInstance().sendForm(player.getUniqueId(), form);
    }

    private void openProfileForm(Player player) {
        SimpleForm form = SimpleForm.builder()
            .title("Student Profile")
            .content("Name: " + player.getName() + "\nUUID: " + player.getUniqueId())
            .button("Close")
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
