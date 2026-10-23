# App icon

![OnMyWay AI app icon](assets/app-icon.svg)

The open route loop echoes the O in OnMyWay. A forward arrow represents the next
destination; the mint point marks departure. The charcoal background and white
route follow the app's neutral UI. No text is embedded, so the mark stays readable
at launcher sizes.

Android uses native vector adaptive-icon layers under
`apps/automotive/app/src/main/res/`. Both demo and live variants share the icon.
The minimum supported API is 28, so no pre-adaptive bitmap fallback is needed.
One adaptive resource supplies the background, foreground and a dedicated
monochrome layer for launchers that support themed icons on API 33 and later.
Both manifest icon attributes reference the same adaptive resource, allowing the
launcher to apply its own circle, squircle or rounded-square mask.

The 108-unit layers keep the mark within the central 66-unit safe circle. The
SVG above is a reusable 512 px vector export with a rounded-square presentation,
using the same geometry and colors. Its 72-unit crop represents the resting
adaptive-icon viewport; it is artwork, not a device screenshot. Generated raster
previews and APKs stay local.
