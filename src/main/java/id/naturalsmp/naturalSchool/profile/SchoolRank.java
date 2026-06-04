package id.naturalsmp.naturalSchool.profile;

public enum SchoolRank {
    // A. Server Management
    KETUA_YAYASAN("KETUA_YAYASAN", "<red><bold>Ketua Yayasan</bold></red>", 100, RankType.MANAGEMENT),
    WAKIL_KETUA_YAYASAN("WAKIL_KETUA_YAYASAN", "<red>Dewan Pembina</red>", 95, RankType.MANAGEMENT),
    KEMENTERIAN_PENDIDIKAN_IT("KEMENTERIAN_PENDIDIKAN_IT", "<gold>Kemendikbud & IT</gold>", 90, RankType.MANAGEMENT),
    PENGAWAS_SEKOLAH("PENGAWAS_SEKOLAH", "<yellow>Pengawas Sekolah</yellow>", 85, RankType.MANAGEMENT),

    // B. Staf Admin
    KEPALA_SEKOLAH("KEPALA_SEKOLAH", "<dark_green><bold>Kepala Sekolah</bold></dark_green>", 80, RankType.STAFF),
    WAKEPSEK_KURIKULUM("WAKEPSEK_KURIKULUM", "<green>Wakepsek Kurikulum</green>", 75, RankType.STAFF),
    WAKEPSEK_SARPRAS("WAKEPSEK_SARPRAS", "<green>Wakepsek Sarpras</green>", 70, RankType.STAFF),
    KOMISI_DISIPLIN("KOMISI_DISIPLIN", "<dark_red>Komisi Disiplin</dark_red>", 65, RankType.STAFF),

    // C. Staf Helper
    KEPALA_TU("KEPALA_TU", "<blue>Kepala TU</blue>", 60, RankType.HELPER),
    GURU_TETAP("GURU_TETAP", "<aqua>Wali Kelas</aqua>", 55, RankType.HELPER),
    GURU_BK("GURU_BK", "<aqua>Guru BK</aqua>", 50, RankType.HELPER),
    GURU_HONORER("GURU_HONORER", "<gray>Guru Honorer</gray>", 45, RankType.HELPER),

    // D. Akademik Siswa
    SMA_12("SMA_12", "<dark_purple>Siswa XII SMA</dark_purple>", 32, RankType.STUDENT),
    SMA_11("SMA_11", "<dark_purple>Siswa XI SMA</dark_purple>", 31, RankType.STUDENT),
    SMA_10("SMA_10", "<dark_purple>Siswa X SMA</dark_purple>", 30, RankType.STUDENT),
    SMP_9("SMP_9", "<blue>Siswa IX SMP</blue>", 23, RankType.STUDENT),
    SMP_8("SMP_8", "<blue>Siswa VIII SMP</blue>", 22, RankType.STUDENT),
    SMP_7("SMP_7", "<blue>Siswa VII SMP</blue>", 21, RankType.STUDENT),
    SD_6("SD_6", "<green>Siswa VI SD</green>", 16, RankType.STUDENT),
    SD_5("SD_5", "<green>Siswa V SD</green>", 15, RankType.STUDENT),
    SD_4("SD_4", "<green>Siswa IV SD</green>", 14, RankType.STUDENT),
    SD_3("SD_3", "<green>Siswa III SD</green>", 13, RankType.STUDENT),
    SD_2("SD_2", "<green>Siswa II SD</green>", 12, RankType.STUDENT),
    SD_1("SD_1", "<green>Siswa I SD</green>", 11, RankType.STUDENT),

    // E. Default State
    NONE("NONE", "<gray>Belum Terdaftar</gray>", 0, RankType.NONE);

    private final String id;
    private final String displayName;
    private final int priority;
    private final RankType type;

    SchoolRank(String id, String displayName, int priority, RankType type) {
        this.id = id;
        this.displayName = displayName;
        this.priority = priority;
        this.type = type;
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getPriority() {
        return priority;
    }

    public RankType getType() {
        return type;
    }

    public enum RankType {
        MANAGEMENT, STAFF, HELPER, STUDENT, NONE
    }
}
