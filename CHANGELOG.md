# Changelog

All notable changes to the NaturalSchool project will be documented in this file.

## [1.7.3] - 2026-06-20
### Added
- **Fine-grained Admin Permissions**: Replaced blanket `naturalschool.admin` with specific permission nodes for each administrative subcommand group (`general`, `rank`, `setclass`, `setstage`, `nis`, `gui`, `semester`, `exam`, `class`), structured hierarchically under a new wildcard node `naturalschool.*`.
- **Dynamic Help Menu Filter**: Auto-filters administration help descriptions so players only see commands they have authority to execute.
- **Smart Permission-Aware Autocomplete**: Restricts command tab-completions so players can only see and execute subcommands they possess permission nodes for.
- **Styled Access-Denied Warning**: Introduced a new styled permission-denied warning message utilizing a visual red prefix (`NaturalSchool » You don't have permission to perform this command!`).

### Changed
- **Version Bump**: Elevated system version to `1.7.3` in `pom.xml`, `plugin.yml`, and `UIManager.java`.

## [1.7.2] - 2026-06-19
### Added
- **Class Fund & Bank Subsystem**: Implemented `/class fund` (and `/class bank`) commands allowing students to pay weekly cash, view class balances, and view transaction history logs.
- **Unified Database consolidation**: Consolidated separate classroom-related tables (`nschool_classrooms`, `nschool_classroom_officers`, and `nschool_classroom_doors`) into a single table `nschool_class` with `officers` and `doors` serialized as JSON (clean column names without suffix).
- **Username Reference/Comparison**: Added and pre-saved username mapping inside `officers` JSON and `wali_kelas_name` in the database to prevent slow Bukkit OfflinePlayer resolution lookups.
- **Vault API Support**: Hooked into Vault API to withdraw/deposit player funds during cash payments, withdrawals, and fines.
- **Class Hub GUI (Java & Bedrock)**: Implemented cross-platform `/class gui` with tabbed navigation: Info, Fund, Struktur, and Siswa.
- **In-GUI Red Error Banners**: Re-renders form views with a colored error header when an error occurs instead of closing or printing to chat.

### Changed
- **Version Bump**: Elevated system version to `1.7.2` in `pom.xml`, `plugin.yml`, and `UIManager.java`.

## [1.7.1] - 2026-06-19
- **Database-Driven Prefix System**: Migrated ranks, class levels, and roles configuration from `rankprefix.yml` into a single, unified database table `nschool_prefixes` with composite primary keys `(target_type, target_key)`.
- **Automatic Prefix Seeding**: Automatically seeds database `nschool_prefixes` table with default configurations from `rankprefix.yml` on first run if the table is empty.
- **Universal Color Code Support**: The prefix parser now supports both legacy codes (`&` / `§`) and Kyori's MiniMessage formatting styles dynamically.
- **Rank Management Command**: Replaced `/ns setrank` with a modernized administrative `/ns rank` command, supporting:
  - `/ns rank set <player> <rank>` - Set internal player rank.
  - `/ns rank list` - List prefixes and ranks from the database.
  - `/ns rank update <RANK|CLASS|ROLE> <key> <prefix...>` - Update prefixes directly in the database asynchronously and refresh the memory cache.
- **Smart Tab Completions**: Implemented smart tab completion suggestions for all `/ns rank` subcommands and their respective parameters.

### Changed
- **Version Bump**: Bumped system version to `1.7.1` in `pom.xml`, `plugin.yml`, and `UIManager.java`.

## [1.7.0] - 2026-06-19
### Added
- **Classroom Organization Subsystem**: Implemented complete Classroom Module managing classroom details, organizational hierarchies, spatial bounds, block-door toggling, custom formatting, and visual transition feedback.
- **Database Tables**: Introduced three new relational tables: `nschool_classrooms` (master classroom data), `nschool_classroom_officers` (student officer roles: KETUA, WAKIL, SEKRETARIS, BENDAHARA, ANGGOTA), and `nschool_classroom_doors` (block door coordinates).
- **Class Chat Formatter**: Added a custom class chat formatter supporting MiniMessage styles configured dynamically in `config.yml` (`class-settings.chat-format` and `class-settings.chat-format-norank`) and rank labels in `rankprefix.yml`. Features `/class chat` channels, `/class chat <message>`, and `/class chat norank` mode.
- **Spatial Area Transition Logs**: Added coordinate checks via `PlayerMoveEvent` that trigger visual enter/exit text feedback when a player crosses a classroom's bounding box.
- **Dynamic Block Doors Toggling**: Automatically sets blocks inside registered classroom door boundaries to `Material.AIR` when a session starts, and closes doors with `Material.TINTED_GLASS` when the session concludes.
- **Classroom Manager GUI Panel**: Created visual managers (`ClassroomManagerGui`) for both Java (Paper Dialog API) and Bedrock (Geyser Cumulus CustomForm) to configure classroom numbers, assign Wali Kelas, and define bounds from active WorldEdit selections.
- **WorldEdit & LuckPerms Reflection Integrations**: Removed compile-time dependencies on soft-depends. WorldEdit/FAWE selections and LuckPerms player prefixes are retrieved at runtime via safe reflection hooks.
- **Version Bump**: Elevated the project version to `1.7.0` in `pom.xml`, `plugin.yml`, and `UIManager.java`.

