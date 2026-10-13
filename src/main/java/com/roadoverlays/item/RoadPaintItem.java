package com.roadoverlays.item;

import com.roadoverlays.RoadOverlays;
import com.roadoverlays.block.RoadPaintBlock;
import com.roadoverlays.block.CurvePaint;
import com.roadoverlays.block.AngledPaint;
import com.roadoverlays.block.entity.RoadPaintBlockEntity;
import com.roadoverlays.client.PaintSettingsScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

public class RoadPaintItem extends BlockItem {
    public RoadPaintItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    protected boolean placeBlock(BlockPlaceContext context, BlockState state) {
        double surfaceHeight = RoadPaintBlock.surfaceHeight(context);
        RoadPaintBlock block = (RoadPaintBlock) state.getBlock();
        AngledPaint.Placement line = block.getAngledPaint() == null ? null
                : block.getAngledPaint().connectedPlacement(context, state, surfaceHeight,
                context.getItemInHand().getOrDefault(RoadOverlays.LINE_ANGLE.get(), AngledPaint.DEFAULT_ANGLE));
        CurvePaint.Parameters curve = block.getCurvePaint() == null ? null
                : block.getCurvePaint().parametersForPlacement(context, state, surfaceHeight,
                context.getItemInHand().getOrDefault(RoadOverlays.CURVE_RADIUS.get(), CurvePaint.DEFAULT_RADIUS));
        if (!super.placeBlock(context, state)) {
            return false;
        }
        if (context.getLevel().getBlockEntity(context.getClickedPos()) instanceof RoadPaintBlockEntity paint) {
            paint.setPlacement(surfaceHeight, line == null ? 0 : line.offset(),
                    line == null ? -1 : line.angleHalfDegrees(), curve);
        }
        return true;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        RoadPaintBlock block = (RoadPaintBlock) getBlock();
        if (block.getCurvePaint() == null && block.getAngledPaint() == null) {
            return super.use(level, player, hand);
        }
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) PaintSettingsScreen.open(player, hand);
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context,
                                List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        if (((RoadPaintBlock) getBlock()).getCurvePaint() != null) {
            int radius = stack.getOrDefault(RoadOverlays.CURVE_RADIUS.get(), CurvePaint.DEFAULT_RADIUS);
            tooltip.add(Component.translatable("tooltip.roadoverlays.curve_radius", radius));
            tooltip.add(Component.translatable("tooltip.roadoverlays.curve_controls"));
        }
        AngledPaint angled = ((RoadPaintBlock) getBlock()).getAngledPaint();
        if (angled != null) {
            int angle = stack.getOrDefault(RoadOverlays.LINE_ANGLE.get(), AngledPaint.DEFAULT_ANGLE);
            tooltip.add(Component.translatable("tooltip.roadoverlays.line_angle", angle / 2.0));
            tooltip.add(Component.translatable("tooltip.roadoverlays.line_controls"));
        }
    }
}
