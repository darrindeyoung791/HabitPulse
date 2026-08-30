# HabitPulse - Project Context

## Custom Rules from Devs (Top Priority)

This document captures the most important contextual information to accelerate AI retrieval of development details, while avoiding excessive context window consumption. AI agents must follow these rules:
- Auto-update this file when significant changes occur to keep it current and accurate.
- Only record information useful for future development; avoid verbose history unless there are lessons learned.
- Do not record information that can be grepped on the spot (e.g. dependency versions). Exceptions:
    - Record exact filenames and brief descriptions to avoid guessing during search.
    - Information requiring multiple grep searches to piece together may be recorded here, still keep it concise.
- Do not rely on content in `README.md`, and do not put development details there, since `README.md` is NOT updated frequently.

## Project Overview

HabitPulse is an Android habit tracking app built with Kotlin + Jetpack Compose. Key features: Material Design 3 UX, habit tracking with slot-based check-in, social supervision (email/phone contacts), AI chat assistant (tool-calling), multi-config LLM settings with biometric-protected API key encryption, responsive navigation (Bottom Bar / Rail / Drawer), and in-app language switching. Minimum SDK 26, Target SDK 37, Room database v5 schema. User-facing documentation is a VitePress site under `docs/` (tutorials, feature guides); developer design specs and plans live under `devdoc/`.

## Project Structure

```
data/
├── model/
│   ├── AIConfig.kt              # Multi-config LLM settings (Gson @SerializedName)
│   ├── SlotCheckInEngine.kt     # Pure slot check-in & status logic (no Android deps)
│   └── HabitStatus.kt           # HabitWithStatus, status enum, pendingCount/isCompletelyOverdue
├── database/
│   ├── HabitDatabase.kt         # Room DB (v5 schema, MIGRATION_3_4, MIGRATION_4_5)
│   └── converter/               # ListConverters, EnumConverters
├── security/
│   ├── KeystoreManager.kt       # Dual AES-256-GCM keys (runtime + reveal)
│   ├── AesGcmCipher.kt          # Pure JCA cipher (no Android deps, JVM-testable)
│   ├── ApiKeyCrypto.kt          # Encrypt/decrypt/reveal API keys
│   └── ApiKeyMigration.kt       # Plaintext detection + idempotent migration
└── preferences/
    └── UserPreferences.kt       # DataStore keys (extensive, scattered throughout)

ai/
├── conversation/
│   └── AIChatConversationManager.kt  # Tool-calling loop engine
├── tools/chat/                  # 9 tools: create_habit, search_habits, etc.
└── llm/
    └── LLMClient.kt             # Streaming chat API client

ui/
├── DeviceFormFactor.kt          # Unified device form factor: rememberDeviceFormInfo()
├── screens/
│   ├── HomeScreen.kt            # Shell: 3 tabs + TopAppBar + reveal drawer + Omnibox
│   ├── HabitScreen.kt           # Habit list, cards, search, reward sheet
│   ├── ai/AIChatScreen.kt       # AI chat with tool-calling UI
│   └── settings/                # Settings screens (segmented list UI)
└── utils/
    └── PressVibrationFeedback.kt # Configurable haptic feedback

navigation/
├── HabitPulseNavGraph.kt        # Nav graph with custom animations
└── Route.kt                     # Route definitions

utils/
├── AppLocaleManager.kt          # Android 13+ per-app language (LocaleManager reflection)
├── NotificationHelper.kt        # Notification creation & management
└── OnboardingPreferences.kt     # SharedPreferences for onboarding state

devdoc/                          # Developer documentation & design specs
├── ux-design-guide/
│   ├── listitem-style.md        # Settings listitem style guide
│   ├── custom-color-scheme.md   # Custom color scheme reference
│   ├── welcome-redesign/        # Welcome screen redesign (HTML prototypes + DESIGN_GUIDE.md)
│   └── home-enty-zone-layout-mockups/  # Home entry zone layout HTML mockups
├── ai-chat-cards-redesign/      # AI chat cards redesign (design.md + preview.html)
├── structure.md                 # Project structure diagrams
├── unit-tests.md                # Unit test conventions
├── checkin-status-plan.md       # Check-in status feature plan
├── home-refresh-plan.md         # Home screen refresh plan
├── ai-habit-creation-plan.md    # AI habit creation plan
├── webview-plan.md              # WebView integration plan
├── lan-sync-plan.md             # Localization sync plan
└── images/                      # Screenshots, icons (SVG, WebP)

docs/                            # VitePress user-facing documentation site
├── package.json                 # VitePress dependencies
└── docs/
    ├── index.md                 # Landing page
    ├── overview.md              # App overview
    ├── download.md              # Download instructions
    ├── about.md                 # About the app
    ├── team.md                  # Team info
    ├── tutorial/                # User tutorials
    │   ├── first-time.md        # First-time usage guide
    │   ├── add-and-edit-habit.md
    │   ├── delete-and-sort-habit.md
    │   ├── checkin.md           # Check-in tutorial
    │   ├── records.md           # Records usage
    │   ├── search-and-filter.md
    │   ├── contacts.md          # Supervisor contacts
    │   └── help-and-feedback.md
    └── advanced/                # Advanced features
        ├── stayin-alive.md      # Foreground service / keep-alive
        ├── force-tablet-landscape.md
        └── storage-cleanup.md
```

