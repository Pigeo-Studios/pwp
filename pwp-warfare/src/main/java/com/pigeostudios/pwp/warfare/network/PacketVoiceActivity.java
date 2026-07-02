package com.pigeostudios.pwp.warfare.network;

import com.pigeostudios.pwp.warfare.client.ClientData;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent.Context;

// Пакет индикации голосовой активности в отрядном канале
// Отправляется сервером для отображения иконки говорящего в отряде
public class PacketVoiceActivity {
   private final String playerName;

   public PacketVoiceActivity(String playerName) {
      this.playerName = playerName;
   }

   public static void encode(PacketVoiceActivity msg, FriendlyByteBuf buf) {
      buf.writeUtf(msg.playerName);
   }

   public static PacketVoiceActivity decode(FriendlyByteBuf buf) {
      return new PacketVoiceActivity(buf.readUtf());
   }

   // Обновляет время последней голосовой активности игрока в отряде
   public static void handle(PacketVoiceActivity msg, Supplier<Context> ctx) {
      ctx.get()
         .enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientData.SQUAD_SPEAKERS.put(msg.playerName, System.currentTimeMillis())));
      ctx.get().setPacketHandled(true);
   }
}
