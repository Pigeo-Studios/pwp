/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.ChatFormatting
 *  net.minecraft.network.FriendlyByteBuf
 *  net.minecraft.network.chat.Component
 *  net.minecraft.server.level.ServerPlayer
 *  net.minecraft.sounds.SoundEvents
 *  net.minecraft.sounds.SoundSource
 *  net.minecraft.world.entity.Entity
 *  net.minecraftforge.network.NetworkEvent$Context
 */
package com.example.aas.network;

import com.example.aas.config.AASConfig;
import com.example.aas.network.ResupplyHandler;
import com.example.aas.world.AASWorldData;
import java.util.function.Supplier;
import net.minecraft.ChatFormatting;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.network.NetworkEvent;

public class PacketRequestVehicleAmmo {
    private final int entityId;
    private final int type;

    public PacketRequestVehicleAmmo(int entityId, int type) {
        this.entityId = entityId;
        this.type = type;
    }

    public static void encode(PacketRequestVehicleAmmo msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.entityId);
        buf.writeInt(msg.type);
    }

    public static PacketRequestVehicleAmmo decode(FriendlyByteBuf buf) {
        return new PacketRequestVehicleAmmo(buf.readInt(), buf.readInt());
    }

    public static void handle(PacketRequestVehicleAmmo msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            AASWorldData.KitInfo kit;
            ServerPlayer player = ((NetworkEvent.Context)ctx.get()).getSender();
            if (player == null) {
                return;
            }
            Entity vehicle = player.level().getEntity(msg.entityId);
            if (vehicle == null || player.distanceToSqr(vehicle) > 64.0) {
                return;
            }
            int currentMats = vehicle.getPersistentData().getInt("AAS_VehicleMats");
            int cost = (Integer)AASConfig.HUB_RESUPPLY_COST.get();
            if (!player.isCreative() && currentMats < cost) {
                player.displayClientMessage((Component)Component.literal((String)("Not enough Materials in Vehicle! (" + currentMats + ")")).withStyle(ChatFormatting.RED), true);
                return;
            }
            String kitName = player.getPersistentData().getString("AAS_CurrentKit");
            if (kitName.isEmpty() || kitName.equals("Unassigned")) {
                player.displayClientMessage((Component)Component.literal((String)"No Kit equipped!").withStyle(ChatFormatting.RED), true);
                return;
            }
            AASWorldData data = AASWorldData.get(player.serverLevel());
            String t = player.getTeam() != null ? player.getTeam().getName().toUpperCase() : "NEUTRAL";
            AASWorldData.KitInfo kitInfo = kit = t.equals("BLUE") ? data.blueKits.get(kitName) : data.redKits.get(kitName);
            if (kit != null) {
                if (ResupplyHandler.resupplyPlayer(player, kit, false)) {
                    int newMats = currentMats;
                    if (!player.isCreative()) {
                        newMats = currentMats - cost;
                        vehicle.getPersistentData().putInt("AAS_VehicleMats", newMats);
                    }
                    player.displayClientMessage((Component)Component.literal((String)("Kit Resupplied! Vehicle Mats: " + newMats)).withStyle(ChatFormatting.GREEN), true);
                    player.level().playSound(null, player.blockPosition(), SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 1.0f, 1.0f);
                } else {
                    player.displayClientMessage((Component)Component.literal((String)("Ammo already full! Vehicle Mats: " + currentMats)).withStyle(ChatFormatting.YELLOW), true);
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }
}

