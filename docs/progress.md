# Delivery ledger

Plan: docs/superpowers/plans/2026-10-23-onmyway.md

- 2026-10-23: Empty repository verified. No existing code or tests. Python 3.13, Node, gh and ffmpeg present; Android SDK/JDK not found in conventional locations.
- Ruling: Work on a feature branch in the existing empty checkout. A linked worktree cannot be based on an unborn main branch; avoid redundant setup before the first project commit.
- Ruling: Proceed autonomously under the user's explicit delegation and fast-delivery preference; no design confirmation gate.
- Ruling: User will add API keys later. Implement configuration and HTTP-mocked tests now; do not provision keys or claim a live AI test.
- Pre-flight: Backend and app share JSON schema. Backend implementer must publish example request/response early; native app must consume that contract rather than invent a second one.

## Verified implementation

- Task 1: complete for configurable implementation. Python suite: 25 passed in 0.72s; `pip check`: no broken requirements. HTTP fixture smoke passed. OpenAI/Kakao adapters tested through mocked transport only; no paid/live provider calls.
- Task 2: native implementation, unit/build/install/demo verification complete; actual live route acceptance/arrival pending. Gradle 8.9 / JDK 17 / Android compile 35, min 28. Final Gradle unit reports: SessionState 7, PlanPayload 2, NavigationPolicy 4, all zero failures/errors. Both signed release APKs built and verified.
- Official Pleos Connect 2.1.0 x86_64 image SHA1 `b4f858d5bcc6c71506d7edfc872bcb9e5a11acaa` verified, isolated AVD emulator-5556 cold-booted. Actual native app installed and launched, CarUxRestrictions state and NaviHelper service connection confirmed.
- Actual official Virtual Console session: Drive / 30 km/h locked editing/navigation approval despite demo override; cleanup confirmed Park / 0 km/h. Evidence: artifacts/evidence/pleos-driving-check.json.
- Actual release-app fixture smoke passed: source-labelled HTTP fixture -> three-stop timeline -> explicit simulated destination approval -> simulated arrival NOT completion -> user completion -> two remaining stops replanned. Actual 47.206-second screen recording retained and disclosure-captioned.
- Fresh independent AI code review found stale restored plans could be navigated. Fixed with live restore invalidation, five-minute freshness and deadline guards at dialog open AND approval. Four policy tests verified. SDK callback ordering is buffered both ways; real route event behavior remains unverified.
- User explicitly approved both Android SDK and Pleos license acceptance/installation/use. API credential creation remains deferred at the user's request.
- Task 3: working application and pitch artifacts created and rendered/inspected. Final identity/signature/legal consent and final email submission remain user actions.

## Environment rulings and pending actions

- JDK 17 Windows NIO failed on MSIX temporary sockets. Verified Gradle workaround: legacy WindowsSelectorProvider and a short writable Windows TEMP/TMP.
- Gradle's transform rename failed in the synchronized workspace cache. Build succeeded with a separate short local Gradle cache and `--no-watch-fs`. No machine-global Java or Android installation changed.
- GitHub CLI lookup/creation failed under a different restricted credential. Treating that as proof of repository absence was incorrect. The user clarified the existing repository; the GitHub plugin confirmed it is private and grants read/write/admin access. No new repository or external grants were created. Use the plugin for upload/PR/CI; ignore the superseded repository-creation question.
- Local feature branch: feat/core-experience; bootstrap d13e992, backend 15b5a87, native/release a870de6. Do not mark the whole goal complete while live integrations and submission conditions remain unverified.
- GitHub plugin upload completed: draft PR #1, remote commit 249ecad924660465b15cf4c893656e4f2cfb0943. Its tree exactly matches local commit 7177670dbb85e3084aaf7ffad4cf9728f77e00f4: 0b2c426f5b043778aafb5f0e2fe46575854d1bb8. GitHub API ancestry differs from local bootstrap history; local history remains preserved.
- GitHub Actions Verify run 39972136674: api and android jobs both completed successfully. No live provider secrets were supplied to CI.
- Anonymous local review package created and archive/hash checks passed; actual API credentials, route acceptance/arrival and human submission steps remain pending. Package is excluded from Git to avoid duplicating APKs/video; it can be regenerated with scripts/package-submission.py and a private OMW_ENTRANT_ACCOUNT scan value.

## Local artifact policy — 2026-10-24

- User requested avoiding large GitHub uploads. Generated APKs, recordings,
  screenshots, office documents and archives are excluded from Git; local files
  are preserved. Text sources, scripts and evidence notes remain versioned.
- Removed the CI APK upload step. Build and unit-test checks remain enabled.
- Existing commits are preserved; removing current tracking does not purge
  binaries already present in earlier Git history. No history rewrite was performed.
