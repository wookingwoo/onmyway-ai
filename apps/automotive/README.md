# OnMyWay native vehicle app

Kotlin Android Views, API 28–35, JDK 17, Gradle 8.9, AGP 8.7.3, Kotlin 2.0.21. Native UI and typed Pleos NaviHelper 3.0.5 integration; no web wrapper. SDK data dependency is navi-data 3.0.4 as published by Pleos.

From this directory, after installing the licensed Android SDK 35 and Build Tools 35.0.0:

```powershell
$env:JAVA_HOME = 'C:\path\to\jdk17'
$env:ANDROID_HOME = 'C:\path\to\android-sdk'
.\gradlew.bat testDemoDebugUnitTest testLiveDebugUnitTest assembleDemoDebug assembleLiveDebug
```

The repository bootstrap/build scripts maintained at the repository root provide local tool paths. On Windows systems with a JDK loopback-selector failure, set `JAVA_TOOL_OPTIONS=-Djava.nio.channels.spi.SelectorProvider=sun.nio.ch.WindowsSelectorProvider` and use a short writable TEMP/TMP directory.

Run backend separately. Default emulator server is `http://10.0.2.2:8000`; expand location/time/server settings to change it. No API key belongs in this app. Live parsing/discovery requires server-side provider configuration and reports provider errors without falling back to fixtures.

The demo package is `ai.onmyway.car.demo`, live package `ai.onmyway.car`. Only demo exposes the explicitly labelled interaction simulation and fixture load. Simulation does not represent a parked car and never requests actual navigation. Actual vehicle restrictions override simulation; unknown state locks live controls. Actual UX restrictions are read from the optional `android.car` shared library and fail closed on service/permission errors.

Route approval is specific to one destination and the current input revision. Connection is separate from request transmission and route-search completion. Route-start and arrival names must match the requested destination; SDK event variants that omit or change these names remain unconfirmed. Arrival never completes an errand. The user explicitly confirms completion and recalculates remaining errands; final home navigation requires a separate approval. Only completed IDs and editable drafts are persisted; navigation approval is never restored.

Pure safety tests can run without an Android SDK:

```powershell
.\gradlew.bat -p safety-tests test
```

Tests cover vehicle lock and simulation boundaries, revision and destination approval, stale responses, navigation failure, arrival versus completion, completed-location matrix trimming and the final home leg. Successful compile is not proof of SDK service connection, request acceptance, arrival or vehicle-state behavior on a Pleos device; capture those separately on the actual emulator/device.
