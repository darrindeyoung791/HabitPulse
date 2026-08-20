## 1. Delete dead legacy code

- [x] 1.1 Delete `app/src/main/java/io/github/darrindeyoung791/habitpulse/SettingsActivity.kt` (old segmented settings; only forwards to new screens)
- [x] 1.2 Delete `app/src/main/java/io/github/darrindeyoung791/habitpulse/ReminderSettingsActivity.kt`
- [x] 1.3 Delete `app/src/main/java/io/github/darrindeyoung791/habitpulse/ui/screens/ReminderSettingsScreen.kt`
- [x] 1.4 Remove `.SettingsActivity` and `.ReminderSettingsActivity` registrations from `AndroidManifest.xml`
- [x] 1.5 Verify no remaining references to the deleted classes (grep `ReminderSettingsScreen|ReminderSettingsActivity`)

## 2. Rename NewWelcome to Welcome

- [x] 2.1 Rename `NewWelcomeActivity.kt` → `WelcomeActivity.kt`; rename class `NewWelcomeActivity`→`WelcomeActivity`, update `this@NewWelcomeActivity` self-refs
- [x] 2.2 Rename `NewWelcomeScreen.kt` → `WelcomeScreen.kt`; rename composable `NewWelcomeScreen`→`WelcomeScreen` in file and in `WelcomeActivity.kt` import/call
- [x] 2.3 Update `LauncherActivity.kt` (comment + `NewWelcomeActivity::class.java`) to `WelcomeActivity`
- [x] 2.4 Update `AndroidManifest.xml` `.NewWelcomeActivity` → `.WelcomeActivity`

## 3. Rename NewSettings activities and screens

- [x] 3.1 Rename `NewSettingsActivity.kt` → `SettingsActivity.kt` (class, content composable `NewSettingsHomeContent`→`SettingsHomeContent`, imports of `NewSettingsHomeScreen`); update references in `HabitPulseNavGraph.kt` and `AIChatScreen.kt`
- [x] 3.2 Rename `NewSettingsAIActivity.kt` → `SettingsAIActivity.kt`; update references in `HabitPulseNavGraph.kt` and `AIChatActivity.kt`
- [x] 3.3 Rename `NewSettingsAIEditActivity.kt` → `SettingsAIEditActivity.kt` (incl. `EXTRA_CONFIG_ID` const); update references in `SettingsAIActivity.kt`
- [x] 3.4 Rename `NewSettingsNotificationsActivity.kt` → `SettingsNotificationsActivity.kt`; update reference in `SettingsActivity.kt`
- [x] 3.5 Rename `NewSettingsTemplateActivity.kt` → `SettingsTemplateActivity.kt`; update reference in `SettingsNotificationsActivity.kt`
- [x] 3.6 Rename `NewSettingsGeneralActivity.kt` → `SettingsGeneralActivity.kt`; update reference in `SettingsActivity.kt`
- [x] 3.7 Rename `NewSettingsLanguageActivity.kt` → `SettingsLanguageActivity.kt`; update reference in `SettingsGeneralActivity.kt`
- [x] 3.8 Rename `NewSettingsFontScaleActivity.kt` → `SettingsFontScaleActivity.kt`; update reference in `SettingsGeneralActivity.kt`
- [x] 3.9 Rename `NewSettingsAboutActivity.kt` → `SettingsAboutActivity.kt`; update reference in `SettingsActivity.kt`
- [x] 3.10 Rename `NewSettingsDebugActivity.kt` → `SettingsDebugActivity.kt`; update reference in `SettingsAboutActivity.kt`
- [x] 3.11 Rename `NewSettingsDebugReminderActivity.kt` → `SettingsDebugReminderActivity.kt`; update reference in `SettingsDebugActivity.kt`
- [x] 3.12 Rename `NewSettingsDebugVibrationActivity.kt` → `SettingsDebugVibrationActivity.kt`; update reference in `SettingsDebugActivity.kt`
- [x] 3.13 Rename `NewSettingsScaffold.kt` → `SettingsScaffold.kt` (composable `NewSettingsScaffold`→`SettingsScaffold`); update all usages in `ui/screens/settings/`
- [x] 3.14 Rename screen files/composables in `ui/screens/settings/`: Home, AI, AIEdit, Notifications, Template, General, Language, FontScale, AboutDetail, Debug, DebugReminder, DebugVibration (`NewSettings*Screen`→`Settings*Screen`), updating each activity's import/call
- [x] 3.15 Update `AndroidManifest.xml` activity names (13 entries) to the renamed classes

## 4. Verify

- [x] 4.1 Grep `NewSettings|NewWelcome` across `app/src` — expect zero matches
- [x] 4.2 Grep deleted legacy names (`SettingsActivity::class` old, `ReminderSettings`) — expect zero matches
- [x] 4.3 Build: `./gradlew :app:assembleDebug` succeeds
- [x] 4.4 Tests: `./gradlew :app:testDebugUnitTest` passes

## 5. Docs sync

- [x] 5.1 Update `AGENTS.md` structure tree, feature bullets, and notes to renamed/deleted file names
- [x] 5.2 Update `QWEN.md` identically (must stay in sync with `AGENTS.md`)
- [x] 5.3 Update `README.md` and `devdoc/readme/README_EN-US.md` structure trees
- [x] 5.4 Final build+tests re-run after doc changes (docs only — quick sanity)