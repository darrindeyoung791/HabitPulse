## 1. ConversationManager — 停止后不自动继续

- [x] 1.1 移除 `ConversationManager.stop()` 中根据习惯存在与否发射 `ConfirmationRequested` 的逻辑
- [x] 1.2 `stop()` 改为发射通用 `Stopped` 事件（无附带数据）
- [x] 1.3 在 `ConversationEvent` sealed class 中新增 `Stopped` 事件类型

## 2. ViewModel — 停止/重试/撤销状态管理

- [x] 2.1 `observeConversation()` 处理 `Stopped` 事件：设 `isLoading = false`，最后一条 AI 消息 `isStreaming = false`
- [x] 2.2 重构 `retryLastTurn()`：找到最后一条用户消息位置，移除从该位置开始的所有消息，将该消息文本写入 `inputText`
- [x] 2.3 新增 `_completedTempIds` 状态追踪已完成的习惯（区别于 `_confirmedTempIds`）
- [x] 2.4 新增 `toggleHabitCompleted(tempId)` 方法：已完成→撤销删除，未完成→创建保存
- [x] 2.5 新增 `completedTempIds` 公开 StateFlow 供 UI 层观察

## 3. Screen — 重试按钮可见性修复

- [x] 3.1 修改 `showRetry` 条件：移除 `!message.isStreaming` 依赖
- [x] 3.2 确保 `showRetry` 在停止/错误/自然结束时均正确显示
- [x] 3.3 UI 层重试逻辑不再清除 `_pendingQuestion`（已由 ViewModel 处理）

## 4. Screen — 完成↔撤销按钮切换

- [x] 4.1 修改 `HabitCreatedCard`：新增 `isCompleted` 参数
- [x] 4.2 「完成」按钮点击后调用 `toggleHabitCompleted()`，按钮变为「撤销创建」
- [x] 4.3 「撤销创建」按钮点击后删除习惯，按钮变回「完成」
- [x] 4.4 完成状态卡片使用不同的视觉样式（如低饱和度颜色）

## 5. 字符串资源

- [x] 5.1 `values/strings.xml` 添加「撤销创建」字符串
- [x] 5.2 `values-en-rUS/strings.xml` 添加英文翻译 "Undo Create"
- [x] 5.3 `values-zh-rHK/strings.xml` 添加繁体香港翻译
- [x] 5.4 `values-zh-rTW/strings.xml` 添加繁体台湾翻译

## 6. 验证

- [x] 6.1 确认停止按钮在各种场景下均能正确停止生成
- [x] 6.2 确认重试按钮在停止/错误/自然结束时均可见
- [x] 6.3 确认重试后用户消息被清除并回填到输入框
- [x] 6.4 确认完成↔撤销双向切换功能正确
- [x] 6.5 确认无习惯时停止不会卡死
- [x] 6.6 确认编译通过
