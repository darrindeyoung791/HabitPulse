## Why

习惯创建页面的监督人邮箱和电话目前使用内联输入框（OutlinedTextField + 添加按钮），与上方的提醒时间 TimePicker 对话框风格完全不搭。编辑习惯时总是展示空输入框，体验糟糕。需要统一这三个控件的交互风格。

## What Changes

- 监督人邮箱改为对话框添加（复用联系人编辑对话框的 AlertDialog 样式）
- 监督人电话改为对话框添加（同上，添加 SMS 费用警告和短信说明）
- 提醒时间、监督人邮箱、监督人电话三个选项的添加按钮统一使用 TimePicker 的按钮样式（animated Surface, primaryContainer 色）
- 电话添加对话框明确说明号码用于短信提醒
- SupervisionContactSection 移除内联输入框，简化为添加按钮 + 可展开列表

## Capabilities

### New Capabilities
- `habit-creation-ui-unification`: 统一习惯创建页面的控件交互风格，三个输入选项使用一致的对话框+按钮模式

### Modified Capabilities
<!-- No spec-level behavior changes -->

## Impact

- `app/src/main/java/io/github/darrindeyoung791/habitpulse/ui/screens/HabitCreationScreen.kt` — 主要修改
- `app/src/main/res/values/strings.xml` — 新增字符串
- `app/src/main/res/values-en-rUS/strings.xml` — 新增字符串
- `app/src/main/res/values-zh-rHK/strings.xml` — 新增字符串
- `app/src/main/res/values-zh-rTW/strings.xml` — 新增字符串
