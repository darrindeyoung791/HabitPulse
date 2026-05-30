## ADDED Requirements

### Requirement: 用户可以配置 LLM API 端点
用户 SHALL be able to configure a custom API endpoint for LLM communication in the Settings screen.

#### Scenario: 配置有效的 API 端点
- **WHEN** user enters a valid URL in the API endpoint field and saves
- **THEN** system stores the endpoint URL in UserPreferences and displays success message

#### Scenario: 配置无效的 API 端点
- **WHEN** user enters an invalid URL format in the API endpoint field
- **THEN** system displays validation error indicating the format is incorrect

### Requirement: 用户可以配置 LLM API 密钥
用户 SHALL be able to configure and store an API key for LLM authentication. The stored key SHALL be masked in the UI.

#### Scenario: 保存 API 密钥
- **WHEN** user enters an API key and saves
- **THEN** system stores the key securely in UserPreferences (encrypted at rest) and displays masked in UI

#### Scenario: 清除 API 密钥
- **WHEN** user clears the API key field and saves
- **THEN** system removes the stored key from UserPreferences

### Requirement: 用户可以配置模型名称
用户 SHALL be able to specify which LLM model to use for habit creation.

#### Scenario: 选择默认模型
- **WHEN** user opens the settings and model field is empty
- **THEN** system displays default value "glm-4.7-flash" as placeholder

#### Scenario: 自定义模型名称
- **WHEN** user enters a custom model name and saves
- **THEN** system stores the model name and uses it in subsequent API requests

### Requirement: 系统验证 API 配置可用性
When the user attempts to use AI creation feature, the system SHALL verify that API endpoint and key are configured.

#### Scenario: 未配置 API 时使用 AI 功能
- **WHEN** user tries to open AI Create Habit screen without configuring API endpoint or key
- **THEN** system displays a dialog prompting user to configure API in Settings

#### Scenario: 配置不完整时使用 AI 功能
- **WHEN** user tries to open AI Create Habit screen with only endpoint configured (no key)
- **THEN** system displays a dialog prompting user to complete API configuration