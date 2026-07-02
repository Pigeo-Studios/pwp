package com.pigeostudios.pwp.medicine.sound;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.IForgeRegistry;
import net.minecraftforge.registries.RegistryObject;

// Регистрация звуков мода PWP Medicine.
public class ModSounds {
    // Регистратор звуков с modid pwp_medicine
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS = DeferredRegister.create((IForgeRegistry)ForgeRegistries.SOUND_EVENTS, (String)"pwp_medicine");
    // Звук начала наложения бинта
    public static final RegistryObject<SoundEvent> BANDAGE_START = ModSounds.registerSoundEvent("bandage_start");
    // Звук завершения наложения бинта
    public static final RegistryObject<SoundEvent> BANDAGE_FINISH = ModSounds.registerSoundEvent("bandage_finish");
    // Звук применения аптечки (каждый тик лечения)
    public static final RegistryObject<SoundEvent> MEDKIT_APPLY = ModSounds.registerSoundEvent("medkit_apply");

    // Вспомогательный метод для регистрации звука
    private static RegistryObject<SoundEvent> registerSoundEvent(String name) {
        return SOUND_EVENTS.register(name, () -> SoundEvent.createVariableRangeEvent(new ResourceLocation("pwp_medicine", name)));
    }

    public static void register(IEventBus eventBus) {
        SOUND_EVENTS.register(eventBus);
    }
}
