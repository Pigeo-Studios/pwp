package com.pigeostudios.pwp.warfare.network;

import com.pigeostudios.pwp.warfare.config.WarfareConfig;
import com.pigeostudios.pwp.warfare.world.WarfareWorldData;
import java.util.function.Supplier;
import net.minecraft.ChatFormatting;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.network.NetworkEvent.Context;

// РџР°РєРµС‚ Р·Р°РїСЂРѕСЃР° РїРѕРїРѕР»РЅРµРЅРёСЏ РєРёС‚Р° РёР· С‚СЂР°РЅСЃРїРѕСЂС‚РЅРѕРіРѕ СЃСЂРµРґСЃС‚РІР°
// РСЃРїРѕР»СЊР·СѓРµС‚ РјР°С‚РµСЂРёР°Р»С‹ С‚СЂР°РЅСЃРїРѕСЂС‚РЅРѕРіРѕ СЃСЂРµРґСЃС‚РІР° РґР»СЏ РѕРїР»Р°С‚С‹
public class PacketRequestVehicleAmmo {
   private final int entityId;
   private final int type;

   public PacketRequestVehicleAmmo(int entityId, int type) {
      this.entityId = entityId;
      this.type = type;
   }

   public static void encode(PacketRequestVehicleAmmo msg, FriendlyByteBuf buf) {
      buf.writeInt(msg.entityId);
      buf.writeInt(msg.type);
   }

   public static PacketRequestVehicleAmmo decode(FriendlyByteBuf buf) {
      return new PacketRequestVehicleAmmo(buf.readInt(), buf.readInt());
   }

   // РџСЂРѕРІРµСЂСЏРµС‚ РјР°С‚РµСЂРёР°Р»С‹ С‚СЂР°РЅСЃРїРѕСЂС‚Р° Рё РїРѕРїРѕР»РЅСЏРµС‚ РєРёС‚ РёРіСЂРѕРєР°
   public static void handle(PacketRequestVehicleAmmo msg, Supplier<Context> ctx) {
      ctx.get().enqueueWork(() -> {
         ServerPlayer player = ctx.get().getSender();
         if (player != null) {
            Entity vehicle = player.level().getEntity(msg.entityId);
            if (vehicle != null && !(player.distanceToSqr(vehicle) > 64.0)) {
               int currentMats = vehicle.getPersistentData().getInt("WARFARE_VehicleMats");
               int cost = (Integer)WarfareConfig.HUB_RESUPPLY_COST.get();
               if (!player.isCreative() && currentMats < cost) {
                  player.displayClientMessage(Component.literal("Not enough Materials in Vehicle! (" + currentMats + ")").withStyle(ChatFormatting.RED), true);
               } else {
                  String kitName = player.getPersistentData().getString("WARFARE_CurrentKit");
                  if (!kitName.isEmpty() && !kitName.equals("Unassigned")) {
                     WarfareWorldData data = WarfareWorldData.get(player.serverLevel());
                     String t = player.getTeam() != null ? player.getTeam().getName().toUpperCase() : "NEUTRAL";
                     WarfareWorldData.KitInfo kit = t.equals("BLUE") ? data.blueKits.get(kitName) : data.redKits.get(kitName);
                     if (kit != null) {
                        if (ResupplyHandler.resupplyPlayer(player, kit, false)) {
                           int newMats = currentMats;
                           if (!player.isCreative()) {
                              newMats = currentMats - cost;
                              vehicle.getPersistentData().putInt("WARFARE_VehicleMats", newMats);
                           }

                           player.displayClientMessage(Component.literal("Kit Resupplied! Vehicle Mats: " + newMats).withStyle(ChatFormatting.GREEN), true);
                           player.level().playSound(null, player.blockPosition(), SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 1.0F, 1.0F);
                        } else {
                           player.displayClientMessage(Component.literal("Ammo already full! Vehicle Mats: " + currentMats).withStyle(ChatFormatting.YELLOW), true);
                        }
                     }
                  } else {
                     player.displayClientMessage(Component.literal("No Kit equipped!").withStyle(ChatFormatting.RED), true);
                  }
               }
            }
         }
      });
      ctx.get().setPacketHandled(true);
   }
}
