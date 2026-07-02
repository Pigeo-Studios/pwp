package com.pigeostudios.pwp.warfare.network;

import com.pigeostudios.pwp.warfare.world.WarfareWorldData;
import java.util.function.Supplier;
import net.minecraft.ChatFormatting;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent.Context;

// РџР°РєРµС‚ РІС‹Р±РѕСЂР° РЅР°Р±РѕСЂР° (РєРёС‚Р°) РёРіСЂРѕРєРѕРј
// РЈСЃС‚Р°РЅР°РІР»РёРІР°РµС‚ РѕР¶РёРґР°СЋС‰РёР№ РєРёС‚ РґР»СЏ РїСЂРёРјРµРЅРµРЅРёСЏ РїСЂРё СЂРµСЃРїР°РІРЅРµ
public class PacketSelectKit {
   public final String kitName;

   public PacketSelectKit(String kitName) {
      this.kitName = kitName;
   }

   public static void encode(PacketSelectKit msg, FriendlyByteBuf buf) {
      buf.writeUtf(msg.kitName);
   }

   public static PacketSelectKit decode(FriendlyByteBuf buf) {
      return new PacketSelectKit(buf.readUtf());
   }

   // РЈСЃС‚Р°РЅР°РІР»РёРІР°РµС‚ WARFARE_PendingKit РІ РґР°РЅРЅС‹Рµ РёРіСЂРѕРєР° Рё СЃРёРЅС…СЂРѕРЅРёР·РёСЂСѓРµС‚ СЃ СЃРµСЂРІРµСЂРѕРј
   public static void handle(PacketSelectKit msg, Supplier<Context> ctx) {
      ctx.get()
         .enqueueWork(
            () -> {
               ServerPlayer player = ctx.get().getSender();
               if (player != null) {
                  WarfareWorldData data = WarfareWorldData.get(player.serverLevel());
                  player.getPersistentData().putString("WARFARE_PendingKit", msg.kitName);
                  PacketHandler.sendToAllClients(player.serverLevel(), data);
                   if (!data.isGameStarted) {
                      player.sendSystemMessage(
                         Component.translatable("pwpwarfare.message.kit_reserved", msg.kitName).withStyle(ChatFormatting.YELLOW)
                      );
                   } else {
                      player.sendSystemMessage(
                         Component.translatable("pwpwarfare.message.kit_selected", msg.kitName).withStyle(ChatFormatting.YELLOW)
                      );
                   }
               }
            }
         );
      ctx.get().setPacketHandled(true);
   }
}
