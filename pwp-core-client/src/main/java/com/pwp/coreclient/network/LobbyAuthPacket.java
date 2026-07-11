package com.pwp.coreclient.network;

import com.pwp.coreserver.PlayerConnectHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Supplier;

public class LobbyAuthPacket {
    private static final Logger log = LoggerFactory.getLogger(LobbyAuthPacket.class);
    private final String token;

    public LobbyAuthPacket(String token) {
        this.token = token;
    }

    public static void encode(LobbyAuthPacket msg, FriendlyByteBuf buf) {
        buf.writeUtf(msg.token != null ? msg.token : "");
    }

    public static LobbyAuthPacket decode(FriendlyByteBuf buf) {
        return new LobbyAuthPacket(buf.readUtf(32767));
    }

    public static void handle(LobbyAuthPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;
            if (msg.token == null || msg.token.isEmpty()) {
                log.warn("LOBBY AUTH TOKEN EMPTY");
                return;
            }
            String uuid = player.getStringUUID();
            log.info("LOBBY AUTH PACKET — uuid={}, tokenLen={}", uuid, msg.token.length());
            PlayerConnectHandler.handleToken(uuid, msg.token);
        });
        ctx.get().setPacketHandled(true);
    }
}
