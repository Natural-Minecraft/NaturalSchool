package id.naturalsmp.naturalSchool.placeholder;

import id.naturalsmp.naturalSchool.NaturalSchool;
import id.naturalsmp.naturalSchool.profile.SchoolRank;
import id.naturalsmp.naturalSchool.profile.StudentProfile;
import id.naturalsmp.naturalSchool.classes.ClassroomManager;
import id.naturalsmp.naturalSchool.classes.ClassroomManager.ClassroomData;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class NaturalSchoolExpansion extends PlaceholderExpansion {

    private final NaturalSchool plugin;

    public NaturalSchoolExpansion(NaturalSchool plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "naturalschool";
    }

    @Override
    public @NotNull String getAuthor() {
        return "NaturalTechnologies";
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public @Nullable String onRequest(OfflinePlayer player, @NotNull String params) {
        if (player == null) {
            return "";
        }

        // Anti-Lag Guardrail: Strictly read from cache, never query database synchronously
        StudentProfile profile = plugin.getProfileManager().getProfile(player.getUniqueId());
        if (profile == null) {
            if (params.equalsIgnoreCase("nis")) {
                return "-";
            }
            return "";
        }

        switch (params.toLowerCase()) {
            case "rank":
                SchoolRank rank = profile.getRank();
                if (rank == null) return "";
                return plugin.getRankPrefixConfig().getFormattedPrefix(rank);

            case "class":
                return String.valueOf(profile.getAcademicClass());

            case "stage":
                return profile.getAcademicStage() != null ? profile.getAcademicStage() : "";

            case "nis":
                String nis = profile.getNis();
                return (nis != null && !nis.trim().isEmpty()) ? nis : "-";

            case "class_cash":
            case "class_cash_balance":
                int cNumCash = profile.getAcademicClass();
                if (cNumCash >= 1 && cNumCash <= 12) {
                    ClassroomData data = plugin.getClassroomManager().getClassroom(cNumCash);
                    return String.format("%,.0f", data.getCashBalance());
                }
                return "0";

            case "class_weekly_fee":
            case "class_fee":
                int cNumFee = profile.getAcademicClass();
                if (cNumFee >= 1 && cNumFee <= 12) {
                    ClassroomData data = plugin.getClassroomManager().getClassroom(cNumFee);
                    return String.format("%,.0f", data.getWeeklyFee());
                }
                return "0";

            case "class_weekly_fee_enabled":
            case "class_fee_status":
                int cNumStatus = profile.getAcademicClass();
                if (cNumStatus >= 1 && cNumStatus <= 12) {
                    ClassroomData data = plugin.getClassroomManager().getClassroom(cNumStatus);
                    return data.isWeeklyFeeEnabled() ? "Aktif" : "Nonaktif";
                }
                return "Nonaktif";

            default:
                return null;
        }
    }
}
