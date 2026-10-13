package com.roadoverlays;

import com.roadoverlays.block.RoadPaintBlock;
import com.roadoverlays.block.LegacyRoadPaintBlock;
import com.roadoverlays.block.entity.RoadPaintBlockEntity;
import com.roadoverlays.item.RoadPaintItem;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

@Mod(RoadOverlays.MOD_ID)
public class RoadOverlays {
    public static final String MOD_ID = "roadoverlays";

    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(Registries.BLOCK, MOD_ID);
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, MOD_ID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, MOD_ID);
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MOD_ID);

    public static final Map<DyeColor, Supplier<Block>> ROAD_LINES = new EnumMap<>(DyeColor.class);
    public static final Map<DyeColor, Supplier<Item>> ROAD_LINE_ITEMS = new EnumMap<>(DyeColor.class);
    public static final Map<String, Map<DyeColor, Supplier<Block>>> PAINT_BLOCKS = new LinkedHashMap<>();
    public static final Map<String, Map<DyeColor, Supplier<Item>>> PAINT_ITEMS = new LinkedHashMap<>();

    static {
        for (PaintCatalog.Design design : PaintCatalog.DESIGNS) {
            Map<DyeColor, Supplier<Block>> blocks = new EnumMap<>(DyeColor.class);
            Map<DyeColor, Supplier<Item>> items = new EnumMap<>(DyeColor.class);
            for (DyeColor color : DyeColor.values()) {
                String name = color.getName() + "_" + design.id();
                Supplier<Block> block = BLOCKS.register(name, () -> design.id().equals("road_line")
                        ? new LegacyRoadPaintBlock(color, Block.Properties.of().noCollission().instabreak().noOcclusion())
                        : new RoadPaintBlock(color, design.texture(),
                        Block.Properties.of().noCollission().instabreak().noOcclusion()));
                blocks.put(color, block);
                items.put(color, ITEMS.register(name,
                        () -> new RoadPaintItem(block.get(), new Item.Properties())));
            }
            PAINT_BLOCKS.put(design.id(), blocks);
            PAINT_ITEMS.put(design.id(), items);
        }
        ROAD_LINES.putAll(PAINT_BLOCKS.get("road_line"));
        ROAD_LINE_ITEMS.putAll(PAINT_ITEMS.get("road_line"));
    }

    public static final Supplier<BlockEntityType<RoadPaintBlockEntity>> ROAD_PAINT_BLOCK_ENTITY =
            BLOCK_ENTITY_TYPES.register("road_paint", () -> BlockEntityType.Builder.of(
                    RoadPaintBlockEntity::new, PAINT_BLOCKS.values().stream()
                            .flatMap(blocks -> blocks.values().stream()).map(Supplier::get).toArray(Block[]::new)
            ).build(null));

    public static final Supplier<Block> WHITE_ROAD_LINE = ROAD_LINES.get(DyeColor.WHITE);
    public static final Supplier<Item> WHITE_ROAD_LINE_ITEM = ROAD_LINE_ITEMS.get(DyeColor.WHITE);

    public static final Supplier<CreativeModeTab> ROAD_OVERLAYS_TAB = CREATIVE_MODE_TABS.register("road_overlays",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.roadoverlays"))
                    .icon(() -> WHITE_ROAD_LINE_ITEM.get().getDefaultInstance())
                    .displayItems((parameters, output) -> {
                        for (Map<DyeColor, Supplier<Item>> items : PAINT_ITEMS.values()) {
                            for (DyeColor color : DyeColor.values()) {
                                output.accept(items.get(color).get());
                            }
                        }
                    })
                    .build());

    public RoadOverlays(IEventBus modEventBus) {
        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        BLOCK_ENTITY_TYPES.register(modEventBus);
        CREATIVE_MODE_TABS.register(modEventBus);
    }
}
