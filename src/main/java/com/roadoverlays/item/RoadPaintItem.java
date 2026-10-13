package com.roadoverlays.item;

import com.roadoverlays.block.RoadPaintBlock;
import com.roadoverlays.block.entity.RoadPaintBlockEntity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

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
        if (!super.placeBlock(context, state)) {
            return false;
        }
        if (context.getLevel().getBlockEntity(context.getClickedPos()) instanceof RoadPaintBlockEntity paint) {
            paint.setPlacement(surfaceHeight, lineOffset);
        }
        return true;
    }
}
