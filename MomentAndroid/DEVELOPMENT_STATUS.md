# MOMENT independent development status

This project remains a Java/SQLite native Android prototype. No Kotlin/Compose/Room migration has been claimed.

## Changes in this handoff
- Widget completion uses an idempotent `complete(id)` instead of toggling.
- Widget broadcast uses `goAsync()` and background thread for DB work.
- Main activity task list is loaded asynchronously; task edits, additions, completion and deletions use a serial DB executor.
- Birthday countdowns use `LocalDate` and calendar days, avoiding DST millisecond differences. Feb 29 birthdays are shown on Feb 28 in non-leap years.
- GitHub Actions builds a debug APK using a provisioned Gradle 8.9, without requiring a local Gradle wrapper.

## Known gaps
- Shared Space dialog still queries SQLite on the main thread. Refactor remaining reads before release.
- Calendar grids, notifications, cloud sync, and Kotlin/Room migration remain future work.
- No APK was compiled in this environment; build success is not verified.
- No Gradle wrapper is included. CI uses installed Gradle via setup-gradle; a checked-in wrapper should be generated in a Gradle-capable environment.
- Date parsing and widget behavior need emulator/device tests.

## GitHub build instructions
Push the entire folder contents to a GitHub repository (workflow must live at `.github/workflows/android-debug.yml`). In Actions, run **Build MOMENT debug APK**. Download the `MOMENT-debug-apk` artifact from a successful run, extract it, and install the APK on an Android device after reviewing installation security prompts.
