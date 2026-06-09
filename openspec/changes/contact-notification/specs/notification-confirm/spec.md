## ADDED Requirements

### Requirement: Localized UI strings
All user-visible strings in the notification confirmation dialog MUST be defined in `strings.xml` resource files across all supported locales (default/Chinese, en-rUS, zh-rHK, zh-rTW) and referenced via `stringResource(R.string.*)` in Compose code. No hardcoded strings SHALL be used.

### Requirement: Notification confirmation dialog
The system SHALL provide a notification confirmation dialog that appears when user taps "Notify Supervisors" in the reward bottom sheet.

#### Scenario: Open confirmation dialog with supervisors
- **WHEN** user taps "Notify Supervisors" button in RewardBottomSheet
- **THEN** the system SHALL display a fullscreen dialog showing:
  - The habit title
  - All supervisor emails with email icons
  - All supervisor phone numbers with phone icons
  - An editable text field pre-filled with the notification template
  - A "Send Email" button
  - A "Send SMS" button
  - A "Cancel" button to dismiss

#### Scenario: No supervisors available edge case
- **WHEN** the habit has no supervisor emails or phones (should not happen since button is gated by `hasSupervision`)
- **THEN** the system SHALL show a message "No supervisors to notify"

### Requirement: Editable notification content
The notification content text field SHALL be editable by the user before sending.

#### Scenario: Edit template before sending
- **WHEN** user taps on the notification content text field
- **THEN** the user SHALL be able to freely edit the text

#### Scenario: Variables rendered in preview
- **WHEN** the notification template contains `{habit_name}` or `{checkin_time}`
- **THEN** the dialog SHALL display the rendered text with actual habit name and current time substituted

### Requirement: Cancel dismisses with toast
The dialog SHALL show a reminder toast when dismissed via back or cancel button.

#### Scenario: Dismiss with toast reminder
- **WHEN** user taps Cancel or presses back
- **THEN** the system SHALL dismiss the dialog and show a Toast: "Remember to notify your supervisors"
