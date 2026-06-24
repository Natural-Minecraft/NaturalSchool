package id.naturalsmp.naturalSchool.teacher;

import id.naturalsmp.naturalSchool.NaturalSchool;
import id.naturalsmp.naturalSchool.teacher.TeacherManager.ClaimResult;
import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickCallback;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.geysermc.cumulus.form.SimpleForm;
import org.geysermc.floodgate.api.FloodgateApi;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class TeacherSalaryGui {

    private final NaturalSchool plugin;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm");

    public TeacherSalaryGui(NaturalSchool plugin) {
        this.plugin = plugin;
    }

    public void openSalaryGui(Player player) {
        if (plugin.getServer().getPluginManager().getPlugin("Floodgate") != null &&
            FloodgateApi.getInstance().isFloodgatePlayer(player.getUniqueId())) {
            openSalaryGuiBedrock(player, null);
        } else {
            openSalaryGuiJava(player, null);
        }
    }

    private void openSalaryGuiJava(Player player, String message) {
        Teacher teacher = plugin.getTeacherManager().getTeacher(player.getUniqueId());
        List<DialogBody> bodies = new ArrayList<>();

        if (message != null) {
            bodies.add(DialogBody.plainMessage(MiniMessage.miniMessage().deserialize(message)));
            bodies.add(DialogBody.plainMessage(Component.text(" ")));
        }

        if (teacher == null) {
            bodies.add(DialogBody.plainMessage(MiniMessage.miniMessage().deserialize("<red><b>[!] Error:</b> Anda tidak terdaftar sebagai Staff Pengajar (Guru).</red>")));
            Dialog dialog = Dialog.create(builder -> builder.empty()
                .base(DialogBase.builder(Component.text("Slip Gaji Guru"))
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

        bodies.add(DialogBody.plainMessage(MiniMessage.miniMessage().deserialize("<gold>=== Slip Gaji Kepegawaian Guru ===</gold>")));
        bodies.add(DialogBody.plainMessage(MiniMessage.miniMessage().deserialize("<yellow>Nama:</yellow> <white>" + teacher.getName() + "</white>")));
        bodies.add(DialogBody.plainMessage(MiniMessage.miniMessage().deserialize("<yellow>Status Kepegawaian:</yellow> <white>" + teacher.getType(plugin).name() + "</white>")));
        bodies.add(DialogBody.plainMessage(MiniMessage.miniMessage().deserialize("<yellow>Peran:</yellow> <white>" + teacher.getRole().name() + "</white>")));
        
        String rateUnit = teacher.getType(plugin) == Teacher.TeacherType.TETAP ? "/minggu (Real-Life)" : "/sesi kelas";
        bodies.add(DialogBody.plainMessage(MiniMessage.miniMessage().deserialize("<yellow>Tarif Gaji:</yellow> <white>$" + String.format("%,.2f", teacher.getSalaryRate()) + " " + rateUnit + "</white>")));
        
        double pending = plugin.getTeacherManager().calculatePendingSalary(player.getUniqueId());
        bodies.add(DialogBody.plainMessage(MiniMessage.miniMessage().deserialize("<yellow>Gaji Belum Diklaim:</yellow> <green>$" + String.format("%,.2f", pending) + "</green>")));
        
        String lastClaimStr = teacher.getLastSalaryClaimTime() != null ? dateFormat.format(new Date(teacher.getLastSalaryClaimTime().getTime())) : "Belum Pernah";
        bodies.add(DialogBody.plainMessage(MiniMessage.miniMessage().deserialize("<yellow>Terakhir Klaim:</yellow> <gray>" + lastClaimStr + "</gray>")));
        bodies.add(DialogBody.plainMessage(Component.text(" ")));

        Component claimLink = MiniMessage.miniMessage().deserialize("<green><bold>[Klaim Gaji Sekarang]</bold></green>")
            .clickEvent(ClickEvent.callback(aud -> {
                if (aud instanceof Player p) {
                    ClaimResult res = plugin.getTeacherManager().claimSalary(p);
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        if (res.isSuccess()) {
                            openSalaryGuiJava(p, "<green>" + res.getMessage() + "</green>");
                        } else {
                            openSalaryGuiJava(p, "<red>" + res.getMessage() + "</red>");
                        }
                    });
                }
            }));
        bodies.add(DialogBody.plainMessage(claimLink));

        Dialog dialog = Dialog.create(builder -> builder.empty()
            .base(DialogBase.builder(Component.text("Slip Gaji - " + teacher.getName()))
                .canCloseWithEscape(true)
                .body(bodies)
                .build())
            .type(DialogType.notice(ActionButton.builder(Component.text("Tutup"))
                .action(DialogAction.customClick((view, audience) -> {}, ClickCallback.Options.builder().uses(1).build()))
                .build()))
        );

        player.showDialog(dialog);
    }

    private void openSalaryGuiBedrock(Player player, String message) {
        Teacher teacher = plugin.getTeacherManager().getTeacher(player.getUniqueId());

        if (teacher == null) {
            String content = "Anda tidak terdaftar sebagai Staff Pengajar (Guru).";
            if (message != null) {
                content = message + "\n\n" + content;
            }
            SimpleForm errForm = SimpleForm.builder()
                .title("Slip Gaji Guru")
                .content(content)
                .button("Tutup")
                .build();
            FloodgateApi.getInstance().sendForm(player.getUniqueId(), errForm);
            return;
        }

        String rateUnit = teacher.getType(plugin) == Teacher.TeacherType.TETAP ? "/minggu (Real-Life)" : "/sesi kelas";
        double pending = plugin.getTeacherManager().calculatePendingSalary(player.getUniqueId());
        String lastClaimStr = teacher.getLastSalaryClaimTime() != null ? dateFormat.format(new Date(teacher.getLastSalaryClaimTime().getTime())) : "Belum Pernah";

        StringBuilder sb = new StringBuilder();
        if (message != null) {
            sb.append(message).append("\n\n");
        }
        sb.append("Nama: ").append(teacher.getName()).append("\n")
          .append("Status Kepegawaian: ").append(teacher.getType(plugin).name()).append("\n")
          .append("Peran: ").append(teacher.getRole().name()).append("\n")
          .append("Tarif Gaji: $").append(String.format("%,.2f", teacher.getSalaryRate())).append(" ").append(rateUnit).append("\n")
          .append("Gaji Belum Diklaim: $").append(String.format("%,.2f", pending)).append("\n")
          .append("Terakhir Klaim: ").append(lastClaimStr);

        SimpleForm.Builder formBuilder = SimpleForm.builder()
            .title("Slip Gaji - " + teacher.getName())
            .content(sb.toString())
            .button("Klaim Gaji")
            .button("Tutup");

        formBuilder.validResultHandler(response -> {
            int clicked = response.clickedButtonId();
            if (clicked == 0) {
                ClaimResult res = plugin.getTeacherManager().claimSalary(player);
                if (res.isSuccess()) {
                    openSalaryGuiBedrock(player, "§a" + res.getMessage());
                } else {
                    openSalaryGuiBedrock(player, "§c" + res.getMessage());
                }
            }
        });

        FloodgateApi.getInstance().sendForm(player.getUniqueId(), formBuilder.build());
    }
}
