package com.pigeostudios.pwp.warfare.network;

import com.pigeostudios.pwp.warfare.client.ClientData;
import com.pigeostudios.pwp.warfare.world.WarfareWorldData;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent.Context;

public class PacketSyncSpawners {
   private final List<WarfareWorldData.SpawnerInfo> spawners;

   public PacketSyncSpawners(List<WarfareWorldData.SpawnerInfo> spawners) {
      this.spawners = spawners;
   }

   public static void encode(PacketSyncSpawners msg, FriendlyByteBuf buf) {
      buf.writeCollection(msg.spawners, (b, s) -> {
         b.writeBlockPos(s.pos);
         b.writeUtf(s.team);
         b.writeUtf(s.type);
         b.writeInt(s.ticketPenalty);
         b.writeInt(s.respawnTime);
         b.writeLong(s.targetSpawnTick);
         b.writeBoolean(s.isAlive);
         b.writeBoolean(s.hasSpawnedOnce);
      });
   }

   public static PacketSyncSpawners decode(FriendlyByteBuf buf) {
      return new PacketSyncSpawners(buf.readList(b -> {
         return new WarfareWorldData.SpawnerInfo(
            b.readBlockPos(), b.readUtf(), b.readUtf(), b.readInt(), b.readInt(), b.readLong(), b.readBoolean(), b.readBoolean()
         );
      }));
   }

   public static void handle(PacketSyncSpawners msg, Supplier<Context> ctx) {
      ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
         ClientData.clientSpawners = msg.spawners;
      }));
      ctx.get().setPacketHandled(true);
   }
}
