# 🏫 Dokumentasi Lengkap NaturalSchool

NaturalSchool adalah *enterprise-grade*, *high-performance core plugin* yang dirancang untuk **Paper/Spigot (Minecraft 1.21.10 / Java 21)**. Plugin ini mengadopsi pola arsitektur **Core-Infrastructure** untuk memisahkan konfigurasi, manajemen data, *onboarding* dinamis, dan basis data akademik siswa ke dalam antarmuka yang modular dan *thread-safe*.

Plugin ini mengelola siklus akademik siswa di NaturalSMP, mulai dari pendaftaran NIS dinamis, presensi harian otomatis berbasis wilayah (WorldGuard), kuis kelas, hingga pelaksanaan ujian semester lintas platform (Java & Bedrock) dan kompilasi E-Rapor otomatis.

---

## 🚀 1. Pilar Arsitektur & Teknologi

### 1.1 Database Pool & Caching Tingkat Tinggi
NaturalSchool mendukung dua sistem penyimpanan data yang dioptimalkan untuk performa tinggi di bawah beban server yang tinggi:
* **HikariCP MySQL Connection Pool**: Digunakan untuk lingkungan produksi terpusat. Mengelola batas *timeout* koneksi (`connection-timeout`), ukuran kolam maksimal (`maximum-pool-size`), penembolokan prepared statement (`cachePrepStmts`), batas ukuran cache statement (`prepStmtCacheSize`), dan batas diagnostik kebocoran koneksi.
* **Optimized Local SQLite Engine**: Sebagai cadangan lokal dengan berkas database `school.db`. Mengaplikasikan PRAGMA SQLite khusus berikut untuk menyelesaikan masalah penguncian database (`SQLITE_BUSY`) selama penulisan intensif:
  * `PRAGMA busy_timeout = 5000;` (ambang batas tunggu kunci database 5 detik)
  * `PRAGMA journal_mode = WAL;` (Write-Ahead Logging untuk konkurensi tinggi)
  * `PRAGMA synchronous = NORMAL;` (sinkronisasi normal untuk meningkatkan performa penulisan)
* **Thread-Safe Memory Caching**: Data akademik siswa disimpan dalam cache memori menggunakan `ConcurrentHashMap<UUID, StudentProfile>`. Sistem ini dirancang untuk **tidak** menimbun objek `org.bukkit.entity.Player` secara langsung demi menghindari terjadinya kebocoran memori (*memory leaks*).
* **Asynchronous Database Processing**: Seluruh kueri baca dan tulis basis data dieksekusi secara asinkron (menggunakan `BukkitScheduler` dan `CompletableFuture`) di luar thread utama Minecraft untuk mempertahankan TPS tetap di angka 20.

### 1.2 Unified Cross-Platform UI Engine
Sistem antarmuka pengguna dirancang secara cerdas agar kompatibel dengan pemain dari edisi Minecraft yang berbeda:
* **Java Edition Clients**: Menerima komponen interaktif kaya warna menggunakan Paper **Dialog API** yang diformat dengan Kyori Adventure `MiniMessage`.
* **Bedrock Edition Clients**: Secara otomatis diarahkan menggunakan Geyser/Floodgate menuju **Cumulus Forms** (SimpleForm dan CustomForm) untuk memberikan pengalaman interaktif yang responsif.
* **Bedrock Button Form Sanitization**: Seluruh teks tombol pilihan dan deskripsi dibersihkan secara dinamis dari kode format warna bawaan (`§`) dan simbol dekoratif seperti (`○`, `●`) guna menghindari kegagalan render (*blank-rendering*) dan ketidakseragaman fon pada klien Bedrock.

