# YouTube Shorts implementation plan

> **For agentic workers:** Execute inline using superpowers:executing-plans.
> The owner has authorized concept through production and PR in this session.

**Goal:** Produce an honest 54-second silent Korean YouTube Short and matching SRT.

**Architecture:** A single declarative scene timeline drives Pillow artwork,
FFmpeg assembly and SRT output. Real existing native footage remains an explicit
external input; generated media remains ignored and local.

**Tech Stack:** Python 3, Pillow, FFmpeg/FFprobe, standard-library unittest.

**Spec:** `docs/superpowers/specs/2026-10-25-youtube-shorts-design.md`

## Global constraints

- 1080×1920, 30 fps, H.264/yuv420p, 30–60 seconds, no audio stream.
- Korean contest copy; English source, engineering documentation and commits.
- Visible fixture/simulation boundary, actual app footage only, no public upload.
- MP4 and all render intermediates remain ignored under `artifacts/shortform/`.

## Review focus

- Missing or changed source footage must stop production rather than fabricate it.
- Crops and captions must remain within the actual frame and safe text margins.
- SRT must be UTF-8, ordered, non-overlapping, bounded and comfortable to read.
- Changed deadlines must not look like one impossible continuous successful run.
- Evidence must distinguish emulator restriction from unverified live providers.

### Task 1: Research and production contract

- [x] Confirm official contest/YouTube guidance and existing actual source material.
- [x] Write the design and this plan; ignore generated shortform media.
- [ ] Inspect diff and commit the research/production contract.

### Task 2: Timeline and reproducible production

**Files:** `assets/shortform/timeline-ko.json`, `scripts/build-shortform.py`,
`scripts/tests/test_shortform.py`, `artifacts/evidence/OnMyWay_AI_Shorts_ko.srt`.

- [ ] Test timing, SRT encoding/cue boundaries, source checks and crop validation.
- [ ] Run `python -m unittest discover -s scripts/tests -p test_shortform.py -v`;
  expected initial failure, then all tests passing after implementation.
- [ ] Implement and render the 54-second video using explicit original footage.
- [ ] Inspect scene frames; correct any visual or semantic defects and regenerate.
- [ ] Commit source, timeline and captions after relevant checks pass.

### Task 3: Verify, document and deliver

**Files:** `docs/submission/shortform-ko.md`, `docs/shortform-verification.json`,
`docs/progress.md`, `docs/submission/application-ko.md`, `docs/readiness.md`.

- [ ] Full FFmpeg decode, FFprobe contract, SRT parser/timing and visual/phone QA.
- [ ] Run Python suite and available Android unit/build checks; record actual results.
- [ ] Browser playback to ended state with no media error; review the whole branch.
- [ ] Update Korean storyboard, later-TTS guidance, upload metadata draft and evidence.
- [ ] Commit small files, audit the entire main-to-head diff for binary deliverables.
- [ ] Push through connected GitHub, create and attach a PR targeting main; preserve
  local generated MP4/SRT and give the owner direct paths. Do not merge or publish.
