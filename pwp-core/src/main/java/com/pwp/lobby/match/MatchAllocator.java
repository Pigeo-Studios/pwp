package com.pwp.lobby.match;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.pwp.api.core.CoreApi;
import com.pwp.api.core.MatchPolicy;
import com.pwp.api.lobby.LobbyCycle;
import com.pwp.api.lobby.MatchHandoff;
import com.pwp.api.time.GameClock;
import com.pwp.api.util.ServerThread;
import com.pwp.coreclient.network.ConnectToServerPacket;
import com.pwp.coreclient.network.LobbyStatePacket;
import com.pwp.coreclient.network.PacketHandler;
import com.pwp.coreserver.TransferHosts;
import com.pwp.lobby.LobbyMod;
import com.pwp.lobby.ServerConfig;
import com.pwp.lobby.ServerManager;
import com.pwp.lobby.maps.MapConfig;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.server.ServerLifecycleHooks;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Матчи лобби. Этап 5 — цикл подготовки ({@link LobbyCycle}): карта выбрана → сервер поднимается сразу
 * (предпрогрев, флаг {@code prewarm} в {@code config/pwp-server.json}) → голосования за режим/фракции идут
 * параллельно → настройки доставляются файлом ({@link MatchHandoff}) → матч-сервер применил их и
 * пререндерил карту → игроки перебрасываются. Без предпрогрева сервер запускается после голосований уже с
 * настройками (прежний порядок), но готовность и сбои идут тем же циклом. Сбой — один повтор, затем понятная
 * ошибка в лобби (чат + LobbyStatePacket) и авто-голосование после паузы.
 */
public class MatchAllocator {

    private static final Logger log = LoggerFactory.getLogger(MatchAllocator.class);

    private static final Map<Integer, MatchInfo> activeMatches = new ConcurrentHashMap<>();
    private static final Set<String> lobbyPlayers = ConcurrentHashMap.newKeySet();
    private static int minPlayersToStart = 2;

    public static void configure(int minPlayers) {
        minPlayersToStart = minPlayers;
    }

    public static void playerJoined(String uuid) {
        lobbyPlayers.add(uuid);
    }

    public static void playerLeft(String uuid) {
        lobbyPlayers.remove(uuid);
        QUEUES.values().forEach(q -> q.leave(uuid));
    }

    // ====== Очередь в полный матч и резервные слоты (этап 10b) ======

    /** Очередь на матч (serverId → очередь). Приоритет — модераторы и выше (резервные слоты). */
    private static final Map<Integer, com.pwp.api.lobby.JoinQueue> QUEUES = new ConcurrentHashMap<>();

    private static final long QUEUE_MAX_WAIT_MS = 15 * 60_000L;
    /** Онлайн матча старше — не знаем реальной заполненности (core молчит): пускаем без очереди. */
    private static final long ONLINE_STALE_MS = 60_000L;

    private static int queueTicks;

    static int reservedSlots() {
        return com.pwp.api.ServerRole.settings().community().reservedSlots();
    }

    private static boolean staff(ServerPlayer p) {
        return com.pwp.api.security.PwpPermissions.has(p, com.pwp.api.security.AccessLevel.MODERATOR);
    }

    /** Свободные общие слоты матча; Integer.MAX_VALUE — лимит неизвестен (нет maxPlayers или онлайн устарел). */
    private static int publicFree(MatchInfo mi) {
        if (mi.maxPlayers <= 0 || GameClock.realMillis() - mi.onlineUpdatedAt > ONLINE_STALE_MS) {
            return Integer.MAX_VALUE;
        }
        return Math.max(0, mi.maxPlayers - mi.playerCount);
    }

    private static int reservedFree(MatchInfo mi) {
        if (publicFree(mi) == Integer.MAX_VALUE) return Integer.MAX_VALUE;
        return Math.max(0, reservedSlots() - Math.max(0, mi.playerCount - mi.maxPlayers));
    }

