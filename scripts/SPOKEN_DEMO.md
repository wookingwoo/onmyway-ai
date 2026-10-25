# Local spoken-plan film production

Current film: ignored `artifacts/evidence/spoken-plan/OnMyWay_AI_spoken_demo.mp4`.
164.355542 seconds,1920x1080, constant30fps, H.264/AAC stereo. Actual Pleos
ON_DEVICE output replaces the previous film's editorial plan explanations.
Prepared user dialogue uses **offline** Windows SAPI/Heami. No online speech
service or application key is used by this production path.

Use the isolated official OnMyWay_Pleos AVD, signed current demo APK, key-free
API on8002 and official console forwarded at8088. Enable the emulator's
authenticated local gRPC output endpoint on loopback when launching that owned
AVD (`-grpc 8556 -grpc-use-token`). Do not weaken token authentication or expose
the endpoint externally. `--emulator-sdk` points to the local official emulator
controller protobuf bindings and compatible grpc/protobuf Python libraries.
The ephemeral local emulator token stays in process memory and is never printed.

```powershell
./scripts/narrate-spoken-demo.ps1
python scripts/record-spoken-demo.py --adb <adb.exe> --port 8002 --emulator-sdk <controller-bindings-directory>
python -m unittest discover -s scripts/tests -p test_native_demo_audio.py
python -m unittest discover -s scripts/tests -p test_spoken_demo.py
python scripts/build-spoken-demo.py
```

Python needs Pillow; FFmpeg/ffprobe and Windows Malgun Gothic must be present.
The recorder verifies all provider flags are false, refuses a live-journey reset,
independently selects samples, owns every screenrecord PID, and restores Park/0.
Audio is only that owned emulator's speaker stream, never the host microphone or
host loopback. Source packet timestamps preserve silence and1x speed. Video and
native PCM receive identical edit windows. Offline user clips are hash/text-bound
to their manifest and remapped by the same cut clock. All source plans are read
in full; cuts remove setup and operation waits. The renderer verifies source
hashes, bounded cuts, constant frame rate, stream dimensions and a full decode.

Captions are authored scene summaries plus proportionally timed prepared-user
sentences, explicitly not provider word boundaries or actual ASR alignment.
The persistent header distinguishes actual native output, prepared sample-user
audio, sample/simulated data and1x abbreviated operations. No app UI is replaced
with a fabricated demo layout. Full left Pleos and bottom controls remain visible.

Local capture manifest, PCM, raw videos, provenance, frames, lifecycle results,
verification JSON and player stay ignored. Selected actual screenshots are
versioned separately. Check every representative frame and uninterrupted browser
playback after rendering; those human/tool review states start pending.
Real ASR/LLM/maps/native route acceptance/arrival and physical driving remain
unverified. Do not publish the film or grant repository access without the owner.
