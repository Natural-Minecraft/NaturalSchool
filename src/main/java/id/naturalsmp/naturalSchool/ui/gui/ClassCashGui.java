package id.naturalsmp.naturalSchool.ui.gui;

import id.naturalsmp.naturalSchool.NaturalSchool;
import id.naturalsmp.naturalSchool.classes.ClassroomManager.ClassroomData;
import id.naturalsmp.naturalSchool.classes.ClassroomManager.OfficerInfo;
import id.naturalsmp.naturalSchool.classes.ClassCashManager.CashOperationResult;
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
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.geysermc.cumulus.form.CustomForm;
import org.geysermc.cumulus.form.SimpleForm;
import org.geysermc.floodgate.api.FloodgateApi;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

public class ClassCashGui {

    private final NaturalSchool plugin;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm");

    public ClassCashGui(NaturalSchool plugin) {
        this.plugin = plugin;
    }

    public void openClassGui(Player player, int classNum) {
        if (FloodgateApi.getInstance().isFloodgatePlayer(player.getUniqueId())) {
            openClassGuiBedrock(player, classNum, "MainMenu", null);
        } else {
            openClassGuiJava(player, classNum, "Info", null);
        }
    }

    private boolean hasOfficerAccess(Player player, ClassroomData data) {
        if (player.getUniqueId().equals(data.getWaliKelasUuid())) {
            return true;
        }
        if (player.hasPermission("naturalschool.admin")) {
            return true;
        }
        OfficerInfo officer = data.getOfficers().get(player.getUniqueId());
        if (officer != null) {
            String role = officer.getRole().toUpperCase();
            return role.equals("KETUA") || role.equals("WAKIL") || role.equals("BENDAHARA");
        }
        return false;
    }

    // ==========================================
    // JAVA EDITION (PAPER DIALOGS) GUI
    // ==========================================

