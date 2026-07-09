/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.particles.ParticleOptions
 *  net.minecraft.core.particles.ParticleTypes
 *  net.minecraft.nbt.CompoundTag
 *  net.minecraft.network.protocol.Packet
 *  net.minecraft.network.protocol.game.ClientGamePacketListener
 *  net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.level.block.Block
 *  net.minecraft.world.level.block.entity.BlockEntity
 *  net.minecraft.world.level.block.entity.BlockEntityType
 *  net.minecraft.world.level.block.state.BlockState
 */
package com.example.aas.block;

import com.example.aas.block.AGSConstructionBlock;
import com.example.aas.block.ModBlocks;
import com.example.aas.config.AASConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class AGSConstructionBlockEntity
extends BlockEntity {
    static final public int MAX_PROGRESS = 2400;
    private int currentProgress = 0;
    private int activeDiggers = 0;
    private String teamOwner = "NEUTRAL";

    public AGSConstructionBlockEntity(BlockPos pos, BlockState state) {
        super((BlockEntityType)ModBlocks.AGS_CONSTRUCTION_BE.get(), pos, state);
    }

    public void setTeam(String team) {
        this.teamOwner = team;
        this.setChanged();
    }

    public String getTeam() {
        return this.teamOwner;
    }

    public float getPercentage() {
        return (float)this.currentProgress / 2400.0f;
    }

    public void addProgress() {
        if (this.currentProgress < 2400) {
            ++this.activeDiggers;
            this.setChanged();
        }
    }

    public void addCreativeProgress(int amount) {
        this.currentProgress += amount;
        if (this.currentProgress >= 2400) {
            this.currentProgress = 2400;
        }
        this.setChanged();
    }

    public static void tick(Level level, BlockPos pos, BlockState state, AGSConstructionBlockEntity entity) {
        if (level.isClientSide) {
            return;
        }
        if (entity.activeDiggers > 0 || entity.currentProgress > 0) {
            Block block;
            if (entity.activeDiggers > 0) {
                float speed = entity.activeDiggers >= 2 ? 2.0f : 1.0f;
                float multiplier = ((Double)AASConfig.DIGGING_SPEED_MULTIPLIER.get()).floatValue();
                entity.currentProgress += (int)Math.ceil(speed *= multiplier);
            }
            if (entity.currentProgress >= 2400 && (block = state.getBlock()) instanceof AGSConstructionBlock) {
                AGSConstructionBlock block2 = (AGSConstructionBlock)block;
                ((ServerLevel)level).sendParticles((ParticleOptions)ParticleTypes.CAMPFIRE_COSY_SMOKE, (double)pos.getX() + 0.5, (double)pos.getY() + 0.5, (double)pos.getZ() + 0.5, 25, 0.6, 0.4, 0.6, 0.05);
                block2.finishConstruction((ServerLevel)level, pos, state);
            }
            if (level.getGameTime() % 10L == 0L) {
                level.sendBlockUpdated(pos, state, state, 3);
            }
        }
        entity.activeDiggers = 0;
    }

    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt("BuildProgress", this.currentProgress);
        tag.putString("TeamOwner", this.teamOwner);
    }

    public void load(CompoundTag tag) {
        super.load(tag);
        this.currentProgress = tag.getInt("BuildProgress");
        if (tag.contains("TeamOwner")) {
            this.teamOwner = tag.getString("TeamOwner");
        }
    }

    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create((BlockEntity)this);
    }

    public CompoundTag getUpdateTag() {
        CompoundTag tag = new CompoundTag();
        this.saveAdditional(tag);
        return tag;
    }
}

