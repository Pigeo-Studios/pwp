package com.pigeostudios.pwp.warfare.network;

import com.pigeostudios.pwp.warfare.world.WarfareWorldData;
import java.util.function.Supplier;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.NetworkEvent.Context;

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

   public static void handle(PacketTeamSelect msg, Supplier<Context> ctx) {
      ctx.get().enqueueWork(() -> {
         ServerPlayer player = ctx.get().getSender();
         if (player == null) return;
         WarfareWorldData data = WarfareWorldData.get(player.serverLevel());

         // Team balance: block if target team has 2+ more players
         int blueCount = 0, redCount = 0;
         for (ServerPlayer p : player.serverLevel().getServer().getPlayerList().getPlayers()) {
            if (p == null || p == player) continue;
            String t = p.getTeam() != null ? p.getTeam().getName() : "";
            if (t.equalsIgnoreCase("Blue")) blueCount++;
            else if (t.equalsIgnoreCase("Red")) redCount++;
         }
         String targetTeam = msg.teamName.equalsIgnoreCase("BLUE") ? "Blue" : "Red";
         int targetCount = targetTeam.equals("Blue") ? blueCount : redCount;
         int otherCount = targetTeam.equals("Blue") ? redCount : blueCount;
         if (targetCount >= otherCount + 2) {
            player.sendSystemMessage(Component.literal("Team " + targetTeam + " is too full! Join the other team or wait.").withStyle(ChatFormatting.RED));
            return;
         }

         PacketSquadAction.leaveCurrentSquad(player, data);
         player.getPersistentData().putString("WARFARE_CurrentKit", "Unassigned");
         player.getPersistentData().remove("WARFARE_PendingKit");
         player.getInventory().clearContent();
         ResupplyHandler.clearCurios(player);
         player.inventoryMenu.broadcastChanges();
         data.setDirty();
         Scoreboard scoreboard = player.getServer().getScoreboard();
         String internalTeamName = targetTeam;
         ChatFormatting color = msg.teamName.equalsIgnoreCase("BLUE") ? ChatFormatting.BLUE : ChatFormatting.RED;
         PlayerTeam team = scoreboard.getPlayerTeam(internalTeamName);
         if (team == null) {
            team = scoreboard.addPlayerTeam(internalTeamName);
            team.setColor(color);
            team.setSeeFriendlyInvisibles(true);
         }

         scoreboard.addPlayerToTeam(player.getScoreboardName(), team);
         player.setGameMode(GameType.SURVIVAL);
         PacketHandler.broadcastPlayerSkin(player);
         PacketHandler.sendToAllClients(player.serverLevel(), data);
         PacketHandler.INSTANCE.send(PacketDistributor.DIMENSION.with(player.level()::dimension), new PacketSyncSquads(data.squads));
         if (data.isGameStarted) {
            String currentDim = player.level().dimension().location().toString();
            BlockPos mainSpawn = msg.teamName.equalsIgnoreCase("BLUE") ? data.blueSpawns.get(currentDim) : data.redSpawns.get(currentDim);
            if (mainSpawn != null) {
               player.teleportTo(mainSpawn.getX() + 0.5, mainSpawn.getY(), mainSpawn.getZ() + 0.5);
               player.sendSystemMessage(Component.literal("Match in progress! Teleporting to Main Base...").withStyle(ChatFormatting.YELLOW));
            } else {
               player.sendSystemMessage(Component.literal("Warning: Main Base spawn point is not set for this team!").withStyle(ChatFormatting.RED));
            }
         }

         player.sendSystemMessage(Component.literal("You joined the " + internalTeamName + " team!").withStyle(color));
      });
      ctx.get().setPacketHandled(true);
   }
}
