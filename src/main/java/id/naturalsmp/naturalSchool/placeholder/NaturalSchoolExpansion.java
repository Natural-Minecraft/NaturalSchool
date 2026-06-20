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

            case "class_prefix": {
                int classNo = profile.getAcademicClass();
                if (classNo < 1 || classNo > 12) return "";
                String rawPrefix = plugin.getRankPrefixConfig().getClassPrefix(classNo);
                return formatRawPrefix(rawPrefix);
            }

            case "role": {
                int classNo = profile.getAcademicClass();
                String role = "ANGGOTA";
                if (classNo >= 1 && classNo <= 12) {
                    ClassroomData classData = plugin.getClassroomManager().getClassroom(classNo);
                    if (classData != null) {
                        if (player.getUniqueId().equals(classData.getWaliKelasUuid())) {
                            role = "WALI_KELAS";
                        } else {
                            ClassroomManager.OfficerInfo assignedOfficer = classData.getOfficers().get(player.getUniqueId());
                            if (assignedOfficer != null) {
                                role = assignedOfficer.getRole();
                            }
                        }
                    }
                }
                if (role.equals("ANGGOTA")) {
                    for (ClassroomData data : plugin.getClassroomManager().getAllClassroomData()) {
                        if (player.getUniqueId().equals(data.getWaliKelasUuid())) {
                            role = "WALI_KELAS";
                            break;
                        }
                    }
                }
                return role;
            }

            case "role_prefix": {
                int classNo = profile.getAcademicClass();
                String role = "ANGGOTA";
                if (classNo >= 1 && classNo <= 12) {
                    ClassroomData classData = plugin.getClassroomManager().getClassroom(classNo);
                    if (classData != null) {
                        if (player.getUniqueId().equals(classData.getWaliKelasUuid())) {
                            role = "WALI_KELAS";
                        } else {
                            ClassroomManager.OfficerInfo assignedOfficer = classData.getOfficers().get(player.getUniqueId());
                            if (assignedOfficer != null) {
                                role = assignedOfficer.getRole();
                            }
                        }
                    }
                }
                if (role.equals("ANGGOTA")) {
                    for (ClassroomData data : plugin.getClassroomManager().getAllClassroomData()) {
                        if (player.getUniqueId().equals(data.getWaliKelasUuid())) {
                            role = "WALI_KELAS";
                            break;
                        }
                    }
                }
                String rawPrefix = plugin.getRankPrefixConfig().getClassRolePrefix(role);
                return formatRawPrefix(rawPrefix);
            }

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

    private String formatRawPrefix(String rawVal) {
        if (rawVal == null || rawVal.isEmpty()) {
            return "";
        }
        
        // Process ItemsAdder if enabled
        String processed = rawVal;
        if (plugin.getRankPrefixConfig().isItemsAdderEnabled() && plugin.getRankPrefixConfig().getItemsAdderWrapper() != null) {
            try {
                Object wrapper = plugin.getRankPrefixConfig().getItemsAdderWrapper();
                processed = (String) wrapper.getClass()
                        .getMethod("replace", String.class)
                        .invoke(wrapper, processed);
            } catch (Exception ignored) {}
        }

        try {
            net.kyori.adventure.text.Component component;
            if (processed.contains("<") && processed.contains(">")) {
                component = MiniMessage.miniMessage().deserialize(processed);
            } else {
                String translated = org.bukkit.ChatColor.translateAlternateColorCodes('&', processed);
                component = LegacyComponentSerializer.legacySection().deserialize(translated);
            }
            return LegacyComponentSerializer.builder()
                    .hexColors()
                    .character(LegacyComponentSerializer.SECTION_CHAR)
                    .build()
                    .serialize(component);
        } catch (Exception e) {
            return org.bukkit.ChatColor.translateAlternateColorCodes('&', processed);
        }
    }
}
