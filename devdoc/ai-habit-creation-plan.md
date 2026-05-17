# AI 创建习惯功能 - 实现计划

## 1. 概述

用户通过自然语言描述习惯，AI 解析意图并逐步引导用户完善信息，最终创建习惯。

## 2. LLM 配置

用户自行配置兼容 OpenAI 格式的 API：

| 设置项     | 类型   | 默认值                                                    | 说明               |
| ---------- | ------ | --------------------------------------------------------- | ------------------ |
| API 端点   | String | `https://open.bigmodel.cn/api/paas/v4/`                   | OpenAI 兼容接口    |
| API 密钥   | String | 空                                                        | 密文显示           |
| 模型名称   | String | `glm-4.7-flash`                                             | 模型名             |

- 提交请求时拼接 `/chat/completions`
- 支持智谱 GLM、OpenAI、DeepSeek 等
- 配置存储在 DataStore (`UserPreferences`)

### 2.1 HTTP 调用参数

```json
POST {endpoint}/chat/completions

Headers:
  Content-Type: application/json
  Authorization: Bearer {api_key}

Body:
{
    "model": "{model}",
    "messages": [...],
    "stream": false,
    "thinking": { "type": "enabled" },
    "max_tokens": 4096,
    "temperature": 0.7
}
```

## 3. 对话机制

核心思路：不依赖 LLM 原生 Function Calling，改用 Prompt 引导模型输出闭合的 Markdown 代码块，程序端解析后执行。

### 3.1 工具调用格式

模型需要执行指令时，使用名称为工具名的 Markdown 代码块包裹可运行的 JSON 指令。

**代码块 = 可运行指令**。代码块之外不要有任何多余的话、解释或评论。

```create_habit
{
    "habitId": "0",
    "title": "吃鸡蛋",
    "repeatCycle": "DAILY",
    "reminderTimes": ["07:00"]
}
```

```ask_question
{
    "questionId": "0_reminderTime",
    "type": "time",
    "prompt": "早上几点吃鸡蛋？",
    "context": "早上",
    "options": ["06:00", "07:00", "08:00"],
    "allowCustomInput": true
}
```

### 3.2 解析逻辑

```kotlin
val pattern = Regex("""```(\w+)\n(.*?)```""", RegexOption.DOT_MATCHES_ALL)
val matches = pattern.findAll(responseText)

for (match in matches) {
    val toolName = match.groupValues[1]    // "create_habit" / "ask_question"
    val argsJson = match.groupValues[2]    // JSON 字符串
    // 根据 toolName 映射到对应工具并执行
}
```

每次模型响应可能包含 0~N 个代码块，按**出现顺序**依次执行。

### 3.3 对话流程

```
用户输入 "提醒我每天早上吃鸡蛋，每周四晚上吃肯德基"
    │
    ▼
LLM 解析（带历史）
  → 识别出 2 个习惯
  → 第 0 个习惯缺时间
  → 输出 ```ask_question...```
    │
    ▼
解析到 ask_question → 渲染时间选择器 UI
  "早上几点吃鸡蛋？"  [6点] [7点] [8点] [🔔滚轮]
    │
    ▼ (用户选择 07:00)
    
LLM 继续解析（带历史）
  → 第 1 个习惯缺时间
  → 输出 ```ask_question...```
    │
    ▼
解析到 ask_question → 渲染时间选择器 UI
  "晚上几点吃肯德基？"  [17点] [18点] [19点] [🔔滚轮]
    │
    ▼ (用户选择 18:00)
    
LLM 继续解析
  → 所有信息齐全
  → 输出 2 个 ```create_habit...```
    │
    ▼
解析到 2 个 create_habit → 收集到习惯列表
  → 渲染确认卡片
  "确认创建以下习惯？"
  ☐ 每天 07:00 吃鸡蛋
  ☑ 每周四 18:00 吃肯德基
  [确认创建] [取消]
    │
    ▼ (用户确认)
批量保存到数据库 → 完成
```

一次只问一个问题。Answer 提交后再交给 LLM 决定下一步。

## 4. 工具定义（完整）

LLM 可输出的代码块工具共 **3 个**：`create_habit`、`ask_question`、`reply`。

### 4.1 create_habit — 创建习惯

**用途**：当 LLM 认为某个习惯的必填信息已收集完整时，输出此工具来创建它。

**`habitId` 命名规则**：与 `ask_question` 的 `questionId` 一致，使用 `{index}` 作为标识。

| 场景                    | habitId          | 对应的 questionId           |
| ----------------------- | ---------------- | --------------------------- |
| 第 1 个习惯             | `0`              | `0_reminderTime`, `0_repeatDays` |
| 第 2 个习惯             | `1`              | `1_reminderTime`            |

**输出格式**：

```create_habit
{
    "habitId": "0",
    "title": "吃鸡蛋",
    "repeatCycle": "DAILY",
    "repeatDays": [1, 3, 5],
    "reminderTimes": ["07:00"],
    "notes": "每天一个水煮蛋",
    "supervisionMethod": "NONE",
    "supervisorEmails": [],
    "supervisorPhones": []
}
```

**参数字段**：

