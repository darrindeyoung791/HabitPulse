# AI Chat Card Components - Visual Style Audit

## Overview

This document catalogs all card-type UI components in `AIChatScreen.kt` with their current visual properties, serving as a reference for future visual refactoring.

**File**: `app/src/main/java/io/github/darrindeyoung791/habitpulse/ui/screens/ai/AIChatScreen.kt`

---

## 1. Chat Bubbles

### UserChatBubble
| Property | Value |
|----------|-------|
| Shape | `RoundedCornerShape(18.dp, 18.dp, 4.dp, 18.dp)` |
| Background | `MaterialTheme.colorScheme.primary` |
| Text Color | `MaterialTheme.colorScheme.onPrimary` |
| Padding | horizontal 16dp, vertical 10dp |
| Alignment | End (right) |
| Outer Padding | start 48dp, end 16dp, vertical 4dp |
| Long Press | Copy to clipboard |

### AIChatBubble
| Property | Value |
|----------|-------|
| Shape | `RoundedCornerShape(18.dp, 18.dp, 18.dp, 4.dp)` |
| Background | `MaterialTheme.colorScheme.surfaceVariant` |
| Text Color | Default |
| Alignment | Start (left) |
| Outer Padding | start 16dp, end 48dp, vertical 4dp |

---

## 2. Habit Cards

### HabitCreatedCard
| Property | Value |
|----------|-------|
| Component | `Card` |
| Container Color (unconfirmed) | `MaterialTheme.colorScheme.surfaceVariant` |
| Container Color (confirmed) | `MaterialTheme.colorScheme.secondaryContainer` |
| Padding | 16dp |
| Outer Padding | horizontal 16dp, vertical 4dp |
| Icon | `Icons.Default.CheckCircle` |
| Icon Tint (unconfirmed) | `onSurfaceVariant.copy(alpha = 0.6f)` |
| Icon Tint (confirmed) | `primary` |
| Icon Size | 20dp |
| Title Style | `titleSmall` |
| Summary Style | `bodySmall`, `onSurfaceVariant` |
| Buttons | `HapticButton` (confirm), `HapticOutlinedButton` (edit), `HapticTextButton` (delete, error color) |

### HabitEditCard
| Property | Value |
|----------|-------|
| Component | `Card` |
| Container Color | `MaterialTheme.colorScheme.surfaceVariant` |
| Padding | 16dp |
| Outer Padding | horizontal 16dp, vertical 4dp |
| Title Style | `bodyLarge` |
| Button | `HapticButton` (edit) |

### HabitDeleteCard
| Property | Value |
|----------|-------|
| Component | `Card` |
| Container Color | `MaterialTheme.colorScheme.surfaceContainerHigh` |
| Padding | 16dp |
| Outer Padding | horizontal 16dp, vertical 4dp |
| Prompt Style | `bodyLarge` |
| Title Style | `bodyMedium`, `onSurfaceVariant` |
| Confirm Button | `Button` with `error` container color, `RoundedCornerShape(28.dp)` |
| Cancel Button | `HapticTextButton` |

### HabitPickerCard
| Property | Value |
|----------|-------|
| Component | `Card` |
| Container Color | `MaterialTheme.colorScheme.surfaceVariant` |
| Padding | 12dp |
| Outer Padding | horizontal 16dp, vertical 4dp |
| Search Field | `OutlinedTextField` with `RoundedCornerShape(12.dp)` |
| List Item (unselected) | Transparent background |
| List Item (selected) | `secondaryContainer` background, `RoundedCornerShape(10.dp)` |
| Check Icon (selected) | `CheckCircle`, `primary` |
| Check Icon (unselected) | `RadioButtonUnchecked`, `onSurfaceVariant` |
| Edit Icon | `Edit`, `primary` |
| Delete Icon | `Delete`, `error` |
| Bottom Buttons | `HapticButton` (submit), `HapticOutlinedButton` (manual done) |

---

## 3. Status Chips

### StatusChip (PartialHabit)
| Property | Value |
|----------|-------|
| Component | `Card` |
| Container Color | `MaterialTheme.colorScheme.secondaryContainer` |
| Padding | 12dp |
| Outer Padding | horizontal 16dp, vertical 4dp |
| Icon | `CheckCircle`, `primary`, 18dp |
| Text Style | `bodyMedium` |

### StatusChip (HabitDeleteData)
| Property | Value |
|----------|-------|
| Component | `Card` |
| Container Color | `MaterialTheme.colorScheme.errorContainer` |
| Padding | 12dp |
| Outer Padding | horizontal 16dp, vertical 4dp |
| Icon | `Delete`, `onErrorContainer`, 18dp |
| Text Style | `bodyMedium`, `onErrorContainer` |

