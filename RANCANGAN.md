# DOKUMEN MASTER PLAN PENGEMBANGAN EKOSISTEM KOMUNAL NATURALSMP
**Kemitraan Strategis: VoidNode × NaturalSMP**
*Target Eksekusi: Tim Builder, Tim Developer Backend (NaturalSchool, NaturalCore, NaturalBridge, NaturalAuth)*
*Format Berkas: Panduan Implementasi Mutlak (.MD)*

---

## BAB 1: DESAIN ARSITEKTUR SERVER DAN SISTEM AKADEMIK UTAMA

Bab ini menetapkan fondasi dasar manajemen birokrasi, penumpukan hierarki kekuasaan, linimasa progresif pemain, serta logika otomatisasi sistem SKS Hybrid untuk mengendalikan roda perekonomian dan aktivitas inti di NaturalSMP.

### 1.1 Tata Kelola Manajemen dan Hierarki Kepemimpinan Staf

Struktur kepengurusan server dibagi ke dalam tiga lapis otoritas fungsional demi menjamin transparansi, pembagian beban kerja, dan pencegahan penyalahgunaan wewenang (*anti-abuse*):

#### 1.1.1 Otoritas Tertinggi (Server Management)
* **Pemegang Jabatan:** Founder / Owner (Haikal Mabrur & Management).
* **Fungsi Otoritas:** Pemilik mutlak database central, penanggung jawab infrastruktur jaringan proxy (`NaturalVelocity`), pemegang akses utama webstore (`https://store.naturalsmp.net`), serta pengambil keputusan final regulasi makro server.

#### 1.1.2 Manajemen Operasional Sekolah (Staf Admin)
* **Pemegang Jabatan:** Kepala Sekolah & Wakil Kepala Sekolah (Admin / Sr. Staff).
* **Fungsi Otoritas:** Mengawasi performa kerja harian jajaran Helper, mengelola ekosistem Discord dan WhatsApp jembatan komunikasi, melakukan eksekusi perintah pemeliharaan wilayah (*WorldGuard / LuckPerms Management*), serta menyetujui atau membatalkan status dispensasi siswa (`SAKIT` atau `IZIN`) pada dashboard website backend.

#### 1.1.3 Tenaga Pengajar & Fungsional (Staf Helper)
* **Pemegang Jabatan:** Guru Honorer / Guru Piket (Helper Server).
* **Fungsi Otoritas:** Penggerak utama konten harian di dalam game. Memiliki izin mutlak untuk mengeksekusi perintah manajemen kelas terbuka, melakukan rekap absensi, membimbing kuis harian, mendistribusikan Pekerjaan Rumah (PR), serta menertibkan perilaku pemain agar sesuai dengan etika bermasyarakat di wilayah yayasan.

### 1.2 Linimasa Akademik dan Integrasi KSE (Keseimbangan Ekonomi)

NaturalSMP menerapkan sistem penataan waktu dan perkembangan pemain (*progression scaling*) yang ketat agar siklus hidup server berlangsung panjang dan menantang:

#### 1.2.1 Konversi Waktu Dunia Nyata (Real-Life Timeline)
Satu Semester Akademik di dalam game setara dengan **1 Bulan Dunia Nyata (RL)**. Dalam satu semester tersebut, aktivitas harian berjalan secara otomatis di bawah kendali plugin `NaturalSchool` dengan siklus:
* **Minggu 1 & 2:** Fase pembelajaran intensif harian (Kuis dan pengumpulan materi harian).
* **Minggu 3:** Fase pengerjaan Tugas Besar, Pekerjaan Rumah (PR) susulan di Perpustakaan, dan pembukaan masa remedial awal.
* **Minggu 4:** Pelaksanaan Ujian Akhir Tahun (Ujian Kelulusan Jenjang) secara massal, disusul oleh pembagian E-Rapor Tri-Platform pada hari Minggu malam.

#### 1.2.2 Sistem Pembukaan Fitur Berbasis Tingkatan (Tier Progression System)
Pemain dilarang keras mengakses seluruh dimensi atau komoditas tingkat lanjut di hari pertama bergabung. Progresi dikunci berdasarkan Rank Akademik:
* **Murid SD (Tier Dasar):** Terkunci di dunia Overworld dalam radius koordinat terbatas. Fokus pada pengumpulan sumber daya pangan dasar (bertani, beternak) dan menebang kayu umum.
* **Murid SMP (Tier Menengah):** Membuka akses gerbang portal dimensi *Nether*, pemanfaatan sistem ramuan (*Brewing Station*), serta perdagangan instrumen *Iron Tools*.
* **Murid SMA (Tier Elit / End-Game):** Membuka gerbang portal *The End Gate*, menguasai teknologi sihir tingkat lanjut (*Custom Enchanting Table*), perlengkapan *Netherite*, serta izin penerbangan terbatas bagi pemilik rank premium tertentu.

### 1.3 Dinamika Pengelolaan Kelas Terbuka dan SKS Hybrid

Sistem kelas diatur agar berjalan dinamis tanpa memaksa staf atau murid untuk selalu online secara bersamaan secara konvensional:

#### 1.3.1 Aktivasi Kelas Dinamis (Dynamic Class Activation)
Kelas tidak akan aktif jika tidak ada Helper yang berjaga atau murid yang mendaftar. Ketika Guru Honorer mengeksekusi perintah pembukaan, ruang kelas berubah status menjadi *Active Session* yang mengirimkan sinyal data ke seluruh jaringan network server.

#### 1.3.2 Mekanisme Wali Kelas Merangkap (Multi-Class Assignment)
Seorang Staf Helper dapat ditugaskan untuk memegang kendali lebih dari satu sub-kelas atau mata pelajaran secara bersamaan melalui interaksi GUI manajemen guru. Hal ini dilakukan guna menghemat kebutuhan personil staf pada jam sepi (*off-peak hours*).

#### 1.3.3 Sinkronisasi Jam Belajar (SKS Hybrid System)
Murid yang tertinggal pelajaran reguler akibat kesibukan dunia nyata diizinkan mengumpulkan poin SKS secara mandiri melalui pembacaan modul pelajaran digital di Perpustakaan Agung pada jam bebas, memastikan keadilan bagi tipe pemain pekerja atau pelajar di dunia nyata.

### 1.4 Logika Otomatisasi Ujian Kenaikan Kelas dan Keuangan

Ujian kelulusan diatur secara mekanis oleh backend server tanpa campur tangan manusia untuk menjamin objektivitas nilai:

#### 1.4.1 Sinkronisasi GUI Lintas Platform (Crossplay Interface)
Menu lembar pertanyaan ujian ditarik langsung dari database web eksternal dan dirender ke dalam bentuk Chest GUI kustom. GUI ini dirancang responsif, kompatibel untuk dibaca secara jernih baik oleh pemain PC (Java Edition) maupun pemain Mobile/Konsol (Bedrock Edition via Geyser proxy).

#### 1.4.2 Alur Logika Penilaian Instan (Instant Backend Scoring)
Begitu tombol "KUMPULKAN JAWABAN" di dalam GUI diklik oleh pemain, skrip `NaturalSchool` akan langsung mencocokkan input jawaban dengan kunci jawaban di database, melakukan kalkulasi nilai instan, menyuntikkan skor ke riwayat nilai, dan menutup akses GUI agar tidak bisa dieksploitasi ulang.

#### 1.4.3 Kumpulan Bank Soal (Question Pool Randomization)
Untuk mencegah tindakan kecurangan antar-pemain (mencontek), sistem menggunakan algoritma pengacakan soal. Setiap murid yang membuka GUI ujian akan menerima paket kombinasi soal yang berbeda dari total 50 bank soal yang tersedia di database `natural_academic_pool`.

