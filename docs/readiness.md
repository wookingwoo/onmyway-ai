# Submission readiness

**Status: KEY-FREE DEMO READY; LIVE LAUNCH AND SUBMISSION PENDING.**
Checked 2026-10-24. This is a functioning native demo, not a verified live service
or a submitted entry.

| Requirement | Actual evidence |
| --- | --- |
| Planner/API | 30 Python tests pass: time windows, mandatory preservation, unknown evidence, delay and clarification constraints |
| Native policies | 41 tests per demo/live variant, 82 executions; fresh approval, completion, clarification, deadline and voice boundaries |
| Builds | Demo/live debug and release builds plus both debug lint checks pass; two local signed release APKs pass v3 verification |
| Current AVD flow | Actual prepared question/answer and explicit home deadline, restore/day/night, three separate completions and separately approved simulated home |
| Tight deadline | 18:20 infeasible; explicit 11-minute extension to 18:31 preserves mandatory errands, names excluded milk, updates reviewed conditions |
| Vehicle restriction | Actual CarUxRestrictions; question dialog closes/locks at Drive 30 km/h, restored Park 0; unknown state fails closed |
| SDK scope | STT 3.3.1 ON_DEVICE initialization/listening/cancellation and NaviHelper 3.0.5 service connection verified; final Korean transcription and actual route acceptance/arrival unverified |
| Missing providers | Actual missing-key 503 without fixture fallback. All provider health booleans false; no external key provisioned |
| Screenshots | Six actual unedited captures in docs/screenshots/intent-review; PR #4's six images render at 2560x1440 |
| Latest video | 107.667 seconds, 1920x1080, silent, explicitly 2x; full home journey, adjustment, actual driving lock; permanent Korean disclosure, full decode and visual/playback verification |
| YouTube Short | Separate 54-second 1080×1920 silent edit and Korean SRT; owner TTS/public posting pending. Storyboard and production instructions: docs/submission/shortform-ko.md |
| Current pitch | Seven Korean self-contained HTML slides generated locally with actual embedded screenshots; layout and navigation checked |
| Application | Current anonymous Korean section 02 source updated. Old local DOCX/PPTX/ZIP predate this flow and must not be submitted as current |
| GitHub/CI | Private repository. PRs #1–#4 merged incrementally. PR #4 head 4776e081 passed Verify run 40055113629; preparation PR evidence recorded in progress and GitHub |

## Run the current demo

Start the official OnMyWay_Pleos AVD in Park / 0, then run
`scripts/start-demo.ps1 -Serial emulator-5556`.
Select **질문과 답변 체험 · 예시 해석**. Follow `docs/demo-guide-ko.md` and
`docs/submission/pitch-and-demo-ko.md`.
The example uses fixed 2026-10-23 18:00 data; it is not today's live recommendation.

Current local deliverables:

- `apk/OnMyWay_AI_demo_release.apk`, `apk/OnMyWay_AI_live_release.apk`
- `artifacts/evidence/intent-demo/OnMyWay_AI_intent_demo.mp4`
- `artifacts/evidence/intent-demo/media-verification.json`
- `artifacts/pitch/current/index.html` (generate with `scripts/build-pitch.py`)
- `artifacts/shortform/OnMyWay_AI_Shorts_silent.mp4` (generate with `scripts/build-shortform.py`; requires approved local raw footage)
- `artifacts/evidence/OnMyWay_AI_Shorts_ko.srt` (versioned small caption file)

## Remaining launch and submission work

1. The user later configures ignored server `.env`: OPENAI_API_KEY, compatible
   OPENAI_MODEL, KAKAO_REST_API_KEY, KAKAO_MOBILITY_REST_API_KEY; restart API.
   No key enters the APK or chat.
2. Validate actual provider response/latency/failure and trusted destination data.
   Current discovery supplies one nearby candidate per errand, no business hours
   or service availability. Unknown evidence remains needs_verification.
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
Only selected authorized actual screenshots and small caption/source files are
committed. The historical allowed SRT remains. The older review ZIP and old
office/video files are superseded; the vertical Short is a separate current edit.
Repository visibility is unchanged. Owner identity in GitHub/Git history has not
been made anonymous; public release and reviewer grants require user action.
Historical remote Git objects may persist; cache erasure is not established.
