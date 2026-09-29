# Project Guidance

## Project overview

- This is a Minecraft 1.21.1 NeoForge mod.
- The mod adds road paints and road overlays for use in Minecraft builds.
- Its intended functionality is to match the road-paint functionality of Fureniku's Roads for Minecraft 1.12 as closely as practical.

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
