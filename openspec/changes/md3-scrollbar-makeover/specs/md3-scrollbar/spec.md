## ADDED Requirements

### Requirement: Md3ScrollableColumn composable

The system SHALL provide an `Md3ScrollableColumn` composable that wraps a `LazyColumn` with an MD3-styled, draggable scrollbar.

**Parameters:**
- `modifier: Modifier = Modifier`
- `listState: LazyListState`
- `contentPadding: PaddingValues = PaddingValues(0.dp)`
- `verticalArrangement: Arrangement.Vertical = Arrangement.Top`
- `content: LazyListScope.() -> Unit`

#### Scenario: Wraps LazyColumn with scrollbar
- **WHEN** `Md3ScrollableColumn` is composed with a `LazyListState`
- **THEN** the content renders inside a `LazyColumn` with a vertical scrollbar indicator on the right edge

#### Scenario: Wraps LazyColumn with scrollbar and content padding
- **WHEN** `contentPadding` is provided
- **THEN** the inner `LazyColumn` uses that padding for its content

#### Scenario: Vertical arrangement propagated
- **WHEN** `verticalArrangement` is provided
- **THEN** the inner `LazyColumn` uses that arrangement for item spacing

### Requirement: Md3ScrollableStaggeredGrid composable

The system SHALL provide an `Md3ScrollableStaggeredGrid` composable that wraps a `LazyVerticalStaggeredGrid` with an MD3-styled, draggable scrollbar.

**Parameters:**
- `modifier: Modifier = Modifier`
- `gridState: LazyStaggeredGridState`
- `columns: StaggeredGridCells = StaggeredGridCells.Fixed(2)`
- `contentPadding: PaddingValues = PaddingValues(0.dp)`
- `horizontalArrangement: Arrangement.Horizontal = Arrangement.spacedBy(16.dp)`
- `verticalItemSpacing: Dp = 8.dp`
- `content: LazyStaggeredGridScope.() -> Unit`

#### Scenario: Wraps LazyVerticalStaggeredGrid with scrollbar
- **WHEN** `Md3ScrollableStaggeredGrid` is composed with a `LazyStaggeredGridState`
- **THEN** the content renders inside a `LazyVerticalStaggeredGrid` with a vertical scrollbar indicator on the right edge

#### Scenario: Grid columns configurable
- **WHEN** `columns` is set to `StaggeredGridCells.Fixed(3)`
- **THEN** the inner grid uses 3 columns

### Requirement: MD3 dynamic color scrollbar thumb

The scrollbar thumb SHALL use `MaterialTheme.colorScheme.tertiary` at 80% opacity with `RoundedCornerShape(2.dp)`.

#### Scenario: Default color is tertiary
- **WHEN** the scrollbar indicator is rendered
- **THEN** its color equals `MaterialTheme.colorScheme.tertiary` at 80% alpha

#### Scenario: Shape is rounded
- **WHEN** the scrollbar indicator is rendered
- **THEN** its shape is `RoundedCornerShape(2.dp)`

### Requirement: Draggable scrollbar thumb

The scrollbar thumb SHALL respond to drag gestures. When the user drags vertically on the scrollbar area, the underlying scrollable container SHALL scroll proportionally.

#### Scenario: Drag down scrolls content down
- **WHEN** the user drags the scrollbar thumb downward by N pixels
- **THEN** the LazyColumn/LazyVerticalStaggeredGrid scrolls proportionally down by the corresponding amount

#### Scenario: Drag up scrolls content up
- **WHEN** the user drags the scrollbar thumb upward by N pixels
- **THEN** the LazyColumn/LazyVerticalStaggeredGrid scrolls proportionally up by the corresponding amount

#### Scenario: Drag at top does not overscroll
- **WHEN** the user drags the scrollbar thumb upward while already at the top of the content
- **THEN** the scroll position stays at 0 (no negative scroll)

#### Scenario: Drag at bottom does not overscroll
- **WHEN** the user drags the scrollbar thumb downward while already at the bottom of the content
- **THEN** the scroll position stays at the maximum (no overscroll)

### Requirement: Auto-hide scrollbar on inactivity

