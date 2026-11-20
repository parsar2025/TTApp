# Design System — Carbon / Cyan

## Experience statement

TTApp should feel like a **premium minimalist stopwatch**, not a productivity dashboard. Its visual calm is functional: users should be able to start and forget about it.

Keywords: **dark carbon, cyan precision, restrained glassmorphism, spacious, quiet, native Android**.

## Color tokens

| Token | Color | Use |
| --- | --- | --- |
| Carbon | `#101417` | Main background |
| Graphite | `#1C2429` | Card and navigation surfaces |
| Raised | `#253138` | Raised/variant surfaces |
| Cyan | `#00D9E8` | Primary actions, focus, timing feedback |
| Soft white | `#E7F0F2` | Primary text |
| Muted | `#9EADB5` | Supporting text |
| Outline | `#425157` | Low-contrast boundaries |
| Error | `#FF818B` | Destructive/error emphasis |

Current source: `ui/Theme.kt`. Dark by default even if the system is set to a light theme.

## Visual rules

- Use simple rounded rectangles and generously sized touch targets; primary action height around 58dp, secondary around 54dp in the current Compose screen.
- Prefer static dark surfaces with faint cyan strokes to costly real-time blur. Glassmorphism should be **suggested**, not simulated with GPU-intensive overlays.
- Primary accent should remain sparse and legible; never turn the interface into a neon dashboard.
- Avoid gradients, hero illustrations, lengthy transitions, looping animations or dense multi-panel analytics.
- Clear hierarchy: task title first, operational state second, elapsed time only when requested.
- Keep navigation to **two primary tabs**; history and Insights are subordinate.
- Use standard Android/Material 3 typography and iconography with readable contrast.
- Avoid introducing visual badges that look like completion states or progress judgments.

## Screen 1 — Stopwatch

Conceptual layout:

```text
┌──────────────────────────────────┐
│ STOPWATCH                        │
│ Tracking quietly / Paused        │
│                                  │
│ [ What are you working on?     ] │
│ [ Description (optional)       ] │
│ Started 13:20                    │
│                                  │
│                                  │
│     quiet empty visual area      │
│      [ Check Time ]              │
│   (01:15:00 briefly revealed)    │
│                                  │
│ [       Start / Pause       ]    │
│ [       Stop session        ]    │
│           New task               │
├──────────────────────────────────┤
│  Stopwatch          Home         │
└──────────────────────────────────┘
```

The combined action shows **Start**, **Pause**, or **Resume** according to operational state. **Stop session** is a separate action. A three-second duration reveal should disappear without leaving a per-second UI update running. For accessibility, a future implementation should respect user-preferred timeouts.

## Screen 2 — Home

- A compact searchable list of tasks (not a kanban, agenda or to-do board).
- Row: title, truncated optional description, total accumulated time, history access.
- Tapping a row selects that existing activity for reuse.
- Search is local, immediate and includes title, description, project names and labels.
- Empty-state text should encourage starting the first task, not setting up a project.

## Contextual screens

- **History:** per-task sessions and recorded intervals, correction and notes, optional project/label assignment.
- **Insights:** simple totals and daily summaries; deeper data presentation must never compete with the stopwatch.
- **Portability:** CSV and local JSON actions with clear explanation before destructive restore.

## Widgets and notifications

- Match carbon/cyan identity where Android's `RemoteViews` theme permits.
- Show task name and session start/status rather than a live chronometer.
- Expanded actions: Pause/Resume, Check, Stop; they must act on the actual persisted tracking state.
- AOD behavior is a **supported-platform question**, not a design license to draw overlays on the screen.

## Accessibility & performance

- Minimum interactive targets 48dp where feasible, support dynamic font scaling and screen readers.
- Respect system animations and user notification privacy settings.
- Validate text wrapping on Samsung S23 and other small layouts.
- Keep contrast sufficient for outdoor/low-brightness viewing.
- Do not poll time every frame, second, or via continuous Compose recomposition.

**Status:** This is the intended design direction. The source currently implements a simplified Compose/Material 3 expression of it; visual QA on an S23 is pending.
