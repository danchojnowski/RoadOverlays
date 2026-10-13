package com.roadoverlays.block;

import com.roadoverlays.block.entity.RoadPaintBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** A circle is shared by all connected pieces of a curve, regardless of its radius. */
public final class CurvePaint {
    public static final int DEFAULT_RADIUS = 2;
    public static final int MAX_RADIUS = 100;
    private static final Pattern NAME = Pattern.compile("curved_line_(single|double|thick|double_thick)");

    public record Parameters(double radius, double centerX, double centerZ) {
    }

    private final LineStyle style;

    private CurvePaint(LineStyle style) {
        this.style = style;
    }

    public static CurvePaint fromTexture(String texture) {
        Matcher match = NAME.matcher(texture);
        return match.matches() ? new CurvePaint(LineStyle.valueOf(match.group(1).toUpperCase(Locale.ROOT))) : null;
    }

    public LineStyle style() {
        return style;
    }

    public Parameters parametersForPlacement(BlockPlaceContext context, BlockState state,
                                             double surfaceHeight, int selectedRadius) {
        BlockPos pos = context.getClickedPos();
        Parameters nearest = null;
        double nearestClick = Double.POSITIVE_INFINITY;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (dx == 0 && dz == 0) continue;
                BlockPos neighborPos = pos.offset(dx, 0, dz);
                BlockState neighborState = context.getLevel().getBlockState(neighborPos);
                if (!(neighborState.getBlock() instanceof RoadPaintBlock neighborBlock)
                        || neighborBlock.getCurvePaint() == null
                        || neighborBlock.getCurvePaint().style != style
                        || !(context.getLevel().getBlockEntity(neighborPos) instanceof RoadPaintBlockEntity neighbor)
                        || neighbor.getCurveRadius() <= 0
                        || Math.abs(neighbor.getSurfaceHeight() - surfaceHeight) > 1.0 / 1024) {
                    continue;
                }
                Parameters candidate = new Parameters(neighbor.getCurveRadius(),
                        neighbor.getCurveCenterX(), neighbor.getCurveCenterZ());
                if (!joinsAcrossBoundary(candidate, pos, dx, dz)
                        || !intersectsTile(candidate, pos)) continue;
                double clickRadius = Math.hypot(context.getClickLocation().x - candidate.centerX,
                        context.getClickLocation().z - candidate.centerZ);
                double distance = ringDistance(clickRadius, candidate.radius);
                if (distance < nearestClick) {
                    nearestClick = distance;
                    nearest = candidate;
                }
            }
        }
        if (nearest != null) return nearest;

        Direction facing = state.getValue(BlockStateProperties.HORIZONTAL_FACING);
        boolean left = context.getPlayer() != null && context.getPlayer().isShiftKeyDown();
        Direction centerSide = left ? facing.getCounterClockWise() : facing.getClockWise();
        int radius = Math.clamp(selectedRadius, 1, MAX_RADIUS);
        return new Parameters(radius,
                pos.getX() + 0.5 + centerSide.getStepX() * radius,
                pos.getZ() + 0.5 + centerSide.getStepZ() * radius);
    }

    private boolean intersectsTile(Parameters circle, BlockPos pos) {
        double minX = pos.getX();
        double maxX = minX + 1;
        double minZ = pos.getZ();
        double maxZ = minZ + 1;
        double closestX = Math.clamp(circle.centerX, minX, maxX);
        double closestZ = Math.clamp(circle.centerZ, minZ, maxZ);
        double minDistance = Math.hypot(circle.centerX - closestX, circle.centerZ - closestZ);
        double maxDistance = Math.max(Math.hypot(circle.centerX - minX, circle.centerZ - minZ),
                Math.max(Math.hypot(circle.centerX - minX, circle.centerZ - maxZ),
                        Math.max(Math.hypot(circle.centerX - maxX, circle.centerZ - minZ),
                                Math.hypot(circle.centerX - maxX, circle.centerZ - maxZ))));
        return ringOverlaps(circle.radius, minDistance, maxDistance);
    }

    private boolean joinsAcrossBoundary(Parameters circle, BlockPos pos, int dx, int dz) {
        double minDistance;
        double maxDistance;
        if (dx != 0 && dz != 0) {
            double x = pos.getX() + (dx > 0 ? 1 : 0);
            double z = pos.getZ() + (dz > 0 ? 1 : 0);
            minDistance = maxDistance = Math.hypot(circle.centerX - x, circle.centerZ - z);
        } else if (dx != 0) {
            double x = pos.getX() + (dx > 0 ? 1 : 0);
            double z0 = pos.getZ();
            double z1 = z0 + 1;
            minDistance = Math.hypot(circle.centerX - x,
                    circle.centerZ - Math.clamp(circle.centerZ, z0, z1));
            maxDistance = Math.max(Math.hypot(circle.centerX - x, circle.centerZ - z0),
                    Math.hypot(circle.centerX - x, circle.centerZ - z1));
        } else {
            double z = pos.getZ() + (dz > 0 ? 1 : 0);
            double x0 = pos.getX();
            double x1 = x0 + 1;
            minDistance = Math.hypot(circle.centerZ - z,
                    circle.centerX - Math.clamp(circle.centerX, x0, x1));
            maxDistance = Math.max(Math.hypot(circle.centerZ - z, circle.centerX - x0),
                    Math.hypot(circle.centerZ - z, circle.centerX - x1));
        }
        return ringOverlaps(circle.radius, minDistance, maxDistance);
    }

    private boolean ringOverlaps(double radius, double minDistance, double maxDistance) {
        if (stripeOverlaps(radius, minDistance, maxDistance)) return true;
        return style.halfSpacing() > 0 && (
                stripeOverlaps(radius - style.halfSpacing(), minDistance, maxDistance)
                        || stripeOverlaps(radius + style.halfSpacing(), minDistance, maxDistance));
    }

    private boolean stripeOverlaps(double radius, double minDistance, double maxDistance) {
        return radius + style.halfWidth() >= minDistance - 1.0e-6
                && radius - style.halfWidth() <= maxDistance + 1.0e-6;
    }

    private double ringDistance(double distance, double radius) {
        double result = Math.abs(distance - radius);
        if (style.halfSpacing() > 0) {
            result = Math.min(result, Math.abs(distance - radius - style.halfSpacing()));
            result = Math.min(result, Math.abs(distance - radius + style.halfSpacing()));
        }
        return result;
    }
}
