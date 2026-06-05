package id.naturalsmp.naturalSchool.ui.factory;

import id.naturalsmp.naturalSchool.NaturalSchool;
import id.naturalsmp.naturalSchool.ui.SchoolMenuType;
import id.naturalsmp.naturalSchool.ui.util.DialogFormatter;
import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.input.DialogInput;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickCallback;
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

        switch (menuType) {
            case REGISTRATION:
                openStep1(player);
                break;
            case PROFILE:
                player.showDialog(createProfileDialog(player));
                break;
            case STAFF_PANEL:
                player.showDialog(createStaffPanelDialog());
                break;
            default:
                throw new IllegalArgumentException("Unsupported menu type: " + menuType);
        }
    }

    /**
     * STEP 1: Welcome & Info Dialog
     */
    public void openStep1(Player player) {
        List<String> rawLines = List.of(
            "<green><bold>NaturalSchool Onboarding</bold></green>",
            "<gray>Welcome, " + player.getName() + "!</gray>",
            "Username: " + player.getName(),
            "NIS: Unregistered",
            "Status: Belum Terdaftar"
        );

        List<String> alignedLines = DialogFormatter.alignLeft(rawLines);
        List<DialogBody> bodies = new ArrayList<>();
        for (String line : alignedLines) {
            bodies.add(DialogBody.plainMessage(MiniMessage.miniMessage().deserialize(line)));
        }

        ActionButton continueBtn = ActionButton.builder(Component.text("Continue"))
            .action(DialogAction.customClick((view, audience) -> {
                if (audience instanceof Player p) {
                    plugin.getUiManager().openStep2(p);
                }
            }, ClickCallback.Options.builder().uses(1).build()))
            .build();

        Dialog dialog = Dialog.create(builder -> builder.empty()
            .base(DialogBase.builder(Component.text("Welcome"))
                .canCloseWithEscape(false)
                .body(bodies)
                .build())
            .type(DialogType.notice(continueBtn))
        );

        player.showDialog(dialog);
    }

    /**
     * STEP 2: Cutscene Cinematic Offer Dialog
     */
    public void openStep2(Player player) {
        List<DialogBody> bodies = List.of(
            DialogBody.plainMessage(Component.text("Apakah anda ingin menonton cinematic perkenalan NaturalSchool?"))
        );

        ActionButton yesBtn = ActionButton.builder(Component.text("Tonton Sinematik"))
            .action(DialogAction.customClick((view, audience) -> {
                if (audience instanceof Player p) {
                    p.sendMessage(MiniMessage.miniMessage().deserialize("<yellow>Fitur cutscene mendatang!</yellow>"));
                    plugin.getUiManager().openStep3(p);
                }
            }, ClickCallback.Options.builder().uses(1).build()))
            .build();

        ActionButton noBtn = ActionButton.builder(Component.text("Lewati"))
            .action(DialogAction.customClick((view, audience) -> {
                if (audience instanceof Player p) {
                    plugin.getUiManager().openStep3(p);
                }
            }, ClickCallback.Options.builder().uses(1).build()))
            .build();

        Dialog dialog = Dialog.create(builder -> builder.empty()
            .base(DialogBase.builder(Component.text("Tonton Sinematik?"))
                .canCloseWithEscape(false)
                .body(bodies)
                .build())
            .type(DialogType.confirmation(yesBtn, noBtn))
        );

        player.showDialog(dialog);
    }

    /**
     * STEP 3: Terms of Service & Rules Agreement Dialog
     */
    public void openStep3(Player player) {
        List<DialogBody> bodies = List.of(
            DialogBody.plainMessage(Component.text("Untuk mulai bermain, anda harus menyetujui ToS dan Rules kami.")),
            DialogBody.plainMessage(MiniMessage.miniMessage().deserialize(
                "Silakan baca aturan di: <click:open_url:'https://naturalsmp.net'><underlined><aqua>https://naturalsmp.net</aqua></underlined></click>"
            ))
        );

        ActionButton agreeBtn = ActionButton.builder(Component.text("SAYA SETUJU & MASUK"))
            .action(DialogAction.customClick((view, audience) -> {
                if (audience instanceof Player p) {
                    boolean acceptTos = view.getBoolean("accept_tos");
                    boolean acceptRules = view.getBoolean("accept_rules");

                    if (acceptTos && acceptRules) {
                        plugin.getUiManager().completeRegistration(p);
                    } else {
                        p.sendActionBar(MiniMessage.miniMessage().deserialize(
                            "<red>Anda harus mencentang KEDUA persetujuan untuk dapat bermain!</red>"
                        ));
                        // Re-open Step 3
                        openStep3(p);
                    }
                }
            }, ClickCallback.Options.builder().uses(1).build()))
            .build();

        ActionButton declineBtn = ActionButton.builder(Component.text("TOLAK"))
            .action(DialogAction.customClick((view, audience) -> {
                if (audience instanceof Player p) {
                    p.kick(MiniMessage.miniMessage().deserialize(
                        "<red>Anda harus menyetujui ToS dan Rules untuk bermain di server ini!</red>"
                    ));
                }
            }, ClickCallback.Options.builder().uses(1).build()))
            .build();

        Dialog dialog = Dialog.create(builder -> builder.empty()
            .base(DialogBase.builder(Component.text("ToS & Rules Agreement"))
                .canCloseWithEscape(false)
                .body(bodies)
                .inputs(List.of(
                    DialogInput.bool("accept_tos", Component.text("Saya Menyetujui Terms Of Service"), false, "Yes", "No"),
                    DialogInput.bool("accept_rules", Component.text("Saya Menyetujui Rules Server"), false, "Yes", "No")
                ))
                .build())
            .type(DialogType.confirmation(agreeBtn, declineBtn))
        );

        player.showDialog(dialog);
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
            bodies.add(DialogBody.plainMessage(MiniMessage.miniMessage().deserialize(line)));
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
            bodies.add(DialogBody.plainMessage(MiniMessage.miniMessage().deserialize(line)));
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
