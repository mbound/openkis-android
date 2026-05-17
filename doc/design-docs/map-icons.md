# Map Icons — Design Document

## Status: Phase 1 in progress

---

## Context

The current map markers are simple 24dp vector drawables (inverted triangle / circle / square) tinted with solid colours. The OpenKIS system uses a richer PNG-based icon system where the icon is built by compositing multiple transparent 128×128 px layers that encode metadata visually (cave trend, hydrology type, air flow, closure status). The goal is to use these actual icons so the Android app's map matches the production website (catastogrotte-piemonte.net).

Reference: icon generation logic lives in the OpenKIS PHP codebase at `extra/openkis.inc.php`, function `openkis_GetIcon()` (lines 175–338).

---

## Icon Asset Inventory

**Source:** OpenKIS repo at `extra/openkis/icons/`

| Category | Available PNGs |
|---|---|
| `caves/` | `caves.png` `asc.png` `desc.png` `hori.png` `water.png` `emitting.png` `absorbent.png` `blow_during_cold.png` `blow_during_heat.png` `suck_during_cold.png` `suck_during_heat.png` `bats.png` `closed.png` `xxx.png` |
| `springs/` | `springs.png` `emitting.png` `absorbent.png` `blow.png` `suck.png` |
| `artificials/` | `artificials.png` `asc.png` `desc.png` `hori.png` `emitting.png` `absorbent.png` `blow.png` `suck.png` `bats.png` `closed.png` |

All icons are 128×128 px transparent PNG, designed to be composited in order by layering onto a single canvas. The server downsamples the composite to 64×64 for display; the Android app composites at 128×128 and lets OSMDroid display at its natural pixel size.

**Android asset location:** `app/src/main/assets/icons/{caves,springs,artificials}/`

---

## Icon Composition Logic

### Caves — layer order: `[base] → [trend] → [type] → [meteorology] → [closed]`

| Field(s) | Condition | Layer added |
|---|---|---|
| `depthNegative`, `depthPositive` | both 0 or missing | `hori` |
| | `|neg| >= |pos|` | `desc` |
| | `|pos| > |neg|` | `asc` |
| `hydrology` | contains "torrents", "siphons", or "lakes" | `water` |
| `hydrology` | contains "emitting" or "issuing" | `emitting` |
| `name`/`synonyms` (fallback if no hydrology match) | contains "risorgenza" or "sorgente" | `emitting` |
| `name`/`synonyms` | contains "inghiottitoio" | `absorbent` |
| `meteorology` | one of: `suck_during_cold`, `blow_during_heat`, `blow_during_cold`, `suck_during_heat` | that value |
| `closed` | not "N" and not blank | `closed` |
| `fauna` | contains "chirotteri" | `bats` — **deferred: no fauna field in CaveEntity yet** |

### Springs — Phase 1: `[springs]` base only
SpringEntity has no hydrology/meteorology fields that map to the available modifier icons.

### Artificials — Phase 1: `[artificials]` base only
ArtificialEntity has no `depthNegative`/`depthPositive` and no `closed` field.

---

## Architecture

### New: `ui/map/IconCompositor.kt`
`@Singleton` injected via Hilt (`@ApplicationContext context`).

- `LruCache<String, Bitmap>(64)` keyed by `"$category:${layers.joinToString(",")}"` — caches composited Bitmaps
- `fun compose(category: String, layers: List<String>): BitmapDrawable` — returns a **new** BitmapDrawable wrapper each call (safe to apply per-marker `setColorFilter` without polluting the cache)
- Compositing loop: create 128×128 ARGB_8888 Bitmap → Canvas → for each layer name, `assets.open("icons/$category/$layer.png")` → `BitmapFactory.decodeStream()` → `canvas.drawBitmap()` → `recycle()`

### New: `ui/map/MarkerIconResolver.kt`
Pure `object` (no DI).

- `fun caveIconLayers(cave: CaveEntity): List<String>` — implements the composition table above
- `fun springIconLayers(spring: SpringEntity): List<String>` — `listOf("springs")`
- `fun artificialIconLayers(art: ArtificialEntity): List<String>` — `listOf("artificials")`

### Modified: `ui/map/MapViewModel.kt`
Add `IconCompositor` to `@HiltViewModel` constructor; expose as `val compositor: IconCompositor`.

### Modified: `ui/map/MapScreen.kt`
- Remove `createIcon()` and the three pre-created icon variables
- Add local `markerIcon()` helper:
  ```kotlin
  fun markerIcon(category: String, layers: List<String>, selected: Boolean): Drawable {
      val d = compositor.compose(category, layers)
      if (selected) d.setColorFilter(selectedColor.toArgb(), PorterDuff.Mode.SRC_ATOP)
      return d
  }
  ```
- Call `MarkerIconResolver.caveIconLayers(cave)` → `markerIcon("caves", layers, isSelected)` per marker
- Change anchor from `ANCHOR_CENTER, ANCHOR_BOTTOM` → `ANCHOR_CENTER, ANCHOR_CENTER` (OpenKIS icons are square symbols centred on the coordinate, not pins)

### Unchanged
- `res/drawable/ic_cave.xml`, `ic_spring.xml`, `ic_artificial.xml` — kept, used in Browse and Detail screens

---

## What Is Deferred

| Feature | Reason | Tracking |
|---|---|---|
| `bats`/fauna icon | `CaveEntity` has no `fauna` field; adding it requires a DB schema bump + new CSV column mapping | Phase 2 |
| Spring/artificial modifier layers | Insufficient metadata fields in those entities | Phase 2 |
| Icon size scaling by depth/length | OpenKIS scales 0.2×–0.4× based on cave dimensions | Phase 2 |
| Springs/artificials metadata mapping | Needs `closed`, `depthNegative`/`depthPositive` fields added to entities | Phase 2 |

---

## Phase 2 Ideas

- Add `fauna` to `CaveEntity` (DB version bump) and map "chirotteri" → `bats` layer
- Add `closed` to `ArtificialEntity` and map it
- Add `depthNegative`/`depthPositive` to `ArtificialEntity` for trend icons
- Implement size scaling: icon dp size proportional to `max(depthTotal, lengthTotal / 3)` using the same breakpoints as OpenKIS (`≤2m → 0.2`, `≥200m → 0.4`)
- Spring modifier icons: would need additional fields (`emitting`/`absorbent` based on spring type)
- Cluster markers at low zoom levels (osmdroid has a clustering overlay)
