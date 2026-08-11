package com.pwp.coreclient.particles;

import com.pwp.coreclient.CoreClientMod;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Регистрация клиентских провайдеров частиц свечения (по уровню — свой профиль). */
@Mod.EventBusSubscriber(modid = CoreClientMod.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class PwpParticleClient {

    private PwpParticleClient() {}

    @SubscribeEvent
    public static void onRegisterParticles(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(PwpParticleTypes.SILVER.get(), new GlowParticle.Provider(GlowParticle.SILVER));
        event.registerSpriteSet(PwpParticleTypes.GOLD.get(), new GlowParticle.Provider(GlowParticle.GOLD));
        event.registerSpriteSet(PwpParticleTypes.PLATINUM.get(), new GlowParticle.Provider(GlowParticle.PLATINUM));
        event.registerSpriteSet(PwpParticleTypes.MODERATOR.get(), new GlowParticle.Provider(GlowParticle.MODERATOR));
        event.registerSpriteSet(PwpParticleTypes.ADMIN.get(), new GlowParticle.Provider(GlowParticle.ADMIN));
    }
}
