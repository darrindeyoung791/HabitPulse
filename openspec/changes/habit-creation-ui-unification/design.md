## Context

习惯创建页面的监督人添加区域目前使用 `SupervisionContactSection` composable，内部包含内联 `OutlinedTextField` + 尾部添加图标按钮。这与上方的提醒时间区域（使用 `TimePickerDialog` + 动画按钮）风格不一致。联系人界面的编辑对话框使用 `AlertDialog` 样式，提供了现成的参考。

## Goals / Non-Goals

**Goals:**
- 监督人邮箱/电话改为 `AlertDialog` 样式对话框添加（与联系人编辑对话框一致）
- 三个选项（提醒时间/邮箱/电话）添加按钮样式统一
- SMS 费用警告和短信说明在电话对话框中展示
- 简化 `SupervisionContactSection`，移除内联输入逻辑

**Non-Goals:**
- 不改变提醒时间现有 TimePickerDialog 行为
- 不修改底层数据模型（Habit entity, DAO, Repository）
- 不改变保存/编辑流程
- 不修改 ContactsScreen

## Decisions

1. **对话框样式：标准 AlertDialog** — 复用 ContactsScreen 中编辑联系人的 `AlertDialog` 样式（Material 3 标准对话框），而非自定义 `Dialog`。这提供了简洁统一的体验，且无需自定义 Surface 配置。

2. **一次添加一个** — 对话框每次只添加一个邮箱或电话，确认后关闭。这牺牲了批量添加的效率，但避免了当前设计"内联输入框始终显示空框"的问题，且与 TimePicker 交互一致。

3. **添加按钮样式统一为 animated Surface** — 当前 TimePicker 的"添加提醒时间"按钮使用 `animateDpAsState` 做按压圆角动画 + `primaryContainer` 背景色。邮箱和电话的添加按钮将复用此样式代码。

4. **电话对话框内嵌 SMS 警告** — 将 SMS 费用警告横幅移入电话添加对话框内，替换当前内联位置的警告条。对话框内仍使用相同的黄色警告样式。

5. **SupervisionContactSection 简化** — 移除外部的 `inputValue`/`onInputChange`/`onAdd`/`isValid` 等参数。改为添加 `onAddClick` 回调，点击按钮时由父级管理对话框显示。

## Risks / Trade-offs

1. **批量添加不便** — 添加多个邮箱/电话需要多次打开对话框。但考虑到大多数用户通常只有 1-2 个监督人，且编辑时不再显示空输入框，收益大于成本。

2. **状态管理变化** — 移除了 `emailInput`/`phoneInput`/`countryCode` 等局部状态变量（移至对话框内部），减少了 HabitCreationScreen 中的状态量。
