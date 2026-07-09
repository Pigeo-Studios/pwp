/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.util.Pair
 *  net.minecraft.network.FriendlyByteBuf
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.world.Container
 *  net.minecraft.world.SimpleContainer
 *  net.minecraft.world.entity.player.Inventory
 *  net.minecraft.world.entity.player.Player
 *  net.minecraft.world.inventory.AbstractContainerMenu
 *  net.minecraft.world.inventory.InventoryMenu
 *  net.minecraft.world.inventory.MenuType
 *  net.minecraft.world.inventory.Slot
 *  net.minecraft.world.item.ItemStack
 */
package com.example.aas.menu;

import com.example.aas.menu.ModMenuTypes;
import com.mojang.datafixers.util.Pair;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class KitEditorMenu
extends AbstractContainerMenu {
    static final public int MAX_KIT_SLOTS = 49;
    final public Container kitInventory;
    final public String team;
    final public String kitName;
    public boolean isLeaderOnly;
    public int maxPerTeam;
    public int maxPerSquad;
    public int minSquadPlayers;
    final public boolean[] resupplyFlags;
    final public boolean[] saveNbtFlags;
    static private final ResourceLocation[] ARMOR_SLOT_TEXTURES = new ResourceLocation[]{InventoryMenu.EMPTY_ARMOR_SLOT_BOOTS, InventoryMenu.EMPTY_ARMOR_SLOT_LEGGINGS, InventoryMenu.EMPTY_ARMOR_SLOT_CHESTPLATE, InventoryMenu.EMPTY_ARMOR_SLOT_HELMET};

    public KitEditorMenu(int id, Inventory playerInv, FriendlyByteBuf data) {
        this(id, playerInv, (Container)new SimpleContainer(49), data.readUtf(), data.readUtf(), data.readBoolean(), data.readInt(), data.readInt(), data.readInt(), new boolean[49], new boolean[49]);
        int i;
        for (i = 0; i < 49; ++i) {
            this.resupplyFlags[i] = data.readBoolean();
        }
        for (i = 0; i < 49; ++i) {
            this.saveNbtFlags[i] = data.readBoolean();
        }
    }

    public KitEditorMenu(int id, Inventory playerInv, Container kitInv, String t, String k, boolean l, int mt, int ms, int minPlayers, boolean[] flags, boolean[] nbtFlags) {
        super((MenuType)ModMenuTypes.KIT_EDITOR_MENU.get(), id);
        int slotIndex;
        int i;
        int col;
        int row;
        this.kitInventory = kitInv;
        this.team = t;
        this.kitName = k;
        this.isLeaderOnly = l;
        this.maxPerTeam = mt;
        this.maxPerSquad = ms;
        this.minSquadPlayers = minPlayers;
        this.resupplyFlags = flags;
        this.saveNbtFlags = nbtFlags;
        for (row = 0; row < 3; ++row) {
            for (col = 0; col < 9; ++col) {
                this.addSlot(new Slot(kitInv, 9 + col + row * 9, 8 + col * 18, 66 + row * 18));
            }
        }
        for (int col2 = 0; col2 < 9; ++col2) {
            this.addSlot(new Slot(kitInv, col2, 8 + col2 * 18, 124));
        }
        for (i = 0; i < 4; ++i) {
            final int armorIndex = i;
            slotIndex = 36 + i;
            this.addSlot(new Slot(kitInv, slotIndex, 8 + i * 18, 150){

                public Pair<ResourceLocation, ResourceLocation> getNoItemIcon() {
                    return Pair.of((Object)InventoryMenu.BLOCK_ATLAS, (Object)ARMOR_SLOT_TEXTURES[armorIndex]);
                }
            });
        }
        this.addSlot(new Slot(kitInv, 40, 84, 150){

            public Pair<ResourceLocation, ResourceLocation> getNoItemIcon() {
                return Pair.of((Object)InventoryMenu.BLOCK_ATLAS, (Object)InventoryMenu.EMPTY_ARMOR_SLOT_SHIELD);
            }
        });
        for (row = 0; row < 4; ++row) {
            for (col = 0; col < 2; ++col) {
                slotIndex = 41 + (row * 2 + col);
                this.addSlot(new Slot(kitInv, slotIndex, -36 + col * 18, 66 + row * 18));
            }
        }
        for (i = 0; i < 3; ++i) {
            for (int j = 0; j < 9; ++j) {
                this.addSlot(new Slot((Container)playerInv, j + i * 9 + 9, 8 + j * 18, 180 + i * 18));
            }
        }
        for (i = 0; i < 9; ++i) {
            this.addSlot(new Slot((Container)playerInv, i, 8 + i * 18, 238));
        }
    }

    public boolean stillValid(Player p) {
        return true;
    }

    public ItemStack quickMoveStack(Player p, int index) {
        return ItemStack.EMPTY;
    }
}

