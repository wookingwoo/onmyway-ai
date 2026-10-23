# Submission readiness

**Status: PARTIALLY READY.** This is a functioning, tested native application with
reviewable submission drafts. It is not a verified live errand service or a
submitted contest entry.

| Requirement | Actual evidence |
| --- | --- |
| Deterministic planner and API | 25 Python tests pass; real localhost HTTP health/demo/plan/missing-key smoke passes |
| Native state and approval policy | 13 Android Gradle unit tests pass, including stale-plan and arrival/completion boundaries |
| APK | Both demo/live release APKs built, v3 signatures verified, installed on Pleos API 34 and launched |
| Vehicle restrictions | Actual CarUxRestrictions reads; Virtual Console Drive / 30 km/h disables controls even in demo; restored Park / 0 |
| SDK | NaviHelper 3.0.5 actual service connection confirmed; route request acceptance and arrival not verified |
| End-to-end demo | Actual HTTP fixture -> timeline -> approved simulated navigation -> simulated arrival -> user completion -> remaining plan |
| Recording | Actual 47.206-second Pleos screen recording with persistent fixture/simulation disclosure |
| Application | Official form section 02 filled, identity/consent/signature unchanged, all five pages visually checked |
| Pitch | Six editable Korean slides with actual evidence and explicit unverified scope; visual/file checks passed |
| GitHub | Existing private repository confirmed through the GitHub plugin with read/write access. Local CLI uses different credentials and cannot access it; plugin upload/PR/CI is being prepared |

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
4. Upload to the existing private GitHub repository through the plugin and
   complete PR/CI. Do not grant reviewers access without user
   authorization. Confirm how the organizer's anonymity rule applies to repository
   owner names before placing a repository URL in section 02.
5. The user reviews their identity, signs required consent, checks final organizer
   updates and sends the final email. No email has been sent, and nothing has been
   publicly deployed.

No current users, measured time savings, retention or hiring benefits are claimed.
Business metrics in the proposal are labelled hypotheses. A larger alternate-route
list, voice input, auto-location and production hosting are not part of this quick
delivery. The implemented alternative is a concrete deadline extension when valid.

See `artifacts/evidence/README.md`, `docs/submission/checklist-ko.md` and
`docs/progress.md` for exact evidence and reproduction commands.
