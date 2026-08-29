# HabitPulse - Project Context

## Project Overview

**HabitPulse** is an Android habit tracking application built with Kotlin and Jetpack Compose. The app helps users build and maintain daily habits through smart reminders and social supervision features.

> **⚠️ IMPORTANT — AI agents: When project structure, features, dependencies, or conventions change significantly, you MUST update BOTH `AGENTS.md` AND `QWEN.md` to keep them in sync.**

### Key Features
- **Habit Tracking**: Create, manage, and track daily habits with custom repeat cycles
- **Completion History**: Track every check-in with timestamps, local dates, and timezone support
- **Records & Analytics**: View completion history with date filtering and habit-specific filtering
- **Social Supervision**: Link supervisor contacts (email/phone) to habits for accountability; supports mixed email + phone contacts per habit
- **Multi-Select & Reorder**: Drag-and-drop reordering with batch delete functionality
- **Smart Search**: Search habits with instant filtering
- **Foreground Service**: Keep-alive service with boot auto-restart for reliability
- **Material Design 3**: Clean, modern UI following MD3 guidelines with dynamic colors
- **Responsive Layout**: Adaptive navigation (Bottom Bar, Rail, Drawer) based on screen size
- **Split-screen Support**: Multi-window support enabled
- **Predictive Back Gesture**: Android 13+ predictive back gesture support
- **Localization**: Chinese (Simplified/Traditional) and English (US/UK) support
- **Accessibility**: TalkBack support for all navigation elements

### Tech Stack
- **Language**: Kotlin
- **UI Framework**: Jetpack Compose with Material Design 3
- **Navigation**: Navigation Compose with custom animations and shared transitions
- **Database**: Room 2.8.4 (v5 schema)
- **Preferences**: DataStore for user settings, SharedPreferences for onboarding state
- **Foreground Service**: Android foreground service for keep-alive
- **Scheduling**: AlarmManager (planned)
- **Build System**: Gradle (Kotlin DSL)
- **Minimum SDK**: 26 (Android 8.0)
- **Target SDK**: 37

## Project Structure

```
HabitPulse/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/io/github/darrindeyoung791/habitpulse/
│   │   │   │   ├── MainActivity.kt              # Main entry point with NavHost
│   │   │   │   ├── SettingsActivity.kt       # Settings home (segmented list UI)
│   │   │   │   ├── SettingsAIActivity.kt     # Settings: AI config list
│   │   │   │   ├── SettingsAIEditActivity.kt # Settings: AI config add/edit
│   │   │   │   ├── SettingsNotificationsActivity.kt # Settings: notifications
│   │   │   │   ├── SettingsDebugReminderActivity.kt # Settings: debug reminders
│   │   │   │   ├── SettingsTemplateActivity.kt # Settings: notification template
│   │   │   │   ├── SettingsGeneralActivity.kt # Settings: general
│   │   │   │   ├── SettingsAboutActivity.kt  # Settings: about
│   │   │   │   ├── SettingsLanguageActivity.kt # Settings: language (app language switcher)
│   │   │   │   ├── SettingsDebugActivity.kt  # Settings: debug (hidden, 5-tap on version)
│   │   │   │   ├── SampleDataGenerator.kt    # Debug sample data generation (moved from legacy SettingsActivity)
│   │   │   │   ├── LauncherActivity.kt       # Launcher that routes to Welcome or MainActivity
│   │   │   │   ├── WelcomeActivity.kt        # Onboarding/welcome flow (4 steps)
│   │   │   │   ├── OpenSourceLicensesActivity.kt # Open source licenses display
│   │   │   │   ├── HabitPulseApplication.kt     # Application class with singleton init
│   │   │   │   ├── navigation/
│   │   │   │   │   ├── HabitPulseNavGraph.kt    # Navigation graph with animations
│   │   │   │   │   └── Route.kt                 # Route definitions
│   │   │   │   ├── data/
│   │   │   │   │   ├── model/
│   │   │   │   │   │   ├── Habit.kt             # Habit entity with Room annotations
│   │   │   │   │   │   ├── HabitCompletion.kt   # Habit completion record entity
│   │   │   │   │   │   ├── HabitStatus.kt       # Habit status enum + HabitWithStatus
│   │   │   │   │   │   ├── SlotCheckInEngine.kt # Pure slot check-in & status calculation logic
│   │   │   │   │   │   ├── AIConfig.kt          # Multi-config LLM settings (Gson-serialized)
│   │   │   │   │   │   └── CheckInResult.kt     # (defined in SlotCheckInEngine)
│   │   │   │   │   ├── database/
│   │   │   │   │   ├── database/
│   │   │   │   │   │   ├── HabitDatabase.kt     # Room database class (v3)
│   │   │   │   │   │   ├── dao/
│   │   │   │   │   │   │   ├── HabitDao.kt      # Data Access Object for habits
│   │   │   │   │   │   │   └── HabitCompletionDao.kt  # DAO for completion records
│   │   │   │   │   │   └── converter/
│   │   │   │   │   │       ├── ListConverters.kt    # List<Int>, List<String> converters
│   │   │   │   │   │       └── EnumConverters.kt    # Enum type converters
│   │   │   │   │   ├── repository/
│   │   │   │   │   │   └── HabitRepository.kt   # Repository pattern for data access
│   │   │   │   │   └── preferences/
│   │   │   │   │       └── UserPreferences.kt   # DataStore-based user settings
│   │   │   │   ├── viewmodel/
│   │   │   │   │   ├── HabitViewModel.kt        # ViewModel for UI state management
│   │   │   │   │   ├── RecordsViewModel.kt      # ViewModel for records screen
│   │   │   │   │   └── ContactsViewModel.kt     # ViewModel for contacts screen
│   │   │   │   ├── ui/
│   │   │   │   │   ├── screens/
│   │   │   │   │   │   ├── HomeScreen.kt        # Layout container with 3 tabs + TopAppBar/Navigation
│   │   │   │   │   │   ├── HabitScreen.kt       # Habit tab content (list, cards, search, reward sheet)
│   │   │   │   │   │   ├── HabitCreationScreen.kt # Create/Edit habit screen
│   │   │   │   │   │   ├── MultiSelectSortScreen.kt # Drag-and-drop reorder screen
│   │   │   │   │   │   ├── RecordsScreen.kt     # Completion records screen
│   │   │   │   │   │   ├── ContactsScreen.kt    # Supervisor contacts list
│   │   │   │   │   │   └── AdScreen.kt          # Splash ad screen
│   │   │   │   │   ├── screens/welcome/
│   │   │   │   │   │   ├── WelcomeScreen.kt  # Onboarding container (4-step AnimatedContent)
│   │   │   │   │   │   ├── WelcomeStepLayout.kt # Shared step skeleton + bottom bar + buttons
│   │   │   │   │   │   ├── WelcomeTopBar.kt     # Back-button-only top bar (no progress dots/label)
│   │   │   │   │   │   ├── WelcomeRiseIn.kt     # Stagger rise-in entrance animation
│   │   │   │   │   │   ├── WelcomeGreetingStep.kt # Step 1: welcome + continue
│   │   │   │   │   │   ├── WelcomePermissionsStep.kt # Step 2: permission rows + consent links
│   │   │   │   │   │   ├── WelcomeNotificationsStep.kt # Step 3: reminder/DND/persistent switches
│   │   │   │   │   │   └── WelcomeDoneStep.kt   # Step 4: done + enter app
│   │   │   │   │   ├── screens/settings/
│   │   │   │   │   │   ├── SettingsScaffold.kt       # Shared scaffold for settings screens
│   │   │   │   │   │   ├── SettingsHomeScreen.kt     # Settings home
│   │   │   │   │   │   ├── SettingsAIScreen.kt       # AI settings (config list, selection)
│   │   │   │   │   │   ├── SettingsAIEditScreen.kt   # AI config add/edit form
│   │   │   │   │   │   ├── SettingsNotificationsScreen.kt # Notifications
│   │   │   │   │   │   ├── SettingsDebugReminderScreen.kt # Debug reminders
│   │   │   │   │   │   ├── SettingsTemplateScreen.kt # Notification template
│   │   │   │   │   │   ├── SettingsGeneralScreen.kt # General
│   │   │   │   │   │   ├── SettingsLanguageScreen.kt # Language (radio list)
│   │   │   │   │   │   ├── SettingsAboutDetailScreen.kt # About detail (5-tap on version opens debug)
│   │   │   │   │   │   ├── SettingsDebugScreen.kt     # Debug settings (add sample habits)
│   │   │   │   │   │   └── components/
│   │   │   │   │   │       ├── SettingsSegmentedItem.kt # Segmented list item + icon chip + surface
│   │   │   │   │   │       ├── SettingsSegmentedSwitch.kt # Segmented switch row
│   │   │   │   │   │       ├── SettingsSegmentedGroup.kt # Vertical group wrapper
│   │   │   │   │   │       ├── SettingsSectionHeader.kt # Segment section header text
│   │   │   │   │   │       ├── SettingsIconTint.kt     # Accent tint palette for icon chips
│   │   │   │   │   │       └── SettingsComponentsPreviews.kt # Compose previews
│   │   │   │   │   ├── theme/
│   │   │   │   │   │   ├── Color.kt             # Color definitions
│   │   │   │   │   │   ├── Theme.kt             # Material theme setup
│   │   │   │   │   │   └── Type.kt              # Typography definitions
│   │   │   │   │   └── utils/
│   │   │   │   │       ├── DebounceClickHandler.kt    # Debounced click prevention
│   │   │   │   │       └── NavigationGuard.kt   # Navigation safety wrapper
│   │   │   │   ├── service/
│   │   │   │   │   └── ForegroundNotificationService.kt  # Foreground service for keep-alive
│   │   │   │   ├── receiver/
│   │   │   │   │   └── BootReceiver.kt          # Restart service on boot completed
│   │   │   │   └── utils/
│   │   │   │       ├── NotificationHelper.kt          # Notification creation & management
│   │   │   │       ├── NotificationPermissionHelper.kt # Permission request helper
│   │   │   │       ├── AccessibilityUtils.kt          # TalkBack detection
│   │   │   │       ├── AppLocaleManager.kt            # Android 13+ per-app language (LocaleManager)
│   │   │   │       └── OnboardingPreferences.kt       # SharedPreferences for onboarding state
│   │   │   ├── res/                             # Android resources
│   │   │   │   ├── values/strings.xml           # Chinese strings
│   │   │   │   ├── values-en-rUS/strings.xml    # English (US) strings
│   │   │   │   ├── values-en-rGB/strings.xml    # English (UK) strings
│   │   │   │   ├── values-zh-rCN/               # Chinese (Simplified)
│   │   │   │   ├── values-zh-rHK/               # Chinese (Traditional, Hong Kong)
│   │   │   │   ├── values-zh-rTW/               # Chinese (Traditional, Taiwan)
│   │   │   │   └── values-night/                # Dark theme overrides
│   │   │   └── AndroidManifest.xml
│   │   ├── test/
│   │   │   └── java/io/github/darrindeyoung791/habitpulse/
│   │   │       ├── ExampleUnitTest.kt
│   │   │       └── data/model/
│   │   │           ├── HabitTest.kt              # Habit JSON parsing helper tests
│   │   │           ├── HabitCompletionTest.kt    # HabitCompletion unit tests
│   │   │           ├── HabitStatusTest.kt        # HabitWithStatus property tests
│   │   │           └── SlotCheckInEngineTest.kt  # Slot check-in & status engine tests
│   │   └── androidTest/                         # Instrumented tests
│   ├── build.gradle.kts                         # App-level build config
│   └── proguard-rules.pro                       # ProGuard rules
├── gradle/
│   └── wrapper/
│       └── gradle-wrapper.properties            # Gradle 9.5.1
├── build.gradle.kts                             # Root build config
├── settings.gradle.kts                          # Project settings
├── gradle.properties                            # Gradle properties
├── QWEN.md                                      # Project context (this file)
├── structure.md                                 # Project structure diagrams
└── README.md                                    # Project documentation
```

