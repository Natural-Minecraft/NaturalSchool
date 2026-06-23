package id.naturalsmp.naturalSchool.classes.gui;

import id.naturalsmp.naturalSchool.NaturalSchool;
import id.naturalsmp.naturalSchool.profile.StudentProfile;
import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.input.DialogInput;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickCallback;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.geysermc.cumulus.form.CustomForm;
import org.geysermc.cumulus.form.SimpleForm;
import org.geysermc.floodgate.api.FloodgateApi;

import java.util.*;

public class BKGui {

    private final NaturalSchool plugin;

    // Predefined violation types and points
    private static final List<ViolationOption> VIOLATION_OPTIONS = Arrays.asList(
            new ViolationOption("Bolos Kelas (Alfa)", 15),
            new ViolationOption("Membuat Keributan", 10),
            new ViolationOption("Merusak Wilayah", 20),
            new ViolationOption("Tidak Memakai Seragam", 5),
            new ViolationOption("Sikap Tidak Sopan", 10),
            new ViolationOption("Pelanggaran Lain", 10)
    );

    public static class ViolationOption {
        private final String label;
        private final int points;

        public ViolationOption(String label, int points) {
            this.label = label;
            this.points = points;
        }

        public String getLabel() {
            return label;
        }

        public int getPoints() {
            return points;
        }
    }

    public BKGui(NaturalSchool plugin) {
        this.plugin = plugin;
    }

    public void openSearchGui(Player player) {
        if (plugin.getServer().getPluginManager().getPlugin("Floodgate") != null &&
            FloodgateApi.getInstance().isFloodgatePlayer(player.getUniqueId())) {
            openSearchGuiBedrock(player, null);
        } else {
            openSearchGuiJava(player, null);
        }
    }

    // =========================================================================
    // JAVA EDITION (PAPER DIALOGS) GUI
    // =========================================================================

    private void openSearchGuiJava(Player player, String errorMsg) {
        List<DialogBody> bodies = new ArrayList<>();
        bodies.add(DialogBody.plainMessage(MiniMessage.miniMessage().deserialize("<gold>=== Form Pencarian Pelanggaran BK ===</gold>")));
        bodies.add(DialogBody.plainMessage(Component.text("Silakan masukkan nama murid yang ingin dicari (bisa nama lengkap/panggilan).")));
        
        if (errorMsg != null) {
            bodies.add(DialogBody.plainMessage(Component.text(" ")));
            bodies.add(DialogBody.plainMessage(MiniMessage.miniMessage().deserialize("<red>[!] Error: " + errorMsg + "</red>")));
        }

        List<DialogInput> inputs = new ArrayList<>();
        inputs.add(DialogInput.text("query", Component.text("Nama Murid")).width(320).build());

        ActionButton searchBtn = ActionButton.builder(Component.text("Cari"))
            .action(DialogAction.customClick((view, audience) -> {
                if (audience instanceof Player p) {
                    String query = view.getText("query");
                    if (query == null || query.trim().isEmpty()) {
                        Bukkit.getScheduler().runTask(plugin, () -> openSearchGuiJava(p, "Kata kunci pencarian tidak boleh kosong!"));
                        return;
                    }

                    List<Map<String, String>> matches = plugin.getDatabaseManager().searchStudents(query.trim());
                    Bukkit.getScheduler().runTask(plugin, () -> handleMatchesJava(p, matches, query.trim()));
                }
            }, ClickCallback.Options.builder().uses(1).build()))
            .build();

        Dialog dialog = Dialog.create(builder -> builder.empty()
            .base(DialogBase.builder(Component.text("Cari Murid - BK"))
                .canCloseWithEscape(true)
                .body(bodies)
                .inputs(inputs)
                .build())
            .type(DialogType.notice(searchBtn))
        );
        player.showDialog(dialog);
    }

