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

/** Geometry for connected lines whose angle is stored on each placed piece. */
public final class AngledPaint {
    public static final int DEFAULT_ANGLE = 90; // Half degrees: 45°.
    public static final int MAX_ANGLE = 180; // Half degrees: 90°.
    private static final Pattern NAME = Pattern.compile("angled_line_(single|double|thick|double_thick)");

    public record Normal(double x, double z) {
    }

    public record Placement(int angleHalfDegrees, double offset) {
    }

    private final LineStyle style;

    private AngledPaint(LineStyle style) {
        this.style = style;
    }

    public static AngledPaint fromTexture(String texture) {
        Matcher match = NAME.matcher(texture);
        if (!match.matches()) return null;
        return new AngledPaint(LineStyle.valueOf(match.group(1).toUpperCase(Locale.ROOT)));
    }

    public LineStyle style() {
        return style;
    }

    public static Normal worldNormal(int angleHalfDegrees, Direction facing) {
        double radians = Math.toRadians(angleHalfDegrees / 2.0);
        double x = Math.cos(radians);
        double z = -Math.sin(radians);
        return switch (facing) {
            case EAST -> new Normal(-z, x);
            case SOUTH -> new Normal(-x, -z);
            case WEST -> new Normal(z, -x);
            default -> new Normal(x, z);
        };
    }

    public Placement connectedPlacement(BlockPlaceContext context, BlockState state,
                                        double surfaceHeight, int selectedAngle) {
        int angle = Math.clamp(selectedAngle, 0, MAX_ANGLE);
        BlockPos pos = context.getClickedPos();
        Normal normal = worldNormal(angle, state.getValue(BlockStateProperties.HORIZONTAL_FACING));
        double click = normal.x * (context.getClickLocation().x - pos.getX() - 0.5)
                + normal.z * (context.getClickLocation().z - pos.getZ() - 0.5);
        double bestOffset = 0;
        double bestDistance = Double.POSITIVE_INFINITY;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (dx == 0 && dz == 0) continue;
                BlockPos neighborPos = pos.offset(dx, 0, dz);
                BlockState neighborState = context.getLevel().getBlockState(neighborPos);
                if (!(neighborState.getBlock() instanceof RoadPaintBlock neighborBlock)
                        || neighborBlock.getAngledPaint() == null
                        || style != neighborBlock.getAngledPaint().style
                        || !(context.getLevel().getBlockEntity(neighborPos) instanceof RoadPaintBlockEntity neighbor)
                        || Math.abs(neighbor.getSurfaceHeight() - surfaceHeight) > 1.0 / 1024) {
                    continue;
                }

                int neighborAngle = neighbor.getLineAngleHalfDegrees() >= 0
                        ? neighbor.getLineAngleHalfDegrees() : DEFAULT_ANGLE;
                Normal other = worldNormal(neighborAngle,
                        neighborState.getValue(BlockStateProperties.HORIZONTAL_FACING));
                double dot = normal.x * other.x + normal.z * other.z;
                if (Math.abs(dot) < 0.999999) continue;
                double offset = (dot < 0 ? -neighbor.getLineOffset() : neighbor.getLineOffset())
                        - normal.x * (pos.getX() - neighborPos.getX())
                        - normal.z * (pos.getZ() - neighborPos.getZ());
                if (!touchesSharedBoundary(offset, normal, dx, dz)) continue;
                double distance = style.halfSpacing() == 0 ? Math.abs(click - offset)
                        : Math.min(Math.abs(click - offset - style.halfSpacing()),
                        Math.abs(click - offset + style.halfSpacing()));
                if (distance < bestDistance) {
                    bestDistance = distance;
                    bestOffset = offset;
                }
            }
        }
        return new Placement(angle, bestOffset);
    }

    private boolean touchesSharedBoundary(double offset, Normal normal, int dx, int dz) {
        double edge = normal.x * dx * 0.5 + normal.z * dz * 0.5;
        double extent = dx == 0 ? Math.abs(normal.x) * 0.5
                : dz == 0 ? Math.abs(normal.z) * 0.5 : 0;
        if (Math.abs(offset - edge) <= extent + style.halfWidth() + 1.0e-6) return true;
        return style.halfSpacing() > 0 && (
                Math.abs(offset + style.halfSpacing() - edge) <= extent + style.halfWidth() + 1.0e-6
                        || Math.abs(offset - style.halfSpacing() - edge) <= extent + style.halfWidth() + 1.0e-6);
    }
}