| 字段               | 类型           | 必填  | 说明                                                            |
| ------------------ | -------------- | ----- | --------------------------------------------------------------- |
| `habitId`          | String         | 是    | 习惯序号，与 `questionId` 的 `{index}` 部分一致                  |
| `title`            | String         | 是    | 习惯名称，≤100 字符                                             |
| `repeatCycle`      | String         | 是    | `"DAILY"` 或 `"WEEKLY"`                                         |
| `repeatDays`       | Int[]          | 条件  | WEEKLY 时必填，0=周日 … 6=周六，如 `[1,3,5]` 表示一三五         |
| `reminderTimes`    | String[]       | 是    | 提醒时间，格式 `"HH:mm"`，至少一项，如 `["07:00"]`              |
| `notes`            | String         | 否    | 备注，≤2000 字符                                                 |
| `supervisionMethod`| String         | 否    | `"NONE"` / `"EMAIL"` / `"SMS"`，默认 `"NONE"`                   |
| `supervisorEmails` | String[]       | 条件  | EMAIL 时必填                                                     |
| `supervisorPhones` | String[]       | 条件  | SMS 时必填                                                       |

**执行逻辑**：

```kotlin
fun executeCreateHabit(params: CreateHabitParams): ValidationResult {
    // 1. 检查必填字段
    val missingFields = mutableListOf<String>()
    if (params.title.isNullOrBlank()) missingFields.add("title")
    if (params.repeatCycle == null) missingFields.add("repeatCycle")
    if (params.reminderTimes.isNullOrEmpty()) missingFields.add("reminderTimes")
    if (params.repeatCycle == "WEEKLY" && params.repeatDays.isNullOrEmpty()) missingFields.add("repeatDays")

    // 2. 不完整 → 拒绝执行，让 AI 补全
    if (missingFields.isNotEmpty()) {
        throw IncompleteHabitException(
            habitId = params.habitId,
            missingFields = missingFields
        )
    }

    // 3. 完整 → 映射为 Habit 实体，保存到 Room
    val habit = mapToHabit(params)
    repository.insert(habit)
}
```

校验不通过时，程序端向 LLM 发一条补全指令（见 4.3）。

---

### 4.2 ask_question — 向用户提问

**用途**：当 LLM 发现某个习惯缺少必填信息时，输出此工具向用户提问。

**输出格式**：

```
```ask_question
{
    "questionId": "0_reminderTime",
    "type": "time",
    "prompt": "早上几点吃鸡蛋？",
    "context": "早上",
    "options": ["06:00", "07:00", "08:00"],
    "allowCustomInput": true
}
```

```

**通用参数字段**（所有 type 共用）：

| 字段               | 类型      | 必填  | 说明                                         |
| ------------------ | --------- | ----- | -------------------------------------------- |
| `questionId`       | String    | 是    | 唯一标识，用于去重检测                       |
| `type`             | String    | 是    | 见下方子类型表                               |
| `prompt`           | String    | 是    | 向用户展示的问题文本                         |
| `context`          | String    | 否    | 辅助说明，如 "早上" / "晚上" / "工作日"       |
| `options`          | String[]  | 否    | 快捷选项，AI 根据上下文生成少量合理选项       |
| `allowCustomInput` | Boolean   | 否    | 是否允许用户自由输入，默认 `false`            |

**`questionId` 命名规则**：`{habitIndex}_{field}`

| questionId               | 场景                    |
| ------------------------ | ----------------------- |
| `0_reminderTime`         | 第 1 个习惯缺提醒时间   |
| `1_reminderTime`         | 第 2 个习惯缺提醒时间   |
| `2_repeatDays`           | 第 3 个习惯缺重复星期   |
| `3_supervision`          | 第 4 个习惯询问监督方式 |

---

#### 4.2.1 type = "choice" — 单选卡片

**场景**：二选一或多选一，如选择重复周期、监督方式等。

**示例**：

```ask_question
{
    "questionId": "0_repeatCycle",
    "type": "choice",
    "prompt": "吃鸡蛋是每天坚持还是每周某几天？",
    "options": ["每天", "每周特定几天"],
    "allowCustomInput": false
}
```

```ask_question
{
    "questionId": "0_supervision",
    "type": "choice",
    "prompt": "是否需要添加监督人？",
    "options": ["不需要", "通过邮件监督", "通过短信监督"],
    "allowCustomInput": false
}
```

**UI 渲染**：选项卡片 Column，点击选中高亮。`allowCustomInput=true` 时额外显示输入框。

**用户回答格式**：

```json
{ "type": "choice", "value": "每天" }
```

---

#### 4.2.2 type = "time" — 时间选择器

**场景**：询问具体提醒时间，如"早上几点"、"晚上几点"。

**示例**：

```ask_question
{
    "questionId": "0_reminderTime",
    "type": "time",
    "prompt": "早上几点吃鸡蛋？",
    "context": "早上",
    "options": ["06:00", "07:00", "08:00"],
    "allowCustomInput": true
}
```

```ask_question
{
    "questionId": "1_reminderTime",
    "type": "time",
    "prompt": "晚上几点吃肯德基？",
    "context": "晚上",
    "options": ["17:00", "18:00", "19:00"],
    "allowCustomInput": true
}
```

**UI 渲染**：
- 快捷 Chip 行：将 `options` 渲染为可点击的 Chip（如"6点""7点""8点"）
- 时间滚轮图标：点击展开 Material `TimePickerDialog`
- 选择后回填到气泡

**用户回答格式**：

```json
{ "type": "time", "value": "07:00" }
```

---

