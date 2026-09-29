# Road Overlays

A Minecraft 1.21.1 NeoForge mod prototype for placing road paint over blocks.

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

## Prototype contents

The creative tab contains a white straight road line. Place it on top of a block; it has no collision and can be removed normally. The paint texture resources are in `assets/roadoverlays/textures/paints/`.
