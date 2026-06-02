# 打卡状态体系 - 设计方案

## 概述

当前 Habit 模型使用 `completedToday: Boolean` 表示当天是否完成，只支持"每天一次打卡"的简单场景。新方案需要支持多时段打卡、逾期补卡、提前打卡等多种场景，并为后续的提醒系统和履行追踪奠定基础。

---

## 一、核心概念

### 1.1 打卡时段定义

每个习惯的 `reminderTimes`（如 `["08:00","20:00"]`）定义了当天的打卡时段。**每个提醒时间对应一个必须完成的打卡时段**。

- 一个习惯有 N 个提醒时间 → 每天需要完成 N 次打卡
- 每个打卡记录**严格绑定**到具体的提醒时段
- 当日完成次数 = reminderTimes.length → 今天已完成

### 1.2 提前打卡窗口

- **单提醒习惯**（reminderTimes.length == 1）：随时可打卡，不限制时间窗口
- **多提醒习惯**（reminderTimes.length >= 2）：当前时间 >= 该时段时间 - 1小时 才能打卡该时段

### 1.3 逾期判定

- 当前时间 > 最后一个提醒时间 + 1小时，且有未完成的时段 → 整体标记为逾期
- 逾期后仍可打卡（补卡），补卡需要不同的视觉反馈

---

## 二、习惯状态体系

### 2.1 状态定义

| 状态 | 含义 | 互斥关系 |
|------|------|---------|
| **无状态** | 习惯今天不适用（如每周习惯不在对应星期） | 独占 |
| **今天待打卡** | 今天适用，存在未完成的时段 | 基础状态，可叠加附加状态 |
| **即将开始** | 存在未完成时段距离当前时间 <= 1小时内 | 附加状态，叠加在 今天待打卡 之上 |
| **今天已完成** | 所有时段都已打卡完成 | 独占 |
| **逾期** | 存在未完成时段已超过该时段时间+1小时 | 附加状态，叠加在 今天待打卡 之上 |

### 2.2 状态组合

```
核心状态（互斥）：
  无状态 | 今天已完成 | 今天待打卡

附加状态（叠加在 今天待打卡 之上）：
  - 即将开始（1小时内有时段）
  - 逾期（有时段已超时）
```

**实际组合表：**

| 组合 | 含义 | 示例 |
|------|------|------|
| 无状态 | 今天不适用 | 每周三的习惯，今天周二 |
| 今天待打卡 | 有未完成时段，不紧急无逾期 | [08:00,20:00]，当前 10:00，08:00 已完成 |
| 今天待打卡 + 即将开始 | 有时段即将开始 | [08:00,20:00]，当前 19:20，08:00 已完成，20:00 即将开始 |
| 今天待打卡 + 逾期 | 有时段已逾期 | [08:00,20:00]，当前 21:30，20:00 逾期未打 |
| 今天待打卡 + 即将开始 + 逾期 | 有些逾期，有些即将开始 | [08:00,20:00]，当前 19:20，两个都未完成 |
| 今天已完成 | 全部完成 | [08:00,20:00]，当前 21:00，两个都已完成 |

### 2.3 状态计算逻辑（伪代码）

```
fun calculateStatus(habit, todayCompletions, currentTime):
    if !isApplicableToday(habit): return {NO_STATUS}

    allSlots = habit.reminderTimes              // ["08:00","20:00"]
    completedSlotTimes = todayCompletions.map { it.slotTime }.toSet()
    incompleteSlots = allSlots.filter { it !in completedSlotTimes }

    if incompleteSlots.isEmpty(): return {COMPLETED_TODAY}

    status = mutableSetOf(PENDING_TODAY)

    hasOverdue = false
    hasAboutToStart = false

    for slot in incompleteSlots:
        slotTime = parseTime(slot)
        if currentTime >= slotTime - 1h && currentTime <= slotTime + 1h:
            hasAboutToStart = true
        if currentTime > slotTime + 1h:
            hasOverdue = true

    if hasOverdue: status.add(OVERDUE)
    if hasAboutToStart: status.add(ABOUT_TO_START)

    return status
```

---

## 三、数据模型变更

### 3.1 HabitCompletion 新增字段

```kotlin
@Entity(tableName = "habit_completions")
data class HabitCompletion(
    @PrimaryKey
    val id: UUID = UUID.randomUUID(),
    val habitId: UUID,
    val completedDate: Long = System.currentTimeMillis(),
    val completedDateLocal: String,
    val timeZone: String = java.util.TimeZone.getDefault().id,
    // 新增 ↓
    val slotTime: String,         // 绑定的提醒时段，如 "08:00"
    val isLate: Boolean = false   // 是否逾期补卡
)
```