```
                  +-----------------------------------+
                  |        UIManager Router           |
                  +-----------------------------------+
                                    |
                    (Deteksi Edisi Klien Pemain)
                                    |
            +-----------------------+-----------------------+
            |                                               |
            v                                               v
    [Java Edition]                                  [Bedrock Edition]
   Paper Dialog API                              Cumulus (Geyser/Floodgate)
- Text Body (MiniMessage)                       - Teks Disterilkan (No §, ○, ●)
- Select Single Option Input                    - Input Toggle / Dropdown
- Multi/Confirmation Action                     - SimpleForm / CustomForm
```

### 1.3 Sistem Scheduler WIB Harian
NaturalSchool menjalankan scheduler berkala setiap 60 detik (1200 ticks) yang disinkronkan langsung dengan waktu **WIB (Asia/Jakarta / GMT+7)** untuk mengotomatisasi siklus akademik harian:
* **18:00 WIB (Pintu Sesi Dibuka)**: Sesi kelas untuk seluruh jenjang (kelas 1-12) dimulai secara otomatis dengan mata pelajaran default "Pelajaran Umum". Sistem WorldGuard secara otomatis mengunci pintu masuk kelas fisik (mengubah flag `ENTRY` region kelas menjadi `DENY`). Hanya murid dengan jenjang kelas terkait yang dapat masuk.
* **18:15 WIB (Pemeriksaan Auto Fallback)**: Jika Helper/Guru belum memuat berkas pembelajaran atau kuis pada jam tersebut, sistem akan menjalankan mekanisme Auto-Fallback (dijelaskan di Bagian 3.2).
* **20:00 WIB (Batas Keterlambatan)**: Batas toleransi presensi dikunci. Murid yang baru memasuki kelas atau baru login setelah jam ini akan dicatat dengan status presensi `TERLAMBAT`.
* **21:00 WIB (Sesi Kelas Berakhir)**: Sesi kelas ditutup secara global. Seluruh pintu masuk kelas dibuka kembali (flag `ENTRY` dikembalikan ke default). Presensi direkap secara asinkron dan dikirimkan ke database serta log Discord.

---

## 🎖️ 2. School Ranks Hierarchy (`SchoolRank`)

Tingkatan jabatan dalam sekolah diatur dalam sistem enum `SchoolRank` yang menampung informasi ID, nama format MiniMessage, prioritas, dan tipe kelompok (`RankType`). Urutan prioritas pangkat menentukan hak izin perintah utilitas serta memandu batasan akademik siswa:

| Kategori Jabatan | ID Enum / Ranks | Display Name | Prioritas | Tipe Kelompok (`RankType`) |
| :--- | :--- | :--- | :--- | :--- |
| **Server Management** | `KETUA_YAYASAN` | `Ketua Yayasan` | 100 | `MANAGEMENT` |
| | `WAKIL_KETUA_YAYASAN` | `Dewan Pembina` | 95 | `MANAGEMENT` |
| | `KEMENTERIAN_PENDIDIKAN_IT`| `Kemendikbud & IT`| 90 | `MANAGEMENT` |
| | `PENGAWAS_SEKOLAH` | `Pengawas Sekolah` | 85 | `MANAGEMENT` |
| **Administrative Staff** | `KEPALA_SEKOLAH` | `Kepala Sekolah` | 80 | `STAFF` |
| | `WAKEPSEK_KURIKULUM` | `Wakepsek Kurikulum`| 75 | `STAFF` |
| | `WAKEPSEK_SARPRAS` | `Wakepsek Sarpras` | 70 | `STAFF` |
| | `KOMISI_DISIPLIN` | `Komisi Disiplin` | 65 | `STAFF` |
| **Helper Staff** | `KEPALA_TU` | `Kepala TU` | 60 | `HELPER` |
| | `GURU_TETAP` | `Wali Kelas` | 55 | `HELPER` |
| | `GURU_BK` | `Guru BK` | 50 | `HELPER` |
| | `GURU_HONORER` | `Guru Honorer` | 45 | `HELPER` |
| **Siswa (SMA)** | `SMA_12` | `Siswa XII SMA` | 32 | `STUDENT` |
| | `SMA_11` | `Siswa XI SMA` | 31 | `STUDENT` |
| | `SMA_10` | `Siswa X SMA` | 30 | `STUDENT` |
| **Siswa (SMP)** | `SMP_9` | `Siswa IX SMP` | 23 | `STUDENT` |
| | `SMP_8` | `Siswa VIII SMP` | 22 | `STUDENT` |
| | `SMP_7` | `Siswa VII SMP` | 21 | `STUDENT` |
| **Siswa (SD)** | `SD_6` | `Siswa VI SD` | 16 | `STUDENT` |
| | `SD_5` | `Siswa V SD` | 15 | `STUDENT` |
| | `SD_4` | `Siswa IV SD` | 14 | `STUDENT` |
| | `SD_3` | `Siswa III SD` | 13 | `STUDENT` |
| | `SD_2` | `Siswa II SD` | 12 | `STUDENT` |
| | `SD_1` | `Siswa I SD` | 11 | `STUDENT` |
| **Default** | `NONE` | `Belum Terdaftar` | 0 | `NONE` |

