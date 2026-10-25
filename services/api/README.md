# OnMyWay API

Python 3.11+ (developed with 3.13), FastAPI, Pydantic 2, HTTPX. No database.

From the repository root in PowerShell:

```powershell
python -m venv .venv
.venv/Scripts/python -m pip install -e './services/api[test]'
Copy-Item .env.example .env
# Edit only the ignored .env when credentials are ready.
.venv/Scripts/python -m uvicorn --app-dir services/api onmyway.main:app --env-file .env --host 127.0.0.1 --port 8000
```

For an Android emulator, call `http://10.0.2.2:8000`. The default development
service is intended for a trusted local environment; it has no user accounts.
Use the repository launcher for a physical device development setup.

```powershell
Push-Location services/api
../../.venv/Scripts/python -m pytest -q
../../.venv/Scripts/python export_contracts.py
../../.venv/Scripts/python smoke_http.py
Pop-Location
```

On this managed Windows host, the network sandbox blocks Python's internal
asyncio loopback socket pair. Provider tests therefore require a local test
process with loopback access, even though provider HTTP is fully mocked.

## End-to-end calls

`GET /health` returns provider configuration booleans, never keys. Configuration
does not prove authentication, quota or real-world availability.

`GET /v1/demo` returns a complete synthetic `PlanRequest`. Send that JSON
unchanged to `POST /v1/plan`; the result is explicitly `is_fixture: true` and
contains a fixture warning. The example remains fixed at 2026-10-23 for a
repeatable demonstration. It is not a current driving suggestion.

The separately selected `GET /v1/demo?scenario=deadline_conflict` example keeps
all synthetic place/road sources and both mandatory errands. Its home deadline
is 18:20 and laundry completion deadline is 18:30; the synthetic pharmacy's hours
and service are explicitly unknown. Planning returns an infeasible original
request plus a conditional 18:31 home-deadline proposal that still requires
verification. It never runs a provider or serves as a live failure fallback.
The default /v1/demo and `scenario=basic` retain the original basic example.

`POST /v1/parse` accepts natural language plus an offset-aware reference time.
It uses the OpenAI Responses API with strict JSON schema and returns editable
errands, optional `latest_finish` constraints, a nullable home deadline, questions
and warnings. The frontend must review and edit this draft before discovery.
`OPENAI_API_KEY` and `OPENAI_MODEL` are both required; no model is silently chosen.
Refusal, incomplete output, invalid output, HTTP failures and timeouts produce
explicit errors and never synthetic results.

`POST /v1/places` accepts `{query, origin}` (nonblank query up to 200 characters)
and returns `{places}` with at most three nearby Kakao candidates, including
road address or fallback parcel address. It requires only the server's
`KAKAO_REST_API_KEY`. Search never establishes hours or requested-service
availability; both remain `unknown`, with no availability evidence.

`POST /v1/discover` accepts the edited errands, explicit origin/home coordinates,
departure time and deadline. Set `KAKAO_REST_API_KEY` and
`KAKAO_MOBILITY_REST_API_KEY` on the server. Each optional `selected_place`
preserves the chosen identity, coordinates, source, address and availability
evidence, bypasses keyword search and becomes its sole candidate. Live discovery
rejects a selected fixture with 422 `fixture_place_not_allowed`, before reading
provider configuration. Pinned-only requests need no Local key. Legacy errands
without selection still search for one candidate. Discovery always obtains the
directed road matrix and returns a `PlanRequest` for review and `POST /v1/plan`.
Maximum five Local calls and 31 Mobility calls per discovery.
These calls can consume provider quota; no live call was made during development.
Discovery searches within 20 km of the origin. Kakao keyword results do not
establish opening hours or the requested service: these remain `unknown`.
Current-traffic durations are snapshots, not forecasts for all future legs.
Every current-traffic edge therefore carries a verification warning. Other
nonfixture road evidence more than 15 minutes from departure also requires
verification. Synthetic demonstration evidence is exempt from the freshness
check and always retains its fixture label.
An explicit no-route result leaves an edge missing; a provider outage returns
an error rather than a guessed duration. Duplicate physical candidates share
their Kakao place id. No straight-line fallback exists.

