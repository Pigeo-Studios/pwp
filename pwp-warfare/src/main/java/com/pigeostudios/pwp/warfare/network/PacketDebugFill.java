package com.pigeostudios.pwp.warfare.network;

import com.pigeostudios.pwp.warfare.world.WarfareWorldData;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.NetworkEvent.Context;

// Отладочный пакет: мгновенный захват точки прогрессом
// Доступен только в креативном режиме
public class PacketDebugFill {
   public static void encode(PacketDebugFill msg, FriendlyByteBuf buf) {
   }

   public static PacketDebugFill decode(FriendlyByteBuf buf) {
      return new PacketDebugFill();
   }

   // Добавляет прогресс захвата точки, на которой стоит игрок
   public static void handle(PacketDebugFill msg, Supplier<Context> ctx) {
      ctx.get().enqueueWork(() -> {
         ServerPlayer player = ctx.get().getSender();
         if (player != null && player.isCreative()) {
            ServerLevel level = player.serverLevel();
            WarfareWorldData data = WarfareWorldData.get(level);
            String pTeam = player.getTeam() != null ? player.getTeam().getName().toUpperCase() : "BLUE";

            for (WarfareWorldData.CapturePoint point : data.capturePoints) {
               if (point.isInside(player.position())) {
                  String oldOwner = point.owner;
                  point.progress += 0.255F;
                  if (point.progress >= 1.0F) {
                     point.progress = 1.0F;
                     if (!point.owner.equals(pTeam)) {
                        point.owner = pTeam;
                        point.capturingTeam = "NONE";
                        PacketHandler.INSTANCE.send(PacketDistributor.ALL.noArg(), new PacketCaptureNotification(point.name, pTeam, false));
                     }
                  }

                  data.setDirty();
                  PacketHandler.sendToAllClients(level, data);
                  break;
               }
            }
         }
      });
      ctx.get().setPacketHandled(true);
   }
}
