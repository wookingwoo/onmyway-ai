# Submission drafts

Official briefing transcript, judging, submission, development, FAQ and notices
rechecked 2026-11-03 KST. Nothing has been submitted or published by this work.
Korean project text and presentation screens contain no entrant identity.
Identity, consent, signature, access grants and final submission remain user actions.

- `application-ko.md`: current anonymous official project section 02 items 1–8.
- `pitch-and-demo-ko.md`: first scene, five-minute talk, demo facts and Q&A.
- `../contest-strategy-ko.md`: official criteria/research, alternatives, target,
  business assumptions and launch gates.
- `../demo-guide-ko.md`: current key-free operation, reset, recovery and recording.
- `checklist-ko.md`: evidence, anonymity and user-controlled submission gates.
- `email-draft-ko.md`: unsent draft with placeholders; recheck all filenames first.
- `briefing-review-ko.md`: timestamped briefing findings, evidence gaps and technical Q&A cards.

Historical submission/media paths below are excluded from Git and absent from
this worktree; playback/signatures were not reverified. New debug/unsigned release
APKs were built under apps/automotive/app/build/outputs/apk. See
[location implementation and actual checks](../location-setup.md).

- `artifacts/evidence/spoken-plan/OnMyWay_AI_spoken_demo.mp4`: 164.355542 seconds,
  actual Pleos ON_DEVICE Korean output, offline prepared user speech and synthetic
  places/roads/simulated guidance. Full Pleos surface, matching 1x cuts, abbreviated
  operation waits. It predates the subsequent native navigation handoff changes;
  compare it with the final APK before submission.
- `artifacts/evidence/spoken-plan/media-verification.json`: recorded decode,
  scene review and uninterrupted unmuted browser playback evidence.
- `artifacts/pitch/current/index.html`: seven self-contained Korean slides with
  five embedded actual screenshots, generated with `scripts/build-pitch.py`.
- `apk/OnMyWay_AI_demo_release.apk` and `apk/OnMyWay_AI_live_release.apk`:
  recorded signed local releases. Rebuild the final chosen source and recheck hashes.

Recorded 2026-10-26 checks: Python 67 tests, Android 106 per variant, demo/live
debug/release builds and release lint-vital, simulated journey and Drive/Park.
Native navigation screen dispatch/foreground was observed; tiles returned HTTP401
and the map was blank. This is not accepted-route or arrival evidence.
No final Korean STT, live OpenAI/Kakao call, actual route acceptance/arrival,
physical-vehicle test, user research or measured benefit is asserted.
See `../readiness.md` and `../progress.md` for exact scope.

## Superseded local binaries

The downloaded official form remains `artifacts/submission/official-template.docx`.
Its original five pages and the earlier filled draft were visually checked through
Word/Poppler on 2026-10-23. The old filled DOCX, six-slide PPTX, old 47/135-second
videos and review ZIP predate the current flow and business assumptions. They are
historical drafts and must not be submitted as current materials. Rebuild official
forms from the latest `application-ko.md`, preserve identity/consent/signature
blanks for the user, then visually recheck every page. The historical packager now
fails closed instead of exporting stale documents. It exports nothing.

A fresh clone includes sources, scripts and selected authorized actual screenshots,
not APKs, video, HTML pitch or office binaries. GitHub owner/commit identity has
not been anonymized. User must determine anonymous sharing and judge access
before placing repository links into the official application. Nothing here grants
access or changes repository visibility. Recheck official notices before sending.