    /** Подключить сразу или поставить в очередь (с сообщением о позиции). */
    private static void connectOrQueue(ServerPlayer player, MatchInfo mi) {
        com.pwp.api.lobby.JoinQueue q = QUEUES.computeIfAbsent(mi.serverId, k -> new com.pwp.api.lobby.JoinQueue());
        boolean prio = staff(player);
        boolean free = prio ? publicFree(mi) > 0 || reservedFree(mi) > 0 : publicFree(mi) > 0;
        // перед ним никого (или он уже первый) — пускаем
        boolean first = q.size() == 0 || q.position(player.getStringUUID()) == 1;
        if (free && first) {
            q.leave(player.getStringUUID());
            mi.playerCount++; // до следующего heartbeat — чтобы не пустить лишних
            connectPlayerToMatch(player, mi, TransferHosts.resolveTransferHost(player));
            return;
        }
        int pos = q.join(player.getStringUUID(), prio, GameClock.realMillis());
        player.sendSystemMessage(
                Component.translatable("pwp_lobby.msg.queue_joined", pos, mi.playerCount, mi.maxPlayers)
                        .withStyle(ChatFormatting.YELLOW),
                false);
    }

    /** Раз в секунду: пустить из очередей, сколько есть мест; раз в 10 с — напомнить позицию. */
    private static void tickQueues() {
        if (++queueTicks % 20 != 0) return;
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;
        boolean remind = queueTicks % 200 == 0;
        for (Map.Entry<Integer, com.pwp.api.lobby.JoinQueue> e : QUEUES.entrySet()) {
            MatchInfo mi = activeMatches.get(e.getKey());
            com.pwp.api.lobby.JoinQueue q = e.getValue();
            if (mi == null || mi.phase != MatchPhase.PLAYING) {
                QUEUES.remove(e.getKey());
                continue;
            }
            for (String gone : q.expire(GameClock.realMillis(), QUEUE_MAX_WAIT_MS)) {
                ServerPlayer p = server.getPlayerList().getPlayer(UUID.fromString(gone));
                if (p != null) p.sendSystemMessage(Component.translatable("pwp_lobby.msg.queue_expired"), false);
            }
            int pub = Math.min(publicFree(mi), 64);
            int res = Math.min(reservedFree(mi), 64);
            for (String id : q.admit(pub, res)) {
                ServerPlayer p = server.getPlayerList().getPlayer(UUID.fromString(id));
                if (p == null) continue;
                mi.playerCount++;
                p.sendSystemMessage(
                        Component.translatable("pwp_lobby.msg.queue_admitted").withStyle(ChatFormatting.GREEN), false);
                connectPlayerToMatch(p, mi, TransferHosts.resolveTransferHost(p));
            }
            if (remind) {
                for (ServerPlayer p : server.getPlayerList().getPlayers()) {
                    int pos = q.position(p.getStringUUID());
                    if (pos > 0) {
                        p.displayClientMessage(
                                Component.translatable("pwp_lobby.msg.queue_position", pos, q.size())
                                        .withStyle(ChatFormatting.YELLOW),
                                true);
                    }
                }
            }
        }
    }

    /** Позиция игрока в очереди любого матча (0 — не в очереди). */
    public static int queuePosition(String uuid) {
        for (com.pwp.api.lobby.JoinQueue q : QUEUES.values()) {
            int pos = q.position(uuid);
            if (pos > 0) return pos;
        }
        return 0;
    }

    /** Матч запущен/идёт/останавливается (предпрогрев PREPARING сюда НЕ входит — голосования продолжаются). */
    public static boolean hasActiveMatch() {
        return activeMatches.values().stream()
                .anyMatch(m -> m.phase == MatchPhase.STARTING
                        || m.phase == MatchPhase.PLAYING
                        || m.phase == MatchPhase.ENDING);
    }

    public static MatchInfo getActiveMatch() {
        return activeMatches.values().stream()
                .filter(m -> m.phase == MatchPhase.STARTING || m.phase == MatchPhase.PLAYING)
                .findFirst()
                .orElse(null);
    }

    // ====== Цикл подготовки (этап 5) ======

    /** Ключи ошибок/статусов — lang core-client {@code pwp_core.lobby.prep.*}. */
    public static final String K = "pwp_core.lobby.prep.";

