package id.naturalsmp.naturalSchool.classes.gui;

import id.naturalsmp.naturalSchool.NaturalSchool;
import id.naturalsmp.naturalSchool.classes.ClassSession;
import id.naturalsmp.naturalSchool.classes.ClassroomManager.ClassroomData;
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
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.geysermc.cumulus.form.CustomForm;
import org.geysermc.cumulus.form.SimpleForm;
import org.geysermc.floodgate.api.FloodgateApi;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class AttendanceGui {

    private final NaturalSchool plugin;

    public AttendanceGui(NaturalSchool plugin) {
        this.plugin = plugin;
    }

    public void openAttendanceGui(Player player) {
        if (FloodgateApi.getInstance().isFloodgatePlayer(player.getUniqueId())) {
            openAttendanceGuiBedrock(player);
        } else {
            openAttendanceGuiJava(player);
        }
    }

    // ==========================================
    // JAVA EDITION (PAPER DIALOGS) GUI
    // ==========================================

    public void openAttendanceGuiJava(Player player) {
        StudentProfile profile = plugin.getProfileManager().getProfile(player.getUniqueId());
        if (profile == null) {
            player.sendMessage(MiniMessage.miniMessage().deserialize("<red>Profil Anda tidak ditemukan!</red>"));
            return;
        }

        int classNum = profile.getAcademicClass();
        if (classNum <= 0) {
            player.sendMessage(MiniMessage.miniMessage().deserialize("<red>Anda tidak terdaftar di kelas manapun!</red>"));
            return;
        }

        String idKelas = "kelas" + classNum;
        ClassSession session = plugin.getClassManager().getSession(idKelas);
        if (session == null) {
            player.sendMessage(MiniMessage.miniMessage().deserialize("<red>Tidak ada sesi kelas aktif untuk kelas Anda saat ini!</red>"));
            return;
        }

        ClassroomData data = plugin.getClassroomManager().getClassroom(classNum);
        boolean isInside = data != null && data.hasBounds() && data.isInside(player.getLocation());

        if (isInside) {
            // Inside class: can Hadir or Izin
            List<DialogBody> bodies = new ArrayList<>();
            bodies.add(DialogBody.plainMessage(MiniMessage.miniMessage().deserialize("<green><bold>Presensi Kelas " + classNum + "</bold></green>")));
            bodies.add(DialogBody.plainMessage(Component.text("Silakan pilih status kehadiran Anda di bawah ini.")));

            ActionButton submitBtn = ActionButton.builder(Component.text("Kirim"))
                .action(DialogAction.customClick((view, audience) -> {
                    if (audience instanceof Player p) {
                        boolean isHadir = view.getBoolean("presence_status");
                        if (isHadir) {
                            // HADIR / TERLAMBAT
                            String status = "HADIR";
                            ZonedDateTime nowWib = ZonedDateTime.now(ZoneId.of("Asia/Jakarta"));
                            if (nowWib.getHour() >= 20) {
                                status = "TERLAMBAT";
                            }
                            session.getAttendanceMap().put(p.getUniqueId(), status);
                            p.sendMessage(MiniMessage.miniMessage().deserialize("<green>Presensi Anda berhasil dicatat sebagai " + status + "!</green>"));
                        } else {
                            // Show nested excuse dialog
                            Bukkit.getScheduler().runTask(plugin, () -> openExcuseReasonJava(p, session, data, null));
                        }
                    }
                }, ClickCallback.Options.builder().uses(1).build()))
                .build();

            Dialog dialog = Dialog.create(builder -> builder.empty()
                .base(DialogBase.builder(Component.text("Absen Kelas"))
                    .canCloseWithEscape(true)
                    .body(bodies)
                    .inputs(List.of(
                        DialogInput.bool("presence_status", Component.text("Status Absen (Aktifkan untuk Hadir, Matikan untuk Izin)"), true, "Hadir", "Izin")
                    ))
                    .build())
                .type(DialogType.notice(submitBtn))
            );

            player.showDialog(dialog);
        } else {
            // Outside class: only Izin
            List<DialogBody> bodies = new ArrayList<>();
            bodies.add(DialogBody.plainMessage(MiniMessage.miniMessage().deserialize("<red><bold>Di Luar Area Kelas</bold></red>")));
            bodies.add(DialogBody.plainMessage(Component.text("Anda berada di luar area kelas. Anda hanya diperbolehkan mengajukan Izin keluar kelas.")));

            ActionButton submitBtn = ActionButton.builder(Component.text("Ajukan Izin"))
                .action(DialogAction.customClick((view, audience) -> {
                    if (audience instanceof Player p) {
                        Bukkit.getScheduler().runTask(plugin, () -> openExcuseReasonJava(p, session, data, null));
                    }
                }, ClickCallback.Options.builder().uses(1).build()))
                .build();

            Dialog dialog = Dialog.create(builder -> builder.empty()
                .base(DialogBase.builder(Component.text("Absen Kelas (Luar Area)"))
                    .canCloseWithEscape(true)
                    .body(bodies)
                    .build())
                .type(DialogType.notice(submitBtn))
            );

            player.showDialog(dialog);
        }
    }

    private void openExcuseReasonJava(Player player, ClassSession session, ClassroomData data, String errorMsg) {
        List<DialogBody> bodies = new ArrayList<>();
        if (errorMsg != null) {
            bodies.add(DialogBody.plainMessage(MiniMessage.miniMessage().deserialize("<red><bold>[!] Error: " + errorMsg + "</bold></red>")));
        }
        bodies.add(DialogBody.plainMessage(MiniMessage.miniMessage().deserialize("<yellow><bold>Form Pengajuan Izin</bold></yellow>")));
        bodies.add(DialogBody.plainMessage(Component.text("Silakan tulis alasan Anda tidak dapat mengikuti kelas/ingin meninggalkan kelas.")));

        ActionButton submitBtn = ActionButton.builder(Component.text("Ajukan"))
            .action(DialogAction.customClick((view, audience) -> {
                if (audience instanceof Player p) {
                    String reason = view.getText("reason");
                    if (reason == null || reason.trim().isEmpty()) {
                        Bukkit.getScheduler().runTask(plugin, () -> openExcuseReasonJava(p, session, data, "Alasan izin wajib diisi!"));
                        return;
                    }

                    // Record leave
                    session.getAttendanceMap().put(p.getUniqueId(), "IZIN");
                    session.getAttendanceReasonMap().put(p.getUniqueId(), reason.trim());
                    p.sendMessage(MiniMessage.miniMessage().deserialize("<green>Izin Anda berhasil diajukan dengan alasan: " + reason.trim() + "</green>"));

                    // Teleport outside class
                    teleportOutsideClassroom(p, data);
                }
            }, ClickCallback.Options.builder().uses(1).build()))
            .build();

        Dialog dialog = Dialog.create(builder -> builder.empty()
            .base(DialogBase.builder(Component.text("Alasan Izin"))
                .canCloseWithEscape(true)
                .body(bodies)
                .inputs(List.of(
                    DialogInput.text("reason", Component.text("Alasan Izin")).width(320).build()
                ))
                .build())
            .type(DialogType.notice(submitBtn))
        );

        player.showDialog(dialog);
    }

    // ==========================================
    // BEDROCK EDITION (CUMULUS FORMS) GUI
    // ==========================================

    public void openAttendanceGuiBedrock(Player player) {
        StudentProfile profile = plugin.getProfileManager().getProfile(player.getUniqueId());
        if (profile == null) {
            player.sendMessage(MiniMessage.miniMessage().deserialize("<red>Profil Anda tidak ditemukan!</red>"));
            return;
        }

        int classNum = profile.getAcademicClass();
        if (classNum <= 0) {
            player.sendMessage(MiniMessage.miniMessage().deserialize("<red>Anda tidak terdaftar di kelas manapun!</red>"));
            return;
        }

        String idKelas = "kelas" + classNum;
        ClassSession session = plugin.getClassManager().getSession(idKelas);
        if (session == null) {
            player.sendMessage(MiniMessage.miniMessage().deserialize("<red>Tidak ada sesi kelas aktif untuk kelas Anda saat ini!</red>"));
            return;
        }

        ClassroomData data = plugin.getClassroomManager().getClassroom(classNum);
        boolean isInside = data != null && data.hasBounds() && data.isInside(player.getLocation());

        if (isInside) {
            SimpleForm form = SimpleForm.builder()
                .title("Absen Kelas - " + idKelas.toUpperCase())
                .content("Silakan pilih status kehadiran Anda.")
                .button("Hadir")
                .button("Izin")
                .validResultHandler(response -> {
                    int clicked = response.clickedButtonId();
                    if (clicked == 0) {
                        // HADIR / TERLAMBAT
                        String status = "HADIR";
                        ZonedDateTime nowWib = ZonedDateTime.now(ZoneId.of("Asia/Jakarta"));
                        if (nowWib.getHour() >= 20) {
                            status = "TERLAMBAT";
                        }
                        session.getAttendanceMap().put(player.getUniqueId(), status);
                        player.sendMessage(MiniMessage.miniMessage().deserialize("<green>Presensi Anda berhasil dicatat sebagai " + status + "!</green>"));
                    } else if (clicked == 1) {
                        openExcuseReasonBedrock(player, session, data, null);
                    }
                })
                .build();
            FloodgateApi.getInstance().sendForm(player.getUniqueId(), form);
        } else {
            // Outside class: directly request reason
            openExcuseReasonBedrock(player, session, data, null);
        }
    }

    private void openExcuseReasonBedrock(Player player, ClassSession session, ClassroomData data, String errorMsg) {
        CustomForm.Builder formBuilder = CustomForm.builder()
            .title("Form Alasan Izin");

        if (errorMsg != null) {
            formBuilder.label("§cError: " + errorMsg);
        }

        formBuilder.label("Anda mengajukan Izin karena berada di luar kelas atau memilih Izin.")
            .input("Alasan Izin", "Sakit / Ada Kepentingan")
            .validResultHandler(response -> {
                String reason = response.asInput(errorMsg != null ? 2 : 1);
                if (reason == null || reason.trim().isEmpty()) {
                    openExcuseReasonBedrock(player, session, data, "Alasan izin wajib diisi!");
                    return;
                }

                // Record leave
                session.getAttendanceMap().put(player.getUniqueId(), "IZIN");
                session.getAttendanceReasonMap().put(player.getUniqueId(), reason.trim());
                player.sendMessage(MiniMessage.miniMessage().deserialize("<green>Izin Anda berhasil diajukan dengan alasan: " + reason.trim() + "</green>"));

                // Teleport outside class
                teleportOutsideClassroom(player, data);
            });

        FloodgateApi.getInstance().sendForm(player.getUniqueId(), formBuilder.build());
    }

    // ==========================================
    // Teleport Helper
    // ==========================================

    private void teleportOutsideClassroom(Player player, ClassroomData data) {
        if (data != null && data.hasBounds()) {
            World world = Bukkit.getWorld(data.getWorldName());
            if (world != null) {
                int minX = Math.min(data.getX1(), data.getX2());
                double targetX = minX - 2.0;
                double targetZ = player.getLocation().getZ();

                int minZ = Math.min(data.getZ1(), data.getZ2());
                int maxZ = Math.max(data.getZ1(), data.getZ2());
                if (targetZ < minZ || targetZ > maxZ) {
                    targetZ = minZ + (maxZ - minZ) / 2.0;
                }

                double targetY = world.getHighestBlockYAt((int) targetX, (int) targetZ) + 1.0;

                Location loc = new Location(world, targetX, targetY, targetZ);
                loc.setYaw(player.getLocation().getYaw());
                loc.setPitch(player.getLocation().getPitch());

                player.teleport(loc);
                player.sendMessage(MiniMessage.miniMessage().deserialize("<yellow>Anda telah diteleportasi keluar dari area kelas.</yellow>"));
                return;
            }
        }
        player.teleport(player.getWorld().getSpawnLocation());
        player.sendMessage(MiniMessage.miniMessage().deserialize("<yellow>Batas area kelas belum diatur. Anda diteleportasi ke spawn.</yellow>"));
    }
}
