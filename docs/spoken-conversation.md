# Spoken plan conversation

The native app reads the actual computed plan, then accepts a condition change
or an explicit departure command. The same UI serves live and independently
selected sample sources. Only the small source badge and saved-text disclosure
identify samples; no alternate demo layout is used.

Start with **말로 부탁하기**. A completed real Pleos transcript submits
automatically. Exact pickup venues and missing/ambiguous constraints still need
confirmation; flexible purchases can be discovered automatically. After the
calculation, Pleos reads ordered venues, dwell/completion times, required finish
deadlines, skipped optional errands and home ETA/deadline. Unknown opening,
service or road evidence is explicitly uncertain rather than spoken as feasible.

In an explicitly started conversation, confirmed playback completion opens the
reply turn. Say a refinement such as **약국에서 30분 걸릴 것 같아**, or explicitly
approve **이 계획대로 출발해**. The command classifier grants departure authority
only to whole allowed phrases. Bare assent, questions, quoted/conditional/mixed
commands, negation and duplicate finals cannot authorize navigation. Each turn
belongs to one revision and next destination; native Park, fresh timing, current
plan eligibility and all mandatory errands are checked again at dispatch.
**집으로 출발해** works only when home is the current next destination.

Arrival does not complete an errand. Confirm **완료했어요**, hear the remaining
plan, then explicitly start **말로 대답하기** to approve its next destination.
Condition changes preserve completed ids and the current post-completion origin.
Replay **계획 듣기** does not open a microphone. Touch approval remains available.

## Output and cancellation

`ai.pleos.playground:TextToSpeech:3.3.1` explicitly uses `Mode.ON_DEVICE`, Korean
locale and navigation-speech audio attributes. It does not select default HYBRID,
register a cloud app or need a speech API key. The manifest declares
`pleos.car.permission.TTS_SERVICE`. An unavailable/failed service shows a bounded
failure with replay/manual recovery; it never invents audio or provider success.

Speech output and input are half duplex. A reply waits for confirmed output
completion or stop. Cancel, edits, leaving the route screen, backgrounding and
restricted/unknown vehicle state invalidate continuation. Late callbacks cannot
reopen input or approve another plan. Returning to Park never restores a mic.
The actual AVD check interrupted speech at Drive/30; final one-second PCM peak
was 4/32768. Cancel and background/return left the reply dialog closed.

## Keys later

No provider key is needed for local Pleos output or the explicitly chosen sample.
Later copy the root `.env.example` to ignored `.env`, set `OPENAI_API_KEY`,
`OPENAI_MODEL` and required Kakao keys, and launch the server through
`scripts/start-api.ps1`. Only the server reads that file. Never put credentials in
Android source, BuildConfig, APKs or screenshots. Live failures do not select a
fixture. No credential was inspected or provisioned for this change.

## Evidence limits

Demo/live each pass101 native unit tests; all67 Python API tests are mocked.
Both debug/release builds and release lint-vital pass. The official AVD completed
actual ON_DEVICE Korean output, prepared spoken-command handler approvals,
remaining-plan/home flow and Drive/cancel/background checks. Prepared sample
utterances are saved text, with offline Windows speech added only in the film.
They do not establish actual final Korean ASR, LLM, Kakao, real native navigation
acceptance/arrival or physical-vehicle usability. Those live checks remain open.

[Korean scenario](submission/spoken-demo-ko.md) · [current screenshots](screenshots/spoken-plan/README.md)
