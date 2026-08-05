package com.pigeostudios.pwp.warfare.network;

import com.pigeostudios.pwp.warfare.client.ClientData;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent.Context;

// Синхронизация состояния канала дрона (сервер -> клиент). Шлётся раз в
// 5 тиков пока идёт установка/перезарядка: клиент рисует компактную анимацию
// (кольцо прогресса + таймер) и маркер над точкой. active=false — отмена/завершение.
// totalTicks — полное время канала (из конфига) для честного отсчёта.
// mode: 0 = установка дрона (лопасти), 1 = перезарядка мавика (без лопастей).
public class PacketDroneDeployState {
   private final boolean active;
   private final float progress;
   private final int x;
   private final int y;
   private final int z;
   private final String droneTypeId;
   private final int totalTicks;
   private final int mode;

   public PacketDroneDeployState(boolean active, float progress, int x, int y, int z, String droneTypeId, int totalTicks, int mode) {
      this.active = active;
      this.progress = progress;
      this.x = x;
      this.y = y;
      this.z = z;
      this.droneTypeId = droneTypeId == null ? "" : droneTypeId;
      this.totalTicks = totalTicks;
      this.mode = mode;
   }

   public static void encode(PacketDroneDeployState msg, FriendlyByteBuf buf) {
      buf.writeBoolean(msg.active);
      buf.writeFloat(msg.progress);
      buf.writeInt(msg.x);
      buf.writeInt(msg.y);
      buf.writeInt(msg.z);
      buf.writeUtf(msg.droneTypeId);
      buf.writeInt(msg.totalTicks);
      buf.writeInt(msg.mode);
   }

   public static PacketDroneDeployState decode(FriendlyByteBuf buf) {
      return new PacketDroneDeployState(buf.readBoolean(), buf.readFloat(), buf.readInt(), buf.readInt(), buf.readInt(), buf.readUtf(), buf.readInt(), buf.readInt());
   }

   public static void handle(PacketDroneDeployState msg, Supplier<Context> ctx) {
      ctx.get().enqueueWork(() -> {
         if (ctx.get().getDirection().getReceptionSide().isClient()) {
            ClientData.droneDeployActive = msg.active;
            ClientData.droneDeployProgress = msg.progress;
            ClientData.droneDeployX = msg.x;
            ClientData.droneDeployY = msg.y;
            ClientData.droneDeployZ = msg.z;
            ClientData.droneDeployTypeId = msg.droneTypeId;
            ClientData.droneDeployTotalTicks = msg.totalTicks;
            ClientData.droneDeployMode = msg.mode;
            ClientData.droneDeployLastUpdate = ClientData.clientTickCounter;
            if (!msg.active) {
               ClientData.droneDeployDisplayProgress = 0.0F;
            }
         }
      });
      ctx.get().setPacketHandled(true);
   }
}
