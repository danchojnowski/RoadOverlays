package com.roadoverlays;

import com.roadoverlays.block.RoadPaintBlock;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.EnumMap;
import java.util.Map;
import java.util.function.Supplier;

@Mod(RoadOverlays.MOD_ID)
public class RoadOverlays {
    public static final String MOD_ID = "roadoverlays";

    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(Registries.BLOCK, MOD_ID);
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, MOD_ID);
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MOD_ID);

    public static final Map<DyeColor, Supplier<Block>> ROAD_LINES = new EnumMap<>(DyeColor.class);
    public static final Map<DyeColor, Supplier<Item>> ROAD_LINE_ITEMS = new EnumMap<>(DyeColor.class);

    static {
        for (DyeColor color : DyeColor.values()) {
            String name = color.getName() + "_road_line";
            Supplier<Block> block = BLOCKS.register(name,
                    () -> new RoadPaintBlock(Block.Properties.of().noCollission().instabreak().noOcclusion()));
            ROAD_LINES.put(color, block);
            ROAD_LINE_ITEMS.put(color, ITEMS.register(name,
                    () -> new BlockItem(block.get(), new Item.Properties())));
        }
    }

    public static final Supplier<Block> WHITE_ROAD_LINE = ROAD_LINES.get(DyeColor.WHITE);
    public static final Supplier<Item> WHITE_ROAD_LINE_ITEM = ROAD_LINE_ITEMS.get(DyeColor.WHITE);

    public static final Supplier<CreativeModeTab> ROAD_OVERLAYS_TAB = CREATIVE_MODE_TABS.register("road_overlays",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.roadoverlays"))
                    .icon(() -> WHITE_ROAD_LINE_ITEM.get().getDefaultInstance())
                    .displayItems((parameters, output) -> {
                        for (DyeColor color : DyeColor.values()) {
                            output.accept(ROAD_LINE_ITEMS.get(color).get());
                        }
                    })
                    .build());

    public RoadOverlays(IEventBus modEventBus) {
        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        CREATIVE_MODE_TABS.register(modEventBus);
    }
}