---

## 🏫 3. Sistem Manajemen & Organisasi Kelas (`/class` / `/kelas`)

Modul Organisasi Kelas menyediakan infrastruktur terintegrasi untuk mengelola struktur kepengurusan kelas, batas wilayah spasial kelas, log transisi visual saat melintasi kelas, obrolan kelas terformat, pintu blok otomatis berbasis tinted glass, serta panel visual manajemen kelas.

### 3.1 Skema Database Baru (Clean DB)
Sistem menyimpan struktur organisasi dan batasan area kelas dalam 3 tabel baru di database SQLite (`school.db`) dan MySQL:
1. **`nschool_classrooms`**: Menyimpan data dasar ruang kelas (1 s.d 12), guru Wali Kelas, dan koordinat area kubus kelas.
   ```sql
   CREATE TABLE IF NOT EXISTS nschool_classrooms (
       id_kelas INT PRIMARY KEY,
       wali_kelas_uuid VARCHAR(36) NULL,
       world VARCHAR(64) NULL,
       x1 INT NULL, y1 INT NULL, z1 INT NULL,
       x2 INT NULL, y2 INT NULL, z2 INT NULL
   );
   ```
2. **`nschool_classroom_officers`**: Menyimpan kepengurusan kelas yang ditunjuk Wali Kelas (Ketua, Wakil, Sekretaris, Bendahara, Anggota).
   ```sql
   CREATE TABLE IF NOT EXISTS nschool_classroom_officers (
       player_uuid VARCHAR(36) PRIMARY KEY,
       id_kelas INT NOT NULL,
       role VARCHAR(20) NOT NULL,
       INDEX idx_officer_class (id_kelas)
   );
   ```
3. **`nschool_classroom_doors`**: Menyimpan batas koordinat pintu kelas yang akan berubah menjadi Tinted Glass (ketika kelas tutup) dan Air (ketika kelas buka).
    ```sql
    CREATE TABLE IF NOT EXISTS nschool_classroom_doors (
        id_kelas INT NOT NULL,
        door_number INT NOT NULL,
        world VARCHAR(64) NOT NULL,
        x1 INT NOT NULL, y1 INT NOT NULL, z1 INT NOT NULL,
        x2 INT NOT NULL, y2 INT NOT NULL, z2 INT NOT NULL,
        PRIMARY KEY (id_kelas, door_number)
    );
    ```
4. **`nschool_prefixes`**: Menyimpan prefix pangkat (Rank), tingkatan kelas (Class), dan peran kelas (Role) terpadu.
    ```sql
    CREATE TABLE IF NOT EXISTS nschool_prefixes (
        target_type VARCHAR(20),
        target_key VARCHAR(50),
        prefix VARCHAR(255) NOT NULL,
        PRIMARY KEY (target_type, target_key)
    );
    ```

