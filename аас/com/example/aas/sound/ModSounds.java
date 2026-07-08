/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.sounds.SoundEvent
 *  net.minecraftforge.eventbus.api.IEventBus
 *  net.minecraftforge.registries.DeferredRegister
 *  net.minecraftforge.registries.ForgeRegistries
 *  net.minecraftforge.registries.IForgeRegistry
 *  net.minecraftforge.registries.RegistryObject
 */
package com.example.aas.sound;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.IForgeRegistry;
import net.minecraftforge.registries.RegistryObject;

public class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS;
    public static final RegistryObject<SoundEvent> RALLY_IDLE;
    public static final RegistryObject<SoundEvent> HUB_IDLE;
    public static final RegistryObject<SoundEvent> M2_LOAD;
    public static final RegistryObject<SoundEvent> M2_UNLOAD;
    public static final RegistryObject<SoundEvent> SIREN_ALARM;
    public static final RegistryObject<SoundEvent> PLAYER_DOWNED;
    public static final RegistryObject<SoundEvent> AGS_SHOOT;
    public static final RegistryObject<SoundEvent> HELP_SCREAM;
    public static final RegistryObject<SoundEvent> RADIO_OPEN;
    public static final RegistryObject<SoundEvent> M2_SHOOT;
    public static final RegistryObject<SoundEvent> SHOVEL_DIG;
    public static final Map<String, List<RegistryObject<SoundEvent>>> FACTION_SCREAMS;

    private static RegistryObject<SoundEvent> registerSoundEvent(String name) {
        return SOUND_EVENTS.register(name, () -> SoundEvent.m_262824_((ResourceLocation)new ResourceLocation("aas", name)));
    }

    public static void register(IEventBus eventBus) {
        SOUND_EVENTS.register(eventBus);
    }

    static {
        String[] factions;
        SOUND_EVENTS = DeferredRegister.create((IForgeRegistry)ForgeRegistries.SOUND_EVENTS, (String)"aas");
        RALLY_IDLE = ModSounds.registerSoundEvent("rally_idle");
        HUB_IDLE = ModSounds.registerSoundEvent("hub_idle");
        M2_LOAD = ModSounds.registerSoundEvent("m2_load");
        M2_UNLOAD = ModSounds.registerSoundEvent("m2_unload");
        SIREN_ALARM = ModSounds.registerSoundEvent("siren_alarm");
        PLAYER_DOWNED = ModSounds.registerSoundEvent("player_downed");
        AGS_SHOOT = ModSounds.registerSoundEvent("ags_shoot");
        HELP_SCREAM = ModSounds.registerSoundEvent("help_scream");
        RADIO_OPEN = ModSounds.registerSoundEvent("radio_open");
        M2_SHOOT = ModSounds.registerSoundEvent("m2_shoot");
        SHOVEL_DIG = ModSounds.registerSoundEvent("shovel_dig");
        FACTION_SCREAMS = new HashMap<String, List<RegistryObject<SoundEvent>>>();
        for (String faction : factions = new String[]{"ukraine", "russia", "usa", "nato", "bluefor", "redfor", "insurgency", "pmc", "army"}) {
            ArrayList<RegistryObject<SoundEvent>> screams = new ArrayList<RegistryObject<SoundEvent>>();
            for (int i = 1; i <= 3; ++i) {
                screams.add(ModSounds.registerSoundEvent(faction + "_scream_" + i));
            }
            FACTION_SCREAMS.put(faction, screams);
        }
    }
}

