## 1. Dependency Updates

- [x] 1.1 Bump `composeBom` to `2026.06.01` in `gradle/libs.versions.toml`
- [x] 1.2 Add `material3 = "1.5.0-alpha24"` version and pin `androidx-compose-material3` to it in `gradle/libs.versions.toml`
- [x] 1.3 Update `app/build.gradle.kts` so all `material3` implementations use the pinned alpha version (single reference via `libs`)
- [x] 1.4 Bump `coreKtx` to `1.19.0` and `lifecycleRuntimeKtx` to `2.11.0` in `gradle/libs.versions.toml`
- [x] 1.5 Bump hardcoded `lifecycle-viewmodel-compose` / `lifecycle-runtime-compose` to `2.11.0` in `app/build.gradle.kts`
- [x] 1.6 Run clean build + unit tests to verify dependency resolution with the alpha pin

## 2. Shared List-Item Components

- [x] 2.1 Create `ui/screens/settings/components/` package and `SettingsSegmentedItem` (clickable `SegmentedListItem` with `segmentedShapes(index, count)`, leading icon, supporting text, trailing arrow)
- [x] 2.2 Add `SettingsSegmentedSwitch` (toggleable `SegmentedListItem(checked, onCheckedChange)` with `Switch(onCheckedChange = null)` trailing, whole-row toggle binding)
- [x] 2.3 Add `SettingsSegmentedGroup` (renders items with `SegmentedGap` spacing, correct `index`/`count`)
- [x] 2.4 Verify press-state shape morph, grouped edge/interior rounding, and disabled "即将推出" state in previews

## 3. New Settings Activity + Navigation Scaffold

- [x] 3.1 Create `NewSettingsActivity` (root package) with `enableEdgeToEdge`, `HabitPulseTheme`, internal `NavHost`
- [x] 3.2 Define settings navigation routes (first screen, ai, ai/provider, notifications, notifications/reminder, notifications/template, general, general/storage, about, about/about)
- [x] 3.3 Register `NewSettingsActivity` in `AndroidManifest.xml` (theme, configChanges, predictive back, resizeable)
- [x] 3.4 Wire predictive back and edge-to-edge handling consistent with other activities (NavHost pop handles predictive back, root route calls `finish()`)

## 4. First Screen (Category Hub)

- [x] 4.1 Build first-screen composable: TopAppBar with back arrow + help icon (WebView HELP_URL)
- [x] 4.2 Render categories as single-item groups in order: AI 配置, 连接与同步, 通知, 通用, 关于, 帮助和反馈
- [x] 4.3 Wire navigation for AI 配置 / 通知 / 通用 / 关于 to their sub-pages
- [x] 4.4 Make 连接与同步 a disabled placeholder ("即将推出")
- [x] 4.5 Make 帮助和反馈 a leaf entry opening the help WebView directly

## 5. Sub-Pages

- [x] 5.1 AI 配置 page: 配置提供商 (→ provider page) + 记忆 (disabled placeholder)
- [x] 5.2 配置提供商 page: re-host endpoint/key/model fields, test connection, streaming response as `SettingsSegmentedSwitch`
- [x] 5.3 通知 page: 保持后台运行 (switch or authorize row with permission + limited-mode flows), 习惯提醒 (→ reminders), 通知模板 (→ template)
- [x] 5.4 习惯提醒 page: re-host reminder settings (master switch, DND switch + slider, status rows, test buttons) with shared components
- [x] 5.5 通知模板 page: template text field with save + reset (moved from legacy dialog)
- [x] 5.6 通用 page: 空间清理 (→ storage) + 界面与显示 (disabled placeholder, renamed from 视觉)
- [x] 5.7 空间清理 page: clear-cache + deep-clean rows with confirmation dialogs (moved from legacy)
- [x] 5.8 关于 page: 关于 (→ detail), 查看应用信息 (system intent), 查看GitHub (WebView)
- [x] 5.9 关于 detail page: privacy notice, app version, developer, open-source licenses

## 6. Legacy Settings Entry

- [x] 6.1 Add a new-settings entry `SettingsListItem` at the very bottom of `SettingsScreen()` in `SettingsActivity.kt` that launches `NewSettingsActivity`

## 7. i18n

- [x] 7.1 Add new string resources (category/sub-page titles, "即将推出", new-settings entry, help titles) to `values/strings.xml`
- [x] 7.2 Add translations to `values-en-rUS/strings.xml`
- [x] 7.3 Add translations to `values-zh-rCN/strings.xml`
- [x] 7.4 Add translations to `values-zh-rHK/strings.xml`
- [x] 7.5 Add translations to `values-zh-rTW/strings.xml`

## 8. Verification

- [x] 8.1 Run `./gradlew test` (unit tests) and fix regressions
- [x] 8.2 Run lint/build (`./gradlew assembleDebug`) and fix compile/lint errors
- [ ] 8.3 Manual device pass: light/dark theme, RTL, TalkBack, phone/tablet, portrait/landscape
- [ ] 8.4 Verify switch-row press ripple binds to the switch across all pages (bug fix check)
