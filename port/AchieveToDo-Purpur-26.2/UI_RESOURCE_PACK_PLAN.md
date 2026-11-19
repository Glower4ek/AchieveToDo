# UI Resource Pack Plan

Stage: `10A - Plan only`

Decision:

- recommended mode: `REQUIRED FOR INTENDED FIDELITY`

Why:

- the original UI’s most important runtime surface is not a generic menu, but a visually distinct icon browser with masked hidden states and custom unlock presentation
- vanilla clients cannot receive the Fabric advancement-screen mixins
- without a server resource pack, the reconstruction would collapse into a functional but much less recognizable inventory/list UI

This file is a plan only. No textures, fonts, or models are created in Stage 10A.

## 1. Goals

The pack should enable:

- recognizable AchieveToDo iconography
- distinct hidden/revealed/unlocked state presentation
- decorative framing for the future inventory-backed browser
- stronger toast/feedback identity where vanilla allows approximation

The pack does not make the following possible by itself:

- injecting into the native advancements screen
- recreating Fabric-only render hooks exactly
- reproducing fullscreen blackout and in-world forcefield overlays exactly

## 2. Asset Categories Needed

### 2.1 Ability icon set

Original visual purpose:

- each ability is quickly recognizable by its icon/theme

Planned vanilla representation:

- custom item models / item textures for UI carrier items
- separate variants for:
  - hidden/locked placeholder
  - revealed locked
  - unlocked/highlighted if useful

### 2.2 Category / tab identity

Original visual purpose:

- categories are navigable and visually distinct

Planned vanilla representation:

- custom tab-selector items
- possible glyph-backed labels or decorative prefix symbols

### 2.3 Hidden-state masking

Original visual purpose:

- mystery state is visibly obscured rather than simply colored red

Planned vanilla representation:

- dedicated hidden-state icon variants
- optionally glyph overlays in item names/lore if needed

### 2.4 Panel / frame decoration

Original visual purpose:

- world-creation tab and toast use custom framing and color identity

Planned vanilla representation:

- decorative glyphs/font framing for titles and separators
- possibly inventory title glyph work if current vanilla rendering allows it well enough

### 2.5 Unlock feedback identity

Original visual purpose:

- custom toast background and category titles

Planned vanilla representation:

- keep vanilla toast mechanics where available
- use custom icon item art plus text styling for stronger identity
- optional sound choices remain server-side, not pack-exclusive

## 3. Font / Glyph Strategy

Original audit result:

- original uses vanilla font only
- no custom font assets exist in the Fabric source

Stage 10C plan:

- introduce a custom font provider only if it materially improves:
  - decorative separators
  - compact icon/text composition
  - pseudo-panel framing
  - controlled spacing

Guidelines:

- do not build a giant icon font for every ability if item-model icons solve the problem more cleanly
- prefer glyph use for:
  - chrome
  - separators
  - small symbolic markers

## 4. Custom Item / Icon Strategy

Recommended approach:

- use custom model data or the current-item-component equivalent supported by the target runtime/version chosen in Stage 10C
- dedicate a stable internal icon registry for:
  - ability entry carriers
  - category entries
  - navigation controls
  - hidden-state placeholders

Rationale:

- inventory UI naturally surfaces items, names, lore, hover tooltips, and click targets
- this is the closest vanilla-compatible mechanism to the original icon-heavy advancements browser

## 5. Delivery Strategy

Recommended delivery mode:

- server resource pack
- expected to be required for intended fidelity on the friends server

Fallback if pack is absent:

- keep UI functional using vanilla items/text only
- degrade visual state distinctions from rich iconography to text/lore markers
- accept lower resemblance to original

Suggested Stage 10C responsibilities:

- define pack metadata/version
- build icon asset set
- build any approved font/glyph providers
- wire pack packaging into the plugin/resource pipeline
- document fallback mode explicitly

## 6. Version / Format Considerations

Stage 10C must verify against the actual target Minecraft/Purpur version at implementation time:

- pack format / supported metadata
- current custom-item-model path
- any font-provider limitations
- title/inventory text rendering constraints

Stage 10A deliberately avoids hardcoding fragile version-specific pack tricks beyond this planning level.

## 7. What Stage 10C Should Actually Implement

- pack metadata and packaging
- icon asset atlas/set for abilities and navigation
- hidden/revealed/unlocked state art rules
- optional decorative glyph layer if still justified
- fallback inventory/text presentation rules for no-pack clients

## 8. Non-Goals

- no attempt to recreate the Fabric advancements screen exactly
- no attempt to recreate fullscreen blackout overlay exactly
- no attempt to recreate the landmark forcefield renderer exactly
