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

        UUID uuid = player.getUniqueId();
        switch (menuType) {
            case REGISTRATION:
                openRegistrationForm(player, uuid);
                break;
            case PROFILE:
                openProfileForm(player, uuid);
                break;
            case STAFF_PANEL:
                openStaffPanelForm(player, uuid);
                break;
            default:
                throw new IllegalArgumentException("Unsupported menu type: " + menuType);
        }
    }

    private void openRegistrationForm(Player player, UUID uuid) {
        SimpleForm form = SimpleForm.builder()
            .title("Registration Form")
            .content("Welcome to NaturalSchool registration!\n\nPlease choose an action below:")
            .button("Register Now")
            .button("Exit")
            .validResultHandler(response -> {
                int clickedId = response.clickedButtonId();
                if (clickedId == 0) {
                    player.sendMessage("§aProcessing registration...");
                } else {
                    player.sendMessage("§cRegistration cancelled.");
                }
            })
            .closedResultHandler(() -> player.sendMessage("§cRegistration dialog dismissed."))
            .build();

        FloodgateApi.getInstance().sendForm(uuid, form);
    }

    private void openProfileForm(Player player, UUID uuid) {
        CustomForm form = CustomForm.builder()
            .title("Student Profile")
            .label("Viewing details for: " + player.getName())
            .input("Change Nickname", "Enter your preferred name...", player.getName())
            .toggle("Receive Notifications", true)
            .validResultHandler(response -> {
                String newNickname = response.asInput(1);
                boolean notify = response.asToggle(2);
                player.sendMessage("§aProfile updated! Nickname: " + newNickname + " | Notifications: " + notify);
            })
            .build();

        FloodgateApi.getInstance().sendForm(uuid, form);
    }

    private void openStaffPanelForm(Player player, UUID uuid) {
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

        FloodgateApi.getInstance().sendForm(uuid, form);
    }
}
