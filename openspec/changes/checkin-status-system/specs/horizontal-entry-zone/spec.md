## ADDED Requirements

### Requirement: "即将开始"入口
"即将开始"入口 SHALL 显示即将开始的习惯数量，点击后导航到 TodayHabitsScreen 并过滤即将开始的习惯。

#### Scenario: 计数显示
- **WHEN** 有习惯的状态包含 ABOUT_TO_START
- **THEN** 卡片显示即将开始的习惯数量

#### Scenario: 零计数隐藏
- **WHEN** 没有习惯的状态包含 ABOUT_TO_START
- **THEN** 入口卡片自动隐藏

#### Scenario: 点击跳转
- **WHEN** 用户点击"即将开始"卡片
- **THEN** 导航到 TodayHabitsScreen，过滤为状态包含 ABOUT_TO_START 的习惯

### Requirement: "逾期"入口
"逾期"入口 SHALL 显示逾期习惯数量，点击后导航到 TodayHabitsScreen 并过滤逾期习惯。

#### Scenario: 计数显示
- **WHEN** 有习惯的状态包含 OVERDUE
- **THEN** 卡片显示逾期习惯数量

#### Scenario: 零计数隐藏
- **WHEN** 没有习惯的状态包含 OVERDUE
- **THEN** 入口卡片自动隐藏

#### Scenario: 点击跳转
- **WHEN** 用户点击"逾期"卡片
- **THEN** 导航到 TodayHabitsScreen，过滤为状态包含 OVERDUE 的习惯

## MODIFIED Requirements

### Requirement: "今天的习惯"入口
"今天的习惯"入口 SHALL 显示今天待打卡的习惯数量（排除已完成和无状态的），点击后导航到 TodayHabitsScreen 显示所有今天待打卡的习惯。

#### Scenario: 计数显示
- **WHEN** EntryZone 渲染"今天的习惯"卡片
- **THEN** badge 显示 PENDING_TODAY 状态的习惯数量（排除 COMPLETED_TODAY 和 NO_STATUS）

#### Scenario: 零计数隐藏
- **WHEN** 没有习惯具有 PENDING_TODAY 状态
- **THEN** 入口卡片自动隐藏

#### Scenario: 点击跳转
- **WHEN** 用户点击"今天的习惯"卡片
- **THEN** 导航到 TodayHabitsScreen，展示所有状态为 PENDING_TODAY 的习惯

#### Scenario: 卡片内容
- **WHEN** "今天的习惯"卡片渲染
- **THEN** 显示图标（如 📋）、标题"今日待打卡"、状态标签显示待打卡数量
