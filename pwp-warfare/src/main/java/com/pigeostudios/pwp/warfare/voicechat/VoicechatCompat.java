package com.pigeostudios.pwp.warfare.voicechat;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.fml.ModList;

// РЎРѕРІРјРµСЃС‚РёРјРѕСЃС‚СЊ СЃ РјРѕРґРѕРј РіРѕР»РѕСЃРѕРІРѕРіРѕ С‡Р°С‚Р°
// Р‘РµР·РѕРїР°СЃРЅС‹Рµ РІС‹Р·РѕРІС‹ РјРµС‚РѕРґРѕРІ РїР»Р°РіРёРЅР° СЃ РѕР±СЂР°Р±РѕС‚РєРѕР№ РѕС€РёР±РѕРє Р·Р°РіСЂСѓР·РєРё
public class VoicechatCompat {
   private static volatile boolean broken = false;

   public static boolean isLoaded() {
      if (broken) {
         return false;
      }

      try {
         return ModList.get().isLoaded("voicechat");
      } catch (Throwable t) {
         System.err.println("[PWP Warfare] Failed to check ModList: " + t);
         broken = true;
         return false;
      }
   }

   public static void createAndJoinGroup(ServerPlayer player, int squadId, String squadName) {
      if (isLoaded()) {
         try {
            WarfareVoicechatPlugin.createAndJoinGroup(player, squadId, squadName);
         } catch (Throwable t) {
            System.err.println("[PWP Warfare] createAndJoinGroup crashed, disabling voicechat integration: " + t);
            broken = true;
         }
      }
   }

   public static void joinGroup(ServerPlayer player, int squadId) {
      if (isLoaded()) {
         try {
            WarfareVoicechatPlugin.joinGroup(player, squadId);
         } catch (Throwable t) {
            System.err.println("[PWP Warfare] joinGroup crashed, disabling voicechat integration: " + t);
            broken = true;
         }
      }
   }

   public static void leaveGroup(ServerPlayer player) {
      if (isLoaded()) {
         try {
            WarfareVoicechatPlugin.leaveGroup(player);
         } catch (Throwable t) {
            System.err.println("[PWP Warfare] leaveGroup crashed, disabling voicechat integration: " + t);
            broken = true;
         }
      }
   }
}
