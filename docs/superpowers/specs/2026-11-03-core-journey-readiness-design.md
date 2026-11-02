# Core journey readiness design

Status: user approved implementation on 2026-11-03 after selecting the existing
errand/home concept. Builder check resolved search scope and all-path guidance
mutation gates before approval. Branch:
`feature/briefing-readiness`. Winning is an objective, not a promised result.

## Intent and chosen approach

Make the existing promise credible: finish mandatory errands within a home
deadline, using the vehicle's own input, output, position and navigation.
The user chose completing this core journey over comparing new customer/concept
directions. Expand verification depth before adding unrelated features.

Approaches considered:

1. Reconcile presentation only: fixes factual drift but leaves manual coordinate
   setup and live journey gaps. Necessary housekeeping, insufficient by itself.
2. Complete the existing journey: remove coordinate setup from the normal path,
   validate native boundaries and fix failures with evidence. Recommended by the
   agent; the user selected this direction.
3. Compare new concepts/customer problems: potentially different differentiation
   but discards the current implementation/evidence. Offered and not selected.

## Scope and sequence

### A. Submission and technical story

Reconcile application, pitch, strategy and checklist with the actual spoken-plan
film and subsequent navigation handoff evidence. Add timestamped briefing findings
and participant-owned technical explanations. Keep evidence dates, synthetic/live
boundaries and missing generated assets explicit. This is documentation upkeep
already requested, not evidence of a new build or live test.

### B. Native departure and home setup

Replace raw-coordinate setup in the normal live path with a Korean **출발 · 귀가
설정** screen. Offer **현재 위치로 출발지 설정**, **현재 위치를 집으로 저장**, and
home name/address search with explicit selection using existing `/v1/places`.
This is keyword place search, not residential-address geocoding. Label it **집 근처
장소 검색** and explain that unsupported addresses need a selected nearby place,
explicit current-position home save while at home, or advanced manual entry.
Do not add a new geocoding endpoint in this phase or imply every home address is
searchable. Confirm the exact selected destination before saving.
Keep coordinate entry as an explicit advanced fallback; do not silently use it
after SDK failure. Show source and configured state without exposing coordinates
in the main product view. A home location is only saved on explicit confirmation;
the app never guesses that current position is home.

Use official Pleos FusedLocation rather than Google Play Services. Verify the
published artifact/signatures and installed emulator compatibility first. The
current Korean setup documentation names `FusedLocation:1.1.3`; English material
still references 1.1.2. Match tested artifact signatures, not inferred APIs.

Acquire only while foreground, native-Parked and not in a fixture/simulated flow,
active guidance or unresolved-guidance recovery. Request dangerous
`pleos.car.permission.FUSED_LOCATION` through Android's runtime flow. Denial,
unsupported SDK, blocked privacy, null, timeout, invalid coordinates or a stale
sample must show a recoverable failure and preserve the existing confirmed home.
Never guess timestamp units or confidence semantics; inspect official API/artifact
and reject data whose validity cannot be established. Policy choice: accept only
finite legal coordinates and a documented source timestamp no older than 60
seconds, allowing at most 5 seconds future clock tolerance. A zero/unknown source
timestamp is invalid. The request expires after 10 seconds.

One attempt owns its listener, generation, watchdog and SDK resource. Cancel,
leaving setup, pause/background, vehicle restriction/unknown transition and reset
invalidate the attempt before cleanup. Late results/permission grants may not save
coordinates, invalidate a newer plan or reopen setup. Cleanup unregisters the
same listener and releases the instance. No background tracking.

The same restrictions apply to every origin/home mutation: current position,
place search/selection and advanced manual editors. Setup must be blocked while
any owned request/accepted guidance or unresolved-guidance hint exists. A result
must recheck these conditions at save time, not just when the dialog opens. The
user must resolve/end old guidance through the existing guarded recovery or
cancellation path before changing locations; editing cannot implicitly cancel it.

Show the selected origin/home and require an explicit save. Applying either
invalidates the prior plan and approval through the existing edit path. Fixture
locations remain wholly independent. Sample mode never requests device position
or sends sample coordinates to live place search/native navigation. New-journey
reset preserves an independently confirmed real home only, never a fixture home.
An active journey's post-completion origin remains its completed stop; a location
setup callback cannot overwrite it. The builder may choose modest Kotlin classes
and reuse the existing native visual components, avoiding a new framework.

For device-derived departure, freshness must be checked again before planning.
If older than 60 seconds, require an explicit refresh; never relabel an old stored
position as current. Explicit manual setup has its own source label and does not
claim automatic current-position freshness. Persist source metadata alongside
coordinates and do not log raw position. Search requires a valid selected origin;
provider failure is explicit and never selects synthetic results.

### C. Native and provider verification

Run Python tests and both Android unit/build variants after product changes;
exercise meaningful location freshness, ownership, lifecycle, fixture isolation,
approval invalidation and home-persistence boundaries. Use official AVD checks
for SDK support, permission denial, response/cancellation and actual restriction
transitions. Record actual outcomes; an unavailable emulator/service is not a pass.

Investigate the known native tile HTTP401 using supported image/version/network
diagnostics without printing tokens or raw URLs. Do not manufacture roads or
weaken authentication. If supported remediation cannot resolve it, leave an
explicit platform dependency and a sanitized, unsent technical question. Do not
mark actual planned-route acceptance/arrival as verified by screen foreground.

Real STT/provider/native-route end-to-end checks remain required. The user will
supply API keys later; do not provision or inspect credentials to finish this
phase. Document a concrete live acceptance procedure for that point. Demo success
must never stand in for it. Rebuild final APK/film/form alignment only from actual
available sources; keep generated deliverables local.

## Non-goals and acceptance limits

No new errands, vehicle controls, embedded maps, cloud speech, paid services,
hosting, user recruitment, public posting, sharing grants, identity/signature or
final submission. Native state fails closed; mandatory errands, travel/wait/dwell,
arrival versus completion and separately approved home remain invariant.
No claimed first place, measured benefit, research, production readiness or
live result without evidence. Physical-vehicle usability is a launch task; the
briefing says contest validation uses the official emulator.

## Sources

- [Briefing findings and timestamps](../../submission/briefing-review-ko.md)
- [Current native handoff and observed map failure](../../navigation-handoff.md)
- [FusedLocation setup](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/FusedLocation/project-setup)
- [Location access and null callbacks](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/FusedLocation/location-data-access)
- [Permission and privacy blocking](https://document.pleos.ai/en/docs/pleos-only/connect-sdk-pleos/FusedLocation/declaring-permissions)