## Database Schema

### habits Table

| Column | Type | Description |
|--------|------|-------------|
| id | TEXT (PRIMARY KEY) | Unique habit identifier (UUID) |
| title | TEXT | Habit title |
| repeatCycle | TEXT | Repeat cycle (DAILY or WEEKLY) |
| repeatDays | TEXT | Days to repeat (JSON format, e.g., [1,3,5]) |
| reminderTimes | TEXT | Reminder times (JSON format, e.g., ["08:00","20:00"]) |
| notes | TEXT | Habit notes |
| supervisorEmails | TEXT | Supervisor emails (JSON format); supports mixed email + phone per habit |
| supervisorPhones | TEXT | Supervisor phones (JSON format) |
| completedToday | INTEGER (BOOLEAN) | Today's completion status (0/1) |
| completionCount | INTEGER | Total completion count |
| lastCompletedDate | INTEGER | Last completion timestamp |
| createdDate | INTEGER | Creation timestamp |
| modifiedDate | INTEGER | Last modification timestamp |
| sortOrder | INTEGER | Sort order for custom reordering (lower values appear first) |
| timeZone | TEXT | Timezone ID for cross-timezone scenarios |

### habit_completions Table

Records every habit completion with timestamp.

| Column | Type | Description |
|--------|------|-------------|
| id | TEXT (PRIMARY KEY) | Unique completion record identifier (UUID) |
| habitId | TEXT (FOREIGN KEY) | References habits.id (CASCADE delete) |
| completedDate | INTEGER | Completion timestamp (milliseconds since epoch) |
| completedDateLocal | TEXT | Local date string in yyyy-MM-dd format (e.g., "2026-03-26") |
| timeZone | TEXT | Timezone ID when completion was recorded (e.g., "Asia/Shanghai") |

**Indices:**
- `habitId` - For fast lookups by habit
- `completedDateLocal` - For fast date-based queries

## Building and Running

### Prerequisites
- Android Studio (latest version recommended)
- JDK 17 or higher
- Android SDK with API level 37

### Commands

```bash
# Build debug APK
./gradlew assembleDebug

# Build release APK
./gradlew assembleRelease

# Run all tests
./gradlew test

# Run unit tests
./gradlew testDebugUnitTest

# Run instrumented tests
./gradlew connectedAndroidTest

# Clean build
./gradlew clean

# Install on connected device
./gradlew installDebug
```

### Gradle Version
- **Gradle**: 9.7.0-rc-2
- **Android Gradle Plugin**: 9.3.0
- **Kotlin**: 2.4.0
- **KSP**: 2.3.11

## Dependencies

### Core
- `androidx.core.ktx` (1.19.0) - Kotlin extensions for Android
- `androidx.lifecycle.runtime.ktx` (2.11.0) - Lifecycle components
- `androidx.lifecycle.viewmodel.compose` (2.11.0) - ViewModel Compose integration
- `androidx.lifecycle.runtime.compose` (2.11.0) - Lifecycle Compose integration
- `androidx.activity.compose` (1.13.0) - Compose integration with Activity
- `androidx.activity.ktx` (1.13.0) - Kotlin extensions for Activity
- `androidx.navigation.compose` (2.9.8) - Navigation Compose

### Room Database
- `androidx.room:room-runtime` (2.8.4) - Room database runtime
- `androidx.room:room-ktx` (2.8.4) - Room Kotlin coroutines support
- `androidx.room:room-compiler` (2.8.4) - Room annotation processor (KSP)

### Compose UI
- `androidx.compose.bom` (2026.06.01) - Compose Bill of Materials
- `androidx.compose.ui` - Core Compose UI
- `androidx.compose.material3` - Material Design 3 components
- `androidx.compose.material.icons.core` - Material icons core
- `androidx.compose.material.icons.extended` - Material icons extended

### Other
- `androidx.core:core-splashscreen` (1.2.0) - Splash screen compatibility
- `com.google.android.material:material` (1.14.0) - Material components for dynamic colors
- `sh.calvin.reorderable:reorderable` (3.1.0) - Drag-and-drop reordering library
- `androidx.datastore:datastore-preferences` (1.2.1) - Modern preferences storage
- `com.mikepenz:aboutlibraries-compose-m3` (15.0.4) - Open source license display
- `com.google.code.gson:gson` (2.14.0) - JSON parsing
- `androidx.biometric:biometric` (1.1.0) - Biometric authentication for gated key reveal
- `androidx.fragment:fragment-ktx` (1.9.0) - Fragment extensions for BiometricPrompt
- `androidx.swiperefreshlayout:swiperefreshlayout` (1.2.0) - Pull-to-refresh

### Testing
- `junit` (4.13.2) - Unit testing framework
- `androidx.junit` (1.3.0) - Android JUnit extensions
- `androidx.espresso.core` (3.7.0) - UI testing framework
- `androidx.compose.ui.test` - Compose testing utilities
- `org.json:json` (20260814) - JVM JSON implementation for tests (Android mocks org.json)

## Development Conventions

### Code Style
- **Kotlin style**: Official (as per `gradle.properties`)
- **JVM target**: Java 17
- **Compose**: Enabled with Material Design 3
- **i18n**: Simplified Chinese, Traditional Chinese, and English (US/UK)

### Architecture Patterns
- Single Activity architecture with Navigation Compose
- UI layer uses Compose with MaterialTheme
- Screen composables in `ui/screens/` package
- Navigation logic in `navigation/` package
- Data layer with Room database (implemented)
- Repository pattern for data abstraction
- ViewModel pattern for UI state management
- Type converters for complex types (List<Int>, List<String>, enums)

### Navigation
- Uses Navigation Compose with custom route definitions
- Custom animations for screen transitions:
  - Home ↔ Create Habit: Vertical slide with spring animation
  - Device corner radius applied to all screens (Android 12+)
  - Scale animation for Home screen during navigation
- `launchSingleTop` used to prevent duplicate destinations

### Commit Policy
- AI agents must NEVER commit changes unless explicitly asked by the user

### Testing Practices
- Unit tests in `src/test/`
- Instrumented tests in `src/androidTest/`
- Compose UI testing with `androidx.compose.ui.test`
- Pure business logic extracted to testable classes (e.g., `SlotCheckInEngine`) — no Android dependencies
- Tests use `org.json:json` JVM library because Android's `org.json` is stubbed in unit tests

### Internationalization (i18n) Guidelines
- **No hardcoded strings**: All user-visible strings must be stored in `strings.xml` resource files
- **String resource location**:
  - Default (Chinese): `res/values/strings.xml`
  - English (US): `res/values-en-rUS/strings.xml`
  - English (UK): `res/values-en-rGB/strings.xml`
- **Usage in Compose**: Use `stringResource(R.string.resource_name)` to retrieve localized strings
- **Naming convention**: Use snake_case for string resource names (e.g., `habit_creation_title`)
- **Supported languages**:
  - Chinese (Simplified) - Default
  - Chinese (Traditional, Hong Kong) - `res/values-zh-rHK/`
  - Chinese (Traditional, Taiwan) - `res/values-zh-rTW/`
  - English (US) - `res/values-en-rUS/`
  - English (UK) - `res/values-en-rGB/`
- **Auto-mirrored icons**: Icons with directional meaning (e.g., arrows, back/forward) should use `autoMirrored="true"` in drawable resources for RTL support
- **Agent requirement**: When adding new UI text, always:
  1. Add string resources to `values/strings.xml` and all locale-specific `strings.xml` files (`values-en-rUS/strings.xml`, `values-en-rGB/strings.xml`, `values-zh-rHK/strings.xml`, `values-zh-rTW/strings.xml`)
  2. Reference via `R.string.*` in code, never inline string literals
- **IMPORTANT — localeFilters**: `app/build.gradle.kts` limits packaged locales via `androidResources { localeFilters += listOf(...) }`. When adding a new locale, you MUST also add it to `localeFilters`, otherwise its resources are stripped from the APK at link time (merged res contains it, but `aapt2 dump resources` shows no entry) and the device silently falls back to another locale. Debugging symptom: adding `values-en-rGB/` alone did nothing on an en-GB device because `en-rGB` was missing from `localeFilters`.
- **Locale resolution**: Runtime locale resolution follows the packaged resource configs, NOT `Locale.getDefault()`. Use `LocalConfiguration.current.locales[0]` for date/time formatters so they match the app's actual resource language (see `RecordsScreen.kt`).

## Current Status

The project is in **early development stage** (v0.5.19-alpha):

