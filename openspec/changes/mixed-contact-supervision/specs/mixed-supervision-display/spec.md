## ADDED Requirements

### Requirement: Reminder detail dialog shows all contact types
The `ReminderDetailDialog` in `HabitScreen.kt` SHALL display all supervisor contacts (emails AND phones) simultaneously when any exist.

#### Scenario: Show both emails and phones in detail
- **WHEN** a habit has both emails and phones
- **AND** the user opens the reminder detail dialog
- **THEN** both email addresses and phone numbers are displayed

#### Scenario: Show only emails
- **WHEN** a habit has only emails
- **AND** the user opens the reminder detail dialog
- **THEN** only email addresses are shown

#### Scenario: Show only phones
- **WHEN** a habit has only phones
- **AND** the user opens the reminder detail dialog
- **THEN** only phone numbers are shown

### Requirement: HomeScreen count uses hasSupervision
The `HomeScreen` contact subtitle count SHALL count habits where `hasSupervision` is true (i.e., either contact list is non-empty).

#### Scenario: Count habits with any supervision
- **WHEN** computing the contact subtitle
- **THEN** count all habits where `supervisorEmails` or `supervisorPhones` is non-empty

### Requirement: RewardBottomSheet uses hasSupervision
The `RewardBottomSheet` SHALL display the "Notify Supervisor" button when the habit has any contacts (email or phone).

#### Scenario: Show notify button for email-only habit
- **WHEN** a habit has only emails
- **THEN** the "Notify Supervisor" button is shown

#### Scenario: Show notify button for mixed habit
- **WHEN** a habit has both emails and phones
- **THEN** the "Notify Supervisor" button is shown

#### Scenario: Hide notify button for no contacts
- **WHEN** a habit has no emails and no phones
- **THEN** the "Notify Supervisor" button is hidden
