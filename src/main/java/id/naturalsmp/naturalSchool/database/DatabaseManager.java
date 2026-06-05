package id.naturalsmp.naturalSchool.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import id.naturalsmp.naturalSchool.NaturalSchool;
import id.naturalsmp.naturalSchool.profile.SchoolRank;
import id.naturalsmp.naturalSchool.profile.StudentProfile;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.UUID;
import java.util.logging.Level;

public class DatabaseManager {

    private final NaturalSchool plugin;
    private HikariDataSource dataSource;
    private String storageType;
    private File sqliteFile;

    public DatabaseManager(NaturalSchool plugin) {
        this.plugin = plugin;
    }

    public void initialize() {
        this.storageType = plugin.getConfig().getString("database.storage-type", "SQLITE").toUpperCase();
        
        if ("MYSQL".equals(storageType)) {
            setupMySQL();
        } else {
            setupSQLite();
        }
        
        createTable();
    }

    private void setupMySQL() {
        plugin.getLogger().info("Initializing MySQL Database Connection Pool (HikariCP)...");
        
        String host = plugin.getConfig().getString("database.host", "localhost");
        int port = plugin.getConfig().getInt("database.port", 3306);
        String database = plugin.getConfig().getString("database.database", "naturalschool");
        String username = plugin.getConfig().getString("database.username", "root");
        String password = plugin.getConfig().getString("database.password", "password");
        int maxPoolSize = plugin.getConfig().getInt("database.pool-settings.maximum-pool-size", 10);
        long timeout = plugin.getConfig().getLong("database.pool-settings.connection-timeout", 30000);

        HikariConfig config = new HikariConfig();
        
        config.setJdbcUrl("jdbc:mysql://" + host + ":" + port + "/" + database + "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC");
        config.setUsername(username);
        config.setPassword(password);
        
        config.setMaximumPoolSize(maxPoolSize);
        config.setConnectionTimeout(timeout);
        
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        config.addDataSourceProperty("useServerPrepStmts", "true");
        
        config.setPoolName("NaturalSchoolPool");

        try {
            this.dataSource = new HikariDataSource(config);
            plugin.getLogger().info("MySQL Database Connection Pool established successfully.");
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Could not establish MySQL connection pool! Check your settings.", e);
        }
    }

    private void setupSQLite() {
        plugin.getLogger().info("Initializing SQLite Database Connection...");
        
        File dataFolder = plugin.getDataFolder();
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }
        