- `slotTime`：记录此次打卡对应的是哪个提醒时段，用于按时段严格绑定
- `isLate`：区分准时打卡与逾期补卡，用于视觉反馈差异化

### 3.2 新增 Kotlin 类型

```kotlin
// 状态枚举
enum class HabitStatus {
    NO_STATUS,
    PENDING_TODAY,
    ABOUT_TO_START,
    COMPLETED_TODAY,
    OVERDUE
}

// 习惯 + 当天状态 打包
data class HabitWithStatus(
    val habit: Habit,
    val todayCompletions: List<HabitCompletion>,
    val status: Set<HabitStatus>
) {
    val pendingCount: Int
        get() = habit.getReminderTimesList().size - todayCompletions.size

    val isCompletelyOverdue: Boolean
        get() = status.contains(OVERDUE) && !status.contains(ABOUT_TO_START)
}
```

### 3.3 DAO 新增方法

```kotlin
// HabitCompletionDao
@Query("""
    SELECT * FROM habit_completions 
    WHERE completedDateLocal = :date 
    ORDER BY completedDate ASC
""")
suspend fun getCompletionsByDate(date: String): List<HabitCompletion>

@Query("""
    SELECT * FROM habit_completions 
    WHERE habitId = :habitId AND completedDateLocal = :date AND slotTime = :slotTime
    LIMIT 1
""")
suspend fun getCompletionByHabitIdDateAndSlot(
    habitId: UUID, date: String, slotTime: String
): HabitCompletion?
```

---

## 四、打卡流程

### 4.1 打卡分配算法

```
User taps check-in → findTargetSlot(habit, todayCompletions, currentTime)

Algorithm:
1. allSlots = sorted(habit.reminderTimes)  // ["08:00","20:00"]
2. completedSlotTimes = todayCompletions.map { it.slotTime }
3. incompleteSlots = allSlots - completedSlotTimes  // 未完成时段

4. if incompleteSlots.isEmpty():
   → 今天已完成，不可再打卡（显示 Toast 提示）

5. if habit is 单提醒 (reminderTimes.length == 1):
   → 直接打卡该时段

6. if habit is 多提醒 (reminderTimes.length >= 2):
   6a. 找出 incompleteSlots 中第一个满足以下条件的时段：
       - parseTime(slot) - 1h <= currentTime
   6b. 如果没找到（当前时间远早于所有未完成时段）：
       → 提示 "最早可打卡时间为 ${firstSlot - 1h}"
   6c. 分配到该时段
```

### 4.2 isLate 判定

```
if currentTime > parseTime(slotTime) + 1h:
   isLate = true   // 补卡
else:
   isLate = false  // 准时打卡/提前打卡
```

### 4.3 打卡上限

- 每天每个习惯最多打卡次数 = `reminderTimes.length`
- 达到上限后再次点击 → 提示"今日已完成"
- RewardSheet 仅当次非逾期打卡时触发

### 4.4 补卡视觉反馈

| 场景 | 按钮状态 | 打卡后反馈 | RewardSheet |
|------|---------|-----------|-------------|
| 准时打卡（isLate=false） | 正常绿色 | ✅ 勾选动画 | 🎉 显示庆祝 |
| 补卡（isLate=true） | 橙色/警告色 | ⚠️ 补卡标记 | ❌ 不显示，改为轻提示 |

---

## 五、UI 层变化

### 5.1 EntryZone 变化

| 卡片 | 当前行为 | 新行为 |
|------|---------|--------|
| 今天的习惯 | 显示习惯总数 | 显示 **今天待打卡** 习惯数（排除已完成和无状态的） |
| 即将开始 | 不存在 | 新增：显示即将开始（1h内有时段）的习惯数 |
| 逾期 | 不存在 | 新增：显示逾期习惯数 |

EntryZone 卡片布局：
```
┌──────────────────┐  ┌──────────────────┐  ┌──────────────────┐
│  📋 今日待打卡    │  │  ⏰ 即将开始      │  │  ⚠️ 逾期        │
│  3项待完成        │  │  1项即将开始      │  │  2项已超时      │
│  badge: 3         │  │  badge: 1        │  │  badge: 2       │
└──────────────────┘  └──────────────────┘  └──────────────────┘
```

### 5.2 习惯卡片 UI 调整

