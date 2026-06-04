## ADDED Requirements

### Requirement: Habit status calculation
The system SHALL calculate a real-time status for each habit based on today's completions and current time.

#### Scenario: Single reminder habit completed
- **WHEN** habit has 1 reminder time and user completed all today
- **THEN** status is COMPLETED_TODAY

#### Scenario: Multi-reminder partially completed
- **WHEN** habit has [08:00,20:00] and current time is 10:00, only 08:00 completed
- **THEN** status is PENDING_TODAY

#### Scenario: Slot within 1 hour
- **WHEN** habit has [20:00] and current time is 19:05
- **THEN** status includes ABOUT_TO_START

#### Scenario: Slot overdue
- **WHEN** habit has [08:00] and current time is 10:00 (past 08:00 + 1h)
- **THEN** status includes OVERDUE

#### Scenario: Habit not applicable today
- **WHEN** weekly habit, current day not in repeatDays
- **THEN** status is NO_STATUS

#### Scenario: Multiple statuses combined
- **WHEN** habit has [08:00,20:00], current time 19:30, both incomplete
- **THEN** status includes PENDING_TODAY + ABOUT_TO_START + OVERDUE

### Requirement: Status-based habit grouping
The system SHALL provide Flow<List<HabitWithStatus>> that combines habits with their computed status.

#### Scenario: Flow emits on habit change
- **WHEN** a new habit is created or a habit is deleted
- **THEN** habitsWithStatusFlow emits updated list

#### Scenario: Flow emits on completion change
- **WHEN** user checks in or undoes a habit
- **THEN** habitsWithStatusFlow emits updated list with correct status

#### Scenario: Flow emits on day change
- **WHEN** calendar date changes (across midnight)
- **THEN** habitsWithStatusFlow emits with fresh completions for new day

### Requirement: Old completion record compatibility
The system SHALL handle HabitCompletion records with empty slotTime from before the migration.

#### Scenario: Empty slotTime ignored in status
- **WHEN** calculating status for a habit with old completions (slotTime="")
- **THEN** those completions are NOT counted toward today's progress

#### Scenario: Old records still visible
- **WHEN** viewing Records screen
- **THEN** old completions with slotTime="" are still displayed
