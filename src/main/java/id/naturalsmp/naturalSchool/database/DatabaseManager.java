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
        String createSubjectsTableSQL;
        String createQuestionsTableSQL;
        String createAttemptsTableSQL;
        String createRaporTableSQL;
        String createCoreStateTableSQL;
        String createAttendanceSQL;
        String createGradesSQL;
        String createRaporSQL;
        String createLessonFilesSQL;
        String createQuestionTypesTableSQL = null;
        String createClassroomsSQL;
        String createClassroomOfficersSQL;
        String createClassroomDoorsSQL;
        
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
            createSubjectsTableSQL = "CREATE TABLE IF NOT EXISTS nschool_subjects ("
                    + "id INT AUTO_INCREMENT PRIMARY KEY, "
                    + "subject_code VARCHAR(50) NOT NULL UNIQUE, "
                    + "subject_name VARCHAR(100) NOT NULL"
                    + ");";
            createQuestionsTableSQL = "CREATE TABLE IF NOT EXISTS nschool_exam_questions ("
                    + "id INT AUTO_INCREMENT PRIMARY KEY, "
                    + "packet_id VARCHAR(50) NOT NULL, "
                    + "academic_class INT NOT NULL, "
                    + "question_number INT NOT NULL, "
                    + "question_type VARCHAR(50) NOT NULL, "
                    + "question_text TEXT NOT NULL, "
                    + "options JSON NULL, "
                    + "correct_answer TEXT NULL, "
                    + "correct_indices JSON NULL, "
                    + "INDEX idx_packet_id (packet_id), "
                    + "INDEX idx_academic_class (academic_class)"
                    + ");";
            createAttemptsTableSQL = "CREATE TABLE IF NOT EXISTS nschool_student_exam_attempts ("
                    + "id INT AUTO_INCREMENT PRIMARY KEY, "
                    + "uuid VARCHAR(36) NOT NULL, "
                    + "nis VARCHAR(20) NOT NULL, "
                    + "packet_id VARCHAR(50) NOT NULL, "
                    + "final_score DOUBLE NOT NULL, "
                    + "completed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, "
                    + "INDEX idx_uuid (uuid), "
                    + "INDEX idx_packet (packet_id)"
                    + ");";
            createRaporTableSQL = "CREATE TABLE IF NOT EXISTS nschool_student_rapor ("
                    + "id INT AUTO_INCREMENT PRIMARY KEY, "
                    + "uuid VARCHAR(36) NOT NULL, "
                    + "nis VARCHAR(20) NOT NULL, "
                    + "academic_class INT NOT NULL, "
                    + "semester VARCHAR(10) NOT NULL, "
                    + "subject_id INT NOT NULL, "
                    + "score_harian DOUBLE NOT NULL DEFAULT 0.0, "
                    + "score_uts DOUBLE NOT NULL DEFAULT 0.0, "
                    + "score_uas DOUBLE NOT NULL DEFAULT 0.0, "
                    + "final_score DOUBLE NOT NULL DEFAULT 0.0, "
                    + "grade_letter VARCHAR(2) NOT NULL, "
                    + "status VARCHAR(20) NOT NULL, "
                    + "UNIQUE KEY uq_rapor (uuid, academic_class, semester, subject_id)"
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
            createQuestionTypesTableSQL = "CREATE TABLE IF NOT EXISTS nschool_question_type ("
                    + "id INT AUTO_INCREMENT PRIMARY KEY, "
                    + "question_code VARCHAR(10) NOT NULL UNIQUE, "
                    + "question_name VARCHAR(100) NOT NULL"
                    + ");";
            createClassroomsSQL = "CREATE TABLE IF NOT EXISTS nschool_classrooms ("
                    + "id_kelas INT PRIMARY KEY, "
                    + "wali_kelas_uuid VARCHAR(36) NULL, "
                    + "world VARCHAR(64) NULL, "
                    + "x1 INT NULL, y1 INT NULL, z1 INT NULL, "
                    + "x2 INT NULL, y2 INT NULL, z2 INT NULL"
                    + ");";
            createClassroomOfficersSQL = "CREATE TABLE IF NOT EXISTS nschool_classroom_officers ("
                    + "player_uuid VARCHAR(36) PRIMARY KEY, "
                    + "id_kelas INT NOT NULL, "
                    + "role VARCHAR(20) NOT NULL, "
                    + "INDEX idx_officer_class (id_kelas)"
                    + ");";
            createClassroomDoorsSQL = "CREATE TABLE IF NOT EXISTS nschool_classroom_doors ("
                    + "id_kelas INT NOT NULL, "
                    + "door_number INT NOT NULL, "
                    + "world VARCHAR(64) NOT NULL, "
                    + "x1 INT NOT NULL, y1 INT NOT NULL, z1 INT NOT NULL, "
                    + "x2 INT NOT NULL, y2 INT NOT NULL, z2 INT NOT NULL, "
                    + "PRIMARY KEY (id_kelas, door_number)"
                    + ");";
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
            createSubjectsTableSQL = "CREATE TABLE IF NOT EXISTS nschool_subjects ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + "subject_code TEXT NOT NULL UNIQUE, "
                    + "subject_name TEXT NOT NULL"
                    + ");";
            createQuestionsTableSQL = "CREATE TABLE IF NOT EXISTS nschool_exam_questions ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + "packet_id TEXT NOT NULL, "
                    + "academic_class INTEGER NOT NULL, "
                    + "question_number INTEGER NOT NULL, "
                    + "question_type TEXT NOT NULL, "
                    + "question_text TEXT NOT NULL, "
                    + "options TEXT NULL, "
                    + "correct_answer TEXT NULL, "
                    + "correct_indices TEXT NULL"
                    + ");";
            createAttemptsTableSQL = "CREATE TABLE IF NOT EXISTS nschool_student_exam_attempts ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + "uuid TEXT NOT NULL, "
                    + "nis TEXT NOT NULL, "
                    + "packet_id TEXT NOT NULL, "
                    + "final_score REAL NOT NULL, "
                    + "completed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP"
                    + ");";
            createRaporTableSQL = "CREATE TABLE IF NOT EXISTS nschool_student_rapor ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + "uuid TEXT NOT NULL, "
                    + "nis TEXT NOT NULL, "
                    + "academic_class INTEGER NOT NULL, "
                    + "semester TEXT NOT NULL, "
                    + "subject_id INTEGER NOT NULL, "
                    + "score_harian REAL NOT NULL DEFAULT 0.0, "
                    + "score_uts REAL NOT NULL DEFAULT 0.0, "
                    + "score_uas REAL NOT NULL DEFAULT 0.0, "
                    + "final_score REAL NOT NULL DEFAULT 0.0, "
                    + "grade_letter TEXT NOT NULL, "
                    + "status TEXT NOT NULL, "
                    + "UNIQUE(uuid, academic_class, semester, subject_id)"
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
            createQuestionTypesTableSQL = "CREATE TABLE IF NOT EXISTS nschool_question_type ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + "question_code TEXT NOT NULL UNIQUE, "
                    + "question_name TEXT NOT NULL"
                    + ");";
            createClassroomsSQL = "CREATE TABLE IF NOT EXISTS nschool_classrooms ("
                    + "id_kelas INTEGER PRIMARY KEY, "
                    + "wali_kelas_uuid TEXT NULL, "
                    + "world TEXT NULL, "
                    + "x1 INTEGER NULL, y1 INTEGER NULL, z1 INTEGER NULL, "
                    + "x2 INTEGER NULL, y2 INTEGER NULL, z2 INTEGER NULL"
                    + ");";
            createClassroomOfficersSQL = "CREATE TABLE IF NOT EXISTS nschool_classroom_officers ("
                    + "player_uuid TEXT PRIMARY KEY, "
                    + "id_kelas INTEGER NOT NULL, "
                    + "role TEXT NOT NULL"
                    + ");";
            createClassroomDoorsSQL = "CREATE TABLE IF NOT EXISTS nschool_classroom_doors ("
                    + "id_kelas INTEGER NOT NULL, "
                    + "door_number INTEGER NOT NULL, "
                    + "world TEXT NOT NULL, "
                    + "x1 INTEGER NOT NULL, y1 INTEGER NOT NULL, z1 INTEGER NOT NULL, "
                    + "x2 INTEGER NOT NULL, y2 INTEGER NOT NULL, z2 INTEGER NOT NULL, "
                    + "PRIMARY KEY (id_kelas, door_number)"
                    + ");";
        }

        try (Connection conn = getConnection()) {
            if (hasColumn(conn, "nschool_exam_questions", "question_id")) {
                plugin.getLogger().info("Old nschool_exam_questions table structure detected. Recreating to match 1.6.4 schema...");
                try (Statement stmt = conn.createStatement()) {
                    stmt.execute("DROP TABLE nschool_exam_questions;");
                }
            }

            try (Statement stmt = conn.createStatement()) {
                stmt.execute(createTableSQL);
                stmt.execute(createLogTableSQL);
                stmt.execute(createSubjectsTableSQL);
                stmt.execute(createQuestionsTableSQL);
                stmt.execute(createAttemptsTableSQL);
                stmt.execute(createRaporTableSQL);
                stmt.execute(createCoreStateTableSQL);
                stmt.execute(createAttendanceSQL);
                stmt.execute(createGradesSQL);
                stmt.execute(createRaporSQL);
                stmt.execute(createLessonFilesSQL);
                stmt.execute(createQuestionTypesTableSQL);
                stmt.execute(createClassroomsSQL);
                stmt.execute(createClassroomOfficersSQL);
                stmt.execute(createClassroomDoorsSQL);
                
                if (!"MYSQL".equals(storageType)) {
                    stmt.execute("CREATE INDEX IF NOT EXISTS idx_questions_packet ON nschool_exam_questions (packet_id);");
                    stmt.execute("CREATE INDEX IF NOT EXISTS idx_questions_cls ON nschool_exam_questions (academic_class);");
                    stmt.execute("CREATE INDEX IF NOT EXISTS idx_attempts_uuid ON nschool_student_exam_attempts (uuid);");
                    stmt.execute("CREATE INDEX IF NOT EXISTS idx_attempts_packet ON nschool_student_exam_attempts (packet_id);");
                    stmt.execute("CREATE INDEX IF NOT EXISTS idx_attendance_player ON natural_student_attendance (player_uuid);");
                    stmt.execute("CREATE INDEX IF NOT EXISTS idx_attendance_status ON natural_student_attendance (status_kehadiran);");
                    stmt.execute("CREATE INDEX IF NOT EXISTS idx_grades_player ON natural_academic_grades (player_uuid);");
                    stmt.execute("CREATE INDEX IF NOT EXISTS idx_rapor_player ON natural_e_rapor_digital (player_uuid);");
                    stmt.execute("CREATE INDEX IF NOT EXISTS idx_lesson_files_jenjang ON natural_lesson_files (jenjang, mata_pelajaran);");
                    stmt.execute("CREATE INDEX IF NOT EXISTS idx_lesson_files_tipe ON natural_lesson_files (tipe);");
                    stmt.execute("CREATE INDEX IF NOT EXISTS idx_officer_class ON nschool_classroom_officers (id_kelas);");
                }

                // Seed default 7 subjects
                if ("MYSQL".equals(storageType)) {
                    String seedSubjectsMySQL = "INSERT INTO nschool_subjects (id, subject_code, subject_name) VALUES "
                            + "(1, 'PENGETAHUAN_UMUM', 'Pengetahuan Umum'), "
                            + "(2, 'PENDIDIKAN_PANCASILA', 'Pendidikan Pancasila'), "
                            + "(3, 'BAHASA_INDONESIA', 'Bahasa Indonesia'), "
                            + "(4, 'BAHASA_INGGRIS', 'Bahasa Inggris'), "
                            + "(5, 'MATEMATIKA', 'Matematika'), "
                            + "(6, 'ILMU_PENGETAHUAN_ALAM', 'Ilmu Pengetahuan Alam'), "
                            + "(7, 'ILMU_PENGETAHUAN_SOSIAL', 'Ilmu Pengetahuan Sosial') "
                            + "ON DUPLICATE KEY UPDATE subject_code = VALUES(subject_code);";
                    stmt.execute(seedSubjectsMySQL);
                } else {
                    String seedSubjectsSQLite = "INSERT OR IGNORE INTO nschool_subjects (id, subject_code, subject_name) VALUES "
                            + "(1, 'PENGETAHUAN_UMUM', 'Pengetahuan Umum'), "
                            + "(2, 'PENDIDIKAN_PANCASILA', 'Pendidikan Pancasila'), "
                            + "(3, 'BAHASA_INDONESIA', 'Bahasa Indonesia'), "
                            + "(4, 'BAHASA_INGGRIS', 'Bahasa Inggris'), "
                            + "(5, 'MATEMATIKA', 'Matematika'), "
                            + "(6, 'ILMU_PENGETAHUAN_ALAM', 'Ilmu Pengetahuan Alam'), "
                            + "(7, 'ILMU_PENGETAHUAN_SOSIAL', 'Ilmu Pengetahuan Sosial');";
                    stmt.execute(seedSubjectsSQLite);
                }

                // Seed default 3 question types
                if ("MYSQL".equals(storageType)) {
                    String seedQuestionTypesMySQL = "INSERT INTO nschool_question_type (id, question_code, question_name) VALUES "
                            + "(1, 'PG', 'Pilihan Ganda'), "
                            + "(2, 'BS', 'Benar Salah'), "
                            + "(3, 'PGK', 'Pilihan Ganda Kompleks') "
                            + "ON DUPLICATE KEY UPDATE question_code = VALUES(question_code), question_name = VALUES(question_name);";
                    stmt.execute(seedQuestionTypesMySQL);
                } else {
                    String seedQuestionTypesSQLite = "INSERT OR IGNORE INTO nschool_question_type (id, question_code, question_name) VALUES "
                            + "(1, 'PG', 'Pilihan Ganda'), "
                            + "(2, 'BS', 'Benar Salah'), "
                            + "(3, 'PGK', 'Pilihan Ganda Kompleks');";
                    stmt.execute(seedQuestionTypesSQLite);
                }

                // Seeding default values for nschool_core_state
                if ("MYSQL".equals(storageType)) {
                    stmt.execute("INSERT INTO nschool_core_state (state_key, state_value, description) VALUES "
                            + "('exam_version', '1', 'Exam questions version') ON DUPLICATE KEY UPDATE state_key = state_key;");
                    stmt.execute("INSERT INTO nschool_core_state (state_key, state_value, description) VALUES "
                            + "('portal_semester_status', 'CLOSED', 'Semester exam status (OPEN/CLOSED)') ON DUPLICATE KEY UPDATE state_key = state_key;");
                    stmt.execute("INSERT INTO nschool_core_state (state_key, state_value, description) VALUES "
                            + "('portal_message', 'Portal Ujian Sedang ditutup!', 'Message displayed when the portal is closed') ON DUPLICATE KEY UPDATE state_key = state_key;");
                    stmt.execute("INSERT INTO nschool_core_state (state_key, state_value, description) VALUES "
                            + "('active_uh_packets', '[]', 'JSON list of active UH packet IDs') ON DUPLICATE KEY UPDATE state_key = state_key;");
                    stmt.execute("INSERT INTO nschool_core_state (state_key, state_value, description) VALUES "
                            + "('current_active_semester_packets', '[]', 'JSON list of active semester packet IDs') ON DUPLICATE KEY UPDATE state_key = state_key;");
                    stmt.execute("INSERT INTO nschool_core_state (state_key, state_value, description) VALUES "
                            + "('is_semester_break', 'false', 'Status of semester break') ON DUPLICATE KEY UPDATE state_key = state_key;");
                } else {
                    stmt.execute("INSERT OR IGNORE INTO nschool_core_state (state_key, state_value, description) VALUES "
                            + "('exam_version', '1', 'Exam questions version');");
                    stmt.execute("INSERT OR IGNORE INTO nschool_core_state (state_key, state_value, description) VALUES "
                            + "('portal_semester_status', 'CLOSED', 'Semester exam status (OPEN/CLOSED)');");
                    stmt.execute("INSERT OR IGNORE INTO nschool_core_state (state_key, state_value, description) VALUES "
                            + "('portal_message', 'Portal Ujian Sedang ditutup!');");
                    stmt.execute("INSERT OR IGNORE INTO nschool_core_state (state_key, state_value, description) VALUES "
                            + "('active_uh_packets', '[]');");
                    stmt.execute("INSERT OR IGNORE INTO nschool_core_state (state_key, state_value, description) VALUES "
                            + "('current_active_semester_packets', '[]');");
                    stmt.execute("INSERT OR IGNORE INTO nschool_core_state (state_key, state_value, description) VALUES "
                            + "('is_semester_break', 'false');");
                }

                plugin.getLogger().info("Database tables verified/created successfully.");
            }
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

            if (!hasIndexOrConstraint(conn, "nschool_student_exam_attempts", "uq_student_packet")) {
                plugin.getLogger().info("Migrating database: Adding unique constraint/index uq_student_packet to nschool_student_exam_attempts...");
                String alterSQL;
                if ("MYSQL".equals(storageType)) {
                    alterSQL = "ALTER TABLE nschool_student_exam_attempts ADD CONSTRAINT uq_student_packet UNIQUE (uuid, packet_id);";
                } else {
                    alterSQL = "CREATE UNIQUE INDEX uq_student_packet ON nschool_student_exam_attempts (uuid, packet_id);";
                }
                try (Statement stmt = conn.createStatement()) {
                    stmt.execute(alterSQL);
                    plugin.getLogger().info("Successfully added uq_student_packet unique constraint/index.");
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to perform database migration!", e);
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

    private boolean hasIndexOrConstraint(Connection conn, String tableName, String indexName) {
        try {
            java.sql.DatabaseMetaData dbm = conn.getMetaData();
            try (ResultSet rs = dbm.getIndexInfo(null, null, tableName, false, false)) {
                while (rs.next()) {
                    String name = rs.getString("INDEX_NAME");
                    if (indexName.equalsIgnoreCase(name)) {
                        return true;
                    }
                }
            }
            try (ResultSet rs = dbm.getIndexInfo(null, null, tableName.toUpperCase(), false, false)) {
                while (rs.next()) {
                    String name = rs.getString("INDEX_NAME");
                    if (indexName.equalsIgnoreCase(name)) {
                        return true;
                    }
                }
            }
            try (ResultSet rs = dbm.getIndexInfo(null, null, tableName.toLowerCase(), false, false)) {
                while (rs.next()) {
                    String name = rs.getString("INDEX_NAME");
                    if (indexName.equalsIgnoreCase(name)) {
                        return true;
                    }
                }
            }
        } catch (SQLException e) {
            // Fallback
        }
        
        if (!"MYSQL".equals(storageType)) {
            String query = "SELECT name FROM sqlite_master WHERE type='index' AND name=?;";
            try (PreparedStatement ps = conn.prepareStatement(query)) {
                ps.setString(1, indexName);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return true;
                    }
                }
            } catch (SQLException e) {
                // Ignore
            }
        }
        return false;
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

    public List<String> getAttemptedPackets(UUID uuid) {
        List<String> list = new ArrayList<>();
        String query = "SELECT packet_id FROM nschool_student_exam_attempts WHERE uuid = ?;";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(rs.getString("packet_id"));
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to load attempts for " + uuid, e);
        }
        return list;
    }

    public boolean hasAttemptedExam(UUID uuid, String packetId) {
        String query = "SELECT 1 FROM nschool_student_exam_attempts WHERE uuid = ? AND packet_id = ? LIMIT 1;";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setString(1, uuid.toString());
            ps.setString(2, packetId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error checking exam attempt for " + uuid + " and " + packetId, e);
            return false;
        }
    }

    public void saveExamAttempt(UUID uuid, String nis, String packetId, double finalScore) {
        String query;
        if ("MYSQL".equals(storageType)) {
            query = "INSERT INTO nschool_student_exam_attempts (uuid, nis, packet_id, final_score, completed_at) VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP);";
        } else {
            query = "INSERT INTO nschool_student_exam_attempts (uuid, nis, packet_id, final_score) VALUES (?, ?, ?, ?);";
        }
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setString(1, uuid.toString());
            ps.setString(2, nis != null ? nis : "");
            ps.setString(3, packetId);
            ps.setDouble(4, finalScore);
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save exam attempt for " + uuid, e);
            throw new RuntimeException(e);
        }
    }

    public double getAverageUhScore(UUID uuid, int subjectId, int academicClass) {
        String packetPrefix = subjectId + "_" + academicClass + "_UH%";
        String query = "SELECT AVG(final_score) FROM nschool_student_exam_attempts WHERE uuid = ? AND packet_id LIKE ?;";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setString(1, uuid.toString());
            ps.setString(2, packetPrefix);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getDouble(1);
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to calculate average UH score for " + uuid, e);
            throw new RuntimeException("Database error calculating average score", e);
        }
        return 0.0;
    }

    public synchronized void upsertStudentRapor(UUID uuid, String nis, int academicClass, String semester, int subjectId, String examType, double score) {
        String selectQuery = "SELECT score_harian, score_uts, score_uas FROM nschool_student_rapor WHERE uuid = ? AND academic_class = ? AND semester = ? AND subject_id = ?;";
        double scoreHarian = 0.0;
        double scoreUts = 0.0;
        double scoreUas = 0.0;

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(selectQuery)) {
            ps.setString(1, uuid.toString());
            ps.setInt(2, academicClass);
            ps.setString(3, semester);
            ps.setInt(4, subjectId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    scoreHarian = rs.getDouble("score_harian");
                    scoreUts = rs.getDouble("score_uts");
                    scoreUas = rs.getDouble("score_uas");
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to query existing student rapor for " + uuid, e);
            throw new RuntimeException("Database error querying student rapor", e);
        }

        if (examType.toUpperCase().contains("UH")) {
            scoreHarian = getAverageUhScore(uuid, subjectId, academicClass);
        } else if (examType.equalsIgnoreCase("UTS")) {
            scoreUts = score;
        } else if (examType.equalsIgnoreCase("UAS")) {
            scoreUas = score;
        }

        double finalScore = (scoreHarian * 0.4) + (scoreUts * 0.3) + (scoreUas * 0.3);

        String gradeLetter;
        String status;
        if (finalScore >= 85) {
            gradeLetter = "A";
            status = "LULUS";
        } else if (finalScore >= 70) {
            gradeLetter = "B";
            status = "LULUS";
        } else if (finalScore >= 55) {
            gradeLetter = "C";
            status = "LULUS";
        } else {
            gradeLetter = "D";
            status = "REMEDI";
        }

        String query;
        if ("MYSQL".equals(storageType)) {
            query = "INSERT INTO nschool_student_rapor (uuid, nis, academic_class, semester, subject_id, score_harian, score_uts, score_uas, final_score, grade_letter, status) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) "
                    + "ON DUPLICATE KEY UPDATE score_harian = VALUES(score_harian), score_uts = VALUES(score_uts), score_uas = VALUES(score_uas), final_score = VALUES(final_score), grade_letter = VALUES(grade_letter), status = VALUES(status);";
        } else {
            query = "INSERT INTO nschool_student_rapor (uuid, nis, academic_class, semester, subject_id, score_harian, score_uts, score_uas, final_score, grade_letter, status) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) "
                    + "ON CONFLICT(uuid, academic_class, semester, subject_id) DO UPDATE SET score_harian = excluded.score_harian, score_uts = excluded.score_uts, score_uas = excluded.score_uas, final_score = excluded.final_score, grade_letter = excluded.grade_letter, status = excluded.status;";
        }

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setString(1, uuid.toString());
            ps.setString(2, nis != null ? nis : "");
            ps.setInt(3, academicClass);
            ps.setString(4, semester);
            ps.setInt(5, subjectId);
            ps.setDouble(6, scoreHarian);
            ps.setDouble(7, scoreUts);
            ps.setDouble(8, scoreUas);
            ps.setDouble(9, finalScore);
            ps.setString(10, gradeLetter);
            ps.setString(11, status);
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to upsert student rapor for " + uuid, e);
            throw new RuntimeException("Database error upserting student rapor", e);
        }
    }

    public List<Map<String, Object>> getAllExamQuestions() {
        List<Map<String, Object>> list = new ArrayList<>();
        String query = "SELECT * FROM nschool_exam_questions ORDER BY question_number ASC;";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(query);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Map<String, Object> map = new HashMap<>();
                map.put("packet_id", rs.getString("packet_id"));
                map.put("academic_class", rs.getInt("academic_class"));
                map.put("question_number", rs.getInt("question_number"));
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

    // Classroom Database Operations
    public void saveClassroom(int idKelas, String waliKelasUuid, String world, Integer x1, Integer y1, Integer z1, Integer x2, Integer y2, Integer z2) {
        String query;
        if ("MYSQL".equals(storageType)) {
            query = "INSERT INTO nschool_classrooms (id_kelas, wali_kelas_uuid, world, x1, y1, z1, x2, y2, z2) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?) "
                    + "ON DUPLICATE KEY UPDATE wali_kelas_uuid = VALUES(wali_kelas_uuid), world = VALUES(world), "
                    + "x1 = VALUES(x1), y1 = VALUES(y1), z1 = VALUES(z1), x2 = VALUES(x2), y2 = VALUES(y2), z2 = VALUES(z2);";
        } else {
            query = "INSERT OR REPLACE INTO nschool_classrooms (id_kelas, wali_kelas_uuid, world, x1, y1, z1, x2, y2, z2) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?);";
        }

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setInt(1, idKelas);
            if (waliKelasUuid == null) ps.setNull(2, java.sql.Types.VARCHAR); else ps.setString(2, waliKelasUuid);
            if (world == null) ps.setNull(3, java.sql.Types.VARCHAR); else ps.setString(3, world);
            if (x1 == null) ps.setNull(4, java.sql.Types.INTEGER); else ps.setInt(4, x1);
            if (y1 == null) ps.setNull(5, java.sql.Types.INTEGER); else ps.setInt(5, y1);
            if (z1 == null) ps.setNull(6, java.sql.Types.INTEGER); else ps.setInt(6, z1);
            if (x2 == null) ps.setNull(7, java.sql.Types.INTEGER); else ps.setInt(7, x2);
            if (y2 == null) ps.setNull(8, java.sql.Types.INTEGER); else ps.setInt(8, y2);
            if (z2 == null) ps.setNull(9, java.sql.Types.INTEGER); else ps.setInt(9, z2);
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error saving classroom " + idKelas, e);
        }
    }

    public void updateClassroomWaliKelas(int idKelas, String waliKelasUuid) {
        String query = "UPDATE nschool_classrooms SET wali_kelas_uuid = ? WHERE id_kelas = ?;";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(query)) {
            if (waliKelasUuid == null) ps.setNull(1, java.sql.Types.VARCHAR); else ps.setString(1, waliKelasUuid);
            ps.setInt(2, idKelas);
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error updating classroom wali kelas for class " + idKelas, e);
        }
    }

    public Map<String, Object> getClassroom(int idKelas) {
        String query = "SELECT * FROM nschool_classrooms WHERE id_kelas = ?;";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setInt(1, idKelas);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id_kelas", rs.getInt("id_kelas"));
                    map.put("wali_kelas_uuid", rs.getString("wali_kelas_uuid"));
                    map.put("world", rs.getString("world"));
                    map.put("x1", rs.getObject("x1") != null ? rs.getInt("x1") : null);
                    map.put("y1", rs.getObject("y1") != null ? rs.getInt("y1") : null);
                    map.put("z1", rs.getObject("z1") != null ? rs.getInt("z1") : null);
                    map.put("x2", rs.getObject("x2") != null ? rs.getInt("x2") : null);
                    map.put("y2", rs.getObject("y2") != null ? rs.getInt("y2") : null);
                    map.put("z2", rs.getObject("z2") != null ? rs.getInt("z2") : null);
                    return map;
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error loading classroom " + idKelas, e);
        }
        return null;
    }

    public List<Map<String, Object>> getAllClassrooms() {
        List<Map<String, Object>> list = new ArrayList<>();
        String query = "SELECT * FROM nschool_classrooms;";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(query);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Map<String, Object> map = new HashMap<>();
                map.put("id_kelas", rs.getInt("id_kelas"));
                map.put("wali_kelas_uuid", rs.getString("wali_kelas_uuid"));
                map.put("world", rs.getString("world"));
                map.put("x1", rs.getObject("x1") != null ? rs.getInt("x1") : null);
                map.put("y1", rs.getObject("y1") != null ? rs.getInt("y1") : null);
                map.put("z1", rs.getObject("z1") != null ? rs.getInt("z1") : null);
                map.put("x2", rs.getObject("x2") != null ? rs.getInt("x2") : null);
                map.put("y2", rs.getObject("y2") != null ? rs.getInt("y2") : null);
                map.put("z2", rs.getObject("z2") != null ? rs.getInt("z2") : null);
                list.add(map);
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error loading all classrooms", e);
        }
        return list;
    }

    // Classroom Officers Database Operations
    public void saveClassroomOfficer(String playerUuid, int idKelas, String role) {
        String query;
        if ("MYSQL".equals(storageType)) {
            query = "INSERT INTO nschool_classroom_officers (player_uuid, id_kelas, role) VALUES (?, ?, ?) "
                    + "ON DUPLICATE KEY UPDATE id_kelas = VALUES(id_kelas), role = VALUES(role);";
        } else {
            query = "INSERT OR REPLACE INTO nschool_classroom_officers (player_uuid, id_kelas, role) VALUES (?, ?, ?);";
        }

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setString(1, playerUuid);
            ps.setInt(2, idKelas);
            ps.setString(3, role);
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error saving classroom officer for player " + playerUuid, e);
        }
    }

    public void removeClassroomOfficer(String playerUuid) {
        String query = "DELETE FROM nschool_classroom_officers WHERE player_uuid = ?;";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setString(1, playerUuid);
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error removing classroom officer " + playerUuid, e);
        }
    }

    public void clearClassroomOfficers(int idKelas) {
        String query = "DELETE FROM nschool_classroom_officers WHERE id_kelas = ?;";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setInt(1, idKelas);
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error clearing classroom officers for class " + idKelas, e);
        }
    }

    public List<Map<String, String>> getClassroomOfficers(int idKelas) {
        List<Map<String, String>> list = new ArrayList<>();
        String query = "SELECT player_uuid, role FROM nschool_classroom_officers WHERE id_kelas = ?;";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setInt(1, idKelas);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, String> map = new HashMap<>();
                    map.put("player_uuid", rs.getString("player_uuid"));
                    map.put("role", rs.getString("role"));
                    list.add(map);
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error loading classroom officers for class " + idKelas, e);
        }
        return list;
    }

    public Map<String, String> getOfficerRoleAndClass(String playerUuid) {
        String query = "SELECT id_kelas, role FROM nschool_classroom_officers WHERE player_uuid = ?;";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setString(1, playerUuid);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Map<String, String> map = new HashMap<>();
                    map.put("id_kelas", String.valueOf(rs.getInt("id_kelas")));
                    map.put("role", rs.getString("role"));
                    return map;
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error loading officer role and class for player " + playerUuid, e);
        }
        return null;
    }

    // Classroom Doors Database Operations
    public void saveClassroomDoor(int idKelas, int doorNumber, String world, int x1, int y1, int z1, int x2, int y2, int z2) {
        String query;
        if ("MYSQL".equals(storageType)) {
            query = "INSERT INTO nschool_classroom_doors (id_kelas, door_number, world, x1, y1, z1, x2, y2, z2) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?) "
                    + "ON DUPLICATE KEY UPDATE world = VALUES(world), x1 = VALUES(x1), y1 = VALUES(y1), z1 = VALUES(z1), "
                    + "x2 = VALUES(x2), y2 = VALUES(y2), z2 = VALUES(z2);";
        } else {
            query = "INSERT OR REPLACE INTO nschool_classroom_doors (id_kelas, door_number, world, x1, y1, z1, x2, y2, z2) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?);";
        }

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setInt(1, idKelas);
            ps.setInt(2, doorNumber);
            ps.setString(3, world);
            ps.setInt(4, x1);
            ps.setInt(5, y1);
            ps.setInt(6, z1);
            ps.setInt(7, x2);
            ps.setInt(8, y2);
            ps.setInt(9, z2);
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error saving classroom door for class " + idKelas + " number " + doorNumber, e);
        }
    }

    public void deleteClassroomDoor(int idKelas, int doorNumber) {
        String query = "DELETE FROM nschool_classroom_doors WHERE id_kelas = ? AND door_number = ?;";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setInt(1, idKelas);
            ps.setInt(2, doorNumber);
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error deleting classroom door for class " + idKelas + " number " + doorNumber, e);
        }
    }

    public void clearClassroomDoors(int idKelas) {
        String query = "DELETE FROM nschool_classroom_doors WHERE id_kelas = ?;";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setInt(1, idKelas);
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error clearing classroom doors for class " + idKelas, e);
        }
    }

    public List<Map<String, Object>> getClassroomDoors(int idKelas) {
        List<Map<String, Object>> list = new ArrayList<>();
        String query = "SELECT * FROM nschool_classroom_doors WHERE id_kelas = ?;";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setInt(1, idKelas);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id_kelas", rs.getInt("id_kelas"));
                    map.put("door_number", rs.getInt("door_number"));
                    map.put("world", rs.getString("world"));
                    map.put("x1", rs.getInt("x1"));
                    map.put("y1", rs.getInt("y1"));
                    map.put("z1", rs.getInt("z1"));
                    map.put("x2", rs.getInt("x2"));
                    map.put("y2", rs.getInt("y2"));
                    map.put("z2", rs.getInt("z2"));
                    list.add(map);
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error loading classroom doors for class " + idKelas, e);
        }
        return list;
    }
}
