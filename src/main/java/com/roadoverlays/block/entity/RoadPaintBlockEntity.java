package com.roadoverlays.block.entity;

import com.roadoverlays.RoadOverlays;
import com.roadoverlays.block.RoadPaintBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class RoadPaintBlockEntity extends BlockEntity {
    private double surfaceHeight;

    public RoadPaintBlockEntity(BlockPos pos, BlockState state) {
        super(RoadOverlays.ROAD_PAINT_BLOCK_ENTITY.get(), pos, state);
        surfaceHeight = state.getValue(RoadPaintBlock.HEIGHT) / 16.0;
    }

    public double getSurfaceHeight() {
        return surfaceHeight;
    }

    public void setSurfaceHeight(double height) {
        surfaceHeight = Mth.clamp(height, 0, 1);
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("surface_height")) {
            surfaceHeight = Mth.clamp(tag.getDouble("surface_height"), 0, 1);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putDouble("surface_height", surfaceHeight);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        saveAdditional(tag, registries);
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
