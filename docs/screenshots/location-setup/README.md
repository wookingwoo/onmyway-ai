# Actual native location setup evidence

Captured 2026-11-03 from the branch's live debug app on official Pleos Connect
2.1.0/API 34, isolated read-only AVD. Full native pixels retain the vehicle panel
and bottom bar. No edits, invented UI, personal home address or raw coordinates.
Emulated position does not establish physical GPS, provider search, actual route
acceptance/arrival or a completed live errand.

| File | Observed scope | SHA256 |
| --- | --- | --- |
| [home-confirmed.png](home-confirmed.png) | Actual SDK fix explicitly saved as home; expired departure visibly unready | `d702be377208f20a2b989d41d67b1f62ffd211ea69c90d701fa973fb5bd804f4` |
| [permission-denied.png](permission-denied.png) | Runtime denial recovery text; saved home preserved | `a3fec28018fe1e8adb905c1cd0533bc0a92a02d3148733756ab6b065a0b83131` |
| [driving-lock.png](driving-lock.png) | Actual Drive/30 dismissed pending position confirmation; native restriction visible | `dc6f42357550b4d6cfdd603b787dedfec755233248b9f8ec07a9d73ccca52995` |

Copies match ignored originals byte for byte. Captured after the permission
status-view fix, before final pure provenance fixes that do not alter these
screens. Final build/unit/lint evidence is in [progress](../../progress.md).
Park/0 restored; no raw native logs committed.
