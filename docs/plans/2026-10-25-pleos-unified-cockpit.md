# Unified Pleos cockpit

Owner steering: demo and live must share a real-service UI; simple, voice-first,
visually consistent with Pleos. API credentials will be supplied later. This
extends the approved voice-first app/film task and keeps its open feature PR.

## Design direction

Use the observed native Pleos surface as the visual reference: system Korean
sans type, white/near-white surfaces, near-black ink, quiet gray secondary actions,
large rounded controls and generous space. No decorative gradients, new typeface,
dashboard tiles or web framework. `frontend-design`, `design-system` and
`ui-ux-pro-max` guide restrained hierarchy, semantic tokens, readable contrast and
spaced touch targets; this is not a claim of official Pleos design certification.

Palette: canvas #F5F6F7, surface #FFFFFF, primary ink #202124, muted ink #62676C,
secondary control #E9ECEF, warning #795313; retain explicit night variants.
Typography: native Korean system face; 40sp heading, 44sp ETA, 26sp important
content, 22sp secondary. Controls remain at least80dp high with12–16dp spacing.

Input has one primary action: speak. Direct text is secondary. Connection/source
selection moves to settings. Source choice is independently selected and retained;
one small persistent sample/simulation badge is sufficient. Labels, cards and
navigation/completion actions otherwise match live service. Never pretend a
prepared transcript is measured real ASR or silently use fixtures after failure.

The route has a compact summary: next exact venue, arrival/home ETA, ordered
required stops with dwell/completion deadlines, optional omissions, one departure
approval. Long original text, explanation and manual editors move behind clear
secondary actions. Spoken refinement stays prominent. Arrival and completion stay
separate; the completion button itself is the explicit completion confirmation.
Simulation event controls move to settings, outside the normal service journey.

## Execution

1. Add/test a shared presentation policy: same main labels in live/sample, sample
   source indication kept, simulation control only in demo+fixture+simulation and
   arrival-not-yet-received. Make the fresh approval/completion revision guards
   explicit. Implement compact native input, plan and voice surfaces.
2. Verify Python and Android tests/builds, inspect day/night and actual Drive lock
   screenshots, iterate against the full Pleos surface. API keys remain server-only.
3. Record the unified service UI and rebuild the full-Pleos film. Verify all final
   frames, captions and actual playback. Replace selected PR screenshots; keep
   recordings/audio/APKs local. Review and update the same PR with accurate evidence.

Ruling: the owner explicitly approved redesign implementation and autonomous PR
work. Proceed without an additional approval checkpoint. A small truthful source
badge preserves fixture transparency without turning each action into a demo UI.
