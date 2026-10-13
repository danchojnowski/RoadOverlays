package com.roadoverlays.item;

import com.roadoverlays.RoadOverlays;
import com.roadoverlays.block.AngledPaint;
import com.roadoverlays.block.CurvePaint;
import com.roadoverlays.block.LegacyRoadPaintBlock;
import com.roadoverlays.block.RoadPaintBlock;
import com.roadoverlays.block.entity.RoadPaintBlockEntity;
import com.roadoverlays.client.ApplicatorSettingsScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Two-click placement of a continuous or dashed road marking using carried paint. */
public final class PaintApplicatorItem extends Item {
    public static final int MAX_PATTERN = 64;
    private static final int MAX_TILES = 512;
    private static final Map<UUID, Start> STARTS = new java.util.HashMap<>();

    private record Start(BlockPos support, String dimension, Item paintItem, Direction facing,
                         double centerX, double centerZ, double hitX, double hitZ,
                         double radiusCenterX, double radiusCenterZ,
                         int radius, boolean curve) { }

    public PaintApplicatorItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null || context.getClickedFace() != Direction.UP) return InteractionResult.PASS;
        Level level = context.getLevel();
        String dimension = level.dimension().location().toString();
        Start start = STARTS.get(player.getUUID());
        ItemStack paintStack = start != null && start.dimension.equals(dimension)
                ? findPaint(player, start.paintItem) : selectPaint(player);
        if (!(paintStack.getItem() instanceof RoadPaintItem paintItem) || !supported(paintItem)) {
            if (!level.isClientSide) {
                STARTS.remove(player.getUUID());
                player.displayClientMessage(Component.translatable("message.roadoverlays.applicator_inventory"), true);
            }
            return InteractionResult.SUCCESS;
        }
        if (level.isClientSide) return InteractionResult.SUCCESS;

        BlockPos support = context.getClickedPos();
        if (start == null || !start.dimension.equals(dimension) || start.paintItem != paintStack.getItem()) {
            Direction facing = player.getDirection().getOpposite();
            RoadPaintBlock block = (RoadPaintBlock) paintItem.getBlock();
            CurvePaint curve = block.getCurvePaint();
            int radius = curve == null ? 0 : Math.clamp(paintStack.getOrDefault(
                    RoadOverlays.CURVE_RADIUS.get(), CurvePaint.DEFAULT_RADIUS), 1, CurvePaint.MAX_RADIUS);
            Direction side = player.isShiftKeyDown() ? facing.getCounterClockWise() : facing.getClockWise();
            STARTS.put(player.getUUID(), new Start(support, dimension, paintStack.getItem(), facing,
                    support.getX() + 0.5, support.getZ() + 0.5,
                    context.getClickLocation().x, context.getClickLocation().z,
                    support.getX() + 0.5 + side.getStepX() * radius,
                    support.getZ() + 0.5 + side.getStepZ() * radius, radius, curve != null));
            player.displayClientMessage(Component.translatable("message.roadoverlays.applicator_start"), true);
            return InteractionResult.SUCCESS;
        }
        STARTS.remove(player.getUUID());
        if (support.equals(start.support)) {
            player.displayClientMessage(Component.translatable("message.roadoverlays.applicator_cancel"), true);
            return InteractionResult.SUCCESS;
        }
        if (support.getY() != start.support.getY()) {
            player.displayClientMessage(Component.translatable("message.roadoverlays.applicator_height"), true);
            return InteractionResult.SUCCESS;
        }
        int placed = layPaint(level, player, paintItem, start, support, context.getClickLocation(),
                context.getItemInHand().getOrDefault(RoadOverlays.APPLICATOR_PLACE.get(), 3),
                context.getItemInHand().getOrDefault(RoadOverlays.APPLICATOR_SKIP.get(), 0));
        player.displayClientMessage(Component.translatable("message.roadoverlays.applicator_placed", placed), true);
        return InteractionResult.SUCCESS;
    }

    private static boolean supported(RoadPaintItem item) {
        String id = BuiltInRegistries.ITEM.getKey(item).getPath();
        return id.endsWith("_road_line") || id.contains("_line_straight_")
                || id.contains("_angled_line_") || id.contains("_curved_line_");
    }

    private static ItemStack selectPaint(Player player) {
        ItemStack offhand = player.getOffhandItem();
        if (offhand.getItem() instanceof RoadPaintItem paint && supported(paint)) return offhand;
        for (ItemStack stack : player.getInventory().items) {
            if (stack.getItem() instanceof RoadPaintItem paint && supported(paint)) return stack;
        }
        return ItemStack.EMPTY;
    }

    private static ItemStack findPaint(Player player, Item item) {
        ItemStack offhand = player.getOffhandItem();
        if (!offhand.isEmpty() && offhand.is(item)) return offhand;
        for (ItemStack stack : player.getInventory().items) {
            if (!stack.isEmpty() && stack.is(item)) return stack;
        }
        return ItemStack.EMPTY;
    }

    private static int layPaint(Level level, Player player, RoadPaintItem paintItem,
                                Start start, BlockPos end, Vec3 endHit, int placeCount, int skipCount) {
        RoadPaintBlock block = (RoadPaintBlock) paintItem.getBlock();
        LinkedHashMap<BlockPos, Vec3> tiles = start.curve ? curveTiles(start, end) : straightTiles(start, end, block);
        Direction facing = start.facing;
        int angle = -1;
        AngledPaint.Normal normal = null;
        if (block.getAngledPaint() != null) {
            double dx = end.getX() - start.support.getX();
            double dz = end.getZ() - start.support.getZ();
            double length = Math.hypot(dx, dz);
            double nx = -dz / length;
            double nz = dx / length;
            double best = -1;
            for (Direction candidate : Direction.Plane.HORIZONTAL) {
                for (int a = 0; a <= AngledPaint.MAX_ANGLE; a++) {
                    AngledPaint.Normal test = AngledPaint.worldNormal(a, candidate);
                    double match = Math.abs(test.x() * nx + test.z() * nz);
                    if (match > best) {
                        best = match;
                        facing = candidate;
                        angle = a;
                        normal = test;
                    }
                }
            }
        } else if (!start.curve) {
            int dx = end.getX() - start.support.getX();
            int dz = end.getZ() - start.support.getZ();
            facing = Math.abs(dx) > Math.abs(dz) ? Direction.EAST : Direction.NORTH;
        }
        int placed = 0;
        int index = 0;
        int cycle = Math.clamp(placeCount, 1, MAX_PATTERN) + Math.clamp(skipCount, 0, MAX_PATTERN);
        for (Map.Entry<BlockPos, Vec3> entry : tiles.entrySet()) {
            if (index++ % cycle >= placeCount) continue;
            ItemStack supply = findPaint(player, start.paintItem);
            if (supply.isEmpty()) break;
            BlockPos support = entry.getKey();
            BlockPos target = support.above();
            if (!level.isLoaded(target) || !level.getBlockState(target).canBeReplaced()
                    || !level.mayInteract(player, target) || !player.mayUseItemAt(target, Direction.UP, supply)) continue;
            Vec3 sample = support.equals(start.support)
                    ? new Vec3(start.hitX, 0, start.hitZ)
                    : support.equals(end) ? endHit : entry.getValue();
            double height = surfaceHeight(level, support, sample);
            if (height < 0 || height > 1) continue;
            BlockState state = block.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, facing);
            if (block instanceof LegacyRoadPaintBlock) {
                state = state.setValue(LegacyRoadPaintBlock.HEIGHT,
                        Math.clamp((int) Math.ceil(height * 16 - 1.0e-6), 1, 16));
            }
            if (!state.canSurvive(level, target) || !level.setBlock(target, state, Block.UPDATE_ALL)) continue;
            double offset = normal == null ? 0 : normal.x() * (start.centerX - target.getX() - 0.5)
                    + normal.z() * (start.centerZ - target.getZ() - 0.5);
            CurvePaint.Parameters curve = start.curve ? new CurvePaint.Parameters(
                    start.radius, start.radiusCenterX, start.radiusCenterZ) : null;
            if (level.getBlockEntity(target) instanceof RoadPaintBlockEntity entity) {
                entity.setPlacement(height, offset, angle, curve);
            }
            if (!player.getAbilities().instabuild) supply.shrink(1);
            placed++;
        }
        return placed;
    }

    private static LinkedHashMap<BlockPos, Vec3> straightTiles(Start start, BlockPos end, RoadPaintBlock block) {
        double dx = end.getX() - start.support.getX();
        double dz = end.getZ() - start.support.getZ();
        // Static line textures only join cleanly along a cardinal axis.
        if (block.getAngledPaint() == null) {
            if (Math.abs(dx) >= Math.abs(dz)) dz = 0;
            else dx = 0;
        }
        int steps = Math.min(MAX_TILES * 16, (int) Math.ceil(Math.hypot(dx, dz) * 16));
        LinkedHashMap<BlockPos, Vec3> result = new LinkedHashMap<>();
        for (int i = 0; i <= steps; i++) {
            double t = steps == 0 ? 0 : i / (double) steps;
            addTile(result, start.support.getY(), start.centerX + dx * t, start.centerZ + dz * t);
            if (result.size() >= MAX_TILES) break;
        }
        return result;
    }

    private static LinkedHashMap<BlockPos, Vec3> curveTiles(Start start, BlockPos end) {
        double first = Math.atan2(start.centerZ - start.radiusCenterZ, start.centerX - start.radiusCenterX);
        double last = Math.atan2(end.getZ() + 0.5 - start.radiusCenterZ,
                end.getX() + 0.5 - start.radiusCenterX);
        double sweep = Math.atan2(Math.sin(last - first), Math.cos(last - first));
        int steps = Math.min(MAX_TILES * 16, (int) Math.ceil(Math.abs(sweep) * start.radius * 16));
        LinkedHashMap<BlockPos, Vec3> result = new LinkedHashMap<>();
        for (int i = 0; i <= steps; i++) {
            double angle = first + sweep * (steps == 0 ? 0 : i / (double) steps);
            addTile(result, start.support.getY(), start.radiusCenterX + Math.cos(angle) * start.radius,
                    start.radiusCenterZ + Math.sin(angle) * start.radius);
            if (result.size() >= MAX_TILES) break;
        }
        return result;
    }

    private static void addTile(LinkedHashMap<BlockPos, Vec3> tiles, int y, double x, double z) {
        BlockPos pos = new BlockPos((int) Math.floor(x), y, (int) Math.floor(z));
        tiles.putIfAbsent(pos, new Vec3(x, y + 1, z));
    }

    private static double surfaceHeight(Level level, BlockPos pos, Vec3 sample) {
        VoxelShape shape = level.getBlockState(pos).getCollisionShape(level, pos);
        if (shape.isEmpty()) return -1;
        double x = sample.x - pos.getX();
        double z = sample.z - pos.getZ();
        double top = -1;
        for (AABB box : shape.toAabbs()) {
            if (x >= box.minX - 1.0e-6 && x <= box.maxX + 1.0e-6
                    && z >= box.minZ - 1.0e-6 && z <= box.maxZ + 1.0e-6) top = Math.max(top, box.maxY);
        }
        return top;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide) ApplicatorSettingsScreen.open(player, hand);
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.roadoverlays.applicator_pattern",
                stack.getOrDefault(RoadOverlays.APPLICATOR_PLACE.get(), 3),
                stack.getOrDefault(RoadOverlays.APPLICATOR_SKIP.get(), 0)));
        tooltip.add(Component.translatable("tooltip.roadoverlays.applicator_use"));
    }
}
