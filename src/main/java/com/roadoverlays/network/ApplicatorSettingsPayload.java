package com.roadoverlays.network;

import com.roadoverlays.RoadOverlays;
import com.roadoverlays.item.PaintApplicatorItem;
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

public record ApplicatorSettingsPayload(boolean offhand, int place, int skip) implements CustomPacketPayload {
    public static final Type<ApplicatorSettingsPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(RoadOverlays.MOD_ID, "applicator_settings"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ApplicatorSettingsPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, ApplicatorSettingsPayload::offhand,
            ByteBufCodecs.VAR_INT, ApplicatorSettingsPayload::place,
            ByteBufCodecs.VAR_INT, ApplicatorSettingsPayload::skip,
            ApplicatorSettingsPayload::new);

    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToServer(TYPE, STREAM_CODEC, ApplicatorSettingsPayload::handle);
    }

    private static void handle(ApplicatorSettingsPayload payload, IPayloadContext context) {
        Player player = context.player();
        ItemStack stack = player.getItemInHand(payload.offhand ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND);
        if (!(stack.getItem() instanceof PaintApplicatorItem)
                || payload.place < 1 || payload.place > PaintApplicatorItem.MAX_PATTERN
                || payload.skip < 0 || payload.skip > PaintApplicatorItem.MAX_PATTERN) return;
        stack.set(RoadOverlays.APPLICATOR_PLACE.get(), payload.place);
        stack.set(RoadOverlays.APPLICATOR_SKIP.get(), payload.skip);
        player.inventoryMenu.broadcastChanges();
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
