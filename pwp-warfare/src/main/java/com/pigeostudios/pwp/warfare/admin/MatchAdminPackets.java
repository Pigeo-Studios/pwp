package com.pigeostudios.pwp.warfare.admin;

import com.google.gson.JsonObject;
import com.pigeostudios.pwp.warfare.match.AdminInput;
import com.pwp.api.net.Net;
import com.pwp.api.net.Validate;
import com.pwp.api.security.AccessLevel;
import com.pwp.api.security.PwpPermissions;
import com.pwp.coreclient.network.data.AdminAction;
import com.pwp.coreclient.network.data.AdminInfo;
import com.pwp.coreclient.network.data.DataRequests;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

/**
 * Сеть v2 вкладки «Матч» админ-панели: один пакет = одно действие, поля проверяются в {@code validate()},
 * уровень ADMIN (OWNER — для записи в конфиг) проверяется на сервере в {@link AdminAction#runLocal}, логика —
 * {@link MatchAdmin}. Ответ — {@code actionResult} + свежее {@code adminMatch}.
 */
public final class MatchAdminPackets {
    public static final String START = "match.start";
    public static final String STOP = "match.stop";
    public static final String PAUSE = "match.pause";
    public static final String SKIP_WAITING = "match.skipWaiting";
    public static final String SET_MODE = "match.setMode";
    public static final String SET_DEFENDER = "match.setDefender";
    public static final String SET_FACTION = "match.setFaction";
    public static final String SET_TICKETS = "match.setTickets";
    public static final String SET_WAITING = "match.setWaiting";

    private MatchAdminPackets() {}

    /** Регистрация типа данных и раздела (commonSetup warfare). */
    public static void registerData() {
        AdminInfo.addFeature(AdminInfo.FEATURE_MATCH);
        AdminInfo.addFeature(AdminInfo.FEATURE_MAP);
        DataRequests.register(MatchAdmin.DATA_TYPE, new DataRequests.Handler() {
            @Override
            public CompletableFuture<JsonObject> fetch(ServerPlayer p, JsonObject q) {
                return DataRequests.empty();
            }

            @Override
            public String toJson(ServerPlayer p, JsonObject q, JsonObject r) {
                if (!PwpPermissions.has(p, AccessLevel.ADMIN)) {
                    JsonObject o = new JsonObject();
                    o.addProperty("error", AdminAction.ERR_NO_PERMISSION);
                    return o.toString();
                }
                return MatchAdmin.toJson(p.serverLevel()).toString();
            }
        });
    }

    private static void run(ServerPlayer sender, String action, AccessLevel level, Supplier<String> body) {
        AdminAction.runLocal(sender, action, level, body);
        // свежее состояние сразу (не ждать опроса вкладки)
        if (PwpPermissions.has(sender, AccessLevel.ADMIN)) {
            DataRequests.send(
                    sender,
                    MatchAdmin.DATA_TYPE,
                    MatchAdmin.toJson(sender.serverLevel()).toString());
        }
    }

    public record Start(boolean force) implements Net.C2S {
        public static void encode(Start m, FriendlyByteBuf b) {
            b.writeBoolean(m.force);
        }

        public static Start decode(FriendlyByteBuf b) {
            return new Start(b.readBoolean());
        }

        @Override
        public void handle(ServerPlayer s) {
            run(s, START, AccessLevel.ADMIN, () -> MatchAdmin.start(s.serverLevel(), force));
        }
    }

    public record Stop() implements Net.C2S {
        public static void encode(Stop m, FriendlyByteBuf b) {}

        public static Stop decode(FriendlyByteBuf b) {
            return new Stop();
        }

        @Override
        public void handle(ServerPlayer s) {
            run(s, STOP, AccessLevel.ADMIN, () -> MatchAdmin.stop(s.serverLevel()));
        }
    }

    public record TogglePause() implements Net.C2S {
        public static void encode(TogglePause m, FriendlyByteBuf b) {}

        public static TogglePause decode(FriendlyByteBuf b) {
            return new TogglePause();
        }

        @Override
        public void handle(ServerPlayer s) {
            run(s, PAUSE, AccessLevel.ADMIN, () -> {
                MatchAdmin.togglePause(s.serverLevel());
                return null;
            });
        }
    }

    public record SkipWaiting(boolean force) implements Net.C2S {
        public static void encode(SkipWaiting m, FriendlyByteBuf b) {
            b.writeBoolean(m.force);
        }

        public static SkipWaiting decode(FriendlyByteBuf b) {
            return new SkipWaiting(b.readBoolean());
        }

        @Override
        public void handle(ServerPlayer s) {
            run(s, SKIP_WAITING, AccessLevel.ADMIN, () -> MatchAdmin.skipWaiting(s.serverLevel(), force));
        }
    }