## [1.6.8] - 2026-06-11
### Changed
- **Exam Portal Dropdown Selector**: Consolidated subject selection into a single dropdown/multiple-choice button in the Exam Portal. Subjects and status legends are now printed as plain text list lines.
- **Portal Warn Banners**: Positioned configuration and state errors as warning labels at the top of the Exam Portal dialog/form layout.
- **Student Profile Info Relocation**: Relocated student name, NIS, and class info display out of the main portal view to the pre-exam landing screen.
- **API Corrections**: Fixed Paper Dialog API compatibility issues by passing the active state flag in OptionEntry creation, utilizing `view.getText()` for values query, and resolving builder compilation bounds.
- **Version Bump**: Bumped version to `1.6.8` in `pom.xml`, `plugin.yml`, and `UIManager.java`.

## [1.6.7.1] - 2026-06-10
### Changed
- **Version Bump**: Bumped version to `1.6.7.1` in `pom.xml`, `plugin.yml`, and UIManager's `GUI_VERSION` constant.
- **Security Hardening**: Applied memory visibility thread-safety on `ExamManager`, implemented API Key & IP verification on REST webhook endpoints, added unique structural index migrations on student attempts, and established immutable student parameter snapshots in `ExamSession` to prevent mid-exam property drift.

## [1.6.7] - 2026-06-10
### Added
- **Fallback Notification Layout**: Added fallback error interfaces (`showDatabaseErrorJava`/`showDatabaseErrorBedrock`) to display alerts when database connections fail, preventing player frustration.

### Changed
- **Atomic Submission Lock (v1.6.7)**: Implemented atomic submission checks in `ExamGui.java` utilizing a server-side `submittingPlayers` lock to block duplicate database transaction dispatches.
- **Robust Exception Propagation**: Modified `DatabaseManager.java` to rethrow caught `SQLException` instances as `RuntimeException` back to the async task pipeline.
- **Liveness Guard Validation**: Added offline validations (`!player.isOnline()`) on Bedrock form closed handlers to prevent recursive payload loops.
- **Stale Session Eviction Hook**: Linked `PlayerQuitEvent` and `PlayerKickEvent` listeners to evict cached student session data on exit, resolving RAM leaks.