#### 4.2.3 type = "day_of_week" — 星期选择器

**场景**：习惯是 WEEKLY 模式时，询问周几重复。

**示例**：

```ask_question
{
    "questionId": "0_repeatDays",
    "type": "day_of_week",
    "prompt": "哪些天吃鸡蛋？",
    "context": "",
    "options": ["每天", "工作日", "周末"],
    "allowCustomInput": false
}
```

**UI 渲染**：
- 星期按钮行：M T W T F S S（点击切换选中/未选中，中文下使用形如「周一」的格式）
- 快捷模式 Chip：`options` 中的预设项（"每天"="全选"、"工作日"="一二三四五"、"周末"="六日"）

**用户回答格式**：

```json
{ "type": "day_of_week", "value": [1, 2, 3, 4, 5] }
```

---

#### 4.2.4 type = "multi_choice" — 多选卡片

**场景**：让用户在多个待创建习惯中勾选/取消，如"你确认要创建以下哪些习惯？"。

**示例**：

```ask_question
{
    "questionId": "confirm_habits",
    "type": "multi_choice",
    "prompt": "确认要创建以下习惯吗？取消勾选则不创建",
    "options": ["每天 07:00 吃鸡蛋", "每周四 18:00 吃肯德基"],
    "allowCustomInput": false
}
```

**UI 渲染**：多选卡片列表，每项前有 Checkbox，默认全选。

**用户回答格式**：

```json
{ "type": "multi_choice", "value": ["每天 07:00 吃鸡蛋"] }
```

---

#### 4.2.5 type = "confirm" — 确认卡片

**场景**：最终确认前展示摘要，让用户确认或取消。

**示例**：

```ask_question
{
    "questionId": "final_confirm",
    "type": "confirm",
    "prompt": "即将创建以下 2 个习惯，确认吗？",
    "options": [
        "① 每天 07:00 吃鸡蛋",
        "② 每周四 18:00 吃肯德基"
    ],
    "allowCustomInput": false
}
```

**UI 渲染**：摘要列表 + 一行确认/取消按钮。每个摘要项可点击 → 跳转编辑。

**用户回答格式**：

```json
{ "type": "confirm", "value": true }
```

---

#### 4.2.6 type = "text" — 文本卡片（兜底）

**场景**：没有合适预设时，让用户自由输入（兜底方案）。

**示例**：

```ask_question
{
    "questionId": "0_notes",
    "type": "text",
    "prompt": "关于吃鸡蛋还有什么要备注的吗？",
    "options": [],
    "allowCustomInput": true
}
```

**UI 渲染**：`OutlinedTextField`，支持多行输入。

**用户回答格式**：

```json
{ "type": "text", "value": "每天一个水煮蛋" }
```

---

### 4.3 reply — 非习惯类回复

**用途**：用户的输入与创建习惯无关时（询问功能、闲聊等），使用此工具回复用户。

**输出格式**：

```reply
{
    "text": "请描述你想要创建的习惯，例如：\"每天早上8点喝水\"。"
}
```

**参数字段**：

| 字段   | 类型   | 必填 | 说明           |
| ------ | ------ | ---- | -------------- |
| `text` | String | 是   | 回复用户的内容 |

**场景**：

| 用户输入                     | `reply` 回复内容                                              |
| ---------------------------- | ------------------------------------------------------------- |
| "这个功能怎么用？"           | "你可以直接描述你想要创建的习惯，比如'每天早上8点喝水'"       |
| "新建一个习惯需要什么信息？" | "需要一个名称、重复周期（每天或每周）、提醒时间。请你描述吧"  |
| "你好"                       | "你好！请描述你想要创建的习惯"                                 |

---

### 4.4 工具协作关系

```
用户输入
    │
    ▼
LLM 判断意图：
  │
  ├── 是习惯创建相关
  │   ├── 信息完整 → ```create_habit```
  │   └── 缺信息    → ```ask_question```
  │
  └── 不是习惯创建相关
      → ```reply``` 回复引导用户
    │
    ▼
程序端按输出顺序依次处理代码块：

  遇到 reply
    → 直接渲染文本到对话界面，不暂停（用户可继续输入）

  遇到 ask_question
    → 暂停，渲染 UI，等用户回答

  遇到 create_habit
    → 校验必填字段（title / repeatCycle / reminderTimes / WEEKLY 时的 repeatDays）
    │
    ├── 校验通过 → 收集到 collectedHabits（不立刻保存）
    │
    └── 校验不通过 → 拒绝执行，给 LLM 发系统消息：
        "create_habit for habit {habitId} 缺少 [{缺失字段}]，请用 ask_question 补全"
        → 本轮作废，LLM 重新生成

  所有代码块处理完毕
    │
    ▼
  有 reply 无其他 → 等待用户下一轮输入
  有 ask_question → 等用户回答后继续循环
  有 create_habit 校验不通过 → LLM 重新生成
  所有 collectedHabits 完整且无待回答问题
    → 进入确认阶段
```

### 4.4 变量追踪（避免重复提问）

```kotlin
data class PendingQuestion(
    val questionId: String,              // 唯一标识
    val habitIndex: Int,                 // 关联第几个习惯
    val field: String,                   // 目标字段名
    val type: String,                    // choice / time / day_of_week / ...
    val prompt: String,                  // 问题文本
    val context: String?,                // 辅助说明
    val options: List<String>?,          // 快捷选项
    val allowCustomInput: Boolean,       // 允许自定义
    var isAnswered: Boolean = false,     // 已回答
    var answer: Any? = null              // 答案值
)
```

