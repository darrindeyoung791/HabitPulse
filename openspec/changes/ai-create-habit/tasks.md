## 1. 配置层

- [x] 1.1 在 UserPreferences 添加 LLM 配置字段（apiEndpoint, apiKey, modelName）
- [x] 1.2 创建 AISettingsActivity 子页面（API 端点、密钥、模型名称输入框）
- [x] 1.3 在 SettingsActivity 添加 AI 设置入口（NavigationLink 跳转到 AISettingsActivity）
- [x] 1.4 实现 API 配置验证（检查必填项，测试连接）
- [x] 1.5 实现模型下拉选择（支持 glm-4.7-flash 默认 + 自定义输入）

## 2. LLM 客户端

- [x] 2.1 创建 ai/llm/LLMConfig.kt 配置数据类
- [x] 2.2 创建 ai/llm/LLMRequest.kt 请求/响应数据类（ChatRequest, ChatResponse）
- [x] 2.3 创建 ai/llm/LLMClient.kt HTTP 调用实现（Retrofit 或 URLConnection）
- [x] 2.4 实现超时配置和自动重试机制
- [x] 2.5 创建 ai/llm/ResponseParser.kt 代码块解析正则实现

## 3. 工具系统

- [x] 3.1 创建 ai/tools/Tool.kt 工具接口定义
- [x] 3.2 创建 ai/tools/ToolRegistry.kt 工具名 → 执行器映射
- [x] 3.3 创建 ai/tools/CreateHabitTool.kt create_habit 执行逻辑（参数验证）
- [x] 3.4 创建 ai/tools/QuestionTool.kt ask_question 执行逻辑（解析问题参数）
- [x] 3.5 创建 ai/tools/ReplyTool.kt reply 执行逻辑（解析回复文本）

## 4. 对话管理

- [x] 4.1 创建 ai/conversation/ConversationState.kt 状态数据类（含 pendingHabitCount, isHabitRelated）
- [x] 4.2 创建 ai/conversation/ChatMessage.kt 消息类型（UserMessage, AIMessage, SystemMessage, QuestionMessage）
- [x] 4.3 创建 ai/conversation/PartialHabit.kt 习惯中间状态
- [x] 4.4 创建 ai/conversation/ConversationManager.kt 对话主循环实现（含任务完成检测）
- [x] 4.5 创建 ai/conversation/Guard.kt 安全守卫（死循环检测、问题上限）
- [x] 4.6 创建 ai/prompt/SystemPrompt.kt 中/英文 System Prompt 动态构建

## 5. 任务数量提取

- [x] 5.1 创建 ai/conversation/HabitCountExtractor.kt 从用户输入中提取习惯数量（正则 + 关键词）
- [x] 5.2 在 ConversationManager 初始化时调用数量提取器，设置 pendingHabitCount
- [x] 5.3 支持中途追加习惯数量（用户说"再加一个"时累加 pendingHabitCount）
- [x] 5.4 强制完成检测：AI 提前输出 confirm 时发送修正消息（AI 过早触发场景）

## 6. 兜底回复系统

- [x] 6.1 创建 ai/conversation/HabitClassifier.kt 判断输入是否为习惯相关（关键词匹配）
- [x] 6.2 创建 ai/conversation/FallbackReply.kt 非习惯相关输入的兜底回复逻辑
- [x] 6.3 实现兜底 reply 消息生成（功能用法/打招呼/无关话题三种场景）
- [x] 6.4 确保兜底 reply 不包含 ask_question 或 create_habit 工具调用

## 5. UI 层 - 导航集成

- [x] 5.1 在 navigation/Route.kt 添加 AI 创建路由（ai-create-habit）
- [x] 5.2 在 navigation/HabitPulseNavGraph.kt 添加 AICreateHabitScreen 导航
- [x] 5.3 在 AICreateHabitScreen AppBar 右上角添加设置图标，导航到 AI 设置

## 6. UI 层 - 习惯创建预填

- [ ] 6.1 修改 HabitCreationScreen 接收 prefill 参数
- [ ] 6.2 实现 JSON 解析预填数据到表单字段
- [ ] 6.3 修改保存后返回逻辑（带更新数据回调）

## 7. UI 层 - HomeScreen FAB

- [ ] 7.1 修改 HomeScreen FAB 点击行为
- [ ] 7.2 创建新建习惯选择 BottomSheet/Dialog
- [ ] 7.3 实现导航到对应页面（手动 vs AI）

## 8. UI 层 - AI 创建界面

- [x] 8.1 创建 ai/ui/AICreateHabitScreen.kt 主对话界面
- [ ] 8.2 创建 ai/ui/AGENUISurface.kt AGenUI SurfaceManager 封装（暂不使用AGenUI）
- [x] 8.3 创建 ai/ui/components/UserBubble.kt 用户气泡（32dp圆角，右对齐）
- [x] 8.4 创建 ai/ui/components/AIBubble.kt AI气泡（直角，左对齐）
- [x] 8.5 创建 ai/ui/components/AIWelcomeContent.kt 欢迎语界面
- [x] 8.6 实现输入框动态高度（空3行，1-3行按实际，3-7行按实际，超7行显示7行+滚动）
- [x] 8.7 实现发送/停止按钮切换逻辑
- [ ] 8.8 实现 AI 流式输出 + Markdown 组件渲染（待实现）
- [ ] 8.9 创建 ai/ui/components/ThinkingBlock.kt 思考块（可折叠动画）
- [x] 8.10 创建 ai/ui/components/AIInputBox.kt 输入框组件
- [x] 8.11 实现清空对话按钮 + 确认对话框
- [x] 8.12 实现退出确认对话框（返回按钮）
- [x] 8.13 实现进入 AI 设置前确认对话框