    private static final LobbyCycle cycle = new LobbyCycle(LobbyCycle.Config.DEFAULT);
    /** Оценка «копия + запуск до загрузки мира» для процента (тест-стенд 17.11.2026: старт → Done ≈ 50 с). */
    private static final long BOOT_ESTIMATE_MS = 90_000;

    private static final long SHOW_RESULT_MS = 30_000;

    private static MatchInfo cycleMatch;
    private static MapConfig cycleMap;
    private static MatchHandoff.Settings cycleSettings;
    private static LobbyCycle.Observed lastObserved;
    private static long lastCycleStepMs;
    private static long resultUntil;
    private static int resultState;
    private static String resultError;

    /** Состояние подготовки для LobbyStatePacket: state — {@code LobbyStatePacket.PREP_*}. */
    public record PrepView(int state, int percent, String errorKey) {}

    public static PrepView prepView() {
        long now = GameClock.realMillis();
        if (cycleMatch != null) {
            int pct = LobbyCycle.percent(now - cycle.launchedAt(), BOOT_ESTIMATE_MS, lastObserved);
            int st =
                    switch (cycle.phase()) {
                        case PREPARING -> LobbyStatePacket.PREP_PREPARING;
                        case WAITING_VOTES -> LobbyStatePacket.PREP_WAITING_VOTES;
                        case FINISHING -> LobbyStatePacket.PREP_FINISHING;
                        default -> LobbyStatePacket.PREP_NONE;
                    };
            if (st != LobbyStatePacket.PREP_NONE) return new PrepView(st, pct, null);
        }
        if (now < resultUntil) return new PrepView(resultState, 100, resultError);
        return new PrepView(LobbyStatePacket.PREP_NONE, 0, null);
    }

    /** Идёт подготовка сервера текущего цикла (до переброса). */
    public static boolean isCycleBusy() {
        return cycleMatch != null;
    }

    /** Общие проверки перед запуском процесса; null — можно. */
    private static String launchBlocker() {
        if (lobbyPlayers.size() < minPlayersToStart) return K + "error.not_enough_players";
        if (activeMatches.size() >= ServerConfig.getMaxMatches()) return K + "error.max_matches";
        if (!MatchPolicy.canStartNewMatch()) return K + "error.policy";
        return null;
    }

    /**
     * Предпрогрев: карта выбрана — сервер поднимается, пока идут голосования за режим/фракции. null — запущен
     * (или уже готовится), иначе ключ ошибки (уже разослан в чат).
     */
    public static synchronized String prepare(MapConfig map) {
        if (cycleMatch != null) return null;
        String err = launchBlocker();
        if (err != null) {
            broadcastKey(err, ChatFormatting.RED);
            return err;
        }
        err = launch(map, null, false);
        if (err != null) return err;
        broadcastKey(K + "started", ChatFormatting.GRAY);
        return null;
    }

    /** Запуск процесса для цикла; settings == null — предпрогрев (настройки придут файлом). */
    private static String launch(MapConfig map, MatchHandoff.Settings settings, boolean retry) {
        ServerManager.StartResult sr = ServerManager.startMatchServer(
                map.name,
                map.maxPlayers,
                map.worldPath,
                settings == null ? null : dir -> writeSettingsBeforeBoot(dir, map, settings),
                settings == null);
        if (sr.error != null) {
            log.error("Failed to start match server: {}", sr.error);
            broadcastKey(K + "error.start", ChatFormatting.RED);
            return K + "error.start";
        }
        MatchInfo mi = new MatchInfo();
        mi.serverId = sr.serverId;
        mi.mapName = map.name;
        mi.displayName = map.displayName;
        mi.modeDisplayName = map.modeDisplayName;
        mi.port = sr.port;
        mi.maxPlayers = map.maxPlayers;
        mi.playerCount = lobbyPlayers.size();
        mi.blueFaction = map.teams.BLUE.faction;
        mi.redFaction = map.teams.RED.faction;
        mi.blueTickets = map.teams.BLUE.tickets;
        mi.redTickets = map.teams.RED.tickets;
        mi.worldPath = map.worldPath;
        mi.mode = map.mode;
        mi.startedAt = System.currentTimeMillis();
        mi.phase = MatchPhase.PREPARING;
        cycleMatch = mi;
        cycleMap = map;
        lastObserved = null;
        activeMatches.put(mi.serverId, mi);
        long now = GameClock.realMillis();
        cycle.launched(now, retry);
        if (settings != null) {
            cycleSettings = settings;
            applySettingsToInfo(mi, settings);
            cycle.settingsDelivered(now);
        }
        log.info(
                "Match {}: {} on port {} ({})",
                mi.serverId,
                map.displayName,
                sr.port,
                settings == null ? "prewarm" : "with settings");
        LobbyMod.broadcastLobbyState();
        return null;
    }

