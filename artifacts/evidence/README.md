# Verification evidence

The screenshots and recording are captured from the actual signed release app
running on the official Pleos Connect 2.1.0 x86_64 API 34 emulator.

- `pleos-live-connected.png`: live variant reads actual unrestricted vehicle UX
  state and connects to the actual Pleos navigation service. No route request
  or live AI call is established by this screenshot.
- `pleos-demo-plan.png`: explicitly labelled synthetic places, business windows
  and road times are returned by the actual local HTTP API and rendered natively.
- `pleos-demo-arrival-not-complete.png`: simulated arrival leaves the errand
  incomplete until explicit user confirmation.
- `pleos-demo-replanned.png`: first errand completed explicitly, remaining errands
  recalculated through the actual HTTP API. It does not use live traffic.
- `pleos-driving-lock.png` and `pleos-driving-check.json`: official local Virtual
  Console changed the isolated AVD to Drive / 30 km/h. Actual CarUxRestrictions
  locked editing and destination approval even with demo simulation enabled.
  The test restored Park / 0 km/h in its cleanup.
- `OnMyWay_AI_Pleos_fixture_demo.mp4`: actual screen recording with a persistent
  disclosure caption. The scene is not an AI response or real navigation demo.

Reproduction scripts: `scripts/smoke-android.py`, `scripts/smoke-pleos-lock.py`,
`scripts/record-demo.ps1`. Use an explicitly selected isolated test emulator.
All screenshots and recordings, including the unedited recording, remain local
and are excluded from Git. A fresh clone contains these notes and reproduction
scripts, not the binary evidence files.

Not yet verified: live OpenAI interpretation, live Kakao places/road responses,
actual Pleos route acceptance/arrival, physical vehicle operation, user research,
and effectiveness claims. Do not conflate service connection with successful
navigation. The live variant never falls back to these fixtures.
