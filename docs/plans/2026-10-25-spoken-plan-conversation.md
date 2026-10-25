# Spoken plan conversation implementation plan

> Use executing-plans inline, test-driven-development and one fresh whole-branch review.

Goal: hear the current computed plan, speak a refinement and explicitly approve
departure/home by speech, in both the native app and full-Pleos film.

## Approved design

The owner explicitly requested app+film implementation and keys later. Reuse the
isolated Git Flow feature branch and open PR8. Preserve the shared service UI and
one small truthful sample-source badge. No further approval checkpoint is needed.

Use Pleos TextToSpeech3.3.1 in ON_DEVICE mode (explicitly avoid default HYBRID).
It needs no app registration/network key. Existing server-only OpenAI/Kakao
configuration remains the later credential path. No keys are inspected/provisioned.
Plan narration is rendered from actual solver fields, not free-form invented
LLM prose: ordered exact venues, dwell/completion limits, home deadline/ETA,
optional omissions and uncertainty. Do not speak a feasible claim for unknown data.

An explicitly started voice conversation continues: final request → compute →
speak current summary → listen after confirmed playback completion → refine or
approve. Cancel/background/restriction clears continuation; no mic/approval is
restored. Stop output before input; a failed stop must not start approval listening.
Keep replay, manual reply and touch approval recovery paths.

Approval is a local exact explicit-command policy, never LLM authority. Reject
bare assent, negation, questions, quoted/mixed/conditional approval. Bind each
voice turn to the displayed revision and next destination, consume once, and
recheck parked/fresh/current plan and feasibility immediately before navigation.
Home phrases cannot skip outstanding required errands. Arrival is still separate
from explicit completion. Support remaining-plan refinements without clearing
completed ids or the current post-completion origin.

Sample speech inputs remain independently selected saved utterances, clearly
identified; they cannot authorize real navigation. Real ASR success must not be
claimed from prepared samples. Film must include full left Pleos/bottom controls.
Try actual native TTS/output capture; if editorial reconstruction is needed, label
it explicitly and never represent it as captured device speech.

## Tasks

- [x]1. RED/GREEN pure narration and spoken command/turn tests: unknown/infeasible,
  mandatory/home boundaries, negation/mixed/question, stale/duplicate approval.
- [x]2. Add native output adapter, lifecycle/audio-turn coordination and conversation
  UI. Preserve completed errands/origin on live refinement. Run all Python and
  Android unit/debug/release checks, commit intermediate changes.
- [x]3. Exercise actual official AVD output/errors/cancellation/Drive restriction,
  record one voice explanation→refinement→voice departure→home journey. Generate
  local-only media, inspect frames/captions/provenance and actual full playback.
- [x]4. Fresh review, fix material findings, update progress/screenshots/PR description,
  publish exact trees and verify final CI. Keep video/audio/APKs ignored.

References: [Pleos modes](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/TextToSpeech/intro),
[lifecycle](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/TextToSpeech/initialization-and-release),
[events](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/TextToSpeech/event-listeners).

Local implementation/media review and playback are complete. Final remote head/CI evidence is maintained in PR8.