    /** Запуск с известными настройками: файл для матч-сервера + (для старых jar матча) правка map_config. */
    private static void writeSettingsBeforeBoot(Path matchDir, MapConfig map, MatchHandoff.Settings s) {
        try {
            Files.writeString(
                    matchDir.resolve(MatchHandoff.SETTINGS_FILE), s.toJson().toString());
            Path configPath = matchDir.resolve("map_config.json");
            if (!Files.exists(configPath)) configPath = Path.of(matchDir.toString(), map.name, "map_config.json");
            if (Files.exists(configPath)) {
                JsonObject root =
                        JsonParser.parseString(Files.readString(configPath)).getAsJsonObject();
                s.applyTo(root);
                Files.writeString(
                        configPath,
                        new GsonBuilder().setPrettyPrinting().create().toJson(root));
            }
        } catch (IOException | RuntimeException e) {
            log.error("Failed to write lobby settings into {}", matchDir, e);
        }
    }

    private static void applySettingsToInfo(MatchInfo mi, MatchHandoff.Settings s) {
        mi.blueFaction = s.blueFaction();
        mi.redFaction = s.redFaction();
        mi.blueTickets = s.blueTickets();
        mi.redTickets = s.redTickets();
        mi.mode = s.mode();
        mi.modeDisplayName = com.pwp.api.match.GameModes.orAas(s.mode()).displayName();
        mi.phase = MatchPhase.STARTING;
    }

    /**
     * Голосования закончились: доставить настройки на уже поднимающийся сервер (или запустить его сейчас, если
     * предпрогрева не было). null — принято, иначе ключ ошибки (разослан в чат; команда показывает его же).
     */
    public static synchronized String startMatch(
            MapConfig map,
            String blueFaction,
            String redFaction,
            String mode,
            int blueTickets,
            int redTickets,
            boolean invasionDefenderIsRed,
            int patternIndex) {
        MatchHandoff.Settings s = new MatchHandoff.Settings(
                blueFaction, redFaction, mode, blueTickets, redTickets, invasionDefenderIsRed, patternIndex);
        if (lobbyPlayers.size() < minPlayersToStart) {
            log.info("Not enough players: {}/{}", lobbyPlayers.size(), minPlayersToStart);
            if (cycleMatch != null) abortCycle(K + "error.not_enough_players");
            else broadcastKey(K + "error.not_enough_players", ChatFormatting.RED);
            return K + "error.not_enough_players";
        }
        if (cycleMatch != null && cycleMap != null && cycleMap.name.equals(map.name) && !cycle.delivered()) {
            cycleSettings = s;
            applySettingsToInfo(cycleMatch, s);
            LobbyMod.broadcastPwp(ChatFormatting.WHITE, "pwp_lobby.msg.settings_chosen", blueFaction, redFaction, mode);
            LobbyMod.broadcastLobbyState();
            return null; // доставка — на ближайшем шаге цикла (DELIVER_SETTINGS)
        }
        if (cycleMatch != null) abortCycle(null); // предпрогрев другой карты (форс-старт) — заменяем
        String err = launchBlocker();
        if (err != null) {
            broadcastKey(err, ChatFormatting.RED);
            return err;
        }
        LobbyMod.broadcastPwp(ChatFormatting.WHITE, "pwp_lobby.msg.match_launching", map.displayName);
        return launch(map, s, false);
    }

