## Context

The current supervision system uses a mutually-exclusive `SupervisionMethod` enum (NONE/EMAIL/SMS). Each habit can only have one method, meaning contacts must be all emails or all phones. The database schema has a `supervisionMethod` TEXT column alongside `supervisorEmails` (TEXT JSON array) and `supervisorPhones` (TEXT JSON array).

The change removes `supervisionMethod` entirely, allowing both contact types to coexist in any habit without a "type gate." All downstream code that checked `supervisionMethod` before accessing contacts will be simplified to direct list checks.

## Goals / Non-Goals

**Goals:**
- Remove `SupervisionMethod` enum from the entire codebase
- DB v5 migration: drop `supervisionMethod` column
- HabitCreationScreen: replace the supervision method dropdown with two independent expandable sections (email + phone)
- ContactsViewModel: aggregate contacts without method filtering; fix `deleteContactFromAllHabits` bug
- All UI displays (HabitScreen reminder dialog, RewardBottomSheet, HomeScreen subtitle) use `hasSupervision` derived from contact list emptiness
- Update all string resources and tests

**Non-Goals:**
- No change to how contacts are stored (still TEXT JSON arrays in `habits` table)
- No new tables or Room relations for contacts
- No changes to the actual notification/sending logic (still placeholder)
- No behavior change to existing habits during migration (emails stay emails, phones stay phones)

## Decisions

### Decision 1: Remove column instead of deprecating
**Option A**: Remove `supervisionMethod` column entirely (v5 migration, DROP COLUMN).
**Option B**: Keep the column but ignore its value everywhere.
**Decision**: Remove it. Room's `DROP COLUMN` is well-supported since SQLite 3.35.0 (Android 12+), and this reduces technical debt. Android minSdk 26 means SQLite version is sufficient.

### Decision 2: `hasSupervision` as computed property
**Option A**: Add a Kotlin computed property `val hasSupervision: Boolean get() = getSupervisorEmailsList().isNotEmpty() || getSupervisorPhonesList().isNotEmpty()`.
**Option B**: Add a database-level computed column.
**Decision**: Kotlin computed property. Simpler, no DB migration complexity, and the JSON parsing is already happening in the entity.

### Decision 3: Independent expandable sections in creation UI
**Option A**: Each contact type has its own expand/collapse toggle (AnimatedVisibility), both collapsed by default for new habits.
**Option B**: A single expandable section that shows both email and phone sub-sections.
**Decision**: Option A. Cleaner separation, follows Material Design 3 expandable section patterns already used in the codebase (e.g., reminder times). Both independently collapsible means less visual clutter.

### Decision 4: ContactsViewModel aggregation without method filter
**Option A**: Remove SupervisionMethod filter entirely — iterate every habit's emails AND phones unconditionally.
**Option B**: Keep an internal `hasEmails`/`hasPhones` flag per habit.
**Decision**: Option A. The SIMPLEST approach. Every habit's two JSON arrays are parsed regardless. No filtering needed. The ViewModel already handles deduplication by building a `Map<String, ContactInfo>` keyed by `"email:$value"` / `"phone:$value"`.

### Decision 5: Bug fix for `deleteContactFromAllHabits`
**Current bug**: When deleting a contact of one type from all habits, the method sets `supervisionMethod = NONE` unconditionally, even if the other contact type remains.
**Fix**: After removal, check if the OTHER list is also empty. Only set "no supervision" if both lists are empty. With `supervisionMethod` removed entirely, this becomes: remove the contact value from the appropriate list, and update the habit.

## Risks / Trade-offs

- **[Data Loss]** Dropping `supervisionMethod` column: Room migration runs `ALTER TABLE habits DROP COLUMN supervisionMethod`. This is reversible only via backup. Rollback plan: before migration, backup database.
- **[UI Churn]** Existing users familiar with the dropdown will see a different layout. Mitigation: the new independent sections are intuitive (add emails here, add phones there) and the "× contacts set" summary is preserved in the save area.
- **[Edge Case]** Old habits created before migration will have `supervisionMethod` values that are simply dropped during v5 migration — no behavioral loss since the actual contact data lives in `supervisorEmails`/`supervisorPhones`.
- **[Test Coverage]** ContactsViewModel has no existing tests. Adding them is part of the tasks but increases scope. Mitigation: at minimum, test the aggregation logic and the delete bug fix.

## Migration Plan

1. Add `MIGRATION_5_6` (v4→v5) that drops `supervisionMethod` column
2. Remove `SupervisionMethod` enum from `Habit.kt`; add `hasSupervision` computed property; update `EnumConverters`
3. Update `HabitCreationScreen` UI (remove dropdown, add independent sections)
4. Update `ContactsViewModel` aggregation and delete logic
5. Update `HabitScreen`/`RewardBottomSheet`/`HomeScreen` supervision checks
6. Update all string resources (add new, remove unused)
7. Update tests
8. Run full build and tests to verify

## Open Questions

- Should the HabitCreationScreen show a clear summary like "已设置 X 个监督人" regardless of type mix? Yes — keep the existing pattern.
