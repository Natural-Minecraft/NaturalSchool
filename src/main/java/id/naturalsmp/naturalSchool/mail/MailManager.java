package id.naturalsmp.naturalSchool.mail;

import id.naturalsmp.naturalSchool.NaturalSchool;
import id.naturalsmp.naturalSchool.profile.StudentProfile;
import id.naturalsmp.naturalSchool.util.ToastUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.UUID;

public class MailManager {

    private final NaturalSchool plugin;

    public MailManager(NaturalSchool plugin) {
        this.plugin = plugin;
    }

    public void sendMail(int parentId, UUID sender, String senderName, String recipientUuidStr, String recipientType, String mailType, String subject, String body) {
        plugin.getDatabaseManager().saveMail(parentId, sender, senderName, recipientUuidStr, recipientType, mailType, subject, body);

        // Real-time notifications
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            if ("PLAYER".equalsIgnoreCase(recipientType)) {
                try {
                    UUID recUuid = UUID.fromString(recipientUuidStr);
                    Player target = Bukkit.getPlayer(recUuid);
                    if (target != null && target.isOnline()) {
                        ToastUtil.sendToast(plugin, target, "Pesan Baru", "Dari: " + senderName, "minecraft:paper", "task");
                    }
                } catch (IllegalArgumentException ignored) {}
            } else if ("CLASS".equalsIgnoreCase(recipientType)) {
                try {
                    int classNum = Integer.parseInt(recipientUuidStr.replace("CLASS_", ""));
                    for (Player online : Bukkit.getOnlinePlayers()) {
                        StudentProfile profile = plugin.getProfileManager().getProfile(online.getUniqueId());
                        if (profile != null && profile.getAcademicClass() == classNum) {
                            ToastUtil.sendToast(plugin, online, "Surat Kelas Baru", "Dari: " + senderName, "minecraft:paper", "task");
                        }
                    }
                } catch (Exception ignored) {}
            } else if ("GLOBAL".equalsIgnoreCase(recipientType)) {
                for (Player online : Bukkit.getOnlinePlayers()) {
                    ToastUtil.sendToast(plugin, online, "Pengumuman Sekolah", "Dari: " + senderName, "minecraft:paper", "task");
                }
            }
        });
    }
}