    public void openClassGuiJava(Player player, int classNum, String tab, String errorMsg) {
        ClassroomData data = plugin.getClassroomManager().getClassroom(classNum);
        String rawClassName = plugin.getRankPrefixConfig().getClassPrefix(classNum);
        if (rawClassName == null || rawClassName.isEmpty()) {
            rawClassName = "Kelas " + classNum;
        }
        final String className = rawClassName;

        List<DialogBody> bodies = new ArrayList<>();

        // Add Error Message if present
        if (errorMsg != null) {
            bodies.add(DialogBody.plainMessage(MiniMessage.miniMessage().deserialize("<red><bold>[!] Error: " + errorMsg + "</bold></red>")));
            bodies.add(DialogBody.plainMessage(Component.text(" ")));
        }

        // Navigation Bar
        bodies.add(DialogBody.plainMessage(buildNavBarJava(player, classNum, tab)));
        bodies.add(DialogBody.plainMessage(Component.text(" ")));

        ActionButton submitBtn = null;
        List<DialogInput> inputs = new ArrayList<>();

        switch (tab.toLowerCase()) {
            case "info":
                bodies.add(DialogBody.plainMessage(MiniMessage.miniMessage().deserialize("<gold>=== Informasi Kelas " + className.trim() + " ===</gold>")));
                
                String waliName = "Tidak Ada";
                if (data.getWaliKelasUuid() != null) {
                    waliName = data.getWaliKelasName() != null ? data.getWaliKelasName() : Bukkit.getOfflinePlayer(data.getWaliKelasUuid()).getName();
                    if (waliName == null) waliName = "Unknown";
                }
                bodies.add(DialogBody.plainMessage(MiniMessage.miniMessage().deserialize("<yellow>Wali Kelas:</yellow> <white>" + waliName + "</white>")));

                String boundsStr = "Belum Diatur";
                if (data.hasBounds()) {
                    boundsStr = String.format("%d, %d, %d s/d %d, %d, %d", data.getX1(), data.getY1(), data.getZ1(), data.getX2(), data.getY2(), data.getZ2());
                }
                bodies.add(DialogBody.plainMessage(MiniMessage.miniMessage().deserialize("<yellow>Area Kelas:</yellow> <white>" + boundsStr + "</white>")));

                List<Map<String, String>> studentList = plugin.getDatabaseManager().getStudentsInClass(classNum);
                bodies.add(DialogBody.plainMessage(MiniMessage.miniMessage().deserialize("<yellow>Total Murid:</yellow> <white>" + studentList.size() + " siswa</white>")));
                break;

            case "fund":
                bodies.add(DialogBody.plainMessage(MiniMessage.miniMessage().deserialize("<gold>=== Keuangan & Kas Kelas ===</gold>")));
                bodies.add(DialogBody.plainMessage(MiniMessage.miniMessage().deserialize("<yellow>Saldo Kas:</yellow> <green>Rp" + String.format("%,.0f", data.getCashBalance()) + "</green>")));
                bodies.add(DialogBody.plainMessage(MiniMessage.miniMessage().deserialize("<yellow>Kas Mingguan:</yellow> <white>Rp" + String.format("%,.0f", data.getWeeklyFee()) + "</white>")));
                bodies.add(DialogBody.plainMessage(MiniMessage.miniMessage().deserialize("<yellow>Status Kas:</yellow> " + (data.isWeeklyFeeEnabled() ? "<green>Aktif</green>" : "<red>Nonaktif</red>"))));
                bodies.add(DialogBody.plainMessage(Component.text(" ")));

                // Actions links
                Component fundActions = Component.empty();
                
                Component payLink = MiniMessage.miniMessage().deserialize("<green><bold>[Bayar Kas Mingguan]</bold></green>")
                    .clickEvent(ClickEvent.callback(aud -> {
                        if (aud instanceof Player p) {
                            CashOperationResult res = plugin.getClassCashManager().payWeeklyFee(p);
                            Bukkit.getScheduler().runTask(plugin, () -> {
                                if (res.isSuccess()) {
                                    p.sendMessage(MiniMessage.miniMessage().deserialize("<green>" + res.getMessage() + "</green>"));
                                    openClassGuiJava(p, classNum, "fund", null);
                                } else {
                                    openClassGuiJava(p, classNum, "fund", res.getMessage());
                                }
                            });
                        }
                    }));
                fundActions = fundActions.append(payLink);

                if (hasOfficerAccess(player, data)) {
                    Component toggleLink = MiniMessage.miniMessage().deserialize(" | <yellow>[Toggle Status]</yellow>")
                        .clickEvent(ClickEvent.callback(aud -> {
                            if (aud instanceof Player p) {
                                boolean current = data.isWeeklyFeeEnabled();
                                plugin.getClassroomManager().updateClassCash(classNum, data.getCashBalance(), data.getWeeklyFee(), !current);
                                Bukkit.getScheduler().runTask(plugin, () -> openClassGuiJava(p, classNum, "fund", null));
                            }
                        }));
                    fundActions = fundActions.append(toggleLink);

                    Component withdrawLink = MiniMessage.miniMessage().deserialize(" | <red>[Tarik Saldo]</red>")
                        .clickEvent(ClickEvent.callback(aud -> {
                            if (aud instanceof Player p) {
                                Bukkit.getScheduler().runTask(plugin, () -> openClassGuiJava(p, classNum, "withdraw_form", null));
                            }
                        }));
                    fundActions = fundActions.append(withdrawLink);

                    Component fineLink = MiniMessage.miniMessage().deserialize(" | <gold>[Denda]</gold>")
                        .clickEvent(ClickEvent.callback(aud -> {
                            if (aud instanceof Player p) {
                                Bukkit.getScheduler().runTask(plugin, () -> openClassGuiJava(p, classNum, "fine_form", null));
                            }
                        }));
                    fundActions = fundActions.append(fineLink);
                }

                bodies.add(DialogBody.plainMessage(fundActions));
                bodies.add(DialogBody.plainMessage(Component.text(" ")));

                // Transactions history
                bodies.add(DialogBody.plainMessage(MiniMessage.miniMessage().deserialize("<yellow><underlined>5 Transaksi Terakhir:</underlined></yellow>")));
                List<Map<String, Object>> txs = plugin.getDatabaseManager().getClassCashTransactions(classNum, 5);
                if (txs.isEmpty()) {
                    bodies.add(DialogBody.plainMessage(MiniMessage.miniMessage().deserialize("<gray>Belum ada riwayat transaksi.</gray>")));
                } else {
                    for (Map<String, Object> tx : txs) {
                        String type = (String) tx.get("tx_type");
                        double amt = (double) tx.get("amount");
                        String desc = (String) tx.get("description");
                        Date date = (Date) tx.get("tx_date");
                        String dateStr = date != null ? dateFormat.format(date) : "-";

                        String color = type.equalsIgnoreCase("DEPOSIT") || type.equalsIgnoreCase("FINE") ? "<green>+" : "<red>-";
                        bodies.add(DialogBody.plainMessage(MiniMessage.miniMessage().deserialize(
                            "<gray>[" + dateStr + "]</gray> " + color + "Rp" + String.format("%,.0f", amt) + "</green> <gray>- " + desc + "</gray>"
                        )));
                    }
                }
                break;

            case "withdraw_form":
                bodies.add(DialogBody.plainMessage(MiniMessage.miniMessage().deserialize("<gold>=== Form Penarikan Saldo Kas ===</gold>")));
                bodies.add(DialogBody.plainMessage(MiniMessage.miniMessage().deserialize("<yellow>Saldo Tersedia: Rp" + String.format("%,.0f", data.getCashBalance()) + "</yellow>")));
                bodies.add(DialogBody.plainMessage(Component.text("Silakan isi jumlah penarikan dan alasan penarikan di bawah.")));

                inputs.add(DialogInput.text("amount", Component.text("Jumlah Penarikan (Angka)")).width(320).build());
                inputs.add(DialogInput.text("reason", Component.text("Alasan Penarikan")).width(320).build());

                submitBtn = ActionButton.builder(Component.text("Tarik Saldo"))
                    .action(DialogAction.customClick((view, audience) -> {
                        if (audience instanceof Player p) {
                            String amtStr = view.getText("amount");
                            String reason = view.getText("reason");

                            double amount;
                            try {
                                amount = Double.parseDouble(amtStr.replaceAll("[^0-9.]", ""));
                            } catch (Exception e) {
                                Bukkit.getScheduler().runTask(plugin, () -> openClassGuiJava(p, classNum, "withdraw_form", "Jumlah penarikan harus berupa angka!"));
                                return;
                            }

                            if (amount <= 0) {
                                Bukkit.getScheduler().runTask(plugin, () -> openClassGuiJava(p, classNum, "withdraw_form", "Jumlah penarikan harus lebih dari 0!"));
                                return;
                            }

                            if (reason == null || reason.trim().isEmpty()) {
                                Bukkit.getScheduler().runTask(plugin, () -> openClassGuiJava(p, classNum, "withdraw_form", "Alasan penarikan wajib diisi!"));
                                return;
                            }

                            CashOperationResult res = plugin.getClassCashManager().withdrawCash(p, classNum, amount, reason);
                            Bukkit.getScheduler().runTask(plugin, () -> {
                                if (res.isSuccess()) {
                                    p.sendMessage(MiniMessage.miniMessage().deserialize("<green>" + res.getMessage() + "</green>"));
                                    openClassGuiJava(p, classNum, "fund", null);
                                } else {
                                    openClassGuiJava(p, classNum, "withdraw_form", res.getMessage());
                                }
                            });
                        }
                    }, ClickCallback.Options.builder().uses(1).build()))
                    .build();
                break;

            case "fine_form":
                bodies.add(DialogBody.plainMessage(MiniMessage.miniMessage().deserialize("<gold>=== Form Denda Murid ===</gold>")));
                bodies.add(DialogBody.plainMessage(Component.text("Pemberian denda akan menarik saldo dompet murid secara paksa ke kas kelas.")));

                inputs.add(DialogInput.text("target", Component.text("Nama Murid (Target)")).width(320).build());
                inputs.add(DialogInput.text("amount", Component.text("Jumlah Denda (Angka)")).width(320).build());
                inputs.add(DialogInput.text("reason", Component.text("Alasan Denda")).width(320).build());

                submitBtn = ActionButton.builder(Component.text("Keluarkan Denda"))
                    .action(DialogAction.customClick((view, audience) -> {
                        if (audience instanceof Player p) {
                            String targetName = view.getText("target");
                            String amtStr = view.getText("amount");
                            String reason = view.getText("reason");

                            if (targetName == null || targetName.trim().isEmpty()) {
                                Bukkit.getScheduler().runTask(plugin, () -> openClassGuiJava(p, classNum, "fine_form", "Nama murid target wajib diisi!"));
                                return;
                            }

                            double amount;
                            try {
                                amount = Double.parseDouble(amtStr.replaceAll("[^0-9.]", ""));
                            } catch (Exception e) {
                                Bukkit.getScheduler().runTask(plugin, () -> openClassGuiJava(p, classNum, "fine_form", "Jumlah denda harus berupa angka!"));
                                return;
                            }

                            if (amount <= 0) {
                                Bukkit.getScheduler().runTask(plugin, () -> openClassGuiJava(p, classNum, "fine_form", "Jumlah denda harus lebih dari 0!"));
                                return;
                            }

                            if (reason == null || reason.trim().isEmpty()) {
                                Bukkit.getScheduler().runTask(plugin, () -> openClassGuiJava(p, classNum, "fine_form", "Alasan denda wajib diisi!"));
                                return;
                            }

                            OfflinePlayer targetPlayer = Bukkit.getOfflinePlayer(targetName);
                            StudentProfile targetProfile = null;
                            if (targetPlayer.isOnline()) {
                                targetProfile = plugin.getProfileManager().getProfile(targetPlayer.getUniqueId());
                            } else {
                                try {
                                    targetProfile = plugin.getDatabaseManager().loadProfile(targetPlayer.getUniqueId());
                                } catch (Exception ignored) {}
                            }

                            if (targetProfile == null || targetProfile.getAcademicClass() != classNum) {
                                Bukkit.getScheduler().runTask(plugin, () -> openClassGuiJava(p, classNum, "fine_form", "Pemain " + targetName + " tidak terdaftar di kelas " + classNum + "!"));
                                return;
                            }

                            CashOperationResult res = plugin.getClassCashManager().applyFine(targetPlayer, p, classNum, amount, reason);
                            Bukkit.getScheduler().runTask(plugin, () -> {
                                if (res.isSuccess()) {
                                    p.sendMessage(MiniMessage.miniMessage().deserialize("<green>" + res.getMessage() + "</green>"));
                                    openClassGuiJava(p, classNum, "fund", null);
                                } else {
                                    openClassGuiJava(p, classNum, "fine_form", res.getMessage());
                                }
                            });
                        }
                    }, ClickCallback.Options.builder().uses(1).build()))
                    .build();
                break;

            case "struktur":
                bodies.add(DialogBody.plainMessage(MiniMessage.miniMessage().deserialize("<gold>=== Struktur Pengurus Kelas " + className.trim() + " ===</gold>")));
                
                String wali = "Tidak Ada";
                if (data.getWaliKelasUuid() != null) {
                    wali = data.getWaliKelasName() != null ? data.getWaliKelasName() : Bukkit.getOfflinePlayer(data.getWaliKelasUuid()).getName();
                    if (wali == null) wali = "Unknown";
                }
                bodies.add(DialogBody.plainMessage(MiniMessage.miniMessage().deserialize("<yellow>» Wali Kelas:</yellow> <white>" + wali + "</white>")));

                String ketua = "Tidak Ada";
                String wakil = "Tidak Ada";
                String sekretaris = "Tidak Ada";
                String bendahara = "Tidak Ada";

                for (Map.Entry<UUID, OfficerInfo> entry : data.getOfficers().entrySet()) {
                    String role = entry.getValue().getRole();
                    String name = entry.getValue().getUsername();
                    if (name == null) name = "Unknown";

                    if ("KETUA".equalsIgnoreCase(role)) ketua = name;
                    else if ("WAKIL".equalsIgnoreCase(role)) wakil = name;
                    else if ("SEKRETARIS".equalsIgnoreCase(role)) sekretaris = name;
                    else if ("BENDAHARA".equalsIgnoreCase(role)) bendahara = name;
                }

                bodies.add(DialogBody.plainMessage(MiniMessage.miniMessage().deserialize("<yellow>» Ketua Kelas:</yellow> <white>" + ketua + "</white>")));
                bodies.add(DialogBody.plainMessage(MiniMessage.miniMessage().deserialize("<yellow>» Wakil Ketua:</yellow> <white>" + wakil + "</white>")));
                bodies.add(DialogBody.plainMessage(MiniMessage.miniMessage().deserialize("<yellow>» Sekretaris:</yellow> <white>" + sekretaris + "</white>")));
                bodies.add(DialogBody.plainMessage(MiniMessage.miniMessage().deserialize("<yellow>» Bendahara:</yellow> <white>" + bendahara + "</white>")));
                break;

            case "siswa":
                bodies.add(DialogBody.plainMessage(MiniMessage.miniMessage().deserialize("<gold>=== Daftar Murid Kelas " + className.trim() + " ===</gold>")));
                List<Map<String, String>> students = plugin.getDatabaseManager().getStudentsInClass(classNum);
                if (students.isEmpty()) {
                    bodies.add(DialogBody.plainMessage(MiniMessage.miniMessage().deserialize("<gray>Tidak ada murid terdaftar di kelas ini.</gray>")));
                } else {
                    String studentStr = students.stream().map(m -> m.get("username")).collect(Collectors.joining(", "));
                    bodies.add(DialogBody.plainMessage(MiniMessage.miniMessage().deserialize("<white>" + studentStr + "</white>")));
                }
                break;
        }

        // Close button if no submit action is defined
        if (submitBtn == null) {
            submitBtn = ActionButton.builder(Component.text("Tutup"))
                .action(DialogAction.customClick((view, audience) -> {}, ClickCallback.Options.builder().uses(1).build()))
                .build();
        }

        ActionButton finalSubmit = submitBtn;
        Dialog dialog = Dialog.create(builder -> builder.empty()
            .base(DialogBase.builder(Component.text("Informasi " + className.trim()))
                .canCloseWithEscape(true)
                .body(bodies)
                .inputs(inputs)
                .build())
            .type(DialogType.notice(finalSubmit))
        );

        player.showDialog(dialog);
    }

