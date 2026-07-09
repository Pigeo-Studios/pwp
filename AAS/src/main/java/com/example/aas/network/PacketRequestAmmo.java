/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.ChatFormatting
 *  net.minecraft.core.BlockPos
 *  net.minecraft.network.FriendlyByteBuf
 *  net.minecraft.network.chat.Component
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.server.level.ServerPlayer
 *  net.minecraft.world.item.Item
 *  net.minecraft.world.item.ItemStack
 *  net.minecraft.world.item.Items
 *  net.minecraft.world.level.ItemLike
 *  net.minecraft.world.level.block.entity.BlockEntity
 *  net.minecraftforge.network.NetworkEvent$Context
 *  net.minecraftforge.registries.ForgeRegistries
 */
package com.example.aas.network;

import com.example.aas.block.HubBlockEntity;
import com.example.aas.config.AASConfig;
import com.example.aas.item.AGSAmmoItem;
import com.example.aas.item.M2AmmoItem;
import com.example.aas.item.ModItems;
import com.example.aas.network.ResupplyHandler;
import com.example.aas.world.AASWorldData;
import java.util.function.Supplier;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.registries.ForgeRegistries;

public class PacketRequestAmmo {
    private final BlockPos pos;
    private final int type;

    public PacketRequestAmmo(BlockPos pos, int type) {
        this.pos = pos;
        this.type = type;
    }

    public static void encode(PacketRequestAmmo msg, FriendlyByteBuf buf) {
        buf.writeBlockPos(msg.pos);
        buf.writeInt(msg.type);
    }

    public static PacketRequestAmmo decode(FriendlyByteBuf buf) {
        return new PacketRequestAmmo(buf.readBlockPos(), buf.readInt());
    }

    public static void handle(PacketRequestAmmo msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ((NetworkEvent.Context)ctx.get()).getSender();
            if (player == null) {
                return;
            }
            if (player.distanceToSqr((double)msg.pos.getX(), (double)msg.pos.getY(), (double)msg.pos.getZ()) > 64.0) {
                return;
            }
            BlockEntity be = player.level().getBlockEntity(msg.pos);
            if (be instanceof HubBlockEntity) {
                HubBlockEntity hub = (HubBlockEntity)be;
                int cost = 0;
                int cooldownTime = 0;
                boolean isOnCooldown = false;
                switch (msg.type) {
                    case 0: {
                        String targetKit;
                        String pendingKit = player.getPersistentData().getString("AAS_PendingKit");
                        String currentKitName = player.getPersistentData().getString("AAS_CurrentKit");
                        boolean hasPending = !pendingKit.isEmpty();
                        String string = targetKit = hasPending ? pendingKit : currentKitName;
                        if (targetKit.isEmpty() || targetKit.equals("Unassigned")) {
                            player.sendSystemMessage((Component)Component.literal((String)"No Kit equipped!").withStyle(ChatFormatting.RED));
                            return;
                        }
                        long lastFobUse = player.getPersistentData().getLong("AAS_LastFobResupply");
                        long currentTime = player.level().getGameTime();
                        if (!player.isCreative() && currentTime < lastFobUse + 1200L) {
                            long secondsLeft = (lastFobUse + 1200L - currentTime) / 20L;
                            player.sendSystemMessage((Component)Component.literal((String)("Resupply on cooldown: " + secondsLeft + "s")).withStyle(ChatFormatting.RED));
                            return;
                        }
                        cost = (Integer)AASConfig.HUB_RESUPPLY_COST.get();
                        if (!player.isCreative() && hub.getMaterials() < cost) {
                            player.sendSystemMessage((Component)Component.literal((String)("Not enough Materials! Need: " + cost)).withStyle(ChatFormatting.RED));
                            return;
                        }
                        AASWorldData data = AASWorldData.get(player.serverLevel());
                        if (hasPending) {
                            ResupplyHandler.tryApplyPendingKit(player, data);
                            if (!player.isCreative()) {
                                hub.consumeMaterials(cost);
                                player.getPersistentData().putLong("AAS_LastFobResupply", currentTime);
                            }
                            player.level().sendBlockUpdated(msg.pos, hub.getBlockState(), hub.getBlockState(), 3);
                            player.sendSystemMessage((Component)Component.literal((String)("New Kit Equipped! (-" + cost + " Mats)")).withStyle(ChatFormatting.GREEN));
                        } else {
                            AASWorldData.KitInfo kit;
                            String t = player.getTeam() != null ? player.getTeam().getName().toUpperCase() : "NEUTRAL";
                            AASWorldData.KitInfo kitInfo = kit = t.equals("BLUE") ? data.blueKits.get(currentKitName) : data.redKits.get(currentKitName);
                            if (kit != null) {
                                if (ResupplyHandler.resupplyPlayer(player, kit, false)) {
                                    if (!player.isCreative()) {
                                        hub.consumeMaterials(cost);
                                        player.getPersistentData().putLong("AAS_LastFobResupply", currentTime);
                                    }
                                    player.level().sendBlockUpdated(msg.pos, hub.getBlockState(), hub.getBlockState(), 3);
                                    player.sendSystemMessage((Component)Component.literal((String)("Kit Resupplied! (-" + cost + " Mats)")).withStyle(ChatFormatting.GREEN));
                                } else {
                                    player.sendSystemMessage((Component)Component.literal((String)"Ammo already full!").withStyle(ChatFormatting.YELLOW));
                                }
                            }
                        }
                        return;
                    }
                    case 1: {
                        cost = 15;
                        cooldownTime = 1200;
                        if (hub.cooldownAGS <= 0) break;
                        isOnCooldown = true;
                        break;
                    }
                    case 2: {
                        cost = 15;
                        cooldownTime = 1200;
                        if (hub.cooldownM2 <= 0) break;
                        isOnCooldown = true;
                        break;
                    }
                    case 3: {
                        cost = 20;
                        cooldownTime = 1200;
                        if (hub.cooldownMortar <= 0) break;
                        isOnCooldown = true;
                        break;
                    }
                    case 4: {
                        cost = 50;
                        cooldownTime = 2400;
                        if (hub.cooldownTOW <= 0) break;
                        isOnCooldown = true;
                    }
                }
                if (!player.isCreative() && isOnCooldown) {
                    player.sendSystemMessage((Component)Component.literal((String)"Supply on Cooldown!").withStyle(ChatFormatting.RED));
                    return;
                }
                if (!player.isCreative() && hub.getMaterials() < cost) {
                    player.sendSystemMessage((Component)Component.literal((String)("Not enough Construction Materials! Need: " + cost)).withStyle(ChatFormatting.RED));
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
                        hub.consumeMaterials(cost);
                        hub.setCooldown(msg.type, cooldownTime);
                    }
                    player.sendSystemMessage((Component)Component.literal((String)("Resupplied! (-" + cost + " Mats)")).withStyle(ChatFormatting.GREEN));
                    player.level().sendBlockUpdated(msg.pos, hub.getBlockState(), hub.getBlockState(), 3);
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }
}

