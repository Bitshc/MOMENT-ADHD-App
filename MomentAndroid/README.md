# MOMENT — native Android starter (v0.1)

An independent, no-Lovable Android Studio project. Written in Java with Android SDK APIs, SQLite local storage and a real Android AppWidget. No server, account, API key or paid service required.

## Build and install
1. Install Android Studio and Android SDK 35 / JDK 17.
2. Open the `MomentAndroid` folder as a project. Let Gradle sync (requires internet for the Android Gradle plugin).
3. Connect an Android phone with USB debugging, or use an emulator; press **Run**.
4. On the phone, long-press the home screen → **Widgets** → **MOMENT** → drag the widget onto the home screen.
5. Tap a task directly in the widget to check it off; the widget refreshes. Tap **+ Add / open MOMENT** to open the app.

## Implemented
- Local task, event and birthday records; day navigation and date picker.
- One-tap task completion in the app and in the **actual Android home-screen widget**.
- Birthday recurrence by month/day in the app's selected-day view.
- Quick-add task button and event/birthday entry.
- Long-press delete; data persists locally using SQLite.

## Not implemented yet
- Scheduled notifications or birthday reminder alerts (birthdays currently appear on their date only).
- Calendar month/week grid, editing existing items, subtasks, recurring events other than birthdays, calendar sync, voice entry, AI language parsing, cross-device sync, backups.
- Widget currently displays up to three pending tasks across dates, not just today's tasks. This is a deliberate starter behavior, not a fully filtered Today widget.
- The widget add button opens the app, not a dedicated quick-add screen.

## Important
The project is source code, **not an APK**. It has not been compiled or tested on a device in this environment. Android Gradle plugin downloads may require internet. Do not rely on it for critical reminders yet. All data is on the device; uninstalling may remove it.

## Research-informed roadmap
External memory aids, breaking tasks into steps, visual cues, reduced interaction cost, adjustable reminders and gentle recovery. These are design hypotheses informed by ADHD organizational guidance, not clinical claims about MOMENT.
- NICE NG87: https://www.nice.org.uk/guidance/ng87/chapter/recommendations
- NIMH ADHD: https://www.nimh.nih.gov/health/topics/attention-deficit-hyperactivity-disorder
- Android widgets: https://developer.android.com/develop/ui/views/appwidgets/overview

## v0.2 changes (November 2026)
- Home-screen widget now shows up to three **unfinished tasks due today or overdue**, not unrelated future tasks. Tapping a task checks it off directly. Widget add action launches the quick-add task dialog.
- Long-press a task/event/birthday in the app to edit its title or reschedule its date, or delete it.
- **What's next?** displays one pending due task and lets you mark it done.
- **Brain dump** accepts one task per line and saves the batch to the selected day.
- **Upcoming birthdays** displays up to eight nearest annual birthdays. These are *in-app only*, not push alerts.

### Evidence-informed design notes
These features are intended as practical external memory and task-initiation aids, not clinically validated treatment. ADHD varies; users should be able to choose which aids work for them.
- NICE ADHD guideline NG87 (individualized environmental modifications): https://www.nice.org.uk/guidance/ng87/chapter/recommendations
- NIMH ADHD overview and coping approaches: https://www.nimh.nih.gov/health/topics/attention-deficit-hyperactivity-disorder
- Android App Widgets implementation guidance: https://developer.android.com/develop/ui/views/appwidgets/overview

### Current limitations
No automatic reminder notifications, background birthday alarms, month calendar, cloud backup, external calendar sync or spoken natural-language capture yet. Widget uses local SQLite. The project has **not been compiled or device-tested** in this environment; Android Studio build/sync is required before installing. Date edits use YYYY-MM-DD; invalid dates are rejected. If you need dependable birthday alerts today, continue using your phone's established calendar reminders until notification scheduling is built and tested.

## v0.3 Shared Space — local prototype
- Partner requests can be entered **on the same phone** in Shared Space. Each request records a sender, title and suggested date.
- The app owner must **accept** a request before it becomes a personal task or appears in the home-screen widget; requests may also be declined.
- Requests are stored locally in SQLite with a v1→v2 migration preserving existing items.
- **Not yet implemented:** invitations, partner accounts, remote requests, push notifications, shared-device synchronization or authentication. This is not yet a partner-to-partner online feature. Do not share the phone with untrusted people or use it as a secure multi-user system.
- Research-informed design rationale: autonomy and reduced interruption burden; user-controlled acceptance is a design choice, not a clinically validated ADHD intervention.