### 3.2 Integrasi WorldGuard Region & Presensi
Perintah `/kelas start` (atau `/class start`) menggunakan prinsip **Auto-Detect Region**. Staf pengajar tidak perlu menulis `<id_kelas>` sebagai argumen jika mereka sedang berada di dalam kelas. Plugin mendeteksi koordinat lokasi pengirim menggunakan API WorldGuard untuk mencocokkan region berawalan `"kelas"` (misal: `kelas8`). 

* **Presensi Awal**: Murid yang berada di dalam region saat sesi dimulai otomatis mendapatkan status `HADIR` (atau `TERLAMBAT` jika dimulai di atas pukul 20:00 WIB).
* **Pemulangan Dini**: Murid yang telah menyelesaikan semua aktivitas belajar dapat dipulangkan oleh guru menggunakan `/kelas selesaikan <player>`. Ini mengunci status kehadiran mereka sebagai `HADIR` dan membuka pintu keluar region bagi pemain terkait untuk dapat meninggalkan area sekolah tanpa penalti.
* **Rekap Sesi**: Saat sesi ditutup, sistem akan mendata seluruh murid terdaftar. Siswa online yang tidak hadir di dalam kelas akan langsung mendapat status `ALFA`. Rekapitulasi nilai kuis dan presensi kemudian disimpan langsung ke database secara asinkron.

### 3.3 Kebijakan Auto-Fallback & Guardian Policy
Auto-Fallback bertindak sebagai pengaman otomatis untuk mengantisipasi kelalaian staf pengajar yang tidak hadir saat kelas dimulai pada jam 18:00 WIB. Mekanisme ini dieksekusi secara otomatis oleh scheduler pada pukul **18:15 WIB**:

```
[18:15 WIB] Scheduler mendeteksi tidak ada materi & kuis yang dimuat oleh Guru
                                  |
                +-----------------+-----------------+
                |                                   |
    [Ada Berkas Cadangan di DB]         [Tidak ada Berkas Cadangan]
                |                                   |
      Sistem memuat otomatis              Sistem menginjeksi Nilai 88
     materi & kuis terbaru dari          ke seluruh siswa kelas tersebut.
       DB untuk jenjang terkait.           Presensi dikunci sebagai HADIR.
                |                                   |
      Kelas berjalan mandiri.             Siswa bebas beraktivitas lain.
                                          Status presensi aman dari ALFA.
```

* **Kondisi 1 (Ada Berkas di DB)**: Sistem memindai tabel `natural_lesson_files` untuk mengambil materi proyektor (`MATERI_PROYEKTOR`) dan kuis pilihan ganda (`SOAL_KUIS`) terakhir berdasarkan kecocokan jenjang (`SD`/`SMP`/`SMA`). Sesi kelas dilanjutkan secara mandiri.
* **Kondisi 2 (Tidak Ada Berkas)**: Jika database kosong, sistem akan menginjeksi nilai default **`88`** ke tabel `natural_academic_grades` untuk seluruh murid terdaftar kelas tersebut dengan catatan `alasan_nilai = 'NILAI_88_GURU_ABSEN'`. Presensi harian juga dikunci sebagai `HADIR` untuk melindungi murid dari penalti `ALFA`.
* **Notifikasi**: Seluruh riwayat Auto-Fallback dikirimkan ke webhook Discord admin secara otomatis.

### 3.4 Obrolan Kelas Kustom & Struktur Peran
Sistem memuat tag peran kepengurusan, prefix pangkat (Ranks), dan prefix tingkatan kelas dari basis data (`nschool_prefixes`) ke dalam volatile memory cache saat startup. Jika database kosong saat startup awal, plugin akan melakukan migrasi (seeding) data default dari berkas `rankprefix.yml` ke dalam database secara otomatis.

