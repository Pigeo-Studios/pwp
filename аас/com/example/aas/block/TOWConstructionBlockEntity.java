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

import com.example.aas.block.ModBlocks;
import com.example.aas.block.TOWConstructionBlock;
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

public class TOWConstructionBlockEntity
extends BlockEntity {
    public static final int MAX_PROGRESS = 2400;
    private int currentProgress = 0;
    private int activeDiggers = 0;
    private String teamOwner = "NEUTRAL";

    public TOWConstructionBlockEntity(BlockPos pos, BlockState state) {
        super((BlockEntityType)ModBlocks.TOW_CONSTRUCTION_BE.get(), pos, state);
    }

    public void setTeam(String team) {
        this.teamOwner = team;
        this.m_6596_();
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
            this.m_6596_();
        }
    }

    public void addCreativeProgress(int amount) {
        this.currentProgress += amount;
        if (this.currentProgress >= 2400) {
            this.currentProgress = 2400;
        }
        this.m_6596_();
    }

    public static void tick(Level level, BlockPos pos, BlockState state, TOWConstructionBlockEntity entity) {
        if (level.f_46443_) {
            return;
        }
        if (entity.activeDiggers > 0 || entity.currentProgress > 0) {
            Block block;
            if (entity.activeDiggers > 0) {
                float speed = entity.activeDiggers >= 2 ? 2.0f : 1.0f;
                float multiplier = ((Double)AASConfig.DIGGING_SPEED_MULTIPLIER.get()).floatValue();
                entity.currentProgress += (int)Math.ceil(speed *= multiplier);
            }
            if (entity.currentProgress >= 2400 && (block = state.m_60734_()) instanceof TOWConstructionBlock) {
                TOWConstructionBlock block2 = (TOWConstructionBlock)block;
                ((ServerLevel)level).m_8767_((ParticleOptions)ParticleTypes.f_123777_, (double)pos.m_123341_() + 0.5, (double)pos.m_123342_() + 0.5, (double)pos.m_123343_() + 0.5, 30, 0.7, 0.4, 0.7, 0.05);
                block2.finishConstruction((ServerLevel)level, pos, state);
            }
            if (level.m_46467_() % 10L == 0L) {
                level.m_7260_(pos, state, state, 3);
            }
        }
        entity.activeDiggers = 0;
    }

    protected void m_183515_(CompoundTag tag) {
        super.m_183515_(tag);
        tag.m_128405_("BuildProgress", this.currentProgress);
        tag.m_128359_("TeamOwner", this.teamOwner);
    }

    public void m_142466_(CompoundTag tag) {
        super.m_142466_(tag);
        this.currentProgress = tag.m_128451_("BuildProgress");
        if (tag.m_128441_("TeamOwner")) {
            this.teamOwner = tag.m_128461_("TeamOwner");
        }
    }

    public Packet<ClientGamePacketListener> m_58483_() {
        return ClientboundBlockEntityDataPacket.m_195640_((BlockEntity)this);
    }

    public CompoundTag m_5995_() {
        CompoundTag tag = new CompoundTag();
        this.m_183515_(tag);
        return tag;
    }
}

