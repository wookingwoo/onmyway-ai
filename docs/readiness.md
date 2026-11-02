# Submission readiness

**Status: KEY-FREE DEMO READY; LIVE LAUNCH AND SUBMISSION PENDING.**

Official briefing and website rechecked 2026-11-03 KST; see the
[timestamped review and priorities](submission/briefing-review-ko.md).
The implementation/media table below is historical. New location implementation,
fresh tests/builds and actual AVD checks are recorded in [location setup](location-setup.md)
and progress. Fresh debug/unsigned release APKs stay local; historical signed
submission APKs, films and office files were not regenerated/reverified here.
Contest validation uses the official emulator; physical-vehicle usability is a
separate launch task. Final APK, film and form versions still need reconciliation.
Checked 2026-10-25. The separate motion Short uses earlier intent-review footage
and is not evidence of the current venue/automatic-preview flow. This is a
functioning native demo, not a verified live service or a submitted entry.

| Requirement | Actual evidence |
| --- | --- |
| Planner/API | 67 Python tests pass; provider HTTP mocked, no live-result claim |
| Native policies | 106 tests each demo/live; spoken revision/destination/freshness, mandatory/home, cancellation, completion and existing boundaries |
| Builds | Both debug/release variants and release lint-vital pass; local signed releases verified and demo installed |
| Current official AVD flow | One saved request → actual ON_DEVICE plan explanation → saved refinement → revised spoken plan → guarded saved voice approval → two explicit completions → separately approved simulated home |
| Vehicle/lifecycle | Actual Drive30 stops output/input; final quiet PCM peak4/32768. Park0 does not reopen input; cancel and background/return leave it closed |
| SDK scope | Actual native map-screen foreground and recovery-button dispatch plus Korean TTS start/done verified. Native map tile requests returned HTTP401 and the canvas stayed blank. Real final Korean ASR, provider authentication, actual navigation acceptance/arrival and physical usability remain unverified |
| Missing providers | Key-free API health flags all false; fixtures independently selected, never live-failure fallback. No key provisioned |
| Screenshots | Current actual spoken-plan/full-Pleos PNGs in docs/screenshots/spoken-plan; earlier cockpit/day-night evidence remains |
| Current film | 164.355542 seconds, FHD, constant30fps; actual device output + offline prepared user speech. Matching1x audio/video cuts, truthful sample/native disclosure, full decode |
| Film review | Representative frames and uninterrupted unmuted browser playback recorded separately in docs/progress.md |
| Historical media | Earlier editorial-narration films, silent captures and motion Short are historical and do not demonstrate the new spoken output/approval |
| Contest materials | Anonymous source remains a draft; old DOCX/PPTX/ZIP/pitch must be regenerated before submission. No public video posting, access grant or final submission performed |
| GitHub/CI | Spoken-film work was merged through PR8; subsequent navigation handoff through PR9. Historical check results belong to those revisions, not this review branch |

## Run the current demo

Start the official OnMyWay_Pleos AVD in Park / 0, then run
`scripts/start-demo.ps1 -Serial emulator-5556`.
Select **샘플로 둘러보기** independently in Settings on a new journey, return,
then **말로 부탁하기**. Follow [spoken scenario](submission/spoken-demo-ko.md)
and [production/evidence](../scripts/SPOKEN_DEMO.md).
The example uses fixed 2026-10-23 18:00 data; it is not today's live recommendation.

The spoken-plan film supersedes the earlier editorial-narration/silent films.
The original pitch, office files and vertical Short still need regeneration before
sharing as current submission materials. Synthetic comparisons and
the unperformed user protocol are in docs/evaluation-guide.md and
docs/submission/user-study-ko.md. Real provider/navigation/user benefit remains unverified.

Local deliverables:

- `apk/OnMyWay_AI_demo_release.apk`, `apk/OnMyWay_AI_live_release.apk`
- `artifacts/evidence/spoken-plan/OnMyWay_AI_spoken_demo.mp4`
- `artifacts/evidence/spoken-plan/OnMyWay_AI_spoken_demo.srt`
- `artifacts/evidence/spoken-plan/index.html` (local player)
- `artifacts/evidence/spoken-plan/media-verification.json`
- `artifacts/pitch/current/index.html` (generate with `scripts/build-pitch.py`)
- `artifacts/shortform/OnMyWay_AI_Shorts_silent.mp4` (generate with `scripts/build-shortform.py`; requires approved local raw footage)
- `artifacts/evidence/OnMyWay_AI_Shorts_ko.srt` (versioned small caption file)

## Remaining launch and submission work

1. The user later configures ignored server `.env`: OPENAI_API_KEY, compatible
   OPENAI_MODEL, KAKAO_REST_API_KEY, KAKAO_MOBILITY_REST_API_KEY; restart API.
   No key enters the APK or chat.
2. Validate actual provider response/latency/failure and trusted destination data.
   Native search supplies up to three nearby candidates for explicit selection. Pinned venues remain fixed; availability must be explicitly reviewed with basis/time. Unknown or stale evidence and current-traffic snapshots remain needs_verification.
3. Verify final Korean STT, actual NaviHelper route acceptance and arrival,
   same-name/unrelated destination callbacks, physical vehicle/region/category UX.
   Fresh device departure and explicit home setup are implemented; home keyword
   search still needs live provider verification. See [location setup](location-setup.md).
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
committed. The historical allowed SRT remains. Current story-film captions stay
local alongside the film. The vertical Short is a separate edit of the earlier
intent-review flow, not evidence of the new core UI. Older review ZIP and
office/video files are superseded.
Repository visibility is unchanged. Owner identity in GitHub/Git history has not
been made anonymous; public release and reviewer grants require user action.
Historical remote Git objects may persist; cache erasure is not established.
# Latest shared-cockpit revision

The current feature work uses one shared native service UI for live/sample,
voice-first automatic proposal preparation, compact day/night summaries and
explicit fresh departure/completion/home actions. See the [actual current images](screenshots/pleos-cockpit/README.md)
and [recorded verification](progress.md). Older transcript-confirmation and silent-film
records below are historical. API credentials remain server-only and are supplied
later; live provider and physical-vehicle results are still unverified.


Current native map handoff: [behavior and limits](navigation-handoff.md). No real planned-route acceptance/arrival result is implied by screen dispatch.
