package com.pwp.coreclient.network.data;

import com.pwp.api.net.Net;
import com.pwp.api.net.Payloads;
import com.pwp.api.net.Validate;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

/** C→S: запрос данных для GUI (профиль, лидерборд, каталоги скинов/фракций/техники/китов). */
public record DataRequest(String type, String paramsJson) implements Net.C2S {

    public static void encode(DataRequest msg, FriendlyByteBuf buf) {
        buf.writeUtf(msg.type, 32);
        buf.writeUtf(msg.paramsJson, Payloads.MAX_REQUEST_CHARS);
    }

    public static DataRequest decode(FriendlyByteBuf buf) {
        return new DataRequest(buf.readUtf(32), buf.readUtf(Payloads.MAX_REQUEST_CHARS));
    }

    @Override
    public String validate() {
        return Validate.first(
                Validate.id("type", type, 32), Validate.optionalText("params", paramsJson, Payloads.MAX_REQUEST_CHARS));
    }

    @Override
    public void handle(ServerPlayer sender) {
        DataRequests.dispatch(sender, type, paramsJson);
    }
}
