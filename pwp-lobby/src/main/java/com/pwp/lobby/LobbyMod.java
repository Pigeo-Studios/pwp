package com.pwp.lobby;

import com.google.gson.JsonObject;
import com.pwp.coreserver.CoreServerApi;

import com.pwp.coreclient.network.OpenMatchListScreenPacket;
import com.pwp.coreclient.network.OpenMatchScreenPacket;
import com.pwp.coreclient.network.OpenModeVotePacket;
import com.pwp.coreclient.network.OpenVotingScreenPacket;
import com.pwp.coreclient.network.OpenFactionVotePacket;
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

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(LobbyMod.class);
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
        FactionVotingManager.tick();
        tickModeVote();
        MatchAllocator.tick();

        if (++heartbeatTicks >= 600) {
            heartbeatTicks = 0;
            int online = MatchAllocator.getLobbyPlayerCount();
            try {
                MatchInfo mi = MatchAllocator.getActiveMatch();
                if (mi != null) {
                    CoreServerApi.sendHeartbeat("lobby", online,
                        mi.mapName, mi.mode,
                        mi.blueFaction, mi.redFaction,
                        mi.blueTickets, mi.redTickets,
                        mi.phase.name(), mi.maxPlayers,
                        mi.startedAt, mi.playerCount);
                } else {
                    CoreServerApi.sendHeartbeat("lobby", online);
                }
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

        if (isMatchServer) return;

        String uuid = player.getStringUUID();
        String name = player.getScoreboardName();

        // Игрок уже создан в БД через Telegram бота — ничего дополнительно не делаем

        MatchAllocator.playerJoined(uuid);
        int online = MatchAllocator.getLobbyPlayerCount();

        serverBroadcast("§7[PWP] §e" + name + " §fзашёл в лобби. §7Онлайн: §e" + online);

        if (MatchAllocator.hasActiveMatch()) {
            broadcastMatchScreenToPlayer(player);
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
        } else if (FactionVotingManager.isActive()) {
            serverBroadcast("§7[PWP] Идёт голосование за фракции! §e/votefaction §7<синие> <красные>");
            OpenFactionVotePacket pkt = FactionVotingManager.buildPacket();
            if (pkt != null) {
                PacketHandler.INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), pkt);
            }
        }

        if (!MatchAllocator.hasActiveMatch() && !VotingManager.isActive() && !modeVoteActive && !FactionVotingManager.isActive()) {
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
            .then(Commands.literal("server")
                .requires(s -> s.hasPermission(2))
                // /pwp server start — full voting cycle
                .then(Commands.literal("start")
                    .executes(ctx -> {
                        if (MatchAllocator.hasActiveMatch()) {
                            ctx.getSource().sendFailure(Component.literal("Матч уже запущен"));
                            return 0;
                        }
                        if (VotingManager.isActive()) {
                            ctx.getSource().sendFailure(Component.literal("Голосование уже активно"));
                            return 0;
                        }
                        List<MapConfig> votable = MapRegistry.getVotable();
                        if (votable.isEmpty()) {
                            ctx.getSource().sendFailure(Component.literal("Нет карт для голосования"));
                            return 0;
                        }
                        VotingManager.startVoting();
                        ctx.getSource().sendSuccess(() -> Component.literal("§aГолосование начато"), false);
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
                            String mn = com.mojang.brigadier.arguments.StringArgumentType.getString(ctx, "map");
                            MapConfig map = MapRegistry.get(mn);
                            if (map == null) { ctx.getSource().sendFailure(Component.literal("Карта не найдена")); return 0; }
                            VotingManager.startVoting();
                            // Immediately finish map vote with forced map
                            onVoteFinished(mn);
                            ctx.getSource().sendSuccess(() -> Component.literal("§aКарта форсирована: " + map.displayName), false);
                            return Command.SINGLE_SUCCESS;
                        })
                        // /pwp server start <map> <mode> — force map+mode
                        .then(Commands.argument("mode", com.mojang.brigadier.arguments.StringArgumentType.word())
                            .suggests((ctx, builder) -> { builder.suggest("aas"); builder.suggest("invasion"); return builder.buildFuture(); })
                            .executes(ctx -> {
                                String mn = com.mojang.brigadier.arguments.StringArgumentType.getString(ctx, "map");
                                String md = com.mojang.brigadier.arguments.StringArgumentType.getString(ctx, "mode");
                                if (!md.equals("aas") && !md.equals("invasion")) {
                                    ctx.getSource().sendFailure(Component.literal("Режим должен быть aas или invasion")); return 0;
                                }
                                MapConfig map = MapRegistry.get(mn);
                                if (map == null) { ctx.getSource().sendFailure(Component.literal("Карта не найдена")); return 0; }
                                // Force map + mode: simulate vote results
                                pendingMapName = mn;
                                modeVoteWinner = md;
                                LobbyMod.serverBroadcast("§e[PWP] §aКарта: §e" + map.displayName + " §aРежим: §e" + md.toUpperCase());
                                List<String> available = getAvailableFactions(map);
                                if (available.size() >= 2) {
                                    FactionVotingManager.startFactionVoting(mn, available);
                                } else {
                                    startMatchAfterFactionVote(map, "usa", "russia");
                                }
                                ctx.getSource().sendSuccess(() -> Component.literal("§aКарта+режим форсированы, голосование за фракции"), false);
                                return Command.SINGLE_SUCCESS;
                            })
                            // /pwp server start <map> <mode> <blue> <red> — instant match
                            .then(Commands.argument("blue", com.mojang.brigadier.arguments.StringArgumentType.word())
                            .then(Commands.argument("red", com.mojang.brigadier.arguments.StringArgumentType.word())
                                .executes(ctx -> {
                                    String mn = com.mojang.brigadier.arguments.StringArgumentType.getString(ctx, "map");
                                    String md = com.mojang.brigadier.arguments.StringArgumentType.getString(ctx, "mode");
                                    String bl = com.mojang.brigadier.arguments.StringArgumentType.getString(ctx, "blue");
                                    String re = com.mojang.brigadier.arguments.StringArgumentType.getString(ctx, "red");
                                    if (!md.equals("aas") && !md.equals("invasion")) {
                                        ctx.getSource().sendFailure(Component.literal("Режим: aas или invasion")); return 0;
                                    }
                                    MapConfig map = MapRegistry.get(mn);
                                    if (map == null) { ctx.getSource().sendFailure(Component.literal("Карта не найдена")); return 0; }
                                    pendingMapName = mn;
                                    modeVoteWinner = md;
                                    startMatchAfterFactionVote(map, bl, re);
                                    ctx.getSource().sendSuccess(() -> Component.literal("§aМатч запущен: " + map.displayName + " (" + bl + " vs " + re + ")"), false);
                                    return Command.SINGLE_SUCCESS;
                                }))))))
                // /pwp server stop
                .then(Commands.literal("stop")
                    .executes(ctx -> {
                        MatchInfo mi = MatchAllocator.getActiveMatch();
                        if (mi == null) { ctx.getSource().sendFailure(Component.literal("Нет активного матча")); return 0; }
                        MatchAllocator.requestMatchStop(mi.serverId);
                        ctx.getSource().sendSuccess(() -> Component.literal("Остановка матча..."), true);
                        return Command.SINGLE_SUCCESS;
                    }))
                // /pwp server stopvote
                .then(Commands.literal("stopvote")
                    .executes(ctx -> {
                        boolean any = false;
                        if (VotingManager.isActive()) { VotingManager.stopVoting(); any = true; }
                        if (modeVoteActive) { modeVoteActive = false; modeVotes.clear(); any = true; }
                        if (FactionVotingManager.isActive()) { FactionVotingManager.stop(); any = true; }
                        if (any) {
                            serverBroadcast("§e[PWP] §fГолосование остановлено администратором");
                            ctx.getSource().sendSuccess(() -> Component.literal("Голосование остановлено"), true);
                        } else {
                            ctx.getSource().sendFailure(Component.literal("Нет активного голосования"));
                        }
                        return Command.SINGLE_SUCCESS;
                    }))
                // /pwp server autostart on|off
                .then(Commands.literal("autostart")
                    .then(Commands.literal("on").executes(ctx -> {
                        ServerConfig.setAutoStartEnabled(true);
                        ctx.getSource().sendSuccess(() -> Component.literal("Авто-старт включён"), true);
                        return Command.SINGLE_SUCCESS;
                    }))
                    .then(Commands.literal("off").executes(ctx -> {
                        ServerConfig.setAutoStartEnabled(false);
                        ctx.getSource().sendSuccess(() -> Component.literal("Авто-старт отключён"), true);
                        return Command.SINGLE_SUCCESS;
                    })))
                // /pwp server maxmatches <n>
                .then(Commands.literal("maxmatches")
                    .then(Commands.argument("count", com.mojang.brigadier.arguments.IntegerArgumentType.integer(1))
                        .executes(ctx -> {
                            int n = com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(ctx, "count");
                            ServerConfig.setMaxMatches(n);
                            ctx.getSource().sendSuccess(() -> Component.literal("Макс. матчей: " + n), true);
                            return Command.SINGLE_SUCCESS;
                        })))
                // /pwp server status
                .then(Commands.literal("status")
                    .executes(ctx -> {
                        MatchInfo mi = MatchAllocator.getActiveMatch();
                        StringBuffer sb = new StringBuffer();
                        sb.append("§e[PWP] §fСтатус сервера:\n");
                        sb.append("§fМатчей: §e" + MatchAllocator.getActiveMatches().size() + "§7/" + ServerConfig.getMaxMatches() + "\n");
                        sb.append("§fАвто-старт: §e" + (ServerConfig.isAutoStartEnabled() ? "ВКЛ" : "ВЫКЛ") + "\n");
                        if (mi != null) {
                            sb.append("§fАктивный матч: §e" + mi.displayName + "§7 (" + mi.blueFaction + " vs " + mi.redFaction + ")\n");
                            sb.append("§fФаза: §e" + mi.phase + " §fИгроки: §e" + mi.playerCount + "§7/" + mi.maxPlayers);
                        } else if (VotingManager.isActive()) {
                            sb.append("§fГолосование за карту: §e" + VotingManager.getRemainingSeconds() + "с");
                        } else if (modeVoteActive) {
                            sb.append("§fГолосование за режим: §e" + getModeVoteRemainingSeconds() + "с");
                        } else if (FactionVotingManager.isActive()) {
                            sb.append("§fГолосование за фракции: §e" + FactionVotingManager.getRemainingSeconds() + "с");
                        } else {
                            sb.append("§fНет активного матча или голосования");
                        }
                        ctx.getSource().sendSuccess(() -> Component.literal(sb.toString()), false);
                        return Command.SINGLE_SUCCESS;
                    })))
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
                    broadcastMatchScreenToPlayer(player);
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
                        ctx.getSource().sendFailure(Component.literal("Неизвестная карта: " + mapName));
                        return 0;
                    }
                    if (!VotingManager.isActive()) {
                        ctx.getSource().sendFailure(Component.literal("Голосование не активно"));
                        return 0;
                    }
                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                    if (!VotingManager.vote(player.getUUID(), mapName)) {
                        ctx.getSource().sendFailure(Component.literal("§cГолос не принят (уже голосовали?)"));
                        return 0;
                    }
                    ctx.getSource().sendSuccess(() -> Component.literal("§aГолос отдан за " + mapName), false);
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
                        ctx.getSource().sendFailure(Component.literal("Голосование за режим не активно"));
                        return 0;
                    }
                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                    voteMode(player.getUUID(), modeName);
                    ctx.getSource().sendSuccess(() -> Component.literal("§aГолос отдан за " + modeName), false);
                    return Command.SINGLE_SUCCESS;
                })));

        dispatcher.register(Commands.literal("votefaction")
            .then(Commands.argument("blue", com.mojang.brigadier.arguments.StringArgumentType.word())
            .then(Commands.argument("red", com.mojang.brigadier.arguments.StringArgumentType.word())
                .executes(ctx -> {
                    String blue = ctx.getArgument("blue", String.class);
                    String red = ctx.getArgument("red", String.class);
                    if (!FactionVotingManager.isActive()) {
                        ctx.getSource().sendFailure(Component.literal("Голосование за фракции не активно"));
                        return 0;
                    }
                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                    FactionVotingManager.vote(player.getUUID(), blue, red);
                    ctx.getSource().sendSuccess(() -> Component.literal("§aГолос отдан за " + blue + " / " + red), false);
                    return Command.SINGLE_SUCCESS;
                }))));
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

    public static void startMatchAfterFactionVote(MapConfig map, String blueFaction, String redFaction) {
        boolean isInvasion = modeVoteWinner.equals("invasion");
        boolean invasionDefenderIsRed = true;
        if (isInvasion) invasionDefenderIsRed = new Random().nextBoolean();

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
            serverBroadcast("§7[PWP] Выбран паттерн точек: §e" + map.capturePointPatterns.get(patternIndex).name);
        }

        MatchAllocator.startMatch(map, blueFaction, redFaction, modeVoteWinner, blueTickets, redTickets, invasionDefenderIsRed, patternIndex);

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
