package com.pigeostudios.pwp.medicine.effect;

import com.pigeostudios.pwp.medicine.effect.BleedingEffect;
import net.minecraft.world.effect.MobEffect;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.IForgeRegistry;
import net.minecraftforge.registries.RegistryObject;

// Регистрация эффектов мода PWP Medicine.
public class ModEffects {
    // Регистратор моб-эффектов с modid pwp_medicine
    public static final DeferredRegister<MobEffect> MOB_EFFECTS = DeferredRegister.create((IForgeRegistry)ForgeRegistries.MOB_EFFECTS, (String)"pwp_medicine");
    // Эффект кровотечения
    public static final RegistryObject<MobEffect> BLEEDING = MOB_EFFECTS.register("bleeding", () -> new BleedingEffect());

    public static void register(IEventBus eventBus) {
        MOB_EFFECTS.register(eventBus);
    }
}
