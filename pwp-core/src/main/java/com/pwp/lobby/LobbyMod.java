package com.pwp.lobby;

import com.mojang.brigadier.Command;
import com.pwp.api.core.CoreApi;
import com.pwp.api.core.Heartbeat;
import com.pwp.api.core.MatchPolicy;
import com.pwp.coreclient.network.LobbyStatePacket;
import com.pwp.coreclient.network.PacketHandler;
import com.pwp.lobby.maps.MapConfig;
import com.pwp.lobby.maps.MapRegistry;
import com.pwp.lobby.match.MatchAllocator;
import com.pwp.lobby.match.MatchAllocator.MatchInfo;
import java.util.*;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.server.ServerLifecycleHooks;

/** Лобби (часть pwp-core). Создаётся только на выделенном сервере вне папки {@code match_*}. */
public class LobbyMod {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(LobbyMod.class);
    private static boolean isMatchServer = false;

    // Mode voting state
    private static boolean modeVoteActive = false;
    private static boolean modeVoteFinished = false;
    private static long modeVoteStartTime = 0;
    private static int modeVoteDurationSec = 120;
    private static final Map<UUID, String> modeVotes = new HashMap<>();

    public static boolean hasVotedMode(java.util.UUID uuid) {
        return modeVotes.containsKey(uuid);
    }

    private static String modeVoteWinner = null;
    private static String pendingMapName = null;
    private static long modeVoteLastTimerBroadcast = 0;
    private static int modeVoteLastBroadcastedRemaining = -1;

    private static final String[] MODE_NAMES = {"aas", "invasion"};
    private static final String[] MODE_DISPLAY_NAMES = {"Advance and Secure", "INVASION"};
    private static final String[] MODE_DESCRIPTIONS = {
        "Обе команды на равных. Захватывайте точки в порядке очереди, чтобы обнулить тикеты противника. Есть ticket bleed — команда без точек теряет тикеты.",
        "Одна команда защищает все точки, вторая штурмует. Захваченные точки блокируются. Нет ticket bleed. Атакующие получают +100 билетов за захват, защитники теряют всё при потере последней точки."
    };

    // Результаты последнего завершённого голосования (для экрана итогов)
    private static String resMap = null, resMode = null, resF1 = null, resF2 = null;
    private static int resMapVotes = 0, resModeVotes = 0, resF1Votes = 0, resF2Votes = 0;

    public static void recordResult(String what, String name, int votes) {
        if (name == null) return;
        switch (what) {
            case "MAP" -> {
                resMap = name;
                resMapVotes = votes;
            }
            case "MODE" -> {
                resMode = name;
                resModeVotes = votes;
            }
            case "F1" -> {
                resF1 = name;
                resF1Votes = votes;
            }
            case "F2" -> {
                resF2 = name;
                resF2Votes = votes;
            }
        }
    }

    /** Любое из голосований активно — новые голосования стартовать нельзя. */
    public static boolean isAnyVoteActive() {
        return VotingManager.isActive() || modeVoteActive || FactionVotingManager.isActive();
    }

    public LobbyMod() {
        // Пакеты core-client (переход в матч) вызывают лобби через интерфейс, без рефлексии
        com.pwp.api.lobby.LobbyActions.install(new LobbyActionsImpl());
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::commonSetup);
        MinecraftForge.EVENT_BUS.register(this);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            isMatchServer = com.pwp.api.ServerRole.isMatchServer();

            if (isMatchServer) {
                return;
            }

            // папка карт: mapsDir из config/pwp-server.json, пусто — прежний авто-поиск
            String configured = com.pwp.api.ServerRole.settings().mapsDir();
            String[] tryPaths = configured.isBlank()
                    ? new String[] {"../maps", "../PWP-Server/maps", "maps"}
                    : new String[] {configured};
            String foundPath = null;
            for (String p : tryPaths) {
                var f = new java.io.File(p);
                if (f.isDirectory()) {
                    foundPath = p;
                    break;
                }
            }
            if (foundPath == null) {
                foundPath = configured.isBlank() ? "../PWP-Server/maps" : configured;
                log.warn("Папка карт не найдена: {} (mapsDir в config/pwp-server.json)", foundPath);
            }

