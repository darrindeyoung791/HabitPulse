## Why

The settings & welcome features were rebuilt ("NewSettings*"/"NewWelcome*"), but the old implementations were only partially removed. The old `SettingsActivity` (and its `ReminderSettingsActivity`/`ReminderSettingsScreen` subtree) is now dead code: nothing launches it and it only redirects to the new screens. The `New` prefix no longer communicates anything and adds confusion across ~30 files.

## What Changes

- **BREAKING (internal)**: Delete legacy dead code:
  - `SettingsActivity.kt` (old segmented settings; never launched, only forwards to new screens)
  - `ReminderSettingsActivity.kt` + `ui/screens/ReminderSettingsScreen.kt` (only reachable from deleted `SettingsActivity`)
  - Manifest entries for `.SettingsActivity` and `.ReminderSettingsActivity`
- Rename all live `New*` classes to drop the prefix:
  - Activities (root package): `NewWelcomeActivity`→`WelcomeActivity`, `NewSettingsActivity`→`SettingsActivity`, `NewSettingsAIActivity`→`SettingsAIActivity`, `NewSettingsAIEditActivity`→`SettingsAIEditActivity`, `NewSettingsNotificationsActivity`→`SettingsNotificationsActivity`, `NewSettingsTemplateActivity`→`SettingsTemplateActivity`, `NewSettingsGeneralActivity`→`SettingsGeneralActivity`, `NewSettingsLanguageActivity`→`SettingsLanguageActivity`, `NewSettingsFontScaleActivity`→`SettingsFontScaleActivity`, `NewSettingsAboutActivity`→`SettingsAboutActivity`, `NewSettingsDebugActivity`→`SettingsDebugActivity`, `NewSettingsDebugReminderActivity`→`SettingsDebugReminderActivity`, `NewSettingsDebugVibrationActivity`→`SettingsDebugVibrationActivity`
  - Screens (`ui/screens/settings/`): `NewSettingsScaffold`→`SettingsScaffold`, `NewSettingsHomeScreen`→`SettingsHomeScreen`, `NewSettingsAIScreen`→`SettingsAIScreen`, `NewSettingsAIEditScreen`→`SettingsAIEditScreen`, `NewSettingsNotificationsScreen`→`SettingsNotificationsScreen`, `NewSettingsTemplateScreen`→`SettingsTemplateScreen`, `NewSettingsGeneralScreen`→`SettingsGeneralScreen`, `NewSettingsLanguageScreen`→`SettingsLanguageScreen`, `NewSettingsFontScaleScreen`→`SettingsFontScaleScreen`, `NewSettingsAboutDetailScreen`→`SettingsAboutDetailScreen`, `NewSettingsDebugScreen`→`SettingsDebugScreen`, `NewSettingsDebugReminderScreen`→`SettingsDebugReminderScreen`, `NewSettingsDebugVibrationScreen`→`SettingsDebugVibrationScreen`
  - Welcome: `NewWelcomeScreen`→`WelcomeScreen`
  - Private content composables inside each Activity file (`NewSettingsHomeContent`→`SettingsHomeContent`, etc.) follow the Activity rename
- Update all references: `AndroidManifest.xml` activity names, `LauncherActivity`, `HabitPulseNavGraph`, `AIChatActivity`, `AIChatScreen`, self-references/imports in renamed files
- Update docs to stay in sync: `AGENTS.md`, `QWEN.md`, `README.md`, `devdoc/readme/README_EN-US.md`

## Capabilities

### New Capabilities

- `settings-welcome-cleanup`: Codebase hygiene — remove dead legacy settings/welcome code and drop the `New` prefix from all live settings/welcome classes, files, and references.

### Modified Capabilities

<!-- No existing requirement-level specs change; this is a pure refactor. -->

## Impact

- Affected files: ~40 Kotlin files (13+2 deleted, ~30 renamed/edited), `AndroidManifest.xml`, 4 documentation files
- No runtime behavior, strings, resources, or DB schema changes
- No dependency changes
- Verification: `./gradlew :app:assembleDebug :app:testDebugUnitTest`