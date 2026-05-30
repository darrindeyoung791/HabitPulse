## ADDED Requirements

### Requirement: AI 设置入口显示在设置页面
设置主页面 SHALL 显示 AI 设置的入口，点击后跳转到 AI 设置子页面。

#### Scenario: 设置页面显示 AI 设置入口
- **WHEN** user opens Settings screen
- **THEN** system displays an item labeled "AI 设置" (or "AI Settings" in English) that is clickable

#### Scenario: 点击 AI 设置入口导航到子页面
- **WHEN** user taps on the AI Settings item
- **THEN** system navigates to AISettingsActivity (or composable screen)

### Requirement: AI 设置页面显示 API 端点配置
AI 设置页面 SHALL provide a text field for users to configure the API endpoint URL.

#### Scenario: 显示 API 端点输入框
- **WHEN** user opens AI Settings screen
- **THEN** system displays a text field labeled "API 端点" (or "API Endpoint")
- **AND** placeholder shows default value "https://open.bigmodel.cn/api/paas/v4/"

#### Scenario: 保存 API 端点
- **WHEN** user enters a valid endpoint URL and saves
- **THEN** system stores the URL in UserPreferences

### Requirement: AI 设置页面显示 API 密钥配置
AI 设置页面 SHALL provide a secure text field for users to configure the API key.

#### Scenario: 显示 API 密钥输入框
- **WHEN** user opens AI Settings screen
- **THEN** system displays a text field labeled "API 密钥" (or "API Key")
- **AND** the input is masked (password field)

#### Scenario: 保存 API 密钥
- **WHEN** user enters an API key and saves
- **THEN** system stores the key securely in UserPreferences

#### Scenario: 清除 API 密钥
- **WHEN** user clears the API key field and saves
- **THEN** system removes the stored key from UserPreferences

### Requirement: AI 设置页面显示模型选择
AI 设置页面 SHALL provide a way for users to select or enter the model name, with a default recommended model.

#### Scenario: 显示模型选择器
- **WHEN** user opens AI Settings screen
- **THEN** system displays a dropdown/autocomplete field labeled "模型" (or "Model")
- **AND** default selection shows "glm-4.7-flash"

#### Scenario: 选择预设模型
- **WHEN** user selects a model from the dropdown (e.g., "glm-4.7-flash")
- **THEN** system stores the model name in UserPreferences

#### Scenario: 输入自定义模型
- **WHEN** user types a custom model name not in the preset list
- **THEN** system allows custom input and stores it as the selected model

### Requirement: AI 设置页面显示连接测试功能
AI 设置页面 SHALL provide a button to test the API connection with current configuration.

#### Scenario: 测试连接按钮显示
- **WHEN** user opens AI Settings screen
- **THEN** system displays a "测试连接" (or "Test Connection") button

#### Scenario: 测试连接成功
- **WHEN** user taps "测试连接" with valid API configuration
- **THEN** system makes a test API call
- **AND** displays a success message "连接成功" (or "Connection successful")

#### Scenario: 测试连接失败
- **WHEN** user taps "测试连接" with invalid configuration
- **THEN** system displays an error message indicating the failure reason

### Requirement: AI 设置页面实时保存
AI 设置页面 SHALL automatically save settings as the user types, with a toast notification when returning.

#### Scenario: 实时保存
- **WHEN** user enters or modifies any field (API endpoint, API key, or model)
- **THEN** system automatically saves the value to UserPreferences immediately

#### Scenario: 返回时显示保存成功提示
- **WHEN** user navigates back from AI Settings screen
- **THEN** system displays a toast message "已保存" (or "Saved")

### Requirement: 输入框支持快速清除
When an input field has content, it SHALL display a clear button to quickly empty the field.

#### Scenario: 显示清除按钮
- **WHEN** an input field has content
- **THEN** system displays a clear (X) icon button on the right side of the field

#### Scenario: 清除内容
- **WHEN** user taps the clear button
- **THEN** system clears the field content and removes the clear button

### Requirement: API 密钥支持显示/隐藏切换
The API key field SHALL have a toggle to show or hide the password.

#### Scenario: 默认隐藏密码
- **WHEN** user opens AI Settings screen
- **THEN** API key field displays masked characters (••••••)

#### Scenario: 切换显示密码
- **WHEN** user taps the visibility toggle icon
- **THEN** field shows actual characters; tapping again hides them

### Requirement: 输入框有最大长度限制
Each input field SHALL enforce a maximum character limit.

#### Scenario: API 端点最大长度
- **WHEN** user enters more than 200 characters in API endpoint field
- **THEN** system prevents further input at 200 characters

#### Scenario: API 密钥最大长度
- **WHEN** user enters more than 100 characters in API key field
- **THEN** system prevents further input at 100 characters

#### Scenario: 模型名称最大长度
- **WHEN** user enters more than 50 characters in model name field
- **THEN** system prevents further input at 50 characters