package com.roadoverlays.client;

import com.roadoverlays.RoadOverlays;
import com.roadoverlays.item.PaintApplicatorItem;
import com.roadoverlays.network.ApplicatorSettingsPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

public final class ApplicatorSettingsScreen extends Screen {
    private final InteractionHand hand;
    private int place;
    private int skip;

    private ApplicatorSettingsScreen(InteractionHand hand, int place, int skip) {
        super(Component.translatable("gui.roadoverlays.applicator_title"));
        this.hand = hand;
        this.place = Math.clamp(place, 1, PaintApplicatorItem.MAX_PATTERN);
        this.skip = Math.clamp(skip, 0, PaintApplicatorItem.MAX_PATTERN);
    }

    public static void open(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!(stack.getItem() instanceof PaintApplicatorItem)) return;
        Minecraft.getInstance().setScreen(new ApplicatorSettingsScreen(hand,
                stack.getOrDefault(RoadOverlays.APPLICATOR_PLACE.get(), 3),
                stack.getOrDefault(RoadOverlays.APPLICATOR_SKIP.get(), 0)));
    }

    @Override
    protected void init() {
        int x = width / 2 - 110;
        int y = height / 2 - 25;
        addRenderableWidget(new CountSlider(x, y, false));
        addRenderableWidget(new CountSlider(x, y + 28, true));
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), button -> save())
                .bounds(width / 2 - 105, y + 68, 100, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.cancel"), button -> onClose())
                .bounds(width / 2 + 5, y + 68, 100, 20).build());
    }

    private void save() {
        PacketDistributor.sendToServer(new ApplicatorSettingsPayload(
                hand == InteractionHand.OFF_HAND, place, skip));
        onClose();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(font, title, width / 2, height / 2 - 55, 0xFFFFFF);
    }

    private final class CountSlider extends AbstractSliderButton {
        private final boolean skipped;

        private CountSlider(int x, int y, boolean skipped) {
            super(x, y, 220, 20, Component.empty(),
                    ((skipped ? skip : place) - (skipped ? 0 : 1)) /
                            (double) (PaintApplicatorItem.MAX_PATTERN - (skipped ? 0 : 1)));
            this.skipped = skipped;
            updateMessage();
        }

        @Override
        protected void updateMessage() {
            setMessage(Component.translatable(skipped ? "gui.roadoverlays.applicator_skip"
                    : "gui.roadoverlays.applicator_place", skipped ? skip : place));
        }

        @Override
        protected void applyValue() {
            int count = (skipped ? 0 : 1) + (int) Math.round(value *
                    (PaintApplicatorItem.MAX_PATTERN - (skipped ? 0 : 1)));
            if (skipped) skip = count;
            else place = count;
        }
    }
}