    private Component buildNavBarJava(Player player, int classNum, String activeTab) {
        Component nav = Component.empty();
        String[] tabs = {"Info", "Fund", "Struktur", "Siswa"};
        for (int i = 0; i < tabs.length; i++) {
            String tab = tabs[i];
            boolean isActive = tab.equalsIgnoreCase(activeTab) || (activeTab.contains("_form") && tab.equalsIgnoreCase("Fund"));
            String color = isActive ? "<green><bold>" : "<gray>";
            String closingColor = isActive ? "</bold></green>" : "</gray>";
            Component tabComp = MiniMessage.miniMessage().deserialize(color + "[" + tab + "]" + closingColor)
                .clickEvent(ClickEvent.callback(audience -> {
                    if (audience instanceof Player p) {
                        Bukkit.getScheduler().runTask(plugin, () -> openClassGuiJava(p, classNum, tab, null));
                    }
                }));
            nav = nav.append(tabComp);
            if (i < tabs.length - 1) {
                nav = nav.append(Component.text(" | "));
            }
        }
        return nav;
    }

    // ==========================================
    // BEDROCK EDITION (CUMULUS FORMS) GUI
    // ==========================================

    public void openClassGuiBedrock(Player player, int classNum, String view, String errorMsg) {
        ClassroomData data = plugin.getClassroomManager().getClassroom(classNum);
        String className = plugin.getRankPrefixConfig().getClassPrefix(classNum);
        if (className == null || className.isEmpty()) {
            className = "Kelas " + classNum;
        }

        switch (view.toLowerCase()) {
            case "mainmenu":
                SimpleForm menuForm = SimpleForm.builder()
                    .title("Class Hub - " + className.trim())
                    .button("Info Kelas")
                    .button("Keuangan / Kas Kelas")
                    .button("Struktur Kelas")
                    .button("Daftar Murid")
                    .button("Tutup")
                    .validResultHandler(response -> {
                        int idx = response.clickedButtonId();
                        switch (idx) {
                            case 0:
                                openClassGuiBedrock(player, classNum, "Info", null);
                                break;
                            case 1:
                                openClassGuiBedrock(player, classNum, "Fund", null);
                                break;
                            case 2:
                                openClassGuiBedrock(player, classNum, "Struktur", null);
                                break;
                            case 3:
                                openClassGuiBedrock(player, classNum, "Siswa", null);
                                break;
                        }
                    })
                    .build();
                FloodgateApi.getInstance().sendForm(player.getUniqueId(), menuForm);
                break;

            case "info":
                String waliName = "Tidak Ada";
                if (data.getWaliKelasUuid() != null) {
                    waliName = data.getWaliKelasName() != null ? data.getWaliKelasName() : Bukkit.getOfflinePlayer(data.getWaliKelasUuid()).getName();
                    if (waliName == null) waliName = "Unknown";
                }
                String boundsStr = data.hasBounds() ? String.format("%d, %d, %d s/d %d, %d, %d", data.getX1(), data.getY1(), data.getZ1(), data.getX2(), data.getY2(), data.getZ2()) : "Belum Diatur";
                List<Map<String, String>> studentList = plugin.getDatabaseManager().getStudentsInClass(classNum);

                SimpleForm infoForm = SimpleForm.builder()
                    .title("Info Kelas - " + className.trim())
                    .content("Wali Kelas: " + waliName + "\n" +
                             "Area Kelas: " + boundsStr + "\n" +
                             "Total Murid: " + studentList.size() + " siswa")
                    .button("Kembali ke Menu Utama")
                    .validResultHandler(response -> openClassGuiBedrock(player, classNum, "MainMenu", null))
                    .build();
                FloodgateApi.getInstance().sendForm(player.getUniqueId(), infoForm);
                break;

            case "fund":
                String fundContent = "Saldo Kas Kelas: Rp" + String.format("%,.0f", data.getCashBalance()) + "\n" +
                                     "Kas Mingguan: Rp" + String.format("%,.0f", data.getWeeklyFee()) + "\n" +
                                     "Status Kas: " + (data.isWeeklyFeeEnabled() ? "Aktif" : "Nonaktif");

                if (errorMsg != null) {
                    fundContent = "§cError: " + errorMsg + "\n\n" + fundContent;
                }

                SimpleForm.Builder fundBuilder = SimpleForm.builder()
                    .title("Kas Kelas - " + className.trim())
                    .content(fundContent)
                    .button("Bayar Kas Mingguan");

                boolean isOfficer = hasOfficerAccess(player, data);
                if (isOfficer) {
                    fundBuilder.button("Toggle Status Kas");
                    fundBuilder.button("Tarik Saldo Kas");
                    fundBuilder.button("Denda Murid");
                }
                fundBuilder.button("Riwayat Transaksi");
                fundBuilder.button("Kembali");

                fundBuilder.validResultHandler(response -> {
                    int clicked = response.clickedButtonId();
                    if (clicked == 0) {
                        CashOperationResult res = plugin.getClassCashManager().payWeeklyFee(player);
                        if (res.isSuccess()) {
                            player.sendMessage(MiniMessage.miniMessage().deserialize("<green>" + res.getMessage() + "</green>"));
                            openClassGuiBedrock(player, classNum, "Fund", null);
                        } else {
                            openClassGuiBedrock(player, classNum, "Fund", res.getMessage());
                        }
                    } else if (isOfficer && clicked == 1) {
                        boolean current = data.isWeeklyFeeEnabled();
                        plugin.getClassroomManager().updateClassCash(classNum, data.getCashBalance(), data.getWeeklyFee(), !current);
                        openClassGuiBedrock(player, classNum, "Fund", null);
                    } else if (isOfficer && clicked == 2) {
                        openClassGuiBedrock(player, classNum, "withdraw_form", null);
                    } else if (isOfficer && clicked == 3) {
                        openClassGuiBedrock(player, classNum, "fine_form", null);
                    } else {
                        // either clicked == 4 (for officer) or clicked == 1 (for normal user)
                        int offset = isOfficer ? 4 : 1;
                        if (clicked == offset) {
                            openClassGuiBedrock(player, classNum, "history", null);
                        } else {
                            openClassGuiBedrock(player, classNum, "MainMenu", null);
                        }
                    }
                });

                FloodgateApi.getInstance().sendForm(player.getUniqueId(), fundBuilder.build());
                break;

            case "withdraw_form":
                CustomForm.Builder withdrawBuilder = CustomForm.builder()
                    .title("Tarik Saldo Kas");
                
                if (errorMsg != null) {
                    withdrawBuilder.label("§cError: " + errorMsg);
                }

                withdrawBuilder.label("Saldo Kas Tersedia: Rp" + String.format("%,.0f", data.getCashBalance()))
                    .input("Jumlah Penarikan (Angka)", "1000")
                    .input("Alasan Penarikan", "")
                    .validResultHandler(response -> {
                        String amtStr = response.asInput(errorMsg != null ? 2 : 1);
                        String reason = response.asInput(errorMsg != null ? 3 : 2);

                        double amount;
                        try {
                            amount = Double.parseDouble(amtStr.replaceAll("[^0-9.]", ""));
                        } catch (Exception e) {
                            openClassGuiBedrock(player, classNum, "withdraw_form", "Jumlah penarikan harus berupa angka!");
                            return;
                        }

                        if (amount <= 0) {
                            openClassGuiBedrock(player, classNum, "withdraw_form", "Jumlah penarikan harus lebih dari 0!");
                            return;
                        }

                        if (reason == null || reason.trim().isEmpty()) {
                            openClassGuiBedrock(player, classNum, "withdraw_form", "Alasan penarikan wajib diisi!");
                            return;
                        }

                        CashOperationResult res = plugin.getClassCashManager().withdrawCash(player, classNum, amount, reason);
                        if (res.isSuccess()) {
                            player.sendMessage(MiniMessage.miniMessage().deserialize("<green>" + res.getMessage() + "</green>"));
                            openClassGuiBedrock(player, classNum, "Fund", null);
                        } else {
                            openClassGuiBedrock(player, classNum, "withdraw_form", res.getMessage());
                        }
                    });
                FloodgateApi.getInstance().sendForm(player.getUniqueId(), withdrawBuilder.build());
                break;

            case "fine_form":
                CustomForm.Builder fineBuilder = CustomForm.builder()
                    .title("Denda Murid");

                if (errorMsg != null) {
                    fineBuilder.label("§cError: " + errorMsg);
                }

                fineBuilder.label("Murid target harus berada di kelas yang sama.")
                    .input("Nama Murid (Target)", "")
                    .input("Jumlah Denda (Angka)", "500")
                    .input("Alasan Denda", "")
                    .validResultHandler(response -> {
                        String targetName = response.asInput(errorMsg != null ? 2 : 1);
                        String amtStr = response.asInput(errorMsg != null ? 3 : 2);
                        String reason = response.asInput(errorMsg != null ? 4 : 3);

                        if (targetName == null || targetName.trim().isEmpty()) {
                            openClassGuiBedrock(player, classNum, "fine_form", "Nama murid target wajib diisi!");
                            return;
                        }

                        double amount;
                        try {
                            amount = Double.parseDouble(amtStr.replaceAll("[^0-9.]", ""));
                        } catch (Exception e) {
                            openClassGuiBedrock(player, classNum, "fine_form", "Jumlah denda harus berupa angka!");
                            return;
                        }

                        if (amount <= 0) {
                            openClassGuiBedrock(player, classNum, "fine_form", "Jumlah denda harus lebih dari 0!");
                            return;
                        }

                        if (reason == null || reason.trim().isEmpty()) {
                            openClassGuiBedrock(player, classNum, "fine_form", "Alasan denda wajib diisi!");
                            return;
                        }

                        OfflinePlayer targetPlayer = Bukkit.getOfflinePlayer(targetName);
                        StudentProfile targetProfile = null;
                        if (targetPlayer.isOnline()) {
                            targetProfile = plugin.getProfileManager().getProfile(targetPlayer.getUniqueId());
                        } else {
                            try {
                                targetProfile = plugin.getDatabaseManager().loadProfile(targetPlayer.getUniqueId());
                            } catch (Exception ignored) {}
                        }

                        if (targetProfile == null || targetProfile.getAcademicClass() != classNum) {
                            openClassGuiBedrock(player, classNum, "fine_form", "Pemain " + targetName + " tidak terdaftar di kelas " + classNum + "!");
                            return;
                        }

                        CashOperationResult res = plugin.getClassCashManager().applyFine(targetPlayer, player, classNum, amount, reason);
                        if (res.isSuccess()) {
                            player.sendMessage(MiniMessage.miniMessage().deserialize("<green>" + res.getMessage() + "</green>"));
                            openClassGuiBedrock(player, classNum, "Fund", null);
                        } else {
                            openClassGuiBedrock(player, classNum, "fine_form", res.getMessage());
                        }
                    });
                FloodgateApi.getInstance().sendForm(player.getUniqueId(), fineBuilder.build());
                break;

            case "history":
                List<Map<String, Object>> txHistory = plugin.getDatabaseManager().getClassCashTransactions(classNum, 10);
                StringBuilder sb = new StringBuilder();
                if (txHistory.isEmpty()) {
                    sb.append("Belum ada riwayat transaksi.");
                } else {
                    for (Map<String, Object> tx : txHistory) {
                        String type = (String) tx.get("tx_type");
                        double amt = (double) tx.get("amount");
                        String desc = (String) tx.get("description");
                        Date date = (Date) tx.get("tx_date");
                        String dateStr = date != null ? dateFormat.format(date) : "-";

                        String color = type.equalsIgnoreCase("DEPOSIT") || type.equalsIgnoreCase("FINE") ? "+" : "-";
                        sb.append("[").append(dateStr).append("] ")
                          .append(color).append("Rp").append(String.format("%,.0f", amt))
                          .append(" - ").append(desc).append("\n");
                    }
                }

                SimpleForm historyForm = SimpleForm.builder()
                    .title("Riwayat Transaksi")
                    .content(sb.toString())
                    .button("Kembali")
                    .validResultHandler(response -> openClassGuiBedrock(player, classNum, "Fund", null))
                    .build();
                FloodgateApi.getInstance().sendForm(player.getUniqueId(), historyForm);
                break;

            case "struktur":
                String structureWali = "Tidak Ada";
                if (data.getWaliKelasUuid() != null) {
                    structureWali = data.getWaliKelasName() != null ? data.getWaliKelasName() : Bukkit.getOfflinePlayer(data.getWaliKelasUuid()).getName();
                    if (structureWali == null) structureWali = "Unknown";
                }

                String bKetua = "Tidak Ada";
                String bWakil = "Tidak Ada";
                String bSekretaris = "Tidak Ada";
                String bBendahara = "Tidak Ada";

                for (Map.Entry<UUID, OfficerInfo> entry : data.getOfficers().entrySet()) {
                    String role = entry.getValue().getRole();
                    String name = entry.getValue().getUsername();
                    if (name == null) name = "Unknown";

                    if ("KETUA".equalsIgnoreCase(role)) bKetua = name;
                    else if ("WAKIL".equalsIgnoreCase(role)) bWakil = name;
                    else if ("SEKRETARIS".equalsIgnoreCase(role)) bSekretaris = name;
                    else if ("BENDAHARA".equalsIgnoreCase(role)) bBendahara = name;
                }

                String structContent = "Wali Kelas: " + structureWali + "\n" +
                                       "Ketua Kelas: " + bKetua + "\n" +
                                       "Wakil Ketua: " + bWakil + "\n" +
                                       "Sekretaris: " + bSekretaris + "\n" +
                                       "Bendahara: " + bBendahara;

                SimpleForm structForm = SimpleForm.builder()
                    .title("Struktur Pengurus - " + className.trim())
                    .content(structContent)
                    .button("Kembali ke Menu Utama")
                    .validResultHandler(response -> openClassGuiBedrock(player, classNum, "MainMenu", null))
                    .build();
                FloodgateApi.getInstance().sendForm(player.getUniqueId(), structForm);
                break;

            case "siswa":
                List<Map<String, String>> classStudents = plugin.getDatabaseManager().getStudentsInClass(classNum);
                String listStr = classStudents.stream().map(m -> m.get("username")).collect(Collectors.joining(", "));
                if (listStr.isEmpty()) {
                    listStr = "Tidak ada murid terdaftar.";
                }

                SimpleForm studentsForm = SimpleForm.builder()
                    .title("Daftar Murid - " + className.trim())
                    .content(listStr)
                    .button("Kembali ke Menu Utama")
                    .validResultHandler(response -> openClassGuiBedrock(player, classNum, "MainMenu", null))
                    .build();
                FloodgateApi.getInstance().sendForm(player.getUniqueId(), studentsForm);
                break;
        }
    }
}
