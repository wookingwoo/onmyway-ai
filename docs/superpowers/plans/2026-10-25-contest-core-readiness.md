# Contest core readiness implementation plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans or scoped independent workers for this authorized execution. Track RED/GREEN evidence and commits in the local ledger.

**Goal:** Make exact venue selection, honest availability review, current-time approval and recoverable continuation concrete and reviewable without provider keys.
**Architecture:** Keep FastAPI stateless and Kotlin native. Extend discovery-specific models, isolate pure timing/lifecycle policy, and integrate editors into the existing review/route screens.
**Tech Stack:** Python/FastAPI/Pydantic/HTTPX; Kotlin/Android Views/Pleos SDK.
**Spec:** docs/superpowers/specs/2026-10-25-contest-core-readiness-design.md

## Global Constraints
- Korean UI and contest prose; English source, engineering docs and commits.
- No keys in Android/source/screenshots; user adds server-only ignored environment configuration later.
- Unknown data cannot establish feasibility. Mandatory errands, road travel, waiting, dwell, deadlines and final home leg remain enforced.
- Require approval for the fresh current plan; actual vehicle state fails closed. Arrival is separate from completion.
- Keep generated deliverables local; only actual selected screenshots under docs/screenshots may be committed.
- No public publication, access grants, identity/signature, email submission or fabricated live/user results.

## Review Focus
- A specific pickup venue must survive completion/replanning and query changes must invalidate its evidence.
- Unknown/stale/future availability never turns feasible; uncertain extension stays conditional.
- Network delay and parked review delay consume short deadline slack.
- A timeout or cancellation must invalidate pending callbacks and prevent accidental completion.
- Auto-refresh failure must leave completed IDs intact and never approve the next destination.

### Task 1: Exact venue discovery and honest evidence contracts
**Files:** services/api/onmyway/{models,providers,planner,main}.py; services/api/tests; contracts.
**Interfaces:** DiscoveryErrand.selected_place, Place.address/availability_evidence, POST /v1/places, conditional DeadlineRelaxation.
- [ ] Add failing tests for top-three search, pinned ID retention/search bypass, fixture rejection, stale/future/missing evidence and conditional extension.
- [ ] Implement the contracts and provider/solver changes; keep parse schema unchanged.
- [ ] Run the complete Python suite and regenerate OpenAPI/examples.
- [ ] Commit the independently reviewable backend change.

### Task 2: Venue selection and availability review in the native app
**Files:** new PlaceReview.kt / native dialog helper; MainActivity.kt; pure unit tests.
**Consumes:** Task 1 contracts.
- [ ] Add failing pure tests for selection/provenance isolation, query invalidation, remaining discovery payload and persistence.
- [ ] Add explicit place selection and basis/time/service review; preserve picked places over replanning.
- [ ] Gate live planning until each remaining venue is explicitly selected; keep existing labelled example route working.
- [ ] Run both Android unit variants and build; commit.

### Task 3: Time-aware approval and recoverable navigation
**Files:** NavigationPolicy.kt; new PlanTiming.kt / NavigationRequestState.kt; PleosNavigation.kt; MainActivity.kt; tests.
**Interfaces:** Pure timing bounds from request/result and pure request lifecycle with generation/timeout/cancel.
- [ ] Write and observe failing tests for computation/review delay, tight home/errand/closing slack and unknown timestamps.
- [ ] Implement conservative current-time approval and clear margin/recalculate UX.
- [ ] Write and observe lifecycle timeout/cancellation/stale-callback tests; implement explicit recovery.
- [ ] Auto-refresh after explicit completion without auto-approval; retain completed state on failure.
- [ ] Run both unit variants and builds; commit.

### Task 4: Actual UI evidence and reviewable delivery
**Files:** scripts smoke checks; docs/progress.md/readiness.md; docs/screenshots/contest-core; relevant README/demo instructions.
- [ ] Run full Python tests and both Android unit/build/lint checks on final code.
- [ ] Install/run only on the isolated official AVD, capture changed review/uncertainty/recovery screens and verify full example home flow and actual driving lock.
- [ ] Record actual evidence and deferred live-provider/STT/physical-vehicle/user-study scope; no unperformed claims.
- [ ] Obtain fresh whole-branch code review, fix actionable findings, commit.
- [ ] Push the feature branch and create/attach PR with clear problem/behavior/validation, limitations and rendered actual screenshot links.

