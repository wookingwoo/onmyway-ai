# Immersive story demo design

The user approved the life-situation plus actual-app approach and explicitly asked for autonomous production, a Git Flow feature branch, incremental commits and a screenshot-rich PR. This records the concrete production decisions without another approval round.

## Intent and approach

Make one anonymous commuter's evening feel relatable: leave at 18:00, collect laundry before 18:30, buy medicine, optionally buy milk, and be home for dinner at 19:00. Use restrained motion typography around fresh actual native footage and a warm Korean explanatory narrator. A screen-only walkthrough would make the need less immediate; a cinematic advertisement would leave too little evidence. The selected hybrid keeps the app actions central.

## Story and verified arithmetic

Explicitly select the prepared clarification example, answer that 18:30 means pickup completion, set home to 19:00 and pin the example laundry branch. The initial synthetic plan ends at 18:40. Before departure, the user realizes the pharmacy consultation needs 30 rather than 10 minutes and edits that duration. Existing calculations then put laundry first (18:06 arrival, 18:11 completion), pharmacy second (18:16 arrival, 18:46 completion), omit optional milk, and predict home at 18:53. The 19:00 deadline stays unchanged. This is an explicit user correction, not an observed delay or live traffic update. Show the two mandatory completions and separately approved home arrival. Actual AVD Drive restrictions appear during the same simulated journey, followed by Park restoration.

## Production boundaries

- Target approximately three minutes, 1920x1080 H.264/AAC, Korean narration and readable captions. Length follows actual voice timing and footage readability; the final edit is 183.3 seconds.
- Real app captures are the sole source of UI. Editorial situation cards and route diagrams are clearly separate from the app viewport. No invented app screens, notifications, live speech recognition, successful real navigation or traffic claims.
- Keep example/simulation disclosure readable throughout; identify narration as explanatory AI voice. Show missing/live-verification scope in the end card.
- Preserve one coherent journey. Capture approval, arrival, completion and next preview as separate states. Count dwell and home travel; never remove a mandatory errand or silently change a deadline.
- Screen recordings, generated audio, rendered video, frame sheets and raw verification files stay local in ignored artifacts/evidence/story-demo/. Track only scripts, prose and selected reviewed PNGs under docs/screenshots/story-demo/. No credentials, identity, access grants, publication or submission.
- Final narration uses one available Korean Microsoft Edge speech voice via the optional edge-tts production client. Connected ElevenLabs quota and Artlist download entitlement prevented their use; restricted previews were not downloaded or incorporated. App STT/LLM/route providers remain untested.

## Acceptance

Fresh AVD recording asserts the changed order, optional omission, 18:53 home estimate, explicit completions, home arrival and actual driving lock/restoration. Renderer checks input provenance, audio presence, chapter timing, captions and full decode. Inspect frames at every chapter and play the final local film. PR documents exact evidence and remaining live limitations. Do not promise contest ranking.