### 4.5 死循环检测（安全守卫）

```kotlin
data class ConversationGuard(
    val maxConsecutiveSameQuestion: Int = 3,   // 连续相同问题上限
    val maxTotalQuestions: Int = 20,            // 总问题上限
    val maxRetries: Int = 3,                    // API 重试上限
)
```

| 检测项                     | 触发条件              | 处理                         |
| -------------------------- | --------------------- | ---------------------------- |
| 连续相同问题               | 相同 questionId ≥ 3   | 强制停止，提示用户           |
| 总问题上限                 | 问题数量 > 20         | 强制停止                     |
| API 重试上限               | 连续失败 ≥ 3          | 递增延迟后提示用户           |
| 手动停止                   | 用户点击停止按钮      | 立即停止，展示已收集信息     |

## 5. System Prompt 设计

### 5.1 中文版

```
你是一个习惯创建助手。你的任务是解析用户的自然语言描述，提取习惯信息，并输出可执行的指令。

## 核心规则：输出即指令

你必须将可执行的指令放入名称为工具名的 Markdown 代码块中。
**代码块 = 可运行指令，不要在里面写注释或解释。**
代码块之外不要有多余的话、解释或评论。

**正确示范**（只输出代码块，没有废话）：

```create_habit
{"habitId": "0", "title": "吃鸡蛋", "repeatCycle": "DAILY", "reminderTimes": ["07:00"]}
```

**错误示范**（说太多话）：

好的，我现在来帮你创建习惯。首先我理解你想要每天吃鸡蛋，让我来创建这个习惯。
```create_habit
{"habitId": "0", "title": "吃鸡蛋", "repeatCycle": "DAILY", "reminderTimes": ["07:00"]}
```
已经为您创建成功！

## 第一原则：判断用户意图

收到任何用户的输入后，首先判断意图：

| 意图                         | 行为                         |
| ---------------------------- | ---------------------------- |
| 描述习惯创建（如"每天早上8点喝水"） | 按规则提取信息 → 调用工具     |
| 询问如何创建习惯             | 用 `reply` 简要说明所需信息   |
| 询问功能用法                 | 用 `reply` 引导用户描述习惯   |
| 打招呼/闲聊                  | 用 `reply` 简短回应后引导     |

**注意**：即使意图不是习惯创建，`reply` 回复也要简短直接，不要长篇大论。

## 可用工具

### create_habit
当你获得一个习惯的完整信息时，输出此工具。

```create_habit
{
    "habitId": "0",
    "title": "习惯名称",
    "repeatCycle": "DAILY",
    "reminderTimes": ["08:00"]
}
```

### ask_question
当缺少必要信息时，输出此工具向用户提问。

```ask_question
{
    "questionId": "序号_字段名",
    "type": "choice/time/day_of_week/multi_choice/confirm/text",
    "prompt": "问题内容",
    "context": "早上/晚上/工作日",
    "options": ["选项A", "选项B"],
    "allowCustomInput": false
}
```

### reply
当用户的输入与创建习惯无关时，输出此工具回复用户。**也要简短，不要啰嗦**。

```reply
{
    "text": "请描述你想要创建的习惯，例如：\"每天早上8点喝水\"。"
}
```

## 工具使用规则

1. `type` 为 `choice` 时，`options` 由你根据对话上下文给出少量合理选项
2. `type` 为 `time` 时，`context` 说明是早上/中午/晚上，`options` 给合理的时间点
3. `type` 为 `day_of_week` 时，`options` 可填 ["每天", "工作日", "周末"]
4. `type` 为 `multi_choice` 时，`options` 填待选择的习惯摘要
5. `type` 为 `confirm` 时，`options` 填所有待创建习惯的摘要
6. 不要问多余的问题。优先获取必填：名称、重复周期、提醒时间
7. 当你认为信息收集完毕，输出 create_habit
8. 一次只问一个问题，不要同时输出多个 ask_question
9. 不要重复相同的 questionId
10. 最多创建 5 个习惯
11. 根据用户输入的语言回复（用户写中文就回中文，写英文就回英文，不依赖任何系统设置）
```

### 5.2 英文版

