package com.pwp.coreclient.network;

import com.pwp.coreserver.PlayerConnectHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Supplier;

public class MatchAuthPacket {
    private static final Logger log = LoggerFactory.getLogger(MatchAuthPacket.class);
    private final String sessionId;

    public MatchAuthPacket(String sessionId) {
        this.sessionId = sessionId;
    }

    public static void encode(MatchAuthPacket msg, FriendlyByteBuf buf) {
        buf.writeUtf(msg.sessionId != null ? msg.sessionId : "");
    }

    public static MatchAuthPacket decode(FriendlyByteBuf buf) {
        return new MatchAuthPacket(buf.readUtf(32767));
    }

    public static void handle(MatchAuthPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;
            if (msg.sessionId == null || msg.sessionId.isEmpty()) {
                log.warn("MATCH AUTH SESSION ID EMPTY");
                return;
            }
            String uuid = player.getStringUUID();
            log.info("MATCH AUTH PACKET — uuid={}, token={}", uuid, msg.sessionId);
            PlayerConnectHandler.handleToken(uuid, msg.sessionId);
        });
        ctx.get().setPacketHandled(true);
    }
}
