# Pleos navigation screen handoff

The app owns errand order, mandatory dwell/finish constraints and home planning.
Pleos Connect Navigation owns the actual road map and guidance. This change uses
the installed NaviHelper3.0.5 `requestNavigationScreen(Foreground)` API and the
`pleos.car.permission.NAVI_CUSTOM_MAP` permission. It adds no embedded map,
provider SDK, client credential or manufactured road line.

## Normal flow

1. Review/hear the computed current plan; explicitly approve the next stop/home.
2. Send only that destination through the existing typed route API. A request
   return is not acceptance: the owned current connection must receive both
   matching route-start and routing-complete events within its bounded wait.
3. Persist progress, stop app speech/input, and request the native screen once.
   The automatic gate also checks current revision/destination, resumed Activity,
   native Park and a non-synthetic source. Duplicate/stale callbacks cannot reopen it.
4. If the screen does not open, use **차량 내비 보기** while parked. Synchronous
   screen failure leaves accepted guidance intact. Dispatch is described as a
   screen request, never as proof that a map became visible.
5. Ordinary pause/stop preserves the SDK connection and arrival callbacks; it
   cancels pending automatic foreground and voice continuation. An app return does
   not automatically open another screen. Drive/unknown transitions invalidate
   pending foreground even if Park returns before acceptance.
6. Arrive, explicitly confirm **완료했어요**, then review/approve the next plan.
   Home remains a separately approved final destination.

Synthetic/simulated acceptance never calls the real screen API. The same service
layout shows a disabled map action and an explicit sample explanation. No sample
coordinate is forwarded to real vehicle navigation.

## Interrupted guidance

Before handing off, persist a synchronous unresolved-guidance hint. On recreation
or process restart, restore that uncertainty hint rather than approval or native
route acceptance. The recovery screen preserves the saved schedule/completed ids,
blocks new destination dispatch, and permits an explicit **차량 내비 보기** action.
If native service is unavailable it remains disabled with an explicit reconnect.

After the user independently ends the old native guidance, **기존 안내를 종료했어요 ·
다시 계획** asks for confirmation, clears the hint and recalculates. This does not
cancel an unowned native route or grant departure authority. Fresh-plan review
and approval are still required. This conservative recovery does not claim
automatic arrival reconciliation after process death.

## Actual validation and limits

The framework-only Android instrumentation smoke explicitly connects to the actual
native SDK and requests a screen without a destination. The official AVD brought
`ai.umos.maps.android.navigation.app/.MainActivity` to foreground and granted the
three requested navigation permissions. Its installed Navigation version is
3.3.2-rc5-hotfix.3. **The captured map canvas is blank**, so tile rendering,
actual route acceptance and driving/arrival are not established by this result.
Sanitized diagnostics observed three native tile HTTP401 responses from
`a.tile.static.maps-stage.umos.ai`; no URLs, tokens or raw native logs were saved.
Do not present the screenshot as a successful road route.

The device smoke has explicit seed-recovery/restore-sample modes, restricted to
an independently selected sample. They create a recovery hint for UI verification,
not a native accepted route; they are excluded from release APKs. Main's actual
automatic acceptance→map integration remains guarded by unit tests until live
providers/native route acceptance can be exercised. The independent sample flow
checks screen blocking, background return, Drive restriction and separate completion.

OpenAI/Kakao credentials remain deferred in the ignored server environment.
The native screen connection does not use them; live plan extraction/discovery
still does. No key was inspected/provisioned. Native routing may select roads and
ETAs different from the planner's road snapshots; live ETA reconciliation and
physical-vehicle usability still require validation. No deadline guarantee is made.

## Reproduction

Use the owned official Pleos AVD in Park/0; do not run concurrent input drivers.
Build `testDemoDebugUnitTest testLiveDebugUnitTest assembleDemoDebug
assembleLiveDebug assembleDemoDebugAndroidTest assembleDemoRelease
assembleLiveRelease` from `apps/automotive`. The documented standalone
`safety-tests test` source list now includes all current pure policy classes.

Install matching-signed debug app/test APKs and explicitly run:

```text
adb -s emulator-5556 shell am instrument -w ai.onmyway.car.demo.test/ai.onmyway.car.NativeNavigationScreenSmoke
```

Inspect actual foreground/screenshot separately from the dispatch-only result.
To verify the recovery UI only, use `-e mode seed-recovery`, launch MainActivity,
then restore the sample with `-e mode restore-sample`. The seed refuses a live
journey and retains a backup until restored. No test mode is in a release app.
All APK/raw/XML/runtime reports stay local. Selected screenshots include their
provenance under `docs/screenshots/navigation-handoff/`.

Official references: [NaviHelper](https://document.pleos.ai/api-reference/connect-sdk-pleos/NaviHelper/intro),
[permissions](https://document.pleos.ai/api-reference/connect-sdk-pleos/NaviHelper/declaring-permissions),
[destinations](https://document.pleos.ai/api-reference/connect-sdk-pleos/NaviHelper/destinations).
