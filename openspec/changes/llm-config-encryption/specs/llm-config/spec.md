## MODIFIED Requirements

### Requirement: 用户可以配置 LLM API 密钥
用户 SHALL be able to configure and store an API key for LLM authentication. The stored key SHALL be encrypted at rest using Android Keystore, SHALL be masked in the UI, and SHALL be decryptable without user authentication for runtime requests.

#### Scenario: 保存 API 密钥
- **WHEN** user enters an API key and saves
- **THEN** system encrypts the key with the runtime Keystore key, stores the runtime ciphertext (and a biometric-gated reveal ciphertext) in UserPreferences, and displays masked in UI

#### Scenario: 清除 API 密钥
- **WHEN** user clears the API key field and saves
- **THEN** system removes the stored ciphertexts from UserPreferences

#### Scenario: 使用已配置的密钥发起请求
- **WHEN** AI creation uses the active config to send an LLM request
- **THEN** system decrypts the runtime ciphertext in memory (no user authentication prompt) and includes the plaintext key in the Authorization header

### Requirement: 系统验证 API 配置可用性
When the user attempts to use AI creation feature, the system SHALL verify that API endpoint and key are configured.

#### Scenario: 未配置 API 时使用 AI 功能
- **WHEN** user tries to open AI Create Habit screen without configuring API endpoint or key
- **THEN** system displays a dialog prompting user to configure API in Settings

#### Scenario: 配置不完整时使用 AI 功能
- **WHEN** user tries to open AI Create Habit screen with only endpoint configured (no key)
- **THEN** system displays a dialog prompting user to complete API configuration

#### Scenario: 已配置但迁移前未加密的密钥
- **WHEN** a legacy plaintext apiKey is read from storage
- **THEN** system lazily encrypts it (runtime + reveal ciphertexts) before use, so the stored value is never left plaintext
