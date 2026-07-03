package com.pwp.lobby;

import com.pwp.coreclient.CoreAPI;
import com.pwp.coreclient.PermissionHelper;
import com.pwp.coreclient.network.OpenMatchScreenPacket;
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

@Mod("pwp_lobby")
public class LobbyMod {

    private static boolean isMatchServer = false;

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

            MapRegistry.configure("../PWP-Server/maps");
            MapRegistry.loadAll();
            MatchAllocator.configure(1);
        });
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) return;
        VotingManager.tick();
        MatchAllocator.tick();
    }

    @SubscribeEvent
    public void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        PermissionHelper.autoOpIfAdmin(player);

        if (isMatchServer) return;

        String uuid = player.getStringUUID();

        try {
            var playerData = CoreAPI.loadPlayer(uuid);
            if (playerData == null || !playerData.has("success") || !playerData.get("success").getAsBoolean()) {
                CoreAPI.createPlayer(uuid, player.getScoreboardName());
            }
        } catch (Exception e) {
            System.out.println("[PWP] Core API unavailable (DB down?), proceeding without registration: " + e.getMessage());
        }

        MatchAllocator.playerJoined(uuid);

        if (!MatchAllocator.hasActiveMatch() && !VotingManager.isActive()) {
            VotingManager.startVoting();
            serverBroadcast("§e[PWP] Voting started! Type /votemap <map> to vote");
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
                    String mapName = "grozny";
                    MapConfig map = MapRegistry.get(mapName);
                    if (map == null) {
                        ctx.getSource().sendFailure(Component.literal("Map config not found"));
                        return 0;
                    }
                    MatchAllocator.startMatch(map);
                    ctx.getSource().sendSuccess(() -> Component.literal("Match started"), true);
                    return Command.SINGLE_SUCCESS;
                }))
            .then(Commands.literal("vote")
                .requires(s -> s.hasPermission(2))
                .executes(ctx -> {
                    if (VotingManager.isActive()) {
                        ctx.getSource().sendFailure(Component.literal("Voting already active"));
                        return 0;
                    }
                    VotingManager.startVoting();
                    serverBroadcast("§e[PWP] Voting started! Type /votemap <map> to vote");
                    ctx.getSource().sendSuccess(() -> Component.literal("Voting started"), true);
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
                    MatchAllocator.matchEnded(mi.serverId);
                    ctx.getSource().sendSuccess(() -> Component.literal("Match stopped"), true);
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
                MatchInfo mi = MatchAllocator.getActiveMatch();
                int online = MatchAllocator.getLobbyPlayerCount();

                String status;
                boolean canJoin = false;
                int remainingSec = 0;
                if (mi != null) {
                    switch (mi.phase) {
                        case STARTING: status = "STARTING"; break;
                        case PLAYING: status = "PLAYING"; canJoin = true; break;
                        default: status = "NONE";
                    }
                } else if (VotingManager.isActive()) {
                    status = "VOTING";
                    remainingSec = VotingManager.getRemainingSeconds();
                } else {
                    status = "NONE";
                }

                String firstMap = "grozny";
                MapConfig cfg = MapRegistry.get(firstMap);
                if (cfg == null) cfg = MapRegistry.getVotable().isEmpty() ? null : MapRegistry.getVotable().get(0);

                String mapDisplayName = mi != null ? mi.displayName : (cfg != null ? cfg.displayName : "Grozny");
                String modeDisplayName = mi != null ? mi.modeDisplayName : (cfg != null ? cfg.modeDisplayName : "AAS");
                String blueFaction = mi != null ? mi.blueFaction : (cfg != null ? cfg.BLUE.faction : "russia");
                String redFaction = mi != null ? mi.redFaction : (cfg != null ? cfg.RED.faction : "insurgency");
                int blueTickets = mi != null ? mi.blueTickets : (cfg != null ? cfg.BLUE.tickets : 600);
                int redTickets = mi != null ? mi.redTickets : (cfg != null ? cfg.RED.tickets : 600);

                OpenMatchScreenPacket pkt = new OpenMatchScreenPacket(
                        mapDisplayName, modeDisplayName,
                        blueFaction, redFaction,
                        blueTickets, redTickets,
                        remainingSec, status, online, canJoin);

                PacketHandler.INSTANCE.send(
                        PacketDistributor.PLAYER.with(() -> player),
                        pkt);

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
    }

    private static void serverBroadcast(String msg) {
        var server = ServerLifecycleHooks.getCurrentServer();
        if (server != null) {
            server.getPlayerList().getPlayers().forEach(p ->
                p.sendSystemMessage(Component.literal(msg), false));
        }
    }

    public static void onVoteFinished(String mapName) {
        MapConfig map = MapRegistry.get(mapName);
        if (map == null) return;

        var server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;

        server.getPlayerList().getPlayers().forEach(p ->
            p.sendSystemMessage(Component.literal("§e[PWP] Starting match on " + mapName + "..."), false));

        MatchAllocator.startMatch(map);
    }
}