The scrollbar SHALL automatically fade out after 1.2 seconds of no scrolling activity, and reappear immediately when scrolling resumes.

#### Scenario: Scrollbar fades out after inactivity
- **WHEN** the user stops scrolling for 1.2 seconds
- **THEN** the scrollbar fades out with a 300ms animation

#### Scenario: Scrollbar reappears on scroll
- **WHEN** the user starts scrolling again after the scrollbar has hidden
- **THEN** the scrollbar immediately becomes visible

### Requirement: Edge gradient overlays

The composable SHALL render gradient overlays at the top and bottom edges that fade content into the background when scrolled.

#### Scenario: Top gradient visible when not at top
- **WHEN** the content is scrolled away from the top
- **THEN** a gradient overlay is visible at the top edge, fading from `MaterialTheme.colorScheme.background` to transparent

#### Scenario: Bottom gradient visible when not at bottom
- **WHEN** the content is scrolled away from the bottom
- **THEN** a gradient overlay is visible at the bottom edge, fading from transparent to `MaterialTheme.colorScheme.background`

#### Scenario: No top gradient when at top
- **WHEN** the content is at the very top (firstVisibleItemIndex == 0 && scrollOffset == 0)
- **THEN** the top gradient overlay is not visible

#### Scenario: No bottom gradient when at bottom
- **WHEN** the content is at the very bottom (last item fully visible)
- **THEN** the bottom gradient overlay is not visible

### Requirement: Scrollbar thumb size proportional to content

The scrollbar thumb height SHALL be proportional to the ratio of visible viewport height to total content height, with a minimum of 3% of the viewport height.

#### Scenario: Small thumb for long lists
- **WHEN** the content is much taller than the viewport
- **THEN** the scrollbar thumb is proportionally small (at least 3% of viewport height)

#### Scenario: Large thumb for short lists
- **WHEN** the content fits within or is slightly taller than the viewport
- **THEN** the scrollbar thumb is proportionally large, up to 100% of viewport height

### Requirement: Remove old scrollbar composables

The old `ScrollableLazyColumnWithScrollbar`, `ScrollableStaggeredGridWithScrollbar`, and `ScrollableWaterfallWithScrollbar` composables SHALL be removed from `HabitScreen.kt`.

#### Scenario: Old composables not defined
- **WHEN** the codebase is built after the change
- **THEN** `ScrollableLazyColumnWithScrollbar`, `ScrollableStaggeredGridWithScrollbar`, and `ScrollableWaterfallWithScrollbar` no longer exist in `HabitScreen.kt`

### Requirement: Replace all call sites

All existing call sites using `ScrollableLazyColumnWithScrollbar` and `ScrollableStaggeredGridWithScrollbar` SHALL be updated to use the new `Md3ScrollableColumn` and `Md3ScrollableStaggeredGrid` respectively.

#### Scenario: HabitScreen call sites updated
- **WHEN** `HabitScreen.kt` is viewed after the change
- **THEN** line 564 uses `Md3ScrollableStaggeredGridWithScrollbar` and line 618 uses `Md3ScrollableColumn`

#### Scenario: TodayHabitsScreen call site updated
- **WHEN** `TodayHabitsScreen.kt` is viewed after the change
- **THEN** line 196 uses `Md3ScrollableColumn`

#### Scenario: ContactsScreen call sites updated
- **WHEN** `ContactsScreen.kt` is viewed after the change
- **THEN** lines 244, 312, 356, 424 use `Md3ScrollableColumn`

#### Scenario: RecordsScreen call sites updated
- **WHEN** `RecordsScreen.kt` is viewed after the change
- **THEN** lines 265, 352 use `Md3ScrollableColumn`

### Requirement: Scrollbar indicator width and touch target

The scrollbar indicator SHALL be 4dp wide with a 2dp corner radius. The total touch target for drag gestures SHALL be 8dp wide (indicator + 2dp padding on each side or equivalent).

#### Scenario: Visual width is 4dp
- **WHEN** the scrollbar is rendered
- **THEN** the visible thumb is exactly 4dp wide

#### Scenario: Touch target is 8dp
- **WHEN** the user touches within 8dp of the right edge
- **THEN** the drag gesture is captured by the scrollbar
