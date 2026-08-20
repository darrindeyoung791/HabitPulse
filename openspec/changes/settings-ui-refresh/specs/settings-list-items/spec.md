## ADDED Requirements

### Requirement: Native expressive list-item components
The settings screens SHALL render list rows using the native material3 expressive `ListItem` / `SegmentedListItem` composables, not custom `Surface`+`Row` wrappers.

#### Scenario: Settings rows use native list items
- **WHEN** any settings page renders a list row
- **THEN** the row is a material3 `ListItem` or `SegmentedListItem` composable

#### Scenario: Non-expressive legacy wrapper is not used
- **WHEN** rendering settings rows in the new settings screens
- **THEN** the legacy `SettingsListItem`/`SettingsSwitchItem` custom wrappers are not used

### Requirement: Press-state corner-radius morph
A clickable list item SHALL animate its corner radius to a distinct `pressedShape` while it is being pressed, and return to its resting shape on release.

#### Scenario: Pressing a list item expands its corner radius
- **WHEN** the user presses and holds a clickable settings list item
- **THEN** the item's corner radius morphs to the pressed shape with ripple feedback

#### Scenario: Releasing restores the resting shape
- **WHEN** the user releases a pressed list item
- **THEN** the item returns to its resting corner radius

### Requirement: Segmented group rounding
When list items are grouped, the first and last items SHALL render with the full edge corner radius and interior items SHALL render with a reduced radius, using `ListItemDefaults.segmentedShapes(index, count)` and `SegmentedGap` spacing.

#### Scenario: Edge items have full radius, interior items reduced
- **WHEN** a group contains three or more items
- **THEN** the first and last items show the edge corner radius and interior items show a smaller radius with gaps between items

#### Scenario: Single-item group renders as a standalone rounded card
- **WHEN** a group contains exactly one item
- **THEN** the item renders with the fully rounded edge shape

### Requirement: No dividers
Settings list groups SHALL NOT use `HorizontalDivider` or any divider line; separation between items and groups SHALL be achieved through segmented shapes, gaps, and spacing.

#### Scenario: Settings lists contain no dividers
- **WHEN** a settings page renders multiple list items
- **THEN** no divider lines are drawn between items

### Requirement: Switch rows bind the whole row to the switch
A switch list item SHALL be a single toggleable surface: the entire row is the toggle target, the trailing `Switch` is rendered non-interactive (`onCheckedChange = null`) and purely reflects state, and pressing anywhere on the row SHALL fire the switch's `onCheckedChange` with proper pressed ripple across the whole row.

#### Scenario: Tapping the row toggles the switch
- **WHEN** the user taps anywhere on a switch list item other than the switch itself
- **THEN** the switch's `onCheckedChange` is invoked and the row shows pressed ripple feedback bound to the switch

#### Scenario: Tapping the switch itself toggles it
- **WHEN** the user taps directly on the switch control
- **THEN** the switch toggles and the row's toggle behavior is consistent with tapping anywhere else

#### Scenario: TalkBack announces the row as toggleable
- **WHEN** a TalkBack user focuses a switch list item
- **THEN** the entire row is announced as a toggleable switch with its current on/off state

### Requirement: Consistent styling across pages
The same logical control (navigation row, switch row, disabled row) SHALL use the same component and visual style on every settings page.

#### Scenario: Same control looks the same on all pages
- **WHEN** the same type of control appears on two different settings pages
- **THEN** both render with identical typography, leading-icon, trailing-icon, shape, and spacing

### Requirement: Disabled placeholder rows
A disabled list item SHALL render greyed out with a "即将推出" supporting text and MUST NOT respond to taps.

#### Scenario: Placeholder row is inert
- **WHEN** the user taps a disabled placeholder row (e.g., 连接与同步)
- **THEN** no action occurs and no navigation happens

#### Scenario: Placeholder row communicates its state
- **WHEN** a disabled placeholder row renders
- **THEN** it is visually disabled and shows "即将推出" as supporting text
