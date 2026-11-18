package com.pigeostudios.pwp.warfare.admin;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import com.pigeostudios.pwp.warfare.config.MapLayoutState;
import com.pigeostudios.pwp.warfare.layout.LayoutTransfer;
import com.pigeostudios.pwp.warfare.layout.MapLayout;
import com.pigeostudios.pwp.warfare.server.map.MapLayoutService;
import com.pigeostudios.pwp.warfare.world.WarfareWorldData;
import com.pwp.api.core.CoreApi;
import com.pwp.api.net.Net;
import com.pwp.api.net.Validate;
import com.pwp.api.security.AccessLevel;
import com.pwp.api.security.PwpPermissions;
import com.pwp.api.util.RequestGuard;
import com.pwp.api.util.ServerThread;
import com.pwp.coreclient.network.data.AdminAction;
import com.pwp.coreclient.network.data.DataRequests;
import java.util.concurrent.CompletableFuture;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;

/**
 * Сеть v2 редактора карты (этап 3). Один пакет = одно действие, уровень доступа — на сервере в каждом
 * пакете: «на этот матч» — ADMIN, «навсегда» и откат — OWNER. Клиент присылает только JSON раскладки
 * (ключи раскладки, ≤ {@link LayoutTransfer#MAX_BYTES}); сервер сам разбирает его кодеком, сливает с
 * map_config матча, валидирует и применяет ({@link MapLayoutService}).
 *
 * <p>Данные для GUI — типы {@code DataRequests}: {@value #DATA_EDITOR} (текущая явная раскладка + флаги) и
 * {@value #DATA_VERSIONS} (список версий из core-service).
 */
public final class MapEditorPackets {
    private static final Logger LOG = LogUtils.getLogger();

    public static final String DATA_EDITOR = "mapEditor";
    public static final String DATA_VERSIONS = "mapLayoutVersions";

    public static final String APPLY_MATCH = "mapEditor.applyMatch";
    public static final String SAVE_PERMANENT = "mapEditor.savePermanent";
    public static final String ROLLBACK = "mapEditor.rollback";

    public static final int COMMENT_MAX = 200;
    /** Строка JSON в буфере: символов не больше байт (UTF-8), лимит readUtf — с запасом. */
    private static final int JSON_CHARS_MAX = LayoutTransfer.MAX_BYTES;

    private static final RequestGuard CORE_GUARD = new RequestGuard(2000);

    private MapEditorPackets() {}

    /** Регистрация типов данных (commonSetup warfare). */
    public static void registerData() {
        DataRequests.register(DATA_EDITOR, new DataRequests.Handler() {
            @Override
            public CompletableFuture<JsonObject> fetch(ServerPlayer p, JsonObject q) {
                return DataRequests.empty();
            }

            @Override
            public String toJson(ServerPlayer p, JsonObject q, JsonObject r) {
                if (!PwpPermissions.has(p, AccessLevel.ADMIN)) return noPermission();
                return editorJson(p).toString();
            }
        });
        DataRequests.register(DATA_VERSIONS, new DataRequests.Handler() {
            @Override
            public CompletableFuture<JsonObject> fetch(ServerPlayer p, JsonObject q) {
                String map = MapLayoutService.mapName();
                if (!PwpPermissions.has(p, AccessLevel.ADMIN) || map.isEmpty() || !MapLayoutService.coreAvailable()) {
                    return DataRequests.empty();
                }
                return CoreApi.listMapLayouts(map);
            }

            @Override
            public String toJson(ServerPlayer p, JsonObject q, JsonObject r) {
                if (!PwpPermissions.has(p, AccessLevel.ADMIN)) return noPermission();
                JsonObject o = new JsonObject();
                o.addProperty("available", r != null);
                o.add(
                        "versions",
                        r != null && r.has("data") && r.get("data").isJsonArray() ? r.get("data") : new JsonArray());
                return o.toString();
            }
        });
    }

