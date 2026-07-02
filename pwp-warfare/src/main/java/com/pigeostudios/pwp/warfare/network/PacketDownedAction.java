package com.pigeostudios.pwp.warfare.network;

import com.pigeostudios.pwp.warfare.events.DownedHandler;
import com.pigeostudios.pwp.warfare.sound.ModSounds;
import com.pigeostudios.pwp.warfare.world.WarfareWorldData;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.network.NetworkEvent.Context;

// РџР°РєРµС‚ РґРµР№СЃС‚РІРёСЏ РІ СЃРѕСЃС‚РѕСЏРЅРёРё "СЂР°РЅРµРЅ": СЃРґР°С‚СЊСЃСЏ РёР»Рё РїРѕР·РІР°С‚СЊ РјРµРґРёРєР°
// РћС‚РїСЂР°РІР»СЏРµС‚СЃСЏ РєР»РёРµРЅС‚РѕРј РїСЂРё РЅР°Р¶Р°С‚РёРё СЃРѕРѕС‚РІРµС‚СЃС‚РІСѓСЋС‰РёС… РєРЅРѕРїРѕРє
public class PacketDownedAction {
   private final int action;

   public PacketDownedAction(int action) {
      this.action = action;
   }

   public static void encode(PacketDownedAction msg, FriendlyByteBuf buf) {
      buf.writeInt(msg.action);
   }

   public static PacketDownedAction decode(FriendlyByteBuf buf) {
      return new PacketDownedAction(buf.readInt());
   }

   // РћР±СЂР°Р±Р°С‚С‹РІР°РµС‚ РґРµР№СЃС‚РІРёРµ: 0 - РєСЂРёРє Рѕ РїРѕРјРѕС‰Рё, 1 - РїСЂРёРЅСѓРґРёС‚РµР»СЊРЅР°СЏ СЃРґР°С‡Р°
   public static void handle(PacketDownedAction msg, Supplier<Context> ctx) {
      ctx.get().enqueueWork(() -> {
         ServerPlayer player = ctx.get().getSender();
         if (player != null) {
            ServerLevel level = (ServerLevel)player.level();
            if (msg.action == 1) {
               DownedHandler.forceGiveUp(player);
            } else if (msg.action == 0) {
               long currentTime = level.getGameTime();
               long lastCall = player.getPersistentData().getLong("WARFARE_LastMedicShout");
               if (currentTime - lastCall >= 300L) {
                  player.getPersistentData().putLong("WARFARE_LastMedicShout", currentTime);
                  player.getPersistentData().putLong("WARFARE_LastMedicShoutTimeMS", level.getGameTime());
                  WarfareWorldData data = WarfareWorldData.get(level);
                  String faction = "none";
                  if (player.getTeam() != null) {
                     String teamName = player.getTeam().getName();
                     faction = teamName.equalsIgnoreCase("Blue") ? data.blueFaction : data.redFaction;
                  }

                  SoundEvent finalSound;
                  if (faction != null && !faction.isEmpty() && !faction.equalsIgnoreCase("none")) {
                     if (ModSounds.FACTION_SCREAMS.containsKey(faction.toLowerCase())) {
                        int randomIndex = player.getRandom().nextInt(3);
                        finalSound = (SoundEvent)ModSounds.FACTION_SCREAMS.get(faction.toLowerCase()).get(randomIndex).get();
                     } else {
                        finalSound = (SoundEvent)ModSounds.HELP_SCREAM.get();
                     }
                  } else {
                     finalSound = (SoundEvent)ModSounds.HELP_SCREAM.get();
                  }

                  level.playSound(null, player.getX(), player.getY(), player.getZ(), finalSound, SoundSource.PLAYERS, 2.0F, 1.0F);
               }
            }
         }
      });
      ctx.get().setPacketHandled(true);
   }
}
