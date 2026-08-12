package com.pigeostudios.pwp.warfare.network;

import com.pigeostudios.pwp.warfare.voicechat.WarfareVoicechatPlugin;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent.Context;

// Клиент -> сервер: состояние войса у мёртвого игрока.
// true — открыт экран «ВЫ МЕРТВЫ» (войс полностью выключен: ни локал, ни рация);
// false — открыт экран деплоя (войс работает, но локал мёртвому недоступен — решает сервер).
public class PacketVoiceDeathState {
   private final boolean muted;

   public PacketVoiceDeathState(boolean muted) {
      this.muted = muted;
   }

   public static void encode(PacketVoiceDeathState msg, FriendlyByteBuf buf) {
      buf.writeBoolean(msg.muted);
   }

   public static PacketVoiceDeathState decode(FriendlyByteBuf buf) {
      return new PacketVoiceDeathState(buf.readBoolean());
   }

   public static void handle(PacketVoiceDeathState msg, Supplier<Context> ctx) {
      ctx.get().enqueueWork(() -> {
         ServerPlayer player = ctx.get().getSender();
         if (player != null) {
            WarfareVoicechatPlugin.setDeathVoiceMuted(player.getUUID(), msg.muted);
         }
      });
      ctx.get().setPacketHandled(true);
   }
}
