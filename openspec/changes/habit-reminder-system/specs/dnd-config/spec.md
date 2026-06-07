## ADDED Requirements

### Requirement: DND configuration storage
The system SHALL store DND configuration in `UserPreferences` (DataStore) with the following keys:
- `REMINDER_ENABLED` (boolean, default: `true`) — master switch for reminder system
- `DND_ENABLED` (boolean, default: `true`) — DND feature toggle
- `DND_START_TIME` (string, format `"HH:mm"`, default: `"22:00"`) — DND start time
- `DND_END_TIME` (string, format `"HH:mm"`, default: `"07:00"`) — DND end time

Each key SHALL have a `Flow` getter and a `suspend` setter, following existing patterns in `UserPreferences`.

#### Scenario: Default configuration
- **WHEN** user first installs app
- **THEN** `REMINDER_ENABLED = true`, `DND_ENABLED = true`, `DND_START = "22:00"`, `DND_END = "07:00"`

### Requirement: DND time comparison
The system SHALL determine if current time is within the DND window.
If `DND_START < DND_END` (e.g., 22:00~07:00 means overnight), the window is:
- `currentTime >= DND_START` OR `currentTime < DND_END`
If `DND_START >= DND_END` (same day, unlikely but handle), the window is:
- `currentTime >= DND_START` AND `currentTime < DND_END`

Comparisons SHALL use `LocalTime` with minute precision.

#### Scenario: Overnight DND - inside window
- **WHEN** DND is 22:00~07:00 and current time is 23:30
- **THEN** isWithinDND = true

#### Scenario: Overnight DND - outside window
- **WHEN** DND is 22:00~07:00 and current time is 14:00
- **THEN** isWithinDND = false

#### Scenario: Overnight DND - boundary at start
- **WHEN** DND is 22:00~07:00 and current time is 22:00
- **THEN** isWithinDND = true

#### Scenario: Overnight DND - boundary at end
- **WHEN** DND is 22:00~07:00 and current time is 07:00
- **THEN** isWithinDND = false (end time is exclusive)

### Requirement: DND UI with MD3 slider
The settings screen SHALL provide:
- A switch to enable/disable reminders entirely
- A switch to enable/disable DND
- When DND is enabled: two time range selectors using `Slider` (MD3) with:
  - Range: 20:00 (8 PM) to 08:00 (8 AM next day)
  - Step: 30 minutes
  - Default: START = 22:00, END = 07:00
  - Start time label and End time label showing current values
  - Visual indication of the DND window on the slider (using a range slider or two separate sliders)

#### Scenario: User adjusts DND start time
- **WHEN** user drags DND start slider to 23:00
- **THEN** `DND_START_TIME` is set to "23:00"

#### Scenario: User adjusts DND end time
- **WHEN** user drags DND end slider to 06:00
- **THEN** `DND_END_TIME` is set to "06:00"

### Requirement: Master switch disables reminders
When `REMINDER_ENABLED` is `false`, the system SHALL:
- Cancel any pending alarm
- Not schedule new alarms
- Show "已关闭" state in settings
- The DND override counter SHALL NOT run when reminders are disabled

#### Scenario: User turns off reminders
- **WHEN** user sets `REMINDER_ENABLED = false`
- **THEN** pending alarm is cancelled
- **THEN** new alarms are not scheduled until re-enabled
