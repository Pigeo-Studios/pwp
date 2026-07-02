package com.pwp.lobby;

import com.pwp.coreclient.network.ConnectToServerPacket;
import com.pwp.coreclient.network.OpenVotingScreenPacket;
import com.pwp.coreclient.network.PacketHandler;
import com.pwp.lobby.maps.MapConfig;
import com.pwp.lobby.maps.MapRegistry;
import com.pwp.lobby.match.MatchAllocator;
import com.pwp.lobby.match.MatchAllocator.MatchInfo;
import net.minecraft.commands.Commands;
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
            MatchAllocator.configure(2, 80);
        });
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) return;
        VotingManager.tick();
    }

    @SubscribeEvent
    public void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            MatchAllocator.playerJoined(player.getStringUUID());
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

        // /vote - start voting
        dispatcher.register(Commands.literal("vote")
                .executes(ctx -> {
                    if (!VotingManager.isActive()) {
                        VotingManager.startVoting();
                        var server = ServerLifecycleHooks.getCurrentServer();
                        if (server != null) {
                            for (var p : server.getPlayerList().getPlayers()) {
                                p.sendSystemMessage(
                                    net.minecraft.network.chat.Component.literal(
                                        "§e[PWP] Voting started!"), false);
                                PacketHandler.INSTANCE.send(
                                    PacketDistributor.PLAYER.with(() -> p),
                                    new OpenVotingScreenPacket());
                            }
                        }
                    }
                    return 1;
                }));

        // /votemap <map> - vote for a map (accepts any string)
        dispatcher.register(Commands.literal("votemap")
                .then(Commands.argument("name", com.mojang.brigadier.arguments.StringArgumentType.greedyString())
                        .executes(ctx -> {
                            String mapName = com.mojang.brigadier.arguments.StringArgumentType
                                    .getString(ctx, "name");
                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                            VotingManager.vote(player.getUUID(), mapName);
                            player.sendSystemMessage(
                                net.minecraft.network.chat.Component.literal(
                                    "§aVoted for: " + mapName), false);
                            return 1;
                        })));

        // /lobby - show status
        dispatcher.register(Commands.literal("lobby")
                .executes(ctx -> {
                    var source = ctx.getSource();
                    source.sendSuccess(() -> net.minecraft.network.chat.Component.literal(
                            "§e===== PWP Lobby ====="), false);
                    source.sendSuccess(() -> net.minecraft.network.chat.Component.literal(
                            "§eActive matches: " + MatchAllocator.getActiveMatches().size()), false);
                    source.sendSuccess(() -> net.minecraft.network.chat.Component.literal(
                            "§ePlayers in lobby: " + MatchAllocator.getLobbyPlayerCount()), false);
                    for (MatchInfo mi : MatchAllocator.getActiveMatches().values()) {
                        source.sendSuccess(() -> net.minecraft.network.chat.Component.literal(
                                " §7- §f" + mi.mapName + " §7port=" + mi.port +
                                " §7" + mi.playerCount + "/" + mi.maxPlayers), false);
                    }
                    return 1;
                }));
    }

    // Called by VotingManager when vote finishes
    public static void onVoteFinished(String mapName) {
        MapConfig map = MapRegistry.get(mapName);
        if (map == null) return;

        MatchAllocator.startMatch(map);

        var server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;

        for (MatchInfo mi : MatchAllocator.getActiveMatches().values()) {
            if (mi.mapName.equals(mapName)) {
                for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                    PacketHandler.INSTANCE.send(
                            PacketDistributor.PLAYER.with(() -> player),
                            new ConnectToServerPacket("127.0.0.1", mi.port));
                }
                break;
            }
        }
    }
}