#### 1.4.4 Manajemen Database E-Rapor & Fungsi Penguras Uang (Money Sink)
Murid yang mendapatkan nilai di bawah Standar Kelulusan Minimum (SKM) diwajibkan mengambil remedial. Proses pendaftaran remedial memotong sejumlah Uang Sekolah (In-Game Salary) dalam jumlah besar. Ini berfungsi sebagai mekanisme *Money Sink* utama untuk menjaga laju inflasi mata uang di dalam game agar harga barang di pasar komunal tetap stabil.

### 1.5 Organisasi Siswa Internal Sekolah

#### 1.5.1 Restrukturisasi Fungsi Organisasi MPK & OSIS
* **MPK (Majelis Perwakilan Kelas) ➔ Perwakilan Derajat Angkatan:** Diisi oleh para Ketua Angkatan murid aktif. Berfungsi sebagai penampung aspirasi keluhan pemain mengenai tingkat kesulitan kuis atau regulasi klaim wilayah luar untuk diteruskan kepada Staf Admin.
* **OSIS (Organisasi Siswa Intra Sekolah) ➔ Eksekutif Event Server:** Badan perwakilan murid yang bertugas menggerakkan keramaian server. Setiap akhir pekan, pengurus OSIS merancang event pasar malam ekonomi, turnamen minigames, atau lomba eksterior bangunan dengan anggaran dana hadiah yang disetujui oleh Bendahara Server.

#### 1.5.2 Kualifikasi Berbasis Waktu Bermain (Playtime Filtering)
Untuk menghindari kepengurusan yang tidak aktif (*inactive leadership*), pencalonan anggota MPK/OSIS disaring ketat secara otomatis oleh database `NaturalCore`:
* **Batas Minimal (3 Hari / 72 Jam RL Online Time):** Syarat mutlak pendaftaran agar posisi diisi oleh pemain yang sudah memahami seluk-beluk komunitas server.
* **Batas Maksimal (30 Hari RL Online Time):** Batas atas khusus untuk posisi Ketua umum, memastikan kepemimpinan dipegang oleh pemain aktif fase pertengahan (*mid-game*) demi mendorong regenerasi kepemimpinan, sementara pemain *end-game* yang super aktif diarahkan menjadi staf fungsional/helper.

---

## BAB 2: ARSITEKTUR KOMPLEKS YAYASAN TERINTEGRASI DAN TATA RUANG SPASIAL

Bab ini menyediakan panduan spesifikasi spasial, tata letak arsitektur, cetak biru *non-player character* (NPC), serta seluruh infrastruktur fisik di dalam area aman Yayasan Natural sebagai panduan mutlak Tim Builder.

### 2.1 Struktur Arsitektur dan Pola Tata Ruang Gedung Kelas

Setiap jenjang pendidikan memiliki karakteristik struktural dan pola bangunan yang khas untuk mencerminkan perkembangan progresi pemain secara visual:

#### 2.1.1 Gedung SD (Klaster Dasar)
* **Pola Arsitektur:** Berbentuk huruf **"U" Terbuka** menghadap langsung ke arah Plaza Utama. Desain bangunan terdiri atas 2 tingkat.
* **Material Utama:** *Oak Wood*, *Stripped Wood*, dan *Stone Bricks* cerah dengan aksen tambahan *Lime* atau *Yellow Concrete*.
* **Tata Letak Ruangan:**
  * **Lantai 1:** Diisi oleh Kelas 1, Kelas 2, Kelas 3, dan Ruang Guru SD. Koridor tengah lantai 1 menyediakan Zona AFK berkode `#AFK_SD-1`.
  * **Lantai 2:** Diisi oleh Kelas 4, Kelas 5, Kelas 6, dan Ruang Aula Ujian Kelulusan SD.
* **Aset Estetik:** Papan tulis menggunakan material *Blackstone*, meja murid memanfaatkan tangga (*Stairs*) terbalik, serta jendela kaca besar untuk memberikan kesan ramah lingkungan.

