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
 * Single-engine centralized dynamic GUI for the Exam Subsystem (v1.6.5).
 * Powered by State-Passing and anti-retake database isolation.
 */
public class ExamGui {

    private final NaturalSchool plugin;

    public ExamGui(NaturalSchool plugin) {
        this.plugin = plugin;
    }

    private String cleanBedrockText(String text) {
        if (text == null) return "";
        return text.replaceAll("§[0-9a-fk-orA-FK-OR]", "").replace("○", "").replace("●", "").trim();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // JAVA EDITION — Paper Dialog API
    // ─────────────────────────────────────────────────────────────────────────

    public void openExamPortalJava(Player player) {
        // Global Portal master check
        if (!plugin.isExamOpen()) {
            openExamClosedJava(player);
            return;
        }

        List<DialogBody> bodies = List.of(
            DialogBody.item(new ItemStack(Material.BOOKSHELF)).build(),
            DialogBody.plainMessage(MiniMessage.miniMessage().deserialize("<gold><bold>Portal Sekolah</bold></gold>")),
            DialogBody.plainMessage(MiniMessage.miniMessage().deserialize("<yellow>Selamat datang di Portal Sekolah</yellow>"))
        );

        List<ActionButton> buttons = List.of(
            ActionButton.builder(Component.text("[ Ujian Harian ]"))
                .action(DialogAction.customClick((view, audience) -> {
                    if (audience instanceof Player p) {
                        openPortalUjianJava(p, "UH", null);
                    }
                }, ClickCallback.Options.builder().uses(1).build()))
                .build(),
            ActionButton.builder(Component.text("[ Ujian Semester ]"))
                .action(DialogAction.customClick((view, audience) -> {
                    if (audience instanceof Player p) {
                        openPortalUjianJava(p, "UTS", null);
                    }
                }, ClickCallback.Options.builder().uses(1).build()))
                .build(),
            ActionButton.builder(Component.text("[ Ujian Akhir Semester / Kelulusan ]"))
                .action(DialogAction.customClick((view, audience) -> {
                    if (audience instanceof Player p) {
                        openPortalUjianJava(p, "UAS", null);
                    }
                }, ClickCallback.Options.builder().uses(1).build()))
                .build()
        );

        ActionButton closeBtn = ActionButton.builder(Component.text("Tutup"))
            .action(DialogAction.customClick((view, audience) -> {}, ClickCallback.Options.builder().uses(1).build()))
            .build();

        Dialog dialog = Dialog.create(builder -> builder.empty()
            .base(DialogBase.builder(Component.text("Portal Sekolah"))
                .canCloseWithEscape(true)
                .body(bodies)
                .build())
            .type(DialogType.multiAction(buttons, closeBtn, 2))
        );

        player.showDialog(dialog);
    }

    public void openPortalUjianJava(Player player, String examType, String warning) {
        CompletableFuture.supplyAsync(() -> plugin.getDatabaseManager().getAttemptedPackets(player.getUniqueId()),
            runnable -> Bukkit.getScheduler().runTaskAsynchronously(plugin, runnable))
        .thenAccept(attempts -> {
            Bukkit.getScheduler().runTask(plugin, () -> {
                renderPortalUjianJava(player, examType, warning, attempts);
            });
        });
    }

    private void renderPortalUjianJava(Player player, String examType, String warning, List<String> attemptedPackets) {
        StudentProfile profile = plugin.getProfileManager().getProfile(player.getUniqueId());
        if (profile == null) {
            showErrorCode2Java(player);
            return;
        }
        int academicClass = profile.getAcademicClass();
        String name = player.getName();
        String nis = profile.getNis() != null ? profile.getNis() : "";

        List<DialogBody> bodies = new ArrayList<>();
        bodies.add(DialogBody.item(new ItemStack(Material.BOOK)).build());
        bodies.add(DialogBody.plainMessage(MiniMessage.miniMessage().deserialize("<gold><bold>Portal Ujian - " + examType + "</bold></gold>")));
        bodies.add(DialogBody.plainMessage(MiniMessage.miniMessage().deserialize("<gray>Siswa: " + name + " | NIS: " + nis + " | Kelas: " + academicClass + "</gray>")));

        if (warning != null && !warning.isEmpty()) {
            bodies.add(DialogBody.plainMessage(MiniMessage.miniMessage().deserialize(warning)));
        } else {
            bodies.add(DialogBody.plainMessage(MiniMessage.miniMessage().deserialize("<yellow>Pilih mata pelajaran di bawah ini untuk memulai:</yellow>")));
        }

        List<ActionButton> buttons = new ArrayList<>();

        int startHour = plugin.getConfig().getInt("exam-schedule.start-hour", 10);
        int endHour = plugin.getConfig().getInt("exam-schedule.end-hour", 16);
        String hours = String.format("%02d:00 - %02d:00", startHour, endHour);

        for (int subjectId = 1; subjectId <= 7; subjectId++) {
            String subjectName = ExamQuestions.getSubjectName(subjectId);
            String targetPacketId = subjectId + "_" + academicClass + "_" + examType;

            // Determine status
            boolean completed = attemptedPackets.contains(targetPacketId);
            boolean active = false;
            if ("UH".equalsIgnoreCase(examType)) {
                active = plugin.getExamManager().getActiveUhPackets().contains(targetPacketId);
            } else {
                active = "OPEN".equalsIgnoreCase(plugin.getExamManager().getPortalSemesterStatus())
                    && plugin.getExamManager().getCurrentActiveSemesterPackets().contains(targetPacketId);
            }

            String statusLegend;
            if (completed) {
                statusLegend = "<green>[Sudah Selesai]</green>";
            } else if (active) {
                statusLegend = "<yellow>[Aktif]</yellow>";
            } else {
                statusLegend = "<red>[Tidak Aktif]</red>";
            }

            String buttonText = subjectName + " - " + statusLegend + " (" + hours + ")";

            buttons.add(ActionButton.builder(MiniMessage.miniMessage().deserialize(buttonText))
                .action(DialogAction.customClick((view, audience) -> {
                    if (audience instanceof Player p) {
                        handleSubjectClickJava(p, examType, targetPacketId);
                    }
                }, ClickCallback.Options.builder().uses(1).build()))
                .build());
        }

        ActionButton backBtn = ActionButton.builder(Component.text("Kembali"))
            .action(DialogAction.customClick((view, audience) -> {
                if (audience instanceof Player p) {
                    openExamPortalJava(p);
                }
            }, ClickCallback.Options.builder().uses(1).build()))
            .build();

        Dialog dialog = Dialog.create(builder -> builder.empty()
            .base(DialogBase.builder(Component.text("Portal Ujian - " + examType))
                .canCloseWithEscape(true)
                .body(bodies)
                .build())
            .type(DialogType.multiAction(buttons, backBtn, 2))
        );

        player.showDialog(dialog);
    }

    private void handleSubjectClickJava(Player player, String examType, String targetPacketId) {
        // Scenario 1: "TIDAK AKTIF"
        boolean active = false;
        if ("UH".equalsIgnoreCase(examType)) {
            active = plugin.getExamManager().getActiveUhPackets().contains(targetPacketId);
        } else {
            active = "OPEN".equalsIgnoreCase(plugin.getExamManager().getPortalSemesterStatus())
                && plugin.getExamManager().getCurrentActiveSemesterPackets().contains(targetPacketId);
        }

        if (!active) {
            openPortalUjianJava(player, examType, "<red><bold>[!] Error: Mata pelajaran tersebut sedang tidak aktif!</bold></red>");
            return;
        }

        // Scenario 2: "TIDAK ADA SOAL"
        List<ExamQuestions.Question> questions = plugin.getExamManager().getQuestionsForPacket(targetPacketId);
        if (questions == null || questions.isEmpty()) {
            openPortalUjianJava(player, examType, "<yellow><bold>[!] Peringatan: Tidak ada soal pada mata pelajaran tersebut! (Hubungi Pengawas)</bold></yellow>");
            return;
        }

        // Scenario 3: "SUDAH MENGERJAKAN"
        CompletableFuture.supplyAsync(() -> plugin.getDatabaseManager().hasAttemptedExam(player.getUniqueId(), targetPacketId),
            runnable -> Bukkit.getScheduler().runTaskAsynchronously(plugin, runnable))
        .thenAccept(hasAttempted -> {
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (hasAttempted) {
                    player.closeInventory();
                    showExamAlreadyCompletedJava(player);
                } else {
                    plugin.getUiManager().startExamSession(player, targetPacketId);
                    plugin.getUiManager().openExamPre(player, targetPacketId);
                }
            });
        });
    }

    public void showExamAlreadyCompletedJava(Player player) {
        List<DialogBody> bodies = List.of(
            DialogBody.plainMessage(MiniMessage.miniMessage().deserialize("<green><bold>Ujian Selesai</bold></green>")),
            DialogBody.plainMessage(MiniMessage.miniMessage().deserialize("§aKamu telah menyelesaikan Ujian untuk mata pelajaran ini. Silahkan tunggu hasil keluar!"))
        );

        ActionButton okBtn = ActionButton.builder(Component.text("Ok"))
            .action(DialogAction.customClick((view, audience) -> {}, ClickCallback.Options.builder().uses(1).build()))
            .build();

        Dialog dialog = Dialog.create(builder -> builder.empty()
            .base(DialogBase.builder(Component.text("Ujian Selesai"))
                .canCloseWithEscape(true)
                .body(bodies)
                .build())
            .type(DialogType.notice(okBtn))
        );

        player.showDialog(dialog);
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
                    openPortalUjianJava(p, parts[2], null);
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

                        // 2. Parse packet_id dynamically
                        String[] parts = packetId.split("_");
                        int subjectId = Integer.parseInt(parts[0]);
                        int academicClass = Integer.parseInt(parts[1]);
                        String examType = parts[2];

                        // 3. Upsert Rapor
                        plugin.getDatabaseManager().upsertStudentRapor(p.getUniqueId(), nis, academicClass, semester, subjectId, examType, pctScore);
                    }, runnable -> Bukkit.getScheduler().runTaskAsynchronously(plugin, runnable))
                    .thenRun(() -> {
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

    // ─────────────────────────────────────────────────────────────────────────
    // BEDROCK EDITION — Geyser/Floodgate Cumulus Form
    // ─────────────────────────────────────────────────────────────────────────

    public void openExamPortalBedrock(Player player) {
        // Global Portal master check
        if (!plugin.isExamOpen()) {
            openExamClosedBedrock(player);
            return;
        }

        SimpleForm form = SimpleForm.builder()
            .title("Portal Sekolah")
            .content("Selamat datang di Portal Sekolah")
            .button("[ Ujian Harian ]")
            .button("[ Ujian Semester ]")
            .button("[ Ujian Ujian Akhir Semester / Kelulusan ]") // Matches standard request UI path
            .button("Tutup")
            .validResultHandler(response -> {
                int clickedId = response.clickedButtonId();
                if (clickedId == 0) {
                    openPortalUjianBedrock(player, "UH", null);
                } else if (clickedId == 1) {
                    openPortalUjianBedrock(player, "UTS", null);
                } else if (clickedId == 2) {
                    openPortalUjianBedrock(player, "UAS", null);
                }
            })
            .build();

        FloodgateApi.getInstance().sendForm(player.getUniqueId(), form);
    }

    public void openPortalUjianBedrock(Player player, String examType, String warning) {
        CompletableFuture.supplyAsync(() -> plugin.getDatabaseManager().getAttemptedPackets(player.getUniqueId()),
            runnable -> Bukkit.getScheduler().runTaskAsynchronously(plugin, runnable))
        .thenAccept(attempts -> {
            Bukkit.getScheduler().runTask(plugin, () -> {
                renderPortalUjianBedrock(player, examType, warning, attempts);
            });
        });
    }

    private void renderPortalUjianBedrock(Player player, String examType, String warning, List<String> attemptedPackets) {
        StudentProfile profile = plugin.getProfileManager().getProfile(player.getUniqueId());
        if (profile == null) {
            showErrorCode2Bedrock(player);
            return;
        }
        int academicClass = profile.getAcademicClass();
        String name = player.getName();
        String nis = profile.getNis() != null ? profile.getNis() : "";

        String title = "Portal Ujian - " + examType;

        StringBuilder content = new StringBuilder();
        content.append("Siswa: ").append(name).append(" | NIS: ").append(nis).append(" | Kelas: ").append(academicClass).append("\n\n");
        if (warning != null && !warning.isEmpty()) {
            content.append(warning).append("\n\n");
        } else {
            content.append("Pilih mata pelajaran di bawah ini untuk memulai:\n\n");
        }

        SimpleForm.Builder formBuilder = SimpleForm.builder()
            .title(title)
            .content(content.toString());

        int startHour = plugin.getConfig().getInt("exam-schedule.start-hour", 10);
        int endHour = plugin.getConfig().getInt("exam-schedule.end-hour", 16);
        String hours = String.format("%02d:00 - %02d:00", startHour, endHour);

        List<String> targetPacketIds = new ArrayList<>();

        for (int subjectId = 1; subjectId <= 7; subjectId++) {
            String subjectName = ExamQuestions.getSubjectName(subjectId);
            String targetPacketId = subjectId + "_" + academicClass + "_" + examType;
            targetPacketIds.add(targetPacketId);

            // Determine status
            boolean completed = attemptedPackets.contains(targetPacketId);
            boolean active = false;
            if ("UH".equalsIgnoreCase(examType)) {
                active = plugin.getExamManager().getActiveUhPackets().contains(targetPacketId);
            } else {
                active = "OPEN".equalsIgnoreCase(plugin.getExamManager().getPortalSemesterStatus())
                    && plugin.getExamManager().getCurrentActiveSemesterPackets().contains(targetPacketId);
            }

            String statusLegend;
            if (completed) {
                statusLegend = "[Sudah Selesai]";
            } else if (active) {
                statusLegend = "[Aktif]";
            } else {
                statusLegend = "[Tidak Aktif]";
            }

            String buttonText = subjectName + " - " + statusLegend + " (" + hours + ")";
            formBuilder.button(buttonText);
        }

        formBuilder.button("Kembali");

        formBuilder.validResultHandler(response -> {
            int clickedId = response.clickedButtonId();
            if (clickedId == 7) {
                openExamPortalBedrock(player);
                return;
            }
            if (clickedId >= 0 && clickedId < 7) {
                String targetPacketId = targetPacketIds.get(clickedId);
                handleSubjectClickBedrock(player, examType, targetPacketId);
            }
        });

        FloodgateApi.getInstance().sendForm(player.getUniqueId(), formBuilder.build());
    }

    private void handleSubjectClickBedrock(Player player, String examType, String targetPacketId) {
        // Scenario 1: "TIDAK AKTIF"
        boolean active = false;
        if ("UH".equalsIgnoreCase(examType)) {
            active = plugin.getExamManager().getActiveUhPackets().contains(targetPacketId);
        } else {
            active = "OPEN".equalsIgnoreCase(plugin.getExamManager().getPortalSemesterStatus())
                && plugin.getExamManager().getCurrentActiveSemesterPackets().contains(targetPacketId);
        }

        if (!active) {
            openPortalUjianBedrock(player, examType, "§c[!] Error: Mata pelajaran tersebut sedang tidak aktif!");
            return;
        }

        // Scenario 2: "TIDAK ADA SOAL"
        List<ExamQuestions.Question> questions = plugin.getExamManager().getQuestionsForPacket(targetPacketId);
        if (questions == null || questions.isEmpty()) {
            openPortalUjianBedrock(player, examType, "§e[!] Peringatan: Tidak ada soal pada mata pelajaran tersebut! (Hubungi Pengawas)");
            return;
        }

        // Scenario 3: "SUDAH MENGERJAKAN"
        CompletableFuture.supplyAsync(() -> plugin.getDatabaseManager().hasAttemptedExam(player.getUniqueId(), targetPacketId),
            runnable -> Bukkit.getScheduler().runTaskAsynchronously(plugin, runnable))
        .thenAccept(hasAttempted -> {
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (hasAttempted) {
                    showExamAlreadyCompletedBedrock(player);
                } else {
                    plugin.getUiManager().startExamSession(player, targetPacketId);
                    plugin.getUiManager().openExamPre(player, targetPacketId);
                }
            });
        });
    }

    public void showExamAlreadyCompletedBedrock(Player player) {
        SimpleForm form = SimpleForm.builder()
            .title("Ujian Selesai")
            .content("§aKamu telah menyelesaikan Ujian untuk mata pelajaran ini. Silahkan tunggu hasil keluar!")
            .button("Ok")
            .build();

        FloodgateApi.getInstance().sendForm(player.getUniqueId(), form);
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
                    openPortalUjianBedrock(player, parts[2], null);
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
                
                // Sanitize Bedrock UI buttons option texts of raw format codes and decorative prefixes
                String optTextClean = cleanBedrockText(optText);
                String btnText = optChar + ". " + optTextClean + (optChar.equalsIgnoreCase(currentAns) ? " (Terpilih)" : "");
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

                        // 2. Parse packet_id dynamically
                        String[] parts = packetId.split("_");
                        int subjectId = Integer.parseInt(parts[0]);
                        int academicClass = Integer.parseInt(parts[1]);
                        String examType = parts[2];

                        // 3. Upsert Rapor
                        plugin.getDatabaseManager().upsertStudentRapor(player.getUniqueId(), nis, academicClass, semester, subjectId, examType, pctScore);
                    }, runnable -> Bukkit.getScheduler().runTaskAsynchronously(plugin, runnable))
                    .thenRun(() -> {
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

    public void showErrorCode2Bedrock(Player player) {
        SimpleForm form = SimpleForm.builder()
            .title("Error")
            .content("Maaf terjadi kesalahan (Code: 2). Jika masalah tetap berlanjut silahkan hubungi kementrian")
            .button("Ok")
            .build();

        FloodgateApi.getInstance().sendForm(player.getUniqueId(), form);
    }
}
