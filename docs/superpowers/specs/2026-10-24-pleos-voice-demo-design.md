# Pleos voice input demo

The owner requested a feature branch implementing the previously discussed Pleos
STT approach, with a demonstrable errand flow. Work on `feat/pleos-voice-demo`.

## Approach

Reuse native Kotlin Views and the existing review/planning flow. Prefer Pleos
SpeechToText in ON_DEVICE mode: no provider secrets in Android and no server
registration for this mode according to the SDK guide. External cloud STT adds
credentials and audio upload; Gleo app-schema registration adds external setup.
Neither is necessary for this first demo. These are implementation choices, not
claims that the owner approved an unseen design or that recognition works yet.

## Behavior

- An explicit Korean voice-input action starts a bounded recognition session only
  when the actual vehicle state is PARKED, even in a simulated fixture session.
- Show connecting/listening/partial/final/error states, with cancel and retry.
- A final transcript is an editable draft. Apply only after explicit confirmation;
  never automatically parse, plan, approve a destination or complete an errand.
- Cancel on driving/unknown restrictions, backgrounding, screen changes or edits.
  Ignore late callbacks and duplicate final results from previous sessions.
- No live failure silently switches to an example. A separately requested,
  prominently labelled example speech transcript can exercise confirmation and
  the existing fixture plan without claiming live STT or LLM interpretation.
- Keep fixtures, real microphone input and provider status distinct. Current
  fixture locations/times are preserved only on an explicitly selected demo flow.
- Existing required errands, deadline accounting and fresh navigation approval
  remain unchanged. Keys stay on the server.

## Verification

Unit tests exercise stale results, cancel/lock, empty transcripts, explicit
confirmation and fixture isolation. Run Python tests and both Android variant
unit/build checks. Install on the isolated Pleos emulator; verify actual SDK
initialization, failure reporting, the labelled example confirmation and complete
fixture journey. Record exactly which real recognition steps succeed or remain
unverified in `docs/progress.md`; never present an injected example as live STT.
