/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.ChatFormatting
 *  net.minecraft.core.BlockPos
 *  net.minecraft.network.FriendlyByteBuf
 *  net.minecraft.network.chat.Component
 *  net.minecraft.server.ServerScoreboard
 *  net.minecraft.server.level.ServerPlayer
 *  net.minecraft.world.level.block.Block
 *  net.minecraft.world.scores.PlayerTeam
 *  net.minecraftforge.network.NetworkEvent$Context
 */
package com.example.aas.network;

import com.example.aas.block.ModBlocks;
import com.example.aas.network.PacketHandler;
import com.example.aas.world.AASWorldData;
import java.util.function.Supplier;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.ServerScoreboard;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraftforge.network.NetworkEvent;

public class PacketDebugSpawnRally {
    private final String team;

    public PacketDebugSpawnRally(String team) {
        this.team = team;
    }

    public static void encode(PacketDebugSpawnRally msg, FriendlyByteBuf buf) {
        buf.m_130070_(msg.team);
    }

    public static PacketDebugSpawnRally decode(FriendlyByteBuf buf) {
        return new PacketDebugSpawnRally(buf.m_130277_());
    }

    public static void handle(PacketDebugSpawnRally msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ((NetworkEvent.Context)ctx.get()).getSender();
            if (player != null) {
                String teamName;
                BlockPos pos = player.m_20183_();
                AASWorldData data = AASWorldData.get(player.m_284548_().m_7654_().m_129783_());
                ServerScoreboard scoreboard = player.m_20194_().m_129896_();
                PlayerTeam pTeam = scoreboard.m_83489_(teamName = msg.team.equalsIgnoreCase("BLUE") ? "Blue" : "Red");
                if (pTeam == null) {
                    pTeam = scoreboard.m_83492_(teamName);
                }
                scoreboard.m_6546_(player.m_6302_(), pTeam);
                if (msg.team.equals("BLUE")) {
                    player.m_9236_().m_7731_(pos, ((Block)ModBlocks.BLUE_RALLY_BLOCK.get()).m_49966_(), 3);
                    data.blueRallies.add(pos);
                    player.m_213846_((Component)Component.m_237113_((String)"DEBUG: BLUE Rally spawned at feet & Joined Blue Team").m_130940_(ChatFormatting.BLUE));
                } else {
                    player.m_9236_().m_7731_(pos, ((Block)ModBlocks.RED_RALLY_BLOCK.get()).m_49966_(), 3);
                    data.redRallies.add(pos);
                    player.m_213846_((Component)Component.m_237113_((String)"DEBUG: RED Rally spawned at feet & Joined Red Team").m_130940_(ChatFormatting.RED));
                }
                data.m_77762_();
                PacketHandler.sendToAllClients(data, false, false, false, false);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}

