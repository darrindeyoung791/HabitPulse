## ADDED Requirements

### Requirement: EntryZone card data from status engine
The system SHALL populate EntryZone card data from the habit status engine rather than raw habit count.

#### Scenario: Today pending card count
- **WHEN** EntryZone renders "今天的习惯" card
- **THEN** badge shows count of habits with PENDING_TODAY status (excluding COMPLETED_TODAY and NO_STATUS)

#### Scenario: About to start card
- **WHEN** EntryZone renders "即将开始" card
- **THEN** badge shows count of habits whose status includes ABOUT_TO_START

#### Scenario: Overdue card
- **WHEN** EntryZone renders "逾期" card
- **THEN** badge shows count of habits whose status includes OVERDUE

#### Scenario: Click on about to start
- **WHEN** user taps "即将开始" entry card
- **THEN** navigates to TodayHabitsScreen filtered to about-to-start habits

#### Scenario: Click on overdue
- **WHEN** user taps "逾期" entry card
- **THEN** navigates to TodayHabitsScreen filtered to overdue habits

### Requirement: Entry Zone hides when no data
The system SHALL hide individual entry cards when their count is zero.

#### Scenario: Zero pending hides card
- **WHEN** no habits have PENDING_TODAY status
- **THEN** "今天的习惯" card is hidden

#### Scenario: Zero about-to-start hides card
- **WHEN** no habits have ABOUT_TO_START status
- **THEN** "即将开始" card is hidden
