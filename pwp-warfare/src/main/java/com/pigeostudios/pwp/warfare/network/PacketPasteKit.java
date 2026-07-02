package com.pigeostudios.pwp.warfare.network;

import com.pigeostudios.pwp.warfare.world.WarfareWorldData;
import java.util.function.Supplier;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent.Context;

// Отладочный пакет: вставка скопированного набора (кита) в мир
// Используется администратором для быстрого создания/редактирования китов
public class PacketPasteKit {
   private final String team;
   private final String kitName;
   private final CompoundTag tag;

   public PacketPasteKit(String team, String kitName, CompoundTag tag) {
      this.team = team;
      this.kitName = kitName;
      this.tag = tag;
   }

   public static void encode(PacketPasteKit msg, FriendlyByteBuf buf) {
      buf.writeUtf(msg.team);
      buf.writeUtf(msg.kitName);
      buf.writeNbt(msg.tag);
   }

   public static PacketPasteKit decode(FriendlyByteBuf buf) {
      return new PacketPasteKit(buf.readUtf(), buf.readUtf(), buf.readNbt());
   }

   // Загружает NBT-данные кита и сохраняет их в мире
   public static void handle(PacketPasteKit msg, Supplier<Context> ctx) {
      ctx.get().enqueueWork(() -> {
         ServerPlayer player = ctx.get().getSender();
         if (player != null && player.isCreative()) {
            WarfareWorldData worldData = WarfareWorldData.get(player.serverLevel());
            WarfareWorldData.KitInfo newKit = WarfareWorldData.KitInfo.load(msg.tag);
            newKit.name = msg.kitName;
            if (msg.team.equals("BLUE")) {
               worldData.blueKits.put(msg.kitName, newKit);
            } else {
               worldData.redKits.put(msg.kitName, newKit);
            }

            worldData.setDirty();
            PacketHandler.sendToAllClients(player.serverLevel(), worldData);
         }
      });
      ctx.get().setPacketHandled(true);
   }
}
