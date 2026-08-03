## Why

The current settings screen (`SettingsActivity.kt`) is built on custom `Surface`+`Row` list-item wrappers that don't follow Material Design 3 Expressive specifications: there is no press-state corner-radius morph, no grouped/segmented rounding, dividers are used for separation, and switch rows toggle via a separate `onClick` on the Surface instead of binding the whole row to the switch's pressed state (no ripple feedback on the switch, wrong semantics). The new material3 `1.5.0-alpha24` provides native expressive `ListItem` / `SegmentedListItem` APIs that solve all of these. We also want to restructure settings by category with a refreshed hierarchy.

## What Changes

- **Dependency update**: Bump Compose BOM to the latest stable (`2026.06.01`) and pin `androidx.compose.material3:material3` to `1.5.0-alpha24` (explicit version overrides the BOM, since stable BOM maps material3 to `1.4.0`).
- **Shared settings list-item component library** built on native MD3 expressive `ListItem` / `SegmentedListItem`:
  - Clickable list items with `ListItemShapes` (press-state morph: corner radius expands on press).
  - Grouped list items via `SegmentedListItem` + `ListItemDefaults.segmentedShapes(index, count)` so adjacent items have smaller corner radius than edge items.
  - No `HorizontalDivider`; grouping + spacing replaces dividers.
  - Switch rows use the native toggleable `ListItem(checked, onCheckedChange)` overload with `Switch(checked, onCheckedChange = null)` as trailing content — the whole row is the toggle target, fixing the current "row toggles but switch gets no pressed state" bug.
  - Consistent styling shared across all settings pages.
- **New settings hierarchy** in a new Activity with internal Compose navigation:
  - First screen = category list, no fine-grained switches on it.
  - Categories: AI 配置, 连接与同步, 通知, 通用, 关于, plus a first-screen 帮助和反馈 entry (NOT a sub-page) that opens the help WebView; the appbar help icon is retained.
  - Sub-pages: AI 配置 (配置提供商 + 记忆 placeholder), 连接与同步 (empty placeholder), 通知 (保持后台运行 switch + 习惯提醒 + 通知模板), 通用 (空间清理 + 界面与显示 placeholder), 关于 (关于 + 支持 HabitPulse switch + 查看应用信息 + 查看GitHub).
  - Placeholder entries (记忆, 连接与同步, 界面与显示) render as disabled items with a "即将推出" supporting text; tapping does nothing.
- **Legacy settings preserved**: `SettingsActivity.kt` stays as-is; only a new entry item is added at the very bottom that launches the new settings.
- Existing leaf screens (AI provider config, habit reminders) are re-hosted inside the new settings navigation and restyled with the shared list-item components.

## Capabilities

### New Capabilities
- `settings-list-items`: Shared MD3 expressive list-item components used across all settings pages — clickable items, toggleable switch items, segmented groups with edge/adjacent rounding, no dividers, consistent styling, and correct switch press-state binding.
- `settings-hierarchy`: The new settings first-screen category hub with internal navigation to sub-pages, placeholder handling (disabled "即将推出" entries), the first-screen 帮助和反馈 entry, and the entry point from the legacy settings screen.

### Modified Capabilities
<!-- No existing spec requirements change; the old settings screen is kept untouched except for one new entry item. -->

## Impact

- **Code**:
  - `gradle/libs.versions.toml` + `app/build.gradle.kts` — dependency version changes.
  - `SettingsActivity.kt` — add one entry item at the bottom (legacy screen otherwise untouched).
  - New: settings Activity (manifest registration), internal navigation + first-screen + sub-page composables, shared list-item component library under `ui/components/` (or `ui/settings/`).
  - Re-host/restyle: AI provider config page (from `AISettingsScreen.kt`) and habit-reminders page (from `ReminderSettingsScreen.kt`) inside the new navigation; replace their custom `SettingsListItem`/`SettingsSwitchItem` usage.
- **Dependencies**: `androidx.compose:compose-bom` → `2026.06.01`; `androidx.compose.material3:material3` → `1.5.0-alpha24` (pinned); `androidx.core:core-ktx` → `1.19.0`; `androidx.lifecycle` (`runtime-ktx`, `runtime-compose`, `viewmodel-compose`) → `2.11.0`.
- **i18n**: New string resources in all 4 locale files (`values`, `values-en-rUS`, `values-zh-rCN`, `values-zh-rHK`, `values-zh-rTW`) for new category/section titles and "即将推出" labels.
- **Manifest**: New Activity registered with `configChanges`, edge-to-edge, predictive back, resizeable.
- **Out of scope**: legacy settings removal, reminder scheduling via AlarmManager, LAN device connection, memory/visual features (placeholders only).
