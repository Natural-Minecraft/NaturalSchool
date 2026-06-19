package id.naturalsmp.naturalSchool.classes;

import id.naturalsmp.naturalSchool.NaturalSchool;
import id.naturalsmp.naturalSchool.profile.StudentProfile;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ClassChatManager {

    private final NaturalSchool plugin;
    
    // Players who are currently in class-only chat channel (toggled)
    private final Set<UUID> classChatChannelPlayers = ConcurrentHashMap.newKeySet();
    
    // Players who have disabled LuckPerms rank prefix in class chat (norank)
    private final Set<UUID> noRankPlayers = ConcurrentHashMap.newKeySet();

    // Staff/Admins who are spying on class chat
    private final Set<UUID> chatSpyPlayers = ConcurrentHashMap.newKeySet();

    public ClassChatManager(NaturalSchool plugin) {
        this.plugin = plugin;
    }

    public boolean toggleClassChatChannel(UUID playerUuid) {
        if (classChatChannelPlayers.contains(playerUuid)) {
            classChatChannelPlayers.remove(playerUuid);
            return false;
        } else {
            classChatChannelPlayers.add(playerUuid);
            return true;
        }
    }

    public boolean isInClassChatChannel(UUID playerUuid) {
        return classChatChannelPlayers.contains(playerUuid);
    }

    public boolean toggleNoRank(UUID playerUuid) {
        if (noRankPlayers.contains(playerUuid)) {
            noRankPlayers.remove(playerUuid);
            return false;
        } else {
            noRankPlayers.add(playerUuid);
            return true;
        }
    }

    public boolean isNoRank(UUID playerUuid) {
        return noRankPlayers.contains(playerUuid);
    }

    public boolean toggleChatSpy(UUID playerUuid) {
        if (chatSpyPlayers.contains(playerUuid)) {
            chatSpyPlayers.remove(playerUuid);
            return false;
        } else {
            chatSpyPlayers.add(playerUuid);
            return true;
        }
    }

    public boolean isChatSpy(UUID playerUuid) {
        return chatSpyPlayers.contains(playerUuid);
    }

    /**
     * Converts a string with legacy formatting codes (e.g. &, §) into MiniMessage tags.
     */
    public String toMiniMessage(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        String translated = org.bukkit.ChatColor.translateAlternateColorCodes('&', text);
        try {
            Component component = LegacyComponentSerializer.legacySection().deserialize(translated);
            return MiniMessage.miniMessage().serialize(component);
        } catch (Exception e) {
            return text;
        }
    }

    public void sendClassChat(Player sender, String message) {
        StudentProfile profile = plugin.getProfileManager().getProfile(sender.getUniqueId());
        int classNum = 0;
        if (profile != null) {
            classNum = profile.getAcademicClass();
        }

        // If player has no class, check if they are a Wali Kelas for some class
        if (classNum == 0) {
            for (ClassroomManager.ClassroomData data : plugin.getClassroomManager().getAllClassroomData()) {
                if (sender.getUniqueId().equals(data.getWaliKelasUuid())) {
                    classNum = data.getIdKelas();
                    break;
                }
            }
        }

        if (classNum == 0) {
            sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>Anda tidak berada di kelas mana pun.</red>"));
            return;
        }

        // Fetch rank prefixes & config
        String classPrefix = plugin.getRankPrefixConfig().getClassPrefix(classNum);
        
        String role = "ANGGOTA";
        ClassroomManager.ClassroomData classData = plugin.getClassroomManager().getClassroom(classNum);
        if (classData != null) {
            if (sender.getUniqueId().equals(classData.getWaliKelasUuid())) {
                role = "WALI_KELAS";
            } else {
                ClassroomManager.OfficerInfo assignedOfficer = classData.getOfficers().get(sender.getUniqueId());
                if (assignedOfficer != null) {
                    role = assignedOfficer.getRole();
                }
            }
        }
        String roleTag = plugin.getRankPrefixConfig().getClassRolePrefix(role);

        // Fetch LuckPerms prefix
        String lpPrefix = "";
        try {
            if (Bukkit.getPluginManager().getPlugin("LuckPerms") != null) {
                Class<?> providerClass = Class.forName("net.luckperms.api.LuckPermsProvider");
                Object lpInstance = providerClass.getMethod("get").invoke(null);
                Object userManager = lpInstance.getClass().getMethod("getUserManager").invoke(lpInstance);
                Object lpUser = userManager.getClass().getMethod("getUser", UUID.class).invoke(userManager, sender.getUniqueId());
                if (lpUser != null) {
                    Object cachedData = lpUser.getClass().getMethod("getCachedData").invoke(lpUser);
                    Object metaData = cachedData.getClass().getMethod("getMetaData").invoke(cachedData);
                    String prefix = (String) metaData.getClass().getMethod("getPrefix").invoke(metaData);
                    if (prefix != null) {
                        lpPrefix = prefix;
                    }
                }
            }
        } catch (Throwable ignored) {}

        // Format templates
        String template = plugin.getConfig().getString("class-settings.chat-format", "<role_tag> <prefix_luckperms><displayname>: <message>");
        if (isNoRank(sender.getUniqueId())) {
            template = plugin.getConfig().getString("class-settings.chat-format-norank", "<role_tag> <displayname>: <message>");
        }

        String formatted = template
                .replace("<class_num>", String.valueOf(classNum))
                .replace("<class_prefix>", classPrefix)
                .replace("<role_tag>", roleTag)
                .replace("<prefix_luckperms>", toMiniMessage(lpPrefix))
                .replace("<displayname>", toMiniMessage(sender.getDisplayName()))
                .replace("<message>", MiniMessage.miniMessage().escapeTags(message));

        Component parsedMessage = MiniMessage.miniMessage().deserialize(formatted);

        // Send to class members (students who are in class classNum, or Wali Kelas)
        final int targetClassNum = classNum;
        final UUID waliKelasUuid = classData != null ? classData.getWaliKelasUuid() : null;

        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            boolean isMember = false;
            StudentProfile onlineProfile = plugin.getProfileManager().getProfile(onlinePlayer.getUniqueId());
            if (onlineProfile != null && onlineProfile.getAcademicClass() == targetClassNum) {
                isMember = true;
            } else if (onlinePlayer.getUniqueId().equals(waliKelasUuid)) {
                isMember = true;
            }

            if (isMember) {
                onlinePlayer.sendMessage(parsedMessage);
            } else if (isChatSpy(onlinePlayer.getUniqueId())) {
                // If they are spyling but not member, send spy notification
                Component spyPrefix = MiniMessage.miniMessage().deserialize("<gray>[SPY-CLASS-" + targetClassNum + "] </gray>");
                onlinePlayer.sendMessage(spyPrefix.append(parsedMessage));
            }
        }

        // Log to console
        Bukkit.getConsoleSender().sendMessage(MiniMessage.miniMessage().deserialize("<gray>[Kelas " + classNum + " Chat]</gray> ").append(parsedMessage));
    }
}
