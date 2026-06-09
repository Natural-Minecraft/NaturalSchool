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
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
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
        migrateTable();
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
        String createLogTableSQL;
        String createQuestionsTableSQL;
        String createCoreStateTableSQL;
        String createAttendanceSQL;
        String createGradesSQL;
        String createRaporSQL;
        String createLessonFilesSQL;

        if ("MYSQL".equals(storageType)) {
            createTableSQL = "CREATE TABLE IF NOT EXISTS nschool_students ("
                    + "uuid VARCHAR(36) PRIMARY KEY, "
                    + "username VARCHAR(16) NOT NULL, "
                    + "nis VARCHAR(20) UNIQUE, "
                    + "academic_stage VARCHAR(10) DEFAULT 'NONE', "
                    + "academic_class INT DEFAULT 0, "
                    + "current_semester VARCHAR(6) NOT NULL DEFAULT 'GANJIL', "
                    + "rank VARCHAR(30) DEFAULT 'NONE', "
                    + "last_updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP"
                    + ");";
            createLogTableSQL = "CREATE TABLE IF NOT EXISTS nschool_semester_log ("
                    + "id INT AUTO_INCREMENT PRIMARY KEY, "
                    + "academic_year VARCHAR(20) NOT NULL, "
                    + "semester VARCHAR(6) NOT NULL, "
                    + "total_students_affected INT NOT NULL, "
                    + "timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP"
                    + ");";
            createQuestionsTableSQL = "CREATE TABLE IF NOT EXISTS nschool_exam_questions ("
                    + "id INT AUTO_INCREMENT PRIMARY KEY, "
                    + "question_id VARCHAR(50) UNIQUE, "
                    + "subject VARCHAR(50), "
                    + "academic_class INT, "
                    + "question_type VARCHAR(20), "
                    + "question_text TEXT, "
                    + "options JSON, "
                    + "correct_answer TEXT, "
                    + "correct_indices JSON, "
                    + "INDEX idx_subject (subject), "
                    + "INDEX idx_class (academic_class)"
                    + ");";
            createCoreStateTableSQL = "CREATE TABLE IF NOT EXISTS nschool_core_state ("
                    + "state_key VARCHAR(50) PRIMARY KEY, "
                    + "state_value TEXT, "
                    + "description TEXT"
                    + ");";
            createAttendanceSQL = "CREATE TABLE IF NOT EXISTS natural_student_attendance ("
                    + "id_log INT AUTO_INCREMENT PRIMARY KEY, "
                    + "player_uuid VARCHAR(36) NOT NULL, "
                    + "player_name VARCHAR(16) NOT NULL, "
                    + "id_kelas VARCHAR(10) NOT NULL, "
                    + "id_helper VARCHAR(36) NOT NULL, "
                    + "mata_pelajaran VARCHAR(32) NOT NULL, "
                    + "status_kehadiran ENUM('HADIR', 'TERLAMBAT', 'ALFA', 'IZIN', 'SAKIT') NOT NULL, "
                    + "waktu_record TIMESTAMP DEFAULT CURRENT_TIMESTAMP, "
                    + "INDEX (player_uuid), "
                    + "INDEX (status_kehadiran)"
                    + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;";
            createGradesSQL = "CREATE TABLE IF NOT EXISTS natural_academic_grades ("
                    + "id_nilai INT AUTO_INCREMENT PRIMARY KEY, "
                    + "player_uuid VARCHAR(36) NOT NULL, "
                    + "jenjang ENUM('SD', 'SMP', 'SMA') NOT NULL, "
                    + "mata_pelajaran VARCHAR(32) NOT NULL, "
                    + "nilai_angka TINYINT UNSIGNED NOT NULL, "
                    + "tipe_ujian ENUM('HARIAN', 'AKHIR_TAHUN', 'PR_PERPUSTAKAAN', 'REMEDIAL') NOT NULL, "
                    + "jumlah_remedial_diambil TINYINT DEFAULT 0, "
                    + "alasan_nilai TEXT DEFAULT NULL, "
                    + "waktu_input TIMESTAMP DEFAULT CURRENT_TIMESTAMP, "
                    + "UNIQUE KEY unique_student_subject (player_uuid, mata_pelajaran, tipe_ujian), "
                    + "INDEX (player_uuid)"
                    + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;";
            createRaporSQL = "CREATE TABLE IF NOT EXISTS natural_e_rapor_digital ("
                    + "id_rapor INT AUTO_INCREMENT PRIMARY KEY, "
                    + "player_uuid VARCHAR(36) NOT NULL, "
                    + "nomor_induk_siswa VARCHAR(12) NOT NULL UNIQUE, "
                    + "tahun_ajaran_rl VARCHAR(10) NOT NULL, "
                    + "jenjang_terakhir ENUM('SD', 'SMP', 'SMA') NOT NULL, "
                    + "total_hadir SMALLINT DEFAULT 0, "
                    + "total_terlambat SMALLINT DEFAULT 0, "
                    + "total_alfa SMALLINT DEFAULT 0, "
                    + "total_izin_sakit SMALLINT DEFAULT 0, "
                    + "nilai_rata_rata_kolektif DECIMAL(5,2) NOT NULL, "
                    + "status_kelulusan ENUM('LULUS', 'TINGGAL_KELAS', 'PROSES') DEFAULT 'PROSES', "
                    + "catatan_wali_kelas TEXT, "
                    + "waktu_cetak TIMESTAMP DEFAULT CURRENT_TIMESTAMP, "
                    + "INDEX (player_uuid)"
                    + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;";
            createLessonFilesSQL = "CREATE TABLE IF NOT EXISTS natural_lesson_files ("
                    + "id_file INT AUTO_INCREMENT PRIMARY KEY, "
                    + "nama_file VARCHAR(64) NOT NULL UNIQUE, "
                    + "tipe ENUM('MATERI_PROYEKTOR', 'SOAL_KUIS') NOT NULL, "
                    + "jenjang ENUM('SD', 'SMP', 'SMA') NOT NULL, "
                    + "mata_pelajaran VARCHAR(32) NOT NULL, "
                    + "konten_json TEXT NOT NULL, "
                    + "dibuat_oleh VARCHAR(36) NOT NULL, "
                    + "dibuat_pada TIMESTAMP DEFAULT CURRENT_TIMESTAMP, "
                    + "dipakai_terakhir TIMESTAMP NULL, "
                    + "INDEX (jenjang, mata_pelajaran), "
                    + "INDEX (tipe)"
                    + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;";
        } else {
            createTableSQL = "CREATE TABLE IF NOT EXISTS nschool_students ("
                    + "uuid TEXT PRIMARY KEY, "
                    + "username TEXT NOT NULL, "
                    + "nis TEXT UNIQUE, "
                    + "academic_stage TEXT DEFAULT 'NONE', "
                    + "academic_class INTEGER DEFAULT 0, "
                    + "current_semester TEXT NOT NULL DEFAULT 'GANJIL', "
                    + "rank TEXT DEFAULT 'NONE', "
                    + "last_updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP"
                    + ");";
            createLogTableSQL = "CREATE TABLE IF NOT EXISTS nschool_semester_log ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + "academic_year TEXT NOT NULL, "
                    + "semester TEXT NOT NULL, "
                    + "total_students_affected INTEGER NOT NULL, "
                    + "timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP"
                    + ");";
            createQuestionsTableSQL = "CREATE TABLE IF NOT EXISTS nschool_exam_questions ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + "question_id TEXT UNIQUE, "
                    + "subject TEXT, "
                    + "academic_class INTEGER, "
                    + "question_type TEXT, "
                    + "question_text TEXT, "
                    + "options TEXT, "
                    + "correct_answer TEXT, "
                    + "correct_indices TEXT"
                    + ");";
            createCoreStateTableSQL = "CREATE TABLE IF NOT EXISTS nschool_core_state ("
                    + "state_key TEXT PRIMARY KEY, "
                    + "state_value TEXT, "
                    + "description TEXT"
                    + ");";
            createAttendanceSQL = "CREATE TABLE IF NOT EXISTS natural_student_attendance ("
                    + "id_log INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + "player_uuid TEXT NOT NULL, "
                    + "player_name TEXT NOT NULL, "
                    + "id_kelas TEXT NOT NULL, "
                    + "id_helper TEXT NOT NULL, "
                    + "mata_pelajaran TEXT NOT NULL, "
                    + "status_kehadiran TEXT NOT NULL, "
                    + "waktu_record TIMESTAMP DEFAULT CURRENT_TIMESTAMP"
                    + ");";
            createGradesSQL = "CREATE TABLE IF NOT EXISTS natural_academic_grades ("
                    + "id_nilai INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + "player_uuid TEXT NOT NULL, "
                    + "jenjang TEXT NOT NULL, "
                    + "mata_pelajaran TEXT NOT NULL, "
                    + "nilai_angka INTEGER NOT NULL, "
                    + "tipe_ujian TEXT NOT NULL, "
                    + "jumlah_remedial_diambil INTEGER DEFAULT 0, "
                    + "alasan_nilai TEXT DEFAULT NULL, "
                    + "waktu_input TIMESTAMP DEFAULT CURRENT_TIMESTAMP, "
                    + "UNIQUE(player_uuid, mata_pelajaran, tipe_ujian)"
                    + ");";
            createRaporSQL = "CREATE TABLE IF NOT EXISTS natural_e_rapor_digital ("
                    + "id_rapor INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + "player_uuid TEXT NOT NULL, "
                    + "nomor_induk_siswa TEXT NOT NULL UNIQUE, "
                    + "tahun_ajaran_rl TEXT NOT NULL, "
                    + "jenjang_terakhir TEXT NOT NULL, "
                    + "total_hadir INTEGER DEFAULT 0, "
                    + "total_terlambat INTEGER DEFAULT 0, "
                    + "total_alfa INTEGER DEFAULT 0, "
                    + "total_izin_sakit INTEGER DEFAULT 0, "
                    + "nilai_rata_rata_kolektif REAL NOT NULL, "
                    + "status_kelulusan TEXT DEFAULT 'PROSES', "
                    + "catatan_wali_kelas TEXT, "
                    + "waktu_cetak TIMESTAMP DEFAULT CURRENT_TIMESTAMP"
                    + ");";
            createLessonFilesSQL = "CREATE TABLE IF NOT EXISTS natural_lesson_files ("
                    + "id_file INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + "nama_file TEXT NOT NULL UNIQUE, "
                    + "tipe TEXT NOT NULL, "
                    + "jenjang TEXT NOT NULL, "
                    + "mata_pelajaran TEXT NOT NULL, "
                    + "konten_json TEXT NOT NULL, "
                    + "dibuat_oleh TEXT NOT NULL, "
                    + "dibuat_pada TIMESTAMP DEFAULT CURRENT_TIMESTAMP, "
                    + "dipakai_terakhir TIMESTAMP NULL"
                    + ");";
        }

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(createTableSQL);
            stmt.execute(createLogTableSQL);
            stmt.execute(createQuestionsTableSQL);
            stmt.execute(createCoreStateTableSQL);
            stmt.execute(createAttendanceSQL);
            stmt.execute(createGradesSQL);
            stmt.execute(createRaporSQL);
            stmt.execute(createLessonFilesSQL);
            
            if (!"MYSQL".equals(storageType)) {
                stmt.execute("CREATE INDEX IF NOT EXISTS idx_questions_sub ON nschool_exam_questions (subject);");
                stmt.execute("CREATE INDEX IF NOT EXISTS idx_questions_cls ON nschool_exam_questions (academic_class);");
                stmt.execute("CREATE INDEX IF NOT EXISTS idx_attendance_player ON natural_student_attendance (player_uuid);");
                stmt.execute("CREATE INDEX IF NOT EXISTS idx_attendance_status ON natural_student_attendance (status_kehadiran);");
                stmt.execute("CREATE INDEX IF NOT EXISTS idx_grades_player ON natural_academic_grades (player_uuid);");
                stmt.execute("CREATE INDEX IF NOT EXISTS idx_rapor_player ON natural_e_rapor_digital (player_uuid);");
                stmt.execute("CREATE INDEX IF NOT EXISTS idx_lesson_files_jenjang ON natural_lesson_files (jenjang, mata_pelajaran);");
                stmt.execute("CREATE INDEX IF NOT EXISTS idx_lesson_files_tipe ON natural_lesson_files (tipe);");
            }
            
            // Seeding default values for nschool_core_state
            String seedVersionSQL = "INSERT INTO nschool_core_state (state_key, state_value, description) "
                    + "SELECT 'exam_version', '1', 'Exam questions version' "
                    + "WHERE NOT EXISTS (SELECT 1 FROM nschool_core_state WHERE state_key = 'exam_version');";
            String seedStatusSQL = "INSERT INTO nschool_core_state (state_key, state_value, description) "
                    + "SELECT 'portal_status', 'CLOSED', 'Status of the exam portal' "
                    + "WHERE NOT EXISTS (SELECT 1 FROM nschool_core_state WHERE state_key = 'portal_status');";
            String seedMessageSQL = "INSERT INTO nschool_core_state (state_key, state_value, description) "
                    + "SELECT 'portal_message', 'Portal Ujian Sedang ditutup!', 'Message displayed when the portal is closed' "
                    + "WHERE NOT EXISTS (SELECT 1 FROM nschool_core_state WHERE state_key = 'portal_message');";
            
            stmt.execute(seedVersionSQL);
            stmt.execute(seedStatusSQL);
            stmt.execute(seedMessageSQL);

            plugin.getLogger().info("Database tables verified/created successfully.");
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to create database tables!", e);
        }
    }

    private void migrateTable() {
        try (Connection conn = getConnection()) {
            if (!hasColumn(conn, "nschool_students", "current_semester")) {
                plugin.getLogger().info("Migrating database: Column 'current_semester' is missing in 'nschool_students'. Adding column...");
                String alterSQL;
                if ("MYSQL".equals(storageType)) {
                    alterSQL = "ALTER TABLE nschool_students ADD COLUMN current_semester VARCHAR(6) NOT NULL DEFAULT 'GANJIL';";
                } else {
                    alterSQL = "ALTER TABLE nschool_students ADD COLUMN current_semester TEXT NOT NULL DEFAULT 'GANJIL';";
                }
                try (Statement stmt = conn.createStatement()) {
                    stmt.execute(alterSQL);
                    plugin.getLogger().info("Successfully added 'current_semester' column to 'nschool_students' table.");
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to perform database migration for column 'current_semester'!", e);
        }
    }

    private boolean hasColumn(Connection conn, String tableName, String columnName) {
        try {
            java.sql.DatabaseMetaData dbm = conn.getMetaData();
            try (ResultSet rs = dbm.getColumns(null, null, tableName, columnName)) {
                if (rs.next()) {
                    return true;
                }
            }
            try (ResultSet rs = dbm.getColumns(null, null, tableName.toUpperCase(), columnName.toUpperCase())) {
                if (rs.next()) {
                    return true;
                }
            }
            try (ResultSet rs = dbm.getColumns(null, null, tableName.toLowerCase(), columnName.toLowerCase())) {
                if (rs.next()) {
                    return true;
                }
            }
        } catch (SQLException e) {
            // Fallback
        }
        
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("SELECT " + columnName + " FROM " + tableName + " LIMIT 1;");
            return true;
        } catch (SQLException e) {
            return false;
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
                    String currentSemester = rs.getString("current_semester");
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

                    return new StudentProfile(uuid, username, nis, stage, academicClass, currentSemester, lastUpdated, rank);
                }
            }
        }
        return null;
    }

    public void saveProfile(StudentProfile profile) {
        String saveQuery;
        
        if ("MYSQL".equals(storageType)) {
            saveQuery = "INSERT INTO nschool_students (uuid, username, nis, academic_stage, academic_class, current_semester, rank, last_updated) "
                    + "VALUES (?, ?, ?, ?, ?, ?, ?, ?) "
                    + "ON DUPLICATE KEY UPDATE "
                    + "username = VALUES(username), "
                    + "nis = VALUES(nis), "
                    + "academic_stage = VALUES(academic_stage), "
                    + "academic_class = VALUES(academic_class), "
                    + "current_semester = VALUES(current_semester), "
                    + "rank = VALUES(rank), "
                    + "last_updated = VALUES(last_updated);";
        } else {
            saveQuery = "INSERT OR REPLACE INTO nschool_students (uuid, username, nis, academic_stage, academic_class, current_semester, rank, last_updated) "
                    + "VALUES (?, ?, ?, ?, ?, ?, ?, ?);";
        }

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(saveQuery)) {
            
            ps.setString(1, profile.getUuid().toString());
            ps.setString(2, profile.getUsername());
            ps.setString(3, profile.getNis());
            ps.setString(4, profile.getAcademicStage());
            ps.setInt(5, profile.getAcademicClass());
            ps.setString(6, profile.getCurrentSemester());
            ps.setString(7, profile.getRank().name());
            
            Timestamp now = new Timestamp(System.currentTimeMillis());
            ps.setTimestamp(8, now);
            profile.setLastUpdated(now);

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

    public String getCoreState(String key) {
        String query = "SELECT state_value FROM nschool_core_state WHERE state_key = ?;";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setString(1, key);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("state_value");
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error loading core state for key: " + key, e);
        }
        return null;
    }

    public void setCoreState(String key, String value) {
        String query;
        if ("MYSQL".equals(storageType)) {
            query = "INSERT INTO nschool_core_state (state_key, state_value) VALUES (?, ?) "
                    + "ON DUPLICATE KEY UPDATE state_value = VALUES(state_value);";
        } else {
            query = "INSERT OR REPLACE INTO nschool_core_state (state_key, state_value) VALUES (?, ?);";
        }

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setString(1, key);
            ps.setString(2, value);
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error setting core state for key: " + key, e);
        }
    }

    public List<Map<String, Object>> getAllExamQuestions() {
        List<Map<String, Object>> list = new ArrayList<>();
        String query = "SELECT * FROM nschool_exam_questions;";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(query);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Map<String, Object> map = new HashMap<>();
                map.put("question_id", rs.getString("question_id"));
                map.put("subject", rs.getString("subject"));
                map.put("academic_class", rs.getInt("academic_class"));
                map.put("question_type", rs.getString("question_type"));
                map.put("question_text", rs.getString("question_text"));
                map.put("options", rs.getString("options"));
                map.put("correct_answer", rs.getString("correct_answer"));
                map.put("correct_indices", rs.getString("correct_indices"));
                list.add(map);
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error loading all exam questions from DB", e);
        }
        return list;
    }

    public void saveAttendance(String uuid, String name, String idKelas, String idHelper, String subject, String status) {
        String query = "INSERT INTO natural_student_attendance (player_uuid, player_name, id_kelas, id_helper, mata_pelajaran, status_kehadiran) "
                + "VALUES (?, ?, ?, ?, ?, ?);";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setString(1, uuid);
            ps.setString(2, name);
            ps.setString(3, idKelas);
            ps.setString(4, idHelper);
            ps.setString(5, subject);
            ps.setString(6, status);
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error saving attendance for " + name, e);
        }
    }

    public void saveGrade(String uuid, String jenjang, String subject, int score, String examType, String reason) {
        String query;
        if ("MYSQL".equals(storageType)) {
            query = "INSERT INTO natural_academic_grades (player_uuid, jenjang, mata_pelajaran, nilai_angka, tipe_ujian, alasan_nilai) "
                    + "VALUES (?, ?, ?, ?, ?, ?) "
                    + "ON DUPLICATE KEY UPDATE nilai_angka = VALUES(nilai_angka), alasan_nilai = VALUES(alasan_nilai);";
        } else {
            query = "INSERT OR REPLACE INTO natural_academic_grades (player_uuid, jenjang, mata_pelajaran, nilai_angka, tipe_ujian, alasan_nilai) "
                    + "VALUES (?, ?, ?, ?, ?, ?);";
        }
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setString(1, uuid);
            ps.setString(2, jenjang);
            ps.setString(3, subject);
            ps.setInt(4, score);
            ps.setString(5, examType);
            ps.setString(6, reason);
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error saving academic grade for player " + uuid, e);
        }
    }

    public Map<String, Object> loadLatestLessonFile(String jenjang, String subject, String type) {
        String query = "SELECT * FROM natural_lesson_files WHERE jenjang = ? AND mata_pelajaran = ? AND tipe = ? ORDER BY dibuat_pada DESC LIMIT 1;";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setString(1, jenjang);
            ps.setString(2, subject);
            ps.setString(3, type);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Map<String, Object> map = new HashMap<>();
                    map.put("nama_file", rs.getString("nama_file"));
                    map.put("tipe", rs.getString("tipe"));
                    map.put("jenjang", rs.getString("jenjang"));
                    map.put("mata_pelajaran", rs.getString("mata_pelajaran"));
                    map.put("konten_json", rs.getString("konten_json"));
                    map.put("dibuat_oleh", rs.getString("dibuat_oleh"));
                    return map;
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error loading latest lesson file", e);
        }
        return null;
    }

    public Map<String, Object> loadLessonFileByName(String fileName) {
        String query = "SELECT * FROM natural_lesson_files WHERE nama_file = ?;";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setString(1, fileName);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Map<String, Object> map = new HashMap<>();
                    map.put("nama_file", rs.getString("nama_file"));
                    map.put("tipe", rs.getString("tipe"));
                    map.put("jenjang", rs.getString("jenjang"));
                    map.put("mata_pelajaran", rs.getString("mata_pelajaran"));
                    map.put("konten_json", rs.getString("konten_json"));
                    map.put("dibuat_oleh", rs.getString("dibuat_oleh"));
                    return map;
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error loading lesson file " + fileName, e);
        }
        return null;
    }

    public void saveLessonFile(String fileName, String type, String jenjang, String subject, String contentJson, String creatorUuid) {
        String query;
        if ("MYSQL".equals(storageType)) {
            query = "INSERT INTO natural_lesson_files (nama_file, tipe, jenjang, mata_pelajaran, konten_json, dibuat_oleh) "
                    + "VALUES (?, ?, ?, ?, ?, ?) "
                    + "ON DUPLICATE KEY UPDATE konten_json = VALUES(konten_json), dibuat_oleh = VALUES(dibuat_oleh);";
        } else {
            query = "INSERT OR REPLACE INTO natural_lesson_files (nama_file, tipe, jenjang, mata_pelajaran, konten_json, dibuat_oleh) "
                    + "VALUES (?, ?, ?, ?, ?, ?);";
        }
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setString(1, fileName);
            ps.setString(2, type);
            ps.setString(3, jenjang);
            ps.setString(4, subject);
            ps.setString(5, contentJson);
            ps.setString(6, creatorUuid);
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error saving lesson file " + fileName, e);
        }
    }

    public void updateLessonFileUsedTime(String fileName) {
        String query = "UPDATE natural_lesson_files SET dipakai_terakhir = CURRENT_TIMESTAMP WHERE nama_file = ?;";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setString(1, fileName);
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error updating lesson file last used time: " + fileName, e);
        }
    }

    public List<Map<String, String>> getStudentsInClass(int classNumber) {
        List<Map<String, String>> list = new ArrayList<>();
        String query = "SELECT uuid, username FROM nschool_students WHERE academic_class = ? AND nis IS NOT NULL;";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setInt(1, classNumber);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, String> map = new HashMap<>();
                    map.put("uuid", rs.getString("uuid"));
                    map.put("username", rs.getString("username"));
                    list.add(map);
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error loading students in class " + classNumber, e);
        }
        return list;
    }
}
