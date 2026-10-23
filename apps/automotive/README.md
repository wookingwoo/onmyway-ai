# OnMyWay native vehicle app

Kotlin Android Views, API 28–35, JDK 17, Gradle 8.9, AGP 8.7.3, Kotlin 2.0.21. Native UI and typed Pleos NaviHelper 3.0.5 integration; no web wrapper. SDK data dependency is navi-data 3.0.4 as published by Pleos.

From this directory, after installing the licensed Android SDK 35 and Build Tools 35.0.0:

```powershell
$env:JAVA_HOME = 'C:\path\to\jdk17'
$env:ANDROID_HOME = 'C:\path\to\android-sdk'
.\gradlew.bat testDemoDebugUnitTest testLiveDebugUnitTest assembleDemoDebug assembleLiveDebug
```

The repository bootstrap/build scripts maintained at the repository root provide local tool paths. On Windows systems with a JDK loopback-selector failure, set `JAVA_TOOL_OPTIONS=-Djava.nio.channels.spi.SelectorProvider=sun.nio.ch.WindowsSelectorProvider` and use a short writable TEMP/TMP directory.

Run backend separately. Default emulator server is `http://10.0.2.2:8000`; use Settings → connection/location settings to change it. No API key belongs in this app. Live parsing/discovery requires server-side provider configuration and reports provider errors without falling back to fixtures.

The demo package is `ai.onmyway.car.demo`, live package `ai.onmyway.car`. Only demo exposes the explicit “예시로 체험 · 모의 안내” action. It requests a fixture and enables simulated guidance together, with provenance visible throughout. Simulation does not represent a parked car and never requests actual navigation. Actual vehicle restrictions override simulation; unknown state locks live controls. Actual UX restrictions are read from the optional `android.car` shared library and fail closed on service/permission errors.

The single-stage journey is INPUT → REVIEW → ROUTE; Settings returns to the
previous stage. An edit invalidates the plan and returns to review. One fixed
primary action, 80dp action buttons, 24sp body text and adaptive neutral day/night
surfaces replace the dense dashboard. Dwell editors use five-minute steppers;
deadline editors use large hour/minute wheels and an explicit date/calendar
button. Driving restrictions dismiss open editors and hide the form. Technical
settings and evidence remain secondary. See the actual captures in
`docs/screenshots/automotive-ux/`.

Route approval is specific to one destination and the current input revision. Connection is separate from request transmission and route-search completion. Route-start and arrival names must match the requested destination; SDK event variants that omit or change these names remain unconfirmed. Arrival never completes an errand. The user explicitly confirms completion and recalculates remaining errands; final home navigation requires a separate approval. Only completed IDs and editable drafts are persisted; navigation approval is never restored.

Pure safety tests can run without an Android SDK:

```powershell
.\gradlew.bat -p safety-tests test
```

The 21 tests cover stage transitions, fixture draft isolation, date/dwell edits, vehicle lock and simulation boundaries, revision and destination approval, stale responses, retained navigation errors, arrival versus completion, completed-location matrix trimming and the final home leg. Successful compile is not proof of SDK service connection, request acceptance, arrival or vehicle-state behavior on a Pleos device; capture those separately on the actual emulator/device.

With the localhost API running and an explicitly selected parked Pleos emulator:

```powershell
# From the repository root; adb reverse tcp:8000 tcp:8000 must be configured.
.\.venv\Scripts\python.exe scripts/smoke-android.py --adb .tools/android-sdk/platform-tools/adb.exe --serial emulator-5556 --full-journey --output artifacts/evidence/automotive-ux
```

`--boundaries-only` checks fixture reset and the native date editor, then leaves
an editor open for `scripts/smoke-pleos-lock.py`. The latter changes the isolated
AVD to Drive / 30 km/h through the official Virtual Console and restores Park / 0.
Do not run concurrent input drivers against that emulator.
