package id.naturalsmp.naturalSchool;

import id.naturalsmp.naturalSchool.api.NaturalSchoolAPI;
import id.naturalsmp.naturalSchool.api.NaturalSchoolAPIImpl;
import id.naturalsmp.naturalSchool.api.NaturalSchoolProvider;
import id.naturalsmp.naturalSchool.classsession.ClassManager;
import id.naturalsmp.naturalSchool.classsession.ClassSession;
import id.naturalsmp.naturalSchool.command.KelasCommand;
import id.naturalsmp.naturalSchool.database.DatabaseManager;
import id.naturalsmp.naturalSchool.database.RankPrefixConfig;
import id.naturalsmp.naturalSchool.listener.PlayerListener;
import id.naturalsmp.naturalSchool.profile.ProfileManager;
import id.naturalsmp.naturalSchool.profile.StudentProfile;
import id.naturalsmp.naturalSchool.command.NaturalSchoolCommand;
import id.naturalsmp.naturalSchool.command.SchoolCommand;
import id.naturalsmp.naturalSchool.placeholder.NaturalSchoolExpansion;
import id.naturalsmp.naturalSchool.ui.UIManager;
import id.naturalsmp.naturalSchool.exam.ExamManager;
import id.naturalsmp.naturalSchool.semester.SemesterManager;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
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
    private ClassManager classManager;

    @Override
    public void onEnable() {
        // Load default config configuration
        saveDefaultConfig();

        // Initialize Semester Manager
        semesterManager = new SemesterManager(this);

        // Initialize Class Manager
        classManager = new ClassManager(this);

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

        KelasCommand kelasCommand = new KelasCommand(this);
        org.bukkit.command.PluginCommand kelasCmd = getCommand("kelas");
        if (kelasCmd != null) {
            kelasCmd.setExecutor(kelasCommand);
            kelasCmd.setTabCompleter(kelasCommand);
        }

        // Register PlaceholderAPI Expansion if PAPI is present on the server
        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new NaturalSchoolExpansion(this).register();
            getLogger().info("PlaceholderAPI integration registered successfully.");
        }

        // Run initial semester auto-rotation check
        semesterManager.checkAndAutoRotate();

        // Automatic time scheduler task (WIB / GMT+7)
        getServer().getScheduler().runTaskTimer(this, () -> {
            java.time.ZonedDateTime nowWib = java.time.ZonedDateTime.now(java.time.ZoneId.of("Asia/Jakarta"));
            
            // Periodically check and auto-rotate semester if needed
            semesterManager.checkAndAutoRotate();

            int hour = nowWib.getHour();
            int minute = nowWib.getMinute();

            if (minute == 0 && hour == 18) {
                // 18:00 WIB - Start sessions for all classes automatically (SD/SMP/SMA classes 1-12)
                for (int i = 1; i <= 12; i++) {
                    String classId = "kelas" + i;
                    if (classManager.getSession(classId) == null) {
                        classManager.startSession(classId, "Pelajaran Umum", null);
                    }
                }
            }

            if (minute == 15 && hour == 18) {
                // 18:15 WIB - Auto Fallback check
                for (int i = 1; i <= 12; i++) {
                    String classId = "kelas" + i;
                    ClassSession session = classManager.getSession(classId);
                    if (session != null) {
                        if (session.getProjectorFileName() == null && session.getQuizFileName() == null) {
                            classManager.runAutoFallback(classId);
                        }
                    }
                }
            }

            if (minute == 0 && hour == 20) {
                // 20:00 WIB - Tolerance limit warning
                Bukkit.broadcast(MiniMessage.miniMessage().deserialize("<gray>[NaturalSchool]</gray> <red>Batas toleransi keterlambatan telah dikunci! Murid yang baru hadir/masuk kelas mulai sekarang akan dicatat sebagai TERLAMBAT.</red>"));
                classManager.sendDiscordLog("Batas Presensi Terkunci ⏱️", "Waktu toleransi keterlambatan (20:00 WIB) telah tercapai. Murid baru dicatat sebagai TERLAMBAT.", 16753920);
            }

            if (minute == 0 && hour == 21) {
                // 21:00 WIB - End class session globally
                for (int i = 1; i <= 12; i++) {
                    String classId = "kelas" + i;
                    classManager.stopSession(classId);
                }
            }
        }, 200L, 1200L); // check every 60s

        org.bukkit.Bukkit.getConsoleSender().sendMessage(
                org.bukkit.ChatColor.translateAlternateColorCodes('&',
                    "\n&a===============\n" +
                    "&b _   _       _                  _     &d____       _                 _\n" +
                    "&b| \ | | __ _| |_ _   _ _ __ __ _| |   &d/ ___|  ___| |__   ___   ___ | |\n" +
                    "&b|  \| |/ _` | __| | | | '__/ _` | |   &d\___ \ / __| '_ \ / _ \ / _ \| |\n" +
                    "&b| |\  | (_| | |_| |_| | | | (_| | |    &d___) | (__| | | | (_) | (_) | |\n" +
                    "&b|_| \_|\__,_|\__|\__,_|_|  \__,_|_|   &d|____/ \___|_| |_|\___/ \___/|_|\n" +
                    "&f       >> &bNaturalSchool v" + getDescription().getVersion() + " Enabled! <<\n" +
                    "&a===============\n"
                )
        );
                )
        );
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

    public ClassManager getClassManager() {
        return classManager;
    }

    public boolean isExamOpen() {
        return getConfig().getBoolean("exam-settings.open", true);
    }

    public void setExamOpen(boolean open) {
        getConfig().set("exam-settings.open", open);
        saveConfig();
    }

    private boolean examForceOpen = false;

    public boolean isExamForceOpen() {
        return examForceOpen;
    }

    public void setExamForceOpen(boolean forceOpen) {
        this.examForceOpen = forceOpen;
    }

    public String getExamMessage() {
        return getConfig().getString("exam-settings.message", "<yellow>Silakan pilih mata pelajaran untuk memulai ujian.</yellow>");
    }

    public void setExamMessage(String message) {
        getConfig().set("exam-settings.message", message);
        saveConfig();
    }
}
