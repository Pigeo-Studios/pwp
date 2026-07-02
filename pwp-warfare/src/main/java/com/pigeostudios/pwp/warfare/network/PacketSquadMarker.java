package com.pigeostudios.pwp.warfare.network;

import com.pigeostudios.pwp.warfare.world.WarfareWorldData;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.NetworkEvent.Context;

// Пакет размещения маркера отряда на карте
// Отправляется лидерами отделений для указания позиций
public class PacketSquadMarker {
   private final int x;
   private final int z;
   private final int type;

   public PacketSquadMarker(int x, int z, int type) {
      this.x = x;
      this.z = z;
      this.type = type;
   }

   public static void encode(PacketSquadMarker msg, FriendlyByteBuf buf) {
      buf.writeInt(msg.x);
      buf.writeInt(msg.z);
      buf.writeInt(msg.type);
   }

   public static PacketSquadMarker decode(FriendlyByteBuf buf) {
      return new PacketSquadMarker(buf.readInt(), buf.readInt(), buf.readInt());
   }

   // Устанавливает маркер для соответствующего лидера (осн., Bravo, Charlie)
   public static void handle(PacketSquadMarker msg, Supplier<Context> ctx) {
      ctx.get().enqueueWork(() -> {
         ServerPlayer player = ctx.get().getSender();
         if (player != null) {
            ServerLevel level = player.serverLevel();
            WarfareWorldData data = WarfareWorldData.get(level);
            long expiry = level.getGameTime() + 6000L;
            String pName = player.getScoreboardName();

            for (WarfareWorldData.Squad s : data.squads) {
               if (s.members.contains(pName)) {
                  if (s.leader.equals(pName)) {
                     if (msg.type == 6) {
                        if (s.rhombusMarkers.size() >= 5) {
                           s.rhombusMarkers.remove(0);
                        }

                        s.rhombusMarkers.add(new WarfareWorldData.SquadMarker(msg.x, 64, msg.z, 6, expiry, false));
                     } else {
                        s.marker = new WarfareWorldData.SquadMarker(msg.x, 64, msg.z, msg.type, expiry, false);
                     }
                  } else if (s.bravoLeader.equals(pName)) {
                     if (msg.type != 6) {
                        s.bravoMarker = new WarfareWorldData.SquadMarker(msg.x, 64, msg.z, msg.type, expiry, false);
                     }
                  } else {
                     if (!s.charlieLeader.equals(pName)) {
                        break;
                     }

                     if (msg.type != 6) {
                        s.charlieMarker = new WarfareWorldData.SquadMarker(msg.x, 64, msg.z, msg.type, expiry, false);
                     }
                  }

                  data.setDirty();
                  PacketHandler.INSTANCE.send(PacketDistributor.DIMENSION.with(level::dimension), new PacketSyncSquads(data.squads));
                  break;
               }
            }
         }
      });
      ctx.get().setPacketHandled(true);
   }
}