Pemain dapat menggunakan perintah `/class chat <text>` atau `/class chat` (untuk toggle mode chat kelas) serta `/class chat norank` (untuk menyembunyikan prefix pangkat LuckPerms).
Format obrolan kelas didefinisikan secara dinamis di `config.yml` melalui node `class-settings.chat-format` and `class-settings.chat-format-norank`, mendukung placeholders:
- `<class_num>`: Angka kelas (1-12)
- `<class_prefix>`: Prefix tingkatan kelas dari basis data
- `<role_tag>`: Tag peran kepengurusan dari basis data
- `<prefix_luckperms>`: Pangkat/prefix LuckPerms (diperoleh secara dinamis via Reflection API)
- `<displayname>`: Nama tampilan pemain
- `<message>`: Isi pesan (aman dari exploit MiniMessage)

### 3.5 Transisi Area Kelas & Manajemen Pintu
* **Deteksi Pergerakan**: Menggunakan listener `PlayerMoveEvent` untuk melacak posisi pemain terhadap batas kubus area kelas. Saat melintasi batas:
  * **Masuk Kelas**: Mengirim pesan chat `§a§l[Nama_Kelas] §aMemasuki ruang kelas §e[Nama_Kelas]`
  * **Keluar Kelas**: Mengirim pesan chat `§c§l[Nama_Kelas] §cMeninggalkan ruang kelas §e[Nama_Kelas]`
* **Pintu Blok (Tinted Glass / Air)**: Ketika sesi kelas dimulai (`/kelas start`), pintu kelas yang terdaftar akan berubah menjadi `Material.AIR` secara otomatis. Ketika sesi selesai (`/kelas selesai`), pintu akan ditutup kembali dengan `Material.TINTED_GLASS`.

### 3.6 Visual Management Panel (UI)
Admins dan Wali Kelas dapat mengakses GUI visual Classroom Manager via `/class panel` (Java Edition menggunakan Paper Dialog API; Bedrock Edition menggunakan Geyser Cumulus CustomForm). Panel ini mempermudah konfigurasi nomor kelas, penunjukan Wali Kelas, dan penetapan batas area kelas langsung dari seleksi WorldEdit aktif pemain yang diambil secara asinkron via Reflection API.

---

## 📝 4. Stateful Exam & E-Rapor Engine

### 4.1 Stateful Ujian & Validasi Keamanan Multi-Tier
Menu ujian kelulusan diakses melalui `/school exam` dan dialirkan melalui satu portal stateful compiler `openPortalUjian(player, examType)`. Sebelum siswa diizinkan mengerjakan paket ujian (berisi tepat 10 soal), sistem mengevaluasi status pemain melalui 3 skenario validasi ketat:
1. **Scenario 1 (TIDAK AKTIF)**: Memeriksa apakah mata pelajaran tersebut masuk ke dalam whitelist paket ujian aktif (`active_uh_packets` untuk Ulangan Harian, atau `current_active_semester_packets` untuk UTS/UAS). Jika tidak aktif, menu subject akan direfresh dan menampilkan pesan peringatan.
2. **Scenario 2 (TIDAK ADA SOAL)**: Mencegah masuk ke dalam ujian apabila paket soal yang dipanggil dari cache memori tidak ditemukan atau tidak berjumlah tepat 10 pertanyaan.
3. **Scenario 3 (SUDAH MENGERJAKAN)**: Menerapkan perisai anti-pengerjaan ulang (*anti-retake database shield*). Sistem memeriksa database secara asinkron; jika pemain sudah memiliki catatan pengerjaan untuk paket tersebut, antarmuka inventaris klien akan langsung ditutup paksa.

### 4.2 Semester Break Interceptor
Selama pelaksanaan ujian UTS atau UAS, administrator dapat menyalakan status jeda istirahat (`is_semester_break`). Jika status ini aktif, siswa yang mencoba memulai mata pelajaran UTS/UAS berikutnya akan diblokir dengan pesan peringatan agar beristirahat hingga sesi berikutnya dibuka.

