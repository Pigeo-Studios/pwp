package com.pwp.coreclient;

import com.pwp.coreclient.gui.screens.PWPMainMenuScreen;
import com.pwp.coreclient.network.PacketHandler;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.client.event.ScreenEvent;
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
        MinecraftForge.EVENT_BUS.addListener(EventPriority.HIGHEST, this::onScreenOpen);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        log.error("CORE CLIENT MOD commonSetup CALLED");
        event.enqueueWork(() -> {
            log.error("PACKET HANDLER REGISTRATION START");
            PacketHandler.register();
            log.error("PACKET HANDLER REGISTRATION COMPLETE");
        });
    }

    private void onScreenOpen(ScreenEvent.Opening event) {
        if (event.getNewScreen() instanceof TitleScreen) {
            event.setNewScreen(new PWPMainMenuScreen());
        }
    }
}
