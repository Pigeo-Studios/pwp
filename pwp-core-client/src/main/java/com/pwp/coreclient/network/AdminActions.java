package com.pwp.coreclient.network;

import com.google.gson.JsonObject;
import java.util.function.Consumer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * Клиентская обёртка над {@link PacketAction}: отправляет админ-действие на сервер и вызывает
 * колбэк с результатом ({@code success}, {@code error}) на клиентском потоке.
 * Одновременно ожидается один ответ на действие с данным именем (редактор один).
 */
@OnlyIn(Dist.CLIENT)
public final class AdminActions {
    private AdminActions() {}

    public record Result(boolean success, String error) {}

    public static void send(String action, JsonObject params, Consumer<Result> onResult) {
        ClientResponseCache.actionResultListener = json -> {
            if (json == null || !json.has("action") || !action.equals(json.get("action").getAsString())) return;
            ClientResponseCache.actionResultListener = null;
            boolean ok = json.has("success") && json.get("success").getAsBoolean();
            String error = json.has("error") ? json.get("error").getAsString() : "";
            onResult.accept(new Result(ok, error));
        };
        PacketHandler.INSTANCE.sendToServer(new PacketAction(action, params));
    }
}