### 4.3 Logika Penilaian & Rumus Rapor Akhir Semester
Setiap paket ujian memiliki 10 pertanyaan. Pada pengiriman jawaban, plugin menghitung jumlah jawaban benar dan mengonversinya ke dalam skala 100 secara asinkron.
Jika nilai yang diinput berupa Ulangan Harian (UH), sistem akan mengambil rata-rata nilai UH dari seluruh paket yang ada untuk kelas tersebut.
Nilai akhir rapor kemudian dikalkulasi secara otomatis menggunakan bobot berikut:

$$\text{Nilai Akhir} = (40\% \times \text{Rata-rata UH}) + (30\% \times \text{UTS}) + (30\% \times \text{UAS})$$

*Grade* huruf dan kelulusan akademik ditentukan berdasarkan standar nilai berikut:
* **Nilai Akhir $\ge$ 85**: Grade `A` (Status: `LULUS`)
* **Nilai Akhir $\ge$ 70**: Grade `B` (Status: `LULUS`)
* **Nilai Akhir $\ge$ 55**: Grade `C` (Status: `LULUS`)
* **Nilai Akhir $<$ 55**: Grade `D` (Status: `REMEDI`)

### 4.4 HTTP Webhook & API Server
Exam Subsystem dilengkapi dengan server HTTP internal (`com.sun.net.httpserver.HttpServer`) yang mendengarkan pada port konfigurasi (default: `8080`) di bawah endpoint `/school/exam/update`.

* **Keamanan**: Kueri POST yang dikirimkan harus lolos dari penyaringan IP (`api.whitelist-ips`) serta menyertakan header otentikasi `X-School-API-Key` yang cocok dengan konfigurasi `api.api-key`.
* **Mekanisme Kerja**: Webhook ini menerima payload JSON berisi daftar soal ujian terbaru dan status konfigurasi portal dari dasbor eksternal. Sistem akan memperbarui nilai database, menimpa berkas `exams.json` lokal secara atomik, dan memperbarui cache memori secara dinamis tanpa perlu merestart server.

---

## 👥 5. Alur Onboarding & Registrasi Siswa

Pemain baru yang baru pertama kali bergabung dengan server akan secara otomatis masuk ke dalam alur onboarding interaktif:

```
[Join Server] -> Check Profile di DB -> NIS Belum Terdaftar?
                                           |
                                           v
                                   [Fase FREEZE]
                      - Pemain dibekukan (tidak bisa bergerak)
                      - Interaksi, break, place, drop, pickup DIBATALKAN
                                           |
                                           v
                              [Registrasi Step 1 (Welcome)]
                                           |
                                           v
                            [Registrasi Step 2 (Cinematic)]
                                           |
                                           v
                             [Registrasi Step 3 (ToS & Rules)]
                                           |
                              (Menyetujui ToS & Rules)
                                           v
                              [Pendaftaran Selesai (Complete)]
                      - Generate 10-Digit NIS secara atomik
                      - Set tingkatan: SD Kelas 1 (SD_1)
                      - Simpan asinkron & UNFREEZE pemain
```

* **Mekanisme Pembekuan (Freeze)**: Berjalan melalui event listener. Pemain yang belum terdaftar dilarang berjalan (hanya diizinkan menoleh), memecahkan/menaruh blok, membuang item, mengambil item, dan berinteraksi dengan dunia game.
* **Keamanan Pembuatan NIS**: Pembuatan NIS diatur menggunakan kunci biner atomik (`AtomicBoolean registrationInProgress`) untuk memastikan tidak terjadi balapan data (*race condition*) yang mengakibatkan terbentuknya nomor urut NIS ganda saat beberapa siswa mendaftar secara bersamaan. NIS dibuat dengan rumus format: `1` + `[3-digit nomor urut]` + `[6-digit tanggal hari ini (ddMMyy)]` (Contoh: `1005190626`).

---

## 💻 6. Kamus Perintah Lengkap (Commands Reference)

### 6.1 Perintah Siswa (`/school` & `/class` / `/kelas`)
* **Izin**: `/school` khusus murid terdaftar dengan NIS. `/class` khusus murid terdaftar di kelas (atau pengajar).

