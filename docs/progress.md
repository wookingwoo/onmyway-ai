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

## Authorized history cleanup — 2026-10-24

- The owner subsequently authorized rewriting history and force-updating the
  affected branch. The preceding no-rewrite policy is superseded.
- Filtered all 15 generated binary paths from the local feature branch's entire
  ancestry with git-filter-repo. The current source tree stayed identical and
  SHA-256 checks confirmed that all 15 local deliverables remained unchanged.
- Prepared equivalent clean remote commits with the GitHub plugin. The main
  branch is clean and unchanged. The feature ref must be updated with a lease
  against its inspected head; remote branch and PR refs are then audited.
- Old commit IDs above are historical execution records, not the current source
  revision. Do not merge or push a clone containing the old history into this
  repository. GitHub may retain unreachable objects or cached old-SHA views;
  rewriting branch history does not establish immediate server-side deletion.

## Integration verification — 2026-10-24

- GitHub Verify run 39975910593 completed successfully for both API and Android
  after history cleanup. PR #1 was marked ready and merged into main as
  2d07cad7b6ecc919fc0969dcfe228ecf4f718994, with an expected-head guard.
- Remote cleanup audit covered all four branch/PR refs and their six reachable
  commits before integration: no excluded binary path or old contaminated commit
  remained reachable. Local stored Git blobs are at most 43,504 bytes; all 15
  deliverables retain their original SHA-256 outside Git.
- Server environment recheck: OpenAI key/model and both Kakao keys remain empty,
  as requested by the owner. Live-provider and actual route tests remain pending;
  no claim of end-to-end live operation or final submission readiness is made.

## Automotive UX branch — 2026-10-24

- Owner requested a separate branch, a much simpler in-car flow consistent with
  the Pleos shell, and a completed review PR with actual screenshots. Work is on
  `feat/automotive-ux`; main is reserved for the owner's later review. The owner
  explicitly authorized selected actual screenshots in Git, superseding the
  blanket screenshot exclusion above. APKs, recordings, office files, archives
  and raw test evidence remain local.
- Replaced the dense two-pane form with INPUT → REVIEW → ROUTE and separate
  Settings. Neutral day/night surfaces follow the system; the main flow uses
  large text, 80dp actions and one fixed primary button. Coordinates, server
  details, ISO timestamps and evidence are secondary. Native five-minute dwell
  steppers and an explicit date/time editor avoid small numeric inputs.
- Added pure JourneyFlow and JourneyDrafts behavior tests. New stage tests were
  observed failing before implementation and passing after implementation.
  Existing revision/freshness, mandatory-errand, arrival/completion and separate
  final-home approval behavior remains in place.
- Fresh independent AI review found three P2 issues: the time-only editor could
  retain yesterday's deadline, asynchronous navigation errors disappeared when
  the active route cleared, and a new live session could inherit fixture
  coordinates. Added failing regression coverage, fixed all three, and verified
  GREEN. The footer retains navigation errors independently of activeId; date
  selection changes the date explicitly; leaving fixtures clears sample location
  and deadline drafts while preserving real input/server configuration.
- Actual-device iteration caught a fragile numeric dwell edit and an invisible
  neutral date button in the platform time dialog. Replaced them with large
  dwell steppers and a native number-wheel dialog with a visible calendar
  button. A Kotlin NumberPicker property-shadowing compile error was corrected
  before the final build. These failed intermediate attempts are not counted as
  successful verification.
- Final `scripts/build-android.ps1 -Release`: exit 0. Demo/live debug and signed
  release builds succeeded; v3 signatures verified. Gradle XML reports contain
  21 tests, zero failures/errors: SessionState 7, PlanPayload 2,
  NavigationPolicy 4, JourneyFlow 5 and JourneyDrafts 3. Final APK SHA-256 values
  are recorded in `artifacts/submission/apk-checksums.txt`; APKs remain ignored.
- Final Python run: `python -u -X faulthandler -m pytest services/api/tests -q`,
  **25 passed in 1.51s**, exit 0. An earlier restricted-shell invocation produced
  no output and was stopped; the same suite completed in the normal local
  execution context. No backend source change or live-provider call was needed.
- Both final signed APKs installed on official Pleos Connect 2.1.0, API 34,
  isolated `OnMyWay_Pleos` / emulator-5556. Final installed-demo
  `smoke-android.py --full-journey`: exit 0. Actual localhost fixture, three-stage
  flow, hidden technical settings, pharmacy dwell 10→15 minutes, settings return,
  expected home ETA 18:45 / deadline 19:00, three individually approved simulated
  destinations/arrivals, explicit completion/replans and separately approved
  final home all passed.
- `--boundaries-only`: exit 0. Four sample coordinate fields were empty after
  fixture reset; the visible native calendar changed the saved deadline from
  October 23 to October 24. The script left a real task editor open.
- `smoke-pleos-lock.py`: exit 0. Official Virtual Console Drive / 30 km/h caused
  actual restrictions, no enabled editing/destination actions, and restored
  Park / 0 in cleanup. A subsequent actual UI dump confirmed the editor had
  been dismissed by the app and review was usable again after parking.
- Day/night screenshots were visually inspected. Night mode was restored to
  light. Six actual selected captures under `docs/screenshots/automotive-ux/`
  include the historical before UI, input, review, next stop, night and actual
  restriction overlay. No entrant identity, keys or account details are visible.
  Raw logs/XML/JSON remain in ignored `artifacts/evidence/automotive-ux/` and
  `.tools/automotive-ux-*.log`.
- Prior recording, office/pitch files and local package ZIP predate this UI
  redesign. They are preserved and explicitly labelled historical; regenerate
  them before sharing as current materials. Live providers, actual SDK route
  acceptance/arrival, physical vehicle usability and measured UX outcomes remain
  unverified. This branch does not establish contest submission readiness.
