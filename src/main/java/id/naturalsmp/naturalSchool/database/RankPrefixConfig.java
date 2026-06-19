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
     * Loads/reloads the rankprefix configuration from the database and rebuilds the legacy formatted prefix cache.
     */
    public synchronized void load() {
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

        // Seed default prefixes from rankprefix.yml if the prefixes table is empty
        boolean empty = plugin.getDatabaseManager().isPrefixesTableEmpty();
        if (empty) {
            plugin.getLogger().info("Prefixes database table is empty. Seeding defaults from rankprefix.yml...");
            if (!configFile.exists()) {
                plugin.saveResource("rankprefix.yml", false);
            }
            config = YamlConfiguration.loadConfiguration(configFile);
            
            // Seed ranks (we combine prefix and nameColor during seeding for backward compatibility)
            if (config.isConfigurationSection("ranks")) {
                for (String key : config.getConfigurationSection("ranks").getKeys(false)) {
                    String prefix = config.getString("ranks." + key + ".prefix", "");
                    String nameColor = config.getString("ranks." + key + ".name-color", "");
                    plugin.getDatabaseManager().savePrefix("RANK", key.toUpperCase(), prefix + nameColor);
                }
            }
            
            // Seed class-prefixes
            if (config.isConfigurationSection("class-prefixes")) {
                for (String key : config.getConfigurationSection("class-prefixes").getKeys(false)) {
                    String prefix = config.getString("class-prefixes." + key, "");
                    plugin.getDatabaseManager().savePrefix("CLASS", key, prefix);
                }
            }
            
            // Seed class-roles
            if (config.isConfigurationSection("class-roles")) {
                for (String key : config.getConfigurationSection("class-roles").getKeys(false)) {
                    String prefix = config.getString("class-roles." + key, "");
                    plugin.getDatabaseManager().savePrefix("ROLE", key.toUpperCase(), prefix);
                }
            }
        }

        // Rebuild cache from database
        formattedPrefixCache.clear();
        classPrefixCache.clear();
        classRoleCache.clear();

        java.util.List<Map<String, String>> allPrefixes = plugin.getDatabaseManager().getAllPrefixes();
        for (Map<String, String> map : allPrefixes) {
            String type = map.get("target_type");
            String key = map.get("target_key");
            String rawVal = map.get("prefix");
            if (type == null || key == null || rawVal == null) continue;

            if ("RANK".equalsIgnoreCase(type)) {
                SchoolRank rank;
                try {
                    rank = SchoolRank.valueOf(key.toUpperCase());
                } catch (IllegalArgumentException e) {
                    continue;
                }

                // Process ItemsAdder
                String processed = rawVal;
                if (itemsAdderEnabled && itemsAdderWrapper != null) {
                    try {
                        processed = (String) itemsAdderWrapper.getClass()
                                .getMethod("replace", String.class)
                                .invoke(itemsAdderWrapper, processed);
                    } catch (Exception e) {
                        plugin.getLogger().warning("Failed to process ItemsAdder replacements for rank " + rank.name() + ": " + e.getMessage());
                    }
                }

                // Parse MiniMessage & legacy codes dynamically
                String legacyFormatted = "";
                if (!processed.isEmpty()) {
                    try {
                        net.kyori.adventure.text.Component component;
                        if (processed.contains("<") && processed.contains(">")) {
                            component = MiniMessage.miniMessage().deserialize(processed);
                        } else {
                            String translated = org.bukkit.ChatColor.translateAlternateColorCodes('&', processed);
                            component = LegacyComponentSerializer.legacySection().deserialize(translated);
                        }
                        
                        legacyFormatted = LegacyComponentSerializer.builder()
                                .hexColors()
                                .character(LegacyComponentSerializer.SECTION_CHAR)
                                .build()
                                .serialize(component);
                    } catch (Exception e) {
                        legacyFormatted = org.bukkit.ChatColor.translateAlternateColorCodes('&', processed);
                    }
                }
                formattedPrefixCache.put(rank, legacyFormatted);
            } else if ("CLASS".equalsIgnoreCase(type)) {
                try {
                    int classNum = Integer.parseInt(key);
                    classPrefixCache.put(classNum, rawVal);
                } catch (NumberFormatException ignored) {}
            } else if ("ROLE".equalsIgnoreCase(type)) {
                classRoleCache.put(key.toUpperCase(), rawVal);
            }
        }

        // Fill in missing ranks with default display names as fallback
        for (SchoolRank rank : SchoolRank.values()) {
            if (!formattedPrefixCache.containsKey(rank)) {
                formattedPrefixCache.put(rank, rank.getDisplayName());
            }
        }

        plugin.getLogger().info("Successfully loaded and cached " + formattedPrefixCache.size() + " rank prefixes, " 
            + classPrefixCache.size() + " class prefixes, and " + classRoleCache.size() + " class roles from database.");
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
