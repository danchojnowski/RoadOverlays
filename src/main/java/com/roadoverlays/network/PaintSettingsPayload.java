package com.roadoverlays.network;

import com.roadoverlays.RoadOverlays;
import com.roadoverlays.block.AngledPaint;
import com.roadoverlays.block.CurvePaint;
import com.roadoverlays.block.RoadPaintBlock;
import com.roadoverlays.item.RoadPaintItem;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Applies a slider selection to the item still held in the selected hand. */
public record PaintSettingsPayload(boolean curve, boolean offhand, ResourceLocation itemId, int value)
        implements CustomPacketPayload {
    public static final Type<PaintSettingsPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(RoadOverlays.MOD_ID, "paint_settings"));
    public static final StreamCodec<RegistryFriendlyByteBuf, PaintSettingsPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, PaintSettingsPayload::curve,
            ByteBufCodecs.BOOL, PaintSettingsPayload::offhand,
            ResourceLocation.STREAM_CODEC, PaintSettingsPayload::itemId,
            ByteBufCodecs.VAR_INT, PaintSettingsPayload::value,
            PaintSettingsPayload::new);

    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToServer(TYPE, STREAM_CODEC, PaintSettingsPayload::handle);
    }

    private static void handle(PaintSettingsPayload payload, IPayloadContext context) {
        Player player = context.player();
        ItemStack stack = player.getItemInHand(payload.offhand ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND);
        if (!(stack.getItem() instanceof RoadPaintItem paintItem)
                || !BuiltInRegistries.ITEM.getKey(stack.getItem()).equals(payload.itemId)) return;
        RoadPaintBlock block = (RoadPaintBlock) paintItem.getBlock();
        if (payload.curve) {
            if (block.getCurvePaint() == null || payload.value < 1 || payload.value > CurvePaint.MAX_RADIUS) return;
            stack.set(RoadOverlays.CURVE_RADIUS.get(), payload.value);
        } else {
            if (block.getAngledPaint() == null || payload.value < 0 || payload.value > AngledPaint.MAX_ANGLE) return;
            stack.set(RoadOverlays.LINE_ANGLE.get(), payload.value);
        }
        player.inventoryMenu.broadcastChanges();
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
