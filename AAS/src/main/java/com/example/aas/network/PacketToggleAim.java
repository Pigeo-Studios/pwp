/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.network.FriendlyByteBuf
 *  net.minecraft.server.level.ServerPlayer
 *  net.minecraft.world.entity.Entity
 *  net.minecraftforge.network.NetworkEvent$Context
 */
package com.example.aas.network;

import com.example.aas.entity.AGS30Entity;
import com.example.aas.entity.M2BrowningEntity;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.network.NetworkEvent;

public class PacketToggleAim {
    public static void encode(PacketToggleAim msg, FriendlyByteBuf buf) {
    }

    public static PacketToggleAim decode(FriendlyByteBuf buf) {
        return new PacketToggleAim();
    }

    public static void handle(PacketToggleAim msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ((NetworkEvent.Context)ctx.get()).getSender();
            if (player != null) {
                Entity vehicle = player.getVehicle();
                if (vehicle instanceof M2BrowningEntity) {
                    M2BrowningEntity m2;
                    m2.setAiming(!(m2 = (M2BrowningEntity)vehicle).isAiming());
                } else if (vehicle instanceof AGS30Entity) {
                    AGS30Entity ags;
                    ags.setAiming(!(ags = (AGS30Entity)vehicle).isAiming());
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }
}

