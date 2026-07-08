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
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.scores.PlayerTeam
 *  net.minecraftforge.network.NetworkEvent$Context
 *  net.minecraftforge.network.PacketDistributor
 */
package com.example.aas.network;

import com.example.aas.network.PacketHandler;
import com.example.aas.network.PacketSquadAction;
import com.example.aas.network.PacketSyncSquads;
import com.example.aas.network.ResupplyHandler;
import com.example.aas.world.AASWorldData;
import java.util.function.Supplier;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.ServerScoreboard;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

public class PacketTeamSelect {
    private final String teamName;

    public PacketTeamSelect(String teamName) {
        this.teamName = teamName;
    }

    public static void encode(PacketTeamSelect msg, FriendlyByteBuf buf) {
        buf.m_130070_(msg.teamName);
    }

    public static PacketTeamSelect decode(FriendlyByteBuf buf) {
        return new PacketTeamSelect(buf.m_130277_());
    }

    public static void handle(PacketTeamSelect msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ((NetworkEvent.Context)ctx.get()).getSender();
            if (player != null) {
                AASWorldData data = AASWorldData.get(player.m_284548_());
                PacketSquadAction.leaveCurrentSquad(player, data);
                player.getPersistentData().m_128359_("AAS_CurrentKit", "Unassigned");
                player.getPersistentData().m_128473_("AAS_PendingKit");
                player.m_150109_().m_6211_();
                ResupplyHandler.clearCurios(player);
                player.f_36095_.m_38946_();
                data.m_77762_();
                PacketHandler.INSTANCE.send(PacketDistributor.DIMENSION.with(() -> ((Level)player.m_9236_()).m_46472_()), (Object)new PacketSyncSquads(data.squads));
                ServerScoreboard scoreboard = player.m_20194_().m_129896_();
                String internalTeamName = msg.teamName.equalsIgnoreCase("BLUE") ? "Blue" : "Red";
                ChatFormatting color = msg.teamName.equalsIgnoreCase("BLUE") ? ChatFormatting.BLUE : ChatFormatting.RED;
                PlayerTeam team = scoreboard.m_83489_(internalTeamName);
                if (team == null) {
                    team = scoreboard.m_83492_(internalTeamName);
                    team.m_83351_(color);
                    team.m_83362_(true);
                }
                scoreboard.m_6546_(player.m_6302_(), team);
                if (data.isGameStarted) {
                    BlockPos mainSpawn;
                    String currentDim = player.m_9236_().m_46472_().m_135782_().toString();
                    BlockPos blockPos = mainSpawn = msg.teamName.equalsIgnoreCase("BLUE") ? data.blueSpawns.get(currentDim) : data.redSpawns.get(currentDim);
                    if (mainSpawn != null) {
                        player.m_6021_((double)mainSpawn.m_123341_() + 0.5, (double)mainSpawn.m_123342_(), (double)mainSpawn.m_123343_() + 0.5);
                        player.m_213846_((Component)Component.m_237113_((String)"Match in progress! Teleporting to Main Base...").m_130940_(ChatFormatting.YELLOW));
                    } else {
                        player.m_213846_((Component)Component.m_237113_((String)"Warning: Main Base spawn point is not set for this team!").m_130940_(ChatFormatting.RED));
                    }
                }
                player.m_213846_((Component)Component.m_237113_((String)("You joined the " + internalTeamName + " team! Kit and inventory reset.")).m_130940_(color));
            }
        });
        ctx.get().setPacketHandled(true);
    }
}

