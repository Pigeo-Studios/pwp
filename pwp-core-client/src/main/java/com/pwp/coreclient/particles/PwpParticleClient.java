package com.pwp.coreclient.particles;

import com.pwp.coreclient.CoreClientMod;
import com.pwp.coreclient.donor.DonorLevel;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Регистрация клиентских провайдеров частиц свечения (по уровню — свой DonorLevel). */
@Mod.EventBusSubscriber(modid = CoreClientMod.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class PwpParticleClient {

    private PwpParticleClient() {}

    @SubscribeEvent
    public static void onRegisterParticles(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(PwpParticleTypes.SILVER.get(), new GlowParticle.Provider(DonorLevel.SILVER));
        event.registerSpriteSet(PwpParticleTypes.GOLD.get(), new GlowParticle.Provider(DonorLevel.GOLD));
        event.registerSpriteSet(PwpParticleTypes.PLATINUM.get(), new GlowParticle.Provider(DonorLevel.PLATINUM));
        event.registerSpriteSet(PwpParticleTypes.MODERATOR.get(), new GlowParticle.Provider(DonorLevel.MODERATOR));
        event.registerSpriteSet(PwpParticleTypes.ADMIN.get(), new GlowParticle.Provider(DonorLevel.ADMIN));
    }
}
