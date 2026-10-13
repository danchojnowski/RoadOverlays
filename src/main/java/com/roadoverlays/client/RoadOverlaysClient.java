package com.roadoverlays.client;

import com.roadoverlays.RoadOverlays;
import net.minecraft.world.item.DyeColor;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;

@EventBusSubscriber(modid = RoadOverlays.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class RoadOverlaysClient {
    private RoadOverlaysClient() {
    }

    @SubscribeEvent
    public static void registerBlockColors(RegisterColorHandlersEvent.Block event) {
        for (DyeColor color : DyeColor.values()) {
            int tint = 0xFF000000 | color.getTextureDiffuseColor();
            event.register((state, level, pos, tintIndex) -> tintIndex == 0 ? tint : 0xFFFFFFFF,
                    RoadOverlays.ROAD_LINES.get(color).get());
        }
    }

    @SubscribeEvent
    public static void registerItemColors(RegisterColorHandlersEvent.Item event) {
        for (DyeColor color : DyeColor.values()) {
            int tint = 0xFF000000 | color.getTextureDiffuseColor();
            event.register((stack, tintIndex) -> tintIndex == 0 ? tint : 0xFFFFFFFF,
                    RoadOverlays.ROAD_LINE_ITEMS.get(color).get());
        }
    }
}
