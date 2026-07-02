package com.pwp.lobby;

import com.pwp.coreclient.CoreAPI;
import com.pwp.lobby.maps.MapConfig;
import com.pwp.lobby.maps.MapRegistry;
import com.pwp.lobby.match.MatchAllocator;
import com.pwp.lobby.match.MatchAllocator.MatchInfo;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
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
import net.minecraftforge.server.ServerLifecycleHooks;

@Mod("pwp_lobby")
public class LobbyMod {

    public LobbyMod() {
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::commonSetup);
        MinecraftForge.EVENT_BUS.register(this);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
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

        String uuid = player.getStringUUID();

        var playerData = CoreAPI.loadPlayer(uuid);
        if (playerData == null || !playerData.has("success") || !playerData.get("success").getAsBoolean()) {
            player.connection.disconnect(
                    Component.literal("§cYou are not registered on this server.\n§7Register at pwp.example.com"));
            return;
        }

        MatchAllocator.playerJoined(uuid);
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

        // /pwp - main admin command
        dispatcher.register(Commands.literal("pwp")
            .requires(s -> s.hasPermission(2))

            // /pwp vote
            .then(Commands.literal("vote")
                .then(Commands.literal("start")
                    .executes(ctx -> {
                        if (!VotingManager.isActive()) {
                            VotingManager.startVoting();
                            var server = ServerLifecycleHooks.getCurrentServer();
                            if (server != null) {
                                for (var p : server.getPlayerList().getPlayers()) {
                                    p.sendSystemMessage(Component.literal("§e[PWP] Voting started! Type /votemap <map> to vote"), false);
                                }
                            }
                            ctx.getSource().sendSuccess(() -> Component.literal("Voting started"), true);
                        } else {
                            ctx.getSource().sendFailure(Component.literal("Voting already active"));
                        }
                        return Command.SINGLE_SUCCESS;
                    }))
                .then(Commands.literal("set")
                    .then(Commands.argument("map", StringArgumentType.greedyString())
                        .suggests((ctx, builder) -> {
                            for (MapConfig m : MapRegistry.getVotable()) {
                                builder.suggest(m.name);
                            }
                            return builder.buildFuture();
                        })
                        .executes(ctx -> {
                            String mapName = StringArgumentType.getString(ctx, "map");
                            if (MapRegistry.get(mapName) != null) {
                                VotingManager.stopVoting();
                                LobbyMod.onVoteFinished(mapName);
                                ctx.getSource().sendSuccess(() -> Component.literal("Match started: " + mapName), true);
                            } else {
                                ctx.getSource().sendFailure(Component.literal("Unknown map: " + mapName));
                            }
                            return Command.SINGLE_SUCCESS;
                        })))
                .then(Commands.literal("list")
                    .executes(ctx -> {
                        var maps = MapRegistry.getVotable();
                        ctx.getSource().sendSuccess(() -> Component.literal("§eAvailable maps:"), false);
                        for (MapConfig m : maps) {
                            String running = MatchAllocator.getActiveMatches().values().stream()
                                .anyMatch(mi -> mi.mapName.equals(m.name)) ? " §c[RUNNING]" : "";
                            ctx.getSource().sendSuccess(() -> Component.literal(
                                " §7- §f" + m.name + " §7(" + m.displayName + ")" + running), false);
                        }
                        return Command.SINGLE_SUCCESS;
                    }))
                .then(Commands.literal("status")
                    .executes(ctx -> {
                        ctx.getSource().sendSuccess(() -> Component.literal(
                            "§eActive: " + VotingManager.isActive() + " Timer: " + VotingManager.getRemainingSeconds() +
                            "s Winner: " + (VotingManager.getWinner() != null ? VotingManager.getWinner() : "none")), false);
                        return Command.SINGLE_SUCCESS;
                    })))

            // /pwp match
            .then(Commands.literal("match")
                .then(Commands.literal("list")
                    .executes(ctx -> {
                        var matches = MatchAllocator.getActiveMatches();
                        if (matches.isEmpty()) {
                            ctx.getSource().sendSuccess(() -> Component.literal("§eNo active matches"), false);
                        } else {
                            for (MatchInfo mi : matches.values()) {
                                ctx.getSource().sendSuccess(() -> Component.literal(
                                    " §7- §f" + mi.mapName + " §7port=" + mi.port +
                                    " §7" + mi.playerCount + "/" + mi.maxPlayers), false);
                            }
                        }
                        return Command.SINGLE_SUCCESS;
                    }))
                .then(Commands.literal("stop")
                    .then(Commands.argument("port", com.mojang.brigadier.arguments.IntegerArgumentType.integer(25565, 65535))
                        .executes(ctx -> {
                            int port = ctx.getArgument("port", Integer.class);
                            var match = MatchAllocator.getActiveMatches().values().stream()
                                .filter(m -> m.port == port).findFirst();
                            if (match.isPresent()) {
                                MatchAllocator.matchEnded(match.get().serverId);
                                ctx.getSource().sendSuccess(() -> Component.literal("Match on port " + port + " stopped"), true);
                            } else {
                                ctx.getSource().sendFailure(Component.literal("No match on port " + port));
                            }
                            return Command.SINGLE_SUCCESS;
                        })))));

        // /votemap <map> (for all players)
        dispatcher.register(Commands.literal("votemap")
            .then(Commands.argument("name", StringArgumentType.greedyString())
                .suggests((ctx, builder) -> {
                    for (MapConfig m : MapRegistry.getVotable()) {
                        if (!MatchAllocator.getActiveMatches().values().stream()
                                .anyMatch(mi -> mi.mapName.equals(m.name))) {
                            builder.suggest(m.name);
                        }
                    }
                    return builder.buildFuture();
                })
                .executes(ctx -> {
                    String mapName = StringArgumentType.getString(ctx, "name");
                    if (MapRegistry.get(mapName) == null) {
                        ctx.getSource().sendFailure(Component.literal("Unknown map: " + mapName));
                        return 0;
                    }
                    if (!VotingManager.isActive()) {
                        ctx.getSource().sendFailure(Component.literal("No active voting. Use /pwp vote start"));
                        return 0;
                    }
                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                    boolean accepted = VotingManager.vote(player.getUUID(), mapName);
                    if (!accepted) {
                        ctx.getSource().sendFailure(Component.literal("§cYou already voted for " + mapName));
                        return 0;
                    }
                    var server = ServerLifecycleHooks.getCurrentServer();
                    if (server != null) {
                        for (var p : server.getPlayerList().getPlayers()) {
                            p.sendSystemMessage(Component.literal(
                                "§7" + player.getScoreboardName() + " voted for §f" + mapName), false);
                        }
                    }
                    ctx.getSource().sendSuccess(() -> Component.literal("§aVoted for " + mapName), false);
                    return Command.SINGLE_SUCCESS;
                })));

        // /votes - show current votes
        dispatcher.register(Commands.literal("votes")
            .executes(ctx -> {
                if (!VotingManager.isActive()) {
                    ctx.getSource().sendSuccess(() -> Component.literal("§eNo active voting"), false);
                    return Command.SINGLE_SUCCESS;
                }
                var counts = VotingManager.getVoteCounts();
                ctx.getSource().sendSuccess(() -> Component.literal(
                    "§eVoting: " + VotingManager.getRemainingSeconds() + "s remaining"), false);
                for (var entry : counts.entrySet()) {
                    ctx.getSource().sendSuccess(() -> Component.literal(
                        " §f" + entry.getKey() + " §7- " + entry.getValue() + " votes"), false);
                }
                return Command.SINGLE_SUCCESS;
            }));
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
