/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.ChatFormatting
 *  net.minecraft.network.FriendlyByteBuf
 *  net.minecraft.network.chat.Component
 *  net.minecraft.server.level.ServerPlayer
 *  net.minecraft.world.entity.Entity
 *  net.minecraftforge.network.NetworkEvent$Context
 */
package com.example.aas.network;

import com.example.aas.entity.SupplyCrateEntity;
import java.util.function.Supplier;
import net.minecraft.ChatFormatting;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.network.NetworkEvent;

public class PacketDropCrate {
    public static void encode(PacketDropCrate msg, FriendlyByteBuf buf) {
    }

    public static PacketDropCrate decode(FriendlyByteBuf buf) {
        return new PacketDropCrate();
    }

    public static void handle(PacketDropCrate msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            Entity vehicle;
            ServerPlayer player = ((NetworkEvent.Context)ctx.get()).getSender();
            if (player != null && (vehicle = player.m_20202_()) != null && vehicle.getPersistentData().m_128471_("AAS_IsSupplyTruck")) {
                if (vehicle.m_146895_() != player) {
                    player.m_5661_((Component)Component.m_237113_((String)"Only the driver can drop supplies!").m_130940_(ChatFormatting.RED), true);
                    return;
                }
                int ammo = vehicle.getPersistentData().m_128451_("AAS_SupplyAmmo");
                if (ammo > 0) {
                    vehicle.getPersistentData().m_128405_("AAS_SupplyAmmo", ammo - 1);
                    String team = vehicle.getPersistentData().m_128461_("AAS_VehicleTeam");
                    if (team.isEmpty()) {
                        team = "NEUTRAL";
                    }
                    double yawRad = Math.toRadians(vehicle.m_146908_());
                    double x = vehicle.m_20185_() + Math.sin(yawRad) * 2.5;
                    double z = vehicle.m_20189_() - Math.cos(yawRad) * 2.5;
                    double y = vehicle.m_20186_() + 1.5;
                    SupplyCrateEntity crate = new SupplyCrateEntity(player.m_9236_(), x, y, z, team, player.m_20148_());
                    player.m_9236_().m_7967_((Entity)crate);
                    player.m_5661_((Component)Component.m_237113_((String)"Supply Crate Dropped!").m_130940_(ChatFormatting.YELLOW), true);
                } else {
                    player.m_5661_((Component)Component.m_237113_((String)"No Supplies! Return to Main Base.").m_130940_(ChatFormatting.RED), true);
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }
}

