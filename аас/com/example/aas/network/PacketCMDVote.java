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

public class PacketCMDVote {
    private final boolean agree;

    public PacketCMDVote(boolean agree) {
        this.agree = agree;
    }

    public static void encode(PacketCMDVote msg, FriendlyByteBuf buf) {
        buf.writeBoolean(msg.agree);
    }

    public static PacketCMDVote decode(FriendlyByteBuf buf) {
        return new PacketCMDVote(buf.readBoolean());
    }

    public static void handle(PacketCMDVote msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ((NetworkEvent.Context)ctx.get()).getSender();
            if (player == null || player.m_5647_() == null) {
                return;
            }
            AASWorldData data = AASWorldData.get(player.m_284548_());
            String team = player.m_5647_().m_5758_().toUpperCase();
            boolean isBlue = team.equals("BLUE");
            if (!player.getPersistentData().m_128471_("AAS_IsSquadLeader")) {
                return;
            }
            if (isBlue) {
                if (data.blueCmdVoteActive && !player.m_6302_().equals(data.blueCmdCandidateName)) {
                    data.blueCmdVotes.put(player.m_20148_(), msg.agree);
                }
            } else if (data.redCmdVoteActive && !player.m_6302_().equals(data.redCmdCandidateName)) {
                data.redCmdVotes.put(player.m_20148_(), msg.agree);
            }
            data.m_77762_();
            PacketHandler.sendToAllClients(player.m_284548_(), data);
        });
        ctx.get().setPacketHandled(true);
    }
}

