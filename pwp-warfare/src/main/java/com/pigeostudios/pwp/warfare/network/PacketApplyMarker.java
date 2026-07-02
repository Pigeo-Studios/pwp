package com.pigeostudios.pwp.warfare.network;

import com.pigeostudios.pwp.warfare.config.WarfareConfig;
import com.pigeostudios.pwp.warfare.item.SupplyTruckMarkerItem;
import com.pigeostudios.pwp.warfare.item.VehicleMarkerItem;
import com.pigeostudios.pwp.warfare.world.WarfareWorldData;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent.Context;

// РџР°РєРµС‚ РїСЂРёРјРµРЅРµРЅРёСЏ РјР°СЂРєРµСЂР° С‚СЂР°РЅСЃРїРѕСЂС‚РЅРѕРіРѕ СЃСЂРµРґСЃС‚РІР° РёР»Рё РіСЂСѓР·РѕРІРёРєР° СЃРЅР°Р±Р¶РµРЅРёСЏ
// РћС‚РїСЂР°РІР»СЏРµС‚СЃСЏ РєР»РёРµРЅС‚РѕРј РїСЂРё РёСЃРїРѕР»СЊР·РѕРІР°РЅРёРё РјР°СЂРєРµСЂР° РЅР° СЃСѓС‰РЅРѕСЃС‚Рё
public class PacketApplyMarker {
   private final int targetEntityId;

   public PacketApplyMarker(int entityId) {
      this.targetEntityId = entityId;
   }

   public static void encode(PacketApplyMarker msg, FriendlyByteBuf buf) {
      buf.writeInt(msg.targetEntityId);
   }

   public static PacketApplyMarker decode(FriendlyByteBuf buf) {
      return new PacketApplyMarker(buf.readInt());
   }

   // РћР±СЂР°Р±Р°С‚С‹РІР°РµС‚ РїСЂРёРјРµРЅРµРЅРёРµ РјР°СЂРєРµСЂР°: РїСЂРѕРІРµСЂСЏРµС‚ РґРёСЃС‚Р°РЅС†РёСЋ, С‚РёРї РїСЂРµРґРјРµС‚Р°,
   // Р·Р°РїРёСЃС‹РІР°РµС‚ РґР°РЅРЅС‹Рµ Рѕ С‚СЂР°РЅСЃРїРѕСЂС‚РЅРѕРј СЃСЂРµРґСЃС‚РІРµ РІ РјРёСЂ Рё СЃРёРЅС…СЂРѕРЅРёР·РёСЂСѓРµС‚ СЃ РєР»РёРµРЅС‚Р°РјРё
   public static void handle(PacketApplyMarker msg, Supplier<Context> ctx) {
      ctx.get()
         .enqueueWork(
            () -> {
               ServerPlayer player = ctx.get().getSender();
               if (player != null) {
                  ServerLevel level = player.serverLevel();
                  Entity target = level.getEntity(msg.targetEntityId);
                  ItemStack stack = player.getMainHandItem();
                  WarfareWorldData data = WarfareWorldData.get(level);
                  if (target != null && player.distanceTo(target) < 10.0F) {
                     String team = "";
                     String type = "";
                     int penalty = 0;
                     int maxMats = 0;
                     boolean isSupply = false;
                     if (stack.getItem() instanceof VehicleMarkerItem markerItem) {
                        team = markerItem.getTeam();
                        type = markerItem.getType();
                        penalty = markerItem.getPenalty();
                        maxMats = markerItem.getMaxMats();
                     } else if (stack.getItem() instanceof SupplyTruckMarkerItem supplyItem) {
                        team = supplyItem.getTeam();
                        type = supplyItem.getVehicleType();
                        penalty = supplyItem.getPenalty();
                        maxMats = supplyItem.getMaxMats();
                        isSupply = true;
                     }

                     if (!team.isEmpty()) {
                        target.getPersistentData().putString("WARFARE_VehicleTeam", team);
                        target.getPersistentData().putString("WARFARE_VehicleType", type);
                        target.getPersistentData().putInt("WARFARE_TicketPenalty", penalty);
                        if (isSupply) {
                           target.getPersistentData().putBoolean("WARFARE_IsSupplyTruck", true);
                           target.getPersistentData().putInt("WARFARE_SupplyAmmo", (Integer)WarfareConfig.SUPPLY_TRUCK_CRATES.get());
                        } else {
                           target.getPersistentData().remove("WARFARE_IsSupplyTruck");
                           target.getPersistentData().remove("WARFARE_SupplyAmmo");
                        }

                        if (maxMats > 0) {
                           target.getPersistentData().putInt("WARFARE_VehicleMaxMats", maxMats);
                           target.getPersistentData().putInt("WARFARE_VehicleMats", maxMats);
                        }

                        data.markedVehicles.removeIf(v -> v.uuid.equals(target.getUUID()));
                        BlockPos spawnerPos = null;
                        if (target.getPersistentData().contains("WARFARE_SpawnerPos")) {
                           spawnerPos = BlockPos.of(target.getPersistentData().getLong("WARFARE_SpawnerPos"));
                        }

                        data.markedVehicles
                           .add(
                              new WarfareWorldData.VehicleRecord(
                                 target.getUUID(), team, type, target.getX(), target.getY(), target.getZ(), target.getYRot(), spawnerPos
                              )
                           );
                        PacketHandler.sendToAllClients(level, data);
                     }
                  }
               }
            }
         );
      ctx.get().setPacketHandled(true);
   }
}
