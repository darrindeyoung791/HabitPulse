# HabitPulse - Project Context

## Project Overview

**HabitPulse** is an Android habit tracking application built with Kotlin and Jetpack Compose. The app helps users build and maintain daily habits through smart reminders and social supervision features.

### Key Features (Planned/In Development)
- **Habit Tracking**: Create, manage, and track daily habits
- **Smart Reminders**: Time-based notifications to help users stick to habits
- **Social Supervision**: Habit completion notifications to designated contacts
- **Background Service**: Long-running background service for reliable reminders
- **Material Design 3**: Clean, modern UI following MD3 guidelines

### Tech Stack
- **Language**: Kotlin
- **UI Framework**: Jetpack Compose with Material Design 3
- **Database**: Room (planned)
- **Scheduling**: AlarmManager
- **Build System**: Gradle (Kotlin DSL)
- **Minimum SDK**: 24 (Android 7.0)
- **Target SDK**: 36

## Project Structure

```
HabitPulse/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/io/github/darrindeyoung791/habitpulse/
│   │   │   │   ├── MainActivity.kt          # Main entry point
│   │   │   │   └── ui/theme/
│   │   │   │       ├── Color.kt             # Color definitions
│   │   │   │       ├── Theme.kt             # Material theme setup
│   │   │   │       └── Type.kt              # Typography definitions
│   │   │   ├── res/                         # Android resources
│   │   │   └── AndroidManifest.xml
│   │   ├── test/                            # Unit tests
│   │   └── androidTest/                     # Instrumented tests
│   ├── build.gradle.kts                     # App-level build config
│   └── proguard-rules.pro                   # ProGuard rules
├── gradle/
│   └── wrapper/
│       └── gradle-wrapper.properties        # Gradle 9.4.0
├── build.gradle.kts                         # Root build config
├── settings.gradle.kts                      # Project settings
├── gradle.properties                        # Gradle properties
└── README.md                                # Project documentation
```

## Database Schema (Planned)

### habits Table

| Column | Type | Description |
|--------|------|-------------|
| id | INTEGER (PRIMARY KEY, AUTOINCREMENT) | Unique habit identifier |
| title | TEXT | Habit title |
| repeatCycle | TEXT | Repeat cycle (DAILY or WEEKLY) |
| repeatDays | TEXT | Days to repeat (JSON format, e.g., [1,3,5]) |
| reminderTimes | TEXT | Reminder times (JSON format, e.g., ["08:00","20:00"]) |
| notes | TEXT | Habit notes |
| supervisionMethod | TEXT | LOCAL_NOTIFICATION_ONLY or EMAIL_REPORTING |
| supervisorEmailAddresses | TEXT | Supervisor emails (JSON format) |
| completed | INTEGER (BOOLEAN) | Today's completion status (0/1) |
| completionCount | INTEGER | Total completion count |
| createdDate | INTEGER | Creation timestamp |

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
- **Android Gradle Plugin**: Latest (via version catalog)
- **Kotlin**: Latest (via version catalog)

## Dependencies

### Core
- `androidx.core.ktx` - Kotlin extensions for Android
- `androidx.lifecycle.runtime.ktx` - Lifecycle components
- `androidx.activity.compose` - Compose integration with Activity

### Compose UI
- `androidx.compose.bom` - Compose Bill of Materials
- `androidx.compose.ui` - Core Compose UI
- `androidx.compose.material3` - Material Design 3 components
- `androidx.compose.material.icons` - Material icons (core + extended)

### Testing
- `junit` - Unit testing framework
- `androidx.junit` - Android JUnit extensions
- `androidx.espresso.core` - UI testing framework
- `androidx.compose.ui.test` - Compose testing utilities

## Development Conventions

### Code Style
- **Kotlin style**: Official (as per `gradle.properties`)
- **JVM target**: Java 11
- **Compose**: Enabled with Material Design 3

### Architecture Patterns
- Follow Android best practices with Jetpack Compose
- UI layer uses Compose with MaterialTheme
- Data layer planned with Room database
- Type converters for complex types (List<Int>, List<String>)

### Testing Practices
- Unit tests in `src/test/`
- Instrumented tests in `src/androidTest/`
- Compose UI testing with `androidx.compose.ui.test`

## Current Status

The project is in **early development stage**:
- ✅ Project structure set up
- ✅ Basic Compose theme configured
- ✅ MainActivity with placeholder content
- ⏳ Database layer (planned)
- ⏳ Habit management features (planned)
- ⏳ Reminder system (planned)
- ⏳ Email integration (planned)

## Package Information

- **Namespace**: `io.github.darrindeyoung791.habitpulse`
- **Application ID**: `io.github.darrindeyoung791.habitpulse`
- **Version Code**: 1
- **Version Name**: 1.0

## Notes

- The app uses a custom mirror for Gradle distribution (`mirrors.cloud.tencent.com`)
- Dynamic color theming is enabled for Android 12+ (API 31+)
- Edge-to-edge display is enabled in MainActivity
- Release builds have minification disabled by default
