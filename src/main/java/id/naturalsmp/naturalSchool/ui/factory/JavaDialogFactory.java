package id.naturalsmp.naturalSchool.ui.factory;

import id.naturalsmp.naturalSchool.NaturalSchool;
import id.naturalsmp.naturalSchool.ui.SchoolMenuType;
import id.naturalsmp.naturalSchool.ui.util.DialogFormatter;
import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class JavaDialogFactory {

    private final NaturalSchool plugin;

    public JavaDialogFactory(NaturalSchool plugin) {
        this.plugin = plugin;
    }

    /**
     * Constructs and opens a Dialog menu for Java players.
     *
     * @param player   the player opening the dialog
     * @param menuType the type of menu to display
     */
    public void openDialog(Player player, SchoolMenuType menuType) {
        if (player == null || menuType == null) {
            return;
        }

        Dialog dialog;
        switch (menuType) {
            case REGISTRATION:
                dialog = createRegistrationDialog();
                break;
            case PROFILE:
                dialog = createProfileDialog(player);
                break;
            case STAFF_PANEL:
                dialog = createStaffPanelDialog();
                break;
            default:
                throw new IllegalArgumentException("Unsupported menu type: " + menuType);
        }

        player.showDialog(dialog);
    }

    private Dialog createRegistrationDialog() {
        List<String> rawLines = List.of(
            "<green><bold>NaturalSchool Registration</bold></green>",
            "<gray>Welcome to NaturalSchool registration page.</gray>",
            "<gray>Please verify your status before proceeding.</gray>",
            "<yellow>Note: You cannot close this window using ESC.</yellow>"
        );

        List<String> alignedLines = DialogFormatter.alignLeft(rawLines);
        List<DialogBody> bodies = new ArrayList<>();
        for (String line : alignedLines) {
            Component component = MiniMessage.miniMessage().deserialize(line);
            bodies.add(DialogBody.plainMessage(component));
        }

        return Dialog.create(builder -> builder.empty()
            .base(DialogBase.builder(Component.text("Registration Form"))
                .canCloseWithEscape(false)
                .body(bodies)
                .build())
            .type(DialogType.notice())
        );
    }

    private Dialog createProfileDialog(Player player) {
        List<String> rawLines = List.of(
            "<aqua><bold>Student Profile</bold></aqua>",
            "<gray>Name: " + player.getName() + "</gray>",
            "<gray>UUID: " + player.getUniqueId().toString().substring(0, 8) + "...</gray>",
            "<green>Active Student Status</green>"
        );

        List<String> alignedLines = DialogFormatter.alignLeft(rawLines);
        List<DialogBody> bodies = new ArrayList<>();
        for (String line : alignedLines) {
            Component component = MiniMessage.miniMessage().deserialize(line);
            bodies.add(DialogBody.plainMessage(component));
        }

        return Dialog.create(builder -> builder.empty()
            .base(DialogBase.builder(Component.text("Student Profile"))
                .canCloseWithEscape(true)
                .body(bodies)
                .build())
            .type(DialogType.notice())
        );
    }

    private Dialog createStaffPanelDialog() {
        List<String> rawLines = List.of(
            "<red><bold>Staff Panel</bold></red>",
            "<gray>Welcome Administrator.</gray>",
            "<gray>Manage registered students here.</gray>",
            "<yellow>Access level: High</yellow>"
        );

        List<String> alignedLines = DialogFormatter.alignLeft(rawLines);
        List<DialogBody> bodies = new ArrayList<>();
        for (String line : alignedLines) {
            Component component = MiniMessage.miniMessage().deserialize(line);
            bodies.add(DialogBody.plainMessage(component));
        }

        return Dialog.create(builder -> builder.empty()
            .base(DialogBase.builder(Component.text("Staff Panel"))
                .canCloseWithEscape(true)
                .body(bodies)
                .build())
            .type(DialogType.notice())
        );
    }
}
