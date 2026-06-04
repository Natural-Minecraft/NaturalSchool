package id.naturalsmp.naturalSchool;

import id.naturalsmp.naturalSchool.api.NaturalSchoolAPI;
import id.naturalsmp.naturalSchool.api.NaturalSchoolAPIImpl;
import id.naturalsmp.naturalSchool.api.NaturalSchoolProvider;
import id.naturalsmp.naturalSchool.database.DatabaseManager;
import id.naturalsmp.naturalSchool.database.RankPrefixConfig;
import id.naturalsmp.naturalSchool.listener.PlayerListener;
import id.naturalsmp.naturalSchool.profile.ProfileManager;
import id.naturalsmp.naturalSchool.profile.StudentProfile;
import id.naturalsmp.naturalSchool.command.NaturalSchoolCommand;
import id.naturalsmp.naturalSchool.placeholder.NaturalSchoolExpansion;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

public final class NaturalSchool extends JavaPlugin {

    private DatabaseManager databaseManager;
    private ProfileManager profileManager;
    private RankPrefixConfig rankPrefixConfig;
    private NaturalSchoolAPI api;

    @Override
    public void onEnable() {
        // Load default config configuration
        saveDefaultConfig();

        // Initialize & Register Developer API
        NaturalSchoolAPIImpl apiImpl = new NaturalSchoolAPIImpl(this);
        this.api = apiImpl;
        getServer().getServicesManager().register(NaturalSchoolAPI.class, apiImpl, this, ServicePriority.Normal);
        NaturalSchoolProvider.register(apiImpl);

        // Initialize Rank Prefix Configuration
        rankPrefixConfig = new RankPrefixConfig(this);
        rankPrefixConfig.load();

        // Initialize Database Infrastructure
        databaseManager = new DatabaseManager(this);
        databaseManager.initialize();

        // Initialize Profile Cache Manager
        profileManager = new ProfileManager(this, databaseManager);

        // Register Listeners
        getServer().getPluginManager().registerEvents(new PlayerListener(this, profileManager), this);

        // Register Commands
        NaturalSchoolCommand mainCommand = new NaturalSchoolCommand(this);
        org.bukkit.command.PluginCommand cmd = getCommand("naturalschool");
        if (cmd != null) {
            cmd.setExecutor(mainCommand);
            cmd.setTabCompleter(mainCommand);
        }

        // Register PlaceholderAPI Expansion if PAPI is present on the server
        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new NaturalSchoolExpansion(this).register();
            getLogger().info("PlaceholderAPI integration registered successfully.");
        }

        getLogger().info("NaturalSchool has been enabled successfully.");
    }

    @Override
    public void onDisable() {
        // Unregister Developer API Provider
        NaturalSchoolProvider.unregister();

        // Flush all cached profiles to database synchronously on shutdown
        if (profileManager != null && databaseManager != null) {
            getLogger().info("Flushing student profiles cache to the database...");
            for (StudentProfile profile : profileManager.getProfileCache().values()) {
                databaseManager.saveProfile(profile);
            }
            profileManager.getProfileCache().clear();
        }

        // Close Database Pools and Connections
        if (databaseManager != null) {
            databaseManager.close();
        }
        getLogger().info("NaturalSchool has been disabled successfully.");
    }

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    public ProfileManager getProfileManager() {
        return profileManager;
    }

    public RankPrefixConfig getRankPrefixConfig() {
        return rankPrefixConfig;
    }

    public NaturalSchoolAPI getNaturalSchoolAPI() {
        return api;
    }
}
