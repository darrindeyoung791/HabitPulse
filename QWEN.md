# HabitPulse - Project Context

## Project Overview

**HabitPulse** is an Android habit tracking application built with Kotlin and Jetpack Compose. The app helps users build and maintain daily habits through smart reminders and social supervision features.

The project is rapidly developing, LLM should update this page when big changes occur.

### Key Features
- **Habit Tracking**: Create, manage, and track daily habits
- **Smart Reminders**: Time-based notifications to help users stick to habits (planned)
- **Social Supervision**: Habit completion notifications to designated contacts (planned)
- **Background Service**: Long-running background service for reliable reminders (planned)
- **Material Design 3**: Clean, modern UI following MD3 guidelines
- **Split-screen Support**: Multi-window and picture-in-picture mode enabled
- **Predictive Back Gesture**: Android 13+ predictive back gesture support
- **Localization**: Chinese (Simplified) and English (US) support

### Tech Stack
- **Language**: Kotlin
- **UI Framework**: Jetpack Compose with Material Design 3
- **Navigation**: Navigation Compose with custom animations
- **Database**: Room 2.8.4 (implemented)
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
│   │   │   │   ├── HabitPulseApplication.kt     # Application class with DB init
│   │   │   │   ├── navigation/
│   │   │   │   │   ├── HabitPulseNavGraph.kt    # Navigation graph with animations
│   │   │   │   │   └── Route.kt                 # Route definitions
│   │   │   │   ├── data/
│   │   │   │   │   ├── model/
│   │   │   │   │   │   └── Habit.kt             # Habit entity with Room annotations
│   │   │   │   │   ├── database/
│   │   │   │   │   │   ├── HabitDatabase.kt     # Room database class
│   │   │   │   │   │   ├── dao/
│   │   │   │   │   │   │   └── HabitDao.kt      # Data Access Object
│   │   │   │   │   │   └── converter/
│   │   │   │   │   │       ├── ListConverters.kt    # List<Int>, List<String> converters
│   │   │   │   │   │       └── EnumConverters.kt    # Enum type converters
│   │   │   │   │   └── repository/
│   │   │   │   │       └── HabitRepository.kt   # Repository pattern for data access
│   │   │   │   ├── viewmodel/
│   │   │   │   │   └── HabitViewModel.kt        # ViewModel for UI state management
│   │   │   │   └── ui/
│   │   │   │       ├── screens/
│   │   │   │       │   ├── HomeScreen.kt        # Home screen with habit list
│   │   │   │       │   └── HabitCreationScreen.kt # Create/Edit habit screen
│   │   │   │       └── theme/
│   │   │   │           ├── Color.kt             # Color definitions
│   │   │   │           ├── Theme.kt             # Material theme setup
│   │   │   │           └── Type.kt              # Typography definitions
│   │   │   ├── res/                             # Android resources
│   │   │   │   ├── values/strings.xml           # Chinese strings
│   │   │   │   └── values-en-rUS/strings.xml    # English strings
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

## Building and Running

### Prerequisites
- Android Studio (latest version recommended)
- JDK 11 or higher
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
- **Kotlin**: 2.3.10

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
- `androidx.compose.bom` (2025.01.00) - Compose Bill of Materials
- `androidx.compose.ui` - Core Compose UI
- `androidx.compose.material3` (1.4.0) - Material Design 3 components
- `androidx.compose.material.icons.core` (1.7.6) - Material icons core
- `androidx.compose.material.icons.extended` (1.7.6) - Material icons extended

### Other
- `androidx.core:core-splashscreen` (1.0.1) - Splash screen compatibility
- `com.google.android.material:material` (1.10.0) - Material components for dynamic colors

### Testing
- `junit` (4.13.2) - Unit testing framework
- `androidx.junit` (1.2.1) - Android JUnit extensions
- `androidx.espresso.core` (3.6.1) - UI testing framework
- `androidx.compose.ui.test` - Compose testing utilities

## Development Conventions

### Code Style
- **Kotlin style**: Official (as per `gradle.properties`)
- **JVM target**: Java 11
- **Compose**: Enabled with Material Design 3

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

### Testing Practices
- Unit tests in `src/test/`
- Instrumented tests in `src/androidTest/`
- Compose UI testing with `androidx.compose.ui.test`

## Current Status

The project is in **early development stage** (v0.1.31-alpha):

### Completed
- ✅ Project structure set up
- ✅ Basic Compose theme configured
- ✅ Navigation Compose integrated
- ✅ Home screen with habit list from database
- ✅ Habit creation screen with navigation
- ✅ Habit edit screen with data loading
- ✅ Settings screen (separate Activity)
- ✅ Custom screen transition animations
- ✅ Device corner radius support (Android 12+)
- ✅ Predictive back gesture support
- ✅ Split-screen and PiP support
- ✅ Localization (Chinese & English)
- ✅ Room database integration (v2.8.4)
- ✅ Habit entity with UUID primary key
- ✅ HabitDao with CRUD operations and Flow support
- ✅ HabitRepository for data abstraction
- ✅ HabitViewModel for UI state management
- ✅ Habit completion toggle functionality
- ✅ Responsive navigation system (NavigationRail, NavigationBar, PermanentNavigationDrawer)
- ✅ Collapsed NavigationBar with circular selection indicator for tablet landscape
- ✅ Animated drawer width for permanent navigation drawer
- ✅ Adaptive horizontal padding for habit cards based on screen size
- ✅ TalkBack accessibility support for all navigation elements
- ✅ Habit card animations (scale + slide enter/exit effects)
- ✅ Dynamic app bar title font size (changes on scroll)
- ✅ Delayed enter animation for newly added habits (after navigation completes)
- ✅ Smooth reposition animation for other cards (animateContentSize)

### In Progress
- 🔄 Habit repeat days selection (weekly cycle)
- 🔄 Reminder time management

### Planned
- ⏳ Reminder system with AlarmManager
- ⏳ Habit completion tracking with daily reset
- ⏳ Social supervision features
- ⏳ Email/SMS integration
- ⏳ Calendar view
- ⏳ Data backup/export

## Package Information

- **Namespace**: `io.github.darrindeyoung791.habitpulse`
- **Application ID**: `io.github.darrindeyoung791.habitpulse`
- **Version Code**: 1
- **Version Name**: 0.1.31-alpha

## Screen Flow

```
┌─────────────────┐     ┌──────────────────────┐     ┌─────────────────┐
│                 │     │                      │     │                 │
│   HomeScreen    │────▶│ HabitCreationScreen  │     │ SettingsActivity│
│                 │◀────│                      │     │                 │
│  - Habit list   │     │  - Create/Edit form  │     │  - App info     │
│  - Empty state  │     │  - Habit name input  │     │  - Version      │
│  - FAB (create) │     │  - More settings...  │     │  - GitHub link  │
│                 │     │                      │     │                 │
└─────────────────┘     └──────────────────────┘     └─────────────────┘
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

## Notes

- The app uses a custom mirror for Gradle distribution (`mirrors.cloud.tencent.com`)
- Dynamic color theming is enabled for Android 12+ (API 31+)
- Edge-to-edge display is enabled in all activities
- Release builds have minification and resource shrinking enabled
- All activities support multi-window and picture-in-picture modes
- Back gesture handling uses `enableOnBackInvokedCallback`
