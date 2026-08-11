package com.pwp.coreclient.particles;

import com.pwp.coreclient.CoreClientMod;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

/**
 * Типы частиц донат-FX — по одному на уровень (SILVER/GOLD/PLATINUM/MODERATOR/ADMIN).
 * Спавн клиентский (DonorParticleSpawner по DonatorCache), серверу пакеты не шлём —
 * но типы регистрируются в общем реестре, чтобы реестр сервера/клиента совпадал.
 */
public final class PwpParticleTypes {

    private PwpParticleTypes() {}

    public static final DeferredRegister<ParticleType<?>> PARTICLES =
            DeferredRegister.create(Registries.PARTICLE_TYPE, CoreClientMod.MODID);

    public static final RegistryObject<SimpleParticleType> SILVER =
            PARTICLES.register("donor_silver", () -> new SimpleParticleType(false));
    public static final RegistryObject<SimpleParticleType> GOLD =
            PARTICLES.register("donor_gold", () -> new SimpleParticleType(false));
    public static final RegistryObject<SimpleParticleType> PLATINUM =
            PARTICLES.register("donor_platinum", () -> new SimpleParticleType(false));
    public static final RegistryObject<SimpleParticleType> MODERATOR =
            PARTICLES.register("donor_moderator", () -> new SimpleParticleType(false));
    public static final RegistryObject<SimpleParticleType> ADMIN =
            PARTICLES.register("donor_admin", () -> new SimpleParticleType(false));

    public static void register(IEventBus bus) {
        PARTICLES.register(bus);
    }
}
