package id.naturalsmp.naturalSchool.ui.gui;

import id.naturalsmp.naturalSchool.NaturalSchool;
import id.naturalsmp.naturalSchool.profile.StudentProfile;
import id.naturalsmp.naturalSchool.ui.util.DialogFormatter;
import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;
import org.geysermc.cumulus.form.SimpleForm;
import org.geysermc.floodgate.api.FloodgateApi;

import java.util.ArrayList;
import java.util.List;

/**
 * GUI mandiri untuk halaman Profil Pelajar.
 * Berisi implementasi Java Edition (Dialog API) dan Bedrock Edition (Cumulus SimpleForm)
 * dalam satu file yang sama.
 */
public class ProfileGui {

    private final NaturalSchool plugin;

    public ProfileGui(NaturalSchool plugin) {
        this.plugin = plugin;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // JAVA EDITION — Paper Dialog API
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * [Java] Tampilkan profil pelajar sebagai Dialog notice.
     */
    public void openProfileJava(Player player) {
        StudentProfile profile       = plugin.getProfileManager().getProfile(player.getUniqueId());
        String username              = profile != null && profile.getUsername() != null ? profile.getUsername() : player.getName();
        String nis                   = (profile == null || profile.getNis() == null) ? "Belum Terdaftar" : profile.getNis();
        int    academicClass         = profile != null ? profile.getAcademicClass() : 0;
        String academicStage         = profile != null ? profile.getAcademicStage() : "NONE";
        String currentSemester       = profile != null ? profile.getCurrentSemester() : "GANJIL";
        String academicYear          = plugin.getSemesterManager().getCurrentAcademicYear();

        List<String> rawLines = List.of(
            "<aqua><bold>Informasi Pelajar</bold></aqua>",
            "<gray>Username:</gray> <white>" + username       + "</white>",
            "<gray>NIS:</gray> <white>"      + nis            + "</white>",
            "<gray>Kelas + Jenjang:</gray> <white>" + academicClass + " (" + academicStage + ")</white>",
            "<gray>Semester:</gray> <white>" + currentSemester + " (TA " + academicYear + ")</white>"
        );

        List<String> alignedLines = DialogFormatter.alignLeft(rawLines);
        List<DialogBody> bodies   = new ArrayList<>();
        for (String line : alignedLines) {
            bodies.add(DialogBody.plainMessage(MiniMessage.miniMessage().deserialize(line)));
        }

        Dialog dialog = Dialog.create(builder -> builder.empty()
            .base(DialogBase.builder(net.kyori.adventure.text.Component.text("Informasi Pelajar"))
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
     * [Bedrock] Tampilkan profil pelajar sebagai SimpleForm.
     */
    public void openProfileBedrock(Player player) {
        StudentProfile profile  = plugin.getProfileManager().getProfile(player.getUniqueId());
        String username         = profile != null && profile.getUsername() != null ? profile.getUsername() : player.getName();
        String nis              = (profile == null || profile.getNis() == null) ? "Belum Terdaftar" : profile.getNis();
        int    academicClass    = profile != null ? profile.getAcademicClass() : 0;
        String academicStage    = profile != null ? profile.getAcademicStage() : "NONE";
        String currentSemester  = profile != null ? profile.getCurrentSemester() : "GANJIL";
        String academicYear     = plugin.getSemesterManager().getCurrentAcademicYear();

        SimpleForm form = SimpleForm.builder()
            .title("Informasi Pelajar")
            .content(
                "Username: "       + username       + "\n" +
                "NIS: "            + nis            + "\n" +
                "Kelas + Jenjang: "+ academicClass  + " (" + academicStage + ")\n" +
                "Semester: "       + currentSemester + " (TA " + academicYear + ")"
            )
            .button("Tutup")
            .build();

        FloodgateApi.getInstance().sendForm(player.getUniqueId(), form);
    }
}
