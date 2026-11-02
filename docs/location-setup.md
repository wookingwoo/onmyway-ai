# Native departure and home setup

While native-Parked, open **설정 → 출발 · 귀가 설정**. Use **현재 위치로 출발지
설정**, or explicitly confirm **현재 위치를 집으로 저장** when at home.
**집 근처 장소 검색** reuses `/v1/places` with exact selection. This is keyword
POI search, not arbitrary residential-address geocoding. **위치 직접 지정** is an
explicit fallback. Search still needs deferred server-side Kakao configuration.

`ai.pleos.playground:FusedLocation:1.1.3` and dangerous permission
`pleos.car.permission.FUSED_LOCATION` are used. Published artifact signatures were
inspected: `LocationData.timestampMillis` supplies source time. No Google location
dependency, client key, new backend endpoint or background tracking was added.
Granting permission never automatically acquires/saves position; press the
location action again. Source state is visible in setup.

Only finite legal coordinates with positive source time within 60 seconds
(5 seconds future tolerance) qualify as a device origin. Cached stale/null
results may wait for a fresh update until the 10-second bound. Save and planning
recheck freshness. Unknown/legacy coordinates without metadata need explicit setup.
Each attempt owns its listener, generation and timeout. Leaving setup, background,
Drive/unknown, reset and rendering invalidate it before unregister/release.
Every save requires foreground, native Park, current epoch/revision, no synthetic
source and no active/unresolved guidance. Location changes invalidate plan approval.

`real-home` persists separately from fixture/journey drafts. A reset restores that
confirmed home and clears departure; fixtures cannot replace it. Completed-stop
origin survives continuation but is removed on a genuinely new request. Manual
origin has explicit provenance and does not claim current-position freshness.
Raw coordinates are not logged or exposed in the main screen/evidence.

## Actual verification on 2026-11-03

Official Pleos Connect 2.1.0/API 34 ran as an isolated read-only AVD. Installed
FusedLocation service version is 1.0.0. Actual fresh SDK position passed the source
timestamp check. Origin/home saves required confirmation; home survived restart
and later permission denial. Stale confirmation was rejected. Actual denial/allow
and recovery text were exercised. Drive/30 dismissed an open SDK-position
confirmation; Park/0 did not reopen it. Park/0 restored in cleanup.

[Actual full native frames](screenshots/location-setup/README.md) show saved home,
permission denial and Drive. This is the emulator's location service, not physical
GPS or a live errand. Pure tests cover invalid/stale data, ownership/duplicates,
revision/gate changes, missing source, fixture isolation, home persistence and
new-journey completed-stop invalidation.

Native SDK screen dispatch/foreground was rechecked but its map stayed blank.
The current sanitized process-log scan found no tile HTTP401 lines; the previous
401 remains historical. No supported authentication fix was established. Real
planned-route acceptance/arrival, final Korean STT and OpenAI/Kakao calls remain
open. Home place-search is compiled, not newly live-verified. Release outputs are
unsigned/local; historical signed APKs, films and office forms were not regenerated.

## Live acceptance after user configures keys

1. User configures only ignored server environment and restarts the local API.
   Keep credentials outside Android/captures/reports; inspect only health flags.
2. In live/native Park, confirm home and fresh departure. Speak an unprepared
   request with two mandatory pickups, an optional purchase and home deadline.
   Check actual final STT and interpretation edits.
3. Confirm exact venues and independent availability evidence/basis/time. Unknown
   hours/service/roads remain uncertain. Include road, wait, dwell, per-errand
   completion and home travel; refresh expired departure.
4. Review and explicitly approve the current next destination. Verify owned SDK
   acceptance separately from map rendering. Exercise cancellation, denial,
   timeout, unrelated callbacks, background and interrupted-guidance recovery.
5. Arrival must leave the errand incomplete. Confirm completion, review/approve
   the remaining plan, then approve home separately.
6. Record actual results/failures, source/APK hash and device versions. Never
   replace failed live steps with examples. If native map remains unavailable,
   retain the blocker and the [unsent technical question](submission/briefing-review-ko.md).