    /** Голосование остановлено до доставки настроек — подготовленный сервер не нужен. */
    public static synchronized void cancelPreparing() {
        if (cycleMatch == null || cycle.delivered()) return;
        log.info("Prewarmed match {} cancelled (vote stopped)", cycleMatch.serverId);
        abortCycle(null);
    }

    /** Сбой без повтора (или отмена при errorKey == null): сервер снять, причину показать. */
    private static void abortCycle(String errorKey) {
        if (cycleMatch != null) {
            int id = cycleMatch.serverId;
            activeMatches.remove(id);
            ServerManager.stopServer(id);
        }
        cycleMatch = null;
        cycleMap = null;
        cycleSettings = null;
        lastObserved = null;
        cycle.reset();
        if (errorKey != null) {
            resultState = LobbyStatePacket.PREP_FAILED;
            resultError = errorKey;
            resultUntil = GameClock.realMillis() + SHOW_RESULT_MS;
            broadcast(Component.translatable(K + "failed", Component.translatable(errorKey))
                    .withStyle(ChatFormatting.RED));
            LobbyMod.onCycleAborted();
        }
        LobbyMod.broadcastLobbyState();
    }

    /** Шаг цикла раз в секунду (главный поток; статус-файл < 1 КБ). */
    private static void stepCycle() {
        long now = GameClock.realMillis();
        if (cycleMatch == null || now - lastCycleStepMs < 1000) return;
        lastCycleStepMs = now;
        MatchInfo mi = cycleMatch;
        MatchHandoff.Status st = ServerManager.readStatus(mi.serverId);
        boolean booted = ServerManager.isBooted(mi.serverId);
        // старый jar матча без статус-файла: «Done» в логе = загружен, настройки (шли до запуска) применены
        boolean applied = st != null ? st.settingsApplied() : booted && cycle.delivered();
        String srvErr = st != null && st.stage() == MatchHandoff.Stage.ERROR ? st.error() : null;
        lastObserved = new LobbyCycle.Observed(
                ServerManager.isAlive(mi.serverId),
                st != null || booted,
                applied,
                st != null ? st.prerenderDone() : 0,
                st != null ? st.prerenderTotal() : 0,
                srvErr);
        switch (cycle.step(now, lastObserved, cycleSettings != null)) {
            case DELIVER_SETTINGS -> {
                if (ServerManager.writeSettings(mi.serverId, cycleSettings)) {
                    cycle.settingsDelivered(now);
                    log.info("Lobby settings delivered to match {}", mi.serverId);
                } else {
                    abortCycle(K + "error.server");
                }
            }
            case TRANSFER -> {
                cycleMatch = null;
                cycleMap = null;
                cycleSettings = null;
                cycle.reset();
                mi.phase = MatchPhase.PLAYING;
                mi.startedAt = System.currentTimeMillis();
                resultState = LobbyStatePacket.PREP_TRANSFERRING;
                resultError = null;
                resultUntil = now + 10_000;
                broadcastKey(K + "ready", ChatFormatting.GREEN);
                log.info("Match {} ready on port {}, transferring players", mi.serverId, mi.port);
                transferPlayers(mi);
                LobbyMod.broadcastLobbyState();
            }
            case RETRY -> {
                String code = cycle.error();
                log.warn("Match {} failed ({}, {}) — retrying", mi.serverId, code, ServerManager.getError(mi.serverId));
                broadcast(Component.translatable(K + "retry", Component.translatable(K + "error." + code))
                        .withStyle(ChatFormatting.YELLOW));
                activeMatches.remove(mi.serverId);
                ServerManager.stopServer(mi.serverId);
                MatchHandoff.Settings s = cycle.delivered() ? cycleSettings : null;
                String err = launch(cycleMap, s, true);
                if (err != null) abortCycle(err);
            }
            case ABORT -> {
                String code = cycle.error();
                log.error(
                        "Match {} failed ({}, {}) — giving up", mi.serverId, code, ServerManager.getError(mi.serverId));
                abortCycle(K + "error." + code);
            }
            case NONE -> {}
        }
    }

