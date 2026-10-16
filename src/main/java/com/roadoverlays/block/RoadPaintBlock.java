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
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.Shapes;

public class RoadPaintBlock extends Block implements EntityBlock {
    private final DyeColor color;
    private final ResourceLocation texture;
    private final AngledPaint angledPaint;
    private final CurvePaint curvePaint;
    private final boolean uprightArtwork;

    public RoadPaintBlock(DyeColor color, String texture, Properties properties) {
        super(properties);
        this.color = color;
        this.angledPaint = AngledPaint.fromTexture(texture);
        this.curvePaint = CurvePaint.fromTexture(texture);
        this.uprightArtwork = !texture.startsWith("line_")
                && !texture.equals("white_road_line")
                && angledPaint == null && curvePaint == null;
        this.texture = ResourceLocation.fromNamespaceAndPath("roadoverlays",
                "block/" + texture);
        registerDefaultState(stateDefinition.any()
                .setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH)
                .setValue(BlockStateProperties.ATTACH_FACE, AttachFace.FLOOR));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) {
        builder.add(BlockStateProperties.HORIZONTAL_FACING, BlockStateProperties.ATTACH_FACE);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction face = context.getClickedFace();
        if (face == Direction.DOWN) return null;
        return defaultBlockState()
                .setValue(BlockStateProperties.ATTACH_FACE,
                        face == Direction.UP ? AttachFace.FLOOR : AttachFace.WALL)
                .setValue(BlockStateProperties.HORIZONTAL_FACING,
                        face == Direction.UP ? context.getHorizontalDirection().getOpposite() : face);
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

    public AngledPaint getAngledPaint() {
        return angledPaint;
    }

    public CurvePaint getCurvePaint() {
        return curvePaint;
    }

    public boolean hasUprightArtwork() {
        return uprightArtwork;
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
        if (state.getValue(BlockStateProperties.ATTACH_FACE) == AttachFace.WALL) {
            return switch (state.getValue(BlockStateProperties.HORIZONTAL_FACING)) {
                case NORTH -> box(0, 0, 15, 16, 16, 16);
                case SOUTH -> box(0, 0, 0, 16, 16, 1);
                case WEST -> box(15, 0, 0, 16, 16, 16);
                default -> box(0, 0, 0, 1, 16, 16);
            };
        }
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
        if (state.getValue(BlockStateProperties.ATTACH_FACE) == AttachFace.WALL) {
            Direction facing = state.getValue(BlockStateProperties.HORIZONTAL_FACING);
            BlockPos support = pos.relative(facing.getOpposite());
            return level.getBlockState(support).isFaceSturdy(level, support, facing);
        }
        return !level.getBlockState(pos.below()).isAir();
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighborState,
                                     LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        Direction supportDirection = state.getValue(BlockStateProperties.ATTACH_FACE) == AttachFace.WALL
                ? state.getValue(BlockStateProperties.HORIZONTAL_FACING).getOpposite() : Direction.DOWN;
        return direction == supportDirection && !canSurvive(state, level, pos)
                ? Blocks.AIR.defaultBlockState()
                : super.updateShape(state, direction, neighborState, level, pos, neighborPos);
    }
}
