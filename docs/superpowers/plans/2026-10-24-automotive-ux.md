# Automotive UX implementation plan

> **For agentic workers:** Use superpowers:executing-plans in this session. Preserve existing API and safety behavior.

**Goal:** Replace the dense form with a simple Pleos-compatible native journey.

**Architecture:** A small pure journey-stage controller and native view builders
present one stage at a time. Existing SessionState, provider, revision/freshness
and navigation adapters remain authoritative.

**Tech Stack:** Kotlin Android Views, existing Gradle/JUnit and FastAPI.

**Spec:** docs/superpowers/specs/2026-10-24-automotive-ux-design.md

## Global constraints

Work on feat/automotive-ux in the requested existing checkout. Keep generated
deliverables local except owner-authorized selected PR screenshots. No backend
changes, provider calls or new dependencies. Retain
driving locks, explicit fixture disclosure and arrival/completion separation.

## Review focus

- Returning from settings must retain the previous journey stage.
- An edit must invalidate route approval and return a stale route to review.
- A restricted vehicle must hide editors and disable dialogs' positive actions.
- Restored live data cannot restore navigation approval or a stale plan.
- Completing an errand must lead to remaining-task review, including final home.

### Task 1: Staged native UI

Files: JourneyFlow.kt, JourneyDrafts.kt, their behavior tests, SessionState.kt,
MainActivity.kt, day/night styles, smoke scripts and selected PR screenshots.

- [x] Add behavior tests for parsed -> review, calculated -> route, edited route
  -> review, settings/back and empty/restored stage selection; observe RED.
- [x] Implement the pure JourneyFlow transitions and verify GREEN.
- [x] Replace view presentation with adaptive neutral palette, large native
  controls and stage-specific content/footer. Preserve service and safety logic.
- [x] Build and run full Android/Python suites. Actual: 21 Android / 25 Python pass;
  demo/live debug and signed release builds succeed, v3 signatures verified.
- [x] Install signed releases and inspect actual Pleos UI at each stage. Exercise
  edit/invalidation, settings return, simulated completion and real driving lock.
- [x] Record current evidence, commit, push a new branch through the plugin and
  create a review PR. Keep main unchanged pending the owner's visual review.
  Actual: PR #2 open/unmerged, screenshots rendered and both CI jobs passed.

Verification details: docs/progress.md, Automotive UX branch — 2026-10-24.
