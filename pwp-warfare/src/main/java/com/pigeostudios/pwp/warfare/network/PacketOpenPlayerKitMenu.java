package com.pigeostudios.pwp.warfare.network;

import com.pigeostudios.pwp.warfare.client.ClientHooks;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
   public final Map<String, Integer> playerSelections;

   public PacketOpenPlayerKitMenu(List<PacketOpenPlayerKitMenu.KitDTO> kits) {
      this(kits, Map.of());
   }

   public PacketOpenPlayerKitMenu(List<PacketOpenPlayerKitMenu.KitDTO> kits, Map<String, Integer> playerSelections) {
      this.kits = kits;
      this.playerSelections = playerSelections != null ? playerSelections : Map.of();
   }

     public static void encode(PacketOpenPlayerKitMenu msg, FriendlyByteBuf buf) {
        buf.writeByte(4);
        buf.writeInt(msg.kits.size());

       for (PacketOpenPlayerKitMenu.KitDTO k : msg.kits) {
          buf.writeUtf(k.name);
          buf.writeUtf(k.category != null ? k.category : "INFANTRY");
          buf.writeUtf(k.description != null ? k.description : "");
          buf.writeBoolean(k.available);
          buf.writeUtf(k.reason);
          buf.writeBoolean(k.isSelected);
          // Счётчики лимитов ролей (v4): в команде и в отряде
          buf.writeInt(k.inTeamCount);
          buf.writeInt(k.maxInTeam);
          buf.writeInt(k.inSquadCount);
          buf.writeInt(k.maxInSquad);
          buf.writeInt(k.items.size());

          for (ItemStack stack : k.items) {
             buf.writeItem(stack);
          }

          // slotSkins
          Map<Integer, List<String>> ss = k.slotSkins != null ? k.slotSkins : Map.of();
          buf.writeInt(ss.size());
          for (Map.Entry<Integer, List<String>> e : ss.entrySet()) {
             buf.writeInt(e.getKey());
             buf.writeInt(e.getValue().size());
             for (String s : e.getValue()) buf.writeUtf(s);
          }
       }

        // playerSelections (version >= 3)
        buf.writeInt(msg.playerSelections.size());
        for (Map.Entry<String, Integer> e : msg.playerSelections.entrySet()) {
           buf.writeUtf(e.getKey());
           buf.writeInt(e.getValue());
        }
     }

     public static PacketOpenPlayerKitMenu decode(FriendlyByteBuf buf) {
        int version = buf.readableBytes() > 0 ? buf.readByte() : 1;
        int size = buf.readInt();
        List<PacketOpenPlayerKitMenu.KitDTO> list = new ArrayList<>();

       for (int i = 0; i < size; i++) {
          String name = buf.readUtf();
          String category = version >= 2 ? buf.readUtf() : "INFANTRY";
          String description = version >= 2 ? buf.readUtf() : "";
boolean avail = buf.readBoolean();
           String reason = buf.readUtf();
           boolean isSelected = buf.readBoolean();
           int inTeam = version >= 4 ? buf.readInt() : 0;
           int maxTeam = version >= 4 ? buf.readInt() : -1;
           int inSquad = version >= 4 ? buf.readInt() : 0;
           int maxSquad = version >= 4 ? buf.readInt() : -1;
           int itemCount = buf.readInt();
           List<ItemStack> items = new ArrayList<>();

           for (int j = 0; j < itemCount; j++) {
              items.add(buf.readItem());
           }

           PacketOpenPlayerKitMenu.KitDTO dto = new PacketOpenPlayerKitMenu.KitDTO(name, category, description, avail, reason, isSelected, items);
           dto.inTeamCount = inTeam;
           dto.maxInTeam = maxTeam;
           dto.inSquadCount = inSquad;
           dto.maxInSquad = maxSquad;
          // slotSkins
          int ssSize = buf.readInt();
          for (int j = 0; j < ssSize; j++) {
             int slot = buf.readInt();
             int listSize = buf.readInt();
             List<String> values = new ArrayList<>();
             for (int k = 0; k < listSize; k++) values.add(buf.readUtf());
             dto.slotSkins.put(slot, values);
          }
          list.add(dto);
       }

        Map<String, Integer> playerSelections = Map.of();
        if (version >= 3) {
           int psSize = buf.readInt();
           playerSelections = new HashMap<>();
           for (int i = 0; i < psSize; i++) {
              playerSelections.put(buf.readUtf(), buf.readInt());
           }
        }

       return new PacketOpenPlayerKitMenu(list, playerSelections);
    }

   // Открывает на клиенте GUI выбора кита с переданным списком
    public static void handle(PacketOpenPlayerKitMenu msg, Supplier<Context> ctx) {
       ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientHooks.openPlayerKitMenu(msg)));
       ctx.get().setPacketHandled(true);
    }

    // DTO для передачи информации о ките: название, доступность, предметы
     public static class KitDTO {
        public String name;
        public String category;
        public String description;
        public boolean available;
        public String reason;
        public boolean isSelected;
        public List<ItemStack> items;
        public Map<Integer, List<String>> slotSkins = new HashMap<>();
        /** Сколько игроков команды уже используют эту роль (для бейджа x/y). */
        public int inTeamCount;
        /** Лимит роли на команду (0 = нет лимита). */
        public int maxInTeam;
        /** Сколько игроков отряда уже использует роль. */
        public int inSquadCount;
        /** Лимит роли на отряд (0 = нет лимита). */
        public int maxInSquad;

        public KitDTO(String n, String cat, String desc, boolean a, String r, boolean sel, List<ItemStack> items) {
           this.name = n;
           this.category = cat;
           this.description = desc;
           this.available = a;
           this.reason = r;
           this.isSelected = sel;
           this.items = items;
        }
     }
}
