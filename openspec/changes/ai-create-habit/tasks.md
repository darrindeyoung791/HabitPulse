## 1. 配置层

- [ ] 1.1 在 UserPreferences 添加 LLM 配置字段（apiEndpoint, apiKey, modelName）
- [ ] 1.2 创建 AISettingsActivity 子页面（API 端点、密钥、模型名称输入框）
- [ ] 1.3 在 SettingsActivity 添加 AI 设置入口（NavigationLink 跳转到 AISettingsActivity）
- [ ] 1.4 实现 API 配置验证（检查必填项，测试连接）
- [ ] 1.5 实现模型下拉选择（支持 glm-4.7-flash 默认 + 自定义输入）

## 2. LLM 客户端

- [ ] 2.1 创建 ai/llm/LLMConfig.kt 配置数据类
- [ ] 2.2 创建 ai/llm/LLMRequest.kt 请求/响应数据类（ChatRequest, ChatResponse）
- [ ] 2.3 创建 ai/llm/LLMClient.kt HTTP 调用实现（Retrofit 或 URLConnection）
- [ ] 2.4 实现超时配置和自动重试机制
- [ ] 2.5 创建 ai/llm/ResponseParser.kt 代码块解析正则实现

## 3. 工具系统

- [ ] 3.1 创建 ai/tools/Tool.kt 工具接口定义
- [ ] 3.2 创建 ai/tools/ToolRegistry.kt 工具名 → 执行器映射
- [ ] 3.3 创建 ai/tools/CreateHabitTool.kt create_habit 执行逻辑（参数验证）
- [ ] 3.4 创建 ai/tools/QuestionTool.kt ask_question 执行逻辑（解析问题参数）
- [ ] 3.5 创建 ai/tools/ReplyTool.kt reply 执行逻辑（解析回复文本）

## 4. 对话管理

- [ ] 4.1 创建 ai/conversation/ConversationState.kt 状态数据类（含 pendingHabitCount, isHabitRelated）
- [ ] 4.2 创建 ai/conversation/ChatMessage.kt 消息类型（UserMessage, AIMessage, SystemMessage, QuestionMessage）
- [ ] 4.3 创建 ai/conversation/PartialHabit.kt 习惯中间状态
- [ ] 4.4 创建 ai/conversation/ConversationManager.kt 对话主循环实现（含任务完成检测）
- [ ] 4.5 创建 ai/conversation/Guard.kt 安全守卫（死循环检测、问题上限）
- [ ] 4.6 创建 ai/prompt/SystemPrompt.kt 中/英文 System Prompt 动态构建

## 5. 任务数量提取

- [ ] 5.1 创建 ai/conversation/HabitCountExtractor.kt 从用户输入中提取习惯数量（正则 + 关键词）
- [ ] 5.2 在 ConversationManager 初始化时调用数量提取器，设置 pendingHabitCount
- [ ] 5.3 支持中途追加习惯数量（用户说"再加一个"时累加 pendingHabitCount）
- [ ] 5.4 强制完成检测：AI 提前输出 confirm 时发送修正消息（AI 过早触发场景）

## 6. 兜底回复系统

- [ ] 6.1 创建 ai/conversation/HabitClassifier.kt 判断输入是否为习惯相关（关键词匹配）
- [ ] 6.2 创建 ai/conversation/FallbackReply.kt 非习惯相关输入的兜底回复逻辑
- [ ] 6.3 实现兜底 reply 消息生成（功能用法/打招呼/无关话题三种场景）
- [ ] 6.4 确保兜底 reply 不包含 ask_question 或 create_habit 工具调用

## 5. UI 层 - 导航集成

- [ ] 5.1 在 navigation/Route.kt 添加 AI 创建路由（ai-create-habit）
- [ ] 5.2 在 navigation/HabitPulseNavGraph.kt 添加 AICreateHabitScreen 导航
- [ ] 5.3 在 AICreateHabitScreen AppBar 右上角添加设置图标，导航到 AI 设置

## 6. UI 层 - 习惯创建预填

- [ ] 6.1 修改 HabitCreationScreen 接收 prefill 参数
- [ ] 6.2 实现 JSON 解析预填数据到表单字段
- [ ] 6.3 修改保存后返回逻辑（带更新数据回调）

## 7. UI 层 - HomeScreen FAB

- [ ] 7.1 修改 HomeScreen FAB 点击行为
- [ ] 7.2 创建新建习惯选择 BottomSheet/Dialog
- [ ] 7.3 实现导航到对应页面（手动 vs AI）

## 8. UI 层 - AI 创建界面

- [ ] 8.1 创建 ai/ui/AICreateHabitScreen.kt 主对话界面（集成 AGenUI SurfaceManager）
- [ ] 8.2 创建 ai/ui/AGENUISurface.kt AGenUI SurfaceManager 封装
- [ ] 8.3 创建 ai/ui/components/UserBubble.kt 用户气泡（32dp圆角，右对齐）
- [ ] 8.4 创建 ai/ui/components/AIBubble.kt AI气泡（直角，左对齐）
- [ ] 8.5 创建 ai/ui/components/AIWelcomeContent.kt 欢迎语界面
- [ ] 8.6 实现输入框动态高度（空3行，1-3行按实际，3-7行按实际，超7行显示7行+滚动）
- [ ] 8.7 实现发送/停止按钮切换逻辑
- [ ] 8.8 实现 AI 流式输出 + AGenUI Markdown 组件渲染
- [ ] 8.9 创建 ai/ui/components/ThinkingBlock.kt 思考块（可折叠动画）
- [ ] 8.10 创建 ai/ui/components/AIInputBox.kt 输入框组件（参考 gpt_mobile ChatInputBox）
- [ ] 8.11 实现清空对话按钮 + 确认对话框
- [ ] 8.12 实现退出确认对话框（返回按钮）
- [ ] 8.13 实现进入 AI 设置前确认对话框

## 9. UI 层 - 问题组件逻辑

- [ ] 9.1 实现时间选择答案提交
- [ ] 9.2 实现星期选择答案提交
- [ ] 9.3 实现选项选择答案提交
- [ ] 9.4 实现确认对话框（批量保存逻辑）
- [ ] 9.5 实现停止按钮和停止后状态展示

## 10. 字符串资源

- [ ] 10.1 在 values/strings.xml 添加 AI 创建相关字符串（中文）
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