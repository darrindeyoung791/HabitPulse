## ADDED Requirements

### Requirement: Remove SupervisionMethod enum
The `SupervisionMethod` enum (NONE, EMAIL, SMS) SHALL be removed from `Habit.kt`. A habit's supervision type SHALL be determined by whether its contact lists are non-empty.

#### Scenario: Habit without any contacts has no supervision
- **WHEN** a `Habit` is created with empty `supervisorEmails` and empty `supervisorPhones`
- **THEN** `hasSupervision` property returns `false`

#### Scenario: Habit with emails only has supervision
- **WHEN** a `Habit` has non-empty `supervisorEmails` and empty `supervisorPhones`
- **THEN** `hasSupervision` returns `true`

#### Scenario: Habit with phones only has supervision
- **WHEN** a `Habit` has empty `supervisorEmails` and non-empty `supervisorPhones`
- **THEN** `hasSupervision` returns `true`

#### Scenario: Habit with both emails and phones has supervision
- **WHEN** a `Habit` has non-empty `supervisorEmails` and non-empty `supervisorPhones`
- **THEN** `hasSupervision` returns `true`

### Requirement: Database v5 migration
The database SHALL migrate from v4 to v5, removing the `supervisionMethod` column from the `habits` table.

#### Scenario: Migration removes supervisionMethod column
- **WHEN** the database is upgraded from v4 to v5
- **THEN** the `supervisionMethod` column is removed from the `habits` table
- **THEN** existing `supervisorEmails` and `supervisorPhones` data is preserved unchanged

### Requirement: SupervisionMethod enum removal from codebase
All references to `SupervisionMethod` enum across the codebase SHALL be replaced with `hasSupervision` checks or removed.

#### Scenario: No compilation references to SupervisionMethod
- **WHEN** the project is compiled after the refactor
- **THEN** there are no references to `SupervisionMethod` in any source file
