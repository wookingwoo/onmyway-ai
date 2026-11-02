# Final contest films implementation plan

> **For agentic workers:** Use superpowers:executing-plans for inline execution.

**Goal:** Deliver a polished Korean main film and vertical short with premium
ElevenLabs speech, synchronized captions, local review files and a source-only PR.

**Architecture:** Authored scenario JSON feeds a server-side audio generator and
a deterministic Pillow/FFmpeg renderer. Character timings drive captions. Rendered
UI is an editorial prototype; no app/runtime behavior is changed.

**Tech stack:** Python, httpx, python-dotenv, Pillow, NumPy, FFmpeg/FFprobe.

**Spec:** docs/superpowers/specs/2026-11-03-final-contest-films-design.md

## Global constraints

- Korean product copy; English code/engineering docs. No identity or key in media.
- Latest remote main verified as50c74a7; feature/final-contest-films created there.
- Root .env read only by server-side helper. Outputs ignored under artifacts/final-films/.
- Preserve mandatory errands, current approval, arrival/completion, separate home.
- Short: vertical30–60s. No upload, publication, final email or merge.

## Review focus

- Stale paid-audio cache must never be silently reused after text/voice changes.
- Character timestamps must match authored text and remain within audio/scene.
- Speech may extend scene duration; short may not exceed60s or be truncated.
- Scenario arithmetic must include opening wait, dwell and direct home travel.
- Final staged files must contain no large media or environment secrets.

## Tasks

1. Story and speech (`scripts/final_film_story.json`,
   `services/api/tools/generate_film_audio.py`). Write boundary tests first for
   cache fingerprints, timestamps and scene timing. Generate audition samples,
   select three native Korean voices, generate timed utterances with no paid retry.
2. Picture and mix (`scripts/final_film.py`, `scripts/build-final-films.py`).
   Implement distinct wide/portrait cockpit compositions, sequential route motion,
   conflict/revision/approval/completion states. Mix original bed/tones under speech.
   Generate aligned SRT, posters, contact sheets and local player. Validate full
   decodes, stream metadata, timing and mix loudness. Inspect representative frames.
3. Verification and handoff (`scripts/tests/test_final_film.py`,
   `docs/submission/final-films-ko.md`, `docs/progress.md`, README.md).
   Run media/API tests and available Android checks; record exact results and
   limits, local hashes, voice provenance, rules and posting draft. Review source,
   commit explicit text files only, push branch, create PR with main as base.

Execution is autonomous as explicitly requested by the owner. Update this document
and production notes to match the final deliverables rather than obsolete drafts.

## Execution ledger

- [x] Verified remote main50c74a7, created feature/final-contest-films there.
- [x] Builder check: nothing important unanswered; scenario arithmetic confirmed.
- [x] Tests RED before timing/cache implementation; final new suite7 GREEN.
- [x] Generated three main-role auditions,19 main utterances and15 final Short
  utterances using native Korean professional voices. Earlier12 Short utterances
  are local exploratory takes, superseded by the owner's viral direction.
- [x] Ruling: use a separate quest/milk comedy for the Short, with a bright narrator
  and milk character — explicit owner steering; keeps the main film restrained.
- [x] Full-source independent review found partial-cache rebilling and ambiguous
  +30-minute wording. Added failing interrupted-request regression; fixed both;
  re-review found no material outstanding issue.
- [x] Main133.5s/19 scenes/43 cues; Short50s/15 scenes/23 cues. Full decode,
  CFR/stream/loudness checks,34 decoded review frames and no full-black intervals.
- [x] Real browser1x playback for both files ended without seek/error, unmuted.
- [x] API67 passed; media37 passed; Android116 per flavor, both debug builds passed.
  Initial JDK Unix-domain socket failure resolved using a short allowed temp path.
- [x] Production notes, rules, posting draft, sanitized hashes and delivery evidence.
- [ ] Commit explicit source/documentation paths, push feature branch, open PR.
