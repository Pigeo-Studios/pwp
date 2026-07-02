package com.pigeostudios.pwp.warfare.network;

import com.pigeostudios.pwp.warfare.world.WarfareWorldData;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.NetworkEvent.Context;

// Пакет размещения пинга отряда (обычный или маркер перемещения)
// Отправляется лидерами отделений для указания целей
public class PacketPlacePing {
   private final boolean isMoveMarker;

   public PacketPlacePing(boolean isMoveMarker) {
      this.isMoveMarker = isMoveMarker;
   }

   public static void encode(PacketPlacePing msg, FriendlyByteBuf buf) {
      buf.writeBoolean(msg.isMoveMarker);
   }

   public static PacketPlacePing decode(FriendlyByteBuf buf) {
      return new PacketPlacePing(buf.readBoolean());
   }

   // Выполняет рейкаст и устанавливает пинг или маркер перемещения отряда
   public static void handle(PacketPlacePing msg, Supplier<Context> ctx) {
      ctx.get().enqueueWork(() -> {
         ServerPlayer player = ctx.get().getSender();
         if (player != null) {
            WarfareWorldData data = WarfareWorldData.get(player.serverLevel());
            String pName = player.getScoreboardName();
            Vec3 eyePos = player.getEyePosition();
            Vec3 reachVec = eyePos.add(player.getLookAngle().scale(300.0));
            BlockHitResult hit = player.level().clip(new ClipContext(eyePos, reachVec, Block.COLLIDER, Fluid.NONE, player));
            if (hit.getType() == Type.BLOCK) {
               BlockPos target = hit.getBlockPos().relative(hit.getDirection());

               for (WarfareWorldData.Squad s : data.squads) {
                  if (s.members.contains(pName)) {
                     long time = player.level().getGameTime();
                     if (s.leader.equals(pName)) {
                        if (msg.isMoveMarker) {
                           s.marker = new WarfareWorldData.SquadMarker(target.getX(), target.getY(), target.getZ(), 0, time + 12000L, true);
                        } else {
                           s.pingPos = target;
                           s.pingExpiry = time + 400L;
                        }
                     } else if (s.bravoLeader.equals(pName)) {
                        if (msg.isMoveMarker) {
                           s.bravoMarker = new WarfareWorldData.SquadMarker(target.getX(), target.getY(), target.getZ(), 0, time + 12000L, true);
                        } else {
                           s.bravoPingPos = target;
                           s.bravoPingExpiry = time + 400L;
                        }
                     } else {
                        if (!s.charlieLeader.equals(pName)) {
                           break;
                        }

                        if (msg.isMoveMarker) {
                           s.charlieMarker = new WarfareWorldData.SquadMarker(target.getX(), target.getY(), target.getZ(), 0, time + 12000L, true);
                        } else {
                           s.charliePingPos = target;
                           s.charliePingExpiry = time + 400L;
                        }
                     }

                     data.setDirty();
                     PacketHandler.INSTANCE.send(PacketDistributor.DIMENSION.with(player.level()::dimension), new PacketSyncSquads(data.squads));
                     break;
                  }
               }
            }
         }
      });
      ctx.get().setPacketHandled(true);
   }
}
