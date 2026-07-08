/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.network.FriendlyByteBuf
 *  net.minecraftforge.api.distmarker.Dist
 *  net.minecraftforge.fml.DistExecutor
 *  net.minecraftforge.network.NetworkEvent$Context
 */
package com.example.aas.network;

import com.example.aas.client.ClientData;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

public class PacketSyncMyKit {
    private final String kitName;

    public PacketSyncMyKit(String kitName) {
        this.kitName = kitName;
    }

    public static void encode(PacketSyncMyKit msg, FriendlyByteBuf buf) {
        buf.m_130070_(msg.kitName == null ? "" : msg.kitName);
    }

    public static PacketSyncMyKit decode(FriendlyByteBuf buf) {
        return new PacketSyncMyKit(buf.m_130277_());
    }

    public static void handle(PacketSyncMyKit msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn((Dist)Dist.CLIENT, () -> () -> {
            ClientData.myCurrentKit = msg.kitName;
        }));
        ctx.get().setPacketHandled(true);
    }
}

