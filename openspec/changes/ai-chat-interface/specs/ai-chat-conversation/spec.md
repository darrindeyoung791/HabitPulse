## ADDED Requirements

### Requirement: 原生 function-calling 请求
会话引擎 SHALL 以原生 function-calling 发起请求：请求体携带 `tools: List<ToolDef>`（name / description / parameters JSON Schema）与 `tool_choice: "auto"`。响应 SHALL 从 `tool_calls` 读取工具调用，废弃 ```json 代码块文本协议。

#### Scenario: 请求带工具定义
- **WHEN** 引擎发送首轮请求
- **THEN** 请求体包含注册的全部工具定义且 `tool_choice` 为 "auto"

#### Scenario: 纯文本回复
- **WHEN** 响应无 `tool_calls`
- **THEN** 引擎将文本渲染为普通助手气泡

### Requirement: 流式工具调用累积
流式响应 SHALL 累积 `delta.tool_calls` 分片（id、function.name、function.arguments），在 `Done` 时若存在完整 tool_call SHALL 触发工具执行。流式 `Done` 时内容为空或无法解析出内容/tool_calls 时，引擎 SHALL 回退一次非流式请求而非静默失败。

#### Scenario: 流式工具调用
- **WHEN** 流式响应分片返回工具调用参数
- **THEN** 引擎累积全部参数分片，完成后按单轮循环执行

#### Scenario: 流式空内容降级
- **WHEN** 流式 `Done` 时内容为空
- **THEN** 引擎以同一消息序列回退一次非流式请求

### Requirement: 多轮工具链与暂停点
引擎 SHALL 循环执行 assistant 消息中的所有 tool_calls，工具结果以 `role=tool` 消息回灌，直到无工具或命中暂停点。命中暂停点的工具：`ask_question`、`create_habit`、`delete_habit`。`ToolResult.Error` SHALL 累计重试计数，达到上限后触发错误事件并停止。

#### Scenario: 连续工具链
- **WHEN** 一个工具的结果触发后续工具调用
- **THEN** 引擎继续执行后续工具直到无工具或暂停

#### Scenario: 工具错误重试上限
- **WHEN** 同一轮内工具连续返回错误且达到重试上限
- **THEN** 引擎触发错误事件并停止该轮

#### Scenario: 提问暂停
- **WHEN** 引擎执行 `ask_question`
- **THEN** 引擎暂停等待用户回答，收到「继续」信号后恢复

### Requirement: 深度思考捕获
当配置启用思考（`thinkingEnabled`）时，流式响应 SHALL 捕获 `delta.reasoning_content` 并触发思考事件；思考文本到达后正文开始。非流式响应 SHALL 从 `message.reasoningContent` 读取。思考关闭时 SHALL 不渲染思考块。

#### Scenario: 流式思考
- **WHEN** 配置启用思考且流式返回推理分片
- **THEN** 引擎按分片发出思考更新事件，正文随后开始

#### Scenario: 关闭思考
- **WHEN** 配置关闭思考
- **THEN** 不出现思考内容与思考块

### Requirement: 纠错守卫扩展
会话守卫 SHALL 在 `update_setting` / `open_settings_page` 返回 `ToolResult.Error`（未知 key / 非法 page / 越权改 AI 配置）时累计次数，连续 ≥ 3 次 SHALL 触发 `GuardBlocked` 停止并提示用户检查后重试。`update_setting` 成功 SHALL 重置计数。

#### Scenario: 设置连续报错
- **WHEN** AI 连续 3 次尝试非法设置操作
- **THEN** 引擎停止该轮并提示用户

### Requirement: Token 精确统计
会话 SHALL 仅累计提供商返回的 `usage`（promptTokens / completionTokens / totalTokens / completedCalls）。流式从 `[DONE]` 前末 chunk 的 usage 捕获；非流式从 `LLMResult.Success.usage` 透传。本次无 `usage` SHALL 不计数。清除对话或切换提供商 SHALL 重置统计。顶栏 SHALL 显示累计 token，无任何计数时显示「—」，不得显示估算值。

#### Scenario: 流式累计
- **WHEN** 一次流式调用在 `Done` 前携带 usage
- **THEN** 会话累计该 usage 并在顶栏更新计数

#### Scenario: 无 usage
- **WHEN** 端点不返回 usage
- **THEN** 该次调用不计数，顶栏保持「—」

#### Scenario: 重置
- **WHEN** 用户清除对话或切换提供商
- **THEN** 会话 Token 统计清零