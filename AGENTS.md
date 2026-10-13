# Project Guidance

## Project overview

- This is a Minecraft 1.21.1 NeoForge mod.
- The mod adds road paints and road overlays for use in Minecraft builds.
- Its intended functionality is to match the road-paint functionality of Fureniku's Roads for Minecraft 1.12 as closely as practical.

## Current status

- The project has a compiling NeoForge prototype and a working Gradle build.
- The prototype currently provides one placeable white straight-line overlay. It is a thin, non-colliding block, rotates in four horizontal directions, and appears in the Road Overlays creative tab.
- The existing paint texture collection is stored under `assets/roadoverlays/textures/paints/`; most textures are not yet connected to in-game paint items.
- The full planned palette and paint designs are not implemented yet. Continue from the working prototype and expand only as needed for the core functionality.

## Development plan

Work through these phases in order. Keep each phase small, buildable, and focused on the road-paint goal.

1. **Confirm overlay behavior.** Run the development client and check placement on common solid blocks, four-way orientation, appearance at normal viewing distance, removal, support loss, world save/reload, and multiplayer/server behavior. Fix any placement or rendering problems in the prototype before multiplying paint variants.
2. **Catalog and connect the assets.** Review the PNGs in `assets/roadoverlays/textures/paints/`, record what each filename represents, and determine what the numeric suffixes mean from the images and existing asset set. Reuse the supplied textures. Add blockstates, models, and English names for the initial straight-line paint types; do not infer that a suffix is a color or direction until verified.
3. **Implement the main color palette.** Make the initial straight-line paints available in all 16 default Minecraft colors. Choose the simplest registration and tinting approach that preserves the transparent paint shape, readable item names, orientation, and normal block placement. Keep the color-to-texture or tint mapping explicit and easy to maintain.
4. **Complete main-release markings.** Add the planned straight and 45-degree diagonal lines, letters, and words using the existing artwork where suitable. Keep symbols, curved lines, and diagonal angles other than 45 degrees out of the main release; they are post-release additions. Add each paint to the creative tab and provide translations and item/block models.
5. **Check the user-facing result.** In the development client, confirm each initial-release design has the expected texture, color, direction, and placement behavior. Check that overlays do not collide with players or obstruct normal block use, and that placed paints persist after reloading a world. Fix issues found before expanding the feature set.
6. **Document and package.** Update `README.md` with the available paint types, placement/use details, and known limitations. Run `./gradlew build` and inspect the resulting JAR to ensure its metadata, models, translations, and paint textures are present at the expected `assets/roadoverlays/` paths.

After the main release is complete, consider post-release additions in this order: symbols (such as bicycle and bus markings), curved lines, then diagonal lines at angles other than 45 degrees. Keep these additions optional and out of scope until the main-release markings are complete.

## Development principles

- Keep the mod as simple as possible.
- Prefer the smallest implementation that provides the required road-paint and overlay behavior.
- Avoid adding systems, configuration, abstractions, dependencies, or features that are not needed for the core goal.
- Follow NeoForge 1.21.1 conventions and use vanilla Minecraft behavior where it is sufficient.
- Preserve compatibility with normal Minecraft worlds and avoid unnecessary changes to unrelated gameplay.

## Scope

- Prioritize road paint placement, appearance, and overlay behavior.
- Road paints are intended to use all 16 default Minecraft colors.
- Supported paint designs should include straight lines, diagonal lines, letters, symbols (for example, bicycle and bus markings), and words.
- Keep the initial release focused on the main road-paint functionality. Add curved lines, diagonal lines at angles other than 45 degrees, and symbols as post-release additions.
- When deciding how a feature should work, use Fureniku's Roads in Minecraft 1.12 as the functional reference, while adapting the implementation to Minecraft 1.21.1 and NeoForge.
- Do not expand the project into a general road-building, vehicle, traffic, or world-generation mod unless explicitly requested.

## Implementation expectations

- Keep code and assets organized according to standard NeoForge project structure.
- Store paint textures under `assets/roadoverlays/textures/`.
- Use clear names and straightforward code over clever or highly generic solutions.
- Test changes in a Minecraft 1.21.1 NeoForge development environment when practical.
- Update documentation when behavior, controls, or setup requirements change.
