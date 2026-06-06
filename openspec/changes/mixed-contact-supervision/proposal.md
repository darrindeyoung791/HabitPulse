## Why

当前习惯的监督方式（SupervisionMethod）是互斥的：要么 NONE、要么 EMAIL、要么 SMS。用户只能为同个习惯选择一种联系方式（全邮箱或全短信），无法混合添加邮箱和短信监督人。这限制了用户的使用场景——例如用户可能希望同时用邮件通知家人、用短信通知自己。

## What Changes

### Database
- **BREAKING**: Remove `supervisionMethod` column from `habits` table (DB v4 → v5 migration)
- A habit now independently stores email contacts AND phone contacts (both can be non-empty simultaneously)
- "Has supervision" = `supervisorEmails` not empty OR `supervisorPhones` not empty

### Model
- **BREAKING**: Remove `SupervisionMethod` enum from `Habit.kt`
- Add `hasSupervision` computed property to `Habit` (derived from non-empty contact lists)

### UI — Habit Creation Screen
- Remove the "Supervision Method" dropdown (NONE / EMAIL / SMS)
- Show BOTH email and phone input sections independently
- Each section has its own toggle/expand control to add contacts of that type
- Validation: no requirement to have any supervision (both sections can be collapsed)

### UI — Contacts Screen
- Aggregation no longer filters by `SupervisionMethod`. All emails and phones from every habit are aggregated regardless
- `deleteContactFromAllHabits` bug fix: when deleting a contact type, only set NONE if the OTHER contact type is also empty

### UI — Habit Card / Reminder Dialog
- "Supervision status" now checks `hasSupervision` (contact lists not empty)
- ReminderDetailDialog shows all contact types simultaneously

### RewardBottomSheet
- "Notify Supervisor" button shown if ANY contact exists (email or phone)

### Data Flow
- ContactsViewModel aggregation simplified: no `supervisionMethod` filter needed
- `HabitRepository` — no changes needed (CRUD remains same)

## Capabilities

### New Capabilities
- `mixed-supervision-model`: Remove SupervisionMethod enum, add hasSupervision, DB v5 migration
- `mixed-supervision-creation-ui`: Refactor HabitCreationScreen to support independent email/phone input sections
- `mixed-supervision-contacts-aggregation`: Refactor ContactsViewModel and ContactsScreen to aggregate contacts without SupervisionMethod filter
- `mixed-supervision-display`: Update HabitScreen, RewardBottomSheet, HomeScreen to use hasSupervision

### Modified Capabilities
<!-- No existing specs are modified -->

## Impact

- **Database**: v5 migration required (drop supervisionMethod column)
- **Model**: Remove `SupervisionMethod` enum from Habit.kt and HabitCreationScreen.kt
- **UI**: HabitCreationScreen structure change (remove dropdown, add independent sections)
- **ViewModel**: ContactsViewModel removes SupervisionMethod filter logic; deleteContactFromAllHabits bug fixed
- **Tests**: Update HabitTest (remove SupervisionMethod-related tests), add/update ContactsViewModel tests
- **Strings**: Add new strings for independent section labels; some old strings become unused
