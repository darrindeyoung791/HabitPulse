# HabitPulse - Project Context

## Project Overview

**HabitPulse** is an Android habit tracking application built with Kotlin and Jetpack Compose. The app helps users build and maintain daily habits through smart reminders and social supervision features.

> **⚠️ IMPORTANT — AI agents: When project structure, features, dependencies, or conventions change significantly, you MUST update BOTH `AGENTS.md` AND `QWEN.md` to keep them in sync.**

### Key Features
- **Habit Tracking**: Create, manage, and track daily habits with custom repeat cycles
- **Completion History**: Track every check-in with timestamps, local dates, and timezone support
- **Records & Analytics**: View completion history with date filtering and habit-specific filtering
- **Social Supervision**: Link supervisor contacts (email/phone) to habits for accountability
- **Multi-Select & Reorder**: Drag-and-drop reordering with batch delete functionality
- **Smart Search**: Search habits with instant filtering
- **Foreground Service**: Keep-alive service with boot auto-restart for reliability
- **Material Design 3**: Clean, modern UI following MD3 guidelines with dynamic colors
- **Responsive Layout**: Adaptive navigation (Bottom Bar, Rail, Drawer) based on screen size
- **Split-screen Support**: Multi-window support enabled
- **Predictive Back Gesture**: Android 13+ predictive back gesture support
- **Localization**: Chinese (Simplified/Traditional) and English (US) support
- **Accessibility**: TalkBack support for all navigation elements

### Tech Stack
- **Language**: Kotlin
- **UI Framework**: Jetpack Compose with Material Design 3
- **Navigation**: Navigation Compose with custom animations and shared transitions
- **Database**: Room 2.8.4 (v3 schema)
- **Preferences**: DataStore for user settings, SharedPreferences for onboarding state
- **Foreground Service**: Android foreground service for keep-alive
- **Scheduling**: AlarmManager (planned)
- **Build System**: Gradle (Kotlin DSL)
- **Minimum SDK**: 26 (Android 8.0)
- **Target SDK**: 36

## Project Structure