## [1.6.6] - 2026-06-10
### Added
- **Semester Break Time Interceptor**: Implemented chronological checks in both Java and Bedrock subject click listeners to block players from starting UTS/UAS attempts during global break windows (`is_semester_break` flag).
- **Macro-Gatekeeper Security Hardening**: Integrated macro-gatekeeper checks in `openExamPortalJava`/`openExamPortalBedrock` that abort routing to UTS/UAS sub-menus if `portal_semester_status` is not `"OPEN"`, displaying a dedicated notice Dialog/Form.
- **Dynamic Live Whitelist Statuses**: Replaced static time config button labels with dynamic whitelist evaluation statuses: `[Sudah Selesai]`, `[Aktif] - (Sedang Berlangsung)`, and `[Tidak Aktif] - (Belum Dimulai / Selesai)`.
- **Version bump**: Elevated the system version to `1.6.6` in [pom.xml](file:///c:/Users/ThinkPad/Documents/NaturalSMP/plugin/NaturalSchool/pom.xml) and [UIManager.java](file:///c:/Users/ThinkPad/Documents/NaturalSMP/plugin/NaturalSchool/src/main/java/id/naturalsmp/naturalSchool/ui/UIManager.java).

## [1.6.5] - 2026-06-10
### Added
- **Centralized Single-Engine State-Passing Portal**: Introduced a new `/school exam` main menu UI mapping to `openExamPortal()` that routes to specific sub-categories (`UH`, `UTS`, `UAS`) through a single unified compiler [openPortalUjian](file:///c:/Users/ThinkPad/Documents/NaturalSMP/plugin/NaturalSchool/src/main/java/id/naturalsmp/naturalSchool/exam/ExamGui.java) in [ExamGui.java](file:///c:/Users/ThinkPad/Documents/NaturalSMP/plugin/NaturalSchool/src/main/java/id/naturalsmp/naturalSchool/exam/ExamGui.java).
- **Asynchronous Attempt Pre-Fetching**: Added [getAttemptedPackets](file:///c:/Users/ThinkPad/Documents/NaturalSMP/plugin/NaturalSchool/src/main/java/id/naturalsmp/naturalSchool/database/DatabaseManager.java#L649-L666) in `DatabaseManager` to query all of a player's previous attempts in a single async database call, optimizing layout compilation on menu load.
- **Dynamic Subject List Rendering**: Generates button listings for all 7 official subjects with dynamic status legends (`[Sudah Selesai]`, `[Aktif]`, `[Tidak Aktif]`) and schedule hours formatted directly into their text labels.
- **Bedrock Button Form Content Cleaners**: Introduced `cleanBedrockText()` helper in `ExamGui` to sanitize choice buttons by removing raw escape codes (`§`) and circle symbols (`○`, `●`), eliminating rendering anomalies.
- **Core Cache State Keys**: Seeded and synchronized new state variables in `nschool_core_state` (`active_uh_packets`, `current_active_semester_packets`, `portal_semester_status`, `is_semester_break`) inside the SQLite/MySQL engine and the `exams.json` cache mapping.

### Changed
- **Unified Validation Interceptors**: Modified subject clicks to evaluate three distinct safety scenarios in sequence:
  - *Scenario 1 (TIDAK AKTIF)*: Refresh portal instantly injecting a colored error message (`§c[!] Error: Mata pelajaran tersebut sedang tidak aktif!`) at the top of the menu layout.
  - *Scenario 2 (TIDAK ADA SOAL)*: Refresh portal instantly injecting warning text (`§e[!] Peringatan: Tidak ada soal pada mata pelajaran tersebut! (Hubungi Pengawas)`) on missing questions cache.
  - *Scenario 3 (SUDAH MENGERJAKAN)*: Force-close the active inventory to avoid spoofing and display a standalone Dialog (Java) / Modal (Bedrock) displaying closure notification.
- **Maven Version Bump**: Bumped the plugin artifact version to `1.6.5` in [pom.xml](file:///c:/Users/ThinkPad/Documents/NaturalSMP/plugin/NaturalSchool/pom.xml) and [UIManager.java](file:///c:/Users/ThinkPad/Documents/NaturalSMP/plugin/NaturalSchool/src/main/java/id/naturalsmp/naturalSchool/ui/UIManager.java).
- **Dynamic Subject Parsing**: Extracted mapel ID references dynamically from packet string prefixes (e.g. `5_1_UH` resolves to Subject ID `5`), deprecating the old `subject_id` DB column.

### Deprecated
- **Deprecated Columns & Keys**: Deprecated `subject_id` field in the bank questions schema and `active_packet_ids` key in core states.

## [1.6.4] - 2026-06-09
### Added
- **Production Data-Driven Exam & E-Rapor Subsystem**: Implemented dynamic packet-based exam architecture (`1.6.4`) powered by `exams.json` local cache and centralized database tables.
- **Anti-Retake Shield**: Added strict attempt checking (`nschool_student_exam_attempts` database table check) to prevent players from re-taking completed exams.
- **Dynamic Grading & E-Rapor Upsert**: Added auto-evaluation on submit, recalculation of average UH scores, and upserting into the `nschool_student_rapor` table with standard weights (40% UH, 30% UTS, 30% UAS) and letter grade assignments (A/B/C/D).
- **Bedrock Layout Refinements**: Stripped formatting codes and decorative symbols from Geyser/Floodgate Bedrock buttons to prevent blank rendering bugs.

## [1.6.3] - 2026-06-08
### Changed
- **GUI Version Command**: Fixed `/ns gui version` to display the general GUI version reference from `UIManager.GUI_VERSION` instead of the old `ExamGui.GUI_VERSION`.
- **Reload Command**: Integrated exam caching and webhook HTTP server reloading into `/ns reload` (`plugin.getExamManager().reload()`).
- **Version Bump**: Upgraded project version to `1.6.3` in `pom.xml` and `UIManager.java`.

## [1.6.2] - 2026-06-08
### Added
- **Data-Driven Exam Subsystem**: Replaced temporary hardcoded exam layouts with a dynamic system reading from local cache `exams.json` and MySQL tables (`nschool_exam_questions` and `nschool_core_state`).
- **HTTP Webhook Server**: Integrated an internal HTTP server (listening on port 8080 `/school/exam/update`) to automatically sync and update the local `exams.json` cache on version bumps.
- **Relocated Exam Subsystem**: Moved all Exam subsystem components (`ExamGui`, `ExamManager`, `ExamQuestions`, `ExamSession`, and `ExamVariantsGui`) into a dedicated package `id.naturalsmp.naturalSchool.exam`.
- **Database Seeding**: Added tables initialization and default data seeding for `portal_status` and `exam_version` in `DatabaseManager.java`.

### Removed
- **Duplicate Cleanups**: Deleted duplicate legacy classes in the `ui` and `ui/gui` packages to clean up compilation references.

## [1.6.1] - 2026-06-06
### Added
- **Mandatory Choice Validation**: Players are now blocked from advancing to the next question if they have not selected an answer. A red warning message is displayed directly under the question.
- **Pre-Exam Back Navigation**: Enabled the "Sebelumnya" button on the first question to return the player back to the pre-exam information landing screen, preventing them from being forced into the exam.

### Changed
- **Exam Navigation Styling**: Styled the "Selanjutnya" button as bold gold (orange) and the "Sebelumnya" button as bold dark green on all question screens, using Unicode escapes (`\u00A7`) to ensure reliable color rendering on Bedrock.
- **Exam Navigation Layout**: Realigned the navigation buttons. Positioned "Selanjutnya" (Berikutnya) as the bottom primary exit action and "Sebelumnya" at the end of the choice grid on Java Dialog, and placed "Sebelumnya" above "Selanjutnya" at the bottom of Bedrock SimpleForm. Removed empty/spacer buttons from Complex Multiple Choice layouts.

## [1.6.0] - 2026-06-06
### Added
- **10 Questions Exam Flow**: Revamped the entire `/school exam` subsystem from a 3-question sequence to a full 10-question sequence containing 6 Multiple Choice questions, 2 True/False statements, and 2 Complex Multiple Choice questions (multi-select checklist).
- **Pre-Exam Information Screen**: Introduced a pre-exam landing interface for both Java and Bedrock displaying player details (Username, NIS), subject name, and total questions, with "Lanjut" and "Kembali ke Portal" buttons.
- **Stateful Answer Selections**: Answer buttons no longer automatically advance. Selected choices now display as green bold with a suffix of `(Dipilih)`.
- **Navigation Controls**: Added "Sebelumnya" and "Selanjutnya" navigation controls on every question to allow reviewing previous answers.
- **Exam Lock Mechanism**: Disabled ESC/Escape closing for Java dialogs and forced Geyser form re-opens for Bedrock clients once the exam is started.

### Changed
- **Bedrock UI Compliance**: Enforced Bedrock Geyser Form button count limit rules. Bedrock's exam portal and final confirmation interfaces (dropdown-based) strictly use at most 1 button.
- **Platform Code Restructuring**: Relocated Java and Bedrock dropdown GUI methods to their platform-specific Java Edition and Bedrock Edition code sections in `ExamGui.java`.

### Removed
- **Deleted Prototype Subcommands**: Cleaned up and deleted the legacy `/school testexam` command execution branch, help texts, tab completions, and variants from `SchoolCommand`, `UIManager`, `BedrockHandler`, and GUI factories.
- **Merged Standalone GUIs**: Removed `DropdownJavaGui.java` and `DropdownBedrockGui.java` completely, merging their dropdown portal logic into `ExamGui.java`.

## [1.5.6] - 2026-06-06
### Added
- **Java & Bedrock Dropdown Portal**: Added dropdown selector version of Exam Portal using Paper Dialog API's `SingleOptionDialogInput` for Java Edition and Geyser CustomForm's `dropdown` for Bedrock Edition.
- **Separate Dropdown GUIs**: Created new `DropdownJavaGui` and `DropdownBedrockGui` classes to replicate the dropdown portal from scratch.

## [1.5.5] - 2026-06-06
### Changed
- **Java Edition — Portal Subject Buttons Cleanup**: Removed color tags (`<aqua>...</aqua>`) from Java Edition exam portal subject buttons to display them with default client styling.

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
