# Changelog

All notable changes to the NaturalSchool project will be documented in this file.

## [1.5.4] - 2026-06-06
### Changed
- **Bedrock — Portal & Confirmation Layout Switch**: Swapped Bedrock exam portal layout to `SimpleForm` (vertical buttons list) and bedrock exam confirmation layout to `CustomForm` (with a dropdown selector actions menu).

## [1.5.3] - 2026-06-06
### Added
- **GUI version command**: Introduced command `/ns gui version` to print active GUI version for diagnostics.
- **GUI Versioning**: Added static `GUI_VERSION` field to `ExamGui.java`.

### Changed
- **Java Edition — Choice Buttons Cleanup**: Removed circle symbols (`○` / `●`) from question option buttons in Soal 1 and Soal 2.

## [1.5.2] - 2026-06-06
### Fixed
- **Bedrock — CustomForm Element Indices**: Corrected element index alignment for all Geyser/Cumulus `CustomForm` response handlers. Since every component (including static labels) counts as an element index in a `CustomForm` builder:
  - Fixed Exam Portal dropdown subject parsing index from `0` to `1` (skipping the top description label).
  - Fixed Exam 1, 3, 4, and 5 choice toggle response index offsets (shifting toggle indices from `0-3` to `1-4` or `1-2` to correctly skip the question label at index 0).
  - Implemented dynamic toggle index calculation in Registration Step 3 to dynamically account for optional warning labels without hardcoding offsets.

## [1.5.1] - 2026-06-06
### Changed
- **GUI Code Refactor**: Split the monolithic `JavaDialogFactory` and `BedrockFormFactory` classes. Reorganized the GUI codebase into 5 self-contained, feature-oriented GUI classes containing both Java Dialogs and Bedrock Cumulus Forms in `id.naturalsmp.naturalSchool.ui.gui`:
  - `RegistrationGui` (Onboarding/registration step flows)
  - `ProfileGui` (Student profile UI)
  - `StaffPanelGui` (Admin staff panel UI)
  - `ExamVariantsGui` (Experimental creative exam variant layouts)
  - `ExamGui` (Main /school exam portal, closed notification, sequential questions, and confirmation flows)
- **Routing Update**: Updated `UIManager` and `BedrockHandlerImpl` to instantiate and delegate UI rendering to these new individual GUI classes.
- **Cleanup**: Deleted legacy factory classes to keep the codebase clean.

## [1.5.0] - 2026-06-06
### Changed
- **Bedrock — Exam Choice Buttons**: Selected answer buttons now display with `§a` green color prefix instead of a text label ("(Terpilih)"/"DIPILIH"). Unselected buttons remain plain text with no color — applies to Soal 1 (A/B/C/D), Soal 2 (BENAR/SALAH), and Soal 3 checklist (1./2./3.).
- **Bedrock — No symbols on choice buttons**: Removed all non-alphanumeric symbols from button labels; only `§a` Minecraft color code is used for the selected-state indicator.

## [1.4.9] - 2026-06-06
### Fixed
- **Bedrock — openExamClosed**: Removed Minecraft legacy `§c§l` color codes from the closed-portal form content; Cumulus API does not render `§`-style formatting and the raw escape characters were being displayed as literal text on Bedrock clients.
- **Bedrock — Soal 3 Checklist**: Replaced `[X]` / `[ ]` ASCII bracket symbols with pure alphanumeric prefixes (`DIPILIH 1.` / `1.`) to prevent Bedrock from switching to the thin/glitched unicode font layout.

## [1.4.8] - 2026-06-06
### Added
- Implemented Notice dialogs for closed exam portals ("Portal Sedang ditutup!") displaying custom administrator messages on both Java and Bedrock.
- Refactored Bedrock Edition's Portal Ujian layout to use a `CustomForm` dropdown menu instead of buttons.
- Refactored Bedrock's Soal 3 (Multiple Statement Checklist) to use a button-based `SimpleForm` checklist (using normal `[X]` / `[ ]` ASCII tags), removing dropdown menus and CustomForm limits.
- Added two distinct navigation buttons ("Berikutnya" and "Kembali ke Soal 2") for Bedrock's Soal 3.
- Standardized Bedrock buttons (MCQ, True/False, and Checklist) to use font normal, default colors, and clean ASCII/alphanumeric strings to prevent Bedrock from rendering thin/glitched custom fonts.

## [1.4.7] - 2026-06-05
### Added
- Improvised the Exam Subsystem UI for both Java Edition (Dialogs) and Bedrock Edition (Geyser/Floodgate Forms):
  - Java Edition: Integrated native item displays in the dialog body (`DialogBody.item(...)`) matching each exam stage.
  - Bedrock Edition: Refactored Soal 1 and Soal 2 from toggle-based `CustomForm` to button-based `SimpleForm` (A/B/C/D and BENAR/SALAH layouts) to align with Java's Kahoot-style quiz flow.
  - Stateful indicators: Dynamic title prefixes ("(Terpilih)") to visualize already selected answers on both Java dialogs and Bedrock forms.
  - Fixed and streamlined escape key prevention and back-navigation across all exam interfaces.

