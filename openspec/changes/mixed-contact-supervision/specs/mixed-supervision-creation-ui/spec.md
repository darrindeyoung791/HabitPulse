## ADDED Requirements

### Requirement: Remove supervision method dropdown
The habit creation/edit screen SHALL remove the single-selection "Supervision Method" dropdown (NONE / EMAIL / SMS). Instead, email and phone sections are displayed independently.

#### Scenario: No supervision method dropdown visible
- **WHEN** the habit creation screen is displayed
- **THEN** there is no "Supervision Method" dropdown

### Requirement: Independent email input section
The habit creation screen SHALL have an email input section that can be independently expanded/collapsed. When expanded, the user can add, view, and delete supervisor email addresses with validation.

#### Scenario: Email section collapsed by default
- **WHEN** creating a new habit
- **THEN** the email input section is collapsed
- **THEN** no email addresses are required

#### Scenario: Add valid email
- **WHEN** the user expands the email section and enters a valid email address and clicks "Add"
- **THEN** the email is added to the contact list and displayed in the list below

#### Scenario: Add invalid email shows error
- **WHEN** the user enters an invalid email format and clicks "Add"
- **THEN** an error message is shown and the email is not added

#### Scenario: Duplicate email shows error
- **WHEN** the user enters an email that already exists in the contact list
- **THEN** a duplicate error message is shown and the email is not added

#### Scenario: Delete email from list
- **WHEN** the user clicks the delete button on an existing email in the list
- **THEN** the email is removed from the contact list

### Requirement: Independent phone input section
The habit creation screen SHALL have a phone input section that can be independently expanded/collapsed. When expanded, the user can add, view, and delete supervisor phone numbers with validation.

#### Scenario: Phone section collapsed by default
- **WHEN** creating a new habit
- **THEN** the phone input section is collapsed
- **THEN** no phone numbers are required

#### Scenario: Add valid phone
- **WHEN** the user expands the phone section and enters a valid phone number and clicks "Add"
- **THEN** the phone is added to the contact list and displayed in the list below

#### Scenario: Duplicate phone shows error
- **WHEN** the user enters a phone that already exists in the contact list
- **THEN** a duplicate error message is shown and the phone is not added

#### Scenario: Delete phone from list
- **WHEN** the user clicks the delete button on an existing phone in the list
- **THEN** the phone is removed from the contact list

### Requirement: Both sections can be simultaneously non-empty
The habit creation screen SHALL allow the user to save a habit with both emails and phones simultaneously.

#### Scenario: Save with mixed contacts
- **WHEN** the user adds at least one email AND at least one phone, then saves the habit
- **THEN** the habit is saved with both `supervisorEmails` and `supervisorPhones` non-empty
- **THEN** the habit returns `true` for `hasSupervision`

#### Scenario: Save with no contacts
- **WHEN** the user collapses both sections (or adds nothing) and saves
- **THEN** the habit is saved with empty `supervisorEmails` and `supervisorPhones`
- **THEN** the habit returns `false` for `hasSupervision`

### Requirement: Edit mode restores existing contacts
When editing an existing habit, the creation screen SHALL pre-populate both email and phone sections based on the habit's saved data.

#### Scenario: Edit habit with mixed contacts
- **WHEN** editing a habit that has both emails and phones
- **THEN** both sections display their respective saved contacts
- **THEN** the email section shows all saved email addresses
- **THEN** the phone section shows all saved phone numbers
