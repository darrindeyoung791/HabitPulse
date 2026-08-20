## ADDED Requirements

### Requirement: 查看 API key 需生物识别验证
系统 SHALL require strong biometric authentication (fingerprint/face) or device credential before revealing an encrypted API key in the UI, and SHALL hide it again automatically.

#### Scenario: 无生物识别与设备凭据
- **WHEN** the device has neither strong biometric nor device credential enrolled
- **THEN** the reveal action is disabled (or hidden), and the app does not offer to display the plaintext key

#### Scenario: 有可用验证方式时查看密钥
- **WHEN** user taps the reveal action and authenticates successfully with biometric or device credential
- **THEN** the app decrypts the reveal ciphertext and displays the plaintext apiKey in the edit field

#### Scenario: 验证失败或取消
- **WHEN** user taps the reveal action but authentication fails or is canceled
- **THEN** the app keeps the key hidden and shows no plaintext

#### Scenario: 展示后自动隐藏
- **WHEN** the plaintext key has been revealed
- **THEN** the app hides it again when the user leaves the edit screen (or the key loses focus / a timeout elapses), and each new reveal requires a fresh authentication

#### Scenario: 运行时使用不受查看验证影响
- **WHEN** the app sends LLM requests after a key has been encrypted
- **THEN** runtime decryption uses the runtime key and never prompts for biometric authentication
