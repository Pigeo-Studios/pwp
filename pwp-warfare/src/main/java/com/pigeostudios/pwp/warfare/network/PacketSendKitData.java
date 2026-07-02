package com.pigeostudios.pwp.warfare.network;

import com.pigeostudios.pwp.warfare.client.WarfareClipboard;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent.Context;

// Пакет отправки NBT-данных кита обратно на клиент
// Используется для копирования/вставки китов администратором
public class PacketSendKitData {
   private final Map<String, CompoundTag> data;

   public PacketSendKitData(Map<String, CompoundTag> data) {
      this.data = data;
   }

   public static void encode(PacketSendKitData msg, FriendlyByteBuf buf) {
      buf.writeInt(msg.data.size());
      msg.data.forEach((name, tag) -> {
         buf.writeUtf(name);
         buf.writeNbt(tag);
      });
   }

   public static PacketSendKitData decode(FriendlyByteBuf buf) {
      int size = buf.readInt();
      Map<String, CompoundTag> map = new HashMap<>();

      for (int i = 0; i < size; i++) {
         map.put(buf.readUtf(), buf.readNbt());
      }

      return new PacketSendKitData(map);
   }

   // Сохраняет полученные NBT-данные в буфер обмена администратора
   public static void handle(PacketSendKitData msg, Supplier<Context> ctx) {
      ctx.get().enqueueWork(() -> {
         if (msg.data.size() > 1) {
            WarfareClipboard.teamKitsData = msg.data;
         } else {
            msg.data.values().stream().findFirst().ifPresent(tag -> WarfareClipboard.kitData = tag);
         }
      });
      ctx.get().setPacketHandled(true);
   }
}
