/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.core.BlockPos
 *  net.minecraft.nbt.CompoundTag
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.level.block.entity.BlockEntity
 *  net.minecraft.world.level.block.entity.BlockEntityType
 *  net.minecraft.world.level.block.state.BlockState
 *  net.minecraftforge.api.distmarker.Dist
 *  net.minecraftforge.fml.DistExecutor
 *  net.minecraftforge.network.PacketDistributor
 */
package com.example.aas.block;

import com.example.aas.block.ModBlocks;
import com.example.aas.client.ClientHooks;
import com.example.aas.network.PacketHandler;
import com.example.aas.network.PacketSyncSquads;
import com.example.aas.world.AASWorldData;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.PacketDistributor;

public class RallyPointBlockEntity
extends BlockEntity {
    public boolean isDecay = false;
    public boolean wasDismantled = false;
    private int squadId = -1;
    private long expiryTick = -1L;
    private Object clientSoundRef = null;

    public RallyPointBlockEntity(BlockPos pos, BlockState state) {
        super((BlockEntityType)ModBlocks.RALLY_BE.get(), pos, state);
    }

    public void setSquadId(int id) {
        this.squadId = id;
        this.setChanged();
    }

    public int getSquadId() {
        return this.squadId;
    }

    public void setExpiryTick(long tick) {
        this.expiryTick = tick;
        this.setChanged();
    }

    public void cleanupData(ServerLevel level) {
        if (this.squadId == -1) {
            return;
        }
        AASWorldData data = AASWorldData.get(level);
        boolean changed = false;
        for (AASWorldData.Squad s : data.squads) {
            if (s.id != this.squadId) continue;
            if (s.rallyPos == null || !s.rallyPos.equals((Object)this.worldPosition)) break;
            s.rallyPos = null;
            s.rallyExpiryTick = -1L;
            changed = true;
            break;
        }
        if (changed) {
            data.setDirty();
            PacketHandler.INSTANCE.send(PacketDistributor.ALL.noArg(), (Object)new PacketSyncSquads(data.squads));
        }
    }

    public void checkExpiry(Level level, BlockPos pos) {
        if (this.expiryTick == -1L) {
            return;
        }
        if (level.getGameTime() >= this.expiryTick) {
            this.isDecay = true;
            level.removeBlock(pos, false);
        }
    }

    public void handleSoundClient() {
        DistExecutor.unsafeRunWhenOn((Dist)Dist.CLIENT, () -> () -> {
            this.clientSoundRef = ClientHooks.playRallySound(this, this.clientSoundRef);
        });
    }

    public void setRemoved() {
        if (this.level != null && this.level.isClientSide) {
            DistExecutor.unsafeRunWhenOn((Dist)Dist.CLIENT, () -> () -> ClientHooks.stopRallySound(this.clientSoundRef));
        }
        super.setRemoved();
    }

    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt("SquadID", this.squadId);
        tag.putBoolean("IsDecay", this.isDecay);
    }

    public void load(CompoundTag tag) {
        super.load(tag);
        this.squadId = tag.getInt("SquadID");
        this.isDecay = tag.getBoolean("IsDecay");
    }
}

