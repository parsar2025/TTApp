# TTApp documentation

This directory is the product and engineering source of truth for **TTApp (Time Tracker App)**, a minimalist, offline-first Android tracker built around recorded timestamps rather than a constantly running stopwatch.

**Start here:** [Product Vision](PRODUCT_VISION.md) to understand *why* the app exists; [User Flows](USER_FLOWS.md) for *how* it behaves; [Design System](DESIGN_SYSTEM.md) for *how* it should feel.

| Document | Covers |
| --- | --- |
| [Product Vision](PRODUCT_VISION.md) | Purpose, principles, scope, non-goals, success criteria |
| [User Flows](USER_FLOWS.md) | Tasks, sessions, pause/resume/stop, search, backup and system shortcuts |
| [Design System](DESIGN_SYSTEM.md) | Carbon/cyan palette, two-tab navigation, layout and accessibility principles |
| [Architecture](ARCHITECTURE.md) | Kotlin/Compose/Room modules, data model, time calculations and event-driven integration |
| [Development Roadmap](DEVELOPMENT_ROADMAP.md) | Seven original phases, implementation status, next work and guardrails |
| [Testing & Limitations](TESTING_AND_LIMITATIONS.md) | Test matrix, known gaps, build prerequisites, Samsung/AOD restrictions |

The repository's top-level [README](../README.md) provides the quick project overview, commands, status, and links to these documents.

## Document status

These documents describe the **intended product** and the **source currently present in the repository**. The source files were committed in seven phases, but the Android app has **not yet passed compilation, automated tests, or device validation**. Statements about implemented features refer to source implementation, not verified production operation. See [Testing & Limitations](TESTING_AND_LIMITATIONS.md).

## Maintaining the docs

If behavior changes, update the corresponding flow, system diagram or product requirement in the same Git commit as the code. Keep architectural invariants (offline operation, one active accumulator, timestamp arithmetic, optional grouping) explicit. Distinguish confirmed behaviors from Android or Samsung capabilities that remain unverified.
