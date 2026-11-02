# Delivery ledger

## 2026-11-03 — replace illustrated edits with actual native Pleos recordings

The owner clarified that product screens must be recorded directly on Pleos.
The preceding illustrated-interface film is superseded, not a native recording.
Added an isolated `film` flavor/package `ai.onmyway.car.film`; live/demo launchers
are unchanged. Native FilmActivity renders real Android Views/Canvas and buttons
inside official Pleos Connect2.1.0/API34. It is an authorized authored filming
prototype, with clean product wording as expressly requested. It does not call
live AI/STT/traffic/navigation providers, and its route illustration and arrival
signals do not establish production navigation success. Editorial ElevenLabs
voices and captions are reused without another paid generation.

Booted the existing official OnMyWay_Pleos AVD without wiping it, installed only
the separate film debug APK, and operated13 real UI stages. Recorded Android
screenrecord raw footage from the2560x1440 display at1920x1080. Native vehicle
panel/status/climate bars remain visible in every main-film scene. Short product
shots are whole-display inserts or crops of these same actual captures, with
original comic bumpers outside product screens. No illustrated cockpit pixels
replace native footage. Raw files, UI XML, snapshots, manifest, APK and both final
films stay local under ignored artifacts/final-films/pleos/.

Vehicle gate is the real CarUxRestrictions service, with UNKNOWN/RESTRICTED
blocked. Bundled Virtual Console runs inside the AVD on8088; forwarded that port,
registered an owned heartbeat session, set real Drive/30, captured the actual OS
distraction blocker, then restored speed0/Park and closed the session. Shell
CarService injection is unsupported on this user build; no root or restriction
bypass was used. Arrival remains incomplete until explicit receipt confirmation;
both mandatory laundry and medicine precede separate home approval. No live/demo
app data were cleared or real navigation destinations sent.

Regression checks: new film policy tests were RED before implementation, then
119 demo +119 live +119 film unit tests passed,0 failures/errors. All three debug
APK builds passed (129 Gradle tasks,43s). After visual waveform sizing, film
unit/build rerun passed (43 tasks,19s). Existing source/SDK/window-API/native-symbol
warnings remain. Python API67 passed/0.84s with provider HTTP mocked. Media40
passed/0.223s, including native manifest/hash,1x trim and restricted-window tests.
CI now also compiles/tests the film flavor and runs the pure native-media tests.

Independent whole-source review found two edit timing defects: a2s Short approval
window ended before the real tap, and the initial driving trim used a late
post-snapshot Park timestamp. Shifted Short approval to1.8–3.8s (decoded actual
guidance visible at2.8/3.8) and main home approval to1.2–4.2s. Current driving raw
SHA903fb7248c95b4d925f9ffad767d8e8e47968dc1af77298e6682974fe97fe85d has a reviewed
locked interval11.2–19.8s; selected main11.2–19.7s. Start/middle/end independently
decoded as actual OS lock with D/30. Missing/short reviewed intervals now fail
closed, with a RED→GREEN regression. Future recorder directly records its
post-Drive-ready/before-Park timestamps. Re-review found no important open issue.

Final native film/Short hashes, complete-decode checks, actual final-frame review
and full unmuted playback evidence are in docs/submission/final-films-verification.json.
Delivery paths and current scope are in docs/submission/final-films-ko.md. PR11
is updated to the native result; media remain excluded and no merge/publication/
external access grant/final contest submission is performed.

## 2026-11-03 — final product film and viral Short

User explicitly delegated conception, premium ElevenLabs speech generation,
polished presentation-only screens, local-only videos and a feature PR from main.
Verified remote main50c74a759c7e8d94b55989f86cb1d7524116ac2a through the connected
GitHub API, then created feature/final-contest-films in this existing worktree.
Rechecked official submission/rules/judging/Shorts pages. First-round mockups and
prototypes are accepted; Short requires vertical30–60s and designated hashtag,
with an independent award and no main-award link bonus. No public upload or
contest submission performed.

Produced original illustrated cockpit art and a single consistent errand scenario
for the main film. On subsequent owner direction, the Short became an independent
quest/milk comedy rather than a cropped main film. Main uses Min-woo/Minjun/Yun;
Short uses KKC/Minjun/Yun/Aria. Actual local-key voice listing and paid timestamped
speech generation succeeded. User endpoint401 was missing user-read permission,
not an invalid key; voices and generation worked. Three short main-role auditions
and19 final main +15 final Short utterances were generated. Earlier12 Short
utterances remain local superseded takes. Model:eleven_multilingual_v2. Original
synthesized chord/pulse bed and UI tones; no borrowed music or voice cloning.

Scene arithmetic counts travel, opening wait, dwell and home: depart17:50,
laundry18:05 before18:30 cutoff; baseline home18:40. Changing pharmacy dwell5→30
produces home19:05 with milk, or18:53 when only optional milk is omitted. Mandatory
laundry/medicine, approval after current revised proposal, driving lock,
arrival/completion distinction, explicit medicine completion and separate home
approval remain visible. Rendered screens, schematic roads and authored times are
editorial prototypes, not live provider data or Android footage. ElevenLabs
assistant audio does not establish native Pleos TTS or live Korean STT success.
No product/runtime feature or successful deployment is claimed by this change.

Final local files (all ignored, never staged):
- Main:artifacts/final-films/main/OnMyWay_AI_Product_Film_Final.mp4;
  133.5s,1920x1080,CFR30,4005 frames,H.264/yuv420p,AAC stereo,
  5,997,007bytes; SHA256313a09756f4d48ddf11971125b1ab71ed6c77090e7077d3fb23197dd0f385f27.
- Short:artifacts/final-films/short/OnMyWay_AI_Shorts_Final.mp4;
  50s,1080x1920,CFR30,1500 frames,H.264/yuv420p,AAC stereo,
  2,195,508bytes; SHA256d3a3603770cda37001cbc3c51abf07f6f1fbcde4f9e966499949ae306d1afb13.
- Burned captions and separate SRTs:43 main/23 Short cues from actual provider
  character alignment. The local player, audio provenance, posters, source/decoded
  contact sheets and detailed verification JSON remain under the same ignored root.

Both complete FFmpeg decodes passed. Measured main-15.95LUFS/-1.48dBTP;
Short-15.94LUFS/-1.50dBTP. Reviewed19+15 decoded scene samples, including conflict,
current approval, arrival, both completions and home; no material text overlap.
Blackdetect found no full-black interval>=0.1s at pix_th0.03/pic_th0.98. Browser
playback ran main0→133.5 and Short0→50 at1x without seeking, ended=true,
muted=false,volume0.7,error=null for both. This is technical playback verification,
not subjective listening certification or a measured viral result. Main regenerated
after cache hardening retained exactly the same bytes/hash.

Actual checks on this branch: Python API67 passed/0.76s (provider HTTP mocked);
all media37 passed/0.206s, including7 new cache/timing/scenario/delivery tests;
Android demo116 and live116 unit tests,0 failures/errors/skips; both debug APKs
built successfully,Gradle86 tasks/40s. Initial Windows JDK Unix-domain loopback
failure was traced to its temporary socket path, resolved by adding
`-Djdk.net.unixdomain.tmpdir=C:\Users\Public\Documents\ESTsoft\CreatorTemp`
alongside the existing WindowsSelectorProvider setting. Existing SDK XML,
deprecated window API and packaged native-library symbol warnings remain. No
release signing or fresh device/live-provider/physical-vehicle validation performed.

Independent whole-source review caught incomplete paid-cache rebilling and
ambiguous '+30분' wording. The generator now writes a pending marker before POST,
preserves the complete successful response before postprocessing, rejects partial
or interrupted cache state and never automatically retries a paid request.
Short wording is '총30분'. Regression observed RED then GREEN; re-review found no
remaining material finding. CI now runs the new7 pure media boundary tests.
Final production notes and sanitized verification are in docs/submission/final-films*.
Git exclusions explicitly cover artifacts/final-films/ and the local.env.
Final PR/head CI are reported in the PR; no merge, access grant or publication.

## 2026-11-03 — briefing review and native departure/home setup

Created `feature/briefing-readiness` from `0dc426e` in the existing isolated
worktree. User prioritized first-place competitiveness, selected completing the
existing errand/home concept, then explicitly approved the written design.
Reviewed the 2026-10-28 YouTube Korean transcript and official judging,
submission, development, FAQ and notices. Timestamped findings/technical cards
are in `docs/submission/briefing-review-ko.md`. Reconciled the application,
pitch, strategy and checklist with the 164.355542-second spoken film and later
navigation evidence. Prior film/signed submission APK/office binaries were not
regenerated; new debug and unsigned release outputs remain local.

