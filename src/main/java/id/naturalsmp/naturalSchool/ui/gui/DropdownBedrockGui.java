package id.naturalsmp.naturalSchool.ui.gui;

import id.naturalsmp.naturalSchool.NaturalSchool;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.Player;
import org.geysermc.cumulus.form.CustomForm;
import org.geysermc.floodgate.api.FloodgateApi;

/**
 * DropdownBedrockGui mereplikasi Portal Ujian Bedrock Edition menggunakan Dropdown
 * secara terpisah dari nol (from scratch).
 */
public class DropdownBedrockGui {

    private final NaturalSchool plugin;

    public DropdownBedrockGui(NaturalSchool plugin) {
        this.plugin = plugin;
    }

    public void open(Player player) {
        String examMessage = PlainTextComponentSerializer.plainText().serialize(
            MiniMessage.miniMessage().deserialize(plugin.getExamMessage())
        );

        CustomForm form = CustomForm.builder()
            .title("Portal Ujian (Dropdown Bedrock)")
            .label(examMessage + "\n\nPilih mata pelajaran Anda di bawah ini:")
            .dropdown("Mata Pelajaran",
                "Pengetahuan Umum",
                "IPA",
                "IPS",
                "Matematika (MTK)",
                "Bahasa Indonesia",
                "PKN",
                "Bahasa Inggris"
            )
            .validResultHandler(response -> {
                int selectedIndex = response.asDropdown(1); // Index 0 is label, Index 1 is dropdown
                if (selectedIndex < 0) return;
                String selectedSubject;
                switch (selectedIndex) {
                    case 0:  selectedSubject = "pengetahuan_umum"; break;
                    case 1:  selectedSubject = "ipa";              break;
                    case 2:  selectedSubject = "ips";              break;
                    case 3:  selectedSubject = "mtk";              break;
                    case 4:  selectedSubject = "b_indo";           break;
                    case 5:  selectedSubject = "pkn";              break;
                    case 6:  selectedSubject = "b_inggris";        break;
                    default: selectedSubject = "pengetahuan_umum"; break;
                }
                plugin.getUiManager().startExamSession(player, selectedSubject);
                plugin.getUiManager().openExamQuestion1(player, selectedSubject, false);
            })
            .build();

        FloodgateApi.getInstance().sendForm(player.getUniqueId(), form);
    }
}
