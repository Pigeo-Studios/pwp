/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.network.FriendlyByteBuf
 *  net.minecraft.server.level.ServerPlayer
 *  net.minecraftforge.network.NetworkEvent$Context
 */
package com.example.aas.network;

import com.example.aas.network.PacketHandler;
import com.example.aas.world.AASWorldData;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

public class PacketVoteAction {
    private final boolean agree;

    public PacketVoteAction(boolean agree) {
        this.agree = agree;
    }

    public static void encode(PacketVoteAction msg, FriendlyByteBuf buf) {
        buf.writeBoolean(msg.agree);
    }

    public static PacketVoteAction decode(FriendlyByteBuf buf) {
        return new PacketVoteAction(buf.readBoolean());
    }

    public static void handle(PacketVoteAction msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ((NetworkEvent.Context)ctx.get()).getSender();
            if (player == null || player.m_5647_() == null) {
                return;
            }
            AASWorldData data = AASWorldData.get(player.m_284548_());
            if (!data.voteActive) {
                return;
            }
            data.votes.put(player.m_20148_(), msg.agree);
            data.m_77762_();
            PacketHandler.sendToAllClients(player.m_284548_(), data);
        });
        ctx.get().setPacketHandled(true);
    }
}

