# MOMENT v0.8: Maps and gentle reminders

No Google Cloud, API key, traffic backend, Vercel account, payment card or subscription is needed by this build. The app has no INTERNET permission or active traffic service. Traffic integration is deferred.

Create an event with a local date and 24-hour time. Optionally enable **Gentle reminders** and allow Android notifications. MOMENT schedules local reminders about one hour, thirty minutes and ten minutes before the event. Existing events remain opted out until you turn reminders on in event details. The **Try a gentle reminder now** button sends an immediate, clearly labeled preview so notification permission/channel settings can be checked without waiting.

Open an event and tap **Open Maps** to open its destination in Google Maps, or a browser if Maps is unavailable. A starting address is optional; omit it to let Maps choose the start. Reminder notifications also have an **Open Maps** action. No traffic estimates are read back into MOMENT. A manually entered travel estimate may still be displayed separately; reminders are always based on the event's arrival/start time.

Edit a calendar event from its details to change its name, date, time, destination or manual travel estimate. Reminders are rescheduled after changes and cancelled when reminders are switched off or an event is deleted. They are restored after reboot, app updates, clock/timezone changes, and reopening the app. Times follow the phone's local timezone. Nonexistent and ambiguous daylight-saving times are rejected. Late alarms use the remaining time at delivery, skip obsolete earlier stages, and do not notify after the event begins. Delivered stages are recorded to prevent duplicate alerts after rescheduling.

These are gentle, inexact Android reminders, not guaranteed exact departure alarms. Android power-saving restrictions can delay them; force-stopping the app suppresses alarms until it is opened again. If notifications are blocked, event details show that state. No special exact-alarm access is requested.

This is an Android APK. There is no iOS build in this repository yet; an eventual iPhone version can use Apple Maps and iOS local notifications.

Validation: Java checks cover reminder boundaries, late/expired delivery decisions, midnight rollover, timezone calculations and daylight-saving rejection. SQLite checks run the actual migration statement against populated v0.3-schema data and verify that tasks, subtasks, events and Shared Space requests survive. CI builds the Android APK. Phone UI, notification delivery, reboot and battery-saving behavior still require device testing.

Installation: if Android offers **Update**, use it. If it requires uninstalling the existing app because of a signing-key mismatch, stop; uninstalling deletes locally saved data. CI debug APK signing continuity with older builds is not guaranteed.
