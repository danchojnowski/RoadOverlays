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

All colors reuse the supplied white `line_straight_full_0.png` paint shape. The other paint artwork in `assets/roadoverlays/textures/paints/` is not yet available as placeable items.
