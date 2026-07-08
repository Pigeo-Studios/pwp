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

public class PacketRadioVoiceActivity {
    private final String playerName;

    public PacketRadioVoiceActivity(String playerName) {
        this.playerName = playerName;
    }

    public static void encode(PacketRadioVoiceActivity msg, FriendlyByteBuf buf) {
        buf.m_130070_(msg.playerName);
    }

    public static PacketRadioVoiceActivity decode(FriendlyByteBuf buf) {
        return new PacketRadioVoiceActivity(buf.m_130277_());
    }

    public static void handle(PacketRadioVoiceActivity msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn((Dist)Dist.CLIENT, () -> () -> ClientData.RADIO_SPEAKERS.put(msg.playerName, System.currentTimeMillis())));
        ctx.get().setPacketHandled(true);
    }
}

