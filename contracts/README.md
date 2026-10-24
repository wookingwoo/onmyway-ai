# API contract 0.1

`openapi.json` is generated from the FastAPI/Pydantic models. Regenerate with
`python services/api/export_contracts.py` after installing the API package.

- `demo-plan-request.json`: complete explicit synthetic fixture, also `GET /v1/demo`.
- `demo-plan-response.json`: deterministic result of sending the fixture to `POST /v1/plan`.
- `parse-request.json`: natural-language request plus timezone-aware reference time.
- `parse-clarification-request.json`: original request/reference plus bounded
  question/answer context, previous errands and home deadline; illustrative input,
  not a recorded model call. Added fields are optional for existing clients.
- `parse-response-example.json`: illustrative editable extraction, **not a recorded live LLM response**.
- `discover-request.json`: edited requirements and explicit coordinates for live Kakao discovery.

Times use ISO 8601 with offsets. Travel, dwell, wait and extension use integer
seconds. Coordinates are `latitude`/`longitude`; provider adapters convert to
Kakao's longitude-first order. Matrix edges are directed.

`GET /v1/demo` -> `POST /v1/plan` works with no keys. The live sequence is
`POST /v1/parse` -> user reviews/edits -> `POST /v1/discover` -> user verifies
hours/service -> `POST /v1/plan`. The app starts Pleos navigation only after
explicit approval. Replan after explicit completion using the remaining errands,
the current location and time, and refreshed road evidence. Empty errands allow
the final direct home leg; retaining a superset road matrix is allowed.

`status` is `feasible`, `needs_verification`, or `infeasible`. Inspect `is_fixture`,
`warnings`, `reasons`, and `skipped_errands` before presenting route confidence.
`latest_finish` is an optional per-errand completion deadline; `deadline` is the
home-arrival deadline. Nullable `relaxation` proposes the exact home deadline
extension for a verified mandatory route when possible. It never authorizes a
change. The demo's fixed 2026-10-23 timestamps make it repeatable, not current.

Provider errors have `detail: {code, message}` and no synthetic fallback. A 503
`provider_not_configured` means configure server environment variables; a 422
means review invalid input/refusal; 502/504 indicates provider output/network
failure. Keys never belong in the Android app.