```
正常状态：
┌─────────────────────────────────────────────────┐
│  跑步                     ⏰ 即将开始 · 1/2    │
│  已完成 12 次                                    │
│  每日 · 08:00 · 20:00                          │
│  下次打卡：19:00（1小时后）                    │
│                                     [✅ 打卡]    │
└─────────────────────────────────────────────────┘

逾期补卡状态：
┌─────────────────────────────────────────────────┐
│  吃药                     ⚠️ 逾期 · 1/3       │
│  已完成 45 次                                    │
│  每日 · 07:00 · 12:00 · 18:00                  │
│  12:00 时段已逾期 3 小时                        │
│                                     [⚠️ 补卡]    │
└─────────────────────────────────────────────────┘

已完成状态：
┌─────────────────────────────────────────────────┐
│  跑步                     ✅ 今天已完成 · 2/2 │
│  已完成 12 次                                    │
│  每日 · 08:00 · 20:00                          │
│                                     [✅ 已打卡]  │
└─────────────────────────────────────────────────┘
```

### 5.3 进度显示

- 习惯卡片右上角或标题旁显示 `x/y`（今日已完成/今日需完成）
- 完整进度：`1/2`, `2/3`, `3/3 ✅`

---

## 六、ViewModel 层变化

### 6.1 新增 Flow

```kotlin
class HabitViewModel(...) {
    // 新核心 Flow：习惯 + 状态
    val habitsWithStatusFlow: Flow<List<HabitWithStatus>>

    // 当日打卡记录（供状态计算）
    private val todayCompletionsFlow: Flow<List<HabitCompletion>>

    // 计数 StateFlows（供 EntryZone）
    val pendingTodayCount: StateFlow<Int>
    val aboutToStartCount: StateFlow<Int>
    val overdueCount: StateFlow<Int>
}
```

### 6.2 打卡方法重构

```kotlin
// 返回结果封装
sealed class CheckInResult {
    data class Success(val isLate: Boolean, val isAllCompleted: Boolean) : CheckInResult()
    data class AlreadyCompleted(val maxCount: Int) : CheckInResult()
    data class TooEarly(val earliestSlotTime: String) : CheckInResult()
}

fun checkIn(habit: Habit) {
    viewModelScope.launch {
        val result = repository.performCheckIn(habit)
        when (result) {
            is CheckInResult.Success -> {
                if (result.isAllCompleted && !result.isLate) {
                    showRewardSheet(habit)
                }
            }
            is CheckInResult.AlreadyCompleted -> {
                _snackbarMessage.value = "今日已完成"
            }
            is CheckInResult.TooEarly -> {
                _snackbarMessage.value = "最早 $earliestSlotTime 可打卡"
            }
        }
    }
}
```

---

## 七、数据库迁移

### 7.1 Migration v3 → v4

```kotlin
val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("""
            ALTER TABLE habit_completions
            ADD COLUMN slotTime TEXT NOT NULL DEFAULT ''
        """)
        db.execSQL("""
            ALTER TABLE habit_completions
            ADD COLUMN isLate INTEGER NOT NULL DEFAULT 0
        """)
    }
}
```

### 7.2 历史数据兼容

- 旧打卡记录 `slotTime = ""` → 视为"未标记时段"记录
- 状态计算时忽略 slotTime 为空的记录（不计入当天完成度）
- 旧记录仍然展示在 Records 页面中，标记为历史记录

---

## 八、分步实施计划

### Phase 1: 数据层
- [ ] 数据库 v3→v4 迁移（slotTime, isLate）
- [ ] HabitCompletion 数据类更新
- [ ] DAO 新增按日期/时段查询方法
- [ ] Repository 新增打卡逻辑（分配算法）

### Phase 2: 状态计算
- [ ] 新增 HabitStatus 枚举 + HabitWithStatus 数据类
- [ ] ViewModel 状态计算引擎（合并 habits + completions）
- [ ] 新增计数 Flow（给 EntryZone 使用）

### Phase 3: 打卡交互
- [ ] 实现时段分配算法（findTargetSlot）
- [ ] 实现 isLate 判定 + 上限控制
- [ ] 补卡与准时的视觉差异
- [ ] Snackbar 提示（已完成/时间未到）

### Phase 4: UI 适配
- [ ] 习惯卡片增加进度显示 (x/y)
- [ ] 习惯卡片增加状态 Badge（即将开始/逾期）
- [ ] 打卡按钮状态区分（正常/补卡/已完成禁止）
- [ ] EntryZone 数据更新（待打卡计数、即将开始计数）
- [ ] TodayHabitsScreen 接入状态数据

---

## 九、开放问题（待确认）

1. **撤销打卡（undo）**：当前 undo 逻辑是删除最近一次打卡记录。改为 slot 绑定后，undo 是否按 slot 删除？比如撤销 08:00 时段的打卡（即使 20:00 时段已经打过）
2. **跨天打卡**：如果习惯有 23:00 的提醒，用户在 00:30 补卡，属于昨天还是今天？
3. **App 恢复时重算**：App 从后台恢复或跨天时，需要重新计算所有习惯的状态。现有的 `habitsFlow` 是 Room Flow 会自动更新，但 `todayCompletionsFlow` 需要确保跨天时自动刷新
