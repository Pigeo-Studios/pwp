/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.network.FriendlyByteBuf
 *  net.minecraft.server.level.ServerPlayer
 *  net.minecraft.world.entity.Entity
 *  net.minecraft.world.entity.player.Player
 *  net.minecraftforge.network.NetworkEvent$Context
 */
package com.example.aas.network;

import com.example.aas.entity.AGS30Entity;
import com.example.aas.entity.M2BrowningEntity;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.network.NetworkEvent;

public class PacketVehicleShoot {
    public static void encode(PacketVehicleShoot msg, FriendlyByteBuf buf) {
    }

    public static PacketVehicleShoot decode(FriendlyByteBuf buf) {
        return new PacketVehicleShoot();
    }

    public static void handle(PacketVehicleShoot msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ((NetworkEvent.Context)ctx.get()).getSender();
            if (player != null) {
                Entity vehicle = player.getVehicle();
                if (vehicle instanceof M2BrowningEntity) {
                    M2BrowningEntity m2 = (M2BrowningEntity)vehicle;
                    m2.tryShoot((Player)player);
                } else if (vehicle instanceof AGS30Entity) {
                    AGS30Entity ags = (AGS30Entity)vehicle;
                    ags.tryShoot((Player)player);
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }
}

