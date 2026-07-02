package com.pigeostudios.pwp.warfare.network;

import com.pigeostudios.pwp.warfare.entity.SupplyCrateEntity;
import java.util.function.Supplier;
import net.minecraft.ChatFormatting;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.network.NetworkEvent.Context;

// РџР°РєРµС‚ СЃР±СЂРѕСЃР° СЏС‰РёРєР° СЃРЅР°Р±Р¶РµРЅРёСЏ РёР· РіСЂСѓР·РѕРІРёРєР° СЃРЅР°Р±Р¶РµРЅРёСЏ
// РћС‚РїСЂР°РІР»СЏРµС‚СЃСЏ РІРѕРґРёС‚РµР»РµРј РіСЂСѓР·РѕРІРёРєР° РїСЂРё РЅР°Р¶Р°С‚РёРё РєРЅРѕРїРєРё СЃР±СЂРѕСЃР°
public class PacketDropCrate {
   public static void encode(PacketDropCrate msg, FriendlyByteBuf buf) {
   }

   public static PacketDropCrate decode(FriendlyByteBuf buf) {
      return new PacketDropCrate();
   }

   // РџСЂРѕРІРµСЂСЏРµС‚, РЅР°С…РѕРґРёС‚СЃСЏ Р»Рё РёРіСЂРѕРє РІ РіСЂСѓР·РѕРІРёРєРµ СЃРЅР°Р±Р¶РµРЅРёСЏ, Рё СЃРѕР·РґР°С‘С‚ СЏС‰РёРє
   public static void handle(PacketDropCrate msg, Supplier<Context> ctx) {
      ctx.get().enqueueWork(() -> {
         ServerPlayer player = ctx.get().getSender();
         if (player != null) {
            Entity vehicle = player.getVehicle();
            if (vehicle != null && vehicle.getPersistentData().getBoolean("WARFARE_IsSupplyTruck")) {
               if (vehicle.getFirstPassenger() != player) {
                  player.displayClientMessage(Component.literal("Only the driver can drop supplies!").withStyle(ChatFormatting.RED), true);
                  return;
               }

               int ammo = vehicle.getPersistentData().getInt("WARFARE_SupplyAmmo");
               if (ammo > 0) {
                  vehicle.getPersistentData().putInt("WARFARE_SupplyAmmo", ammo - 1);
                  String team = vehicle.getPersistentData().getString("WARFARE_VehicleTeam");
                  if (team.isEmpty()) {
                     team = "NEUTRAL";
                  }

                  double yawRad = Math.toRadians(vehicle.getYRot());
                  double x = vehicle.getX() + Math.sin(yawRad) * 2.5;
                  double z = vehicle.getZ() - Math.cos(yawRad) * 2.5;
                  double y = vehicle.getY() + 1.5;
                  SupplyCrateEntity crate = new SupplyCrateEntity(player.level(), x, y, z, team, player.getUUID());
                  player.level().addFreshEntity(crate);
                  player.displayClientMessage(Component.literal("Supply Crate Dropped!").withStyle(ChatFormatting.YELLOW), true);
               } else {
                  player.displayClientMessage(Component.literal("No Supplies! Return to Main Base.").withStyle(ChatFormatting.RED), true);
               }
            }
         }
      });
      ctx.get().setPacketHandled(true);
   }
}
