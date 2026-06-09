## ADDED Requirements

### Requirement: Localized error and status messages
All user-facing messages (Toasts, dialog texts) in the notification channel SHALL be defined in `strings.xml` across all supported locales.

#### Scenario: Localized error messages
- **WHEN** no email app is found
- **THEN** the system SHALL show a Toast with the localized string for "No email application found"
- **WHEN** no SMS app is found
- **THEN** the system SHALL show a Toast with the localized string for "No SMS application found"

### Requirement: Send email via system mail app
The system SHALL launch the default email application via `mailto:` Intent with all supervisor emails as recipients and the notification content as the email body.

#### Scenario: Send email with all supervisor emails
- **WHEN** user taps "Send Email" button
- **THEN** the system SHALL create an Intent with `ACTION_SENDTO` and `mailto:` URI
- **AND** set all supervisor email addresses as recipients (TO field)
- **AND** set the notification content as the email body
- **AND** set the email subject to `"关于 {habit_name} 的打卡通知"`
- **AND** launch the system email app via `startActivity()`

#### Scenario: No email app available
- **WHEN** no email application is installed on the device
- **THEN** the system SHALL show a Toast: "No email application found"

### Requirement: Send SMS via system SMS app
The system SHALL launch the default SMS application via `smsto:` Intent for each phone contact.

#### Scenario: Send SMS to a phone contact
- **WHEN** user taps "Send SMS" button
- **THEN** the system SHALL create an Intent with `ACTION_SENDTO` and `smsto:` URI for the first phone contact
- **AND** set the notification content as the SMS text
- **AND** launch the system SMS app

#### Scenario: Multiple phone contacts
- **WHEN** the habit has multiple phone supervisors
- **THEN** the system SHALL list all phone numbers in the dialog
- **AND** allow user to tap each to send separately, OR show a message explaining they need to send individually

#### Scenario: No SMS app available
- **WHEN** no SMS application is installed on the device
- **THEN** the system SHALL show a Toast: "No SMS application found"