### StatusChip (labelRes)
| Property | Value |
|----------|-------|
| Component | `Card` |
| Container Color | `MaterialTheme.colorScheme.secondaryContainer` |
| Padding | 12dp |
| Outer Padding | horizontal 16dp, vertical 4dp |
| Icon | `CheckCircle`, `primary`, 18dp |
| Text Style | `bodyMedium` |

---

## 4. Settings Cards

### SettingStatusCard
| Property | Value |
|----------|-------|
| Component | `SettingsSegmentedGroup` |
| Outer Padding | horizontal 16dp, vertical 4dp |
| Inner Items | `SettingsListSurface` with `SettingsIconChip` |
| Trailing Icon | `KeyboardArrowRight`, `onSurfaceVariant`, 24dp |

### SettingChangeCard
| Property | Value |
|----------|-------|
| Component | `Card` |
| Container Color | `MaterialTheme.colorScheme.secondaryContainer` |
| Padding | 12dp |
| Outer Padding | horizontal 16dp, vertical 4dp |
| Icon | `settingIconFor(key)`, 24dp, `onSecondaryContainer` |
| Text Style | `bodyMedium` |
| Switch Colors | checked: `onPrimary` thumb / `primary` track; unchecked: `outline` thumb / `surfaceContainerHighest` track |
| Dropdown | `DarkModeDropdown` with `primary` text color |

### SettingsNavCard
| Property | Value |
|----------|-------|
| Component | `Card` |
| Container Color | `MaterialTheme.colorScheme.surfaceVariant` |
| Padding | 16dp |
| Outer Padding | horizontal 16dp, vertical 4dp |
| Icon Box | 40dp, `tint.container` background, `RoundedCornerShape(12.dp)` |
| Icon | 24dp, `tint.content` |
| Title Style | `titleSmall` |
| Subtitle Style | `bodySmall`, `onSurfaceVariant` |
| Button | `HapticTextButton` |

---

## 5. Question Cards

### ChatQuestionCard
| Property | Value |
|----------|-------|
| Component | `Card` |
| Container Color | `MaterialTheme.colorScheme.tertiaryContainer` |
| Padding | 16dp |
| Outer Padding | horizontal 16dp, vertical 4dp |
| Title Style | `titleSmall`, `onTertiaryContainer` |
| Options | `FilterChip` with selection state |
| Selected Chip | `secondaryContainer` |
| Unselected Chip | `surface` |

---

## 6. Error/System Messages

### ToolError
| Property | Value |
|----------|-------|
| Component | `AIChatBubble` (reused) |
| Prefix | `ai_error_prefix` string |

### SystemError
| Property | Value |
|----------|-------|
| Component | `AIChatBubble` (reused) |
| Prefix | `ai_error_prefix` string |

---

## 7. Shared Button Components

### HapticButton
| Property | Value |
|----------|-------|
| Component | `Button` |
| Haptic | `PressVibrationFeedback` |

### HapticTextButton
| Property | Value |
|----------|-------|
| Component | `TextButton` |
| Haptic | `PressVibrationFeedback` |

### HapticOutlinedButton
| Property | Value |
|----------|-------|
| Component | `OutlinedButton` |
| Haptic | `PressVibrationFeedback` |

### HapticIconButton
| Property | Value |
|----------|-------|
| Component | `IconButton` |
| Size | 36dp |
| Icon Size | 20dp |
| Haptic | `PressVibrationFeedback` |

---

## 8. Color Scheme Summary

| Component | Container Color | Icon Color | Text Color |
|-----------|-----------------|------------|------------|
| UserBubble | `primary` | - | `onPrimary` |
| AIBubble | `surfaceVariant` | - | default |
| HabitCreated (unconfirmed) | `surfaceVariant` | `onSurfaceVariant` | default |
| HabitCreated (confirmed) | `secondaryContainer` | `primary` | default |
| HabitEdit | `surfaceVariant` | - | default |
| HabitDelete | `surfaceContainerHigh` | - | `onSurfaceVariant` |
| StatusChip (general) | `secondaryContainer` | `primary` | default |
| StatusChip (deleted) | `errorContainer` | `onErrorContainer` | `onErrorContainer` |
| SettingChange | `secondaryContainer` | `onSecondaryContainer` | default |
| SettingsNav | `surfaceVariant` | seed tint | default |
| QuestionCard | `tertiaryContainer` | - | `onTertiaryContainer` |
| Delete Button | `error` container | - | - |
