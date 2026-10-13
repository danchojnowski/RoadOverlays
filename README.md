# Road Overlays

A Minecraft 1.21.1 NeoForge mod prototype for placing road paint over blocks. Requires NeoForge 21.1.248 or newer in the 21.1 series.

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

The Road Overlays creative tab contains a straight road line in all 16 Minecraft dye colors: white, orange, magenta, light blue, yellow, lime, pink, gray, light gray, cyan, purple, blue, brown, green, red, and black. Place a line on top of a block and rotate it by facing a different direction. It has no collision and can be removed normally.

To craft one road line, place three dyes of the same color vertically in a crafting table's center column. Use the dye color matching the line you want.

When placed on the top of a bottom slab or another partial block, the line uses the exact height of the surface you clicked. This includes finely divided surfaces such as LittleTiles or Chisels & Bits, provided they expose a top face to Minecraft's normal block targeting. The line remains flat across the whole block and cannot follow an uneven surface. If the supporting block is reshaped, place the line again to choose the new height. Removing the supporting block also removes the line.

All colors reuse the supplied white `line_straight_full_0.png` paint shape. The other paint artwork in `assets/roadoverlays/textures/paints/` is not yet available as placeable items.
