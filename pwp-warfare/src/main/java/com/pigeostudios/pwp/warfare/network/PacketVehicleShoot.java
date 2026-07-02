package com.pigeostudios.pwp.warfare.network;

import com.pigeostudios.pwp.warfare.entity.AGS30Entity;
import com.pigeostudios.pwp.warfare.entity.M2BrowningEntity;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.network.NetworkEvent.Context;

// Пакет стрельбы из установленного оружия (M2 Browning, АГС-30)
// Отправляется клиентом при нажатии кнопки стрельбы
public class PacketVehicleShoot {
   public static void encode(PacketVehicleShoot msg, FriendlyByteBuf buf) {
   }

   public static PacketVehicleShoot decode(FriendlyByteBuf buf) {
      return new PacketVehicleShoot();
   }

   // Вызывает метод стрельбы у M2 Browning или АГС-30
   public static void handle(PacketVehicleShoot msg, Supplier<Context> ctx) {
      ctx.get().enqueueWork(() -> {
         ServerPlayer player = ctx.get().getSender();
         if (player != null) {
            Entity vehicle = player.getVehicle();
            if (vehicle instanceof M2BrowningEntity m2) {
               m2.tryShoot(player);
            } else if (vehicle instanceof AGS30Entity ags) {
               ags.tryShoot(player);
            }
         }
      });
      ctx.get().setPacketHandled(true);
   }
}
