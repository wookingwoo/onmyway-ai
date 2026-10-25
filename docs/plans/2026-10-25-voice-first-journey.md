# Voice-first journey

Approved scope: the owner selected improving both the actual app and demo film.

## Design

One parked utterance starts interpretation, candidate discovery and route calculation automatically. The first useful destination is a reviewable route summary, followed by the existing current-plan departure approval. The transcript is visible in the summary, with speech available for missing conditions and refinements. Text and detailed editors remain recovery paths.

Replaceable purchases (for example a nearby pharmacy or grocery) may be proposed automatically. An existing pickup, reservation or explicitly designated venue remains fixed and requires an exact user-selected venue; AI must not substitute the nearest shop. Missing home time, location setup, ambiguous requirements and provider errors stop automatic progress visibly. Unknown opening/service/road data never establish feasibility.

Refinements reuse stable errand ids and conservatively retain mandatory errands and deadlines. Speaking a longer dwell duration works. Relaxing constraints remains an explicit editor operation in this iteration. Unchanged pinned venues survive interpretation; changed queries invalidate the pin. Departure approval, arrival/completion separation, fresh replans, home approval and parked-only real speech stay intact.

The independently selected example conversation runs the same native orchestration and planner with labelled prepared data. It contains the exact pickup branch, completion deadline, pharmacy dwell, optional milk and home deadline in one request. A prepared spoken refinement changes the pharmacy dwell to 30 minutes and automatically recalculates. Example speech is never sent to a live parser or presented as measured ASR/LLM performance.

## Implementation plan

1. Add and test a conservative `place_mode` contract (`fixed` by default, `flexible` only when explicitly extracted). Live discovery rejects unselected fixed pickups, proposes up to three flexible candidates and preserves provider failures. Clarification can refine fixed search queries without dropping mandatory errands or weakening deadlines.
2. Add pure orchestration/continuation tests, consume final speech once without a transcript confirmation, automatically parse/advance/calculate, support spoken answers/refinement, preserve pins and expose a complete summary. Keep every navigation safety gate.
3. Run backend and Android tests/builds, record a fresh native example journey, rebuild the local film around one request and one spoken correction, verify playback/provenance and commit selected actual screenshots.
4. Obtain a fresh whole-branch review, fix material findings, update progress and the existing feature PR with truthful evidence and screenshots; verify CI. Keep large media local.

## Rulings

- Reuse `feature/immersive-demo-video` and its isolated worktree: PR #8 is still open and this request changes its demo/product scope. Rewrite the PR around the final result rather than creating a dependent duplicate.
- Keys are supplied later, as explicitly requested by the owner. Do not inspect/provision keys or make live OpenAI/Kakao calls. Mocked provider tests and labelled examples are the available evidence.
- Proceed under the owner's explicit app-and-video selection and prior autonomous branch/commit/PR authorization; no additional design approval checkpoint is needed.
- Use inline execution plus one fresh final reviewer. Keep the existing small architecture and avoid account services, memory infrastructure or a new voice provider.
