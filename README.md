# 🏫 NaturalSchool

NaturalSchool is an enterprise-grade, high-performance core plugin built for **Paper/Spigot (Minecraft 1.21.10 / Java 21)**. Adhering to the **Core-Infrastructure** pattern, it decouples configurations, data managers, dynamic onboarding, and student databases into thread-safe, modular interfaces.

NaturalSchool powers player academic student lifecycle management, featuring direct SQLite integration, HikariCP-managed MySQL connection pooling, dynamic NIS generation, and a centralized state-passing exam & digital rapor evaluation engine.

---

## 🚀 Key Architectural Pillars

### 1. Enterprise Database Pool & Caching
* **HikariCP MySQL Pool**: Features robust connection pool configurations managing connection timeout threshold limits, prepared statement caching limits, and connection leak diagnostics.
* **Optimized Local SQLite Engine**: Fallback to an optimized local `school.db` connection. Applies SQLite PRAGMAs (`WAL` journaling, `NORMAL` synchronous mode, and `busy_timeout` threshold limits) to resolve database locks (`SQLITE_BUSY`) during intensive writing.
* **Thread-Safe Memory Caching**: Student profiles are cached in volatile memory using `ConcurrentHashMap<UUID, StudentProfile>`. The system does **not** cache `Org.bukkit.entity.Player` objects to prevent memory leaks.
* **Asynchronous Database Processing**: All database read/write queries run asynchronously off the main server thread (`BukkitScheduler`) to maintain 20 TPS.

### 2. Unified Cross-Platform UI Engine
* **Dynamic Connection Routing**: Automatically routes players to appropriate interfaces. Java Edition clients receive rich interactive components styled with Kyori Adventure `MiniMessage`, while Bedrock Edition clients (via Geyser/Floodgate) interact with Cumulus Forms.
* **Bedrock Button Form Sanitization**: All Bedrock choice buttons are dynamically cleaned of raw legacy format codes (`§`) and decorative icons (`○`, `●`) to prevent blank-rendering bugs and maintain font uniformity.

### 3. Stateful Data-Driven Exam & E-Rapor Engine
* **Stateful Dynamic UI Compiler**: Replaces legacy subject routes with a single dynamic portal compiler mapping to `openPortalUjian(player, examType)`.
* **Multi-Tier whitelisting & Validations**: Evaluates player actions against three distinct safety scenarios:
  1. *Scenario 1 (TIDAK AKTIF)*: Intercepts clicks and refreshes the subject portal injecting localized warning headers if the subject is not whitelisted.
  2. *Scenario 2 (TIDAK ADA SOAL)*: Prevents entering exams with missing questions, displaying alert text.
  3. *Scenario 3 (SUDAH MENGERJAKAN)*: Implements an asynchronous anti-retake database shield. Force-closes client inventories upon detecting existing records.
* **Weighted Rapor Calculations**: Automates assessment grades calculations on final submission:
  $$\text{Final Score} = (40\% \times \text{Average UH}) + (30\% \times \text{UTS}) + (30\% \times \text{UAS})$$
  Computes letter grades (`A` / `B` / `C` / `D`) and updates status variables (`LULUS` / `REMEDI`).

---

## 🛠️ Configuration (`config.yml`)

Customize database connection pools, local storage modes, default initialization nodes, and scheduler offsets.

```yaml
database:
  storage-type: "SQLITE" # Options: SQLITE, MYSQL
  host: "localhost"
  port: 3306
  database: "naturalschool"
  username: "root"
  password: "password"
  pool-settings:
    maximum-pool-size: 10
    connection-timeout: 30000

academic-settings:
  general-settings:
    enable-debug-logs: false
  default-start-class: 1
  default-start-stage: "SD"

exam-schedule:
  day-of-week: "SUNDAY"
  start-hour: 10
  end-hour: 16
```

---

## 🎖️ School Ranks Hierarchy (`SchoolRank`)

