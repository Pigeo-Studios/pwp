package com.pwp.coreclient.network;

import com.pwp.api.net.Net;
import com.pwp.coreclient.network.data.AdminAction;
import java.util.function.Consumer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * Клиентская обёртка над {@link AdminAction}: отправляет админ-действие на сервер и вызывает
 * колбэк с результатом ({@code success}, {@code error}) на клиентском потоке.
 * Одновременно ожидается один ответ на действие с данным именем (редактор один).
 */
@OnlyIn(Dist.CLIENT)
public final class AdminActions {
    private AdminActions() {}

    public record Result(boolean success, String error) {}

    public static void saveSkin(AdminAction.SaveSkin action, Consumer<Result> onResult) {
        send(AdminAction.SAVE_SKIN, action, onResult);
    }

    public static void deleteSkin(String skinId, Consumer<Result> onResult) {
        send(AdminAction.DELETE_SKIN, new AdminAction.DeleteSkin(skinId), onResult);
    }

    public static void deleteFactionVehicle(String faction, String vehicleName, Consumer<Result> onResult) {
        send(AdminAction.DELETE_FACTION_VEHICLE, new AdminAction.DeleteFactionVehicle(faction, vehicleName), onResult);
    }

    private static void send(String action, Net.C2S packet, Consumer<Result> onResult) {
        ClientResponseCache.actionResultListener = json -> {
            if (json == null
                    || !json.has("action")
                    || !action.equals(json.get("action").getAsString())) return;
            ClientResponseCache.actionResultListener = null;
            boolean ok = json.has("success") && json.get("success").getAsBoolean();
            String error = json.has("error") ? json.get("error").getAsString() : "";
            onResult.accept(new Result(ok, error));
        };
        PacketHandler.INSTANCE.sendToServer(packet);
    }
}