## Database Schema (v5)

**habits** — PK: `id` (UUID TEXT)

| Column | Type | Notes |
|--------|------|-------|
| title | TEXT | |
| repeatCycle | TEXT | DAILY or WEEKLY |
| repeatDays | TEXT | JSON array `[1,3,5]` (0=Mon..6=Sun) |
| reminderTimes | TEXT | JSON array `["08:00","20:00"]` |
| notes | TEXT | |
| supervisorEmails | TEXT | JSON array |
| supervisorPhones | TEXT | JSON array |
| completedToday | INTEGER | Boolean 0/1 |
| completionCount | INTEGER | |
| lastCompletedDate | INTEGER | Millis since epoch |
| createdDate | INTEGER | Millis |
| modifiedDate | INTEGER | Millis |
| sortOrder | INTEGER | Lower = first |
| timeZone | TEXT | e.g. "Asia/Shanghai" |

**habit_completions** — PK: `id` (UUID TEXT), FK: `habitId` → habits.id (CASCADE)

| Column | Type | Notes |
|--------|------|-------|
| habitId | TEXT | Indexed |
| completedDate | INTEGER | Millis |
| completedDateLocal | TEXT | yyyy-MM-dd, indexed |
| timeZone | TEXT | |
| slotTime | TEXT | e.g. "08:00" (added v4) |
| isLate | INTEGER | Boolean (added v4) |

## Testing Practices
- Unit tests in `src/test/`
- Instrumented tests in `src/androidTest/`
- Compose UI testing with `androidx.compose.ui.test`
- Pure business logic extracted to testable classes (e.g., `SlotCheckInEngine`) — no Android dependencies
- Tests use `org.json:json` JVM library because Android's `org.json` is stubbed in unit tests

## Design Guidelines

- Design interfaces following Material Design 3 specifications
    - Prefer native Material Design 3 controls over custom-drawn controls, and ensure TalkBack correctly identifies interactions
    - Prefer Material Design 3 color semantics and typography semantics
- Use Material Icons; emoji are strictly prohibited
- Unless explicitly mentioned, do not use shadow elements for controls
- Follow other UX-related guidelines under `devdoc/`

## i18n Rules

- **No hardcoded strings** — all UI text via `stringResource(R.string.*)` 
- **6 locale files**: `values/` (Chinese default), `values-zh-rCN/`, `values-zh-rHK/`, `values-zh-rTW/`, `values-en-rUS/`, `values-en-rGB/`
- **Requirement for Traditional Chinese Localization**:
    - **No literal conversion**: Do not simply map each Simplified Chinese character to its Traditional Chinese equivalent.
    - **Regional adaptation**: Use the idiomatic expressions commonly used in Hong Kong and Taiwan, respectively. Use a formal, written style rather than a colloquial one.
