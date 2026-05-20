# AI Tool 执行修复

## 问题

1. **Gson 数字解析错误**：`ConversationManager.parseArguments()` 使用 `Gson.fromJson(json, Map<String, Any?>)` 反序列化，Gson 默认将所有数字转为 `Double`（如 `[3]` → `[3.0]`）。`CreateHabitTool.parseIntList()` 使用 `it?.toString()?.toIntOrNull()` 导致 `"3.0".toIntOrNull() = null`，所有 repeat_days 值被过滤为空列表 `[]`，导致 WEEKLY 习惯校验失败（"每周习惯必须指定重复日期"），触发 10 次重试后报错。

2. **自动确认过早清除会话**：`ConversationManager.processResponse()` 末尾的自动确认触发后，ViewModel 的 `confirmAndSaveHabits()` 调用 `clearConversation()`，清空所有消息和卡片。用户看不到 HabitCreatedCard，也无法编辑。

3. **缺少自动继续**：第一个习惯自动保存后，不会自动驱动 AI 继续创建剩余习惯，需要用户手动输入。

## 解决方案

### 1. `CreateHabitTool.kt` — 修复 parseIntList

```kotlin
// 改前
is List<*> -> value.mapNotNull { it?.toString()?.toIntOrNull() }
// 改后
is List<*> -> value.mapNotNull { (it as? Number)?.toInt() }
```

`3.0 as? Number` → `3.0`，`3.0.toInt()` → `3` ✅

### 2. `PartialHabit.kt` — 添加 tempId

```kotlin
data class PartialHabit(
    val tempId: UUID = UUID.randomUUID(),  // 新增
    val title: String,
    ...
)
```

用于追踪哪些 PartialHabit 已被自动保存到 DB，以及对应的 DB 记录 ID。

### 3. `AICreateHabitViewModel.kt` — 增量保存 + 保留上下文

- 新增 `savedPartialToDbId: Map<TempId, DbId>` 映射
- `confirmAndSaveHabits()`：只保存 `tempId` 不在映射中的新习惯；
  不再调用 `clearConversation()`；卡片和消息保持可见
- 新增 `deleteHabitByTempId()`：用于编辑后删除旧版本（预留接口）
- `clearConversation()` 同时清空 `savedPartialToDbId`

### 4. `ConversationManager.kt` — 自动继续对话

- 新增 `needsAutoContinue: Boolean` 标志
- `processResponse()` 自动确认改为**每次有习惯收集都发射** `ConfirmationRequested` 事件（不再仅限"全部收集"）
- 当 `pendingCount != null && collectedCount < pendingCount` 时设置 `needsAutoContinue = true`
- `sendToLLMWithRetry()` 循环条件扩展：`while (needsRetry || needsAutoContinue)`
- `needsAutoContinue` 时重置 `retryCount = 0`（不是错误重试，而是正常继续）
- `stop()` 和 `reset()` 同步重置 `needsAutoContinue`

## 修改文件

- `app/src/main/java/io/github/darrindeyoung791/habitpulse/ai/tools/CreateHabitTool.kt` — 1 行
- `app/src/main/java/io/github/darrindeyoung791/habitpulse/ai/conversation/PartialHabit.kt` — +2 行 (import + tempId)
- `app/src/main/java/io/github/darrindeyoung791/habitpulse/viewmodel/AICreateHabitViewModel.kt` — ~15 行
- `app/src/main/java/io/github/darrindeyoung791/habitpulse/ai/conversation/ConversationManager.kt` — ~10 行

## 预期流程

```
用户: "帮我建立每周四吃肯德基和每天早上跑步的习惯"
  ↓
AI: ask_question("什么时间吃肯德基？")
  ↓
用户: "每周四晚上5点"
  ↓
AI: create_habit({title:"吃肯德基", repeat_days:[4], reminder_times:["17:00"]})
  ↓
系统: 显示 HabitCreatedCard ✅ | 增量保存到 DB | 反馈 "(1/2)"
  ↓ 自动继续
AI: ask_question("跑步什么时间？")
  ↓
用户: "每天早上8点"
  ↓
AI: create_habit({title:"跑步", repeat_days:[], reminder_times:["08:00"]}) + reply("已全部完成！")
  ↓
系统: 显示第二张卡片 + 再见文字 | 保存 | 交互保持
```
