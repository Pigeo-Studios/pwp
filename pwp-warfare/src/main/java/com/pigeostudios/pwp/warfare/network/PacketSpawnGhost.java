package com.pigeostudios.pwp.warfare.network;

import com.pigeostudios.pwp.warfare.client.ClientHooks;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent.Context;

// Пакет отображения призрачной (ghost) модели блока на клиенте
// Используется для предварительного просмотра размещаемой конструкции
public class PacketSpawnGhost {
   public final BlockPos pos;
   public final int blockId;

   public PacketSpawnGhost(BlockPos pos, int blockId) {
      this.pos = pos;
      this.blockId = blockId;
   }

   public static void encode(PacketSpawnGhost msg, FriendlyByteBuf buf) {
      buf.writeBlockPos(msg.pos);
      buf.writeInt(msg.blockId);
   }

   public static PacketSpawnGhost decode(FriendlyByteBuf buf) {
      return new PacketSpawnGhost(buf.readBlockPos(), buf.readInt());
   }

   // Отображает призрачный блок на клиенте в указанной позиции
   public static void handle(PacketSpawnGhost msg, Supplier<Context> ctx) {
      ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientHooks.handleSpawnGhost(msg)));
      ctx.get().setPacketHandled(true);
   }
}
