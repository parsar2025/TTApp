# Development Roadmap & Change Rules

## Scope

The original plan specifies **seven phases**. They are reflected in seven Git milestones, but “code committed” does not imply “tested/working on a phone.” Source validation and real Android verification are separate gates.

| Phase | Scope | Git commit | Current status |
| --- | --- | --- | --- |
| 1 | Room model, timer engine and atomic transitions | `2e20e3a` | Source implemented; not compiled |
| 2 | Carbon/cyan Material 3 theme | `70203f9` | Theme in source; visual QA pending |
| 3 | Stopwatch entry and on-demand check | `e21003f` | Source implemented; device QA pending |
| 4 | Home, search, session history | `f0a696b` | Source implemented; UX QA pending |
| 5 | Widgets and notification shortcuts | `0d4cd3a` | Source implemented; S23 QA pending |
| 6 | Organization, insights, backup/export | `2347d6b` | Source implemented; data roundtrip QA pending |
| 7 | Tests, toolchain and documentation | `a7f5aa6` | Test source added; tests not executed successfully |

## Priority sequence for a subsequent development session

1. **Build gate.** Finish Android SDK Platform 36 + Build Tools installation after developer accepts Google's terms; configure Gradle repository access; run `gradlew.bat :app:assembleDebug` and `gradlew.bat :app:testDebugUnitTest`. Fix actual Kotlin, Compose, Room, manifest, or resource errors.
2. **Correctness gate.** Exercise start/pause/resume/stop, task switching, active-state persistence, reboots and clock changes with automated repository/instrumentation tests.
3. **Data gate.** Test editable history, optional organization, CSV safety, JSON backup/restore (including in-progress sessions), and date-boundary calculations.
4. **Samsung gate.** Test widgets, notification permissions, locked/unlocked behavior and AOD visibility on the Galaxy S23 with relevant One UI settings.
5. **UI gate.** Verify 3-second reveal, accessibility timeouts, one-hand interactions, font scaling, search, and visual consistency.
6. **Performance gate.** Measure idle CPU, wakeups, memory and battery in a full-day session; confirm no recurring background timer.
7. **Delivery gate.** Produce and install a verified debug or signed APK, record exact Gradle/test results and document OS limitations.

## Deferred improvements (not silently completed)

- Friendly calendar/time editor instead of advanced epoch-millisecond correction fields.
- Explicit session-switch policy and stronger tests around paused sessions.
- Better empty states and responsiveness in compact widget layouts.
- Accessibility-aware elapsed reveal duration.
- Error handling / UX polish for import/export, deletions and permission denials.
- Documented Room migrations as versions evolve.
- Formal performance benchmark with thousands of tasks/sessions.

## Engineering constraints for every change

- No continuously executing stopwatch loop, background foreground service, permanent animation, or per-second widget update.
- No mandatory classification at task creation.
- No “Done,” “To Do,” “Pending” statuses or productivity scoring.
- Centralize time arithmetic and protect state transitions with database transactions.
- Prefer official platform APIs; Samsung native AOD access must be verified before a feature claim.
- Keep code changes scoped and commit each coherent improvement with tests and matching docs.
- Avoid cloud infrastructure and unnecessary dependencies.

## Recommended Git workflow

From the repository root:

```powershell
git status
git log --oneline --reverse
git switch -c feature/<descriptive-name>
# Implement + test + update docs
git add .
git commit -m "docs: ... / fix: ... / feat: ..."
```

Use the active branch policy of the repository; never publish to a remote without explicit instruction. Ensure the final commit has a clean working tree.
