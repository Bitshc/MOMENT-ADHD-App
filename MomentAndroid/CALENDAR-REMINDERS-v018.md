# MOMENT v0.18: calendar interaction and reminders

## Calendar

The month, week and widget run Sunday–Saturday. The Week view now has seven time columns with colored event/task blocks. Swipe sideways for all days on a phone and vertically through the hours. Tap an empty time to create an event, or use the + menu. Hold a block and drag it to another time/day; times snap to 15-minute intervals. Month event chips can be held and dropped on another date while keeping their time. A move that overlaps another plan asks whether to keep both. Tap blocks for details/editing. Categories still use names as well as colors.

Task and chore blocks appear once you give them a time. Their type has its own color. Untimed items remain in the selected-day list. Dragging timed blocks preserves their duration and reminder setting and reschedules their reminders. Midnight-crossing blocks are clamped to finish within the selected day. Existing events and stored data are preserved.

## Individual reminders

Create/edit a chore or to-do to select its day, time, estimated length and Remind me setting. Optional repeats are daily or weekly. Reminders are local Android alarms and include Done and Snooze 10 min actions. Done completes a one-off task; repeating routines advance to their next date after today and reset their step checkboxes. Repeated use of an old Done action does not skip another occurrence. Snoozing changes the alert time without completing the item. Overdue, opted-in items can notify once when scheduled or re-enabled; dismissing a notification does not repeatedly nag. Use Snooze for a follow-up. Untimed/opted-out or completed tasks do not generate individual alarms.

The bell opens a Reminder Center with the enabled timed chore/to-do list, notification settings, optional precise timing, an actual scheduled 30-second test and its last delivery timestamp. Daily summary settings remain separate. Existing event reminders remain available and benefit from the same optional precise timing.

On Android 12+, precise alarms require the user to allow Alarms & reminders. Without this permission, the app uses flexible alarms and says so. Notification permission and the To-do and chore reminders channel must be enabled. Do Not Disturb, manufacturer battery controls and force-stop can still prevent or delay visible alerts. Granting precise access is not a guarantee of delivery. Alarm schedules are restored on reboot, updates, clock/timezone changes, permission grants and reopening the app. Times follow the phone timezone.

This build requires no Google/Vercel account, routing API, backend, subscription or Internet permission. No billing has been enabled. It is an Android build; Google Calendar synchronization, recurring calendar events and iOS remain separate work. Repeats in this version apply to chores and to-dos only. Finishing early advances a routine to the next occurrence; this is a simple daily/weekly routine model, not a recurring-series exception editor.

## Validation

Unit tests cover Sunday week boundaries, drag snapping, recurring date advancement, calendar interval layout, reminder boundaries and database upgrades. SQLite tests use the app's actual query/schema statements to check opt-in filtering, completed-item filtering, timed chores in the calendar and stale snooze protection. A disposable Android CI emulator test exercises real alarm delivery, notification Done/Snooze actions, recurrence idempotency, event moves and the weekly screen, and captures a synthetic-data screenshot. Report the actual CI results separately; source presence alone is not proof that the device test passed. Samsung phone testing is still required.

Installation: select Update if offered. If Android asks you to uninstall an earlier build because its debug signing key differs, stop; uninstalling deletes locally saved data.
