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
        buf.writeUtf(msg.team);
    }

    public static PacketDebugSpawnRally decode(FriendlyByteBuf buf) {
        return new PacketDebugSpawnRally(buf.readUtf());
    }

    public static void handle(PacketDebugSpawnRally msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ((NetworkEvent.Context)ctx.get()).getSender();
            if (player != null) {
                String teamName;
                BlockPos pos = player.blockPosition();
                AASWorldData data = AASWorldData.get(player.serverLevel().getServer().overworld());
                ServerScoreboard scoreboard = player.getServer().getScoreboard();
                PlayerTeam pTeam = scoreboard.getPlayerTeam(teamName = msg.team.equalsIgnoreCase("BLUE") ? "Blue" : "Red");
                if (pTeam == null) {
                    pTeam = scoreboard.addPlayerTeam(teamName);
                }
                scoreboard.addPlayerToTeam(player.getScoreboardName(), pTeam);
                if (msg.team.equals("BLUE")) {
                    player.level().setBlock(pos, ((Block)ModBlocks.BLUE_RALLY_BLOCK.get()).defaultBlockState(), 3);
                    data.blueRallies.add(pos);
                    player.sendSystemMessage((Component)Component.literal((String)"DEBUG: BLUE Rally spawned at feet & Joined Blue Team").withStyle(ChatFormatting.BLUE));
                } else {
                    player.level().setBlock(pos, ((Block)ModBlocks.RED_RALLY_BLOCK.get()).defaultBlockState(), 3);
                    data.redRallies.add(pos);
                    player.sendSystemMessage((Component)Component.literal((String)"DEBUG: RED Rally spawned at feet & Joined Red Team").withStyle(ChatFormatting.RED));
                }
                data.setDirty();
                PacketHandler.sendToAllClients(data, false, false, false, false);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}

