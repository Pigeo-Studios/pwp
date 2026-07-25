package com.pwp.coreclient;

import com.pwp.coreclient.network.PacketHandler;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.common.Mod;
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
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::commonSetup);
        if (FMLEnvironment.dist == Dist.CLIENT) {
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