### Completed
- ✅ Project structure set up
- ✅ Basic Compose theme configured with Monet dynamic colors
- ✅ Navigation Compose integrated with custom animations and shared transitions
- ✅ LauncherActivity for routing between Welcome and MainActivity
- ✅ WelcomeActivity + WelcomeScreen with 4-step onboarding flow (greeting → permissions → notifications → done)
- ✅ AdScreen with countdown skip for splash ads
- ✅ Home screen with 3 tabs: Habits, Contacts, Records
- ✅ **HomeScreen Refactoring** - Split into layout shell (HomeScreen.kt) and content screens (HabitScreen.kt, etc.) for better maintainability (~1100 lines vs original 2900+)
- ✅ Habit creation/edit screen with form validation
- ✅ Settings screen (separate Activity) with app info, visual options, about
- ✅ OpenSourceLicensesActivity using AboutLibraries library
- ✅ Custom screen transition animations
- ✅ Device corner radius support (Android 12+)
- ✅ Predictive back gesture support
- ✅ Split-screen support
- ✅ Localization (Simplified Chinese, Traditional Chinese, English US/UK) with values-zh-rCN, values-zh-rHK, values-zh-rTW, values-en-rUS, values-en-rGB, values-night
- ✅ Room database integration (v2.8.4, v3 schema)
- ✅ Habit entity with UUID primary key
- ✅ HabitDao with CRUD operations and Flow support
- ✅ HabitCompletionDao for completion record tracking
- ✅ HabitRepository for data abstraction (single source of truth)
- ✅ HabitViewModel, RecordsViewModel, ContactsViewModel for UI state management
- ✅ Habit completion toggle functionality with haptic feedback (50ms)
- ✅ Habit completion history tracking - every check-in recorded with timestamp
- ✅ HabitCompletion entity - records completion date, local date, and timezone
- ✅ Database v3 - Added sortOrder and timeZone columns to habits table
- ✅ Records screen with completion records grouped by date
- ✅ Habit filtering - dropdown to filter by specific habit or show all
- ✅ Date picker filter for records with orientation-aware dialog
- ✅ Contacts screen - aggregated supervisor contacts with per-habit associations
- ✅ Bottom sheet for managing contact-habit associations
- ✅ Responsive navigation system (NavigationRail, NavigationBar, PermanentNavigationDrawer)
- ✅ Collapsed NavigationBar with circular selection indicator for tablet landscape
- ✅ Animated drawer width for permanent navigation drawer
- ✅ Adaptive horizontal padding for habit cards based on screen size
- ✅ TalkBack accessibility support for all navigation elements
- ✅ Habit card animations (scale + slide enter/exit effects)
- ✅ Dynamic app bar title font size (changes on scroll)
- ✅ Delayed enter animation for newly added habits (after navigation completes)
- ✅ Smooth reposition animation for other cards (animateContentSize)
- ✅ Debug feature: tap version 5 times in 10s to add 20 sample habits
- ✅ Habit repeat days selection (weekly cycle)
- ✅ Reminder time management
- ✅ Multi-Select & Sort - Long-press to enter multi-select mode, drag-and-drop to reorder, batch delete
- ✅ Multi-Select & Sort UX Optimization - Consistent dialog style, auto-scroll to top on save/delete
- ✅ Search Experience Optimization - No brief "No habits" state when exiting search
- ✅ Reorderable integration - Using sh.calvin.reorderable:reorderable:3.0.0
- ✅ Predictive Back Gesture for MultiSelectSort - Navigation system handles back automatically
- ✅ Delete Confirmation Dialog - Consistent with MultiSelectSort delete dialog style
- ✅ Supervisor Display in Reminder Dialog - Shows emails and phones when available
- ✅ Debounce & Navigation Guard - Prevents rapid clicks and navigation errors
- ✅ ScrollToTop Fix - Changed to Int counter for reliable LaunchedEffect triggering
- ✅ ForegroundNotificationService for keep-alive with boot auto-restart
- ✅ BootReceiver for BOOT_COMPLETED and LOCKED_BOOT_COMPLETED
- ✅ NotificationHelper and NotificationPermissionHelper for notification management
- ✅ UserPreferences (DataStore) for showSplashAd, forceTabletLandscape, persistentNotification
- ✅ OnboardingPreferences (SharedPreferences) for hasCompletedOnboarding, isLimitedMode
- ✅ DebounceClickHandler (300ms) and NavigationGuard (500ms) utilities
- ✅ AccessibilityUtils for TalkBack detection
- ✅ Reward Bottom Sheet - Celebration modal after habit check-in with animated 12-sided polygon shape and MD3 easing animations
- ✅ WebView Security - SSL certificate warning dialog for insecure connections
- ✅ WebView External Link Warning - Dialog warning when leaving app domain (shown once per session)
- ✅ WebView in Settings - GitHub link opens in WebView instead of external browser
- ✅ URL Variables - Domain allowlist uses RouteConfig variables for easy renaming
- ✅ Predictive Back Gesture Fix - 修复返回手势与系统预测性返回动画冲突导致的杀后台问题
- ✅ Check-in Status System (Phase 1-4) - Slot-based check-in with DAO v4 migration, status engine, `HabitWithStatus` data class, ViewModel flows, and UI integration:
  - ✅ DB v4 migration (`slotTime`, `isLate` columns in habit_completions; `MIGRATION_3_4`)
  - ✅ DAO methods: `getCompletionsByDate()`, `getCompletionByHabitIdDateAndSlot()`
  - ✅ Repository: `performSlotCheckIn()`, `undoSlotCompletion()`, `getTodayCompletions()`
  - ✅ `HabitStatus` enum + `HabitWithStatus` data class with `pendingCount` / `isCompletelyOverdue`
  - ✅ Status engine: `calculateHabitStatus()` with ABOUT_TO_START / OVERDUE / COMPLETED_TODAY detection
  - ✅ Count flows: `pendingTodayCount`, `aboutToStartCount`, `overdueCount`
  - ✅ 60s day-change polling with ProcessLifecycleOwner
  - ✅ Slot-based check-in: `findTargetSlot` (earliest incomplete), 1h window, overdue detection
  - ✅ `CheckInResult` sealed class for feedback (Success/AlreadyCompleted/TooEarly)
  - ✅ Undo: deletes most recent completion record
  - ✅ RewardSheet gated to on-time full completion only
  - ✅ HabitCard status badges (已完成/即将开始/逾期) with colored chips
  - ✅ HabitCard progress display (x/y for multi-reminder habits)
  - ✅ EntryZone dynamic cards (今日提醒/即将开始/逾期) with auto-hide on zero count
- ✅ `filteredHabitsWithStatus` StateFlow for search with status data
- ✅ TodayHabitsScreen migrated to `habitsWithStatusForDisplay`
- ✅ **SlotCheckInEngine Extraction** - Pure business logic (`calculateHabitStatus`, `isApplicableToday`, `executeSlotCheckIn`, `CheckInResult`) extracted from HabitViewModel into testable standalone `SlotCheckInEngine` object in `data/model/`
- ✅ **Mixed Contact Supervision** - Removed `SupervisionMethod` enum, DB v5 migration (DROP COLUMN), independent email/phone input sections in creation UI, unified contact aggregation
- ✅ **Unit Test Setup** - 65 unit tests across 4 test classes:
  - `SlotCheckInEngineTest` (31 tests) - covers status calculation, slot check-in logic, applicable day detection, edge cases (empty slots, midnight, boundary conditions, old-style completions)
  - `HabitTest` (21 tests) - covers JSON parsing helper methods, `copyWith*` methods, `hasSupervision`, edge cases (all 7 days, duplicates, empty strings)
  - `HabitStatusTest` (12 tests) - covers `pendingCount`, `isCompletelyOverdue`, negative pendingCount, old-style completion compatibility
  - `HabitCompletionTest` (5 tests) - covers `getTodayDate()`, `getFormattedDate()`, and default values
