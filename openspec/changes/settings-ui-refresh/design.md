## Context

The current settings (`SettingsActivity.kt`) is a single legacy screen built on custom `Surface`+`Row` wrappers (`SettingsListItem`, `SettingsSwitchItem`) that don't follow MD3 Expressive specs: no press-state shape morph, no segmented/grouped rounding, uses `HorizontalDivider`, and switch rows toggle via an extra `onClick` on the Surface while the `Switch` never receives a pressed interaction.

material3 `1.5.0-alpha24` (chosen over `alpha23`; released 2026-07-15) graduates the expressive list-item APIs:
- `ListItem(onClick, ...)`, `ListItem(checked, onCheckedChange, ...)`, `ListItem(selected, onClick, ...)` with `ListItemShapes` (per-state shapes incl. `pressedShape`).
- `SegmentedListItem` with `ListItemDefaults.segmentedShapes(index, count)` — edge items get full corner radius, adjacent items get reduced radius, separated by `ListItemDefaults.SegmentedGap`. Non-expressive `ListItem` is deprecated.

Current sub-pages live in separate Activities (`AISettingsActivity`, `ReminderSettingsActivity`) and reuse the same custom list-item pattern.

## Goals / Non-Goals

**Goals:**
- Update Compose BOM to latest stable (`2026.06.01`) and pin `material3` to `1.5.0-alpha24` (explicit version overrides BOM's `1.4.0`).
- Build a shared settings list-item component library on native expressive `ListItem`/`SegmentedListItem`, used uniformly across all new settings pages: press-state shape morph, grouped rounding, no dividers, consistent styling.
- Fix switch-row behavior: whole row is the toggleable surface (ripple on press) with the `Switch` as a non-interactive trailing visual.
- New settings hierarchy: first screen = categories only (no fine-grained switches), sub-pages hold switches/actions. New Activity with internal Navigation Compose.
- Legacy settings preserved as-is; add one entry item at its bottom that opens the new settings.

**Non-Goals:**
- Removing the legacy settings (future work after this change is validated).
- Reminder scheduling via AlarmManager, LAN device sync, AI memory, visual/display settings — placeholders only.
- Restyling the welcome/onboarding screens (they reuse some strings but are not settings pages).

## Decisions

### 1. Dependency strategy: stable BOM + pinned material3 alpha
- Bump `composeBom` to `2026.06.01` (maps ui/foundation to `1.11.x`, material3 to `1.4.0`).
- Pin `androidx.compose.material3:material3` to `1.5.0-alpha24` via an explicit version in `libs.versions.toml`; explicit version wins over BOM.
- **Alternatives considered**: `compose-bom-alpha` (manages all alpha/beta compose libs together). Rejected — user wants the stable BOM for the rest of the stack and only material3 pinned to alpha.
- **Risk**: material3 alpha may transitively require newer foundation/ui than the stable BOM. Gradle resolves to the highest; verify with a clean build. If conflicts arise, add explicit versions for the affected artifacts or switch to `compose-bom-alpha`.

### 2. New Activity + internal Navigation Compose
- New `NewSettingsActivity` (root package, like `SettingsActivity`), registered in the manifest with `Theme.HabitPulse`, `configChanges`, `resizeableActivity`, predictive back.
- Internal `NavHost` (consistent with `MainActivity`'s pattern) with routes:
  - `new_settings` (first screen / categories hub)
  - `new_settings/ai` (AI 配置 page)
  - `new_settings/ai/provider` (配置提供商)
  - `new_settings/notifications` (通知 page)
  - `new_settings/notifications/reminder` (习惯提醒)
  - `new_settings/notifications/template` (通知模板)
  - `new_settings/general` (通用 page)
  - `new_settings/general/storage` (空间清理)
  - `new_settings/about` (关于 page)
  - `new_settings/about/about` (关于 detail)
- Placeholder categories (连接与同步, 记忆, 界面与显示) and 支持 HabitPulse / 查看应用信息 / 查看GitHub do **not** need routes (see below).
- **Alternatives considered**: reusing `SettingsActivity` with an added NavHost (touches legacy host — rejected for lower regression risk); putting new settings in `MainActivity`'s NavHost (cross-activity entry is awkward — rejected).

### 3. Shared list-item component library (`ui/screens/settings/components/`)
All components are thin, consistent wrappers around native material3 APIs. `enabled = false` renders the disabled + "即将推出" state.

```kotlin
// Clickable segmented item (navigation row / leaf action)
@Composable fun SettingsSegmentedItem(
    index: Int, count: Int, headline: String,
    supportingText: String? = null, leadingIcon: ImageVector? = null,
    trailing: @Composable () -> Unit = { KeyboardArrowRight() },
    enabled: Boolean = true, onClick: () -> Unit)

// Toggleable segmented item (switch row)
@Composable fun SettingsSegmentedSwitch(
    index: Int, count: Int, headline: String,
    supportingText: String? = null, leadingIcon: ImageVector? = null,
    checked: Boolean, enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit)

// Renders items as one segmented group with shapes(index, count) + SegmentedGap
@Composable fun SettingsSegmentedGroup(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit)
```

- Each item uses `SegmentedListItem` with `shapes = ListItemDefaults.segmentedShapes(index, count)`, `colors = ListItemDefaults.segmentedColors()`.
- Single-item groups (first-screen categories) render via `segmentedShapes(0, 1)` → fully-rounded standalone card.
- **Switch fix** (resolves the reported bug):
  ```kotlin
  SegmentedListItem(
      checked = checked,
      onCheckedChange = onCheckedChange,      // whole row is the toggle target
      shapes = ListItemDefaults.segmentedShapes(index, count),
      trailingContent = {
          Switch(checked = checked, onCheckedChange = null, enabled = enabled)
      },
      /* headline / supporting / leading */
  )
  ```
  `Switch(onCheckedChange = null)` makes the Switch non-interactive — it renders enabled and reflects state, but does not consume taps; the `SegmentedListItem` owns the interaction, so the pressed ripple covers the full row and TalkBack announces the row as a toggleable switch.
- **No dividers**: group membership + `SegmentedGap` spacing provide separation.

### 4. First screen (hub) layout — no switches
```
TopAppBar: title "设置" · back arrow · help icon (appbar entry retained → WebView HELP_URL)
── group ──────────────────────────────
  AI 配置                 → nav new_settings/ai
  连接与同步              → disabled, "即将推出"
  通知                    → nav new_settings/notifications
  通用                    → nav new_settings/general
  关于                    → nav new_settings/about
  帮助和反馈              → leaf: opens WebView HELP_URL (NOT a sub-page)
```
Each category is a single-item segmented group (`segmentedShapes(0,1)`).

### 5. Sub-pages
- **AI 配置** (`new_settings/ai`): group [配置提供商 → nav provider, 记忆 disabled "即将推出"].
- **配置提供商** (`new_settings/ai/provider`): re-hosted AI provider config — endpoint / API key / model fields (OutlinedTextField, unchanged semantics), test connection button, streaming response as `SettingsSegmentedSwitch`.
- **通知** (`new_settings/notifications`): group [保持后台运行 (switch or authorize row when permission missing — preserves current permission + limited-mode dialog flows), 习惯提醒 → nav reminder, 通知模板 → nav template].
- **习惯提醒** (`new_settings/notifications/reminder`): re-hosted reminder settings — master switch, DND switch + slider, status rows, test buttons, system-settings + keep-alive links; all converted to the shared segmented components.
- **通知模板** (`new_settings/notifications/template`): template text field + reset/save (was a dialog in legacy).
- **通用** (`new_settings/general`): group [空间清理 → nav storage, 界面与显示 disabled "即将推出"].
- **空间清理** (`new_settings/general/storage`): clear cache + deep clean rows with confirmation dialogs (moved from legacy).
- **关于** (`new_settings/about`): group [关于 → nav about detail, 支持 HabitPulse (switch, TalkBack-disable behavior preserved), 查看应用信息 (system app-info intent), 查看GitHub (WebView)].
- **关于 detail** (`new_settings/about/about`): privacy notice, app version (5-tap debug feature preserved), developer, open-source licenses.

### 6. Placeholder presentation
Disabled `SegmentedListItem` (`enabled = false`) + supporting text "即将推出". No route, tap does nothing. Applies to 连接与同步, 记忆, 界面与显示.

### 7. Legacy settings entry
Append one `SettingsListItem` at the very bottom of `SettingsScreen()` that launches `NewSettingsActivity`. No other legacy changes. Legacy `AISettingsActivity` / `ReminderSettingsActivity` remain untouched; the new settings re-hosts their content in new composables (temporary duplication, resolved when legacy is removed).

### 8. i18n
New strings in all 5 files (`values`, `values-en-rUS`, `values-zh-rCN`, `values-zh-rHK`, `values-zh-rTW`): category titles (AI 配置, 连接与同步, 通用), sub-page titles, "即将推出" label, new-settings entry label, help/feedback titles.

## Risks / Trade-offs

- **material3 alpha stability** → Alpha-only APIs may change before stable; pin exact version, review release notes on upgrade, run full build + unit tests before each alpha bump.
- **Alpha/stable compose version skew** (material3 alpha vs BOM-stable foundation/ui) → Verify clean build; add explicit transitive overrides or switch to `compose-bom-alpha` if resolution conflicts surface.
- **`Switch(onCheckedChange = null)` visual state** → If the trailing Switch renders greyed in alpha24, fall back to a visual-only switch or hoist interaction source; verify visually on device.
- **Behavior parity loss during re-host** (permission dialogs, limited mode, DND slider, debug tap) → Reuse the existing logic verbatim from `SettingsActivity` / `ReminderSettingsScreen` / `AISettingsScreen` when re-hosting; keep legacy screens as reference until verified.
- **Temporary duplication** of AI/reminder screen content (legacy + new) → Accepted; removed together with legacy settings later.

## Migration Plan

1. Bump BOM + pin material3 alpha; clean build + run unit tests to validate resolution.
2. Build shared list-item components; verify press morph / grouped rounding / switch binding in previews and on device.
3. Scaffold `NewSettingsActivity` + internal NavHost + first screen (categories + placeholders + help entry + appbar).
4. Implement sub-pages by re-hosting existing logic with new components (AI, notifications, reminders, template, storage, about).
5. Add legacy settings bottom entry; i18n all new strings.
6. Full build, lint, unit tests, manual device pass (light/dark, RTL, TalkBack, phone/tablet, landscape).

## Open Questions

- None blocking. (Re-hosting legacy content into new composables — decided to duplicate temporarily; revisit when removing legacy.)
