/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.core.BlockPos
 *  net.minecraft.network.FriendlyByteBuf
 *  net.minecraft.world.Container
 *  net.minecraft.world.entity.player.Inventory
 *  net.minecraft.world.entity.player.Player
 *  net.minecraft.world.inventory.AbstractContainerMenu
 *  net.minecraft.world.inventory.ContainerLevelAccess
 *  net.minecraft.world.inventory.MenuType
 *  net.minecraft.world.inventory.Slot
 *  net.minecraft.world.item.ItemStack
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.level.block.Block
 *  net.minecraft.world.level.block.entity.BlockEntity
 *  net.minecraftforge.items.IItemHandler
 *  net.minecraftforge.items.SlotItemHandler
 */
package com.example.aas.menu;

import com.example.aas.block.ModBlocks;
import com.example.aas.block.VehicleSpawnerBlockEntity;
import com.example.aas.menu.ModMenuTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.SlotItemHandler;

public class VehicleSpawnerMenu
extends AbstractContainerMenu {
    public final VehicleSpawnerBlockEntity blockEntity;

    public VehicleSpawnerMenu(int id, Inventory inv, FriendlyByteBuf extraData) {
        this(id, inv, inv.f_35978_.m_9236_().m_7702_(extraData.m_130135_()));
    }

    public VehicleSpawnerMenu(int id, Inventory inv, BlockEntity entity) {
        super((MenuType)ModMenuTypes.VEHICLE_SPAWNER_MENU.get(), id);
        this.blockEntity = (VehicleSpawnerBlockEntity)entity;
        this.m_38897_((Slot)new SlotItemHandler((IItemHandler)this.blockEntity.inventory, 0, 15, 45));
        int gridStartX = 26;
        int gridStartY = 75;
        for (int row = 0; row < 4; ++row) {
            for (int col = 0; col < 8; ++col) {
                this.m_38897_((Slot)new SlotItemHandler((IItemHandler)this.blockEntity.inventory, 1 + col + row * 8, gridStartX + col * 18, gridStartY + row * 18));
            }
        }
        this.addPlayerInventory(inv, 160);
        this.addPlayerHotbar(inv, 218);
    }

    private void addPlayerInventory(Inventory playerInventory, int startY) {
        for (int i = 0; i < 3; ++i) {
            for (int l = 0; l < 9; ++l) {
                this.m_38897_(new Slot((Container)playerInventory, l + i * 9 + 9, 17 + l * 18, startY + i * 18));
            }
        }
    }

    private void addPlayerHotbar(Inventory playerInventory, int startY) {
        for (int i = 0; i < 9; ++i) {
            this.m_38897_(new Slot((Container)playerInventory, i, 17 + i * 18, startY));
        }
    }

    public ItemStack m_7648_(Player playerIn, int index) {
        return ItemStack.f_41583_;
    }

    public boolean m_6875_(Player player) {
        return VehicleSpawnerMenu.m_38889_((ContainerLevelAccess)ContainerLevelAccess.m_39289_((Level)this.blockEntity.m_58904_(), (BlockPos)this.blockEntity.m_58899_()), (Player)player, (Block)((Block)ModBlocks.VEHICLE_SPAWNER_BLOCK.get()));
    }
}

