package com.pigeostudios.pwp.warfare.network;

import com.pigeostudios.pwp.warfare.client.ClientData;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent.Context;

// Пакет статуса техники (сервер -> клиент, пассажирам). Отправляется раз в 20 тиков
// из MainSupplyBlockEntity/SupplyCrateEntity вместо спама в actionbar. Клиент сам
// тикает обратный таймер вниз и рисует статус в едином HUD-блоке техники.
public class PacketVehicleStatus {
   public static final int KIND_NONE = 0;
   public static final int KIND_REARMING = 1;
   public static final int KIND_LOADING_CRATE = 2;
   public static final int KIND_REARM_COOLDOWN = 3;
   public static final int KIND_CRATE_RESUPPLY = 4;

   private final int entityId;
   private final int kind;
   private final int secondsLeft;

   public PacketVehicleStatus(int entityId, int kind, int secondsLeft) {
      this.entityId = entityId;
      this.kind = kind;
      this.secondsLeft = secondsLeft;
   }

   public static void encode(PacketVehicleStatus msg, FriendlyByteBuf buf) {
      buf.writeInt(msg.entityId);
      buf.writeInt(msg.kind);
      buf.writeInt(msg.secondsLeft);
   }

   public static PacketVehicleStatus decode(FriendlyByteBuf buf) {
      return new PacketVehicleStatus(buf.readInt(), buf.readInt(), buf.readInt());
   }

   public static void handle(PacketVehicleStatus msg, Supplier<Context> ctx) {
      ctx.get().enqueueWork(() -> {
         if (ctx.get().getDirection().getReceptionSide().isClient()) {
            if (msg.kind == KIND_NONE) {
               ClientData.vehicleStatuses.remove(msg.entityId);
            } else {
               ClientData.vehicleStatuses.put(msg.entityId, new ClientData.VehicleStatus(msg.kind, msg.secondsLeft));
            }
         }
      });
      ctx.get().setPacketHandled(true);
   }
}
