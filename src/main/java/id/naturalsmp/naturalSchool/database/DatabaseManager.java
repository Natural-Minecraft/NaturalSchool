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

    /**
     * Initializes the database connection and tables.
     */
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
        
        // Optimizations for MySQL
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

    /**
     * Gets a database connection (from pool if MySQL, or a direct connection if SQLite).
     * Callers must close this connection using try-with-resources.
     */
    public Connection getConnection() throws SQLException {
        if ("MYSQL".equals(storageType)) {
            if (dataSource == null) {
                throw new SQLException("MySQL Data Source is not initialized!");
            }
            return dataSource.getConnection();
        } else {
            return DriverManager.getConnection("jdbc:sqlite:" + sqliteFile.getAbsolutePath());
        }
    }

    /**
     * Closes the database pool and connections.
     */
    public void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            dataSource = null;
            plugin.getLogger().info("MySQL Hikari Pool closed.");
        }
    }

    /**
     * Safely reloads database connections and connection pools.
     */
    public synchronized void reload() {
        plugin.getLogger().info("Reloading Database Manager...");
        close();
        initialize();
    }

    private void createTable() {
        String createTableSQL;
        if ("MYSQL".equals(storageType)) {
            createTableSQL = "CREATE TABLE IF NOT EXISTS naturalsmp_students ("
                    + "uuid VARCHAR(36) PRIMARY KEY, "
                    + "nis VARCHAR(20) UNIQUE, "
                    + "academic_stage VARCHAR(10) NOT NULL, "
                    + "academic_class INT NOT NULL, "
                    + "practical_passed BOOLEAN NOT NULL, "
                    + "temporary_grade INT NOT NULL, "
                    + "last_updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, "
                    + "rank VARCHAR(30) DEFAULT 'NONE'"
                    + ");";
        } else {
            createTableSQL = "CREATE TABLE IF NOT EXISTS naturalsmp_students ("
                    + "uuid TEXT PRIMARY KEY, "
                    + "nis TEXT UNIQUE, "
                    + "academic_stage TEXT NOT NULL, "
                    + "academic_class INTEGER NOT NULL, "
                    + "practical_passed INTEGER NOT NULL, "
                    + "temporary_grade INTEGER NOT NULL, "
                    + "last_updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP, "
                    + "rank TEXT DEFAULT 'NONE'"
                    + ");";
        }

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(createTableSQL);
            plugin.getLogger().info("Database table 'naturalsmp_students' verified/created successfully.");
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to create database table 'naturalsmp_students'!", e);
        }

        // Migrate: Add 'rank' column dynamically if it doesn't exist yet for existing databases
        try (Connection conn = getConnection()) {
            boolean hasRank = false;
            try (ResultSet rs = conn.getMetaData().getColumns(null, null, "naturalsmp_students", "rank")) {
                if (rs.next()) {
                    hasRank = true;
                }
            }
            if (!hasRank) {
                String alterTableSQL = "MYSQL".equals(storageType)
                        ? "ALTER TABLE naturalsmp_students ADD COLUMN rank VARCHAR(30) DEFAULT 'NONE';"
                        : "ALTER TABLE naturalsmp_students ADD COLUMN rank TEXT DEFAULT 'NONE';";
                try (Statement stmt = conn.createStatement()) {
                    stmt.execute(alterTableSQL);
                    plugin.getLogger().info("Database table 'naturalsmp_students' successfully migrated: added 'rank' column.");
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Could not verify or migrate 'rank' column in database.", e);
        }
    }

    /**
     * Loads a StudentProfile from the database by UUID.
     * This query runs synchronously within the method and should be executed on an async scheduler thread.
     */
    public StudentProfile loadProfile(UUID uuid) {
        String query = "SELECT * FROM naturalsmp_students WHERE uuid = ?;";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(query)) {
            
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String nis = rs.getString("nis");
                    String stage = rs.getString("academic_stage");
                    int academicClass = rs.getInt("academic_class");
                    boolean practicalPassed = rs.getBoolean("practical_passed");
                    int tempGrade = rs.getInt("temporary_grade");
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

                    return new StudentProfile(uuid, nis, stage, academicClass, practicalPassed, tempGrade, lastUpdated, rank);
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error loading profile for UUID: " + uuid, e);
        }
        return null;
    }

    /**
     * Saves a StudentProfile to the database using an upsert mechanism.
     * This query runs synchronously within the method and should be executed on an async scheduler thread.
     */
    public void saveProfile(StudentProfile profile) {
        String saveQuery;
        
        if ("MYSQL".equals(storageType)) {
            saveQuery = "INSERT INTO naturalsmp_students (uuid, nis, academic_stage, academic_class, practical_passed, temporary_grade, last_updated, rank) "
                    + "VALUES (?, ?, ?, ?, ?, ?, ?, ?) "
                    + "ON DUPLICATE KEY UPDATE "
                    + "nis = VALUES(nis), "
                    + "academic_stage = VALUES(academic_stage), "
                    + "academic_class = VALUES(academic_class), "
                    + "practical_passed = VALUES(practical_passed), "
                    + "temporary_grade = VALUES(temporary_grade), "
                    + "last_updated = VALUES(last_updated), "
                    + "rank = VALUES(rank);";
        } else {
            saveQuery = "INSERT OR REPLACE INTO naturalsmp_students (uuid, nis, academic_stage, academic_class, practical_passed, temporary_grade, last_updated, rank) "
                    + "VALUES (?, ?, ?, ?, ?, ?, ?, ?);";
        }

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(saveQuery)) {
            
            ps.setString(1, profile.getUuid().toString());
            ps.setString(2, profile.getNis());
            ps.setString(3, profile.getAcademicStage());
            ps.setInt(4, profile.getAcademicClass());
            ps.setBoolean(5, profile.isPracticalPassed());
            ps.setInt(6, profile.getTemporaryGrade());
            
            Timestamp now = new Timestamp(System.currentTimeMillis());
            ps.setTimestamp(7, now);
            profile.setLastUpdated(now);
            
            ps.setString(8, profile.getRank().name());

            ps.executeUpdate();
            
            if (plugin.getConfig().getBoolean("academic-settings.general-settings.enable-debug-logs", false)) {
                plugin.getLogger().info("Saved profile successfully to database for UUID: " + profile.getUuid());
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error saving profile for UUID: " + profile.getUuid(), e);
        }
    }
}
