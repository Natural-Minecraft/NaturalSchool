package id.naturalsmp.naturalSchool.ui.factory;

import id.naturalsmp.naturalSchool.NaturalSchool;
import id.naturalsmp.naturalSchool.profile.StudentProfile;
import id.naturalsmp.naturalSchool.ui.SchoolMenuType;
import org.bukkit.entity.Player;
import org.geysermc.cumulus.form.CustomForm;
import org.geysermc.cumulus.form.SimpleForm;
import org.geysermc.cumulus.response.CustomFormResponse;
import org.geysermc.cumulus.response.SimpleFormResponse;
import org.geysermc.floodgate.api.FloodgateApi;

import java.util.UUID;

public class BedrockFormFactory {

    private final NaturalSchool plugin;

    public BedrockFormFactory(NaturalSchool plugin) {
        this.plugin = plugin;
    }

    /**
     * Constructs and opens a Bedrock Form menu for Geyser/Floodgate players.
     *
     * @param player   the player opening the form
     * @param menuType the type of menu to display
     */
    public void openForm(Player player, SchoolMenuType menuType) {
        if (player == null || menuType == null) {
            return;
        }

        switch (menuType) {
            case REGISTRATION:
                openStep1(player);
                break;
            case PROFILE:
                openProfileForm(player);
                break;
            case STAFF_PANEL:
                openStaffPanelForm(player);
                break;
            default:
                throw new IllegalArgumentException("Unsupported menu type: " + menuType);
        }
    }

    /**
     * STEP 1: Welcome & Info CustomForm
     */
    public void openStep1(Player player) {
        StudentProfile profile = plugin.getProfileManager().getProfile(player.getUniqueId());
        String nis = (profile == null || profile.getNis() == null) ? "Unregistered" : profile.getNis();
        String status = (profile == null || profile.getNis() == null) ? "Belum Terdaftar" : "Terdaftar";

        CustomForm form = CustomForm.builder()
            .title("NaturalSchool Onboarding")
            .label("Welcome, " + player.getName() + "!\n\n" +
                "Username: " + player.getName() + "\n" +
                "NIS: " + nis + "\n" +
                "Status: " + status + "\n\n" +
                "Silakan klik Submit untuk melanjutkan.")
            .validResultHandler(response -> {
                plugin.getUiManager().openStep2(player);
            })
            .closedResultHandler(() -> {
                // Prevent escape: reopen Step 1
                openStep1(player);
            })
            .build();

        FloodgateApi.getInstance().sendForm(player.getUniqueId(), form);
    }

    /**
     * STEP 2: Cutscene cinematic offer CustomForm
     */
    public void openStep2(Player player) {
        CustomForm form = CustomForm.builder()
            .title("Tonton Cinematic?")
            .label("Apakah anda ingin menonton cinematic perkenalan NaturalSchool?")
            .toggle("Tonton Sinematik", false)
            .validResultHandler(response -> {
                boolean watchCinematic = response.asToggle(1);
                if (watchCinematic) {
                    player.sendMessage(net.kyori.adventure.text.minimessage.MiniMessage.miniMessage()
                        .deserialize("<yellow>Fitur cutscene mendatang!</yellow>"));
                }
                plugin.getUiManager().openStep3(player);
            })
            .closedResultHandler(() -> {
                // Prevent escape: reopen Step 2
                openStep2(player);
            })
            .build();

        FloodgateApi.getInstance().sendForm(player.getUniqueId(), form);
    }

    /**
     * STEP 3: ToS & Rules Agreement CustomForm
     */
    public void openStep3(Player player, boolean showWarning) {
        CustomForm.Builder builder = CustomForm.builder()
            .title("ToS & Rules Agreement");

        if (showWarning) {
            builder.label("§c§lAnda wajib menyetujui untuk bermain");
        }

        builder.label("Untuk mulai bermain, anda harus menyetujui ToS dan Rules kami.\nAturan di: https://naturalsmp.net")
            .toggle("Saya Menyetujui Terms Of Service", false)
            .toggle("Saya Menyetujui Rules Server", false);

        CustomForm form = builder.validResultHandler(response -> {
                // Cumulus asToggle(n) is 0-indexed by toggle elements only, not by all form elements.
                // The warning label prepended does NOT shift toggle indices.
                boolean acceptTos = response.asToggle(0);
                boolean acceptRules = response.asToggle(1);

                if (acceptTos && acceptRules) {
                    plugin.getUiManager().completeRegistration(player);
                } else {
                    plugin.getUiManager().openStep3(player, true);
                }
            })
            .closedResultHandler(() -> {
                // Prevent escape: reopen Step 3
                openStep3(player, showWarning);
            })
            .build();

        FloodgateApi.getInstance().sendForm(player.getUniqueId(), form);
    }

