## ADDED Requirements

### Requirement: AI 可以解析用户习惯描述
The system SHALL parse natural language habit descriptions from the user and extract relevant information.

#### Scenario: 解析单个完整习惯描述
- **WHEN** user says "提醒我每天早上8点喝水"
- **THEN** AI extracts: title="喝水", repeatCycle="DAILY", reminderTimes=["08:00"]

#### Scenario: 解析多个习惯描述
- **WHEN** user says "提醒我每天早上吃鸡蛋，每周四晚上吃肯德基"
- **THEN** AI extracts two separate habits with appropriate fields

### Requirement: AI 可以询问缺失的必要信息
When habit information is incomplete, the AI SHALL use the ask_question tool to request missing required fields.

#### Scenario: 缺少提醒时间
- **WHEN** AI receives "提醒我每天早上吃鸡蛋" without a specific time
- **THEN** AI outputs ask_question with type="time" to ask for the reminder time

#### Scenario: 缺少重复周期
- **WHEN** AI receives habit description without repeat cycle (DAILY/WEEKLY)
- **THEN** AI outputs ask_question with type="choice" to ask for repeat cycle

#### Scenario: 缺少重复星期（WEEKLY 模式）
- **WHEN** user describes a weekly habit without specifying which days
- **THEN** AI outputs ask_question with type="day_of_week" to ask for the days

### Requirement: AI 可以创建完整习惯
When all required information is collected, the AI SHALL output create_habit tool with complete parameters.

#### Scenario: 创建 DAILY 习惯
- **WHEN** AI has title, repeatCycle="DAILY", and reminderTimes
- **THEN** AI outputs create_habit tool with all required fields

#### Scenario: 创建 WEEKLY 习惯
- **WHEN** AI has title, repeatCycle="WEEKLY", repeatDays, and reminderTimes
- **THEN** AI outputs create_habit tool with all required fields

### Requirement: AI 可以处理非习惯相关输入
When user input is not related to habit creation, the AI SHALL use the reply tool to guide the conversation.

#### Scenario: 用户询问功能用法
- **WHEN** user asks "这个功能怎么用？"
- **THEN** AI outputs reply with brief explanation guiding user to describe a habit

#### Scenario: 用户打招呼
- **WHEN** user says "你好"
- **THEN** AI outputs reply with brief greeting and prompt to describe a habit

### Requirement: 系统检测死循环
The conversation manager SHALL detect when AI asks the same question repeatedly and stop the conversation.

#### Scenario: 连续相同问题超过3次
- **WHEN** AI outputs the same questionId more than 3 consecutive times
- **THEN** system stops the conversation and displays "检测到重复提问，已自动停止"

#### Scenario: 超过总问题上限
- **WHEN** total number of questions exceeds 20
- **THEN** system stops the conversation and displays appropriate message

### Requirement: 系统支持答案提交后继续对话
User answers a question, the system SHALL send the answer back to AI and continue the conversation.

#### Scenario: 用户回答时间选择
- **WHEN** user selects a time from the time picker (e.g., "07:00")
- **THEN** system sends answer to AI and AI continues with next question or creates habit

#### Scenario: 用户选择选项
- **WHEN** user clicks on a choice option
- **THEN** system sends the selected value to AI and AI continues

### Requirement: 系统执行创建习惯工具
When AI outputs create_habit tool, the system SHALL validate and prepare the habit for confirmation.

#### Scenario: 验证通过
- **WHEN** AI outputs create_habit with all required fields (title, repeatCycle, reminderTimes, repeatDays for WEEKLY)
- **THEN** system collects the habit and continues conversation

#### Scenario: 验证失败（缺少字段）
- **WHEN** AI outputs create_habit missing required fields
- **THEN** system rejects execution and sends feedback to AI asking to complete the information

### Requirement: AI 语言跟随用户输入
The AI SHALL respond in the same language as the user's input, regardless of system language settings.

#### Scenario: 用户使用中文
- **WHEN** user writes input in Chinese
- **THEN** AI responds in Chinese throughout the conversation

#### Scenario: 用户使用英文
- **WHEN** user writes input in English
- **THEN** AI responds in English throughout the conversation

### Requirement: 系统强制检测任务完成（强制流程）
The system SHALL track the number of habits the user intends to create and enforce completion.

#### Scenario: 用户描述多个习惯
- **WHEN** user says "帮我创建两个习惯：每天跑步、每周读书"
- **THEN** system extracts intent count = 2 and stores `pendingHabitCount = 2`

#### Scenario: AI 创建所有习惯后触发确认
- **WHEN** `pendingHabitCount = N` AND AI has collected and confirmed `N` habits via `create_habit`
- **THEN** system displays the batch confirmation dialog

#### Scenario: AI 提前输出确认（强制干预）
- **WHEN** AI outputs `confirm` before `pendingHabitCount` habits are collected
- **THEN** system sends a correction message to AI: "用户描述了 N 个习惯，您只收集了 M 个，请继续收集剩余习惯。" (where N = pendingHabitCount, M = collected count)
- **AND** the incorrect `confirm` is NOT rendered as UI

#### Scenario: pendingHabitCount 未设置时（单个习惯）
- **WHEN** user input does not specify a count (e.g., "帮我创建一个习惯")
- **THEN** system sets `pendingHabitCount = 1` after first `create_habit`

#### Scenario: 用户在对话过程中追加描述
- **WHEN** user mid-conversation says "再帮我加一个习惯：每周游泳"
- **THEN** system increments `pendingHabitCount` and AI continues collecting

### Requirement: 系统处理无法生成习惯的输入（兜底回复）
When user input cannot be used to generate any habit, the system SHALL return a concise help message.

#### Scenario: 用户询问功能用法
- **WHEN** user asks "这个功能怎么用？" or "how does this work?"
- **THEN** AI outputs `reply` tool with a brief help message (max 50 words in user's language)
- **AND** the help message does NOT contain `ask_question` or `create_habit`

#### Scenario: 用户打招呼
- **WHEN** user says "你好" or "hi"
- **THEN** AI outputs `reply` tool with brief greeting + one-line habit prompt

#### Scenario: 用户询问无关话题
- **WHEN** user says something completely unrelated (e.g., "今天天气怎么样")
- **THEN** AI outputs `reply` tool with a brief redirect message (max 30 words)
- **AND** the message guides user back to habit creation

### Requirement: 系统检测不构成习惯的输入
The ConversationManager SHALL classify user input into habit-related or non-habit-related.

#### Scenario: 分类为习惯相关
- **WHEN** user input contains keywords such as "习惯", "提醒", "每天", "每周", "运动", "跑步", "喝水", "读书" (or English equivalents: habit, remind, daily, weekly, exercise, run, water, read)
- **THEN** system marks this as `habitRelated = true`

#### Scenario: 分类为非习惯相关
- **WHEN** user input does not contain any habit-related keywords
- **THEN** system marks this as `habitRelated = false`
- **AND** sends the full user text to AI with the fallback prompt (do not extract or guess habits)

### Requirement: 用户可以停止对话并获取已收集的习惯
When user stops the conversation via stop button, the system SHALL display all already-collected habits for confirmation.

#### Scenario: 用户主动停止
- **WHEN** user taps the stop button
- **THEN** if `collectedHabits.isNotEmpty()`, system shows the collected habits summary
- **AND** user can confirm to create them or discard

#### Scenario: 停止时无已收集习惯
- **WHEN** user taps stop button with no habits collected
- **THEN** system exits the conversation directly