## 1. 字符串资源

- [x] 1.1 更新 values/strings.xml（中文）添加对话框相关字符串
- [x] 1.2 更新 values-en-rUS/strings.xml（英文）添加对话框相关字符串
- [x] 1.3 更新 values-zh-rHK/strings.xml（繁体香港）添加对话框相关字符串
- [x] 1.4 更新 values-zh-rTW/strings.xml（繁体台湾）添加对话框相关字符串

## 2. 对话框组件

- [x] 2.1 创建 AddEmailDialog composable（AlertDialog 样式，邮箱输入，验证错误）
- [x] 2.2 创建 AddPhoneDialog composable（AlertDialog 样式，区号选择器，电话输入，SMS 警告，验证错误）

## 3. 创建 AddSection 可复用组件

- [x] 3.1 创建 AddSection composable 统一三个区域的视觉样式
- [x] 3.2 标题左侧 + 添加按钮右侧，背景透明，动画按钮样式

## 4. 重构页面结构

- [x] 4.1 移除"监督人"包裹层，邮箱/电话各自独立为与提醒时间同级的区域
- [x] 4.2 三区均使用 AddSection（提醒时间/邮箱/电话）
- [x] 4.3 删除旧的 SupervisionContactSection

## 5. 细节优化

- [x] 5.1 对话框自动聚焦输入框
- [x] 5.2 电话输入字体大小与区号一致（bodyLarge）
- [x] 5.3 电话区域 SMS 说明文字始终可见（展开前）

## 6. 验证

- [x] 6.1 运行编译检查（BUILD SUCCESSFUL）
- [x] 6.2 运行全部单元测试（BUILD SUCCESSFUL）
- [x] 6.3 运行完整 debug 构建（BUILD SUCCESSFUL）
