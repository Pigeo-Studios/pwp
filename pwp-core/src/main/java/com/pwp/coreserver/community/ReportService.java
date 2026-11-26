package com.pwp.coreserver.community;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.pwp.api.ServerRole;
import com.pwp.api.core.CoreApi;
import com.pwp.api.security.AccessLevel;
import com.pwp.api.security.PwpPermissions;
import com.pwp.api.time.GameClock;
import com.pwp.api.util.ServerThread;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Жалобы игроков (этап 10a). Игрок жалуется на игрока онлайн этого сервера (экран «Пожаловаться» в паузе или
 * {@code /report}): кулдаун ({@code community.reportCooldownSeconds}), запись в core ({@code player_reports} —
 * её видят F6 «Жалобы» на любом сервере и боты TG/Discord) и сразу сообщение модераторам онлайн на этом
 * сервере. Пока core недоступен — жалоба остаётся в памяти сервера (последние {@link #LOCAL_MAX}) и видна в F6.
 * Автоматические жалобы (тимкиллы) идут от имени «PWP».
 */
public final class ReportService {
    private static final Logger LOG = LoggerFactory.getLogger(ReportService.class);
    public static final List<String> REASONS = List.of("teamkill", "grief", "cheat", "toxic", "spam", "afk", "rules");
    public static final String SYSTEM_UUID = "00000000-0000-0000-0000-000000000000";
    public static final String SYSTEM_NAME = "PWP";
    static final int LOCAL_MAX = 50;

    public static final String ERR_COOLDOWN = "pwp_core.report.error.cooldown";
    public static final String ERR_OFFLINE = "pwp_core.report.error.offline";
    public static final String ERR_SELF = "pwp_core.report.error.self";
    public static final String ERR_REASON = "pwp_core.report.error.reason";

    private static final Map<String, Long> LAST = new ConcurrentHashMap<>();
    private static final Deque<JsonObject> LOCAL = new ArrayDeque<>();

    private ReportService() {}

    /** Жалоба игрока: null — принята (игроку уже сказано), иначе ключ причины отказа. */
    public static String report(ServerPlayer reporter, String targetName, String reason, String details) {
        if (!REASONS.contains(reason)) return ERR_REASON;
        ServerPlayer target = reporter.server.getPlayerList().getPlayerByName(targetName);
        if (target == null) return ERR_OFFLINE;
        if (target.getUUID().equals(reporter.getUUID())) return ERR_SELF;
        long now = GameClock.realMillis();
        long cd = ServerRole.settings().community().reportCooldownSeconds() * 1000L;
        Long last = LAST.get(reporter.getStringUUID());
        if (last != null && now - last < cd && !PwpPermissions.has(reporter, AccessLevel.MODERATOR)) {
            return ERR_COOLDOWN;
        }
        LAST.put(reporter.getStringUUID(), now);
        submit(
                reporter.server,
                reporter.getStringUUID(),
                reporter.getGameProfile().getName(),
                target,
                reason,
                details);
        reporter.sendSystemMessage(Component.translatable(
                        "pwp_core.report.sent", target.getGameProfile().getName())
                .withStyle(ChatFormatting.GREEN));
        return null;
    }

    /** Жалоба от системы (лестница тимкиллов и т.п.). */
    public static void auto(ServerPlayer target, String reason, String details) {
        submit(target.server, SYSTEM_UUID, SYSTEM_NAME, target, reason, details);
    }

    private static void submit(
            MinecraftServer server,
            String reporterUuid,
            String reporterName,
            ServerPlayer target,
            String reason,
            String details) {
        JsonObject r = new JsonObject();
        r.addProperty("server", serverName());
        r.addProperty("reporterUuid", reporterUuid);
        r.addProperty("reporterName", reporterName);
        r.addProperty("targetUuid", target.getStringUUID());
        r.addProperty("targetName", target.getGameProfile().getName());
        r.addProperty("reason", reason);
        r.addProperty("details", details == null ? "" : details);
        r.addProperty("createdAt", GameClock.realMillis());
        synchronized (LOCAL) {
            LOCAL.addFirst(r.deepCopy());
            while (LOCAL.size() > LOCAL_MAX) LOCAL.removeLast();
        }
        LOG.info(
                "Жалоба: {} → {} ({}) {}", reporterName, target.getGameProfile().getName(), reason, details);
        notifyStaff(server, reporterName, target.getGameProfile().getName(), reason);
        ServerThread.thenOnServer(CoreApi.sendReport(r), server, resp -> {
            if (!CoreApi.isSuccess(resp)) LOG.warn("Жалоба не записана в core (останется только на этом сервере)");
        });
    }

    private static void notifyStaff(MinecraftServer server, String from, String target, String reason) {
        Component msg = Component.translatable(
                        "pwp_core.report.staff",
                        from,
                        target,
                        Component.translatable("pwp_core.report.reason." + reason))
                .withStyle(s -> s.withColor(ChatFormatting.GOLD)
                        .withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/tp " + target))
                        .withHoverEvent(new HoverEvent(
                                HoverEvent.Action.SHOW_TEXT, Component.translatable("pwp_core.report.staff_hover"))));
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            if (PwpPermissions.has(p, AccessLevel.MODERATOR)) p.sendSystemMessage(msg);
        }
    }

    /** Локальные жалобы (новые сверху) — для F6, когда core недоступен. */
    public static JsonArray local() {
        JsonArray a = new JsonArray();
        synchronized (LOCAL) {
            LOCAL.forEach(a::add);
        }
        return a;
    }

    public static void forget(ServerPlayer p) {
        LAST.remove(p.getStringUUID());
    }

    /** Имя сервера для жалобы: папка (match_01, lobby). */
    static String serverName() {
        Path dir = Path.of("").toAbsolutePath().getFileName();
        return dir == null ? "server" : dir.toString();
    }
}
