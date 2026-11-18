package com.pwp.api.lobby;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Канал «лобби ↔ матч-сервер» (этап 5): файлы в папке матч-сервера {@code match_NN/}. Лобби и матч-сервер
 * всегда на одной машине (лобби само копирует шаблон и запускает процесс), а core-service на тест-стенде
 * может отсутствовать — поэтому основной канал файловый (как давний {@code stop.signal}), heartbeat в
 * core-service — только дублирующая телеметрия.
 *
 * <ul>
 *   <li>{@link #AWAIT_FLAG} — пишет лобби при предпрогреве: «настройки придут позже, фазу ожидания не начинай».
 *   <li>{@link #SETTINGS_FILE} — пишет лобби (атомарно tmp→move), когда голосования закончились: фракции,
 *       режим, тикеты, защитник, паттерн. Матч-сервер вливает их в свой map_config ({@link Settings#applyTo})
 *       — правило «map_config матча переписывает лобби» прежнее, меняется только момент.
 *   <li>{@link #STATUS_FILE} — пишет матч-сервер раз в секунду: стадия, пререндер, применены ли настройки.
 * </ul>
 */
public final class MatchHandoff {
    public static final String SETTINGS_FILE = "lobby_settings.json";
    public static final String AWAIT_FLAG = "lobby_await.flag";
    public static final String STATUS_FILE = "match_status.json";

    public static final int MAX_TICKETS = 100_000;
    private static final Pattern FACTION = Pattern.compile("[a-z0-9_]{1,32}");

    private MatchHandoff() {}

    /** Итог голосований лобби для одного матча. */
    public record Settings(
            String blueFaction,
            String redFaction,
            String mode,
            int blueTickets,
            int redTickets,
            boolean invasionDefenderIsRed,
            int patternIndex) {

        public boolean invasion() {
            return "invasion".equals(mode);
        }

        public JsonObject toJson() {
            JsonObject o = new JsonObject();
            o.addProperty("blueFaction", blueFaction);
            o.addProperty("redFaction", redFaction);
            o.addProperty("mode", mode);
            o.addProperty("blueTickets", blueTickets);
            o.addProperty("redTickets", redTickets);
            o.addProperty("invasionDefender", invasionDefenderIsRed ? "RED" : "BLUE");
            o.addProperty("patternIndex", patternIndex);
            return o;
        }

        /** Разбор с проверкой; null — файл битый (причины в {@code errors}). Без «тихих» дефолтов. */
        public static Settings fromJson(JsonObject o, List<String> errors) {
            if (o == null) {
                errors.add("not an object");
                return null;
            }
            String blue = str(o, "blueFaction");
            String red = str(o, "redFaction");
            String mode = str(o, "mode");
            String def = str(o, "invasionDefender");
            Integer bt = integer(o, "blueTickets");
            Integer rt = integer(o, "redTickets");
            Integer pi = integer(o, "patternIndex");
            int before = errors.size();
            if (blue == null || !FACTION.matcher(blue).matches()) errors.add("blueFaction");
            if (red == null || !FACTION.matcher(red).matches()) errors.add("redFaction");
            if (!"aas".equals(mode) && !"invasion".equals(mode)) errors.add("mode");
            if (!"RED".equals(def) && !"BLUE".equals(def)) errors.add("invasionDefender");
            if (bt == null || bt < 0 || bt > MAX_TICKETS) errors.add("blueTickets");
            if (rt == null || rt < 0 || rt > MAX_TICKETS) errors.add("redTickets");
            if (pi == null || pi < -1) errors.add("patternIndex");
            if (errors.size() != before) return null;
            return new Settings(blue, red, mode, bt, rt, "RED".equals(def), pi);
        }

        /**
         * Влить в корень map_config матча (то же, что раньше делал кастомайзер лобби до запуска). Паттерн
         * вне массива {@code capturePointPatterns} не пишется (валидатор раскладки иначе ругнётся на индекс).
         */
        public void applyTo(JsonObject root) {
            JsonObject teams = obj(root, "teams");
            JsonObject b = obj(teams, "BLUE");
            JsonObject r = obj(teams, "RED");
            b.addProperty("faction", blueFaction);
            r.addProperty("faction", redFaction);
            b.addProperty("tickets", blueTickets);
            r.addProperty("tickets", redTickets);
            root.addProperty("mode", mode);
            root.addProperty("modeDisplayName", invasion() ? "INVASION" : "Advance and Secure");
            root.addProperty("invasionDefender", invasionDefenderIsRed ? "RED" : "BLUE");
            JsonElement patterns = root.get("capturePointPatterns");
            int count = patterns != null && patterns.isJsonArray()
                    ? patterns.getAsJsonArray().size()
                    : 0;
            if (patternIndex >= 0 && patternIndex < count) root.addProperty("capturePointPatternIndex", patternIndex);
        }
    }

    /** Стадия матч-сервера в {@link #STATUS_FILE}. */
    public enum Stage {
        /** Мир загружен, ждёт {@link #SETTINGS_FILE} (предпрогрев). */
        AWAITING_SETTINGS,
        /** Настройки применены, идёт фаза ожидания — можно переносить игроков. */
        READY,
        /** Файл настроек битый — лобби должно показать ошибку. */
        ERROR
    }

    /** Отчёт матч-сервера. {@code prerenderTotal == 0} — пререндер не идёт (или зона не задана). */
    public record Status(Stage stage, boolean settingsApplied, int prerenderDone, int prerenderTotal, String error) {
        public JsonObject toJson(long writtenAtMs) {
            JsonObject o = new JsonObject();
            o.addProperty("stage", stage.name());
            o.addProperty("settingsApplied", settingsApplied);
            o.addProperty("prerenderDone", prerenderDone);
            o.addProperty("prerenderTotal", prerenderTotal);
            if (error != null) o.addProperty("error", error);
            o.addProperty("writtenAt", writtenAtMs);
            return o;
        }

        /** null — не разобрать (файл пишется прямо сейчас/битый): лобби просто ждёт следующего чтения. */
        public static Status fromJson(JsonObject o) {
            if (o == null) return null;
            String st = str(o, "stage");
            Stage stage = null;
            if (st != null) {
                for (Stage s : Stage.values()) if (s.name().equals(st)) stage = s;
            }
            if (stage == null) return null;
            Integer done = integer(o, "prerenderDone");
            Integer total = integer(o, "prerenderTotal");
            JsonElement applied = o.get("settingsApplied");
            boolean ap = applied != null
                    && applied.isJsonPrimitive()
                    && applied.getAsJsonPrimitive().isBoolean()
                    && applied.getAsBoolean();
            int t = total == null ? 0 : Math.max(0, total);
            int d = done == null ? 0 : Math.max(0, Math.min(done, t));
            return new Status(stage, ap, d, t, str(o, "error"));
        }
    }

    private static JsonObject obj(JsonObject parent, String key) {
        JsonElement e = parent.get(key);
        if (e != null && e.isJsonObject()) return e.getAsJsonObject();
        JsonObject o = new JsonObject();
        parent.add(key, o);
        return o;
    }

    private static String str(JsonObject o, String key) {
        JsonElement e = o.get(key);
        return e != null && e.isJsonPrimitive() && e.getAsJsonPrimitive().isString() ? e.getAsString() : null;
    }

    private static Integer integer(JsonObject o, String key) {
        JsonElement e = o.get(key);
        if (e == null || !e.isJsonPrimitive() || !e.getAsJsonPrimitive().isNumber()) return null;
        double d = e.getAsDouble();
        if (d != Math.rint(d) || Math.abs(d) > Integer.MAX_VALUE) return null;
        return (int) d;
    }
}
