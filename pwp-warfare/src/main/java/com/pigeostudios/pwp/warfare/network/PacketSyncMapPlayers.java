package com.pigeostudios.pwp.warfare.network;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent.Context;

// Пакет синхронизации списка игроков на карте
// Отправляется сервером для отображения позиций всех игроков
public class PacketSyncMapPlayers {
   private final List<MapPlayerInfo> players;

   public PacketSyncMapPlayers(List<MapPlayerInfo> players) {
      this.players = players;
   }

   public List<MapPlayerInfo> getPlayers() {
      return this.players;
   }

   public static void encode(PacketSyncMapPlayers msg, FriendlyByteBuf buf) {
      buf.writeInt(msg.players.size());

      for (MapPlayerInfo p : msg.players) {
         buf.writeUtf(p.name);
         buf.writeUUID(p.uuid);
         buf.writeDouble(p.x);
         buf.writeDouble(p.z);
         buf.writeFloat(p.rot);
         buf.writeInt(p.squadId);
         buf.writeBoolean(p.isLeader);
         buf.writeBoolean(p.isDowned);
         buf.writeLong(p.lastShoutTime);
         buf.writeBoolean(p.inVehicle);
         buf.writeInt(p.vehicleId);
         buf.writeInt(p.seatIndex);
         buf.writeUtf(p.team);
      }
   }

   public static PacketSyncMapPlayers decode(FriendlyByteBuf buf) {
      int size = buf.readInt();
      List<MapPlayerInfo> list = new ArrayList<>(size);

      for (int i = 0; i < size; i++) {
         list.add(
            new MapPlayerInfo(
               buf.readUtf(),
               buf.readUUID(),
               buf.readDouble(),
               buf.readDouble(),
               buf.readFloat(),
               buf.readInt(),
               buf.readBoolean(),
               buf.readBoolean(),
               buf.readLong(),
               buf.readBoolean(),
               buf.readInt(),
               buf.readInt(),
               buf.readUtf()
            )
         );
      }

      return new PacketSyncMapPlayers(list);
   }

   // Передаёт список игроков клиентскому обработчику для отображения на карте
   public static void handle(PacketSyncMapPlayers msg, Supplier<Context> ctx) {
      ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientPacketHandler.handleSyncMap(msg)));
      ctx.get().setPacketHandled(true);
   }
}
