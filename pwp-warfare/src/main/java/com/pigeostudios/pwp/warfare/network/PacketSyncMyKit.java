package com.pigeostudios.pwp.warfare.network;

import com.pigeostudios.pwp.warfare.client.ClientData;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent.Context;

// Пакет синхронизации текущего кита игрока
// Отправляется сервером для обновления информации о выбранном наборе
public class PacketSyncMyKit {
   private final String kitName;

   public PacketSyncMyKit(String kitName) {
      this.kitName = kitName;
   }

   public static void encode(PacketSyncMyKit msg, FriendlyByteBuf buf) {
      buf.writeUtf(msg.kitName == null ? "" : msg.kitName);
   }

   public static PacketSyncMyKit decode(FriendlyByteBuf buf) {
      return new PacketSyncMyKit(buf.readUtf());
   }

   // Обновляет название текущего кита в клиентских данных
   public static void handle(PacketSyncMyKit msg, Supplier<Context> ctx) {
      ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientData.myCurrentKit = msg.kitName));
      ctx.get().setPacketHandled(true);
   }
}
