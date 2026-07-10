package com.pwp.coreclient;

import com.pwp.coreclient.network.PacketDataRequest;
import com.pwp.coreclient.network.PacketHandler;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod.EventBusSubscriber(modid = CoreClientMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class ClientJoinHandler {

    private static final Logger log = LoggerFactory.getLogger(ClientJoinHandler.class);

    @SubscribeEvent
    public void onClientLogin(ClientPlayerNetworkEvent.LoggingIn event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        new Thread(() -> {
            try {
                Thread.sleep(2000);
                PacketHandler.INSTANCE.sendToServer(new PacketDataRequest("profile", ""));
                log.info("Profile request sent for {}", mc.player.getStringUUID());
            } catch (Exception e) {
                log.warn("Could not request profile: {}", e.getMessage());
            }
        }, "PWP-Profile-Loader").start();
    }
}
