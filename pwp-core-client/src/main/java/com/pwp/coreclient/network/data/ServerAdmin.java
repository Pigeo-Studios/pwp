package com.pwp.coreclient.network.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.pwp.api.admin.AuditTrail;
import com.pwp.api.admin.LineRing;
import com.pwp.api.core.CoreApi;
import com.pwp.api.net.Net;
import com.pwp.api.net.Validate;
import com.pwp.api.security.AccessLevel;
import com.pwp.api.security.PwpPermissions;
import java.lang.management.ManagementFactory;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import net.minecraft.ChatFormatting;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/**
 * Вкладка «Сервер» админ-панели (MODERATOR+): этот сервер (TPS/MSPT/память/игроки/аптайм — локально,
 * без core), все серверы сети (heartbeat из {@code /api/v1/admin/metrics}; core недоступен — раздел
 * пустой), последние WARN/ERROR ({@link ServerLogTail}, уже очищены от секретов), аудит-лог (ADMIN+),
 * объявление всем/команде готовым текстом (в ките нет поля ввода).
 */
public final class ServerAdmin {
    public static final String TYPE = "adminServer";
    public static final String ANNOUNCE = "server.announce";
    /** Заготовки объявлений: ключ {@code pwp_core.admin.announce.<id>} переводит клиент каждого игрока. */
    public static final List<String> TEMPLATES =
            List.of("match_soon", "restart_soon", "restart_now", "fair_play", "tech_issue", "thanks");

    static final int ERRORS_SHOWN = 30;
    static final int AUDIT_SHOWN = 50;
    static final String ERR_NO_TEAM = "pwp_core.admin.server.error.no_team";

    private ServerAdmin() {}

    static final DataRequests.Handler DATA = new DataRequests.Handler() {
        @Override
        public CompletableFuture<JsonObject> fetch(ServerPlayer p, JsonObject q) {
            // метрики сети — только админам и только если core настроен (иначе пустой ответ, не ошибка)
            if (!PwpPermissions.has(p, AccessLevel.MODERATOR) || !CoreApi.isConfigured()) return DataRequests.empty();
            return CoreApi.getAdminMetrics();
        }

        @Override
        public String toJson(ServerPlayer p, JsonObject q, JsonObject r) {
            JsonObject o = new JsonObject();
            if (!PwpPermissions.has(p, AccessLevel.MODERATOR)) {
                o.addProperty("error", AdminAction.ERR_NO_PERMISSION);
                return o.toString();
            }
            o.add("local", local(p.server));
            o.add("servers", servers(r));
            o.addProperty("coreOk", r != null && r.has("servers"));
            JsonArray errors = new JsonArray();
            for (LineRing.Line l : ServerLogTail.recent(ERRORS_SHOWN)) {
                JsonObject e = new JsonObject();
                e.addProperty("at", l.epochMs());
                e.addProperty("level", l.level());
                e.addProperty("logger", l.logger());
                e.addProperty("text", l.text());
                e.addProperty("repeats", l.repeats());
                errors.add(e);
            }
            o.add("errors", errors);
            JsonArray audit = new JsonArray();
            if (PwpPermissions.has(p, AccessLevel.ADMIN)) {
                for (AuditTrail.Entry e : AdminAudit.recent(AUDIT_SHOWN)) audit.add(AuditTrail.format(e));
            }
            o.add("audit", audit);
            JsonArray templates = new JsonArray();
            TEMPLATES.forEach(templates::add);
            o.add("templates", templates);
            return o.toString();
        }
    };

    /** Этот сервер: средний тик (MSPT за 100 тиков), TPS = min(20, 1000/MSPT), heap, игроки, аптайм. */
    static JsonObject local(MinecraftServer server) {
        JsonObject o = new JsonObject();
        double mspt = server.getAverageTickTime();
        o.addProperty("mspt", Math.round(mspt * 10) / 10.0);
        o.addProperty("tps", Math.round(Math.min(20.0, mspt <= 0 ? 20.0 : 1000.0 / mspt) * 10) / 10.0);
        Runtime rt = Runtime.getRuntime();
        o.addProperty("heapUsedMb", (rt.totalMemory() - rt.freeMemory()) >> 20);
        o.addProperty("heapMaxMb", rt.maxMemory() >> 20);
        o.addProperty("online", server.getPlayerCount());
        o.addProperty("max", server.getMaxPlayers());
        o.addProperty("uptimeSec", ManagementFactory.getRuntimeMXBean().getUptime() / 1000);
        o.addProperty("port", server.getPort());
        return o;
    }

    /** {@code servers} ответа core ({имя: {online, mspt, tps, heapUsedMb, heapMaxMb, ageSec}}) → массив. */
    static JsonArray servers(JsonObject r) {
        JsonArray out = new JsonArray();
        if (r == null || !r.has("servers") || !r.get("servers").isJsonObject()) return out;
        for (var e : r.getAsJsonObject("servers").entrySet()) {
            if (!e.getValue().isJsonObject()) continue;
            JsonObject s = e.getValue().getAsJsonObject().deepCopy();
            s.addProperty("name", e.getKey());
            out.add(s);
        }
        return out;
    }

    /**
     * Объявление: {@code template} из {@link #TEMPLATES}, {@code team} = ALL/BLUE/RED (команда — scoreboard,
     * без учёта регистра). Каждый получатель видит текст на своём языке; чат + надпись над хотбаром.
     */
    public record Announce(String template, String team) implements Net.C2S {
        public static void encode(Announce m, FriendlyByteBuf b) {
            b.writeUtf(m.template, 32);
            b.writeUtf(m.team, 8);
        }

        public static Announce decode(FriendlyByteBuf b) {
            return new Announce(b.readUtf(32), b.readUtf(8));
        }

        @Override
        public String validate() {
            if (!TEMPLATES.contains(template)) return "template: " + template;
            return Validate.oneOf("team", team, "ALL", "BLUE", "RED");
        }

        @Override
        public void handle(ServerPlayer s) {
            AdminAction.runLocal(s, ANNOUNCE, AccessLevel.MODERATOR, template + " → " + team, () -> {
                Component text = Component.translatable("pwp_core.admin.announce.prefix")
                        .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD)
                        .append(Component.translatable("pwp_core.admin.announce." + template)
                                .withStyle(ChatFormatting.YELLOW));
                int sent = 0;
                for (ServerPlayer pl : s.server.getPlayerList().getPlayers()) {
                    if (!team.equals("ALL")) {
                        String t = pl.getTeam() == null ? "" : pl.getTeam().getName();
                        if (!t.toUpperCase(Locale.ROOT).equals(team)) continue;
                    }
                    pl.sendSystemMessage(text);
                    pl.displayClientMessage(text, true);
                    sent++;
                }
                return sent == 0 && !team.equals("ALL") ? ERR_NO_TEAM : null;
            });
        }
    }

    /** Строки JSON-массива (клиенту удобно). */
    static List<String> strings(JsonArray a) {
        return a.asList().stream().map(JsonElement::getAsString).toList();
    }
}
