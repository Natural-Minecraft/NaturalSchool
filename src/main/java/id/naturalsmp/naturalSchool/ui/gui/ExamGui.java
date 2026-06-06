package id.naturalsmp.naturalSchool.ui.gui;

import id.naturalsmp.naturalSchool.NaturalSchool;
import id.naturalsmp.naturalSchool.ui.ExamQuestions;
import id.naturalsmp.naturalSchool.ui.ExamSession;
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
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.geysermc.cumulus.form.CustomForm;
import org.geysermc.cumulus.form.SimpleForm;
import org.geysermc.floodgate.api.FloodgateApi;

import java.util.ArrayList;
import java.util.List;

/**
 * GUI mandiri untuk alur Ujian utama (/school exam).
 * Mencakup: Portal Ujian, Soal 1 (MCQ), Soal 2 (True/False), Soal 3 (Checklist),
 * Konfirmasi Kirim, dan Portal Ditutup.
 *
 * Berisi implementasi Java Edition (Dialog API) dan Bedrock Edition (Cumulus Form)
 * dalam satu file yang sama.
 */
public class ExamGui {

    public static final String GUI_VERSION = "1.5.4";

    private final NaturalSchool plugin;

    public ExamGui(NaturalSchool plugin) {
        this.plugin = plugin;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // JAVA EDITION — Paper Dialog API
    // ─────────────────────────────────────────────────────────────────────────

    /** [Java] Portal Ujian — pilih mata pelajaran. */
    public void openExamPortalJava(Player player) {
        List<DialogBody> bodies = new ArrayList<>();
        bodies.add(DialogBody.item(new ItemStack(Material.BOOKSHELF)).build());
        bodies.add(DialogBody.plainMessage(MiniMessage.miniMessage().deserialize("<gold><bold>Portal Ujian</bold></gold>")));
        bodies.add(DialogBody.plainMessage(MiniMessage.miniMessage().deserialize(plugin.getExamMessage())));
        bodies.add(DialogBody.plainMessage(MiniMessage.miniMessage().deserialize("<yellow>Pilih mata pelajaran untuk diuji di bawah ini:</yellow>")));

        List<ActionButton> mapelButtons = new ArrayList<>();
        mapelButtons.add(createMapelButton("<aqua>Pengetahuan Umum</aqua>", "pengetahuan_umum"));
        mapelButtons.add(createMapelButton("<aqua>IPA</aqua>",              "ipa"));
        mapelButtons.add(createMapelButton("<aqua>IPS</aqua>",              "ips"));
        mapelButtons.add(createMapelButton("<aqua>Matematika (MTK)</aqua>", "mtk"));
        mapelButtons.add(createMapelButton("<aqua>Bahasa Indonesia</aqua>", "b_indo"));
        mapelButtons.add(createMapelButton("<aqua>PKN</aqua>",              "pkn"));
        mapelButtons.add(createMapelButton("<aqua>Bahasa Inggris</aqua>",   "b_inggris"));

        ActionButton closeBtn = ActionButton.builder(Component.text("Tutup Portal"))
            .action(DialogAction.customClick((view, audience) -> {
                // Kembali ke gameplay
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

    /** [Java] Soal 1 — Multiple Choice (pilih satu, stateful). */
    public void openExamQuestion1Java(Player player, String subject, boolean showWarning) {
        ExamSession session = plugin.getUiManager().getExamSession(player.getUniqueId());
        if (session == null) { openExamPortalJava(player); return; }

        ExamQuestions.QuestionSet questions = ExamQuestions.getQuestions(subject);
        if (questions == null) { openExamPortalJava(player); return; }

        List<DialogBody> bodies = new ArrayList<>();
        bodies.add(DialogBody.item(new ItemStack(Material.BOOK)).build());
        bodies.add(DialogBody.plainMessage(MiniMessage.miniMessage().deserialize("<gold><bold>Ujian: " + ExamQuestions.getSubjectDisplayName(subject) + " (Soal 1/3)</bold></gold>")));
        bodies.add(DialogBody.plainMessage(MiniMessage.miniMessage().deserialize("<gray>Pertanyaan:</gray> <white>" + questions.q1Text + "</white>")));
        bodies.add(DialogBody.plainMessage(MiniMessage.miniMessage().deserialize("<yellow>Pilih salah satu tombol jawaban di bawah ini:</yellow>")));

        List<ActionButton> optionButtons = new ArrayList<>();
        optionButtons.add(createOptionButton(
            session.isAnsA() ? "<green><bold>A. " + questions.q1A + " (Terpilih)</bold></green>" : "A. " + questions.q1A,
            subject, session, "A"
        ));
        optionButtons.add(createOptionButton(
            session.isAnsB() ? "<green><bold>B. " + questions.q1B + " (Terpilih)</bold></green>" : "B. " + questions.q1B,
            subject, session, "B"
        ));
        optionButtons.add(createOptionButton(
            session.isAnsC() ? "<green><bold>C. " + questions.q1C + " (Terpilih)</bold></green>" : "C. " + questions.q1C,
            subject, session, "C"
        ));
        optionButtons.add(createOptionButton(
            session.isAnsD() ? "<green><bold>D. " + questions.q1D + " (Terpilih)</bold></green>" : "D. " + questions.q1D,
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

    /** [Java] Soal 2 — True / False (stateful). */
    public void openExamQuestion2Java(Player player, String subject) {
        ExamSession session = plugin.getUiManager().getExamSession(player.getUniqueId());
        if (session == null) { openExamPortalJava(player); return; }

        ExamQuestions.QuestionSet questions = ExamQuestions.getQuestions(subject);
        if (questions == null) { openExamPortalJava(player); return; }

        List<DialogBody> bodies = new ArrayList<>();
        bodies.add(DialogBody.item(new ItemStack(Material.COMPASS)).build());
        bodies.add(DialogBody.plainMessage(MiniMessage.miniMessage().deserialize("<gold><bold>Ujian: " + ExamQuestions.getSubjectDisplayName(subject) + " (Soal 2/3)</bold></gold>")));
        bodies.add(DialogBody.plainMessage(MiniMessage.miniMessage().deserialize("<gray>Pertanyaan (Benar / Salah):</gray> <white>" + questions.q2Text + "</white>")));
        bodies.add(DialogBody.plainMessage(MiniMessage.miniMessage().deserialize("<yellow>Pilih jawaban Anda:</yellow>")));

        String trueText  = (session.getTrueOrFalse() != null && session.getTrueOrFalse())
            ? "<green><bold>BENAR (Terpilih)</bold></green>" : "BENAR";
        String falseText = (session.getTrueOrFalse() != null && !session.getTrueOrFalse())
            ? "<red><bold>SALAH (Terpilih)</bold></red>"   : "SALAH";

        List<ActionButton> tfButtons = new ArrayList<>();
        tfButtons.add(createTrueFalseButton(trueText,  true,  subject, session));
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

    /** [Java] Soal 3 — Multiple Statement Checklist (stateful boolean inputs). */
    public void openExamQuestion3Java(Player player, String subject, boolean showWarning) {
        ExamSession session = plugin.getUiManager().getExamSession(player.getUniqueId());
        if (session == null) { openExamPortalJava(player); return; }

        ExamQuestions.QuestionSet questions = ExamQuestions.getQuestions(subject);
        if (questions == null) { openExamPortalJava(player); return; }

        List<DialogBody> bodies = new ArrayList<>();
        bodies.add(DialogBody.item(new ItemStack(Material.PAPER)).build());
        bodies.add(DialogBody.plainMessage(MiniMessage.miniMessage().deserialize("<gold><bold>Ujian: " + ExamQuestions.getSubjectDisplayName(subject) + " (Soal 3/3)</bold></gold>")));
        bodies.add(DialogBody.plainMessage(MiniMessage.miniMessage().deserialize("<gray>Pertanyaan (Pernyataan Majemuk):</gray> <white>" + questions.q3Text + "</white>")));
        bodies.add(DialogBody.plainMessage(MiniMessage.miniMessage().deserialize("<yellow>Pilih semua pernyataan yang benar:</yellow>")));

        ActionButton nextBtn = ActionButton.builder(Component.text("Lanjut ke Konfirmasi"))
            .action(DialogAction.customClick((view, audience) -> {
                if (audience instanceof Player p) {
                    session.setStmt1(view.getBoolean("stmt_1"));
                    session.setStmt2(view.getBoolean("stmt_2"));
                    session.setStmt3(view.getBoolean("stmt_3"));
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

    /** [Java] Konfirmasi pengiriman jawaban. */
    public void openExamConfirmationJava(Player player, String subject) {
        ExamSession session = plugin.getUiManager().getExamSession(player.getUniqueId());
        if (session == null) { openExamPortalJava(player); return; }

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

    /** [Java] Portal Ditutup — notice dengan pesan admin. */
    public void openExamClosedJava(Player player) {
        List<DialogBody> bodies = new ArrayList<>();
        bodies.add(DialogBody.item(new ItemStack(Material.BARRIER)).build());
        bodies.add(DialogBody.plainMessage(MiniMessage.miniMessage().deserialize("<red><bold>Portal Sedang ditutup!</bold></red>")));
        bodies.add(DialogBody.plainMessage(MiniMessage.miniMessage().deserialize(plugin.getExamMessage())));

        ActionButton closeBtn = ActionButton.builder(Component.text("Tutup"))
            .action(DialogAction.customClick((view, audience) -> {
                // Kembali ke gameplay
            }, ClickCallback.Options.builder().uses(1).build()))
            .build();

        Dialog dialog = Dialog.create(builder -> builder.empty()
            .base(DialogBase.builder(Component.text("Portal Ditutup"))
                .canCloseWithEscape(true)
                .body(bodies)
                .build())
            .type(DialogType.notice(closeBtn))
        );

        player.showDialog(dialog);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // BEDROCK EDITION — Geyser/Floodgate Cumulus Form
    // ─────────────────────────────────────────────────────────────────────────

    /** [Bedrock] Portal Ujian — pilih mata pelajaran (SimpleForm). */
    public void openExamPortalBedrock(Player player) {
        String examMessage = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(
            MiniMessage.miniMessage().deserialize(plugin.getExamMessage())
        );

        SimpleForm form = SimpleForm.builder()
            .title("Portal Ujian")
            .content(examMessage + "\n\nPilih mata pelajaran untuk diuji di bawah ini:")
            .button("Pengetahuan Umum")
            .button("IPA")
            .button("IPS")
            .button("Matematika (MTK)")
            .button("Bahasa Indonesia")
            .button("PKN")
            .button("Bahasa Inggris")
            .button("Tutup Portal")
            .validResultHandler(response -> {
                int clickedId = response.clickedButtonId();
                if (clickedId == 7) {
                    return; // Tutup portal
                }
                String selectedSubject;
                switch (clickedId) {
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

    /** [Bedrock] Soal 1 — MCQ (SimpleForm, button hijau §a untuk pilihan terpilih). */
    public void openExamQuestion1Bedrock(Player player, String subject, boolean showWarning) {
        ExamSession session = plugin.getUiManager().getExamSession(player.getUniqueId());
        if (session == null) { openExamPortalBedrock(player); return; }

        ExamQuestions.QuestionSet questions = ExamQuestions.getQuestions(subject);
        if (questions == null) { openExamPortalBedrock(player); return; }

        // Selected = §a (hijau). Unselected = plain text default.
        String btnA = (session.isAnsA() ? "\u00a7aA. " : "A. ") + questions.q1A;
        String btnB = (session.isAnsB() ? "\u00a7aB. " : "B. ") + questions.q1B;
        String btnC = (session.isAnsC() ? "\u00a7aC. " : "C. ") + questions.q1C;
        String btnD = (session.isAnsD() ? "\u00a7aD. " : "D. ") + questions.q1D;

        SimpleForm form = SimpleForm.builder()
            .title("Ujian: Soal 1/3")
            .content("Mata Pelajaran: " + ExamQuestions.getSubjectDisplayName(subject) +
                "\n\nPertanyaan:\n" + questions.q1Text + "\n\nPilih salah satu jawaban di bawah ini:")
            .button(btnA)
            .button(btnB)
            .button(btnC)
            .button(btnD)
            .button("Kembali ke Portal")
            .validResultHandler(response -> {
                int clickedId = response.clickedButtonId();
                if (clickedId == 4) {
                    plugin.getUiManager().clearExamSession(player);
                    plugin.getUiManager().openExamPortal(player);
                    return;
                }
                session.setAnsA(clickedId == 0);
                session.setAnsB(clickedId == 1);
                session.setAnsC(clickedId == 2);
                session.setAnsD(clickedId == 3);
                session.setCurrentQuestion(2);
                plugin.getUiManager().openExamQuestion2(player, subject);
            })
            .closedResultHandler(() -> openExamQuestion1Bedrock(player, subject, showWarning))
            .build();

        FloodgateApi.getInstance().sendForm(player.getUniqueId(), form);
    }

    /** [Bedrock] Soal 2 — True / False (SimpleForm, button hijau §a untuk pilihan terpilih). */
    public void openExamQuestion2Bedrock(Player player, String subject) {
        ExamSession session = plugin.getUiManager().getExamSession(player.getUniqueId());
        if (session == null) { openExamPortalBedrock(player); return; }

        ExamQuestions.QuestionSet questions = ExamQuestions.getQuestions(subject);
        if (questions == null) { openExamPortalBedrock(player); return; }

        String trueBtn  = (session.getTrueOrFalse() != null && session.getTrueOrFalse())  ? "\u00a7aBENAR" : "BENAR";
        String falseBtn = (session.getTrueOrFalse() != null && !session.getTrueOrFalse()) ? "\u00a7aSALAH" : "SALAH";

        SimpleForm form = SimpleForm.builder()
            .title("Ujian: Soal 2/3")
            .content("Mata Pelajaran: " + ExamQuestions.getSubjectDisplayName(subject) +
                "\n\nPertanyaan (Benar / Salah):\n" + questions.q2Text + "\n\nPilih jawaban Anda:")
            .button(trueBtn)
            .button(falseBtn)
            .button("Kembali ke Soal 1")
            .validResultHandler(response -> {
                int clickedId = response.clickedButtonId();
                if (clickedId == 2) {
                    session.setCurrentQuestion(1);
                    plugin.getUiManager().openExamQuestion1(player, subject, false);
                    return;
                }
                session.setTrueOrFalse(clickedId == 0);
                session.setCurrentQuestion(3);
                plugin.getUiManager().openExamQuestion3(player, subject, false);
            })
            .closedResultHandler(() -> openExamQuestion2Bedrock(player, subject))
            .build();

        FloodgateApi.getInstance().sendForm(player.getUniqueId(), form);
    }

    /**
     * [Bedrock] Soal 3 — Multiple Statement Checklist (SimpleForm, 3 toggle button + 2 navigasi).
     * Klik pernyataan untuk toggle §a/plain. "Berikutnya" untuk konfirmasi.
     */
    public void openExamQuestion3Bedrock(Player player, String subject, boolean showWarning) {
        ExamSession session = plugin.getUiManager().getExamSession(player.getUniqueId());
        if (session == null) { openExamPortalBedrock(player); return; }

        ExamQuestions.QuestionSet questions = ExamQuestions.getQuestions(subject);
        if (questions == null) { openExamPortalBedrock(player); return; }

        // Selected = §a (hijau). Unselected = plain text.
        String btnStmt1 = (session.isStmt1() ? "\u00a7a1. " : "1. ") + questions.q3Stmt1;
        String btnStmt2 = (session.isStmt2() ? "\u00a7a2. " : "2. ") + questions.q3Stmt2;
        String btnStmt3 = (session.isStmt3() ? "\u00a7a3. " : "3. ") + questions.q3Stmt3;

        SimpleForm form = SimpleForm.builder()
            .title("Ujian: Soal 3/3")
            .content("Mata Pelajaran: " + ExamQuestions.getSubjectDisplayName(subject) +
                "\n\nPertanyaan (Pernyataan Majemuk):\n" + questions.q3Text +
                "\n\nKlik pernyataan untuk mencentang/membatalkan centang, lalu pilih 'Berikutnya' jika sudah selesai:")
            .button(btnStmt1)
            .button(btnStmt2)
            .button(btnStmt3)
            .button("Berikutnya")
            .button("Kembali ke Soal 2")
            .validResultHandler(response -> {
                int clickedId = response.clickedButtonId();
                if (clickedId == 4) {
                    session.setCurrentQuestion(2);
                    plugin.getUiManager().openExamQuestion2(player, subject);
                    return;
                }
                if (clickedId == 3) {
                    session.setCurrentQuestion(4);
                    plugin.getUiManager().openExamConfirmation(player, subject);
                    return;
                }
                // Toggle checklist state
                if (clickedId == 0) session.setStmt1(!session.isStmt1());
                else if (clickedId == 1) session.setStmt2(!session.isStmt2());
                else if (clickedId == 2) session.setStmt3(!session.isStmt3());

                // Refresh form untuk tampilkan state terbaru
                openExamQuestion3Bedrock(player, subject, showWarning);
            })
            .closedResultHandler(() -> openExamQuestion3Bedrock(player, subject, showWarning))
            .build();

        FloodgateApi.getInstance().sendForm(player.getUniqueId(), form);
    }

    /** [Bedrock] Konfirmasi pengiriman jawaban (CustomForm). */
    public void openExamConfirmationBedrock(Player player, String subject) {
        ExamSession session = plugin.getUiManager().getExamSession(player.getUniqueId());
        if (session == null) { openExamPortalBedrock(player); return; }

        CustomForm form = CustomForm.builder()
            .title("Konfirmasi Akhir")
            .label("Apakah Anda yakin ingin menyelesaikan ujian dan mengirimkan jawaban Anda sekarang?")
            .dropdown("Pilih Tindakan",
                "Kirim Jawaban",
                "Kembali ke Soal 3"
            )
            .validResultHandler(response -> {
                int selectedIndex = response.asDropdown(1); // Index 0 is label, Index 1 is dropdown
                if (selectedIndex == 0) {
                    int[] score = ExamQuestions.evaluateExam(session);
                    player.sendTitle("§a§lUJIAN SELESAI", "§7Benar: §a" + score[0] + " §f| Salah: §c" + score[1], 20, 100, 20);
                    player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
                    plugin.getUiManager().clearExamSession(player);
                } else {
                    session.setCurrentQuestion(3);
                    plugin.getUiManager().openExamQuestion3(player, subject, false);
                }
            })
            .closedResultHandler(() -> openExamConfirmationBedrock(player, subject))
            .build();

        FloodgateApi.getInstance().sendForm(player.getUniqueId(), form);
    }

    /** [Bedrock] Portal Ditutup — SimpleForm notice tanpa §-codes. */
    public void openExamClosedBedrock(Player player) {
        String adminMessage = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(
            MiniMessage.miniMessage().deserialize(plugin.getExamMessage())
        );

        SimpleForm form = SimpleForm.builder()
            .title("Portal Ditutup")
            .content("Portal Sedang ditutup!\n\n" + adminMessage)
            .button("Tutup")
            .build();

        FloodgateApi.getInstance().sendForm(player.getUniqueId(), form);
    }
}
