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
            Entity vehicle = player.m_9236_().m_6815_(msg.entityId);
            if (vehicle == null || player.m_20280_(vehicle) > 64.0) {
                return;
            }
            int currentMats = vehicle.getPersistentData().m_128451_("AAS_VehicleMats");
            int cost = (Integer)AASConfig.HUB_RESUPPLY_COST.get();
            if (!player.m_7500_() && currentMats < cost) {
                player.m_5661_((Component)Component.m_237113_((String)("Not enough Materials in Vehicle! (" + currentMats + ")")).m_130940_(ChatFormatting.RED), true);
                return;
            }
            String kitName = player.getPersistentData().m_128461_("AAS_CurrentKit");
            if (kitName.isEmpty() || kitName.equals("Unassigned")) {
                player.m_5661_((Component)Component.m_237113_((String)"No Kit equipped!").m_130940_(ChatFormatting.RED), true);
                return;
            }
            AASWorldData data = AASWorldData.get(player.m_284548_());
            String t = player.m_5647_() != null ? player.m_5647_().m_5758_().toUpperCase() : "NEUTRAL";
            AASWorldData.KitInfo kitInfo = kit = t.equals("BLUE") ? data.blueKits.get(kitName) : data.redKits.get(kitName);
            if (kit != null) {
                if (ResupplyHandler.resupplyPlayer(player, kit, false)) {
                    int newMats = currentMats;
                    if (!player.m_7500_()) {
                        newMats = currentMats - cost;
                        vehicle.getPersistentData().m_128405_("AAS_VehicleMats", newMats);
                    }
                    player.m_5661_((Component)Component.m_237113_((String)("Kit Resupplied! Vehicle Mats: " + newMats)).m_130940_(ChatFormatting.GREEN), true);
                    player.m_9236_().m_5594_(null, player.m_20183_(), SoundEvents.f_12019_, SoundSource.PLAYERS, 1.0f, 1.0f);
                } else {
                    player.m_5661_((Component)Component.m_237113_((String)("Ammo already full! Vehicle Mats: " + currentMats)).m_130940_(ChatFormatting.YELLOW), true);
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }
}

