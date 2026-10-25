# YouTube Shorts design

## Intent and authorization

Create a compelling Korean contest short, a silent MP4 and matching UTF-8 SRT,
for the owner to add TTS later. The owner delegated research, concept, production,
incremental commits and a PR, requested a new worktree based on main, and excluded
large generated media from Git. Creative and execution choices below are the
builder's decisions within that authorization; no publication is authorized.

## Research (checked 2026-10-25, Korea)

- [Contest rules](https://appdevchallenge.auto-hmg.io/guide/shortform): 30–60 seconds,
  portrait, Instagram Reels or YouTube Shorts, link in the application. Include
  `#HMG차량용앱개발공모전` and 2–3 hashtags in total. Optional, no main-award bonus;
  one independent 500,000 KRW award. No detailed shortform scoring rubric is given.
- [YouTube upload requirements](https://support.google.com/youtube/answer/12779649):
  square/vertical videos up to three minutes qualify; the contest's stricter
  60-second ceiling determines this edit.
- [YouTube creator interview](https://blog.youtube/creator-and-artist-stories/youtube-shorts-deep-dive/):
  the opening moment and a self-contained miniature story deserve attention.
  This is creative guidance, not evidence of guaranteed retention or a win.
- [YouTube SRT support](https://support.google.com/youtube/answer/2734698): basic
  SubRip in plain UTF-8; styling is not supported.

## Approaches considered

1. Compress the full landscape demo: strong evidence, weak phone readability.
2. Entirely animated explainer: readable, but loses proof that the native app exists.
3. **Selected:** original typography/route graphics with tightly cropped genuine
   native footage. One everyday story, readable Korean captions, visible evidence.

## Creative treatment

54 seconds, 1080×1920, 30 fps, H.264/yuv420p, no audio stream. Midnight blue,
warm white and lime; orange marks the deadline conflict. No stock media, borrowed
music, fabricated testimonials, people, identity or claims of measured savings.
The first frame starts with the deadline and three unfinished errands, not a logo.
Large editorial text and a moving route motif carry silent viewing. Actual app
footage supplies the question, route, adjustment, completion, restriction and home.

Story: home by 19:00 → medicine/laundry required, milk optional → clarify pickup
completion → count travel/wait/dwell/home → example home 18:40 → deadline 18:20
fails → propose +11 minutes → user approves, milk excluded → arrival is separate
from completion → driving lock → separately return home → a relatable closing
question. The normal and tight branches are explicitly separated with a new
deadline card. App crops are editorial enlargements, never invented app screens.

Critical text stays within x=72..880 and y=180..1680 to leave room for typical
Shorts controls; these are conservative production margins, not a YouTube guarantee.
Narration captions are burned in for silent viewing and exported from the same
timeline as plain SRT, with readable two-line cues and space for natural later TTS.
SRT timing will need review if the owner's eventual TTS delivery differs.

## Truth and source boundary

Permanent legible Korean disclosure: prepared interpretation and synthetic place/
time data; guidance/arrival simulated; actual AI/road/navigation results are not
represented. Driving restriction was checked in the official Pleos emulator,
not a physical vehicle. Do not imply live STT/LLM, road-route success or arrival.
Use only existing actual raw recordings and owner-authorized repository captures.
Missing inputs fail clearly; never synthesize replacement footage silently.

## Deliverables and acceptance

- Local ignored `artifacts/shortform/OnMyWay_AI_Shorts_silent.mp4`.
- Small versioned `artifacts/evidence/OnMyWay_AI_Shorts_ko.srt`.
- Versioned timeline, reproducible Python/Pillow/FFmpeg builder, source hashes,
  Korean storyboard/upload draft and actual verification notes.
- Verify duration 30–60 seconds, portrait dimensions, frame rate, H.264 pixel
  format, zero audio, metadata, full decode, SRT sequencing/timing/readability,
  all scene samples, miniature-phone readability and browser playback.
- Commit no videos, raw footage, rendered frames or contact sheets. Keep only
  source, captions and concise evidence in the PR. Do not publish to YouTube.