```
You are a habit creation assistant. Parse the user's natural language, extract habit information, and output executable instructions.

## Core Rule: Output = Instruction

Put executable instructions inside Markdown code blocks named after the tool.
**The code block IS the executable instruction — no comments or explanations inside.**
Do NOT output any text outside code blocks — no greetings, no explanations, no follow-ups.

**Correct** (code block only, no extra words):

```create_habit
{"habitId": "0", "title": "Eat eggs", "repeatCycle": "DAILY", "reminderTimes": ["07:00"]}
```

**Wrong** (too much talking):

Okay, I understand you want to eat eggs every day. Let me create this habit for you.
```create_habit
{"habitId": "0", "title": "Eat eggs", "repeatCycle": "DAILY", "reminderTimes": ["07:00"]}
```
Done! I've successfully created the habit!

## First Principle: Classify Intent

On every user input, first determine the intent:

| Intent                        | Action                           |
| ----------------------------- | -------------------------------- |
| Describing habit creation     | Extract info → call tools        |
| Asking how to create habits   | Use `reply` to briefly guide     |
| Asking about functionality    | Use `reply` to redirect          |
| Greetings / chit-chat         | Use `reply` briefly, then guide  |

**Keep `reply` short and direct even for non-habit inputs.**

## Available Tools

### create_habit
When you have all required information for a habit, output this tool.

```create_habit
{
    "habitId": "0",
    "title": "habit name",
    "repeatCycle": "DAILY",
    "reminderTimes": ["08:00"]
}
```

### ask_question
When required information is missing, output this tool to ask the user.

```ask_question
{
    "questionId": "index_field",
    "type": "choice/time/day_of_week/multi_choice/confirm/text",
    "prompt": "question text",
    "context": "morning/evening/weekday",
    "options": ["Option A", "Option B"],
    "allowCustomInput": false
}
```

### reply
When the user's input is not about creating habits, output this tool to respond. **Keep it brief.**

```reply
{
    "text": "Please describe the habit you want to create, e.g. \"drink water at 8am every day\"."
}
```

## Rules

1. `type=choice` → give a few reasonable options
2. `type=time` → set context to morning/afternoon/evening, give reasonable times
3. `type=day_of_week` → options can be ["Every day", "Weekdays", "Weekends"]
4. `type=multi_choice` → options are habit summaries
5. `type=confirm` → options are all habit summaries
6. Only ask necessary questions. Priority: name, repeat cycle, reminder time
7. Output create_habit when ready
8. Ask ONE question at a time
9. Never repeat the same questionId
10. Max 5 habits
11. Match the user's input language — if they write Chinese, reply in Chinese; if English, reply in English.
```

## 6. UI 设计

### 6.1 FAB 入口 → 创建方式选择

```
    ┌────────────────────┐
    │    新建习惯          │
    ├────────────────────┤
    │ ✏️  手动填写        │
    │ 🤖  AI 智能创建     │
    └────────────────────┘
```

点击 FAB 弹出 BottomSheet / Dialog → 选择进入对应页面。

### 6.2 AI 创建界面 (AICreateHabitScreen)

```
┌──────────────────────────────────────┐
│ ← AI 创建习惯              [停止] X   │  ← TopAppBar
├──────────────────────────────────────┤
│                                      │
│  ┌─────────────────────────┐         │
│  │ 提醒我每天早上吃鸡蛋...  │ ← 用户   │
│  └─────────────────────────┘         │
│                                      │
│  ┌─────────────────────────┐         │
│  │ 早上几点吃鸡蛋？          │ ← AI     │
│  │ [6点] [7点] [8点] [🔔]   │ ← 快捷   │
│  └─────────────────────────┘    Chip  │
│                                      │
│  ┌─────────────────────────┐         │
│  │ 7点                     │ ← 用户   │
│  └─────────────────────────┘         │
│                                      │
│  ┌─────────────────────────┐         │
│  │ 晚上几点吃肯德基？        │ ← AI     │
│  │ [17点] [18点] [19点]    │          │
│  └─────────────────────────┘         │
│                                      │
│  ┌─────────────────────────┐         │
│  │ ☐ 每天 07:00 吃鸡蛋     │ ← 确认   │
│  │ ☑ 每周四 18:00 吃肯德基 │   卡片   │
│  │   [确认创建] [取消]      │         │
│  └─────────────────────────┘         │
│                                      │
├──────────────────────────────────────┤
│ [描述你的习惯...          [发送]  ]  │  ← 底部输入
└──────────────────────────────────────┘
```

### 6.3 对话气泡组件 (ConversationBubble)

| 方向  | 样式                          |
| ----- | ----------------------------- |
| 用户  | 右对齐，底色 accent           |
| AI    | 左对齐，底色 surfaceVariant   |
| 系统  | 居中，小字，可带错误样式       |

AI 气泡内可内嵌问题组件（Chip、TimePicker、选项卡片等）。

### 6.4 问题组件明细

| 组件                        | type            | 渲染内容                                                    |
| --------------------------- | --------------- | ----------------------------------------------------------- |
| `ChoiceQuestionContent`     | `choice`        | 选项卡片 Column，点击选中高亮                               |
| `MultiChoiceQuestionContent`| `multi_choice`  | 选项卡片 + Checkbox，默认全选                               |
| `TimeQuestionContent`       | `time`          | 快捷 Chip Row + 滚轮图标 → 展开 TimePickerDialog            |
| `DayOfWeekQuestionContent`  | `day_of_week`   | 星期按钮 Row (M/T/W/T/F/S/S) + 快捷模式 Chip               |
| `ConfirmQuestionContent`    | `confirm`       | 摘要列表 + 确认/取消按钮，可点击跳转编辑                     |
| `TextQuestionContent`       | `text`          | OutlinedTextField（支持多行）                                |

### 6.5 加载 & 错误状态

**加载状态**：AI 思考时显示 "思考中..." + 打字动画（三个跳动圆点）。

**错误状态**：
- 错误消息以系统气泡居中显示（红色文字 + 红色左边框）
- 气泡底部显示 [重试] [停止] 按钮

## 7. 确认与编辑机制

### 7.1 确认阶段（type = "confirm"）

当所有习惯信息已收集完整时，LLM 输出 `type: "confirm"` 的问题，程序端渲染确认卡片：