            MapRegistry.configure(foundPath);
            MapRegistry.loadAll();
            // настройки лобби — секция lobby файла config/pwp-server.json (до 25.11.2026 — config/pwp_lobby.json)
            ServerConfig.load();
            // админ-панель: вкладка «Лобби» видна только на лобби-сервере
            LobbyActionsImpl.registerData();
            com.pwp.coreclient.network.data.AdminInfo.addFeature(
                    com.pwp.coreclient.network.data.AdminInfo.FEATURE_LOBBY);
        });
    }

    private int heartbeatTicks = 0;
    private int autoVoteTicks = 0;

    /** Авто-голосование не раньше этого момента (пауза после сбоя подготовки / ручной остановки). */
    private static long autoVoteNotBefore = 0;

    private static final long AUTO_VOTE_PAUSE_AFTER_FAIL_MS = 60_000;
    private static final long AUTO_VOTE_PAUSE_AFTER_STOP_MS = 300_000;

    /**
     * Авто-цикл (этап 5): автостарт вкл., лобби свободно, игроков ≥ minPlayers, пауза прошла — начать
     * голосование. Зовётся при входе игрока, конце матча и раз в 5 с (лобби «добрало» игроков — цикл пошёл сам).
     */
    public static void maybeAutoStartVote() {
        boolean busy = MatchAllocator.hasActiveMatch() || isAnyVoteActive() || MatchAllocator.isCycleBusy();
        if (!com.pwp.api.lobby.LobbyCycle.autoVoteAllowed(
                ServerConfig.isAutoStartEnabled(),
                busy,
                MatchAllocator.getLobbyPlayerCount(),
                ServerConfig.getMinPlayers(),
                com.pwp.api.time.GameClock.realMillis(),
                autoVoteNotBefore)) return;
        if (!com.pwp.api.core.MatchPolicy.canStartNewMatch()) return; // причина видна в GUI (policyReason)
        if (MapRegistry.getVotable().isEmpty()) return;
        VotingManager.startVoting();
    }

    /** Подготовка матча сорвалась окончательно: голосования этого цикла снять, авто-цикл — после паузы. */
    public static void onCycleAborted() {
        autoVoteNotBefore = com.pwp.api.time.GameClock.realMillis() + AUTO_VOTE_PAUSE_AFTER_FAIL_MS;
        stopVotesQuietly();
    }

    private static void stopVotesQuietly() {
        if (VotingManager.isActive()) VotingManager.stopVoting();
        if (modeVoteActive) {
            modeVoteActive = false;
            modeVotes.clear();
        }
        if (FactionVotingManager.isActive()) FactionVotingManager.stop();
        pendingMapName = null;
    }

    private int stateBroadcastTicks = 0;

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) return;
        VotingManager.tick();
        FactionVotingManager.tick();
        tickModeVote();
        MatchAllocator.tick();
        if (++autoVoteTicks >= 100) {
            autoVoteTicks = 0;
            maybeAutoStartVote();
        }

        // Авторитетное состояние лобби — раз в секунду всем игрокам
        if (++stateBroadcastTicks >= 20) {
            stateBroadcastTicks = 0;
            try {
                broadcastLobbyState();
            } catch (RuntimeException e) {
                // тик не ломаем, но причину видно в логе
                log.warn("Не удалось разослать состояние лобби", e);
            }
        }

        if (++heartbeatTicks >= 200) {
            heartbeatTicks = 0;
            int online = MatchAllocator.getLobbyPlayerCount();
            try {
                MatchInfo mi = MatchAllocator.getActiveMatch();
                if (mi != null) {
                    CoreApi.sendHeartbeat(new Heartbeat("lobby", online)
                            .perf(lobbyTickMs())
                            .match(
                                    mi.mapName,
                                    mi.mode,
                                    mi.blueFaction,
                                    mi.redFaction,
                                    mi.blueTickets,
                                    mi.redTickets,
                                    mi.phase.name(),
                                    mi.maxPlayers,
                                    mi.startedAt,
                                    mi.playerCount));
                } else {
                    CoreApi.sendHeartbeat(new Heartbeat("lobby", online).perf(lobbyTickMs()));
                }
            } catch (RuntimeException e) {
                log.warn("Не удалось отправить heartbeat лобби", e);
            }
        }
    }

    // ====== Авторитетное состояние лобби ======

    /** Собрать полное состояние лобби для GUI. Сервер — единственный источник истины. */
    public static LobbyStatePacket buildStatePacket(boolean requestOpen) {
        return buildStatePacket(requestOpen, null);
    }

    /** Состояние лобби с персональным голосом игрока (playerUuid может быть null — голос не заполняется). */
    public static LobbyStatePacket buildStatePacket(boolean requestOpen, UUID playerUuid) {
        MatchInfo mi = MatchAllocator.getActiveMatch();

        int phase;
        int remaining = 0;
        int totalVotes = 0;
        String leading = null;
        if (mi != null) {
            phase = mi.phase == MatchAllocator.MatchPhase.STARTING
                    ? LobbyStatePacket.PHASE_MATCH_STARTING
                    : LobbyStatePacket.PHASE_MATCH_PLAYING;
        } else if (VotingManager.isActive()) {
            phase = LobbyStatePacket.PHASE_MAP_VOTE;
            remaining = VotingManager.getRemainingSeconds();
            totalVotes = VotingManager.getVoteCount();
            leading = VotingManager.getLeadingMap();
        } else if (modeVoteActive) {
            phase = LobbyStatePacket.PHASE_MODE_VOTE;
            remaining = getModeVoteRemainingSeconds();
            totalVotes = modeVotes.size();
        } else if (FactionVotingManager.isActive()) {
            phase = LobbyStatePacket.PHASE_FACTION_VOTE;
            remaining = FactionVotingManager.getRemainingSeconds();
            totalVotes = FactionVotingManager.getVoteCount();
        } else {
            phase = LobbyStatePacket.PHASE_IDLE;
        }

        String mode = modeVoteWinner != null ? modeVoteWinner : (mi != null ? mi.mode : "aas");

        // Карты (всегда — клиент может открыть вкладку в любой момент)
        List<MapConfig> maps = MapRegistry.getVotable();
        int ml = maps.size();
        String[] mapNames = new String[ml], mapDisplayNames = new String[ml], mapDescriptions = new String[ml];
        String[] worldPaths = new String[ml], blueFactions = new String[ml], redFactions = new String[ml];
        int[] maxPlayers = new int[ml], voteCounts = new int[ml];
        for (int i = 0; i < ml; i++) {
            MapConfig cfg = maps.get(i);
            mapNames[i] = cfg.name;
            mapDisplayNames[i] = cfg.displayName;
            mapDescriptions[i] = cfg.description != null ? cfg.description : "";
            worldPaths[i] = cfg.worldPath;
            blueFactions[i] = cfg.teams.BLUE.faction;
            redFactions[i] = cfg.teams.RED.faction;
            maxPlayers[i] = cfg.maxPlayers;
            voteCounts[i] = VotingManager.getVoteCountForMap(cfg.name);
        }

        // Режимы
        int modeCountsLen = MODE_NAMES.length;
        int[] modeVoteCounts = new int[modeCountsLen];
        for (int i = 0; i < modeCountsLen; i++) {
            String mn = MODE_NAMES[i];
            modeVoteCounts[i] =
                    (int) modeVotes.values().stream().filter(v -> v.equals(mn)).count();
        }

        // Фракции
        String[] t1f = FactionVotingManager.getTeam1Factions();
        String[] t2f = FactionVotingManager.getTeam2Factions();
        int[] t1v = FactionVotingManager.getTeam1Votes();
        int[] t2v = FactionVotingManager.getTeam2Votes();

        // Матч
        String matchMapDisplay = "",
                matchModeDisplay = "",
                matchWorldPath = "",
                matchBlueFaction = "",
                matchRedFaction = "",
                matchStatus = "";
        int matchBlueTickets = 0,
                matchRedTickets = 0,
                matchServerId = -1,
                matchElapsed = 0,
                matchPlayers = 0,
                matchMaxPlayers = 0;
        boolean matchCanJoin = false;
        if (mi != null) {
            matchMapDisplay = mi.displayName;
            matchModeDisplay = mi.modeDisplayName;
            matchWorldPath = mi.worldPath != null ? mi.worldPath : "";
            matchBlueFaction = mi.blueFaction;
            matchRedFaction = mi.redFaction;
            matchBlueTickets = mi.blueTickets;
            matchRedTickets = mi.redTickets;
            matchServerId = mi.serverId;
            matchElapsed = mi.getElapsedSeconds();
            matchPlayers = mi.playerCount;
            matchMaxPlayers = mi.maxPlayers;
            matchCanJoin = mi.phase == MatchAllocator.MatchPhase.PLAYING;
            matchStatus = matchCanJoin ? "PLAYING" : "STARTING";
        }

        // Персональный голос игрока
        int myMapVote = -1, myModeVote = -1, myFaction1 = -1, myFaction2 = -1;
        if (playerUuid != null) {
            String mapVote = VotingManager.getVote(playerUuid);
            if (mapVote != null) {
                for (int i = 0; i < ml; i++) {
                    if (mapNames[i].equals(mapVote)) {
                        myMapVote = i;
                        break;
                    }
                }
            }
            String modeVote = modeVotes.get(playerUuid);
            if (modeVote != null) {
                for (int i = 0; i < MODE_NAMES.length; i++) {
                    if (MODE_NAMES[i].equals(modeVote)) {
                        myModeVote = i;
                        break;
                    }
                }
            }
            String[] factVote = FactionVotingManager.getVote(playerUuid);
            if (factVote != null) {
                for (int i = 0; i < t1f.length; i++) {
                    if (t1f[i].equals(factVote[0])) {
                        myFaction1 = i;
                        break;
                    }
                }
                for (int i = 0; i < t2f.length; i++) {
                    if (t2f[i].equals(factVote[1])) {
                        myFaction2 = i;
                        break;
                    }
                }
            }
        }

        // Список активных матчей (активный матч лобби показывается отдельной карточкой — исключаем дубль)
        var matches = MatchAllocator.getActiveMatches();
        List<MatchInfo> list = new ArrayList<>();
        for (MatchInfo m : matches.values()) {
            if (mi != null && m.serverId == mi.serverId) continue;
            if (m.phase == MatchAllocator.MatchPhase.PREPARING) continue; // предпрогрев — в prepState
            list.add(m);
        }
        int mc = list.size();
        int[] mServerIds = new int[mc], mBlueTickets = new int[mc], mRedTickets = new int[mc];
        int[] mPlayers = new int[mc], mMaxPlayers = new int[mc], mElapsed = new int[mc];
        String[] mDisplayNames = new String[mc], mStatuses = new String[mc];
        String[] mBlueFactions = new String[mc], mRedFactions = new String[mc], mWorldPaths = new String[mc];
        int idx = 0;
        for (MatchInfo m : list) {
            mServerIds[idx] = m.serverId;
            mDisplayNames[idx] = m.displayName;
            mStatuses[idx] = m.phase == MatchAllocator.MatchPhase.PLAYING ? "PLAYING" : "STARTING";
            mBlueTickets[idx] = m.blueTickets;
            mRedTickets[idx] = m.redTickets;
            mPlayers[idx] = m.playerCount;
            mMaxPlayers[idx] = m.maxPlayers;
            mElapsed[idx] = m.getElapsedSeconds();
            mBlueFactions[idx] = m.blueFaction;
            mRedFactions[idx] = m.redFaction;
            mWorldPaths[idx] = m.worldPath != null ? m.worldPath : "";
            idx++;
        }

        LobbyStatePacket pkt = new LobbyStatePacket(
                phase,
                remaining,
                MatchAllocator.getLobbyPlayerCount(),
                totalVotes,
                leading,
                MatchPolicy.canStartNewMatch(),
                MatchPolicy.reason(),
                mode,
                requestOpen,
                mapNames,
                mapDisplayNames,
                mapDescriptions,
                maxPlayers,
                voteCounts,
                worldPaths,
                blueFactions,
                redFactions,
                MODE_NAMES,
                MODE_DISPLAY_NAMES,
                MODE_DESCRIPTIONS,
                modeVoteCounts,
                t1f,
                t2f,
                t1v,
                t2v,
                myMapVote,
                myModeVote,
                myFaction1,
                myFaction2,
                resMap,
                resMapVotes,
                resMode,
                resModeVotes,
                resF1,
                resF1Votes,
                resF2,
                resF2Votes,
                matchMapDisplay,
                matchModeDisplay,
                matchWorldPath,
                matchBlueFaction,
                matchRedFaction,
                matchStatus,
                matchBlueTickets,
                matchRedTickets,
                matchServerId,
                matchElapsed,
                matchPlayers,
                matchMaxPlayers,
                matchCanJoin,
                mc,
                mServerIds,
                mDisplayNames,
                mStatuses,
                mBlueTickets,
                mRedTickets,
                mPlayers,
                mMaxPlayers,
                mElapsed,
                mBlueFactions,
                mRedFactions,
                mWorldPaths);
        pkt.minPlayers = ServerConfig.getMinPlayers();
        MatchAllocator.PrepView prep = MatchAllocator.prepView();
        pkt.prepState = prep.state();
        pkt.prepPercent = prep.percent();
        pkt.prepError = prep.errorKey();
        return pkt;
    }

    public static void broadcastLobbyState() {
        var server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;
        // Пакет персональный — каждый игрок получает свой голос в my*Vote
        server.getPlayerList().getPlayers().forEach(p -> {
            LobbyStatePacket pkt = buildStatePacket(false, p.getUUID());
            PacketHandler.INSTANCE.send(PacketDistributor.PLAYER.with(() -> p), pkt);
        });
    }

    public static void sendLobbyStateToPlayer(ServerPlayer player) {
        LobbyStatePacket pkt = buildStatePacket(true, player.getUUID());
        if (pkt != null) {
            PacketHandler.INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), pkt);
        }
    }

    @SubscribeEvent
    public void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        if (isMatchServer) return;

        String uuid = player.getStringUUID();
        String name = player.getScoreboardName();

        MatchAllocator.playerJoined(uuid);
        int online = MatchAllocator.getLobbyPlayerCount();

        broadcastPwp(ChatFormatting.WHITE, "pwp_lobby.msg.player_joined", name, online);

        // Сразу отправляем актуальное состояние новому игроку
        sendLobbyStateToPlayer(player);
        broadcastLobbyState();

        if (VotingManager.isActive()) {
            broadcastPwp(ChatFormatting.GRAY, "pwp_lobby.msg.map_vote_running", VotingManager.getRemainingSeconds());
        } else if (modeVoteActive) {
            broadcastPwp(ChatFormatting.GRAY, "pwp_lobby.msg.mode_vote_running", getModeVoteRemainingSeconds());
        } else if (FactionVotingManager.isActive()) {
            broadcastPwp(ChatFormatting.GRAY, "pwp_lobby.msg.faction_vote_running");
        }

        // Автозапуск голосования: автостарт + свободное лобби + minPlayers (раньше minPlayers тут не
        // проверялся — голосование шло с 1 игроком и молча упиралось в «мало игроков» только в конце)
        if (!isAnyVoteActive()) {
            maybeAutoStartVote();
            if (isAnyVoteActive()) sendLobbyStateToPlayer(player);
        }
    }

    @SubscribeEvent
    public void onPlayerLeave(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            MatchAllocator.playerLeft(player.getStringUUID());
            broadcastLobbyState();
        }
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        var dispatcher = event.getDispatcher();

        dispatcher.register(Commands.literal("pwp")
                .then(Commands.literal("join").executes(ctx -> {
                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                    MatchAllocator.joinActiveMatch(player);
                    return Command.SINGLE_SUCCESS;
                }))
                .then(Commands.literal("server")
                        .requires(s -> s.hasPermission(2))
                        // /pwp server start — full voting cycle
                        .then(Commands.literal("start")
                                .executes(ctx -> {
                                    if (MatchAllocator.hasActiveMatch()) {
                                        ctx.getSource()
                                                .sendFailure(Component.translatable("pwp_lobby.msg.cmd.match_running"));
                                        return 0;
                                    }
                                    if (isAnyVoteActive()) {
                                        ctx.getSource()
                                                .sendFailure(Component.translatable("pwp_lobby.msg.cmd.vote_running"));
                                        return 0;
                                    }
                                    if (MatchAllocator.isCycleBusy()) {
                                        ctx.getSource()
                                                .sendFailure(Component.translatable("pwp_core.lobby.prep.error.busy"));
                                        return 0;
                                    }
                                    String err = VotingManager.startVoting();
                                    if (err != null) {
                                        // раньше startVoting молча выходил, а команда рапортовала «начато»
                                        ctx.getSource().sendFailure(Component.translatable(err));
                                        return 0;
                                    }
                                    autoVoteNotBefore = 0;
                                    ctx.getSource()
                                            .sendSuccess(
                                                    () -> Component.translatable("pwp_lobby.msg.cmd.vote_started")
                                                            .withStyle(ChatFormatting.GREEN),
                                                    false);
                                    return Command.SINGLE_SUCCESS;
                                })
                                // /pwp server start <map> — force map
                                .then(Commands.argument("map", com.mojang.brigadier.arguments.StringArgumentType.word())
                                        .suggests((ctx, builder) -> {
                                            for (MapConfig m : MapRegistry.getAll()) {
                                                builder.suggest(m.name, Component.literal(m.displayName));
                                            }
                                            return builder.buildFuture();
                                        })
                                        .executes(ctx -> {
                                            String mn = com.mojang.brigadier.arguments.StringArgumentType.getString(
                                                    ctx, "map");
                                            MapConfig map = MapRegistry.get(mn);
                                            if (map == null) {
                                                ctx.getSource()
                                                        .sendFailure(Component.translatable(
                                                                "pwp_lobby.msg.cmd.map_not_found"));
                                                return 0;
                                            }
                                            if (modeVoteActive || FactionVotingManager.isActive()) {
                                                ctx.getSource()
                                                        .sendFailure(Component.translatable(
                                                                "pwp_lobby.msg.cmd.stop_vote_first"));
                                                return 0;
                                            }
                                            VotingManager.stopVoting();
                                            // Immediately finish map vote with forced map
                                            String err = onVoteFinished(mn);
                                            if (err != null) {
                                                ctx.getSource().sendFailure(Component.translatable(err));
                                                return 0;
                                            }
                                            ctx.getSource()
                                                    .sendSuccess(
                                                            () -> Component.translatable(
                                                                            "pwp_lobby.msg.cmd.map_forced",
                                                                            map.displayName)
                                                                    .withStyle(ChatFormatting.GREEN),
                                                            false);
                                            return Command.SINGLE_SUCCESS;
                                        })
                                        // /pwp server start <map> <mode> — force map+mode
                                        .then(Commands.argument(
                                                        "mode",
                                                        com.mojang.brigadier.arguments.StringArgumentType.word())
                                                .suggests((ctx, builder) -> {
                                                    builder.suggest("aas");
                                                    builder.suggest("invasion");
                                                    return builder.buildFuture();
                                                })
                                                .executes(ctx -> {
                                                    String mn =
                                                            com.mojang.brigadier.arguments.StringArgumentType.getString(
                                                                    ctx, "map");
                                                    String md =
                                                            com.mojang.brigadier.arguments.StringArgumentType.getString(
                                                                    ctx, "mode");
                                                    if (!md.equals("aas") && !md.equals("invasion")) {
                                                        ctx.getSource()
                                                                .sendFailure(Component.translatable(
                                                                        "pwp_lobby.msg.cmd.mode_invalid"));
                                                        return 0;
                                                    }
                                                    MapConfig map = MapRegistry.get(mn);
                                                    if (map == null) {
                                                        ctx.getSource()
                                                                .sendFailure(Component.translatable(
                                                                        "pwp_lobby.msg.cmd.map_not_found"));
                                                        return 0;
                                                    }
                                                    if (isAnyVoteActive()) {
                                                        ctx.getSource()
                                                                .sendFailure(Component.translatable(
                                                                        "pwp_lobby.msg.cmd.stop_vote_first"));
                                                        return 0;
                                                    }
                                                    // Force map + mode: simulate vote results
                                                    pendingMapName = mn;
                                                    modeVoteWinner = md;
                                                    LobbyMod.broadcastPwp(
                                                            ChatFormatting.GREEN,
                                                            "pwp_lobby.msg.forced_map_mode",
                                                            map.displayName,
                                                            md.toUpperCase());
                                                    String err = prewarmIfEnabled(map);
                                                    List<String> available = getAvailableFactions(map);
                                                    if (err == null && available.size() >= 2) {
                                                        FactionVotingManager.startFactionVoting(mn, available);
                                                    } else if (err == null) {
                                                        err = startMatchAfterFactionVote(map, "usa", "russia");
                                                    }
                                                    if (err != null) {
                                                        ctx.getSource().sendFailure(Component.translatable(err));
                                                        return 0;
                                                    }
                                                    ctx.getSource()
                                                            .sendSuccess(
                                                                    () -> Component.translatable(
                                                                                    "pwp_lobby.msg.cmd.map_mode_forced")
                                                                            .withStyle(ChatFormatting.GREEN),
                                                                    false);
                                                    return Command.SINGLE_SUCCESS;
                                                })
                                                // /pwp server start <map> <mode> <blue> <red> — instant match
                                                .then(Commands.argument(
                                                                "blue",
                                                                com.mojang.brigadier.arguments.StringArgumentType
                                                                        .word())
                                                        .then(Commands.argument(
                                                                        "red",
                                                                        com.mojang.brigadier.arguments
                                                                                .StringArgumentType.word())
                                                                .executes(ctx -> {
                                                                    String mn = com.mojang.brigadier.arguments
                                                                            .StringArgumentType.getString(ctx, "map");
                                                                    String md = com.mojang.brigadier.arguments
                                                                            .StringArgumentType.getString(ctx, "mode");
                                                                    String bl = com.mojang.brigadier.arguments
                                                                            .StringArgumentType.getString(ctx, "blue");
                                                                    String re = com.mojang.brigadier.arguments
                                                                            .StringArgumentType.getString(ctx, "red");
                                                                    if (!md.equals("aas") && !md.equals("invasion")) {
                                                                        ctx.getSource()
                                                                                .sendFailure(
                                                                                        Component.translatable(
                                                                                                "pwp_lobby.msg.cmd.mode_invalid_short"));
                                                                        return 0;
                                                                    }
                                                                    MapConfig map = MapRegistry.get(mn);
                                                                    if (map == null) {
                                                                        ctx.getSource()
                                                                                .sendFailure(
                                                                                        Component.translatable(
                                                                                                "pwp_lobby.msg.cmd.map_not_found"));
                                                                        return 0;
                                                                    }
                                                                    if (isAnyVoteActive()) {
                                                                        ctx.getSource()
                                                                                .sendFailure(
                                                                                        Component.translatable(
                                                                                                "pwp_lobby.msg.cmd.stop_vote_first"));
                                                                        return 0;
                                                                    }
                                                                    pendingMapName = mn;
                                                                    modeVoteWinner = md;
                                                                    String err =
                                                                            startMatchAfterFactionVote(map, bl, re);
                                                                    if (err != null) {
                                                                        // раньше: «Матч запущен» при отказе (мало
                                                                        // игроков)
                                                                        ctx.getSource()
                                                                                .sendFailure(
                                                                                        Component.translatable(err));
                                                                        return 0;
                                                                    }
                                                                    ctx.getSource()
                                                                            .sendSuccess(
                                                                                    () -> Component.translatable(
                                                                                                    "pwp_lobby.msg.cmd.match_started",
                                                                                                    map.displayName,
                                                                                                    bl,
                                                                                                    re)
                                                                                            .withStyle(
                                                                                                    ChatFormatting
                                                                                                            .GREEN),
                                                                                    false);
                                                                    return Command.SINGLE_SUCCESS;
                                                                }))))))
                        // /pwp server stop
                        .then(Commands.literal("stop").executes(ctx -> {
                            MatchInfo mi = MatchAllocator.getActiveMatch();
                            if (mi == null) {
                                ctx.getSource().sendFailure(Component.translatable("pwp_lobby.msg.cmd.no_match"));
                                return 0;
                            }
                            MatchAllocator.requestMatchStop(mi.serverId);
                            ctx.getSource()
                                    .sendSuccess(() -> Component.translatable("pwp_lobby.msg.cmd.stopping"), true);
                            return Command.SINGLE_SUCCESS;
                        }))
                        // /pwp server stopvote
                        .then(Commands.literal("stopvote").executes(ctx -> {
                            if (stopAllVotes()) {
                                ctx.getSource()
                                        .sendSuccess(
                                                () -> Component.translatable("pwp_lobby.msg.cmd.vote_stopped"), true);
                                return Command.SINGLE_SUCCESS;
                            }
                            ctx.getSource().sendFailure(Component.translatable("pwp_lobby.msg.cmd.no_vote"));
                            return 0;
                        }))
                        // /pwp server prewarm on|off — предпрогрев матч-сервера (этап 5, по умолчанию выкл.)
                        .then(Commands.literal("prewarm")
                                .then(Commands.literal("on").executes(ctx -> {
                                    ServerConfig.setPrewarm(
                                            true, ctx.getSource().hasPermission(4));
                                    ctx.getSource()
                                            .sendSuccess(
                                                    () -> Component.translatable("pwp_lobby.msg.cmd.prewarm_on"), true);
                                    return Command.SINGLE_SUCCESS;
                                }))
                                .then(Commands.literal("off").executes(ctx -> {
                                    ServerConfig.setPrewarm(
                                            false, ctx.getSource().hasPermission(4));
                                    ctx.getSource()
                                            .sendSuccess(
                                                    () -> Component.translatable("pwp_lobby.msg.cmd.prewarm_off"),
                                                    true);
                                    return Command.SINGLE_SUCCESS;
                                })))
                        // /pwp server autostart on|off
                        .then(Commands.literal("autostart")
                                .then(Commands.literal("on").executes(ctx -> {
                                    ServerConfig.setAutoStartEnabled(
                                            true, ctx.getSource().hasPermission(4));
                                    ctx.getSource()
                                            .sendSuccess(
                                                    () -> Component.translatable("pwp_lobby.msg.cmd.autostart_on"),
                                                    true);
                                    return Command.SINGLE_SUCCESS;
                                }))
                                .then(Commands.literal("off").executes(ctx -> {
                                    ServerConfig.setAutoStartEnabled(
                                            false, ctx.getSource().hasPermission(4));
                                    ctx.getSource()
                                            .sendSuccess(
                                                    () -> Component.translatable("pwp_lobby.msg.cmd.autostart_off"),
                                                    true);
                                    return Command.SINGLE_SUCCESS;
                                })))
                        // /pwp server maxmatches <n>
                        .then(Commands.literal("maxmatches")
                                .then(Commands.argument(
                                                "count", com.mojang.brigadier.arguments.IntegerArgumentType.integer(1))
                                        .executes(ctx -> {
                                            int n = com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(
                                                    ctx, "count");
                                            ServerConfig.setMaxMatches(
                                                    n, ctx.getSource().hasPermission(4));
                                            ctx.getSource()
                                                    .sendSuccess(
                                                            () -> Component.translatable(
                                                                    "pwp_lobby.msg.cmd.max_matches", n),
                                                            true);
                                            return Command.SINGLE_SUCCESS;
                                        })))
                        // /pwp server status
                        .then(Commands.literal("status").executes(ctx -> {
                            MatchInfo mi = MatchAllocator.getActiveMatch();
                            MutableComponent sb = Component.empty();
                            sb.append(pwpMsg(ChatFormatting.WHITE, "pwp_lobby.msg.status.title"))
                                    .append("\n");
                            sb.append(Component.translatable(
                                            "pwp_lobby.msg.status.matches",
                                            MatchAllocator.getActiveMatches().size(),
                                            ServerConfig.getMaxMatches()))
                                    .append("\n");
                            sb.append(Component.translatable(
                                            "pwp_lobby.msg.status.autostart",
                                            Component.translatable(
                                                    ServerConfig.isAutoStartEnabled()
                                                            ? "pwp_lobby.msg.status.on"
                                                            : "pwp_lobby.msg.status.off")))
                                    .append("\n");
                            sb.append(Component.translatable(
                                            "pwp_lobby.msg.status.prewarm",
                                            Component.translatable(
                                                    ServerConfig.isPrewarm()
                                                            ? "pwp_lobby.msg.status.on"
                                                            : "pwp_lobby.msg.status.off"),
                                            ServerConfig.getMinPlayers()))
                                    .append("\n");
                            MatchAllocator.PrepView pv = MatchAllocator.prepView();
                            if (pv.state() != 0) {
                                sb.append(Component.translatable(
                                                "pwp_lobby.msg.status.prep",
                                                pv.state(),
                                                pv.percent(),
                                                pv.errorKey() != null ? Component.translatable(pv.errorKey()) : ""))
                                        .append("\n");
                            }
                            if (mi != null) {
                                sb.append(Component.translatable(
                                                "pwp_lobby.msg.status.active",
                                                mi.displayName,
                                                mi.blueFaction,
                                                mi.redFaction))
                                        .append("\n");
                                sb.append(Component.translatable(
                                        "pwp_lobby.msg.status.phase",
                                        String.valueOf(mi.phase),
                                        mi.playerCount,
                                        mi.maxPlayers));
                            } else if (VotingManager.isActive()) {
                                sb.append(Component.translatable(
                                        "pwp_lobby.msg.status.map_vote", VotingManager.getRemainingSeconds()));
                            } else if (modeVoteActive) {
                                sb.append(Component.translatable(
                                        "pwp_lobby.msg.status.mode_vote", getModeVoteRemainingSeconds()));
                            } else if (FactionVotingManager.isActive()) {
                                sb.append(Component.translatable(
                                        "pwp_lobby.msg.status.faction_vote",
                                        FactionVotingManager.getRemainingSeconds()));
                            } else {
                                sb.append(Component.translatable("pwp_lobby.msg.status.idle"));
                            }
                            ctx.getSource().sendSuccess(() -> sb, false);
                            return Command.SINGLE_SUCCESS;
                        })))
                .executes(ctx -> {
                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                    // Без побочных эффектов: просто шлём актуальное состояние (сервер и так пушит его каждую секунду)
                    sendLobbyStateToPlayer(player);
                    return Command.SINGLE_SUCCESS;
                }));

        dispatcher.register(Commands.literal("votemap")
                .then(Commands.argument("name", com.mojang.brigadier.arguments.StringArgumentType.word())
                        .suggests((ctx, builder) -> {
                            for (MapConfig m : MapRegistry.getVotable()) {
                                builder.suggest(m.name);
                            }
                            return builder.buildFuture();
                        })
                        .executes(ctx -> {
                            String mapName = ctx.getArgument("name", String.class);
                            if (MapRegistry.get(mapName) == null) {
                                ctx.getSource()
                                        .sendFailure(Component.translatable("pwp_lobby.msg.cmd.unknown_map", mapName));
                                return 0;
                            }
                            if (!VotingManager.isActive()) {
                                ctx.getSource().sendFailure(Component.translatable("pwp_lobby.msg.cmd.vote_inactive"));
                                return 0;
                            }
                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                            if (!VotingManager.vote(player.getUUID(), mapName)) {
                                ctx.getSource()
                                        .sendFailure(Component.translatable("pwp_lobby.msg.cmd.vote_rejected")
                                                .withStyle(ChatFormatting.RED));
                                return 0;
                            }
                            ctx.getSource()
                                    .sendSuccess(
                                            () -> Component.translatable("pwp_lobby.msg.cmd.voted_for", mapName)
                                                    .withStyle(ChatFormatting.GREEN),
                                            false);
                            return Command.SINGLE_SUCCESS;
                        })));

        dispatcher.register(Commands.literal("votemode")
                .then(Commands.argument("mode", com.mojang.brigadier.arguments.StringArgumentType.word())
                        .suggests((ctx, builder) -> {
                            for (String m : MODE_NAMES) {
                                builder.suggest(m);
                            }
                            return builder.buildFuture();
                        })
                        .executes(ctx -> {
                            String modeName = ctx.getArgument("mode", String.class);
                            if (!modeVoteActive) {
                                ctx.getSource()
                                        .sendFailure(Component.translatable("pwp_lobby.msg.cmd.mode_vote_inactive"));
                                return 0;
                            }
                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                            voteMode(player.getUUID(), modeName);
                            ctx.getSource()
                                    .sendSuccess(
                                            () -> Component.translatable("pwp_lobby.msg.cmd.voted_for", modeName)
                                                    .withStyle(ChatFormatting.GREEN),
                                            false);
                            return Command.SINGLE_SUCCESS;
                        })));

        dispatcher.register(Commands.literal("votefaction")
                .then(Commands.argument("blue", com.mojang.brigadier.arguments.StringArgumentType.word())
                        .then(Commands.argument("red", com.mojang.brigadier.arguments.StringArgumentType.word())
                                .executes(ctx -> {
                                    String blue = ctx.getArgument("blue", String.class);
                                    String red = ctx.getArgument("red", String.class);
                                    if (!FactionVotingManager.isActive()) {
                                        ctx.getSource()
                                                .sendFailure(Component.translatable(
                                                        "pwp_lobby.msg.cmd.faction_vote_inactive"));
                                        return 0;
                                    }
                                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                                    FactionVotingManager.vote(player.getUUID(), blue, red);
                                    ctx.getSource()
                                            .sendSuccess(
                                                    () -> Component.translatable(
                                                                    "pwp_lobby.msg.cmd.voted_for_factions", blue, red)
                                                            .withStyle(ChatFormatting.GREEN),
                                                    false);
                                    return Command.SINGLE_SUCCESS;
                                }))));
    }

    public static void serverBroadcast(Component msg) {
        var server = ServerLifecycleHooks.getCurrentServer();
        if (server != null) {
            server.getPlayerList().getPlayers().forEach(p -> p.sendSystemMessage(msg, false));
        }
    }

    /** «[PWP] текст» из lang: префикс жёлтый, текст цвета color, аргументы-не-компоненты — жёлтые. */
    public static MutableComponent pwpMsg(ChatFormatting color, String key, Object... args) {
        Object[] a = new Object[args.length];
        for (int i = 0; i < args.length; i++) {
            a[i] = args[i] instanceof Component c
                    ? c
                    : Component.literal(String.valueOf(args[i])).withStyle(ChatFormatting.YELLOW);
        }
        return Component.literal("[PWP] ")
                .withStyle(ChatFormatting.YELLOW)
                .append(Component.translatable(key, a).withStyle(color));
    }

    public static void broadcastPwp(ChatFormatting color, String key, Object... args) {
        serverBroadcast(pwpMsg(color, key, args));
    }

    /** Список «имя (N гол.[, P%])» через « | » для итогов голосований; total <= 0 — без процентов. */
    public static MutableComponent voteList(List<Map.Entry<String, Integer>> entries, int total) {
        MutableComponent out = Component.empty();
        for (int i = 0; i < entries.size(); i++) {
            Map.Entry<String, Integer> e = entries.get(i);
            if (i > 0) out.append(Component.literal(" | ").withStyle(ChatFormatting.DARK_GRAY));
            out.append(Component.literal(e.getKey() + " ").withStyle(ChatFormatting.YELLOW));
            Component cnt = total > 0
                    ? Component.translatable("pwp_lobby.msg.vote.count_pct", e.getValue(), e.getValue() * 100 / total)
                    : Component.translatable("pwp_lobby.msg.vote.count", e.getValue());
            out.append(cnt.copy().withStyle(ChatFormatting.GRAY));
        }
        return out;
    }

    /** Карта выбрана (голосованием или форсом). null — цикл продолжается, иначе ключ ошибки. */
    public static String onVoteFinished(String mapName) {
        MapConfig map = MapRegistry.get(mapName);
        if (map == null) return "pwp_core.lobby.prep.error.no_map";

        var server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return "pwp_core.admin.error.internal";

        // этап 5: сервер начинает подниматься сразу — режим/фракции дотянутся файлом настроек
        String err = prewarmIfEnabled(map);
        if (err != null) {
            pendingMapName = null;
            autoVoteNotBefore = com.pwp.api.time.GameClock.realMillis() + AUTO_VOTE_PAUSE_AFTER_FAIL_MS;
            broadcastLobbyState();
            return err;
        }
        pendingMapName = mapName;
        startModeVoting();
        return null;
    }

    private static String prewarmIfEnabled(MapConfig map) {
        return ServerConfig.isPrewarm() ? MatchAllocator.prepare(map) : null;
    }

    // ====== MODE VOTING ======

    public static void startModeVoting() {
        if (MatchAllocator.hasActiveMatch() || isAnyVoteActive()) {
            return;
        }
        if (pendingMapName == null) return;

        modeVoteActive = true;
        modeVoteFinished = false;
        modeVoteWinner = null;
        modeVotes.clear();
        modeVoteStartTime = System.currentTimeMillis();
        modeVoteLastTimerBroadcast = 0;
        modeVoteLastBroadcastedRemaining = -1;

        String modeList = String.join(", ", MODE_DISPLAY_NAMES);
        broadcastPwp(ChatFormatting.WHITE, "pwp_lobby.msg.mode_vote_started", modeList);
        serverBroadcast(Component.translatable("pwp_lobby.msg.mode_vote_hint").withStyle(ChatFormatting.GRAY));
        broadcastLobbyState();
    }

    public static void voteMode(UUID playerUuid, String modeName) {
        if (!modeVoteActive || modeVoteFinished) return;
        boolean valid = false;
        for (String m : MODE_NAMES) {
            if (m.equals(modeName)) {
                valid = true;
                break;
            }
        }
        if (!valid) return;
        String current = modeVotes.get(playerUuid);
        if (modeName.equals(current)) return;
        modeVotes.put(playerUuid, modeName);
        broadcastLobbyState();
    }

    private static void tickModeVote() {
        if (!modeVoteActive || modeVoteFinished) return;
        long now = System.currentTimeMillis();
        long elapsed = now - modeVoteStartTime;
        int remaining = modeVoteDurationSec - (int) (elapsed / 1000);

        if (remaining != modeVoteLastBroadcastedRemaining && now - modeVoteLastTimerBroadcast > 1000) {
            if (remaining <= 5
                    || remaining == 10
                    || remaining == 15
                    || remaining == 30
                    || remaining == 60
                    || (remaining <= 120 && remaining % 60 == 0)) {
                if (remaining > 0) {
                    broadcastPwp(ChatFormatting.WHITE, "pwp_lobby.msg.mode_vote_ends_in", remaining);
                }
                modeVoteLastTimerBroadcast = now;
                modeVoteLastBroadcastedRemaining = remaining;
            }
        }

        if (elapsed >= modeVoteDurationSec * 1000L) {
            finishModeVote();
        }
    }

    private static void finishModeVote() {
        if (modeVoteFinished) return;
        modeVoteFinished = true;
        modeVoteActive = false;

        Map<String, Integer> counts = new HashMap<>();
        for (String vote : modeVotes.values()) {
            counts.merge(vote, 1, Integer::sum);
        }

        if (!counts.isEmpty()) {
            MutableComponent results = voteList(
                    counts.entrySet().stream()
                            .sorted(Map.Entry.<String, Integer>comparingByValue()
                                    .reversed())
                            .toList(),
                    0);
            broadcastPwp(ChatFormatting.WHITE, "pwp_lobby.msg.mode_vote_results", results);
        }

        modeVoteWinner = counts.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("aas");

        broadcastPwp(ChatFormatting.GREEN, "pwp_lobby.msg.mode_winner", modeVoteWinner.toUpperCase());
        recordResult("MODE", modeVoteWinner, counts.getOrDefault(modeVoteWinner, 0));
        broadcastLobbyState();

        if (pendingMapName != null) {
            MapConfig map = MapRegistry.get(pendingMapName);
            if (map != null) {
                List<String> available = (map.availableFactions != null && !map.availableFactions.isEmpty())
                        ? new ArrayList<>(map.availableFactions)
                        : new ArrayList<>(Arrays.asList("ukraine", "russia", "usa", "nato", "insurgency", "pmc"));
                if (available.size() >= 2) {
                    FactionVotingManager.startFactionVoting(pendingMapName, available);
                } else {
                    startMatchAfterFactionVote(map, "usa", "russia");
                }
            } else {
                pendingMapName = null;
                modeVotes.clear();
            }
        } else {
            pendingMapName = null;
            modeVotes.clear();
        }
    }

    private static List<String> getAvailableFactions(MapConfig map) {
        return (map.availableFactions != null && !map.availableFactions.isEmpty())
                ? new ArrayList<>(map.availableFactions)
                : new ArrayList<>(Arrays.asList("ukraine", "russia", "usa", "nato", "insurgency", "pmc"));
    }

    /** Все голосования завершены: настройки → матч-серверу. null — принято, иначе ключ ошибки (уже в чате). */
    public static String startMatchAfterFactionVote(MapConfig map, String blueFaction, String redFaction) {
        boolean isInvasion = modeVoteWinner != null && modeVoteWinner.equals("invasion");
        boolean invasionDefenderIsRed = true;

        int blueTickets = map.teams.BLUE.tickets;
        int redTickets = map.teams.RED.tickets;
        if (isInvasion && map.modes != null && map.modes.containsKey("invasion")) {
            var invConfig = map.modes.get("invasion");
            if (invasionDefenderIsRed) {
                blueTickets = invConfig.attackerTickets;
                redTickets = invConfig.defenderTickets;
            } else {
                blueTickets = invConfig.defenderTickets;
                redTickets = invConfig.attackerTickets;
            }
        }

        int patternIndex = -1;
        if (map.capturePointPatterns != null && !map.capturePointPatterns.isEmpty()) {
            patternIndex = new Random().nextInt(map.capturePointPatterns.size());
            broadcastPwp(
                    ChatFormatting.GRAY,
                    "pwp_lobby.msg.pattern_chosen",
                    map.capturePointPatterns.get(patternIndex).name);
        }

        String err = MatchAllocator.startMatch(
                map,
                blueFaction,
                redFaction,
                modeVoteWinner != null ? modeVoteWinner : "aas",
                blueTickets,
                redTickets,
                invasionDefenderIsRed,
                patternIndex);

        pendingMapName = null;
        modeVotes.clear();
        if (err != null) autoVoteNotBefore = com.pwp.api.time.GameClock.realMillis() + AUTO_VOTE_PAUSE_AFTER_FAIL_MS;
        return err;
    }

    /**
     * Остановить любое идущее голосование (карта/режим/фракции) — команда {@code stopvote} и админ-панель.
     * false — ничего не шло.
     */
    public static boolean stopAllVotes() {
        boolean any = false;
        if (VotingManager.isActive()) {
            VotingManager.stopVoting();
            any = true;
        }
        if (modeVoteActive) {
            modeVoteActive = false;
            modeVotes.clear();
            any = true;
        }
        if (FactionVotingManager.isActive()) {
            FactionVotingManager.stop();
            any = true;
        }
        pendingMapName = null;
        // сервер, поднятый под эти голосования, не нужен; авто-цикл не перезапускает голосование 5 мин
        MatchAllocator.cancelPreparing();
        if (any) {
            autoVoteNotBefore = com.pwp.api.time.GameClock.realMillis() + AUTO_VOTE_PAUSE_AFTER_STOP_MS;
            broadcastPwp(ChatFormatting.WHITE, "pwp_lobby.msg.vote_stopped_admin");
        }
        broadcastLobbyState();
        return any;
    }

    public static boolean isModeVoteActive() {
        return modeVoteActive;
    }

    static int getModeVoteRemainingSeconds() {
        if (!modeVoteActive) return 0;
        long elapsed = System.currentTimeMillis() - modeVoteStartTime;
        int remaining = modeVoteDurationSec - (int) (elapsed / 1000);
        return Math.max(0, remaining);
    }

    /** Средний MSPT лобби для heartbeat (0 — сервер не найден). */
    private static double lobbyTickMs() {
        net.minecraft.server.MinecraftServer srv = net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer();
        return srv == null ? 0 : srv.getAverageTickTime();
    }
}
