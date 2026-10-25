# Motion Shorts implementation plan

> **For agentic workers:** Execute inline with superpowers:executing-plans.
> The owner delegated revision and production in the existing PR.

**Goal:** Replace the static 54-second edit with a funny 42-second animated Short.

**Architecture:** A deterministic Pillow frame compositor supplies genuine cropped
footage and original animated layers to FFmpeg. The same 18-scene timeline defines
frame-aligned transitions and SRT timing. Evidence/disclosure remain stationary.

**Tech Stack:** Python/Pillow, FFmpeg/FFprobe, unittest, local browser playback.

**Spec:** `docs/superpowers/specs/2026-10-25-motion-shorts-design.md`

## Global constraints

- 42 seconds / 1,260 frames / 1080×1920 / 30 fps / H.264/yuv420p / no audio.
- Genuine hash-checked sources, permanent fixture/simulation disclosure.
- Korean creative copy, English engineering docs/source/commits; no binary uploads.
- Same feature branch, existing PR #6; preserve the previous edit locally.

## Review focus

- Fractional scene durations must map exactly to frames and SRT milliseconds.
- Transitions must not add runtime, black frames, or animate safety disclosure.
- Animation must not misrepresent app interaction or clip critical text.
- Shorter TTS copy must still distinguish example branches and current approval.
- Cached segments must invalidate when motion code, timeline, fonts or sources change.

### Task 1: Contract and frame compositor

- [x] Archive v1 locally and commit this design/plan.
- [x] Add RED tests for frame alignment, transition bounds and deterministic motion.
- [x] Implement `scripts/shortform_motion.py`, integrate the frame renderer and run
  `python -m unittest discover -s scripts/tests -p test_shortform.py -v`; expect green.
- [x] Commit compositor and 18-scene timeline/SRT after rendering a preview.

### Task 2: Production and review

- [x] Render all 1,260 final frames, decode fully and inspect multiple moments per
  animation plus first/last frames and every cut at phone size.
- [x] Correct visual/semantic defects; verify 42-second SRT and audio-free output.
- [x] Fresh independent review, Python API tests and Android unit/debug builds.
- [x] Update storyboard, TTS/upload copy, hashes and actual progress evidence.
- [x] Commit/publish small files to PR #6, verify remote tree and final CI, deliver
  the revised MP4/SRT paths without uploading the movie or publishing to YouTube.

Delivery: existing PR #6, main-based feature branch; local motion-v2 MP4/SRT.
