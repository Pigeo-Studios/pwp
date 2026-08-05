package com.pigeostudios.pwp.warfare.network;

import com.pigeostudios.pwp.warfare.client.ClientSafetyState;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent.Context;

// Пакет состояния мейн-зоны (сервер -> клиент). Отправляется при входе/выходе
// игрока из мейн-зоны и при входе на сервер. Клиент хранит флаг в
// ClientSafetyState и использует его для мгновенной отмены локальных попыток
// выстрела (TACZ, FCL) — без лишней анимации/расхода патрона.
// Само уведомление «Вы вошли в мейн-зону» приходит отдельным PacketNotification.
public class PacketMainZoneState {
   private final boolean inMainZone;

   public PacketMainZoneState(boolean inMainZone) {
      this.inMainZone = inMainZone;
   }

   public static void encode(PacketMainZoneState msg, FriendlyByteBuf buf) {
      buf.writeBoolean(msg.inMainZone);
   }

   public static PacketMainZoneState decode(FriendlyByteBuf buf) {
      return new PacketMainZoneState(buf.readBoolean());
   }

   public static void handle(PacketMainZoneState msg, Supplier<Context> ctx) {
      ctx.get().enqueueWork(() -> {
         if (ctx.get().getDirection().getReceptionSide().isClient()) {
            ClientSafetyState.inMainZone = msg.inMainZone;
         }
      });
      ctx.get().setPacketHandled(true);
   }
}
