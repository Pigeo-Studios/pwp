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

import com.example.aas.block.BarbedWireBlock;
import com.example.aas.block.ModBlocks;
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

public class BarbedWireBlockEntity
extends BlockEntity {
    public static final int MAX_PROGRESS = 1200;
    private int currentProgress = 0;
    private int activeDiggers = 0;
    private String teamOwner = "NEUTRAL";
    private List<BlockPos> linkedWires = new ArrayList<BlockPos>();
    private boolean isMultiWire = false;
    private boolean isUpdatingLinked = false;

    public BarbedWireBlockEntity(BlockPos pos, BlockState state) {
        super((BlockEntityType)ModBlocks.WIRE_BE.get(), pos, state);
    }

    public void setTeam(String team) {
        this.teamOwner = team;
        this.m_6596_();
    }

    public void setLinkedWires(List<BlockPos> links) {
        this.linkedWires = new ArrayList<BlockPos>(links);
        this.isMultiWire = true;
        this.m_6596_();
    }

    public String getTeam() {
        return this.teamOwner;
    }

    public float getPercentage() {
        return (float)this.currentProgress / 1200.0f;
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
        if (this.isMultiWire && !this.linkedWires.isEmpty() && this.f_58857_ != null) {
            this.isUpdatingLinked = true;
            for (BlockPos linkPos : this.linkedWires) {
                BlockEntity be;
                if (linkPos.equals((Object)this.f_58858_) || !this.f_58857_.m_46749_(linkPos) || !((be = this.f_58857_.m_7702_(linkPos)) instanceof BarbedWireBlockEntity)) continue;
                BarbedWireBlockEntity linkedWire = (BarbedWireBlockEntity)be;
                if (isCreative) {
                    linkedWire.performCreativeAdd(amount);
                    continue;
                }
                linkedWire.performAddProgress();
            }
            this.isUpdatingLinked = false;
        }
    }

    private void performAddProgress() {
        if (this.currentProgress < 1200) {
            ++this.activeDiggers;
            this.m_6596_();
        }
    }

    private void performCreativeAdd(int amount) {
        this.currentProgress += amount;
        if (this.currentProgress >= 1200) {
            this.currentProgress = 1200;
        }
        this.m_6596_();
    }

    public static void tick(Level level, BlockPos pos, BlockState state, BarbedWireBlockEntity entity) {
        if (level.f_46443_) {
            return;
        }
        if (((Boolean)state.m_61143_((Property)BarbedWireBlock.CONSTRUCTED)).booleanValue()) {
            return;
        }
        if (entity.activeDiggers > 0 || entity.currentProgress > 0) {
            if (entity.activeDiggers > 0) {
                float speed = entity.activeDiggers >= 3 ? 2.0f : 1.0f;
                float multiplier = ((Double)AASConfig.DIGGING_SPEED_MULTIPLIER.get()).floatValue();
                entity.currentProgress += (int)Math.ceil(speed *= multiplier);
            }
            if (entity.currentProgress >= 1200) {
                entity.currentProgress = 1200;
                level.m_7731_(pos, (BlockState)state.m_61124_((Property)BarbedWireBlock.CONSTRUCTED, (Comparable)Boolean.valueOf(true)), 3);
                if (!level.f_46443_) {
                    ((ServerLevel)level).m_8767_((ParticleOptions)ParticleTypes.f_123777_, (double)pos.m_123341_() + 0.5, (double)pos.m_123342_() + 0.5, (double)pos.m_123343_() + 0.5, 15, 0.5, 0.3, 0.5, 0.03);
                }
            }
            if (level.m_46467_() % 5L == 0L || entity.currentProgress >= 1200) {
                level.m_7260_(pos, state, state, 3);
            }
        }
        entity.activeDiggers = 0;
    }

    protected void m_183515_(CompoundTag tag) {
        super.m_183515_(tag);
        tag.m_128405_("BuildProgress", this.currentProgress);
        tag.m_128359_("TeamOwner", this.teamOwner);
        tag.m_128379_("IsMultiWire", this.isMultiWire);
        ListTag list = new ListTag();
        for (BlockPos p : this.linkedWires) {
            list.add((Object)LongTag.m_128882_((long)p.m_121878_()));
        }
        tag.m_128365_("LinkedWires", (Tag)list);
    }

    public void m_142466_(CompoundTag tag) {
        super.m_142466_(tag);
        this.currentProgress = tag.m_128451_("BuildProgress");
        if (tag.m_128441_("TeamOwner")) {
            this.teamOwner = tag.m_128461_("TeamOwner");
        }
        if (tag.m_128441_("IsMultiWire")) {
            this.isMultiWire = tag.m_128471_("IsMultiWire");
        }
        this.linkedWires.clear();
        if (tag.m_128441_("LinkedWires")) {
            ListTag list = tag.m_128437_("LinkedWires", 4);
            for (Tag t : list) {
                this.linkedWires.add(BlockPos.m_122022_((long)((LongTag)t).m_7046_()));
            }
        }
    }

    public CompoundTag m_5995_() {
        CompoundTag tag = new CompoundTag();
        this.m_183515_(tag);
        return tag;
    }

    public Packet<ClientGamePacketListener> m_58483_() {
        return ClientboundBlockEntityDataPacket.m_195640_((BlockEntity)this);
    }
}