- ✅ **New Settings Redesign** - Segmented list settings UI with grouped items, leading icon chips, switches, and per-screen scaffolds
- ✅ **Debug Settings Page** - Hidden debug page reached by tapping the version item 5 times within 5 seconds on the new About screen; hosts developer tools (add sample habits, reset onboarding to show the welcome flow on next launch), icon-less list items; sample-data dialog shows a warning that adding a large batch may disrupt existing habits and trigger many unnecessary reminders (6 locale `debug_add_sample_data_warning`); reset-onboarding item asks for confirmation then calls `HabitViewModel.resetOnboarding()` (writes `hasCompletedOnboarding=false` so `LauncherActivity` routes to `WelcomeActivity`)
- ✅ **AI Config Provider Merge (superseded)** - AI provider config form was merged directly into the AI config page (second-level page); `SettingsAIProviderScreen.kt` / `SettingsAIProviderActivity.kt` deleted. Superseded by **Multi-AI-Config Settings** below, which moved the form out into a separate add/edit page (`SettingsAIEditActivity`)
- ✅ **AI Config Page Layout** - Provider config form (endpoint/key/model/test connection), streaming output switch + memory entry grouped as segmented list items; notice shown as standalone text (same style as About screen), no horizontal divider
- ✅ **Model Label Localization** - Preset model labels (`glm-4-flash-250414（默认）`, `glm-5.1（最新旗舰）`) resource-ized via `ai_settings_model_default_label` / `ai_settings_model_flagship_label` format strings in all 4 locale files
- ✅ **Old Settings Interface Migration** - Entry points to legacy settings migrated to new settings: Home settings button → `SettingsActivity`, AI Create Habit settings button → `SettingsAIActivity` (both in `HabitPulseNavGraph.kt`)
- ✅ **In-App Language Switching** - Settings → General → Language entry (Android 13+ per-app language via platform `LocaleManager`; `LocaleManagerCompat` getter + reflection fallback for API 33 `@SystemApi` setter); dedicated `SettingsLanguageActivity` sub-page: "跟随系统" in its own group with system-language supporting text + fixed self-named labels (中文（简体，中国大陆）/ 中文（繁体，台湾）/ 中文（繁体，香港）/ English (US) / English (UK), `translatable="false"`), radio-button rows; selecting a language returns to the previous page with a single refresh; option hidden on API < 33; managed by `utils/AppLocaleManager.kt`
- ✅ **Settings Press Haptics** - Non-disabled settings list items (`SettingsListSurface`), text link buttons (`SettingsTextLinkButton`), and scaffold back/help `IconButton`s vibrate 25ms on press-down and 25ms on release (half of the 50ms home check-in button) via `ui/utils/PressVibrationFeedback.kt` (`PressVibrationFeedback` composable + `vibrateShort` + `rememberHapticsEnabled`); new General toggle "关闭应用内全部震动" (`HAPTIC_FEEDBACK_ENABLED` in `UserPreferences.kt`, default on, with the "关闭" switch showing off by default so the phone vibrates by default) gates all in-app vibration including the check-in button; "界面与显示" group renamed to "显示与触感" (all 6 string files)
- ✅ **Vibration Debug Sub-page** - 调试 → 震动调试子页 (`SettingsDebugVibrationActivity`/`SettingsDebugVibrationScreen`): sliders for press-vibration duration (5-200ms) and amplitude (1-255) with immediate test button and reset-to-default; stored in DataStore (`PRESS_VIBRATION_DURATION_MS` / `PRESS_VIBRATION_AMPLITUDE` in `UserPreferences.kt`); `vibrateShort`/`PressVibrationFeedback`/`DndRangeSlider` now read configurable params via `rememberPressVibrationParams()`, amplitude only applies when hardware supports amplitude control (`hasAmplitudeControl()`, fallback `DEFAULT_AMPLITUDE`); settings scaffold (`SettingsScaffold`) also vibrates once when scrolling to top/bottom edge via `snapshotFlow` on scrollState (no vibration on non-scrollable short pages); devdoc `listitem-style.md` §7 updated
- ✅ **Multi-AI-Config Settings** - AI settings upgraded from a single provider config to a managed list: AI config list page (`SettingsAIScreen`) shows all configs (tap row = set active, trailing edit pencil = edit page, radio shows active), "添加 AI 配置" opens `SettingsAIEditActivity`; edit page (`SettingsAIEditScreen`) has name/endpoint/key/model fields + per-config streaming & deep-thinking switches + test connection + save/delete; storage migrated to DataStore `llm_ai_configs` (JSON array of `AIConfig`, Gson + `@SerializedName`) + `llm_active_config_id`; legacy `llm_api_endpoint/llm_api_key/llm_model_name/llm_streaming_response` keys are deprecated and one-time migrated into a "默认配置" (via `migrateLegacyAiConfig()` on cold start, then physically removed); consumers (`AICreateHabitViewModel`, `AICreateHabitScreen`, welcome flow) read the active config through `getActiveAIConfig()`/`activeConfigFlow`; shared `AiConnectionTester` for test-connection; `SettingsSectionHeader` extracted as a shared component; legacy `AISettingsScreen`/`AISettingsActivity` deleted, legacy `SettingsActivity` AI entry repointed to `SettingsAIActivity`; welcome flow kept compiling with minimal changes (full welcome/AI feature refactor deferred)
- ✅ **LLM API Key 加密** - API key 在 DataStore 中以密文存放，运行时静默解密，查看需生物识别：
  - 双密钥 AES-256-GCM：`llm_config_runtime`（无认证，静默解密）/ `llm_config_reveal`（`setUserAuthenticationParameters(0, AUTH_BIOMETRIC_STRONG | AUTH_DEVICE_CREDENTIAL)`），由 `data/security/KeystoreManager.kt` 管理
  - `data/security/AesGcmCipher.kt`：密文格式 `Base64(IV + ciphertext)`，每次新 12 字节随机 IV，解密失败抛 `AesGcmCipherException`（可恢复）
  - `data/security/ApiKeyCrypto.kt`：`encryptConfig`（运行时+展示双密文）、`decryptRuntime`（发请求用）、`createRevealDecryptCipher`/`finishRevealDecrypt`（BiometricPrompt CryptoObject 门控）、`hasAuthenticationMethod`/`createRevealPromptInfo`；`AesGcmCipher` 纯 JCA 无 Android 依赖便于 JVM 测试
  - **BiometricPrompt API 契约**：`PromptInfo.Builder.build()` 在允许 `DEVICE_CREDENTIAL` 时**禁止**设置 negative button（否则抛 `IllegalArgumentException: Negative text must not be set if device credential authentication is allowed`，真机点击「查看密钥」即崩溃）。因展示密钥恒为 `AUTH_BIOMETRIC_STRONG | AUTH_DEVICE_CREDENTIAL`，`createRevealPromptInfo` 在 API 30+ 不设负按钮（系统自带「使用设备凭据」兜底），仅 API ≤ 29 才设 `negativeButtonText`
  - `data/security/ApiKeyMigration.kt`：明文判定 / 幂等加密 / 安全解密的纯逻辑，注入 fake 加密层可单测
  - `AIConfig` 新增 `displayCipher`（展示密文）与 `keyVersion: Int = 0`（0 = 明文遗留）字段（`@SerializedName`）；`hasApiKeyConfigured()` 基于密文判空
  - 迁移：冷启动（`HabitPulseApplication.onCreate` 调 `encryptAndPersistConfigs()`）+ 惰性兜底（`getActiveAIConfig()` 读到明文先迁移）；`decodeConfigs` 经 `normalizeForStorage()` 兼容缺 `displayCipher` 的旧 JSON
  - 运行时链路：`getActiveAIConfig()` 解密后返回明文版配置供 `toLLMConfig()`/`LLMClient` 发请求；`aiConfigsFlow`/`activeConfigFlow` 保持密文，UI 判空改用 `hasApiKeyConfigured()`（`AICreateHabitScreen`）
  - 查看门控：`SettingsAIEditActivity` 改为 `FragmentActivity`；编辑页 API key 输入框加密态以不可选中的圆点 `•••` 展示（`enabled = false` + `OutlinedTextFieldDefaults` disabled 配色，尾图标仍可点击，示意「已填写但已加密」），`PasswordVisualTransformation` 仅在未展示明文时生效，点击查看 → BiometricPrompt（`canAuthenticate` 检测，无生物识别/设备凭据时按钮禁用）→ CryptoObject 解密填入明文；离开页面/60s 超时自动隐藏，每次查看重新验证；解密失败引导重新录入不崩溃
  - 测试连接（`AiConnectionTester`）在编辑已配置 key 时用 `decryptRuntime` 静默解密后测试
  - 依赖：`androidx.biometric:biometric:1.1.0` + `androidx.fragment:fragment-ktx:1.8.5`
  - 单元测试：`AesGcmCipherTest`（9 用例：往返/IV 唯一/Base64/篡改/错误密钥）、`ApiKeyMigrationTest`（11 用例：明文判定/迁移幂等/失败保底）、`AIConfigCompatTest`（5 用例：密文判空/旧 JSON 兼容）
- ✅ **AI Chat 全场景助手（tool-calling，与旧 AI 并存）** - 原生 function-calling 新版对话界面 `AIChatScreen` + `Route.AIChat`（旧 `AICreateHabitScreen`/`Route.AICreateHabit`/首页 FAB「AI 创建」全部保留，旧界面顶栏新增「新版 AI 对话」入口图标）：
  - 协议层：`ToolDef`（JSON Schema）/`ChatRequest.tools`/`ChatMessage.role="tool"`+`toolCallId`；`LLMClient.chatStream` 累积 `delta.tool_calls` 分片（id/name/arguments），`StreamChunk.Usage` 捕获 `[DONE]` 前末 chunk 的 usage；捕获 `delta.reasoning_content`
  - 引擎 `AIChatConversationManager`（`ai/conversation/`，与旧 `ConversationManager` 并存）：单轮 while 循环执行全部 tool_calls 并以 `role=tool` 回灌，直到无工具或命中暂停点（ask_question / create_habit / delete_habit）；`ToolResult.Error` ≥ MAX_RETRIES 触发错误事件；流式空内容/无工具回退一次非流式；`ConversationGuard` 扩展 `invalidSettingTries`（设置类连续报错 ≥3 触发 GuardBlocked）；`retryLastTurn` 删除最后一个 assistant 轮后重发
  - 工具注册表 `ai/tools/chat/`（`ChatTool`/`ChatToolRegistry`/`functionSpec`）：9 个工具 - `create_habit`（HH:mm 去重、repeat_days 0..6 去重、WEEKLY 缺天报错提问、title≤30/notes≤200 截断）、`search_habits`（`HabitRepository.searchHabitsFlow` first()，无关键字返回全部习惯按 sortOrder；`habitToBrief` 共享转换函数，含主键+打卡统计）、`edit_habit`/`delete_habit`（校验 id+title 回显）、`get_settings_status`/`update_setting`/`open_settings_page`（`ControllableSetting` 6 开关白名单映射 `UserPreferences` setter 与 Activity）、`ask_question`/`reply`
  - ViewModel `AIChatViewModel`（`viewmodel/`）：`messages` 单一数据源 + `usage: SessionUsage` 精确统计；`selectedConfigId` 会话级切换（默认读 `activeConfigFlow`，**不写回全局** `setActiveAIConfig`）；确认才 `insertHabit`/`deleteHabit`（无孤儿行）；未确认新习惯仅内存卡片；`HabitPickerCard` 带 `cardId`，支持 `searchHabitsInPicker`（卡内搜索全部习惯）/`submitPickerSelection`（多选提交给 AI）/`manualPickerDone`（我已手动操作继续会话）
  - UI `AIChatScreen`（`ui/screens/ai/`）：TopAppBar（标题输出中变化、返回确认、`ProviderSwitcher` AssistChip→DropdownMenu 底部「管理提供商」、清除对话确认、Token 小字）；`ProviderSwitcher` 无配置时显「未配置」直接进 `SettingsAIActivity`；消息列表渲染气泡/`ThinkingBlock`/提问卡（choice/time/day_of_week/multi_choice/text/confirm）/新建习惯卡（确认/编辑/删除）/选择卡/编辑跳转卡/删除确认卡/设置状态与变更卡（撤销）/设置导航卡；底部输入栏横屏压缩 2 行 + 免责声明（横屏+键盘隐藏）；`HabitPickerCard` 含顶部搜索框（300ms 防抖）、多选高亮、行内编辑/删除、底部「提交选择」+「我已手动操作」
  - `UserPreferences.getAIConfig(id)` 会话内按 id 取明文 key 配置（不写回全局）
  - 系统提示词新增 `assets/prompts/chat-system-prompt.md`（tool-calling 专用，`SystemPrompt.getChatSystemPrompt`）；6 个 strings 文件新增 `ai_chat_*` 文案；新增 `ai_error_guard_blocked`
  - 单元测试：`AIChatConversationManagerTest`（工具循环/暂停点/重试上限/流式降级）、`ConversationGuardTest`（同题拦截/纠错计数）、`ToolRegistryTest`（校验/白名单/状态数据）
- ✅ **Unified Device Form Factor** - 全应用统一的设备形态判定单一入口 `ui/DeviceFormFactor.kt`：`DeviceFormInfo`（携带 `windowSizeClass`/`windowPosture`）+ 纯函数 `classifyDeviceForm()`（无 Android 依赖可单测）+ `@Composable rememberDeviceFormInfo()`（`currentWindowAdaptiveInfo()`，旋转/分屏/折叠自动重组）。断点：`TABLET_MIN_WIDTH_DP=600`（`isTabletDevice` = `min(宽,高)>=600`，对齐旧 `smallestScreenWidthDp`）、`WIDE_LAYOUT_MIN_WIDTH_DP=840`（`isWideLayout` = 横屏且宽>=840）、`LARGE_SCREEN_MIN_WIDTH_DP=1200`（`isLargeWindow`）。派生标志 `isTabletLandscape`/`isPhoneLandscape`/`isWideLayout`/`isLargeWindow`。迁移调用点：`HomeScreen`（导航模式 `isTabletLandscape`/`isPhoneLandscape`、`isWaterfallMode`→`isWideLayout`）、`HabitScreen`（`useStaggeredGrid` = `isWideLayout || (forceTabletLandscape && isLandscape)`，删除 `screenWidthDp=840` hack）、`RecordsScreen`/`ContactsScreen`（`useTwoColumnLayout`→`isWideLayout`）、`WelcomeScreen`（`shouldUseSplitLayout`→`isLandscape`、`isTablet`→`isLargeWindow`）、`RewardBottomSheet`/`NotificationConfirmDialog`/`SettingsGeneralScreen`/`SettingsActivity`/`AIChatScreen`/`AICreateHabitScreen`。约定：页面禁止自行用 `LocalConfiguration`/`smallestScreenWidthDp`/`screenWidthDp`/`orientation` 判定设备形态，一律走 `rememberDeviceFormInfo()`；`@Preview` 的 `uiMode` 可保留 `Configuration.ORIENTATION_LANDSCAPE`
  - 单元测试：`DeviceFormFactorTest`（19 用例：600/840/1200 边界、手机/平板竖横屏、方形窗口、派生标志）
