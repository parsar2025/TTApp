# TTApp — Offline Time Tracker

Native Kotlin / Jetpack Compose Android project. The app records timestamp intervals rather than continuously executing a timer.

## Features

- Offline Room database for tasks, work sessions, intervals, optional projects, and labels.
- Start, pause, resume, stop; only one task may actively accumulate time.
- Carbon/cyan Material 3 UI, stopwatch, three-second on-demand duration, searchable Home and editable history.
- Optional organization, daily/weekly insights, CSV export, JSON backup/restore.
- Home-screen widget, event-driven notifications and lock-screen controls where supported.
- No background stopwatch service, periodic refresh, or network service.

## Build prerequisites

JDK 17+, Android SDK Platform 36, Build Tools 36.0.0, and Gradle 8.13 (wrapper included). The development computer has Java 21 and Gradle 8.13. Android SDK command-line tools were downloaded but SDK packages were not installed because the SDK license requires user acceptance.

From Windows PowerShell in this folder, run:

    $env:JAVA_HOME = 'C:\Program Files\JetBrains\PyCharm 2025.3.3\jbr'
    .\gradlew.bat :app:testDebugUnitTest
    .\gradlew.bat :app:assembleDebug

If these succeed, the debug APK is at app\build\outputs\apk\debug\app-debug.apk. The initial Gradle attempt could not resolve the Android Gradle Plugin; no APK or passed Android tests are claimed.

## Architecture

Models.kt defines Room entities and the DAO. TrackerRepository.kt uses database transactions for state transitions. TimeMath.kt uses the monotonic Android clock within the same boot and a wall-clock fallback after reboot. MainActivity.kt supplies the Compose UI. TrackerWidget.kt and TrackerNotification.kt update only upon events. BackupIO.kt exports/imports JSON using the system document picker.

## Caveats

The Samsung native Always-On Display cannot be directly customized with ordinary third-party Android widget APIs. Notification visibility and lock-screen actions depend on One UI settings, permissions and system restrictions. After a device reboot, the interval calculation uses wall time, potentially including powered-off time. Significant clock edits across reboots may require manual correction. JSON restore replaces current task data after confirmation and restores an in-progress session as paused. Automatic cloud backup is disabled. Future Room schema changes need explicit migrations.

## Before release

Compile the APK, run tests, test on a Samsung S23, verify Android notification and widget behavior, review accessibility, and measure battery use. Git commits document each of seven development phases.
