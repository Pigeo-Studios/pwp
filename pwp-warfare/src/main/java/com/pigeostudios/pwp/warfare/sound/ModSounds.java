package com.pigeostudios.pwp.warfare.sound;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

// Реестр звуковых событий мода
// Регистрация всех звуков через DeferredRegister
public class ModSounds {
   public static final DeferredRegister<SoundEvent> SOUND_EVENTS = DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, "pwpwarfare");
   public static final RegistryObject<SoundEvent> RALLY_IDLE = registerSoundEvent("rally_idle");
   public static final RegistryObject<SoundEvent> HUB_IDLE = registerSoundEvent("hub_idle");
   public static final RegistryObject<SoundEvent> M2_LOAD = registerSoundEvent("m2_load");
   public static final RegistryObject<SoundEvent> M2_UNLOAD = registerSoundEvent("m2_unload");
   public static final RegistryObject<SoundEvent> SIREN_ALARM = registerSoundEvent("siren_alarm");
   public static final RegistryObject<SoundEvent> PLAYER_DOWNED = registerSoundEvent("player_downed");
   public static final RegistryObject<SoundEvent> AGS_SHOOT = registerSoundEvent("ags_shoot");
   public static final RegistryObject<SoundEvent> HELP_SCREAM = registerSoundEvent("help_scream");
   public static final RegistryObject<SoundEvent> RADIO_OPEN = registerSoundEvent("radio_open");
   public static final RegistryObject<SoundEvent> M2_SHOOT = registerSoundEvent("m2_shoot");
   public static final RegistryObject<SoundEvent> SHOVEL_DIG = registerSoundEvent("shovel_dig");
   public static final Map<String, List<RegistryObject<SoundEvent>>> FACTION_SCREAMS = new HashMap<>();

   private static RegistryObject<SoundEvent> registerSoundEvent(String name) {
      return SOUND_EVENTS.register(name, () -> SoundEvent.createVariableRangeEvent(new ResourceLocation("pwpwarfare", name)));
   }

   public static void register(IEventBus eventBus) {
      SOUND_EVENTS.register(eventBus);
   }

   static {
      String[] factions = new String[]{"ukraine", "russia", "usa", "nato", "bluefor", "redfor", "insurgency", "pmc", "army"};

      for (String faction : factions) {
         List<RegistryObject<SoundEvent>> screams = new ArrayList<>();

         for (int i = 1; i <= 3; i++) {
            screams.add(registerSoundEvent(faction + "_scream_" + i));
         }

         FACTION_SCREAMS.put(faction, screams);
      }
   }
}
