package com.roadoverlays.item;

import com.roadoverlays.RoadOverlays;
import com.roadoverlays.block.AngledPaint;
import com.roadoverlays.block.CurvePaint;
import com.roadoverlays.block.LegacyRoadPaintBlock;
import com.roadoverlays.block.LineStyle;
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

import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Two-click placement of a continuous or dashed road marking using carried paint. */
public final class PaintApplicatorItem extends Item {
    public static final int MAX_PATTERN = 64;
    private static final int MAX_TILES = 1024;
    private static final Map<UUID, Start> STARTS = new java.util.HashMap<>();

    private record Start(BlockPos support, String dimension, Item paintItem,
                         double centerX, double centerZ, double hitX, double hitZ,
                         int radius, boolean curve, boolean turnRight) { }

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
            RoadPaintBlock block = (RoadPaintBlock) paintItem.getBlock();
            CurvePaint curve = block.getCurvePaint();
            int radius = curve == null ? 0 : Math.clamp(paintStack.getOrDefault(
                    RoadOverlays.CURVE_RADIUS.get(), CurvePaint.DEFAULT_RADIUS), 1, CurvePaint.MAX_RADIUS);
            boolean turnRight = !player.isShiftKeyDown();
            STARTS.put(player.getUUID(), new Start(support, dimension, paintStack.getItem(),
                    support.getX() + 0.5, support.getZ() + 0.5,
                    context.getClickLocation().x, context.getClickLocation().z,
                    radius, curve != null, turnRight));
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
        CurvePaint.Parameters circle = start.curve ? circleThroughEnds(start, support) : null;
        if (start.curve && circle == null) {
            player.displayClientMessage(Component.translatable("message.roadoverlays.applicator_radius"), true);
            return InteractionResult.SUCCESS;
        }
        int placed = layPaint(level, player, paintItem, start, support, context.getClickLocation(), circle,
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
                                Start start, BlockPos end, Vec3 endHit, CurvePaint.Parameters circle,
                                int placeCount, int skipCount) {
        RoadPaintBlock block = (RoadPaintBlock) paintItem.getBlock();
        Direction facing = Direction.NORTH;
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
        LinkedHashMap<BlockPos, Vec3> tiles = start.curve
                ? curveTiles(start, end, circle, block.getCurvePaint().style())
                : straightTiles(start, end, normal,
                block.getAngledPaint() == null ? null : block.getAngledPaint().style());
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
            if (level.getBlockEntity(target) instanceof RoadPaintBlockEntity entity) {
                entity.setPlacement(height, offset, angle, circle);
            }
            if (!player.getAbilities().instabuild) supply.shrink(1);
            placed++;
        }
        return placed;
    }

    private static LinkedHashMap<BlockPos, Vec3> straightTiles(Start start, BlockPos end,
                                                                AngledPaint.Normal normal, LineStyle style) {
        double dx = end.getX() - start.support.getX();
        double dz = end.getZ() - start.support.getZ();
        if (normal != null) {
            // Follow the rendered half-degree angle so a long run stays on its painted centerline.
            double tx = -normal.z();
            double tz = normal.x();
            double distance = dx * tx + dz * tz;
            dx = tx * distance;
            dz = tz * distance;
        } else {
            // Static line textures only join cleanly along a cardinal axis.
            if (Math.abs(dx) >= Math.abs(dz)) dz = 0;
            else dx = 0;
        }
        int steps = Math.min(MAX_TILES * 16, (int) Math.ceil(Math.hypot(dx, dz) * 16));
        LinkedHashMap<BlockPos, Vec3> result = new LinkedHashMap<>();
        Set<BlockPos> checked = normal == null ? null : new HashSet<>();
        double distance = normal == null ? 0 : dx * -normal.z() + dz * normal.x();
        for (int i = 0; i <= steps; i++) {
            double t = steps == 0 ? 0 : i / (double) steps;
            double x = start.centerX + dx * t;
            double z = start.centerZ + dz * t;
            if (normal == null) {
                addTile(result, start.support.getY(), x, z);
            } else {
                int tileX = (int) Math.floor(x);
                int tileZ = (int) Math.floor(z);
                for (int ox = -1; ox <= 1; ox++) {
                    for (int oz = -1; oz <= 1; oz++) {
                        BlockPos pos = new BlockPos(tileX + ox, start.support.getY(), tileZ + oz);
                        if (!checked.add(pos) || !lineIntersectsTile(start, normal, style, distance, pos)) continue;
                        double sampleX = Math.clamp(x, pos.getX() + 1.0e-4, pos.getX() + 1 - 1.0e-4);
                        double sampleZ = Math.clamp(z, pos.getZ() + 1.0e-4, pos.getZ() + 1 - 1.0e-4);
                        result.put(pos, new Vec3(sampleX, pos.getY() + 1, sampleZ));
                    }
                }
            }
            if (result.size() >= MAX_TILES) break;
        }
        return result;
    }

    private static boolean lineIntersectsTile(Start start, AngledPaint.Normal normal,
                                              LineStyle style, double distance, BlockPos pos) {
        double nx = normal.x();
        double nz = normal.z();
        double tx = -nz;
        double tz = nx;
        double minAcross = Double.POSITIVE_INFINITY;
        double maxAcross = Double.NEGATIVE_INFINITY;
        double minAlong = Double.POSITIVE_INFINITY;
        double maxAlong = Double.NEGATIVE_INFINITY;
        for (int x = pos.getX(); x <= pos.getX() + 1; x++) {
            for (int z = pos.getZ(); z <= pos.getZ() + 1; z++) {
                double across = nx * (x - start.centerX) + nz * (z - start.centerZ);
                double along = tx * (x - start.centerX) + tz * (z - start.centerZ);
                minAcross = Math.min(minAcross, across);
                maxAcross = Math.max(maxAcross, across);
                minAlong = Math.min(minAlong, along);
                maxAlong = Math.max(maxAlong, along);
            }
        }
        if (maxAlong < Math.min(0, distance) - 1.0e-6
                || minAlong > Math.max(0, distance) + 1.0e-6) return false;
        if (style.halfSpacing() == 0) {
            return stripeIntersectsTile(start, normal, pos, distance,
                    0, style.halfWidth(), minAcross, maxAcross);
        }
        return stripeIntersectsTile(start, normal, pos, distance,
                -style.halfSpacing(), style.halfWidth(), minAcross, maxAcross)
                || stripeIntersectsTile(start, normal, pos, distance,
                style.halfSpacing(), style.halfWidth(), minAcross, maxAcross);
    }

    private static boolean stripeIntersectsTile(Start start, AngledPaint.Normal normal, BlockPos pos,
                                                double distance, double offset, double halfWidth,
                                                double minimum, double maximum) {
        if (offset + halfWidth < minimum - 1.0e-6
                || offset - halfWidth > maximum + 1.0e-6) return false;
        double tx = -normal.z();
        double tz = normal.x();
        double firstX = start.centerX + normal.x() * offset;
        double firstZ = start.centerZ + normal.z() * offset;
        double lastX = firstX + tx * distance;
        double lastZ = firstZ + tz * distance;
        double reachX = Math.abs(normal.x()) * halfWidth;
        double reachZ = Math.abs(normal.z()) * halfWidth;
        return pos.getX() + 1 >= Math.min(firstX, lastX) - reachX - 1.0e-6
                && pos.getX() <= Math.max(firstX, lastX) + reachX + 1.0e-6
                && pos.getZ() + 1 >= Math.min(firstZ, lastZ) - reachZ - 1.0e-6
                && pos.getZ() <= Math.max(firstZ, lastZ) + reachZ + 1.0e-6;
    }

    private static CurvePaint.Parameters circleThroughEnds(Start start, BlockPos end) {
        double dx = end.getX() + 0.5 - start.centerX;
        double dz = end.getZ() + 0.5 - start.centerZ;
        double distance = Math.hypot(dx, dz);
        if (distance < 1.0e-6 || distance > start.radius * 2.0) return null;
        double half = distance / 2.0;
        double side = Math.sqrt(Math.max(0, start.radius * start.radius - half * half));
        double sign = start.turnRight ? 1 : -1;
        return new CurvePaint.Parameters(start.radius,
                start.centerX + dx / 2.0 - sign * dz / distance * side,
                start.centerZ + dz / 2.0 + sign * dx / distance * side);
    }

    private static LinkedHashMap<BlockPos, Vec3> curveTiles(Start start, BlockPos end,
                                                             CurvePaint.Parameters circle, LineStyle style) {
        double first = Math.atan2(start.centerZ - circle.centerZ(), start.centerX - circle.centerX());
        double last = Math.atan2(end.getZ() + 0.5 - circle.centerZ(),
                end.getX() + 0.5 - circle.centerX());
        double fullTurn = Math.PI * 2;
        double difference = last - first;
        // The selected side chooses one of the two circles through both clicked blocks.
        // Follow its arc from start to end, with a positive sweep for right bends.
        double sweep = start.turnRight
                ? ((difference % fullTurn) + fullTurn) % fullTurn
                : -(((-difference % fullTurn) + fullTurn) % fullTurn);
        int steps = Math.min(MAX_TILES * 16, (int) Math.ceil(Math.abs(sweep) * start.radius * 16));
        LinkedHashMap<BlockPos, Vec3> result = new LinkedHashMap<>();
        Set<BlockPos> checked = new HashSet<>();
        for (int i = 0; i <= steps; i++) {
            double angle = first + sweep * (steps == 0 ? 0 : i / (double) steps);
            double x = circle.centerX() + Math.cos(angle) * start.radius;
            double z = circle.centerZ() + Math.sin(angle) * start.radius;
            int tileX = (int) Math.floor(x);
            int tileZ = (int) Math.floor(z);
            // A double or thick stripe can enter a neighboring tile without its
            // centerline entering that tile. Check the swept paint area itself.
            for (int ox = -1; ox <= 1; ox++) {
                for (int oz = -1; oz <= 1; oz++) {
                    BlockPos pos = new BlockPos(tileX + ox, start.support.getY(), tileZ + oz);
                    if (!checked.add(pos) || !curveIntersectsTile(circle, style, first, sweep, pos)) continue;
                    double sampleX = Math.clamp(x, pos.getX() + 1.0e-4, pos.getX() + 1 - 1.0e-4);
                    double sampleZ = Math.clamp(z, pos.getZ() + 1.0e-4, pos.getZ() + 1 - 1.0e-4);
                    result.put(pos, new Vec3(sampleX, pos.getY() + 1, sampleZ));
                }
            }
            if (result.size() >= MAX_TILES) break;
        }
        return result;
    }

    private static boolean curveIntersectsTile(CurvePaint.Parameters circle, LineStyle style,
                                               double first, double sweep, BlockPos pos) {
        double cx = circle.centerX();
        double cz = circle.centerZ();
        double x0 = pos.getX();
        double z0 = pos.getZ();
        double x1 = x0 + 1;
        double z1 = z0 + 1;
        double nearestX = Math.clamp(cx, x0, x1);
        double nearestZ = Math.clamp(cz, z0, z1);
        double minimum = Math.hypot(cx - nearestX, cz - nearestZ);
        double maximum = Math.max(Math.max(Math.hypot(cx - x0, cz - z0), Math.hypot(cx - x0, cz - z1)),
                Math.max(Math.hypot(cx - x1, cz - z0), Math.hypot(cx - x1, cz - z1)));
        boolean radial = style.halfSpacing() == 0
                ? ringIntersectsTile(circle.radius(), style.halfWidth(), minimum, maximum)
                : ringIntersectsTile(circle.radius() - style.halfSpacing(), style.halfWidth(), minimum, maximum)
                || ringIntersectsTile(circle.radius() + style.halfSpacing(), style.halfWidth(), minimum, maximum);
        if (!radial) return false;
        if (cx >= x0 && cx <= x1 && cz >= z0 && cz <= z1) return true;

        double[] corners = {
                positiveAngle(Math.atan2(z0 - cz, x0 - cx)),
                positiveAngle(Math.atan2(z1 - cz, x0 - cx)),
                positiveAngle(Math.atan2(z0 - cz, x1 - cx)),
                positiveAngle(Math.atan2(z1 - cz, x1 - cx))
        };
        Arrays.sort(corners);
        double fullTurn = Math.PI * 2;
        double largestGap = -1;
        int afterGap = 0;
        for (int i = 0; i < corners.length; i++) {
            double next = i == corners.length - 1 ? corners[0] + fullTurn : corners[i + 1];
            double gap = next - corners[i];
            if (gap > largestGap) {
                largestGap = gap;
                afterGap = (i + 1) % corners.length;
            }
        }
        double tileStart = corners[afterGap];
        double tileEnd = tileStart + fullTurn - largestGap;
        double arcStart = positiveAngle(sweep >= 0 ? first : first + sweep);
        double arcEnd = arcStart + Math.abs(sweep);
        for (int turn = -1; turn <= 1; turn++) {
            double shifted = tileStart + turn * fullTurn;
            if (shifted <= arcEnd + 1.0e-6 && shifted + tileEnd - tileStart >= arcStart - 1.0e-6) return true;
        }
        return false;
    }

    private static boolean ringIntersectsTile(double radius, double halfWidth, double minimum, double maximum) {
        return radius + halfWidth >= minimum - 1.0e-6
                && radius - halfWidth <= maximum + 1.0e-6;
    }

    private static double positiveAngle(double angle) {
        double fullTurn = Math.PI * 2;
        return ((angle % fullTurn) + fullTurn) % fullTurn;
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
