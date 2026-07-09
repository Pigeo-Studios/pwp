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
    final public String winnerName;
    final public String winnerFaction;
    final public String subText;
    final public boolean isBlueWinner;

    public PacketOpenVictoryScreen(String winnerName, String winnerFaction, String subText, boolean isBlueWinner) {
        this.winnerName = winnerName;
        this.winnerFaction = winnerFaction;
        this.subText = subText;
        this.isBlueWinner = isBlueWinner;
    }

    public static void encode(PacketOpenVictoryScreen msg, FriendlyByteBuf buf) {
        buf.writeUtf(msg.winnerName);
        buf.writeUtf(msg.winnerFaction);
        buf.writeUtf(msg.subText);
        buf.writeBoolean(msg.isBlueWinner);
    }

    public static PacketOpenVictoryScreen decode(FriendlyByteBuf buf) {
        return new PacketOpenVictoryScreen(buf.readUtf(), buf.readUtf(), buf.readUtf(), buf.readBoolean());
    }

    public static void handle(PacketOpenVictoryScreen msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn((Dist)Dist.CLIENT, () -> () -> ClientHooks.openVictoryScreen(msg.winnerName, msg.winnerFaction, msg.subText, msg.isBlueWinner)));
        ctx.get().setPacketHandled(true);
    }
}

