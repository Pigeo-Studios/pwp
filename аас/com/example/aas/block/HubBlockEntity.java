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
 *  net.minecraft.world.level.block.entity.BlockEntity
 *  net.minecraft.world.level.block.entity.BlockEntityType
 *  net.minecraft.world.level.block.state.BlockState
 *  net.minecraft.world.level.block.state.properties.Property
 *  net.minecraftforge.api.distmarker.Dist
 *  net.minecraftforge.fml.DistExecutor
 */
package com.example.aas.block;

import com.example.aas.block.HubBlock;
import com.example.aas.block.ModBlocks;
import com.example.aas.client.ClientHooks;
import com.example.aas.config.AASConfig;
import com.example.aas.network.PacketHandler;
import com.example.aas.world.AASWorldData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;

public class HubBlockEntity
extends BlockEntity {
    public static final int MAX_PROGRESS = 2400;
    private int currentProgress = 0;
    private int activeDiggers = 0;
    private String teamOwner = "NEUTRAL";
    private int constructionMaterials = 200;
    public int cooldownAGS = 0;
    public int cooldownM2 = 0;
    public int cooldownMortar = 0;
    public int cooldownTOW = 0;
    public boolean wasDismantled = false;
    private Object clientSoundRef = null;

    public HubBlockEntity(BlockPos pos, BlockState state) {
        super((BlockEntityType)ModBlocks.HUB_BE.get(), pos, state);
    }

    public void addProgress() {
        if (this.currentProgress < 2400) {
            ++this.activeDiggers;
        }
    }

    public void addCreativeProgress(int amount) {
        if (this.currentProgress < 2400) {
            this.currentProgress += amount;
            if (this.currentProgress >= 2400) {
                this.currentProgress = 2400;
            }
        }
    }

    public void setTeam(String team) {
        this.teamOwner = team;
        this.m_6596_();
        if (this.f_58857_ != null) {
            this.f_58857_.m_7260_(this.f_58858_, this.m_58900_(), this.m_58900_(), 3);
        }
    }

    public String getTeam() {
        return this.teamOwner;
    }

    public void setCooldown(int type, int ticks) {
        if (type == 1) {
            this.cooldownAGS = ticks;
        }
        if (type == 2) {
            this.cooldownM2 = ticks;
        }
        if (type == 3) {
            this.cooldownMortar = ticks;
        }
        if (type == 4) {
            this.cooldownTOW = ticks;
        }
        this.m_6596_();
    }

    public int getMaterials() {
        return this.constructionMaterials;
    }

    public void consumeMaterials(int amount) {
        this.constructionMaterials = Math.max(0, this.constructionMaterials - amount);
        this.m_6596_();
        if (this.f_58857_ != null && !this.f_58857_.f_46443_) {
            this.f_58857_.m_7260_(this.f_58858_, this.m_58900_(), this.m_58900_(), 3);
        }
    }

    public void addMaterials(int amount) {
        this.constructionMaterials += amount;
        if (this.constructionMaterials > 3000) {
            this.constructionMaterials = 3000;
        }
        this.m_6596_();
        if (this.f_58857_ != null && !this.f_58857_.f_46443_) {
            this.f_58857_.m_7260_(this.f_58858_, this.m_58900_(), this.m_58900_(), 3);
        }
    }

    public static void tick(Level level, BlockPos pos, BlockState state, HubBlockEntity entity) {
        boolean needsSync = false;
        if (entity.cooldownAGS > 0) {
            --entity.cooldownAGS;
            if (entity.cooldownAGS == 0) {
                needsSync = true;
            }
        }
        if (entity.cooldownM2 > 0) {
            --entity.cooldownM2;
            if (entity.cooldownM2 == 0) {
                needsSync = true;
            }
        }
        if (entity.cooldownMortar > 0) {
            --entity.cooldownMortar;
            if (entity.cooldownMortar == 0) {
                needsSync = true;
            }
        }
        if (entity.cooldownTOW > 0) {
            --entity.cooldownTOW;
            if (entity.cooldownTOW == 0) {
                needsSync = true;
            }
        }
        if (!level.f_46443_ && needsSync) {
            level.m_7260_(pos, state, state, 3);
        }
        if (level.f_46443_) {
            if (((Boolean)state.m_61143_((Property)HubBlock.CONSTRUCTED)).booleanValue()) {
                entity.handleSoundClient();
            }
            return;
        }
        if (((Boolean)state.m_61143_((Property)HubBlock.CONSTRUCTED)).booleanValue()) {
            return;
        }
        if (entity.activeDiggers > 0 || entity.currentProgress > 0) {
            if (entity.activeDiggers > 0) {
                float speed = entity.activeDiggers == 1 ? 1.0f : (entity.activeDiggers == 2 ? 1.34f : (entity.activeDiggers == 3 ? 2.0f : 4.0f));
                float multiplier = ((Double)AASConfig.DIGGING_SPEED_MULTIPLIER.get()).floatValue();
                entity.currentProgress += (int)Math.ceil(speed *= multiplier);
            }
            if (entity.currentProgress >= 2400) {
                entity.currentProgress = 2400;
                level.m_7731_(pos, (BlockState)state.m_61124_((Property)HubBlock.CONSTRUCTED, (Comparable)Boolean.valueOf(true)), 3);
                if (!level.f_46443_) {
                    ((ServerLevel)level).m_8767_((ParticleOptions)ParticleTypes.f_123777_, (double)pos.m_123341_() + 0.5, (double)pos.m_123342_() + 0.5, (double)pos.m_123343_() + 0.5, 50, 1.2, 0.5, 1.2, 0.05);
                    AASWorldData data = AASWorldData.get((ServerLevel)level);
                    for (AASWorldData.HubInfo h : data.hubs) {
                        if (!h.pos.equals((Object)pos)) continue;
                        h.constructed = true;
                        data.m_77762_();
                        PacketHandler.sendToAllClients((ServerLevel)level, data);
                        break;
                    }
                }
            }
            if (level.m_46467_() % 5L == 0L || entity.currentProgress >= 2400) {
                level.m_7260_(pos, state, state, 3);
            }
        }
        entity.activeDiggers = 0;
    }

    private void handleSoundClient() {
        DistExecutor.unsafeRunWhenOn((Dist)Dist.CLIENT, () -> () -> {
            this.clientSoundRef = ClientHooks.playHubSound(this, this.clientSoundRef);
        });
    }

    public void m_7651_() {
        if (this.f_58857_ != null && this.f_58857_.f_46443_) {
            DistExecutor.unsafeRunWhenOn((Dist)Dist.CLIENT, () -> () -> ClientHooks.stopHubSound(this.clientSoundRef));
        }
        super.m_7651_();
    }

    public float getPercentage() {
        return (float)this.currentProgress / 2400.0f;
    }

    protected void m_183515_(CompoundTag tag) {
        super.m_183515_(tag);
        tag.m_128405_("BuildProgress", this.currentProgress);
        tag.m_128359_("TeamOwner", this.teamOwner);
        tag.m_128405_("Materials", this.constructionMaterials);
        tag.m_128405_("CooldownAGS", this.cooldownAGS);
        tag.m_128405_("CooldownM2", this.cooldownM2);
        tag.m_128405_("CooldownMortar", this.cooldownMortar);
        tag.m_128405_("CooldownTOW", this.cooldownTOW);
    }

    public void m_142466_(CompoundTag tag) {
        super.m_142466_(tag);
        this.currentProgress = tag.m_128451_("BuildProgress");
        if (tag.m_128441_("TeamOwner")) {
            this.teamOwner = tag.m_128461_("TeamOwner");
        }
        if (tag.m_128441_("Materials")) {
            this.constructionMaterials = tag.m_128451_("Materials");
        }
        if (tag.m_128441_("CooldownAGS")) {
            this.cooldownAGS = tag.m_128451_("CooldownAGS");
        }
        if (tag.m_128441_("CooldownM2")) {
            this.cooldownM2 = tag.m_128451_("CooldownM2");
        }
        if (tag.m_128441_("CooldownMortar")) {
            this.cooldownMortar = tag.m_128451_("CooldownMortar");
        }
        if (tag.m_128441_("CooldownTOW")) {
            this.cooldownTOW = tag.m_128451_("CooldownTOW");
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

