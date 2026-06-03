## ADDED Requirements

### Requirement: Slot-based check-in assignment
The system SHALL assign each check-in to a specific reminder time slot from the habit's reminderTimes.

#### Scenario: Single reminder - always assignable
- **WHEN** habit has 1 reminder time at 08:00 and user taps check-in at any time
- **THEN** completion is assigned to that single slot

#### Scenario: Multi-reminder - assign to earliest eligible slot
- **WHEN** habit has [08:00,20:00], none completed, current time 07:30
- **THEN** check-in is assigned to 08:00 slot (within 1h window)

#### Scenario: Multi-reminder - too early rejected
- **WHEN** habit has [08:00,20:00], none completed, current time 06:00
- **THEN** check-in is rejected with TOO_EARLY, earliest eligible time is 07:00

#### Scenario: Multi-reminder - assign to next incomplete slot
- **WHEN** habit has [08:00,20:00], 08:00 already completed, current time 19:30
- **THEN** check-in is assigned to 20:00 slot

#### Scenario: Multi-reminder - overdue slot assignable
- **WHEN** habit has [08:00,20:00], none completed, current time 21:00
- **THEN** check-in is assigned to 08:00 (earliest incomplete overdue slot)

### Requirement: Daily check-in limit
The system SHALL limit daily check-ins per habit to reminderTimes.length.

#### Scenario: At limit - reject
- **WHEN** user taps check-in on a habit with [08:00,20:00] and both slots already completed
- **THEN** check-in is rejected with ALREADY_COMPLETED

#### Scenario: At limit - show message
- **WHEN** check-in rejected due to daily limit
- **THEN** Snackbar shows "今日已完成"

### Requirement: Late check-in detection
The system SHALL mark check-ins as late when completed after the slot window.

#### Scenario: Within window - not late
- **WHEN** user checks in at 07:30 for an 08:00 slot
- **THEN** isLate = false

#### Scenario: Past window - late
- **WHEN** user checks in at 10:00 for an 08:00 slot (past 08:00 + 1h)
- **THEN** isLate = true

### Requirement: Undo removes most recent completion
The system SHALL undo the most recent completion (by completedDate).

#### Scenario: Undo single completion
- **WHEN** habit has [08:00], user completed it at 07:30, then taps undo
- **THEN** the 08:00 completion is deleted, habit becomes PENDING_TODAY

#### Scenario: Undo most recent of multiple
- **WHEN** habit has [08:00,20:00], user completed 08:00 at 07:30 and 20:00 at 19:30, then taps undo
- **THEN** the 20:00 completion (most recent) is deleted, habit shows 1/2

### Requirement: Reward sheet only for on-time full completion
The system SHALL show RewardSheet only when all slots are completed and none are late.

#### Scenario: On-time completion triggers reward
- **WHEN** single-reminder habit is checked in on time, becoming COMPLETED_TODAY
- **THEN** RewardSheet is shown

#### Scenario: Late completion suppresses reward
- **WHEN** last remaining slot is checked in with isLate=true
- **THEN** no RewardSheet, only a lightweight snackbar
