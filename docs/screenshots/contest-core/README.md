# Actual contest core captures

Seven owner-authorized, unedited full-device PNGs from the locally signed demo
on the isolated official Pleos Connect 2.1.0 / API 34 AVD, 2026-10-25. Copies are
byte-identical to local originals under ignored artifacts/evidence/contest-core.
The final installed APK was built from local source commit b338e55; its SHA-256
is recorded in artifacts/submission/apk-checksums.txt. No APK is committed.

| Image | Actual evidence |
| --- | --- |
| review-pinned.png | Explicit example pickup venue stays selected in review |
| venue-selected.png | Exact pickup identity, example availability and selection notice |
| automatic-next-preview.png | First explicit completion automatically refreshes the next preview |
| conditional-proposal.png | Independently selected uncertain fixture; extension remains conditional |
| low-margin-uncertainty.png | Unknown feasibility and zero-margin caution remain visible after adjustment |
| night.png | Actual system night mode; simulation must be enabled again after recreation |
| driving-lock.png | Actual Drive / 30 km/h closes/blocks the venue dialog |

All errand text, venue identities, opening windows and road times are explicitly
selected synthetic examples fixed at 2026-10-23. Guidance and arrival are simulated.
No image represents live STT/LLM parsing, Kakao output or actual navigation success.
Only the vehicle restriction is an actual OS/CarUxRestrictions event. The official
Virtual Console was restored to Park / 0; the system theme was restored to day.

Reproduce the actual UI flow with scripts/smoke-contest-core.py, then the driving
restriction with scripts/smoke-pleos-lock.py. Use only the isolated OnMyWay_Pleos
AVD; the UI checker refuses to reset a live journey and checks that all provider
configuration booleans are false. Logs/XML/reports remain local. No identity,
credential, public publication or external access grant was added.
