# Core journey readiness implementation plan

> For agentic workers: use superpowers:executing-plans inline. One fresh whole-branch review after implementation.

**Goal:** Remove raw coordinate setup from the normal native journey and align
contest materials with actual evidence and the organizer briefing.

**Architecture:** Retain native Kotlin Views and FastAPI. Add a pure location
setup policy/model, a bounded Pleos location adapter and a dedicated setup UI.
Reuse `/v1/places`; no new provider or geocoder. Existing edit/approval boundaries
own invalidation. Fixtures remain independent.

**Tech stack:** Kotlin/JUnit, Pleos FusedLocation (verified published signatures),
Android API 35 build / official Pleos API 34 runtime; Python pytest.

**Spec:** [Approved design](../specs/2026-11-03-core-journey-readiness-design.md)

## Global constraints

- Korean UI/contest prose; English source and engineering docs.
- Foreground/native-Parked only; no fixture/simulation or active/unresolved guidance mutations.
- Source timestamp age <=60s, future tolerance <=5s, finite valid coordinates; timeout 10s.
- Cancellation invalidates ownership before SDK cleanup; permission return grants no authority by itself.
- Explicit home confirmation; keyword places only, advanced manual fallback explicit.
- Every location save invalidates prior plan/approval; no stale post-completion origin overwrite.
- Keys server-only and deferred. Generated deliverables local. No external grants/publication/submission.

## Review focus

- Permission grant after background/Drive, stale SDK callbacks and duplicate results cannot save state.
- Restored coordinates lack freshness unless their explicit source metadata supports it.
- Fixture reset cannot overwrite a separately confirmed real home.
- Place-search response must belong to current setup revision and cannot mutate after guidance starts.
- Setup/map failure is visible and never masquerades as live success or a fixture replacement.

## Task 1: Source and policy boundaries

- [x] Verify briefing transcript, official rules and historical evidence; reconcile source drafts.
- [x] Inspect FusedLocation artifact/API and toolchain without secrets; resolve version/timestamp semantics.
- [x] Add failing JUnit tests for fresh/stale/invalid fixes, attempts/cancel, save gates, persistence and fixture isolation.
- [x] Run RED, implement `LocationSetup.kt`, rerun GREEN and existing native safety suite.

## Task 2: Native setup and adapter

- [x] Add bounded `PleosLocation.kt` adapter, verified dependency and dangerous permission declaration.
- [x] Integrate native setup into MainActivity: source labels, explicit apply/home confirmation,
  keyword-place selection, and guarded advanced fallback. Recheck all gates at result/save time.
- [x] Preserve real-home metadata separately from journey/fixture drafts; enforce device-origin freshness at calculation.
- [x] Verify targeted behavior and both unit/build variants; fix observed failures with regression tests.

## Task 3: Actual validation and delivery

- [x] Run Python suite and synthetic comparison; Android both unit/debug/release/lint checks.
- [x] Check official AVD availability and exercise actual supported location/permission/lifecycle paths.
  Investigate native map 401 with supported diagnostics. Record unresolved platform/provider limits.
- [x] Update README, readiness, submission drafts and docs/progress.md with actual final evidence.
- [x] Fresh whole-branch review; fix material findings with RED/GREEN evidence; diff/link checks.
- [x] Commit feature branch. No push/merge/publication or final submission is implied.

## Execution ledger

- 2026-11-03: User approved design. Reused isolated worktree, created requested
  `feature/briefing-readiness` from `0dc426e`. Builder review clarified keyword
  search limits and mutation blocking during all active/unresolved guidance.
- Documentation check before product changes: 73 relative links across 14 files
  resolve; `git diff --check` clean. Pure synthetic comparison 3/3 passed.
- Task 1 complete: published FusedLocation1.1.3 AAR inspected; source field is
  timestampMillis. Initial eight location tests failed against unimplemented
  methods, then passed. Existing safety baseline106 passed. An interpretation
  regression failed before fixing it to preserve newly configured live origin.
- Task 2 complete: native UI/adapter/permission integrated; actual official AVD
  fresh location, explicit saves, denied permission/home preservation, stale save
  rejection and Drive30/Park0 confirmed. Final policies add10 tests to baseline.
- Final review: fresh reviewer found two Important issues, missing provenance
  treated as manual and completed-stop origin reused for a new request. Both
  failed focused regression tests before fix; full demo/live suites116/116 pass.
  SDK/AVD reproduction and evidence prose excluded by reviewer were checked by
  implementer; deferred live-provider checks follow the user's keys-later scope.
  No supported native-map authentication remediation was established.
- Actual UI corrections: permission result status survives callback-before-resume;
  completed/cancelled acquisition no longer leaves its progress message active.
  Final API67 and standalone safety116 pass. Final source build/lint and exact
  output counts are in docs/progress.md. No public posting or access grants.
- Remaining external validation: real final STT, actual home-place/provider calls,
  map rendering and real route acceptance/arrival. See docs/location-setup.md for
  the explicit acceptance procedure. No result is substituted with a fixture.
