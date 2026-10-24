# Intent review and grounded explanations

The owner authorized completing the previously proposed improvements, creating
branches, reviewing PRs and merging the verified result into main without further
checkpoints. Work in the existing checkout on `feat/ai-intent-review` from merged
main `979de875`; credentials and generated deliverables remain ignored/local.

## Scope and choices

Reuse native Korean Views and the existing stateless FastAPI/OpenAI structured
parser. Show the original request beside the extracted mandatory/optional errands,
durations and deadlines. Missing home deadlines must be explicitly selected;
unanswered clarification questions block planning. Users answer questions and
explicitly re-submit the original request plus bounded answer context. Preserve
previous errand IDs and mandatory flags across clarification; reject lost errands.
Edits, stale responses, restrictions and navigation approval remain fail closed.

Prefer deterministic Korean plan explanations grounded in the solver's actual
times/status/skipped items over another LLM call: this avoids invented hours,
causal explanations or feasibility. Display road, wait, dwell, home deadline and
verification limits. No chain-of-thought is requested or displayed.

Provide an independently selected, prominently labelled example interpretation
and clarification flow using the existing synthetic route data. This demonstrates
the interaction without claiming a live model result. Existing voice example and
full three-stop/home flow remain usable. No silent provider fallback, TTS, new
infrastructure, location integration or external deployment.

## Verification

Observe regression failures before implementation. Cover clarification context,
errand preservation, unresolved questions/missing deadline, original-text edits,
cancel/stale answers, fixture isolation and explanations for feasible/uncertain/
infeasible results. Run complete Python and both Android variant unit/build/lint
checks; inspect native day/night and actual Drive restriction. Capture selected
actual screenshots for the PR and run the complete simulated journey. Live
STT/LLM/provider checks remain separately reported when credentials are absent.
