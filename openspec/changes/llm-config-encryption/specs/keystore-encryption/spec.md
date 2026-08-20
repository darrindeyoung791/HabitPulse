## ADDED Requirements

### Requirement: Keystore 双密钥加解密基础设施
系统 SHALL provide a Keystore-backed encryption layer with two AES-GCM keys: a runtime key (usable without user authentication) and a reveal key (gated by strong biometric or device credential).

#### Scenario: 生成运行时密钥
- **WHEN** the encryption layer initializes
- **THEN** it generates/loads an AES-256 key in Android Keystore with GCM block mode and no user-authentication requirement

#### Scenario: 生成展示密钥
- **WHEN** the encryption layer initializes
- **THEN** it generates/loads an AES-256 key in Android Keystore with GCM block mode and user-authentication required (strong biometric OR device credential)

#### Scenario: 加密明文
- **WHEN** the layer encrypts an apiKey with the runtime key
- **THEN** it produces a Base64 string of (random 12-byte IV + GCM ciphertext), a fresh IV per encryption, and separately produces a reveal ciphertext with the reveal key

#### Scenario: 运行时解密
- **WHEN** the layer decrypts a runtime ciphertext with the runtime key
- **THEN** it returns the plaintext apiKey without prompting the user for authentication

#### Scenario: 展示解密
- **WHEN** the layer decrypts a reveal ciphertext with the reveal key
- **THEN** decryption succeeds only while a biometric/device-credential authentication is active (via BiometricPrompt CryptoObject); otherwise it throws an auth-required error

#### Scenario: 密钥不可用时降级
- **WHEN** Keystore key creation or decryption fails (e.g., credentials changed or device reset)
- **THEN** the layer surfaces a recoverable error so the UI can prompt the user to re-enter the apiKey, and does not crash

### Requirement: 存量明文迁移
系统 SHALL migrate legacy plaintext apiKey values to encrypted form so that storage never retains plaintext.

#### Scenario: 冷启动迁移
- **WHEN** the application starts and finds an AIConfig whose apiKey is still plaintext (no keyVersion marker)
- **THEN** it encrypts the key (runtime + reveal ciphertexts), updates the stored config with keyVersion, and persists the migrated value

#### Scenario: 惰性迁移兜底
- **WHEN** a plaintext apiKey is read outside cold-start migration (e.g., cold-start migration was skipped)
- **THEN** reading the config encrypts it on the fly before returning it, keeping the write path idempotent

#### Scenario: 迁移幂等
- **WHEN** migration runs against an already-encrypted config
- **THEN** it leaves the config unchanged (no double encryption, no data loss)
