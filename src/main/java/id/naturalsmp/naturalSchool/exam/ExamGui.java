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
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.geysermc.cumulus.form.CustomForm;
import org.geysermc.cumulus.form.SimpleForm;
import org.geysermc.floodgate.api.FloodgateApi;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * GUI mandiri untuk alur Ujian utama (/school exam).
 * Terintegrasi dengan Packet-Based Architecture dan validasi asinkron (v1.6.4).
 */
public class ExamGui {

    private final NaturalSchool plugin;

    public ExamGui(NaturalSchool plugin) {
        this.plugin = plugin;
    }

    private List<String> getAvailablePacketsForPlayer(Player player) {
        StudentProfile profile = plugin.getProfileManager().getProfile(player.getUniqueId());
        int playerClass = (profile != null) ? profile.getAcademicClass() : 1;
        List<String> activePackets = plugin.getExamManager().getActivePacketIds();
        List<String> available = new ArrayList<>();
        for (String packetId : activePackets) {
            String[] parts = packetId.split("_");
            if (parts.length >= 2) {
                try {
                    int packetClass = Integer.parseInt(parts[1]);
                    if (packetClass == playerClass) {
                        available.add(packetId);
                    }
                } catch (NumberFormatException e) {
                    // Ignore malformed packets
                }
            }
        }
        return available;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // JAVA EDITION — Paper Dialog API
    // ─────────────────────────────────────────────────────────────────────────

    public void openExamPortalJava(Player player) {
        // Validation A: Portal Closed
        if ("CLOSED".equalsIgnoreCase(plugin.getExamManager().getPortalStatus())) {
            openExamClosedJava(player);
            return;
        }
        if (!plugin.isExamForceOpen() && !plugin.getSemesterManager().isAllowedExamTime()) {
            openExamScheduleClosedJava(player);
            return;
        }

        List<String> availablePackets = getAvailablePacketsForPlayer(player);
        if (availablePackets.isEmpty()) {
            List<DialogBody> bodies = List.of(
                DialogBody.item(new ItemStack(Material.BARRIER)).build(),
                DialogBody.plainMessage(MiniMessage.miniMessage().deserialize("<red><bold>Portal Ujian</bold></red>")),
                DialogBody.plainMessage(MiniMessage.miniMessage().deserialize("<yellow>Tidak ada paket ujian aktif untuk kelas Anda saat ini.</yellow>"))
            );

            ActionButton closeBtn = ActionButton.builder(Component.text("Tutup Portal"))
                .action(DialogAction.customClick((view, audience) -> {}, ClickCallback.Options.builder().uses(1).build()))
                .build();

            Dialog dialog = Dialog.create(builder -> builder.empty()
                .base(DialogBase.builder(Component.text("Portal Ujian"))
                    .canCloseWithEscape(true)
                    .body(bodies)
                    .build())
                .type(DialogType.notice(closeBtn))
            );
            player.showDialog(dialog);
            return;
        }

        List<DialogBody> bodies = new ArrayList<>();
        bodies.add(DialogBody.item(new ItemStack(Material.BOOKSHELF)).build());
        bodies.add(DialogBody.plainMessage(MiniMessage.miniMessage().deserialize("<gold><bold>Portal Ujian</bold></gold>")));
        bodies.add(DialogBody.plainMessage(MiniMessage.miniMessage().deserialize(plugin.getExamManager().getPortalMessage())));
        bodies.add(DialogBody.plainMessage(MiniMessage.miniMessage().deserialize("<yellow>Pilih paket ujian di bawah ini untuk memulai:</yellow>")));

        List<ActionButton> packetButtons = new ArrayList<>();
        for (String packetId : availablePackets) {
            String[] parts = packetId.split("_");
            int subjectId = 1;
            String examType = "";
            try {
                subjectId = Integer.parseInt(parts[0]);
                examType = parts[2];
            } catch (Exception e) {}
            String subjectName = ExamQuestions.getSubjectName(subjectId);
            String displayName = subjectName + " (" + examType + ")";

            packetButtons.add(ActionButton.builder(Component.text(displayName))
                .action(DialogAction.customClick((view, audience) -> {
                    if (audience instanceof Player p) {
                        runPreExamChecksJava(p, packetId);
                    }
                }, ClickCallback.Options.builder().uses(1).build()))
                .build());
        }

        ActionButton closeBtn = ActionButton.builder(Component.text("Tutup Portal"))
            .action(DialogAction.customClick((view, audience) -> {}, ClickCallback.Options.builder().uses(1).build()))
            .build();

        Dialog dialog = Dialog.create(builder -> builder.empty()
            .base(DialogBase.builder(Component.text("Portal Ujian"))
                .canCloseWithEscape(true)
                .body(bodies)
                .build())
            .type(DialogType.multiAction(packetButtons, closeBtn, 2))
        );

        player.showDialog(dialog);
    }

    private void runPreExamChecksJava(Player player, String packetId) {
        CompletableFuture.supplyAsync(() -> plugin.getDatabaseManager().hasAttemptedExam(player.getUniqueId(), packetId),
            runnable -> Bukkit.getScheduler().runTaskAsynchronously(plugin, runnable))
        .thenAccept(hasAttempted -> {
            Bukkit.getScheduler().runTask(plugin, () -> {
                // Validation B: Anti-Retake Shield
                if (hasAttempted) {
                    showErrorCode3Java(player);
                    return;
                }

                // Validation C: Fail-Safe Empty Packet Check
                List<ExamQuestions.Question> questions = plugin.getExamManager().getQuestionsForPacket(packetId);
                if (questions == null || questions.isEmpty()) {
                    showErrorCode2Java(player);
                    return;
                }

                plugin.getUiManager().startExamSession(player, packetId);
                plugin.getUiManager().openExamPre(player, packetId);
            });
        });
    }

    public void openExamPreJava(Player player, String packetId) {
        String[] parts = packetId.split("_");
        int subjectId = 1;
        String examType = "";
        try {
            subjectId = Integer.parseInt(parts[0]);
            examType = parts[2];
        } catch (Exception e) {}
        String subjectName = ExamQuestions.getSubjectName(subjectId);

        List<DialogBody> bodies = List.of(
            DialogBody.item(new ItemStack(Material.WRITABLE_BOOK)).build(),
            DialogBody.plainMessage(MiniMessage.miniMessage().deserialize("<gold><bold>Pra-Ujian: " + subjectName + " (" + examType + ")</bold></gold>")),
            DialogBody.plainMessage(MiniMessage.miniMessage().deserialize("<gray>Petunjuk: Jawab semua pertanyaan dengan teliti. Nilai kelulusan akan dihitung setelah Anda mengirimkan jawaban.</gray>")),
            DialogBody.plainMessage(MiniMessage.miniMessage().deserialize("<yellow>Apakah Anda siap memulai ujian?</yellow>"))
        );

        ActionButton startBtn = ActionButton.builder(Component.text("Mulai Ujian"))
            .action(DialogAction.customClick((view, audience) -> {
                if (audience instanceof Player p) {
                    plugin.getUiManager().openExamQuestion(p, packetId, 1);
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

    public void openExamQuestionJava(Player player, String packetId, int qNum) {
        ExamSession session = plugin.getUiManager().getExamSession(player.getUniqueId());
        if (session == null) { openExamPortalJava(player); return; }

        List<ExamQuestions.Question> questions = plugin.getExamManager().getQuestionsForPacket(packetId);
        if (questions == null || questions.isEmpty() || qNum < 1 || qNum > questions.size()) {
            showErrorCode2Java(player);
            return;
        }

        ExamQuestions.Question q = questions.get(qNum - 1);
        String[] parts = packetId.split("_");
        int subjectId = 1;
        String examType = "";
        try {
            subjectId = Integer.parseInt(parts[0]);
            examType = parts[2];
        } catch (Exception e) {}
        String subjectName = ExamQuestions.getSubjectName(subjectId);

        List<DialogBody> bodies = new ArrayList<>();
        bodies.add(DialogBody.item(new ItemStack(Material.BOOK)).build());
        bodies.add(DialogBody.plainMessage(MiniMessage.miniMessage().deserialize("<gold><bold>Ujian: " + subjectName + " (" + examType + ") - Soal " + qNum + "/" + questions.size() + "</bold></gold>")));
        bodies.add(DialogBody.plainMessage(MiniMessage.miniMessage().deserialize("<gray>Pertanyaan:</gray> <white>" + q.questionText + "</white>")));
        
        String currentAns = session.getAnswer(qNum - 1);

        if (session.isShowWarning() && currentAns == null) {
            bodies.add(DialogBody.plainMessage(MiniMessage.miniMessage().deserialize("<red><bold>Anda wajib memilih salah satu jawaban sebelum melanjutkan!</bold></red>")));
        } else {
            bodies.add(DialogBody.plainMessage(MiniMessage.miniMessage().deserialize("<yellow>Pilih salah satu tombol jawaban di bawah ini:</yellow>")));
        }

        List<ActionButton> optionButtons = new ArrayList<>();
        List<String> options = q.options;
        if (options == null || options.isEmpty()) {
            if ("TRUE_FALSE".equalsIgnoreCase(q.questionType) || "TF".equalsIgnoreCase(q.questionType)) {
                options = Arrays.asList("Benar", "Salah");
            }
        }

        if (options != null) {
            for (int i = 0; i < options.size(); i++) {
                String optText = options.get(i);
                String optChar = String.valueOf((char) ('A' + i));

                String displayOptionText = optChar.equalsIgnoreCase(currentAns)
                    ? "<green><bold>" + optChar + ". " + optText + " (Terpilih)</bold></green>"
                    : optChar + ". " + optText;

                optionButtons.add(createOptionButton(displayOptionText, packetId, session, qNum, optChar, questions.size()));
            }
        }

        ActionButton backBtn;
        if (qNum == 1) {
            backBtn = ActionButton.builder(Component.text("Sebelumnya"))
                .action(DialogAction.customClick((view, audience) -> {
                    if (audience instanceof Player p) {
                        session.setCurrentQuestion(0);
                        session.setShowWarning(false);
                        plugin.getUiManager().openExamPre(p, packetId);
                    }
                }, ClickCallback.Options.builder().uses(1).build()))
                .build();
        } else {
            backBtn = ActionButton.builder(Component.text("Kembali ke Soal " + (qNum - 1)))
                .action(DialogAction.customClick((view, audience) -> {
                    if (audience instanceof Player p) {
                        session.setCurrentQuestion(qNum - 1);
                        session.setShowWarning(false);
                        plugin.getUiManager().openExamQuestion(p, packetId, qNum - 1);
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

    private ActionButton createOptionButton(String text, String packetId, ExamSession session, int qNum, String optChar, int totalQuestions) {
        return ActionButton.builder(MiniMessage.miniMessage().deserialize(text))
            .action(DialogAction.customClick((view, audience) -> {
                if (audience instanceof Player p) {
                    session.setAnswer(qNum - 1, optChar);
                    session.setShowWarning(false);
                    if (qNum < totalQuestions) {
                        session.setCurrentQuestion(qNum + 1);
                        plugin.getUiManager().openExamQuestion(p, packetId, qNum + 1);
                    } else {
                        session.setCurrentQuestion(totalQuestions + 1);
                        plugin.getUiManager().openExamConfirmation(p, packetId);
                    }
                }
            }, ClickCallback.Options.builder().uses(1).build()))
            .build();
    }

    public void openExamConfirmationJava(Player player, String packetId) {
        ExamSession session = plugin.getUiManager().getExamSession(player.getUniqueId());
        if (session == null) { openExamPortalJava(player); return; }

        List<ExamQuestions.Question> questions = plugin.getExamManager().getQuestionsForPacket(packetId);
        int totalQuestions = (questions != null) ? questions.size() : 10;

        List<DialogBody> bodies = List.of(
            DialogBody.item(new ItemStack(Material.WRITTEN_BOOK)).build(),
            DialogBody.plainMessage(MiniMessage.miniMessage().deserialize("<gold><bold>Portal Ujian: Konfirmasi Kirim</bold></gold>")),
            DialogBody.plainMessage(MiniMessage.miniMessage().deserialize("<yellow>Apakah Anda yakin ingin menyelesaikan ujian dan mengirimkan jawaban Anda sekarang?</yellow>"))
        );

        ActionButton confirmBtn = ActionButton.builder(Component.text("Kirim Jawaban"))
            .action(DialogAction.customClick((view, audience) -> {
                if (audience instanceof Player p) {
                    // Submit & Upsert rapor asynchronously
                    int[] score = ExamQuestions.evaluateExam(session, questions);
                    double pctScore = questions != null && !questions.isEmpty()
                        ? ((double) score[0] / questions.size()) * 100.0
                        : 0.0;

                    CompletableFuture.runAsync(() -> {
                        StudentProfile profile = plugin.getProfileManager().getProfile(p.getUniqueId());
                        String nis = (profile != null) ? profile.getNis() : "";
                        String semester = (profile != null) ? profile.getCurrentSemester() : "GANJIL";

                        // 1. Save attempt
                        plugin.getDatabaseManager().saveExamAttempt(p.getUniqueId(), nis, packetId, pctScore);

                        // 2. Parse packet_id
                        String[] parts = packetId.split("_");
                        int subjectId = Integer.parseInt(parts[0]);
                        int academicClass = Integer.parseInt(parts[1]);
                        String examType = parts[2];

                        // 3. Upsert Rapor
                        plugin.getDatabaseManager().upsertStudentRapor(p.getUniqueId(), nis, academicClass, semester, subjectId, examType, pctScore);
                    }, runnable -> Bukkit.getScheduler().runTaskAsynchronously(plugin, runnable))
                    .thenRun(() -> {
                        // Return to main thread
                        Bukkit.getScheduler().runTask(plugin, () -> {
                            p.sendTitle("§a§lUJIAN SELESAI", "§7Benar: §a" + score[0] + " §f| Salah: §c" + score[1], 20, 100, 20);
                            p.playSound(p.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
                            plugin.getUiManager().clearExamSession(p);
                        });
                    });
                }
            }, ClickCallback.Options.builder().uses(1).build()))
            .build();

        ActionButton backBtn = ActionButton.builder(Component.text("Kembali ke Soal " + totalQuestions))
            .action(DialogAction.customClick((view, audience) -> {
                if (audience instanceof Player p) {
                    session.setCurrentQuestion(totalQuestions);
                    session.setShowWarning(false);
                    plugin.getUiManager().openExamQuestion(p, packetId, totalQuestions);
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

    public void openExamClosedJava(Player player) {
        List<DialogBody> bodies = new ArrayList<>();
        bodies.add(DialogBody.item(new ItemStack(Material.BARRIER)).build());
        bodies.add(DialogBody.plainMessage(MiniMessage.miniMessage().deserialize("<red><bold>Portal Sedang ditutup!</bold></red>")));
        bodies.add(DialogBody.plainMessage(MiniMessage.miniMessage().deserialize(plugin.getExamManager().getPortalMessage())));

        ActionButton closeBtn = ActionButton.builder(Component.text("Tutup"))
            .action(DialogAction.customClick((view, audience) -> {}, ClickCallback.Options.builder().uses(1).build()))
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

    public void openExamScheduleClosedJava(Player player) {
        List<DialogBody> bodies = new ArrayList<>();
        bodies.add(DialogBody.item(new ItemStack(Material.BARRIER)).build());
        bodies.add(DialogBody.plainMessage(MiniMessage.miniMessage().deserialize(plugin.getSemesterManager().getExamScheduleMessage())));

        ActionButton closeBtn = ActionButton.builder(Component.text("Tutup"))
            .action(DialogAction.customClick((view, audience) -> {}, ClickCallback.Options.builder().uses(1).build()))
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
            .action(DialogAction.customClick((view, audience) -> {}, ClickCallback.Options.builder().uses(1).build()))
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

    public void showErrorCode3Java(Player player) {
        List<DialogBody> bodies = List.of(
            DialogBody.plainMessage(MiniMessage.miniMessage().deserialize("<red><bold>Error</bold></red>")),
            DialogBody.plainMessage(MiniMessage.miniMessage().deserialize("Kamu sudah menyelesaikan paket ujian ini dan tidak dapat mengulangnya kembali!"))
        );

        ActionButton okBtn = ActionButton.builder(Component.text("Ok"))
            .action(DialogAction.customClick((view, audience) -> {}, ClickCallback.Options.builder().uses(1).build()))
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

    public void openExamPortalBedrock(Player player) {
        // Validation A: Portal Closed
        if ("CLOSED".equalsIgnoreCase(plugin.getExamManager().getPortalStatus())) {
            openExamClosedBedrock(player);
            return;
        }
        if (!plugin.isExamForceOpen() && !plugin.getSemesterManager().isAllowedExamTime()) {
            openExamScheduleClosedBedrock(player);
            return;
        }

        List<String> availablePackets = getAvailablePacketsForPlayer(player);
        if (availablePackets.isEmpty()) {
            SimpleForm form = SimpleForm.builder()
                .title("Portal Ujian")
                .content("Tidak ada paket ujian aktif untuk kelas Anda saat ini.")
                .button("Tutup")
                .build();
            FloodgateApi.getInstance().sendForm(player.getUniqueId(), form);
            return;
        }

        String examMessage = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(
            MiniMessage.miniMessage().deserialize(plugin.getExamManager().getPortalMessage())
        );

        CustomForm.Builder formBuilder = CustomForm.builder()
            .title("Portal Ujian")
            .label(examMessage + "\n\nPilih paket ujian di bawah ini untuk memulai:");

        List<String> dropdownOptions = new ArrayList<>();
        for (String packetId : availablePackets) {
            String[] parts = packetId.split("_");
            int subjectId = 1;
            String examType = "";
            try {
                subjectId = Integer.parseInt(parts[0]);
                examType = parts[2];
            } catch (Exception e) {}
            String subjectName = ExamQuestions.getSubjectName(subjectId);
            dropdownOptions.add(subjectName + " (" + examType + ")");
        }

        formBuilder.dropdown("Daftar Ujian Aktif", dropdownOptions);
        formBuilder.validResultHandler(response -> {
            int selectedIndex = response.asDropdown(1);
            if (selectedIndex >= 0 && selectedIndex < availablePackets.size()) {
                String packetId = availablePackets.get(selectedIndex);
                runPreExamChecksBedrock(player, packetId);
            }
        });

        FloodgateApi.getInstance().sendForm(player.getUniqueId(), formBuilder.build());
    }

    private void runPreExamChecksBedrock(Player player, String packetId) {
        CompletableFuture.supplyAsync(() -> plugin.getDatabaseManager().hasAttemptedExam(player.getUniqueId(), packetId),
            runnable -> Bukkit.getScheduler().runTaskAsynchronously(plugin, runnable))
        .thenAccept(hasAttempted -> {
            Bukkit.getScheduler().runTask(plugin, () -> {
                // Validation B: Anti-Retake Shield
                if (hasAttempted) {
                    showErrorCode3Bedrock(player);
                    return;
                }

                // Validation C: Fail-Safe Empty Packet Check
                List<ExamQuestions.Question> questions = plugin.getExamManager().getQuestionsForPacket(packetId);
                if (questions == null || questions.isEmpty()) {
                    showErrorCode2Bedrock(player);
                    return;
                }

                plugin.getUiManager().startExamSession(player, packetId);
                plugin.getUiManager().openExamPre(player, packetId);
            });
        });
    }

    public void openExamPreBedrock(Player player, String packetId) {
        String[] parts = packetId.split("_");
        int subjectId = 1;
        String examType = "";
        try {
            subjectId = Integer.parseInt(parts[0]);
            examType = parts[2];
        } catch (Exception e) {}
        String subjectName = ExamQuestions.getSubjectName(subjectId);

        SimpleForm form = SimpleForm.builder()
            .title("Pra-Ujian: " + subjectName + " (" + examType + ")")
            .content("Petunjuk: Jawab semua pertanyaan dengan teliti. Nilai kelulusan akan dihitung setelah Anda mengirimkan jawaban.\n\nApakah Anda siap memulai ujian?")
            .button("Mulai Ujian")
            .button("Batal")
            .validResultHandler(response -> {
                int clickedId = response.clickedButtonId();
                if (clickedId == 0) {
                    plugin.getUiManager().openExamQuestion(player, packetId, 1);
                } else {
                    plugin.getUiManager().clearExamSession(player);
                    plugin.getUiManager().openExamPortal(player);
                }
            })
            .closedResultHandler(() -> openExamPreBedrock(player, packetId))
            .build();

        FloodgateApi.getInstance().sendForm(player.getUniqueId(), form);
    }

    public void openExamQuestionBedrock(Player player, String packetId, int qNum) {
        ExamSession session = plugin.getUiManager().getExamSession(player.getUniqueId());
        if (session == null) { openExamPortalBedrock(player); return; }

        List<ExamQuestions.Question> questions = plugin.getExamManager().getQuestionsForPacket(packetId);
        if (questions == null || questions.isEmpty() || qNum < 1 || qNum > questions.size()) {
            showErrorCode2Bedrock(player);
            return;
        }

        ExamQuestions.Question q = questions.get(qNum - 1);
        String currentAns = session.getAnswer(qNum - 1);
        String[] parts = packetId.split("_");
        int subjectId = 1;
        String examType = "";
        try {
            subjectId = Integer.parseInt(parts[0]);
            examType = parts[2];
        } catch (Exception e) {}
        String subjectName = ExamQuestions.getSubjectName(subjectId);

        List<String> optionButtons = new ArrayList<>();
        List<String> options = q.options;
        if (options == null || options.isEmpty()) {
            if ("TRUE_FALSE".equalsIgnoreCase(q.questionType) || "TF".equalsIgnoreCase(q.questionType)) {
                options = Arrays.asList("Benar", "Salah");
            }
        }
        final List<String> finalOptions = options;

        if (finalOptions != null) {
            for (int i = 0; i < finalOptions.size(); i++) {
                String optText = finalOptions.get(i);
                String optChar = String.valueOf((char) ('A' + i));
                // STRIPPED of raw formatting codes and decorative symbols to avoid Bedrock layout bugs
                String btnText = optChar + ". " + optText + (optChar.equalsIgnoreCase(currentAns) ? " (Terpilih)" : "");
                optionButtons.add(btnText);
            }
        }

        String navText = (qNum == 1) ? "Sebelumnya" : "Kembali ke Soal " + (qNum - 1);

        String warningLabel = (session.isShowWarning() && currentAns == null)
            ? "\n\nAnda wajib memilih salah satu jawaban sebelum melanjutkan!"
            : "";

        SimpleForm.Builder formBuilder = SimpleForm.builder()
            .title("Ujian: Soal " + qNum + "/" + questions.size())
            .content("Mata Pelajaran: " + subjectName + " (" + examType + ")" +
                "\n\nPertanyaan:\n" + q.questionText + warningLabel + "\n\nPilih salah satu jawaban di bawah ini:");

        for (String btn : optionButtons) {
            formBuilder.button(btn);
        }
        formBuilder.button(navText);

        formBuilder.validResultHandler(response -> {
            int clickedId = response.clickedButtonId();
            int totalOptions = (finalOptions != null) ? finalOptions.size() : 0;

            if (clickedId == totalOptions) {
                // Clicked Nav button
                if (qNum == 1) {
                    session.setCurrentQuestion(0);
                    session.setShowWarning(false);
                    plugin.getUiManager().openExamPre(player, packetId);
                } else {
                    session.setCurrentQuestion(qNum - 1);
                    session.setShowWarning(false);
                    plugin.getUiManager().openExamQuestion(player, packetId, qNum - 1);
                }
                return;
            }

            if (clickedId >= 0 && clickedId < totalOptions) {
                String optChar = String.valueOf((char) ('A' + clickedId));
                session.setAnswer(qNum - 1, optChar);
                session.setShowWarning(false);
                if (qNum < questions.size()) {
                    session.setCurrentQuestion(qNum + 1);
                    plugin.getUiManager().openExamQuestion(player, packetId, qNum + 1);
                } else {
                    session.setCurrentQuestion(questions.size() + 1);
                    plugin.getUiManager().openExamConfirmation(player, packetId);
                }
            }
        })
        .closedResultHandler(() -> openExamQuestionBedrock(player, packetId, qNum));

        FloodgateApi.getInstance().sendForm(player.getUniqueId(), formBuilder.build());
    }

    public void openExamConfirmationBedrock(Player player, String packetId) {
        ExamSession session = plugin.getUiManager().getExamSession(player.getUniqueId());
        if (session == null) { openExamPortalBedrock(player); return; }

        List<ExamQuestions.Question> questions = plugin.getExamManager().getQuestionsForPacket(packetId);
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
                    // Submit & Recalculate asynchronously
                    int[] score = ExamQuestions.evaluateExam(session, questions);
                    double pctScore = questions != null && !questions.isEmpty()
                        ? ((double) score[0] / questions.size()) * 100.0
                        : 0.0;

                    CompletableFuture.runAsync(() -> {
                        StudentProfile profile = plugin.getProfileManager().getProfile(player.getUniqueId());
                        String nis = (profile != null) ? profile.getNis() : "";
                        String semester = (profile != null) ? profile.getCurrentSemester() : "GANJIL";

                        // 1. Save attempt
                        plugin.getDatabaseManager().saveExamAttempt(player.getUniqueId(), nis, packetId, pctScore);

                        // 2. Parse packet_id
                        String[] parts = packetId.split("_");
                        int subjectId = Integer.parseInt(parts[0]);
                        int academicClass = Integer.parseInt(parts[1]);
                        String examType = parts[2];

                        // 3. Upsert Rapor
                        plugin.getDatabaseManager().upsertStudentRapor(player.getUniqueId(), nis, academicClass, semester, subjectId, examType, pctScore);
                    }, runnable -> Bukkit.getScheduler().runTaskAsynchronously(plugin, runnable))
                    .thenRun(() -> {
                        // Return to main thread
                        Bukkit.getScheduler().runTask(plugin, () -> {
                            player.sendTitle("§a§lUJIAN SELESAI", "§7Benar: §a" + score[0] + " §f| Salah: §c" + score[1], 20, 100, 20);
                            player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
                            plugin.getUiManager().clearExamSession(player);
                        });
                    });
                } else {
                    session.setCurrentQuestion(totalQuestions);
                    session.setShowWarning(false);
                    plugin.getUiManager().openExamQuestion(player, packetId, totalQuestions);
                }
            })
            .closedResultHandler(() -> openExamConfirmationBedrock(player, packetId))
            .build();

        FloodgateApi.getInstance().sendForm(player.getUniqueId(), form);
    }

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

    public void showErrorCode3Bedrock(Player player) {
        SimpleForm form = SimpleForm.builder()
            .title("Error")
            .content("Kamu sudah menyelesaikan paket ujian ini dan tidak dapat mengulangnya kembali!")
            .button("Ok")
            .build();

        FloodgateApi.getInstance().sendForm(player.getUniqueId(), form);
    }
}