    private static void broadcastKey(String key, ChatFormatting color) {
        broadcast(Component.translatable(key).withStyle(color));
    }

    private static void broadcast(Component c) {
        var server = ServerLifecycleHooks.getCurrentServer();
        if (server != null) server.getPlayerList().getPlayers().forEach(p -> p.sendSystemMessage(c, false));
    }

    // ====== Подключение к матчу ======

    public static void joinActiveMatch(ServerPlayer player) {
        MatchInfo mi = getActiveMatch();
        if (mi == null || mi.phase != MatchPhase.PLAYING) {
            player.sendSystemMessage(
                    Component.translatable("pwp_lobby.msg.no_match_to_join").withStyle(ChatFormatting.RED), false);
            return;
        }
        connectOrQueue(player, mi);
    }

    public static void joinMatchById(int serverId, ServerPlayer player) {
        MatchInfo mi = activeMatches.get(serverId);
        if (mi == null || mi.phase != MatchPhase.PLAYING) {
            player.sendSystemMessage(
                    Component.translatable("pwp_lobby.msg.match_unavailable").withStyle(ChatFormatting.RED), false);
            return;
        }
        connectOrQueue(player, mi);
    }

    private static void connectPlayerToMatch(ServerPlayer player, MatchInfo mi, String host) {
        // адрес хоста в чат не пишем (светит LAN-адрес) — только карта
        player.sendSystemMessage(
                Component.translatable("pwp_core.lobby.transfer", mi.displayName != null ? mi.displayName : mi.mapName)
                        .withStyle(ChatFormatting.YELLOW),
                false);
        PacketHandler.INSTANCE.send(
                PacketDistributor.PLAYER.with(() -> player), new ConnectToServerPacket(host, mi.port));
    }

    public static void tick() {
        ServerManager.tick();
        stepCycle();
        tickQueues();
        if (++statusPollTicks >= 200) {
            statusPollTicks = 0;
            updateMatchOnlineFromCore();
        }

        for (MatchInfo mi : activeMatches.values()) {
            if (mi == cycleMatch) continue; // подготовку ведёт stepCycle
            if (mi.phase == MatchPhase.ENDING) {
                if (!ServerManager.isAlive(mi.serverId)) {
                    log.info("Match {} stopped gracefully", mi.serverId);
                    matchEnded(mi.serverId);
                } else if (System.currentTimeMillis() - mi.stopRequestedAt > STOP_TIMEOUT_MS) {
                    log.warn("Match {} stop timed out, force killing", mi.serverId);
                    matchEnded(mi.serverId);
                }
                continue;
            }

            if (!ServerManager.isAlive(mi.serverId)) {
                log.warn("Match {} server is dead, cleaning up", mi.serverId);
                LobbyMod.broadcastPwp(ChatFormatting.RED, "pwp_lobby.msg.match_crashed", mi.displayName);
                matchEnded(mi.serverId);
            }
        }
    }

