## ADDED Requirements

### Requirement: Habit card status badge
The system SHALL display a status badge on each habit card based on its computed status.

#### Scenario: About to start badge
- **WHEN** habit status includes ABOUT_TO_START
- **THEN** card shows "⏰ 即将开始" badge with accent color

#### Scenario: Overdue badge
- **WHEN** habit status includes OVERDUE
- **THEN** card shows "⚠️ 逾期" badge with warning color

#### Scenario: Completed badge
- **WHEN** habit status is COMPLETED_TODAY
- **THEN** card shows "✅ 今天已完成" badge with success color

#### Scenario: No badge for normal pending
- **WHEN** habit status is PENDING_TODAY only (no ABOUT_TO_START or OVERDUE)
- **THEN** no status badge is shown

#### Scenario: Priority display
- **WHEN** habit has both OVERDUE and ABOUT_TO_START
- **THEN** OVERDUE badge takes display priority

### Requirement: Daily progress indicator
The system SHALL show daily completion progress on each habit card.

#### Scenario: Shows x/y progress
- **WHEN** habit has [08:00,20:00], 1 completion today
- **THEN** card shows "1/2" progress indicator

#### Scenario: Full progress
- **WHEN** all slots completed
- **THEN** card shows "2/2 ✅" or "已完成"

#### Scenario: Single reminder no progress
- **WHEN** habit has 1 reminder and no completions today
- **THEN** no progress fraction shown (only single reminder doesn't need x/y)

### Requirement: Check-in button tri-state
The system SHALL adapt the check-in button based on the habit's check-in availability.

#### Scenario: Normal check-in
- **WHEN** habit has at least one eligible slot (not all completed)
- **THEN** button shows "✓" with primary color, animated on press

#### Scenario: Overdue catch-up
- **WHEN** habit status includes OVERDUE
- **THEN** button shows "⚠️ 补卡" with warning/orange color

#### Scenario: Already completed
- **WHEN** all slots completed today
- **THEN** button is disabled, shows "✓ 已打卡" with muted color
