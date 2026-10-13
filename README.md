# Road Overlays

A Minecraft 1.21.1 NeoForge mod for placing road paint over blocks. Requires NeoForge 21.1.248 or newer in the 21.1 series. The tested paint catalog is the 1.0 baseline; later versions add connected angled and curved road lines.

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

The Road Overlays creative tab contains 142 paint designs in all 16 Minecraft dye colors: lines, junctions, arrows, chevrons, words, letters, numbers, and symbols. Every distinct image in `assets/roadoverlays/textures/paints/` is available. The other 25 PNGs are exact copies of a design already listed; their filenames are recorded as aliases in [`paint_catalog.tsv`](src/main/resources/paint_catalog.tsv).

Version 1.5 provides adjustable single, double, thick, and double thick line items. Each item starts at 45°. Right-click without placing to open the angle slider. The range is 0°–90° in 0.5° steps. Click Done to save the selection or Cancel to leave it unchanged. The tooltip shows the selected angle. Face a different direction when placing to rotate the stripe. Matching lines on neighboring blocks align directly, including across colors or equivalent rotated angles. The angle and alignment are saved with each piece and survive world reloads. Paints at different surface heights do not connect.

The old fixed-angle block and item IDs were removed in 1.4. Replace any of those paints in older worlds before upgrading to this version.

Version 1.2 adds curved single, double, thick, and double thick lines. Each curve item has a radius starting at 2 blocks; version 1.5 raises the maximum to 100. Right-click without placing to open the radius slider, then click Done to save or Cancel to discard the selection. The tooltip shows the selected radius. Place the first piece while facing along the intended road direction; the curve turns right, or left if you sneak while placing it. Place more pieces on adjacent blocks along the curve. They inherit the first piece's circle center and radius, so their painted edges meet without transition pieces. You can mix colors within the same line style. Curves only connect when the neighboring paint is at the same surface height. Each piece saves its circle parameters with the world.

Place a paint on top of a block and change its direction by facing a different way. Paint has no collision and can be removed normally. Each image is drawn within one block, regardless of its source PNG resolution.

## Paint applicator

Craft a Paint Applicator from an iron ingot and a stick, or find it in the Road Overlays creative tab. Hold it in your main hand and carry a road line, straight line, adjustable angled line, or curved line anywhere in your inventory. Paint in the offhand takes priority; otherwise the tool selects the first supported paint stack from the hotbar, then the rest of the inventory. Right-click the top of a block to mark the start, then right-click a second block to place the run. Click the start again to cancel. The tool uses matching paint stacks from the offhand and inventory, consuming one item for each block placed in Survival mode. It leaves occupied spaces and unsupported surfaces untouched. Start and end must be in the same block layer; the paint follows the top collision surface of each block, including slabs and multipart blocks with a collision shape.

Right-click air with the applicator to set how many blocks to **place** and then **skip**. The defaults are 3 placed and 0 skipped; set 3 and 3 for repeating three-block dashes and three-block gaps. Each count can be set up to 64, and skip can be 0 for a continuous line. The pattern starts with a placed block at the first click and continues along the path. The tool lays up to 1024 blocks in one run.

For a normal straight line, the tool follows the horizontal or vertical axis closest to the second click. For an adjustable angled line, it calculates the angle from the centers of the selected start and end blocks, rounds to the nearest 0.5°, and follows that same angle across the run so the pieces connect. The held paint item's angle setting is only used for manual placement. For a curve, the tool fits the selected paint stack's radius through both clicked block centers and places the arc from start to end. Sneak during the first click to bend left; otherwise it bends right. If the selected blocks are farther apart than the curve's diameter, increase the radius or choose a closer end block.

To craft six road lines, place three dyes of the same color vertically in a crafting table's center column. Other paints use a matching color line, sometimes with matching dyes, in a shaped recipe. Recipes for line designs also yield six; other paint designs yield one. Each curved line is crafted from the matching adjustable angled line with the same color. The exact 3×3 patterns are recorded in [`paint_catalog.tsv`](src/main/resources/paint_catalog.tsv): `L` is the colored line named in the recipe source column, `D` is the matching dye, and `.` is an empty slot.

When placed on the top of a bottom slab or another partial block, paint uses the exact height of the surface you clicked. This includes finely divided surfaces such as LittleTiles or Chisels & Bits when they expose the clicked top face through normal Minecraft block targeting. Each paint stays flat across its block footprint, so a surface with multiple heights needs separate paint placements. If the supporting block is reshaped, place the paint again to choose the new height. Removing the supporting block also removes the paint.

The supplied and generated white PNGs are tinted to the selected Minecraft dye color. Build time generation creates item textures, blockstates, models, translations, and recipes from the paint catalog. Adjustable angled paint is drawn from its saved angle at render time, so one item covers every angle. Curved paint is drawn from a shared circle at render time, so one item covers every radius.
