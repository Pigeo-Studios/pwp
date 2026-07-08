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
        buf.m_130064_(msg.pos);
        buf.writeInt(msg.type);
    }

    public static PacketRequestAmmo decode(FriendlyByteBuf buf) {
        return new PacketRequestAmmo(buf.m_130135_(), buf.readInt());
    }

    public static void handle(PacketRequestAmmo msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ((NetworkEvent.Context)ctx.get()).getSender();
            if (player == null) {
                return;
            }
            if (player.m_20275_((double)msg.pos.m_123341_(), (double)msg.pos.m_123342_(), (double)msg.pos.m_123343_()) > 64.0) {
                return;
            }
            BlockEntity be = player.m_9236_().m_7702_(msg.pos);
            if (be instanceof HubBlockEntity) {
                HubBlockEntity hub = (HubBlockEntity)be;
                int cost = 0;
                int cooldownTime = 0;
                boolean isOnCooldown = false;
                switch (msg.type) {
                    case 0: {
                        String targetKit;
                        String pendingKit = player.getPersistentData().m_128461_("AAS_PendingKit");
                        String currentKitName = player.getPersistentData().m_128461_("AAS_CurrentKit");
                        boolean hasPending = !pendingKit.isEmpty();
                        String string = targetKit = hasPending ? pendingKit : currentKitName;
                        if (targetKit.isEmpty() || targetKit.equals("Unassigned")) {
                            player.m_213846_((Component)Component.m_237113_((String)"No Kit equipped!").m_130940_(ChatFormatting.RED));
                            return;
                        }
                        long lastFobUse = player.getPersistentData().m_128454_("AAS_LastFobResupply");
                        long currentTime = player.m_9236_().m_46467_();
                        if (!player.m_7500_() && currentTime < lastFobUse + 1200L) {
                            long secondsLeft = (lastFobUse + 1200L - currentTime) / 20L;
                            player.m_213846_((Component)Component.m_237113_((String)("Resupply on cooldown: " + secondsLeft + "s")).m_130940_(ChatFormatting.RED));
                            return;
                        }
                        cost = (Integer)AASConfig.HUB_RESUPPLY_COST.get();
                        if (!player.m_7500_() && hub.getMaterials() < cost) {
                            player.m_213846_((Component)Component.m_237113_((String)("Not enough Materials! Need: " + cost)).m_130940_(ChatFormatting.RED));
                            return;
                        }
                        AASWorldData data = AASWorldData.get(player.m_284548_());
                        if (hasPending) {
                            ResupplyHandler.tryApplyPendingKit(player, data);
                            if (!player.m_7500_()) {
                                hub.consumeMaterials(cost);
                                player.getPersistentData().m_128356_("AAS_LastFobResupply", currentTime);
                            }
                            player.m_9236_().m_7260_(msg.pos, hub.m_58900_(), hub.m_58900_(), 3);
                            player.m_213846_((Component)Component.m_237113_((String)("New Kit Equipped! (-" + cost + " Mats)")).m_130940_(ChatFormatting.GREEN));
                        } else {
                            AASWorldData.KitInfo kit;
                            String t = player.m_5647_() != null ? player.m_5647_().m_5758_().toUpperCase() : "NEUTRAL";
                            AASWorldData.KitInfo kitInfo = kit = t.equals("BLUE") ? data.blueKits.get(currentKitName) : data.redKits.get(currentKitName);
                            if (kit != null) {
                                if (ResupplyHandler.resupplyPlayer(player, kit, false)) {
                                    if (!player.m_7500_()) {
                                        hub.consumeMaterials(cost);
                                        player.getPersistentData().m_128356_("AAS_LastFobResupply", currentTime);
                                    }
                                    player.m_9236_().m_7260_(msg.pos, hub.m_58900_(), hub.m_58900_(), 3);
                                    player.m_213846_((Component)Component.m_237113_((String)("Kit Resupplied! (-" + cost + " Mats)")).m_130940_(ChatFormatting.GREEN));
                                } else {
                                    player.m_213846_((Component)Component.m_237113_((String)"Ammo already full!").m_130940_(ChatFormatting.YELLOW));
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
                if (!player.m_7500_() && isOnCooldown) {
                    player.m_213846_((Component)Component.m_237113_((String)"Supply on Cooldown!").m_130940_(ChatFormatting.RED));
                    return;
                }
                if (!player.m_7500_() && hub.getMaterials() < cost) {
                    player.m_213846_((Component)Component.m_237113_((String)("Not enough Construction Materials! Need: " + cost)).m_130940_(ChatFormatting.RED));
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
                        hub.consumeMaterials(cost);
                        hub.setCooldown(msg.type, cooldownTime);
                    }
                    player.m_213846_((Component)Component.m_237113_((String)("Resupplied! (-" + cost + " Mats)")).m_130940_(ChatFormatting.GREEN));
                    player.m_9236_().m_7260_(msg.pos, hub.m_58900_(), hub.m_58900_(), 3);
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }
}

