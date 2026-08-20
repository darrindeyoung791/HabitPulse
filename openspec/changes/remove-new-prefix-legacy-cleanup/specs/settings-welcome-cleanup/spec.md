## ADDED Requirements

### Requirement: Dead legacy settings/welcome code removed
The system SHALL delete all legacy settings/welcome code that is no longer reachable from any live entry point. This includes `SettingsActivity` (old segmented settings that only forwards to the new screens), `ReminderSettingsActivity`, and `ui/screens/ReminderSettingsScreen`. Their `AndroidManifest.xml` registrations MUST also be removed.

#### Scenario: No dead legacy references remain
- **WHEN** searching the entire `app/src` tree
- **THEN** no reference to `SettingsActivity` (old), `ReminderSettingsActivity`, or `ReminderSettingsScreen` remains, and no manifest entry registers them

#### Scenario: App still builds after removal
- **WHEN** running `./gradlew :app:assembleDebug`
- **THEN** the build succeeds with no unresolved symbol errors

### Requirement: New-prefix names dropped
All live settings and welcome classes, files, and references SHALL be renamed by removing the `New` prefix. This applies to activities (root package), screens (`ui/screens/settings/`), the welcome screen (`ui/screens/welcome/NewWelcomeScreen.kt`), private content composables inside the activity files, manifest activity names, and all referencing call sites.

#### Scenario: No New-prefix identifiers remain in source
- **WHEN** searching the `app/src/main/java` tree for `NewSettings*` and `NewWelcome*`
- **THEN** no matches are found

#### Scenario: Manifest activity names match renamed classes
- **WHEN** reading `AndroidManifest.xml`
- **THEN** every settings/welcome activity's `android:name` points to a class that exists after the rename

#### Scenario: All call sites compile
- **WHEN** running `./gradlew :app:assembleDebug`
- **THEN** the build succeeds, confirming `LauncherActivity`, `HabitPulseNavGraph`, `AIChatActivity`, and `AIChatScreen` reference the renamed classes correctly

### Requirement: Docs stay in sync
Project documentation that references the renamed or deleted files SHALL be updated to match: `AGENTS.md`, `QWEN.md`, `README.md`, and `devdoc/readme/README_EN-US.md`. `AGENTS.md` and `QWEN.md` MUST remain in sync with each other per project convention.

#### Scenario: Docs reference current names
- **WHEN** reading the structure trees and feature notes in `AGENTS.md` and `QWEN.md`
- **THEN** they reference the renamed class/file names and no longer mention deleted legacy files

#### Scenario: Unit tests pass
- **WHEN** running `./gradlew :app:testDebugUnitTest`
- **THEN** all unit tests pass, confirming no behavior regression from the refactor