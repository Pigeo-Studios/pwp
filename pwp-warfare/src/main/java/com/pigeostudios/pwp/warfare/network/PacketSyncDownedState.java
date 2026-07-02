package com.pigeostudios.pwp.warfare.network;

import com.pigeostudios.pwp.warfare.client.ClientHooks;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent.Context;

// Пакет синхронизации состояния "ранен/не ранен" для игрока
// Отправляется с сервера на клиент при изменении статуса
public class PacketSyncDownedState {
   private final int entityId;
   private final boolean isDowned;

   public PacketSyncDownedState(int id, boolean downed) {
      this.entityId = id;
      this.isDowned = downed;
   }

   public static void encode(PacketSyncDownedState msg, FriendlyByteBuf buf) {
      buf.writeInt(msg.entityId);
      buf.writeBoolean(msg.isDowned);
   }

   public static PacketSyncDownedState decode(FriendlyByteBuf buf) {
      return new PacketSyncDownedState(buf.readInt(), buf.readBoolean());
   }

   // Обновляет состояние downed для указанной сущности на клиенте
   public static void handle(PacketSyncDownedState msg, Supplier<Context> ctx) {
      ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientHooks.handleDownedState(msg.entityId, msg.isDowned)));
      ctx.get().setPacketHandled(true);
   }
}
