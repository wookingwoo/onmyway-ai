# Synthetic decision evidence and observed-user protocol

These files provide reproducible synthetic examples and a proposed observed-user study. No recruitment, interviews, participant measurements, live-provider performance, time savings or retention have been established.

## Reproduce the comparison

From this repository root, with API dependencies installed:

```powershell
$env:PYTHONPATH = (Resolve-Path services/api).Path
python scripts/evaluate-errand-decisions.py --check
python scripts/evaluate-errand-decisions.py --check --json
python scripts/evaluate-errand-decisions.py --check --output artifacts/evidence/contest-core/decision-evaluation.json
```

Use your configured virtual environment's Python executable. The script imports this checkout's pure `onmyway.planner.plan` and `PlanRequest`. It makes no network calls, reads no credentials or environment files, and calls no provider services. Every place and road source explicitly carries `is_fixture: true`. Dummy coordinates never establish travel time. `--check` exits nonzero on a literal hand-calculated expectation mismatch. JSON includes complete inputs, expected outcomes, both timelines and the planner response. Reports stay local in the ignored evidence directory; `--output` rejects other destinations.

The baseline selects the remaining task with the shortest next directed road time, breaking ties by errand/place ID. It visits all requested tasks without lookahead or optional omission. It calculates road time, waiting until an opening interval that fits the full dwell, dwell, per-task completion deadlines and the final home leg, then reports violations and unknown availability. It continues the timeline after a deadline miss so the violation and eventual home arrival remain visible. This is a deliberately simple nearest-next rule, not a commercial maps implementation or observed human behavior. The solver can reorder and omit optional tasks: the comparison demonstrates different decision policies, not equal-feature routing performance.

All cases depart at 18:00 +09:00 on 2026-10-25. Directed road minutes: origin→near 2, origin→pickup 8, origin→home 10, near→pickup 8, near→home 5, pickup→near 8, pickup→home 8. Near opens 18:05–20:00 with ten minutes dwell. Pickup opens 18:00–20:00 with five minutes dwell and must finish by 18:15. Case 3 removes pickup's opening interval and marks opening/service availability unknown.

| Synthetic case | Nearest-first calculation | Solver calculation | Demonstrated boundary |
| --- | --- | --- | --- |
| Both mandatory; home by 18:45 | Near: arrive 18:02, wait 3m, finish 18:15. Pickup: arrive 18:23, finish 18:28, thirteen minutes late. Home 18:36. | Pickup 18:08–18:13; near 18:21–18:31; home 18:36. | Home arrival alone cannot establish mandatory completion success. |
| Near optional; home by 18:25 | Same route; misses pickup and arrives home eleven minutes late. | Omit optional near task with reason; pickup 18:08–18:13; home 18:21. | Optional omission protects mandatory and home conditions. |
| Pickup only, unknown availability; home by 18:20 | Hypothetical pickup 18:08–18:13; home 18:21, late; availability unverified. | No accepted route. A +60s proposal retains `requires_verification` and availability warnings. | Unknown availability cannot prove feasibility, including after a conditional extension. |

Three hand-selected cases do not estimate average quality, failure prevalence, real traffic, provider accuracy, runtime or successful field errands. Fixture `feasible` means only that supplied synthetic constraints fit. No user-time savings are calculated. A proposal never changes a nonextendable deadline or grants approval. Live provider and Android integration require separate verification when authorized credentials and hardware are available.

## Proposed exploratory observation (not yet conducted)

Use the Korean moderator instructions, task cards and blank tables in [user-study-ko.md](submission/user-study-ko.md). Plan for five to eight adults with recent experience combining errands on the way home. Recruitment, consent and recording are future human actions; this document sends no invitations.

Run a seated or parked session, never a driving experiment. Allow 30–45 minutes including consent, equal practice in both conditions, six short tasks and discussion. Condition M uses a map/manual worksheet; condition A uses the app. Both receive the same frozen directed road matrix, exact venues, hours, service information, dwell and deadlines. Allow a calculator and notes in both conditions. A static venue schematic plus the road matrix is sufficient; disclose the limited map realism. Do not let live map recalculation silently change one condition's facts.

Before collecting paired results, verify that the app can load every card through an independently selected, visibly labelled synthetic session with the exact same venue IDs, road times, availability and constraints. The CLI is not an app fixture loader. Do not substitute an existing built-in example with different inputs. If equivalent app fixtures cannot be prepared within the authorized workflow, record comparison readiness as blocked instead of collecting purported paired results. No provider failure may silently trigger fixture use.

Use isomorphic packets X and Y, renaming venues while preserving numbers, exact-venue restrictions and uncertainty. Cycle these four assignments; their counts should differ by at most one:

| Assignment | First condition | Second condition |
| --- | --- | --- |
| 1 | Manual X | App Y |
| 2 | App X | Manual Y |
| 3 | Manual Y | App X |
| 4 | App Y | Manual X |

Rotate scenario order across participants (1–2–3, 2–3–1, 3–1–2); keep that order within a participant's two conditions. Give no correctness feedback between conditions. Isomorphic repeats still introduce learning effects; balancing cannot eliminate them. Record carryover, assistance, failures and deviations rather than discarding inconvenient observations.

Start the task clock after reading the card and presenting empty input/worksheet; stop after the final decision and explanation. Record setup/input time, review/decision time, total time, edits, help and timeouts separately. End a task after six minutes and label it unfinished. Record stop order, mandatory completion times, home arrival, omissions and approved changes. Score against the frozen facts, including wait, dwell and home travel. Ask confidence before revealing correctness. False confidence means claiming verified feasibility despite a mandatory/home violation or unknown availability.

Ask the exact-venue and nonextendable-deadline questions printed in Korean. Record explanations of omissions, changed constraints, verification, arrival versus completion and approval of the current plan; a rating alone cannot establish comprehension. Separate verbatim responses, observed actions and moderator interpretation.

Report participant-level paired differences, descriptive medians/ranges, misses, false-confidence events, comprehension, unfinished tasks and help. State sample, device/build, fixture conditions, order, missing data and learning effects. Five to eight convenience participants cannot establish statistical significance, population time savings, product-market fit or retention. Never combine synthetic-case and participant counts.

Only with separate follow-up consent, ask after seven to fourteen days whether a real opportunity arose, whether the participant actually reused the app and what happened. Separate actual use/self-report, intention, opportunity and inability to run the live app. This is exploratory reuse observation, not a retention rate. Store notes and optional recordings privately with a deletion date; publish only authorized de-identified findings later.
