## 1. Model & Database

- [x] 1.1 Remove `SupervisionMethod` enum from `Habit.kt`; add `hasSupervision` computed property
- [x] 1.2 Remove `SupervisionMethod` enum from `EnumConverters.kt`
- [x] 1.3 Add `MIGRATION_4_5` to `HabitDatabase.kt` that drops `supervisionMethod` column
- [x] 1.4 Update database version from 4 to 5 in `HabitDatabase.kt`
- [x] 1.5 Update Room schema export JSON for v5 (build project to generate)

## 2. Habit Creation Screen UI

- [x] 2.1 Remove `SupervisionMethod` dropdown from `HabitCreationScreen.kt`
- [x] 2.2 Remove `supervisionMethod` state variable and references
- [x] 2.3 Refactor email input section to be independently expandable (AnimatedVisibility)
- [x] 2.4 Refactor phone input section to be independently expandable (AnimatedVisibility)
- [x] 2.5 Update validation: remove method-based check; validate each section independently
- [x] 2.6 Update edit mode: load emails and phones independently from existing habit
- [x] 2.7 Update save logic to always save both contact lists

## 3. Contacts ViewModel & Screen

- [x] 3.1 Refactor `ContactsViewModel.init` aggregation: remove `supervisionMethod` filter; iterate every habit's emails and phones unconditionally
- [x] 3.2 Fix `deleteContactFromAllHabits`: after removal, only clear supervision if BOTH contact lists are empty
- [x] 3.3 Fix `deleteContactFromHabit`: after removal, only clear supervision if BOTH contact lists are empty
- [x] 3.4 Update `ContactsScreen` to remove any remaining `SupervisionMethod` references
- [x] 3.5 Update `ContactsScreen` last-contact warning logic for mixed contacts

## 4. Display & Other UI

- [x] 4.1 Update `HabitScreen.ReminderDetailDialog`: show all contact types simultaneously (already handled)
- [x] 4.2 Update `RewardBottomSheet`: use `hasSupervision` instead of `supervisionMethod != NONE`
- [x] 4.3 Update `HomeScreen`: count habits with `hasSupervision` for subtitle
- [x] 4.4 Update `HabitScreen` / `HabitCard` any remaining supervision checks
- [x] 4.5 Remove `SupervisionMethod` local re-declaration in `HabitCreationScreen.kt` import

## 5. String Resources

- [x] 5.1 Update `values/strings.xml`: add new strings for independent section labels; remove unused supervision method strings
- [x] 5.2 Update `values-en-rUS/strings.xml`: same changes
- [x] 5.3 Update `values-zh-rHK/strings.xml`: same changes
- [x] 5.4 Update `values-zh-rTW/strings.xml`: same changes

## 6. Tests & Cleanup

- [x] 6.1 Update `HabitTest.kt`: remove `SupervisionMethod`-related tests; add tests for `hasSupervision`
- [ ] 6.2 Add `ContactsViewModelTest.kt`: test aggregation without method filter; test delete bug fix (future)
- [x] 6.3 Remove unused imports across all modified files
- [x] 6.4 Run full build and tests: `./gradlew test`

## 7. Verify

- [x] 7.1 Build debug APK: `./gradlew assembleDebug`
- [x] 7.2 Run all unit tests: `./gradlew test`
