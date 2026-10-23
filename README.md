# OnMyWay AI

**Your errands, one smart drive.** A native Pleos Connect app that plans errands around opening windows, service time and an arrival deadline.

Signed demo/live release APKs build and run on Pleos Connect 2.1.0. The fixture flow and actual driving restriction gate are verified. Live AI/place/road calls and actual navigation acceptance/arrival remain unverified. See [readiness](docs/readiness.md) and [delivery evidence](docs/progress.md) before presenting any claim.

## Repository

| Path | Purpose |
| --- | --- |
| `apps/automotive` | Native Kotlin Android app, explicit demo/live variants, Pleos navigation |
| `services/api` | FastAPI, structured intent extraction, place/routing adapters, deterministic planner |
| `contracts` | OpenAPI and example payloads shared with the app |
| `docs/submission` | Korean application draft, pitch, recording script and checklist |
| `artifacts/submission` | Local submission documents |
| `scripts` | Reproducible local setup and verification |

## Start the API

Python 3.11 or later is required. From the repository root on Windows:

```powershell
.\scripts\setup-api.ps1
Copy-Item .env.example .env  # only the first time; preserve existing settings
.\scripts\start-api.ps1
```

Add credentials later to the ignored root `.env` file and restart the API. Keep `OPENAI_API_KEY`, `OPENAI_MODEL` and provider keys on the server. Never place them in the Android project or send them in chat. Empty credentials cause an explicit configuration error on the relevant live endpoint; the demo endpoint remains independently available.

API documentation: `http://127.0.0.1:8000/docs`. The default server binds only to localhost. A standard Android emulator reaches the host through `10.0.2.2`; alternatively use `adb reverse tcp:8000 tcp:8000` and set the app server to `http://127.0.0.1:8000`. Configure an authenticated HTTPS server before any public use.

```powershell
.\.venv\Scripts\python.exe -m pytest services/api/tests
```

## Android

Use JDK 17, Android SDK platform 35, build tools 35.0.0 and Gradle 8.9 (wrapper included). SDK/tool licenses require the developer's own acceptance. Pleos testing requires the official API 34 Pleos Connect 2.1.0 emulator and cold boot.

```powershell
cd apps/automotive
.\gradlew.bat testDemoDebugUnitTest assembleDemoDebug assembleLiveDebug
```

The `demo` application is visibly labelled and permits explicit simulation. The `live` application requires real vehicle restrictions and SDK readiness. A fixture plan is never a live-data guarantee. Signed release builds are in `apk/OnMyWay_AI_demo_release.apk` and `apk/OnMyWay_AI_live_release.apk`.

To reproduce local signed builds after SDK setup:

```powershell
.\scripts\build-android.ps1 -Release -GenerateSigningKey
```

The first invocation generates a local development signing identity in ignored `.tools/signing`; subsequent builds reuse it. Keep that folder private and backed up for compatible app updates. The script does not publish the app.

On Windows hosts where Java NIO fails with `Unable to establish loopback connection`, the local JDK 17 workaround verified for Gradle setup is:

```powershell
$env:JAVA_TOOL_OPTIONS='-Djava.nio.channels.spi.SelectorProvider=sun.nio.ch.WindowsSelectorProvider'
# Use an existing writable, short Windows temporary directory for TEMP and TMP.
```

## Product boundaries

- AI extracts editable requirements; deterministic code validates route arithmetic.
- Travel, opening wait and errand duration all count toward deadlines.
- Required errands remain required unless the user explicitly changes them.
- Missing business/service evidence is shown as needing verification.
- Navigation requires approval for the current plan and next destination.
- Arrival does not mark an errand complete. The user confirms completion before replanning.
- Editing is disabled when driving restrictions are active or unavailable in the live app.

## Submission

Use the official application form and keep identity details in its operational section only. Keep personal names, company, contact information and secrets out of project materials and shared assets. Final legal signatures, access grants, public publishing and contest email submission remain user actions. Repository publication is not automatic.

Actual recording: `artifacts/evidence/OnMyWay_AI_Pleos_fixture_demo.mp4`. Written materials: `artifacts/submission/OnMyWay_AI_application_WORKING_DRAFT.docx` and `artifacts/pitch/output/OnMyWay-AI-pitch-ko.pptx`. The final package remains a working draft until live integrations and the human submission steps are complete.

Primary references: [contest](https://appdevchallenge.auto-hmg.io/), [submission guide](https://appdevchallenge.auto-hmg.io/guide/submission), [Pleos SDK](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/NaviHelper/intro), [OpenAI structured outputs](https://developers.openai.com/api/docs/guides/structured-outputs?api-mode=responses).
