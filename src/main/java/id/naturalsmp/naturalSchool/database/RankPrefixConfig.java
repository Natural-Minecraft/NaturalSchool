package id.naturalsmp.naturalSchool.database;

import id.naturalsmp.naturalSchool.NaturalSchool;
import id.naturalsmp.naturalSchool.profile.SchoolRank;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.EnumMap;
import java.util.Map;

public class RankPrefixConfig {
    private final NaturalSchool plugin;
    private final File configFile;
    private FileConfiguration config;
    private final Map<SchoolRank, String> formattedPrefixCache = new EnumMap<>(SchoolRank.class);
    private final Map<Integer, String> classPrefixCache = new java.util.concurrent.ConcurrentHashMap<>();
    private final Map<String, String> classRoleCache = new java.util.concurrent.ConcurrentHashMap<>();
    private boolean itemsAdderEnabled;
    private Object itemsAdderWrapper;

    public RankPrefixConfig(NaturalSchool plugin) {
        this.plugin = plugin;
        this.configFile = new File(plugin.getDataFolder(), "rankprefix.yml");
    }

    /**
     * Loads/reloads the rankprefix.yml configuration and rebuilds the legacy formatted prefix cache.
     */
    public synchronized void load() {
        if (!configFile.exists()) {
            plugin.saveResource("rankprefix.yml", false);
        }
        config = YamlConfiguration.loadConfiguration(configFile);

        // Detect ItemsAdder
        this.itemsAdderEnabled = Bukkit.getPluginManager().isPluginEnabled("ItemsAdder");
        if (itemsAdderEnabled) {
            try {
                // Dynamically load to prevent classloader error when ItemsAdder is missing
                Class<?> wrapperClass = Class.forName("id.naturalsmp.naturalSchool.hook.ItemsAdderImpl");
                this.itemsAdderWrapper = wrapperClass.getDeclaredConstructor().newInstance();
                plugin.getLogger().info("Successfully hooked into ItemsAdder for font images/icons.");
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to hook into ItemsAdder: " + e.getMessage());
                this.itemsAdderEnabled = false;
                this.itemsAdderWrapper = null;
            }
        } else {
            this.itemsAdderWrapper = null;
        }

        // Rebuild cache
        formattedPrefixCache.clear();
        for (SchoolRank rank : SchoolRank.values()) {
            String path = "ranks." + rank.name();
            String prefix = config.getString(path + ".prefix", "");
            String nameColor = config.getString(path + ".name-color", "");

            // Fallback to enum display name if configuration is completely missing
            if (prefix.isEmpty() && rank != SchoolRank.NONE) {
                prefix = rank.getDisplayName();
            }

            // Combine prefix + nameColor
            String combinedRaw = prefix + nameColor;

            // Apply ItemsAdder replacement if present
            if (itemsAdderEnabled && itemsAdderWrapper != null) {
                try {
                    combinedRaw = (String) itemsAdderWrapper.getClass()
                            .getMethod("replace", String.class)
                            .invoke(itemsAdderWrapper, combinedRaw);
                } catch (Exception e) {
                    plugin.getLogger().warning("Failed to process ItemsAdder replacements for rank " + rank.name() + ": " + e.getMessage());
                }
            }

            // Parse MiniMessage color gradients/RGB and serialize to legacy Section formatting for PAPI external tools
            String legacyFormatted = "";
            if (!combinedRaw.isEmpty()) {
                try {
                    net.kyori.adventure.text.Component component = MiniMessage.miniMessage().deserialize(combinedRaw);
                    legacyFormatted = LegacyComponentSerializer.builder()
                            .hexColors()
                            .character(LegacyComponentSerializer.SECTION_CHAR)
                            .build()
                            .serialize(component);
                } catch (Exception e) {
                    plugin.getLogger().warning("Failed to parse MiniMessage for rank " + rank.name() + ": " + e.getMessage());
                    legacyFormatted = combinedRaw;
                }
            }

            formattedPrefixCache.put(rank, legacyFormatted);
        }

        // Rebuild classPrefixCache and classRoleCache
        classPrefixCache.clear();
        if (config.isConfigurationSection("class-prefixes")) {
            for (String key : config.getConfigurationSection("class-prefixes").getKeys(false)) {
                try {
                    int classNum = Integer.parseInt(key);
                    String val = config.getString("class-prefixes." + key, "");
                    classPrefixCache.put(classNum, val);
                } catch (NumberFormatException ignored) {}
            }
        }

        classRoleCache.clear();
        if (config.isConfigurationSection("class-roles")) {
            for (String key : config.getConfigurationSection("class-roles").getKeys(false)) {
                String val = config.getString("class-roles." + key, "");
                classRoleCache.put(key.toUpperCase(), val);
            }
        }

        plugin.getLogger().info("Successfully loaded and cached " + formattedPrefixCache.size() + " rank prefixes, " 
            + classPrefixCache.size() + " class prefixes, and " + classRoleCache.size() + " class roles.");
    }

    /**
     * Gets the pre-compiled and legacy serialized prefix + name color for the given SchoolRank.
     *
     * @param rank the SchoolRank to look up
     * @return legacy serialized string with section signs for hex colors and icons
     */
    public String getFormattedPrefix(SchoolRank rank) {
        if (rank == null) {
            return "";
        }
        return formattedPrefixCache.getOrDefault(rank, "");
    }

    public String getClassPrefix(int classNum) {
        return classPrefixCache.getOrDefault(classNum, "");
    }

    public String getClassRolePrefix(String role) {
        if (role == null) return "";
        return classRoleCache.getOrDefault(role.toUpperCase(), "");
    }
}
