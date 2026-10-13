package com.roadoverlays.block;

import com.roadoverlays.block.entity.RoadPaintBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Locale;

/** Geometry shared by connected angled paint placement and rendering. */
public final class AngledPaint {
    private static final Pattern NAME = Pattern.compile(
            "angled_line_(single|double|thick|double_thick)_(22_5|30|45|60|67_5)");
    private static final Direction[] NEIGHBORS = {
            Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST
    };

    private final double normalX;
    private final double normalZ;
    private final LineStyle style;

    private AngledPaint(double degrees, String style) {
        double radians = Math.toRadians(degrees);
        normalX = Math.cos(radians);
        normalZ = -Math.sin(radians);
        this.style = LineStyle.valueOf(style.toUpperCase(Locale.ROOT));
    }

    public static AngledPaint fromTexture(String texture) {
        Matcher match = NAME.matcher(texture);
        return match.matches()
                ? new AngledPaint(Double.parseDouble(match.group(2).replace('_', '.')), match.group(1))
                : null;
    }

    public double normalX() {
        return normalX;
    }

    public double normalZ() {
        return normalZ;
    }

    public LineStyle style() {
        return style;
    }

    public double connectedOffset(BlockPlaceContext context, BlockState state, double surfaceHeight) {
        BlockPos pos = context.getClickedPos();
        Normal normal = worldNormal(state.getValue(BlockStateProperties.HORIZONTAL_FACING));
        double click = normal.x * (context.getClickLocation().x - pos.getX() - 0.5)
                + normal.z * (context.getClickLocation().z - pos.getZ() - 0.5);
        double bestOffset = 0;
        double bestDistance = Double.POSITIVE_INFINITY;
        for (Direction side : NEIGHBORS) {
            BlockPos neighborPos = pos.relative(side);
            BlockState neighborState = context.getLevel().getBlockState(neighborPos);
            if (!(neighborState.getBlock() instanceof RoadPaintBlock neighborBlock)
                    || neighborBlock.getAngledPaint() == null
                    || style != neighborBlock.getAngledPaint().style
                    || !(context.getLevel().getBlockEntity(neighborPos) instanceof RoadPaintBlockEntity neighbor)
                    || Math.abs(neighbor.getSurfaceHeight() - surfaceHeight) > 1.0 / 1024) {
                continue;
            }

            Normal other = neighborBlock.getAngledPaint()
                    .worldNormal(neighborState.getValue(BlockStateProperties.HORIZONTAL_FACING));
            double dot = normal.x * other.x + normal.z * other.z;
            if (Math.abs(dot) < 0.9999) continue;
            double offset = (dot < 0 ? -neighbor.getLineOffset() : neighbor.getLineOffset())
                    - normal.x * (pos.getX() - neighborPos.getX())
                    - normal.z * (pos.getZ() - neighborPos.getZ());
            if (!touchesSharedEdge(offset, normal, side)) continue;
            double distance = style.halfSpacing() == 0 ? Math.abs(click - offset)
                    : Math.min(Math.abs(click - offset - style.halfSpacing()),
                    Math.abs(click - offset + style.halfSpacing()));
            if (distance < bestDistance) {
                bestDistance = distance;
                bestOffset = offset;
            }
        }
        return bestOffset;
    }

    private Normal worldNormal(Direction facing) {
        return switch (facing) {
            case EAST -> new Normal(-normalZ, normalX);
            case SOUTH -> new Normal(-normalX, -normalZ);
            case WEST -> new Normal(normalZ, -normalX);
            default -> new Normal(normalX, normalZ);
        };
    }

    private boolean touchesSharedEdge(double offset, Normal normal, Direction side) {
        double edge = switch (side) {
            case EAST -> normal.x * 0.5;
            case WEST -> normal.x * -0.5;
            case SOUTH -> normal.z * 0.5;
            default -> normal.z * -0.5;
        };
        double extent = (side.getAxis() == Direction.Axis.X ? Math.abs(normal.z) : Math.abs(normal.x)) * 0.5;
        if (Math.abs(offset - edge) <= extent + style.halfWidth() + 1.0e-6) return true;
        return style.halfSpacing() > 0 && (
                Math.abs(offset + style.halfSpacing() - edge) <= extent + style.halfWidth() + 1.0e-6
                        || Math.abs(offset - style.halfSpacing() - edge) <= extent + style.halfWidth() + 1.0e-6);
    }

    private record Normal(double x, double z) {
    }
}
