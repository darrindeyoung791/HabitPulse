![HabitPulse Banner](../images/readme/hero.png)

# HabitPulse

<div align="center">

[![MIT License](https://img.shields.io/badge/License-MIT-yellow.svg)](https://choosealicense.com/licenses/mit/)
[![Platform](https://img.shields.io/badge/Platform-Android-green.svg)](https://developer.android.com/)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.3.20-purple.svg)](https://kotlinlang.org/)
[![Compose BOM](https://img.shields.io/badge/Compose%20BOM-2026.03.00-brightgreen.svg)](https://developer.android.com/jetpack/compose/bom)
[![Room](https://img.shields.io/badge/Room-2.8.4-blue.svg)](https://developer.android.com/training/data-storage/room)
[![API](https://img.shields.io/badge/API-26%2B-orange.svg)](https://developer.android.com/about/versions/oreo/android-8.0-api-26)

</div>

---

## 📱 Introduction

**HabitPulse** is an Android habit tracking application built with Material Design 3, dedicated to helping users build and maintain good daily habits. Through a concise and intuitive interface design and intelligent reminder mechanisms, HabitPulse makes habit formation easier and more effective.

> 💡 HabitPulse is developed with AI assistance. If you use Qwen Code or Claude Code for further development, please read [`QWEN.md`](../../QWEN.md) for important information.

> 🌏 [中文版本](../../README.md) | English Version

---

## ✨ Features

### 🎯 Core Features
- **Habit Tracking**: Create, manage, and track daily habits, recording every check-in
- **Completion Records**: Complete check-in history with support for viewing completion status on any date
- **Supervisor Contacts**: Add supervisor emails or phone numbers; habit completion can notify designated contacts (planned)
- **Smart Reminders**: Time-based reminder feature to help users stick to habits (planned)

### 🎨 UI/UX Features
- **Material Design 3**: Latest MD3 design specifications for a clean and beautiful interface
- **Dynamic Colors**: Dynamic theming support for Android 12+ (Material You)
- **Responsive Layout**: Perfect adaptation for phones and tablets with landscape/portrait orientation support
- **Split-screen Support**: Multi-window and picture-in-picture mode support
- **Accessibility Optimized**: Full TalkBack support, caring for every user
- **Predictive Back Gesture**: Android 13+ predictive back gesture support

### 🔧 Technical Features
- **Jetpack Compose**: Declarative UI framework for a modern development experience
- **Room Database**: Local data persistence, available offline
- **ViewModel + Flow**: Reactive architecture, data-driven UI
- **Navigation Component**: Navigation Compose for smooth page transition animations

---

## 🖼️ Interface Preview

<div align="center">

| <div align="center">**Phone Interface**</div> | <div align="center">**Tablet Interface**</div> |
|---|---|
| ![Phone Interface](../images/showcases/Screenshot_Pixel_6a_Habits_ZH-CN.png) | ![Tablet Interface](../images/showcases/Screenshot_Pixel_Tablet_Habits_ZH-CN.png) |

</div>

---

## 🚀 Quick Start

### Requirements
- **Android Studio**: Latest version
- **JDK**: 17 or higher
- **Android SDK**:
  - Minimum SDK: 26 (Android 8.0)
  - Target SDK: 36 (Android 16)

### Clone the Project
```bash
git clone https://github.com/darrindeyoung791/HabitPulse.git
cd HabitPulse
```

### Build the Project

Open the project in the latest version of Android Studio and follow the prompts.

Or use other IDEs or editors.

```bash
# Build using Gradle Wrapper
./gradlew assembleDebug

# Or open the project in Android Studio and run directly
```

> [!IMPORTANT]
> - You may need to manually modify the JDK path in [`gradle.properties`](../../gradle.properties) in the project.
> - This project is configured to use mirrors from Tencent Cloud and Aliyun. If you are a developer outside mainland China, you will need to modify these settings.

#### VSCode Users

If you are using VSCode for development, you need to configure the JDK 17 path in [`.vscode/settings.json`](../../.vscode/settings.json):

```json
{
    "java.jdt.ls.java.home": "C:\\\\Program Files\\\\Java\\\\jdk-17",
    "java.home": "C:\\\\Program Files\\\\Java\\\\jdk-17"
}
```

> ⚠️ **Important**: Please modify the above configuration according to your actual JDK installation path. The default path on Windows is usually `C:\Program Files\Java\jdk-17`, and on macOS it is typically `/Library/Java/JavaVirtualMachines/jdk-17.jdk/Contents/Home`.

After configuration, press `Ctrl+Shift+P` and select **"Java: Clean Java Language Server Workspace"**, or reload the VSCode window for the configuration to take effect.

### Install the Application
```bash
# Install to connected device via ADB
./gradlew installDebug
```

---

## 🛠️ Tech Stack

| Component | Version | Description |
|------|------|------|
| **Language** | Kotlin 2.3.20 | Modern Android development language |
| **UI Framework** | Jetpack Compose (BOM 2026.03.00) | Declarative UI framework |
| **Material 3** | 1.4.0 | Material Design 3 component library |
| **Navigation** | Navigation Compose 2.8.0 | Page navigation and animations |
| **Database** | Room 2.8.4 | Local data persistence |
| **Lifecycle** | 2.10.0 | Lifecycle-aware components |
| **ViewModel** | 2.8.7 | UI state management |
| **Build Tool** | Gradle 9.4.0 + AGP 9.1.0 | Project build system |
| **JVM Target** | Java 17 | Compiled bytecode version |

---

## 📦 Project Structure

```
HabitPulse/
├── app/
│   ├── src/main/
│   │   ├── java/io/github/darrindeyoung791/habitpulse/
│   │   │   ├── MainActivity.kt              # Main entry point
│   │   │   ├── SettingsActivity.kt          # Settings screen
│   │   │   ├── HabitPulseApplication.kt     # Application class
│   │   │   ├── navigation/                  # Navigation graph
│   │   │   ├── data/                        # Data layer
│   │   │   │   ├── model/                   # Data models
│   │   │   │   ├── database/                # Room database
│   │   │   │   └── repository/              # Data repository
│   │   │   ├── viewmodel/                   # ViewModel layer
│   │   │   └── ui/                          # UI layer
│   │   │       ├── screens/                 # Screen components
│   │   │       └── theme/                   # Theme and styling
│   │   └── res/                             # Resource files
│   └── build.gradle.kts                     # Module build configuration
├── gradle/                                  # Gradle wrapper
├── QWEN.md                                  # Project context documentation
└── README.md                                # Project documentation
```

---

## 📄 Database Design

### Core Tables

#### habits Table
Stores all user-created habit information.

| Field | Type | Description |
|------|------|------|
| id | TEXT (PRIMARY KEY) | Habit unique identifier (UUID) |
| title | TEXT | Habit title |
| repeatCycle | TEXT | Repeat cycle (DAILY/WEEKLY) |
| repeatDays | TEXT | Repeat days (JSON format) |
| reminderTimes | TEXT | Reminder times (JSON format) |
| notes | TEXT | Notes |
| supervisionMethod | TEXT | Supervision method (NONE/EMAIL/SMS) |
| supervisorEmails | TEXT | Supervisor emails (JSON format) |
| supervisorPhones | TEXT | Supervisor phones (JSON format) |
| completedToday | INTEGER | Today's completion status (0/1) |
| completionCount | INTEGER | Total completion count |
| lastCompletedDate | INTEGER | Last completion timestamp |
| createdDate | INTEGER | Creation timestamp |
| modifiedDate | INTEGER | Modification timestamp |

#### habit_completions Table
Records detailed information for each habit check-in.

| Field | Type | Description |
|------|------|------|
| id | TEXT (PRIMARY KEY) | Record unique identifier (UUID) |
| habitId | TEXT (FOREIGN KEY) | Associated habit ID |
| completedDate | INTEGER | Completion timestamp |
| completedDateLocal | TEXT | Local date (yyyy-MM-dd) |
| timeZone | TEXT | Timezone information |

> 📚 Detailed database design documentation will be provided in future updates

---

## 🤝 Contributing

We welcome contributions of all kinds!

### How to Contribute
1. **Fork** this project
2. Create your feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a **Pull Request**

### Development Environment Setup
1. After cloning the project, open it with Android Studio
2. Sync the Gradle project
3. Run `./gradlew assembleDebug` to ensure the build succeeds
4. Run debugging on an emulator or physical device

### Code Style
- Follow [Kotlin Coding Conventions](https://kotlinlang.org/docs/coding-conventions.html)
- Use KDoc for documentation comments
- Keep code clean and follow the DRY principle

### Reporting Issues
Found a bug? Please report it via [Issues](https://github.com/darrindeyoung791/HabitPulse/issues).

---

## 📜 License

This project is open source under the [MIT License](../../LICENSE).

```
MIT License

Copyright (c) 2026 darrindeyoung791

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```

---

<div align="center">

**Made with ❤️ by darrindeyoung791**

**由 darrindeyoung791 用 ❤️ 制作**

[⭐ Star this repo](https://github.com/darrindeyoung791/HabitPulse/stargazers) | [🍴 Fork](https://github.com/darrindeyoung791/HabitPulse/fork) | [📢 Issues](https://github.com/darrindeyoung791/HabitPulse/issues)

</div>
