package com.pwp.lobby;

import com.pwp.coreclient.CoreAPI;
import com.pwp.coreclient.PermissionHelper;
import com.pwp.coreclient.network.OpenMatchListScreenPacket;
import com.pwp.coreclient.network.OpenMatchScreenPacket;
import com.pwp.coreclient.network.OpenModeVotePacket;
import com.pwp.coreclient.network.OpenVotingScreenPacket;
import com.pwp.coreclient.network.PacketHandler;
import com.pwp.lobby.maps.MapConfig;
import com.pwp.lobby.maps.MapRegistry;
import com.pwp.lobby.match.MatchAllocator;
import com.pwp.lobby.match.MatchAllocator.MatchInfo;
import com.mojang.brigadier.Command;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

@Mod("pwp_lobby")
public class LobbyMod {

    private static boolean isMatchServer = false;

    // Mode voting state
    private static boolean modeVoteActive = false;
    private static boolean modeVoteFinished = false;
    private static long modeVoteStartTime = 0;
    private static int modeVoteDurationSec = 120;
    private static final Map<UUID, String> modeVotes = new HashMap<>();
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

    public LobbyMod() {
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::commonSetup);
        MinecraftForge.EVENT_BUS.register(this);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            String dirName = Paths.get("").toAbsolutePath().getFileName().toString();
            isMatchServer = dirName.startsWith("match_");

            if (isMatchServer) {
                return;
            }

            String[] tryPaths = {"../maps", "../PWP-Server/maps", "maps"};
            String foundPath = null;
            for (String p : tryPaths) {
                var f = new java.io.File(p);
                if (f.isDirectory()) {
                    foundPath = p;
                    break;
                }
            }
            if (foundPath == null) foundPath = "../PWP-Server/maps";

