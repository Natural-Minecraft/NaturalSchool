package id.naturalsmp.naturalSchool.exam;

import id.naturalsmp.naturalSchool.NaturalSchool;
import id.naturalsmp.naturalSchool.profile.StudentProfile;
import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import io.papermc.paper.registry.data.dialog.input.DialogInput;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickCallback;
import net.kyori.adventure.text.event.ClickEvent;
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
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CompletableFuture;

/**
 * Stateful unified GUI engine for the Exam Subsystem.
 * Written from scratch using the consolidated ClassCashGui layout.
 * Features Java Edition Paper Dialogs and Bedrock Edition Cumulus Forms.
 */
public class ExamGui {

    private final NaturalSchool plugin;
    private static final Set<UUID> submittingPlayers = ConcurrentHashMap.newKeySet();

    public ExamGui(NaturalSchool plugin) {
        this.plugin = plugin;
    }

    private boolean isBedrock(Player player) {
        try {
            return FloodgateApi.getInstance().isFloodgatePlayer(player.getUniqueId());
        } catch (Throwable t) {
            return false;
        }
    }

    private String cleanBedrockText(String text) {
        if (text == null) return "";
        return text.replaceAll("§[0-9a-fk-orA-FK-OR]", "").replace("○", "").replace("●", "").trim();
    }

