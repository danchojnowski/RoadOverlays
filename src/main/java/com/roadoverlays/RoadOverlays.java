package com.roadoverlays;

import com.roadoverlays.block.RoadPaintBlock;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

@Mod(RoadOverlays.MOD_ID)
public class RoadOverlays {
    public static final String MOD_ID = "roadoverlays";

    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(Registries.BLOCK, MOD_ID);
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, MOD_ID);
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MOD_ID);

    public static final Supplier<Block> WHITE_ROAD_LINE = BLOCKS.register("white_road_line",
            () -> new RoadPaintBlock(Block.Properties.of().noCollission().instabreak().noOcclusion()));

    public static final Supplier<Item> WHITE_ROAD_LINE_ITEM = ITEMS.register("white_road_line",
            () -> new BlockItem(WHITE_ROAD_LINE.get(), new Item.Properties()));

    public static final Supplier<CreativeModeTab> ROAD_OVERLAYS_TAB = CREATIVE_MODE_TABS.register("road_overlays",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.roadoverlays"))
                    .icon(() -> WHITE_ROAD_LINE_ITEM.get().getDefaultInstance())
                    .displayItems((parameters, output) -> output.accept(WHITE_ROAD_LINE_ITEM.get()))
                    .build());

    public RoadOverlays(IEventBus modEventBus) {
        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        CREATIVE_MODE_TABS.register(modEventBus);
    }
}
