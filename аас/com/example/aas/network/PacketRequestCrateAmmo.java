/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.ChatFormatting
 *  net.minecraft.network.FriendlyByteBuf
 *  net.minecraft.network.chat.Component
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.server.level.ServerPlayer
 *  net.minecraft.world.entity.Entity
 *  net.minecraft.world.item.Item
 *  net.minecraft.world.item.ItemStack
 *  net.minecraft.world.item.Items
 *  net.minecraft.world.level.ItemLike
 *  net.minecraftforge.network.NetworkEvent$Context
 *  net.minecraftforge.registries.ForgeRegistries
 */
package com.example.aas.network;

import com.example.aas.config.AASConfig;
import com.example.aas.entity.SupplyCrateEntity;
import com.example.aas.item.AGSAmmoItem;
import com.example.aas.item.M2AmmoItem;
import com.example.aas.item.ModItems;
import com.example.aas.network.ResupplyHandler;
import com.example.aas.world.AASWorldData;
import java.util.function.Supplier;
import net.minecraft.ChatFormatting;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.registries.ForgeRegistries;

public class PacketRequestCrateAmmo {
    private final int entityId;
    private final int type;

    public PacketRequestCrateAmmo(int entityId, int type) {
        this.entityId = entityId;
        this.type = type;
    }

    public static void encode(PacketRequestCrateAmmo msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.entityId);
        buf.writeInt(msg.type);
    }

    public static PacketRequestCrateAmmo decode(FriendlyByteBuf buf) {
        return new PacketRequestCrateAmmo(buf.readInt(), buf.readInt());
    }

    public static void handle(PacketRequestCrateAmmo msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ((NetworkEvent.Context)ctx.get()).getSender();
            if (player == null) {
                return;
            }
            Entity target = player.m_9236_().m_6815_(msg.entityId);
            if (!(target instanceof SupplyCrateEntity)) {
                return;
            }
            SupplyCrateEntity crate = (SupplyCrateEntity)target;
            if (player.m_20280_((Entity)crate) > 64.0) {
                return;
            }
            int cost = 0;
            switch (msg.type) {
                case 0: {
                    cost = (Integer)AASConfig.HUB_RESUPPLY_COST.get();
                    break;
                }
                case 1: {
                    cost = 20;
                    break;
                }
                case 2: {
                    cost = 15;
                    break;
                }
                case 3: {
                    cost = 20;
                    break;
                }
                case 4: {
                    cost = 50;
                }
            }
            if (msg.type == 0) {
                AASWorldData.KitInfo kit;
                String kitName = player.getPersistentData().m_128461_("AAS_CurrentKit");
                if (kitName.isEmpty()) {
                    player.m_213846_((Component)Component.m_237113_((String)"No Kit equipped!").m_130940_(ChatFormatting.RED));
                    return;
                }
                long lastFobUse = player.getPersistentData().m_128454_("AAS_LastFobResupply");
                long currentTime = player.m_9236_().m_46467_();
                if (!player.m_7500_() && currentTime < lastFobUse + 1200L) {
                    player.m_213846_((Component)Component.m_237113_((String)"Kit Resupply is on cooldown!").m_130940_(ChatFormatting.RED));
                    return;
                }
                if (!player.m_7500_() && crate.getMaterials() < cost) {
                    player.m_213846_((Component)Component.m_237113_((String)("Not enough Materials in Crate! Need: " + cost)).m_130940_(ChatFormatting.RED));
                    return;
                }
                AASWorldData data = AASWorldData.get(player.m_284548_());
                String t = player.m_5647_() != null ? player.m_5647_().m_5758_().toUpperCase() : "NEUTRAL";
                AASWorldData.KitInfo kitInfo = kit = t.equals("BLUE") ? data.blueKits.get(kitName) : data.redKits.get(kitName);
                if (kit != null) {
                    if (ResupplyHandler.resupplyPlayer(player, kit, false)) {
                        if (!player.m_7500_()) {
                            crate.setMaterials(crate.getMaterials() - cost);
                            player.getPersistentData().m_128356_("AAS_LastFobResupply", currentTime);
                        }
                        player.m_213846_((Component)Component.m_237113_((String)("Kit Resupplied! (-" + cost + " Mats)")).m_130940_(ChatFormatting.GREEN));
                    } else {
                        player.m_213846_((Component)Component.m_237113_((String)"Ammo already full!").m_130940_(ChatFormatting.YELLOW));
                    }
                }
                return;
            }
            if (!player.m_7500_() && crate.getMaterials() < cost) {
                player.m_213846_((Component)Component.m_237113_((String)("Not enough Materials in Crate! Need: " + cost)).m_130940_(ChatFormatting.RED));
                return;
            }
            boolean success = false;
            if (msg.type == 1) {
                ItemStack stack = new ItemStack((ItemLike)ModItems.AGS_AMMO.get());
                AGSAmmoItem.setAmmo(stack, 30);
                if (player.m_150109_().m_36054_(stack)) {
                    success = true;
                } else {
                    player.m_36176_(stack, false);
                }
                success = true;
            } else if (msg.type == 2) {
                ItemStack stack = new ItemStack((ItemLike)ModItems.M2_AMMO.get());
                M2AmmoItem.setAmmo(stack, 200);
                if (player.m_150109_().m_36054_(stack)) {
                    success = true;
                } else {
                    player.m_36176_(stack, false);
                }
                success = true;
            } else if (msg.type == 3) {
                Item mortarItem = (Item)ForgeRegistries.ITEMS.getValue(new ResourceLocation("superbwarfare", "mortar_shell"));
                if (mortarItem == null || mortarItem == Items.f_41852_) {
                    mortarItem = Items.f_42412_;
                }
                ItemStack stack = new ItemStack((ItemLike)mortarItem, 8);
                if (player.m_150109_().m_36054_(stack)) {
                    success = true;
                } else {
                    player.m_36176_(stack, false);
                }
                success = true;
            } else if (msg.type == 4) {
                Item towItem = (Item)ForgeRegistries.ITEMS.getValue(new ResourceLocation("superbwarfare", "medium_anti_ground_missile"));
                if (towItem == null || towItem == Items.f_41852_) {
                    towItem = Items.f_42737_;
                }
                ItemStack stack = new ItemStack((ItemLike)towItem, 2);
                if (player.m_150109_().m_36054_(stack)) {
                    success = true;
                } else {
                    player.m_36176_(stack, false);
                }
                success = true;
            }
            if (success) {
                if (!player.m_7500_()) {
                    crate.setMaterials(0);
                }
                player.m_213846_((Component)Component.m_237113_((String)"Heavy Ammo Resupplied! Crate consumed.").m_130940_(ChatFormatting.GREEN));
            }
        });
        ctx.get().setPacketHandled(true);
    }
}

