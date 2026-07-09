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

public class PacketVoiceActivity {
    private final String playerName;

    public PacketVoiceActivity(String playerName) {
        this.playerName = playerName;
    }

    public static void encode(PacketVoiceActivity msg, FriendlyByteBuf buf) {
        buf.writeUtf(msg.playerName);
    }

    public static PacketVoiceActivity decode(FriendlyByteBuf buf) {
        return new PacketVoiceActivity(buf.readUtf());
    }

    public static void handle(PacketVoiceActivity msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn((Dist)Dist.CLIENT, () -> () -> ClientData.SQUAD_SPEAKERS.put(msg.playerName, System.currentTimeMillis())));
        ctx.get().setPacketHandled(true);
    }
}

