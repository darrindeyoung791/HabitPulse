## 1. 数据层

- [x] 1.1 数据库 v3→v4 迁移：HabitCompletion 新增 slotTime 和 isLate 字段
- [x] 1.2 HabitCompletion 数据类更新：添加 slotTime（String）和 isLate（Boolean）字段
- [x] 1.3 HabitDatabase 注册 MIGRATION_3_4 并更新版本号
- [x] 1.4 HabitCompletionDao 新增：getCompletionsByDate(date)、getCompletionByHabitIdDateAndSlot(habitId, date, slotTime)
- [x] 1.5 Repository 新增 getTodayCompletions(): 按当天日期查询所有打卡记录
- [x] 1.6 Repository 新增 performSlotCheckIn(): slot 分配 + 插入记录 + 更新统计

## 2. 状态计算引擎

- [x] 2.1 新建 HabitStatus 枚举（NO_STATUS, PENDING_TODAY, ABOUT_TO_START, COMPLETED_TODAY, OVERDUE）
- [x] 2.2 新建 HabitWithStatus 数据类（habit, todayCompletions, status, pendingCount）
- [x] 2.3 ViewModel 新增 todayCompletionsFlow（每日定时刷新 + 每60s定时检查）
- [x] 2.4 ViewModel 新增 habitsWithStatusFlow: combine(habitsFlow, todayCompletionsFlow) → 计算每个习惯的状态
- [x] 2.5 ViewModel 新增 pendingTodayCount / aboutToStartCount / overdueCount 计数 StateFlows
- [x] 2.6 跨天自动刷新：协程每60s 检查 currentDate 变化

## 3. 打卡交互改造

- [x] 3.1 实现 findTargetSlot 算法（支持单提醒/多提醒/时间窗口/逾期补卡）
- [x] 3.2 实现 performSlotCheckIn：分配 slot → 创建 HabitCompletion → 判断 isLate → 更新 habits 统计
- [x] 3.3 实现打卡上限控制：已达 reminderTimes.length 时返回 ALREADY_COMPLETED
- [x] 3.4 实现 undo 适配：按最近一次 completedDate 删除 HabitCompletion
- [x] 3.5 RewardSheet 控制：仅当非补卡且全部完成时触发

## 4. UI 适配

- [x] 4.1 HabitCard 增加状态 Badge：即将开始（accent）/逾期（warning）/已完成（success）
- [x] 4.2 HabitCard 增加进度显示 x/y（多提醒习惯显示分数进度）
- [x] 4.3 打卡按钮使用新 slot 打卡方法（performSlotCheckIn）
- [x] 4.4 EntryZone 新增"即将开始"和"逾期"两张入口卡片
- [x] 4.5 EntryZone 卡片数据源改为从 ViewModel 的计数 StateFlows 获取
- [x] 4.6 TodayHabitsScreen 更新：使用 habitsWithStatusFlow，支持状态过滤参数
- [x] 4.7 空计数时对应入口卡片自动隐藏（entry-zone-status spec）
