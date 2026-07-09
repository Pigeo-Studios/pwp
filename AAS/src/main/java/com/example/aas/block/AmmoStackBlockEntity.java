/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.core.BlockPos
 *  net.minecraft.nbt.CompoundTag
 *  net.minecraft.network.protocol.Packet
 *  net.minecraft.network.protocol.game.ClientGamePacketListener
 *  net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket
 *  net.minecraft.world.level.block.entity.BlockEntity
 *  net.minecraft.world.level.block.entity.BlockEntityType
 *  net.minecraft.world.level.block.state.BlockState
 */
package com.example.aas.block;

import com.example.aas.block.ModBlocks;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class AmmoStackBlockEntity
extends BlockEntity {
    private final List<Integer> ammoCounts = new ArrayList<Integer>();

    public AmmoStackBlockEntity(BlockPos pos, BlockState state) {
        super((BlockEntityType)ModBlocks.AMMO_STACK_BE.get(), pos, state);
    }

    public void addAmmoBox(int amount) {
        if (this.ammoCounts.size() < 4) {
            this.ammoCounts.add(amount);
            this.setChanged();
            if (this.level != null) {
                this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), 3);
            }
        }
    }

    public int removeTopAmmoBox() {
        if (!this.ammoCounts.isEmpty()) {
            int amount = this.ammoCounts.remove(this.ammoCounts.size() - 1);
            this.setChanged();
            if (this.level != null) {
                this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), 3);
            }
            return amount;
        }
        return 0;
    }

    public List<Integer> getAmmoCounts() {
        return new ArrayList<Integer>(this.ammoCounts);
    }

    public void clear() {
        this.ammoCounts.clear();
        this.setChanged();
    }

    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putIntArray("AmmoCounts", this.ammoCounts.stream().mapToInt(i -> i).toArray());
    }

    public void load(CompoundTag tag) {
        super.load(tag);
        this.ammoCounts.clear();
        if (tag.contains("AmmoCounts")) {
            int[] loadedArray;
            for (int i : loadedArray = tag.getIntArray("AmmoCounts")) {
                this.ammoCounts.add(i);
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

