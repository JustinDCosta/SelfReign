# SelfReign logo options

Six variations on the original green-leaf-on-black mark. Open the `.svg` files in
any browser or vector editor to preview them.

| File | Style |
|------|-------|
| `leaf-classic.svg` | Solid leaf + midrib (closest to the original) |
| `leaf-veined.svg` | Solid leaf with midrib and side veins |
| `leaf-gradient.svg` | Soft top-to-bottom green gradient leaf |
| `leaf-outline.svg` | Minimalist stroked outline, no fill |
| `leaf-ring.svg` | Leaf inside a thin "clean-time" progress ring |
| `leaf-sprout.svg` | Two new-growth leaves rising from a stem |

## Palette
- Leaf green: `#4CC9A0` (gradient variant: `#63E6AD` → `#2BA37A`)
- Vein / stroke: `#1E6F58`
- Background: `#000000` (true AMOLED black)

All are drawn on a 512×512 canvas with a rounded-square black tile, matching the
Play Store icon size.

## Using your pick in the app
Tell me which file you want and I'll wire it through everywhere the brand mark
appears (no manual work needed):

- `app/src/main/res/drawable/ic_launcher_foreground.xml` — adaptive icon foreground
- `app/src/main/res/drawable/ic_launcher_monochrome.xml` — themed-icon silhouette
- `app/src/main/res/mipmap-anydpi/ic_launcher*.xml` — pre-API-26 fallback
- `app/src/main/res/drawable/ic_notification.xml` — status-bar notification icon

(The app icons are Android **vector drawables**, not SVG, so the chosen leaf path
is translated into that format — the shapes here map over directly.)
