package id.naturalsmp.naturalSchool.ui.gui;

import id.naturalsmp.naturalSchool.NaturalSchool;
import id.naturalsmp.naturalSchool.profile.StudentProfile;
import id.naturalsmp.naturalSchool.ui.util.DialogFormatter;
import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.input.DialogInput;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickCallback;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;
import org.geysermc.cumulus.form.CustomForm;
import org.geysermc.floodgate.api.FloodgateApi;

import java.util.ArrayList;
import java.util.List;

/**
 * GUI mandiri untuk alur registrasi NaturalSchool (Step 1 s/d 3).
 * Berisi implementasi Java Edition (Dialog API) dan Bedrock Edition (Cumulus CustomForm)
 * dalam satu file yang sama.
 */
public class RegistrationGui {

    private final NaturalSchool plugin;

    public RegistrationGui(NaturalSchool plugin) {
        this.plugin = plugin;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // JAVA EDITION — Paper Dialog API
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * [Java] STEP 1: Welcome & Info Dialog
     */
    public void openStep1Java(Player player) {
        StudentProfile profile = plugin.getProfileManager().getProfile(player.getUniqueId());
        String nis    = (profile == null || profile.getNis() == null) ? "Unregistered" : profile.getNis();
        String status = (profile == null || profile.getNis() == null) ? "Belum Terdaftar" : "Terdaftar";

        List<String> rawLines = List.of(
            "<green><bold>NaturalSchool Onboarding</bold></green>",
            "<gray>Welcome, " + player.getName() + "!</gray>",
            "Username: " + player.getName(),
            "NIS: " + nis,
            "Status: " + status
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
     * [Java] STEP 2: Cutscene Cinematic Offer Dialog
     */
    public void openStep2Java(Player player) {
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
     * [Java] STEP 3: Terms of Service & Rules Agreement Dialog
     */
    public void openStep3Java(Player player, boolean showWarning) {
        List<DialogBody> bodies = new ArrayList<>();
        bodies.add(DialogBody.plainMessage(Component.text("Untuk mulai bermain, anda harus menyetujui ToS dan Rules kami.")));
        bodies.add(DialogBody.plainMessage(MiniMessage.miniMessage().deserialize(
            "Silakan baca aturan di: <click:open_url:'https://naturalsmp.net'><underlined><aqua>https://naturalsmp.net</aqua></underlined></click>"
        )));

        if (showWarning) {
            bodies.add(DialogBody.plainMessage(MiniMessage.miniMessage().deserialize(
                "<red><bold>Anda wajib menyetujui untuk bermain</bold></red>"
            )));
        }

        ActionButton submitBtn = ActionButton.builder(Component.text("Submit"))
            .action(DialogAction.customClick((view, audience) -> {
                if (audience instanceof Player p) {
                    boolean acceptTos   = view.getBoolean("accept_tos");
                    boolean acceptRules = view.getBoolean("accept_rules");

                    if (acceptTos && acceptRules) {
                        plugin.getUiManager().completeRegistration(p);
                    } else {
                        plugin.getUiManager().openStep3(p, true);
                    }
                }
            }, ClickCallback.Options.builder().uses(1).build()))
            .build();

        Dialog dialog = Dialog.create(builder -> builder.empty()
            .base(DialogBase.builder(Component.text("ToS & Rules Agreement"))
                .canCloseWithEscape(false)
                .body(bodies)
                .inputs(List.of(
                    DialogInput.bool("accept_tos",   Component.text("Saya Menyetujui Terms Of Service"), false, "Yes", "No"),
                    DialogInput.bool("accept_rules", Component.text("Saya Menyetujui Rules Server"),     false, "Yes", "No")
                ))
                .build())
            .type(DialogType.notice(submitBtn))
        );

        player.showDialog(dialog);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // BEDROCK EDITION — Geyser/Floodgate Cumulus CustomForm
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * [Bedrock] STEP 1: Welcome & Info CustomForm
     */
    public void openStep1Bedrock(Player player) {
        StudentProfile profile = plugin.getProfileManager().getProfile(player.getUniqueId());
        String nis    = (profile == null || profile.getNis() == null) ? "Unregistered" : profile.getNis();
        String status = (profile == null || profile.getNis() == null) ? "Belum Terdaftar" : "Terdaftar";

        CustomForm form = CustomForm.builder()
            .title("NaturalSchool Onboarding")
            .label("Welcome, " + player.getName() + "!\n\n" +
                "Username: " + player.getName() + "\n" +
                "NIS: "      + nis              + "\n" +
                "Status: "   + status           + "\n\n" +
                "Silakan klik Submit untuk melanjutkan.")
            .validResultHandler(response -> {
                plugin.getUiManager().openStep2(player);
            })
            .closedResultHandler(() -> openStep1Bedrock(player))
            .build();

        FloodgateApi.getInstance().sendForm(player.getUniqueId(), form);
    }

    /**
     * [Bedrock] STEP 2: Cutscene Cinematic Offer CustomForm
     */
    public void openStep2Bedrock(Player player) {
        CustomForm form = CustomForm.builder()
            .title("Tonton Cinematic?")
            .label("Apakah anda ingin menonton cinematic perkenalan NaturalSchool?")
            .toggle("Tonton Sinematik", false)
            .validResultHandler(response -> {
                boolean watchCinematic = response.asToggle(1);
                if (watchCinematic) {
                    player.sendMessage(MiniMessage.miniMessage().deserialize("<yellow>Fitur cutscene mendatang!</yellow>"));
                }
                plugin.getUiManager().openStep3(player);
            })
            .closedResultHandler(() -> openStep2Bedrock(player))
            .build();

        FloodgateApi.getInstance().sendForm(player.getUniqueId(), form);
    }

    /**
     * [Bedrock] STEP 3: ToS & Rules Agreement CustomForm
     */
    public void openStep3Bedrock(Player player, boolean showWarning) {
        CustomForm.Builder builder = CustomForm.builder()
            .title("ToS & Rules Agreement");

        if (showWarning) {
            builder.label("§c§lAnda wajib menyetujui untuk bermain");
        }

        builder.label("Untuk mulai bermain, anda harus menyetujui ToS dan Rules kami.\nAturan di: https://naturalsmp.net")
            .toggle("Saya Menyetujui Terms Of Service", false)
            .toggle("Saya Menyetujui Rules Server",     false);

        CustomForm form = builder
            .validResultHandler(response -> {
                boolean acceptTos   = response.asToggle(0);
                boolean acceptRules = response.asToggle(1);

                if (acceptTos && acceptRules) {
                    plugin.getUiManager().completeRegistration(player);
                } else {
                    plugin.getUiManager().openStep3(player, true);
                }
            })
            .closedResultHandler(() -> openStep3Bedrock(player, showWarning))
            .build();

        FloodgateApi.getInstance().sendForm(player.getUniqueId(), form);
    }
}