- ✅ **In-App Font Size** - 应用内字体大小自定义（通用 → 字体大小，入口在「语言」上方、同一「显示与触感」组）：DataStore 新增 `FONT_SCALE_FOLLOW_SYSTEM`（默认 true）/ `FONT_SCALE`（默认 1.0f）；`HabitPulseTheme` 通过 `CompositionLocalProvider(LocalDensity provides Density(density, effectiveFontScale))` 全局覆盖字体缩放（跟随系统=系统 `fontScale`，关闭=自定义值；`@Preview` 用 `LocalInspectionMode` 跳过 DataStore 访问）。字体大小页 `SettingsFontScaleScreen`/`SettingsFontScaleActivity`（已注册 manifest）：跟随系统开关 = 单行 listitem 开关（无图标无说明，走标准按压/长按/震动）；滑杆档位 = Android 系统字体缩放预设 `[0.85, 1.0, 1.15, 1.3, 1.45, 1.6, 1.75, 1.9, 2.0]`（索引用 0..8，标准 1.0 刻度标记 + 最小/最大大小 A 字母）；开关打开时滑杆滚动到 `LocalConfiguration.current.fontScale` 最近档位并呈禁用灰化样式（仍可拖动，首次拖动自动关闭跟随系统），滑动按档位震动（`rememberHapticsEnabled`/`rememberPressVibrationParams`/`vibrateShort`，遵循全局震动开关）；设置离开页面时一次性写入生效（`BackHandler` + commit 标志）。通用页入口图标 `Icons.Outlined.FormatSize`（index 0 → 蓝），存储分组 `tintOffset` 2→4 避免同页图标颜色重复；通用首页入口描述改为「语言、显示与触感」（6 个 strings.xml）
- ✅ **Open Source Licenses Screen Listitem Adaptation** - 开放源代码许可页（`OpenSourceLicensesActivity`）从 aboutlibraries 默认 `LibrariesContainer` 改为项目 listitem 规范样式：新增共享组件 `SettingsExpandableListSurface`（`SettingsSegmentedItem.kt`，`AnimatedVisibility` 展开/收起 + spring 动画 + chevron 180° 旋转，保留分段圆角/按下 20dp 圆角/按压震动，展开内容与表头同属一块 `surfaceContainer` 表面；表头支持 `badgeContent` 槽位，展开前即显示 license badge）；每库一行 = 无前置图标的 listitem（headline = 库名，supporting = 作者 · 版本，点击展开，**license pill badge 常驻表头**）；展开区显示原版 `LibraryActions` 三个功能按钮（Source/Website/Sponsor 描边 + License 填充，8dp 圆角 + 12/6 padding + `labelLarge` 12sp Medium，**按下圆角 8→20dp 动画** + `PressVibrationFeedback`，点击行为：有 URL 走**应用内 `WebViewActivity` 打开**，License 无 URL 弹许可证内容对话框 `LicenseContentDialog` 显示 `strippedLicenseContent`）。按钮文案保持原版英文不本地化；新增 shared 组件时把 `LargeCorner`/`SmallCorner`/`PressedCorner` 从 private 改为 internal 供跨包复用。**清理**：删除 git 提交的陈旧 `app/src/main/res/raw/aboutlibraries.json`（167 库旧版本，打包时被插件 generated 覆盖，保留 `keep.xml` 与 `app/config/`），debug/release APK 内 aboutlibraries.json 哈希验证与插件生成一致

