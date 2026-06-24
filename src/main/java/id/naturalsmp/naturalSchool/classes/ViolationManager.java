package id.naturalsmp.naturalSchool.classes;

import id.naturalsmp.naturalSchool.NaturalSchool;
import id.naturalsmp.naturalSchool.profile.StudentProfile;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.UUID;

public class ViolationManager {

    private final NaturalSchool plugin;

    public ViolationManager(NaturalSchool plugin) {
        this.plugin = plugin;
    }

    /**
     * Records a student violation in the database and sends an in-game notification if the student is online.
     *
     * @param studentUuid   The unique ID of the student.
     * @param studentName   The username of the student.
     * @param studentNis    The NIS of the student, if available.
     * @param reporterUuid  The unique ID of the reporter (e.g. Guru BK/Admin).
     * @param reporterName  The name of the reporter.
     * @param violationType The category of the violation.
     * @param comment       A written comment detailing the offense.
     * @param points        Violation points to add.
     */
    public void recordViolation(UUID studentUuid, String studentName, String studentNis, UUID reporterUuid, String reporterName, String violationType, String comment, int points) {
        String finalNis = studentNis;
        if (finalNis == null || finalNis.isEmpty()) {
            StudentProfile profile = plugin.getProfileManager().getProfile(studentUuid);
            if (profile != null) {
                finalNis = profile.getNis();
            }
        }

        // Save to DB
        plugin.getDatabaseManager().saveViolation(studentUuid, studentName, finalNis, reporterUuid, reporterName, violationType, comment, points);

        // Notify online target student
        Player target = Bukkit.getPlayer(studentUuid);
        if (target != null && target.isOnline()) {
            target.sendMessage(MiniMessage.miniMessage().deserialize(
                    "\n<red><b>[PERINGATAN BK]</b></red> <gray>Anda telah dicatat melakukan pelanggaran:</gray>\n" +
                    "<yellow>» Tipe Pelanggaran:</yellow> <white>" + violationType + "</white> <red>(+" + points + " Poin)</red>\n" +
                    "<yellow>» Keterangan:</yellow> <white>" + (comment != null && !comment.trim().isEmpty() ? comment : "-") + "</white>\n" +
                    "<yellow>» Dilaporkan Oleh:</yellow> <gray>" + reporterName + "</gray>\n"
            ));
        } else {
            String subject = "Peringatan BK: " + violationType;
            String body = "Detail Pelanggaran:\n" +
                          "- Tipe Pelanggaran: " + violationType + " (+" + points + " Poin)\n" +
                          "- Keterangan: " + (comment != null && !comment.trim().isEmpty() ? comment : "-") + "\n" +
                          "- Dilaporkan Oleh: " + reporterName + "\n" +
                          "- Tanggal: " + new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm").format(new java.util.Date());

            plugin.getMailManager().sendMail(
                0,
                new UUID(0L, 0L),
                "Bimbingan Konseling (BK)",
                studentUuid.toString(),
                "PLAYER",
                "OFFICIAL",
                subject,
                body
            );
        }
    }
}
