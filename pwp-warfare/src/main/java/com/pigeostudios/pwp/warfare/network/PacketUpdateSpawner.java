package com.pigeostudios.pwp.warfare.network;

import com.pigeostudios.pwp.warfare.block.VehicleSpawnerBlockEntity;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent.Context;

// Пакет обновления настроек спавнера транспортных средств
// Отправляется администратором при настройке времени респавна и типа техники
public class PacketUpdateSpawner {
   private final BlockPos pos;
   private final int respawnTime;
   private final int initialTime;
   private final String vehicleId;
   private final float vehicleYaw;

   public PacketUpdateSpawner(BlockPos pos, int respawn, int init, String id, float yaw) {
      this.pos = pos;
      this.respawnTime = respawn;
      this.initialTime = init;
      this.vehicleId = id;
      this.vehicleYaw = yaw;
   }

   public static void encode(PacketUpdateSpawner msg, FriendlyByteBuf buf) {
      buf.writeBlockPos(msg.pos);
      buf.writeInt(msg.respawnTime);
      buf.writeInt(msg.initialTime);
      buf.writeUtf(msg.vehicleId);
      buf.writeFloat(msg.vehicleYaw);
   }

   public static PacketUpdateSpawner decode(FriendlyByteBuf buf) {
      return new PacketUpdateSpawner(buf.readBlockPos(), buf.readInt(), buf.readInt(), buf.readUtf(), buf.readFloat());
   }

   // Применяет настройки к блоку спавнера транспортных средств
   public static void handle(PacketUpdateSpawner msg, Supplier<Context> ctx) {
      ctx.get().enqueueWork(() -> {
         ServerPlayer player = ctx.get().getSender();
         if (player != null && player.isCreative() && player.level().getBlockEntity(msg.pos) instanceof VehicleSpawnerBlockEntity spawner) {
            spawner.respawnTimeSettings = msg.respawnTime;
            spawner.initialTimeSettings = msg.initialTime;
            spawner.vehicleIdString = msg.vehicleId;
            spawner.vehicleYaw = msg.vehicleYaw;
            spawner.setChanged();
            player.level().sendBlockUpdated(msg.pos, spawner.getBlockState(), spawner.getBlockState(), 3);
         }
      });
      ctx.get().setPacketHandled(true);
   }
}
