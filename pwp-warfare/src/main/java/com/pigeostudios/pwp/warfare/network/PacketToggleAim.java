package com.pigeostudios.pwp.warfare.network;

import com.pigeostudios.pwp.warfare.entity.AGS30Entity;
import com.pigeostudios.pwp.warfare.entity.M2BrowningEntity;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.network.NetworkEvent.Context;

// Пакет переключения режима прицеливания на установленном оружии (M2, АГС-30)
// Отправляется клиентом при нажатии кнопки прицела
public class PacketToggleAim {
   public static void encode(PacketToggleAim msg, FriendlyByteBuf buf) {
   }

   public static PacketToggleAim decode(FriendlyByteBuf buf) {
      return new PacketToggleAim();
   }

   // Переключает состояние прицеливания у M2 Browning или АГС-30
   public static void handle(PacketToggleAim msg, Supplier<Context> ctx) {
      ctx.get().enqueueWork(() -> {
         ServerPlayer player = ctx.get().getSender();
         if (player != null) {
            Entity vehicle = player.getVehicle();
            if (vehicle instanceof M2BrowningEntity m2) {
               m2.setAiming(!m2.isAiming());
            } else if (vehicle instanceof AGS30Entity ags) {
               ags.setAiming(!ags.isAiming());
            }
         }
      });
      ctx.get().setPacketHandled(true);
   }
}
