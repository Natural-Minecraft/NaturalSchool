package id.naturalsmp.naturalSchool.ui.gui;

import id.naturalsmp.naturalSchool.NaturalSchool;
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
import org.bukkit.entity.Player;
import org.geysermc.cumulus.form.CustomForm;
import org.geysermc.cumulus.form.SimpleForm;
import org.geysermc.floodgate.api.FloodgateApi;

import java.util.ArrayList;
import java.util.List;

/**
 * GUI mandiri untuk varian soal ujian prototype (/ns gui exam1–5).
 * Berisi implementasi Java Edition (Dialog API) dan Bedrock Edition (Cumulus Form)
 * dalam satu file yang sama.
 *
 * - exam0 / testexam : soal tunggal pilihan ganda (legacy test)
 * - exam1            : MCQ dengan validasi pilih-satu
 * - exam2            : Benar / Salah
 * - exam3            : MCQ matematika dengan validasi pilih-satu
 * - exam4            : Pernyataan majemuk (multiple statement checklist)
 * - exam5            : Pakta Integritas (deklarasi)
 */
public class ExamVariantsGui {

    private final NaturalSchool plugin;

    public ExamVariantsGui(NaturalSchool plugin) {
        this.plugin = plugin;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // JAVA EDITION — Paper Dialog API
    // ─────────────────────────────────────────────────────────────────────────

    /** [Java] Test Exam tunggal (legacy /school testexam). */
    public void openTestExamJava(Player player) {
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
                    DialogInput.bool("option_a", Component.text("A. Saya"),        false, "Pilih", "Kosong"),
                    DialogInput.bool("option_b", Component.text("B. Jopeh"),       false, "Pilih", "Kosong"),
                    DialogInput.bool("option_c", Component.text("C. AnakTentara"), false, "Pilih", "Kosong"),
                    DialogInput.bool("option_d", Component.text("D. Gua"),         false, "Pilih", "Kosong")
                ))
                .build())
            .type(DialogType.notice(submitBtn))
        );

        player.showDialog(dialog);
    }

    /** [Java] Exam 1 — MCQ pilih-satu dengan validasi. */
    public void openExam1Java(Player player, boolean showMoreThanOneWarning) {
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
                    DialogInput.bool("option_a", Component.text("A. Saya"),        false, "Pilih", "Kosong"),
                    DialogInput.bool("option_b", Component.text("B. Jopeh"),       false, "Pilih", "Kosong"),
                    DialogInput.bool("option_c", Component.text("C. AnakTentara"), false, "Pilih", "Kosong"),
                    DialogInput.bool("option_d", Component.text("D. Gua"),         false, "Pilih", "Kosong")
                ))
                .build())
            .type(DialogType.notice(submitBtn))
        );

        player.showDialog(dialog);
    }

    /** [Java] Exam 2 — Benar / Salah. */
    public void openExam2Java(Player player) {
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

    /** [Java] Exam 3 — MCQ matematika dengan validasi pilih-satu. */
    public void openExam3Java(Player player, boolean showMoreThanOneWarning) {
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
                    DialogInput.bool("option_a", Component.text("A. 7 Hari"),  false, "Pilih", "Kosong"),
                    DialogInput.bool("option_b", Component.text("B. 14 Hari"), false, "Pilih", "Kosong"),
                    DialogInput.bool("option_c", Component.text("C. 28 Hari"), false, "Pilih", "Kosong"),
                    DialogInput.bool("option_d", Component.text("D. 30 Hari"), false, "Pilih", "Kosong")
                ))
                .build())
            .type(DialogType.notice(submitBtn))
        );

        player.showDialog(dialog);
    }

    /** [Java] Exam 4 — Pernyataan majemuk (multiple statement checklist). */
    public void openExam4Java(Player player) {
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
                    DialogInput.bool("stmt_1", Component.text("1. Kelas dikunci sebelum lulus ujian"),         false, "Benar", "Salah"),
                    DialogInput.bool("stmt_2", Component.text("2. Semester otomatis berputar tiap 14 hari"),   false, "Benar", "Salah"),
                    DialogInput.bool("stmt_3", Component.text("3. Kelas otomatis naik tanpa perlu ujian"),     false, "Benar", "Salah")
                ))
                .build())
            .type(DialogType.notice(submitBtn))
        );

        player.showDialog(dialog);
    }

    /** [Java] Exam 5 — Pakta Integritas (deklarasi komitmen). */
    public void openExam5Java(Player player, boolean showWarning) {
        List<DialogBody> bodies = new ArrayList<>();
        bodies.add(DialogBody.plainMessage(MiniMessage.miniMessage().deserialize("<gold><bold>Ujian Kelulusan - Pakta Integritas</bold></gold>")));
        bodies.add(DialogBody.plainMessage(MiniMessage.miniMessage().deserialize("<gray>Deklarasi Komitmen:</gray> <white>Harap setujui pakta integritas di bawah ini.</white>")));

        if (showWarning) {
            bodies.add(DialogBody.plainMessage(MiniMessage.miniMessage().deserialize("<red><bold>Anda wajib mencentang seluruh pakta integritas untuk melanjutkan!</bold></red>")));
        }

        ActionButton submitBtn = ActionButton.builder(Component.text("Kirim Jawaban"))
            .action(DialogAction.customClick((view, audience) -> {
                if (audience instanceof Player p) {
                    boolean agreeRules   = view.getBoolean("agree_rules");
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
                    DialogInput.bool("agree_rules",   Component.text("Saya berjanji menaati seluruh peraturan server"),        false, "Setuju", "Batal"),
                    DialogInput.bool("agree_honesty", Component.text("Saya menyatakan siap mengikuti ujian dengan jujur"),    false, "Setuju", "Batal")
                ))
                .build())
            .type(DialogType.notice(submitBtn))
        );

        player.showDialog(dialog);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // BEDROCK EDITION — Geyser/Floodgate Cumulus Form
    // ─────────────────────────────────────────────────────────────────────────

    /** [Bedrock] Test Exam tunggal (legacy). */
    public void openTestExamBedrock(Player player) {
        SimpleForm form = SimpleForm.builder()
            .title("Ujian: Pilihan Ganda")
            .content("Siapa pencipta NaturalSMP?")
            .button("A. Saya")
            .button("B. Jopeh")
            .button("C. AnakTentara")
            .button("D. Gua")
            .validResultHandler(response -> {
                int clickedId = response.clickedButtonId();
                if (clickedId == 2) {
                    player.sendTitle("§a§lJAWABAN BENAR", "§7Selamat, kamu lulus!", 10, 70, 20);
                    player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
                } else {
                    player.sendTitle("§c§lJAWABAN SALAH", "§7Coba belajar lagi ya!", 10, 70, 20);
                    player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                }
            })
            .build();

        FloodgateApi.getInstance().sendForm(player.getUniqueId(), form);
    }

    /** [Bedrock] Exam 1 — MCQ pilih-satu dengan validasi (CustomForm toggle). */
    public void openExam1Bedrock(Player player, boolean showWarning) {
        CustomForm.Builder builder = CustomForm.builder().title("Ujian 1: Pilihan Ganda");

        if (showWarning) {
            builder.label("§c§lPilih hanya satu jawaban!");
        } else {
            builder.label("Pertanyaan: Siapa pencipta NaturalSMP?\nPilih satu jawaban yang benar:");
        }

        CustomForm form = builder
            .toggle("A. Saya",        false)
            .toggle("B. Jopeh",       false)
            .toggle("C. AnakTentara", false)
            .toggle("D. Gua",         false)
            .validResultHandler(response -> {
                boolean ansA = response.asToggle(1);
                boolean ansB = response.asToggle(2);
                boolean ansC = response.asToggle(3);
                boolean ansD = response.asToggle(4);

                int selectedCount = (ansA ? 1 : 0) + (ansB ? 1 : 0) + (ansC ? 1 : 0) + (ansD ? 1 : 0);

                if (selectedCount > 1) {
                    plugin.getUiManager().openExam1(player, true);
                } else if (selectedCount == 1 && ansC) {
                    player.sendTitle("§a§lJAWABAN BENAR", "§7Selamat, kamu lulus!", 10, 70, 20);
                    player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
                } else {
                    player.sendTitle("§c§lJAWABAN SALAH", "§7Coba belajar lagi ya!", 10, 70, 20);
                    player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                }
            })
            .build();

        FloodgateApi.getInstance().sendForm(player.getUniqueId(), form);
    }

    /** [Bedrock] Exam 2 — Benar / Salah (SimpleForm). */
    public void openExam2Bedrock(Player player) {
        SimpleForm form = SimpleForm.builder()
            .title("Ujian 2: Benar / Salah")
            .content("Apakah 1 Semester di NaturalSchool sama dengan 14 hari real-life?")
            .button("YA, Benar")
            .button("TIDAK, Salah")
            .validResultHandler(response -> {
                int clickedId = response.clickedButtonId();
                if (clickedId == 0) {
                    player.sendTitle("§a§lJAWABAN BENAR", "§7Selamat, jawaban Anda benar!", 10, 70, 20);
                    player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
                } else {
                    player.sendTitle("§c§lJAWABAN SALAH", "§7Silakan baca panduan kembali!", 10, 70, 20);
                    player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                }
            })
            .build();

        FloodgateApi.getInstance().sendForm(player.getUniqueId(), form);
    }

    /** [Bedrock] Exam 3 — MCQ matematika pilih-satu (CustomForm toggle). */
    public void openExam3Bedrock(Player player, boolean showWarning) {
        CustomForm.Builder builder = CustomForm.builder().title("Ujian 3: Pilihan Ganda");

        if (showWarning) {
            builder.label("§c§lPilih hanya satu jawaban!");
        } else {
            builder.label("Pertanyaan: Jika 1 Semester = 14 hari, berapa hari untuk menyelesaikan 2 Semester?\nPilih satu:");
        }

        CustomForm form = builder
            .toggle("A. 7 Hari",  false)
            .toggle("B. 14 Hari", false)
            .toggle("C. 28 Hari", false)
            .toggle("D. 30 Hari", false)
            .validResultHandler(response -> {
                boolean ansA = response.asToggle(1);
                boolean ansB = response.asToggle(2);
                boolean ansC = response.asToggle(3);
                boolean ansD = response.asToggle(4);

                int selectedCount = (ansA ? 1 : 0) + (ansB ? 1 : 0) + (ansC ? 1 : 0) + (ansD ? 1 : 0);

                if (selectedCount > 1) {
                    plugin.getUiManager().openExam3(player, true);
                } else if (selectedCount == 1 && ansC) {
                    player.sendTitle("§a§lJAWABAN BENAR", "§7Selamat, jawaban Anda benar!", 10, 70, 20);
                    player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
                } else {
                    player.sendTitle("§c§lJAWABAN SALAH", "§7Coba hitung kembali ya!", 10, 70, 20);
                    player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                }
            })
            .build();

        FloodgateApi.getInstance().sendForm(player.getUniqueId(), form);
    }

    /** [Bedrock] Exam 4 — Pernyataan majemuk (CustomForm toggle). */
    public void openExam4Bedrock(Player player) {
        CustomForm form = CustomForm.builder()
            .title("Ujian 4: Pernyataan Majemuk")
            .label("Pertanyaan: Manakah dari pernyataan berikut yang BENAR mengenai kenaikan kelas?\nPilih semua pernyataan yang benar:")
            .toggle("1. Kelas dikunci sebelum lulus ujian",       false)
            .toggle("2. Semester otomatis berputar tiap 14 hari", false)
            .toggle("3. Kelas otomatis naik tanpa perlu ujian",   false)
            .validResultHandler(response -> {
                boolean stmt1 = response.asToggle(1);
                boolean stmt2 = response.asToggle(2);
                boolean stmt3 = response.asToggle(3);

                if (stmt1 && stmt2 && !stmt3) {
                    player.sendTitle("§a§lJAWABAN BENAR", "§7Pemahaman logika Anda sangat baik!", 10, 70, 20);
                    player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
                } else {
                    player.sendTitle("§c§lJAWABAN SALAH", "§7Ada pernyataan salah yang Anda pilih!", 10, 70, 20);
                    player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                }
            })
            .build();

        FloodgateApi.getInstance().sendForm(player.getUniqueId(), form);
    }

    /** [Bedrock] Exam 5 — Pakta Integritas (CustomForm toggle). */
    public void openExam5Bedrock(Player player, boolean showWarning) {
        CustomForm.Builder builder = CustomForm.builder().title("Ujian 5: Pakta Integritas");

        if (showWarning) {
            builder.label("§c§lAnda wajib mencentang seluruh pakta integritas untuk melanjutkan!");
        } else {
            builder.label("Deklarasi Komitmen: Harap setujui pakta integritas di bawah ini.");
        }

        CustomForm form = builder
            .toggle("Saya berjanji menaati seluruh peraturan server",         false)
            .toggle("Saya menyatakan siap mengikuti ujian dengan jujur",      false)
            .validResultHandler(response -> {
                boolean agreeRules   = response.asToggle(1);
                boolean agreeHonesty = response.asToggle(2);

                if (agreeRules && agreeHonesty) {
                    player.sendTitle("§a§lJAWABAN BENAR", "§7Terima kasih atas komitmen Anda!", 10, 70, 20);
                    player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
                } else {
                    plugin.getUiManager().openExam5(player, true);
                }
            })
            .build();

        FloodgateApi.getInstance().sendForm(player.getUniqueId(), form);
    }
}
