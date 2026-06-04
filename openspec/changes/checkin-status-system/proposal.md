## Why

当前 Habit 模型仅用 `completedToday: Boolean` 表示"今日是否完成"，无法支持多提醒时段、提前打卡、逾期补卡等场景。需要建立完整的状态体系来支撑接下来的提醒系统和履行追踪功能。

## What Changes

- 数据库 v3→v4 迁移：HabitCompletion 新增 `slotTime`（绑定提醒时段）和 `isLate`（是否补卡）字段
- 新增习惯状态引擎：计算每个习惯当天的实时状态（无状态/待打卡/即将开始/已完成/逾期），支持多状态组合
- 改造打卡逻辑：按时段分配打卡（findTargetSlot），单提醒不限制时间，多提醒 1h 提前窗口，逾期可补卡
- 打卡上限控制：每天最多打卡次数 = reminderTimes.length，超限提示
- UI 适配：习惯卡片增加状态 Badge 和进度(x/y)，打卡按钮区分准时/补卡/禁止
- EntryZone 卡片数据更新：今日待打卡计数、即将开始计数、逾期计数
- 撤销打卡适配：undo 按最近一次打卡记录删除（保持现有逻辑）
- 跨天补卡：按实际打卡时间归属，isLate=true
- 跨天自动刷新：ProcessLifecycleOwner + 每分钟轻量检查 currentDate

## Capabilities

### New Capabilities
- `habit-status-engine`: 习惯状态枚举定义、HabitWithStatus 数据类、状态计算逻辑（combine habits + todayCompletions）
- `slot-based-checkin`: slot 分配算法、isLate 判定、打卡上限控制、补卡与正常打卡的差异处理
- `status-ui`: 习惯卡片状态 Badge（即将开始/逾期）、当日进度(x/y)、打卡按钮三态（正常/补卡/已完成禁止）
- `entry-zone-status`: EntryZone 入口卡片数据改为从状态引擎获取（待打卡/即将开始/逾期计数）

### Modified Capabilities
- `horizontal-entry-zone`: EntryZone 的"今天的习惯"卡片数据源变更，数据从状态引擎获取

## Impact

- **数据库**: v3→v4 迁移，HabitCompletion 两个新字段
- **ViewModel**: 新增 habitsWithStatusFlow 替换 habitsFlow，新增计数 StateFlows
- **Repository**: 新增 slot 分配打卡方法，改造现有 incrementCompletionCount
- **HabitDao**: 新增按时段查询方法
- **HabitScreen**: 卡片 UI 重构（状态 Badge、进度、按钮三态）
- **EntryZone**: 数据源改为状态计数
- **TodayHabitsScreen**: 接入新状态系统
- **跨天刷新**: 新增 ProcessLifecycleOwner 监听