#### 2.1.2 Gedung SMP (Klaster Menengah)
* **Pola Arsitektur:** Simetris Linier Berkoridor Ganda (Pola **"H"**). Desain bangunan terdiri atas 2 tingkat dilengkapi dengan balkon terbuka di area luar lantai dua.
* **Material Utama:** *Spruce Wood*, *Deepslate Tile*, dengan aksen warna *Light Blue Concrete*.
* **Tata Letak Ruangan:**
  * **Lantai 1:** Mencakup Kelas 7, Kelas 8, Ruang Tata Usaha (TU) SMP, dan Zona AFK berkode `#AFK_SMP-1`.
  * **Lantai 2:** Mencakup Kelas 9, Laboratorium Kimia (area pembuatan ramuan/*brewing*), dan Ruang Ujian Kelulusan SMP.

#### 2.1.3 Gedung SMA (Klaster Elit / End-Game)
* **Pola Arsitektur:** **Kastel Universitas Klasik Gothic (Pola Kotak Tertutup dengan Courtyard atau taman terbuka di bagian tengah)**. Desain bangunan terdiri atas 3 tingkat dengan Menara Jam tinggi sebagai ikon utama.
* **Material Utama:** *Quartz*, *Nether Bricks*, *Dark Oak Wood*, dengan aksen mewah berupa *Red Concrete* or *Gold Block*.
* **Tata Letak Ruangan:**
  * **Lantai 1:** Mencakup Kelas 10, Ruang OSIS Inti, dan Zona AFK berkode `#AFK_SMA-1`.
  * **Lantai 2:** Mencakup Kelas 11, Laboratorium Sihir (*Custom Enchanting Area*), dan Ruang Guru SMA.
  * **Lantai 3 (Menara):** Menjadi area Kelas 12, Ruang Rapat MPK, dan Ruang Portal Ujian Akhir (*The End Gate*).

#### 2.1.4 Perpustakaan Agung Yayasan (The Grand Library)
* **Posisi Strategis Peta:** Terletak tepat di ujung belakang Plaza Utama, berada pada poros tengah kompleks yayasan. Bangunan ini menjulang tinggi di antara ketiga klaster gedung sekolah sebagai pusat ilmu pengetahuan.
* **Desain Interior:** Bergaya katedral klasik dengan atap kubah kaca transparan (*skylight*), jajaran rak buku raksasa setinggi 4 hingga 5 blok, tangga kayu melingkar, dan hamparan karpet merah panjang di sepanjang koridor utama.
* **Konten Interaktif:** Rak buku interaktif yang dapat diklik kanan untuk mendapatkan *Written Book* panduan lore, bilik-bilik meja belajar mandiri bersekat, dan papan pengumuman pengumpulan tugas mingguan sistem SKS Hybrid.

#### 2.1.5 Kompleks Gedung Ekstrakurikuler & Pusat Bakat
* **Pola Arsitektur:** Gedung olahraga dan seni bergaya modern kontemporer yang terletak di antara gedung SMP dan SMA. Memiliki kubah besar dan dinding kaca melingkar.
* **Hubungan dengan Gameplay:** Tempat menampung fasilitas latihan fisik, studio seni, atau arena uji ketangkasan. Bangunan ini memiliki ruang fungsional seperti Ruang Gym & Latihan Fisik tempat berdirinya NPC Pelatih Khusus penyedia quest tantangan fisik (seperti parkour bawah tanah sekolah) untuk meningkatkan kapasitas status karakter secara permanen.

#### 2.1.6 Dermaga Pemancingan Relaksasi Siswa (Plaza Fishing Dock)
* **Posisi Spasial:** Terletak tepat di area luar (sebelah) Air Mancur Plaza Tengah, memanfaatkan aliran air buatan yang didesain mengelilingi pusat Social Hub.
* **Tujuan Arsitektur & Gameplay:** Dirancang sebagai fasilitas ekonomi-relaksasi aktif bagi pemain yang berada di zona AFK. Builder wajib membangun dermaga kayu estetis menggunakan material *Dark Oak Slabs* lengkap dengan dekorasi jala ikan, tong kayu, dan jembatan kecil. Murid dapat memancing di sini sembari menunggu jam operasional kelas untuk mendapatkan item roleplay kustom, ikan konsumsi, atau sampah kuno yang bernilai tinggi jika dijual kembali ke koperasi.

### 2.2 Daftar Spesifikasi dan Penempatan NPC Terintegrasi

| ID NPC | Nama Resmi NPC | Lokasi Build | Fungsi Mekanis (Gameplay) | Catatan Visual (Skin) |
| :--- | :--- | :--- | :--- | :--- |
| `NPC_REGIST_01` | Pak Haris (Panitia MOS) | Aula Pendaftaran / Spawn | Memicu GUI pendaftaran murid baru, memberikan NIS dan starter pack buku panduan. | Setelan jas formal rapi lengkap dengan kacamata. |
| `NPC_SATPAM_01` | Pak Joko (Keamanan) | Gerbang Utama Kompleks | Menyediakan informasi regulasi server dan fitur teleportasi darurat jika pemain tersesat. | Seragam lengkap Satpam Indonesia dengan atribut topi baja/pet. |
| `NPC_PUSTAKA_01` | Ibu Sofia (Pustakawan) | Meja Depan Perpustakaan | Mengatur sistem peminjaman buku tugas atau penukaran quest item buku kuno. | Wanita paruh baya, rambut disanggul rapi, memegang sebuah buku. |
| `NPC_INFO_01` | Pak Bambang (Arsiparis) | Sudut Dalam Perpustakaan | Membuka akses informasi kurikulum akademik, jadwal ujian nasional, dan biaya remedial. | Pria tua bijaksana berpakaian kemeja batik tradisional. |
| `NPC_KANTIN_01` | Mbak Sri (Warung Makan) | Kios Utama Kantin Pusat | Admin Shop dasar untuk kebutuhan konsumsi makanan (roti, steak, wortel). | Memakai baju celemek memasak (*apron*) kain. |
| `NPC_KANTIN_02` | Mas Anto (Pengepul) | Sudut Kantin Pusat | Admin Shop penampungan tempat menjual hasil tani atau hasil tambang mentah. | Model pakaian petani tradisional atau pedagang pasar bumi. |
| `NPC_OSIS_01` | Ketua OSIS (Event Tracker) | Panggung Event Plaza | Menyediakan info quest mingguan OSIS dan tempat klaim reward pemenang event. | Karakter remaja mengenakan jas almamater sekolah kebanggaan. |
| `NPC_EXAM_SD` | Pengawas Ujian SD | Ruang Ujian Gedung SD | Memicu menu GUI lembar pertanyaan kelulusan jenjang SD. | Pakaian guru dinas rapi formal. |
| `NPC_EXAM_SMP` | Pengawas Ujian SMP | Ruang Ujian Gedung SMP | Memicu menu GUI lembar pertanyaan kelulusan jenjang SMP. | Pakaian guru dinas rapi formal. |

### 2.3 Cetak Biru Komponen Social Hub Yayasan Natural

Plaza tengah dirancang sebagai inti dari seluruh aktivitas sosial dan ekonomi server komunal:

#### 2.3.1 Sistem Zona AFK Berperingkat (Integrated AFK Pools)
Zona AFK ditempatkan pada titik regional strategis sekolah dengan pembagian reward bertingkat:
* `#AFK_GLOBAL-MAIN` (Air Mancur Plaza Tengah): Terbuka untuk Umum. Multiplier EXP dan Uang sekolah standar (*base rate*) per 10 menit.
* `#AFK_SD-1` (Lobi Istirahat Gedung SD): Hanya Rank Murid SD. Tambahan uang saku berkala yang disesuaikan untuk ekonomi pemula.
* `#AFK_SMP-1` (Taman Dalam Gedung SMP): Hanya Rank Murid SMP. Pemberian material crafting tingkat menengah acak (Iron/Coal shards).
* `#AFK_SMA-1` (VIP Lounge Courtyard SMA): Hanya Rank Murid SMA. Pemberian token khusus atau pecahan kosmetik/enchantment shards tingkat lanjut.

#### 2.3.2 Atmosfer Audio Sekolah (BGM Regional)
Memanfaatkan kerja integrasi plugin audio berbasis WorldGuard region flags untuk memperkuat imersi visual: Plaza Utama memainkan musik instrumen mars sekolah ceria, Perpustakaan Agung memainkan instrumen musik Lofi sunyi, dan Gedung SMA memainkan musik orkestra klasik gothic yang megah.

#### 2.3.3 Koperasi Sekolah (Gacha Crates Station)
Didesain menyerupai bangunan Koperasi Sekolah modern di sisi Plaza Utama, berisi jajaran Ender Chest kustom sebagai stasiun pembukaan peti (Peti Persediaan SD, Peti Beasiswa SMP, Peti Prestasi SMA, Peti OSIS).

#### 2.3.4 Kaveling Kios ChestShop & Bursa Saham Siswa
Area pasar diatur secara fisik di dalam Kantin Pusat berupa penyewaan 10-15 petak tanah kosong ukuran 3x3 bagi murid tingkat lanjut untuk membuka toko dagang mandiri, serta papan mading interaktif yang memicu pembukaan command lelang `/ah`.

#### 2.3.5 Premium Store Terminal & Paid Rank Preview Station
Paviliun semi-modern beratap kaca di sudut Plaza Utama untuk penukaran **Natural Coin** (`https://store.naturalsmp.net`). Bersebelahan dengan Altar Almamater Premium berisi 10 manekin kustom ber-pelat tekan kustom (*Weighted Pressure Plate*) untuk melihat pratinjau hak istimewa rank premium via Custom GUI khusus.

---

## BAB 3: INTEGRASI LUCKPERMS DAN ATRIBUT FUNGSIONAL PREMIUM (VIRTUAL MECHANICS)

Bab ini mengatur seluruh logika virtual, mekanis *perks*, fungsionalitas ekonomi premium melalui integrasi toko web, serta detail keuntungan fungsional dari **10 Paid Ranks** yang dikendalikan secara absolut menggunakan engine **LuckPerms**.

### 3.1 Sistem Jembatan LuckPerms & Kebijakan Natural Coin

Seluruh manajemen hak perizinan pemain menggunakan database terpusat plugin **LuckPerms**. Ketika skrip backend `NaturalBridge` mendeteksi adanya transaksi pembelian sukses di website `https://store.naturalsmp.net`, konsol server game akan mengeksekusi perintah sinkronisasi otomatis:

```

```text
File Master_Plan_NaturalSMP_Bab_1_5_Final.md successfully generated.

```bash
/lp user <player> parent add <nama_group_rank>

```

Sistem grup dikonfigurasi secara hierarkis berjenjang (*Inheritance System*), di mana rank yang lebih tinggi otomatis mendapatkan hak perintah dari rank di bawahnya secara mutlak.

### 3.2 Spesifikasi Detail Perizinan, Hak Istimewa, & Perks Teritori 10 Paid Ranks

Untuk mendongkrak nilai jual paket donasi (*value proposition*), pembagian kuota perintah utilitas, batas rumah (`/sethome`), batas lelang (`/ah`), serta kuota proteksi wilayah Land Claim (**Kavling Kosan Siswa**) dan faksi (**Klub Ekstrakurikuler/Geng Sekolah**) diatur dengan skema berikut:

#### 3.2.1 Tier 1: [MIDI]

* **LuckPerms Group Name:** `midi` | **Chat Box Prefix:** `&8[&7MIDI&8]&f` (Abu-abu Bersih)
* **Spesifikasi Permissions Nodes & Perks:**
* `essentials.sethome.max.3` (Maksimal 3 titik home koordinat)
* `auctionhouse.sell.max.3` (Maksimal 3 item di bursa lelang bersamaan)
* `essentials.hat` (Akses memakai blok kosmetik di kepala via perintah `/hat`)
* `naturalcore.command.condense` (Mengubah otomatis tumpukan ingot menjadi bentuk blok padat)
* `landclaim.limit.max.2` (Maksimal memiliki 2 lokasi Kavling Kosan Siswa)
* `landclaim.blocks.bonus.1500` (Bonus awal 1.500 balok proteksi wilayah)



#### 3.2.2 Tier 2: [VIP]

* **LuckPerms Group Name:** `vip` (Mewarisi grup `midi`) | **Chat Box Prefix:** `&a[&2VIP&a]&f` (Hijau Daun Muda)
* **Spesifikasi Permissions Nodes & Perks:**
* `essentials.sethome.max.5` | `auctionhouse.sell.max.5`
* `essentials.workbench` atau `essentials.wb` (Membuka Meja Kerja Crafting portabel di mana saja)
* `essentials.trash` (Membuka menu Tempat Sampah Virtual instan via `/trash`)
* `landclaim.limit.max.3` (Maksimal memiliki 3 lokasi Kavling Kosan Siswa)
* `landclaim.blocks.bonus.3000` (Bonus perluasan wilayah sebanyak 3.000 balok)



#### 3.2.3 Tier 3: [VIP+]

* **LuckPerms Group Name:** `vip_plus` (Mewarisi grup `vip`) | **Chat Box Prefix:** `&3[&bVIP+&3]&f` (Cyan Toska Magis)
* **Spesifikasi Permissions Nodes & Perks:**
* `essentials.sethome.max.7` | `auctionhouse.sell.max.6`
* `essentials.enderchest` atau `essentials.ec` (Membuka Peti Ender portabel secara instan untuk mengamankan barang)
* `landclaim.limit.max.4` (Maksimal memiliki 4 lokasi Kavling Kosan Siswa)
* `landclaim.blocks.bonus.5000` (Bonus perluasan wilayah sebanyak 5.000 balok)
* `naturalschool.discount.remedial.5` (Sinkronisasi database memberikan diskon biaya remedial ujian sebesar 5%)



#### 3.2.4 Tier 4: [MVP]

* **LuckPerms Group Name:** `mvp` (Mewarisi grup `vip_plus`) | **Chat Box Prefix:** `&b[&3MVP&b]` (Biru Langit Cerah)
* **Spesifikasi Permissions Nodes & Perks:**
* `essentials.sethome.max.10` | `auctionhouse.sell.max.8`
* `essentials.feed` (Mengisi bar lapar makanan karakter secara instan. Cooldown: 10 Menit)
* `landclaim.limit.max.5` (Maksimal memiliki 5 lokasi Kavling Kosan Siswa)
* `landclaim.blocks.bonus.8000` (Bonus perluasan wilayah sebanyak 8.000 balok)
* `naturalkantin.priority.chestshop` (Mendapatkan hak prioritas otomatis/antrean utama penyewaan kios dagang kantin)



#### 3.2.5 Tier 5: [MVP+]

* **LuckPerms Group Name:** `mvp_plus` (Mewarisi grup `mvp`) | **Chat Box Prefix:** `&9[&1MVP+&9]&f` (Biru Safir Tua)
* **Spesifikasi Permissions Nodes & Perks:**
* `essentials.sethome.max.15` | `auctionhouse.sell.max.10`
* `essentials.feed.cooldown.300` (Waktu tunggu perintah `/feed` dipangkas menjadi hanya 5 menit)
* `essentials.near` (Melihat daftar jarak pemain lain di dunia survival luar untuk mengantisipasi serangan)
* `supertrails.common.allow` (Membuka kategori efek partikel berjalan dasar seperti debu asap atau sparkles)
* `landclaim.limit.max.7` (Maksimal memiliki 7 lokasi Kavling Kosan Siswa)
* `landclaim.blocks.bonus.12000` (Bonus perluasan wilayah sebanyak 12.000 balok)



#### 3.2.6 Tier 6: [GOLD]

* **LuckPerms Group Name:** `gold` (Mewarisi grup `mvp_plus`) | **Chat Box Prefix:** `&e&l[&6&lGOLD&e&l]&f` (Emas Menyala Bold)
* **Spesifikasi Permissions Nodes & Perks:**
* `essentials.sethome.max.20` | `auctionhouse.sell.max.12`
* `essentials.heal` (Memulihkan bar darah penuh secara instan. Cooldown: 15 Menit. Otomatis terkunci jika berada dalam Combat Tag PvP)
* `naturalschool.discount.remedial.10` (Potongan biaya remedial naik menjadi 10% di database)
* `worldguard.region.bypass.afk_smp_1` (Bypass otoritas akademik, bebas masuk ruang AFK eksklusif SMP kapan saja)
* `landclaim.limit.max.10` (Maksimal memiliki 10 lokasi Kavling Kosan Siswa)
* `landclaim.blocks.bonus.20000` (Bonus perluasan wilayah sebanyak 20.000 balok)



#### 3.2.7 Tier 7: [NATURE]

* **LuckPerms Group Name:** `nature` (Mewarisi grup `gold`) | **Chat Box Prefix:** `&a&l[&2&lNATURE&a&l]&f` (Hijau Zamrud Khas)
* **Spesifikasi Permissions Nodes & Perks:**
* `essentials.sethome.max.30` | `auctionhouse.sell.max.15`
* `essentials.feed.cooldown.0` (Perintah `/feed` berubah menjadi tanpa batas waktu / makanan tidak terbatas selamanya)
* `essentials.nick` (Akses perintah mengubah nama panggilan chatbox kustom, menyaring kata kasar otomatis)
* `supertrails.nature.allow` (Membuka efek partikel berjalan eksklusif bertema alam hijau zamrud)
* `landclaim.limit.max.15` (Maksimal memiliki 15 lokasi Kavling Kosan Siswa)
* `landclaim.blocks.bonus.35000` (Bonus perluasan wilayah sebanyak 35000 balok)



#### 3.2.8 Tier 8: [NATURE+]

* **LuckPerms Group Name:** `nature_plus` (Mewarisi grup `nature`) | **Chat Box Prefix:** `&b&l[&a&lNATURE+&b&l]&f` (Gradasi Neon Magis)
* **Spesifikasi Permissions Nodes & Perks:**
* `essentials.sethome.max.40` | `auctionhouse.sell.max.20`
* `essentials.teleport.cooldown.0` (*No Teleport Delay* / Eksekusi perintah `/home` atau `/tpa` instan tanpa jeda tunggu diam 3 detik)
* `naturalafk.multiplier.15` (Multiplier ekonomi, mendapatkan bonus +15% Saldo Uang Sekolah saat berdiam di zona AFK)
* `landclaim.limit.max.20` (Maksimal memiliki 20 lokasi Kavling Kosan Siswa)
* `landclaim.blocks.bonus.50000` (Bonus perluasan wilayah sebanyak 50.000 balok)



#### 3.2.9 Tier 9: [CAKRAWALA]

* **LuckPerms Group Name:** `cakrawala` (Mewarisi grup `nature_plus`) | **Chat Box Prefix:** Gradasi Iridium Teks Bergerak Hex Purplish-Magenta: `[CAKRAWALA]`
* **Spesifikasi Permissions Nodes & Perks:**
* `essentials.sethome.max.60` | `auctionhouse.sell.max.25`
* `essentials.fly` (Akses terbang bebas. *Regulasi ketat:* Hanya aktif di dalam area Safe Zone kompleks yayasan sekolah dan area batas koordinat Land Claim pribadi).
* `worldguard.region.bypass.afk_sma_1` (Akses bebas masuk ke VIP Lounge SMA secara permanen)
* `landclaim.limit.max.30` (Maksimal memiliki 30 lokasi Kavling Kosan Siswa)
* `landclaim.blocks.bonus.80000` (Bonus perluasan wilayah sebanyak 80.000 balok)



#### 3.2.10 Tier 10: [INVESTOR]

* **LuckPerms Group Name:** `investor` (Mewarisi seluruh grup dari tier 1-9) | **Chat Box Prefix:** Teks Beranimasi Pelangi Bergerak Bergelombang: `[INVESTOR]`
* **Spesifikasi Permissions Nodes & Perks:**
* `essentials.sethome.max.unlimited` (Batas penyimpanan titik koordinat home tidak terbatas)
* `auctionhouse.sell.max.40` (Maksimal menjual 40 item sekaligus di bursa lelang)
* `essentials.heal.cooldown.300` (Waktu tunggu perintah `/heal` dipangkas ekstrem menjadi hanya 5 menit)
* `essentials.fly.wilderness.allow` (Izin terbang `/fly` aktif penuh di area luar eksplorasi survival, otomatis mati selama 3 menit jika terkena status Combat Tag pertempuran)
* `naturalschool.discount.remedial.25` (Hak pemilik modal komite sekolah, mendapatkan diskon biaya remedial ujian ekstrem sebesar 25%)
* `naturalbridge.cron.monthly.keydividen` (Sistem backend otomatis menyuntikkan dana dividen bulanan setiap tanggal 1 berupa: 2x Kunci Peti OSIS dan 1x Kunci Premium Crate gratis ke inventaris)
* `landclaim.limit.max.unlimited` (Batas jumlah lokasi Kavling Kosan Siswa tidak terbatas)
* `landclaim.blocks.bonus.150000` (Bonus perluasan wilayah raksasa sebanyak 150.000 balok proteksi)



### 3.3 Katalog Tambahan Premium Store & Atribut Karakter Sekunder

Di area Premium Store Terminal, koin premium hasil top-up dari webstore resmi bisa ditukarkan secara satuan untuk peningkatan status permanen (*Atribut Scaling*) dengan batasan ketat (*hard-capped*):

* **Token Atribut `+1 Heal` (Instant Recovery Cooldown System):** Setiap level upgrade memotong waktu tunggu pemulihan regenerasi darah alami karakter (*Saturation Regen Rate*) sebesar 5% di luar sekolah (Maksimal Level 5 / 25% Reduction).
* **Token Atribut `Extra Heart Shard`:** Memberikan penambahan slot kapasitas maksimal bar kesehatan di luar batas standar Minecraft bawaan (Maksimal pembelian 4 Shards / Setara 2 ekstra jantung penuh).

### 3.4 Mekanisme Kerja Custom GUI Preview Station

Menghapus sistem obrolan teks konvensional, pemicu mekanis di area Paid Rank Preview Station diatur dengan alur teknis berikut:

1. Pemain berdiri di atas pelat tekan kustom di depan manekin Donasi premium ➔ Sistem mendeteksi koordinat pelat ➔ Mengirimkan perintah backend: `/ui open rank_preview_<nama_rank> <player_name>`.
2. Layar pemain memunculkan wadah **Chest GUI / Custom UI Kustom** bertekstur indah terintegrasi *Resource Pack* server yang merender visualisasi keuntungan, spesifikasi *permissions node*, kuota kosan, dan batas lelang.
3. Di dalam GUI terpasang item tombol besar **"KLIK DI SINI UNTUK MEMBELI"**. Jika murid mengklik kanan tombol tersebut, server Minecraft mengirimkan tautan konfirmasi aman yang otomatis membuka jendela web browser pemain menuju URL target transaksi: `https://store.naturalsmp.net`.

---

## BAB 4: LOGIKA AKADEMIK 1-KLIK & DATABASE MANAJEMEN KESISWAAN INTERAKTIF (BACKEND GURU)

Bab ini mengatur arsitektur backend, hak akses pengajaran staf (Helper / Guru Honorer), manajemen ketertiban waktu nyata (WIB), serta perintah ringkas otomatisasi kompilasi nilai kesiswaan.

### 4.1 Skema Jadwal Operasional Kelas Terstruktur (WIB Fixed Time)

Untuk memastikan keseimbangan hidup riil para pemain, siklus jam operasional sekolah utama berjalan kaku menggunakan patokan zona waktu dunia nyata (WIB) pada malam hari:

* **Pukul 18.00 WIB (Pintu Kelas Terbuka & Sesi Dimulai):** Helper memasuki ruangan kelas kustom dan mengeksekusi perintah `/kelas start <id_matapelajaran>`. Sistem WorldGuard otomatis mengunci akses wilayah pintu masuk kelas secara fisik. Hanya murid dengan angkatan jenjang terkait yang diizinkan masuk dan duduk di kursi tangga terbalik.
* **Pukul 20.00 WIB (Batas Toleransi Mengunci):** Pintu absensi ditutup oleh sistem otomatis `NaturalSchool`. Murid baru yang melewati batas pintu kelas atau baru melakukan login ke server di atas jam ini akan otomatis dijatuhi status kehadiran permanent berkode **`TERLAMBAT`** pada database record harian.
* **Pukul 21.00 WIB (Kelas Selesai & Pembubaran):** Sesi pengajaran resmi ditutup secara global. Pemain yang tercatat online di server game namun tidak berada di dalam wilayah kelas dari rentang waktu 18.00 - 20.00 WIB akan langsung dijatuhi hukuman otomatis berkode **`ALFA`** (Membolos).

### 4.2 Sistem Perintah Guru, Materi Pembelajaran & Soal Ujian

Helper memiliki dua perintah utama yang **harus dijalankan secara manual** sebelum sesi kelas dimulai, yaitu memuat materi proyektor dan memuat soal kuis. File sumber kedua perintah ini disiapkan terlebih dahulu oleh Helper melalui **dashboard website** dan disimpan di sistem database pusat.

#### 4.2.1 Perintah Memuat Materi Pembelajaran (`/kelas pembelajaran <id_kelas> <namaFile>`)

* **Fungsi:** Menampilkan materi pelajaran pada proyektor/papan informasi virtual di dalam ruang kelas (berbasis Armor Stand display atau ItemFrame kustom).
* **Format Perintah:** `/kelas pembelajaran kelas1 matematika_aljabar_v2`
* **Mekanisme:** Plugin mengambil file konten dengan nama `matematika_aljabar_v2` dari tabel `natural_lesson_files` di database, lalu me-render teksnya ke dalam display proyektor kelas yang ditentukan.
* **Catatan:** File materi **harus dibuat terlebih dahulu** oleh Helper melalui panel website sebelum perintah ini bisa dieksekusi. Satu file dapat dipakai ulang di sesi berikutnya.

#### 4.2.2 Perintah Memulai Soal Kuis (`/kelas startsoal <namaFile>`)

* **Fungsi:** Membuka dan mendistribusikan soal kuis kepada seluruh murid yang hadir di kelas secara serentak melalui **Custom UI** bespoke NaturalSchool.
* **Format Perintah:** `/kelas startsoal matematika_kuis_minggu3`
* **Mekanisme:** Plugin mengambil paket soal dari database berdasarkan nama file, mengacak urutan soal, lalu mengirimkannya ke antarmuka Custom UI masing-masing murid. Jawaban dikirim balik secara asinkron ke backend untuk dinilai otomatis.
* **Catatan Penting:** Antarmuka soal kuis menggunakan **Custom UI bespoke** (bukan Chest GUI standar). Spesifikasi teknis Custom UI akan didokumentasikan terpisah di dokumen `SPEC_NaturalSchool_UI.md`.

#### 4.2.3 Perintah Pemulangan Dini (`/kelas selesaikan <player>`)

* **Mekanisme Kerja:** Jika seorang murid telah berhasil merampungkan kuis, menyimak materi proyektor, atau merampungkan praktik belajar sebelum pukul 21.00 WIB, Guru berhak mengeksekusi perintah ini.
* **Dampak Mekanis:** Status kehadiran murid tersebut langsung dikunci di database server sebagai `HADIR (COMPLETED)`. Wilayah pintu kelas akan terbuka khusus untuk karakter tersebut, mengizinkannya melompat keluar kompleks sekolah untuk melakukan grinding survival luar atau log-out dari game dengan aman tanpa takut kehilangan poin presensi harian.

#### 4.2.4 Sistem Pekerjaan Rumah (PR System) & Akses Perpustakaan

Guru dapat memberikan tugas Pekerjaan Rumah (PR) tambahan melalui dashboard web. Fitur ini menjadi penyelamat bagi murid yang terpaksa pulang dini atau murid yang tidak hadir sama sekali karena urusan real-life (`ALFA`, `SAKIT`, `IZIN`).

* Murid dapat mengerjakan PR ini kapan saja di luar jam operasional utama dengan mendatangi Perpustakaan Agung pada jam bebas, berinteraksi dengan buku digital, dan mengumpulkan lembar jawaban kuis mandiri.
* Penyelesaian tugas PR yang valid akan memicu eksekusi query SQL otomatis untuk menganulir rekap catatan poin `ALFA` mereka pada database mingguan sebelum hari kelulusan akhir pekan tiba.

### 4.3 Logika Rekapitulasi Nilai Otomatis 1-Klik (`/kelas rekap`)

Untuk mencegah fenomena kelelahan pengurus akibat mencatat data ratusan siswa secara manual, Helper dibekali perintah otomatisasi cerdas:

```bash
/kelas rekap <id_kelas>

```

**Alur Eksekusi Skrip Backend:**

1. Sistem melakukan pemindaian (*scanning*) kilat terhadap data nilai sementara yang tersimpan pada cache memori server dari pengerjaan kuis GUI para murid di sesi tersebut.
2. Skrip backend secara asinkron langsung melakukan *push data* massal, mengelompokkan nilai, menyuntikkan angka ke tabel `natural_academic_grades`, dan memperbarui status ketidakhadiran di tabel `natural_student_attendance`.
3. Mengirimkan sinyal otomatis ke API `NaturalBridge` untuk langsung mencetak dokumen E-Rapor Digital secara riil ke platform eksternal.

### 4.4 Struktur Tabel Database Kesiswaan Utama (MySQL DDL Blueprint)

Developer wajib membangun arsitektur tabel database terpusat berikut pada backend sistem:

```sql
-- 1. TABEL PENCATATAN PRESENSI SEKOLAH HARIAN
CREATE TABLE natural_student_attendance (
    id_log INT AUTO_INCREMENT PRIMARY KEY,
    player_uuid VARCHAR(36) NOT NULL,
    player_name VARCHAR(16) NOT NULL,
    id_kelas VARCHAR(10) NOT NULL,         -- Format ID: SD_01, SMP_07, SMA_11
    id_helper VARCHAR(36) NOT NULL,        -- UUID Helper pengajar sesi terkait
    mata_pelajaran VARCHAR(32) NOT NULL,
    status_kehadiran ENUM('HADIR', 'TERLAMBAT', 'ALFA', 'IZIN', 'SAKIT') NOT NULL,
    waktu_record TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX (player_uuid),
    INDEX (status_kehadiran)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 2. TABEL AKUMULASI NILAI AKADEMIK & TUGAS PR
CREATE TABLE natural_academic_grades (
    id_nilai INT AUTO_INCREMENT PRIMARY KEY,
    player_uuid VARCHAR(36) NOT NULL,
    jenjang ENUM('SD', 'SMP', 'SMA') NOT NULL,
    mata_pelajaran VARCHAR(32) NOT NULL,
    nilai_angka TINYINT UNSIGNED NOT NULL,  -- Range Angka 0 s.d 100
    tipe_ujian ENUM('HARIAN', 'AKHIR_TAHUN', 'PR_PERPUSTAKAAN', 'REMEDIAL') NOT NULL,
    jumlah_remedial_diambil TINYINT DEFAULT 0,
    alasan_nilai TEXT DEFAULT NULL,        -- Diisi 'NILAI_88_GURU_ABSEN' jika Helper tidak siapkan file
    waktu_input TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY unique_student_subject (player_uuid, mata_pelajaran, tipe_ujian),
    INDEX (player_uuid)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 3. TABEL RANGKUMAN PENCETAKAN RAPOR AKHIR SEMESTER
CREATE TABLE natural_e_rapor_digital (
    id_rapor INT AUTO_INCREMENT PRIMARY KEY,
    player_uuid VARCHAR(36) NOT NULL,
    nomor_induk_siswa VARCHAR(12) NOT NULL UNIQUE,
    tahun_ajaran_rl VARCHAR(10) NOT NULL,    -- Format: 2026-A, 2026-B
    jenjang_terakhir ENUM('SD', 'SMP', 'SMA') NOT NULL,
    total_hadir SMALLINT DEFAULT 0,
    total_terlambat SMALLINT DEFAULT 0,
    total_alfa SMALLINT DEFAULT 0,
    total_izin_sakit SMALLINT DEFAULT 0,
    nilai_rata_rata_kolektif DECIMAL(5,2) NOT NULL,
    status_kelulusan ENUM('LULUS', 'TINGGAL_KELAS', 'PROSES') DEFAULT 'PROSES',
    catatan_wali_kelas TEXT,
    waktu_cetak TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX (player_uuid)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 4. TABEL FILE MATERI & SOAL YANG DISIAPKAN HELPER VIA WEBSITE
CREATE TABLE natural_lesson_files (
    id_file INT AUTO_INCREMENT PRIMARY KEY,
    nama_file VARCHAR(64) NOT NULL UNIQUE, -- Nama unik file, dipakai di command
    tipe ENUM('MATERI_PROYEKTOR', 'SOAL_KUIS') NOT NULL,
    jenjang ENUM('SD', 'SMP', 'SMA') NOT NULL,
    mata_pelajaran VARCHAR(32) NOT NULL,
    konten_json TEXT NOT NULL,             -- JSON isi materi atau array soal
    dibuat_oleh VARCHAR(36) NOT NULL,      -- UUID Helper yang membuat
    dibuat_pada TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    dipakai_terakhir TIMESTAMP NULL,
    INDEX (jenjang, mata_pelajaran),
    INDEX (tipe)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

```

### 4.5 Sistem Auto-Fallback Kelas & Kebijakan Nilai 88 (Guardian Policy)

Sistem ini adalah jaring pengaman yang melindungi murid dari kelalaian operasional staf. Logika berjalan otomatis setiap hari pada pukul 18.00 WIB jika Helper belum mengeksekusi perintah `/kelas pembelajaran` dan `/kelas startsoal` dalam waktu 15 menit.

#### Diagram Alur Logika Auto-Fallback

```
[18.15 WIB] Helper belum /kelas pembelajaran & /kelas startsoal?
                              |
              +---------------+---------------+
              |                               |
    [Ada file terbaru di database]   [Tidak ada file sama sekali]
              |                               |
    Sistem otomatis load file        Sistem injeksi nilai 88 ke
    MATERI + SOAL terbaru yang       seluruh murid terdaftar di
    pernah dibuat Helper untuk       kelas tersebut pada hari itu.
    jenjang & mapel tersebut.        Alasan: 'NILAI_88_GURU_ABSEN'
              |                               |
    Kelas berjalan mandiri.          Murid bebas lakukan aktivitas
    Murid mengerjakan soal           lain. Tidak ada hukuman untuk
    dari Custom UI seperti biasa.    murid karena bukan salah mereka.
```

#### Rincian Kebijakan

* **Kondisi 1 — Ada File Terbaru:** Plugin mencari file `MATERI_PROYEKTOR` dan `SOAL_KUIS` terbaru dari tabel `natural_lesson_files` berdasarkan kolom `jenjang` dan `mata_pelajaran` yang cocok, diurutkan descending berdasarkan `dibuat_pada`. File tersebut diload otomatis tanpa intervensi Helper.
* **Kondisi 2 — Tidak Ada File:** Jika query mengembalikan hasil kosong (Helper belum pernah membuat satu pun file), server mengeksekusi batch INSERT ke tabel `natural_academic_grades` untuk seluruh murid terdaftar di kelas tersebut dengan `nilai_angka = 88` dan `alasan_nilai = 'NILAI_88_GURU_ABSEN'`. Nilai ini **tidak dihitung sebagai hukuman bagi murid** dan tidak mempengaruhi rekap ALFA mereka.
* **Transparansi:** Notifikasi otomatis dikirim ke kanal Discord admin server dengan log lengkap: nama kelas, jenjang, mata pelajaran, nama Helper yang bertanggung jawab, serta status yang dipilih sistem (auto-load file atau injeksi nilai 88).

---

## BAB 5: SIKLUS PENGALAMAN HARIAN SISWA (THE DAILY PLAYER JOURNEY LOOP)

Bab ini mendesain alur pergerakan perjalanan pemain dari menit pertama memasuki server network jaringan hingga rekapitulasi data lintas platform di luar game.

### 5.1 Fase Login dan Validasi Akun (The Gateway Phase)

Siklus dimulai saat pemain melakukan koneksi ke gerbang proxy network server:

1. **Penyaringan Jaringan (`NaturalVelocity` & `NaturalAuth`):** Pemain Java dan Bedrock disatukan melewati proxy performa tinggi `NaturalVelocity`. Plugin `NaturalAuth` melakukan pengecekan status whitelist tautan Discord akun. Jika valid, data UUID dikirim ke `NaturalCore` untuk memuat data grup perizinan **LuckPerms** (Bab 3) serta tingkatan akademik karakter.
2. **Titik Pendaratan (Spawn Area):** Murid baru mendarat di kawasan Aula Masa Orientasi Siswa (MOS), wajib berinteraksi dengan `NPC_REGIST_01` (Pak Haris) untuk mendaftarkan NIS kustom. Murid lama otomatis mendarat di Plaza Utama atau titik koordinat terakhir dunia survival luar tempat mereka log-out.

### 5.2 Skenario Jam Operasional Sekolah (The Peak Academic Loop)

Memasuki pukul 18.00 WIB dunia nyata, siklus akademik utama diaktifkan:

* Pintu besi ruang kelas mengunci otomatis. Murid-murid memasuki meja belajar tangga terbalik untuk menyerap materi pelajaran digital GUI yang ditarik dari database backend.
* Pukul 20.00 WIB, batas absensi terkunci. Murid telat otomatis mendapat tanda status `TERLAMBAT`.
* Bagi murid yang menyelesaikan kuis harian dengan cepat, Guru mengeksekusi `/kelas selesaikan <player>` agar murid dapat pulang mendahului jadwal untuk menghemat waktu dunia nyata mereka yang berharga.

### 5.3 Skenario Jam Bebas Sekolah (The Off-Peak Grinding Loop)

Begitu bel tanda sekolah dibubarkan tepat pukul 21.00 WIB, kompleks dalam sekolah berubah fungsi menjadi zona aman (*Safe Zone Anti-PvP & Anti-Grief*), dan pemain masuk ke mode Jam Bebas:

#### 5.3.1 Eksplorasi Survival & Rivalitas Antar-Geng

Murid melintasi gerbang utama keluar dari pengawasan `NPC_SATPAM_01` (Pak Joko) memasuki area Wilderness tanpa batas:

* **Kavling Kosan Siswa (Land Claim):** Murid menggunakan hak proteksi wilayah mereka sesuai limitasi jatah pangkat donasi untuk membangun pangkalan peti penyimpanan atau rumah kos pribadi agar aman dari penjarahan murid lain.
* **Klub Ekstrakurikuler & Geng Sekolah (Clan Faction):** Murid berkumpul mendirikan faksi kustom bertema sekolah (seperti "Klub Robotik", "Klub Pencak Silat", atau "Geng Motor Belakang Sekolah"). Geng-geng ini saling bertempur memperebutkan kendali wilayah tambang luar Overworld, Nether, maupun End untuk mengumpulkan komoditas ekonomi berharga.

#### 5.3.2 Sesi Kejar Paket Mandiri (Perpustakaan)

Siswa yang membolos atau tidak bisa login di jam operasional reguler memanfaatkan jam bebas ini untuk masuk ke Perpustakaan Agung. Mereka membaca arsip modul dan menyelesaikan Pekerjaan Rumah (PR) susulan untuk mengubah status dosa `ALFA` mereka di database kesiswaan menjadi status aman sebelum akhir pekan.

### 5.4 Sesi Akhir Pekan: Sinkronisasi E-Rapor Global Tri-Platform via NaturalBridge

Setiap akhir minggu malam (RL), sistem otomatisasi *cronjob backend* pada plugin **NaturalBridge** akan menarik data rangkuman dari tabel database `natural_e_rapor_digital` untuk ditembakkan secara berkala menuju tiga platform komunikasi eksternal secara simultan:

```
                  +-----------------------------------+
                  |   DATABASE CENTRAL E-RAPOR SISWA   |
                  +-----------------------------------+
                                    |
                        (Trigger NaturalBridge API)
                                    |
        +---------------------------+---------------------------+
        |                           |                           |
        v                           v                           v
+-------------------+       +-------------------+       +-------------------+
|    DISCORD BOT    |       |   WHATSAPP ALERTS |       |   EMAIL SERVICES  |
| Rich Embed Image  |       | Text Webhook Alert|       | Automated PDF     |
| E-Rapor Card DM   |       | Ringkasan Kelulusan|      | Official Report   |
+-------------------+       +-------------------+       +-------------------+

```

1. **Discord Integration Hub:** Bot resmi NaturalSMP menembakkan pesan privat (DM) langsung ke akun Discord murid yang tertaut berupa kartu hasil studi (*Rich Embed Card*) ber-badge emas, menampilkan statistik kehadiran, nilai angka ujian harian, serta catatan wali kelas.
2. **WhatsApp Notification Alert:** Mengirimkan pesan teks ringkas instan ke nomor WhatsApp pribadi pemain yang terdaftar sebagai notifikasi saku cepat mengenai ringkasan rata-rata skor kesiswaan dan status promosi rank akademik baru.
3. **Official Email Service (Surat Elektronik Yayasan):** Skrip generator di backend `NaturalBridge` akan mengonversi baris tabel MySQL menjadi berkas dokumen cetak **PDF E-Rapor Resmi**. Dokumen PDF ini dilampirkan dan dikirim ke alamat email pemain dengan subjek profesional: `[YAYASAN NATURAL] Laporan Hasil Evaluasi Belajar Mengajar Siswa - Akhir Semester`, menghadirkan impresi *roleplay* sekolah modern yang sangat imersif dan nyata.

### 5.5 Alur Data Detail Sesi Kelas (Class Session Data Flow)

Sistem kelas akademik mengalirkan data secara real-time dari aktivitas in-game murid dan guru langsung menuju database pusat. Berikut adalah alur visual dan penjelasannya:

#### 5.5.1 Perspektif Guru / Helper (Teacher Workflow)

```mermaid
sequenceDiagram
    actor G as Helper / Guru
    participant S as NaturalSchool (Spigot)
    participant D as MySQL Database
    participant W as Discord Admin Webhook

    G->>S: Eksekusi /kelas start <id_kelas> <id_matapelajaran>
    Note over S: Kunci region pintu kelas via WorldGuard (entry deny)
    S->>G: Sesi kelas aktif & log ke broadcast chat
    
    G->>S: Eksekusi /kelas pembelajaran <id_kelas> <namaFile>
    S->>D: SELECT konten_json FROM natural_lesson_files
    D-->>S: Return berkas materi pelajaran
    S-->>G: Tampilkan materi ke papan tulis / proyektor
    
    G->>S: Eksekusi /kelas startsoal <namaFile>
    S->>D: SELECT konten_json FROM natural_lesson_files
    D-->>S: Return array soal kuis
    S-->>G: Distribusikan kuis ke murid di kelas
    
    G->>S: Eksekusi /kelas selesaikan <player>
    Note over S: Tandai murid: HADIR (COMPLETED)
    Note over S: Buka akses keluar region khusus player terkait
    S-->>G: Murid dipulangkan dini
    
    G->>S: Eksekusi /kelas selesai
    S->>D: Batch INSERT status presensi (natural_student_attendance)
    S->>D: Batch INSERT nilai akhir kuis (natural_academic_grades)
    S->>W: POST embed log rangkuman kelas (Discord)
    S-->>G: Sesi ditutup & rekap database berhasil
```

* **Penjelasan Alur Guru:**
  1. **Inisiasi Sesi:** Guru mengawali kelas, memicu penguncian pintu kelas via WorldGuard secara otomatis.
  2. **Pemuatan Berkas Pelajaran:** Perintah `/kelas pembelajaran` dan `/kelas startsoal` memicu penarikan data JSON dari tabel `natural_lesson_files`.
  3. **Penilaian Terpusat:** Saat guru menutup kelas (`/kelas selesai`), seluruh data kehadiran (termasuk deteksi murid membolos sebagai `ALFA`) dan akumulasi nilai kuis di-push ke database, diikuti dengan pengiriman status log ke Discord webhook.

#### 5.5.2 Perspektif Murid / Siswa (Student Workflow)

```mermaid
sequenceDiagram
    actor M as Murid / Siswa
    participant S as NaturalSchool (Spigot)
    participant D as MySQL Database
    participant B as NaturalBridge (E-Rapor)

    M->>S: Masuk ke wilayah kelas (18.00 - 20.00 WIB)
    Note over S: Presensi awal tercatat: HADIR
    
    M->>S: Masuk kelas setelah jam toleransi (20.00 - 21.00 WIB)
    Note over S: Presensi awal tercatat: TERLAMBAT
    
    M->>S: Menyimak papan proyektor
    M->>S: Membuka lembar kuis via Custom UI
    M->>S: Mengklik pilihan ganda (Custom UI trigger: /kelas jawab)
    Note over S: Kalkulasi skor otomatis di backend (asinkron)
    S->>S: Update nilai kuis sementara dalam memori cache
    
    M->>S: Meninggalkan kelas dini (jika di-selesaikan Guru)
    Note over S: Status HADIR dikunci, bebas menjelajah dunia luar
    
    Note over S: Pukul 21.00 WIB: Kelas ditutup otomatis oleh sistem
    S->>D: Simpan data presensi harian & nilai kuis siswa
    
    Note over B: Minggu Malam: Cronjob berjalan
    B->>D: SELECT data rekap rapor (natural_e_rapor_digital)
    B-->>M: Push kartu E-Rapor via Discord DM & Email PDF
```

* **Penjelasan Alur Murid:**
  1. **Presensi Otomatis:** Deteksi kehadiran murid terbagi menjadi zona waktu reguler (`HADIR`) dan zona waktu terlambat (`TERLAMBAT`).
  2. **Interaksi Kuis:** Setiap klik pilihan ganda pada Custom UI mengirimkan respons asinkron ke server untuk dicocokkan dengan kunci jawaban. Nilai disimpan di cache memori sebelum disimpan permanen saat sesi kelas berakhir.
  3. **Kejar Paket Mandiri:** Murid yang bolos (`ALFA`) dapat mengerjakan tugas PR susulan di Perpustakaan Agung pada jam bebas, yang secara otomatis akan mengeksekusi query SQL UPDATE untuk menghapus penalti membolos mereka.

---

## BAB 6: MITIGASI RISIKO OPERASIONAL & ROADMAP IMPLEMENTASI

Bab ini mendokumentasikan ancaman utama terhadap kelangsungan hidup server serta strategi mitigasi resmi yang menjadi panduan keputusan teknis dan manajerial.

### 6.1 Risiko Burnout Staf — Kelas Mandiri & Rotasi Shift

Helper adalah aset paling kritis sekaligus paling rentan di seluruh ekosistem NaturalSMP. Sistem telah dirancang agar server **tidak mati** meski tidak ada satu pun Helper yang online.

**Mitigasi yang sudah ter-embed di sistem:**
* **Auto-Fallback (Subbab 4.5):** Jika Helper absen, sistem otomatis load file materi + soal terbaru dari database, atau menginjeksi nilai 88 jika tidak ada file.
* **Sistem Rotasi Shift:** Jadwal Helper dibagi menjadi 3 kelompok rotasi mingguan (misal: Kelompok A aktif Senin-Rabu, B aktif Rabu-Jumat, C aktif Sabtu-Minggu). Tidak ada Helper yang wajib online setiap hari.
* **Batas Minimum File:** Setiap Helper baru wajib menyiapkan minimal **5 file materi** dan **5 file soal** per mata pelajaran sebelum aktif mengajar. File-file ini menjadi cadangan otomatis sistem.

### 6.2 Risiko Ketergantungan Jam — Jendela Waktu Fleksibel

Pemain dari latar belakang berbeda (pelajar, pekerja, beda timezone) tidak boleh dihukum secara sistematis.

**Solusi: 3 Jendela Harian**

| Jendela | Waktu WIB | Mode | Keterangan |
| :--- | :--- | :--- | :--- |
| **Pagi** | 07.00 – 09.00 | Mandiri | Auto-load file, tanpa Helper wajib |
| **Sore** | 15.00 – 17.00 | Mandiri | Auto-load file, tanpa Helper wajib |
| **Malam** | 18.00 – 21.00 | Reguler | Helper aktif, reward lebih besar |

Murid memilih **satu jendela per hari** untuk mendapatkan poin kehadiran. Nilai dari kelas Reguler malam mendapat bonus multiplier kecil sebagai insentif untuk tetap aktif bersama Helper.

### 6.3 Risiko Kompleksitas Plugin — Roadmap MVP Bertahap

Plugin `NaturalSchool` tidak perlu selesai sepenuhnya sebelum server bisa launch. Pembangunan dibagi dalam tiga fase dengan target yang jelas:

#### Fase 1 — "Buka Dulu" (Target: 2–4 Minggu Pertama)
* `/kelas start`, `/kelas selesai`, `/kelas selesaikan <player>` aktif
* Perintah `/kelas pembelajaran` dan `/kelas startsoal` aktif
* Absensi sederhana: `HADIR` / `ALFA` / `TERLAMBAT`
* Custom UI soal kuis (versi awal, pilihan ganda dasar)
* Nilai tersimpan di `natural_academic_grades`
* Auto-fallback + Kebijakan Nilai 88 aktif

#### Fase 2 — "Rapor Hidup" (Target: +2–4 Minggu)
* Sistem PR & Perpustakaan Agung aktif
* Status `IZIN` / `SAKIT` + approval dashboard admin
* Rekapitulasi `/kelas rekap` aktif
* E-Rapor via Discord DM (Rich Embed)
* Notifikasi Discord ke kanal admin untuk log Guardian Policy

#### Fase 3 — "Full Vision" (Target: +1 Bulan)
* Tiga jendela waktu harian aktif
* E-Rapor PDF via Email Service
* WhatsApp Notification Alert
* Ujian Akhir Semester dengan pool soal acak massal
* Custom UI soal kuis versi final (bespoke NaturalSchool)

