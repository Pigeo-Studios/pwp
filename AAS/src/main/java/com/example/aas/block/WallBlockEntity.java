/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.particles.ParticleOptions
 *  net.minecraft.core.particles.ParticleTypes
 *  net.minecraft.nbt.CompoundTag
 *  net.minecraft.nbt.ListTag
 *  net.minecraft.nbt.LongTag
 *  net.minecraft.nbt.Tag
 *  net.minecraft.network.protocol.Packet
 *  net.minecraft.network.protocol.game.ClientGamePacketListener
 *  net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.level.block.entity.BlockEntity
 *  net.minecraft.world.level.block.entity.BlockEntityType
 *  net.minecraft.world.level.block.state.BlockState
 *  net.minecraft.world.level.block.state.properties.Property
 */
package com.example.aas.block;

import com.example.aas.block.ModBlocks;
import com.example.aas.block.WallBlock;
import com.example.aas.config.AASConfig;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;

public class WallBlockEntity
extends BlockEntity {
    private int currentProgress = 0;
    private int activeDiggers = 0;
    private String teamOwner = "NEUTRAL";
    private List<BlockPos> linkedWalls = new ArrayList<BlockPos>();
    private boolean isMultiWall = false;
    private boolean isUpdatingLinked = false;

    public WallBlockEntity(BlockPos pos, BlockState state) {
        super((BlockEntityType)ModBlocks.WALL_BE.get(), pos, state);
    }

    public int getMaxProgress() {
        int size = this.linkedWalls.size();
        if (size > 5) {
            return 1200;
        }
        if (size > 0 || this.isMultiWall) {
            return 600;
        }
        return 300;
    }

    public void setTeam(String team) {
        this.teamOwner = team;
        this.setChanged();
    }

    public void setLinkedWalls(List<BlockPos> links) {
        this.linkedWalls = new ArrayList<BlockPos>(links);
        this.isMultiWall = true;
        this.setChanged();
    }

    public String getTeam() {
        return this.teamOwner;
    }

    public float getPercentage() {
        return (float)this.currentProgress / (float)this.getMaxProgress();
    }

    public void addProgress() {
        if (this.isUpdatingLinked) {
            return;
        }
        this.performAddProgress();
        this.propagateToLinks(false, 0);
    }

    public void addCreativeProgress(int amount) {
        if (this.isUpdatingLinked) {
            return;
        }
        this.performCreativeAdd(amount);
        this.propagateToLinks(true, amount);
    }

    private void propagateToLinks(boolean isCreative, int amount) {
        if (this.isMultiWall && !this.linkedWalls.isEmpty() && this.level != null) {
            this.isUpdatingLinked = true;
            for (BlockPos linkPos : this.linkedWalls) {
                BlockEntity be;
                if (linkPos.equals((Object)this.worldPosition) || !this.level.isLoaded(linkPos) || !((be = this.level.getBlockEntity(linkPos)) instanceof WallBlockEntity)) continue;
                WallBlockEntity linkedWall = (WallBlockEntity)be;
                if (isCreative) {
                    linkedWall.performCreativeAdd(amount);
                    continue;
                }
                linkedWall.performAddProgress();
            }
            this.isUpdatingLinked = false;
        }
    }

    private void performAddProgress() {
        if (this.currentProgress < this.getMaxProgress()) {
            ++this.activeDiggers;
            this.setChanged();
        }
    }

    private void performCreativeAdd(int amount) {
        if (this.currentProgress < this.getMaxProgress()) {
            this.currentProgress += amount;
            if (this.currentProgress >= this.getMaxProgress()) {
                this.currentProgress = this.getMaxProgress();
            }
            this.setChanged();
        }
    }

    public static void tick(Level level, BlockPos pos, BlockState state, WallBlockEntity entity) {
        if (level.isClientSide) {
            return;
        }
        if (((Boolean)state.getValue((Property)WallBlock.CONSTRUCTED)).booleanValue()) {
            return;
        }
        if (entity.activeDiggers > 0 || entity.currentProgress > 0) {
            int max;
            if (entity.activeDiggers > 0) {
                float speed = entity.activeDiggers == 1 ? 1.0f : (entity.activeDiggers == 2 ? 1.34f : (entity.activeDiggers == 3 ? 2.0f : 4.0f));
                float multiplier = ((Double)AASConfig.DIGGING_SPEED_MULTIPLIER.get()).floatValue();
                entity.currentProgress += (int)Math.ceil(speed *= multiplier);
            }
            if (entity.currentProgress >= (max = entity.getMaxProgress())) {
                entity.currentProgress = max;
                level.setBlock(pos, (BlockState)state.setValue((Property)WallBlock.CONSTRUCTED, (Comparable)Boolean.valueOf(true)), 3);
                if (!level.isClientSide) {
                    ((ServerLevel)level).sendParticles((ParticleOptions)ParticleTypes.CAMPFIRE_COSY_SMOKE, (double)pos.getX() + 0.5, (double)pos.getY() + 0.5, (double)pos.getZ() + 0.5, 25, 0.6, 0.4, 0.6, 0.05);
                }
            }
            if (level.getGameTime() % 5L == 0L || entity.currentProgress >= max) {
                level.sendBlockUpdated(pos, state, state, 3);
            }
        }
        entity.activeDiggers = 0;
    }

    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt("BuildProgress", this.currentProgress);
        tag.putString("TeamOwner", this.teamOwner);
        tag.putBoolean("IsMultiWall", this.isMultiWall);
        ListTag list = new ListTag();
        for (BlockPos p : this.linkedWalls) {
            list.add((Object)LongTag.valueOf((long)p.asLong()));
        }
        tag.put("LinkedWalls", (Tag)list);
    }

    public void load(CompoundTag tag) {
        super.load(tag);
        this.currentProgress = tag.getInt("BuildProgress");
        if (tag.contains("TeamOwner")) {
            this.teamOwner = tag.getString("TeamOwner");
        }
        if (tag.contains("IsMultiWall")) {
            this.isMultiWall = tag.getBoolean("IsMultiWall");
        }
        this.linkedWalls.clear();
        if (tag.contains("LinkedWalls")) {
            ListTag list = tag.getList("LinkedWalls", 4);
            for (Tag t : list) {
                this.linkedWalls.add(BlockPos.of((long)((LongTag)t).getAsLong()));
            }
        }
    }

    public CompoundTag getUpdateTag() {
        CompoundTag tag = new CompoundTag();
        this.saveAdditional(tag);
        return tag;
    }

    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create((BlockEntity)this);
    }
}

