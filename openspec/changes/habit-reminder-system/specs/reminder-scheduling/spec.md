## ADDED Requirements

### Requirement: System schedules reminders every 30 minutes
The system SHALL use `AlarmManager.setAlarmClock()` to schedule reminder notifications at each hour and half-hour (e.g., 08:00, 08:30, 09:00, 09:30).
After each alarm fires, the system SHALL reschedule the next alarm for the next :00 or :30 mark.

#### Scenario: Schedule next alarm at :00
- **WHEN** current time is 08:15
- **THEN** system schedules next alarm at 08:30

#### Scenario: Schedule next alarm at :30
- **WHEN** current time is 08:35
- **THEN** system schedules next alarm at 09:00

### Requirement: ReminderReceiver handles alarm trigger
The system SHALL register a `BroadcastReceiver` named `ReminderReceiver` that handles the alarm intent.
On trigger, the receiver SHALL:
1. Check notification permission — skip if not granted
2. Check DND (Do Not Disturb) — skip if within DND window
3. Query database for habits with `ABOUT_TO_START` status in the next hour
4. If habits found → build and send notification
5. Schedule next alarm

#### Scenario: Permission not granted
- **WHEN** `ReminderReceiver` fires and notification permission is not granted
- **THEN** no notification is sent
- **THEN** next alarm is still scheduled to maintain wake-up cycle

#### Scenario: Within DND window
- **WHEN** `ReminderReceiver` fires and current time is within DND window
- **THEN** no notification is sent
- **THEN** DND override counter increments
- **THEN** next alarm is still scheduled

### Requirement: Boot recovery
The system SHALL recover reminder scheduling on device boot. The existing `BootReceiver` SHALL call `ReminderManager.scheduleReminders()` after starting the foreground service.

#### Scenario: Device boots
- **WHEN** device completes boot
- **THEN** `BootReceiver` restores alarm schedule for reminder notifications

### Requirement: DND override protection
The system SHALL track a counter of consecutive suppressed reminders due to DND.
When the counter reaches 5 (approximately 2.5 hours of suppressed notifications), the system SHALL send an informational notification alerting the user that reminders are being suppressed, and reset the counter.

The counter SHALL reset on any of:
- User sends a test notification
- A reminder fires outside DND window
- Application restart/process recreate

#### Scenario: 5 consecutive DND suppressions
- **WHEN** 5 consecutive reminder triggers are suppressed by DND
- **THEN** system sends an informational notification: "提醒功能已被静音，请检查免打扰设置"
- **THEN** counter resets to 0
