## 1. Core: Replace scrollbar composables in HabitScreen.kt

- [x] 1.1 Add necessary imports (`pointerInput`, `detectDragGestures`); remove unused imports (`ScrollState`)
- [x] 1.2 Implement internal `calculateScrollbarIndicator` helper function (shared position/size calculation, reused by both new composables)
- [x] 1.3 Implement `Md3ScrollableColumn` composable — wraps `LazyColumn` with MD3 scrollbar, drag support, auto-hide, edge gradients
- [x] 1.4 Implement `Md3ScrollableStaggeredGrid` composable — wraps `LazyVerticalStaggeredGrid` with same MD3 scrollbar features
- [x] 1.5 Remove old `ScrollableLazyColumnWithScrollbar`, `ScrollableStaggeredGridWithScrollbar`, `ScrollableWaterfallWithScrollbar` composables
- [x] 1.6 Update the import in the file to point to the new composables

## 2. Update call sites

- [x] 2.1 Update `HabitScreen.kt` call site (line 564: `ScrollableStaggeredGridWithScrollbar` → `Md3ScrollableStaggeredGrid`, line 618: `ScrollableLazyColumnWithScrollbar` → `Md3ScrollableColumn`)
- [x] 2.2 Update `TodayHabitsScreen.kt` call site (line 196: `ScrollableLazyColumnWithScrollbar` → `Md3ScrollableColumn`)
- [x] 2.3 Update `ContactsScreen.kt` call sites (lines 244, 312, 356, 424: `ScrollableLazyColumnWithScrollbar` → `Md3ScrollableColumn`)
- [x] 2.4 Update `RecordsScreen.kt` call sites (lines 265, 352: `ScrollableLazyColumnWithScrollbar` → `Md3ScrollableColumn`)

## 3. Verify and clean up

- [x] 3.1 Build project and verify no compile errors
- [x] 3.2 Run existing unit tests to ensure no regressions
- [x] 3.3 Visually verify scrollbar appearance and drag behavior on all 3 tabs (Habits, Contacts, Records)

## 4. Add MD3 scrollbar to settings screens

- [x] 4.1 `SettingsActivity.kt` — Replace plain `LazyColumn` + custom gradient overlays with `Md3ScrollableColumn` (remove ~80 lines of inline scroll/gradient state, reuse shared composable)
- [x] 4.2 `AISettingsScreen.kt` — Replace plain `LazyColumn` with `Md3ScrollableColumn` (preserves contentPadding + verticalArrangement)
- [x] 4.3 `ReminderSettingsScreen.kt` — Add `rememberLazyListState()`, replace plain `LazyColumn` with `Md3ScrollableColumn`
- [x] 4.4 `OpenSourceLicensesActivity.kt` — Skipped: `LibrariesContainer` (AboutLibraries) manages its own internal `LazyColumn` — scroll state is inaccessible from outside
