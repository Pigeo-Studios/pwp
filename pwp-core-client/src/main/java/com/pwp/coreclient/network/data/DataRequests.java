package com.pwp.coreclient.network.data;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.pwp.api.core.CoreApi;
import com.pwp.api.net.Payloads;
import com.pwp.api.util.Parse;
import com.pwp.api.util.RateLimiter;
import com.pwp.api.util.RequestGuard;
import com.pwp.api.util.ServerThread;
import com.pwp.coreclient.network.PacketHandler;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.PacketDistributor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ЕДИНСТВЕННАЯ серверная система запросов данных для GUI (раньше их было две: core-client
 * PacketDataRequest/Response и warfare PacketRequestData/PacketSendData).
 *
 * <p>Клиент шлёт {@link DataRequest}(type, params) → обработчик типа запускает HTTP к core-service
 * в фоне → на главном потоке строится JSON-ответ (с фолбэком, если API недоступен) →
 * {@link DataResponse} уходит игроку, найденному заново по UUID. Повтор того же типа, пока первый
 * в полёте или не прошёл кулдаун 300 мс, игнорируется (ответ на первый всё равно придёт).
 *
 * <p>Другие моды добавляют свои типы через {@link #register} (warfare: {@code factionKits}).
 */
public final class DataRequests {
    private static final Logger LOG = LoggerFactory.getLogger(DataRequests.class);
    /** Тип ответа на админ-действие: {@code {action, success, error}}. */
    public static final String ACTION_RESULT = "actionResult";

    private static final Gson GSON = new Gson();
    private static final RequestGuard GUARD = new RequestGuard(300);
    /** Некорректные параметры — клиентский ввод: в лог не чаще раза в 10 с на игрока, без стека. */
    private static final RateLimiter BAD_PARAMS_LOG = new RateLimiter(10_000);
    /** Страница лидерборда: [1, MAX_PAGE]. */
    static final int MAX_PAGE = 1000;

    private static final Map<String, Handler> HANDLERS = new ConcurrentHashMap<>();

    /** Обработчик типа запроса. */
    public interface Handler {
        /** Фоновый запрос; null/ошибка API → ответ строится по {@link #toJson} с response = null. */
        CompletableFuture<JsonObject> fetch(ServerPlayer player, JsonObject params);

        /** JSON для клиента (главный поток, игрок онлайн). По умолчанию — ответ API или {@code {}}. */
        default String toJson(ServerPlayer player, JsonObject params, JsonObject response) {
            return response != null ? response.toString() : "{}";
        }
    }

    private DataRequests() {}

    /** Регистрирует (или заменяет) обработчик типа. */
    public static void register(String type, Handler handler) {
        HANDLERS.put(type, handler);
    }

    static {
        register(AdminInfo.TYPE, AdminInfo.HANDLER);
        register(ModerationAction.PLAYERS_TYPE, ModerationAction.PLAYERS);
        register(ServerAdmin.TYPE, ServerAdmin.DATA);
        register("profile", (p, q) -> CoreApi.loadPlayer(p.getStringUUID()));
        register("leaderboard", (p, q) -> CoreApi.getLeaderboard(str(q, "orderBy", "kills"), page(q), 20));
        register("skins", withDefault((p, q) -> CoreApi.getSkins(), "{\"data\":[]}"));
        register("factions", withDefault((p, q) -> CoreApi.getFactions(), "{\"data\":[]}"));
        register("factionVehicles", new Handler() {
            @Override
            public CompletableFuture<JsonObject> fetch(ServerPlayer p, JsonObject q) {
                String faction = str(q, "faction", "");
                return faction.isEmpty() ? empty() : CoreApi.getFactionVehicles(faction);
            }

            @Override
            public String toJson(ServerPlayer p, JsonObject q, JsonObject r) {
                if (str(q, "faction", "").isEmpty()) return "{}";
                return r != null ? r.toString() : "{\"data\":[]}";
            }
        });
        register("factionVehicle", new Handler() {
            @Override
            public CompletableFuture<JsonObject> fetch(ServerPlayer p, JsonObject q) {
                String faction = str(q, "faction", "");
                String vehicle = str(q, "vehicle", "");
                return faction.isEmpty() || vehicle.isEmpty() ? empty() : CoreApi.getFactionVehicle(faction, vehicle);
            }

            @Override
            public String toJson(ServerPlayer p, JsonObject q, JsonObject r) {
                if (str(q, "faction", "").isEmpty() || str(q, "vehicle", "").isEmpty()) return "{}";
                return r != null ? r.toString() : "{\"data\":{}}";
            }
        });
    }

    /** Обработка запроса на главном потоке (вызывается из {@link DataRequest#handle}). */
    static void dispatch(ServerPlayer player, String type, String paramsJson) {
        Handler handler = HANDLERS.get(type);
        if (handler == null) {
            LOG.debug(
                    "Неизвестный тип запроса данных {} от {}",
                    type,
                    player.getGameProfile().getName());
            send(player, type, "{}");
            return;
        }
        JsonObject params = parse(player, type, paramsJson);
        ServerThread.guardedForPlayer(GUARD, type, player, () -> handler.fetch(player, params), (online, response) -> {
            String json = handler.toJson(online, params, response);
            send(online, type, json);
        });
    }

    /** Отправка ответа; слишком большой ответ заменяется пустым с ошибкой в логе (а не крашем канала). */
    public static void send(ServerPlayer player, String type, String json) {
        if (!Payloads.fits(json)) {
            LOG.error(
                    "Ответ {} для {} больше {} байт — не отправлен",
                    type,
                    player.getGameProfile().getName(),
                    Payloads.MAX_LARGE_BYTES);
            json = "{}";
        }
        PacketHandler.INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), new DataResponse(type, json));
    }

    private static Handler withDefault(Handler fetcher, String fallback) {
        return new Handler() {
            @Override
            public CompletableFuture<JsonObject> fetch(ServerPlayer p, JsonObject q) {
                return fetcher.fetch(p, q);
            }

            @Override
            public String toJson(ServerPlayer p, JsonObject q, JsonObject r) {
                return r != null ? r.toString() : fallback;
            }
        };
    }

    public static CompletableFuture<JsonObject> empty() {
        return CompletableFuture.completedFuture(new JsonObject());
    }

    public static String str(JsonObject o, String key, String def) {
        return o.has(key) && o.get(key).isJsonPrimitive() ? o.get(key).getAsString() : def;
    }

    /** Номер страницы из параметров (число или строка), вне диапазона — прижимается к [1, MAX_PAGE]. */
    static int page(JsonObject q) {
        int page = Parse.intOr(str(q, "page", "1"), 1);
        return Math.max(1, Math.min(MAX_PAGE, page));
    }

    private static JsonObject parse(ServerPlayer player, String type, String params) {
        if (params == null || params.isEmpty()) return new JsonObject();
        try {
            JsonObject o = GSON.fromJson(params, JsonObject.class);
            return o != null ? o : new JsonObject();
        } catch (JsonParseException | IllegalStateException e) {
            if (BAD_PARAMS_LOG.tryAcquire(player.getStringUUID())) {
                LOG.warn(
                        "Некорректные параметры запроса данных {} от {}: {} (повторы подавляются 10 с)",
                        type,
                        player.getGameProfile().getName(),
                        e.getMessage());
            }
            return new JsonObject();
        }
    }
}
