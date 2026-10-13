# Road Overlays

A Minecraft 1.21.1 NeoForge mod for placing road paint over blocks. Requires NeoForge 21.1.248 or newer in the 21.1 series. The tested paint catalog is the 1.0 baseline; versions 1.1 and 1.2 add connected angled and curved road lines.

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

The Road Overlays creative tab contains 158 paint designs in all 16 Minecraft dye colors: lines, junctions, arrows, chevrons, words, letters, numbers, and symbols. Every distinct image in `assets/roadoverlays/textures/paints/` is available. The other 25 PNGs are exact copies of a design already listed; their filenames are recorded as aliases in [`paint_catalog.tsv`](src/main/resources/paint_catalog.tsv).

Version 1.1 adds 22.5°, 30°, 45°, 60°, and 67.5° variants of the single, double, thick, and double thick straight lines. These 20 designs use the same line widths and spacing as the original artwork. Their textures are generated at build time, and each design is available in all 16 colors.

Version 1.1.1 lets angled lines connect directly. Start a line with any angled paint, then place matching width and spacing variants on the neighboring blocks that the stripe reaches. The new piece lines up with the existing edge, including when the color changes or an equivalent rotated angle is used. The alignment is saved with each piece and survives world reloads. Segments placed with version 1.1.0 keep their old centered position; re-place them to connect an existing line. Paints at different surface heights do not connect.

Version 1.2 adds curved single, double, thick, and double thick lines. Each curve item has a radius from 1 to 64 blocks, starting at 2. Right-click without placing to increase the radius; sneak-right-click without placing to decrease it. The selected radius appears in the item tooltip and briefly above the hotbar when changed. Place the first piece while facing along the intended road direction; the curve turns right, or left if you sneak while placing it. Place more pieces on adjacent blocks along the curve. They inherit the first piece's circle center and radius, so their painted edges meet without transition pieces. You can mix colors within the same line style. Curves only connect when the neighboring paint is at the same surface height. Each piece saves its circle parameters with the world.

Place a paint on top of a block and change its direction by facing a different way. Paint has no collision and can be removed normally. Each image is drawn within one block, regardless of its source PNG resolution.

To craft one road line, place three dyes of the same color vertically in a crafting table's center column. Other paints use a matching color line, sometimes with matching dyes, in a shaped recipe. Each curved line is crafted from the matching 45° angled line with the same color. The exact 3×3 patterns are recorded in [`paint_catalog.tsv`](src/main/resources/paint_catalog.tsv): `L` is the colored line named in the recipe source column, `D` is the matching dye, and `.` is an empty slot.

When placed on the top of a bottom slab or another partial block, paint uses the exact height of the surface you clicked. This includes finely divided surfaces such as LittleTiles or Chisels & Bits when they expose the clicked top face through normal Minecraft block targeting. Each paint stays flat across its block footprint, so a surface with multiple heights needs separate paint placements. If the supporting block is reshaped, place the paint again to choose the new height. Removing the supporting block also removes the paint.

The supplied and generated white PNGs are tinted to the selected Minecraft dye color. Build time generation creates the angled and curved item textures, blockstates, models, translations, and recipes from the paint catalog, so the source tree stays small while every design and color is packaged in the JAR. The angled textures include a larger paint canvas for selecting a continuous stripe across adjacent blocks. Curved paint is drawn from a shared circle at render time, so one item covers every radius.
