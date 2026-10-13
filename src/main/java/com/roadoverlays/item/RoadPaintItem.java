package com.roadoverlays.item;

import com.roadoverlays.RoadOverlays;
import com.roadoverlays.block.RoadPaintBlock;
import com.roadoverlays.block.CurvePaint;
import com.roadoverlays.block.entity.RoadPaintBlockEntity;
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
        double lineOffset = block.getAngledPaint() == null ? 0
                : block.getAngledPaint().connectedOffset(context, state, surfaceHeight);
        CurvePaint.Parameters curve = block.getCurvePaint() == null ? null
                : block.getCurvePaint().parametersForPlacement(context, state, surfaceHeight,
                context.getItemInHand().getOrDefault(RoadOverlays.CURVE_RADIUS.get(), CurvePaint.DEFAULT_RADIUS));
        if (!super.placeBlock(context, state)) {
            return false;
        }
        if (context.getLevel().getBlockEntity(context.getClickedPos()) instanceof RoadPaintBlockEntity paint) {
            paint.setPlacement(surfaceHeight, lineOffset, curve);
        }
        return true;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (((RoadPaintBlock) getBlock()).getCurvePaint() == null) {
            return super.use(level, player, hand);
        }
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide) {
            int radius = stack.getOrDefault(RoadOverlays.CURVE_RADIUS.get(), CurvePaint.DEFAULT_RADIUS);
            radius = player.isShiftKeyDown() ? (radius == 1 ? CurvePaint.MAX_RADIUS : radius - 1)
                    : (radius == CurvePaint.MAX_RADIUS ? 1 : radius + 1);
            stack.set(RoadOverlays.CURVE_RADIUS.get(), radius);
            player.displayClientMessage(Component.translatable("message.roadoverlays.curve_radius", radius), true);
        }
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
    }
}
