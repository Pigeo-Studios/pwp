package com.pigeostudios.pwp.warfare.menu;

import com.mojang.datafixers.util.Pair;
import java.util.*;
import java.util.stream.Collectors;
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

public class KitEditorMenu extends AbstractContainerMenu {
   public static final int MAX_KIT_SLOTS = 49;
   public final Container kitInventory;
   public final String team;
   public final String kitName;
   public String category = "INFANTRY";
   public String description = "";
   public boolean isLeaderOnly;
   public int maxPerTeam;
   public int maxPerSquad;
   public int minSquadPlayers;
   public final boolean[] resupplyFlags;
   public final boolean[] saveNbtFlags;
   public final Map<Integer, List<String>> slotSkins;
   private static final ResourceLocation[] ARMOR_SLOT_TEXTURES = new ResourceLocation[]{
      InventoryMenu.EMPTY_ARMOR_SLOT_BOOTS, InventoryMenu.EMPTY_ARMOR_SLOT_LEGGINGS, InventoryMenu.EMPTY_ARMOR_SLOT_CHESTPLATE, InventoryMenu.EMPTY_ARMOR_SLOT_HELMET
   };

   public KitEditorMenu(int id, Inventory playerInv, FriendlyByteBuf data) {
      this(
         id,
         playerInv,
         new SimpleContainer(49),
         data.readUtf(),
         data.readUtf(),
         data.readUtf(),
         data.readUtf(),
         data.readBoolean(),
         data.readInt(),
         data.readInt(),
         data.readInt(),
         new boolean[49],
         new boolean[49],
         new HashMap<>()
      );

      for (int i = 0; i < 49; i++) {
         this.resupplyFlags[i] = data.readBoolean();
      }

      for (int i = 0; i < 49; i++) {
         this.saveNbtFlags[i] = data.readBoolean();
      }

      int skinCount = data.readInt();
      for (int i = 0; i < skinCount; i++) {
         int slot = data.readInt();
         int listSize = data.readInt();
         List<String> ids = new ArrayList<>();
         for (int j = 0; j < listSize; j++) ids.add(data.readUtf());
         this.slotSkins.put(slot, ids);
      }
   }

   public KitEditorMenu(
      int id, Inventory playerInv, Container kitInv, String t, String k, String cat, String desc, boolean l, int mt, int ms, int minPlayers,
      boolean[] flags, boolean[] nbtFlags, Map<Integer, List<String>> slotSkins
   ) {
      super((MenuType)ModMenuTypes.KIT_EDITOR_MENU.get(), id);
      this.kitInventory = kitInv;
      this.team = t;
      this.kitName = k;
      this.category = cat;
      this.description = desc;
      this.isLeaderOnly = l;
      this.maxPerTeam = mt;
      this.maxPerSquad = ms;
      this.minSquadPlayers = minPlayers;
      this.resupplyFlags = flags;
      this.saveNbtFlags = nbtFlags;
      this.slotSkins = slotSkins;

      for (int row = 0; row < 3; row++) {
         for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(kitInv, 9 + col + row * 9, 8 + col * 18, 66 + row * 18));
         }
      }

      for (int col = 0; col < 9; col++) {
         this.addSlot(new Slot(kitInv, col, 8 + col * 18, 124));
      }

      for (int i = 0; i < 4; i++) {
         final int armorIndex = i;
         int slotIndex = 36 + i;
         this.addSlot(new Slot(kitInv, slotIndex, 8 + i * 18, 150) {
            public Pair<ResourceLocation, ResourceLocation> getNoItemIcon() {
               return Pair.of(InventoryMenu.BLOCK_ATLAS, KitEditorMenu.ARMOR_SLOT_TEXTURES[armorIndex]);
            }
         });
      }

      this.addSlot(new Slot(kitInv, 40, 84, 150) {
         public Pair<ResourceLocation, ResourceLocation> getNoItemIcon() {
            return Pair.of(InventoryMenu.BLOCK_ATLAS, InventoryMenu.EMPTY_ARMOR_SLOT_SHIELD);
         }
      });

      for (int row = 0; row < 4; row++) {
         for (int col = 0; col < 2; col++) {
            int slotIndex = 41 + row * 2 + col;
            this.addSlot(new Slot(kitInv, slotIndex, -36 + col * 18, 66 + row * 18));
         }
      }

      for (int i = 0; i < 3; i++) {
         for (int j = 0; j < 9; j++) {
            this.addSlot(new Slot(playerInv, j + i * 9 + 9, 8 + j * 18, 180 + i * 18));
         }
      }

      for (int i = 0; i < 9; i++) {
         this.addSlot(new Slot(playerInv, i, 8 + i * 18, 238));
      }
   }

   public boolean stillValid(Player p) {
      return true;
   }

   public ItemStack quickMoveStack(Player p, int index) {
      return ItemStack.EMPTY;
   }
}