`POST /v1/plan` is pure and makes no network calls. Supply up to five errands,
up to three candidates per errand, all relevant directed road times, timezone
offsets, dwell seconds and explicit evidence. It searches all candidate choices,
orders and optional subsets. It prioritizes a route with verified evidence,
then the number of optional errands, earliest home arrival, total driving time
and stable id order. Thus optional tasks with unverified data can be skipped in
favor of a verified mandatory route. Every skipped optional task has a reason.
Every unscheduled mandatory task is explicitly named in failure reasons.

`feasible` means all mandatory errands fit the supplied opening intervals and
service evidence, full dwell fits before both closing and `latest_finish`, and
the final home leg meets the deadline. Waiting is included. It is conditional
on the supplied evidence; traffic and shop availability can change. Unknown
hours/service yield `needs_verification`. Live `verified` flags also require
`availability_evidence: {basis, confirmed_at}`: a nonblank basis of at most 500
characters and an aware confirmation time no later than departure and no more
than 24 hours before it. Missing, future or older evidence adds verification
warnings; it cannot establish feasibility. Stated opening intervals and dwell
are still enforced, including intervals supplied with unknown hours. Those intervals
retain their uncertainty warning and all opening waits. Evidence never upgrades
unknown hours or service status.
Explicit fixture places remain labelled and exempt from this live evidence rule.
A missing candidate or required road edge with no complete route also yields `needs_verification`, with no invented
timeline. Proven timing failure with complete evidence yields `infeasible`.
Mandatory tasks are never silently dropped. A selected place must be the exact
sole candidate, including coordinates, source and evidence; /v1/plan rejects a
mismatch with 422. Legacy requests without selection remain supported. An input
location id must always
refer to the same coordinates; duplicate errands and directed edges are rejected.
Matrix supersets are accepted so completed candidates can be removed while
retaining the old matrix; unreferenced pairs never create stops.
The planning horizon is capped at 24 hours. Arrival is never errand completion.

The Android app owns explicit approval, vehicle-state restrictions and Pleos
navigation. This API never starts navigation. Replanning is a new `PlanRequest`
with explicit remaining errands, current origin/departure and updated matrix;
completed errands are removed only by the user's completion action.
An empty errand list computes the remaining direct home leg. A timing failure
may include `relaxation` with a computed mandatory-only home deadline within 24 hours and the exact extension in seconds. When evidence is
uncertain, `requires_verification: true`, retained `warnings` and a conditional
description identify that uncertainty. The original plan status is preserved.
Missing required roads or no complete relaxed route means no suggestion.
Opening intervals and individual finish deadlines remain enforced; the
suggestion never changes the input or implies user approval.

## Sources and remaining verification

- [OpenAI Structured Outputs](https://developers.openai.com/api/docs/guides/structured-outputs?api-mode=responses)
- [Kakao keyword search](https://developers.kakao.com/docs/ko/local/dev-guide#search-by-keyword)
- [Kakao Mobility driving directions](https://developers.kakaomobility.com/en/guide/navi-api/directions)

HTTP mock tests cover these adapter contracts. Authentication, account permissions,
real place results and road results require user-provided credentials and a
separate live smoke test. No real provider integration is claimed here.

## Clarification context

`POST /v1/parse` also accepts optional `answers` (at most ten question/answer
pairs), `previous_errands` (at most five) and `previous_deadline`. The original
`text` and timezone-aware `reference_time` remain unchanged across clarification.
All context is untrusted data. Missing/replaced errands or downgraded mandatory
flags reject the provider result. Existing dwell and per-errand/home deadlines
are retained conservatively if a clarification weakens them, with an explicit
Korean warning to use the manual editor for relaxation. No provider failure falls
back to an example. Credentials remain server-only and are not required for the
independently requested fixture endpoints.
