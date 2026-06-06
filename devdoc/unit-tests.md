# 单元测试文档

## 概览

66 个单元测试，分布在 4 个测试类中，覆盖 `data/model` 包的核心业务逻辑。

```bash
./gradlew testDebugUnitTest   # 运行全部单元测试
```

---

## 目录

- [SlotCheckInEngineTest](#slotcheckinenginetest) — 31 tests
- [HabitTest](#habitest) — 17 tests
- [HabitStatusTest](#habitstatustest) — 12 tests
- [HabitCompletionTest](#habitcompletiontest) — 5 tests

---

## SlotCheckInEngineTest

**测试对象**: `SlotCheckInEngine`（纯逻辑引擎，无 Android 依赖）

**辅助工具**:
- `timestamp(y, m, d, h, min)` — 将日历时间转成毫秒戳，注入 `currentTimeMillis` 以控制时间
- `habit(...)` — 构造任意配置的 Habit 实例
- `completion(habitId, slotTime)` — 构造打卡记录

### isApplicableToday（3 tests）

| 测试 | 条件 | 预期 |
|------|------|------|
| `returns true for daily habit` | `RepeatCycle.DAILY` | `true` |
| `returns true when today matches repeat day` | WEEKLY, repeatDays=[0], 周一 | `true` |
| `returns false when today does not match repeat day` | WEEKLY, repeatDays=[1] (周二), 周一 | `false` |
| `returns true for weekly with all 7 days` | WEEKLY, repeatDays=[0..6], 周一/二/日 | `true` |
| `returns false for weekly with empty repeatDays` | WEEKLY, repeatDays=[] | `false` |

### calculateHabitStatus（12 tests）

时窗规则：对每个 Slot，`[slotTime - 1h, slotTime + 1h]` 为 `ABOUT_TO_START`；超过 `slotTime + 1h` 为 `OVERDUE`。

| 测试 | 条件 | 预期 Status |
|------|------|-------------|
| `returns COMPLETED_TODAY when all slots completed` | slots [08:00, 20:00] 全部完成 | `{COMPLETED_TODAY}` |
| `returns PENDING_TODAY only when far before first slot` | slot 08:00, 当前 05:00 | `{PENDING_TODAY}` |
| `returns ABOUT_TO_START within 1h window` | slot 08:00, 当前 07:30 | `{PENDING_TODAY, ABOUT_TO_START}` |
| `returns ABOUT_TO_START at exact slot time` | slot 08:00, 当前 08:00 | `{PENDING_TODAY, ABOUT_TO_START}` |
| `returns OVERDUE when past slot plus 1h` | slot 08:00, 当前 09:30 | `{PENDING_TODAY, OVERDUE}` |
| `returns ABOUT_TO_START at slot plus exactly 1h boundary` | slot 08:00, 当前 09:00 | `{PENDING_TODAY, ABOUT_TO_START}` |
| `returns OVERDUE just past 1h boundary` | slot 08:00, 当前 09:01 | `{PENDING_TODAY, OVERDUE}` |
| `returns NO_STATUS when not applicable today` | WEEKLY 周二, 周一 | `{NO_STATUS}` |
| `returns mixed status with multiple slots` | slots [08:00, 20:00], 当前 09:30 | `{PENDING_TODAY, OVERDUE}` |
| `one slot overdue and one about to start` | slots [08:00, 10:00], 当前 09:30 | `{PENDING_TODAY, OVERDUE, ABOUT_TO_START}` |
| `with some slots completed` | slots [08:00, 12:00, 20:00], 08:00 完成, 当前 13:01 | `{PENDING_TODAY, OVERDUE}` |
| **`with empty slots returns PENDING_TODAY`** | 无提醒习惯, 每日 | `{PENDING_TODAY}` |
| **`with empty slots returns NO_STATUS for non-applicable weekly`** | 无提醒 + WEEKLY 不适用 | `{NO_STATUS}` |
| **`at slotTime minus oneHour exactly is ABOUT_TO_START`** | slot 08:00, 当前 07:00 (边界点 `>=`) | `{PENDING_TODAY, ABOUT_TO_START}` |
| **`just before slotTime minus oneHour is not ABOUT_TO_START`** | slot 08:00, 当前 06:59 | `{PENDING_TODAY}` |
| **`ignores old-style completions without slotTime`** | slot 08:00, 完成记录不带 slotTime, 当前 12:00 | `{PENDING_TODAY, OVERDUE}` |

> **粗体** = 新增的边缘情况测试

### executeSlotCheckIn（11 tests）

| 测试 | 条件 | 预期 |
|------|------|------|
| `returns NotApplicableToday for wrong day` | WEEKLY 周二, 周一 | `NotApplicableToday`, callback NOT invoked |
| `returns AlreadyCompleted when all slots done` | slot 08:00 已完成 | `AlreadyCompleted(1)` |
| `single slot on time` | slot 08:00, 当前 08:00 | `Success(isLate=false, isAllCompleted=true)` |
| `single slot late` | slot 08:00, 当前 10:00 | `Success(isLate=true, isAllCompleted=true)` |
| `multiple slots picks first incomplete within window` | slots [08:00(✓), 12:00, 20:00], 当前 12:30 | `Success`, slot=12:00 |
| `last slot remaining returns isAllCompleted true` | slots [08:00(✓), 12:00], 当前 12:00 | `Success(isAllCompleted=true)` |
| `returns TooEarly when before all slots window` | slots [08:00, 20:00], 当前 06:00 | `TooEarly(earliestSlotTime="07:00")` |
| `single slot is never TooEarly` | slot 08:00, 当前 05:00（未到窗口） | `Success`（单 slot 立即打卡，不检查窗口） |
| `multiple slots all completed` | 3 slots 全部完成 | `AlreadyCompleted(3)` |
| **`with empty slots returns AlreadyCompleted with count 0`** | 无提醒习惯 | `AlreadyCompleted(0)` |

---

## HabitTest

**测试对象**: `Habit` 实体类中的 JSON 解析 helper 和 `copyWith*` 工厂方法。

### getRepeatDaysList（4 tests）

| 测试 | 输入 JSON | 预期 |
|------|-----------|------|
| `returns empty list for empty JSON` | `"[]"` | `[]` |
| `parses single element` | `"[0]"` | `[0]` |
| `parses multiple elements` | `"[0,2,4]"` | `[0, 2, 4]` |
| `parses all weekdays` | `"[0,1,2,3,4]"` | `[0, 1, 2, 3, 4]` |
| **`parses all 7 days Sunday through Saturday`** | `"[0,1,2,3,4,5,6]"` | `[0, 1, 2, 3, 4, 5, 6]` |
| **`preserves duplicate values`** | `"[0,0,1]"` | `[0, 0, 1]`（代码不做去重） |

### getReminderTimesList（3 tests）

| 测试 | 输入 JSON | 预期 |
|------|-----------|------|
| `returns empty for empty JSON` | `"[]"` | `[]` |
| `parses single time` | `'["08:00"]'` | `["08:00"]` |
| `parses multiple times` | `'["08:00","12:00","20:00"]'` | `["08:00", "12:00", "20:00"]` |
| **`handles empty string inside list`** | `'[""]'` | `[""]` |

### getSupervisorEmailsList / getSupervisorPhonesList（2 tests）

验证 Email/Phone JSON 解析。

### copyWith* 方法（5 tests）

验证 `copyWithRepeatDays`、`copyWithReminderTimes`、`copyWithSupervisorEmails`、`copyWithSupervisorPhones`、`copyWithSortOrder` 正确生成新的 JSON 字符串。

---

## HabitStatusTest

**测试对象**: `HabitWithStatus` 数据类的计算属性 `pendingCount` 和 `isCompletelyOverdue`。

### pendingCount（4 tests）

`pendingCount = reminderTimes.size - todayCompletions.size`

| 测试 | 条件 | 预期 |
|------|------|------|
| `equals reminder count minus completed count` | 3 reminders, 1 completion | `2` |
| `is zero when all slots completed` | 1 reminder, 1 completion | `0` |
| `equals reminder count when no completions` | 2 reminders, 0 completions | `2` |
| `is zero for habit with no reminders` | 0 reminders | `0` |
| **`can be negative when completions exceed reminders`** | 1 reminder, 2 completions | `-1` |
| **`includes completions without slotTime`** | 1 reminder, 1 old-style completion (slotTime="") | `0` |

> ⚠️ **已知问题**: `pendingCount` 与 `calculateHabitStatus` 对旧版完成记录的处理不一致。
> - `pendingCount` 计数所有 `todayCompletions`（包括 `slotTime=""` 的记录）
> - `calculateHabitStatus` 只认 `slotTime.isNotEmpty()` 的记录
> - 当旧版完成记录存在时，两者给出矛盾的未完成数量

### isCompletelyOverdue（5 tests）

`isCompletelyOverdue = OVERDUE in status && ABOUT_TO_START not in status`

| 测试 | Status 组合 | 预期 |
|------|------------|------|
| `true when OVERDUE and not ABOUT_TO_START` | `{PENDING_TODAY, OVERDUE}` | `true` |
| `false when only ABOUT_TO_START` | `{PENDING_TODAY, ABOUT_TO_START}` | `false` |
| `false when both OVERDUE and ABOUT_TO_START` | `{PENDING_TODAY, OVERDUE, ABOUT_TO_START}` | `false` |
| `false when COMPLETED_TODAY` | `{COMPLETED_TODAY}` | `false` |
| `false when NO_STATUS` | `{NO_STATUS}` | `false` |
| **`false when PENDING_TODAY only`** | `{PENDING_TODAY}` | `false` |

---

## HabitCompletionTest

**测试对象**: `HabitCompletion` 实体类。

| 测试 | 验证点 | 说明 |
|------|--------|------|
| `getTodayDate returns yyyy-MM-dd format` | 正则匹配 `\d{4}-\d{2}-\d{2}` | 格式正确性 |
| `getTodayDate month and day are zero-padded` | 各部分长度: year=4, month=2, day=2 | 零填充 |
| `getFormattedDate returns completedDateLocal` | `getFormattedDate()` 返回构造函数传入的日期 | 透传 |
| `new completion has empty slotTime by default` | 默认 `slotTime=""` | 默认值 |
| `new completion is not late by default` | 默认 `isLate=false` | 默认值 |

---

## 测试策略说明

### 时间控制

`SlotCheckInEngine` 的所有函数接受 `currentTimeMillis` 参数（默认 `System.currentTimeMillis()`），测试用例通过 `timestamp()` 辅助函数构造可控的日历时间戳注入，从而不依赖系统时钟。

### 纯逻辑提取

所有测试针对 `data/model` 包中的纯 Kotlin 代码，没有 Activity、Fragment、Composable 等 Android 框架依赖。这保证了测试在 JVM 上秒级运行。

### org.json:json 依赖

Android 的 `org.json.JSONArray` 在单元测试环境中被 mock，所有方法默认抛出 `RuntimeException`。测试通过添加 `org.json:json:20230227` 依赖提供真正的 JVM 实现。

### 边界覆盖

测试覆盖的边界条件包括：
- 时窗的精确边界点（`slotTime - 1h`、`slotTime + 1h`）
- 边界内外 1 分钟差异
- 空列表（无提醒、无重复日）
- 重复值和特殊字符
- 旧版数据兼容（无 slotTime 的完成记录）
- WEEKLY 全部 7 天/空列表
