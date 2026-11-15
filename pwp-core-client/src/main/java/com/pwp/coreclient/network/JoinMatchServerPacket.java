package com.pwp.coreclient.network;

import com.pwp.api.lobby.LobbyActions;
import com.pwp.api.net.Net;
import com.pwp.api.net.Validate;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

/** C→S: перейти в матч с данным id сервера (кнопка в лобби). На матч-сервере игнорируется. */
public record JoinMatchServerPacket(int serverId) implements Net.C2S {

    public static void encode(JoinMatchServerPacket msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.serverId);
    }

    public static JoinMatchServerPacket decode(FriendlyByteBuf buf) {
        return new JoinMatchServerPacket(buf.readInt());
    }

    @Override
    public String validate() {
        return Validate.range("serverId", serverId, 0, 10_000);
    }

    @Override
    public void handle(ServerPlayer sender) {
        LobbyActions lobby = LobbyActions.get();
        if (lobby != null) lobby.joinMatchById(serverId, sender);
    }
}
