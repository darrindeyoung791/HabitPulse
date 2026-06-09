## ADDED Requirements

### Requirement: Localized default notification template
The system SHALL provide a default notification template per locale, stored in UserPreferences (DataStore). All user-visible strings MUST be defined in `strings.xml` files across all supported locales and referenced via `R.string.*`.

#### Scenario: Default template on first use
- **WHEN** the user opens the notification confirmation dialog for the first time
- **THEN** the system SHALL pre-fill the content with the localized default template from string resources
- **AND** the default template SHALL include `{habit_name}`, `{checkin_time}`, `{habit_notes}` placeholders

#### Scenario: Chinese (Simplified) default
- **WHEN** the device locale is Chinese (Simplified)
- **THEN** the default template SHALL be: `"已完成 {habit_name} 打卡（{checkin_time}），特此通知。"`

#### Scenario: English (US) default
- **WHEN** the device locale is English (US)
- **THEN** the default template SHALL be: `"Completed {habit_name} check-in ({checkin_time}), just so you know."`

#### Scenario: Variables in template
- **WHEN** the template contains `{habit_name}`
- **THEN** the system SHALL replace it with the actual habit title
- **WHEN** the template contains `{checkin_time}`
- **THEN** the system SHALL replace it with the current time formatted as "HH:mm"

### Requirement: Template settings in Settings screen
The system SHALL provide a settings entry in the Settings screen to view and edit the notification template.

#### Scenario: Settings entry
- **WHEN** user opens Settings screen
- **THEN** the system SHALL display a "Notification Template" item under the Notification section

#### Scenario: Edit template in settings
- **WHEN** user taps "Notification Template" in Settings
- **THEN** the system SHALL show a dialog or screen with:
  - An editable text field containing the current template
  - A section explaining available variables with localized examples (from string resources):
    - Examples source in Chinese: `{habit_name}` → 习惯名称（例如：按时喝水）, `{checkin_time}` → 打卡时间（例如：08:30）, `{habit_notes}` → 习惯备注（例如：每天喝8杯水）
    - Examples source in English: `{habit_name}` → habit name (e.g. Drink Water), `{checkin_time}` → check-in time (e.g. 08:30), `{habit_notes}` → habit notes (e.g. Drink 8 glasses of water daily)
  - A "Save" button
  - A "Reset to Default" button

#### Scenario: Save template
- **WHEN** user edits the template and taps Save
- **THEN** the system SHALL persist the new template to UserPreferences
- **AND** show a success Toast

#### Scenario: Reset to default
- **WHEN** user taps "Reset to Default"
- **THEN** the system SHALL restore the default template text
