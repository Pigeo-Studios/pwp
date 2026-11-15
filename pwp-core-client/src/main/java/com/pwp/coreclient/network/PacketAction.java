package com.pwp.coreclient.network;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.pwp.api.core.CoreApi;
import com.pwp.api.security.PwpPermissions;
import com.pwp.api.util.RequestGuard;
import com.pwp.api.util.ServerThread;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent.Context;
import net.minecraftforge.network.PacketDistributor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Клиент → сервер: админ-действие над данными core-service (редакторы скинов и техники).
 *
 * <p>Все действия требуют прав {@link PwpPermissions#isAdmin} (op 2+) — проверка на сервере.
 * HTTP выполняется асинхронно; результат возвращается клиенту ответом
 * {@link PacketDataResponse} с типом {@code actionResult}: {@code {action, success, error}}.
 *
 * <p>Экипировка косметики сюда больше не входит: она в pwp-cosmetics (PacketCosmeticEquip)
 * с проверкой владения. Прежний equipCosmetic выдавал игроку любой скин по skinId от клиента.
 */
public class PacketAction {
    private static final Logger LOG = LoggerFactory.getLogger(PacketAction.class);
    private static final Gson GSON = new Gson();

    public static final String SAVE_SKIN = "saveSkin";
    public static final String DELETE_SKIN = "deleteSkin";
    public static final String DELETE_FACTION_VEHICLE = "deleteFactionVehicle";

    private final String action;
    private final String paramsJson;

    public PacketAction(String action, String paramsJson) {
        this.action = action;
        this.paramsJson = paramsJson;
    }

    public PacketAction(String action, JsonObject params) {
        this(action, params.toString());
    }

    public static void encode(PacketAction msg, FriendlyByteBuf buf) {
        buf.writeUtf(msg.action);
        buf.writeUtf(msg.paramsJson);
    }

    public static PacketAction decode(FriendlyByteBuf buf) {
        return new PacketAction(buf.readUtf(), buf.readUtf());
    }

    public static void handle(PacketAction msg, Supplier<Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;
            if (!PwpPermissions.isAdmin(player)) {
                LOG.warn(
                        "Игрок {} без прав прислал админ-действие {}",
                        player.getGameProfile().getName(),
                        msg.action);
                reply(player, msg.action, false, "Недостаточно прав");
                return;
            }
            JsonObject params;
            try {
                params = GSON.fromJson(msg.paramsJson, JsonObject.class);
            } catch (JsonParseException e) {
                LOG.warn("Некорректные параметры действия {}", msg.action, e);
                reply(player, msg.action, false, "Некорректные параметры");
                return;
            }
            JsonObject p = params == null ? new JsonObject() : params;
            if (!isKnown(msg.action, p)) {
                reply(player, msg.action, false, "Неизвестное действие");
                return;
            }
            boolean started = ServerThread.guardedForPlayer(
                    GUARD, msg.action, player, () -> start(msg.action, p), (online, resp) -> {
                        String error = resp != null
                                        && resp.has("error")
                                        && !resp.get("error").isJsonNull()
                                ? resp.get("error").getAsString()
                                : "core-service недоступен";
                        boolean ok = CoreApi.isSuccess(resp);
                        reply(online, msg.action, ok, ok ? "" : error);
                    });
            if (!started) reply(player, msg.action, false, "Подождите, предыдущий запрос ещё выполняется");
        });
        ctx.get().setPacketHandled(true);
    }

    /** Антиспам: один запрос на действие за раз, не чаще раза в секунду. */
    private static final RequestGuard GUARD = new RequestGuard(1000);

    /** true — действие известно и все обязательные параметры на месте. */
    private static boolean isKnown(String action, JsonObject p) {
        switch (action) {
            case SAVE_SKIN:
                return has(p, "skinId", "name", "slotType", "rarity", "modelPath");
            case DELETE_SKIN:
                return has(p, "skinId");
            case DELETE_FACTION_VEHICLE:
                return has(p, "faction", "vehicleName");
            default:
                return false;
        }
    }

    /** Запускает HTTP-вызов действия; null — неизвестное действие или нет обязательных параметров. */
    private static CompletableFuture<JsonObject> start(String action, JsonObject p) {
        switch (action) {
            case SAVE_SKIN:
                if (!has(p, "skinId", "name", "slotType", "rarity", "modelPath")) return null;
                return CoreApi.saveSkin(
                        str(p, "skinId"),
                        str(p, "name"),
                        str(p, "description"),
                        str(p, "slotType"),
                        str(p, "weaponTag"),
                        str(p, "rarity"),
                        str(p, "modelPath"));
            case DELETE_SKIN:
                if (!has(p, "skinId")) return null;
                return CoreApi.deleteSkin(str(p, "skinId"));
            case DELETE_FACTION_VEHICLE:
                if (!has(p, "faction", "vehicleName")) return null;
                return CoreApi.deleteFactionVehicle(str(p, "faction"), str(p, "vehicleName"));
            default:
                return null;
        }
    }

    private static void reply(ServerPlayer player, String action, boolean success, String error) {
        if (player.hasDisconnected()) return;
        JsonObject result = new JsonObject();
        result.addProperty("action", action);
        result.addProperty("success", success);
        result.addProperty("error", error);
        PacketHandler.INSTANCE.send(
                PacketDistributor.PLAYER.with(() -> player),
                new PacketDataResponse(PacketDataResponse.ACTION_RESULT, result.toString()));
    }

    private static boolean has(JsonObject p, String... keys) {
        for (String k : keys) {
            if (!p.has(k) || p.get(k).isJsonNull()) return false;
        }
        return true;
    }

    private static String str(JsonObject p, String key) {
        return p.has(key) && !p.get(key).isJsonNull() ? p.get(key).getAsString() : "";
    }
}
