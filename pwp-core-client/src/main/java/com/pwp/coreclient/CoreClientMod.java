package com.pwp.coreclient;

import com.pwp.coreclient.network.PacketHandler;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(CoreClientMod.MODID)
public class CoreClientMod {

    public static final String MODID = "pwp_core_client";
    public static final String CORE_API_URL = "http://localhost:8080";
    public static final String CORE_API_KEY = "pwp_server_key_change_me";

    public CoreClientMod() {
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::commonSetup);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(PacketHandler::register);
    }
}