## [1.4.6] - 2026-06-05
### Added
- Added the `/school exam` Command to open the Portal Ujian dialog.
- Added administrative controls via `/ns exam <open|close|message>` to toggle the exam portal state and set a custom description.
- Implemented stateful navigation across a 3-question sequence (Multiple Choice, True/False, Multiple Statement) with a final confirmation card:
  - Added a "Kembali ke soal sebelumnya" button/toggle that remembers previous answers (stateful tracking).
  - Disabled the ESC escape key in all exam question dialogs.
  - Added automatic exam score calculation (correct and incorrect answers) showing up on submission.

## [1.4.5] - 2026-06-05
### Added
- Added five creative layout variants for the Exam Subsystem GUI via command `/ns gui <exam1|exam2|exam3|exam4|exam5> <player>`.
- Added input validation logic for multiple-choice checkbox dialogs (Exam 1 and Exam 3): choosing more than 1 option triggers a reopen loop with a bold red error warning ("Pilih hanya satu jawaban!").

## [1.4.4] - 2026-06-05
### Changed
- Replaced the temporary Java Edition chest inventory GUI quiz layout with the native Paper Dialog system.

## [1.4.3] - 2026-06-05
### Added
- Added temporary creative prototype Exam GUI quiz dialog for testing the Exam Subsystem. Works on both Java Edition and Bedrock Edition via command `/school testexam`.

## [1.4.2] - 2026-06-05
### Fixed
- Fixed race condition during global batch updates by updating online player caches first on the primary server thread and excluding their UUIDs from the async database update query.
- Added self-healing logic during profile loads to align out-of-sync student profile semesters with the global active semester.

## [1.4.1] - 2026-06-05
### Added
- Added `/ns semester reset` command to reset the active semester back to align with the real-life system clock calendar.

### Fixed
- Fixed the academic year month not advancing programmatically during `processSemesterEnd()` when force-rotating multiple times in the same real-life month.

## [1.4.0] - 2026-06-05
### Added
- Upgraded the database schema for `nschool_students` table (SQLite and MySQL) to include `current_semester` column with a non-destructive migration.
- Added `nschool_semester_log` logging table.
- Added Semester rotation engine (`SemesterManager.java`) that rotates semesters every 14 days and month-based academic years, performing asynchronous batch updates and logs.
- Added admin command `/ns semester <info|end>` to view status and trigger rotation.
- Updated `/school info` GUI to show semester and academic year information for both Java and Bedrock Edition.

## [1.3.6] - 2026-06-05
### Added
- Added `/school` command, specifically for registered student members. Running `/school` or `/school help` prints subcommand help, and running `/school info` opens the student profile GUI dialog.
- Updated Java profile Dialog and Bedrock profile Form to dynamically retrieve and display student details including Username, NIS, and Kelas + Jenjang (class and stage).

## [1.3.5] - 2026-06-05
### Fixed (Critical)
- **UIManager:** Fixed premature in-memory mutation bug — `StudentProfile` fields (NIS, rank, stage, class) are now only committed after the async database save succeeds. On save failure, the in-memory state is rolled back to prevent a corrupted cache.
- **UIManager:** Fixed NIS race condition — added an `AtomicBoolean` registration lock to prevent two simultaneous players from being assigned the same NIS sequence number.
- **BedrockFormFactory:** Fixed incorrect Cumulus toggle index calculation in Step 3. `asToggle(n)` is 0-indexed per toggle element only (not all elements). Removed the incorrect `offset` logic; indices are now always `0` and `1`.
- **PlayerListener:** Fixed unsafe `Bukkit.getPlayer(uuid)` call inside the async profile load thread. Player name is now captured on the main thread before the async task and passed as a parameter.
- **ProfileManager:** Updated `loadProfile` signature to accept `username` as a parameter (captured on main thread) instead of calling `Bukkit.getPlayer()` asynchronously.
- **PlayerListener:** Removed duplicate `onPlayerKick` event handler. Paper fires `PlayerQuitEvent` alongside `PlayerKickEvent`, so the previous implementation caused double profile saves on player kick.

### Fixed (Security)
- **UIManager:** Exception messages from database failures are no longer shown in player-facing chat. Internal errors are now logged server-side only; players receive a generic localized message.

### Changed (Optimization)
- **NaturalSchoolExpansion:** Replaced hardcoded version string `"1.2.0"` with `plugin.getDescription().getVersion()` so the PAPI expansion always reports the correct plugin version.
- **BedrockFormFactory:** Added proper `import id.naturalsmp.naturalSchool.profile.StudentProfile` declaration, removing the inline fully-qualified class name workaround.
- **ProfileManager:** Extracted `generateNis(int count)` as a canonical static utility method.
- **UIManager:** Removed duplicated NIS generation logic — now delegates to `ProfileManager.generateNis()`.
- **UIManager:** Extracted `MiniMessage.miniMessage()` as a static constant to avoid repeated static method calls.

## [1.3.4] - 2026-06-05
### Added
- Implemented real-time dynamic querying of player NIS and Status in onboarding Step 1 for both Java and Bedrock.
- Integrated a guardrail check in registration completion to ensure players with an existing NIS skip the registration sequence.
- Added a validation warning mechanism in Step 3 that redisplays the forms with a bold red warning at the top (Bedrock) or bottom (Java) if rules/ToS are not accepted.
- Restructured Step 3 dialogs to utilize a single submit button and removed the decline/exit toggle to ensure clean submission routing.

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
