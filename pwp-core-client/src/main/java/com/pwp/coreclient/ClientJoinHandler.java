package com.pwp.coreclient;

import com.pwp.coreclient.network.LobbyAuthPacket;
import com.pwp.coreclient.network.MatchAuthPacket;
import com.pwp.coreclient.network.ClientConnectHandler;
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
    public static void onClientLogin(ClientPlayerNetworkEvent.LoggingIn event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        String uuid = mc.player.getStringUUID();
        log.info("CLIENT LOGIN EVENT — player={}", uuid);

        String sessionId = ClientConnectHandler.pendingSessionId;
        ClientConnectHandler.pendingSessionId = null;

        // Try authToken (v2), fallback to sessionToken (v1 legacy)
        String token = System.getProperty("pwp.authToken", "");
        if (token.isEmpty()) {
            token = System.getProperty("pwp.sessionToken", "");
        }
        if (sessionId != null && !sessionId.isEmpty()) {
            log.info("SENDING MATCH AUTH — token (via sessionId) length={}", token.length());
            PacketHandler.INSTANCE.sendToServer(new MatchAuthPacket(token));
            log.info("MATCH AUTH SENT");
        } else if (!token.isEmpty()) {
            log.info("SENDING LOBBY AUTH — token.length={}", token.length());
            PacketHandler.INSTANCE.sendToServer(new LobbyAuthPacket(token));
            log.info("LOBBY AUTH SENT");
        } else {
            log.warn("NO AUTH — no pwp.authToken set");
        }

        new Thread(() -> {
            try {
                Thread.sleep(2000);
                PacketHandler.INSTANCE.sendToServer(new PacketDataRequest("profile", ""));
                log.info("Profile request sent for {}", uuid);
            } catch (Exception e) {
                log.warn("Could not request profile: {}", e.getMessage());
            }
        }, "PWP-Profile-Loader").start();
    }
}
