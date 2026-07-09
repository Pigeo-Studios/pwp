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
            if (player == null || player.getTeam() == null) {
                return;
            }
            AASWorldData data = AASWorldData.get(player.serverLevel());
            if (!data.voteActive) {
                return;
            }
            data.votes.put(player.getUUID(), msg.agree);
            data.setDirty();
            PacketHandler.sendToAllClients(player.serverLevel(), data);
        });
        ctx.get().setPacketHandled(true);
    }
}

