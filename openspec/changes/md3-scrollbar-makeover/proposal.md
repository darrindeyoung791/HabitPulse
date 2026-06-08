## Why

The current scrollbar implementation uses custom composables with a hardcoded `onSurface` color at 60% opacity, which does not follow Material Design 3 dynamic color guidelines. It lacks a draggable thumb — the user cannot grab and drag the scrollbar to navigate, unlike the Pixel Launcher app drawer. The code is also unnecessarily fragmented into 3 separate composables (LazyColumn, StaggeredGrid, Waterfall), with one unused.

## What Changes

- **BREAKING**: Remove `ScrollableLazyColumnWithScrollbar`, `ScrollableStaggeredGridWithScrollbar`, and `ScrollableWaterfallWithScrollbar` composables from `HabitScreen.kt`
- Create a unified `Md3ScrollableColumn` composable that wraps `LazyColumn`/`LazyVerticalStaggeredGrid` with an MD3 dynamic-color scrollbar
- MD3 dynamic color thumb using `tertiary` container colors (matching Pixel Launcher app drawer style)
- Draggable thumb — user can grab and drag the scrollbar indicator to scroll
- Thin rounded thumb (4dp wide, 2dp corner radius) with smooth fade-in/out animation
- Auto-hide scrollbar after 1.2s of inactivity (existing behavior preserved)
- Apply new scrollbar to all 7 call sites in Habits, Contacts, and Records tabs
- Preserve existing edge gradient overlays for content boundary indication

## Capabilities

### New Capabilities
- `md3-scrollbar`: A reusable, theme-aware scrollbar composable that wraps scrollable containers (LazyColumn, LazyVerticalStaggeredGrid) with MD3 dynamic coloring, drag-to-scroll support, and auto-hide behavior. Replaces all 3 existing custom scrollbar composables.

### Modified Capabilities
- *(none — no existing scrollbar specs)*

## Impact

- **Files modified**:
  - `app/src/main/java/io/github/darrindeyoung791/habitpulse/ui/screens/HabitScreen.kt` — remove ~430 lines of old composables, add ~200 lines of new unified composable
  - `app/src/main/java/io/github/darrindeyoung791/habitpulse/ui/screens/TodayHabitsScreen.kt` — update 1 call site
  - `app/src/main/java/io/github/darrindeyoung791/habitpulse/ui/screens/ContactsScreen.kt` — update 4 call sites
  - `app/src/main/java/io/github/darrindeyoung791/habitpulse/ui/screens/RecordsScreen.kt` — update 2 call sites
- **No new dependencies** — uses built-in Compose APIs (`pointerInput`, `scrollBy`, `animateFloatAsState`)
- **No API surface changes** — the public composable signature changes slightly (unified parameter list)
- **Visual change**: scrollbar thumb color changes from `onSurface` to MD3 dynamic tertiary color
- **Behavioral change**: scrollbar thumb becomes draggable
