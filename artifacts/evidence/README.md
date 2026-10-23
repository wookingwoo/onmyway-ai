# Verification evidence

## Current automotive UI recording — 2026-10-24

Versioned captions: [OnMyWay_AI_Pleos_new_ui_demo.srt](OnMyWay_AI_Pleos_new_ui_demo.srt).
The owner authorized this 2,027-byte UTF-8 Korean SRT in Git. Its 18 cues match
the 135.7-second clean video below; it is also retained beside that local video.
The video, raw captures and verification JSON remain local and ignored.

Local files under `artifacts/evidence/automotive-ux/`:

- `OnMyWay_AI_Pleos_new_ui_demo.mp4`: clean, silent 135.7-second, 1920 × 1080,
  30 fps edit for later voiceover work.
- `OnMyWay_AI_Pleos_new_ui_demo.srt`: matching UTF-8 Korean subtitles, 18 cues.
- `OnMyWay_AI_Pleos_new_ui_demo_subtitled.mp4`: silent preview with the same
  captions rendered and persistent fixture/simulation/editing disclosure.

Actual signed release on the official Pleos emulator: input → explicit fixture
request → dwell 10→15 minutes → plan → approval → simulated arrival → explicit
completion/replan → remaining errands → separately approved simulated home.
Two real takes are joined after an accessibility-query timeout; no app screen is
synthesized. Repeated actions and capture-tool waits are abridged. Both raw takes
are preserved. This is not evidence of live providers or actual SDK navigation.

Both final MP4s passed full FFmpeg decoding and contain zero audio streams. The
SRT passed format, ordering, overlap, duration and UTF-8 parsing checks. After
correcting the first edit's source-clock alignment, representative frames for all
11 stages were visually checked for matching UI, Korean glyphs, caption placement
and unobstructed primary controls. Exact files, hashes and edit ranges are in
local `new-ui-video-verification.json`; raw timelines and frames stay local.

## Historical recording

The original screenshots and recording below show the previous UI, captured from the actual signed release app
running on the official Pleos Connect 2.1.0 x86_64 API 34 emulator.

- `pleos-live-connected.png`: live variant reads actual unrestricted vehicle UX
  state and connects to the actual Pleos navigation service. No route request
  or live AI call is established by this screenshot.
- `pleos-demo-plan.png`: explicitly labelled synthetic places, business windows
  and road times are returned by the actual local HTTP API and rendered natively.
- `pleos-demo-arrival-not-complete.png`: simulated arrival leaves the errand
  incomplete until explicit user confirmation.
- `pleos-demo-replanned.png`: first errand completed explicitly, remaining errands
  recalculated through the actual HTTP API. It does not use live traffic.
- `pleos-driving-lock.png` and `pleos-driving-check.json`: official local Virtual
  Console changed the isolated AVD to Drive / 30 km/h. Actual CarUxRestrictions
  locked editing and destination approval even with demo simulation enabled.
  The test restored Park / 0 km/h in its cleanup.
- `OnMyWay_AI_Pleos_fixture_demo.mp4`: actual screen recording with a persistent
  disclosure caption. The scene is not an AI response or real navigation demo.

Reproduction scripts: `scripts/smoke-android.py`, `scripts/smoke-pleos-lock.py`,
`scripts/record-demo.ps1`. Use an explicitly selected isolated test emulator.
Raw screenshots and recordings, including the unedited recording, remain local
and are excluded from Git. The owner separately authorized selected actual
screenshots of the automotive redesign under `docs/screenshots/automotive-ux/`.
That directory documents the current input, review, route, night and driving-lock
screens, plus the historical before image. Current raw UI dumps, smoke captures
and `pleos-driving-check.json` are in ignored `artifacts/evidence/automotive-ux/`.
The old recording and existing local review ZIP have not been regenerated for
the redesign; do not present them as captures of the new UI.

Not yet verified: live OpenAI interpretation, live Kakao places/road responses,
actual Pleos route acceptance/arrival, physical vehicle operation, user research,
and effectiveness claims. Do not conflate service connection with successful
navigation. The live variant never falls back to these fixtures.
