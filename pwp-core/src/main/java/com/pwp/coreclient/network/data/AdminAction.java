package com.pwp.coreclient.network.data;

import com.google.gson.JsonObject;
import com.pwp.api.admin.AuditTrail;
import com.pwp.api.config.LobbySettings;
import com.pwp.api.core.CoreApi;
import com.pwp.api.lobby.LobbyActions;
import com.pwp.api.net.Net;
import com.pwp.api.net.Validate;
import com.pwp.api.security.AccessLevel;
import com.pwp.api.security.PwpPermissions;
import com.pwp.api.util.RateLimiter;
import com.pwp.api.util.RequestGuard;
import com.pwp.api.util.ServerThread;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Админ-действия редакторов (op 2+) — по пакету на действие, поля типизированы и проверяются
 * сервером. Раньше был один PacketAction(action, paramsJson) с разбором JSON на сервере.
 * Ответ — {@link DataResponse} типа {@link DataRequests#ACTION_RESULT}.
 */
public final class AdminAction {
    private static final Logger LOG = LoggerFactory.getLogger(AdminAction.class);
    /** Один запрос на действие за раз, не чаще раза в секунду. */
    private static final RequestGuard GUARD = new RequestGuard(1000);
    /** Локальные действия (без HTTP): не чаще раза в 250 мс на игрока и действие (двойной клик). */
    private static final RateLimiter LOCAL_GUARD = new RateLimiter(250);

    public static final String LOBBY_START_VOTE = "lobby.startVote";
    public static final String LOBBY_STOP_VOTE = "lobby.stopVote";
    public static final String LOBBY_SAVE_SETTINGS = "lobby.saveSettings";
    /** Ошибка «на этом сервере нет лобби» (матч-сервер). */
    public static final String ERR_NO_LOBBY = "pwp_core.admin.error.no_lobby";

    public static final String ERR_NO_PERMISSION = "pwp_core.admin.error.no_permission";
    public static final String ERR_BUSY = "pwp_core.admin.error.busy";
    public static final String ERR_INTERNAL = "pwp_core.admin.error.internal";
    /** core-service не ответил (раньше — русский литерал вместо ключа). */
    public static final String ERR_CORE_DOWN = "pwp_core.admin.error.core_down";

    /** Лобби: начать голосование за карту (полный цикл карта → режим → фракции). */
    public record LobbyStartVote() implements Net.C2S {
        public static void encode(LobbyStartVote m, FriendlyByteBuf b) {}

        public static LobbyStartVote decode(FriendlyByteBuf b) {
            return new LobbyStartVote();
        }

        @Override
        public void handle(ServerPlayer sender) {
            runLocal(sender, LOBBY_START_VOTE, AccessLevel.ADMIN, () -> {
                LobbyActions lobby = LobbyActions.get();
                return lobby == null ? ERR_NO_LOBBY : lobby.adminStartVote(sender);
            });
        }
    }

    /** Лобби: остановить любое идущее голосование (карта/режим/фракции). */
    public record LobbyStopVote() implements Net.C2S {
        public static void encode(LobbyStopVote m, FriendlyByteBuf b) {}

        public static LobbyStopVote decode(FriendlyByteBuf b) {
            return new LobbyStopVote();
        }

        @Override
        public void handle(ServerPlayer sender) {
            runLocal(sender, LOBBY_STOP_VOTE, AccessLevel.ADMIN, () -> {
                LobbyActions lobby = LobbyActions.get();
                return lobby == null ? ERR_NO_LOBBY : lobby.adminStopVote(sender);
            });
        }
    }

    /**
     * Лобби: применить настройки (автостарт, лимит матчей, минимум игроков). ADMIN — до рестарта лобби;
     * {@code persist} (только OWNER) — ещё и записать в {@code config/pwp-server.json} «навсегда».
     */
    public record LobbySaveSettings(boolean autostart, int maxMatches, int minPlayers, boolean persist)
            implements Net.C2S {
        public static void encode(LobbySaveSettings m, FriendlyByteBuf b) {
            b.writeBoolean(m.autostart);
            b.writeVarInt(m.maxMatches);
            b.writeVarInt(m.minPlayers);
            b.writeBoolean(m.persist);
        }

        public static LobbySaveSettings decode(FriendlyByteBuf b) {
            return new LobbySaveSettings(b.readBoolean(), b.readVarInt(), b.readVarInt(), b.readBoolean());
        }

        @Override
        public String validate() {
            return Validate.first(
                    Validate.range(
                            "maxMatches", maxMatches, LobbySettings.MAX_MATCHES_MIN, LobbySettings.MAX_MATCHES_MAX),
                    Validate.range(
                            "minPlayers", minPlayers, LobbySettings.MIN_PLAYERS_MIN, LobbySettings.MIN_PLAYERS_MAX));
        }

        @Override
        public void handle(ServerPlayer sender) {
            runLocal(sender, LOBBY_SAVE_SETTINGS, persist ? AccessLevel.OWNER : AccessLevel.ADMIN, () -> {
                LobbyActions lobby = LobbyActions.get();
                return lobby == null
                        ? ERR_NO_LOBBY
                        : lobby.adminSaveSettings(
                                sender, new LobbySettings(autostart, maxMatches, minPlayers), persist);
            });
        }
    }

    /**
     * Синхронное админ-действие (без HTTP): уровень доступа на сервере ({@code required}) → антиспам →
     * действие на главном потоке → ответ {@code actionResult}. {@code body} возвращает null при успехе или
     * ключ перевода ошибки. Общий путь для пакетов других модов (warfare: вкладка «Матч»).
     */
    public static void runLocal(ServerPlayer player, String action, AccessLevel required, Supplier<String> body) {
        runLocal(player, action, required, "", body);
    }

    /**
     * То же с {@code details} для аудит-лога (цель/параметры: ник, команда, значение). Каждый вызов —
     * запись в {@link AdminAudit} (успех, ошибка или отказ по правам).
     */
    public static void runLocal(
            ServerPlayer player, String action, AccessLevel required, String details, Supplier<String> body) {
        if (!PwpPermissions.has(player, required)) {
            AdminAudit.record(player, action, details, AuditTrail.Result.DENIED, required.name());
            LOG.warn(
                    "Игрок {} (уровень {}) без прав {} прислал админ-действие {}",
                    player.getGameProfile().getName(),
                    PwpPermissions.level(player),
                    required,
                    action);
            reply(player, action, false, ERR_NO_PERMISSION);
            return;
        }
        if (!LOCAL_GUARD.tryAcquire(player.getStringUUID() + "|" + action)) {
            reply(player, action, false, ERR_BUSY);
            return;
        }
        String error;
        try {
            error = body.get();
        } catch (RuntimeException e) {
            LOG.warn(
                    "Админ-действие {} от {} упало",
                    action,
                    player.getGameProfile().getName(),
                    e);
            error = ERR_INTERNAL;
        }
        LOG.info(
                "Админ-действие {} от {}: {}", action, player.getGameProfile().getName(), error == null ? "OK" : error);
        AdminAudit.record(
                player, action, details, error == null ? AuditTrail.Result.OK : AuditTrail.Result.FAILED, error);
        reply(player, action, error == null, error == null ? "" : error);
    }

    public static final String SAVE_SKIN = "saveSkin";
    public static final String DELETE_SKIN = "deleteSkin";
    public static final String DELETE_FACTION_VEHICLE = "deleteFactionVehicle";

    private AdminAction() {}

    /** Сохранить/создать скин в каталоге. */
    public record SaveSkin(
            String skinId,
            String name,
            String description,
            String slotType,
            String weaponTag,
            String rarity,
            String modelPath)
            implements Net.C2S {
        public static void encode(SaveSkin m, FriendlyByteBuf b) {
            b.writeUtf(m.skinId, 64);
            b.writeUtf(m.name, 64);
            b.writeUtf(m.description, 512);
            b.writeUtf(m.slotType, 32);
            b.writeUtf(m.weaponTag, 128);
            b.writeUtf(m.rarity, 32);
            b.writeUtf(m.modelPath, 256);
        }

        public static SaveSkin decode(FriendlyByteBuf b) {
            return new SaveSkin(
                    b.readUtf(64),
                    b.readUtf(64),
                    b.readUtf(512),
                    b.readUtf(32),
                    b.readUtf(128),
                    b.readUtf(32),
                    b.readUtf(256));
        }

        @Override
        public String validate() {
            return Validate.first(
                    Validate.id("skinId", skinId, 64),
                    Validate.text("name", name, 64),
                    Validate.optionalText("description", description, 512),
                    Validate.id("slotType", slotType, 32),
                    Validate.optionalText("weaponTag", weaponTag, 128),
                    Validate.id("rarity", rarity, 32),
                    Validate.id("modelPath", modelPath, 256));
        }

        @Override
        public void handle(ServerPlayer sender) {
            run(
                    sender,
                    SAVE_SKIN,
                    () -> CoreApi.saveSkin(skinId, name, description, slotType, weaponTag, rarity, modelPath));
        }
    }

    /** Удалить скин из каталога. */
    public record DeleteSkin(String skinId) implements Net.C2S {
        public static void encode(DeleteSkin m, FriendlyByteBuf b) {
            b.writeUtf(m.skinId, 64);
        }

        public static DeleteSkin decode(FriendlyByteBuf b) {
            return new DeleteSkin(b.readUtf(64));
        }

        @Override
        public String validate() {
            return Validate.id("skinId", skinId, 64);
        }

        @Override
        public void handle(ServerPlayer sender) {
            run(sender, DELETE_SKIN, () -> CoreApi.deleteSkin(skinId));
        }
    }

    /** Удалить технику из пула фракции. */
    public record DeleteFactionVehicle(String faction, String vehicleName) implements Net.C2S {
        public static void encode(DeleteFactionVehicle m, FriendlyByteBuf b) {
            b.writeUtf(m.faction, 64);
            b.writeUtf(m.vehicleName, 128);
        }

        public static DeleteFactionVehicle decode(FriendlyByteBuf b) {
            return new DeleteFactionVehicle(b.readUtf(64), b.readUtf(128));
        }

        @Override
        public String validate() {
            return Validate.first(Validate.id("faction", faction, 64), Validate.text("vehicleName", vehicleName, 128));
        }

        @Override
        public void handle(ServerPlayer sender) {
            run(sender, DELETE_FACTION_VEHICLE, () -> CoreApi.deleteFactionVehicle(faction, vehicleName));
        }
    }

    /** Общий путь: право op 2+ на сервере → антиспам → HTTP в фоне → ответ игроку (по UUID). */
    private static void run(ServerPlayer player, String action, Supplier<CompletableFuture<JsonObject>> call) {
        if (!PwpPermissions.isAdmin(player)) {
            LOG.warn(
                    "Игрок {} без прав прислал админ-действие {}",
                    player.getGameProfile().getName(),
                    action);
            AdminAudit.record(player, action, "", AuditTrail.Result.DENIED, AccessLevel.ADMIN.name());
            reply(player, action, false, ERR_NO_PERMISSION);
            return;
        }
        boolean started = ServerThread.guardedForPlayer(GUARD, action, player, call, (online, resp) -> {
            boolean ok = CoreApi.isSuccess(resp);
            String error =
                    resp != null && resp.has("error") && !resp.get("error").isJsonNull()
                            ? resp.get("error").getAsString()
                            : ERR_CORE_DOWN;
            AdminAudit.record(
                    online, action, "", ok ? AuditTrail.Result.OK : AuditTrail.Result.FAILED, ok ? "" : error);
            reply(online, action, ok, ok ? "" : error);
        });
        if (!started) reply(player, action, false, ERR_BUSY);
    }

    /** Ответ {@code actionResult}; {@code error} — текст или ключ перевода (клиент переводит, если ключ есть). */
    public static void reply(ServerPlayer player, String action, boolean success, String error) {
        JsonObject result = new JsonObject();
        result.addProperty("action", action);
        result.addProperty("success", success);
        result.addProperty("error", error);
        DataRequests.send(player, DataRequests.ACTION_RESULT, result.toString());
    }
}
