# Automotive interaction redesign

The owner requested a separate branch, visual consistency with the Pleos shell,
and dramatically simpler interactions. The existing two-pane form exposes too
many inputs, technical details and competing actions at once.

## Approach

Use native Android Views and the existing API/state/navigation layers. A cosmetic
recolor would retain the interaction problem; a new UI framework would add build
risk without improving the core flow. The selected implementation is a staged
native flow: input, review, route. Advanced settings and evidence are secondary
screens/dialogs. This is the implementer's choice under the owner's existing
delegation, not a claim of separate design approval.

## Presentation

- Follow Android day/night mode. The light Pleos shell receives off-white/white
  surfaces, black text and restrained monochrome buttons; dark mode has equivalent
  neutral surfaces. No decorative dashboard panels or mint brand accent.
- One main content area and one fixed primary action. Main action controls at least 76dp;
  24sp body, 32–40sp headings, clear spacing. No dense two-pane form.
- Input asks only for errands. Review uses compact task rows and an edit dialog.
  Route emphasizes the next errand, destination and expected home arrival.
- Server URLs, coordinates, ISO timestamps and source diagnostics are absent from
  the main flow. They remain available in explicit advanced settings/details.
- Duration uses large five-minute steppers. Time uses large native number wheels;
  date changes use a secondary native calendar, available only while parked.
- Demo entry explicitly enables simulation; fixture provenance remains visible.
  Do not add nonfunctional voice buttons or imply automatic location support.

## Invariants

Preserve mandatory errands, provider errors, unknown data warnings, approval tied
to the fresh current revision, actual driving locks, explicit completion after
arrival and the separately approved final home leg. Navigation and completion
dialogs recheck the driving gate. Driving hides editing and presents a readable
locked state. No provider keys or public publication. Binary deliverables remain
local except the selected screenshots subsequently authorized by the owner.

## Evidence

Verify the staged flow and edits using Kotlin behavior tests, retain existing
safety tests, run Python and Android build/unit suites, install signed releases,
and inspect actual Pleos screenshots. Exercise demo load, review/edit, calculation,
approval, simulated arrival, completion/replan, settings return and driving lock.
The owner subsequently authorized selected screenshots in Git for the PR. Save
reviewed actual captures under docs/screenshots/; retain raw evidence locally.
Preserve the previous recording as evidence of the old UI.
