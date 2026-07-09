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
        buf.writeUtf(msg.teamName);
    }

    public static PacketTeamSelect decode(FriendlyByteBuf buf) {
        return new PacketTeamSelect(buf.readUtf());
    }

    public static void handle(PacketTeamSelect msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ((NetworkEvent.Context)ctx.get()).getSender();
            if (player != null) {
                AASWorldData data = AASWorldData.get(player.serverLevel());
                PacketSquadAction.leaveCurrentSquad(player, data);
                player.getPersistentData().putString("AAS_CurrentKit", "Unassigned");
                player.getPersistentData().remove("AAS_PendingKit");
                player.getInventory().clearContent();
                ResupplyHandler.clearCurios(player);
                player.inventoryMenu.broadcastChanges();
                data.setDirty();
                PacketHandler.INSTANCE.send(PacketDistributor.DIMENSION.with(() -> ((Level)player.level()).dimension()), (Object)new PacketSyncSquads(data.squads));
                ServerScoreboard scoreboard = player.getServer().getScoreboard();
                String internalTeamName = msg.teamName.equalsIgnoreCase("BLUE") ? "Blue" : "Red";
                ChatFormatting color = msg.teamName.equalsIgnoreCase("BLUE") ? ChatFormatting.BLUE : ChatFormatting.RED;
                PlayerTeam team = scoreboard.getPlayerTeam(internalTeamName);
                if (team == null) {
                    team = scoreboard.addPlayerTeam(internalTeamName);
                    team.setColor(color);
                    team.setSeeFriendlyInvisibles(true);
                }
                scoreboard.addPlayerToTeam(player.getScoreboardName(), team);
                if (data.isGameStarted) {
                    BlockPos mainSpawn;
                    String currentDim = player.level().dimension().location().toString();
                    BlockPos blockPos = mainSpawn = msg.teamName.equalsIgnoreCase("BLUE") ? data.blueSpawns.get(currentDim) : data.redSpawns.get(currentDim);
                    if (mainSpawn != null) {
                        player.teleportTo((double)mainSpawn.getX() + 0.5, (double)mainSpawn.getY(), (double)mainSpawn.getZ() + 0.5);
                        player.sendSystemMessage((Component)Component.literal((String)"Match in progress! Teleporting to Main Base...").withStyle(ChatFormatting.YELLOW));
                    } else {
                        player.sendSystemMessage((Component)Component.literal((String)"Warning: Main Base spawn point is not set for this team!").withStyle(ChatFormatting.RED));
                    }
                }
                player.sendSystemMessage((Component)Component.literal((String)("You joined the " + internalTeamName + " team! Kit and inventory reset.")).withStyle(color));
            }
        });
        ctx.get().setPacketHandled(true);
    }
}