The internal ranks system controls command permissions and academic Stage boundaries:

| Rank Category | Internal Enum Ranks |
| :--- | :--- |
| **School Management** | `KETUA_YAYASAN`, `WAKIL_KETUA_YAYASAN`, `KEMENTERIAN_PENDIDIKAN_IT`, `PENGAWAS_SEKOLAH` |
| **Administrative Staff** | `KEPALA_SEKOLAH`, `WAKEPSEK_KURIKULUM`, `WAKEPSEK_SARPRAS`, `KOMISI_DISIPLIN` |
| **Helper Staff** | `KEPALA_TU`, `GURU_TETAP` (Wali Kelas), `GURU_BK`, `GURU_HONORER` |
| **Student (SMA)** | `SMA_12`, `SMA_11`, `SMA_10` |
| **Student (SMP)** | `SMP_9`, `SMP_8`, `SMP_7` |
| **Student (SD)** | `SD_6`, `SD_5`, `SD_4`, `SD_3`, `SD_2`, `SD_1` |
| **Default** | `NONE` (Unregistered Profile) |

---

## 💻 Commands Reference

All commands support smart tab completion and MiniMessage text formatting.

### 👤 Student / Member Commands
* **Base Command**: `/school`

| Command Sub-Node | Description |
| :--- | :--- |
| `/school info` | Opens the GUI profile interface showcasing player details. |
| `/school exam` | Opens the dynamic core Portal Gateway UI. |
| `/school help` | Displays list of student subcommands. |

### 🛠️ Administrative Commands
* **Base Command**: `/naturalschool` (Aliases: `/nschool`, `/ns`)
* **Permission**: `naturalschool.admin` (Bypassed if cached rank is `KETUA_YAYASAN` or `WAKIL_KETUA_YAYASAN`).

| Command Sub-Node | Description |
| :--- | :--- |
| `/ns reload` | Safely reloads configurations and database connection pools. |
| `/ns info <player>` | Displays administrative academic profile details for a player. |
| `/ns setrank <player> <rank>` | Mutates player school rank and saves to database asynchronously. |
| `/ns setclass <player> <1-12>` | Updates student academic class level. |
| `/ns setstage <player> <SD\|SMP\|SMA>` | Updates student academic stage level. |
| `/ns nis register <player>` | Generates a 10-digit NIS registration number for a player. |
| `/ns nis unregister <player>` | Completely unregisters a student's NIS (with a 15s confirmation window). |
| `/ns semester info` | Shows the active global semester and academic year. |
| `/ns semester end` | Programmatically triggers async semester rotation. |
| `/ns semester reset` | Resets global semesters to sync with the real-life calendar. |
| `/ns exam open` | Force opens the exam portal (bypassing time schedules). |
| `/ns exam close` | Forces the exam portal to close. |
| `/ns exam message <msg>` | Sets the custom portal closed announcement description. |

---

## 📈 PlaceholderAPI Integration

Expose academic student variables to external scoreboard, chat, and tab plugins:

* **Prefix**: `%naturalschool_<placeholder>%` (Resolved from memory cache to prevent thread block).

| Placeholder | Description | Example Output |
| :--- | :--- | :--- |
| `%naturalschool_rank%` | Formatted rank string with prefix configurations | `§c§lKetua Yayasan` |
| `%naturalschool_class%` | Player's academic class number (1-12) | `10` |
| `%naturalschool_stage%` | Player's academic stage (SD/SMP/SMA) | `SMA` |
| `%naturalschool_nis%` | Student registration number | `1002090626` |

---

## 📦 Compilation & Setup

1. Ensure **Java 21** and **Maven** are installed.
2. Compile and package the shaded bundle:
   ```bash
   mvn clean package
   ```
3. Locate `NaturalSchool.jar` (or shaded artifact) in the `/target` directory and deploy to your server's `plugins/` directory.
