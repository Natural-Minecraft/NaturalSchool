package id.naturalsmp.naturalSchool.exam;

import id.naturalsmp.naturalSchool.NaturalSchool;
import id.naturalsmp.naturalSchool.profile.StudentProfile;
import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
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
 * Mencakup: Portal Ujian, Soal 1-N (MCQ), Konfirmasi Kirim, dan Portal Ditutup.
 *
 * Berisi implementasi Java Edition (Dialog API) dan Bedrock Edition (Cumulus Form)
 * dalam satu file yang sama.
 */
public class ExamGui {

    private final NaturalSchool plugin;

    public ExamGui(NaturalSchool plugin) {
        this.plugin = plugin;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // JAVA EDITION — Paper Dialog API
    // ─────────────────────────────────────────────────────────────────────────

    /** [Java] Portal Ujian — pilih mata pelajaran. */
    public void openExamPortalJava(Player player) {
        // Cek status portal dari ExamManager
        if ("CLOSED".equalsIgnoreCase(plugin.getExamManager().getPortalStatus())) {
            openExamClosedJava(player);
            return;
        }
        if (!plugin.isExamForceOpen() && !plugin.getSemesterManager().isAllowedExamTime()) {
            openExamScheduleClosedJava(player);
            return;
        }

        List<DialogBody> bodies = new ArrayList<>();
        bodies.add(DialogBody.item(new ItemStack(Material.BOOKSHELF)).build());
        bodies.add(DialogBody.plainMessage(MiniMessage.miniMessage().deserialize("<gold><bold>Portal Ujian</bold></gold>")));
        bodies.add(DialogBody.plainMessage(MiniMessage.miniMessage().deserialize(plugin.getExamManager().getPortalMessage())));
        bodies.add(DialogBody.plainMessage(MiniMessage.miniMessage().deserialize("<yellow>Pilih mata pelajaran untuk diuji di bawah ini:</yellow>")));

        List<ActionButton> mapelButtons = new ArrayList<>();
        mapelButtons.add(createMapelButton("Pengetahuan Umum", "pengetahuan_umum"));
        mapelButtons.add(createMapelButton("IPA",              "ipa"));
        mapelButtons.add(createMapelButton("IPS",              "ips"));
        mapelButtons.add(createMapelButton("Matematika (MTK)", "mtk"));
        mapelButtons.add(createMapelButton("Bahasa Indonesia", "b_indo"));
        mapelButtons.add(createMapelButton("PKN",              "pkn"));
        mapelButtons.add(createMapelButton("Bahasa Inggris",   "b_inggris"));

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
                    // Cek ketersediaan soal (Error Code 2)
                    StudentProfile profile = plugin.getProfileManager().getProfile(p.getUniqueId());
                    int academicClass = (profile != null) ? profile.getAcademicClass() : 1;
                    List<ExamQuestions.Question> questions = plugin.getExamManager().getQuestions(subjectKey, academicClass);

                    if (questions == null || questions.isEmpty()) {
                        showErrorCode2Java(p);
                        return;
                    }

                    plugin.getUiManager().startExamSession(p, subjectKey);
                    plugin.getUiManager().openExamPre(p, subjectKey);
                }
            }, ClickCallback.Options.builder().uses(1).build()))
            .build();
    }

    public void openExamPreJava(Player player, String subject) {
        List<DialogBody> bodies = List.of(
            DialogBody.item(new ItemStack(Material.WRITABLE_BOOK)).build(),
            DialogBody.plainMessage(MiniMessage.miniMessage().deserialize("<gold><bold>Pra-Ujian: " + ExamQuestions.getSubjectDisplayName(subject) + "</bold></gold>")),
            DialogBody.plainMessage(MiniMessage.miniMessage().deserialize("<gray>Petunjuk: Jawab semua pertanyaan dengan teliti. Nilai kelulusan akan dihitung setelah Anda mengirimkan jawaban.</gray>")),
            DialogBody.plainMessage(MiniMessage.miniMessage().deserialize("<yellow>Apakah Anda siap memulai ujian?</yellow>"))
        );

        ActionButton startBtn = ActionButton.builder(Component.text("Mulai Ujian"))
            .action(DialogAction.customClick((view, audience) -> {
                if (audience instanceof Player p) {
                    plugin.getUiManager().openExamQuestion(p, subject, 1);
                }
            }, ClickCallback.Options.builder().uses(1).build()))
            .build();

        ActionButton cancelBtn = ActionButton.builder(Component.text("Batal"))
            .action(DialogAction.customClick((view, audience) -> {
                if (audience instanceof Player p) {
                    plugin.getUiManager().clearExamSession(p);
                    plugin.getUiManager().openExamPortal(p);
                }
            }, ClickCallback.Options.builder().uses(1).build()))
            .build();

        Dialog dialog = Dialog.create(builder -> builder.empty()
            .base(DialogBase.builder(Component.text("Pra-Ujian"))
                .canCloseWithEscape(false)
                .body(bodies)
                .build())
            .type(DialogType.confirmation(startBtn, cancelBtn))
        );

        player.showDialog(dialog);
    }

    /** [Java] Soal dinamis (MCQ 1-N). */
    public void openExamQuestionJava(Player player, String subject, int qNum) {
        ExamSession session = plugin.getUiManager().getExamSession(player.getUniqueId());
        if (session == null) { openExamPortalJava(player); return; }

        StudentProfile profile = plugin.getProfileManager().getProfile(player.getUniqueId());
        int academicClass = (profile != null) ? profile.getAcademicClass() : 1;
        List<ExamQuestions.Question> questions = plugin.getExamManager().getQuestions(subject, academicClass);

        if (questions == null || questions.isEmpty() || qNum < 1 || qNum > questions.size()) {
            showErrorCode2Java(player);
            return;
        }

        ExamQuestions.Question q = questions.get(qNum - 1);

        List<DialogBody> bodies = new ArrayList<>();
        bodies.add(DialogBody.item(new ItemStack(Material.BOOK)).build());
        bodies.add(DialogBody.plainMessage(MiniMessage.miniMessage().deserialize("<gold><bold>Ujian: " + ExamQuestions.getSubjectDisplayName(subject) + " (Soal " + qNum + "/" + questions.size() + ")</bold></gold>")));
        bodies.add(DialogBody.plainMessage(MiniMessage.miniMessage().deserialize("<gray>Pertanyaan:</gray> <white>" + q.questionText + "</white>")));
        bodies.add(DialogBody.plainMessage(MiniMessage.miniMessage().deserialize("<yellow>Pilih salah satu tombol jawaban di bawah ini:</yellow>")));

        String currentAns = session.getAnswer(qNum - 1);

        List<ActionButton> optionButtons = new ArrayList<>();
        if (q.options != null) {
            for (int i = 0; i < q.options.size(); i++) {
                String optText = q.options.get(i);
                String optChar = String.valueOf((char) ('A' + i)); // A, B, C, D...

                String displayOptionText = optChar.equalsIgnoreCase(currentAns)
                    ? "<green><bold>" + optChar + ". " + optText + " (Terpilih)</bold></green>"
                    : optChar + ". " + optText;

                optionButtons.add(createOptionButton(displayOptionText, subject, session, qNum, optChar, questions.size()));
            }
        }

        ActionButton backBtn;
        if (qNum == 1) {
            backBtn = ActionButton.builder(Component.text("Kembali ke Portal"))
                .action(DialogAction.customClick((view, audience) -> {
                    if (audience instanceof Player p) {
                        plugin.getUiManager().clearExamSession(p);
                        plugin.getUiManager().openExamPortal(p);
                    }
                }, ClickCallback.Options.builder().uses(1).build()))
                .build();
        } else {
            backBtn = ActionButton.builder(Component.text("Kembali ke Soal " + (qNum - 1)))
                .action(DialogAction.customClick((view, audience) -> {
                    if (audience instanceof Player p) {
                        session.setCurrentQuestion(qNum - 1);
                        plugin.getUiManager().openExamQuestion(p, subject, qNum - 1);
                    }
                }, ClickCallback.Options.builder().uses(1).build()))
                .build();
        }

        Dialog dialog = Dialog.create(builder -> builder.empty()
            .base(DialogBase.builder(Component.text("Ujian - Soal " + qNum))
                .canCloseWithEscape(false)
                .body(bodies)
                .build())
            .type(DialogType.multiAction(optionButtons, backBtn, 2))
        );

        player.showDialog(dialog);
    }

    private ActionButton createOptionButton(String text, String subject, ExamSession session, int qNum, String optChar, int totalQuestions) {
        return ActionButton.builder(MiniMessage.miniMessage().deserialize(text))
            .action(DialogAction.customClick((view, audience) -> {
                if (audience instanceof Player p) {
                    session.setAnswer(qNum - 1, optChar);
                    if (qNum < totalQuestions) {
                        session.setCurrentQuestion(qNum + 1);
                        plugin.getUiManager().openExamQuestion(p, subject, qNum + 1);
                    } else {
                        session.setCurrentQuestion(totalQuestions + 1);
                        plugin.getUiManager().openExamConfirmation(p, subject);
                    }
                }
            }, ClickCallback.Options.builder().uses(1).build()))
            .build();
    }

    /** [Java] Konfirmasi pengiriman jawaban. */
    public void openExamConfirmationJava(Player player, String subject) {
        ExamSession session = plugin.getUiManager().getExamSession(player.getUniqueId());
        if (session == null) { openExamPortalJava(player); return; }

        StudentProfile profile = plugin.getProfileManager().getProfile(player.getUniqueId());
        int academicClass = (profile != null) ? profile.getAcademicClass() : 1;
        List<ExamQuestions.Question> questions = plugin.getExamManager().getQuestions(subject, academicClass);
        int totalQuestions = (questions != null) ? questions.size() : 10;

        List<DialogBody> bodies = List.of(
            DialogBody.item(new ItemStack(Material.WRITTEN_BOOK)).build(),
            DialogBody.plainMessage(MiniMessage.miniMessage().deserialize("<gold><bold>Portal Ujian: Konfirmasi Kirim</bold></gold>")),
            DialogBody.plainMessage(MiniMessage.miniMessage().deserialize("<yellow>Apakah Anda yakin ingin menyelesaikan ujian dan mengirimkan jawaban Anda sekarang?</yellow>"))
        );

        ActionButton confirmBtn = ActionButton.builder(Component.text("Kirim Jawaban"))
            .action(DialogAction.customClick((view, audience) -> {
                if (audience instanceof Player p) {
                    int[] score = ExamQuestions.evaluateExam(session, questions);
                    p.sendTitle("§a§lUJIAN SELESAI", "§7Benar: §a" + score[0] + " §f| Salah: §c" + score[1], 20, 100, 20);
                    p.playSound(p.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
                    plugin.getUiManager().clearExamSession(p);
                }
            }, ClickCallback.Options.builder().uses(1).build()))
            .build();

        ActionButton backBtn = ActionButton.builder(Component.text("Kembali ke Soal " + totalQuestions))
            .action(DialogAction.customClick((view, audience) -> {
                if (audience instanceof Player p) {
                    session.setCurrentQuestion(totalQuestions);
                    plugin.getUiManager().openExamQuestion(p, subject, totalQuestions);
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
        bodies.add(DialogBody.plainMessage(MiniMessage.miniMessage().deserialize(plugin.getExamManager().getPortalMessage())));

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

    /** [Java] Portal Ditutup — notice dengan pesan jadwal ujian semester. */
    public void openExamScheduleClosedJava(Player player) {
        List<DialogBody> bodies = new ArrayList<>();
        bodies.add(DialogBody.item(new ItemStack(Material.BARRIER)).build());
        bodies.add(DialogBody.plainMessage(MiniMessage.miniMessage().deserialize(plugin.getSemesterManager().getExamScheduleMessage())));

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

    public void showErrorCode2Java(Player player) {
        List<DialogBody> bodies = List.of(
            DialogBody.plainMessage(MiniMessage.miniMessage().deserialize("<red><bold>Error</bold></red>")),
            DialogBody.plainMessage(MiniMessage.miniMessage().deserialize("Maaf terjadi kesalahan (Code: 2). Jika masalah tetap berlanjut silahkan hubungi kementrian"))
        );

        ActionButton okBtn = ActionButton.builder(Component.text("Ok"))
            .action(DialogAction.customClick((view, audience) -> {
                // Safely close dialog
            }, ClickCallback.Options.builder().uses(1).build()))
            .build();

        Dialog dialog = Dialog.create(builder -> builder.empty()
            .base(DialogBase.builder(Component.text("Error"))
                .canCloseWithEscape(true)
                .body(bodies)
                .build())
            .type(DialogType.notice(okBtn))
        );

        player.showDialog(dialog);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // BEDROCK EDITION — Geyser/Floodgate Cumulus Form
    // ─────────────────────────────────────────────────────────────────────────

    /** [Bedrock] Portal Ujian — pilih mata pelajaran (SimpleForm). */
    public void openExamPortalBedrock(Player player) {
        // Cek status portal dari ExamManager
        if ("CLOSED".equalsIgnoreCase(plugin.getExamManager().getPortalStatus())) {
            openExamClosedBedrock(player);
            return;
        }
        if (!plugin.isExamForceOpen() && !plugin.getSemesterManager().isAllowedExamTime()) {
            openExamScheduleClosedBedrock(player);
            return;
        }

        String examMessage = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(
            MiniMessage.miniMessage().deserialize(plugin.getExamManager().getPortalMessage())
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

                // Cek ketersediaan soal (Error Code 2)
                StudentProfile profile = plugin.getProfileManager().getProfile(player.getUniqueId());
                int academicClass = (profile != null) ? profile.getAcademicClass() : 1;
                List<ExamQuestions.Question> questions = plugin.getExamManager().getQuestions(selectedSubject, academicClass);

                if (questions == null || questions.isEmpty()) {
                    showErrorCode2Bedrock(player);
                    return;
                }

                plugin.getUiManager().startExamSession(player, selectedSubject);
                plugin.getUiManager().openExamPre(player, selectedSubject);
            })
            .build();

        FloodgateApi.getInstance().sendForm(player.getUniqueId(), form);
    }

    public void openExamPreBedrock(Player player, String subject) {
        SimpleForm form = SimpleForm.builder()
            .title("Pra-Ujian: " + ExamQuestions.getSubjectDisplayName(subject))
            .content("Petunjuk: Jawab semua pertanyaan dengan teliti. Nilai kelulusan akan dihitung setelah Anda mengirimkan jawaban.\n\nApakah Anda siap memulai ujian?")
            .button("Mulai Ujian")
            .button("Batal")
            .validResultHandler(response -> {
                int clickedId = response.clickedButtonId();
                if (clickedId == 0) {
                    plugin.getUiManager().openExamQuestion(player, subject, 1);
                } else {
                    plugin.getUiManager().clearExamSession(player);
                    plugin.getUiManager().openExamPortal(player);
                }
            })
            .closedResultHandler(() -> openExamPreBedrock(player, subject))
            .build();

        FloodgateApi.getInstance().sendForm(player.getUniqueId(), form);
    }

    /** [Bedrock] Soal dinamis (MCQ 1-N). */
    public void openExamQuestionBedrock(Player player, String subject, int qNum) {
        ExamSession session = plugin.getUiManager().getExamSession(player.getUniqueId());
        if (session == null) { openExamPortalBedrock(player); return; }

        StudentProfile profile = plugin.getProfileManager().getProfile(player.getUniqueId());
        int academicClass = (profile != null) ? profile.getAcademicClass() : 1;
        List<ExamQuestions.Question> questions = plugin.getExamManager().getQuestions(subject, academicClass);

        if (questions == null || questions.isEmpty() || qNum < 1 || qNum > questions.size()) {
            showErrorCode2Bedrock(player);
            return;
        }

        ExamQuestions.Question q = questions.get(qNum - 1);
        String currentAns = session.getAnswer(qNum - 1);

        List<String> optionButtons = new ArrayList<>();
        if (q.options != null) {
            for (int i = 0; i < q.options.size(); i++) {
                String optText = q.options.get(i);
                String optChar = String.valueOf((char) ('A' + i));
                String btnText = (optChar.equalsIgnoreCase(currentAns) ? "\u00a7a" + optChar + ". " : optChar + ". ") + optText;
                optionButtons.add(btnText);
            }
        }

        String navText = (qNum == 1) ? "Kembali ke Portal" : "Kembali ke Soal " + (qNum - 1);

        SimpleForm.Builder formBuilder = SimpleForm.builder()
            .title("Ujian: Soal " + qNum + "/" + questions.size())
            .content("Mata Pelajaran: " + ExamQuestions.getSubjectDisplayName(subject) +
                "\n\nPertanyaan:\n" + q.questionText + "\n\nPilih salah satu jawaban di bawah ini:");

        for (String btn : optionButtons) {
            formBuilder.button(btn);
        }
        formBuilder.button(navText);

        formBuilder.validResultHandler(response -> {
            int clickedId = response.clickedButtonId();
            int totalOptions = (q.options != null) ? q.options.size() : 0;

            if (clickedId == totalOptions) {
                if (qNum == 1) {
                    plugin.getUiManager().clearExamSession(player);
                    plugin.getUiManager().openExamPortal(player);
                } else {
                    session.setCurrentQuestion(qNum - 1);
                    plugin.getUiManager().openExamQuestion(player, subject, qNum - 1);
                }
                return;
            }

            if (clickedId >= 0 && clickedId < totalOptions) {
                String optChar = String.valueOf((char) ('A' + clickedId));
                session.setAnswer(qNum - 1, optChar);
                if (qNum < questions.size()) {
                    session.setCurrentQuestion(qNum + 1);
                    plugin.getUiManager().openExamQuestion(player, subject, qNum + 1);
                } else {
                    session.setCurrentQuestion(questions.size() + 1);
                    plugin.getUiManager().openExamConfirmation(player, subject);
                }
            }
        })
        .closedResultHandler(() -> openExamQuestionBedrock(player, subject, qNum));

        FloodgateApi.getInstance().sendForm(player.getUniqueId(), formBuilder.build());
    }

    /** [Bedrock] Konfirmasi pengiriman jawaban (CustomForm). */
    public void openExamConfirmationBedrock(Player player, String subject) {
        ExamSession session = plugin.getUiManager().getExamSession(player.getUniqueId());
        if (session == null) { openExamPortalBedrock(player); return; }

        StudentProfile profile = plugin.getProfileManager().getProfile(player.getUniqueId());
        int academicClass = (profile != null) ? profile.getAcademicClass() : 1;
        List<ExamQuestions.Question> questions = plugin.getExamManager().getQuestions(subject, academicClass);
        int totalQuestions = (questions != null) ? questions.size() : 10;

        CustomForm form = CustomForm.builder()
            .title("Konfirmasi Akhir")
            .label("Apakah Anda yakin ingin menyelesaikan ujian dan mengirimkan jawaban Anda sekarang?")
            .dropdown("Pilih Tindakan",
                "Kirim Jawaban",
                "Kembali ke Soal " + totalQuestions
            )
            .validResultHandler(response -> {
                int selectedIndex = response.asDropdown(1);
                if (selectedIndex == 0) {
                    int[] score = ExamQuestions.evaluateExam(session, questions);
                    player.sendTitle("§a§lUJIAN SELESAI", "§7Benar: §a" + score[0] + " §f| Salah: §c" + score[1], 20, 100, 20);
                    player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
                    plugin.getUiManager().clearExamSession(player);
                } else {
                    session.setCurrentQuestion(totalQuestions);
                    plugin.getUiManager().openExamQuestion(player, subject, totalQuestions);
                }
            })
            .closedResultHandler(() -> openExamConfirmationBedrock(player, subject))
            .build();

        FloodgateApi.getInstance().sendForm(player.getUniqueId(), form);
    }

    /** [Bedrock] Portal Ditutup — SimpleForm notice tanpa §-codes. */
    public void openExamClosedBedrock(Player player) {
        String adminMessage = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(
            MiniMessage.miniMessage().deserialize(plugin.getExamManager().getPortalMessage())
        );

        SimpleForm form = SimpleForm.builder()
            .title("Portal Ditutup")
            .content("Portal Sedang ditutup!\n\n" + adminMessage)
            .button("Tutup")
            .build();

        FloodgateApi.getInstance().sendForm(player.getUniqueId(), form);
    }

    /** [Bedrock] Portal Ditutup — SimpleForm notice untuk jadwal ujian. */
    public void openExamScheduleClosedBedrock(Player player) {
        String scheduleMessage = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(
            MiniMessage.miniMessage().deserialize(plugin.getSemesterManager().getExamScheduleMessage())
        );

        SimpleForm form = SimpleForm.builder()
            .title("Portal Ditutup")
            .content(scheduleMessage)
            .button("Tutup")
            .build();

        FloodgateApi.getInstance().sendForm(player.getUniqueId(), form);
    }

    public void showErrorCode2Bedrock(Player player) {
        SimpleForm form = SimpleForm.builder()
            .title("Error")
            .content("Maaf terjadi kesalahan (Code: 2). Jika masalah tetap berlanjut silahkan hubungi kementrian")
            .button("Ok")
            .build();

        FloodgateApi.getInstance().sendForm(player.getUniqueId(), form);
    }
}
