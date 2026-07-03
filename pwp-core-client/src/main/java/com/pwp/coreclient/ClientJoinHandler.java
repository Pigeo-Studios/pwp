package com.pwp.coreclient;

import com.google.gson.JsonObject;
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

        String uuid = mc.player.getStringUUID();

        new Thread(() -> {
            try {
                Thread.sleep(1000);
                JsonObject profile = CoreAPI.loadPlayer(uuid);
                if (profile != null && profile.has("success") && profile.get("success").getAsBoolean() && profile.has("data")) {
                    JsonObject data = profile.getAsJsonObject("data");
                    PlayerData.put(mc.player.getUUID(), data);
                    log.info("Player profile cached for {}", uuid);
                } else {
                    log.warn("Player not found, creating...");
                    JsonObject created = CoreAPI.createPlayer(uuid, mc.player.getScoreboardName());
                    if (created != null && created.has("success") && created.get("success").getAsBoolean()) {
                        Thread.sleep(500);
                        JsonObject retry = CoreAPI.loadPlayer(uuid);
                        if (retry != null && retry.has("data")) {
                            PlayerData.put(mc.player.getUUID(), retry.getAsJsonObject("data"));
                            log.info("Player created and cached for {}", uuid);
                        }
                    }
                }
            } catch (Exception e) {
                log.warn("Could not load player profile: {}", e.getMessage());
            }
        }, "PWP-Profile-Loader").start();
    }
}
