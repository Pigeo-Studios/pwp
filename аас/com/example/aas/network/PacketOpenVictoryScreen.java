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

import com.example.aas.client.ClientHooks;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

public class PacketOpenVictoryScreen {
    public final String winnerName;
    public final String winnerFaction;
    public final String subText;
    public final boolean isBlueWinner;

    public PacketOpenVictoryScreen(String winnerName, String winnerFaction, String subText, boolean isBlueWinner) {
        this.winnerName = winnerName;
        this.winnerFaction = winnerFaction;
        this.subText = subText;
        this.isBlueWinner = isBlueWinner;
    }

    public static void encode(PacketOpenVictoryScreen msg, FriendlyByteBuf buf) {
        buf.m_130070_(msg.winnerName);
        buf.m_130070_(msg.winnerFaction);
        buf.m_130070_(msg.subText);
        buf.writeBoolean(msg.isBlueWinner);
    }

    public static PacketOpenVictoryScreen decode(FriendlyByteBuf buf) {
        return new PacketOpenVictoryScreen(buf.m_130277_(), buf.m_130277_(), buf.m_130277_(), buf.readBoolean());
    }

    public static void handle(PacketOpenVictoryScreen msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn((Dist)Dist.CLIENT, () -> () -> ClientHooks.openVictoryScreen(msg.winnerName, msg.winnerFaction, msg.subText, msg.isBlueWinner)));
        ctx.get().setPacketHandled(true);
    }
}