    private void openProfileForm(Player player) {
        StudentProfile profile = plugin.getProfileManager().getProfile(player.getUniqueId());
        String username = profile != null && profile.getUsername() != null ? profile.getUsername() : player.getName();
        String nis = (profile == null || profile.getNis() == null) ? "Belum Terdaftar" : profile.getNis();
        int academicClass = profile != null ? profile.getAcademicClass() : 0;
        String academicStage = profile != null ? profile.getAcademicStage() : "NONE";
        String currentSemester = profile != null ? profile.getCurrentSemester() : "GANJIL";
        String academicYear = plugin.getSemesterManager().getCurrentAcademicYear();

        SimpleForm form = SimpleForm.builder()
            .title("Informasi Pelajar")
            .content("Username: " + username + "\n" +
                     "NIS: " + nis + "\n" +
                     "Kelas + Jenjang: " + academicClass + " (" + academicStage + ")\n" +
                     "Semester: " + currentSemester + " (TA " + academicYear + ")")
            .button("Tutup")
            .build();

        FloodgateApi.getInstance().sendForm(player.getUniqueId(), form);
    }

    private void openStaffPanelForm(Player player) {
        SimpleForm form = SimpleForm.builder()
            .title("Staff Control Panel")
            .content("Administrator management options:")
            .button("View Student Database")
            .button("Clear Log Cache")
            .validResultHandler(response -> {
                int clickedId = response.clickedButtonId();
                player.sendMessage("§aStaff action performed: Option " + (clickedId + 1));
            })
            .build();

        FloodgateApi.getInstance().sendForm(player.getUniqueId(), form);
    }

    public void openTestExam(Player player) {
        SimpleForm form = SimpleForm.builder()
            .title("Ujian: Pilihan Ganda")
            .content("Siapa pencipta NaturalSMP?")
            .button("A. Saya")
            .button("B. Jopeh")
            .button("C. AnakTentara")
            .button("D. Gua")
            .validResultHandler(response -> {
                int clickedId = response.clickedButtonId();
                if (clickedId == 2) { // Button C
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

    public void openExam1(Player player, boolean showWarning) {
        CustomForm.Builder builder = CustomForm.builder()
            .title("Ujian 1: Pilihan Ganda");

        if (showWarning) {
            builder.label("§c§lPilih hanya satu jawaban!");
        } else {
            builder.label("Pertanyaan: Siapa pencipta NaturalSMP?\nPilih satu jawaban yang benar:");
        }

        CustomForm form = builder
            .toggle("A. Saya", false)
            .toggle("B. Jopeh", false)
            .toggle("C. AnakTentara", false)
            .toggle("D. Gua", false)
            .validResultHandler(response -> {
                boolean ansA = response.asToggle(0);
                boolean ansB = response.asToggle(1);
                boolean ansC = response.asToggle(2);
                boolean ansD = response.asToggle(3);

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

    public void openExam2(Player player) {
        SimpleForm form = SimpleForm.builder()
            .title("Ujian 2: Benar / Salah")
            .content("Apakah 1 Semester di NaturalSchool sama dengan 14 hari real-life?")
            .button("YA, Benar")
            .button("TIDAK, Salah")
            .validResultHandler(response -> {
                int clickedId = response.clickedButtonId();
                if (clickedId == 0) { // YA, Benar
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

    public void openExam3(Player player, boolean showWarning) {
        CustomForm.Builder builder = CustomForm.builder()
            .title("Ujian 3: Pilihan Ganda");

        if (showWarning) {
            builder.label("§c§lPilih hanya satu jawaban!");
        } else {
            builder.label("Pertanyaan: Jika 1 Semester = 14 hari, berapa hari untuk menyelesaikan 2 Semester?\nPilih satu:");
        }

        CustomForm form = builder
            .toggle("A. 7 Hari", false)
            .toggle("B. 14 Hari", false)
            .toggle("C. 28 Hari", false)
            .toggle("D. 30 Hari", false)
            .validResultHandler(response -> {
                boolean ansA = response.asToggle(0);
                boolean ansB = response.asToggle(1);
                boolean ansC = response.asToggle(2);
                boolean ansD = response.asToggle(3);

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

    public void openExam4(Player player) {
        CustomForm form = CustomForm.builder()
            .title("Ujian 4: Pernyataan Majemuk")
            .label("Pertanyaan: Manakah dari pernyataan berikut yang BENAR mengenai kenaikan kelas?\nPilih semua pernyataan yang benar:")
            .toggle("1. Kelas dikunci sebelum lulus ujian", false)
            .toggle("2. Semester otomatis berputar tiap 14 hari", false)
            .toggle("3. Kelas otomatis naik tanpa perlu ujian", false)
            .validResultHandler(response -> {
                boolean stmt1 = response.asToggle(0);
                boolean stmt2 = response.asToggle(1);
                boolean stmt3 = response.asToggle(2);

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

    public void openExam5(Player player, boolean showWarning) {
        CustomForm.Builder builder = CustomForm.builder()
            .title("Ujian 5: Pakta Integritas");

        if (showWarning) {
            builder.label("§c§lAnda wajib mencentang seluruh pakta integritas untuk melanjutkan!");
        } else {
            builder.label("Deklarasi Komitmen: Harap setujui pakta integritas di bawah ini.");
        }

        CustomForm form = builder
            .toggle("Saya berjanji menaati seluruh peraturan server", false)
            .toggle("Saya menyatakan siap mengikuti ujian dengan jujur", false)
            .validResultHandler(response -> {
                boolean agreeRules = response.asToggle(0);
                boolean agreeHonesty = response.asToggle(1);

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
