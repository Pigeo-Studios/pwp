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
    public static final int MAX_KIT_SLOTS = 49;
    public final Container kitInventory;
    public final String team;
    public final String kitName;
    public boolean isLeaderOnly;
    public int maxPerTeam;
    public int maxPerSquad;
    public int minSquadPlayers;
    public final boolean[] resupplyFlags;
    public final boolean[] saveNbtFlags;
    private static final ResourceLocation[] ARMOR_SLOT_TEXTURES = new ResourceLocation[]{InventoryMenu.f_39696_, InventoryMenu.f_39695_, InventoryMenu.f_39694_, InventoryMenu.f_39693_};

    public KitEditorMenu(int id, Inventory playerInv, FriendlyByteBuf data) {
        this(id, playerInv, (Container)new SimpleContainer(49), data.m_130277_(), data.m_130277_(), data.readBoolean(), data.readInt(), data.readInt(), data.readInt(), new boolean[49], new boolean[49]);
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
                this.m_38897_(new Slot(kitInv, 9 + col + row * 9, 8 + col * 18, 66 + row * 18));
            }
        }
        for (int col2 = 0; col2 < 9; ++col2) {
            this.m_38897_(new Slot(kitInv, col2, 8 + col2 * 18, 124));
        }
        for (i = 0; i < 4; ++i) {
            final int armorIndex = i;
            slotIndex = 36 + i;
            this.m_38897_(new Slot(kitInv, slotIndex, 8 + i * 18, 150){

                public Pair<ResourceLocation, ResourceLocation> m_7543_() {
                    return Pair.of((Object)InventoryMenu.f_39692_, (Object)ARMOR_SLOT_TEXTURES[armorIndex]);
                }
            });
        }
        this.m_38897_(new Slot(kitInv, 40, 84, 150){

            public Pair<ResourceLocation, ResourceLocation> m_7543_() {
                return Pair.of((Object)InventoryMenu.f_39692_, (Object)InventoryMenu.f_39697_);
            }
        });
        for (row = 0; row < 4; ++row) {
            for (col = 0; col < 2; ++col) {
                slotIndex = 41 + (row * 2 + col);
                this.m_38897_(new Slot(kitInv, slotIndex, -36 + col * 18, 66 + row * 18));
            }
        }
        for (i = 0; i < 3; ++i) {
            for (int j = 0; j < 9; ++j) {
                this.m_38897_(new Slot((Container)playerInv, j + i * 9 + 9, 8 + j * 18, 180 + i * 18));
            }
        }
        for (i = 0; i < 9; ++i) {
            this.m_38897_(new Slot((Container)playerInv, i, 8 + i * 18, 238));
        }
    }

    public boolean m_6875_(Player p) {
        return true;
    }

    public ItemStack m_7648_(Player p, int index) {
        return ItemStack.f_41583_;
    }
}

