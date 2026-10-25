# Submission readiness

**Status: KEY-FREE DEMO READY; LIVE LAUNCH AND SUBMISSION PENDING.**
Checked 2026-10-25. This is a functioning native demo, not a verified live service
or a submitted entry.

| Requirement | Actual evidence |
| --- | --- |
| Planner/API | 63 Python tests pass: exact venues, provenance/freshness, supplied windows, conditional proposals and existing constraints |
| Native policies | 80 tests per demo/live variant, 160 executions; venue edits, elapsed slack, owned navigation recovery and existing boundaries |
| Builds | Demo/live debug and release builds plus both debug lint checks pass; two local signed release APKs pass v3 verification |
| Current AVD flow | Actual pinned example pickup, three explicit completions with automatic previews, separately approved simulated home, conditional proposal, zero-margin caution and night mode |
| Tight deadline | 18:20 infeasible; explicit 11-minute extension to 18:31 preserves mandatory errands, names excluded milk, updates reviewed conditions |
| Vehicle restriction | Actual CarUxRestrictions; question dialog closes/locks at Drive 30 km/h, restored Park 0; unknown state fails closed |
| SDK scope | Earlier STT initialization/listening/cancellation and NaviHelper binding checks are recorded; new lifecycle policies have JVM tests. Final Korean transcription, real route acceptance/arrival and SDK release/replay effects remain unverified |
| Missing providers | Actual missing-key 503 without fixture fallback. All provider health booleans false; no external key provisioned |
| Screenshots | Six actual unedited captures in docs/screenshots/intent-review; PR #4's six images render at 2560x1440 |
| Historical video | 107.667 seconds, 1920x1080, silent, explicitly 2x; full home journey, adjustment, actual driving lock; permanent Korean disclosure, full decode and visual/playback verification |
| Historical pitch | Seven previously checked Korean HTML slides; regenerate to reflect the new venue/automatic-preview flow before contest sharing |
| Application | Current anonymous Korean section 02 source updated. Old local DOCX/PPTX/ZIP predate this flow and must not be submitted as current |
| GitHub/CI | Private repository. PRs #1–#4 merged incrementally. PR #4 head 4776e081 passed Verify run 40055113629; preparation PR evidence recorded in progress and GitHub |

## Run the current demo

Start the official OnMyWay_Pleos AVD in Park / 0, then run
`scripts/start-demo.ps1 -Serial emulator-5556`.
Select **질문과 답변 체험 · 예시 해석**. Follow `docs/demo-guide-ko.md` and
`docs/submission/pitch-and-demo-ko.md`.
The example uses fixed 2026-10-23 18:00 data; it is not today's live recommendation.

Current APKs and screenshots supersede the previous recording/pitch flow. Regenerate video, pitch and official office files before contest sharing. Synthetic comparison results and the unperformed user protocol are in docs/evaluation-guide.md and docs/submission/user-study-ko.md.

Local deliverables:

- `apk/OnMyWay_AI_demo_release.apk`, `apk/OnMyWay_AI_live_release.apk`
- `artifacts/evidence/intent-demo/OnMyWay_AI_intent_demo.mp4`
- `artifacts/evidence/intent-demo/media-verification.json`
- `artifacts/pitch/current/index.html` (generate with `scripts/build-pitch.py`)

## Remaining launch and submission work

1. The user later configures ignored server `.env`: OPENAI_API_KEY, compatible
   OPENAI_MODEL, KAKAO_REST_API_KEY, KAKAO_MOBILITY_REST_API_KEY; restart API.
   No key enters the APK or chat.
2. Validate actual provider response/latency/failure and trusted destination data.
   Native search supplies up to three nearby candidates for explicit selection. Pinned venues remain fixed; availability must be explicitly reviewed with basis/time. Unknown or stale evidence and current-traffic snapshots remain needs_verification.
3. Verify final Korean STT, actual NaviHelper route acceptance and arrival,
   same-name/unrelated destination callbacks, physical vehicle/region/category UX.
   Current departure/home coordinates are manual; auto-location remains future.
4. Before public hosting: authentication/TLS/rate limits, privacy/deletion,
   shared-driver sessions, monitoring/support, API costs and user validation.
   This trusted local prototype is not an authenticated production backend.
5. Rebuild official application binaries from the current source and visually
   inspect them. User completes identity/consent/signature, anonymity/access,
   organizer updates and final submission. No email sent or access granted.

No user research, measured savings, retention, revenue, partnership or safety
certification is claimed. Business numbers are explicitly hypothetical in
`docs/contest-strategy-ko.md`, which also connects official judging, SDK/FAQ/notices,
alternatives and launch gates.

APKs, video, HTML pitch, raw captures/logs, office files and archives stay local.
Only selected authorized actual screenshots are committed; the historical allowed
SRT remains. The older review ZIP and old office/video files are superseded.
Repository visibility is unchanged. Owner identity in GitHub/Git history has not
been made anonymous; public release and reviewer grants require user action.
Historical remote Git objects may persist; cache erasure is not established.