| Perintah | Deskripsi |
| :--- | :--- |
| `/school info` | Membuka antarmuka dialog profil berisi data username, NIS, jenjang kelas, dan detail semester. |
| `/school exam` | Membuka antarmuka menu Portal Ujian utama jika portal dalam status dibuka. |
| `/school class [class/player]` | Menampilkan struktur pengurus kelas (diri sendiri, kelas 1-12, atau kelas tempat player lain berada). |
| `/school help` | Menampilkan seluruh bantuan sub-perintah `/school` untuk siswa. |
| `/class` atau `/kelas` | Menampilkan bantuan lengkap perintah kelas bagi siswa (dan pengajar jika mereka staf). |
| `/class chat <pesan>` | Mengirim pesan ke obrolan kelas yang terdaftar. |
| `/class chat` | Toggle masuk/keluar saluran chat kelas secara permanen. |
| `/class chat norank` | Toggle menyembunyikan prefix pangkat LuckPerms di chat kelas. |
| `/class info` | Menampilkan kepengurusan kelas Anda saat ini. |

### 6.2 Perintah Guru & Helper (`/kelas` / `/class`)
* **Izin**: `naturalschool.admin` atau pengajar dengan peringkat prioritas $\ge$ `GURU_HONORER` (serta Wali Kelas untuk kelas terkait).

| Perintah | Deskripsi |
| :--- | :--- |
| `/class panel [player]` | Membuka GUI visual Classroom Manager bagi staf pengajar untuk menunjuk pengurus/wali kelas. |
| `/kelas start <id_kelas> <id_matapelajaran>` | Memulai sesi kelas baru. Membuka blok pintu (Air) dan mendata kehadiran siswa yang terdaftar. |
| `/kelas selesai [id_kelas]` | Mengakhiri sesi kelas. Menutup kembali blok pintu (Tinted Glass) dan memproses rekapitulasi data. |
| `/kelas selesaikan <player>` | Mengizinkan murid tertentu pulang dini dari kelas yang aktif. Kehadirannya dikunci sebagai HADIR. |
| `/kelas pembelajaran <id_kelas> <namaFile>` | Memuat materi papan proyektor kelas dari database. |
| `/kelas startsoal <namaFile>` | Mengambil kuis dari database dan mendistribusikannya ke murid yang berada di region kelas. |
| `/kelas rekap <id_kelas>` | Mengompilasi presensi dan nilai kuis sesi aktif saat ini ke database secara instan (1-klik manual). |
| `/kelas jawab <index> <jawaban>` | (Internal Siswa) Mengirimkan opsi jawaban kuis harian ke memori kuis kelas yang aktif. |

### 6.3 Perintah Administrator (`/naturalschool` / `/ns`)
* **Izin**: `naturalschool.admin` atau peringkat internal `KETUA_YAYASAN` / `WAKIL_KETUA_YAYASAN`.

