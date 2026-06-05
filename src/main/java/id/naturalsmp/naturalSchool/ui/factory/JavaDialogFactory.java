package id.naturalsmp.naturalSchool.ui.factory;

import id.naturalsmp.naturalSchool.NaturalSchool;
import id.naturalsmp.naturalSchool.ui.SchoolMenuType;
import id.naturalsmp.naturalSchool.profile.StudentProfile;
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
        id.naturalsmp.naturalSchool.profile.StudentProfile profile = plugin.getProfileManager().getProfile(player.getUniqueId());
        String nis = (profile == null || profile.getNis() == null) ? "Unregistered" : profile.getNis();
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
    public void openStep3(Player player, boolean showWarning) {
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
                    boolean acceptTos = view.getBoolean("accept_tos");
                    boolean acceptRules = view.getBoolean("accept_rules");

                    if (acceptTos && acceptRules) {
                        plugin.getUiManager().completeRegistration(p);
                    } else {
                        // Re-open Step 3 with warning
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
                    DialogInput.bool("accept_tos", Component.text("Saya Menyetujui Terms Of Service"), false, "Yes", "No"),
                    DialogInput.bool("accept_rules", Component.text("Saya Menyetujui Rules Server"), false, "Yes", "No")
                ))
                .build())
            .type(DialogType.notice(submitBtn))
        );

        player.showDialog(dialog);
    }

    private Dialog createProfileDialog(Player player) {
        StudentProfile profile = plugin.getProfileManager().getProfile(player.getUniqueId());
        String username = profile != null && profile.getUsername() != null ? profile.getUsername() : player.getName();
        String nis = (profile == null || profile.getNis() == null) ? "Belum Terdaftar" : profile.getNis();
        int academicClass = profile != null ? profile.getAcademicClass() : 0;
        String academicStage = profile != null ? profile.getAcademicStage() : "NONE";
        String currentSemester = profile != null ? profile.getCurrentSemester() : "GANJIL";
        String academicYear = plugin.getSemesterManager().getCurrentAcademicYear();

        List<String> rawLines = List.of(
            "<aqua><bold>Informasi Pelajar</bold></aqua>",
            "<gray>Username:</gray> <white>" + username + "</white>",
            "<gray>NIS:</gray> <white>" + nis + "</white>",
            "<gray>Kelas + Jenjang:</gray> <white>" + academicClass + " (" + academicStage + ")</white>",
            "<gray>Semester:</gray> <white>" + currentSemester + " (TA " + academicYear + ")</white>"
        );

        List<String> alignedLines = DialogFormatter.alignLeft(rawLines);
        List<DialogBody> bodies = new ArrayList<>();
        for (String line : alignedLines) {
            bodies.add(DialogBody.plainMessage(MiniMessage.miniMessage().deserialize(line)));
        }

        return Dialog.create(builder -> builder.empty()
            .base(DialogBase.builder(Component.text("Informasi Pelajar"))
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

    public void openTestExam(Player player) {
        List<DialogBody> bodies = List.of(
            DialogBody.plainMessage(MiniMessage.miniMessage().deserialize("<gold><bold>Ujian Kelulusan</bold></gold>")),
            DialogBody.plainMessage(MiniMessage.miniMessage().deserialize("<gray>Pertanyaan:</gray> <white>Siapa pencipta NaturalSMP?</white>")),
            DialogBody.plainMessage(MiniMessage.miniMessage().deserialize("<yellow>Pilih satu jawaban yang benar di bawah ini:</yellow>"))
        );

        ActionButton submitBtn = ActionButton.builder(Component.text("Kirim Jawaban"))
            .action(DialogAction.customClick((view, audience) -> {
                if (audience instanceof Player p) {
                    boolean ansA = view.getBoolean("option_a");
                    boolean ansB = view.getBoolean("option_b");
                    boolean ansC = view.getBoolean("option_c");
                    boolean ansD = view.getBoolean("option_d");

                    // Correct answer is ONLY Option C
                    if (ansC && !ansA && !ansB && !ansD) {
                        p.sendTitle("§a§lJAWABAN BENAR", "§7Selamat, kamu lulus!", 10, 70, 20);
                        p.playSound(p.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
                    } else {
                        p.sendTitle("§c§lJAWABAN SALAH", "§7Coba belajar lagi ya!", 10, 70, 20);
                        p.playSound(p.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                    }
                }
            }, ClickCallback.Options.builder().uses(1).build()))
            .build();

        Dialog dialog = Dialog.create(builder -> builder.empty()
            .base(DialogBase.builder(Component.text("Ujian: Pilihan Ganda"))
                .canCloseWithEscape(true)
                .body(bodies)
                .inputs(List.of(
                    DialogInput.bool("option_a", Component.text("A. Saya"), false, "Pilih", "Kosong"),
                    DialogInput.bool("option_b", Component.text("B. Jopeh"), false, "Pilih", "Kosong"),
                    DialogInput.bool("option_c", Component.text("C. AnakTentara"), false, "Pilih", "Kosong"),
                    DialogInput.bool("option_d", Component.text("D. Gua"), false, "Pilih", "Kosong")
                ))
                .build())
            .type(DialogType.notice(submitBtn))
        );

        player.showDialog(dialog);
    }
}
