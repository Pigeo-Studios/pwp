package com.pigeostudios.pwp.warfare.voicechat;

import net.minecraftforge.fml.ModList;

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
}
