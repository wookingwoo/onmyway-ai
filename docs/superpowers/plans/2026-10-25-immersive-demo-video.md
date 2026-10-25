# Immersive Story Demo Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Deliver a compelling, honest Korean story film of the existing native errand flow, with reproducible sources and a reviewable PR.

**Architecture:** Record real actions on the isolated official Pleos AVD. Assemble labelled actual footage, editorial motion graphics, Korean narration and subtitles using local Python/Pillow/FFmpeg. Product behavior remains unchanged.

**Tech Stack:** Python, Pillow, adb, FFmpeg/ffprobe, optional edge-tts editorial narration.

**Spec:** docs/superpowers/specs/2026-10-25-immersive-demo-video-design.md

## Global Constraints

- Korean UI and contest prose; English engineering docs, source and commits.
- Large media stays local/ignored; only reviewed actual screenshots are tracked.
- Explicit fixtures and simulated guidance; no silent provider fallback or unperformed live claims.
- Preserve mandatory errands, exact venue, completion/deadline semantics and separate home approval.
- Use feature/immersive-demo-video; PR only, no merge or contest publication.

## Review Focus

- One journey retains its departure/deadline, venue and completed IDs across edits and cuts.
- Consultation duration is a user correction before travel, not automatic detection of traffic/waiting.
- Captions remain readable and do not cover native controls or disclosures.
- A missing/truncated recording or missing narration must fail assembly, never produce a successful report.
- Media outputs, signed URLs and any credentials must remain outside tracked changes.

## Task 1: Scenario and capture

Files: docs/submission/story-demo-ko.md, scripts/record-story-demo.py, .gitignore.

- [x] Record the Korean shot/narration source and provenance rules.
- [x] Record fresh clarification, pinned venue, baseline, duration correction, revised route, real driving lock, two explicit completions and home approval/arrival.
- [x] Assert 18:53, mandatory order and optional exclusion, save local capture manifest and unedited screenshots.
- [x] Commit scenario and recorder.

## Task 2: Narration and local film

Files: scripts/build-story-demo.py, scripts/story_demo.json, scripts/tests/test_story_demo.py.

- [x] Pin meaningful renderer boundaries with failing tests: missing audio, untrusted sources, invalid time maps and mixed timebases.
- [x] Generate one Korean narration take per chapter into local files and record anonymous model/voice provenance without signed URLs.
- [x] Render a coherent 1080p film, SRT/ASS, chapter frames, a local player and machine-readable verification.
- [x] Run boundary checks, ffprobe, full decode, all-chapter frame review and actual playback/technical audio checks; repair material issues.
- [x] Commit reproducible renderer and story source.

## Task 3: Evidence and PR

Files: README.md, docs/demo-guide-ko.md, docs/readiness.md, docs/progress.md, docs/screenshots/story-demo/.

- [x] Update authoritative current-media references and capture provenance; avoid changing historical records.
- [x] Run relevant Python and native checks and independent whole-branch review; resolve material findings.
- [x] Commit reviewed small PNG evidence and notes; verify no large video/audio or private files are staged.
- [ ] Publish incremental feature commits, create and attach PR with screenshots, verify CI and image rendering.
