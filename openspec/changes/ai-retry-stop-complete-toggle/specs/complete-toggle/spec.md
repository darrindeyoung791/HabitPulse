## ADDED Requirements

### Requirement: 习惯卡片支持完成/撤销双向切换
Each habit created card in the AI conversation SHALL support toggling between "完成" (Complete) and "撤销创建" (Undo) states.

#### Scenario: 初始状态显示完成按钮
- **WHEN** a habit card is first displayed
- **THEN** it shows a "完成" (Complete) button

#### Scenario: 点击完成创建习惯
- **WHEN** user clicks "完成" on a habit card
- **THEN** system saves the habit to the database, marks the card as completed (visually dimmed/different color), and changes the button text to "撤销创建" (Undo)

#### Scenario: 点击撤销删除习惯
- **WHEN** user clicks "撤销创建" on a completed habit card
- **THEN** system deletes the habit from the database, restores the card to its original visual state, and changes the button text back to "完成" (Complete)

#### Scenario: 多次切换
- **WHEN** user toggles between complete and undo multiple times
- **THEN** each toggle correctly creates or deletes the habit in the database, and the UI state stays in sync

### Requirement: 完成/撤销状态持久化
The completed state of a habit card SHALL be tracked within the current conversation session.

#### Scenario: 停止后状态保持
- **WHEN** user completes a habit card, then stops the conversation
- **THEN** the card remains in its completed state until user explicitly undoes it
