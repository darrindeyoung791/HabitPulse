## Context

The home screen has three tabs (Habits, Contacts, Records) using three custom scrollbar composables (`ScrollableLazyColumnWithScrollbar`, `ScrollableStaggeredGridWithScrollbar`, `ScrollableWaterfallWithScrollbar`). All share the same scrollbar rendering pattern:

- A `Box` with `fillMaxHeight()` + `width(4.dp)` aligned to `CenterEnd`
- Custom scroll fraction calculation with `averageItemHeight` estimation
- Indicator colored `onSurface` at 60% opacity, `RoundedCornerShape(2.dp)`
- Two edge gradient overlays (`16.dp` height) for content boundary indication
- 300ms fade-in, 1.2s delay on inactivity, 300ms fade-out
- No drag support

The `ScrollableWaterfallWithScrollbar` is defined but never called anywhere.

Compose Foundation provides `Modifier.verticalScrollbar(rememberScrollbarAdapter(state), style)` but:
1. It lacks built-in drag-on-thumb support
2. The style customization is limited without Material3's `rememberScrollbarStyle`
3. Material3's scrollbar style uses `colorScheme.outlineVariant` for track and `colorScheme.onSurfaceVariant` for thumb — not the Pixel Launcher tertiary accent approach

## Goals / Non-Goals

**Goals:**
- Replace all 3 old composables with 2 new MD3-styled, drag-supported composables
- Scrollbar thumb color uses MD3 dynamic `tertiary` color (matching Pixel Launcher visual identity)
- Draggable thumb — user can touch and drag the scrollbar indicator to scroll
- Thin 4dp rounded thumb with smooth fade-in/out
- Preserve edge gradient overlays and auto-hide behavior
- Apply to all 7 existing call sites with minimal API changes
- Remove unused `ScrollableWaterfallWithScrollbar`

**Non-Goals:**
- Not changing scroll behavior of non-scrollbar content (LazyColumn/StaggeredGrid internals)
- Not adding scrollbars to screens that don't already have them (Settings, MultiSelectSort, etc.)
- Not changing the LazyColumn content padding, arrangement, or item rendering
- Not modifying the HomeScreen's scroll state management or scroll-to-top logic

## Decisions

### Decision 1: Two composables instead of one

**Choice**: Keep separate `Md3ScrollableColumn` (for `LazyColumn`) and `Md3ScrollableStaggeredGrid` (for `LazyVerticalStaggeredGrid`).

**Rationale**: `LazyColumn` and `LazyVerticalStaggeredGrid` use completely different content lambda types (`LazyListScope.() -> Unit` vs `LazyStaggeredGridScope.() -> Unit`). A single unified composable would require an `@Composable` content parameter instead, which breaks the ergonomic `items`/`itemsIndexed` scope functions.

**Alternatives considered**:
- Single composable with `@Composable` content → loses `LazyListScope` convenience
- Single composable with sealed class parameter for grid config → over-engineered
- `Modifier` extension approach → cannot wrap content with gradient overlays (need Box parent)

### Decision 2: Drag implementation via `Modifier.pointerInput` + `detectDragGestures`

**Choice**: Use `pointerInput` with `detectDragGestures` on the scrollbar indicator Box, calling `listState.scrollBy()` / `gridState.scrollBy()` in response to vertical drag delta.

**Rationale**: This is the standard Compose approach for custom drag interactions. It cleanly translates drag distance on the scrollbar (proportional to content) into scroll amount.

**Details**: The scrollbar occupies a fixed-width column (4dp thumb + 2dp padding = 8dp touch target). The entire hit area handles drag. A `ScrollbarDragState` is tracked to convert pixel drag deltas into proportional scroll adjustments.

### Decision 3: MD3 dynamic color — tertiary accent

**Choice**: `MaterialTheme.colorScheme.tertiary` at 80% opacity for the scrollbar thumb.

**Rationale**: Pixel Launcher app drawer uses the system's tertiary accent (derived from wallpaper via Monet) for its scrollbar. The tertiary palette in MD3 provides the most distinctive accent color that visually differentiates scrollbars from regular UI elements. 80% opacity provides sufficient contrast without being distracting.

**Alternatives considered**:
- `secondary` → too subtle, blends with navigation elements
- `onSurfaceVariant` → too gray, not matching Pixel Launcher style
- `primary` → conflicts with primary buttons and FAB

### Decision 4: Scrollbar width and shape

**Choice**: Keep 4dp width with 2dp corner radius (same as current), but increase touch target to 8dp via transparent padding.

**Rationale**: The current dimensions match Pixel Launcher's thin scrollbar aesthetic. Increasing the hit target to 8dp (the MD3 minimum touch target is 48dp, but this is already a scrolling interaction, not a discrete button) improves usability while maintaining the thin visual appearance.

### Decision 5: Shared internal scrollbar rendering

**Choice**: Extract a private `@Composable` function `ScrollbarIndicator` used by both `Md3ScrollableColumn` and `Md3ScrollableStaggeredGrid`.

**Rationale**: The scrollbar rendering logic (Box with MD3 color, drag handler, fade animation, position calculation) is identical between the two composables. Only the scroll state type differs (`LazyListState` vs `LazyStaggeredGridState`). This follows DRY.

### Decision 6: Edge gradient layout

**Choice**: Preserve `Box` + `Alignment.TopCenter`/`Alignment.BottomCenter` approach from the current implementation.

**Rationale**: These gradients are necessary for visual polish — they fade content edges into the background, preventing a harsh cut-off look. No reason to change them.

## Risks / Trade-offs

- **[Drag calc accuracy]** Scroll position estimation for `LazyStaggeredGrid` is less precise than `LazyColumn` due to variable-height items. → Use scroll offset directly from `gridState.firstVisibleItemScrollOffset` + index averaging, same as the current implementation, which is already an approximation.
- **[Touch conflict]** The drag gesture on the scrollbar may conflict with the LazyColumn's own scroll gesture if the user touches near the edge. → Use narrow touch target (8dp) with `pointerInput` precedence over scroll container.
- **[Accessibility]** Drag-only interaction is not accessible to TalkBack users. → The scrollbar is a visual indicator enhancement; the primary scroll mechanism (finger swipe on content) remains fully accessible. No regression.
- **[Flicker]** Rapid show/hide during quick scrolling. → The existing 1.2s delay + 300ms animation provides smooth transitions.
