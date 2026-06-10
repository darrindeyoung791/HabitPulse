## ADDED Requirements

### Requirement: 邮箱添加对话框
系统 SHALL 提供 AlertDialog 样式的对话框用于添加监督人邮箱。

#### Scenario: 打开邮箱添加对话框
- **WHEN** 用户点击"添加邮箱"按钮
- **THEN** 显示 AlertDialog，标题为"添加监督人邮箱"，包含说明文字和 OutlinedTextField（email 键盘类型）
- **AND** 对话框底部有"确定"和"取消"按钮

#### Scenario: 确认添加有效邮箱
- **WHEN** 用户在输入框中输入有效邮箱地址并点击"确定"
- **THEN** 邮箱被添加到列表，对话框关闭

#### Scenario: 确认添加无效邮箱
- **WHEN** 用户在输入框中输入无效邮箱地址并点击"确定"
- **THEN** 显示错误提示，对话框不关闭

#### Scenario: 取消添加
- **WHEN** 用户点击"取消"按钮或点击对话框外部
- **THEN** 对话框关闭，不添加邮箱

### Requirement: 电话添加对话框
系统 SHALL 提供 AlertDialog 样式的对话框用于添加监督人电话，包含 SMS 费用警告。

#### Scenario: 打开电话添加对话框
- **WHEN** 用户点击"添加电话"按钮
- **THEN** 显示 AlertDialog，标题为"添加监督人电话"
- **AND** 包含说明文字（明确说明号码用于短信提醒）
- **AND** 包含 SMS 费用警告横幅（黄色背景，警告图标）
- **AND** 包含区号选择器和电话输入框

#### Scenario: 确认添加有效电话
- **WHEN** 用户输入有效电话号码并点击"确定"
- **THEN** 电话被添加到列表（含区号前缀），对话框关闭

#### Scenario: 确认添加无效电话
- **WHEN** 用户输入无效电话号码并点击"确定"
- **THEN** 显示错误提示，对话框不关闭

### Requirement: 添加按钮样式统一
提醒时间、监督人邮箱、监督人电话的添加按钮 SHALL 使用统一的视觉样式。

#### Scenario: 按钮样式一致
- **WHEN** 用户查看习惯创建页面
- **THEN** 三个选项的添加按钮均使用 animated Surface 样式：primaryContainer 背景色、按压圆角动画（8dp→24dp）、高度 40dp、Add 图标 + 文字

### Requirement: 联系人列表展开/收起
监督人区域在展开后 SHALL 只显示已添加的联系人列表和添加按钮（无内联输入框）。

#### Scenario: 展开监督人区域
- **WHEN** 用户展开监督人邮箱或电话区域
- **THEN** 显示添加按钮（与 TimePicker 按钮样式一致）和已有联系人列表（可删除），没有内联输入框

#### Scenario: 展开监督人区域（无联系人）
- **WHEN** 用户展开监督人邮箱或电话区域且尚无联系人
- **THEN** 只显示添加按钮
