## ADDED Requirements

### Requirement: HabitCreationScreen 支持预填参数
The HabitCreationScreen SHALL accept prefill parameters to pre-populate the form fields.

#### Scenario: 预填参数传递
- **WHEN** navigating to HabitCreationScreen with prefill query parameter containing JSON-encoded habit data
- **THEN** system parses the JSON and populates all corresponding form fields

#### Scenario: 无预填参数
- **WHEN** navigating to HabitCreationScreen without prefill parameter (normal creation)
- **THEN** system displays empty form as before

### Requirement: 预填后保存返回 AI 界面
When user edits a pre-filled habit and saves, the system SHALL return to the AICreateHabitScreen.

#### Scenario: 保存后返回
- **WHEN** user edits a habit from AI confirmation and taps save button
- **THEN** system navigates back to AICreateHabitScreen with updated habit data

#### Scenario: 取消编辑
- **WHEN** user taps back button or cancel in HabitCreationScreen (with prefill)
- **THEN** system navigates back to AICreateHabitScreen without saving changes

### Requirement: AI 确认界面显示所有待创建习惯
The confirmation dialog SHALL display all collected habits before final creation.

#### Scenario: 显示确认列表
- **WHEN** AI outputs confirm question type with multiple habits
- **THEN** system displays a list showing:
  - Habit summary (e.g., "每天 07:00 吃鸡蛋")
  - Checkbox to include/exclude from creation
  - "确认创建" and "取消" buttons

### Requirement: 用户确认后批量保存习惯
When user confirms, the system SHALL save all selected habits to the database.

#### Scenario: 批量保存成功
- **WHEN** user taps "确认创建" with 2 habits selected
- **THEN** system saves both habits to Room database
- **AND** navigates back to HomeScreen
- **AND** displays success message

#### Scenario: 保存失败
- **WHEN** database save operation fails
- **THEN** system displays error message with specific failure reason

### Requirement: 用户取消确认返回对话
When user cancels at the confirmation stage, the system SHALL return to the conversation.

#### Scenario: 取消确认
- **WHEN** user taps "取消" at confirmation dialog
- **THEN** system dismisses the dialog and returns to conversation mode
- **AND** collected habits are cleared