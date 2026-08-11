package com.pwp.coreclient;

import com.pwp.coreclient.network.PacketHandler;
import com.pwp.coreclient.particles.DonorFxConfig;
import com.pwp.coreclient.particles.PwpParticleTypes;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(CoreClientMod.MODID)
public class CoreClientMod {

    public static final String MODID = "pwp_core_client";
    private static final Logger log = LoggerFactory.getLogger(CoreClientMod.class);

    public CoreClientMod() {
        log.error("CORE CLIENT MOD CONSTRUCTOR CALLED");
        log.error("MODID=" + MODID);
        PwpParticleTypes.register(FMLJavaModLoadingContext.get().getModEventBus());
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::commonSetup);
        if (FMLEnvironment.dist == Dist.CLIENT) {
            ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, DonorFxConfig.SPEC, "pwp_core_client/donor_fx.toml");
            ClientScreenHandler.init();
        }
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        log.error("CORE CLIENT MOD commonSetup CALLED");
        event.enqueueWork(() -> {
            log.error("PACKET HANDLER REGISTRATION START");
            PacketHandler.register();
            log.error("PACKET HANDLER REGISTRATION COMPLETE");
        });
    }
}
