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

public class PacketRequestCMD {
    public static void encode(PacketRequestCMD msg, FriendlyByteBuf buf) {
    }

    public static PacketRequestCMD decode(FriendlyByteBuf buf) {
        return new PacketRequestCMD();
    }

    public static void handle(PacketRequestCMD msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            int currentCMD;
            boolean alreadyVoting;
            ServerPlayer player = ((NetworkEvent.Context)ctx.get()).getSender();
            if (player == null || player.m_5647_() == null) {
                return;
            }
            AASWorldData data = AASWorldData.get(player.m_284548_());
            String pName = player.m_6302_();
            String team = player.m_5647_().m_5758_().toUpperCase();
            boolean isBlue = team.equals("BLUE");
            boolean bl = alreadyVoting = isBlue ? data.blueCmdVoteActive : data.redCmdVoteActive;
            if (alreadyVoting) {
                return;
            }
            int n = currentCMD = isBlue ? data.blueCMDId : data.redCMDId;
            if (currentCMD != -1) {
                return;
            }
            if (player.getPersistentData().m_128471_("AAS_IsSquadLeader")) {
                if (isBlue) {
                    data.blueCmdVoteActive = true;
                    data.blueCmdCandidateName = pName;
                    data.blueCmdCandidateId = player.getPersistentData().m_128451_("AAS_SquadID");
                    data.blueCmdVoteTimer = 600;
                    data.blueCmdVotes.clear();
                } else {
                    data.redCmdVoteActive = true;
                    data.redCmdCandidateName = pName;
                    data.redCmdCandidateId = player.getPersistentData().m_128451_("AAS_SquadID");
                    data.redCmdVoteTimer = 600;
                    data.redCmdVotes.clear();
                }
                data.m_77762_();
                PacketHandler.sendToAllClients(player.m_284548_(), data);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}

