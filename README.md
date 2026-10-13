# Road Overlays

A Minecraft 1.21.1 NeoForge mod for placing road paint over blocks. Requires NeoForge 21.1.248 or newer in the 21.1 series.

## Build and run

Requires Java 21. Build the mod with:

```sh
./gradlew build
```

Launch the development client with:

```sh
./gradlew runClient
```

The first Gradle run downloads Gradle and the NeoForge development dependencies.

## Available paints

The Road Overlays creative tab contains 134 paint designs in all 16 Minecraft dye colors: lines, junctions, arrows, chevrons, words, letters, numbers, and symbols. Every distinct image in `assets/roadoverlays/textures/paints/` is available. The other 25 PNGs are exact copies of a design already listed; their filenames are recorded as aliases in [`paint_catalog.tsv`](src/main/resources/paint_catalog.tsv).

Place a paint on top of a block and change its direction by facing a different way. Paint has no collision and can be removed normally. Each image is drawn within one block, regardless of its source PNG resolution.

To craft one road line, place three dyes of the same color vertically in a crafting table's center column. Other paints use the matching color road line, sometimes with matching dyes, in a shaped recipe. The exact 3×3 patterns are recorded in [`paint_catalog.tsv`](src/main/resources/paint_catalog.tsv): `L` is the colored road line, `D` is the matching dye, and `.` is an empty slot.

When placed on the top of a bottom slab or another partial block, paint uses the exact height of the surface you clicked. This includes finely divided surfaces such as LittleTiles or Chisels & Bits when they expose the clicked top face through normal Minecraft block targeting. Each paint stays flat across its block footprint, so a surface with multiple heights needs separate paint placements. If the supporting block is reshaped, place the paint again to choose the new height. Removing the supporting block also removes the paint.

The supplied white PNGs are tinted to the selected Minecraft dye color. Build time generation creates the blockstates, models, translations, and recipes from the paint catalog, so the source tree stays small while every design and color is packaged in the JAR.
