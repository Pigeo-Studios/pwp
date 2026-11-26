package com.pwp.api.config;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import java.util.List;

/**
 * Правила публичного сервера (этап 10 ROADMAP) — секция {@code community} файла {@code config/pwp-server.json}.
 * Одинаковый файл на лобби и матчах; 0 в числовом поле = выключено.
 *
 * <pre>
 * "community": {
 *   "afkKickMinutes": 10,        // AFK в матче дольше — кик (за afkWarnSeconds до — предупреждение)
 *   "afkWarnSeconds": 60,
 *   "reportCooldownSeconds": 60, // жалоба от одного игрока не чаще
 *   "teamkillBanMinutes": 60,    // на пороге тимкиллов (teamkillSpectatorThreshold) — бан вместо наблюдателя
 *   "seedingBelow": 12,          // онлайн лобби меньше — в голосовании только малые режимы (если карта их объявила)
 *   "reservedSlots": 4,          // слоты матча сверх публичного лимита — для модераторов и выше
 *   "chatFilter": true,          // мат/оскорбления — звёздочки (список — config/pwp_chat_filter.txt)
 *   "chatMessagesPer5s": 4,      // антиспам; повтор того же сообщения за 30 с — тоже отказ
 *   "vehicleClaim": true         // технику, которую повёл отряд, водит только этот отряд
 * }
 * </pre>
 */
public record CommunitySettings(
        int afkKickMinutes,
        int afkWarnSeconds,
        int reportCooldownSeconds,
        int teamkillBanMinutes,
        int seedingBelow,
        int reservedSlots,
        boolean chatFilter,
        int chatMessagesPer5s,
        boolean vehicleClaim) {

    public static final CommunitySettings DEFAULT = new CommunitySettings(10, 60, 60, 60, 12, 4, true, 4, true);

    public CommunitySettings {
        afkKickMinutes = clamp(afkKickMinutes, 0, 240);
        afkWarnSeconds = clamp(afkWarnSeconds, 0, 600);
        reportCooldownSeconds = clamp(reportCooldownSeconds, 0, 3600);
        teamkillBanMinutes = clamp(teamkillBanMinutes, 0, 60 * 24 * 30);
        seedingBelow = clamp(seedingBelow, 0, 200);
        reservedSlots = clamp(reservedSlots, 0, 50);
        chatMessagesPer5s = clamp(chatMessagesPer5s, 0, 100);
    }

    public JsonObject toJson() {
        JsonObject o = new JsonObject();
        o.addProperty("afkKickMinutes", afkKickMinutes);
        o.addProperty("afkWarnSeconds", afkWarnSeconds);
        o.addProperty("reportCooldownSeconds", reportCooldownSeconds);
        o.addProperty("teamkillBanMinutes", teamkillBanMinutes);
        o.addProperty("seedingBelow", seedingBelow);
        o.addProperty("reservedSlots", reservedSlots);
        o.addProperty("chatFilter", chatFilter);
        o.addProperty("chatMessagesPer5s", chatMessagesPer5s);
        o.addProperty("vehicleClaim", vehicleClaim);
        return o;
    }

    /** Разбор секции; null — дефолт. Битое поле — дефолт и запись в {@code warnings}. */
    public static CommunitySettings fromJson(JsonObject o, List<String> warnings) {
        if (o == null) return DEFAULT;
        CommunitySettings d = DEFAULT;
        return new CommunitySettings(
                integer(o, "afkKickMinutes", d.afkKickMinutes, warnings),
                integer(o, "afkWarnSeconds", d.afkWarnSeconds, warnings),
                integer(o, "reportCooldownSeconds", d.reportCooldownSeconds, warnings),
                integer(o, "teamkillBanMinutes", d.teamkillBanMinutes, warnings),
                integer(o, "seedingBelow", d.seedingBelow, warnings),
                integer(o, "reservedSlots", d.reservedSlots, warnings),
                bool(o, "chatFilter", d.chatFilter, warnings),
                integer(o, "chatMessagesPer5s", d.chatMessagesPer5s, warnings),
                bool(o, "vehicleClaim", d.vehicleClaim, warnings));
    }

    private static boolean bool(JsonObject o, String key, boolean def, List<String> warnings) {
        JsonElement e = o.get(key);
        if (e == null) return def;
        if (e.isJsonPrimitive() && e.getAsJsonPrimitive().isBoolean()) return e.getAsBoolean();
        warnings.add("community." + key + ": expected true/false, got " + e);
        return def;
    }

    private static int integer(JsonObject o, String key, int def, List<String> warnings) {
        JsonElement e = o.get(key);
        if (e == null) return def;
        if (e.isJsonPrimitive()) {
            JsonPrimitive p = e.getAsJsonPrimitive();
            if (p.isNumber()) {
                double v = p.getAsDouble();
                if (v == Math.rint(v) && !Double.isInfinite(v)) return (int) Math.max(-1e9, Math.min(1e9, v));
            }
        }
        warnings.add("community." + key + ": expected integer, got " + e);
        return def;
    }

    private static int clamp(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }
}