```
HabitPulse/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/io/github/darrindeyoung791/habitpulse/
│   │   │   │   ├── MainActivity.kt              # Main entry point with NavHost
│   │   │   │   ├── SettingsActivity.kt          # Settings screen
│   │   │   │   ├── LauncherActivity.kt          # Launcher that routes to Welcome or MainActivity
│   │   │   │   ├── WelcomeActivity.kt           # Onboarding/welcome flow
│   │   │   │   ├── OpenSourceLicensesActivity.kt # Open source licenses display
│   │   │   │   ├── HabitPulseApplication.kt     # Application class with singleton init
│   │   │   │   ├── navigation/
│   │   │   │   │   ├── HabitPulseNavGraph.kt    # Navigation graph with animations
│   │   │   │   │   └── Route.kt                 # Route definitions
│   │   │   │   ├── data/
│   │   │   │   │   ├── model/
│   │   │   │   │   │   ├── Habit.kt             # Habit entity with Room annotations
│   │   │   │   │   │   └── HabitCompletion.kt   # Habit completion record entity
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
│   │   │   │   │   │   ├── WelcomeScreen.kt     # Onboarding/consent screen
│   │   │   │   │   │   └── AdScreen.kt          # Splash ad screen
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
│   │   │   │       └── OnboardingPreferences.kt       # SharedPreferences for onboarding state
│   │   │   ├── res/                             # Android resources
│   │   │   │   ├── values/strings.xml           # Chinese strings
│   │   │   │   ├── values-en-rUS/strings.xml    # English strings
│   │   │   │   ├── values-zh-rCN/               # Chinese (Simplified)
│   │   │   │   ├── values-zh-rHK/               # Chinese (Traditional, Hong Kong)
│   │   │   │   ├── values-zh-rTW/               # Chinese (Traditional, Taiwan)
│   │   │   │   └── values-night/                # Dark theme overrides
│   │   │   └── AndroidManifest.xml
│   │   ├── test/                                # Unit tests
│   │   └── androidTest/                         # Instrumented tests
│   ├── build.gradle.kts                         # App-level build config
│   └── proguard-rules.pro                       # ProGuard rules
├── gradle/
│   └── wrapper/
│       └── gradle-wrapper.properties            # Gradle 9.4.0
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
| supervisionMethod | TEXT | NONE, EMAIL, or SMS |
| supervisorEmails | TEXT | Supervisor emails (JSON format) |
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
- Android SDK with API level 36

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
- **Gradle**: 9.4.0
- **Android Gradle Plugin**: 9.1.0
- **Kotlin**: 2.3.20
- **KSP**: 2.3.6

## Dependencies

### Core
- `androidx.core.ktx` (1.17.0) - Kotlin extensions for Android
- `androidx.lifecycle.runtime.ktx` (2.10.0) - Lifecycle components
- `androidx.lifecycle.viewmodel.compose` (2.8.7) - ViewModel Compose integration
- `androidx.activity.compose` (1.10.1) - Compose integration with Activity
- `androidx.activity.ktx` (1.10.1) - Kotlin extensions for Activity
- `androidx.navigation.compose` (2.8.0) - Navigation Compose

### Room Database
- `androidx.room:room-runtime` (2.8.4) - Room database runtime
- `androidx.room:room-ktx` (2.8.4) - Room Kotlin coroutines support
- `androidx.room:room-compiler` (2.8.4) - Room annotation processor (KSP)

### Compose UI
- `androidx.compose.bom` (2026.03.00) - Compose Bill of Materials
- `androidx.compose.ui` - Core Compose UI
- `androidx.compose.material3` (1.4.0) - Material Design 3 components
- `androidx.compose.material.icons.core` (1.7.6) - Material icons core
- `androidx.compose.material.icons.extended` (1.7.6) - Material icons extended

### Other
- `androidx.core:core-splashscreen` (1.0.1) - Splash screen compatibility
- `com.google.android.material:material` (1.10.0) - Material components for dynamic colors
- `sh.calvin.reorderable:reorderable` (3.0.0) - Drag-and-drop reordering library
- `androidx.datastore:datastore-preferences` (1.1.1) - Modern preferences storage
- `com.mikepenz:aboutlibraries` (13.2.1) - Open source license display

### Testing
- `junit` (4.13.2) - Unit testing framework
- `androidx.junit` (1.2.1) - Android JUnit extensions
- `androidx.espresso.core` (3.6.1) - UI testing framework
- `androidx.compose.ui.test` - Compose testing utilities

## Development Conventions

### Code Style
- **Kotlin style**: Official (as per `gradle.properties`)
- **JVM target**: Java 17
- **Compose**: Enabled with Material Design 3
- **i18n**: Simplified Chinese, Traditional Chinese, and English(US)

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

### Internationalization (i18n) Guidelines
- **No hardcoded strings**: All user-visible strings must be stored in `strings.xml` resource files
- **String resource location**:
  - Default (Chinese): `res/values/strings.xml`
  - English: `res/values-en-rUS/strings.xml`
- **Usage in Compose**: Use `stringResource(R.string.resource_name)` to retrieve localized strings
- **Naming convention**: Use snake_case for string resource names (e.g., `habit_creation_title`)
- **Supported languages**:
  - Chinese (Simplified) - Default
  - Chinese (Traditional, Hong Kong) - `res/values-zh-rHK/`
  - Chinese (Traditional, Taiwan) - `res/values-zh-rTW/`
  - English (US)
- **Auto-mirrored icons**: Icons with directional meaning (e.g., arrows, back/forward) should use `autoMirrored="true"` in drawable resources for RTL support
- **Agent requirement**: When adding new UI text, always:
  1. Add string resources to `values/strings.xml` and all locale-specific `strings.xml` files (`values-en-rUS/strings.xml`, `values-zh-rHK/strings.xml`, `values-zh-rTW/strings.xml`)
  2. Reference via `R.string.*` in code, never inline string literals

## Current Status

The project is in **early development stage** (v0.5.19-alpha):

### Completed
- ✅ Project structure set up
- ✅ Basic Compose theme configured with Monet dynamic colors
- ✅ Navigation Compose integrated with custom animations and shared transitions
- ✅ LauncherActivity for routing between Welcome and MainActivity
- ✅ WelcomeActivity with onboarding/consent flow and permission requests
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
- ✅ Localization (Simplified Chinese, Traditional Chinese, English) with values-zh-rCN, values-zh-rHK, values-zh-rTW, values-en-rUS, values-night
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

### In Progress
- 🔄 Count section (track unplanned events, such as game scores)
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
- **Version Code**: 119 (incremented for home screen refactoring)
- **Version Name**: 0.5.20-alpha

## Screen Flow

```
┌──────────────┐      ┌──────────────┐      ┌─────────────────┐
│              │      │              │      │                 │
│LauncherActivity│───▶│WelcomeActivity│      │SettingsActivity │
│              │      │              │      │                 │
│  Route logic │      │  Consent     │      │  - App info     │
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

The app uses a responsive navigation system that adapts to screen size and orientation:

| Device/Orientation | Threshold | Navigation Mode | FAB | Hamburger Menu |
|---|-----------|---|---|---|
| Phone Portrait | < 840dp   | Bottom Navigation Bar | ✅ Extended | ❌ |
| Phone Landscape | < 1200dp  | Navigation Rail | ✅ Extended | ❌ |
| Tablet Portrait | ≥ 840dp   | Bottom Navigation Bar | ✅ Extended | ❌ |
| Tablet Landscape | ≥ 1200dp  | Permanent Navigation Drawer | ✅ Extended | ✅ |

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

## Qwen Added Memories
- VitePress docs (docs/) should be user-facing product feature introductions. Old doc/ folder contains technical/dev documentation. Two audiences: users (VitePress) and developers (doc/).
