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
    static final public int MAX_PROGRESS = 1200;
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
        this.setChanged();
    }

    public void setLinkedWires(List<BlockPos> links) {
        this.linkedWires = new ArrayList<BlockPos>(links);
        this.isMultiWire = true;
        this.setChanged();
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
        if (this.isMultiWire && !this.linkedWires.isEmpty() && this.level != null) {
            this.isUpdatingLinked = true;
            for (BlockPos linkPos : this.linkedWires) {
                BlockEntity be;
                if (linkPos.equals((Object)this.worldPosition) || !this.level.isLoaded(linkPos) || !((be = this.level.getBlockEntity(linkPos)) instanceof BarbedWireBlockEntity)) continue;
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
            this.setChanged();
        }
    }

    private void performCreativeAdd(int amount) {
        this.currentProgress += amount;
        if (this.currentProgress >= 1200) {
            this.currentProgress = 1200;
        }
        this.setChanged();
    }

    public static void tick(Level level, BlockPos pos, BlockState state, BarbedWireBlockEntity entity) {
        if (level.isClientSide) {
            return;
        }
        if (((Boolean)state.getValue((Property)BarbedWireBlock.CONSTRUCTED)).booleanValue()) {
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
                level.setBlock(pos, (BlockState)state.setValue((Property)BarbedWireBlock.CONSTRUCTED, (Comparable)Boolean.valueOf(true)), 3);
                if (!level.isClientSide) {
                    ((ServerLevel)level).sendParticles((ParticleOptions)ParticleTypes.CAMPFIRE_COSY_SMOKE, (double)pos.getX() + 0.5, (double)pos.getY() + 0.5, (double)pos.getZ() + 0.5, 15, 0.5, 0.3, 0.5, 0.03);
                }
            }
            if (level.getGameTime() % 5L == 0L || entity.currentProgress >= 1200) {
                level.sendBlockUpdated(pos, state, state, 3);
            }
        }
        entity.activeDiggers = 0;
    }

    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt("BuildProgress", this.currentProgress);
        tag.putString("TeamOwner", this.teamOwner);
        tag.putBoolean("IsMultiWire", this.isMultiWire);
        ListTag list = new ListTag();
        for (BlockPos p : this.linkedWires) {
            list.add((Object)LongTag.valueOf((long)p.asLong()));
        }
        tag.put("LinkedWires", (Tag)list);
    }

    public void load(CompoundTag tag) {
        super.load(tag);
        this.currentProgress = tag.getInt("BuildProgress");
        if (tag.contains("TeamOwner")) {
            this.teamOwner = tag.getString("TeamOwner");
        }
        if (tag.contains("IsMultiWire")) {
            this.isMultiWire = tag.getBoolean("IsMultiWire");
        }
        this.linkedWires.clear();
        if (tag.contains("LinkedWires")) {
            ListTag list = tag.getList("LinkedWires", 4);
            for (Tag t : list) {
                this.linkedWires.add(BlockPos.of((long)((LongTag)t).getAsLong()));
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

