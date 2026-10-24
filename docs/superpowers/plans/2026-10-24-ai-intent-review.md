# AI Intent Review Implementation Plan

> **For agentic workers:** Use superpowers:executing-plans inline, then request a fresh whole-branch review.

**Goal:** Make interpretation, clarification and grounded plan rationale visible
and demonstrable, with the verified result merged into main.

**Architecture:** Extend the existing parse request with bounded answers and prior
errands; keep strict validated output. A pure Kotlin IntentReview protects review
state and planning prerequisites. Native Views render it; PlanExplanation formats
existing solver evidence without another model call.

**Tech Stack:** Kotlin Views / org.json / JUnit, FastAPI / Pydantic / httpx.

**Spec:** `docs/superpowers/specs/2026-10-24-ai-intent-review-design.md`

## Global constraints

Korean UI; English engineering docs/commits. No embedded secrets or silent fixture
fallback. Mandatory errands, unknown-data warnings, vehicle gate and fresh approval
stay enforced. User authorized implementation, push, PR and final main merge.

## Review focus

- Answers must carry the original request/reference and retain every prior errand.
- Missing deadline and unanswered questions must never permit calculation.
- Editing original text invalidates interpretation and any previous plan.
- Prepared examples are labelled and independently requested, never a live fallback.
- Explanation must describe actual evidence without inventing exclusion causes.

## Tasks

- [ ] Backend: add QuestionAnswer and ParseRequest.answers/previous_errands;
  test request context, duplicate IDs and missing/downgraded errands RED/GREEN.
  Send bounded context as data, Korean questions/warnings, preserve strict schema.
  Run full Python tests; commit.
- [ ] Native: add IntentReview.kt and tests for pending questions, explicit
  deadline, original-text invalidation, answer payload and fixture isolation.
  Add persistent review/source context, native answer editor, independent example
  questions, explicit deadline selection and planning guard. Run Android units;
  commit.
- [ ] Explanation: add PlanExplanation.kt, test verified/unknown/infeasible,
  duration totals and non-causal skipped explanations; show native detail card.
  Run both variant unit/build/lint and full Python tests; commit.
- [ ] Evidence: signed demo install, actual parked/day/night/Drive checks, example
  clarification -> plan -> all completions -> home. Save selected screenshots and
  reproducible smoke; update progress/readiness/demo guide; commit.
- [ ] Fresh branch review; fix findings with regressions. Push, create PR with
  screenshot embeds, verify CI and images, merge to main and synchronize locally.

## Rulings

- Existing AGENTS.md explicitly says the owner supplies API keys later; do not
  provision OpenAI keys or stop independent implementation for credentials.
- User asked to continue to completion without design checkpoints; the agreed
  three improvements above are the approved scope. No additional worktree needed.
