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
    static final public int MAX_PROGRESS = 2400;
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
        this.setChanged();
        if (this.level != null) {
            this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), 3);
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
        this.setChanged();
    }

    public int getMaterials() {
        return this.constructionMaterials;
    }

    public void consumeMaterials(int amount) {
        this.constructionMaterials = Math.max(0, this.constructionMaterials - amount);
        this.setChanged();
        if (this.level != null && !this.level.isClientSide) {
            this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), 3);
        }
    }

    public void addMaterials(int amount) {
        this.constructionMaterials += amount;
        if (this.constructionMaterials > 3000) {
            this.constructionMaterials = 3000;
        }
        this.setChanged();
        if (this.level != null && !this.level.isClientSide) {
            this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), 3);
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
        if (!level.isClientSide && needsSync) {
            level.sendBlockUpdated(pos, state, state, 3);
        }
        if (level.isClientSide) {
            if (((Boolean)state.getValue((Property)HubBlock.CONSTRUCTED)).booleanValue()) {
                entity.handleSoundClient();
            }
            return;
        }
        if (((Boolean)state.getValue((Property)HubBlock.CONSTRUCTED)).booleanValue()) {
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
                level.setBlock(pos, (BlockState)state.setValue((Property)HubBlock.CONSTRUCTED, (Comparable)Boolean.valueOf(true)), 3);
                if (!level.isClientSide) {
                    ((ServerLevel)level).sendParticles((ParticleOptions)ParticleTypes.CAMPFIRE_COSY_SMOKE, (double)pos.getX() + 0.5, (double)pos.getY() + 0.5, (double)pos.getZ() + 0.5, 50, 1.2, 0.5, 1.2, 0.05);
                    AASWorldData data = AASWorldData.get((ServerLevel)level);
                    for (AASWorldData.HubInfo h : data.hubs) {
                        if (!h.pos.equals((Object)pos)) continue;
                        h.constructed = true;
                        data.setDirty();
                        PacketHandler.sendToAllClients((ServerLevel)level, data);
                        break;
                    }
                }
            }
            if (level.getGameTime() % 5L == 0L || entity.currentProgress >= 2400) {
                level.sendBlockUpdated(pos, state, state, 3);
            }
        }
        entity.activeDiggers = 0;
    }

    private void handleSoundClient() {
        DistExecutor.unsafeRunWhenOn((Dist)Dist.CLIENT, () -> () -> {
            this.clientSoundRef = ClientHooks.playHubSound(this, this.clientSoundRef);
        });
    }

    public void setRemoved() {
        if (this.level != null && this.level.isClientSide) {
            DistExecutor.unsafeRunWhenOn((Dist)Dist.CLIENT, () -> () -> ClientHooks.stopHubSound(this.clientSoundRef));
        }
        super.setRemoved();
    }

    public float getPercentage() {
        return (float)this.currentProgress / 2400.0f;
    }

    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt("BuildProgress", this.currentProgress);
        tag.putString("TeamOwner", this.teamOwner);
        tag.putInt("Materials", this.constructionMaterials);
        tag.putInt("CooldownAGS", this.cooldownAGS);
        tag.putInt("CooldownM2", this.cooldownM2);
        tag.putInt("CooldownMortar", this.cooldownMortar);
        tag.putInt("CooldownTOW", this.cooldownTOW);
    }

    public void load(CompoundTag tag) {
        super.load(tag);
        this.currentProgress = tag.getInt("BuildProgress");
        if (tag.contains("TeamOwner")) {
            this.teamOwner = tag.getString("TeamOwner");
        }
        if (tag.contains("Materials")) {
            this.constructionMaterials = tag.getInt("Materials");
        }
        if (tag.contains("CooldownAGS")) {
            this.cooldownAGS = tag.getInt("CooldownAGS");
        }
        if (tag.contains("CooldownM2")) {
            this.cooldownM2 = tag.getInt("CooldownM2");
        }
        if (tag.contains("CooldownMortar")) {
            this.cooldownMortar = tag.getInt("CooldownMortar");
        }
        if (tag.contains("CooldownTOW")) {
            this.cooldownTOW = tag.getInt("CooldownTOW");
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