- 展示所有待创建习惯的摘要列表
- 每个摘要项可点击 → 跳转到 `HabitCreationScreen`（预填参数，可编辑）
- 用户确认 → 批量保存到 Room 数据库
- 用户取消 → 返回对话界面

### 7.2 编辑跳转

```kotlin
navController.navigate(
    "${RouteConfig.HABIT_CREATION}?habitIndex={index}&prefill={jsonEncoded}"
)
```

复用 `HabitCreationScreen`，接收 `prefill` 参数解析为 Habit 对象预填充表单。保存后返回 `AICreateHabitScreen`。

### 7.3 停止后恢复

手动停止后，对话界面显示已收集的习惯摘要 + "是否继续或清空" 选项。

## 8. 错误处理机制

### 8.1 错误类型一览

| 错误场景           | 用户消息                               | 处理方式                           |
| ------------------ | -------------------------------------- | ---------------------------------- |
| API 连接超时       | "连接超时，请检查网络"                 | 自动重试，递增延迟                 |
| API Key 无效       | "API 密钥无效，请前往设置检查"         | 停止，引导到设置                   |
| 模型返回空内容     | "AI 未返回有效指令"                    | 重试 1 次                          |
| 代码块解析失败     | "AI 输出格式异常"                      | 重试 1 次，带上"请严格按格式输出"  |
| 连续相同提问       | "检测到重复提问，已自动停止"           | 强制停止                           |
| 创建数据库失败     | "保存习惯失败：{具体原因}"             | 显示具体错误                       |
| 用户手动停止       | "操作已手动停止"                       | 展示已收集信息                     |

### 8.2 自动重试策略

```kotlin
data class RetryPolicy(
    val maxRetries: Int = 3,
    val baseDelayMs: Long = 1000L,
)

fun getRetryDelay(attempt: Int): Long =
    baseDelayMs * (1L shl (attempt - 1))  // 1s → 2s → 4s
```

### 8.3 用户交互

所有错误和重试状态都以**系统气泡**形式展示在对话界面中，让用户清晰感知发生了什么。

## 9. 多语言支持

### 9.1 语言策略

AI 的语言永远跟随用户第一条消息的语言，不依赖系统语言设置。

```
用户第一句话是中文 → AI 全程用中文交互
用户第一句话是英文 → AI 全程用英文交互
用户第一句话是日文 → AI 全程用日文交互
```

**原理**：
- 用户永远先说话，AI 永远后回复
- 用户用什么语言写，AI 就用什么语言回复
**实现方式**：在 System Prompt 中要求 AI 根据用户消息的语言回复，不硬编码任何语言。

### 9.2 UI 字符串

所有 UI 文本使用 `stringResource`，维护在 `strings.xml` 及其 locale 变体中。

## 10. 数据结构与状态管理

### 10.1 对话状态

```kotlin
data class AIConversationState(
    val messages: List<ChatMessage>,               // 对话历史
    val pendingQuestions: Queue<PendingQuestion>,  // 待处理问题
    val collectedHabits: List<PartialHabit>,       // 已收集的习惯
    val currentQuestion: PendingQuestion?,         // 当前显示的问题
    val isLoading: Boolean,                        // AI 思考中
    val isFinished: Boolean,                       // 对话结束
    val isError: Boolean,                          // 错误状态
    val errorMessage: String?,                     // 错误信息
    val retryCount: Int,                           // 当前重试次数
)
```

### 10.2 对话历史消息

```kotlin
sealed class ChatMessage {
    data class UserMessage(val text: String) : ChatMessage()
    data class AIMessage(val text: String, val toolCall: ToolCall?) : ChatMessage()
    data class SystemMessage(val text: String, val isError: Boolean = false) : ChatMessage()
    data class QuestionMessage(
        val question: PendingQuestion,
        val answered: Boolean = false
    ) : ChatMessage()
}
```

### 10.3 习惯中间状态

```kotlin
data class PartialHabit(
    val index: Int,                          // 在列表中的序号，对应 create_habit 的 habitId
    val title: String?,                      // 习惯名称
    val repeatCycle: RepeatCycle?,           // 重复周期
    val repeatDays: List<Int>?,              // 重复星期
    val reminderTimes: List<String>?,        // 提醒时间
    val notes: String?,                      // 备注
    val supervisionMethod: SupervisionMethod?,
    val supervisorEmails: List<String>?,
    val supervisorPhones: List<String>?,
) {
    val isComplete: Boolean
        get() = title != null && repeatCycle != null &&
                !reminderTimes.isNullOrEmpty()

    val missingFields: List<String>
        get() = buildList {
            if (title == null) add("title")
            if (repeatCycle == null) add("repeatCycle")
            if (repeatCycle == RepeatCycle.WEEKLY && repeatDays.isNullOrEmpty()) add("repeatDays")
            if (reminderTimes.isNullOrEmpty()) add("reminderTimes")
        }
}
```

## 11. ConversationManager 核心逻辑

### 11.1 类结构

```kotlin
class ConversationManager(
    private val llmClient: LLMClient,
    private val retryPolicy: RetryPolicy = RetryPolicy(),
    private val guard: ConversationGuard = ConversationGuard(),
) {
    private var state = AIConversationState(...)
    private var consecutiveSameQuestion = 0
    private var previousQuestionId: String? = null
    private var retryAttempt = 0
}
```

### 11.2 处理用户输入

