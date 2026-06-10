## ADDED Requirements

### Requirement: Onboarding multi-step wizard
The system SHALL display a 3-step onboarding wizard on first app launch (when `hasCompletedOnboarding` is false).

#### Scenario: First launch shows wizard
- **WHEN** user launches the app for the first time
- **THEN** system shows WelcomeActivity with a 3-step wizard instead of a single-page welcome screen

#### Scenario: Step indicator display
- **WHEN** user is on any step of the wizard
- **THEN** system SHALL display a step indicator showing current step (e.g., "① ② ③" with current step highlighted)

#### Scenario: Forward-only navigation
- **WHEN** user completes a step (by clicking next/skip/agree)
- **THEN** system SHALL advance to the next step, with no way to return to previous steps

#### Scenario: Disagree skips wizard
- **WHEN** user clicks "不同意，进入受限模式" on Step 1
- **THEN** system SHALL enter limited mode and proceed directly to MainActivity, skipping Steps 2 and 3

---

### Requirement: Welcome step (Step 1) with TOS/Privacy links
Step 1 SHALL display the existing welcome content (app logo, name, description, permissions) and add clickable "使用条款" and "隐私政策" links.

#### Scenario: TOS link click
- **WHEN** user clicks "使用条款" link on Step 1
- **THEN** system SHALL show a Snackbar indicating "即将推出" (no actual TOS page required)

#### Scenario: Privacy link click
- **WHEN** user clicks "隐私政策" link on Step 1
- **THEN** system SHALL show a Snackbar indicating "即将推出" (no actual privacy page required)

#### Scenario: Agree advances to Step 2
- **WHEN** user clicks "我同意，继续" on Step 1
- **THEN** system SHALL advance to Step 2 (notification settings)

---

### Requirement: Notification settings step (Step 2) with DND configuration
Step 2 SHALL display notification/DND configuration. The user SHALL be able to toggle DND on/off and adjust the DND time range.

#### Scenario: DND toggle default state
- **WHEN** Step 2 renders
- **THEN** system SHALL display the DND toggle enabled by default, with start time 22:00 and end time 07:00

#### Scenario: DND range adjustment
- **WHEN** user adjusts the DND time range slider
- **THEN** system SHALL update the displayed start and end times in real-time, snapping to 30-minute steps

#### Scenario: DND settings saved on next
- **WHEN** user clicks "下一步" on Step 2
- **THEN** system SHALL save current DND settings (enabled, start time, end time) to UserPreferences and advance to Step 3

#### Scenario: DND settings skipped
- **WHEN** user clicks "跳过" on Step 2
- **THEN** system SHALL keep default DND settings and advance to Step 3

---

### Requirement: AI settings step (Step 3) with skip support
Step 3 SHALL display AI configuration fields (API endpoint, API key, model name) with pre-filled defaults and a skip option.

#### Scenario: AI defaults pre-filled
- **WHEN** Step 3 renders
- **THEN** system SHALL display the API endpoint pre-filled with "https://open.bigmodel.cn/api/paas/v4/chat/completions" and model pre-filled with "glm-4-flash-250414"

#### Scenario: API key optional
- **WHEN** Step 3 renders
- **THEN** system SHALL show the API key field empty, with password visual transformation (toggle visibility)

#### Scenario: AI settings saved on complete
- **WHEN** user fills in fields and clicks "完成"
- **THEN** system SHALL save API endpoint, API key, and model to UserPreferences, complete onboarding, request notification permission, and launch MainActivity

#### Scenario: AI settings skipped
- **WHEN** user clicks "跳过" on Step 3
- **THEN** system SHALL NOT save AI settings (keep existing defaults), complete onboarding, request notification permission, and launch MainActivity

---

### Requirement: Settings persistence
All configurations made during the wizard SHALL persist via UserPreferences (DataStore) and SHALL be editable later in the app's Settings screens.

#### Scenario: DND config shared
- **WHEN** user configured DND in the wizard
- **THEN** the same DND settings SHALL be reflected when opening ReminderSettings later

#### Scenario: AI config shared
- **WHEN** user configured AI in the wizard
- **THEN** the same AI settings SHALL be reflected when opening AISettings later
