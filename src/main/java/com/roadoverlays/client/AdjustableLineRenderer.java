package com.roadoverlays.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.roadoverlays.block.AngledPaint;
import com.roadoverlays.block.LineStyle;
import com.roadoverlays.block.entity.RoadPaintBlockEntity;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

import java.util.ArrayList;
import java.util.List;

/** Clips a configurable straight stripe to its block without generating an angle texture. */
final class AdjustableLineRenderer {
    private record Point(double x, double z) {
    }

    private AdjustableLineRenderer() {
    }

    static void render(RoadPaintBlockEntity paint, AngledPaint angled, PoseStack.Pose pose,
                       VertexConsumer vertices, TextureAtlasSprite whitePixel,
                       int red, int green, int blue, int light, int overlay) {
        int angle = paint.getLineAngleHalfDegrees() >= 0
                ? paint.getLineAngleHalfDegrees() : AngledPaint.DEFAULT_ANGLE;
        Direction facing = paint.getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING);
        AngledPaint.Normal normal = AngledPaint.worldNormal(angle, facing);
        LineStyle style = angled.style();
        float u = (whitePixel.getU0() + whitePixel.getU1()) * 0.5f;
        float v = (whitePixel.getV0() + whitePixel.getV1()) * 0.5f;
        double offset = paint.getLineOffset();
        if (style.halfSpacing() == 0) {
            stripe(normal, offset, style.halfWidth(), pose, vertices, u, v,
                    red, green, blue, light, overlay);
        } else {
            stripe(normal, offset - style.halfSpacing(), style.halfWidth(), pose, vertices, u, v,
                    red, green, blue, light, overlay);
            stripe(normal, offset + style.halfSpacing(), style.halfWidth(), pose, vertices, u, v,
                    red, green, blue, light, overlay);
        }
    }

    private static void stripe(AngledPaint.Normal normal, double offset, double halfWidth,
                               PoseStack.Pose pose, VertexConsumer vertices, float u, float v,
                               int red, int green, int blue, int light, int overlay) {
        // The tangent is perpendicular to the stripe's normal. Two blocks cover the whole tile.
        double tx = -normal.z();
        double tz = normal.x();
        List<Point> polygon = List.of(
                point(normal, tx, tz, offset - halfWidth, -2),
                point(normal, tx, tz, offset - halfWidth, 2),
                point(normal, tx, tz, offset + halfWidth, 2),
                point(normal, tx, tz, offset + halfWidth, -2));
        polygon = clip(polygon, true, -0.5, false);
        polygon = clip(polygon, true, 0.5, true);
        polygon = clip(polygon, false, -0.5, false);
        polygon = clip(polygon, false, 0.5, true);
        if (polygon.size() == 4) {
            for (Point point : polygon) {
                vertex(vertices, pose, point, u, v, red, green, blue, light, overlay);
            }
            return;
        }
        for (int j = 1; j + 1 < polygon.size(); j++) {
            vertex(vertices, pose, polygon.get(0), u, v, red, green, blue, light, overlay);
            vertex(vertices, pose, polygon.get(j), u, v, red, green, blue, light, overlay);
            vertex(vertices, pose, polygon.get(j + 1), u, v, red, green, blue, light, overlay);
            vertex(vertices, pose, polygon.get(j + 1), u, v, red, green, blue, light, overlay);
        }
    }

    private static Point point(AngledPaint.Normal normal, double tx, double tz,
                               double offset, double along) {
        return new Point(normal.x() * offset + tx * along,
                normal.z() * offset + tz * along);
    }

    private static List<Point> clip(List<Point> input, boolean xAxis, double boundary, boolean keepLess) {
        if (input.isEmpty()) return input;
        List<Point> output = new ArrayList<>(input.size() + 2);
        Point previous = input.get(input.size() - 1);
        boolean previousInside = inside(previous, xAxis, boundary, keepLess);
        for (Point current : input) {
            boolean currentInside = inside(current, xAxis, boundary, keepLess);
            if (currentInside != previousInside) {
                double from = xAxis ? previous.x : previous.z;
                double to = xAxis ? current.x : current.z;
                double t = (boundary - from) / (to - from);
                output.add(new Point(previous.x + t * (current.x - previous.x),
                        previous.z + t * (current.z - previous.z)));
            }
            if (currentInside) output.add(current);
            previous = current;
            previousInside = currentInside;
        }
        return output;
    }

    private static boolean inside(Point point, boolean xAxis, double boundary, boolean keepLess) {
        double value = xAxis ? point.x : point.z;
        return keepLess ? value <= boundary : value >= boundary;
    }

    private static void vertex(VertexConsumer vertices, PoseStack.Pose pose, Point point,
                               float u, float v, int red, int green, int blue, int light, int overlay) {
        vertices.addVertex(pose, (float) point.x, 0, (float) point.z)
                .setColor(red, green, blue, 255)
                .setUv(u, v)
                .setOverlay(overlay)
                .setLight(light)
                .setNormal(pose, 0, 1, 0);
    }
}
