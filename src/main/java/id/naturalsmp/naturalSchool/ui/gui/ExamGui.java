package id.naturalsmp.naturalSchool.ui.gui;

import id.naturalsmp.naturalSchool.NaturalSchool;
import id.naturalsmp.naturalSchool.profile.StudentProfile;
import id.naturalsmp.naturalSchool.ui.ExamQuestions;
import id.naturalsmp.naturalSchool.ui.ExamQuestions.Question;
import id.naturalsmp.naturalSchool.ui.ExamQuestions.QuestionType;
import id.naturalsmp.naturalSchool.ui.ExamSession;
import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.input.DialogInput;
import io.papermc.paper.registry.data.dialog.input.SingleOptionDialogInput;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
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

public class ExamGui {

    public static final String GUI_VERSION = "2.0.0";

    private final NaturalSchool plugin;

    public ExamGui(NaturalSchool plugin) {
        this.plugin = plugin;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // JAVA EDITION — Paper Dialog API
    // ─────────────────────────────────────────────────────────────────────────

    /** [Java] Portal Ujian — pilih mata pelajaran (Multiple Buttons). */
    public void openExamPortalJava(Player player) {
        List<DialogBody> bodies = new ArrayList<>();
        bodies.add(DialogBody.item(new ItemStack(Material.BOOKSHELF)).build());
        bodies.add(DialogBody.plainMessage(MiniMessage.miniMessage().deserialize("<gold><bold>Portal Ujian</bold></gold>")));
        bodies.add(DialogBody.plainMessage(MiniMessage.miniMessage().deserialize(plugin.getExamMessage())));
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
                    plugin.getUiManager().startExamSession(p, subjectKey);
                    openExamPreJava(p, subjectKey);
                }
            }, ClickCallback.Options.builder().uses(1).build()))
            .build();
    }

    /** [Java] UI Pra-Soal — Info peserta sebelum mulai ujian. */
    public void openExamPreJava(Player player, String subject) {
        StudentProfile profile = plugin.getProfileManager().getProfile(player.getUniqueId());
        String name = player.getName();
        String nis = (profile != null && profile.getNis() != null) ? profile.getNis() : "-";

        List<DialogBody> bodies = new ArrayList<>();
        bodies.add(DialogBody.item(new ItemStack(Material.WRITABLE_BOOK)).build());
        bodies.add(DialogBody.plainMessage(MiniMessage.miniMessage().deserialize("<gold><bold>Informasi Peserta Ujian</bold></gold>")));
        bodies.add(DialogBody.plainMessage(MiniMessage.miniMessage().deserialize("<gray>Username: </gray><white>" + name + "</white>")));
        bodies.add(DialogBody.plainMessage(MiniMessage.miniMessage().deserialize("<gray>NIS: </gray><white>" + nis + "</white>")));
        bodies.add(DialogBody.plainMessage(MiniMessage.miniMessage().deserialize("<gray>Mata Pelajaran: </gray><yellow>" + ExamQuestions.getSubjectDisplayName(subject) + "</yellow>")));
        bodies.add(DialogBody.plainMessage(MiniMessage.miniMessage().deserialize("<gray>Total Soal: </gray><white>10 Soal</white>")));

        ActionButton nextBtn = ActionButton.builder(Component.text("Lanjut"))
            .action(DialogAction.customClick((view, audience) -> {
                if (audience instanceof Player p) {
                    ExamSession session = plugin.getUiManager().getExamSession(p.getUniqueId());
                    if (session != null) {
                        session.setCurrentQuestion(1);
                        plugin.getUiManager().openExamQuestion(p, subject, 1);
                    }
                }
            }, ClickCallback.Options.builder().uses(1).build()))
            .build();

        ActionButton backBtn = ActionButton.builder(Component.text("Kembali ke Portal"))
            .action(DialogAction.customClick((view, audience) -> {
                if (audience instanceof Player p) {
                    plugin.getUiManager().clearExamSession(p);
                    openExamPortalJava(p);
                }
            }, ClickCallback.Options.builder().uses(1).build()))
            .build();

        Dialog dialog = Dialog.create(builder -> builder.empty()
            .base(DialogBase.builder(Component.text("Pra-Ujian"))
                .canCloseWithEscape(true)
                .body(bodies)
                .build())
            .type(DialogType.confirmation(nextBtn, backBtn))
        );

        player.showDialog(dialog);
    }

    /** [Java] Soal Dinamis 1-10. */
    public void openExamQuestionJava(Player player, String subject, int questionNum) {
        ExamSession session = plugin.getUiManager().getExamSession(player.getUniqueId());
        if (session == null) {
            openExamPortalJava(player);
            return;
        }

        ExamQuestions.QuestionSet questions = ExamQuestions.getQuestions(subject);
        if (questions == null) {
            openExamPortalJava(player);
            return;
        }

        Question q = questions.getQuestion(questionNum - 1);
        if (q == null) {
            openExamPortalJava(player);
            return;
        }

        List<DialogBody> bodies = new ArrayList<>();
        Material mat = Material.BOOK;
        if (q.getType() == QuestionType.TRUE_FALSE) {
            mat = Material.COMPASS;
        } else if (q.getType() == QuestionType.COMPLEX_MULTIPLE_CHOICE) {
            mat = Material.PAPER;
        }

        bodies.add(DialogBody.item(new ItemStack(mat)).build());
        bodies.add(DialogBody.plainMessage(MiniMessage.miniMessage().deserialize("<gold><bold>Ujian: " + ExamQuestions.getSubjectDisplayName(subject) + " (Soal " + questionNum + "/10)</bold></gold>")));
        bodies.add(DialogBody.plainMessage(MiniMessage.miniMessage().deserialize("<gray>Pertanyaan:</gray> <white>" + q.getText() + "</white>")));

        if (q.getType() == QuestionType.COMPLEX_MULTIPLE_CHOICE) {
            bodies.add(DialogBody.plainMessage(MiniMessage.miniMessage().deserialize("<yellow>Pilih lebih dari satu pernyataan (klik lagi untuk batal):</yellow>")));
        } else {
            bodies.add(DialogBody.plainMessage(MiniMessage.miniMessage().deserialize("<yellow>Pilih salah satu jawaban di bawah ini:</yellow>")));
        }

        List<ActionButton> optionButtons = new ArrayList<>();

        if (q.getType() == QuestionType.MULTIPLE_CHOICE) {
            String[] optChars = {"A", "B", "C", "D"};
            String currentAns = session.getMcAnswer(questionNum - 1);
            for (int i = 0; i < 4; i++) {
                String optChar = optChars[i];
                String optText = q.getOptions()[i];
                boolean isSelected = optChar.equalsIgnoreCase(currentAns);
                String buttonText = optChar + ". " + optText;
                if (isSelected) {
                    buttonText = "<green><bold>" + buttonText + " (Dipilih)</bold></green>";
                }

                optionButtons.add(ActionButton.builder(MiniMessage.miniMessage().deserialize(buttonText))
                    .action(DialogAction.customClick((view, audience) -> {
                        if (audience instanceof Player p) {
                            session.setMcAnswer(questionNum - 1, optChar);
                            openExamQuestionJava(p, subject, questionNum);
                        }
                    }, ClickCallback.Options.builder().uses(1).build()))
                    .build());
            }
        } else if (q.getType() == QuestionType.TRUE_FALSE) {
            Boolean currentAns = session.getTfAnswer(questionNum - 7);

            boolean trueSelected = (currentAns != null && currentAns);
            String trueText = "Benar";
            if (trueSelected) {
                trueText = "<green><bold>" + trueText + " (Dipilih)</bold></green>";
            }
            optionButtons.add(ActionButton.builder(MiniMessage.miniMessage().deserialize(trueText))
                .action(DialogAction.customClick((view, audience) -> {
                    if (audience instanceof Player p) {
                        session.setTfAnswer(questionNum - 7, true);
                        openExamQuestionJava(p, subject, questionNum);
                    }
                }, ClickCallback.Options.builder().uses(1).build()))
                .build());

            boolean falseSelected = (currentAns != null && !currentAns);
            String falseText = "Salah";
            if (falseSelected) {
                falseText = "<green><bold>" + falseText + " (Dipilih)</bold></green>";
            }
            optionButtons.add(ActionButton.builder(MiniMessage.miniMessage().deserialize(falseText))
                .action(DialogAction.customClick((view, audience) -> {
                    if (audience instanceof Player p) {
                        session.setTfAnswer(questionNum - 7, false);
                        openExamQuestionJava(p, subject, questionNum);
                    }
                }, ClickCallback.Options.builder().uses(1).build()))
                .build());
        } else if (q.getType() == QuestionType.COMPLEX_MULTIPLE_CHOICE) {
            int complexIdx = questionNum - 9;
            for (int i = 0; i < 3; i++) {
                int optIdx = i;
                String optText = q.getOptions()[i];
                boolean isSelected = session.getComplexOption(complexIdx, optIdx);
                String buttonText = (optIdx + 1) + ". " + optText;
                if (isSelected) {
                    buttonText = "<green><bold>" + buttonText + " (Dipilih)</bold></green>";
                }

                optionButtons.add(ActionButton.builder(MiniMessage.miniMessage().deserialize(buttonText))
                    .action(DialogAction.customClick((view, audience) -> {
                        if (audience instanceof Player p) {
                            boolean currentlySelected = session.getComplexOption(complexIdx, optIdx);
                            session.setComplexOption(complexIdx, optIdx, !currentlySelected);
                            openExamQuestionJava(p, subject, questionNum);
                        }
                    }, ClickCallback.Options.builder().uses(1).build()))
                    .build());
            }
        }

        // Tombol Sebelumnya (Previous) dimasukkan ke actionButtons jika index > 1
        if (questionNum > 1) {
            optionButtons.add(ActionButton.builder(Component.text("Sebelumnya"))
                .action(DialogAction.customClick((view, audience) -> {
                    if (audience instanceof Player p) {
                        session.setCurrentQuestion(questionNum - 1);
                        openExamQuestionJava(p, subject, questionNum - 1);
                    }
                }, ClickCallback.Options.builder().uses(1).build()))
                .build());
        }

        // Tombol Selanjutnya (Next) adalah primary button dari multiAction
        ActionButton nextBtn = ActionButton.builder(Component.text(questionNum == 10 ? "Berikutnya (Konfirmasi)" : "Selanjutnya"))
            .action(DialogAction.customClick((view, audience) -> {
                if (audience instanceof Player p) {
                    if (questionNum == 10) {
                        session.setCurrentQuestion(11);
                        openExamConfirmationJava(p, subject);
                    } else {
                        session.setCurrentQuestion(questionNum + 1);
                        openExamQuestionJava(p, subject, questionNum + 1);
                    }
                }
            }, ClickCallback.Options.builder().uses(1).build()))
            .build();

        Dialog dialog = Dialog.create(builder -> builder.empty()
            .base(DialogBase.builder(Component.text("Ujian - Soal " + questionNum))
                .canCloseWithEscape(false) // Locked UI
                .body(bodies)
                .build())
            .type(DialogType.multiAction(optionButtons, nextBtn, 2))
        );

        player.showDialog(dialog);
    }

    /** [Java] Konfirmasi Akhir (Dialog). */
    public void openExamConfirmationJava(Player player, String subject) {
        ExamSession session = plugin.getUiManager().getExamSession(player.getUniqueId());
        if (session == null) {
            openExamPortalJava(player);
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

        ActionButton backBtn = ActionButton.builder(Component.text("Kembali ke Soal 10"))
            .action(DialogAction.customClick((view, audience) -> {
                if (audience instanceof Player p) {
                    session.setCurrentQuestion(10);
                    openExamQuestionJava(p, subject, 10);
                }
            }, ClickCallback.Options.builder().uses(1).build()))
            .build();

        Dialog dialog = Dialog.create(builder -> builder.empty()
            .base(DialogBase.builder(Component.text("Konfirmasi Akhir"))
                .canCloseWithEscape(false) // Locked UI
                .body(bodies)
                .build())
            .type(DialogType.confirmation(confirmBtn, backBtn))
        );

        player.showDialog(dialog);
    }

    /** [Java] Portal Ditutup. */
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

    /** [Bedrock] Portal Ujian — pilih mata pelajaran (CustomForm Dropdown - 1 Submit Button). */
    public void openExamPortalBedrockDropdown(Player player) {
        String examMessage = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(
            MiniMessage.miniMessage().deserialize(plugin.getExamMessage())
        );

        CustomForm form = CustomForm.builder()
            .title("Portal Ujian")
            .label(examMessage + "\n\nPilih mata pelajaran untuk diuji di bawah ini:")
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
                int selectedIndex = response.asDropdown(1);
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
                openExamPreBedrock(player, selectedSubject);
            })
            .build();

        FloodgateApi.getInstance().sendForm(player.getUniqueId(), form);
    }

    /** [Bedrock] UI Pra-Soal (SimpleForm - Info peserta sebelum mulai ujian). */
    public void openExamPreBedrock(Player player, String subject) {
        StudentProfile profile = plugin.getProfileManager().getProfile(player.getUniqueId());
        String name = player.getName();
        String nis = (profile != null && profile.getNis() != null) ? profile.getNis() : "-";

        SimpleForm form = SimpleForm.builder()
            .title("Informasi Peserta Ujian")
            .content("Username Peserta: " + name + "\n" +
                     "NIS Peserta: " + nis + "\n\n" +
                     "Mata Pelajaran: " + ExamQuestions.getSubjectDisplayName(subject) + "\n" +
                     "Total Soal: 10 Soal")
            .button("Lanjut")
            .button("Kembali ke Portal")
            .validResultHandler(response -> {
                int clickedId = response.clickedButtonId();
                if (clickedId == 0) {
                    ExamSession session = plugin.getUiManager().getExamSession(player.getUniqueId());
                    if (session != null) {
                        session.setCurrentQuestion(1);
                        openExamQuestionBedrock(player, subject, 1);
                    }
                } else {
                    plugin.getUiManager().clearExamSession(player);
                    openExamPortalBedrockDropdown(player);
                }
            })
            .closedResultHandler(() -> {
                plugin.getUiManager().clearExamSession(player);
            })
            .build();

        FloodgateApi.getInstance().sendForm(player.getUniqueId(), form);
    }

    /** [Bedrock] Soal Dinamis 1-10 (SimpleForm). */
    public void openExamQuestionBedrock(Player player, String subject, int questionNum) {
        ExamSession session = plugin.getUiManager().getExamSession(player.getUniqueId());
        if (session == null) {
            openExamPortalBedrockDropdown(player);
            return;
        }

        ExamQuestions.QuestionSet questions = ExamQuestions.getQuestions(subject);
        if (questions == null) {
            openExamPortalBedrockDropdown(player);
            return;
        }

        Question q = questions.getQuestion(questionNum - 1);
        if (q == null) {
            openExamPortalBedrockDropdown(player);
            return;
        }

        SimpleForm.Builder builder = SimpleForm.builder()
            .title("Ujian: Soal " + questionNum + "/10")
            .content("Mata Pelajaran: " + ExamQuestions.getSubjectDisplayName(subject) +
                     "\n\nPertanyaan:\n" + q.getText() + "\n\n" +
                     (q.getType() == QuestionType.COMPLEX_MULTIPLE_CHOICE 
                      ? "Pilih lebih dari satu (klik lagi untuk batal):" 
                      : "Pilih jawaban Anda:"));

        if (q.getType() == QuestionType.MULTIPLE_CHOICE) {
            String[] optChars = {"A", "B", "C", "D"};
            String currentAns = session.getMcAnswer(questionNum - 1);
            for (int i = 0; i < 4; i++) {
                String optChar = optChars[i];
                String optText = q.getOptions()[i];
                boolean isSelected = optChar.equalsIgnoreCase(currentAns);
                String buttonText = optChar + ". " + optText;
                if (isSelected) {
                    buttonText = "§a§l" + buttonText + " (Dipilih)";
                }
                builder.button(buttonText);
            }
        } else if (q.getType() == QuestionType.TRUE_FALSE) {
            Boolean currentAns = session.getTfAnswer(questionNum - 7);

            boolean trueSelected = (currentAns != null && currentAns);
            builder.button(trueSelected ? "§a§lBenar (Dipilih)" : "Benar");

            boolean falseSelected = (currentAns != null && !currentAns);
            builder.button(falseSelected ? "§a§lSalah (Dipilih)" : "Salah");
        } else if (q.getType() == QuestionType.COMPLEX_MULTIPLE_CHOICE) {
            int complexIdx = questionNum - 9;
            for (int i = 0; i < 3; i++) {
                int optIdx = i;
                String optText = q.getOptions()[i];
                boolean isSelected = session.getComplexOption(complexIdx, optIdx);
                String buttonText = (optIdx + 1) + ". " + optText;
                if (isSelected) {
                    buttonText = "§a§l" + buttonText + " (Dipilih)";
                }
                builder.button(buttonText);
            }
        }

        // Tombol Navigasi
        if (questionNum > 1) {
            builder.button("Sebelumnya");
        }
        builder.button(questionNum == 10 ? "Berikutnya (Konfirmasi)" : "Selanjutnya");

        builder.validResultHandler(response -> {
            int clickedId = response.clickedButtonId();
            int numChoices = (q.getType() == QuestionType.MULTIPLE_CHOICE) ? 4 
                           : (q.getType() == QuestionType.TRUE_FALSE) ? 2 
                           : 3;

            if (clickedId < numChoices) {
                // User klik jawaban
                if (q.getType() == QuestionType.MULTIPLE_CHOICE) {
                    String[] optChars = {"A", "B", "C", "D"};
                    session.setMcAnswer(questionNum - 1, optChars[clickedId]);
                } else if (q.getType() == QuestionType.TRUE_FALSE) {
                    session.setTfAnswer(questionNum - 7, clickedId == 0);
                } else if (q.getType() == QuestionType.COMPLEX_MULTIPLE_CHOICE) {
                    int complexIdx = questionNum - 9;
                    boolean currentlySelected = session.getComplexOption(complexIdx, clickedId);
                    session.setComplexOption(complexIdx, clickedId, !currentlySelected);
                }
                openExamQuestionBedrock(player, subject, questionNum); // Refresh UI
            } else {
                // User klik navigasi
                boolean hasPrev = (questionNum > 1);
                int prevButtonIdx = numChoices;
                int nextButtonIdx = hasPrev ? (numChoices + 1) : numChoices;

                if (hasPrev && clickedId == prevButtonIdx) {
                    session.setCurrentQuestion(questionNum - 1);
                    openExamQuestionBedrock(player, subject, questionNum - 1);
                } else if (clickedId == nextButtonIdx) {
                    if (questionNum == 10) {
                        session.setCurrentQuestion(11);
                        openExamConfirmationBedrock(player, subject);
                    } else {
                        session.setCurrentQuestion(questionNum + 1);
                        openExamQuestionBedrock(player, subject, questionNum + 1);
                    }
                }
            }
        })
        .closedResultHandler(() -> openExamQuestionBedrock(player, subject, questionNum)); // Locked UI

        FloodgateApi.getInstance().sendForm(player.getUniqueId(), builder.build());
    }

    /** [Bedrock] Konfirmasi Akhir (CustomForm dengan Dropdown dan 1 Tombol Submit). */
    public void openExamConfirmationBedrock(Player player, String subject) {
        ExamSession session = plugin.getUiManager().getExamSession(player.getUniqueId());
        if (session == null) {
            openExamPortalBedrockDropdown(player);
            return;
        }

        CustomForm form = CustomForm.builder()
            .title("Konfirmasi Akhir")
            .label("Apakah Anda yakin ingin menyelesaikan ujian dan mengirimkan jawaban Anda sekarang?")
            .dropdown("Pilih Tindakan",
                "Kirim Jawaban",
                "Kembali ke Soal 10"
            )
            .validResultHandler(response -> {
                int selectedIndex = response.asDropdown(1);
                if (selectedIndex == 0) {
                    int[] score = ExamQuestions.evaluateExam(session);
                    player.sendTitle("§a§lUJIAN SELESAI", "§7Benar: §a" + score[0] + " §f| Salah: §c" + score[1], 20, 100, 20);
                    player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
                    plugin.getUiManager().clearExamSession(player);
                } else {
                    session.setCurrentQuestion(10);
                    openExamQuestionBedrock(player, subject, 10);
                }
            })
            .closedResultHandler(() -> openExamConfirmationBedrock(player, subject)) // Locked UI
            .build();

        FloodgateApi.getInstance().sendForm(player.getUniqueId(), form);
    }

    /** [Bedrock] Portal Ditutup. */
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
