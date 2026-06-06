## ADDED Requirements

### Requirement: Contacts aggregated from all contacts regardless of type
The ContactsViewModel SHALL aggregate contacts from ALL habits by extracting both `supervisorEmails` and `supervisorPhones` from every habit, without filtering by a `supervisionMethod`.

#### Scenario: Contacts show emails from habit with only emails
- **WHEN** a habit has non-empty `supervisorEmails` and empty `supervisorPhones`
- **THEN** those emails appear in the contacts list

#### Scenario: Contacts show phones from habit with only phones
- **WHEN** a habit has empty `supervisorEmails` and non-empty `supervisorPhones`
- **THEN** those phones appear in the contacts list

#### Scenario: Contacts show both types from mixed habit
- **WHEN** a habit has both emails and phones
- **THEN** both the emails and phones appear in the contacts list

#### Scenario: Contact used in multiple habits
- **WHEN** the same email or phone is used in multiple habits
- **THEN** the contact card shows the count and lists all associated habits

## MODIFIED Requirements

### Requirement: Delete contact from all habits sets NONE only if no contacts remain
When deleting a contact from all habits in `ContactsViewModel.deleteContactFromAllHabits()`, the habit's `supervisionMethod` shall not be set to NONE if the habit still has contacts of the other type.

#### Scenario: Delete email from habit with both types
- **WHEN** `deleteContactFromAllHabits` is called for an email contact
- **AND** the habit also has phone contacts
- **THEN** the email is removed from `supervisorEmails`
- **THEN** `supervisionMethod` is NOT set to NONE (or handled appropriately in new model)

#### Scenario: Delete last contact from habit
- **WHEN** `deleteContactFromAllHabits` removes the last remaining contact of any type from a habit
- **THEN** the habit has no supervision (both lists empty)