            MapRegistry.configure(foundPath);
            MapRegistry.loadAll();
            MatchAllocator.configure(1);
        });
    }

    private int heartbeatTicks = 0;

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) return;
        VotingManager.tick();
        tickModeVote();
        MatchAllocator.tick();

        if (++heartbeatTicks >= 600) {
            heartbeatTicks = 0;
            int online = MatchAllocator.getLobbyPlayerCount();
            try {
                CoreAPI.sendHeartbeat("lobby", online);
            } catch (Exception e) {
                // silently ignore
            }
        }
    }

    public static void sendMatchListUpdateToAll() {
        OpenMatchListScreenPacket pkt = buildMatchListPacket();
        if (pkt == null) return;
        var server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;
        server.getPlayerList().getPlayers().forEach(p ->
            PacketHandler.INSTANCE.send(PacketDistributor.PLAYER.with(() -> p), pkt));
    }

    private static OpenMatchScreenPacket buildMatchScreenPacket() {
        MatchAllocator.MatchInfo mi = MatchAllocator.getActiveMatch();

        String mapDisplayName, modeDisplayName, status;
        int remainingSec = 0, blueTickets = 0, redTickets = 0, online = 0;
        boolean canJoin = false;
        String blueFaction = "", redFaction = "";

        if (mi != null) {
            mapDisplayName = mi.displayName;
            modeDisplayName = mi.modeDisplayName;
            blueTickets = mi.blueTickets;
            redTickets = mi.redTickets;
            blueFaction = mi.blueFaction;
            redFaction = mi.redFaction;
            online = MatchAllocator.getLobbyPlayerCount();
            switch (mi.phase) {
                case STARTING: status = "STARTING"; break;
                case PLAYING: status = "PLAYING"; canJoin = true; break;
                default: status = "NONE";
            }
        } else if (VotingManager.isActive()) {
            mapDisplayName = "Voting in progress";
            modeDisplayName = "";
            status = "VOTING";
            remainingSec = VotingManager.getRemainingSeconds();
            online = MatchAllocator.getLobbyPlayerCount();

            var maps = MapRegistry.getVotable();
            if (!maps.isEmpty()) {
                MapConfig cfg = maps.get(0);
                blueFaction = cfg.teams.BLUE.faction;
                redFaction = cfg.teams.RED.faction;
                blueTickets = cfg.teams.BLUE.tickets;
                redTickets = cfg.teams.RED.tickets;
            }
        } else {
            return null;
        }

        return new OpenMatchScreenPacket(
                mapDisplayName, modeDisplayName,
                blueFaction, redFaction,
                blueTickets, redTickets,
                remainingSec, status, online, canJoin);
    }

    private static void broadcastMatchScreenToPlayer(ServerPlayer player) {
        OpenMatchScreenPacket pkt = buildMatchScreenPacket();
        if (pkt != null) {
            PacketHandler.INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), pkt);
        }
    }

    private static void broadcastMatchScreenToAll() {
        OpenMatchScreenPacket pkt = buildMatchScreenPacket();
        if (pkt == null) return;
        var server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;
        server.getPlayerList().getPlayers().forEach(p ->
            PacketHandler.INSTANCE.send(PacketDistributor.PLAYER.with(() -> p), pkt));
    }

    private static OpenVotingScreenPacket buildVotingPacket() {
        if (!VotingManager.isActive()) return null;
        java.util.List<com.pwp.lobby.maps.MapConfig> maps = VotingManager.getVotableMaps();
        int len = maps.size();
        String[] mapNames = new String[len];
        String[] mapDisplayNames = new String[len];
        String[] mapDescriptions = new String[len];
        int[] maxPlayers = new int[len];
        int[] voteCounts = new int[len];
        String[] worldPaths = new String[len];
        String[] blueFactions = new String[len];
        String[] redFactions = new String[len];
        for (int i = 0; i < len; i++) {
            com.pwp.lobby.maps.MapConfig cfg = maps.get(i);
            mapNames[i] = cfg.name;
            mapDisplayNames[i] = cfg.displayName;
            mapDescriptions[i] = cfg.description != null ? cfg.description : "";
            maxPlayers[i] = cfg.maxPlayers;
            voteCounts[i] = VotingManager.getVoteCountForMap(cfg.name);
            worldPaths[i] = cfg.worldPath;
            blueFactions[i] = cfg.teams.BLUE.faction;
            redFactions[i] = cfg.teams.RED.faction;
        }
        return new OpenVotingScreenPacket(
                VotingManager.getRemainingSeconds(),
                MatchAllocator.getLobbyPlayerCount(),
                VotingManager.getVoteCount(),
                VotingManager.getLeadingMap(),
                mapNames, mapDisplayNames, mapDescriptions,
                maxPlayers, voteCounts, worldPaths,
                blueFactions, redFactions);
    }

    private static void broadcastVotingScreen() {
        OpenVotingScreenPacket pkt = buildVotingPacket();
        if (pkt == null) return;
        var server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;
        server.getPlayerList().getPlayers().forEach(p ->
            PacketHandler.INSTANCE.send(PacketDistributor.PLAYER.with(() -> p), pkt));
    }

    @SubscribeEvent
    public void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        PermissionHelper.autoOpIfAdmin(player);

        if (isMatchServer) return;

        String uuid = player.getStringUUID();
        String name = player.getScoreboardName();

        try {
            var playerData = CoreAPI.loadPlayer(uuid);
            if (playerData == null || !playerData.has("success") || !playerData.get("success").getAsBoolean()) {
                CoreAPI.createPlayer(uuid, name);
            }
        } catch (Exception e) {
            System.out.println("[PWP] Core API unavailable (DB down?), proceeding without registration: " + e.getMessage());
        }

        MatchAllocator.playerJoined(uuid);
        int online = MatchAllocator.getLobbyPlayerCount();

        serverBroadcast("§7[PWP] §e" + name + " §fзашёл в лобби. §7Онлайн: §e" + online);

        if (MatchAllocator.hasActiveMatch()) {
            sendMatchListToPlayer(player);
        } else {
            broadcastMatchScreenToPlayer(player);
        }

        if (VotingManager.isActive()) {
            serverBroadcast("§7[PWP] Идёт голосование за карту! §e/votemap §7<карта> — осталось §e" + VotingManager.getRemainingSeconds() + "с");
            OpenVotingScreenPacket pkt = buildVotingPacket();
            if (pkt != null) {
                PacketHandler.INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), pkt);
            }
        } else if (modeVoteActive) {
            serverBroadcast("§7[PWP] Идёт голосование за режим! §e/votemode §7<aas/invasion> — осталось §e" + getModeVoteRemainingSeconds() + "с");
            OpenModeVotePacket pkt = buildModeVotePacket();
            if (pkt != null) {
                PacketHandler.INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), pkt);
            }
        }

        if (!MatchAllocator.hasActiveMatch() && !VotingManager.isActive() && !modeVoteActive) {
            VotingManager.startVoting();
        }
    }

    @SubscribeEvent
    public void onPlayerLeave(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            MatchAllocator.playerLeft(player.getStringUUID());
        }
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        var dispatcher = event.getDispatcher();

        dispatcher.register(Commands.literal("pwp")
            .then(Commands.literal("join")
                .executes(ctx -> {
                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                    MatchAllocator.joinActiveMatch(player);
                    return Command.SINGLE_SUCCESS;
                }))
            .then(Commands.literal("start")
                .requires(s -> s.hasPermission(2))
                .executes(ctx -> {
                    if (MatchAllocator.hasActiveMatch()) {
                        ctx.getSource().sendFailure(Component.literal("A match is already running"));
                        return 0;
                    }
                    MapConfig map = MapRegistry.getBestFit(MatchAllocator.getLobbyPlayerCount());
                    if (map == null) {
                        ctx.getSource().sendFailure(Component.literal("No maps available"));
                        return 0;
                    }
                    MatchAllocator.startMatch(map);
                    ctx.getSource().sendSuccess(() -> Component.literal("Match started: " + map.displayName), true);
                    return Command.SINGLE_SUCCESS;
                })
                .then(Commands.argument("mapname", com.mojang.brigadier.arguments.StringArgumentType.word())
                    .suggests((ctx, builder) -> {
                        for (MapConfig m : MapRegistry.getAll()) {
                            builder.suggest(m.name, Component.literal(m.displayName + " (" + m.modeDisplayName + ")"));
                        }
                        return builder.buildFuture();
                    })
                    .executes(ctx -> {
                        if (MatchAllocator.hasActiveMatch()) {
                            ctx.getSource().sendFailure(Component.literal("A match is already running"));
                            return 0;
                        }
                        String mapName = com.mojang.brigadier.arguments.StringArgumentType.getString(ctx, "mapname");
                        MapConfig map = MapRegistry.get(mapName);
                        if (map == null) {
                            ctx.getSource().sendFailure(Component.literal("Map not found: " + mapName));
                            return 0;
                        }
                        MatchAllocator.startMatch(map);
                        ctx.getSource().sendSuccess(() -> Component.literal("Match started: " + map.displayName), true);
                        return Command.SINGLE_SUCCESS;
                    })))
            .then(Commands.literal("vote")
                .requires(s -> s.hasPermission(2))
                .executes(ctx -> {
                    if (VotingManager.isActive()) {
                        ctx.getSource().sendFailure(Component.literal("Voting already active"));
                        return 0;
                    }
                    List<MapConfig> votable = MapRegistry.getVotable();
                    if (votable.isEmpty()) {
                        ctx.getSource().sendFailure(Component.literal("§cNo maps available for voting! Check maps directory."));
                        return 0;
                    }
                    VotingManager.startVoting();
                    return Command.SINGLE_SUCCESS;
                }))
            .then(Commands.literal("stop")
                .requires(s -> s.hasPermission(2))
                .executes(ctx -> {
                    MatchInfo mi = MatchAllocator.getActiveMatch();
                    if (mi == null) {
                        ctx.getSource().sendFailure(Component.literal("No active match"));
                        return 0;
                    }
                    MatchAllocator.requestMatchStop(mi.serverId);
                    ctx.getSource().sendSuccess(() -> Component.literal("Match stopping gracefully..."), true);
                    return Command.SINGLE_SUCCESS;
                }))
            .then(Commands.literal("status")
                .requires(s -> s.hasPermission(2))
                .executes(ctx -> {
                    MatchInfo mi = MatchAllocator.getActiveMatch();
                    if (mi != null) {
                        ctx.getSource().sendSuccess(() -> Component.literal(
                            "§eMatch: " + mi.displayName + " | " + mi.modeDisplayName +
                            " | " + mi.blueFaction + " vs " + mi.redFaction +
                            " | Tickets: " + mi.blueTickets + "/" + mi.redTickets +
                            " | Phase: " + mi.phase +
                            " | Players: " + mi.playerCount + "/" + mi.maxPlayers), false);
                    } else if (VotingManager.isActive()) {
                        ctx.getSource().sendSuccess(() -> Component.literal(
                            "§eVoting active: " + VotingManager.getRemainingSeconds() + "s remaining"), false);
                    } else {
                        ctx.getSource().sendSuccess(() -> Component.literal("§eNo active match or voting"), false);
                    }
                    return Command.SINGLE_SUCCESS;
                }))
            .executes(ctx -> {
                ServerPlayer player = ctx.getSource().getPlayerOrException();

                if (modeVoteActive) {
                    OpenModeVotePacket mvPkt = buildModeVotePacket();
                    if (mvPkt != null) {
                        PacketHandler.INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), mvPkt);
                    }
                } else if (VotingManager.isActive()) {
                    OpenVotingScreenPacket vPkt = buildVotingPacket();
                    if (vPkt != null) {
                        PacketHandler.INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), vPkt);
                    }
                } else if (MatchAllocator.hasActiveMatch()) {
                    sendMatchListToPlayer(player);
                } else {
                    VotingManager.startVoting();
                    OpenVotingScreenPacket vPkt = buildVotingPacket();
                    if (vPkt != null) {
                        PacketHandler.INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), vPkt);
                    }
                }
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
                        ctx.getSource().sendFailure(Component.literal("Unknown map: " + mapName));
                        return 0;
                    }
                    if (!VotingManager.isActive()) {
                        ctx.getSource().sendFailure(Component.literal("No active voting"));
                        return 0;
                    }
                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                    if (!VotingManager.vote(player.getUUID(), mapName)) {
                        ctx.getSource().sendFailure(Component.literal("§cVote not accepted (already voted?)"));
                        return 0;
                    }
                    ctx.getSource().sendSuccess(() -> Component.literal("§aVoted for " + mapName), false);
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
                        ctx.getSource().sendFailure(Component.literal("No active mode voting"));
                        return 0;
                    }
                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                    voteMode(player.getUUID(), modeName);
                    ctx.getSource().sendSuccess(() -> Component.literal("§aVoted for " + modeName), false);
                    return Command.SINGLE_SUCCESS;
                })));
    }

    private static OpenMatchListScreenPacket buildMatchListPacket() {
        var matches = MatchAllocator.getActiveMatches();
        if (matches.isEmpty()) return null;
        int len = matches.size();
        String[] mapNames = new String[len];
        String[] displayNames = new String[len];
        String[] statuses = new String[len];
        int[] blueTickets = new int[len];
        int[] redTickets = new int[len];
        int[] playerCounts = new int[len];
        int[] maxPlayers = new int[len];
        int[] elapsedSeconds = new int[len];
        int[] serverIds = new int[len];
        String[] worldPaths = new String[len];
        String[] blueFactions = new String[len];
        String[] redFactions = new String[len];
        int idx = 0;
        for (MatchAllocator.MatchInfo mi : matches.values()) {
            mapNames[idx] = mi.mapName;
            displayNames[idx] = mi.displayName;
            statuses[idx] = mi.phase == MatchAllocator.MatchPhase.PLAYING ? "PLAYING" : "STARTING";
            blueTickets[idx] = mi.blueTickets;
            redTickets[idx] = mi.redTickets;
            playerCounts[idx] = mi.playerCount;
            maxPlayers[idx] = mi.maxPlayers;
            elapsedSeconds[idx] = mi.getElapsedSeconds();
            serverIds[idx] = mi.serverId;
            worldPaths[idx] = mi.worldPath != null ? mi.worldPath : "";
            blueFactions[idx] = mi.blueFaction;
            redFactions[idx] = mi.redFaction;
            idx++;
        }
        return new OpenMatchListScreenPacket(len, mapNames, displayNames, statuses,
                blueTickets, redTickets, playerCounts, maxPlayers,
                elapsedSeconds, serverIds, worldPaths, blueFactions, redFactions);
    }

    private static void sendMatchListToPlayer(ServerPlayer player) {
        OpenMatchListScreenPacket pkt = buildMatchListPacket();
        if (pkt != null) {
            PacketHandler.INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), pkt);
        }
    }

    public static void serverBroadcast(String msg) {
        var server = ServerLifecycleHooks.getCurrentServer();
        if (server != null) {
            server.getPlayerList().getPlayers().forEach(p ->
                p.sendSystemMessage(Component.literal(msg), false));
        }
    }

    public static void broadcastVotingUpdate() {
        OpenVotingScreenPacket pkt = buildVotingPacket();
        if (pkt == null) return;
        var server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;
        server.getPlayerList().getPlayers().forEach(p ->
            PacketHandler.INSTANCE.send(PacketDistributor.PLAYER.with(() -> p), pkt));
    }

    public static void broadcastMatchListUpdate() {
        OpenMatchListScreenPacket pkt = buildMatchListPacket();
        if (pkt == null) return;
        var server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;
        server.getPlayerList().getPlayers().forEach(p ->
            PacketHandler.INSTANCE.send(PacketDistributor.PLAYER.with(() -> p), pkt));
    }

    public static void onVoteFinished(String mapName) {
        MapConfig map = MapRegistry.get(mapName);
        if (map == null) return;

        var server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;

        pendingMapName = mapName;
        startModeVoting();
    }

    // ====== MODE VOTING ======

    public static void startModeVoting() {
        if (MatchAllocator.hasActiveMatch()) {
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

        String modeList = String.join("§7, §e", MODE_DISPLAY_NAMES);
        serverBroadcast("§e[PWP] §fГолосование за режим! §7Режимы: §e" + modeList);
        serverBroadcast("§7Напишите §e/votemode <название> §7чтобы проголосовать");
        broadcastModeVoteUpdate();
    }

    public static void voteMode(UUID playerUuid, String modeName) {
        if (!modeVoteActive || modeVoteFinished) return;
        boolean valid = false;
        for (String m : MODE_NAMES) {
            if (m.equals(modeName)) { valid = true; break; }
        }
        if (!valid) return;
        String current = modeVotes.get(playerUuid);
        if (modeName.equals(current)) return;
        modeVotes.put(playerUuid, modeName);
        broadcastModeVoteUpdate();
    }

    private static void tickModeVote() {
        if (!modeVoteActive || modeVoteFinished) return;
        long now = System.currentTimeMillis();
        long elapsed = now - modeVoteStartTime;
        int remaining = modeVoteDurationSec - (int)(elapsed / 1000);

        if (remaining != modeVoteLastBroadcastedRemaining && now - modeVoteLastTimerBroadcast > 1000) {
            if (remaining <= 5 || remaining == 10 || remaining == 15 || remaining == 30 || remaining == 60 || (remaining <= 120 && remaining % 60 == 0)) {
                if (remaining > 0) {
                    serverBroadcast("§e[PWP] §fГолосование за режим закончится через §e" + remaining + "с");
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
            String results = counts.entrySet().stream()
                    .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                    .map(e -> "§e" + e.getKey() + " §7(" + e.getValue() + "гол.)")
                    .collect(Collectors.joining(" §8| "));
            serverBroadcast("§e[PWP] §fРезультаты голосования за режим: " + results);
        }

        modeVoteWinner = counts.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("aas");

        serverBroadcast("§e[PWP] §aРежим: §e" + modeVoteWinner.toUpperCase() + " §a— запуск матча!");

        if (pendingMapName != null) {
            MapConfig map = MapRegistry.get(pendingMapName);
            if (map != null) {
                // Randomize factions
                String blueFaction, redFaction;
                List<String> available = (map.availableFactions != null && !map.availableFactions.isEmpty())
                        ? new ArrayList<>(map.availableFactions)
                        : new ArrayList<>(Arrays.asList("ukraine", "russia", "usa", "nato", "insurgency", "pmc"));
                if (available.size() < 2) {
                    blueFaction = "usa";
                    redFaction = "russia";
                } else {
                    Collections.shuffle(available);
                    blueFaction = available.get(0);
                    redFaction = available.get(1);
                }

                boolean isInvasion = modeVoteWinner.equals("invasion");
                boolean invasionDefenderIsRed = true;
                if (isInvasion) {
                    invasionDefenderIsRed = new Random().nextBoolean();
                }

                // Resolve tickets per mode
                int blueTickets = map.teams.BLUE.tickets;
                int redTickets = map.teams.RED.tickets;
                if (isInvasion && map.modes != null && map.modes.containsKey("invasion")) {
                    MapConfig.ModeConfig invConfig = map.modes.get("invasion");
                    if (invasionDefenderIsRed) {
                        blueTickets = invConfig.attackerTickets;
                        redTickets = invConfig.defenderTickets;
                    } else {
                        blueTickets = invConfig.defenderTickets;
                        redTickets = invConfig.attackerTickets;
                    }
                }

                // Randomize capture point pattern
                int patternIndex = -1;
                if (map.capturePointPatterns != null && !map.capturePointPatterns.isEmpty()) {
                    patternIndex = new Random().nextInt(map.capturePointPatterns.size());
                    serverBroadcast("§7[PWP] Выбран паттерн точек: §e" + map.capturePointPatterns.get(patternIndex).name);
                }

                MatchAllocator.startMatch(map, blueFaction, redFaction, modeVoteWinner, blueTickets, redTickets, invasionDefenderIsRed, patternIndex);
            }
        }

        pendingMapName = null;
        modeVotes.clear();
    }

    private static OpenModeVotePacket buildModeVotePacket() {
        if (!modeVoteActive) return null;
        int len = MODE_NAMES.length;
        int[] voteCounts = new int[len];
        for (int i = 0; i < len; i++) {
            String mn = MODE_NAMES[i];
            voteCounts[i] = (int) modeVotes.values().stream().filter(v -> v.equals(mn)).count();
        }
        return new OpenModeVotePacket(
                getModeVoteRemainingSeconds(),
                MatchAllocator.getLobbyPlayerCount(),
                modeVotes.size(),
                MODE_NAMES, MODE_DISPLAY_NAMES, MODE_DESCRIPTIONS,
                voteCounts);
    }

    private static int getModeVoteRemainingSeconds() {
        if (!modeVoteActive) return 0;
        long elapsed = System.currentTimeMillis() - modeVoteStartTime;
        int remaining = modeVoteDurationSec - (int)(elapsed / 1000);
        return Math.max(0, remaining);
    }

    public static void broadcastModeVoteUpdate() {
        OpenModeVotePacket pkt = buildModeVotePacket();
        if (pkt == null) return;
        var server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;
        server.getPlayerList().getPlayers().forEach(p ->
            PacketHandler.INSTANCE.send(PacketDistributor.PLAYER.with(() -> p), pkt));
    }
}
