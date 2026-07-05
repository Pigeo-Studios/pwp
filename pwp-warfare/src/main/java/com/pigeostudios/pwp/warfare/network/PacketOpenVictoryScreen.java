package com.pigeostudios.pwp.warfare.network;

import com.pigeostudios.pwp.warfare.client.ClientHooks;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent.Context;

public class PacketOpenVictoryScreen {
   public final String winnerName;
   public final String winnerFaction;
   public final String subText;
   public final boolean isBlueWinner;
   public final int matchKills;
   public final int matchDeaths;
   public final int matchAssists;
   public final int matchVehicleKills;
   public final int matchVehiclesDestroyed;
   public final int matchAirVehiclesDestroyed;
   public final int matchCaptures;
   public final int matchRevives;
   public final int matchHeadshots;
   public final int matchScore;
   public final int matchDurationSec;

   public PacketOpenVictoryScreen(String winnerName, String winnerFaction, String subText, boolean isBlueWinner,
                                   int matchKills, int matchDeaths, int matchAssists,
                                   int matchVehicleKills, int matchVehiclesDestroyed,
                                   int matchAirVehiclesDestroyed, int matchCaptures,
                                   int matchRevives, int matchHeadshots, int matchScore,
                                   int matchDurationSec) {
      this.winnerName = winnerName;
      this.winnerFaction = winnerFaction;
      this.subText = subText;
      this.isBlueWinner = isBlueWinner;
      this.matchKills = matchKills;
      this.matchDeaths = matchDeaths;
      this.matchAssists = matchAssists;
      this.matchVehicleKills = matchVehicleKills;
      this.matchVehiclesDestroyed = matchVehiclesDestroyed;
      this.matchAirVehiclesDestroyed = matchAirVehiclesDestroyed;
      this.matchCaptures = matchCaptures;
      this.matchRevives = matchRevives;
      this.matchHeadshots = matchHeadshots;
      this.matchScore = matchScore;
      this.matchDurationSec = matchDurationSec;
   }

   public static void encode(PacketOpenVictoryScreen msg, FriendlyByteBuf buf) {
      buf.writeUtf(msg.winnerName);
      buf.writeUtf(msg.winnerFaction);
      buf.writeUtf(msg.subText);
      buf.writeBoolean(msg.isBlueWinner);
      buf.writeInt(msg.matchKills);
      buf.writeInt(msg.matchDeaths);
      buf.writeInt(msg.matchAssists);
      buf.writeInt(msg.matchVehicleKills);
      buf.writeInt(msg.matchVehiclesDestroyed);
      buf.writeInt(msg.matchAirVehiclesDestroyed);
      buf.writeInt(msg.matchCaptures);
      buf.writeInt(msg.matchRevives);
      buf.writeInt(msg.matchHeadshots);
      buf.writeInt(msg.matchScore);
      buf.writeInt(msg.matchDurationSec);
   }

   public static PacketOpenVictoryScreen decode(FriendlyByteBuf buf) {
      return new PacketOpenVictoryScreen(
         buf.readUtf(), buf.readUtf(), buf.readUtf(), buf.readBoolean(),
         buf.readInt(), buf.readInt(), buf.readInt(),
         buf.readInt(), buf.readInt(), buf.readInt(),
         buf.readInt(), buf.readInt(), buf.readInt(),
         buf.readInt(), buf.readInt()
      );
   }

   public static void handle(PacketOpenVictoryScreen msg, Supplier<Context> ctx) {
      ctx.get()
         .enqueueWork(
            () -> DistExecutor.unsafeRunWhenOn(
               Dist.CLIENT, () -> () -> ClientHooks.openVictoryScreen(msg.winnerName, msg.winnerFaction, msg.subText, msg.isBlueWinner,
                  msg.matchKills, msg.matchDeaths, msg.matchAssists,
                  msg.matchVehicleKills, msg.matchVehiclesDestroyed,
                  msg.matchAirVehiclesDestroyed, msg.matchCaptures,
                  msg.matchRevives, msg.matchHeadshots, msg.matchScore,
                  msg.matchDurationSec)
            )
         );
      ctx.get().setPacketHandled(true);
   }
}
