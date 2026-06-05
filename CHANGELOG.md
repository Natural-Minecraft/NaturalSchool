# Changelog

All notable changes to the NaturalSchool project will be documented in this file.

## [1.3.3] - 2026-06-05
### Changed
- Removed `<font:uniform>` tag wrappers from `DialogFormatter` to revert to standard/default Minecraft font rendering for Java Dialogs.

## [1.3.2] - 2026-06-05
### Changed
- Converted Bedrock onboarding Step 1 and Step 2 screens from `SimpleForm` to `CustomForm` utilizing a toggle switch for the cinematic cutscene offer.

## [1.3.1] - 2026-06-05
### Added
- Implemented the complete **3-Step Onboarding UI Subsystem** for first-time/unregistered players.
  * *STEP 1*: Welcome notice showing player info (aligned via DialogFormatter for Java).
  * *STEP 2*: Introductory cinematic offer dialog (confirms choice to watch or skip).
  * *STEP 3*: Terms of Service and Rules agreement screen (incorporates native checkboxes/toggles and external web links).
- Added `/naturalschool gui welcome <player>` administrative command to manually trigger the onboarding welcome flow, with tab completions.
- Integrated automated login hooks in `PlayerListener` that freezes unregistered players (where NIS is null) and automatically triggers the onboarding flow after a 1-second delay.

### Fixed
- Fixed a startup crash (`NoClassDefFoundError: org/geysermc/cumulus/form/Form`) by refactoring `UIManager` to lazily instantiate `BedrockFormFactory` via an abstract `BedrockHandler` interface using reflection, ensuring that Geyser/Floodgate classes are not resolved when Floodgate is missing.
- Updated `plugin.yml` to include `floodgate` and `Geyser-Spigot` under `softdepend` to guarantee proper plugin initialization order when they are present.

## [1.3.0] - 2026-06-05
### Added
- Created the **Unified UI Subsystem** to route player interface rendering dynamically by connection platform.
- Implemented `DialogFormatter` with a pixel-perfect auto-alignment engine (`alignLeft`) using uniform monospaced font tag wrappers.
- Created cross-platform `UIManager` routing Java players to native client-side Dialogs and Bedrock players to Cumulus Simple/Custom Forms.
- Added `JavaDialogFactory` using Paper's native experimental Dialog API and `BedrockFormFactory` using Geyser/Floodgate's Cumulus Forms API.
- Integrated the subsystem into `NaturalSchool`'s startup initialization and exposed `getUiManager()`.

## [1.2.0] - 2026-06-05
### Added
- Implemented a comprehensive **NIS (Nomor Induk Siswa) Management Subsystem**.
- Added database method `getRegisteredNisCount()` to calculate sequence numbers asynchronously.
- Implemented special 10-digit NIS Generation Engine (`1` + `3-digit sequence` + `DDMMYY` formatted date).
- Created subcommands under `/naturalschool nis` (`register`, `unregister`, `set`, `show`, `help`) with Adventure MiniMessage styling.
- Integrated a 15-second double execution confirmation cache map for `/ns nis unregister`.
- Added dynamic smart tab completions for all NIS subcommands, online players, and template custom inputs.

### Changed
- Configured default first-join profile parameters to NIS = `null`, Stage = `"NONE"`, Class = `0`, and Rank = `SchoolRank.NONE` (temporary/unregistered state).
- `/ns nis set` dynamically converts unregistered players (NULL NIS) into registered ones, automatically mapping them to SD Class 1.

## [1.1.0] - 2026-06-05
### Added
- Created `StudentStageChangeEvent` custom Bukkit event to notify when player academic stage changes.
- Added `setPlayerStage` to `NaturalSchoolAPI` and `NaturalSchoolAPIImpl`.
- Implemented **Rank Override Locks** for staff members (Staff, Helper, Management types). Staff academic stage and class remain locked and protected from standard progress.
- Added auto-mapping in `setPlayerRank` that unlocks and parses student ranks (e.g. `SMP_7`) back into their respective stage and class if a staff member is demoted.
- Added `username` column to the master database table `nschool_students` positioned right after `uuid`.
- Populated `username` from real-time player name with fallback to "Unknown" when profile is created for the first time.

### Changed
- Refactored master database table name to `nschool_students` (previously `naturalsmp_students`).
- Streamlined database structure strictly to 7 columns. Removed `practical_passed` and `temporary_grade` columns.
- Removed `/naturalschool setpractical` subcommand and tab completion.
- Updated `/naturalschool info` command layout to display Username right below UUID, and removed practical exam status and temporary grade indicators.
- Changed `loadProfile` database query to throw `SQLException` rather than catching it, enabling `ProfileManager` to handle database offline scenarios.
- Implemented player kick guardrail upon login if the database fails to load their profile to prevent profile reset and data loss.
- Resolved asynchronous database write race conditions by tracking active database writes in a `pendingSaves` CompletableFuture map. Rapid player rejoin attempts will block and wait for pending writes to finish.
- Set SQLite connection pragmas (`busy_timeout = 5000`, `journal_mode = WAL`, `synchronous = NORMAL`) to solve database lock contention (`SQLITE_BUSY`).
- Removed static `permission: naturalschool.admin` restriction from `plugin.yml` for `/naturalschool` command to allow in-game ranks (`KETUA_YAYASAN`/`WAKIL_KETUA_YAYASAN`) to be processed dynamically by the Java executor.

### Removed
- Removed `isPracticalPassed` and `setPracticalPassed` methods from `NaturalSchoolAPI` and `NaturalSchoolAPIImpl`.

## [1.0.5] - 2026-06-05
### Added
- Implemented Developer API interface (`NaturalSchoolAPI`) and concrete class (`NaturalSchoolAPIImpl`).
- Registered `NaturalSchoolAPI` into Bukkit's `ServicesManager` for other plugins to depend on.
- Created `NaturalSchoolProvider` static class helper.
- Added custom Bukkit events (`StudentRankChangeEvent`, `StudentClassChangeEvent`, `StudentPracticalToggleEvent`) implementing `Cancellable`.
- Integrated events inside set commands and blocked changes if external plugins cancel the event.

## [1.0.4] - 2026-06-05
### Added
- Upgraded rank prefix system to support Kyori Adventure's MiniMessage Gradients, Hex colors, and RGB colors.
- Integrated ItemsAdder font images and icons (e.g. `:ia_owner_icon:`) dynamically using reflection to prevent `NoClassDefFoundError` when ItemsAdder is absent.
- Introduced `rankprefix.yml` config file for rank customization.

## [1.0.3] - 2026-06-05
### Added
- Integrated PlaceholderAPI with `NaturalSchoolExpansion`.
- Supported placeholders: `%naturalschool_rank%`, `%naturalschool_class%`, `%naturalschool_stage%`, `%naturalschool_nis%`.

## [1.0.2] - 2026-06-05
### Added
- Created `/naturalschool` command, aliases (`/nschool`, `/ns`), subcommands (`reload`, `info`, `setrank`, `setclass`, `setstage`, `setpractical`), and dynamic tab completion.
- Implemented `SchoolRank` hierarchy system.

## [1.0.0] - 2026-06-05
### Added
- Core database infrastructure supporting SQLite and MySQL (using HikariCP connection pool).
- Thread-safe online player caching using `ConcurrentHashMap`.
- Player listener handling asynchronous login profile loading and strict disconnect cache eviction.
- Added `config.yml` settings.
