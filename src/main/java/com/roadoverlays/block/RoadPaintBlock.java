package com.roadoverlays.block;

import com.roadoverlays.block.entity.RoadPaintBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.Shapes;

public class RoadPaintBlock extends Block implements EntityBlock {
    private final DyeColor color;
    private final ResourceLocation texture;

    public RoadPaintBlock(DyeColor color, String texture, Properties properties) {
        super(properties);
        this.color = color;
        this.texture = ResourceLocation.fromNamespaceAndPath("roadoverlays", "block/" + texture);
        registerDefaultState(stateDefinition.any()
                .setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) {
        builder.add(BlockStateProperties.HORIZONTAL_FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState()
                .setValue(BlockStateProperties.HORIZONTAL_FACING, context.getHorizontalDirection().getOpposite());
    }

    public static double surfaceHeight(BlockPlaceContext context) {
        BlockPos supportPos = context.getClickedPos().below();
        if (context.getClickedFace() == Direction.UP) {
            return Mth.clamp(context.getClickLocation().y - supportPos.getY(), 0, 1);
        }
        VoxelShape supportShape = context.getLevel().getBlockState(supportPos)
                .getCollisionShape(context.getLevel(), supportPos);
        return supportShape.isEmpty() ? 1 : Mth.clamp(supportShape.max(Direction.Axis.Y), 0, 1);
    }

    public DyeColor getPaintColor() {
        return color;
    }

    public ResourceLocation getPaintTexture() {
        return texture;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new RoadPaintBlockEntity(pos, state);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        double height = level.getBlockEntity(pos) instanceof RoadPaintBlockEntity paint
                ? paint.getSurfaceHeight() : state.hasProperty(LegacyRoadPaintBlock.HEIGHT)
                ? state.getValue(LegacyRoadPaintBlock.HEIGHT) / 16.0 : 1;
        return box(0, (height - 1) * 16, 0, 16, 1, 16);
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return !level.getBlockState(pos.below()).isAir();
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighborState,
                                     LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        return direction == Direction.DOWN && neighborState.isAir()
                ? Blocks.AIR.defaultBlockState()
                : super.updateShape(state, direction, neighborState, level, pos, neighborPos);
    }
}