    private void handleMatchesJava(Player player, List<Map<String, String>> matches, String query) {
        if (matches.isEmpty()) {
            List<DialogBody> bodies = new ArrayList<>();
            bodies.add(DialogBody.plainMessage(MiniMessage.miniMessage().deserialize("<red><b>[!] Hasil Pencarian</b></red>")));
            bodies.add(DialogBody.plainMessage(Component.text("Tidak ditemukan murid dengan nama '" + query + "'.")));
            bodies.add(DialogBody.plainMessage(Component.text(" ")));

            Component searchAgain = MiniMessage.miniMessage().deserialize("<yellow>[Cari Lagi]</yellow>")
                .clickEvent(ClickEvent.callback(aud -> {
                    if (aud instanceof Player p) {
                        Bukkit.getScheduler().runTask(plugin, () -> openSearchGuiJava(p, null));
                    }
                }));
            bodies.add(DialogBody.plainMessage(searchAgain));

            Dialog dialog = Dialog.create(builder -> builder.empty()
                .base(DialogBase.builder(Component.text("Hasil BK - Murid Kosong"))
                    .canCloseWithEscape(true)
                    .body(bodies)
                    .build())
                .type(DialogType.notice(ActionButton.builder(Component.text("Tutup"))
                    .action(DialogAction.customClick((view, audience) -> {}, ClickCallback.Options.builder().uses(1).build()))
                    .build()))
            );
            player.showDialog(dialog);
            return;
        }

        if (matches.size() == 1) {
            Map<String, String> match = matches.get(0);
            UUID studentUuid = UUID.fromString(match.get("uuid"));
            String studentName = match.get("username");
            String studentNis = match.get("nis");
            openDetailFormJava(player, studentUuid, studentName, studentNis, null, null);
            return;
        }

        // Multiple matches selection screen
        List<DialogBody> bodies = new ArrayList<>();
        bodies.add(DialogBody.plainMessage(MiniMessage.miniMessage().deserialize("<gold>=== Hasil Pencarian MuridBK (" + matches.size() + ") ===</gold>")));
        bodies.add(DialogBody.plainMessage(Component.text("Ditemukan beberapa murid yang mirip. Silakan pilih salah satu:")));
        bodies.add(DialogBody.plainMessage(Component.text(" ")));

        for (Map<String, String> student : matches) {
            UUID uuid = UUID.fromString(student.get("uuid"));
            String name = student.get("username");
            String nis = student.get("nis");
            String nisDisplay = nis != null ? nis : "Tidak Ada NIS";

            Component studentLink = MiniMessage.miniMessage().deserialize("<yellow>»</yellow> <white>" + name + "</white> <gray>(" + nisDisplay + ")</gray> <green>[Pilih]</green>")
                .clickEvent(ClickEvent.callback(aud -> {
                    if (aud instanceof Player p) {
                        Bukkit.getScheduler().runTask(plugin, () -> openDetailFormJava(p, uuid, name, nis, null, null));
                    }
                }));
            bodies.add(DialogBody.plainMessage(studentLink));
        }

        Dialog dialog = Dialog.create(builder -> builder.empty()
            .base(DialogBase.builder(Component.text("Pilih Murid BK"))
                .canCloseWithEscape(true)
                .body(bodies)
                .build())
            .type(DialogType.notice(ActionButton.builder(Component.text("Kembali"))
                .action(DialogAction.customClick((view, audience) -> {
                    if (audience instanceof Player p) {
                        Bukkit.getScheduler().runTask(plugin, () -> openSearchGuiJava(p, null));
                    }
                }, ClickCallback.Options.builder().uses(1).build()))
                .build()))
        );
        player.showDialog(dialog);
    }