    public record SetMode(String mode) implements Net.C2S {
        public static void encode(SetMode m, FriendlyByteBuf b) {
            b.writeUtf(m.mode, 16);
        }

        public static SetMode decode(FriendlyByteBuf b) {
            return new SetMode(b.readUtf(16));
        }

        @Override
        public String validate() {
            return Validate.oneOf("mode", mode, AdminInput.MODE_AAS, AdminInput.MODE_INVASION);
        }

        @Override
        public void handle(ServerPlayer s) {
            run(s, SET_MODE, AccessLevel.ADMIN, () -> MatchAdmin.setMode(s.serverLevel(), mode));
        }
    }

    public record SetDefender(String team) implements Net.C2S {
        public static void encode(SetDefender m, FriendlyByteBuf b) {
            b.writeUtf(m.team, 8);
        }

        public static SetDefender decode(FriendlyByteBuf b) {
            return new SetDefender(b.readUtf(8));
        }

        @Override
        public String validate() {
            return Validate.oneOf("team", team, "BLUE", "RED");
        }

        @Override
        public void handle(ServerPlayer s) {
            run(s, SET_DEFENDER, AccessLevel.ADMIN, () -> MatchAdmin.setDefender(s.serverLevel(), team));
        }
    }

    public record SetFaction(String team, String faction) implements Net.C2S {
        public static void encode(SetFaction m, FriendlyByteBuf b) {
            b.writeUtf(m.team, 8);
            b.writeUtf(m.faction, 32);
        }

        public static SetFaction decode(FriendlyByteBuf b) {
            return new SetFaction(b.readUtf(8), b.readUtf(32));
        }

        @Override
        public String validate() {
            return Validate.first(Validate.oneOf("team", team, "BLUE", "RED"), Validate.id("faction", faction, 32));
        }

        @Override
        public void handle(ServerPlayer s) {
            run(s, SET_FACTION, AccessLevel.ADMIN, () -> MatchAdmin.setFaction(s.serverLevel(), team, faction));
        }
    }

    public record SetTickets(String team, int amount) implements Net.C2S {
        public static void encode(SetTickets m, FriendlyByteBuf b) {
            b.writeUtf(m.team, 8);
            b.writeVarInt(m.amount);
        }

        public static SetTickets decode(FriendlyByteBuf b) {
            return new SetTickets(b.readUtf(8), b.readVarInt());
        }

        @Override
        public String validate() {
            return Validate.first(
                    Validate.oneOf("team", team, "BLUE", "RED"),
                    Validate.range("amount", amount, 0, AdminInput.TICKETS_MAX));
        }

        @Override
        public void handle(ServerPlayer s) {
            run(s, SET_TICKETS, AccessLevel.ADMIN, () -> MatchAdmin.setTickets(s.serverLevel(), team, amount));
        }
    }

    /** {@code persist} — записать дефолт в конфиг «навсегда» (только OWNER). */
    public record SetWaitingSeconds(int seconds, boolean persist) implements Net.C2S {
        public static void encode(SetWaitingSeconds m, FriendlyByteBuf b) {
            b.writeVarInt(m.seconds);
            b.writeBoolean(m.persist);
        }

        public static SetWaitingSeconds decode(FriendlyByteBuf b) {
            return new SetWaitingSeconds(b.readVarInt(), b.readBoolean());
        }

        @Override
        public String validate() {
            return Validate.range("seconds", seconds, AdminInput.WAITING_MIN, AdminInput.WAITING_MAX);
        }

        @Override
        public void handle(ServerPlayer s) {
            run(
                    s,
                    SET_WAITING,
                    persist ? AccessLevel.OWNER : AccessLevel.ADMIN,
                    () -> MatchAdmin.setWaitingSeconds(s.serverLevel(), seconds, persist));
        }
    }

    /** Регистрация в канале warfare — В КОНЕЦ списка (вызывается из PacketHandler.register). */
    public static void register(Net.Registrar r) {
        r.c2s(Start.class, Start::encode, Start::decode)
                .c2s(Stop.class, Stop::encode, Stop::decode)
                .c2s(TogglePause.class, TogglePause::encode, TogglePause::decode)
                .c2s(SkipWaiting.class, SkipWaiting::encode, SkipWaiting::decode)
                .c2s(SetMode.class, SetMode::encode, SetMode::decode)
                .c2s(SetDefender.class, SetDefender::encode, SetDefender::decode)
                .c2s(SetFaction.class, SetFaction::encode, SetFaction::decode)
                .c2s(SetTickets.class, SetTickets::encode, SetTickets::decode)
                .c2s(SetWaitingSeconds.class, SetWaitingSeconds::encode, SetWaitingSeconds::decode);
    }
}