Added Pleos FusedLocation1.1.3, its dangerous runtime permission and a bounded
foreground adapter. Published AAR signatures were inspected, including
`LocationData.timestampMillis`. Actual installed location service version1.0.0
worked on official Pleos Connect2.1.0/API34. New Korean setup supports fresh
device departure, explicitly confirmed home, keyword-place home selection via
existing `/v1/places`, and explicit manual fallback. It adds no endpoint,
background tracking, Google location library or client credential.

Source freshness is <=60 seconds with <=5 seconds future tolerance; acquisition
is bounded to10 seconds. Every save rechecks foreground/native Park, current
revision/epoch, non-synthetic source and absence of owned/unresolved guidance.
Location edits invalidate approval. Confirmed real home persists independently
of fixture drafts. Missing provenance and old completed-stop origins cannot
silently become a new journey's current position. Actual user-facing cancellation
and permission-result status are handled without automatically retrying/saving.

TDD evidence: initial8 location cases failed against unimplemented methods, then
passed. A live-interpretation origin-clearing regression failed before its fix.
Fresh whole-branch review identified2 Important issues: absent metadata accepted
as manual provenance, and completed-stop origin surviving a genuinely new parse.
Both were observed RED as assertion failures, fixed, then verified GREEN in the
full suites. Reviewer reported no Critical issues and no other material findings;
author handled actual SDK/AVD validation and evidence prose separately.

Actual isolated **read-only** official AVD checks:

- Runtime permission denied/allowed; permission grant alone did not acquire or
  save. Permission recovery text now survives callback-before-resume timing.
- Actual fresh SDK fix produced explicit origin/home confirmation; both saved
  only on confirmation. Home remained after restart and a later permission denial.
- A stale confirmation was rejected; home remained unset until a fresh new save.
- Actual Virtual Console Drive/30 dismissed an open SDK-position confirmation;
  Park/0 did not reopen it. Console state restored to Park/0 in `finally`.
- Explicit app switch to actual native Navigation, with its foreground separately
  observed, then return dismissed the position confirmation. The vehicle HOME key
  alone was not treated as proof of background lifecycle. Completed/cancelled
  acquisition progress text was subsequently cleared in the final UI revision.
- SDK native-screen instrumentation smoke passed without sending a destination;
  native Navigation3.3.2-rc5-hotfix.3 was foreground. Map remained blank. A sanitized
  current-process scan found0 tile HTTP401 lines; the old401 is historical, not
  newly reproduced. No supported authentication remedy was established. No raw
  native logs, URLs or tokens were saved/published.

Selected full native PNGs under `docs/screenshots/location-setup/` were visually
reviewed and copied byte-identically with SHA256 provenance. They show virtual
device state, not physical GPS, live providers or a completed live errand.
Original signed apps conflicted with debug signatures; only the isolated read-only
AVD's app installations were replaced. Original persistent AVD state is not saved.

Final verification from this checkout:

- API: original configured Python environment, `-m pytest -q` in `services/api`:
  **67 passed /1.59s**. Provider HTTP mocked; no live provider result.
- `evaluate-errand-decisions.py --check`: **3/3** literal hand-calculated synthetic
  cases passed. No traffic/user-performance claim.
- Gradle8.9/JDK17 with existing licensed Android SDK35:
  `testDemoDebugUnitTest testLiveDebugUnitTest assembleDemoDebug assembleLiveDebug
  assembleDemoRelease assembleLiveRelease lintDemoDebug lintLiveDebug
  assembleDemoDebugAndroidTest --offline --no-daemon --no-watch-fs`:
  **BUILD SUCCESSFUL /1m25s /227 tasks**. Unit XML: **116 per variant**, zero
  failures/errors/skips. Debug lint: **0 errors /9 warnings per variant**;
  release lint-vital passed. Release APKs are unsigned, not submission releases.
- Standalone `safety-tests test`: **116 passed**, zero failures/errors/skips;
  BUILD SUCCESSFUL /23s. Its source include list now includes LocationSetup.
- **89** relative Markdown links across18 documents resolve; `git diff --check`
  clean. Existing SDK XML/deprecated-window/native-library strip and lint warnings
  remain. No warning-free claim.

Environment failures were diagnosed before product changes: sandboxed asyncio
blocked in socketpair creation, and Gradle could not load its native Windows DLL.
Approved execution outside the sandbox passed; neither required a product fix.
No credentials were inspected/provisioned. No public post, access grant, email,
source-history cleanup or repository visibility change was performed.

Open requirements: actual final Korean STT, live home-place/OpenAI/Kakao results,
map rendering and actual planned-route acceptance/arrival. Physical usability,
user observation, measured benefits and final signed-APK/film/form alignment also
remain. `docs/location-setup.md` gives the concrete live acceptance procedure for
when the user supplies server keys. Winning and live launch readiness are not
claimed by this feature.

## 2026-10-25 — reconcile the Short with latest main

