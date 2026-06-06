package id.naturalsmp.naturalSchool.ui.gui;

import id.naturalsmp.naturalSchool.NaturalSchool;
import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.input.DialogInput;
import io.papermc.paper.registry.data.dialog.input.SingleOptionDialogInput;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickCallback;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * DropdownJavaGui mereplikasi Portal Ujian Java Edition menggunakan Dropdown
 * secara terpisah dari nol (from scratch).
 */
public class DropdownJavaGui {

    private final NaturalSchool plugin;

    public DropdownJavaGui(NaturalSchool plugin) {
        this.plugin = plugin;
    }

    public void open(Player player) {
        List<DialogBody> bodies = new ArrayList<>();
        bodies.add(DialogBody.item(new ItemStack(Material.BOOKSHELF)).build());
        bodies.add(DialogBody.plainMessage(MiniMessage.miniMessage().deserialize("<gold><bold>Portal Ujian (Dropdown Java)</bold></gold>")));
        bodies.add(DialogBody.plainMessage(MiniMessage.miniMessage().deserialize(plugin.getExamMessage())));
        bodies.add(DialogBody.plainMessage(MiniMessage.miniMessage().deserialize("<yellow>Pilih mata pelajaran Anda di bawah ini:</yellow>")));

        List<SingleOptionDialogInput.OptionEntry> entries = List.of(
            SingleOptionDialogInput.OptionEntry.create("pengetahuan_umum", Component.text("Pengetahuan Umum"), false),
            SingleOptionDialogInput.OptionEntry.create("ipa", Component.text("IPA"), false),
            SingleOptionDialogInput.OptionEntry.create("ips", Component.text("IPS"), false),
            SingleOptionDialogInput.OptionEntry.create("mtk", Component.text("Matematika (MTK)"), false),
            SingleOptionDialogInput.OptionEntry.create("b_indo", Component.text("Bahasa Indonesia"), false),
            SingleOptionDialogInput.OptionEntry.create("pkn", Component.text("PKN"), false),
            SingleOptionDialogInput.OptionEntry.create("b_inggris", Component.text("Bahasa Inggris"), false)
        );

        ActionButton submitBtn = ActionButton.builder(Component.text("Mulai Ujian"))
            .action(DialogAction.customClick((view, audience) -> {
                if (audience instanceof Player p) {
                    String selectedSubject = view.getText("subject");
                    if (selectedSubject != null && !selectedSubject.isEmpty()) {
                        plugin.getUiManager().startExamSession(p, selectedSubject);
                        plugin.getUiManager().openExamQuestion1(p, selectedSubject, false);
                    }
                }
            }, ClickCallback.Options.builder().uses(1).build()))
            .build();

        ActionButton closeBtn = ActionButton.builder(Component.text("Tutup"))
            .action(DialogAction.customClick((view, audience) -> {
                // Tutup
            }, ClickCallback.Options.builder().uses(1).build()))
            .build();

        Dialog dialog = Dialog.create(builder -> builder.empty()
            .base(DialogBase.builder(Component.text("Pilih Ujian"))
                .canCloseWithEscape(true)
                .body(bodies)
                .inputs(List.of(
                    DialogInput.singleOption("subject", Component.text("Mata Pelajaran"), entries).build()
                ))
                .build())
            .type(DialogType.confirmation(submitBtn, closeBtn))
        );

        player.showDialog(dialog);
    }
}