    private void openDetailFormJava(Player player, UUID studentUuid, String studentName, String studentNis, ViolationOption selectedViolation, String errorMsg) {
        List<DialogBody> bodies = new ArrayList<>();
        bodies.add(DialogBody.plainMessage(MiniMessage.miniMessage().deserialize("<gold>=== Catat Pelanggaran BK ===</gold>")));
        bodies.add(DialogBody.plainMessage(MiniMessage.miniMessage().deserialize("<yellow>Nama Pelaku:</yellow> <white>" + studentName + "</white>")));
        bodies.add(DialogBody.plainMessage(MiniMessage.miniMessage().deserialize("<yellow>NIS:</yellow> <white>" + (studentNis != null ? studentNis : "-") + "</white>")));
        bodies.add(DialogBody.plainMessage(Component.text(" ")));

        if (errorMsg != null) {
            bodies.add(DialogBody.plainMessage(MiniMessage.miniMessage().deserialize("<red>[!] Error: " + errorMsg + "</red>")));
            bodies.add(DialogBody.plainMessage(Component.text(" ")));
        }

        bodies.add(DialogBody.plainMessage(MiniMessage.miniMessage().deserialize("<yellow>Langkah 1: Pilih Tipe Pelanggaran</yellow>")));
        
        for (ViolationOption opt : VIOLATION_OPTIONS) {
            boolean isSelected = selectedViolation != null && selectedViolation.getLabel().equals(opt.getLabel());
            String prefix = isSelected ? "<green><bold>[✔] " : "<gray>[ ] ";
            String suffix = isSelected ? "</bold></green>" : "</gray>";
            
            Component optLink = MiniMessage.miniMessage().deserialize(prefix + opt.getLabel() + " (+" + opt.getPoints() + " Poin)" + suffix)
                .clickEvent(ClickEvent.callback(aud -> {
                    if (aud instanceof Player p) {
                        Bukkit.getScheduler().runTask(plugin, () -> openDetailFormJava(p, studentUuid, studentName, studentNis, opt, null));
                    }
                }));
            bodies.add(DialogBody.plainMessage(optLink));
        }

        bodies.add(DialogBody.plainMessage(Component.text(" ")));
        bodies.add(DialogBody.plainMessage(MiniMessage.miniMessage().deserialize("<yellow>Langkah 2: Isi Keterangan & Kirim</yellow>")));

        List<DialogInput> inputs = new ArrayList<>();
        inputs.add(DialogInput.text("comment", Component.text("Keterangan Pelanggaran")).width(320).build());

        ActionButton submitBtn = ActionButton.builder(Component.text("Catat Pelanggaran"))
            .action(DialogAction.customClick((view, audience) -> {
                if (audience instanceof Player p) {
                    if (selectedViolation == null) {
                        Bukkit.getScheduler().runTask(plugin, () -> openDetailFormJava(p, studentUuid, studentName, studentNis, null, "Anda harus memilih tipe pelanggaran terlebih dahulu!"));
                        return;
                    }

                    String comment = view.getText("comment");
                    if (comment == null || comment.trim().isEmpty()) {
                        Bukkit.getScheduler().runTask(plugin, () -> openDetailFormJava(p, studentUuid, studentName, studentNis, selectedViolation, "Keterangan/komentar pelanggaran tidak boleh kosong!"));
                        return;
                    }

                    // Record the violation
                    plugin.getViolationManager().recordViolation(
                            studentUuid,
                            studentName,
                            studentNis,
                            p.getUniqueId(),
                            p.getName(),
                            selectedViolation.getLabel(),
                            comment.trim(),
                            selectedViolation.getPoints()
                    );

                    Bukkit.getScheduler().runTask(plugin, () -> {
                        p.sendMessage(MiniMessage.miniMessage().deserialize("<green>Berhasil mencatat pelanggaran untuk " + studentName + ".</green>"));
                    });
                }
            }, ClickCallback.Options.builder().uses(1).build()))
            .build();

        Dialog dialog = Dialog.create(builder -> builder.empty()
            .base(DialogBase.builder(Component.text("Form Pelanggaran - " + studentName))
                .canCloseWithEscape(true)
                .body(bodies)
                .inputs(inputs)
                .build())
            .type(DialogType.notice(submitBtn))
        );
        player.showDialog(dialog);
    }

    // =========================================================================
    // BEDROCK EDITION (CUMULUS FORMS) GUI
    // =========================================================================

    private void openSearchGuiBedrock(Player player, String errorMsg) {
        CustomForm.Builder searchBuilder = CustomForm.builder()
            .title("BK - Cari Murid");
        
        if (errorMsg != null) {
            searchBuilder.label("§cError: " + errorMsg);
        }

        searchBuilder.input("Nama Murid (Target)", "")
            .validResultHandler(response -> {
                String query = response.asInput(errorMsg != null ? 1 : 0);
                if (query == null || query.trim().isEmpty()) {
                    openSearchGuiBedrock(player, "Kata kunci pencarian tidak boleh kosong!");
                    return;
                }

                List<Map<String, String>> matches = plugin.getDatabaseManager().searchStudents(query.trim());
                handleMatchesBedrock(player, matches, query.trim());
            });

        FloodgateApi.getInstance().sendForm(player.getUniqueId(), searchBuilder.build());
    }

