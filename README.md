# NaturalSchool

NaturalSchool is a high-performance core plugin for Paper servers (targeting Java Edition 1.21.1 and Java 17+) built with a Core-Infrastructure architectural pattern. It manages player academic student profiles smoothly with direct SQLite support and a MySQL connection pool powered by HikariCP.

## Key Features

- **Core-Infrastructure Pattern**: Decoupled modules. Configuration and Database act as global core components, while student player data runs under a separate caching manager module.
- **High-Performance Database Support**:
  - **SQLite**: Local database storage using `school.db` located inside the plugin's data folder.
  - **MySQL**: Connection pool initialization utilizing HikariCP to prevent connection leaks, connection timeouts, and optimized prepared statement caching.
- **Anti-Memory Leak Guardrails**:
  - **No Bukkit Player Objects Cached**: The cache strictly keys records using `java.util.UUID`.
  - **Thread-Safe Runtime Cache**: Online players' database records are stored in a managed `ConcurrentHashMap<UUID, StudentProfile>`.
  - **Asynchronous Join Processing**: Fetches profiles asynchronously from the database during the join lifecycle event.
  - **Strict Cache Eviction**: Saves profiles asynchronously and strictly clears the cache entry immediately on quit or kick events.
  - **No Ticking Drops**: All database (I/O) queries run on the asynchronous Bukkit Scheduler thread to maintain 20 TPS.

## Configuration (`config.yml`)

The plugin can be configured with MySQL server settings, pool limits, and default start parameters for new players.

```yaml
database:
  storage-type: "SQLITE" # SQLITE or MYSQL
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
```

## School Ranks

The plugin implements an independent internal rank system (`SchoolRank`), stored directly in the database (`rank` column) and managed through profiles.

* **Server Management**: `KETUA_YAYASAN` (Ketua Yayasan), `WAKIL_KETUA_YAYASAN` (Dewan Pembina), `KEMENTERIAN_PENDIDIKAN_IT` (Kemendikbud & IT), `PENGAWAS_SEKOLAH` (Pengawas Sekolah)
* **Staff Admin**: `KEPALA_SEKOLAH` (Kepala Sekolah), `WAKEPSEK_KURIKULUM` (Wakepsek Kurikulum), `WAKEPSEK_SARPRAS` (Wakepsek Sarpras), `KOMISI_DISIPLIN` (Komisi Disiplin)
* **Staff Helper**: `KEPALA_TU` (Kepala TU), `GURU_TETAP` (Wali Kelas), `GURU_BK` (Guru BK), `GURU_HONORER` (Guru Honorer)
* **Student Academic**: `SMA_12` to `SMA_10` (Siswa X-XII SMA), `SMP_9` to `SMP_7` (Siswa VII-IX SMP), `SD_6` to `SD_1` (Siswa I-VI SD)
* **Default**: `NONE` (Belum Terdaftar)

## Administrative Commands

The plugin provides a central command `/naturalschool` (aliases: `/nschool`, `/ns`) to manage student academic profiles.

* **Permission**: Requires `naturalschool.admin` OR a cached rank of `KETUA_YAYASAN` or `WAKIL_KETUA_YAYASAN`.
* **Text Formatting**: Messages utilize Paper's modern Adventure API (`MiniMessage`) for formatting.

| Command | Description |
| :--- | :--- |
| `/naturalschool reload` | Reloads `config.yml` from disk and safely refreshes database connections. |
| `/naturalschool info <player>` | Displays the full academic profile (including Rank) of a player (Online: cached, Offline: queried asynchronously from DB). |
| `/naturalschool setrank <player> <rank>` | Updates the player's school rank (saves to DB asynchronously). |
| `/naturalschool setclass <player> <1-12>` | Updates the player's academic class (saves to DB asynchronously). |
| `/naturalschool setstage <player> <SD\|SMP\|SMA>` | Updates the player's academic stage (saves to DB asynchronously). |
| `/naturalschool setpractical <player> <true\|false>` | Toggles the player's practical exam completion status. |

Smart tab completion is fully implemented, suggesting options dynamically based on the argument position.

## PlaceholderAPI Integration

The plugin integrates with PlaceholderAPI to expose student academic details. 

* **Identifier**: `naturalschool` (Syntax: `%naturalschool_<placeholder>%`)
* **Anti-Lag Guard**: All placeholder requests are resolved strictly from the memory cache. Direct database queries are never made inside request processes.

| Placeholder | Description | Output Example |
| :--- | :--- | :--- |
| `%naturalschool_rank%` | Returns the formatted legacy rank string from the hierarchy | `§c§lKetua Yayasan` |
| `%naturalschool_class%` | Returns the player's academic class number (1-12) | `10` |
| `%naturalschool_stage%` | Returns the player's academic stage | `SMA` |
| `%naturalschool_nis%` | Returns the student's registration number (or `-` if none) | `2026-0004` |

## Compilation & Installation

1. **Requirements**: Java 21+ and Apache Maven installed (since Paper 1.21.1 requires Java 21).
2. **Build package**:
   ```bash
   mvn clean package
   ```
3. Copy the compiled JAR `NaturalSchool.jar` from the `target` folder into your Paper server's `plugins` folder.
