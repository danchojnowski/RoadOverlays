package com.roadoverlays.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.roadoverlays.block.CurvePaint;
import com.roadoverlays.block.LineStyle;
import com.roadoverlays.block.entity.RoadPaintBlockEntity;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Draws the visible part of a shared circular stripe inside one paint block. */
final class CurvePaintRenderer {
    private static final double TWO_PI = Math.PI * 2;

    private record Point(double x, double z) {
    }

    private CurvePaintRenderer() {
    }

    static void render(RoadPaintBlockEntity paint, CurvePaint curve, PoseStack.Pose pose,
                       VertexConsumer vertices, TextureAtlasSprite whitePixel,
                       int red, int green, int blue, int light, int overlay) {
        double radius = paint.getCurveRadius();
        if (radius <= 0) return;
        BlockPos pos = paint.getBlockPos();
        double cx = paint.getCurveCenterX() - pos.getX() - 0.5;
        double cz = paint.getCurveCenterZ() - pos.getZ() - 0.5;
        LineStyle style = curve.style();
        double[] range = angleRange(cx, cz, radius, style);
        int segmentsPerCircle = (int) Math.ceil(TWO_PI
                * (radius + style.halfSpacing() + style.halfWidth()) * 32);
        double angleStep = TWO_PI / segmentsPerCircle;
        int firstSegment = (int) Math.floor(range[0] / angleStep);
        int lastSegment = (int) Math.ceil(range[1] / angleStep);
        float u = (whitePixel.getU0() + whitePixel.getU1()) * 0.5f;
        float v = (whitePixel.getV0() + whitePixel.getV1()) * 0.5f;
        if (style.halfSpacing() == 0) {
            stripe(cx, cz, radius, style.halfWidth(), angleStep, firstSegment, lastSegment,
                    pose, vertices, u, v, red, green, blue, light, overlay);
        } else {
            stripe(cx, cz, radius - style.halfSpacing(), style.halfWidth(), angleStep, firstSegment, lastSegment,
                    pose, vertices, u, v, red, green, blue, light, overlay);
            stripe(cx, cz, radius + style.halfSpacing(), style.halfWidth(), angleStep, firstSegment, lastSegment,
                    pose, vertices, u, v, red, green, blue, light, overlay);
        }
    }

    private static double[] angleRange(double cx, double cz, double radius, LineStyle style) {
        if (Math.abs(cx) <= 0.5 && Math.abs(cz) <= 0.5) return new double[]{0, TWO_PI};
        double[] angles = new double[4];
        int index = 0;
        for (double x : new double[]{-0.5, 0.5}) {
            for (double z : new double[]{-0.5, 0.5}) {
                double angle = Math.atan2(z - cz, x - cx);
                angles[index++] = angle < 0 ? angle + TWO_PI : angle;
            }
        }
        Arrays.sort(angles);
        double largestGap = -1;
        int afterGap = 0;
        for (int i = 0; i < angles.length; i++) {
            double next = i == angles.length - 1 ? angles[0] + TWO_PI : angles[i + 1];
            double gap = next - angles[i];
            if (gap > largestGap) {
                largestGap = gap;
                afterGap = (i + 1) % angles.length;
            }
        }
        double start = angles[afterGap];
        double end = start + TWO_PI - largestGap;
        double nearestX = Math.max(Math.abs(cx) - 0.5, 0);
        double nearestZ = Math.max(Math.abs(cz) - 0.5, 0);
        double nearestDistance = Math.hypot(nearestX, nearestZ);
        double reach = style.halfSpacing() + style.halfWidth();
        double margin = Math.asin(Math.min(1, reach / Math.max(0.001, nearestDistance))) + 0.02;
        return new double[]{start - margin, end + margin};
    }

    private static void stripe(double cx, double cz, double radius, double halfWidth,
                               double angleStep, int firstSegment, int lastSegment, PoseStack.Pose pose,
                               VertexConsumer vertices, float u, float v,
                               int red, int green, int blue, int light, int overlay) {
        double inner = Math.max(0, radius - halfWidth);
        double outer = radius + halfWidth;
        for (int i = firstSegment; i < lastSegment; i++) {
            double a = i * angleStep;
            double b = (i + 1) * angleStep;
            List<Point> polygon = List.of(
                    polar(cx, cz, inner, a), polar(cx, cz, inner, b),
                    polar(cx, cz, outer, b), polar(cx, cz, outer, a));
            polygon = clip(polygon, true, -0.5, false);
            polygon = clip(polygon, true, 0.5, true);
            polygon = clip(polygon, false, -0.5, false);
            polygon = clip(polygon, false, 0.5, true);
            if (polygon.size() == 4) {
                for (Point point : polygon) {
                    vertex(vertices, pose, point, u, v, red, green, blue, light, overlay);
                }
                continue;
            }
            for (int j = 1; j + 1 < polygon.size(); j++) {
                vertex(vertices, pose, polygon.get(0), u, v, red, green, blue, light, overlay);
                vertex(vertices, pose, polygon.get(j), u, v, red, green, blue, light, overlay);
                vertex(vertices, pose, polygon.get(j + 1), u, v, red, green, blue, light, overlay);
                // The cutout buffer uses quads, so make the second triangle degenerate.
                vertex(vertices, pose, polygon.get(j + 1), u, v, red, green, blue, light, overlay);
            }
        }
    }

    private static Point polar(double cx, double cz, double radius, double angle) {
        return new Point(cx + Math.cos(angle) * radius, cz + Math.sin(angle) * radius);
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
