# Contest core readiness design

User authorization: implement the previously reviewed improvements today in a new worktree, commit incrementally and open a reviewable PR with actual screenshots. Keys will be supplied later. This document records implementation choices, not claims of winning or completed live validation.

## Scope
Complete the small native Android + FastAPI errand flow: select and retain the exact pickup venue; collect explicit availability evidence; recheck elapsed time before approval; recover navigation requests; refresh the remaining plan after explicit completion; show uncertainties and low margin plainly. Keep existing example flow independently selected and labelled.

## Contracts
- Keep ErrandInput and the LLM parse schema unchanged. Introduce DiscoveryErrand(ErrandInput) with selected_place: Place | None = None; Errand extends DiscoveryErrand with candidates.
- Add Place.address (default empty) and Place.availability_evidence (optional). AvailabilityEvidence contains basis (nonblank, <=500 chars) and confirmed_at (aware datetime). Verified live availability without a fresh, nonfuture basis stays needs_verification; maximum age 24 hours. Fixtures remain labelled and exempt.
- POST /v1/places accepts query (1..200 chars) and origin: Location, returns places: list[Place] up to three Kakao results. Only server-side Kakao key is read. Results keep hours/service unknown.
- Discovery preserves selected_place identity, coordinates, source and evidence, and skips its keyword search. Fixture selected places are rejected by live discovery. Unselected legacy requests retain existing one-result behavior; the native live workflow requires explicit venue selection before planning.
- Conditional deadline proposals may be returned for routes needing verification, carrying requires_verification and warnings. A proposal never upgrades the plan status, invents missing road data or changes user conditions. The native dialog names this uncertainty.
- Native selection uses fixture candidates only in explicit example sessions; live uses /v1/places. Evidence is entered by the user with an explicit basis, time window and service confirmation, not inferred from search. Changing query/place invalidates previous evidence and approval.

## Timing
Bind approval to request revision/destination, vehicle state, monotonic computation start, departure, final deadline and the smallest remaining margin among home, per-errand completion and applicable closing intervals. Computation and user review elapsed time count. Expired/unknown bounds require recalculation. Show low/zero margin as a caution, not reliable real-world success.

## Navigation and completion
Keep arrival separate from completion, and home separately approved. Add a pure request lifecycle policy with bounded response timeout and generation guards. Never accept an uncorrelated callback as arrival; provide explicit cancellation/recalculation and avoid restoring approval. After explicit completion, retain selected venues and automatically request a new preview while parked; the new destination still needs approval.

## Evidence and delivery
No credentials are provisioned, copied or used. Missing live providers keep explicit errors. Tests cover pinned identity, availability provenance/freshness, uncertain proposals, elapsed/slack boundaries, timeout/cancel and stale events. Verify Python suite and Android demo/live unit tests/build/lint. Capture actual changed screens on the isolated official AVD with clearly labelled examples. Track only authorized PNGs under docs/screenshots; APKs/logs remain local. Update docs/progress.md, provider setup notes and PR with actual verification and remaining live/user-research limits.

