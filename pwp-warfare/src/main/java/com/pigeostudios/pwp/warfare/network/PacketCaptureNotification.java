package com.pigeostudios.pwp.warfare.network;

import com.pigeostudios.pwp.warfare.client.ClientData;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent.Context;

// Пакет уведомления о захвате точки
// Отправляется сервером всем клиентам при смене владельца точки
public class PacketCaptureNotification {
   private final String pointName;
   private final String team;
   private final boolean neutralized;

   public PacketCaptureNotification(String name, String team, boolean neut) {
      this.pointName = name;
      this.team = team;
      this.neutralized = neut;
   }

   public static void encode(PacketCaptureNotification msg, FriendlyByteBuf buf) {
      buf.writeUtf(msg.pointName);
      buf.writeUtf(msg.team);
      buf.writeBoolean(msg.neutralized);
   }

   public static PacketCaptureNotification decode(FriendlyByteBuf buf) {
      return new PacketCaptureNotification(buf.readUtf(), buf.readUtf(), buf.readBoolean());
   }

   // Добавляет уведомление о захвате в очередь клиентских уведомлений
   public static void handle(PacketCaptureNotification msg, Supplier<Context> ctx) {
      ctx.get().enqueueWork(() -> ClientData.captureNotifications.add(new ClientData.CaptureNotification(msg.pointName, msg.team, msg.neutralized)));
      ctx.get().setPacketHandled(true);
   }
}