    private void handleMatchesBedrock(Player player, List<Map<String, String>> matches, String query) {
        if (matches.isEmpty()) {
            SimpleForm emptyForm = SimpleForm.builder()
                .title("BK - Murid Tidak Ditemukan")
                .content("Tidak ditemukan murid dengan nama '" + query + "'.")
                .button("Cari Lagi")
                .button("Tutup")
                .validResultHandler(response -> {
                    if (response.clickedButtonId() == 0) {
                        openSearchGuiBedrock(player, null);
                    }
                })
                .build();
            FloodgateApi.getInstance().sendForm(player.getUniqueId(), emptyForm);
            return;
        }

        if (matches.size() == 1) {
            Map<String, String> match = matches.get(0);
            UUID studentUuid = UUID.fromString(match.get("uuid"));
            String studentName = match.get("username");
            String studentNis = match.get("nis");
            openDetailFormBedrock(player, studentUuid, studentName, studentNis, null);
            return;
        }

        SimpleForm.Builder selectBuilder = SimpleForm.builder()
            .title("BK - Pilih Murid")
            .content("Ditemukan " + matches.size() + " murid yang mirip. Pilih salah satu:");

        for (Map<String, String> student : matches) {
            String name = student.get("username");
            String nis = student.get("nis");
            String nisDisplay = nis != null ? nis : "Tidak Ada NIS";
            selectBuilder.button(name + "\n(" + nisDisplay + ")");
        }

        selectBuilder.validResultHandler(response -> {
            int idx = response.clickedButtonId();
            if (idx >= 0 && idx < matches.size()) {
                Map<String, String> match = matches.get(idx);
                UUID studentUuid = UUID.fromString(match.get("uuid"));
                String studentName = match.get("username");
                String studentNis = match.get("nis");
                openDetailFormBedrock(player, studentUuid, studentName, studentNis, null);
            }
        });

        FloodgateApi.getInstance().sendForm(player.getUniqueId(), selectBuilder.build());
    }

    private void openDetailFormBedrock(Player player, UUID studentUuid, String studentName, String studentNis, String errorMsg) {
        List<String> optionsLabels = new ArrayList<>();
        for (ViolationOption opt : VIOLATION_OPTIONS) {
            optionsLabels.add(opt.getLabel() + " (+" + opt.getPoints() + " Pts)");
        }

        CustomForm.Builder formBuilder = CustomForm.builder()
            .title("BK - Form Pelanggaran");

        if (errorMsg != null) {
            formBuilder.label("§cError: " + errorMsg);
        }

        formBuilder.label("Nama Pelaku: " + studentName + "\nNIS: " + (studentNis != null ? studentNis : "-"))
            .dropdown("Tipe Pelanggaran", optionsLabels)
            .input("Keterangan Pelanggaran", "")
            .validResultHandler(response -> {
                int dropIdx = response.asDropdown(errorMsg != null ? 2 : 1);
                String comment = response.asInput(errorMsg != null ? 3 : 2);

                if (dropIdx < 0 || dropIdx >= VIOLATION_OPTIONS.size()) {
                    openDetailFormBedrock(player, studentUuid, studentName, studentNis, "Tipe pelanggaran tidak valid.");
                    return;
                }

                if (comment == null || comment.trim().isEmpty()) {
                    openDetailFormBedrock(player, studentUuid, studentName, studentNis, "Keterangan/komentar pelanggaran tidak boleh kosong!");
                    return;
                }

                ViolationOption opt = VIOLATION_OPTIONS.get(dropIdx);

                // Record the violation
                plugin.getViolationManager().recordViolation(
                        studentUuid,
                        studentName,
                        studentNis,
                        player.getUniqueId(),
                        player.getName(),
                        opt.getLabel(),
                        comment.trim(),
                        opt.getPoints()
                );

                player.sendMessage(MiniMessage.miniMessage().deserialize("<green>Berhasil mencatat pelanggaran untuk " + studentName + ".</green>"));
            });

        FloodgateApi.getInstance().sendForm(player.getUniqueId(), formBuilder.build());
    }
}