- ✅ **UI Catalog 调试页** - 调试设置新增「UI catalog」入口（独立单行分组，`onNavigateUICatalog` → `SettingsUICatalogActivity`/`SettingsUICatalogScreen`）：集中展示新设置统一组件体系（排除通知/震动调试等旧版残留）的全部元素——`SettingsSegmentedItem` 变体（标准导航项 / `showArrow=false` 动作项）、**分组与同组圆角特性专组**（首项顶部 16dp / 中间项四角 4dp / 末项底部 16dp，按压四角 animate 到 20dp，涟漪裁剪到形变形状；`tintOffset=2` 演示图标低饱和配色跨组延续不重复）、状态变体组（`enabled=false` 禁用占位自动显示「即将推出」、`selected=true` secondaryContainer 高亮）、`SettingsSegmentedSwitch` 开关行（整行切换、实时交互）、`SettingsSegmentedNumberItem` 序号选择行（点击切换选中，含单行/双行序号 chip 两态）、`SettingsExpandableListSurface` 可展开项（badgeContent pill 常驻表头 + 展开区与表头同一表面 + chevron 180° 旋转）、`SettingsSegmentedBox` 非点击分段表面（放说明文字模拟滑块场景）、`SettingsTextLinkButton` 紧凑文本链接按钮堆叠；文案按需求硬编码英文不进 strings.xml（manifest label 用字面量），后续计划扩展到设置以外界面并逐步引入 design token 式样式管理。**迭代**：序号选择行（`SettingsSegmentedNumberItem`）统一双行 40dp 数字位防错位，catalog 三行均带 supportingText + 尾部 `RadioButton` 镜像选中；`SettingsExpandableListSurface` 升级为**整面可点击**（`clickable` 移到表面根节点，涟漪/按压形变覆盖展开体，开放源代码许可页自动继承）并新增 `leadingIcon`/`tintIndex`/`enabled`/`trailing`/`interactionSource` 参数支持「开关即展开」变体（trailing 传 `Switch(onCheckedChange=null)` 镜像 expanded、共享 interactionSource 同步按压态）；通知设置页与欢迎流程的免打扰滑块从「开关行 + `SettingsSegmentedBox` 两段式（中间 2dp 间隙）」统一迁移为该模式；catalog 新增 Sliders 分组（DnD 滑块开关展开演示）、Scaffold 级 Extended FAB 与 Dialogs 分组（条目与 FAB 均触发示例 AlertDialog）；带 FAB 的设置页统一走 `SettingsScaffold(reserveFabSpace = true)` 在内容尾部自动追加 1/4 屏高 Spacer 防 FAB 遮挡（已启用：AI 配置编辑、通知模板、UI catalog）
- ✅ **竖屏揭示式抽屉（Reveal Drawer，DeepSeek 风格同平面滑动）** - 主页竖屏（手机+平板）导航从覆盖式 `ModalNavigationDrawer` 重写为自定义揭示式抽屉（`HomeScreen.kt` 竖屏分支）：抽屉与主页面**左右相邻固定在同一虚拟平面上**，打开时两者同步右移相同距离（`drawerWidth * fraction`），抽屉从屏幕外左侧滑入、主页面向右退出，主页上叠加一层随页面移动的 32% 黑色遮罩使剩余可见部分稍微变暗。交互：主页任意位置右滑、点顶栏菜单按钮打开；抽屉内向左滑、点遮罩区域或系统返回键关闭。**防误触轴向仲裁**：手势用自定义 `pointerInput` 检测器实现（不用 `draggable`——其挂在整个主页容器上会与子组件点击/垂直滚动手势仲裁冲突导致失效）：越过 touch slop 前不消费任何事件（点击完全不受影响）；若垂直滚动先认领指针流（change 已被 consume）或纵向位移先于横向越过阈值则立即放弃本次触摸，之后手指再水平移动也不会触发抽屉；只有横向位移率先越阈才 engage 并逐帧消费。因 `AwaitPointerEventScope` 受限挂起作用域禁调外部 suspend（如 `Animatable.snapTo`），engage 后手势循环逐帧以普通函数 `scope.launch { snapTo }` 应用位移（launch 非 suspend 可直接调用；UI dispatcher FIFO 保证顺序，每帧即时生效、抽屉严格跟手而非固定动画），宽度在 launch 内部实时读屏宽状态避免旋转后过期捕获。**坐标系修正与松手判定**：检测器挂在**静止的根容器**上而非随动的页面/抽屉容器——挂在跟手容器内时本地坐标随平面每帧平移，手指的世界位移被容器自身位移抵消，totalX 与测速在跟手后塌缩到 ≈0 甚至因抖动变负，造成间歇性「快滑反而回弹/反向」；根容器本地坐标=世界坐标彻底规避。`VelocityTracker` 仅自 engage 起记录（engage 时 `resetTracking()` 并补记当前帧）且抬手 UP 帧也计入。松手两级判定：① 本次手势位移 ≥ 1/5 屏时**无论速度**按方向直接开合（右滑展开/左滑收起对称）；② 更短手势用动量投影（pager 式：`fraction + 速度/宽 × DrawerFlingProjectionSeconds(0.16s)` 是否越过中点）预测落点取最近锚点——短快扫投影远超中点、极短距即可触发，慢速短拖保持原位，速度读数失真时仅退化为就近吸附而**绝不会反向**。收尾动画以**手指释放速度为初始速度**滑向锚点（`settlePortraitDrawer`：`spring(DampingRatioNoBouncy, StiffnessLow)` + `animateTo(initialVelocity = 释放速度/抽屉宽)`，临界阻尼无过冲），末端跟随手势动量而非固定 tween；顶栏按钮/返回键等无手势路径仍用 320ms tween。抽屉宽度为 **3/4 屏宽**（随屏幕自适应），内部导航条目作为整体垂直居中便于拇指触达，无顶部收起按钮；滑动主页面容器按设备屏幕圆角（`navigation/getDeviceCornerRadius()`）裁剪为圆角卡片，遮罩随卡片一同移动。**点击抽屉项时切换 Section 与收起动画并行执行**（`navigateToSection(section)` 同步触发 `AnimatedContent`，`closePortraitDrawer()` 异步动画）。实现要点：`Animatable<Float>` fraction 状态 + 抽屉/页面两个 `Modifier.offset { }` lambda 在 placement 相位读取（动画零重组）；遮罩 alpha 走 `drawBehind` 绘制相位且全关时不组合（彻底排除对主页命中的干扰）；菜单图标/遮罩点击 enabled/BackHandler enabled 均走 `derivedStateOf` 布尔阈值避免逐帧重组；全开/全关时对滑出页与抽屉分别 `clearAndSetSemantics {}` 屏蔽无障碍焦点；状态经 `rememberSaveable` 存活旋转。横屏 Rail / 平板横屏 PermanentDrawer 行为不变
- ✅ **主页 Omnibox（底部常驻搜索框，Google 风格）** - 习惯/联系人/记录三个 Section 的主页底部新增常驻搜索框（`HomeScreen.kt` 私有 `HomeOmnibox` composable，三个布局分支各挂一份）：100% 圆角胶囊 Surface（`surfaceContainerHighest`、无阴影——与习惯卡片 `surfaceContainer` 区分），左侧放大镜图标、中间提示文本「搜索与AI」（新字符串 `main_omnibox_hint`，6 locale 同步）、右侧麦克风图标为语音识别预留位（无行为）；有输入时麦克风替换为清除按钮（`accessibility_omnibox_clear`）。**行为**：单一 `homeOmniboxText` 状态（rememberSaveable）三 Tab 共享，`LaunchedEffect` 单向同步到 HabitViewModel/ContactsViewModel 的 debounce 过滤管道；Records 由 UI 层按习惯名称过滤（v0.8.62-alpha 起）；AI 逻辑纯预留。点击任意处聚焦弹输入法，容器走 `windowInsetsPadding(navigationBars.union(ime))` 正确适配键盘高度（edge-to-edge 下 adjustResize 不缩窗、ime insets 直通）；键盘 Search 动作收起输入法。**渐变过渡**：`BottomOmniboxWithFade`（BoxScope 扩展）在外层无 insets 的 Box 内先铺一层 `drawBehind` 垂直渐变（自框上方 `OmniboxGradientHeadroom(64dp)` 处从全透明加深到背景色直至屏幕底边含导航栏区域，区分列表背景与搜索框），再叠 omnibox；**焦点联动**：打开竖屏抽屉时 `focusManager.clearFocus()` 自动取消 Omnibox 焦点并收起键盘——两条开启路径均已覆盖（`openPortraitDrawer` 菜单按钮路径 + `settlePortraitDrawer(open=true)` 手势吸附路径）；**展开到位触感**：抽屉动画完全展开（fraction ≥ 0.999）时按设置里的按压震动时长/强度震动一次（`vibrateShort`，遵循「关闭应用内全部震动」总开关；已在全开状态或收起时不震）；**列表抬升**：三个布局分支的内容容器在净空 padding 之外追加 `.imePadding()`，键盘弹出时列表显示高度随之上移，习惯不被遮挡。**布局**：BottomCenter 对齐 + horizontal 16dp / bottom `OmniboxBottomMargin(20dp)`，位置带即原 FAB 区域略高；内容容器追加 `OmniboxContentBottomClearance(24dp)` 底部净空使列表可滚动范围在搜索框上方结束。**联动清理**：新建习惯 FAB 三个分支全部隐藏（`CreateHabitSelectionDialog` 保留），手动创建入口改为**习惯页顶部下拉释放触发**（`HabitScreenContent` 用 M3 `PullToRefreshBox(isRefreshing=false, onRefresh=onCreateHabitSelection)` 实现，注意其位于 `material3.pulltorefresh` 子包需单独 import；指示器为自定义加号图标——`rememberPullToRefreshState().distanceFraction` 驱动圆形 `secondaryContainer` 徽章的缩放淡入，非默认刷新样式）；顶栏 Habits/Contacts 搜索图标删除；旧顶部搜索框组件 `SearchBarFixed` 整体移除，`isSearchActive`/`onSearchActiveChange` 参数链路清除，`HabitScreenContent` 改收 `searchQuery: String` + `onClearSearch`（`searching = searchQuery.isNotBlank()` 驱动过滤数据源/空态/EntryZone 隐藏），`ContactsScreenContent` 内部直接收集 VM searchQuery。版本 0.8.58-alpha (206)
- ✅ **Omnibox → AI 对话页 连续形变（拖拽把手无极擦洗）** - 主页 Omnibox 升级为带横向拖拽把手的底部把手（自绘 `OmniboxDragHandle`，m3 `DragHandle` 在 1.5.0-alpha24 已更名不可用）：点按把手弹簧展开、纵向拖拽把手**无极擦洗**形变进度 p∈[0,1]（`aiSheetProgress: Animatable` + 自定义纵向手势：engage 时草稿交接+测速重置，松手动量投影 0.16s 取最近锚点，临界阻尼弹簧带初速收尾）。**架构决策**：Compose SharedTransition 无法跟手擦洗且无法跨 Activity 窗口 → 弃用，改为**主页窗口内嵌进度驱动形变层**：`AIChatScreen` 作为纯 composable 直接内嵌形变面片内（不经导航/AIChatActivity），数学上等价 sharedBounds 但完全可擦洗可中断可逆。**单一驱动源（踩坑修正）**：纵向拖拽最初同时挂在装饰容器与形变面片上——两节点随 p 越阈交替挂载/卸载，共存期双检测器同时消费同一指针流致进度翻倍「瞬间满屏」，且拖拽中途节点卸载会杀掉手势协程使松手 settle 永不执行、引发持续无效重组风暴（GC 狂刷卡死）；已改为**常驻主页根层的底部手势条**（仅覆盖把手落点 96×32dp，位于滑动页之上/形变层之下）作为唯一驱动：抽屉开启或 p≥0.999 时直接短路返回，engage 仅认上滑意图并就地完成草稿交接+测速重置；**行程距离防御（二次踩坑）**：三处 `BottomOmniboxWithFade` 调用点最初均未传 `onPillBounds`，`omniboxWindowBounds` 恒为 Rect.Zero → travelPx=max(top,1)=1px，首帧位移即把 p 顶满 1（「瞬间无动画满屏」）+ 一帧内强行组合整棵聊天树引发 GC 风暴/卡死；已接通全部回调、新增 `aiSheetTravelPx()` 屏高比例兜底，并在形变层加几何就绪守卫（bounds 未上报不渲染）；**松手 ANR（三次踩坑）**：p 原本在 HomeScreen 主组合作用域直接读取，动画每帧重组整棵主树，且 AIChatScreen 回调参数每帧都是新 lambda 实例导致其永不可 skip——聊天树逐帧全量重组打满主线程；已抽出文件级 `AiSheetOverlay`（BoxScope 扩展）把 p 读取隔离进最小重组作用域，回调经 `remember{}`+`rememberUpdatedState` 固化为稳定实例使 AIChatScreen 可跳过、alpha/门控改走绘制相位与 derivedStateOf 布尔阈值；松手收尾弃用大初速弹簧改确定性 tween(300)，动量意图仅由投影判定体现；**热自旋死循环（四次踩坑，ANR 真因）**：手势条守卫写在 `awaitFirstDown` 之前——`awaitEachGesture` 的语义是「block → 等全部指针抬起 → 无限循环」，block 在任何挂起点之前 return 且无按下指针时，循环**不经挂起立即重启**，完全展开态（p≥0.999 命中守卫）下变成 100% CPU 主线程热自旋（每轮分配事件快照 → 持续 GC/ANR）；修复=守卫一律移到首个挂起点之后（down 后 return 安全，finally 会等抬手）；**同型雷二次排查**：抽屉检测器的 `if (isAiSheetActive) return` 同样写在挂起点之前——拖拽期间因指针按下 finally 会挂起而无症状，松手后无指针可等立即热自旋（解释「拖拽流畅、松手即死」的完整时序）；已同款修复并全文件审计确认无第三处；**热区与首帧连续性（五次迭代）**：拖拽手势条从「仅把手落点 96×32dp」扩大到「把手+搜索框整体」（宽 = 屏宽-28dp、高 88dp、底距对齐胶囊），点击仍穿透到输入框（手势条不消费 tap，仅接管越过 slop 的上滑）；形变起点矩形改为与真实胶囊完全重合（不再上扩 headroom），半径从真实半高起步——拖拽启动首帧面片与 Omnibox 像素级重合零跳变，随后连续向上生长；**聚焦修复与常驻装饰（六次迭代）**：扩大后的根层手势条会遮挡输入框导致无法聚焦——改为删除独立手势条、把纵向拖拽直接挂回装饰容器自身，并让装饰在形变全程**保持挂载不卸载**（展开后被全屏面片覆盖不可见但节点存活）：手势协程天然贯穿全程无中断、输入框无遮挡可正常聚焦；检测器加 p≥0.999 短路守卫防止吞掉聊天列表滚动；展开前已输入文本经 engage 时的草稿交接自动填入展开后的聊天输入框（`rememberSaveable(initialInputText)` 种子）；**端点回弹与触感（七次迭代）**：三条收尾入口（手势 settle/把手点按展开/返回收缩）统一收敛到 `animateAiSheetTo()`——弹性弹簧（`DampingRatioMediumBouncy` + `StiffnessMediumLow`）产生完全展开/完全收起时的回弹动效，手势路径恢复注入释放初速（钳制 ±8 progress/s）末端跟随动量；到达端点落定后按全局震动设置震动一次（起点已在端点则跳过），过冲时面片短暂超出屏幕边界/缩小于胶囊均被底层常驻装饰无缝承接；**双把手消除（八次迭代）**：形变期间装饰把手（原位）与随面片上缘移动的幽灵把手曾同时可见——装饰容器新增 `showHandle` 参数（HomeScreen 经 `derivedStateOf { p<=0.001f && !aiSheetOpen }` 传入），形变启动即隐藏装饰把手视觉，但保留 64×24dp 固定占位尺寸防布局跳动、点击禁用；幽灵把手在面片上缘同像素位置接替，过渡无缝；**松手判定对齐抽屉（九次迭代）**：① 本次手势位移 ≥ 1/5 屏宽时无论速度按方向直接展开；② 更短手势用动量投影（0.16s）取最近锚点——快速轻扫短距即触发，慢速短拖保持原位；**形变细节**：变化的是背景面片而非输入框本身——起点矩形=胶囊边界向上扩展把手区高度，rect/圆角(胶囊半高→设备圆角)/颜色(surfaceContainerHighest→surface)三线连续插值；handle 幽灵层随拖拽上移渐隐至进度 1/4 完全透明（避让刘海）；页面压暗遮罩点击收回；对话页元素在后半程（p>0.3 组合、alpha 0.45→0.8 区间浮现）。**文本衔接**：展开瞬间 Omnibox 文本作为草稿移交（`aiDraftText`→`initialInputText` 种子进聊天输入框），交叉淡入方案。**联动**：形变激活期间抽屉横滑手势禁用、Omnibox 装饰隐藏、旋转复位收起（VM 会话保留）；聊天页返回按钮/系统返回→收缩回搜索框。**AIChatScreen 配套改造**：新增 `initialInputText`/`onManualCreateHabit` 参数；顶栏 Add 图标 + 欢迎区显眼「手动创建习惯」卡双入口（独立 Activity 路径经 `EXTRA_MANUAL_CREATE` 回 MainActivity 导航 `Route.CreateHabit`）；示例指令 chips 改 FlowRow 紧凑排布；`AIChatInputBox` 填充色统一为 surfaceContainerHighest、去描边去阴影与 Omnibox 匹配。版本 0.8.59-alpha (207)；**拖拽区域收窄（十次迭代）**：拖拽手势从整个 Omnibox 容器（含渐变背景区）收窄至仅把手+搜索框 Column（`dragModifier` 挂在 Column 而非外层 Box），空白渐变区不再误触拖拽；**端点回弹阻尼调整**：弹簧阻尼从 `DampingRatioMediumBouncy`(0.5) 提升至 0.7，回弹幅度收敛但仍可见；**Omnibox 渐隐**：形变过程中装饰层搜索框/把手通过 `graphicsLayer { alpha = 1-p }` 逐帧淡出至透明，回弹收缩时无缝衔接原位；**AI 界面精简**：移除 TopAppBar 手动创建习惯 + 号按钮（保留欢迎区卡片入口）；点击标题栏空白区域可下滑收起（仅 AI 未输出时生效）；返回仅在 AI 仍在输出时弹确认对话框（会话内容全程保留在 ViewModel 内不丢失）；示例指令 FlowRow 间距收紧；**顶栏反向拖拽收起（十一次迭代）**：AIChatScreen 顶栏区域支持下拉跟手收回至搜索框（反向擦洗 `progress`，松手半值判定开合），返回按钮改为向下箭头（↓）；Omnibox drag handle 新增 TalkBack 语义「打开AI助手」；Token 用量与设置合并为三点菜单，新增新建对话图标（仅输出终止后启用）；设置入口直接导向 AI 配置页面；**横屏适配（十二次迭代）**：Omnibox → AI 形变覆盖方案扩展至手机横屏（NavigationRail）和平板横屏（PermanentNavigationDrawer），三种布局分支均共享 `AiSheetOverlay` + `aiSheetStripDragModifier` + `BottomOmniboxWithFade` 完整参数；`aiSheetProgress` 改用 `rememberSaveable` 自定义 Saver 保存进度，屏幕旋转后形变状态不丢失（对话内容保留在 ViewModel 中）；`portraitScreenWidthPx`/`portraitScreenHeightPx` 重命名为 `sheetScreenWidthPx`/`sheetScreenHeightPx` 供所有分支共用；`deviceCornerRadius` 提升至共享作用域；平板横屏分支额外包裹外层 `Box` + `onSizeChanged`，`AiSheetOverlay` 移至 `PermanentNavigationDrawer` 之外确保全屏覆盖（含 Drawer）

