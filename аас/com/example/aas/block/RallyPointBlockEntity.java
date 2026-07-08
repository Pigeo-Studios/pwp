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
        this.m_6596_();
    }

    public int getSquadId() {
        return this.squadId;
    }

    public void setExpiryTick(long tick) {
        this.expiryTick = tick;
        this.m_6596_();
    }

    public void cleanupData(ServerLevel level) {
        if (this.squadId == -1) {
            return;
        }
        AASWorldData data = AASWorldData.get(level);
        boolean changed = false;
        for (AASWorldData.Squad s : data.squads) {
            if (s.id != this.squadId) continue;
            if (s.rallyPos == null || !s.rallyPos.equals((Object)this.f_58858_)) break;
            s.rallyPos = null;
            s.rallyExpiryTick = -1L;
            changed = true;
            break;
        }
        if (changed) {
            data.m_77762_();
            PacketHandler.INSTANCE.send(PacketDistributor.ALL.noArg(), (Object)new PacketSyncSquads(data.squads));
        }
    }

    public void checkExpiry(Level level, BlockPos pos) {
        if (this.expiryTick == -1L) {
            return;
        }
        if (level.m_46467_() >= this.expiryTick) {
            this.isDecay = true;
            level.m_7471_(pos, false);
        }
    }

    public void handleSoundClient() {
        DistExecutor.unsafeRunWhenOn((Dist)Dist.CLIENT, () -> () -> {
            this.clientSoundRef = ClientHooks.playRallySound(this, this.clientSoundRef);
        });
    }

    public void m_7651_() {
        if (this.f_58857_ != null && this.f_58857_.f_46443_) {
            DistExecutor.unsafeRunWhenOn((Dist)Dist.CLIENT, () -> () -> ClientHooks.stopRallySound(this.clientSoundRef));
        }
        super.m_7651_();
    }

    protected void m_183515_(CompoundTag tag) {
        super.m_183515_(tag);
        tag.m_128405_("SquadID", this.squadId);
        tag.m_128379_("IsDecay", this.isDecay);
    }

    public void m_142466_(CompoundTag tag) {
        super.m_142466_(tag);
        this.squadId = tag.m_128451_("SquadID");
        this.isDecay = tag.m_128471_("IsDecay");
    }
}

