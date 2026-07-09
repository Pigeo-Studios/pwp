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
            Entity target = player.level().getEntity(msg.entityId);
            if (!(target instanceof SupplyCrateEntity)) {
                return;
            }
            SupplyCrateEntity crate = (SupplyCrateEntity)target;
            if (player.distanceToSqr((Entity)crate) > 64.0) {
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
                String kitName = player.getPersistentData().getString("AAS_CurrentKit");
                if (kitName.isEmpty()) {
                    player.sendSystemMessage((Component)Component.literal((String)"No Kit equipped!").withStyle(ChatFormatting.RED));
                    return;
                }
                long lastFobUse = player.getPersistentData().getLong("AAS_LastFobResupply");
                long currentTime = player.level().getGameTime();
                if (!player.isCreative() && currentTime < lastFobUse + 1200L) {
                    player.sendSystemMessage((Component)Component.literal((String)"Kit Resupply is on cooldown!").withStyle(ChatFormatting.RED));
                    return;
                }
                if (!player.isCreative() && crate.getMaterials() < cost) {
                    player.sendSystemMessage((Component)Component.literal((String)("Not enough Materials in Crate! Need: " + cost)).withStyle(ChatFormatting.RED));
                    return;
                }
                AASWorldData data = AASWorldData.get(player.serverLevel());
                String t = player.getTeam() != null ? player.getTeam().getName().toUpperCase() : "NEUTRAL";
                AASWorldData.KitInfo kitInfo = kit = t.equals("BLUE") ? data.blueKits.get(kitName) : data.redKits.get(kitName);
                if (kit != null) {
                    if (ResupplyHandler.resupplyPlayer(player, kit, false)) {
                        if (!player.isCreative()) {
                            crate.setMaterials(crate.getMaterials() - cost);
                            player.getPersistentData().putLong("AAS_LastFobResupply", currentTime);
                        }
                        player.sendSystemMessage((Component)Component.literal((String)("Kit Resupplied! (-" + cost + " Mats)")).withStyle(ChatFormatting.GREEN));
                    } else {
                        player.sendSystemMessage((Component)Component.literal((String)"Ammo already full!").withStyle(ChatFormatting.YELLOW));
                    }
                }
                return;
            }
            if (!player.isCreative() && crate.getMaterials() < cost) {
                player.sendSystemMessage((Component)Component.literal((String)("Not enough Materials in Crate! Need: " + cost)).withStyle(ChatFormatting.RED));
                return;
            }
            boolean success = false;
            if (msg.type == 1) {
                ItemStack stack = new ItemStack((ItemLike)ModItems.AGS_AMMO.get());
                AGSAmmoItem.setAmmo(stack, 30);
                if (player.getInventory().add(stack)) {
                    success = true;
                } else {
                    player.drop(stack, false);
                }
                success = true;
            } else if (msg.type == 2) {
                ItemStack stack = new ItemStack((ItemLike)ModItems.M2_AMMO.get());
                M2AmmoItem.setAmmo(stack, 200);
                if (player.getInventory().add(stack)) {
                    success = true;
                } else {
                    player.drop(stack, false);
                }
                success = true;
            } else if (msg.type == 3) {
                Item mortarItem = (Item)ForgeRegistries.ITEMS.getValue(new ResourceLocation("superbwarfare", "mortar_shell"));
                if (mortarItem == null || mortarItem == Items.AIR) {
                    mortarItem = Items.ARROW;
                }
                ItemStack stack = new ItemStack((ItemLike)mortarItem, 8);
                if (player.getInventory().add(stack)) {
                    success = true;
                } else {
                    player.drop(stack, false);
                }
                success = true;
            } else if (msg.type == 4) {
                Item towItem = (Item)ForgeRegistries.ITEMS.getValue(new ResourceLocation("superbwarfare", "medium_anti_ground_missile"));
                if (towItem == null || towItem == Items.AIR) {
                    towItem = Items.SPECTRAL_ARROW;
                }
                ItemStack stack = new ItemStack((ItemLike)towItem, 2);
                if (player.getInventory().add(stack)) {
                    success = true;
                } else {
                    player.drop(stack, false);
                }
                success = true;
            }
            if (success) {
                if (!player.isCreative()) {
                    crate.setMaterials(0);
                }
                player.sendSystemMessage((Component)Component.literal((String)"Heavy Ammo Resupplied! Crate consumed.").withStyle(ChatFormatting.GREEN));
            }
        });
        ctx.get().setPacketHandled(true);
    }
}

