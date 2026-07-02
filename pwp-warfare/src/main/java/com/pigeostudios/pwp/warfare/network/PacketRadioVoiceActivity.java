package com.pigeostudios.pwp.warfare.network;

import com.pigeostudios.pwp.warfare.client.ClientData;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent.Context;

// Пакет индикации голосовой активности в радио-канале
// Отправляется сервером для отображения иконки говорящего по радио
public class PacketRadioVoiceActivity {
   private final String playerName;

   public PacketRadioVoiceActivity(String playerName) {
      this.playerName = playerName;
   }

   public static void encode(PacketRadioVoiceActivity msg, FriendlyByteBuf buf) {
      buf.writeUtf(msg.playerName);
   }

   public static PacketRadioVoiceActivity decode(FriendlyByteBuf buf) {
      return new PacketRadioVoiceActivity(buf.readUtf());
   }

   // Обновляет время последней радио-активности для указанного игрока
   public static void handle(PacketRadioVoiceActivity msg, Supplier<Context> ctx) {
      ctx.get()
         .enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientData.RADIO_SPEAKERS.put(msg.playerName, System.currentTimeMillis())));
      ctx.get().setPacketHandled(true);
   }
}
