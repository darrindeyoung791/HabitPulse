## ADDED Requirements

### Requirement: Notification channel
The system SHALL create a notification channel with:
- `CHANNEL_ID = "habit_reminder"`
- `CHANNEL_NAME = "习惯提醒"` / `"Habit Reminders"`
- `IMPORTANCE_HIGH` (sound + vibration)
- Description explaining the channel purpose

#### Scenario: Channel creation
- **WHEN** application starts or `ReminderReceiver` first triggers
- **THEN** notification channel `habit_reminder` exists with HIGH importance

### Requirement: Notification content
The system SHALL build a notification with:
- **Title**: Format `"N个习惯即将打卡"` / `"N habits to check in"` where N is the count of habits with `ABOUT_TO_START` status in the next hour (count = 0 → return without sending)
- **Body**: List of habit titles, each on a new line (max ~5 lines, uses `setStyle(NotificationCompat.InboxStyle())` for expandability)
- **Icon**: `R.drawable.ic_stat_name` (reuse existing)
- **Category**: `CATEGORY_REMINDER`

If the count exceeds the notification's expanded view limit, the system SHALL show first N items with "还有N个..." summary.

#### Scenario: Single habit about to start
- **WHEN** 1 habit (e.g., "晨跑") has `ABOUT_TO_START` status
- **THEN** title = "1个习惯即将打卡"
- **THEN** body = "晨跑"

#### Scenario: Multiple habits about to start
- **WHEN** 3 habits ("晨跑", "阅读", "冥想") have `ABOUT_TO_START` status
- **THEN** title = "3个习惯即将打卡"
- **THEN** body (inbox style) shows each habit on its own line

#### Scenario: No habits about to start
- **WHEN** no habit has `ABOUT_TO_START` status at trigger time
- **THEN** no notification is sent (skip silently)

### Requirement: Notification tap action
The notification SHALL have a `PendingIntent` that:
1. Opens `MainActivity` (or brings it to front if already running)
2. Passes an extra `EXTRA_NAVIGATE_TO = "about_to_start"`
3. `MainActivity` reads this extra and navigates to `Route.TodayHabits.createRoute("about_to_start")`

The notification SHALL use `setContentIntent()` with a `FLAG_UPDATE_CURRENT` PendingIntent.
A unique notification ID SHALL be generated per trigger (e.g., based on timestamp) to allow multiple notifications in the shade.

#### Scenario: User taps notification
- **WHEN** user taps the reminder notification
- **THEN** MainActivity opens/comes to foreground
- **THEN** navigation navigates to TodayHabitsScreen filtered to "about_to_start"

### Requirement: English notification text
When the device locale is English (US), the notification SHALL use English text.

#### Scenario: English locale
- **WHEN** device is set to English
- **THEN** title = "3 habits to check in"
- **THEN** use English strings throughout
