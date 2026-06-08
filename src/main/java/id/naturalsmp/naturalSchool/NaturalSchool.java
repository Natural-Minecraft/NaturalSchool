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
import id.naturalsmp.naturalSchool.command.SchoolCommand;
import id.naturalsmp.naturalSchool.placeholder.NaturalSchoolExpansion;
import id.naturalsmp.naturalSchool.ui.UIManager;
import id.naturalsmp.naturalSchool.ui.ExamManager;
import id.naturalsmp.naturalSchool.semester.SemesterManager;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

public final class NaturalSchool extends JavaPlugin {

    private DatabaseManager databaseManager;
    private ProfileManager profileManager;
    private RankPrefixConfig rankPrefixConfig;
    private UIManager uiManager;
    private NaturalSchoolAPI api;
    private SemesterManager semesterManager;
    private ExamManager examManager;

    @Override
    public void onEnable() {
        // Load default config configuration
        saveDefaultConfig();

        // Initialize Semester Manager
        semesterManager = new SemesterManager(this);

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

        // Initialize Exam Cache Manager & Webhook Server
        examManager = new ExamManager(this);
        examManager.initialize();

        // Initialize Profile Cache Manager
        profileManager = new ProfileManager(this, databaseManager);

        // Initialize UI Subsystem
        uiManager = new UIManager(this);

        // Register Listeners
        getServer().getPluginManager().registerEvents(new PlayerListener(this, profileManager), this);

        // Register Commands
        NaturalSchoolCommand mainCommand = new NaturalSchoolCommand(this);
        org.bukkit.command.PluginCommand cmd = getCommand("naturalschool");
        if (cmd != null) {
            cmd.setExecutor(mainCommand);
            cmd.setTabCompleter(mainCommand);
        }

        SchoolCommand schoolCommand = new SchoolCommand(this);
        org.bukkit.command.PluginCommand schoolCmd = getCommand("school");
        if (schoolCmd != null) {
            schoolCmd.setExecutor(schoolCommand);
            schoolCmd.setTabCompleter(schoolCommand);
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

        // Stop Webhook Server
        if (examManager != null) {
            examManager.stopWebhookServer();
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

    public ExamManager getExamManager() {
        return examManager;
    }

    public ProfileManager getProfileManager() {
        return profileManager;
    }

    public RankPrefixConfig getRankPrefixConfig() {
        return rankPrefixConfig;
    }

    public UIManager getUiManager() {
        return uiManager;
    }

    public NaturalSchoolAPI getNaturalSchoolAPI() {
        return api;
    }

    public SemesterManager getSemesterManager() {
        return semesterManager;
    }

    public boolean isExamOpen() {
        return getConfig().getBoolean("exam-settings.open", true);
    }

    public void setExamOpen(boolean open) {
        getConfig().set("exam-settings.open", open);
        saveConfig();
    }

    public String getExamMessage() {
        return getConfig().getString("exam-settings.message", "<yellow>Silakan pilih mata pelajaran untuk memulai ujian.</yellow>");
    }

    public void setExamMessage(String message) {
        getConfig().set("exam-settings.message", message);
        saveConfig();
    }
}