```kotlin
suspend fun processUserInput(userText: String): AIConversationState {
    state.messages.add(ChatMessage.UserMessage(userText))

    loop@ while (true) {
        // 1. 调用 LLM
        val response = llmClient.chat(
            messages = buildMessages(state),
            systemPrompt = getSystemPrompt()
        )

        if (response.isError) {
            // 自动重试逻辑
            if (retryAttempt < retryPolicy.maxRetries) {
                retryAttempt++
                val delay = retryPolicy.getRetryDelay(retryAttempt)
                delay(delay)
                continue
            } else {
                return state.copy(
                    isError = true,
                    errorMessage = "重试次数已达上限"
                )
            }
        }
        retryAttempt = 0

        // 2. 解析工具调用
        val toolCalls = ResponseParser.parse(response.text)

        if (toolCalls.isEmpty()) {
            // 无工具调用（不应发生，但防御处理）
            state.messages.add(ChatMessage.SystemMessage("AI 未返回有效指令"))
            return state.copy(isError = true, errorMessage = "AI 未返回有效指令")
        }

        // 3. 逐个执行工具
        var hasReply = false
        var hasQuestion = false
        for (toolCall in toolCalls) {
            when (toolCall.name) {
                "reply" -> {
                    val text = parseReplyText(toolCall)
                    state.messages.add(ChatMessage.AIMessage(text, null))
                    hasReply = true
                }
                "ask_question" -> {
                    val question = parseQuestion(toolCall)
                    if (checkDuplicate(question)) return state.copy(isError = true, ...)
                    state.pendingQuestions.add(question)
                    state.currentQuestion = question
                    hasQuestion = true
                }
                "create_habit" -> {
                    val habit = parseHabit(toolCall)
                    val missing = habit.missingFields
                    if (missing.isNotEmpty()) {
                        val feedback = ChatMessage.SystemMessage(
                            "create_habit for habit ${habit.index} 缺少 [${
                                missing.joinToString(", ")
                            }]，请用 ask_question 询问用户补全"
                        )
                        state.messages.add(feedback)
                        continue@loop
                    }
                    state.collectedHabits.add(habit)
                }
            }
        }

        if (hasQuestion) return state  // 暂停等用户
        if (hasReply && !hasQuestion) return state  // 纯回复，等用户继续输入
        if (state.collectedHabits.isNotEmpty() &&
            state.collectedHabits.all { it.isComplete }) {
            state.isFinished = true
            return state
        }
    }
}
```

### 11.3 提交答案

```kotlin
suspend fun submitAnswer(answer: Any): AIConversationState {
    val question = state.currentQuestion ?: return state
    question.isAnswered = true
    question.answer = answer

    // 构建 LLM 消息：AI 的问题 → 用户的回答
    state.messages.add(ChatMessage.QuestionMessage(question, answered = true))
    state.currentQuestion = null

    return processUserInput("")  // 触发 LLM 继续
}
```

### 11.4 停止

```kotlin
fun stop(): AIConversationState {
    state.isFinished = true
    state.isError = true
    state.errorMessage = "操作已手动停止"
    return state
}
```

## 12. 文件清单 & 实现顺序

### Phase 1: 配置层

| 文件路径                                       | 说明                       |
| ---------------------------------------------- | -------------------------- |
| `data/preferences/UserPreferences.kt`          | 添加 LLM 配置字段          |
| `ui/screens/SettingsActivity.kt`               | 添加 AI 设置区块           |

### Phase 2: LLM 客户端

| 文件路径                                       | 说明                       |
| ---------------------------------------------- | -------------------------- |
| `ai/llm/LLMConfig.kt`                          | 配置数据类                 |
| `ai/llm/LLMClient.kt`                          | HTTP 调用 + 超时 + 重试     |
| `ai/llm/LLMRequest.kt`                         | 请求/响应数据类             |
| `ai/llm/ResponseParser.kt`                     | 代码块解析正则              |

### Phase 3: 工具系统

| 文件路径                                       | 说明                       |
| ---------------------------------------------- | -------------------------- |
| `ai/tools/Tool.kt`                             | 工具接口                   |
| `ai/tools/ToolRegistry.kt`                     | 工具名 → 执行器映射         |
| `ai/tools/CreateHabitTool.kt`                  | create_habit 执行逻辑       |
| `ai/tools/QuestionTool.kt`                     | ask_question 执行逻辑        |
| `ai/tools/ReplyTool.kt`                        | reply 执行逻辑               |

### Phase 4: 对话管理

| 文件路径                                       | 说明                       |
| ---------------------------------------------- | -------------------------- |
| `ai/conversation/ConversationState.kt`         | 状态数据类                 |
| `ai/conversation/ConversationManager.kt`       | 对话主循环                 |
| `ai/conversation/Guard.kt`                     | 安全守卫（死循环检测）      |
| `ai/prompt/SystemPrompt.kt`                    | 中/英文 System Prompt       |

### Phase 5: UI 层

