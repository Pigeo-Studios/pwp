package com.pigeostudios.pwp.warfare.network;

import com.pigeostudios.pwp.warfare.client.ClientHooks;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent.Context;

// Пакет открытия экрана победы для всех игроков
// Отправляется сервером при окончании игры
public class PacketOpenVictoryScreen {
   public final String winnerName;
   public final String winnerFaction;
   public final String subText;
   public final boolean isBlueWinner;

   public PacketOpenVictoryScreen(String winnerName, String winnerFaction, String subText, boolean isBlueWinner) {
      this.winnerName = winnerName;
      this.winnerFaction = winnerFaction;
      this.subText = subText;
      this.isBlueWinner = isBlueWinner;
   }

   public static void encode(PacketOpenVictoryScreen msg, FriendlyByteBuf buf) {
      buf.writeUtf(msg.winnerName);
      buf.writeUtf(msg.winnerFaction);
      buf.writeUtf(msg.subText);
      buf.writeBoolean(msg.isBlueWinner);
   }

   public static PacketOpenVictoryScreen decode(FriendlyByteBuf buf) {
      return new PacketOpenVictoryScreen(buf.readUtf(), buf.readUtf(), buf.readUtf(), buf.readBoolean());
   }

   // Открывает на клиенте экран победы с информацией о победителе
   public static void handle(PacketOpenVictoryScreen msg, Supplier<Context> ctx) {
      ctx.get()
         .enqueueWork(
            () -> DistExecutor.unsafeRunWhenOn(
               Dist.CLIENT, () -> () -> ClientHooks.openVictoryScreen(msg.winnerName, msg.winnerFaction, msg.subText, msg.isBlueWinner)
            )
         );
      ctx.get().setPacketHandled(true);
   }
}
