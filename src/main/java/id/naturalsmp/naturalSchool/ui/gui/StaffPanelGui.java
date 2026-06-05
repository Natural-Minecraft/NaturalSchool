package id.naturalsmp.naturalSchool.ui.gui;

import id.naturalsmp.naturalSchool.NaturalSchool;
import id.naturalsmp.naturalSchool.ui.util.DialogFormatter;
import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;
import org.geysermc.cumulus.form.SimpleForm;
import org.geysermc.floodgate.api.FloodgateApi;

import java.util.ArrayList;
import java.util.List;

/**
 * GUI mandiri untuk Staff Panel administrator.
 * Berisi implementasi Java Edition (Dialog API) dan Bedrock Edition (Cumulus SimpleForm)
 * dalam satu file yang sama.
 */
public class StaffPanelGui {

    private final NaturalSchool plugin;

    public StaffPanelGui(NaturalSchool plugin) {
        this.plugin = plugin;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // JAVA EDITION — Paper Dialog API
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * [Java] Tampilkan Staff Panel sebagai Dialog notice.
     */
    public void openStaffPanelJava(Player player) {
        List<String> rawLines = List.of(
            "<red><bold>Staff Panel</bold></red>",
            "<gray>Welcome Administrator.</gray>",
            "<gray>Manage registered students here.</gray>",
            "<yellow>Access level: High</yellow>"
        );

        List<String> alignedLines = DialogFormatter.alignLeft(rawLines);
        List<DialogBody> bodies   = new ArrayList<>();
        for (String line : alignedLines) {
            bodies.add(DialogBody.plainMessage(MiniMessage.miniMessage().deserialize(line)));
        }

        Dialog dialog = Dialog.create(builder -> builder.empty()
            .base(DialogBase.builder(Component.text("Staff Panel"))
                .canCloseWithEscape(true)
                .body(bodies)
                .build())
            .type(DialogType.notice())
        );

        player.showDialog(dialog);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // BEDROCK EDITION — Geyser/Floodgate Cumulus SimpleForm
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * [Bedrock] Tampilkan Staff Panel sebagai SimpleForm.
     */
    public void openStaffPanelBedrock(Player player) {
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
