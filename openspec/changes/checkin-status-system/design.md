## Context

详细设计方案参见 `devdoc/checkin-status-plan.md`。本文件记录关键架构决策。

当前 Habit 模型只有 `completedToday: Boolean`，无法区分多时段打卡、提前打卡、逾期补卡。打卡入口使用 HapticFeedback + ViewModel 的 incrementCompletionCount 直接完成，没有时段感知。HabitCompletion 表已有时段无关的打卡记录。

## Goals / Non-Goals

**Goals:**
- 建立习惯状态体系：无状态/待打卡/即将开始/已完成/逾期
- 打卡按时段绑定：每个打卡对应一个具体提醒时段
- 单提醒全天可打，多提醒提前1h窗口 + 逾期可补
- 打卡上限控制 = reminderTimes.length
- UI 状态展示：Badge、进度(x/y)、按钮三态
- EntryZone 卡片数据来自状态计数

**Non-Goals:**
- 不实现提醒通知系统（仅为此做准备）
- 不实现局域网同步
- 不修改 Records 页面核心架构
- 不改动导航系统

## Decisions

### D1: 状态计算在 ViewModel 层，不做复杂 SQL
- **选择**：ViewModel 中通过 combine(habitsFlow, todayCompletionsFlow) 计算状态
- **理由**：状态计算涉及时间比对和多状态组合逻辑，SQL 难以表达。在 ViewModel 层利用 Kotlin Flow 做响应式合并，清晰可测
- **替代方案**：Room 中做一个聚合查询返回状态 — 复杂且不灵活

### D2: newCheckIn() 替代修改现有 incrementCompletionCount
- **选择**：新增 `performSlotCheckIn()` 方法，保留旧的 incrementCompletionCount 只在非 slot 场景兜底
- **理由**：新逻辑完全不同（slot 分配、isLate、limit 检查），改旧方法风险高，且旧数据兼容需要旧逻辑
- **替代方案**：重构增 incrementCompletionCount — 破坏现有已测逻辑

### D3: slotTime="" 兼容旧数据
- **选择**：旧记录 slotTime=""，状态计算忽略空 slotTime 的记录
- **理由**：无需迁移旧数据，无需占位值
- **替代方案**：全量迁移补 slotTime — 无法推断旧打卡对应哪个时段

### D4: 跨天刷新用 ProcessLifecycleOwner + 定时检查
- **选择**：ProcessLifecycleOwner.onStart 时检测日期变化 + 协程每60秒轮询 currentDate
- **理由**：保证App长时间前台时跨天自动刷新，前后台切换也刷新
- **替代方案**：仅 Ping 或仅定时—各有所缺

### D5: 撤销打卡（undo）删除最近一条记录
- **选择**：按 completedDate 降序取第一条匹配的 HabitCompletion 删除，更新 completedToday 状态
- **理由**：与现有 undo 逻辑一致，用户预期"撤销最近操作"
- **替代方案**：按 slot 撤销 — UX 复杂且不必要

## Risks / Trade-offs

| 风险 | 缓解措施 |
|------|----------|
| slotTime 解析性能问题（数千次打卡记录） | 只按当天日期查询，数据量小（≤50条/天） |
| 多提醒习惯频繁打卡导致 UI 频繁刷新 | StateFlow 使用 distinctUntilChanged |
| 旧记录 slotTime="" 参与统计 | 状态计算明确过滤 empty slotTime |
| 跨天定时检查消耗电量 | 60秒间隔 + 仅检查日期字符串变化，不做网络请求 |
