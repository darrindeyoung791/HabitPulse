## Context

AI 创建习惯界面已实现基础对话功能，但重试/停止按钮的行为不符合用户预期：

1. **重试按钮**：当前仅当 AI 消息输出完毕 (`isStreaming=false`) 且无加载状态时显示。用户手动停止或错误发生时，AI 消息的 `isStreaming` 标记未正确复位，导致重试按钮不显示。
2. **停止按钮**：`ConversationManager.stop()` 在无习惯卡片时不发送任何事件，`isLoading` 无限期保持 `true`。有习惯卡片时通过 `ConfirmationRequested` 事件触发自动保存并继续对话，违背用户停止意图。
3. **习惯卡片**：「完成」按钮为一次性点击，创建后无法撤销。

## Goals / Non-Goals

**Goals:**
- 修复重试按钮在手动停止/错误时的可见性
- 重试时清除用户最后一条消息并回填输入框
- 修复停止按钮在无习惯时的卡死问题
- 停止后不自动继续对话，由用户主动操作
- 习惯卡片完成/撤销双向切换

**Non-Goals:**
- 不改动 AI 对话引擎的核心架构（`ConversationManager`）
- 不改动 LLM 客户端网络层
- 不改动导航或路由逻辑

## Decisions

### 1. 重试按钮显示条件

**方案**：将 `showRetry` 条件从依赖 `message.isStreaming` 改为依赖系统整体状态。
- 重试按钮显示条件：`isLastMessage && !uiState.isLoading && pendingQuestion == null`
- 移除 `!message.isStreaming` 条件
- 同时确保 `ConversationManager.stop()` 后，最后一条 AI 消息的 `isStreaming` 被复位为 `false`

### 2. 停止后状态复位

**方案**：`ConversationManager.stop()` 移除习惯存在时自动发射 `ConfirmationRequested` 的逻辑。改为：
- 无论有无习惯，停止后发射一个通用 `Stopped` 事件
- ViewModel 收到 `Stopped` 事件后：`isLoading = false`，最后一条 AI 消息 `isStreaming = false`
- 用户可自由选择重试/发新消息/清空

### 3. Retry 清除用户消息

**方案**：`retryLastTurn()` 中，找到最后一条用户消息的位置，移除从该位置开始的所有消息，并将该用户消息的文本存入 `inputText`。

### 4. 完成↔撤销切换

**方案**：`HabitCreatedCard` 的按钮为独立状态机：
- 初始状态：显示「完成」按钮
- 点击「完成」：调用 `createHabit()` 保存到 DB，按钮变为「撤销创建」
- 点击「撤销创建」：调用 `deleteHabitByTempId()` 从 DB 删除，按钮变回「完成」
- 状态由 ViewModel 管理：新增 `_completedTempIds` 与现有 `_confirmedTempIds` 区分

## Risks / Trade-offs

- **风险**：移除 `ConversationManager.stop()` 的 `ConfirmationRequested` 自动发射，可能影响其他依赖该事件的流程。**缓解**：目前 `ConfirmationRequested` 仅由 `stop()` 和自动流程触发，修改后统一由 ViewModel 的 `stopGeneration()` 方法管理状态。
- **风险**：重试清除用户消息可能导致用户意外丢失长输入。**缓解**：文本自动回填到输入框，用户可修改后重发。
- **风险**：完成/撤销涉及数据库操作，存在并发冲突。**缓解**：所有 DB 操作通过 `viewModelScope` 顺序执行。
