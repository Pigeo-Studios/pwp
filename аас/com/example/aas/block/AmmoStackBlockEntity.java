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
            this.m_6596_();
            if (this.f_58857_ != null) {
                this.f_58857_.m_7260_(this.f_58858_, this.m_58900_(), this.m_58900_(), 3);
            }
        }
    }

    public int removeTopAmmoBox() {
        if (!this.ammoCounts.isEmpty()) {
            int amount = this.ammoCounts.remove(this.ammoCounts.size() - 1);
            this.m_6596_();
            if (this.f_58857_ != null) {
                this.f_58857_.m_7260_(this.f_58858_, this.m_58900_(), this.m_58900_(), 3);
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
        this.m_6596_();
    }

    protected void m_183515_(CompoundTag tag) {
        super.m_183515_(tag);
        tag.m_128385_("AmmoCounts", this.ammoCounts.stream().mapToInt(i -> i).toArray());
    }

    public void m_142466_(CompoundTag tag) {
        super.m_142466_(tag);
        this.ammoCounts.clear();
        if (tag.m_128441_("AmmoCounts")) {
            int[] loadedArray;
            for (int i : loadedArray = tag.m_128465_("AmmoCounts")) {
                this.ammoCounts.add(i);
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

