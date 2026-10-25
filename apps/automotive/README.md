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

Route approval is specific to one destination and the current input revision. Connection is separate from request transmission and route-search completion. Route-start and arrival names must match the requested destination; SDK event variants that omit or change these names remain unconfirmed. Arrival never completes an errand. The user explicitly confirms completion; the remaining preview refreshes automatically while parked, retaining selected venues. Final home navigation requires a separate approval. Only completed IDs and editable drafts are persisted; navigation approval is never restored.

Pure safety tests can run without an Android SDK:

```powershell
.\gradlew.bat -p safety-tests test
```

The 80 tests per variant cover clarification and deadline preservation, explanation
evidence, voice confirmation/cancellation, stale and duplicate
speech callbacks, example provenance, stage transitions, fixture draft isolation,
date/dwell edits, vehicle lock and simulation boundaries, revision and destination
approval, stale responses, arrival versus completion and the final home leg.
Exact venue selection, explicit availability basis/freshness, elapsed computation/review slack, owned-request timeout/cancellation and invalidated callback generations are also covered. Successful compile is not proof of SDK service connection, request acceptance,
arrival or vehicle-state behavior; capture those separately on the emulator/device.

## Voice input demo

For the current complete presenter flow, see `../../docs/demo-guide-ko.md`.
`scripts/start-demo.ps1` installs the existing local signed APK and checks/starts
the local API against an already running official AVD. The independently selected
**질문과 답변 체험 · 예시 해석** path shows prepared interpretation, a clarification,
explicit home-time selection, full simulated completion/home and deadline adjustment.
There is one primary next action. Unresolved questions or a missing home deadline
cannot trigger planning. Original text and answers are available on demand.

Clarification sends the original reference time, bounded answers and prior errands
to the existing live parser. Existing deadlines and dwell cannot be silently
weakened; users change these explicitly through the editor. Native home-time
selection and approved deadline extensions update the same review state.

The app uses `ai.pleos.playground:SpeechToText:3.3.1` in `ON_DEVICE` mode with
`pleos.car.permission.STT_SERVICE`. There are no Android provider credentials or
server registration calls. The actual vehicle must be PARKED to start the mic,
including in the demo variant. The user taps **음성으로 말하기**, waits for
**듣고 있어요**, speaks Korean, then explicitly confirms a final result with
**이 내용 사용하기**. The result remains editable in the original input field;
**할 일 확인하기** uses the existing server-side live parser and requires its
configured provider. Speech alone never sends a destination or completes a task.

The SDK has an 8-second connection deadline and a 30-second recognition deadline.
Cancel, restrictions and backgrounding discard the session. SDK `stop()` is
asynchronous: the adapter retains its binding until the ended callback or a
2-second cleanup grace before release, and serializes retries behind cleanup.

For a key-free presentation, explicitly tap **예시 음성 흐름 체험**, confirm the
prepared transcript, then select **예시 할 일 확인하기**. This requests the existing
labelled fixture plan with simulated navigation. The UI says this is example text,
not real speech recognition or LLM interpretation. Editing the sentence clears
the example marker; a failed live recognition never selects this mode.

Actual SDK initialization/listening and cancellation/no-audio failure are verified;
successful final Korean transcription is not. A synthetic microphone injection
attempt through emulator gRPC terminated the emulator before a final result. The
same AVD was restored without wiping app data. No cloud STT, live LLM parsing or
physical-vehicle speech accuracy is claimed. See `../../docs/progress.md`.

With the signed demo at the input stage on the parked isolated AVD:

```powershell
.\.venv\Scripts\python.exe scripts/smoke-voice.py --adb .tools/android-sdk/platform-tools/adb.exe --serial emulator-5556 --mode sdk
.\.venv\Scripts\python.exe scripts/smoke-voice.py --adb .tools/android-sdk/platform-tools/adb.exe --serial emulator-5556 --mode example
```

Run these commands from the repository root. `--reset-fixture` explicitly resets
an earlier labelled fixture journey; it refuses to reset a live journey. The SDK
smoke includes checking that the shared Speech Service no longer records audio
after cancel, background and timeout. Generated captures remain ignored under
`artifacts/evidence/voice-demo/`; selected owner-authorized screenshots are in
`docs/screenshots/voice-input/`.

With the localhost API running and an explicitly selected parked Pleos emulator:

```powershell
# From the repository root; adb reverse tcp:8000 tcp:8000 must be configured.
.\.venv\Scripts\python.exe scripts/smoke-android.py --adb .tools/android-sdk/platform-tools/adb.exe --serial emulator-5556 --full-journey --output artifacts/evidence/automotive-ux
```

`--boundaries-only` checks fixture reset and the native date editor, then leaves
an editor open for `scripts/smoke-pleos-lock.py`. The latter changes the isolated
AVD to Drive / 30 km/h through the official Virtual Console and restores Park / 0.
Do not run concurrent input drivers against that emulator.
