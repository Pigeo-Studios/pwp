package com.pwp.coreclient.network.data;

import com.pwp.api.net.Net;
import com.pwp.api.net.Payloads;
import net.minecraft.network.FriendlyByteBuf;

/**
 * S→C: ответ на {@link DataRequest}. JSON пишется массивом байт с лимитом 1 МБ
 * ({@link Payloads}) — старый writeUtf(32767) не вмещал большие каталоги.
 */
public record DataResponse(String type, String json) implements Net.S2C {

    public static void encode(DataResponse msg, FriendlyByteBuf buf) {
        buf.writeUtf(msg.type, 32);
        Payloads.writeLarge(buf, msg.json);
    }

    public static DataResponse decode(FriendlyByteBuf buf) {
        return new DataResponse(buf.readUtf(32), Payloads.readLarge(buf));
    }

    @Override
    public void handleClient() {
        ClientDataResponses.dispatch(type, json);
    }
}
