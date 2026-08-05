package com.pigeostudios.pwp.warfare.network;

import com.pigeostudios.pwp.warfare.client.NotificationFeed;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent.Context;

// Единый канал уведомлений (сервер -> клиент): любой текст можно отправить
// в ленту уведомлений одной строчкой. textKey — ключ из lang, severity —
// NotificationFeed.SEVERITY_INFO (акцентная полоска) / SEVERITY_DANGER (красная).
public class PacketNotification {
   private final String textKey;
   private final byte severity;

   public PacketNotification(String textKey, byte severity) {
      this.textKey = textKey;
      this.severity = severity;
   }

   public static void encode(PacketNotification msg, FriendlyByteBuf buf) {
      buf.writeUtf(msg.textKey);
      buf.writeByte(msg.severity);
   }

   public static PacketNotification decode(FriendlyByteBuf buf) {
      return new PacketNotification(buf.readUtf(), buf.readByte());
   }

   public static void handle(PacketNotification msg, Supplier<Context> ctx) {
      ctx.get().enqueueWork(() -> {
         if (ctx.get().getDirection().getReceptionSide().isClient()) {
            NotificationFeed.push(msg.textKey, msg.severity);
         }
      });
      ctx.get().setPacketHandled(true);
   }
}
