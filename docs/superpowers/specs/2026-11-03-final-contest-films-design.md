# Final contest films design

The owner requested autonomous conception and production of a polished main film
and vertical short, premium ElevenLabs voices, a feature branch from main, a PR,
and local-only large deliverables. Presentation-only UI is explicitly authorized;
no persistent demo watermark is requested. This is authorization to make creative
choices, not a claim of production deployment or successful live-provider tests.

## Approach and intent

Selected by the producer under the owner's delegation: an original animated
product film, with a consistent cinematic cockpit UI, a single coherent errand
story, and three distinct Korean speech roles in the main film. The owner's later
viral-Short direction adds a brighter narrator and an original milk character,
using five voices across both films. Existing footage-only editing
would retain dated UI and lengthy waits; a generic promotional montage would
fail to explain the differentiating decisions. The product film explains them
through request, clarification, revision, approval, completion, and home.

The rendered interface is an editorial prototype, not a new Android application
or a recording of live STT, planning, traffic or navigation. Keep this provenance
explicit in the production notes, PR and handoff. Do not claim deployed Pleos
integration, commercial availability, saved minutes, users or awards. The spoken
assistant is ElevenLabs editorial audio, not Pleos runtime TTS. No on-screen demo
badge is required by the official first-round video guidance.

## Rules verified on 2026-11-03 KST

- https://appdevchallenge.auto-hmg.io/guide/submission accepts mockups,
  prototypes and screen recordings. No main-film duration specification found.
- https://appdevchallenge.auto-hmg.io/guide/shortform requires vertical 30–60s,
  public Reels/Shorts plus 2–3 hashtags including #HMG차량용앱개발공모전.
  The short-form award is independent; no main-contest bonus for the short link.
- https://appdevchallenge.auto-hmg.io/guide/judging-awards evaluates vehicle
  specificity, creativity, technical application and business potential.
  Pleos App Market distribution requires separate verification and review.
- https://appdevchallenge.auto-hmg.io/guide/rules requires a working app before
  second-round PT. Publishing, access grants and final submission remain owner work.

## Scenario and exact arithmetic

All places/times are authored scenario inputs, not real-provider observations.
Depart 17:50. Mandatory laundry must be collected by 18:30, mandatory medicine,
optional milk, final home deadline 19:00. Laundry: drive8 + opening wait2 + dwell5
= complete18:05. Pharmacy: drive8 + dwell5 = complete18:18. Market: drive7 +
dwell7 = complete18:32. Home drive8 = 18:40. With pharmacy dwell30, the unchanged
three-stop plan reaches home19:05; omitting only optional milk gives pharmacy
complete18:43 + direct home drive10 = home18:53. No mandatory errand is dropped.
Show estimates as estimates, not guarantees. Unknown data cannot prove feasibility.

Arrival does not mark completion. Approve the current revised plan before the
next destination. Driving locks editing/approval; home is separately approved.
The short is a compressed retelling of the same scenario, not a second set of times.

## Visual and sound direction

1920×1080 main film, approximately 120–155s; 1080×1920 short, 45–59s; 30fps H.264
yuv420p, AAC stereo, faststart. Graphite cockpit, warm white type, restrained mint
route line, amber only for conflicts. Large Korean typography, ample negative
space, persistent home deadline and mandatory/optional distinction. A drawn road
network is schematic original artwork, not a real map. Separate portrait layout;
never crop a wide cockpit into unreadable portrait text. No participant identity.

Narrator: calm Korean announcer; driver: natural conversational Korean male;
assistant: clear warm Korean female. Discover actual available voices, audition
short samples, generate character timestamps and align captions to provider
boundaries. Use a quiet original synthesized music bed and brief original UI
tones, with speech ducking. No borrowed music/memes or cloned personal voices.
Credit local audio provenance, selected voice IDs, text hashes and output hashes.

## Engineering and review

Server-side audio helper alone reads root ignored .env; Android and renderers
never read keys. Fail on API errors and do not silently use offline speech.
Cache each utterance by voice/model/text/settings hash to avoid duplicate billing.
No automatic retries of paid POSTs. Validate complete timing arrays and matching
text, reject subtitles crossing scene bounds, reject short outside30–60s, contain
all outputs under ignored artifacts/final-films/. Generate a local player, SRTs,
posters, review contact sheets, and sanitized verification JSON.

Verify frame dimensions, CFR, audio streams, full decode, captions, loudness,
black/freeze issues at intended scene boundaries, and representative screens.
Run meaningful media tests plus Python API baseline and Android unit/build checks
where installed tooling permits. Record actual results and limits in progress.md.
Commit only source, scenario, tests and documentation. Push feature branch and
open a thorough PR against main. Do not merge or publish videos.

Remaining details (durations after speech generation, easing, precise voice
settings and balanced sound levels) are producer decisions delegated by the owner.