- ✅ **空态居中修正 + 记录搜索 + Omnibox AI 快捷 chip + AI 对话清理与顶栏下拉收回重建** - 版本 0.8.62-alpha (210) 五项体验修复与增强：
  - **空状态占位垂直居中**：习惯/联系人/记录三界面空态占位（没有习惯/没有联系人/未找到结果）此前在延伸到屏幕底部的容器内 `Arrangement.Center` 居中，被 Omnibox 装饰层（把手24dp + 间距6dp + 胶囊52dp + 底距20dp ≈ 102dp + 导航栏 inset）叠压导致视觉中心偏低；HomeScreen 抽出 `OmniboxHandleBoxHeight`/`OmniboxHandlePillSpacing`/`OmniboxPillMinHeight` 常量并新增 internal 共享 `Modifier.emptyStateOmniboxClearance()`（= 装饰总高 − 内容区已有 `OmniboxContentBottomClearance(24dp)` + `windowInsetsPadding(navigationBars bottom)`），`EmptyStateContent`/`SearchEmptyState`/`EmptyContactsContent`/`EmptyRecordsContent` 统一追加；**记录页特殊处理**：根容器 Column→Box，空态/加载态时 `FilterBarSection(modifier)` 悬浮顶部可继续操作、占位铺满整屏居中（忽略筛选 Chip 行高度，与其他界面观感一致）；有数据时维持 Column 布局不变；**图标颜色统一 onSurface** 与标题文字一致（原 secondary/onSurfaceVariant 混用）
  - **记录界面 Omnibox 搜索**：`RecordsScreenContent` 新增 `searchQuery`/`onClearSearch` 参数接通 Omnibox 查询词，按 `habit.title.contains(trimmed, ignoreCase=true)` 过滤各日期组记录并剔除空组（输入「跑」匹配「长跑」「晨跑」）；搜索时直接用实时 `groupedRecords`、绕过 `lastNonEmptyData` 缓存防闪旧数据；无匹配显示新私有组件 `RecordsSearchEmptyState`（复用 `search_no_results` 文案 + 清除搜索按钮，同样走居中净空）；日期筛选/习惯筛选/进入 Records 重置逻辑保留
  - **Omnibox AI 快捷 chip（双 chip 并列，无意图判断）**：有输入且已配置 AI（HomeScreen 收集 `UserPreferences.aiConfigsFlow` 任一 `hasApiKeyConfigured()` 得 `aiAssistEnabled`）时在输入框上方左对齐同时显示「用 AI 创建」「用 AI 查询」两枚 AssistChip——扁平样式（`AssistChipDefaults.assistChipElevation()` 全交互态 0 阴影、不透明 `surfaceContainerHighest` 容器与 Omnibox 胶囊同色，禁用态同色，列表滚过不影响可读性）、120ms 淡入/100ms 淡出简约入场（无位移展开）、`PressVibrationFeedback` 触感、`showHandle` 同时作可点击门控防形变淡出期误触；点击经 `sendOmniboxToAi(promptRes, text)` 清空搜索框 + 展开对话页，固定模板提示词（`ai_chat_auto_create_prompt`「请帮我创建习惯：「%1$s」」/ `ai_chat_auto_search_prompt`「请帮我查找与「%1$s」相关的习惯」，6 locale）写入 `pendingAiPrompt`（rememberSaveable）移交 AIChatScreen——新增 `autoSendText`/`onAutoSendConsumed` 参数，以 progress 为 key 的 LaunchedEffect 在完全展开且非输出中时以用户气泡自动发送一次后回调消费（不依赖 AI 判断意图，创建/查询由用户点选 chip 决定）；未配置 AI 时两枚 chip 都不显示；收起时在 `animateAiSheetTo(!open)` 中丢弃未发送提示词防下次展开误发
  - **AI 对话页清理与触感补齐**：欢迎态移除 3 个示例提问 chips 及 6 locale `ai_chat_suggestion_*` 字符串（保留手动创建卡/添加提供商入口，`AIWelcomeContent` 删除 `onSuggestionClick` 参数）；三点菜单项文案改用现成 `settings_category_ai`（「AI 配置」，MoreVert contentDescription 与 SettingsNavCard "ai" 页标签同步；导航本就指向 `SettingsAIActivity` 不变）；补齐 DropdownMenuItem×3（Token 用量/AI 配置/手动创建）、暗色模式下拉×3、时间选择对话框添加时间按钮的 PressVibrationFeedback（DropdownMenuItem 经 interactionSource 挂载）

- ✅ **平板横屏设置双栏（Android 15 风格）** - 版本 0.8.63-alpha (211)：平板横屏时设置界面重写为双栏布局，其余形态/手机路径完全不变：
  - **布局**：`SettingsActivity` 根路由按「有效平板横屏」判定分流——真平板横屏或非平板开启「强制平板横屏显示」后横屏走新 `SettingsTwoPaneScreen`（判定公式与主页 `effectiveIsPermanentDrawer` 一致）；左栏固定 360dp：返回按钮在上（从设置整体返回上一页）、加大 headlineLarge「设置」在下、分类列表随当前子页显示 `SettingsSegmentedItem(selected=true)` 高亮，点击任意分类随时重置右栏栈切换；右栏为悬浮圆角面板——本体 `surface`（整屏画布与左栏同用 `surfaceContainer` 变体色、四周空隙同色连续）+ 设备圆角 + 四周 12dp 边距 + `shadowElevation=0/tonalElevation=0` 零阴影，内部 `Crossfade(150ms)` 渲染当前页；`SettingsSegmentedItem` 新增 `containerColorOverride: Color? = null` 并转发 `SettingsListSurface`，左栏列表项未选中态反转为 `surface` 与面板底色呼应（选中态不变，其余调用点默认零影响）
  - **内嵌路由栈**：`rememberSaveable`（listSaver 存 enum name，旋转保持）维护 `SettingsPage` 枚举栈，默认进入通用设置；通知→模板、关于→调试→{提醒调试/震动调试/UI catalog} 全部栈内导航；**全屏特例**经回调交宿主独立 Activity 打开——语言、字体大小（需重建 Activity）、AI 编辑配置（BiometricPrompt 要求 FragmentActivity 宿主，本次决定不迁移宿主故保持全屏）；帮助与反馈 WebView 天然全屏
  - **返回体系**：`BackHandler` 栈>1 弹一层、分类根再返回即 finish 设置；`SettingsScaffold` 新增 `showBack: Boolean = true` 并穿透 9 个子 Screen——右栏四个分类根页隐藏返回按钮（无处可返回），二级/三级页保留返回=弹栈；帮助按钮只在右栏子页 app bar 显示（左栏不重复）
  - **强制平板横屏开关当场生效**：`SettingsGeneralScreen` 开关持久化前后对比「有效平板横屏」判定，变化即 `recreate()` 宿主设置 Activity（开/关都覆盖；真平板竖屏开开关因判定不变不触发）；仅影响设置界面，其他界面自行响应数据流
### In Progress
- 🔄 Calendar section

### Planned
- ⏳ Reminder system with AlarmManager
- ⏳ Social supervision features (email/SMS notifications)
- ⏳ Calendar view (full implementation)
- ⏳ AI habit suggestions
- ⏳ Data backup/export

## Package Information

- **Namespace**: `io.github.darrindeyoung791.habitpulse`
- **Application ID**: `io.github.darrindeyoung791.habitpulse`
- **Version Code**: 211
- **Version Name**: 0.8.63-alpha

## Screen Flow

```
┌──────────────┐      ┌──────────────┐      ┌─────────────────┐
│              │      │              │      │                 │
│LauncherActivity│───▶│WelcomeActivity│      │SettingsActivity │
│              │      │                  │      │                 │
│  Route logic │      │  4-step guide   │      │  - App info     │
└──────┬───────┘      └──────┬───────┘      │  - Visual opts  │
       │                     │              │  - About        │
       │                     ▼              │  - GitHub link  │
       │              ┌──────────────┐      │                 │
       │              │              │      └────────┬────────┘
       └─────────────▶│MainActivity  │               │
                      │  (NavHost)   │◀──────────────┘
                      │              │
                      └──────┬───────┘
                             │
            ┌────────────────┼────────────────┐
            ▼                ▼                ▼
       ┌──────────┐    ┌──────────┐    ┌──────────────┐
       │ HomeScreen│    │HabitCreate│    │MultiSelect   │
       │  (Shell)  │◀──▶│  Screen   │    │Sort Screen   │
       │          │    │          │    │              │
       │ - Habits │    │ - Create │    │ - Reorder    │
       │ - Contacts│   │ - Edit   │    │ - Batch del  │
       │ - Records│    └──────────┘    └──────────────┘
       └──────────┘
          │
          ├── HabitScreenContent (Habit list, cards, search)
          ├── ContactsScreenContent (Supervisor contacts)
          └── RecordsScreenContent (Completion records)
```

### Responsive Navigation System

