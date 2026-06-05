package id.naturalsmp.naturalSchool.placeholder;

import id.naturalsmp.naturalSchool.NaturalSchool;
import id.naturalsmp.naturalSchool.profile.SchoolRank;
import id.naturalsmp.naturalSchool.profile.StudentProfile;
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
        return "1.1.0";
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

            default:
                return null;
        }
    }
}
