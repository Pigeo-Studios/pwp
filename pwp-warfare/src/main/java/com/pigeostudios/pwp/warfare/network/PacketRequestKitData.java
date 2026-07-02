package com.pigeostudios.pwp.warfare.network;

import com.pigeostudios.pwp.warfare.world.WarfareWorldData;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.NetworkEvent.Context;

// Отладочный пакет: запрос данных набора (кита) в NBT-формате
// Используется для копирования китов администратором
public class PacketRequestKitData {
   private final String team;
   private final String kitName;

   public PacketRequestKitData(String team, String kitName) {
      this.team = team;
      this.kitName = kitName;
   }

   public static void encode(PacketRequestKitData msg, FriendlyByteBuf buf) {
      buf.writeUtf(msg.team);
      buf.writeUtf(msg.kitName);
   }

   public static PacketRequestKitData decode(FriendlyByteBuf buf) {
      return new PacketRequestKitData(buf.readUtf(), buf.readUtf());
   }

   // Отправляет запрошенные NBT-данные кита обратно клиенту
   public static void handle(PacketRequestKitData msg, Supplier<Context> ctx) {
      ctx.get().enqueueWork(() -> {
         ServerPlayer player = ctx.get().getSender();
         if (player != null && player.isCreative()) {
            WarfareWorldData data = WarfareWorldData.get(player.serverLevel());
            Map<String, CompoundTag> toSend = new HashMap<>();
            if (msg.kitName.equals("ALL")) {
               Map<String, WarfareWorldData.KitInfo> source = msg.team.equals("BLUE") ? data.blueKits : data.redKits;
               source.forEach((name, kitx) -> toSend.put(name, kitx.save()));
            } else {
               WarfareWorldData.KitInfo kit = msg.team.equals("BLUE") ? data.blueKits.get(msg.kitName) : data.redKits.get(msg.kitName);
               if (kit != null) {
                  toSend.put(msg.kitName, kit.save());
               }
            }

            PacketHandler.INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), new PacketSendKitData(toSend));
         }
      });
      ctx.get().setPacketHandled(true);
   }
}