    private static String noPermission() {
        JsonObject o = new JsonObject();
        o.addProperty("error", AdminAction.ERR_NO_PERMISSION);
        return o.toString();
    }

    /** Состояние редактора: явная раскладка (ключи раскладки) + флаги для кнопок. */
    static JsonObject editorJson(ServerPlayer p) {
        JsonObject o = new JsonObject();
        MapLayout explicit = MapLayoutState.explicit();
        WarfareWorldData data = WarfareWorldData.get(p.serverLevel());
        o.addProperty("loaded", explicit != null);
        o.addProperty("mapName", MapLayoutService.mapName());
        o.addProperty("source", MapLayoutState.source());
        o.addProperty("canPersist", PwpPermissions.has(p, AccessLevel.OWNER));
        o.addProperty("coreAvailable", MapLayoutService.coreAvailable());
        o.addProperty("matchRunning", MapLayoutService.matchRunning(data));
        o.addProperty("matchEdit", data.mapEdit.hasMatchLayout());
        o.addProperty("versionId", MapLayoutService.appliedVersionId());
        if (explicit != null) o.add("layout", LayoutTransfer.strip(explicit, MapLayoutService.mapName()));
        return o;
    }

    private static void refresh(ServerPlayer p) {
        if (PwpPermissions.has(p, AccessLevel.ADMIN)) {
            DataRequests.send(p, DATA_EDITOR, editorJson(p).toString());
        }
    }

    private static void writeJson(FriendlyByteBuf b, String json) {
        b.writeUtf(json, JSON_CHARS_MAX);
    }

    private static String readJson(FriendlyByteBuf b) {
        return b.readUtf(JSON_CHARS_MAX);
    }

    /** «Только на этот матч» (ADMIN): в SavedData матча + применить сразу. */
    public record ApplyForMatch(String json) implements Net.C2S {
        public static void encode(ApplyForMatch m, FriendlyByteBuf b) {
            writeJson(b, m.json);
        }

        public static ApplyForMatch decode(FriendlyByteBuf b) {
            return new ApplyForMatch(readJson(b));
        }

        @Override
        public String validate() {
            return LayoutTransfer.checkSize(json);
        }

        @Override
        public void handle(ServerPlayer s) {
            AdminAction.runLocal(
                    s,
                    APPLY_MATCH,
                    AccessLevel.ADMIN,
                    () -> MapLayoutService.applyForMatch(
                            s.serverLevel(), json, s.getGameProfile().getName()));
            refresh(s);
        }
    }

    /**
     * «Навсегда» (OWNER): новая версия в БД через core-service (асинхронно) и применение на матч. Если матч
     * уже идёт — версия сохранена, применится со следующего матча.
     */
    public record SavePermanent(String json, String comment) implements Net.C2S {
        public static void encode(SavePermanent m, FriendlyByteBuf b) {
            writeJson(b, m.json);
            b.writeUtf(m.comment, COMMENT_MAX);
        }

        public static SavePermanent decode(FriendlyByteBuf b) {
            return new SavePermanent(readJson(b), b.readUtf(COMMENT_MAX));
        }

        @Override
        public String validate() {
            return Validate.first(
                    LayoutTransfer.checkSize(json), Validate.optionalText("comment", comment, COMMENT_MAX));
        }

