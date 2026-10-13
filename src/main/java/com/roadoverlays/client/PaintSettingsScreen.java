package com.roadoverlays.client;

import com.roadoverlays.RoadOverlays;
import com.roadoverlays.block.AngledPaint;
import com.roadoverlays.block.CurvePaint;
import com.roadoverlays.block.RoadPaintBlock;
import com.roadoverlays.item.RoadPaintItem;
import com.roadoverlays.network.PaintSettingsPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

/** Shared settings screen for adjustable line angles and curve radii. */
public final class PaintSettingsScreen extends Screen {
    private final InteractionHand hand;
    private final ResourceLocation itemId;
    private final boolean curve;
    private final int minimum;
    private final int maximum;
    private int selected;

    private PaintSettingsScreen(InteractionHand hand, ResourceLocation itemId, boolean curve, int selected) {
        super(Component.translatable(curve ? "gui.roadoverlays.curve_title" : "gui.roadoverlays.angle_title"));
        this.hand = hand;
        this.itemId = itemId;
        this.curve = curve;
        this.minimum = curve ? 1 : 0;
        this.maximum = curve ? CurvePaint.MAX_RADIUS : AngledPaint.MAX_ANGLE;
        this.selected = Math.clamp(selected, minimum, maximum);
    }

    public static void open(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!(stack.getItem() instanceof RoadPaintItem item)) return;
        RoadPaintBlock block = (RoadPaintBlock) item.getBlock();
        boolean curve = block.getCurvePaint() != null;
        if (!curve && block.getAngledPaint() == null) return;
        int selected = curve
                ? stack.getOrDefault(RoadOverlays.CURVE_RADIUS.get(), CurvePaint.DEFAULT_RADIUS)
                : stack.getOrDefault(RoadOverlays.LINE_ANGLE.get(), AngledPaint.DEFAULT_ANGLE);
        Minecraft.getInstance().setScreen(new PaintSettingsScreen(hand,
                BuiltInRegistries.ITEM.getKey(stack.getItem()), curve, selected));
    }

    @Override
    protected void init() {
        int centerX = width / 2;
        int centerY = height / 2;
        addRenderableWidget(new SettingSlider(centerX - 110, centerY - 10, 220, 20));
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), button -> save())
                .bounds(centerX - 105, centerY + 44, 100, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.cancel"), button -> onClose())
                .bounds(centerX + 5, centerY + 44, 100, 20).build());
    }

    private void save() {
        PacketDistributor.sendToServer(new PaintSettingsPayload(curve, hand == InteractionHand.OFF_HAND,
                itemId, selected));
        onClose();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(font, title, width / 2, height / 2 - 48, 0xFFFFFF);
        String low = curve ? "1" : "0°";
        String high = curve ? Integer.toString(CurvePaint.MAX_RADIUS) : "90°";
        graphics.drawString(font, low, width / 2 - 110, height / 2 + 17, 0xA0A0A0);
        graphics.drawString(font, high, width / 2 + 110 - font.width(high), height / 2 + 17, 0xA0A0A0);
    }

    private final class SettingSlider extends AbstractSliderButton {
        private SettingSlider(int x, int y, int width, int height) {
            super(x, y, width, height, Component.empty(),
                    (selected - minimum) / (double) (maximum - minimum));
            updateMessage();
        }

        @Override
        protected void updateMessage() {
            setMessage(Component.translatable(curve ? "gui.roadoverlays.curve_value" : "gui.roadoverlays.angle_value",
                    curve ? Integer.toString(selected)
                            : selected % 2 == 0 ? Integer.toString(selected / 2) : Double.toString(selected / 2.0)));
        }

        @Override
        protected void applyValue() {
            selected = minimum + (int) Math.round(value * (maximum - minimum));
            value = (selected - minimum) / (double) (maximum - minimum);
        }

        @Override
        public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
            if (isFocused() && (keyCode == 263 || keyCode == 262)) {
                selected = Math.clamp(selected + (keyCode == 262 ? 1 : -1), minimum, maximum);
                value = (selected - minimum) / (double) (maximum - minimum);
                updateMessage();
                return true;
            }
            return super.keyPressed(keyCode, scanCode, modifiers);
        }
    }
}