The app uses a responsive navigation system that adapts to screen size and orientation. All device-form decisions (tablet/phone, landscape, wide layout) come from the unified `rememberDeviceFormInfo()` in `ui/DeviceFormFactor.kt` — pages must NOT read `LocalConfiguration`/`smallestScreenWidthDp`/`screenWidthDp`/`orientation` directly:

| Device/Orientation | Derived Flag | Navigation Mode | FAB | Hamburger Menu |
|---|-----------|---|---|---|
| Phone Portrait | `!isLandscape`   | Bottom Navigation Bar | ✅ Extended | ❌ |
| Phone Landscape | `isPhoneLandscape` | Navigation Rail | ✅ Extended | ❌ |
| Tablet Portrait | `isTabletDevice && !isLandscape` | Bottom Navigation Bar | ✅ Extended | ❌ |
| Tablet Landscape | `isTabletLandscape` | Permanent Navigation Drawer | ✅ Extended | ✅ |

**Permanent Navigation Drawer Behavior (Tablet Landscape)**

- **Collapsed (80dp)**: Icons only with circular selection indicator
- **Expanded (240dp)**: Icons + text labels
- Smooth width animation (300ms tween)
- Hamburger menu icon toggles between expand/collapse states

## Git Branch Strategy

- **`master`**: Main development branch. Contains all source code and resources.
- **`docs`**: Documentation branch. Contains user-facing VitePress docs in `docs/` directory. Kept in sync with `master` via periodic fast-forward merges (`git checkout docs && git pull origin master`).
- All other branches are feature/topic branches for specific work items.

## Notes

- The app uses Aliyun Maven mirrors for better connectivity in China (`maven.aliyun.com`)
- Dynamic color theming is enabled for Android 12+ (API 31+)
- Edge-to-edge display is enabled in all activities
- Release builds have minification and resource shrinking enabled
- All activities support multi-window modes
- Back gesture handling uses `enableOnBackInvokedCallback`

## Development Guidelines & Lessons Learned

### DatePicker Implementation Pattern (March 2026)

**Problem**: When implementing orientation-dependent DatePicker dialogs, using multiple `if` conditions can cause both dialogs to appear simultaneously during recomposition, even when conditions appear mutually exclusive.

**Solution**: Extract the DatePicker into a separate `@Composable` function and use `key()` to force complete recreation when orientation changes.

**Correct Pattern**:
```kotlin
// In main composable
val configuration = LocalConfiguration.current
val screenWidthDp = configuration.screenWidthDp
val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
val isPhoneLandscape = screenWidthDp < 1200 && isLandscape  // Match HomeScreen logic

if (datePickerExpanded) {
    DatePickerContent(
        isPhoneLandscape = isPhoneLandscape,
        selectedDate = selectedDate,
        onDismiss = { viewModel.setDatePickerExpanded(false) },
        onDateSelected = { date -> viewModel.selectDate(date) }
    )
}

// Separate private composable function
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DatePickerContent(
    isPhoneLandscape: Boolean,
    selectedDate: LocalDate?,
    onDismiss: () -> Unit,
    onDateSelected: (LocalDate) -> Unit
) {
    // CRITICAL: Use key() to force complete recreation when orientation changes
    key(isPhoneLandscape) {
        val initialDisplayMode = if (isPhoneLandscape) DisplayMode.Input else DisplayMode.Picker
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = selectedDate?.atStartOfDay()?.toInstant(java.time.ZoneOffset.UTC)?.toEpochMilli()
                ?: System.currentTimeMillis(),
            initialDisplayMode = initialDisplayMode
        )

        DatePickerDialog(
            onDismissRequest = onDismiss,
            // ... buttons
        ) {
            DatePicker(state = datePickerState)
        }
    }
}
```

**Key Points**:
1. **Single `if` condition**: Only check `datePickerExpanded` in the main function, not orientation
2. **Extract to separate composable**: Move DatePicker logic to a private `@Composable` function
3. **Use `key()` wrapper**: Wrap the entire DatePicker creation in `key(isPhoneLandscape)` to force recreation
4. **Pass callbacks**: Use lambda parameters for dismiss and date selection actions
5. **Match HomeScreen logic**: Use `screenWidthDp < 1200 && isLandscape` for phone landscape detection (consistent with app's navigation logic)

**Why This Works**:
- `key()` forces Compose to completely destroy and recreate the composable when the key value changes
- This prevents state bleeding between orientation changes
- Separate function ensures clean composition scope
- Single `if` condition eliminates race conditions during recomposition

**Device Detection Thresholds** (consistent with app navigation):
- Phone Landscape: `screenWidthDp < 1200 && isLandscape` → Use `DisplayMode.Input`
- All other cases (portrait, tablet): Use `DisplayMode.Picker`

### Screen Architecture Pattern (April 2026)

**Problem**: Child screens (RecordsScreen, ContactsScreen) should not manage TopAppBar or shared dialogs. When DatePicker dialog logic exists in both parent (HomeScreen) and child (RecordsScreen), clicking the date filter button triggers both dialogs simultaneously.

**Solution**: Single Source of Truth architecture - Parent (HomeScreen) manages all chrome elements (TopAppBar, dialogs), child screens focus on content only.

**Architecture**:
```
┌─────────────────────────────────────────────────────────────┐
│                      HomeScreen                              │
│  ┌────────────────────────────────────────────────────────┐  │
│  │                    TopAppBar                            │  │
│  │  - Title                                                │  │
│  │  - Actions (Search, DateFilter, Settings)              │  │
│  │  - DatePicker Dialog (managed here, single instance)   │  │
│  └────────────────────────────────────────────────────────┘  │
│  ┌────────────────────────────────────────────────────────┐  │
│  │              Child Screen Content                      │  │
│  │  - RecordsScreenContent (NO dialog, pure content)      │  │
│  │  - ContactsScreenContent (NO dialog, pure content)     │  │
│  │  - HabitListContent (NO dialog, pure content)          │  │
│  └────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────┘
```

**Implementation Guidelines**:

1. **State Ownership**:
   - ViewModel owns UI state (e.g., `datePickerExpanded`, `selectedDate`)
   - Parent (HomeScreen) reads/writes state to manage dialogs
   - Child screens only read necessary states for content rendering

2. **Dialog Management**:
   - All dialogs are declared at the END of the parent composable, after Scaffold/NavigationDrawer
   - Use `key()` pattern for orientation-dependent dialogs
   - Child screens NEVER declare dialogs

3. **TopAppBar Actions**:
   - Action buttons (DateFilterButton, Search icon) are declared in TopAppBar actions
   - Buttons trigger state changes in ViewModel (e.g., `setDatePickerExpanded(true)`)
   - Dialog rendering is separate from button declaration

**Example Structure**:
```kotlin
@Composable
fun HomeScreen(...) {
    // ... state collection

    // TopAppBar with action buttons
    val topAppBarContent: @Composable (Boolean) -> Unit = { isRailVisible ->
        TopAppBar(
            actions = {
                // Button triggers state change, NOT dialog
                if (currentSection == HomeSection.Records) {
                    DateFilterButton(
                        selectedDate = recSelectedDate,
                        onDateSelected = { recordsVM.setDatePickerExpanded(true) },
                        onDateCleared = { recordsVM.clearDate() }
                    )
                }
            }
        )
    }

    // Scaffold with child screen content
    Scaffold(
        topBar = { topAppBarContent(false) },
        content = { paddingValues ->
            when (currentSection) {
                HomeSection.Records -> RecordsScreenContent(...)  // Pure content, no dialog
                // ...
            }
        }
    )

    // Dialog declared ONCE at end of parent, after Scaffold
    if (recDatePickerExpanded) {
        DatePickerDialogContent(
            isPhoneLandscape = useRail,
            selectedDate = recSelectedDate,
            onDismiss = { recordsVM.setDatePickerExpanded(false) },
            onDateSelected = { recordsVM.selectDate(it) }
        )
    }
}

// Separate dialog composable with key() pattern
@Composable
private fun DatePickerDialogContent(...) {
    key(isPhoneLandscape) {
        // ... dialog implementation
    }
}
```

**Benefits**:
- **No duplicate dialogs**: Single dialog instance managed by parent
- **Clear separation of concerns**: Parent manages chrome, children manage content
- **Maintainable**: Dialog logic changes only need to be made in one place
- **Consistent**: All screens follow the same architecture pattern

**Device Detection** (consistent across app):
- Phone Landscape: `screenWidthDp < 1200 && isLandscape` (use `useRail` variable from HomeScreen)
- Tablet/Portrait: All other cases

### Theme Switch Black Screen Fix (June 2026)

**Background**: Switching system dark/light mode caused a completely black, unresponsive screen with no logcat errors. Restarting the app fixed it. The app uses `isSystemInDarkTheme()` (no custom theme toggle) and `installSplashScreen()`.

**Root Cause**: Deadlock between three interacting components:
1. **`configChanges` missing `uiMode`** — `AndroidManifest.xml` declared `configChanges` without `uiMode`. System dark mode change triggered Activity destruction + recreation, which re-ran `installSplashScreen()`.
2. **`AnimatedVisibility` gating content** — Main content was wrapped in `AnimatedVisibility(visible = contentFadeInStarted)` with `contentFadeInStarted = false` initially. `HomeScreen` never entered the composition tree, so `onHomeDataLoaded()` was never called.
3. **SplashScreen stuck forever** — `setKeepOnScreenCondition { !homeDataLoaded }` kept returning `true`. The SplashScreen library's custom overlay (`@color/black` in dark mode) covered the app permanently, appearing as a black unresponsive screen.

**Fix** (3 changes):
1. Added `uiMode` to all Activity `configChanges` — prevents recreation; Compose reacts via `LocalConfiguration`.
2. Removed `AnimatedVisibility` fade-in wrapper and `contentFadeInStarted` state — content renders immediately, breaking the deadlock.
3. Replaced `Box` + `.background()` with `Surface` at root — `Surface` propagates `LocalContentColor` correctly during theme transitions (unlike bare `.background()`).

**Key Takeaways**:
- `configChanges` MUST include `uiMode` in Compose projects using `isSystemInDarkTheme()`.
- Never gate critical initialization (e.g. data loading) behind `AnimatedVisibility(visible = false)` — hidden content never composes, so nothing initializes.
- `setKeepOnScreenCondition` reads Compose `State` synchronously but is evaluated by the non-Compose SplashScreen library. Any state that depends on content visibility creates a latent deadlock.
- `Surface` is not just a "background box" — it sets `LocalContentColor` which affects all child composables during theme transitions.
- When Activity recreation happens (even briefly) and ViewModel is application-scoped, `collectAsStateWithLifecycle` initializes with `initialValue` until the Flow emits — this timing window can cause issues.

## Qwen Added Memories
- VitePress docs (docs/) should be user-facing product feature introductions. Old doc/ folder contains technical/dev documentation. Two audiences: users (VitePress) and developers (doc/).
