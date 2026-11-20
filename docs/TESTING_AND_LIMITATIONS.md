# Testing, Build Setup & Platform Limitations

## Validation snapshot

The debug APK built with Android SDK 36 and a full JDK 21. APK Signature Scheme v2 verification passed. The APK installed and the stopwatch screen opened on Samsung model SM-S918B running Android 17. All seven JVM tests passed, including local midnight and daylight-saving boundaries. Two Room instrumentation tests passed on the same phone. Android lint passed with warnings.

## Build steps (Windows)

Prerequisites: JDK 17+, Android SDK Platform 36, Android Build Tools 36.0.0 and connectivity to Gradle/Google Maven repositories. Review and accept the Android SDK licenses yourself through Android Studio > SDK Manager (or supported command-line tooling).

```powershell
cd C:\Users\ASUS\Documents\Code\TTApp
$env:JAVA_HOME = 'C:\path\to\jdk-21'
.\gradlew.bat --version
.\gradlew.bat :app:testDebugUnitTest
.\gradlew.bat :app:assembleDebug
```

The signed debug APK is at `app/build/outputs/apk/debug/app-debug.apk`. Run `gradlew.bat :app:connectedDebugAndroidTest` with an authorized USB device to repeat the repository and backup tests.

## Functional acceptance matrix

| Scenario | Expected behavior | Validation |
| --- | --- | --- |
| Start → close app → reopen | Recover task and correct elapsed time without background tick | Pending device test |
| Start → Pause → Resume → Stop | Correct interval records; no paused time added | Repository flow covered on device; UI sequence pending |
| Return to stopped task | New session, lifetime total accumulates | Pending |
| Start task B while A runs | A's active interval closes; only B accumulates | Passed on-device repository test |
| Process killed | Tracking anchor persisted | Pending |
| Reboot | Recover with documented wall-clock fallback | Pending |
| Clock/timezone edit | Monotonic duration within boot, consistent date grouping | Pending |
| Crossing midnight | Duration assigned across relevant local days | Passed JVM tests for midnight and DST |
| Edit/delete intervals | Totals update from saved interval records | Pending |
| Search titles/descriptions/tags | Relevant task rows returned | Pending |
| JSON backup/restore | Consistent data, protected replacement, paused restored session | Passed paused-session round trip on device |
| CSV export | Rows preserve task, session and interval data | Pending |
| Widget / notification actions | Buttons apply repository operations, UI refreshes on events | Pending S23 test |
| Notification permission denied | Core stopwatch still works | Pending |
| Large history and battery | Acceptable responsiveness, no periodic CPU wakeups | Pending profile |

JVM tests cover time arithmetic and local date boundaries. Room instrumentation tests cover switching between tasks and JSON backup and restore using an isolated database.

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
- Restore replaces records in one Room transaction. A paused-session round trip passed on device. Active-session snapshots and malformed backups still need dedicated tests.
- Room schema v1 has no published migration path yet; never enable destructive fallback as a shortcut for release.

## Release definition

Only call an APK **working** after `assembleDebug` succeeds, `testDebugUnitTest` passes, critical user flows are exercised on a device, and errors or restrictions are accurately recorded. Only call the app **production-ready** after explicit device, accessibility, history/backup, and battery/performance sign-off.
