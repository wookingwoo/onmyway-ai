# Pleos Voice Demo Implementation Plan

> **For agentic workers:** Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Demonstrate speech-draft confirmation feeding the existing errand flow.

**Architecture:** A small pure Kotlin session model protects recognition results.
A Pleos SDK adapter manages callbacks and timeouts. MainActivity renders a native
voice dialog and keeps the existing live parse and explicit fixture paths.

**Tech Stack:** Kotlin Views, Pleos SpeechToText, JUnit, existing FastAPI.

**Spec:** `docs/superpowers/specs/2026-10-24-pleos-voice-demo-design.md`

## Global Constraints

- Korean UI; English source, engineering docs and commits.
- Actual PARKED state for microphone access; cancel on restriction or background.
- No Android secrets, automatic navigation, implicit completion or silent fixtures.
- APKs, audio, recordings and generated evidence stay local.

## Review Focus

- Late callbacks after cancellation must not replace the current draft.
- Empty/partial transcripts must not be applied as final results.
- Existing text survives cancellation and errors.
- Example transcript selection must never enable real navigation or live claims.
- Bounded service connection and recognition failures keep text entry usable.

## Task 1: Recognition session behavior

Files: new `VoiceInputSession.kt`, `VoiceInputSessionTest.kt`; add pure model to
`safety-tests/build.gradle.kts` source includes.

- [ ] Write failing tests for actual-park gate, partial/final, confirmation,
  cancellation, stale/duplicate results and example provenance.
- [ ] Run tests and observe the missing behavior; implement the pure model.
- [ ] Run the focused tests and full Android unit suites.

## Task 2: SDK adapter and native UI

Files: new `PleosSpeechInput.kt`; modify MainActivity, Gradle dependencies and
AndroidManifest permissions; add demo-only sample input where appropriate.

- [ ] Inspect the actual SDK signatures and use ON_DEVICE without secrets.
- [ ] Implement bounded connect/listen, cleanup and safe callback routing.
- [ ] Add transcript review, explicit apply/cancel, real-SDK errors and a labelled
  independent example flow. Existing text input and fixture action stay usable.
- [ ] Build both variants and inspect the actual emulator UI and SDK response.

## Task 3: Demo and evidence

Files: voice smoke script, README/Android guide and `docs/progress.md`.

- [ ] Exercise example transcript confirmation and complete fixture journey.
- [ ] Check driving/background cancellation and actual SDK connection behavior.
- [ ] Run Python tests, Android demo/live unit tests and debug/release builds.
- [ ] Record commands, results, limitations and how to run the demo; review diff.
