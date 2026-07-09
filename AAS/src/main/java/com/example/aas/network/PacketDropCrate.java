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
            if (player != null && (vehicle = player.getVehicle()) != null && vehicle.getPersistentData().getBoolean("AAS_IsSupplyTruck")) {
                if (vehicle.getFirstPassenger() != player) {
                    player.displayClientMessage((Component)Component.literal((String)"Only the driver can drop supplies!").withStyle(ChatFormatting.RED), true);
                    return;
                }
                int ammo = vehicle.getPersistentData().getInt("AAS_SupplyAmmo");
                if (ammo > 0) {
                    vehicle.getPersistentData().putInt("AAS_SupplyAmmo", ammo - 1);
                    String team = vehicle.getPersistentData().getString("AAS_VehicleTeam");
                    if (team.isEmpty()) {
                        team = "NEUTRAL";
                    }
                    double yawRad = Math.toRadians(vehicle.getYRot());
                    double x = vehicle.getX() + Math.sin(yawRad) * 2.5;
                    double z = vehicle.getZ() - Math.cos(yawRad) * 2.5;
                    double y = vehicle.getY() + 1.5;
                    SupplyCrateEntity crate = new SupplyCrateEntity(player.level(), x, y, z, team, player.getUUID());
                    player.level().addFreshEntity((Entity)crate);
                    player.displayClientMessage((Component)Component.literal((String)"Supply Crate Dropped!").withStyle(ChatFormatting.YELLOW), true);
                } else {
                    player.displayClientMessage((Component)Component.literal((String)"No Supplies! Return to Main Base.").withStyle(ChatFormatting.RED), true);
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }
}

