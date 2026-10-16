package com.roadoverlays.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.roadoverlays.block.RoadPaintBlock;
import com.roadoverlays.block.AngledPaint;
import com.roadoverlays.block.entity.RoadPaintBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import com.mojang.math.Axis;
import net.minecraft.resources.ResourceLocation;

public class RoadPaintRenderer implements BlockEntityRenderer<RoadPaintBlockEntity> {
    private static final double SURFACE_OFFSET = 1.0 / 1024.0;
    private static final ResourceLocation WHITE_PIXEL =
            ResourceLocation.fromNamespaceAndPath("roadoverlays", "block/white_road_line");

    @Override
    public void render(RoadPaintBlockEntity paint, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffers, int packedLight, int packedOverlay) {
        RoadPaintBlock block = (RoadPaintBlock) paint.getBlockState().getBlock();
        int color = block.getPaintColor().getTextureDiffuseColor();
        int red = color >> 16 & 0xFF;
        int green = color >> 8 & 0xFF;
        int blue = color & 0xFF;

        poseStack.pushPose();
        poseStack.translate(0.5, paint.getSurfaceHeight() - 1 + SURFACE_OFFSET, 0.5);
        VertexConsumer vertices = buffers.getBuffer(RenderType.entityCutout(TextureAtlas.LOCATION_BLOCKS));
        if (block.getCurvePaint() != null) {
            TextureAtlasSprite whitePixel = Minecraft.getInstance()
                    .getTextureAtlas(TextureAtlas.LOCATION_BLOCKS).apply(WHITE_PIXEL);
            CurvePaintRenderer.render(paint, block.getCurvePaint(), poseStack.last(),
                    vertices, whitePixel, red, green, blue, packedLight, packedOverlay);
            poseStack.popPose();
            return;
        }
        AngledPaint angled = block.getAngledPaint();
        if (angled != null) {
            TextureAtlasSprite whitePixel = Minecraft.getInstance()
                    .getTextureAtlas(TextureAtlas.LOCATION_BLOCKS).apply(WHITE_PIXEL);
            AdjustableLineRenderer.render(paint, angled, poseStack.last(), vertices,
                    whitePixel, red, green, blue, packedLight, packedOverlay);
            poseStack.popPose();
            return;
        }
        TextureAtlasSprite sprite = Minecraft.getInstance()
                .getTextureAtlas(TextureAtlas.LOCATION_BLOCKS).apply(block.getPaintTexture());
        Direction facing = paint.getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING);
        int rotation = switch (facing) {
            case EAST -> 270;
            case SOUTH -> 180;
            case WEST -> 90;
            default -> 0;
        };
        // Block placement faces toward the player. Turn readable artwork so its top
        // points away from the player, while keeping line markings aligned as before.
        if (block.hasUprightArtwork()) rotation += 180;
        poseStack.mulPose(Axis.YP.rotationDegrees(rotation));

        PoseStack.Pose pose = poseStack.last();
        vertex(vertices, pose, -0.5f, -0.5f, sprite.getU0(), sprite.getV0(), red, green, blue, packedLight, packedOverlay);
        vertex(vertices, pose, -0.5f, 0.5f, sprite.getU0(), sprite.getV1(), red, green, blue, packedLight, packedOverlay);
        vertex(vertices, pose, 0.5f, 0.5f, sprite.getU1(), sprite.getV1(), red, green, blue, packedLight, packedOverlay);
        vertex(vertices, pose, 0.5f, -0.5f, sprite.getU1(), sprite.getV0(), red, green, blue, packedLight, packedOverlay);
        poseStack.popPose();
    }

    private static void vertex(VertexConsumer vertices, PoseStack.Pose pose, float x, float z,
                               float u, float v, int red, int green, int blue, int light, int overlay) {
        vertices.addVertex(pose, x, 0, z)
                .setColor(red, green, blue, 255)
                .setUv(u, v)
                .setOverlay(overlay)
                .setLight(light)
                .setNormal(pose, 0, 1, 0);
    }

    @Override
    public AABB getRenderBoundingBox(RoadPaintBlockEntity paint) {
        BlockPos pos = paint.getBlockPos();
        return new AABB(pos.getX(), pos.getY() - 1, pos.getZ(),
                pos.getX() + 1, pos.getY() + 1, pos.getZ() + 1);
    }
}
