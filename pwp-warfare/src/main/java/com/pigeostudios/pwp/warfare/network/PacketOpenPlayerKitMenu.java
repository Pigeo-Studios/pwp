package com.pigeostudios.pwp.warfare.network;

import com.pigeostudios.pwp.warfare.client.ClientHooks;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent.Context;

// Пакет открытия меню выбора набора (кита) для игрока
// Отправляется сервером клиенту со списком доступных китов
public class PacketOpenPlayerKitMenu {
   public final List<PacketOpenPlayerKitMenu.KitDTO> kits;

   public PacketOpenPlayerKitMenu(List<PacketOpenPlayerKitMenu.KitDTO> kits) {
      this.kits = kits;
   }

   public static void encode(PacketOpenPlayerKitMenu msg, FriendlyByteBuf buf) {
      buf.writeInt(msg.kits.size());

      for (PacketOpenPlayerKitMenu.KitDTO k : msg.kits) {
         buf.writeUtf(k.name);
         buf.writeBoolean(k.available);
         buf.writeUtf(k.reason);
         buf.writeInt(k.items.size());

         for (ItemStack stack : k.items) {
            buf.writeItem(stack);
         }
      }
   }

   public static PacketOpenPlayerKitMenu decode(FriendlyByteBuf buf) {
      int size = buf.readInt();
      List<PacketOpenPlayerKitMenu.KitDTO> list = new ArrayList<>();

      for (int i = 0; i < size; i++) {
         String name = buf.readUtf();
         boolean avail = buf.readBoolean();
         String reason = buf.readUtf();
         int itemCount = buf.readInt();
         List<ItemStack> items = new ArrayList<>();

         for (int j = 0; j < itemCount; j++) {
            items.add(buf.readItem());
         }

         list.add(new PacketOpenPlayerKitMenu.KitDTO(name, avail, reason, items));
      }

      return new PacketOpenPlayerKitMenu(list);
   }

   // Открывает на клиенте GUI выбора кита с переданным списком
   public static void handle(PacketOpenPlayerKitMenu msg, Supplier<Context> ctx) {
      ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientHooks.openPlayerKitMenu(msg.kits)));
      ctx.get().setPacketHandled(true);
   }

   // DTO для передачи информации о ките: название, доступность, предметы
   public static class KitDTO {
      public String name;
      public boolean available;
      public String reason;
      public List<ItemStack> items;

      public KitDTO(String n, boolean a, String r, List<ItemStack> items) {
         this.name = n;
         this.available = a;
         this.reason = r;
         this.items = items;
      }
   }
}
