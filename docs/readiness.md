# Submission readiness

**Status: PARTIALLY READY.** This is a functioning, tested native application with
reviewable submission drafts. It is not a verified live errand service or a
submitted contest entry.

| Requirement | Actual evidence |
| --- | --- |
| Deterministic planner and API | 25 Python tests pass; real localhost HTTP health/demo/plan/missing-key smoke passes |
| Native state, journey and approval policy | 29 tests per Android variant pass, including voice confirmation, stale speech callbacks, example provenance and existing journey/approval boundaries |
| Voice input | Pleos STT 3.3.1 ON_DEVICE integration; actual SDK initialization/listening and cancellation/no-audio paths checked; final Korean transcription still unverified |
| APK | Both demo/live release APKs built, v3 signatures verified, installed on Pleos API 34 and launched |
| Vehicle restrictions | Actual CarUxRestrictions reads; Virtual Console Drive / 30 km/h disables controls even in demo; restored Park / 0 |
| SDK | NaviHelper 3.0.5 actual service connection confirmed; route request acceptance and arrival not verified |
| End-to-end demo | Staged native journey; actual HTTP fixture -> dwell edit -> plan -> separately approved simulated stops -> explicit completion/replan -> separately approved final home |
| UI evidence | Actual day/night, input/review/route and driving-lock captures under docs/screenshots/automotive-ux; native date/reset and real open-dialog driving restriction checks |
| Recording | Current 135.7-second silent new-UI recording, 18-cue Korean SRT, full decode and stage/caption checks passed; videos local, authorized SRT versioned |
| Application | Official form section 02 filled, identity/consent/signature unchanged, all five pages visually checked |
| Pitch | Six editable Korean slides with actual evidence and explicit unverified scope; visual/file checks passed |
| GitHub | Private repository; PR #1 merged after history cleanup; automotive UX PR #2 open for review with six authorized actual screenshots, all images rendered successfully |
| CI | GitHub Actions Verify run 39996479748 passed on documentation/subtitle head fb3707b; UX implementation df674a4 also passed run 39988320825 |

## Remaining work

1. The user will add server credentials later: `OPENAI_API_KEY`, a compatible
   `OPENAI_MODEL`, `KAKAO_REST_API_KEY` and `KAKAO_MOBILITY_REST_API_KEY` in ignored
   root `.env`, then restart `scripts/start-api.ps1`. No key needs to enter chat.
2. Run real provider smoke tests. Verify account permissions, response shape,
   latency, coordinates and road-time results. The current Local adapter returns
   one nearby candidate per errand and does not provide business hours or service
   availability. Those remain explicitly unknown, requiring separate verification.
3. Run a fresh live plan against verified real destination data and validate
   actual Pleos route acceptance and arrival. The SDK callback matching uses
   destination names, not a unique request ID; test unrelated/same-name routes.
4. Review the submission materials in the existing private repository. PR #1 is
   merged. Do not grant reviewers access without user authorization. Confirm how
   the organizer's anonymity rule applies to repository
   owner names before placing a repository URL in section 02.
5. The user reviews their identity, signs required consent, checks final organizer
   updates and sends the final email. No email has been sent, and nothing has been
   publicly deployed.

No current users, measured time savings, retention or hiring benefits are claimed.
Generated APKs, recordings, office documents and archives remain local. The owner
explicitly authorized selected actual screenshots in Git for the UI review.
The owner also authorized the small Korean new-UI SRT. GIF conversion previews
remain local after the owner declined uploading them.
The old local review ZIP predates this redesign and must be regenerated before
sharing. GitHub can still serve unreachable old-SHA objects; immediate
server-side cache deletion has not been established.
Business metrics in the proposal are labelled hypotheses. A larger alternate-route
list, TTS, Gleo app-schema integration, auto-location and production hosting are not part of this quick
delivery. The implemented alternative is a concrete deadline extension when valid.

See `artifacts/evidence/README.md`, `docs/submission/checklist-ko.md` and
`docs/progress.md` for exact evidence and reproduction commands.
