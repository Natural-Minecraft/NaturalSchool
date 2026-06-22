package id.naturalsmp.naturalSchool.classes.gui;

import id.naturalsmp.naturalSchool.NaturalSchool;
import id.naturalsmp.naturalSchool.classes.ClassroomManager;
import id.naturalsmp.naturalSchool.ui.util.DialogFormatter;
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
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.geysermc.cumulus.form.CustomForm;
import org.geysermc.floodgate.api.FloodgateApi;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ClassroomManagerGui {

    private final NaturalSchool plugin;

    public ClassroomManagerGui(NaturalSchool plugin) {
        this.plugin = plugin;
    }

    public void openClassroomManagerJava(Player player) {
        List<DialogBody> bodies = new ArrayList<>();
        bodies.add(DialogBody.plainMessage(MiniMessage.miniMessage().deserialize("<green><bold>Classroom Wali Kelas Manager</bold></green>")));
        bodies.add(DialogBody.plainMessage(Component.text("Assign Wali Kelas or configure classroom bounds.")));

        ActionButton submitBtn = ActionButton.builder(Component.text("Apply"))
            .action(DialogAction.customClick((view, audience) -> {
                if (audience instanceof Player p) {
                    String classStr = view.getText("class_num");
                    String waliStr = view.getText("wali_kelas");
                    boolean setBounds = view.getBoolean("set_bounds");

                    int classNum;
                    try {
                        classNum = Integer.parseInt(classStr.replaceAll("\\D+", ""));
                    } catch (Exception e) {
                        p.sendMessage(MiniMessage.miniMessage().deserialize("<red>Kelas harus berupa angka 1-12!</red>"));
                        return;
                    }

                    if (classNum < 1 || classNum > 12) {
                        p.sendMessage(MiniMessage.miniMessage().deserialize("<red>Kelas harus bernilai 1-12!</red>"));
                        return;
                    }

                    if (waliStr != null && !waliStr.trim().isEmpty()) {
                        OfflinePlayer op = Bukkit.getOfflinePlayer(waliStr.trim());
                        plugin.getClassroomManager().updateWaliKelas(classNum, op.getUniqueId());
                        p.sendMessage(MiniMessage.miniMessage().deserialize("<green>Berhasil mengubah Wali Kelas " + classNum + " menjadi " + op.getName() + ".</green>"));
                    }

                    if (setBounds) {
                        try {
                            Integer[] sel = getWorldEditSelection(p);
                            if (sel != null) {
                                plugin.getClassroomManager().saveClassroomBounds(
                                    classNum,
                                    p.getWorld().getName(),
                                    sel[0], sel[1], sel[2],
                                    sel[3], sel[4], sel[5]
                                );
                                p.sendMessage(MiniMessage.miniMessage().deserialize("<green>Berhasil menyimpan batas koordinat area kelas " + classNum + " sesuai seleksi WorldEdit Anda!</green>"));
                            } else {
                                p.sendMessage(MiniMessage.miniMessage().deserialize("<red>Gagal! Anda belum membuat seleksi WorldEdit.</red>"));
                            }
                        } catch (Throwable t) {
                            p.sendMessage(MiniMessage.miniMessage().deserialize("<red>Gagal mengambil seleksi: " + t.getMessage() + "</red>"));
                        }
                    }
                }
            }, ClickCallback.Options.builder().uses(1).build()))
            .build();

        Dialog dialog = Dialog.create(builder -> builder.empty()
            .base(DialogBase.builder(Component.text("Wali Kelas Manager"))
                .canCloseWithEscape(true)
                .body(bodies)
                .inputs(List.of(
                    DialogInput.text("class_num", Component.text("Nomor Kelas (1-12)")).width(320).build(),
                    DialogInput.text("wali_kelas", Component.text("Nama Wali Kelas (Kosongkan jika tidak diubah)")).width(320).build(),
                    DialogInput.bool("set_bounds", Component.text("Atur Area Kelas dari seleksi WorldEdit"), false, "Yes", "No")
                ))
                .build())
            .type(DialogType.notice(submitBtn))
        );

        player.showDialog(dialog);
    }

    public void openClassroomManagerBedrock(Player player) {
        CustomForm form = CustomForm.builder()
            .title("Classroom & Wali Kelas Manager")
            .input("Nomor Kelas (1-12)", "10")
            .input("Nama Wali Kelas (Kosongkan jika tidak diubah)", "")
            .toggle("Atur Area Kelas dari seleksi WorldEdit", false)
            .validResultHandler(response -> {
                String classStr = response.asInput(0);
                String waliStr = response.asInput(1);
                boolean setBounds = response.asToggle(2);

                int classNum;
                try {
                    classNum = Integer.parseInt(classStr.replaceAll("\\D+", ""));
                } catch (Exception e) {
                    player.sendMessage(MiniMessage.miniMessage().deserialize("<red>Kelas harus berupa angka 1-12!</red>"));
                    return;
                }

                if (classNum < 1 || classNum > 12) {
                    player.sendMessage(MiniMessage.miniMessage().deserialize("<red>Kelas harus bernilai 1-12!</red>"));
                    return;
                }

                if (waliStr != null && !waliStr.trim().isEmpty()) {
                    OfflinePlayer op = Bukkit.getOfflinePlayer(waliStr.trim());
                    plugin.getClassroomManager().updateWaliKelas(classNum, op.getUniqueId());
                    player.sendMessage(MiniMessage.miniMessage().deserialize("<green>Berhasil mengubah Wali Kelas " + classNum + " menjadi " + op.getName() + ".</green>"));
                }

                if (setBounds) {
                    try {
                        Integer[] sel = getWorldEditSelection(player);
                        if (sel != null) {
                            plugin.getClassroomManager().saveClassroomBounds(
                                classNum,
                                player.getWorld().getName(),
                                sel[0], sel[1], sel[2],
                                sel[3], sel[4], sel[5]
                            );
                            player.sendMessage(MiniMessage.miniMessage().deserialize("<green>Berhasil menyimpan batas koordinat area kelas " + classNum + " sesuai seleksi WorldEdit Anda!</green>"));
                        } else {
                            player.sendMessage(MiniMessage.miniMessage().deserialize("<red>Gagal! Anda belum membuat seleksi WorldEdit.</red>"));
                        }
                    } catch (Throwable t) {
                        player.sendMessage(MiniMessage.miniMessage().deserialize("<red>Gagal mengambil seleksi: " + t.getMessage() + "</red>"));
                    }
                }
            })
            .build();

        FloodgateApi.getInstance().sendForm(player.getUniqueId(), form);
    }

    private Integer[] getWorldEditSelection(Player player) {
        try {
            if (Bukkit.getPluginManager().getPlugin("WorldEdit") == null) {
                return null;
            }
            Class<?> bukkitAdapterClass = Class.forName("com.sk89q.worldedit.bukkit.BukkitAdapter");
            Object actor = bukkitAdapterClass.getMethod("adapt", Player.class).invoke(null, player);
            Class<?> worldEditClass = Class.forName("com.sk89q.worldedit.WorldEdit");
            Object worldEditInstance = worldEditClass.getMethod("getInstance").invoke(null);
            Object sessionManager = worldEditClass.getMethod("getSessionManager").invoke(worldEditInstance);
            Class<?> sessionOwnerClass = Class.forName("com.sk89q.worldedit.session.SessionOwner");
            Object localSession = sessionManager.getClass().getMethod("get", sessionOwnerClass).invoke(sessionManager, actor);
            Object world = actor.getClass().getMethod("getWorld").invoke(actor);
            Class<?> worldClass = Class.forName("com.sk89q.worldedit.world.World");
            Object region = localSession.getClass().getMethod("getSelection", worldClass).invoke(localSession, world);
            if (region == null) {
                return null;
            }
            Object minPoint = region.getClass().getMethod("getMinimumPoint").invoke(region);
            Object maxPoint = region.getClass().getMethod("getMaximumPoint").invoke(region);
            int minX, minY, minZ, maxX, maxY, maxZ;
            try {
                minX = (int) minPoint.getClass().getMethod("getBlockX").invoke(minPoint);
                minY = (int) minPoint.getClass().getMethod("getBlockY").invoke(minPoint);
                minZ = (int) minPoint.getClass().getMethod("getBlockZ").invoke(minPoint);
                maxX = (int) maxPoint.getClass().getMethod("getBlockX").invoke(maxPoint);
                maxY = (int) maxPoint.getClass().getMethod("getBlockY").invoke(maxPoint);
                maxZ = (int) maxPoint.getClass().getMethod("getBlockZ").invoke(maxPoint);
            } catch (NoSuchMethodException e) {
                minX = ((Double) minPoint.getClass().getMethod("getX").invoke(minPoint)).intValue();
                minY = ((Double) minPoint.getClass().getMethod("getY").invoke(minPoint)).intValue();
                minZ = ((Double) minPoint.getClass().getMethod("getZ").invoke(minPoint)).intValue();
                maxX = ((Double) maxPoint.getClass().getMethod("getX").invoke(maxPoint)).intValue();
                maxY = ((Double) maxPoint.getClass().getMethod("getY").invoke(maxPoint)).intValue();
                maxZ = ((Double) maxPoint.getClass().getMethod("getZ").invoke(maxPoint)).intValue();
            }
            return new Integer[]{minX, minY, minZ, maxX, maxY, maxZ};
        } catch (Throwable t) {
            plugin.getLogger().warning("Error getting WorldEdit selection: " + t.getMessage());
            return null;
        }
    }
}
