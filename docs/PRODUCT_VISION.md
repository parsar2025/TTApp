# Product Vision & Requirements

## The problem

Traditional stopwatches invite users to watch seconds tick, forget to record what the time was for, or lose context when the app is closed. Heavy task managers add deadlines, statuses, project setup and administrative work. TTApp is meant to remove both distractions.

**Product promise:** A personal stopwatch with a memory.

> Start working → Put the phone away → Check elapsed time when curious → Pause or stop → Keep a reliable record.

TTApp should be less demanding than a task manager and more useful than a basic stopwatch.

## Primary use case

A user on a Samsung Galaxy S23 starts “Reading papers,” sets down the phone, works for hours, and returns to check elapsed session time. They may pause for a break, resume, or stop. Later they can reopen “Reading papers” and record another session. The task remains available without any “Done” step.

## Non-negotiable principles

1. **Timestamp-driven, not timer-driven.** Capture clock anchors at state transitions and derive elapsed time only when needed. No continuous ticking loop, foreground stopwatch service or wake lock.
2. **Zero friction.** A title is enough to start. Description is optional; projects, categories, labels and colors are never prerequisites.
3. **Offline-first.** Local storage, no account, no sync requirement and no backend.
4. **Accurate and durable.** Persist sessions across app termination; use monotonic time within one boot; explain reboot/clock limitations.
5. **Only one active accumulator.** Starting another task must stop accumulation on the previous one without double counting.
6. **No task-manager overhead.** No mandatory completion, priority, due date, workflow status, productivity gamification or streaks.
7. **Calm interface.** Dark carbon + cyan; no continuously moving time display.
8. **System-native where possible.** Respect Android notification and widget APIs; do not fabricate direct Samsung AOD integration.
9. **Control and portability.** Inspect/correct history, export CSV and locally back up/restore.
10. **Performance before decoration.** Lightweight UI, low idle CPU activity and minimized battery impact.

## Mental model

- **Task:** A reusable named activity, not a to-do item.
- **Session:** One visit to that activity (from Start until Stop). A session may include pauses and resumes.
- **Interval:** One uninterrupted measured block within a session. Pausing closes the current interval, resuming opens another.
- **Project/category:** Optional organization assigned now or later.
- **Label:** Optional searchable tag, also assignable later.

Example: “API integration” can have three sessions this month; the second session can contain two intervals because the user took a break. Task lifetime total is the sum of all its recorded intervals.

## Must-have experience

- Two primary tabs: **Stopwatch** and **Home**.
- Stopwatch: editable task title and optional description, session start indication, **one Start / Pause / Resume control**, separate **Stop**, and **Check Time**.
- On-demand elapsed time in `HH:MM:SS`; hide again after roughly three seconds (respect accessibility timing when implemented).
- Home: compact task rows, lifetime totals, search across title and description (and optional organization), task reuse, contextual history.
- Session history: dates, intervals, notes and correction/removal tools.
- Native home-screen widget and lock-screen notifications as lightweight complements, not permanent timers.
- Local CSV export and JSON backup/restore with explicit confirmation before replacing data.

## Explicit non-goals

No cloud, collaboration, calendar scheduling, pomodoro alarms, automatic activity detection, mandatory grouping, to-do workflow statuses, persistent seconds display, ad/analytics SDK, high-frequency live widget refresh, or custom always-on overlay.

## Success criteria

- Starting a fresh task feels nearly instantaneous.
- The user can leave the phone unused while elapsed time is accounted for.
- Pause/resume/stop are distinct, predictable, and non-destructive to task history.
- A long history stays searchable and correctable.
- The CPU does not work continuously simply because a task is running.
- S23 lock-screen access is as minimal as publicly supported Android/One UI allows.

**Verification status:** The design and source implementation aim at these outcomes; compile success, device behavior, durability and battery performance are pending real tests.