| Perintah | Deskripsi |
| :--- | :--- |
| `/ns reload` | Memuat ulang seluruh file konfigurasi (`config.yml`), prefix pangkat, basis data, dan modul ujian. |
| `/ns info <player>` | Menampilkan informasi akademik detail mengenai profil pemain terkait (dapat mencari pemain offline dari DB). |
| `/ns rank set <player> <rank>` | Mengubah peringkat internal sekolah pemain secara dinamis. |
| `/ns rank list` | Menampilkan seluruh pangkat/prefix beserta kuncinya dari database. |
| `/ns rank update <RANK\|CLASS\|ROLE> <key> <prefix...>` | Mengubah prefix tertentu langsung di database dan memperbarui cache memori. |
| `/ns setclass <player> <1-12>` | Mengubah nomor tingkatan kelas murid (1-12). |
| `/ns setstage <player> <SD\|SMP\|SMA>` | Mengubah jenjang akademik murid. |
| `/ns class list` | Menampilkan daftar seluruh kelas 1-12 beserta detail Wali Kelas, batas area, pengurus, dan pintu. |
| `/ns class spy [player]` | Mengaktifkan/menonaktifkan mode penyadapan (spy) obrolan kelas lain. |
| `/ns class setwali <1-12> <player>` | Menunjuk Wali Kelas secara instan. |
| `/ns class area <1-12> set` | Menyimpan batas area kelas menggunakan seleksi WorldEdit aktif (diambil via Reflection). |
| `/ns class area <1-12> remove` | Menghapus batas area kelas. |
| `/ns class door <1-12> <door_num> set` | Menyimpan koordinat pintu kelas dari seleksi WorldEdit aktif (diambil via Reflection). |
| `/ns class door <1-12> <door_num> remove` | Menghapus koordinat pintu kelas. |
| `/ns class panel [player]` | Membuka GUI visual Classroom Manager untuk player terkait. |
| `/ns nis register <player>` | Mendaftarkan NIS otomatis untuk siswa tertentu secara paksa. |
| `/ns nis unregister <player>` | Menghapus NIS pemain dan mereset progresi akademik ke NONE (Memerlukan konfirmasi ulang dalam 15 detik). |
| `/ns nis set <player> <10-digit>` | Menentukan NIS numerik kustom 10-digit secara manual untuk siswa. |
| `/ns nis show [player]` | Menampilkan informasi NIS siswa terkait. |
| `/ns gui <welcome\|exam1-5\|version> [player]` | Membuka antarmuka prototipe atau memeriksa versi visual GUI secara paksa. |
| `/ns semester info` | Menampilkan tahun ajaran dan semester aktif. |
| `/ns semester end` | Melakukan pemutaran semester baru secara asinkron (GANJIL <-> GENAP). |
| `/ns semester reset` | Menyelaraskan status semester aktif kembali dengan waktu kalender real-life. |
| `/ns exam open` | Membuka akses portal ujian kelulusan secara paksa (bypass jadwal). |
| `/ns exam close` | Menutup portal ujian kelulusan secara paksa. |
| `/ns exam message <message...>` | Mengubah pesan deskripsi penolakan saat portal ujian ditutup. |
| `/ns exam sync` | Menyinkronkan seluruh daftar soal ujian dari database pusat ke berkas JSON lokal. |

---

## 📈 7. Integrasi Eksternal

### 7.1 PlaceholderAPI (PAPI Expansion)
NaturalSchool mengekspos variabel data internal siswa menggunakan pengidentifikasi `%naturalschool_<placeholder>%`.
* **Prinsip Anti-Lag**: Semua data placeholder ditarik langsung dari volatile memory cache di dalam `ProfileManager`. Sistem tidak akan pernah melakukan kueri database secara sinkron saat merender placeholder untuk mencegah terjadinya FPS drop atau server tersendat (*lag*).

| Placeholder | Kegunaan | Contoh Output |
| :--- | :--- | :--- |
| `%naturalschool_rank%` | Menampilkan awalan/prefix pangkat yang terformat | `§c§lKetua Yayasan` |
| `%naturalschool_class%` | Menampilkan nomor tingkatan kelas murid (1-12) | `10` |
| `%naturalschool_stage%` | Menampilkan nama jenjang akademik murid (SD/SMP/SMA) | `SMA` |
| `%naturalschool_nis%` | Menampilkan nomor NIS terdaftar, atau `-` jika belum | `1002090626` |

### 7.2 Log Discord Terintegrasi (Discord Webhooks)
Plugin menembakkan log peristiwa penting dalam format Rich Embed secara asinkron ke URL webhook Discord yang dikonfigurasi di `api.discord-webhook-url`:
* Memuat judul aktivitas, deskripsi status kelas, nama pengajar/Helper yang memulai, detail materi, serta status Auto-Fallback.
* Setiap log peristiwa memiliki representasi warna embed yang berbeda (Hijau untuk sesi kelas dimulai, Merah untuk sesi kelas berakhir / peringatan, Kuning untuk tindakan Auto-Fallback).
