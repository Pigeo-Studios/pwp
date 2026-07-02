package com.pigeostudios.pwp.warfare.network;

import com.pigeostudios.pwp.warfare.client.ClientData;
import com.pigeostudios.pwp.warfare.world.WarfareWorldData;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent.Context;

// Пакет синхронизации данных всех отрядов
// Отправляется сервером при любых изменениях в составах отделений
public class PacketSyncSquads {
   private final CompoundTag data;

   // Конструктор: сериализует список отрядов в CompoundTag
   public PacketSyncSquads(List<WarfareWorldData.Squad> squads) {
      this.data = new CompoundTag();
      ListTag list = new ListTag();

      for (WarfareWorldData.Squad s : squads) {
         list.add(s.save());
      }

      this.data.put("List", list);
   }

   public PacketSyncSquads(FriendlyByteBuf buf) {
      this.data = buf.readNbt();
   }

   public void encode(FriendlyByteBuf buf) {
      buf.writeNbt(this.data);
   }

   // Десериализует и обновляет список отрядов на клиенте
   public void handle(Supplier<Context> ctx) {
      ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
         ClientData.clientSquads.clear();
         ListTag list = this.data.getList("List", 10);

         for (int i = 0; i < list.size(); i++) {
            ClientData.clientSquads.add(WarfareWorldData.Squad.load(list.getCompound(i)));
         }
      }));
      ctx.get().setPacketHandled(true);
   }
}
