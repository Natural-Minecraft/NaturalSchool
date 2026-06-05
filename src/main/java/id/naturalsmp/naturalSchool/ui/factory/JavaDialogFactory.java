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

import id.naturalsmp.naturalSchool.ui.ExamSession;
import id.naturalsmp.naturalSchool.ui.ExamQuestions;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.inventory.ItemStack;
import org.bukkit.Material;

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

    public void openExam1(Player player, boolean showMoreThanOneWarning) {
        List<DialogBody> bodies = new ArrayList<>();
        bodies.add(DialogBody.plainMessage(MiniMessage.miniMessage().deserialize("<gold><bold>Ujian Kelulusan - Sejarah</bold></gold>")));
        bodies.add(DialogBody.plainMessage(MiniMessage.miniMessage().deserialize("<gray>Pertanyaan:</gray> <white>Siapa pencipta NaturalSMP?</white>")));

        if (showMoreThanOneWarning) {
            bodies.add(DialogBody.plainMessage(MiniMessage.miniMessage().deserialize("<red><bold>Pilih hanya satu jawaban!</bold></red>")));
        } else {
            bodies.add(DialogBody.plainMessage(MiniMessage.miniMessage().deserialize("<yellow>Pilih satu jawaban yang benar di bawah ini:</yellow>")));
        }

        ActionButton submitBtn = ActionButton.builder(Component.text("Kirim Jawaban"))
            .action(DialogAction.customClick((view, audience) -> {
                if (audience instanceof Player p) {
                    boolean ansA = view.getBoolean("option_a");
                    boolean ansB = view.getBoolean("option_b");
                    boolean ansC = view.getBoolean("option_c");
                    boolean ansD = view.getBoolean("option_d");

                    int selectedCount = (ansA ? 1 : 0) + (ansB ? 1 : 0) + (ansC ? 1 : 0) + (ansD ? 1 : 0);

                    if (selectedCount > 1) {
                        plugin.getUiManager().openExam1(p, true);
                    } else if (selectedCount == 1 && ansC) {
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
            .base(DialogBase.builder(Component.text("Ujian 1: Pilihan Ganda"))
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

    public void openExam2(Player player) {
        List<DialogBody> bodies = List.of(
            DialogBody.plainMessage(MiniMessage.miniMessage().deserialize("<gold><bold>Ujian Kelulusan - Kebijakan</bold></gold>")),
            DialogBody.plainMessage(MiniMessage.miniMessage().deserialize("<gray>Pertanyaan:</gray> <white>Apakah 1 Semester di NaturalSchool sama dengan 14 hari real-life?</white>"))
        );

        ActionButton yesBtn = ActionButton.builder(Component.text("YA, Benar"))
            .action(DialogAction.customClick((view, audience) -> {
                if (audience instanceof Player p) {
                    p.sendTitle("§a§lJAWABAN BENAR", "§7Selamat, jawaban Anda benar!", 10, 70, 20);
                    p.playSound(p.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
                }
            }, ClickCallback.Options.builder().uses(1).build()))
            .build();

        ActionButton noBtn = ActionButton.builder(Component.text("TIDAK, Salah"))
            .action(DialogAction.customClick((view, audience) -> {
                if (audience instanceof Player p) {
                    p.sendTitle("§c§lJAWABAN SALAH", "§7Silakan baca panduan kembali!", 10, 70, 20);
                    p.playSound(p.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                }
            }, ClickCallback.Options.builder().uses(1).build()))
            .build();

        Dialog dialog = Dialog.create(builder -> builder.empty()
            .base(DialogBase.builder(Component.text("Ujian 2: Benar / Salah"))
                .canCloseWithEscape(true)
                .body(bodies)
                .build())
            .type(DialogType.confirmation(yesBtn, noBtn))
        );

        player.showDialog(dialog);
    }

    public void openExam3(Player player, boolean showMoreThanOneWarning) {
        List<DialogBody> bodies = new ArrayList<>();
        bodies.add(DialogBody.plainMessage(MiniMessage.miniMessage().deserialize("<gold><bold>Ujian Kelulusan - Matematika</bold></gold>")));
        bodies.add(DialogBody.plainMessage(MiniMessage.miniMessage().deserialize("<gray>Pertanyaan:</gray> <white>Jika 1 Semester = 14 hari, berapa hari untuk menyelesaikan 2 Semester?</white>")));

        if (showMoreThanOneWarning) {
            bodies.add(DialogBody.plainMessage(MiniMessage.miniMessage().deserialize("<red><bold>Pilih hanya satu jawaban!</bold></red>")));
        } else {
            bodies.add(DialogBody.plainMessage(MiniMessage.miniMessage().deserialize("<yellow>Pilih satu jawaban yang benar di bawah ini:</yellow>")));
        }

        ActionButton submitBtn = ActionButton.builder(Component.text("Kirim Jawaban"))
            .action(DialogAction.customClick((view, audience) -> {
                if (audience instanceof Player p) {
                    boolean ansA = view.getBoolean("option_a");
                    boolean ansB = view.getBoolean("option_b");
                    boolean ansC = view.getBoolean("option_c");
                    boolean ansD = view.getBoolean("option_d");

                    int selectedCount = (ansA ? 1 : 0) + (ansB ? 1 : 0) + (ansC ? 1 : 0) + (ansD ? 1 : 0);

                    if (selectedCount > 1) {
                        plugin.getUiManager().openExam3(p, true);
                    } else if (selectedCount == 1 && ansC) {
                        p.sendTitle("§a§lJAWABAN BENAR", "§7Selamat, jawaban Anda benar!", 10, 70, 20);
                        p.playSound(p.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
                    } else {
                        p.sendTitle("§c§lJAWABAN SALAH", "§7Coba hitung kembali ya!", 10, 70, 20);
                        p.playSound(p.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                    }
                }
            }, ClickCallback.Options.builder().uses(1).build()))
            .build();

        Dialog dialog = Dialog.create(builder -> builder.empty()
            .base(DialogBase.builder(Component.text("Ujian 3: Pilihan Ganda"))
                .canCloseWithEscape(true)
                .body(bodies)
                .inputs(List.of(
                    DialogInput.bool("option_a", Component.text("A. 7 Hari"), false, "Pilih", "Kosong"),
                    DialogInput.bool("option_b", Component.text("B. 14 Hari"), false, "Pilih", "Kosong"),
                    DialogInput.bool("option_c", Component.text("C. 28 Hari"), false, "Pilih", "Kosong"),
                    DialogInput.bool("option_d", Component.text("D. 30 Hari"), false, "Pilih", "Kosong")
                ))
                .build())
            .type(DialogType.notice(submitBtn))
        );

        player.showDialog(dialog);
    }

    public void openExam4(Player player) {
        List<DialogBody> bodies = List.of(
            DialogBody.plainMessage(MiniMessage.miniMessage().deserialize("<gold><bold>Ujian Kelulusan - Logika Kelas</bold></gold>")),
            DialogBody.plainMessage(MiniMessage.miniMessage().deserialize("<gray>Pertanyaan:</gray> <white>Manakah dari pernyataan berikut yang BENAR mengenai kenaikan kelas?</white>")),
            DialogBody.plainMessage(MiniMessage.miniMessage().deserialize("<yellow>Pilih semua pernyataan yang benar:</yellow>"))
        );

        ActionButton submitBtn = ActionButton.builder(Component.text("Kirim Jawaban"))
            .action(DialogAction.customClick((view, audience) -> {
                if (audience instanceof Player p) {
                    boolean stmt1 = view.getBoolean("stmt_1");
                    boolean stmt2 = view.getBoolean("stmt_2");
                    boolean stmt3 = view.getBoolean("stmt_3");

                    // stmt_1 and stmt_2 are correct, stmt_3 (auto-promotion without exam) is incorrect
                    if (stmt1 && stmt2 && !stmt3) {
                        p.sendTitle("§a§lJAWABAN BENAR", "§7Pemahaman logika Anda sangat baik!", 10, 70, 20);
                        p.playSound(p.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
                    } else {
                        p.sendTitle("§c§lJAWABAN SALAH", "§7Ada pernyataan salah yang Anda pilih!", 10, 70, 20);
                        p.playSound(p.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                    }
                }
            }, ClickCallback.Options.builder().uses(1).build()))
            .build();

        Dialog dialog = Dialog.create(builder -> builder.empty()
            .base(DialogBase.builder(Component.text("Ujian 4: Pernyataan Majemuk"))
                .canCloseWithEscape(true)
                .body(bodies)
                .inputs(List.of(
                    DialogInput.bool("stmt_1", Component.text("1. Kelas dikunci sebelum lulus ujian"), false, "Benar", "Salah"),
                    DialogInput.bool("stmt_2", Component.text("2. Semester otomatis berputar tiap 14 hari"), false, "Benar", "Salah"),
                    DialogInput.bool("stmt_3", Component.text("3. Kelas otomatis naik tanpa perlu ujian"), false, "Benar", "Salah")
                ))
                .build())
            .type(DialogType.notice(submitBtn))
        );

        player.showDialog(dialog);
    }

    public void openExam5(Player player, boolean showWarning) {
        List<DialogBody> bodies = new ArrayList<>();
        bodies.add(DialogBody.plainMessage(MiniMessage.miniMessage().deserialize("<gold><bold>Ujian Kelulusan - Pakta Integritas</bold></gold>")));
        bodies.add(DialogBody.plainMessage(MiniMessage.miniMessage().deserialize("<gray>Deklarasi Komitmen:</gray> <white>Harap setujui pakta integritas di bawah ini.</white>")));

        if (showWarning) {
            bodies.add(DialogBody.plainMessage(MiniMessage.miniMessage().deserialize("<red><bold>Anda wajib mencentang seluruh pakta integritas untuk melanjutkan!</bold></red>")));
        }

        ActionButton submitBtn = ActionButton.builder(Component.text("Kirim Jawaban"))
            .action(DialogAction.customClick((view, audience) -> {
                if (audience instanceof Player p) {
                    boolean agreeRules = view.getBoolean("agree_rules");
                    boolean agreeHonesty = view.getBoolean("agree_honesty");

                    if (agreeRules && agreeHonesty) {
                        p.sendTitle("§a§lJAWABAN BENAR", "§7Terima kasih atas komitmen Anda!", 10, 70, 20);
                        p.playSound(p.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
                    } else {
                        plugin.getUiManager().openExam5(p, true);
                    }
                }
            }, ClickCallback.Options.builder().uses(1).build()))
            .build();

        Dialog dialog = Dialog.create(builder -> builder.empty()
            .base(DialogBase.builder(Component.text("Ujian 5: Pakta Integritas"))
                .canCloseWithEscape(true)
                .body(bodies)
                .inputs(List.of(
                    DialogInput.bool("agree_rules", Component.text("Saya berjanji menaati seluruh peraturan server"), false, "Setuju", "Batal"),
                    DialogInput.bool("agree_honesty", Component.text("Saya menyatakan siap mengikuti ujian dengan jujur"), false, "Setuju", "Batal")
                ))
                .build())
            .type(DialogType.notice(submitBtn))
        );

        player.showDialog(dialog);
    }

    public void openExamPortal(Player player) {
        List<DialogBody> bodies = new ArrayList<>();
        bodies.add(DialogBody.item(new ItemStack(Material.BOOKSHELF)).build());
        bodies.add(DialogBody.plainMessage(MiniMessage.miniMessage().deserialize("<gold><bold>Portal Ujian</bold></gold>")));
        bodies.add(DialogBody.plainMessage(MiniMessage.miniMessage().deserialize(plugin.getExamMessage())));
        bodies.add(DialogBody.plainMessage(MiniMessage.miniMessage().deserialize("<yellow>Pilih mata pelajaran untuk diuji di bawah ini:</yellow>")));

        List<ActionButton> mapelButtons = new ArrayList<>();
        mapelButtons.add(createMapelButton("<aqua>Pengetahuan Umum</aqua>", "pengetahuan_umum"));
        mapelButtons.add(createMapelButton("<aqua>IPA</aqua>", "ipa"));
        mapelButtons.add(createMapelButton("<aqua>IPS</aqua>", "ips"));
        mapelButtons.add(createMapelButton("<aqua>Matematika (MTK)</aqua>", "mtk"));
        mapelButtons.add(createMapelButton("<aqua>Bahasa Indonesia</aqua>", "b_indo"));
        mapelButtons.add(createMapelButton("<aqua>PKN</aqua>", "pkn"));
        mapelButtons.add(createMapelButton("<aqua>Bahasa Inggris</aqua>", "b_inggris"));

        ActionButton closeBtn = ActionButton.builder(Component.text("Tutup Portal"))
            .action(DialogAction.customClick((view, audience) -> {
                // Returns to gameplay
            }, ClickCallback.Options.builder().uses(1).build()))
            .build();

        Dialog dialog = Dialog.create(builder -> builder.empty()
            .base(DialogBase.builder(Component.text("Portal Ujian"))
                .canCloseWithEscape(true)
                .body(bodies)
                .build())
            .type(DialogType.multiAction(mapelButtons, closeBtn, 2))
        );

        player.showDialog(dialog);
    }

    private ActionButton createMapelButton(String displayName, String subjectKey) {
        return ActionButton.builder(MiniMessage.miniMessage().deserialize(displayName))
            .action(DialogAction.customClick((view, audience) -> {
                if (audience instanceof Player p) {
                    plugin.getUiManager().startExamSession(p, subjectKey);
                    plugin.getUiManager().openExamQuestion1(p, subjectKey, false);
                }
            }, ClickCallback.Options.builder().uses(1).build()))
            .build();
    }

    public void openExamQuestion1(Player player, String subject, boolean showWarning) {
        ExamSession session = plugin.getUiManager().getExamSession(player.getUniqueId());
        if (session == null) {
            openExamPortal(player);
            return;
        }

        ExamQuestions.QuestionSet questions = ExamQuestions.getQuestions(subject);
        if (questions == null) {
            openExamPortal(player);
            return;
        }

        List<DialogBody> bodies = new ArrayList<>();
        bodies.add(DialogBody.item(new ItemStack(Material.BOOK)).build());
        bodies.add(DialogBody.plainMessage(MiniMessage.miniMessage().deserialize("<gold><bold>Ujian: " + ExamQuestions.getSubjectDisplayName(subject) + " (Soal 1/3)</bold></gold>")));
        bodies.add(DialogBody.plainMessage(MiniMessage.miniMessage().deserialize("<gray>Pertanyaan:</gray> <white>" + questions.q1Text + "</white>")));
        bodies.add(DialogBody.plainMessage(MiniMessage.miniMessage().deserialize("<yellow>Pilih salah satu tombol jawaban di bawah ini:</yellow>")));

        List<ActionButton> optionButtons = new ArrayList<>();
        optionButtons.add(createOptionButton(
            session.isAnsA() ? "<green><bold>● A. " + questions.q1A + " (Terpilih)</bold></green>" : "<gray>○ A. " + questions.q1A + "</gray>",
            subject, session, "A"
        ));
        optionButtons.add(createOptionButton(
            session.isAnsB() ? "<green><bold>● B. " + questions.q1B + " (Terpilih)</bold></green>" : "<gray>○ B. " + questions.q1B + "</gray>",
            subject, session, "B"
        ));
        optionButtons.add(createOptionButton(
            session.isAnsC() ? "<green><bold>● C. " + questions.q1C + " (Terpilih)</bold></green>" : "<gray>○ C. " + questions.q1C + "</gray>",
            subject, session, "C"
        ));
        optionButtons.add(createOptionButton(
            session.isAnsD() ? "<green><bold>● D. " + questions.q1D + " (Terpilih)</bold></green>" : "<gray>○ D. " + questions.q1D + "</gray>",
            subject, session, "D"
        ));

        ActionButton backBtn = ActionButton.builder(Component.text("Kembali ke Portal"))
            .action(DialogAction.customClick((view, audience) -> {
                if (audience instanceof Player p) {
                    plugin.getUiManager().clearExamSession(p);
                    plugin.getUiManager().openExamPortal(p);
                }
            }, ClickCallback.Options.builder().uses(1).build()))
            .build();

        Dialog dialog = Dialog.create(builder -> builder.empty()
            .base(DialogBase.builder(Component.text("Ujian - Soal 1"))
                .canCloseWithEscape(false)
                .body(bodies)
                .build())
            .type(DialogType.multiAction(optionButtons, backBtn, 2))
        );

        player.showDialog(dialog);
    }

    private ActionButton createOptionButton(String text, String subject, ExamSession session, String optChar) {
        return ActionButton.builder(MiniMessage.miniMessage().deserialize(text))
            .action(DialogAction.customClick((view, audience) -> {
                if (audience instanceof Player p) {
                    session.setAnsA("A".equalsIgnoreCase(optChar));
                    session.setAnsB("B".equalsIgnoreCase(optChar));
                    session.setAnsC("C".equalsIgnoreCase(optChar));
                    session.setAnsD("D".equalsIgnoreCase(optChar));
                    session.setCurrentQuestion(2);
                    plugin.getUiManager().openExamQuestion2(p, subject);
                }
            }, ClickCallback.Options.builder().uses(1).build()))
            .build();
    }

    public void openExamQuestion2(Player player, String subject) {
        ExamSession session = plugin.getUiManager().getExamSession(player.getUniqueId());
        if (session == null) {
            openExamPortal(player);
            return;
        }

        ExamQuestions.QuestionSet questions = ExamQuestions.getQuestions(subject);
        if (questions == null) {
            openExamPortal(player);
            return;
        }

        List<DialogBody> bodies = new ArrayList<>();
        bodies.add(DialogBody.item(new ItemStack(Material.COMPASS)).build());
        bodies.add(DialogBody.plainMessage(MiniMessage.miniMessage().deserialize("<gold><bold>Ujian: " + ExamQuestions.getSubjectDisplayName(subject) + " (Soal 2/3)</bold></gold>")));
        bodies.add(DialogBody.plainMessage(MiniMessage.miniMessage().deserialize("<gray>Pertanyaan (Benar / Salah):</gray> <white>" + questions.q2Text + "</white>")));
        bodies.add(DialogBody.plainMessage(MiniMessage.miniMessage().deserialize("<yellow>Pilih jawaban Anda:</yellow>")));

        List<ActionButton> tfButtons = new ArrayList<>();
        String trueText = (session.getTrueOrFalse() != null && session.getTrueOrFalse()) 
            ? "<green><bold>● BENAR (Terpilih)</bold></green>" 
            : "<gray>○ BENAR</gray>";
        String falseText = (session.getTrueOrFalse() != null && !session.getTrueOrFalse()) 
            ? "<red><bold>● SALAH (Terpilih)</bold></red>" 
            : "<gray>○ SALAH</gray>";

        tfButtons.add(createTrueFalseButton(trueText, true, subject, session));
        tfButtons.add(createTrueFalseButton(falseText, false, subject, session));

        ActionButton backBtn = ActionButton.builder(Component.text("Kembali ke Soal 1"))
            .action(DialogAction.customClick((view, audience) -> {
                if (audience instanceof Player p) {
                    session.setCurrentQuestion(1);
                    plugin.getUiManager().openExamQuestion1(p, subject, false);
                }
            }, ClickCallback.Options.builder().uses(1).build()))
            .build();

        Dialog dialog = Dialog.create(builder -> builder.empty()
            .base(DialogBase.builder(Component.text("Ujian - Soal 2"))
                .canCloseWithEscape(false)
                .body(bodies)
                .build())
            .type(DialogType.multiAction(tfButtons, backBtn, 2))
        );

        player.showDialog(dialog);
    }

    private ActionButton createTrueFalseButton(String text, boolean isTrue, String subject, ExamSession session) {
        return ActionButton.builder(MiniMessage.miniMessage().deserialize(text))
            .action(DialogAction.customClick((view, audience) -> {
                if (audience instanceof Player p) {
                    session.setTrueOrFalse(isTrue);
                    session.setCurrentQuestion(3);
                    plugin.getUiManager().openExamQuestion3(p, subject, false);
                }
            }, ClickCallback.Options.builder().uses(1).build()))
            .build();
    }

    public void openExamQuestion3(Player player, String subject, boolean showWarning) {
        ExamSession session = plugin.getUiManager().getExamSession(player.getUniqueId());
        if (session == null) {
            openExamPortal(player);
            return;
        }

        ExamQuestions.QuestionSet questions = ExamQuestions.getQuestions(subject);
        if (questions == null) {
            openExamPortal(player);
            return;
        }

        List<DialogBody> bodies = new ArrayList<>();
        bodies.add(DialogBody.item(new ItemStack(Material.PAPER)).build());
        bodies.add(DialogBody.plainMessage(MiniMessage.miniMessage().deserialize("<gold><bold>Ujian: " + ExamQuestions.getSubjectDisplayName(subject) + " (Soal 3/3)</bold></gold>")));
        bodies.add(DialogBody.plainMessage(MiniMessage.miniMessage().deserialize("<gray>Pertanyaan (Pernyataan Majemuk):</gray> <white>" + questions.q3Text + "</white>")));
        bodies.add(DialogBody.plainMessage(MiniMessage.miniMessage().deserialize("<yellow>Pilih semua pernyataan yang benar:</yellow>")));

        ActionButton nextBtn = ActionButton.builder(Component.text("Lanjut ke Konfirmasi"))
            .action(DialogAction.customClick((view, audience) -> {
                if (audience instanceof Player p) {
                    boolean stmt1 = view.getBoolean("stmt_1");
                    boolean stmt2 = view.getBoolean("stmt_2");
                    boolean stmt3 = view.getBoolean("stmt_3");

                    session.setStmt1(stmt1);
                    session.setStmt2(stmt2);
                    session.setStmt3(stmt3);
                    session.setCurrentQuestion(4);
                    plugin.getUiManager().openExamConfirmation(p, subject);
                }
            }, ClickCallback.Options.builder().uses(1).build()))
            .build();

        ActionButton backBtn = ActionButton.builder(Component.text("Kembali ke Soal 2"))
            .action(DialogAction.customClick((view, audience) -> {
                if (audience instanceof Player p) {
                    session.setCurrentQuestion(2);
                    plugin.getUiManager().openExamQuestion2(p, subject);
                }
            }, ClickCallback.Options.builder().uses(1).build()))
            .build();

        Dialog dialog = Dialog.create(builder -> builder.empty()
            .base(DialogBase.builder(Component.text("Ujian - Soal 3"))
                .canCloseWithEscape(false)
                .body(bodies)
                .inputs(List.of(
                    DialogInput.bool("stmt_1", Component.text("1. " + questions.q3Stmt1), session.isStmt1(), "Benar", "Salah"),
                    DialogInput.bool("stmt_2", Component.text("2. " + questions.q3Stmt2), session.isStmt2(), "Benar", "Salah"),
                    DialogInput.bool("stmt_3", Component.text("3. " + questions.q3Stmt3), session.isStmt3(), "Benar", "Salah")
                ))
                .build())
            .type(DialogType.confirmation(nextBtn, backBtn))
        );

        player.showDialog(dialog);
    }

    public void openExamConfirmation(Player player, String subject) {
        ExamSession session = plugin.getUiManager().getExamSession(player.getUniqueId());
        if (session == null) {
            openExamPortal(player);
            return;
        }

        List<DialogBody> bodies = List.of(
            DialogBody.item(new ItemStack(Material.WRITTEN_BOOK)).build(),
            DialogBody.plainMessage(MiniMessage.miniMessage().deserialize("<gold><bold>Portal Ujian: Konfirmasi Kirim</bold></gold>")),
            DialogBody.plainMessage(MiniMessage.miniMessage().deserialize("<yellow>Apakah Anda yakin ingin menyelesaikan ujian dan mengirimkan jawaban Anda sekarang?</yellow>"))
        );

        ActionButton confirmBtn = ActionButton.builder(Component.text("Kirim Jawaban"))
            .action(DialogAction.customClick((view, audience) -> {
                if (audience instanceof Player p) {
                    int[] score = ExamQuestions.evaluateExam(session);
                    p.sendTitle("§a§lUJIAN SELESAI", "§7Benar: §a" + score[0] + " §f| Salah: §c" + score[1], 20, 100, 20);
                    p.playSound(p.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
                    plugin.getUiManager().clearExamSession(p);
                }
            }, ClickCallback.Options.builder().uses(1).build()))
            .build();

        ActionButton backBtn = ActionButton.builder(Component.text("Kembali ke Soal 3"))
            .action(DialogAction.customClick((view, audience) -> {
                if (audience instanceof Player p) {
                    session.setCurrentQuestion(3);
                    plugin.getUiManager().openExamQuestion3(p, subject, false);
                }
            }, ClickCallback.Options.builder().uses(1).build()))
            .build();

        Dialog dialog = Dialog.create(builder -> builder.empty()
            .base(DialogBase.builder(Component.text("Konfirmasi Akhir"))
                .canCloseWithEscape(false)
                .body(bodies)
                .build())
            .type(DialogType.confirmation(confirmBtn, backBtn))
        );

        player.showDialog(dialog);
    }
}