    private static void transferPlayers(MatchInfo mi) {
        var server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;

        // Локальные игроки получают LAN-адрес сервера (NAT-loopback роутера
        // теряет обратные пакеты после первого соединения), внешние — домен
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (!lobbyPlayers.contains(player.getStringUUID())) continue;
            connectPlayerToMatch(player, mi, TransferHosts.resolveTransferHost(player));
        }
        log.info("Transferring {} players to match {} on port {}", lobbyPlayers.size(), mi.mapName, mi.port);
    }

    private static final long STOP_TIMEOUT_MS = 30_000;

    private static int statusPollTicks = 0;

    /**
     * Живой онлайн и тикеты матчей из heartbeat'ов матч-серверов.
     * Матч-сервер шлёт heartbeat с портом — сверяем по порту.
     */
    private static void updateMatchOnlineFromCore() {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;
        // Запрос — в фоне, применение к activeMatches — на главном потоке
        ServerThread.thenOnServer(CoreApi.fetchServersStatus(), server, MatchAllocator::applyServersStatus);
    }

    private static void applyServersStatus(JsonObject resp) {
        try {
            if (resp == null || !resp.has("success") || !resp.get("success").getAsBoolean()) return;
            JsonObject data = resp.getAsJsonObject("data");
            if (data == null || !data.has("servers")) return;
            JsonArray servers = data.getAsJsonArray("servers");
            for (com.google.gson.JsonElement el : servers) {
                JsonObject s = el.getAsJsonObject();
                if (!s.has("port") || s.get("port").getAsInt() <= 0) continue;
                if (!s.has("match")) continue;
                int port = s.get("port").getAsInt();
                int online = s.get("online").getAsInt();
                for (MatchInfo mi : activeMatches.values()) {
                    if (mi.port != port || mi == cycleMatch) continue;
                    mi.playerCount = online;
                    mi.onlineUpdatedAt = GameClock.realMillis();
                    JsonObject m = s.getAsJsonObject("match");
                    if (m.has("blueScore")) mi.blueTickets = m.get("blueScore").getAsInt();
                    if (m.has("redScore")) mi.redTickets = m.get("redScore").getAsInt();
                }
            }
        } catch (RuntimeException e) {
            log.warn("Некорректный ответ /network/status", e);
        }
    }

    public static void requestMatchStop(int serverId) {
        MatchInfo mi = activeMatches.get(serverId);
        if (mi == null) return;
        if (mi == cycleMatch) {
            // остановка ещё не начавшегося матча = отмена подготовки
            abortCycle(null);
            return;
        }
        mi.phase = MatchPhase.ENDING;
        mi.stopRequestedAt = System.currentTimeMillis();
        Path dir = ServerManager.getServerDirectory(serverId);
        if (dir != null) {
            try {
                Files.writeString(dir.resolve("stop.signal"), "{\"requestedAt\":" + System.currentTimeMillis() + "}");
                log.info("Stop signal sent to match {} ({})", serverId, mi.mapName);
            } catch (Exception e) {
                log.error("Failed to write stop signal for match {}: {}", serverId, e.getMessage());
            }
        }
        LobbyMod.broadcastPwp(ChatFormatting.WHITE, "pwp_lobby.msg.match_stopping", mi.displayName);
        LobbyMod.broadcastLobbyState();
    }

    public static void matchEnded(int serverId) {
        MatchInfo mi = activeMatches.remove(serverId);
        if (mi != null) {
            ServerManager.stopServer(serverId);
            LobbyMod.broadcastPwp(ChatFormatting.WHITE, "pwp_lobby.msg.match_ended", mi.displayName);
            log.info("Match ended: {} on port {}", mi.mapName, mi.port);
        }
        LobbyMod.broadcastLobbyState();
        LobbyMod.maybeAutoStartVote();
    }

    public static Map<Integer, MatchInfo> getActiveMatches() {
        return activeMatches;
    }

    public static int getLobbyPlayerCount() {
        return lobbyPlayers.size();
    }

    public static int getMinPlayers() {
        return minPlayersToStart;
    }

    public enum MatchPhase {
        /** Предпрогрев: сервер поднимается, голосования за режим/фракции ещё идут (не «активный матч»). */
        PREPARING,
        STARTING,
        PLAYING,
        ENDING
    }

    public static class MatchInfo {
        public int serverId;
        public String mapName;
        public String displayName;
        public String modeDisplayName;
        public String mode = "aas";
        public int port;
        public int playerCount;
        public int maxPlayers;
        /** Когда онлайн последний раз пришёл из heartbeat (0 — ещё не приходил): для очереди (этап 10b). */
        public long onlineUpdatedAt;

        public String blueFaction;
        public String redFaction;
        public int blueTickets;
        public int redTickets;
        public String worldPath;
        public long startedAt;
        public long stopRequestedAt;
        public MatchPhase phase = MatchPhase.STARTING;

        public int getElapsedSeconds() {
            if (startedAt == 0) return 0;
            return (int) ((System.currentTimeMillis() - startedAt) / 1000);
        }
    }
}