- **When adding new text**: update ALL 6 files
- **Locale resolution**: Use `LocalConfiguration.current.locales[0]` for date/time formatters, NOT `Locale.getDefault()`
- **Auto-mirrored icons**: Icons with directional meaning (e.g., arrows, back/forward) should use `autoMirrored="true"` in drawable resources for RTL support
- **CRITICAL — localeFilters**: `app/build.gradle.kts` has `androidResources { localeFilters += listOf(...) }`. Adding a locale file without adding to `localeFilters` = silently stripped from APK. Symptom: adding `values-en-rGB/` alone did nothing because `en-rGB` was missing from `localeFilters`.

## Commit Policy

- AI agents should only commit changes after obtaining explicit permission, and such permission should, by default, authorize a single commit; subsequent commits still require authorization, unless explicit permission is granted for all coming commits by the AI agents.
- AI agents should follow the Conventional Commits guidelines when committing changes.

## Current Status

> Subject to change.

### In Progress
- Omnibox and other UX refresh

### Planned
- Calendar section
- Social supervision features (email/SMS notifications)
- Calendar view (full implementation)
- AI habit suggestions
- Data backup/export

## Screen Flow

```
LauncherActivity ──▶ WelcomeActivity (4 steps)
                     │
                     ▼
                  MainActivity (NavHost)
                     │
    ┌────────────────┼────────────────┐
    ▼                ▼                ▼
HomeScreen       HabitCreate     MultiSelect
 (3 tabs)         Screen         Sort Screen
    │
    ├── HabitScreenContent
    ├── ContactsScreenContent
    └── RecordsScreenContent
```

## Responsive Navigation

All device-form decisions come from `rememberDeviceFormInfo()` in `ui/DeviceFormFactor.kt`. Pages must NOT use `LocalConfiguration`/`smallestScreenWidthDp`/`orientation` directly.

| Form | Navigation | FAB |
|------|-----------|-----|
| Phone Portrait | Bottom Bar | Extended |
| Phone Landscape | Navigation Rail | Extended |
| Tablet Portrait | Bottom Bar | Extended |
| Tablet Landscape | Permanent Drawer | Extended + Hamburger |

Breakpoints: `isTabletDevice` = min(w,h)≥600dp, `isWideLayout` = landscape w≥840dp, `isLargeWindow` = min(w,h)≥1200dp.

## Development Lessons

### DatePicker: Use `key()` to prevent dual dialogs
Orientation-dependent DatePicker in `if` conditions causes both to appear during recomposition. Fix: extract to separate composable + `key(isPhoneLandscape)` wrapper forces full recreation.

### Screen Architecture: Parent manages chrome, children manage content
Child screens (RecordsScreen, ContactsScreen) must NOT manage TopAppBar or dialogs. Parent (HomeScreen) owns all chrome — prevents duplicate dialog triggers. Dialogs declared at END of parent composable.

### Theme Switch Black Screen (June 2026)
Three-way deadlock: `configChanges` missing `uiMode` → Activity recreation → `installSplashScreen()` re-runs → `AnimatedVisibility(visible=false)` blocks content → `setKeepOnScreenCondition` stuck forever → black screen. Fix: add `uiMode` to `configChanges`, remove `AnimatedVisibility` gate, use `Surface` not `Box+.background()` for root.

### Key Conventions
- `configChanges` MUST include `uiMode` in Compose projects using `isSystemInDarkTheme()`
- Never gate critical init behind `AnimatedVisibility(visible=false)` — hidden content never composes
- `Surface` propagates `LocalContentColor` correctly during theme transitions; `Box+.background()` does not
- BiometricPrompt: `DEVICE_CREDENTIAL` allowed → MUST NOT set negative button (API 30+)
