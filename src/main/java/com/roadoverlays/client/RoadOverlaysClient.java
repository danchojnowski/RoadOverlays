package com.roadoverlays.client;

import com.roadoverlays.RoadOverlays;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

@EventBusSubscriber(modid = RoadOverlays.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class RoadOverlaysClient {
    private RoadOverlaysClient() {
    }

    @SubscribeEvent
    public static void registerBlockColors(RegisterColorHandlersEvent.Block event) {
        RoadOverlays.PAINT_BLOCKS.values().forEach(blocks -> blocks.forEach((color, block) -> {
            int tint = 0xFF000000 | color.getTextureDiffuseColor();
            event.register((state, level, pos, tintIndex) -> tintIndex == 0 ? tint : 0xFFFFFFFF, block.get());
        }));
    }

    @SubscribeEvent
    public static void registerItemColors(RegisterColorHandlersEvent.Item event) {
        RoadOverlays.PAINT_ITEMS.values().forEach(items -> items.forEach((color, item) -> {
            int tint = 0xFF000000 | color.getTextureDiffuseColor();
            event.register((stack, tintIndex) -> tintIndex == 0 ? tint : 0xFFFFFFFF, item.get());
        }));
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(RoadOverlays.ROAD_PAINT_BLOCK_ENTITY.get(),
                context -> new RoadPaintRenderer());
    }
}
