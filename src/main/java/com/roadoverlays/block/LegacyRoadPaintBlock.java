package com.roadoverlays.block;

import net.minecraft.util.Mth;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

/** Keeps the original road line block states loadable in existing worlds. */
public class LegacyRoadPaintBlock extends RoadPaintBlock {
    public static final IntegerProperty HEIGHT = IntegerProperty.create("height", 1, 16);

    public LegacyRoadPaintBlock(DyeColor color, Properties properties) {
        super(color, "white_road_line", properties);
        registerDefaultState(defaultBlockState().setValue(HEIGHT, 16));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(HEIGHT);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        int height = Mth.clamp((int) Math.ceil(surfaceHeight(context) * 16 - 1.0e-6), 1, 16);
        return super.getStateForPlacement(context).setValue(HEIGHT, height);
    }
}