        @Override
        public void handle(ServerPlayer s) {
            if (!PwpPermissions.has(s, AccessLevel.OWNER)) {
                LOG.warn(
                        "{} без OWNER пытался сохранить раскладку навсегда",
                        s.getGameProfile().getName());
                AdminAction.reply(s, SAVE_PERMANENT, false, AdminAction.ERR_NO_PERMISSION);
                return;
            }
            String map = MapLayoutService.mapName();
            MapLayout incoming = MapLayoutService.parse(json);
            if (map.isEmpty() || MapLayoutState.explicit() == null) {
                AdminAction.reply(s, SAVE_PERMANENT, false, MapLayoutService.ERR_NOT_LOADED);
                return;
            }
            if (incoming == null) {
                AdminAction.reply(s, SAVE_PERMANENT, false, MapLayoutService.ERR_BAD_JSON);
                return;
            }
            if (!MapLayoutService.coreAvailable()) {
                AdminAction.reply(s, SAVE_PERMANENT, false, MapLayoutService.ERR_CORE);
                return;
            }
            // в БД — нормализованный JSON кодека (только раскладка), а не строка клиента как есть
            String normalized = LayoutTransfer.strip(incoming, map).toString();
            boolean started = ServerThread.guardedForPlayer(
                    CORE_GUARD,
                    SAVE_PERMANENT,
                    s,
                    () -> CoreApi.saveMapLayout(
                            map,
                            normalized,
                            s.getStringUUID(),
                            s.getGameProfile().getName(),
                            comment.strip()),
                    (online, resp) -> onSaved(online, SAVE_PERMANENT, resp, "saved"));
            if (!started) AdminAction.reply(s, SAVE_PERMANENT, false, AdminAction.ERR_BUSY);
        }
    }

    /** Откат (OWNER) = новая версия-копия {@code id}; применяется на матч, как «навсегда». */
    public record Rollback(long id) implements Net.C2S {
        public static void encode(Rollback m, FriendlyByteBuf b) {
            b.writeVarLong(m.id);
        }

        public static Rollback decode(FriendlyByteBuf b) {
            return new Rollback(b.readVarLong());
        }

        @Override
        public String validate() {
            return Validate.range("id", id, 1, Long.MAX_VALUE);
        }

        @Override
        public void handle(ServerPlayer s) {
            if (!PwpPermissions.has(s, AccessLevel.OWNER)) {
                AdminAction.reply(s, ROLLBACK, false, AdminAction.ERR_NO_PERMISSION);
                return;
            }
            if (!MapLayoutService.coreAvailable()) {
                AdminAction.reply(s, ROLLBACK, false, MapLayoutService.ERR_CORE);
                return;
            }
            boolean started = ServerThread.guardedForPlayer(
                    CORE_GUARD,
                    ROLLBACK,
                    s,
                    () -> CoreApi.rollbackMapLayout(
                            id, s.getStringUUID(), s.getGameProfile().getName()),
                    (online, resp) -> onSaved(online, ROLLBACK, resp, "rollback"));
            if (!started) AdminAction.reply(s, ROLLBACK, false, AdminAction.ERR_BUSY);
        }
    }

    /** Ответ core-service на сохранение/откат (главный поток, игрок онлайн). */
    private static void onSaved(ServerPlayer p, String action, JsonObject resp, String what) {
        JsonObject v = MapLayoutService.data(resp);
        if (v == null) {
            AdminAction.reply(p, action, false, MapLayoutService.ERR_CORE);
            return;
        }
        long id = MapLayoutService.id(v);
        LOG.info(
                "[MapLayout] {} {} version {} by {}",
                MapLayoutService.mapName(),
                what,
                id,
                p.getGameProfile().getName());
        WarfareWorldData data = WarfareWorldData.get(p.serverLevel());
        String err = null;
        if (!MapLayoutService.matchRunning(data)) {
            err = MapLayoutService.apply(
                    p.serverLevel(), MapLayoutService.str(v, "json"), "core-service version " + id);
            if (err == null) {
                MapLayoutService.setAppliedVersionId(id);
                // версия «навсегда» стала базой — правка «на матч» больше не нужна
                data.mapEdit.clear();
                data.setDirty();
            }
        }
        AdminAction.reply(p, action, err == null, err == null ? "" : err);
        refresh(p);
    }

    /** Регистрация в канале warfare — В КОНЕЦ списка (после MatchAdminPackets). */
    public static void register(Net.Registrar r) {
        r.c2s(ApplyForMatch.class, ApplyForMatch::encode, ApplyForMatch::decode)
                .c2s(SavePermanent.class, SavePermanent::encode, SavePermanent::decode)
                .c2s(Rollback.class, Rollback::encode, Rollback::decode);
    }
}