    private void submitExam(Player player, String packetId, ExamSession session, List<ExamQuestions.Question> questions) {
        UUID uuid = player.getUniqueId();

        // Atomic submission lock
        if (!submittingPlayers.add(uuid)) {
            return;
        }

        int[] score = ExamQuestions.evaluateExam(session, questions);
        double pctScore = (questions != null && !questions.isEmpty())
            ? ((double) score[0] / questions.size()) * 100.0
            : 0.0;

        CompletableFuture.runAsync(() -> {
            String nis = session.getNis();
            // Save attempt to database
            plugin.getDatabaseManager().saveExamAttempt(uuid, nis, packetId, pctScore);
        }, runnable -> Bukkit.getScheduler().runTaskAsynchronously(plugin, runnable))
        .whenComplete((unused, throwable) -> {
            Bukkit.getScheduler().runTask(plugin, () -> {
                submittingPlayers.remove(uuid);

                if (!player.isOnline()) {
                    return;
                }

                if (throwable != null) {
                    // Intercept exception & do not clear session (anti-data loss)
                    openExamGui(player, "error", packetId, "DATABASE_ERROR");
                    plugin.getLogger().log(java.util.logging.Level.SEVERE, "Failed to submit exam for " + player.getName(), throwable);
                } else {
                    player.sendTitle("§a§lUJIAN SELESAI", "§7Benar: §a" + score[0] + " §f| Salah: §c" + score[1], 20, 100, 20);
                    player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
                    plugin.getUiManager().clearExamSession(player);
                }
            });
        });
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Consolidated Master Routing
    // ─────────────────────────────────────────────────────────────────────────

    public void openExamGui(Player player, String view, String packetId, String errorMsg) {
        if (isBedrock(player)) {
            openExamGuiBedrock(player, view, packetId, errorMsg);
        } else {
            openExamGuiJava(player, view, packetId, errorMsg);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // JAVA EDITION — Paper Dialog API (Consolidated)
    // ─────────────────────────────────────────────────────────────────────────

    public void openExamGuiJava(Player player, String view, String packetId, String errorMsg) {
        String rawTitle = "Portal Ujian";
        List<DialogBody> bodies = new ArrayList<>();
        List<DialogInput> inputs = new ArrayList<>();
        ActionButton submitBtn = null;

        switch (view.toLowerCase()) {
            case "portal": {
                rawTitle = "Portal Sekolah";
                bodies.add(DialogBody.item(new ItemStack(Material.BOOKSHELF)).build());
                bodies.add(DialogBody.plainMessage(MiniMessage.miniMessage().deserialize("<gold><bold>=== Portal Sekolah ===</bold></gold>")));
                if (errorMsg != null && !errorMsg.isEmpty()) {
                    bodies.add(DialogBody.plainMessage(MiniMessage.miniMessage().deserialize("<red><bold>[!] Error: " + errorMsg + "</bold></red>")));
                }
                bodies.add(DialogBody.plainMessage(MiniMessage.miniMessage().deserialize("<yellow>Silakan pilih tipe ujian di bawah ini:</yellow>")));

                List<ActionButton> buttons = List.of(
                    ActionButton.builder(Component.text("[ Ujian Harian ]"))
                        .action(DialogAction.customClick((v, aud) -> {
                            if (aud instanceof Player p) {
                                openExamGuiJava(p, "subject_select", "UH", null);
                            }
                        }, ClickCallback.Options.builder().uses(1).build()))
                        .build(),
                    ActionButton.builder(Component.text("[ Ujian Semester ]"))
                        .action(DialogAction.customClick((v, aud) -> {
                            if (aud instanceof Player p) {
                                if (!"OPEN".equalsIgnoreCase(plugin.getExamManager().getPortalSemesterStatus())) {
                                    openExamGuiJava(p, "semester_closed", null, null);
                                } else {
                                    openExamGuiJava(p, "subject_select", "UTS", null);
                                }
                            }
                        }, ClickCallback.Options.builder().uses(1).build()))
                        .build(),
                    ActionButton.builder(Component.text("[ Ujian Akhir Semester ]"))
                        .action(DialogAction.customClick((v, aud) -> {
                            if (aud instanceof Player p) {
                                if (!"OPEN".equalsIgnoreCase(plugin.getExamManager().getPortalSemesterStatus())) {
                                    openExamGuiJava(p, "semester_closed", null, null);
                                } else {
                                    openExamGuiJava(p, "subject_select", "UAS", null);
                                }
                            }
                        }, ClickCallback.Options.builder().uses(1).build()))
                        .build()
                );

                submitBtn = ActionButton.builder(Component.text("Tutup"))
                    .action(DialogAction.customClick((v, aud) -> {}, ClickCallback.Options.builder().uses(1).build()))
                    .build();

                renderDialog(player, rawTitle, bodies, inputs, submitBtn, buttons);
                return;
            }

            case "subject_select": {
                final String examType = packetId != null ? packetId.toUpperCase() : "UH";
                rawTitle = "Portal Ujian - " + examType;

                // Async fetch attempted packets for student to display legends
                CompletableFuture.supplyAsync(() -> plugin.getDatabaseManager().getAttemptedPackets(player.getUniqueId()),
                    runnable -> Bukkit.getScheduler().runTaskAsynchronously(plugin, runnable))
                .thenAccept(attempts -> {
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        StudentProfile profile = plugin.getProfileManager().getProfile(player.getUniqueId());
                        if (profile == null) {
                            openExamGuiJava(player, "error", null, "Gagal memuat profil.");
                            return;
                        }
                        int academicClass = profile.getAcademicClass();

                        List<DialogBody> asyncBodies = new ArrayList<>();

                        asyncBodies.add(DialogBody.item(new ItemStack(Material.BOOK)).build());
                        asyncBodies.add(DialogBody.plainMessage(MiniMessage.miniMessage().deserialize("<gold><bold>=== Portal Ujian " + examType + " ===</bold></gold>")));
                        if (errorMsg != null && !errorMsg.isEmpty()) {
                            asyncBodies.add(DialogBody.plainMessage(MiniMessage.miniMessage().deserialize("<red><bold>[!] Error: " + errorMsg + "</bold></red>")));
                        }
                        asyncBodies.add(DialogBody.plainMessage(MiniMessage.miniMessage().deserialize("<yellow>Pilih mata pelajaran di bawah ini:</yellow>")));

                        List<io.papermc.paper.registry.data.dialog.input.SingleOptionDialogInput.OptionEntry> selectOptions = new ArrayList<>();

                        for (int subjectId = 1; subjectId <= 7; subjectId++) {
                            String subjectName = ExamQuestions.getSubjectName(subjectId);
                            String targetPacketId = subjectId + "_" + academicClass + "_" + examType;

                            boolean completed = attempts.contains(targetPacketId);
                            boolean active;
                            if ("UH".equalsIgnoreCase(examType)) {
                                active = plugin.getExamManager().getActiveUhPackets().contains(targetPacketId);
                            } else {
                                active = "OPEN".equalsIgnoreCase(plugin.getExamManager().getPortalSemesterStatus())
                                    && plugin.getExamManager().getCurrentActiveSemesterPackets().contains(targetPacketId);
                            }

                            List<ExamQuestions.Question> qList = plugin.getExamManager().getQuestionsForPacket(targetPacketId);
                            boolean isComplete = (qList != null && qList.size() == 10);

                            String statusLegend;
                            if (!isComplete) {
                                statusLegend = "<yellow>[Tidak Lengkap]</yellow>";
                            } else if (completed) {
                                statusLegend = "<green>[Sudah Selesai]</green>";
                            } else if (active) {
                                statusLegend = "<yellow>[Aktif]</yellow>";
                            } else {
                                statusLegend = "<red>[Tidak Aktif]</red>";
                            }

                            asyncBodies.add(DialogBody.plainMessage(MiniMessage.miniMessage().deserialize("<white>• " + subjectName + ":</white> " + statusLegend)));

                            selectOptions.add(io.papermc.paper.registry.data.dialog.input.SingleOptionDialogInput.OptionEntry.create(
                                String.valueOf(subjectId),
                                Component.text(subjectName),
                                subjectId == 1
                            ));
                        }

                        List<DialogInput> asyncInputs = List.of(
                            DialogInput.singleOption("selected_subject", Component.text("Mata Pelajaran"), selectOptions)
                                .width(320)
                                .build()
                        );

                        ActionButton startBtn = ActionButton.builder(Component.text("Mulai Ujian"))
                            .action(DialogAction.customClick((viewObj, audience) -> {
                                if (audience instanceof Player p) {
                                    String selectedKey = viewObj.getText("selected_subject");
                                    if (selectedKey != null) {
                                        try {
                                            int subjectId = Integer.parseInt(selectedKey);
                                            String targetPacketId = subjectId + "_" + academicClass + "_" + examType;
                                            handleSubjectStartJava(p, examType, targetPacketId);
                                        } catch (Exception e) {
                                            openExamGuiJava(p, "subject_select", examType, "Gagal memproses pilihan.");
                                        }
                                    } else {
                                        openExamGuiJava(p, "subject_select", examType, "Pilih mata pelajaran dahulu!");
                                    }
                                }
                            }, ClickCallback.Options.builder().uses(1).build()))
                            .build();

                        ActionButton backBtn = ActionButton.builder(Component.text("Kembali"))
                            .action(DialogAction.customClick((viewObj, audience) -> {
                                if (audience instanceof Player p) {
                                    openExamGuiJava(p, "portal", null, null);
                                }
                            }, ClickCallback.Options.builder().uses(1).build()))
                            .build();

                        // Render single notice with back and start confirmation
                        Dialog dialog = Dialog.create(builder -> builder.empty()
                            .base(DialogBase.builder(Component.text("Portal Ujian - " + examType))
                                .canCloseWithEscape(true)
                                .body(asyncBodies)
                                .inputs(asyncInputs)
                                .build())
                            .type(DialogType.confirmation(startBtn, backBtn))
                        );
                        player.showDialog(dialog);
                    });
                });
                return;
            }

            case "pre_exam": {
                rawTitle = "Pra-Ujian";
                String[] parts = packetId.split("_");
                int subjectId = Integer.parseInt(parts[0]);
                String examType = parts[2];
                String subjectName = ExamQuestions.getSubjectName(subjectId);

                StudentProfile profile = plugin.getProfileManager().getProfile(player.getUniqueId());
                String name = player.getName();
                String nis = (profile != null && profile.getNis() != null) ? profile.getNis() : "";
                int academicClass = (profile != null) ? profile.getAcademicClass() : 0;

                bodies.add(DialogBody.item(new ItemStack(Material.WRITABLE_BOOK)).build());
                bodies.add(DialogBody.plainMessage(MiniMessage.miniMessage().deserialize("<gold><bold>=== Pra-Ujian ===</bold></gold>")));
                bodies.add(DialogBody.plainMessage(MiniMessage.miniMessage().deserialize("<yellow>Nama Siswa:</yellow> <white>" + name + "</white>")));
                bodies.add(DialogBody.plainMessage(MiniMessage.miniMessage().deserialize("<yellow>NIS:</yellow> <white>" + nis + "</white>")));
                bodies.add(DialogBody.plainMessage(MiniMessage.miniMessage().deserialize("<yellow>Kelas:</yellow> <white>" + academicClass + "</white>")));
                bodies.add(DialogBody.plainMessage(MiniMessage.miniMessage().deserialize("<yellow>Mata Pelajaran:</yellow> <white>" + subjectName + " (" + examType + ")</white>")));
                bodies.add(DialogBody.plainMessage(MiniMessage.miniMessage().deserialize("<yellow>Jumlah Soal:</yellow> <white>10 Pertanyaan</white>")));
                bodies.add(DialogBody.plainMessage(Component.text(" ")));
                bodies.add(DialogBody.plainMessage(MiniMessage.miniMessage().deserialize("<yellow>Apakah Anda siap memulai?</yellow>")));

                ActionButton startBtn = ActionButton.builder(Component.text("Mulai Ujian"))
                    .action(DialogAction.customClick((v, aud) -> {
                        if (aud instanceof Player p) {
                            ExamSession session = plugin.getUiManager().getExamSession(p.getUniqueId());
                            if (session != null) {
                                session.setCurrentQuestion(1);
                                openExamGuiJava(p, "question", packetId, null);
                            }
                        }
                    }, ClickCallback.Options.builder().uses(1).build()))
                    .build();

                ActionButton cancelBtn = ActionButton.builder(Component.text("Batal"))
                    .action(DialogAction.customClick((v, aud) -> {
                        if (aud instanceof Player p) {
                            plugin.getUiManager().clearExamSession(p);
                            openExamGuiJava(p, "subject_select", examType, null);
                        }
                    }, ClickCallback.Options.builder().uses(1).build()))
                    .build();

                final String currentTitle = rawTitle;
                Dialog dialog = Dialog.create(builder -> builder.empty()
                    .base(DialogBase.builder(Component.text(currentTitle))
                        .canCloseWithEscape(false)
                        .body(bodies)
                        .build())
                    .type(DialogType.confirmation(startBtn, cancelBtn))
                );
                player.showDialog(dialog);
                return;
            }

            case "question": {
                ExamSession session = plugin.getUiManager().getExamSession(player.getUniqueId());
                if (session == null) {
                    openExamGuiJava(player, "portal", null, null);
                    return;
                }

                List<ExamQuestions.Question> questions = plugin.getExamManager().getQuestionsForPacket(packetId);
                int qNum = session.getCurrentQuestion();
                if (questions == null || questions.isEmpty() || qNum < 1 || qNum > questions.size()) {
                    openExamGuiJava(player, "error", null, null);
                    return;
                }

                ExamQuestions.Question q = questions.get(qNum - 1);
                String[] parts = packetId.split("_");
                int subjectId = Integer.parseInt(parts[0]);
                String examType = parts[2];
                String subjectName = ExamQuestions.getSubjectName(subjectId);

                rawTitle = "Ujian - Soal " + qNum;

                // Header Navigation Links [Soal 1] | [Soal 2] ...
                bodies.add(DialogBody.plainMessage(buildNavBarJava(player, packetId, session, qNum)));

                bodies.add(DialogBody.plainMessage(MiniMessage.miniMessage().deserialize(
                    "<gold><bold>" + subjectName + " (" + examType + ") - Soal " + qNum + "/10</bold></gold>"
                )));
                bodies.add(DialogBody.plainMessage(MiniMessage.miniMessage().deserialize(
                    "<yellow>Pertanyaan:</yellow> <white>" + q.questionText + "</white>"
                )));

                // Render MCQ choices as Action Links in the body
                String currentAns = session.getAnswer(qNum - 1);
                List<String> options = q.options;
                if (options == null || options.isEmpty()) {
                    if (q.questionType.contains("TF") || q.questionType.contains("BS") || q.questionType.contains("BENAR")) {
                        options = Arrays.asList("Benar", "Salah");
                    }
                }

                bodies.add(DialogBody.plainMessage(Component.text(" ")));
                List<io.papermc.paper.registry.data.dialog.input.SingleOptionDialogInput.OptionEntry> answerOptions = new ArrayList<>();

                if (options != null) {
                    for (int i = 0; i < options.size(); i++) {
                        String optText = options.get(i);
                        String optChar = String.valueOf((char) ('A' + i));
                        boolean isSelected = optChar.equalsIgnoreCase(currentAns);

                        answerOptions.add(io.papermc.paper.registry.data.dialog.input.SingleOptionDialogInput.OptionEntry.create(
                            optChar,
                            Component.text(optChar + ". " + optText),
                            isSelected
                        ));

                        if (isSelected) {
                            bodies.add(DialogBody.plainMessage(MiniMessage.miniMessage().deserialize(
                                "<green><bold>" + optChar + ". " + optText + " (Terpilih)</bold></green>"
                            )));
                        } else {
                            final String finalOptChar = optChar;
                            Component optLink = MiniMessage.miniMessage().deserialize(
                                "<white>" + optChar + ". " + optText + "</white>"
                            ).clickEvent(ClickEvent.callback(aud -> {
                                if (aud instanceof Player p) {
                                    session.setAnswer(qNum - 1, finalOptChar);
                                    session.setShowWarning(false);
                                    // Refresh exact same question form to show highlighted choice without transitioning
                                    Bukkit.getScheduler().runTask(plugin, () -> openExamGuiJava(p, "question", packetId, null));
                                }
                            }));
                            bodies.add(DialogBody.plainMessage(optLink));
                        }
                    }
                }

                inputs = List.of(
                    DialogInput.singleOption("answer", Component.text("Pilihan Jawaban"), answerOptions)
                        .width(320)
                        .build()
                );

                // Single Navigation Button at the bottom
                if (qNum < 10) {
                    submitBtn = ActionButton.builder(Component.text("Ke Soal Selanjutnya"))
                        .action(DialogAction.customClick((viewObj, aud) -> {
                            if (aud instanceof Player p) {
                                String selectedAns = viewObj.getText("answer");
                                if (selectedAns != null) {
                                    session.setAnswer(qNum - 1, selectedAns);
                                }
                                session.setCurrentQuestion(qNum + 1);
                                session.setShowWarning(false);
                                Bukkit.getScheduler().runTask(plugin, () -> openExamGuiJava(p, "question", packetId, null));
                            }
                        }, ClickCallback.Options.builder().uses(1).build()))
                        .build();
                } else {
                    submitBtn = ActionButton.builder(Component.text("Selesai & Kirim"))
                        .action(DialogAction.customClick((viewObj, aud) -> {
                            if (aud instanceof Player p) {
                                String selectedAns = viewObj.getText("answer");
                                if (selectedAns != null) {
                                    session.setAnswer(qNum - 1, selectedAns);
                                }
                                session.setCurrentQuestion(11);
                                session.setShowWarning(false);
                                Bukkit.getScheduler().runTask(plugin, () -> openExamGuiJava(p, "confirmation", packetId, null));
                            }
                        }, ClickCallback.Options.builder().uses(1).build()))
                        .build();
                }

                renderDialog(player, rawTitle, bodies, inputs, submitBtn, List.of());
                return;
            }

            case "confirmation": {
                rawTitle = "Konfirmasi Akhir";
                bodies.add(DialogBody.item(new ItemStack(Material.WRITTEN_BOOK)).build());
                bodies.add(DialogBody.plainMessage(MiniMessage.miniMessage().deserialize("<gold><bold>=== Konfirmasi Pengiriman ===</bold></gold>")));
                bodies.add(DialogBody.plainMessage(MiniMessage.miniMessage().deserialize(
                    "<yellow>Apakah Anda yakin ingin menyelesaikan ujian dan mengirimkan jawaban Anda sekarang?</yellow>"
                )));

                List<ExamQuestions.Question> questions = plugin.getExamManager().getQuestionsForPacket(packetId);
                ExamSession session = plugin.getUiManager().getExamSession(player.getUniqueId());

                ActionButton confirmBtn = ActionButton.builder(Component.text("Kirim Jawaban"))
                    .action(DialogAction.customClick((v, aud) -> {
                        if (aud instanceof Player p) {
                            submitExam(p, packetId, session, questions);
                        }
                    }, ClickCallback.Options.builder().uses(1).build()))
                    .build();

                ActionButton backBtn = ActionButton.builder(Component.text("Kembali ke Soal 10"))
                    .action(DialogAction.customClick((v, aud) -> {
                        if (aud instanceof Player p) {
                            if (session != null) {
                                session.setCurrentQuestion(10);
                                openExamGuiJava(p, "question", packetId, null);
                            }
                        }
                    }, ClickCallback.Options.builder().uses(1).build()))
                    .build();

                final String confirmTitle = rawTitle;
                Dialog dialog = Dialog.create(builder -> builder.empty()
                    .base(DialogBase.builder(Component.text(confirmTitle))
                        .canCloseWithEscape(false)
                        .body(bodies)
                        .build())
                    .type(DialogType.confirmation(confirmBtn, backBtn))
                );
                player.showDialog(dialog);
                return;
            }

            case "closed": {
                rawTitle = "Portal Ditutup";
                bodies.add(DialogBody.item(new ItemStack(Material.BARRIER)).build());
                bodies.add(DialogBody.plainMessage(MiniMessage.miniMessage().deserialize("<red><bold>Portal Sedang ditutup!</bold></red>")));
                bodies.add(DialogBody.plainMessage(MiniMessage.miniMessage().deserialize(
                    plugin.getExamManager().getPortalMessage()
                )));
                break;
            }

            case "completed": {
                rawTitle = "Ujian Selesai";
                bodies.add(DialogBody.item(new ItemStack(Material.BOOK)).build());
                bodies.add(DialogBody.plainMessage(MiniMessage.miniMessage().deserialize("<green><bold>Sudah Selesai</bold></green>")));
                bodies.add(DialogBody.plainMessage(MiniMessage.miniMessage().deserialize(
                    "Anda telah menyelesaikan Ujian untuk mata pelajaran ini. Hasil nilai Anda sudah tersimpan di database."
                )));
                break;
            }

            case "semester_closed": {
                rawTitle = "Portal Ditutup";
                bodies.add(DialogBody.item(new ItemStack(Material.BARRIER)).build());
                bodies.add(DialogBody.plainMessage(MiniMessage.miniMessage().deserialize("<red><bold>Portal Semester Ditutup</bold></red>")));
                bodies.add(DialogBody.plainMessage(MiniMessage.miniMessage().deserialize(
                    "Maaf, saat ini portal ujian semester tidak dibuka. Hubungi Kepala Sekolah atau Kementerian!"
                )));
                break;
            }

            case "error": {
                rawTitle = "Error Ujian";
                bodies.add(DialogBody.plainMessage(MiniMessage.miniMessage().deserialize("<red><bold>Gagal Mengirim Ujian</bold></red>")));
                if ("DATABASE_ERROR".equals(packetId)) {
                    bodies.add(DialogBody.plainMessage(MiniMessage.miniMessage().deserialize(
                        "<red>Gagal mengirim jawaban ke database. Jawaban Anda masih aman di memori server. Hubungi pengawas teknis!</red>"
                    )));
                } else {
                    bodies.add(DialogBody.plainMessage(MiniMessage.miniMessage().deserialize(
                        "Maaf terjadi kesalahan (Code: 2). Jika masalah berlanjut hubungi kementerian."
                    )));
                }
                break;
            }
        }

        // Render generic notice screen with single Close button
        if (submitBtn == null) {
            submitBtn = ActionButton.builder(Component.text("Tutup"))
                .action(DialogAction.customClick((v, aud) -> {}, ClickCallback.Options.builder().uses(1).build()))
                .build();
        }

        renderDialog(player, rawTitle, bodies, inputs, submitBtn, List.of());
    }

    private Component buildNavBarJava(Player player, String packetId, ExamSession session, int currentQ) {
        Component nav = Component.empty();
        for (int i = 1; i <= 10; i++) {
            final int idx = i;
            String ans = session.getAnswer(idx - 1);
            boolean isCurrent = (idx == currentQ);
            String color;
            if (isCurrent) {
                color = "<gold><bold>";
            } else if (ans != null) {
                color = "<green>";
            } else {
                color = "<gray>";
            }
            String closing = isCurrent ? "</bold></gold>" : (ans != null ? "</green>" : "</gray>");
            Component link = MiniMessage.miniMessage().deserialize(color + "[S" + idx + "]" + closing)
                .clickEvent(ClickEvent.callback(aud -> {
                    if (aud instanceof Player p) {
                        session.setCurrentQuestion(idx);
                        session.setShowWarning(false);
                        // Open exact same GUI view
                        Bukkit.getScheduler().runTask(plugin, () -> openExamGuiJava(p, "question", packetId, null));
                    }
                }));
            nav = nav.append(link);
            if (i < 10) {
                nav = nav.append(Component.text(" | "));
            }
        }
        return nav;
    }

    private void renderDialog(Player player, String rawTitle, List<DialogBody> bodies, List<DialogInput> inputs, ActionButton submitBtn, List<ActionButton> buttons) {
        Dialog dialog;
        if (buttons != null && !buttons.isEmpty()) {
            dialog = Dialog.create(builder -> builder.empty()
                .base(DialogBase.builder(Component.text(rawTitle))
                    .canCloseWithEscape(true)
                    .body(bodies)
                    .inputs(inputs)
                    .build())
                .type(DialogType.multiAction(buttons, submitBtn, 2))
            );
        } else {
            dialog = Dialog.create(builder -> builder.empty()
                .base(DialogBase.builder(Component.text(rawTitle))
                    .canCloseWithEscape(true)
                    .body(bodies)
                    .inputs(inputs)
                    .build())
                .type(DialogType.notice(submitBtn))
            );
        }

        player.showDialog(dialog);
    }

    private void handleSubjectStartJava(Player player, String examType, String targetPacketId) {
        List<ExamQuestions.Question> questions = plugin.getExamManager().getQuestionsForPacket(targetPacketId);
        if (questions == null || questions.size() != 10) {
            openExamGuiJava(player, "subject_select", examType, "Maaf, paket soal ini tidak lengkap.");
            return;
        }

        if (("UTS".equalsIgnoreCase(examType) || "UAS".equalsIgnoreCase(examType))
            && plugin.getExamManager().isSemesterBreak()) {
            openExamGuiJava(player, "subject_select", examType, "Sedang masa jeda/istirahat antar mata pelajaran.");
            return;
        }

        boolean active;
        if ("UH".equalsIgnoreCase(examType)) {
            active = plugin.getExamManager().getActiveUhPackets().contains(targetPacketId);
        } else {
            active = "OPEN".equalsIgnoreCase(plugin.getExamManager().getPortalSemesterStatus())
                && plugin.getExamManager().getCurrentActiveSemesterPackets().contains(targetPacketId);
        }

        if (!active) {
            openExamGuiJava(player, "subject_select", examType, "Mata pelajaran tersebut tidak aktif!");
            return;
        }

        CompletableFuture.supplyAsync(() -> plugin.getDatabaseManager().hasAttemptedExam(player.getUniqueId(), targetPacketId),
            runnable -> Bukkit.getScheduler().runTaskAsynchronously(plugin, runnable))
        .thenAccept(hasAttempted -> {
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (hasAttempted) {
                    player.closeInventory();
                    openExamGuiJava(player, "completed", targetPacketId, null);
                } else {
                    plugin.getUiManager().startExamSession(player, targetPacketId);
                    openExamGuiJava(player, "pre_exam", targetPacketId, null);
                }
            });
        });
    }

    // ─────────────────────────────────────────────────────────────────────────
    // BEDROCK EDITION — Geyser/Floodgate Cumulus Form (Consolidated)
    // ─────────────────────────────────────────────────────────────────────────

    public void openExamGuiBedrock(Player player, String view, String packetId, String errorMsg) {
        String title = "Portal Ujian";
        String content = "";

        if (errorMsg != null && !errorMsg.isEmpty()) {
            content = "§c[!] Error: " + errorMsg + "\n\n";
        }

        switch (view.toLowerCase()) {
            case "portal": {
                if (!plugin.isExamOpen()) {
                    openExamGuiBedrock(player, "closed", null, null);
                    return;
                }

                String body = (errorMsg != null && !errorMsg.isEmpty() ? "§c[!] Error: " + errorMsg + "\n\n" : "")
                    + "Selamat datang di Portal Sekolah\nSilakan pilih tipe ujian di bawah ini:";
                SimpleForm form = SimpleForm.builder()
                    .title("Portal Sekolah")
                    .content(body)
                    .button("[ Ujian Harian ]")
                    .button("[ Ujian Semester ]")
                    .button("[ Ujian Akhir Semester ]")
                    .button("Tutup")
                    .validResultHandler(response -> {
                        int clickedId = response.clickedButtonId();
                        if (clickedId == 0) {
                            openExamGuiBedrock(player, "subject_select", "UH", null);
                        } else if (clickedId == 1) {
                            if (!"OPEN".equalsIgnoreCase(plugin.getExamManager().getPortalSemesterStatus())) {
                                openExamGuiBedrock(player, "semester_closed", null, null);
                            } else {
                                openExamGuiBedrock(player, "subject_select", "UTS", null);
                            }
                        } else if (clickedId == 2) {
                            if (!"OPEN".equalsIgnoreCase(plugin.getExamManager().getPortalSemesterStatus())) {
                                openExamGuiBedrock(player, "semester_closed", null, null);
                            } else {
                                openExamGuiBedrock(player, "subject_select", "UAS", null);
                            }
                        }
                    })
                    .build();

                FloodgateApi.getInstance().sendForm(player.getUniqueId(), form);
                return;
            }

            case "subject_select": {
                final String examType = packetId != null ? packetId.toUpperCase() : "UH";

                CompletableFuture.supplyAsync(() -> plugin.getDatabaseManager().getAttemptedPackets(player.getUniqueId()),
                    runnable -> Bukkit.getScheduler().runTaskAsynchronously(plugin, runnable))
                .thenAccept(attempts -> {
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        StudentProfile profile = plugin.getProfileManager().getProfile(player.getUniqueId());
                        if (profile == null) {
                            openExamGuiBedrock(player, "error", null, "Gagal memuat profil.");
                            return;
                        }
                        int academicClass = profile.getAcademicClass();

                        StringBuilder contentBuilder = new StringBuilder();
                        contentBuilder.append("=== Portal Ujian ").append(examType).append(" ===\n\n");
                        if (errorMsg != null && !errorMsg.isEmpty()) {
                            contentBuilder.append("§c[!] Error: ").append(errorMsg).append("\n\n");
                        }

                        contentBuilder.append("Pilih mata pelajaran di bawah ini untuk memulai:\n\n");
                        contentBuilder.append("§7Daftar Mata Pelajaran:§r\n");

                        for (int subjectId = 1; subjectId <= 7; subjectId++) {
                            String subjectName = ExamQuestions.getSubjectName(subjectId);
                            String targetPacketId = subjectId + "_" + academicClass + "_" + examType;

                            boolean completed = attempts.contains(targetPacketId);
                            boolean active;
                            if ("UH".equalsIgnoreCase(examType)) {
                                active = plugin.getExamManager().getActiveUhPackets().contains(targetPacketId);
                            } else {
                                active = "OPEN".equalsIgnoreCase(plugin.getExamManager().getPortalSemesterStatus())
                                    && plugin.getExamManager().getCurrentActiveSemesterPackets().contains(targetPacketId);
                            }

                            List<ExamQuestions.Question> qList = plugin.getExamManager().getQuestionsForPacket(targetPacketId);
                            boolean isComplete = (qList != null && qList.size() == 10);

                            String statusLegend;
                            if (!isComplete) {
                                statusLegend = "§e[Tidak Lengkap]§r";
                            } else if (completed) {
                                statusLegend = "§a[Sudah Selesai]§r";
                            } else if (active) {
                                statusLegend = "§e[Aktif]§r";
                            } else {
                                statusLegend = "§c[Tidak Aktif]§r";
                            }

                            contentBuilder.append("• ").append(subjectName).append(": ").append(statusLegend).append("\n");
                        }

                        CustomForm form = CustomForm.builder()
                            .title("Portal Ujian - " + examType)
                            .label(contentBuilder.toString())
                            .dropdown("Pilih Mata Pelajaran",
                                "Pengetahuan Umum",
                                "Pendidikan Pancasila",
                                "Bahasa Indonesia",
                                "Bahasa Inggris",
                                "Matematika",
                                "Ilmu Pengetahuan Alam",
                                "Ilmu Pengetahuan Sosial"
                            )
                            .validResultHandler(response -> {
                                int selectedIndex = response.asDropdown(1);
                                int subjectId = selectedIndex + 1;
                                String targetPacketId = subjectId + "_" + academicClass + "_" + examType;
                                handleSubjectStartBedrock(player, examType, targetPacketId);
                            })
                            .closedResultHandler(() -> {
                                if (!player.isOnline()) return;
                                openExamGuiBedrock(player, "portal", null, null);
                            })
                            .build();

                        FloodgateApi.getInstance().sendForm(player.getUniqueId(), form);
                    });
                });
                return;
            }

            case "pre_exam": {
                String[] parts = packetId.split("_");
                int subjectId = Integer.parseInt(parts[0]);
                String examType = parts[2];
                String subjectName = ExamQuestions.getSubjectName(subjectId);

                StudentProfile profile = plugin.getProfileManager().getProfile(player.getUniqueId());
                String name = player.getName();
                String nis = (profile != null && profile.getNis() != null) ? profile.getNis() : "";
                int academicClass = (profile != null) ? profile.getAcademicClass() : 0;

                SimpleForm form = SimpleForm.builder()
                    .title("Pra-Ujian")
                    .content("Siswa: " + name + " | NIS: " + nis + " | Kelas: " + academicClass + "\n\n"
                        + "Mata Pelajaran: " + subjectName + " (" + examType + ")\n"
                        + "Jumlah Soal: 10 Pertanyaan\n\n"
                        + "Petunjuk: Jawab semua pertanyaan dengan teliti.\n\nApakah Anda siap memulai?")
                    .button("Mulai Ujian")
                    .button("Batal")
                    .validResultHandler(response -> {
                        int clickedId = response.clickedButtonId();
                        if (clickedId == 0) {
                            ExamSession session = plugin.getUiManager().getExamSession(player.getUniqueId());
                            if (session != null) {
                                session.setCurrentQuestion(1);
                                openExamGuiBedrock(player, "question", packetId, null);
                            }
                        } else {
                            plugin.getUiManager().clearExamSession(player);
                            openExamGuiBedrock(player, "subject_select", examType, null);
                        }
                    })
                    .closedResultHandler(() -> {
                        if (!player.isOnline()) return;
                        openExamGuiBedrock(player, "pre_exam", packetId, null);
                    })
                    .build();

                FloodgateApi.getInstance().sendForm(player.getUniqueId(), form);
                return;
            }

            case "question": {
                ExamSession session = plugin.getUiManager().getExamSession(player.getUniqueId());
                if (session == null) {
                    openExamGuiBedrock(player, "portal", null, null);
                    return;
                }

                List<ExamQuestions.Question> questions = plugin.getExamManager().getQuestionsForPacket(packetId);
                int qNum = session.getCurrentQuestion();
                if (questions == null || questions.isEmpty() || qNum < 1 || qNum > questions.size()) {
                    openExamGuiBedrock(player, "error", null, null);
                    return;
                }

                ExamQuestions.Question q = questions.get(qNum - 1);
                String currentAns = session.getAnswer(qNum - 1);
                String[] parts = packetId.split("_");
                int subjectId = Integer.parseInt(parts[0]);
                String examType = parts[2];
                String subjectName = ExamQuestions.getSubjectName(subjectId);

                List<String> options = q.options;
                if (options == null || options.isEmpty()) {
                    if (q.questionType.contains("TF") || q.questionType.contains("BS") || q.questionType.contains("BENAR")) {
                        options = Arrays.asList("Benar", "Salah");
                    }
                }
                final List<String> finalOptions = options;

                SimpleForm.Builder formBuilder = SimpleForm.builder()
                    .title("Soal " + qNum + "/10")
                    .content("Mata Pelajaran: " + subjectName + " (" + examType + ")\n\n"
                        + "Pertanyaan:\n" + q.questionText + "\n\nPilih jawaban Anda:");

                // Render MCQ choices as buttons
                if (finalOptions != null) {
                    for (int i = 0; i < finalOptions.size(); i++) {
                        String optText = finalOptions.get(i);
                        String optChar = String.valueOf((char) ('A' + i));
                        boolean isSelected = optChar.equalsIgnoreCase(currentAns);

                        String btnText = isSelected
                            ? "§a§l[✓] " + optChar + ". " + cleanBedrockText(optText) + " (Terpilih)"
                            : optChar + ". " + cleanBedrockText(optText);

                        formBuilder.button(btnText);
                    }
                }

                // Add Navigation Buttons at the bottom
                String nextText = (qNum < 10) ? "§e[Soal Selanjutnya]" : "§e[Selesai & Kirim]";
                formBuilder.button(nextText);

                String prevText = (qNum > 1) ? "§7[Soal Sebelumnya]" : "§7[Kembali ke Pra-Ujian]";
                formBuilder.button(prevText);

                formBuilder.button("§6[Lompat ke Soal...]");

                formBuilder.validResultHandler(response -> {
                    int clickedId = response.clickedButtonId();
                    int numOptions = (finalOptions != null) ? finalOptions.size() : 0;

                    // Option buttons
                    if (clickedId >= 0 && clickedId < numOptions) {
                        String optChar = String.valueOf((char) ('A' + clickedId));
                        session.setAnswer(qNum - 1, optChar);
                        session.setShowWarning(false);
                        // Refresh GUI without transitioning (Hijau Bold)
                        openExamGuiBedrock(player, "question", packetId, null);
                        return;
                    }

                    // Next/Submit button
                    if (clickedId == numOptions) {
                        if (qNum < 10) {
                            session.setCurrentQuestion(qNum + 1);
                            openExamGuiBedrock(player, "question", packetId, null);
                        } else {
                            session.setCurrentQuestion(11);
                            openExamGuiBedrock(player, "confirmation", packetId, null);
                        }
                        return;
                    }

                    // Previous button
                    if (clickedId == numOptions + 1) {
                        if (qNum > 1) {
                            session.setCurrentQuestion(qNum - 1);
                            openExamGuiBedrock(player, "question", packetId, null);
                        } else {
                            session.setCurrentQuestion(0);
                            openExamGuiBedrock(player, "pre_exam", packetId, null);
                        }
                        return;
                    }

                    // Jump to question button
                    if (clickedId == numOptions + 2) {
                        CustomForm jumpForm = CustomForm.builder()
                            .title("Lompat ke Soal")
                            .dropdown("Pilih Soal", "Soal 1", "Soal 2", "Soal 3", "Soal 4", "Soal 5", "Soal 6", "Soal 7", "Soal 8", "Soal 9", "Soal 10")
                            .validResultHandler(resp -> {
                                int selectedIdx = resp.asDropdown(1) + 1;
                                session.setCurrentQuestion(selectedIdx);
                                openExamGuiBedrock(player, "question", packetId, null);
                            })
                            .closedResultHandler(() -> {
                                if (!player.isOnline()) return;
                                openExamGuiBedrock(player, "question", packetId, null);
                            })
                            .build();

                        FloodgateApi.getInstance().sendForm(player.getUniqueId(), jumpForm);
                    }
                })
                .closedResultHandler(() -> {
                    if (!player.isOnline()) return;
                    openExamGuiBedrock(player, "question", packetId, null);
                });

                FloodgateApi.getInstance().sendForm(player.getUniqueId(), formBuilder.build());
                return;
            }

            case "confirmation": {
                ExamSession session = plugin.getUiManager().getExamSession(player.getUniqueId());
                SimpleForm form = SimpleForm.builder()
                    .title("Konfirmasi Akhir")
                    .content("Apakah Anda yakin ingin menyelesaikan ujian dan mengirimkan jawaban Anda sekarang?")
                    .button("Kirim Jawaban")
                    .button("Kembali ke Soal 10")
                    .validResultHandler(response -> {
                        int clickedId = response.clickedButtonId();
                        List<ExamQuestions.Question> questions = plugin.getExamManager().getQuestionsForPacket(packetId);
                        if (clickedId == 0) {
                            submitExam(player, packetId, session, questions);
                        } else {
                            if (session != null) {
                                session.setCurrentQuestion(10);
                                openExamGuiBedrock(player, "question", packetId, null);
                            }
                        }
                    })
                    .closedResultHandler(() -> {
                        if (!player.isOnline()) return;
                        openExamGuiBedrock(player, "confirmation", packetId, null);
                    })
                    .build();

                FloodgateApi.getInstance().sendForm(player.getUniqueId(), form);
                return;
            }

            case "closed": {
                String adminMessage = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(
                    MiniMessage.miniMessage().deserialize(plugin.getExamManager().getPortalMessage())
                );
                SimpleForm form = SimpleForm.builder()
                    .title("Portal Ditutup")
                    .content("Portal Sedang ditutup!\n\n" + adminMessage)
                    .button("Tutup")
                    .build();

                FloodgateApi.getInstance().sendForm(player.getUniqueId(), form);
                return;
            }

            case "completed": {
                SimpleForm form = SimpleForm.builder()
                    .title("Ujian Selesai")
                    .content("Anda telah menyelesaikan Ujian untuk mata pelajaran ini. Hasil nilai Anda sudah tersimpan di database.")
                    .button("Tutup")
                    .build();

                FloodgateApi.getInstance().sendForm(player.getUniqueId(), form);
                return;
            }

            case "semester_closed": {
                SimpleForm form = SimpleForm.builder()
                    .title("Portal Ditutup")
                    .content("Maaf, saat ini portal ujian semester tidak dibuka. Hubungi Kepala Sekolah atau Kementerian!")
                    .button("Ok")
                    .build();

                FloodgateApi.getInstance().sendForm(player.getUniqueId(), form);
                return;
            }

            case "error": {
                String errorLabel = (packetId != null && packetId.equals("DATABASE_ERROR"))
                    ? "Gagal mengirim jawaban ke database. Jawaban Anda masih aman di memori server. Hubungi pengawas teknis!"
                    : "Maaf terjadi kesalahan (Code: 2). Jika masalah berlanjut hubungi kementerian.";

                SimpleForm form = SimpleForm.builder()
                    .title("Error Ujian")
                    .content(errorLabel)
                    .button("Ok")
                    .build();

                FloodgateApi.getInstance().sendForm(player.getUniqueId(), form);
                return;
            }
        }
    }

    private void handleSubjectStartBedrock(Player player, String examType, String targetPacketId) {
        List<ExamQuestions.Question> questions = plugin.getExamManager().getQuestionsForPacket(targetPacketId);
        if (questions == null || questions.size() != 10) {
            openExamGuiBedrock(player, "subject_select", examType, "Maaf, paket soal ini tidak lengkap.");
            return;
        }

        if (("UTS".equalsIgnoreCase(examType) || "UAS".equalsIgnoreCase(examType))
            && plugin.getExamManager().isSemesterBreak()) {
            openExamGuiBedrock(player, "subject_select", examType, "Sedang masa jeda/istirahat antar mata pelajaran.");
            return;
        }

        boolean active;
        if ("UH".equalsIgnoreCase(examType)) {
            active = plugin.getExamManager().getActiveUhPackets().contains(targetPacketId);
        } else {
            active = "OPEN".equalsIgnoreCase(plugin.getExamManager().getPortalSemesterStatus())
                && plugin.getExamManager().getCurrentActiveSemesterPackets().contains(targetPacketId);
        }

        if (!active) {
            openExamGuiBedrock(player, "subject_select", examType, "Mata pelajaran tersebut tidak aktif!");
            return;
        }

        CompletableFuture.supplyAsync(() -> plugin.getDatabaseManager().hasAttemptedExam(player.getUniqueId(), targetPacketId),
            runnable -> Bukkit.getScheduler().runTaskAsynchronously(plugin, runnable))
        .thenAccept(hasAttempted -> {
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (hasAttempted) {
                    openExamGuiBedrock(player, "completed", targetPacketId, null);
                } else {
                    plugin.getUiManager().startExamSession(player, targetPacketId);
                    openExamGuiBedrock(player, "pre_exam", targetPacketId, null);
                }
            });
        });
    }
}