- Merged main `a4b24cd` (contest-core PR #7) into the existing Short branch at
  remote `addfc6e`. Read verified Git objects through the connected GitHub API
  because Git HTTP still returned 403. Reconstructed only SHA-identical commits,
  reused existing local trees and preserved a local checkpoint before aligning
  the feature branch with its remote history. No force push or main-branch write.
- The only conflicted path was `docs/readiness.md`. Retained the latest main
  app/test/venue/recovery evidence and its historical video/pitch status, then
  included the separate Short. Auto-merged ignore rules, progress and application
  changes were checked against main. Production code, contracts, screenshots,
  application sections 1–7 and main's progress history remain intact.
- Explicitly identified the Short as an edit of earlier intent-review footage,
  not a demonstration of the new venue/automatic-preview/recovery UI. Current
  core sharing requires fresh footage; no video was silently regenerated.
  Existing MP4/SRT hashes remain unchanged; the provenance note is also in
  `shortform-verification.json`. No binary deliverables were added to the PR.
- Fresh merged-worktree checks: Short unittest **11 passed**, Python API pytest
  **63 passed / 5.31s**, synthetic decision comparison **3/3 passed**. Android
  demo/live unit tests each **80 passed**, with no failures/errors/skips; both
  debug builds passed: **50s, 86 tasks: 16 executed / 70 up-to-date**. Existing
  SDK XML/deprecated API warnings remain. Whitespace check against latest main
  passed; pre-existing main plan/spec EOF whitespace was not rewritten.

## 2026-10-25 — motion Short v2, faster quest-comedy revision

- Continued the owner-requested revision in the same isolated feature worktree
  and PR #6. Preserved the previous 54-second MP4/SRT/report locally under
  `artifacts/shortform/archive-v1/`; no large media was added to Git.
- Selected original workday/quest humor and a hand-drawn milk character after
  researching official YouTube creator/trend guidance, linked in the design and
  Korean storyboard. No claim that a specific joke is currently a top Korean
  meme, or that reach/retention/awards are guaranteed.
- New edit: **42.000 seconds / 18 scenes / 1,260 actually counted frames**,
  1080×1920 / 30 fps / H.264/yuv420p / zero audio streams. About 22% shorter than
  v1; mean scene length falls from 3.86 to 2.33 seconds. Staggered task cards,
  title/stamp entrances, a wobbling milk character and its exit callback, plus
  swipe/wipe/split/zoom cuts provide in-scene motion. Closing cards echo the hook.
- `shortform_motion.py` streams deterministic original graphics with genuine
  cropped footage through the builder. No fabricated native interaction or
  silent source replacement. Caption/disclosure layers stay stationary through
  transitions. A→B→A, mandatory errands, current approval, arrival/completion,
  emulator-only driving evidence and separate home approval remain explicit.
- Final canonical and friendly delivery-copy MP4 SHA-256:
  `6eb92f83090280897b219f0583e9a6ddd30fe675e4df84f40aa1c59f379125d5`,
  **2,701,260 bytes**. SRT SHA-256:
  `90f4110cb289f17545114aa18c3c2a9e7ff4b4b3c065d9dcf23e7313c8b049d6`.
  18 plain UTF-8 cues match the burned copy and exact scene boundaries, ending
  at 42,000 ms. Future owner TTS synchronization remains pending.
- Verification: FFmpeg `-xerror` full decode; FFprobe codec/rate/dimensions/audio,
  actual decoded frame count and subtitle packets; 18 midpoint samples, 25
  motion-beat samples, all 18 entries at 0/100/300 ms (54 frames), first/last
  frames and 430-pixel browser preview. Continuous browser playback reached
  42 seconds/ended with **1,260 decoded frames, zero dropped frames, no error**.
  Metadata contains generic container/handler/encoder tags. No actual YouTube
  overlay, physical device, TTS or physical-vehicle result is claimed.
- Independent read-only review of `bdff612..1376478` found no Critical/Important
  issue and confirmed the file hash, actual 1,260 frames and every SRT boundary.
  Fixed the minor duration-derived frame report: the verifier now counts frames;
  a mismatched 1,259-frame case went RED→GREEN. Regeneration retained identical
  MP4/SRT hashes. The reviewer's test rerun hit a sandbox TEMP error; it is not
  represented as green. Author's separate full run passed all **11 tests**.
- Actual commands: `python -X utf8 -m unittest discover -s scripts/tests -p
  test_shortform.py -v`: **11 passed**; existing virtualenv `python -m pytest
  services/api/tests -q`: **30 passed / 1.19s**. Android offline
  `testDemoDebugUnitTest testLiveDebugUnitTest assembleDemoDebug assembleLiveDebug`:
  **BUILD SUCCESSFUL / 19s, 86 tasks up-to-date**. These reused the unchanged
  app's existing 41-test results per variant; no fresh 82-test execution is claimed.
- Friendly local delivery copies: `artifacts/shortform/OnMyWay_AI_Shorts_motion_v2.mp4`
  and `OnMyWay_AI_Shorts_motion_v2_ko.srt`. Canonical names remain reproducible;
  `docs/shortform-verification.json` identifies both. All media/scratch images
  remain ignored. Public posting, TTS and final contest submission remain pending.

## 2026-10-25 — silent YouTube Short and matching captions

- Created a new managed `youtube-shorts/onmyway-ai` worktree and named
  `feat/youtube-shorts` from main `53fcde9`. Connected GitHub confirmed the same
  current main SHA. Local Git HTTP authentication returned 403, so publishing
  uses the connected GitHub API; each remote tree is compared with its local
  checkpoint tree. No repository visibility or access grants changed.
- User delegated research, concept, production, incremental commits and PR.
  Selected a 54-second portrait mini-story: three errands and home by 19:00,
  clarification, calculated completion, a separately labeled 18:20 conflict,
  approved +11 minutes with optional milk excluded, return to the first example,
  separate completion, emulator driving restriction and separate home approval.
- Official contest and YouTube sources are linked in the design and Korean
  `submission/shortform-ko.md`. No award, reach or retention is guaranteed.
- `scripts/build-shortform.py` and `assets/shortform/timeline-ko.json` reproduce
  the original graphics, exact subtitles and editorial crops of three approved
  actual raw captures. Missing/changed raw sources fail closed by hash. Large
  media and all rendered frames are ignored under `artifacts/shortform/`.
- Final local MP4: **54.000 seconds, 1080×1920, 30 fps, H.264/yuv420p, zero audio
  streams, 1,252,032 bytes**. SHA-256
  `f1ff16c79799f25bd4d7d44f7721fe11a26dc70157f65fdfa08bfcd4f0222ffe`.
  Source excerpts are 1× and labeled editorial crops. Persistent Korean copy
  states fixture interpretation/place/time and simulated guidance/arrival.
- Small versioned UTF-8 SRT: 14 ordered cues from 0 to 54 seconds, no BOM or
  style tags; byte-identical local delivery copy. SHA-256
  `0ae051725ff25c1985503716c3451028d580ddb717543949a2568099ce0ffba4`.
  FFprobe parsed all 14 subtitle packets; the final cue ends at 54 seconds.
  Removed the redundant final blank line after a focused RED→GREEN assertion;
  regenerated output has the same MP4 hash and passes Git whitespace checking.
  It matches burned captions; synchronization to future owner TTS is pending.
- Full FFmpeg `-xerror` decode passed. FFprobe checked codec/dimensions/frame
  rate/audio/duration; metadata contains only generic container/encoder tags.
  Inspected every scene midpoint, full-size critical UI crops, first/last frames
  and 430-CSS-pixel browser preview. Corrected two raw trim/crop choices and made
  the A→B→A example transition explicit. No physical-phone/YouTube-overlay test.
- Fresh independent read-only review inspected code, docs, exact SRT and all 14
  samples: zero Critical/Important issues. Removed one skipped chapter number.
  Reviewer independently confirmed SRT/timeline and MP4 hash; its temp-file test
  hit a sandbox permission limit. Author's full test command passed all 6.
- Actual fresh regression commands/results in this worktree:
  - `python -m unittest discover -s scripts/tests -p test_shortform.py -v`:
    **6 passed**, after observing the initial missing-builder RED run.
  - Existing virtualenv `python -m pytest services/api/tests -q`: **30 passed**.
  - Gradle 8.9/JDK 17, offline: `testDemoDebugUnitTest testLiveDebugUnitTest
    assembleDemoDebug assembleLiveDebug`: **BUILD SUCCESSFUL, 86 tasks executed**,
    **41 tests per variant / 82 executions**, no failures/errors/skips. Existing
    SDK XML/deprecated Android API/native-library stripping warnings remain.
- Generated APKs stay local. App/API production behavior did not change.
  Continuous browser playback evidence and delivery checks are recorded in
  `shortform-verification.json`. Public upload, final TTS, participant identity,
  consent/signature and final entry submission remain owner actions.

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
- Published review PR #2: https://github.com/wookingwoo/onmyway-ai/pull/2.
  Implementation commit `df674a49f38bb6b7a1c52430d14e2f0b0c048fe5` has the exact
  local source/screenshot tree `c5abfd19c35a6be1b8136b0e7b1c25c402099340`.
  GitHub Verify run **39988320825** completed successfully for both `api` and
  `android`. In the authenticated PR page all seven embedded image instances
  (six unique captures) loaded at their original 2560 × 1440 size; the actual
  before/after table was visually checked. Main remains at
  `f0e35d6d1bb6bd2c4b3f4f488fabcf62b74dbf95`; PR #2 is open and unmerged.
  A documentation-only follow-up records this publication/CI evidence without
  changing the verified implementation or APKs.

## New UI recording and Korean subtitles — 2026-10-24

- Owner requested an actual new-UI recording with separate SRT, excluding voice
  for now and preserving a clean video for later TTS. No app/API source changed;
  no credentials or voice-generation service was used.
- Recorded actual signed-demo operation on the official isolated Pleos emulator
  through all three errands and a separately approved simulated home arrival.
  A full-screen input keyboard was dismissed before capture. An accessibility
  query later timed out at the second completion dialog; the raw take was kept
  and the remaining same-session flow was captured with fresh screenshots and
  visually verified touches. The final video joins the two actual takes and
  abridges repeated steps/tool waits. No synthetic UI frames were used.
- First edit passed decoding but visual QA found a source-timing mismatch.
  Preserved that edit locally, aligned each take against actual UI transitions,
  regenerated the video/subtitles, and visually checked all 11 stage samples.
- Final deliverables in ignored `artifacts/evidence/automotive-ux/`: clean
  `OnMyWay_AI_Pleos_new_ui_demo.mp4`, matching `.srt`, and `_subtitled.mp4` preview.
  Actual duration **135.7 seconds**, **1920 × 1080 / 30 fps**, **18 Korean cues**.
  Both MP4s have one video stream and **zero audio streams**, and pass complete
  FFmpeg decoding. SRT numbering, timestamps, ordering, non-overlap, two-line
  limit and end-within-video bounds pass; FFprobe parses it as SubRip.
- Final SHA-256: clean MP4
  `6dd7b07e4d0942c372089caf47cde160ad98bca5fc9e3571ee11af8b12132210`;
  SRT `1b319f6cb70c6047db5a7421d77992cf4ad547d3d50bcd47b15cb59cf38f892e`;
  subtitled preview
  `fed03af59e009534faafb6329bb2ecf1b2b0eaf235a7d362891757087e5d2f57`.
  Original recordings and verification JSON are local. Nothing was uploaded or
  force-added to Git; the earlier 47-second old-UI recording remains unchanged.

## Versioned new-UI subtitles — 2026-10-24

- Owner subsequently authorized uploading the small SRT. Copied the final caption
  bytes to tracked `artifacts/evidence/OnMyWay_AI_Pleos_new_ui_demo.srt`, beside
  the existing historical caption sources. The paired local copy is retained.
  Video, raw evidence and APK exclusions remain in place; no ignored file is
  force-added and no app/API source changes.
- Verified UTF-8 parsing, 18 sequential non-overlapping cues, two-line limit and
  timestamps within the 135.7-second video. The new copy is byte-identical to the
  previously verified local SRT: 2,027 bytes, SHA-256
  `1b319f6cb70c6047db5a7421d77992cf4ad547d3d50bcd47b15cb59cf38f892e`.
  Document/caption-only change; Android and Python application checks are not rerun.

## Repository README refresh — 2026-10-24

- Owner requested a stronger repository README. Reorganized it around the errand
  problem, actual native experience, architecture, reproducible local setup,
  explicit demo flow, recorded validation and remaining live verification.
  Engineering prose remains English; Korean UI labels and submission links are
  preserved. Reused five authorized tracked screenshots; added no binary assets.
- Updated readiness to reference the current 135.7-second recording, versioned
  SRT and successful CI run 39996479748 on subtitle head fb3707b. The owner
  declined GIF upload; all three conversion previews remain ignored and local.
- Verified 26 relative file links, 13 heading anchors and five tracked images.
  Four PowerShell code blocks parse without errors. Local GFM rendering in an
  isolated headless Edge session loaded all images without horizontal overflow;
  the opening layout and expanded screenshot tables were visually inspected.
  Mermaid is retained as source in that local renderer, not claimed as a GitHub
  diagram-rendering check. Local HTML, captures and validation report stay ignored
  under `.tools/readme-preview/`.
- Fresh localhost checks: health `ok`; explicit demo -> pure planner returned
  `feasible`, `is_fixture: true`. No live provider endpoint was called. Sandboxed
  loopback/browser attempts were blocked; both checks passed in normal local
  execution. Application source is unchanged; Python/Android suites and builds
  were not rerun for this documentation-only update.

## Native launcher icon — 2026-10-24

- Owner requested an app icon. Added a charcoal field, white open route loop and
  forward arrow, and a mint departure point. Both demo/live manifest icon
  attributes now use the shared native adaptive resource. A dedicated monochrome
  vector supports themed launchers. Existing application behavior is unchanged.
- Added reusable vector artwork in `docs/assets/app-icon.svg` and construction
  notes in `docs/app-icon.md`. Artwork and native vectors share the same geometry.
  Local rendered previews under ignored `.tools/app-icon/` were visually checked
  at 192 px and 48 px with rounded-square, circle, squircle and themed masks.
  Rasterized alpha bounds measured a maximum radius of **31.15 dp**, within the
  **33 dp** adaptive safe radius. These previews are artwork, not device captures.
- Fresh Python verification: `.venv/Scripts/python.exe -m pytest
  services/api/tests -q` — **25 passed**. Fresh Android verification with local
  Gradle 8.9 / JDK 17 / SDK 35: `clean testDemoDebugUnitTest
  testLiveDebugUnitTest assembleDemoDebug assembleLiveDebug lintDemoDebug
  lintLiveDebug --offline --no-daemon --no-watch-fs` — **BUILD SUCCESSFUL**, all
  **102 tasks executed**, **21 tests per variant / 42 executions**, zero failures,
  errors or skips. Lint completed without errors; warning reports stay local.
- Initial sandbox/uncached offline attempts failed before validation; the final
  run used normal local execution after fetching missing build dependencies.
  Moving the adaptive resource exposed stale incremental resource state; the
  successful clean build above rebuilt both packages. AAPT inspection confirmed
  both manifest icon references and packaged background/foreground/monochrome
  layers. `git diff --check` passed. Generated debug APKs remain ignored and local;
  existing signed release deliverables were not regenerated. Device launcher and
  live-provider tests were not performed for this artwork change.

## Pleos voice input and demonstrable transcript flow — 2026-10-24

- Owner requested a feature branch, incremental commits, a completed PR with
  screenshots in its description, and native styling consistent with the app.
  Branch: `feat/pleos-voice-demo`, based on `0ed4cc4`. Added Pleos SpeechToText
  3.3.1 in ON_DEVICE mode, STT_SERVICE permission, a native day/night transcript
  dialog, explicit final-result confirmation, cancel/retry and editable input.
  Real microphone access requires actual PARKED state even in a fixture session.
  There are no Android API keys or registration credentials. Gleo registration,
  TTS, cloud STT and live LLM changes are outside this change.
- SDK artifact retrieval returned HTTP 200. Its manifest requires CAAS >=4.1.29;
  the installed service is 4.2.10-coral-aosp. On the official Pleos Connect 2.1.0
  / API 34 AVD, the signed demo actually initialized the SDK, requested Korean
  ON_DEVICE recognition and received listening-readiness events. No final Korean
  transcription success is claimed. A local Windows Heami synthetic Korean WAV
  was generated for testing; injecting it through the emulator's authenticated
  localhost gRPC audio API terminated the emulator before a result. Restored the
  same AVD without wiping data. The generated audio and gRPC helpers stay ignored.
- Added eight behavior tests: actual-park gate, partial versus final transcript,
  explicit confirmation, stale callbacks, duplicate/blank results, invalidated
  confirmation, example provenance and missing-final-result handling. First seven
  were observed RED then GREEN. A fresh read-only reviewer found a P2 where the
  example-labelled footer could call live parsing after returning from a fixture
  review; the eighth test reproduced that mismatch RED, then passed after using
  one exact-text/provenance predicate for both labels and actions and clearing the
  marker on fixture load/reset. No other reviewer findings or deferred minors.
- Actual runtime checking found another bug beyond dialog visibility: immediate
  `stop()` followed by `release()` cancelled the SDK's asynchronous remote close,
  leaving Speech Service RECORD_AUDIO marked `(running)`. Reproduced that failure
  before changing cleanup. The adapter now keeps its binding/listener until the
  ended callback or a bounded 2-second grace and serializes retries behind cleanup.
  The strengthened SDK smoke passed after the fix: cancel, actual backgrounding
  via the Settings Activity, and 30-second no-audio timeout each leave the original
  draft intact and the service microphone idle. AppOps confirmed a completed
  recording duration with no `(running)` marker. Pleos Home alone is not used as
  a background test because its multi-window workspace can leave this app visible.
- `scripts/smoke-voice.py --mode example` passed on the actual AVD: independent
  labelled sample -> cancel preserves text -> explicit apply -> explicit fixture
  request -> provenance round trip -> plan -> three separately approved simulated
  arrivals and explicit completions -> separately approved simulated home arrival.
  The sample predicts 18:40 home arrival against 19:00. It is prepared text and
  fixture planning, not a live STT/LLM result or actual navigation.
- Actual `--mode lock-ready` plus `scripts/smoke-pleos-lock.py` passed at Drive /
  30 km/h: the live SDK dialog dismissed, all voice/approval controls were locked,
  and the service microphone stopped. The console restored Park / 0. Screenshot
  capture checks also assert no microphone activity in example-only screens.
- Fresh final checks: Python `pytest services/api/tests -q` — **25 passed**.
  Android `testDemoDebugUnitTest testLiveDebugUnitTest assembleDemoDebug
  assembleLiveDebug assembleDemoRelease assembleLiveRelease lintDemoDebug
  lintLiveDebug --offline --no-daemon --no-watch-fs` — **BUILD SUCCESSFUL**, **29
  tests per variant / 58 executions**, no failures/errors/skips. Both signed
  release APKs passed v3 verification; the updated demo installed with the existing
  development signature, preserving data. SDK and debug-build warnings are local.
  Existing long-TEMP Java socket/native-library failures were resolved by using
  the writable short CreatorTemp directory and the existing Windows selector flag.
- Selected actual, unedited full-device screenshots are authorized for Git under
  `docs/screenshots/voice-input/`; raw captures and test logs remain ignored in
  `artifacts/evidence/voice-demo/` and `.tools/`. Local Git/gh credentials cannot
  access this repository (403); the connected GitHub app has repository push
  permission, so commits are transferred through Git Data APIs with exact source
  tree checks. No new access grant or credential was created. PR body contains
  explicit screenshot embeds and validation limits. No merge, publication or
  contest submission is performed by this task.

## Intent review, grounded explanation and complete demo — 2026-10-24

- Branch `feat/ai-intent-review` starts from merged voice PR #3 (`979de875`). The
  owner authorized implementation, incremental commits, screenshot PRs and final
  main merges, while keeping external provider keys for later and binaries local.
- Added bounded clarification answers, previous errands and previous home deadline
  to the stateless parser. Context is untrusted input. Lost/replaced errands and
  downgraded mandatory flags reject the result. Dwell and finish/home deadlines
  cannot silently weaken; conservative retention adds a Korean warning directing
  intentional relaxation to the manual editor.
- Native review retains original text, answers and explicit deadlines across
  recreation. One primary action progresses through a question, missing home time
  and planning. Original text/details are available on demand; vehicle styling
  follows the existing neutral Pleos day/night surfaces and 80dp controls.
  Missing conditions block calculation; editing the original invalidates review.
- Plan explanations use actual solver status, home margin, road/wait/dwell totals,
  excluded items and adjustment data. They do not invent exclusion causes or
  business hours and do not require another LLM call.
- A fresh read-only review found P1 weakened time constraints and P2 an approved
  extension not updating interpretation. Both were reproduced with regressions
  and fixed, including sharing home-time mutation between selection and extension.
  Native tests initially failed for the new model, then passed; the extra home
  deadline regression was observed failing before its fix. Provider constraint
  regression was likewise observed failing before conservative normalization.
- Final code checks: Python **30 passed**; Android **41 tests per variant / 82
  executions**, demo/live debug and release builds and both debug lint checks
  passed. `.tools/intent-final-validation.log` records BUILD SUCCESSFUL; both
  regenerated local release APKs passed v3 verification. The signed demo updated
  the existing official API 34 AVD without wiping app data.
- `scripts/smoke-intent.py --mode all` passed on that AVD: prepared question ->
  cancel -> restore pending state -> explicit answer -> missing home still blocks
  -> explicit home selection -> restore/theme change -> plan -> three separately
  approved simulated arrivals and explicit completions -> separate home approval.
  Normal fixture arrival is 18:40 against 19:00. Tight home time 18:20 requires an
  explicitly approved 11-minute extension to 18:31; the recalculated mandatory
  route excludes milk, and returning to review confirms the new 18:31 condition.
- The first tight-case automation attempted typing into a focus-blocked native
  wheel; actual UI inspection proved it left 19:00 unchanged. The driver now uses
  wheel buttons and asserts 18:20 before applying. This was a driver failure,
  not a successful tight-case test. Likewise the OEM full-screen keyboard was
  explicitly closed before testing the main action, instead of tapping through it.
- Additional actual `--mode edits` passed: editing the prepared original invalidates
  review and an explicitly requested parse returns missing-key 503 without fixture
  fallback. `--mode lock-ready` plus `smoke-pleos-lock.py` passed actual Drive /
  30 km/h, with no enabled question/approval/editor controls. Restored Park / 0.
- Selected actual screenshots are under `docs/screenshots/intent-review/`; originals
  and logs remain ignored. `scripts/start-demo.ps1` was actually run to install and
  launch the local signed demo and check API health. Korean reproduction, presenter
  and recovery instructions are in `docs/demo-guide-ko.md`.
- Live final STT, OpenAI/Kakao calls, real route acceptance/arrival and physical
  vehicle usability remain unverified. No external API key was provisioned and
  none of these fixture results is claimed as a live model or navigation result.

## 2026-10-24 — contest strategy and fresh intent-review demo

PR #4 head 4776e081 passed Verify run 40055113629. Six images rendered at 2560x1440
in actual GitHub Chrome. Merged as 8ec7b9de335e0cb84acc2ab232533563162a02ec,
fast-forwarded local main, then created docs/contest-demo-readiness.

Official overview/judging/development/FAQ/all three notices and Pleos
driving/design/privacy docs were read. Google Maps and Route4Me official docs
confirm waypoint/time-window alternatives. Korean strategy narrows the target
to commuters with mandatory errands and a home deadline. No unsupported monopoly,
ranking or customer claims. Small MAU 60/84/about 94 is an explicit calculation
hypothesis; monthly reuse is distinct from cohort retention. Launch gates cover
real evidence, platform, privacy/operations, demand/cost and authorized submission.

Actual local media:
- Ran smoke-intent.py --record on current installed signed demo: prepared Q&A,
  explicit missing-home selection, three separate completions, approved simulated
  home, deadline adjustment and named milk exclusion passed.
- Recording mode skips cancel/restart/theme checks; those passed in the prior
  full non-recording run below. Review found the unconditional report incorrectly
  included them; now executed and skipped checks are separate, and local recording
  JSON was corrected to the actual run.
- record-intent-demo.py --reuse-journeys added fresh actual Drive 30 / Park 0 capture.
  Raw durations: normal 130.716311s, tight 73.636s, driving 10.935533s.
- Final artifacts/evidence/intent-demo/OnMyWay_AI_intent_demo.mp4:
  **107.666667s /1920x1080 /30fps /silent**, labelled 2x with permanent Korean
  synthetic/prepared/simulated disclosure. First caption layout clipped its second
  line; fixed with explicit line positions and re-encoded (--assemble-only).
  Final full ffmpeg decode passed; normal/tight/driving scene frames inspected.
  SHA256:4dfd90e56005af11a8d11921cb0f0bcb801ba4db65024987c79ed5e2e1547817.
- Actual Chrome playback: currentTime=duration 107.666667, ended=true, paused=true,
  no media error, width1920/height1080. Local media-verification.json records it.
- build-pitch.py: seven self-contained Korean HTML slides with five actual images,
  no external assets. Actual Chrome checked all image loads, seven page navigation
  and layout at 1448x772. Initial dense pages overflowed; reduced desktop spacing/
  type and rechecked all seven. Output artifacts/pitch/current/index.html.
- Korean official application text, five-minute opening/demo/Q&A and recovery
  match current UI. Old DOCX/PPTX/ZIP/videos labelled superseded; historical
  packager exits without exporting stale material. Official form rebuild and
  user identity/consent/signature review remain pre-submission work.

Actual final checks:
- .venv/Scripts/python.exe -m pytest services/api/tests -q: **30 passed /0.58s**.
- .tools/intent-gradle.ps1 with both demo/live unit tests, debug/release assemblies
  and both debug lint tasks: **BUILD SUCCESSFUL in 38s / 200 tasks**. JUnit 41 each,
  82 executions, zero failures. Existing shortTEMP/WindowsSelector setup used.
- BuildTools35 apksigner verify --verbose: both local release APKs v3=true.
  demo:b2125e44216bdcb6d69d0d3a1bdea8328c34fde887fc291188bc02dfc6ba7f42
  live:a2a986e9751767bff450e6c2ec19b2d54cc79cad80ec22ed1d89cc2cb867c8e1
  Updated small checksum metadata only; APKs remain ignored.
- Found the old project uvicorn by its exact command. Stopped only known
  process 39900 / child 70168 and restarted current server hidden. Actual OpenAPI now
  exposes answers/previous_errands/previous_deadline; provider health booleans
  all false. Public schema regenerated and illustrative clarification input
  validated. No external key provisioned. Guarded start-demo.ps1 checks isolated
  official AVD and current API schema; actual install/launch passed preserving data.
- smoke-voice.py --mode sdk --reset-fixture: **PASS** actual SDK ready/listening;
  mic released after cancel/background/no-audio timeout; original draft retained,
  no fallback. Final Korean transcription is still unverified.
- Independent review found skipped-check reporting, stale evidence index and
  premature playback completion wording. Conditional reports, historical labels
  and actual completed playback/JSON resolve them. No further substantive
  assembler/pitch defects reported. Narrow current file/credential checks are
  not comprehensive Git-history or anonymity certification.

Unverified: final Korean STT, live LLM/Kakao, actual route acceptance/arrival,
physical vehicle usability, measured demand/retention/economics. Coordinates
are manual, current live candidate hours/service are unknown. Public hosting
requires authentication/privacy/operations work. No visibility change, grants,
publication, identity, consent, signature or email submission performed.

## 2026-10-25 — exact venues, current-time approval and recoverable continuation

Scope: improve the already reviewed contest-critical errand flow in an isolated
managed worktree, commit incrementally and prepare a private review PR. Provider
keys remain deferred by the owner's explicit instruction. No key/environment file
was inspected, copied or provisioned; the isolated evidence API had all provider
configuration booleans false and never silently substituted fixtures.

Implemented:
- Search up to three places through POST /v1/places; the driver explicitly selects
  the exact pickup venue. Discovery preserves identity/coordinates/evidence and
  bypasses repeated keyword selection. The plan boundary rejects conflicting
  selected-place/candidate data; legacy unselected API requests remain compatible.
- Native availability review records an explicit window, service confirmation,
  basis and timestamp. Changed query clears selection; changed service description
  clears prior availability evidence. Missing/future/>24-hour live evidence
  withholds feasibility. Supplied opening windows bind even when unverified.
- Current-traffic warnings remain. A complete hypothetical route may carry a
  conditional deadline proposal with warnings; it never upgrades feasibility,
  relaxes another supplied constraint or changes the user's deadline automatically.
- Approval counts computation and parked review time against the minimum home,
  completion and supplied closing slack. Missing/malformed bounds, stale clocks
  and expired margins fail closed. Zero margin has an explicit caution.
- Request acceptance expires after 15 seconds, invalidates local generations and
  requires reconnection/new approval. Only app-owned active guidance is cancelled.
  Ordinary replanning asks to cancel owned guidance after validating the draft.
  Teardown cancels unresolved requests without explicitly cancelling accepted
  guidance. SDK release effects and same-name remote events remain unverified.
- Explicit completion persists IDs, retains selected venues and automatically
  refreshes the remaining preview. Errors retain completion and show recovery;
  the next destination and final home always require separate approval.
- Enlarged native confirmation message text after actual screenshot inspection.
  Added independently selected uncertain/deadline example; no live error fallback.

Review and regression evidence:
- Backend review found selected-place/candidate conflicts and ignored unverified
  opening windows; both reproduced RED and fixed. Scoped re-review clean.
- Whole-branch review found SDK ownership stranded by ordinary replanning, then
  unhandled draft validation in the cancellation callback; both fixed. Validation
  now precedes cancellation and dialog/auto-refresh callbacks catch errors.
  Final independent code-review verdict: ready to merge, no remaining blocker.
- TDD reports and RED/GREEN logs remain under ignored .superpowers. Tests exercise
  venue identity, provenance age, opening/wait constraints, conditional labels,
  elapsed/wall-clock boundaries, mandatory preservation and request ownership.

Actual final verification:
- python -m pytest services/api/tests -q: **63 passed in 1.17s**. Provider HTTP is
  mocked. Eight contract JSON examples validate; OpenAPI matches the application.
- Android: testDemoDebugUnitTest, testLiveDebugUnitTest, assembleDemoDebug,
  assembleLiveDebug, assembleDemoRelease, assembleLiveRelease, lintDemoDebug and
  lintLiveDebug: **BUILD SUCCESSFUL in 1m18s /200 tasks**, **80 tests per variant**
  (160 executions), zero failures/errors. Final callback correction included.
- SDK-independent safety-tests project: **BUILD SUCCESSFUL in 29s**, the same 80
  pure tests. Existing SDK XML/deprecation/native-library strip warnings remain;
  no dependency or SDK-version change was introduced.
- Both local release APKs passed v3 signature verification. Final source build:
  local commit b338e55. Demo SHA256 ee3d62e4067568222ffe54a9ef9e931072b48f8fce65cd56f6e188856300ecea;
  live c1c3789a1c478f4c76458cfd656a78a20e22bf1cf3a360b308e979b9331d1938.
  APKs/aligned intermediates remain local; small checksum metadata updated.
- Final signed demo installed without data wipe on the existing isolated official
  Pleos Connect2.1.0/API34 AVD. scripts/smoke-contest-core.py --port8001 --lock-ready
  passed exact example pickup selection, arrival-not-completion, three explicit
  completions with automatic previews, separately approved simulated home,
  conditional +11-minute proposal, retained uncertainty/optional omission and
  zero-margin caution, and actual system night mode. Earlier run also passed.
- scripts/smoke-pleos-lock.py passed actual Drive/30 km/h venue-dialog restriction
  with no enabled unsafe controls; cleanup recorded Park/0. Theme restored to day.
  Selected seven PNGs in docs/screenshots/contest-core are unedited, byte-identical
  copies of actual final-APK captures. Raw XML/JSON/logs remain ignored.
- scripts/evaluate-errand-decisions.py --check: **3/3 hand-calculated synthetic
  examples pass**. Nearest-first misses the pickup completion deadline; the solver
  visits pickup first with the same home ETA. A tighter case explicitly omits an
  optional purchase; unknown availability remains conditional. This is a chosen
  counterexample demonstration, not a real-provider or user/time-saving benchmark.
- Updated existing smoke scripts for automatic previews; syntax checked. Their
  extended historical voice/restoration modes were not all rerun in this session.

Delivery limits:
Final Korean STT, live OpenAI/Kakao authentication/data/latency, actual NaviHelper
route acceptance/arrival, same-name SDK replay, physical-vehicle usability and
observed user benefit remain unverified. Coordinates are still manual. The new
Korean study cards are an unperformed exploratory protocol; no user contacted,
recruited or measured. The prior107.667-second video, HTML pitch and older office
files predate this flow and need regeneration before contest sharing. No contest
submission, identity/consent/signature, public publication or access grant occurred.

## 2026-10-25 — One narrated dinner-deadline story film

Branch: feature/immersive-demo-video. The user approved the life-situation plus actual-app approach and requested autonomous production, incremental commits and a screenshot-rich PR. Product Kotlin/FastAPI behavior is unchanged.

The new anonymous story follows one independently selected synthetic journey: 18:00 departure, laundry pickup completion by 18:30, mandatory medicine, optional milk and a 19:00 home deadline. Clarification and exact laundry selection precede a baseline 18:40 home estimate. Before departure the user changes pharmacy dwell from 10 to 30 minutes. The existing HTTP planner puts laundry first (arrival 18:06, completion 18:11), medicine second (18:16, 18:46), excludes optional milk and predicts home at 18:53. The deadline and both mandatory errands remain. This is a user correction, not detected live delay or traffic. Two explicit completions, automatic remaining preview and separately approved simulated home arrival were freshly recorded.

Official Pleos Connect 2.1.0 / API 34 AVD capture verified actual Drive / 30 km/h restriction and Park / 0 restoration. The first recorder assertion expected the wrong literal; actual XML established the native wording, then a bounded resume required the same pending labelled synthetic journey. No live journey or app data was cleared. All nine raw hashes and final-state markers fit independently probed media durations.

Final local film: artifacts/evidence/story-demo/OnMyWay_AI_story_demo.mp4 — 183.3 seconds, 1920x1080, 30 fps H.264/AAC stereo, 7,050,518 bytes. SHA-256: e1b3935b41dbb98339bb773ef896aa5bd3508eae613701e8dd15f4ca43aae2b8. Actual footage remains at 1x; waiting cuts and held states are visibly labelled. Editorial graphics remain outside the native viewport. Korean AI narration is separate from app STT/voice output. Complete sentences are anchored to actual returned word timings. Local SRT/ASS and an original quiet synthesized music bed accompany the film.

Media checks: ffprobe dimensions/streams and matching chapter frame/timebases pass; full video/audio decode passes. All 11 final chapter frames and key full-size native/approval frames were inspected. Actual in-app browser playback advanced from 0 to 183.3/183.3, ended=true, muted=false, readyState=4, without a media error. Technical audio analysis: integrated -16.42 LUFS, true peak -4.37 dBTP. This does not claim subjective listening certification. An intermediate rebuild encountered a Windows file lock from the owned review player; closing that tab allowed final staged replacement. Failed builds invalidate stale success reports and retain the previous film.

Production checks: 11 unittest cases pass (0.046 seconds, final scoped run). RED→GREEN evidence covers missing narration, path escape, spoken timing, mixed 25/30 fps timebase drift, stale narration/boundary provenance, empty cues, truncated final-state markers and Korean sentence continuity. Initial visual QA caught concat drift; all chapters now use 30 fps and timebase 1/90000. Independent review found two P2 provenance/truncation gaps; both were reproduced, repaired and cleared. Final caption re-review checked all 35 cues across 11 actual audio chapters: complete authored sentences, positive duration, valid bounds, no overlaps and at most two wrapped lines. The CI API job now includes the dependency-free media checks.

Application checks: 63 Python tests passed in 1.31 seconds. Native testDemoDebugUnitTest, testLiveDebugUnitTest, assembleDemoDebug and assembleLiveDebug succeeded in 28 seconds, with 86 tasks UP-TO-DATE and matching 80-test reports per variant. These cached checks are not presented as 160 fresh executions. Fresh AVD behavior is separate evidence. No new product dependency, app key or live app-provider call was introduced.

Final audio uses ko-KR-SunHiNeural through optional edge-tts 7.3.1 in a separate ignored production environment (Pillow 13.0.0). Connected ElevenLabs quota and Artlist download entitlement prevented final use; restricted previews were not downloaded or incorporated. No billing, credential or access change was made. Anonymous text was sent for editorial speech only; source, audio and boundary hashes are bound at assembly.

Selected evidence: docs/screenshots/story-demo/ contains four unedited 2560x1440 app/vehicle PNGs and two clearly labelled 1920x1080 final-film frames. They are byte-identical to local sources, with hashes in the README. Video, audio, raw clips/XML, player and reports remain ignored under artifacts/evidence/story-demo/. No MP4/MP3/WAV, APK, office file or archive is added to Git. Current media references point to the story film; old silent video, pitch and office files remain historical.

Live STT/LLM/Kakao, actual navigation acceptance/arrival, physical-vehicle use, measured user benefit and contest ranking remain unverified. No publication, reviewer grant, identity, consent/signature or final email submission occurred. The PR reports remote CI after publication.

Integration follow-up: while PR #8 was being prepared, main advanced to 6762f2f6 through the separate motion-Short work. GitHub comparison confirmed local e361e52 and that main merge have identical file contents. Merged that snapshot into this feature branch, preserving all Short assets/scripts/captions and both verification histories. Resolved only shared documentation/ignore rules, retaining the new story as the current full demo and the Short as a separate earlier-footage edit. No app/server source changed. The combined production suite passes: 22 tests in 0.278 seconds; the Short's existing Pillow deprecation warning is non-fatal. Final story-film bytes/hash and completed playback evidence are unchanged. The remote merge commit includes current main as its second parent so PR review/CI can run without conflicts.
# Voice-first app and full-Pleos film revision — 2026-10-25

Owner scope: actual app and film together; keys supplied later. Work continues in
the existing isolated `feature/immersive-demo-video` worktree and open PR #8.

- Final parked speech is consumed once and starts interpretation, necessary
  clarification, flexible venue discovery and planning automatically. Spoken
  answers/refinements retain the original request, required errands and deadlines.
  The summary includes the latest answer, ordered venues, dwell/completion times,
  home deadline and omitted optional errands. One explicit current-summary action
  approves departure; final home approval remains separate.
- `place_mode` defaults to fixed. Existing pickups cannot use nearest-shop
  substitution. Flexible purchases search up to three candidates; source hours
  and service remain unknown. Failed live operations never select examples.
- New constraints immediately invalidate old approval, including parse failures.
  A fresh reviewer found two P1 boundary cases: an earlier spoken home deadline
  after picker use, and retrying a failed refinement after newer editor changes.
  Both were fixed; targeted re-review is clean. The first regression was observed
  failing before the fix. Retry context is rebased, and new input/reset clear it.
- Actual backend verification: **67 passed in 1.12 s**, provider HTTP mocked.
  Actual Android verification: demo/live **86 unit tests each**, zero failures,
  errors or skips; both debug and release builds and release lint-vital checks
  succeeded in **1m 5s**, 182 tasks (32 executed,150 up-to-date). JDK17 short TEMP
  and WindowsSelector setup used; existing SDK XML/deprecation warnings remain.
- Current local release APKs were signed with the existing ignored development
  keystore, signatures verified, compatible demo update installed on the isolated
  official AVD. Demo SHA256 `73f0364be81208faba400b6dc6c675dc7bd8af4ee905218d72ec70b9fe2921bc`;
  live `d474954aba98786c0063d8c05f98b564845c6dd0882afaa5f9fd8529dac17640`.
- Owner additionally requires the left Pleos surface in the movie. Every native
  chapter now fits the entire 16:9 vehicle/app/bottom-control surface in1568×882,
  with separate chapter and subtitle bands. Framing regression observed RED then
  GREEN; **12 film-boundary tests passed in0.018s**. Final movie verification is recorded below.
- Live Korean final ASR, OpenAI/Kakao authentication/results, actual native route
  acceptance/arrival and physical-vehicle usability remain unverified. Locations
  still need setup; spoken removal/relaxation of required constraints is not
  supported in this iteration. No real time-savings or guaranteed contest result.

## Final full-Pleos film verification

- New film: ignored `artifacts/evidence/voice-first-demo/OnMyWay_AI_story_demo.mp4`,
  **173.966666 s (2:54), 1920×1080, H.264/AAC stereo, 6,595,516 bytes**.
  SHA256 `10c7582610cde433a0f22ccf4534c6983ad8e801e559d817edbddda648599d92`.
- Nine actual capture chapters complete the same prepared evening journey. First
  four chapters were retained with matching hashes after a recorder selector
  expected an old offscreen optional-row label; the revised preview was explicitly
  verified before resuming. APK update correctly restored simulation off; the
  labelled fixture mode was explicitly re-enabled. No alternate scenario or live
  fallback was substituted. Final AVD state is Park/0.
- Reviewed all11 final chapter frames/contact sheet, full-size revised film frame
  and actual request/refinement/plan captures. Every native movie chapter shows
  the complete Pleos left vehicle surface, app and bottom bar; header/subtitles
  stay outside. Source windows remain1x with cuts/holds disclosed.
- Full decode passed. Actual IAB playback from0 to173.966666 ended=true,
  muted=false,error=null, without seeking. This is playback/technical verification,
  not subjective narration listening certification.31 subtitle cues retain every
  authored sentence, no more than2 lines (actual maximum1); caption boundaries
  and complete text checked. Audio integrated−16.4LUFS, true peak−4.1dBFS.
- Final editorial audio uses the existing Microsoft Edge/SunHi workflow, not
  Artlist. App/request/refinement audio is clearly identified as prepared editorial
  speech, not measured real ASR/LLM success. Raw/audio/manifests/movie remain local.
  Five selected actual screenshots are permitted under `docs/screenshots/voice-first/`.

## Unified Pleos service cockpit — owner steering

Live/sample main surfaces now share service language, input, compact summary,
spoken refinement, approval and explicit completion. Source is selected separately
in settings; a small truthful badge remains. Simulation arrival utilities moved
to settings and require demo+fixture+simulation+parked/current-active-leg gates.
Raw sample venue names are plausible fictional names with fixture provenance.
Long original/answer/evidence text lives behind the details action; mandatory
flags, dwell/completion deadlines, omitted optional work and home ETA remain in
view. No new API credentials or live provider calls.

Design skills used: frontend-design, design-system and ui-ux-pro-max. Reviewed
[official Pleos UI principles](https://document.pleos.ai/docs/connect/guide/docs-design/Introduction/intro)
and [display layout](https://document.pleos.ai/en/docs/connect/guide/docs-design/getting-started/layout).
Shared semantic day/night tokens, native Korean type and≥80dp touch targets;
all normal/secondary/warning text surface pairs are unit-tested at≥4.5:1.
This is implementation guidance, not platform certification or physical-user proof.

Actual final checks: backend67 pass1.47s; Android demo/live90 tests each, zero
failures/errors, both debug/release builds and lint-vital pass in1m5s,182 tasks
(30 executed,152 up-to-date).14 media boundary tests pass0.020s. A fresh reviewer
found speech missing on infeasible plans and recorder source/heading issues;
all fixed and targeted re-review clean. Source controls and fresh completion
boundaries tested. Final signed APKs verified and updated on official isolated AVD.

OnMyWay_AI_demo_release.apk SHA256 `470d785c149175ff4abbeefccc5838f08ea446849a3bb457973b57f59847f0f9`.

OnMyWay_AI_live_release.apk SHA256 `54ce6334cf1d775230123eb301fbb04bf403f921643860adfdf921e4d651d28c`.

### Final shared-cockpit media and visual evidence

New local film `artifacts/evidence/pleos-service-demo/OnMyWay_AI_story_demo.mp4`:
**158.045333s (2:38),1920×1080,H.264/AAC stereo,5,332,388bytes**;
SHA256 `0e17bc0427a9e1dca19b76749e1cd2891281bc31bf219ac02a81f1e67fd53ff2`.
Ten narrative chapters use actual independently selected sample UI and the same
one evening journey; required stops and18:53/19:00 home remain. Native captures
complete; actual Drive30 lock and Park0 restore. Day/night input/request/plan
inspection passed on the official AVD and original night mode `no` was restored.

All10 final chapter frames reviewed plus full native day/night plan and input.
Prepared quote scenes hold their actual recorded request state (disclosed),
then show actual automatic results. The full left Pleos panel/bottom bar remain
in every native chapter. Final encode fixes observed concat timestamp gaps:
r_frame_rate=avg_frame_rate=30/1,start_time=0,4741frames. Complete decode passes.
Actual browser playback0→158.045333 ended=true,muted=false,error=null,no seeking.

29 caption cues retain every authored word; max1 lines, timing boundary checks pass. Audio−16.3LUFS, true peak−4.2dBFS; no
subjective listening certification. Six owner-authorized actual images are in
`docs/screenshots/pleos-cockpit/`. Earlier movie revisions above are historical;
no MP4/audio/APK/raw/XML/report is committed. Final live ASR/LLM/Kakao/native
navigation and physical-vehicle validation remain outstanding; keys are added
later only on the server. Public video upload/final contest submission not done.
## 2026-10-25 — spoken plan conversation and actual native-audio film

The actual Android app now reads the solver's ordered plan with Pleos
TextToSpeech 3.3.1 in explicit ON_DEVICE mode. No speech key, app registration,
cloud fallback or provider credential was read. Speech states uncertainty,
required completion deadlines, dwell, omitted optional errands and home ETA.
Explicit voice turns can refine conditions or approve only the current next
destination/home. Bare assent, questions, negation, mixed commands, duplicate or
stale turns cannot grant departure authority. Arrival still requires separate
completion. Remaining-plan refinement preserves completed ids and current origin.

Cancellation, leaving the screen, backgrounding and vehicle restriction cancel
output/listening continuation. Replaying a plan cannot enable the microphone.
Automatic reply listening begins only after confirmed output completion in an
explicit conversation. A fresh review identified two continuation/recovery issues;
both were fixed and the review found no remaining material issue.

Actual checks: demo/live 101 unit tests each, zero failures/errors; both debug
and release builds including release lint-vital succeeded (Gradle 182 tasks,
1m10s). Official OnMyWay_Pleos AVD emitted actual ON_DEVICE tts-start/tts-done;
authenticated local emulator speaker capture produced 1,532,102 bytes of PCM
across 2,909 packets in the preliminary output probe. Final actual native flow
passed saved request/refinement/three guarded voice approvals, two separate
completions and home arrival. Actual Drive30 interrupted TTS/input; the final
one-second captured PCM peak was4/32768. Park0 did not reopen the mic. Explicit
cancel and background/return also left the reply dialog closed.

Python API67 passed/1.70s; full media suite30 passed/0.223s. Earlier broad media
run under the Windows sandbox temp path hit cleanup permission errors; a rerun
with the permitted short CreatorTemp path passed. Existing SDK XML/deprecation
warnings remain; no product-test failure was suppressed. Both fresh source
reviews found no remaining material app or media issue.

Current film:164.355542s,1920x1080,constant30fps,H.264/AAC stereo,5,780,474bytes.
SHA256 `640ae133e5900c4bac60b9efc54c0a4a9b72954f3dc43e7e40f0303721034580`.
Actual ON_DEVICE output and offline Windows Heami prepared-user speech share
1x source windows. No external speech generation was performed for this film;
an online-generation attempt was rejected by automatic approval review and was
replaced with offline SAPI. All prepared intervals have native PCM RMS below1,
so user/app speech do not overlap. Captions are authored scene summaries and
proportional prepared-dialogue sentences, not recognition word boundaries.
Full FFmpeg decode and five representative frames passed. Actual IAB playback
ran0→164.355542 without seeking; ended=true,muted=false,error=null. No subjective
listening certification is claimed. Selected actual PNGs preserve source pixels
under docs/screenshots/spoken-plan; large audio/video/APK/raw/report files remain
ignored. Earlier editorial-narration films above are historical.

Signed local APK SHA256:
- demo: `793339cc096c3686052f7be82e98fd76ee2b09b341d33b585460403d730c53cb`
- live: `aa2a75f1bfd68244f0c68211fe48e5ecedf95b726494685b10dd1aa2b8a2c5ca`

Core update was published as remote0e029c0; Verify34/40200933102 passed. The
final media/evidence head is reported in PR8. Keys remain deferred/server-only;
no publication/access grant or final contest submission was performed.
Real Korean ASR/provider/navigation and physical vehicle success are not claimed.

# 2026-10-26 — official Pleos navigation screen handoff

Created Git Flow `feature/pleos-navigation-handoff` in the existing isolated
worktree from verified merged main2876838/treeff3ae318. Git HTTP fetch still
returned403; the connected GitHub branch API verified that local main exactly
matches the current remote. Earlier merged branch/history was not rewritten.

Current live approval arms one revision/destination-bound automatic foreground
request. Only actual owned route acceptance while resumed and native-Parked can
consume it. Duplicate/stale callbacks, pause/background, Drive/unknown transitions,
cancel/arrival and fixture/simulation cannot launch it. Explicit parked map retry
uses the official screen API without resending a destination. Synchronous screen
failure preserves accepted guidance. Native connection survives ordinary pause/
stop/return. A persisted interrupted-guidance hint restores uncertainty rather
than approval and blocks replacement until the user checks/ends the old guidance,
then recalculates and separately approves a fresh plan. Arrival/completion/home
boundaries remain intact. No map/provider library or client credential was added.

NaviHelper3.0.5/navi-data3.0.4 installed signatures and NAVI_CUSTOM_MAP were
verified. The actual framework-only device smoke dispatched Foreground and
`ai.umos.maps.android.navigation.app/.MainActivity` became top resumed. All three
navigation permissions were granted; installed Navigation is3.3.2-rc5-hotfix.3.
The actual MainActivity recovery-button path also dispatched the SDK and brought
that native app forward. The recovery hint was explicitly seeded on a selected
sample, not inferred from a real accepted route, and restored afterward.

**Native map limitation:** the map canvas was blank. Sanitized native-process
diagnostics observed three tile HTTP401 responses from
`a.tile.static.maps-stage.umos.ai`. No raw URL, token, credential or native log was
stored/published. Screen dispatch/foreground do not prove successful tiles,
live planned-route acceptance or actual arrival. No undocumented authentication
workaround, SDK access grant, public posting or key provisioning was attempted.

Actual sample flow passed: independently selected request/refinement;18:53 home
estimate; disabled map action; zero SDK screen calls on synthetic acceptance;
manual OS app switch/return retained accepted sample guidance; actual Drive30
locked input; Park0 restored; arrival remained incomplete until explicit action;
mandatory laundry/medicine completed; home separately approved. Existing app
voice output was stopped before handoff. Selected four full native screenshots
are under docs/screenshots/navigation-handoff with source/pixel/hash provenance.

Checks after final source fixes: demo/live106 unit tests each, zero failures or
errors; both debug/release APKs, release lint-vital and device-test APK passed
(210 Gradle tasks,3m11s). Standalone SDK-free safety suite106 passed (1m14s);
its stale baseline include list was updated for all current pure classes.
Python API67 passed/1.66s with provider HTTP mocked. Initial device-runner compile
failed because deprecated android.test classes are absent from API35; replaced
it with framework Instrumentation, without a new dependency. A separate explicit
Drive→Park regression was observed RED before its gate fix. Existing SDK XML,
deprecated-window API and one Kotlin daemon startup-retry warning remain.

Fresh whole-branch review found restriction-transition and pause/resume races;
both were fixed and re-reviewed, with no outstanding material findings. Manual
map gates now reject fixture OR simulation. CI now also runs live unit tests and
compiles the device-only smoke APK; it does not claim actual emulator execution.

Local signed APK SHA256 (not committed):
- demo `a4fbb9ba3ad34431a544625eb130b119bb3df8e7c1e1047a01712864777f48b4`
- live `82eb390dc4deb6e77a873e5c697ee13205de37c6298ddf449a36b77407ec801b`

Large APK/raw/XML/reports and the earlier local spoken film stay ignored. This
task did not regenerate that historical film or office/pitch materials. Real
ASR/LLM/Kakao, native accepted-route/home arrival and physical-vehicle validation
remain open; AI/provider keys are still deferred to the ignored server env.
Final feature PR and exact-head CI are reported in the PR description.

