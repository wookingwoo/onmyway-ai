# Native Pleos recording correction

The owner rejected the illustrated-screen films: actual Pleos screen recording
is mandatory. Keep the approved audio, story and viral Short direction, but make
all product interaction footage by operating an installed native Android app on
the official OnMyWay_Pleos AVD. The vehicle panel and Pleos chrome must be visible.

Owner authorization explicitly includes presentation-only functionality and clean
product copy without a demo badge. A separate film flavor/package contains the
authored scenario, isolated from live/demo app behavior. This is a native filming
prototype, not live provider or production navigation evidence. The native vehicle
restriction service must still gate actions; unknown/restricted states fail closed.
Never bypass vehicle gates for the recording. Arrival/completion and current-plan
approval, mandatory errands and separate home approval remain explicit.

1. Add a film-only journey model with tests for current approval, arrival versus
   completion, mandatory preservation and vehicle gating. Implement a native
   FilmActivity with large, polished view-based panels and actual button actions.
   Launch it from a film flavor manifest; live/demo launcher is untouched.
2. Boot the official AVD without wiping it. Install only the isolated .film APK.
   Operate real UI controls through ADB, record screenrecord raw clips, save real
   screenshots/UI XML and hashes. Exercise actual Park/Drive restriction changes.
3. Build a native-footage film using source windows that correspond to narration.
   No illustrated cockpit pixels may replace product footage. Keep full Pleos
   framing in the main film; use a readable full-screen insert plus punch-ins for
   the vertical Short. Preserve the original comic inserts only between captures.
4. Decode both complete results, inspect actual final frames, check sound/caption
   timing and full playback; rerun Python and Android tests/builds. Update PR11
   and delivery notes, superseding the rejected illustrated-screen versions.

All recordings, APKs and audio remain under ignored artifacts/final-films/.

## Execution notes

- Native film policy tests observed RED, then119 tests/flavor GREEN for three
  flavors; all debug builds passed. API67, media40 passed.
- Captured13 native stages with real button taps. Actual OS Drive30 block and
  Park0 restoration captured via the AVD's bundled Console8088.
- Ruling: use actual Android screenrecord pixels for every product shot, with
  full Pleos framing in the main film. Original milk inserts are Short bumpers.
- Review fixed Short approval trim and driving-window provenance. Independent
  decoded start/mid/end checks confirm the reviewed locked interval. No important
  source finding remains; final edited-file validation is recorded separately.
