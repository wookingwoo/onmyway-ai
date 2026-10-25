# Pleos navigation handoff implementation plan

> Execute inline using executing-plans and TDD, with one fresh whole-branch review.

**Goal:** After approval and actual route acceptance, show the official Pleos
navigation screen, preserving the errand flow and clear failure/recovery states.

**Architecture:** Keep NaviHelper3.0.5 and its separate route/screen APIs. A pure
handoff gate binds one automatic launch to the approved revision/destination.
Opening a screen cannot grant route authority or revoke accepted guidance.

**Tech stack:** Native Kotlin/AAOS, existing FastAPI, official NaviHelper.
**Approved design:** Owner selected the proposed Pleos native-screen approach
and authorized feature branch, incremental commits and screenshot-bearing PR.

## Global constraints

- Reuse isolated worktree; new Git Flow feature/pleos-navigation-handoff from
  verified main2876838. No changes to main or earlier merged feature history.
- Korean UI; English engineering docs/commits. No key provisioning or client key.
- Actual native Park and current fresh-plan approval remain required. No fixture
  destination ever reaches real route/screen handoff through simulated acceptance.
- Arrival is not completion; required errands and separately approved home remain.
- No embedded/fabricated map or new map-provider dependency. Screen-request dispatch
  alone does not establish visible navigation UI or actual route acceptance.
- No unsolicited foreground after background/cancel/revision change/restriction.
- No public video/access grants/submission. Only actual screenshots in Git.

## Review focus

- Arrival/cancel or a different current revision/destination invalidates a queued launch.
- Duplicate acceptance cannot reopen navigation repeatedly; manual retry is explicit.
- Screen failure must not clear accepted route state or cancel native guidance.
- Normal onStop/return must preserve the session, while recreation/process death
  never restores route approval or silently permits replacement of uncertain guidance.
- A disconnected/unavailable navigation app and missing callbacks show truthful,
  bounded recovery rather than simulated success.

## Task1 — guarded handoff policy

Files: create NavigationHandoff.kt and NavigationHandoffTest.kt.
- [ ] Write RED tests: live current accepted next stop/home once; fixture, stale,
  background, non-Park, canceled/arrived and duplicates denied; manual retry gated.
- [ ] Implement minimal gate; run native unit suite; commit policy.

## Task2 — actual SDK screen connection and native recovery

Files: PleosNavigation.kt, MainActivity.kt, AndroidManifest.xml; focused policy tests.
- [ ] Inspect installed Foreground signature and official permissions.
- [ ] Add NAVI_CUSTOM_MAP; expose screen request separately from route request,
  return errors without route cancellation and never label dispatch as visible.
- [ ] Arm after current approval, consume after actual accepted callback, persist
  before switching and stop app speech/input. Add explicit map/retry action.
- [ ] Invalidate pending automatic launch on background/edits/cancel/restriction.
- [ ] Preserve normal background callbacks; add conservative persisted interrupted
  guidance hint on recreation, explicit map-only recovery and acknowledged reset.
- [ ] Unit/debug/release and Python checks; commit native integration.

## Task3 — official AVD evidence, review and PR

- [ ] Inspect actual installed navigation availability and exercise real SDK screen
  dispatch without sending synthetic destinations. Keep actual accepted-route tests
  distinct from mocked tests; record limitations if platform prevents real routing.
- [ ] Exercise selected sample full errand/home flow and actual Drive restriction;
  verify sample never calls native map handoff. Capture truthful actual screenshots.
- [ ] Fresh branch review; fix material findings; record actual evidence/progress.
- [ ] Publish incremental feature commits; create PR against main, attach it,
  include actual screenshot images and verified CI. Keep APK/raw media local.

References: https://document.pleos.ai/api-reference/connect-sdk-pleos/NaviHelper/intro
and https://document.pleos.ai/api-reference/connect-sdk-pleos/NaviHelper/declaring-permissions