| 文件路径                                               | 说明                     |
| ------------------------------------------------------ | ------------------------ |
| `ai/ui/AICreateHabitScreen.kt`                          | 主对话界面               |
| `ai/ui/ConversationBubble.kt`                          | 对话气泡组件             |
| `ai/ui/components/ChoiceQuestionContent.kt`             | 单选卡片                 |
| `ai/ui/components/MultiChoiceQuestionContent.kt`        | 多选卡片                 |
| `ai/ui/components/TimeQuestionContent.kt`               | 时间选择器               |
| `ai/ui/components/DayOfWeekQuestionContent.kt`          | 星期选择器               |
| `ai/ui/components/ConfirmQuestionContent.kt`            | 确认卡片                 |
| `ai/ui/components/TextQuestionContent.kt`               | 文本卡片                 |
| `ui/screens/HomeScreen.kt`                              | 修改 FAB 入口            |
| `ui/screens/HabitCreationScreen.kt`                     | 支持预填参数             |

### Phase 6: 集成

| 文件路径                                       | 说明                       |
| ---------------------------------------------- | -------------------------- |
| `navigation/Route.kt`                          | 添加 AI 创建路由           |
| `navigation/HabitPulseNavGraph.kt`             | 添加导航                   |
| `res/values/strings.xml` × 4 locales          | 多语言字符串               |

## 13. 字符串资源

### 13.1 中文版 (`values/strings.xml`)

```xml
<!-- AI 创建习惯 -->
<string name="ai_create_title">AI 创建习惯</string>
<string name="ai_create_option">AI 智能创建</string>
<string name="manual_create_option">手动填写</string>
<string name="ai_input_placeholder">描述你的习惯，例如：每天早上8点喝水…</string>
<string name="ai_send_button">发送</string>
<string name="ai_stop_button">停止</string>
<string name="ai_thinking">思考中…</string>
<string name="ai_retry_button">重试</string>
<string name="ai_confirm_button">确认创建</string>
<string name="ai_cancel_button">取消</string>
<string name="ai_confirm_title">确认创建以下习惯？</string>
<string name="ai_stopped_message">操作已手动停止</string>
<string name="ai_max_retries_exceeded">重试次数已达上限，请稍后重试</string>
<string name="ai_repeated_question">检测到重复提问，已自动停止</string>
<string name="ai_api_error">连接失败，请检查网络和 API 配置</string>
<string name="ai_api_key_error">API 密钥无效，请前往设置检查</string>
<string name="ai_parse_error">AI 输出格式异常，正在重试</string>
<string name="ai_habit_limit">一次最多创建 5 个习惯</string>

<!-- 设置 -->
<string name="settings_ai_section">AI 设置</string>
<string name="settings_ai_section_description">配置 AI 创建习惯功能</string>
<string name="settings_api_endpoint_label">API 端点</string>
<string name="settings_api_endpoint_description">OpenAI 兼容接口地址</string>
<string name="settings_api_key_label">API 密钥</string>
<string name="settings_api_key_description">你的 API 密钥</string>
<string name="settings_model_label">模型名称</string>
<string name="settings_model_description">使用的模型名称</string>
```

### 13.2 英文版 (`values-en-rUS/strings.xml`)

```xml
<!-- AI Create Habit -->
<string name="ai_create_title">AI Create Habit</string>
<string name="ai_create_option">AI Smart Create</string>
<string name="manual_create_option">Manual Input</string>
<string name="ai_input_placeholder">Describe your habit, e.g. drink water at 8am every day…</string>
<string name="ai_send_button">Send</string>
<string name="ai_stop_button">Stop</string>
<string name="ai_thinking">Thinking…</string>
<string name="ai_retry_button">Retry</string>
<string name="ai_confirm_button">Confirm</string>
<string name="ai_cancel_button">Cancel</string>
<string name="ai_confirm_title">Confirm to create these habits?</string>
<string name="ai_stopped_message">Operation stopped manually</string>
<string name="ai_max_retries_exceeded">Retry limit reached, please try again later</string>
<string name="ai_repeated_question">Repeated question detected, auto-stopped</string>
<string name="ai_api_error">Connection failed, please check network and API settings</string>
<string name="ai_api_key_error">Invalid API key, please check in Settings</string>
<string name="ai_parse_error">Unexpected AI output format, retrying</string>
<string name="ai_habit_limit">Maximum of 5 habits per session</string>

<!-- Settings -->
<string name="settings_ai_section">AI Settings</string>
<string name="settings_ai_section_description">Configure AI habit creation</string>
<string name="settings_api_endpoint_label">API Endpoint</string>
<string name="settings_api_endpoint_description">OpenAI-compatible API URL</string>
<string name="settings_api_key_label">API Key</string>
<string name="settings_api_key_description">Your API key</string>
<string name="settings_model_label">Model Name</string>
<string name="settings_model_description">Model to use</string>
```

## 14. 潜在风险与缓解

| 风险                         | 缓解策略                                            |
| ---------------------------- | --------------------------------------------------- |
| LLM 输出格式不一致           | 多次重试 + 带上格式纠正指令                          |
| LLM 编造习惯信息             | 必须经过确认卡片才能创建，不自动保存                |
| API Key 泄露                 | 仅存储于 DataStore，不写入日志，传输使用 HTTPS      |
| 无限对话                     | 总问题上限 20 + 连续相同问题上限 3                    |
| 用户一次输入过多习惯         | 上限 5 个 + 多选让用户筛选                          |
| 网络断开                     | 检测网络状态 + 清晰提示 + 自动重试                  |
| 用户退出界面后状态丢失       | 保存对话状态到 ViewModel，横竖屏切换保留            |

## 15. 后续扩展

- AI 建议习惯（根据用户历史数据推荐）
- AI 编辑已创建习惯
- AI 分析习惯完成率与趋势
