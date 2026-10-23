# Actual Pleos voice input captures

Owner-authorized PR evidence from the signed demo on the official Pleos Connect
2.1.0 x86_64 API 34 emulator, captured on 2026-10-24. These are unedited full-device
screenshots; the vehicle sidebar and system controls are part of the real runtime.
Native vehicle state is Park / 0 for voice and example screens. The separate lock
capture uses actual Virtual Console Drive / 30 km/h and was restored to Park / 0.

| File | What it demonstrates |
| --- | --- |
| `input.png` | Native voice entry beside the existing editable text flow |
| `sdk-listening.png` | Actual Pleos SDK readiness; not successful final transcription |
| `example-review.png` | Explicitly labelled prepared text, awaiting confirmation |
| `night.png` | System night mode, same native transcript dialog |
| `route.png` | Explicitly requested fixture plan; simulated navigation only |
| `driving-lock.png` | Actual vehicle restriction dismisses the speech dialog |

Example text, locations, opening windows and road durations are demonstration
inputs. The example screenshots do not show real STT or LLM interpretation. The
capture check confirms no Speech Service microphone activity in example-only
screens. A real SDK session showed initialization and listening; successful final
Korean recognition remains unverified after a synthetic-audio injection caused an
emulator termination. Raw screenshots, audio, APKs and logs stay local. See
`../../progress.md` and `../../../apps/automotive/README.md` for verification and
reproduction commands. The older local video predates this voice UI.