## 9. UI 层 - 问题组件逻辑

- [x] 9.1 实现时间选择答案提交
- [x] 9.2 实现星期选择答案提交
- [x] 9.3 实现选项选择答案提交
- [x] 9.4 实现确认对话框（批量保存逻辑）
- [x] 9.5 实现停止按钮和停止后状态展示

## 10. 字符串资源

- [x] 10.1 在 values/strings.xml 添加 AI 创建相关字符串（中文）
- [ ] 10.2 在 values-en-rUS/strings.xml 添加英文翻译
- [ ] 10.3 在 values-zh-rHK/strings.xml 添加繁体香港翻译
- [ ] 10.4 在 values-zh-rTW/strings.xml 添加繁体台湾翻译

## 11. 测试与验证

- [ ] 11.1 单元测试：ResponseParser 代码块解析
- [ ] 11.2 单元测试：ConversationManager 状态机
- [ ] 11.3 集成测试：完整对话流程（用户输入 → AI 解析 → 问题 → 回答 → 创建）
- [ ] 11.4 手动测试：各种问题类型的 UI 渲染
- [ ] 11.5 手动测试：错误状态和重试逻辑

## 12. 最终整合

- [ ] 12.1 端到端测试：从 FAB → AI 创建 → 完整对话 → 习惯保存
- [ ] 12.2 屏幕旋转测试（状态保持）
- [ ] 12.3 多语言测试（中文/英文对话）
- [ ] 12.4 清理调试代码，优化日志输出

## 13. 对话流程修复：连续聊天与打断按钮

- [x] 13.1 新增 `continueConversation()` 方法用于后续消息（ConversationManager.kt）
  - 不重新检测 `isHabitRelated`，不走 FallbackReply
  - 不重复插入 system prompt
  - 仅添加 user message 后直接调 `sendToLLM()`
- [x] 13.2 `sendMessage()` 区分首次/后续消息（AICreateHabitViewModel.kt）
  - `conversationManager == null` 时创建 Manager 并调用 `startConversation()`
  - `conversationManager != null` 时调用 `continueConversation()`
- [x] 13.3 `streamJob` 赋值使停止按钮生效（ConversationManager.kt）
  - 添加 `scope: CoroutineScope` 构造参数（传 `viewModelScope`）
  - `sendToLLM()` 中使用 `scope.launch(Dispatchers.IO)` 并赋值给 `streamJob`
  - `stop()` 调用 `streamJob?.cancel()` 实际取消当前请求
- [x] 13.4 LLMClient 可取消改进（LLMClient.kt）
  - `chat()` 中使用 `delay()` 替代 `Thread.sleep()` 实现可取消等待
  - 重试循环中添加 `ensureActive()` 检查协程取消

## 14. Tool 调用修复：字段推断 + 引号归一化 + SystemPrompt 优化

- [x] 14.1 新增 `normalizeJsonString()` 归一化函数（ResponseParser.kt）
  - 替换全角引号 `\u201C\u201D` → `"`，`\u2018\u2019` → `'`
  - 替换日式引号 `\u300C\u300D` → `"`
- [x] 14.2 重写 `extractToolName()` 增加字段推断回退（ResponseParser.kt）
  - 保留原有 3 种前缀模式匹配
  - 新增回退：`"text"` → reply，`"title"`/`"repeat_cycle"` → create_habit，`"type"`+`"prompt"` → ask_question
  - 对所有输入先做 `normalizeJsonString()` 归一化
- [x] 14.3 `extractArguments()` 增加入参归一化（ResponseParser.kt）
  - 调用 `normalizeJsonString()` 后再提取 JSON
  - 结果 `trimEnd()` 移除尾随 `)`、空格等
- [x] 14.4 `parseToolCallsFromJson()` 增加全文本归一化（ResponseParser.kt）
  - 调用 `normalizeJsonString(text)` 后再用 Gson 解析
- [x] 14.5 SystemPrompt 改用 `tool_name({...})` 前缀格式（SystemPrompt.kt）
  - 中/英文提示改为 `ask_question({...})`、`create_habit({...})`、`reply({...})`、`confirm({})`
  - 要求严格使用半角引号 `"` 而非全角 `"`
  - 明确说明 `confirm` 工具用于用户确认

## 15. SystemPrompt 精简：移除语言检测，统一英语

- [x] 15.1 删除 `ZH_PROMPT` 常量，只保留英语 prompt（SystemPrompt.kt）
- [x] 15.2 删除 `detectLanguage()` 方法，停止动态检测输入语言
- [x] 15.3 简化 `getSystemPrompt()` — 去掉 `language` 参数，直接返回单套 prompt
- [x] 15.4 prompt 新增 "Follow the user's language — reply in the same language the user writes in" 说明
- [x] 15.5 删除 `ConversationManager.currentLanguage` 字段（ConversationManager.kt）
- [x] 15.6 更新 `startConversation()` — 去掉 `detectLanguage()` 调用，`getSystemPrompt()` 无参调用

## 16. Confrim 工具注册 + 重复气泡修复

- [x] 16.1 新建 `ConfirmTool.kt` — 简易工具，`execute()` 返回 `ToolResult.Success(Unit)`
- [x] 16.2 `ToolRegistry.kt` 注册 `ConfirmTool()` — confirm 工具现在可被 `processResponse()` 的 `when` 分支执行
- [x] 16.3 修复重复 AI 气泡（AICreateHabitViewModel.kt）
  - `QuestionReceived`：删除 `addOrUpdateAIMessage("")`，只设 `_pendingQuestion`；问题通过 `PendingQuestionUI` 组件独立渲染
  - `ThinkingStarted`：删除 `addOrUpdateAIMessage("")`，思考内容附着在后续 `AIMessageReceived`