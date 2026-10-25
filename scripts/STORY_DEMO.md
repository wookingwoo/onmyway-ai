# Local story-film production

The Korean scenario is `docs/submission/story-demo-ko.md`; exact editorial copy
and narration are in `scripts/story_demo.json`. This is a media-production path,
editorial evidence of actual native automation with prepared inputs, not proof of live STT/LLM/provider/navigation success.

## Local prerequisites

Use the isolated official `OnMyWay_Pleos` AVD, the current locally signed demo
APK and the key-free API on localhost:8002. The recorder checks all provider
configuration flags are false, independently selects the sample source in settings, and never clears app data or resets a live journey.
Keep the official Virtual Console forwarded at localhost:8088. Recordings own
their screenrecord PID and restore Park/0 in a finally block.

Install optional production dependencies in a separate ignored environment:

```powershell
python -m venv .tools/media-venv
.tools/media-venv/Scripts/python.exe -m pip install -r scripts/requirements-media.txt
```

FFmpeg/ffprobe and Windows Malgun Gothic fonts must be available. The renderer
accepts `--font-dir`. No application dependency or environment file is changed.

## Produce

```powershell
python scripts/record-story-demo.py --adb <local-adb.exe> --serial emulator-5556 --port 8002
.tools/media-venv/Scripts/python.exe scripts/narrate-story-demo.py
.tools/media-venv/Scripts/python.exe -m unittest discover -s scripts/tests -p test_story_demo.py
.tools/media-venv/Scripts/python.exe scripts/build-story-demo.py
```

The first script should use the Python runtime that runs the local API. A
carefully bounded `--resume-decision` / `--resume-driving` option retains only the first four / five hash-verified recorded chapters and requires the same pending, labelled synthetic laundry journey with
18:53/19:00 visible. It never substitutes another scenario after a failed check.

Narration is explicitly selected editorial audio using the [edge-tts client](https://github.com/rany2/edge-tts)
and an available Korean voice, currently `ko-KR-SunHiNeural`. It sends only the
anonymous Korean script to Microsoft's online speech service and reads no app
key or env file. It fails on missing audio or word boundaries. Actual provider
boundaries drive subtitles; they are not a claim of speech-recognition testing.
The original quiet music bed is synthesized locally. It uses no stock track.

Outputs stay under ignored `artifacts/evidence/pleos-service-demo/`: raw recordings,
unedited PNG/XML, capture manifest, narration/boundaries, edit intermediates,
final MP4/SRT, chapter frames, local player and verification JSON. Narration or
copy changes require regeneration before assembly. Never force-add outputs.

## Review

The renderer requires successful capture provenance, matching raw/audio/boundary
hashes, narration matching the current script, nonempty valid spoken timing and
capture beats inside actual ffprobe duration. It rejects mixed frame/timebases
before concatenation. A build invalidates stale reports and only atomically
replaces the final file after decode/timeline checks. It keeps actual source windows at 1x, removes
waiting and holds final states; the edit label is visible. It does not fabricate
UI or suggest measured response latency. Every native chapter preserves the full 16:9 Pleos surface, including the left vehicle panel and bottom controls. The source is fit into a 1568×882 box without cropping; chapter text and captions stay outside this viewport. The raw source remains available to audit every cut. Keep
the local player closed while rebuilding on Windows; an open video stream can
lock the final MP4. A failed replacement leaves the previous film intact and the
new report marked failed.

Full decode and audio presence are necessary but insufficient. Inspect every
chapter frame and actual playback, including captions, transitions, narration,
the 10-to-30-minute correction, mandatory order, omitted milk, home approval and
actual driving/Park restore. Mark only checks actually performed in the local
verification JSON and `docs/progress.md`. Select small actual PNGs for PR evidence.

Large media is local. Uploading/publishing the film, credentials, contest email
submission, identity and external access remain separate user actions.

The current cut holds the actual saved-request frame while its editorial quote is
spoken, then shows the automatic summary; the same applies to the refinement.
All held states are labelled. The normal service screen contains shared actions;
simulated arrival is explicitly sent through the scoped settings utility. The
final encode normalizes concat timestamp gaps to CFR30 from timestamp zero and
rejects inconsistent final frame counts. The older 173.97s `voice-first-demo/`
film and previous `story-demo/` are historical local revisions.