        this.sqliteFile = new File(dataFolder, "school.db");
        if (!sqliteFile.exists()) {
            try {
                sqliteFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "Could not create SQLite database file!", e);
            }
        }

        try {
            Class.forName("org.sqlite.JDBC");
            plugin.getLogger().info("SQLite Database driver loaded successfully.");
        } catch (ClassNotFoundException e) {
            plugin.getLogger().severe("SQLite JDBC Driver not found! Ensure your environment supports SQLite.");
        }
    }

    public Connection getConnection() throws SQLException {
        if ("MYSQL".equals(storageType)) {
            if (dataSource == null) {
                throw new SQLException("MySQL Data Source is not initialized!");
            }
            return dataSource.getConnection();
        } else {
            Connection conn = DriverManager.getConnection("jdbc:sqlite:" + sqliteFile.getAbsolutePath());
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("PRAGMA busy_timeout = 5000;");
                stmt.execute("PRAGMA journal_mode = WAL;");
                stmt.execute("PRAGMA synchronous = NORMAL;");
            }
            return conn;
        }
    }

    public void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            dataSource = null;
            plugin.getLogger().info("MySQL Hikari Pool closed.");
        }
    }

    public synchronized void reload() {
        plugin.getLogger().info("Reloading Database Manager...");
        close();
        initialize();
    }

    private void createTable() {
        String createTableSQL;
        if ("MYSQL".equals(storageType)) {
            createTableSQL = "CREATE TABLE IF NOT EXISTS nschool_students ("
                    + "uuid VARCHAR(36) PRIMARY KEY, "
                    + "username VARCHAR(16) NOT NULL, "
                    + "nis VARCHAR(20) UNIQUE, "
                    + "academic_stage VARCHAR(10) NOT NULL, "
                    + "academic_class INT NOT NULL, "
                    + "last_updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, "
                    + "rank VARCHAR(30) DEFAULT 'NONE'"
                    + ");";
        } else {
            createTableSQL = "CREATE TABLE IF NOT EXISTS nschool_students ("
                    + "uuid TEXT PRIMARY KEY, "
                    + "username TEXT NOT NULL, "
                    + "nis TEXT UNIQUE, "
                    + "academic_stage TEXT NOT NULL, "
                    + "academic_class INTEGER NOT NULL, "
                    + "last_updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP, "
                    + "rank TEXT DEFAULT 'NONE'"
                    + ");";
        }

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(createTableSQL);
            plugin.getLogger().info("Database table 'nschool_students' verified/created successfully.");
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to create database table 'nschool_students'!", e);
        }
    }

    public StudentProfile loadProfile(UUID uuid) throws SQLException {
        String query = "SELECT * FROM nschool_students WHERE uuid = ?;";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(query)) {
            
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String username = rs.getString("username");
                    String nis = rs.getString("nis");
                    String stage = rs.getString("academic_stage");
                    int academicClass = rs.getInt("academic_class");
                    Timestamp lastUpdated = rs.getTimestamp("last_updated");
                    
                    String rankStr = rs.getString("rank");
                    SchoolRank rank = SchoolRank.NONE;
                    if (rankStr != null) {
                        try {
                            rank = SchoolRank.valueOf(rankStr.toUpperCase());
                        } catch (IllegalArgumentException e) {
                            // Defaults to SchoolRank.NONE
                        }
                    }

                    return new StudentProfile(uuid, username, nis, stage, academicClass, lastUpdated, rank);
                }
            }
        }
        return null;
    }

    public void saveProfile(StudentProfile profile) {
        String saveQuery;
        
        if ("MYSQL".equals(storageType)) {
            saveQuery = "INSERT INTO nschool_students (uuid, username, nis, academic_stage, academic_class, last_updated, rank) "
                    + "VALUES (?, ?, ?, ?, ?, ?, ?) "
                    + "ON DUPLICATE KEY UPDATE "
                    + "username = VALUES(username), "
                    + "nis = VALUES(nis), "
                    + "academic_stage = VALUES(academic_stage), "
                    + "academic_class = VALUES(academic_class), "
                    + "last_updated = VALUES(last_updated), "
                    + "rank = VALUES(rank);";
        } else {
            saveQuery = "INSERT OR REPLACE INTO nschool_students (uuid, username, nis, academic_stage, academic_class, last_updated, rank) "
                    + "VALUES (?, ?, ?, ?, ?, ?, ?);";
        }

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(saveQuery)) {
            
            ps.setString(1, profile.getUuid().toString());
            ps.setString(2, profile.getUsername());
            ps.setString(3, profile.getNis());
            ps.setString(4, profile.getAcademicStage());
            ps.setInt(5, profile.getAcademicClass());
            
            Timestamp now = new Timestamp(System.currentTimeMillis());
            ps.setTimestamp(6, now);
            profile.setLastUpdated(now);
            
            ps.setString(7, profile.getRank().name());

            ps.executeUpdate();
            
            if (plugin.getConfig().getBoolean("academic-settings.general-settings.enable-debug-logs", false)) {
                plugin.getLogger().info("Saved profile successfully to database for UUID: " + profile.getUuid());
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error saving profile for UUID: " + profile.getUuid(), e);
        }
    }

    public int getRegisteredNisCount() throws SQLException {
        String query = "SELECT COUNT(*) FROM nschool_students WHERE nis IS NOT NULL;";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(query);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        }
        return 0;
    }
}
