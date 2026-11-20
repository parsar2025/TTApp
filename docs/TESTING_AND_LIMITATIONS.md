# Testing, Build Setup & Platform Limitations

## Validation snapshot

At documentation time, **the Git repository and source files exist**, Gradle 8.13 wrapper launched with a Java 21 runtime, and main Android XML resources passed basic XML parsing. **No successful Android compile, unit test execution, installed APK, or Samsung S23 field test has been reported.**

The build attempt failed during Android Gradle Plugin resolution. Android command-line tools were downloaded, but platform/build-tools packages were not installed because Google SDK license acceptance was not granted. Do not represent this repository as a verified release.

## Build steps (Windows)

Prerequisites: JDK 17+, Android SDK Platform 36, Android Build Tools 36.0.0 and connectivity to Gradle/Google Maven repositories. Review and accept the Android SDK licenses yourself through Android Studio > SDK Manager (or supported command-line tooling).

```powershell
cd C:\Users\ASUS\Documents\Code\TTApp
$env:JAVA_HOME = 'C:\Program Files\JetBrains\PyCharm 2025.3.3\jbr'
.\gradlew.bat --version
.\gradlew.bat :app:testDebugUnitTest
.\gradlew.bat :app:assembleDebug
```

If successful, inspect `app/build/outputs/apk/debug/app-debug.apk`. Its existence has **not** been confirmed. Review Gradle repository configuration if `com.android.application` or other artifacts fail to resolve.

## Functional acceptance matrix

| Scenario | Expected behavior | Validation |
| --- | --- | --- |
| Start → close app → reopen | Recover task and correct elapsed time without background tick | Pending device test |
| Start → Pause → Resume → Stop | Correct interval records; no paused time added | Pending |
| Return to stopped task | New session, lifetime total accumulates | Pending |
| Start task B while A runs | A's active interval closes; only B accumulates | Pending |
| Process killed | Tracking anchor persisted | Pending |
| Reboot | Recover with documented wall-clock fallback | Pending |
| Clock/timezone edit | Monotonic duration within boot, consistent date grouping | Pending |
| Crossing midnight | Duration assigned across relevant local days | JVM test source present; not executed |
| Edit/delete intervals | Totals update from saved interval records | Pending |
| Search titles/descriptions/tags | Relevant task rows returned | Pending |
| JSON backup/restore | Consistent data, protected replacement, paused restored session | Pending |
| CSV export | Rows preserve task, session and interval data | Pending |
| Widget / notification actions | Buttons apply repository operations, UI refreshes on events | Pending S23 test |
| Notification permission denied | Core stopwatch still works | Pending |
| Large history and battery | Acceptable responsiveness, no periodic CPU wakeups | Pending profile |

Source unit tests currently include `TimeMathTest.kt` and `DaySplitTest.kt`; full repository, Room integration, widget and UI instrumentation tests remain to be written.

## Android and Samsung One UI restrictions

- Third-party applications cannot assume public APIs for editing Samsung's **native Always-On Display**. TTApp source does not implement direct interactive AOD elements.
- A notification may be visible on lock screen or AOD depending on the device's security and One UI notification settings; actions may require waking the display or unlocking.
- Android 13+ notification permission is required for optional notifications. Denial must not stop normal task recording.
- System notification `setTimeoutAfter(3000)` is a request; actual display duration and system styling are implementation dependent.
- Widget dimensions, action delivery, launch restrictions, and battery optimization behavior need actual device testing.
- Do not use a battery-intensive full-screen AOD overlay to approximate missing native behavior.

## Timekeeping limitations

The monotonic elapsed-time clock is reliable for time intervals in a single boot, including screen-off operation. It resets on reboot; wall-clock fallback cannot distinguish powered-off time and may be wrong if device time changed. Cross-midnight accounting uses locally calculated calendar boundaries, so timezone transitions must be tested. A corrected history entry is an explicit user override.

## Data & privacy

- All core data is stored on device; app automatic cloud backup is disabled.
- CSV and JSON files become accessible wherever the user saves them with Android's document picker; treat them as personal activity records.
- Restore **replaces** the existing database's tasks and tracking data, so export a fresh backup first.
- Restore is intended to be atomic, but a full round-trip verification and malformed-backup tests are required.
- Room schema v1 has no published migration path yet; never enable destructive fallback as a shortcut for release.

## Release definition

Only call an APK **working** after `assembleDebug` succeeds, `testDebugUnitTest` passes, critical user flows are exercised on a device, and errors or restrictions are accurately recorded. Only call the app **production-ready** after explicit device, accessibility, history/backup, and battery/performance sign-off.
