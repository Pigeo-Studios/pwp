/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.brigadier.CommandDispatcher
 *  com.mojang.brigadier.arguments.ArgumentType
 *  com.mojang.brigadier.arguments.BoolArgumentType
 *  com.mojang.brigadier.arguments.IntegerArgumentType
 *  com.mojang.brigadier.arguments.StringArgumentType
 *  com.mojang.brigadier.builder.LiteralArgumentBuilder
 *  com.mojang.brigadier.context.CommandContext
 *  com.mojang.brigadier.suggestion.Suggestions
 *  com.mojang.brigadier.suggestion.SuggestionsBuilder
 *  net.minecraft.ChatFormatting
 *  net.minecraft.commands.CommandSourceStack
 *  net.minecraft.commands.Commands
 *  net.minecraft.commands.SharedSuggestionProvider
 *  net.minecraft.commands.arguments.EntityArgument
 *  net.minecraft.commands.arguments.coordinates.BlockPosArgument
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.Vec3i
 *  net.minecraft.network.chat.Component
 *  net.minecraft.network.protocol.Packet
 *  net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket
 *  net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket
 *  net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket
 *  net.minecraft.server.ServerScoreboard
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.server.level.ServerPlayer
 *  net.minecraft.sounds.SoundEvents
 *  net.minecraft.sounds.SoundSource
 *  net.minecraft.world.phys.AABB
 *  net.minecraft.world.scores.PlayerTeam
 *  net.minecraftforge.network.PacketDistributor
 */
package com.example.aas.command;

import com.example.aas.config.AASConfig;
import com.example.aas.events.GameLogicEvents;
import com.example.aas.network.PacketHandler;
import com.example.aas.network.PacketSquadAction;
import com.example.aas.network.PacketSyncSquads;
import com.example.aas.world.AASWorldData;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.ServerScoreboard;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraftforge.network.PacketDistributor;

public class ModCommands {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal((String)"aas").requires(s -> s.hasPermission(2))).then(Commands.literal((String)"gamestart").then(Commands.argument((String)"active", (ArgumentType)BoolArgumentType.bool()).executes(ctx -> ModCommands.setGameStart((CommandSourceStack)ctx.getSource(), BoolArgumentType.getBool((CommandContext)ctx, (String)"active")))))).then(Commands.literal((String)"deathtimer").then(Commands.argument((String)"seconds", (ArgumentType)IntegerArgumentType.integer(0)).executes(ctx -> ModCommands.setRespawnTime((CommandSourceStack)ctx.getSource(), IntegerArgumentType.getInteger((CommandContext)ctx, (String)"seconds")))))).then(Commands.literal((String)"deathtickets").then(Commands.argument((String)"amount", (ArgumentType)IntegerArgumentType.integer(0)).executes(ctx -> ModCommands.setDeathTickets((CommandSourceStack)ctx.getSource(), IntegerArgumentType.getInteger((CommandContext)ctx, (String)"amount")))))).then(Commands.literal((String)"clearsquad").then(Commands.argument((String)"team", (ArgumentType)StringArgumentType.word()).suggests((ctx, builder) -> SharedSuggestionProvider.suggest(List.of("blue", "red"), (SuggestionsBuilder)builder)).executes(ctx -> ModCommands.clearSquads((CommandSourceStack)ctx.getSource(), StringArgumentType.getString((CommandContext)ctx, (String)"team")))))).then(Commands.literal((String)"teamtickets").then(Commands.argument((String)"team", (ArgumentType)StringArgumentType.word()).suggests((ctx, builder) -> SharedSuggestionProvider.suggest(List.of("blue", "red"), (SuggestionsBuilder)builder)).then(Commands.argument((String)"amount", (ArgumentType)IntegerArgumentType.integer()).executes(ctx -> ModCommands.setTickets((CommandSourceStack)ctx.getSource(), StringArgumentType.getString((CommandContext)ctx, (String)"team"), IntegerArgumentType.getInteger((CommandContext)ctx, (String)"amount"))))))).then(Commands.literal((String)"mainzone").then(Commands.argument((String)"team", (ArgumentType)StringArgumentType.word()).suggests((ctx, builder) -> SharedSuggestionProvider.suggest(List.of("blue", "red"), (SuggestionsBuilder)builder)).then(Commands.argument((String)"shape", (ArgumentType)StringArgumentType.word()).suggests((ctx, builder) -> SharedSuggestionProvider.suggest(List.of("cube", "cylinder"), (SuggestionsBuilder)builder)).then(Commands.argument((String)"pos1", (ArgumentType)BlockPosArgument.blockPos()).then(Commands.argument((String)"pos2", (ArgumentType)BlockPosArgument.blockPos()).executes(ctx -> ModCommands.addMainZone((CommandSourceStack)ctx.getSource(), StringArgumentType.getString((CommandContext)ctx, (String)"team"), StringArgumentType.getString((CommandContext)ctx, (String)"shape"), BlockPosArgument.getSpawnablePos((CommandContext)ctx, (String)"pos1"), BlockPosArgument.getSpawnablePos((CommandContext)ctx, (String)"pos2"))))))))).then(Commands.literal((String)"removemainzone").then(Commands.argument((String)"team", (ArgumentType)StringArgumentType.word()).suggests((ctx, builder) -> SharedSuggestionProvider.suggest(List.of("blue", "red"), (SuggestionsBuilder)builder)).executes(ctx -> ModCommands.removeMainZone((CommandSourceStack)ctx.getSource(), StringArgumentType.getString((CommandContext)ctx, (String)"team")))))).then(Commands.literal((String)"removemainzone").then(Commands.argument((String)"team", (ArgumentType)StringArgumentType.word()).suggests((ctx, builder) -> SharedSuggestionProvider.suggest(List.of("blue", "red"), (SuggestionsBuilder)builder)).executes(ctx -> ModCommands.removeMainZone((CommandSourceStack)ctx.getSource(), StringArgumentType.getString((CommandContext)ctx, (String)"team")))))).then(Commands.literal((String)"teamjoin").then(Commands.argument((String)"team", (ArgumentType)StringArgumentType.word()).suggests((ctx, builder) -> SharedSuggestionProvider.suggest(List.of("blue", "red"), (SuggestionsBuilder)builder)).then(Commands.argument((String)"player", (ArgumentType)EntityArgument.player()).executes(ctx -> ModCommands.joinTeam((CommandSourceStack)ctx.getSource(), StringArgumentType.getString((CommandContext)ctx, (String)"team"), EntityArgument.getPlayer((CommandContext)ctx, (String)"player"))))))).then(Commands.literal((String)"addpoint").then(Commands.argument((String)"shape", (ArgumentType)StringArgumentType.word()).suggests((ctx, builder) -> SharedSuggestionProvider.suggest(List.of("cube", "cylinder"), (SuggestionsBuilder)builder)).then(Commands.argument((String)"pos1", (ArgumentType)BlockPosArgument.blockPos()).then(Commands.argument((String)"pos2", (ArgumentType)BlockPosArgument.blockPos()).then(Commands.argument((String)"name", (ArgumentType)StringArgumentType.string()).then(Commands.argument((String)"bluePriority", (ArgumentType)IntegerArgumentType.integer(0, 999)).then(Commands.argument((String)"redPriority", (ArgumentType)IntegerArgumentType.integer(0, 999)).then(Commands.argument((String)"timeMin", (ArgumentType)IntegerArgumentType.integer(1)).then(Commands.argument((String)"penalty", (ArgumentType)IntegerArgumentType.integer(0)).then(Commands.argument((String)"captureDeduct", (ArgumentType)IntegerArgumentType.integer(0)).then(Commands.argument((String)"lockMinutes", (ArgumentType)IntegerArgumentType.integer(0)).executes(ctx -> ModCommands.addPoint((CommandSourceStack)ctx.getSource(), StringArgumentType.getString((CommandContext)ctx, (String)"shape"), BlockPosArgument.getSpawnablePos((CommandContext)ctx, (String)"pos1"), BlockPosArgument.getSpawnablePos((CommandContext)ctx, (String)"pos2"), StringArgumentType.getString((CommandContext)ctx, (String)"name"), IntegerArgumentType.getInteger((CommandContext)ctx, (String)"bluePriority"), IntegerArgumentType.getInteger((CommandContext)ctx, (String)"redPriority"), IntegerArgumentType.getInteger((CommandContext)ctx, (String)"timeMin"), IntegerArgumentType.getInteger((CommandContext)ctx, (String)"penalty"), IntegerArgumentType.getInteger((CommandContext)ctx, (String)"captureDeduct"), IntegerArgumentType.getInteger((CommandContext)ctx, (String)"lockMinutes"))))))))))))))).then(Commands.literal((String)"warn").then(Commands.argument((String)"target", (ArgumentType)EntityArgument.player()).then(Commands.argument((String)"message", (ArgumentType)StringArgumentType.greedyString()).executes(ctx -> ModCommands.issueWarning((CommandSourceStack)ctx.getSource(), EntityArgument.getPlayer((CommandContext)ctx, (String)"target"), StringArgumentType.getString((CommandContext)ctx, (String)"message"))))))).then(Commands.literal((String)"removepoint").then(Commands.argument((String)"name", (ArgumentType)StringArgumentType.greedyString()).suggests((ctx, builder) -> ModCommands.suggestLocalPoints((CommandContext<CommandSourceStack>)ctx, builder)).executes(ctx -> ModCommands.removePoint((CommandSourceStack)ctx.getSource(), StringArgumentType.getString((CommandContext)ctx, (String)"name")))))).then(Commands.literal((String)"pointclear").then(Commands.argument((String)"name", (ArgumentType)StringArgumentType.greedyString()).suggests((ctx, builder) -> ModCommands.suggestLocalPoints((CommandContext<CommandSourceStack>)ctx, builder)).executes(ctx -> ModCommands.clearSpecificPoint((CommandSourceStack)ctx.getSource(), StringArgumentType.getString((CommandContext)ctx, (String)"name")))))).then(Commands.literal((String)"pointcapture").then(Commands.argument((String)"team", (ArgumentType)StringArgumentType.word()).suggests((ctx, builder) -> SharedSuggestionProvider.suggest(List.of("blue", "red"), (SuggestionsBuilder)builder)).then(Commands.argument((String)"name", (ArgumentType)StringArgumentType.greedyString()).suggests((ctx, builder) -> ModCommands.suggestLocalPoints((CommandContext<CommandSourceStack>)ctx, builder)).executes(ctx -> ModCommands.forceCapturePoint((CommandSourceStack)ctx.getSource(), StringArgumentType.getString((CommandContext)ctx, (String)"team"), StringArgumentType.getString((CommandContext)ctx, (String)"name"))))))).then(Commands.literal((String)"teamspawn").then(Commands.argument((String)"team", (ArgumentType)StringArgumentType.word()).suggests((ctx, builder) -> SharedSuggestionProvider.suggest(List.of("blue", "red", "none"), (SuggestionsBuilder)builder)).then(Commands.argument((String)"pos", (ArgumentType)BlockPosArgument.blockPos()).executes(ctx -> ModCommands.setTeamSpawn((CommandSourceStack)ctx.getSource(), StringArgumentType.getString((CommandContext)ctx, (String)"team"), BlockPosArgument.getSpawnablePos((CommandContext)ctx, (String)"pos"))))))).then(((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal((String)"map").then(Commands.literal((String)"setimage").then(Commands.argument((String)"imagename", (ArgumentType)StringArgumentType.word()).suggests((ctx, builder) -> SharedSuggestionProvider.suggest(List.of("map1", "map2", "map3", "map4", "map5", "map6", "map7", "map8", "map9", "map10", "map11", "map12"), (SuggestionsBuilder)builder)).executes(ctx -> {
            String imgName = StringArgumentType.getString((CommandContext)ctx, (String)"imagename");
            ServerLevel level = ((CommandSourceStack)ctx.getSource()).getLevel();
            AASWorldData data = AASWorldData.get(level);
            data.currentMapImage = imgName;
            data.setDirty();
            PacketHandler.sendToAllClients(level, data);
            ((CommandSourceStack)ctx.getSource()).sendSuccess(() -> Component.literal((String)("Map image set to: " + imgName + ".png")), true);
            return 1;
        })))).then(Commands.literal((String)"setcenter").then(Commands.argument((String)"x", (ArgumentType)IntegerArgumentType.integer()).then(Commands.argument((String)"z", (ArgumentType)IntegerArgumentType.integer()).executes(ctx -> {
            int x = IntegerArgumentType.getInteger((CommandContext)ctx, (String)"x");
            int z = IntegerArgumentType.getInteger((CommandContext)ctx, (String)"z");
            ServerLevel level = ((CommandSourceStack)ctx.getSource()).getLevel();
            AASWorldData data = AASWorldData.get(level);
            data.mapCenterX = x;
            data.mapCenterZ = z;
            data.setDirty();
            PacketHandler.sendToAllClients(level, data);
            ((CommandSourceStack)ctx.getSource()).sendSuccess(() -> Component.literal((String)("Map center manually set to X: " + x + ", Z: " + z)), true);
            return 1;
        }))))).then(Commands.literal((String)"setsize").then(Commands.argument((String)"blocks", (ArgumentType)IntegerArgumentType.integer(128)).executes(ctx -> {
            int size = IntegerArgumentType.getInteger((CommandContext)ctx, (String)"blocks");
            AASWorldData data = AASWorldData.get(((CommandSourceStack)ctx.getSource()).getLevel());
            data.mapSizeBlocks = size;
            data.setDirty();
            PacketHandler.sendToAllClients(((CommandSourceStack)ctx.getSource()).getLevel(), data);
            ((CommandSourceStack)ctx.getSource()).sendSuccess(() -> Component.literal((String)("Map world size set to " + size + " blocks.")), true);
            return 1;
        }))))).then(Commands.literal((String)"fraction").then(Commands.argument((String)"team", (ArgumentType)StringArgumentType.word()).suggests((ctx, builder) -> SharedSuggestionProvider.suggest(List.of("blue", "red"), (SuggestionsBuilder)builder)).then(Commands.argument((String)"faction", (ArgumentType)StringArgumentType.word()).suggests((ctx, builder) -> SharedSuggestionProvider.suggest(List.of("ukraine", "russia", "usa", "nato", "bluefor", "redfor", "insurgency", "pmc", "clear"), (SuggestionsBuilder)builder)).executes(ctx -> ModCommands.setFaction((CommandSourceStack)ctx.getSource(), StringArgumentType.getString((CommandContext)ctx, (String)"team"), StringArgumentType.getString((CommandContext)ctx, (String)"faction"))))))).then(Commands.literal((String)"votestart").then(Commands.argument((String)"active", (ArgumentType)BoolArgumentType.bool()).executes(ctx -> {
            boolean active;
            ServerLevel level = ((CommandSourceStack)ctx.getSource()).getLevel();
            AASWorldData data = AASWorldData.get(level);
            data.voteActive = active = BoolArgumentType.getBool((CommandContext)ctx, (String)"active");
            if (active) {
                data.voteTimer = (Integer)AASConfig.VOTE_AUTO_START_TIME.get() * 60;
                data.votes.clear();
                data.blueReady = false;
                data.redReady = false;
            }
            data.setDirty();
            PacketHandler.sendToAllClients(level, data);
            String status = active ? "started" : "stopped";
            ((CommandSourceStack)ctx.getSource()).sendSuccess(() -> Component.literal((String)("Voting process " + status)), true);
            return 1;
        }))));
    }

    private static CompletableFuture<Suggestions> suggestLocalPoints(CommandContext<CommandSourceStack> ctx, SuggestionsBuilder builder) {
        ServerLevel level = ((CommandSourceStack)ctx.getSource()).getLevel();
        AASWorldData data = AASWorldData.get(level);
        ArrayList<String> pointNames = new ArrayList<String>();
        for (AASWorldData.CapturePoint p : data.capturePoints) {
            pointNames.add(p.name);
        }
        return SharedSuggestionProvider.suggest(pointNames, (SuggestionsBuilder)builder);
    }

    private static int setGameStart(CommandSourceStack source, boolean active) {
        ServerLevel level = source.getLevel();
        AASWorldData data = AASWorldData.get(level);
        if (active) {
            data.playedBlueSiren = false;
            data.playedRedSiren = false;
            GameLogicEvents.startGameCountdown(level);
            source.sendSuccess(() -> Component.literal((String)"Countdown started in this world!").withStyle(ChatFormatting.GREEN), true);
        } else {
            data.isGameStarted = false;
            GameLogicEvents.cancelCountdown(level);
            data.setDirty();
            ModCommands.syncDataToAll(level, data);
            source.sendSuccess(() -> Component.literal((String)"Game Stopped in this world!").withStyle(ChatFormatting.RED), true);
        }
        return 1;
    }

    private static int setRespawnTime(CommandSourceStack source, int seconds) {
        ServerLevel level = source.getLevel();
        AASWorldData data = AASWorldData.get(level);
        data.respawnTimer = seconds;
        data.setDirty();
        ModCommands.syncDataToAll(level, data);
        source.sendSuccess(() -> Component.literal((String)("Respawn timer set to " + seconds + "s for current world")).withStyle(ChatFormatting.GREEN), true);
        return 1;
    }

    private static int setFaction(CommandSourceStack source, String team, String faction) {
        String valueToSave;
        ServerLevel level = source.getLevel();
        AASWorldData data = AASWorldData.get(level);
        String cleanFaction = faction.toLowerCase();
        String string = valueToSave = cleanFaction.equals("clear") ? "none" : cleanFaction;
        if (team.equalsIgnoreCase("blue")) {
            data.blueFaction = valueToSave;
        } else if (team.equalsIgnoreCase("red")) {
            data.redFaction = valueToSave;
        }
        data.setDirty();
        ModCommands.syncDataToAll(level, data);
        return 1;
    }

    private static int setTickets(CommandSourceStack source, String team, int amount) {
        ServerLevel level = source.getLevel();
        AASWorldData data = AASWorldData.get(level);
        if (team.equalsIgnoreCase("blue")) {
            data.blueTickets = amount;
            if (amount > 50) {
                data.playedBlueSiren = false;
            }
        } else if (team.equalsIgnoreCase("red")) {
            data.redTickets = amount;
            if (amount > 50) {
                data.playedRedSiren = false;
            }
        }
        data.setDirty();
        ModCommands.syncDataToAll(level, data);
        source.sendSuccess(() -> Component.literal((String)(team.toUpperCase() + " tickets set to " + amount)).withStyle(ChatFormatting.GOLD), true);
        return 1;
    }

    private static int setDeathTickets(CommandSourceStack source, int amount) {
        ServerLevel level = source.getLevel();
        AASWorldData data = AASWorldData.get(level);
        data.deathTicketCost = amount;
        data.setDirty();
        ModCommands.syncDataToAll(level, data);
        source.sendSuccess(() -> Component.literal((String)("Death cost set to " + amount)).withStyle(ChatFormatting.GOLD), true);
        return 1;
    }

    private static int joinTeam(CommandSourceStack source, String teamName, ServerPlayer player) {
        ServerLevel level = source.getLevel();
        AASWorldData data = AASWorldData.get(level);
        PacketSquadAction.leaveCurrentSquad(player, data);
        data.setDirty();
        ModCommands.syncDataToAll(level, data);
        PacketHandler.INSTANCE.send(PacketDistributor.ALL.noArg(), (Object)new PacketSyncSquads(data.squads));
        ServerScoreboard scoreboard = source.getServer().getScoreboard();
        String internalTeamName = teamName.equalsIgnoreCase("blue") ? "Blue" : "Red";
        ChatFormatting color = teamName.equalsIgnoreCase("blue") ? ChatFormatting.BLUE : ChatFormatting.RED;
        PlayerTeam team = scoreboard.getPlayerTeam(internalTeamName);
        if (team == null) {
            team = scoreboard.addPlayerTeam(internalTeamName);
        }
        team.setColor(color);
        scoreboard.addPlayerToTeam(player.getScoreboardName(), team);
        source.sendSuccess(() -> Component.literal((String)("Player joined " + internalTeamName)).withStyle(color), true);
        return 1;
    }

    private static int addPoint(CommandSourceStack source, String shape, BlockPos pos1, BlockPos pos2, String name, int bp, int rp, int time, int penalty, int captureDeduct, int lockMinutes) {
        AABB area;
        ServerLevel level = source.getLevel();
        AASWorldData data = AASWorldData.get(level);
        if (shape.equalsIgnoreCase("cylinder")) {
            double radius = Math.sqrt(pos1.distSqr((Vec3i)new BlockPos(pos2.getX(), pos1.getY(), pos2.getZ())));
            double minY = Math.min(pos1.getY(), pos2.getY());
            double maxY = Math.max(pos1.getY(), pos2.getY()) + 1;
            area = new AABB((double)pos1.getX() - radius, minY, (double)pos1.getZ() - radius, (double)pos1.getX() + radius, maxY, (double)pos1.getZ() + radius);
        } else {
            double minX = Math.min(pos1.getX(), pos2.getX());
            double minY = Math.min(pos1.getY(), pos2.getY());
            double minZ = Math.min(pos1.getZ(), pos2.getZ());
            double maxX = Math.max(pos1.getX(), pos2.getX()) + 1;
            double maxY = Math.max(pos1.getY(), pos2.getY()) + 1;
            double maxZ = Math.max(pos1.getZ(), pos2.getZ()) + 1;
            area = new AABB(minX, minY, minZ, maxX, maxY, maxZ);
        }
        data.capturePoints.add(new AASWorldData.CapturePoint(name, area, bp, rp, time, penalty, captureDeduct, shape.toUpperCase(), lockMinutes));
        data.setDirty();
        source.sendSuccess(() -> Component.literal((String)("Point '" + name + "' (" + shape + ") added! Lock: " + lockMinutes + " min.")), true);
        PacketHandler.sendToAllClients(level, data);
        return 1;
    }

    private static int removePoint(CommandSourceStack source, String name) {
        ServerLevel level = source.getLevel();
        AASWorldData data = AASWorldData.get(level);
        boolean removed = data.capturePoints.removeIf(p -> p.name.equals(name));
        if (removed) {
            data.setDirty();
            ModCommands.syncDataToAll(level, data);
            source.sendSuccess(() -> Component.literal((String)("Point '" + name + "' removed from " + String.valueOf(level.dimension().location()))).withStyle(ChatFormatting.RED), true);
            return 1;
        }
        source.sendFailure((Component)Component.literal((String)("Point '" + name + "' not found in THIS world!")));
        return 0;
    }

    private static int clearSpecificPoint(CommandSourceStack source, String name) {
        ServerLevel level = source.getLevel();
        AASWorldData data = AASWorldData.get(level);
        for (AASWorldData.CapturePoint point : data.capturePoints) {
            if (!point.name.equals(name)) continue;
            point.owner = "NEUTRAL";
            point.progress = 0.0f;
            point.capturingTeam = "NONE";
            data.setDirty();
            ModCommands.syncDataToAll(level, data);
            source.sendSuccess(() -> Component.literal((String)("Point '" + name + "' reset to NEUTRAL in this world!")).withStyle(ChatFormatting.YELLOW), true);
            return 1;
        }
        source.sendFailure((Component)Component.literal((String)("Point '" + name + "' not found in THIS world!")));
        return 0;
    }

    private static int forceCapturePoint(CommandSourceStack source, String teamInput, String name) {
        ServerLevel level = source.getLevel();
        AASWorldData data = AASWorldData.get(level);
        String targetTeam = teamInput.toUpperCase();
        if (!targetTeam.equals("BLUE") && !targetTeam.equals("RED")) {
            source.sendFailure((Component)Component.literal((String)"Invalid team! Please use 'blue' or 'red'."));
            return 0;
        }
        for (AASWorldData.CapturePoint point : data.capturePoints) {
            if (!point.name.equals(name)) continue;
            point.owner = targetTeam;
            point.progress = 1.0f;
            point.capturingTeam = "NONE";
            data.setDirty();
            ModCommands.syncDataToAll(level, data);
            ChatFormatting color = targetTeam.equals("BLUE") ? ChatFormatting.BLUE : ChatFormatting.RED;
            source.sendSuccess(() -> Component.literal((String)("Point '" + name + "' forcefully captured by " + targetTeam + "!")).withStyle(color), true);
            level.getServer().getPlayerList().broadcastSystemMessage((Component)Component.literal((String)("[ADMIN] Point " + name + " forcefully captured by " + targetTeam)).withStyle(color), false);
            return 1;
        }
        source.sendFailure((Component)Component.literal((String)("Point '" + name + "' not found in THIS world!")));
        return 0;
    }

    private static int setTeamSpawn(CommandSourceStack source, String teamName, BlockPos pos) {
        ServerLevel level = source.getLevel();
        AASWorldData data = AASWorldData.get(level);
        String currentDim = level.dimension().location().toString();
        if (teamName.equalsIgnoreCase("blue")) {
            data.blueSpawns.put(currentDim, pos);
        } else if (teamName.equalsIgnoreCase("red")) {
            data.redSpawns.put(currentDim, pos);
        } else if (teamName.equalsIgnoreCase("none")) {
            data.neutralSpawns.put(currentDim, pos);
        }
        data.setDirty();
        ModCommands.syncDataToAll(level, data);
        source.sendSuccess(() -> Component.literal((String)"Spawn set for this dimension.").withStyle(ChatFormatting.GREEN), true);
        return 1;
    }

    private static int issueWarning(CommandSourceStack source, ServerPlayer target, String message) {
        target.connection.send((Packet)new ClientboundSetTitlesAnimationPacket(10, 140, 20));
        target.connection.send((Packet)new ClientboundSetSubtitleTextPacket((Component)Component.literal((String)message).withStyle(ChatFormatting.YELLOW)));
        target.connection.send((Packet)new ClientboundSetTitleTextPacket((Component)Component.literal((String)"!WARNING!").withStyle(new ChatFormatting[]{ChatFormatting.DARK_RED, ChatFormatting.BOLD})));
        target.playNotifySound(SoundEvents.ANVIL_LAND, SoundSource.MASTER, 1.0f, 0.8f);
        target.sendSystemMessage((Component)Component.literal((String)("[ADMIN WARN] " + message)).withStyle(new ChatFormatting[]{ChatFormatting.RED, ChatFormatting.BOLD}));
        source.sendSuccess(() -> Component.literal((String)("Successfully warned " + target.getScoreboardName() + "!")).withStyle(ChatFormatting.GREEN), true);
        return 1;
    }

    private static void syncDataToAll(ServerLevel level, AASWorldData data) {
        PacketHandler.sendToAllClients(level, data);
    }

    private static int addMainZone(CommandSourceStack source, String team, String shape, BlockPos pos1, BlockPos pos2) {
        AABB area;
        ServerLevel level = source.getLevel();
        AASWorldData data = AASWorldData.get(level);
        if (shape.equalsIgnoreCase("cylinder")) {
            double radius = Math.sqrt(pos1.distSqr((Vec3i)new BlockPos(pos2.getX(), pos1.getY(), pos2.getZ())));
            double minY = Math.min(pos1.getY(), pos2.getY());
            double maxY = Math.max(pos1.getY(), pos2.getY()) + 1;
            area = new AABB((double)pos1.getX() - radius, minY, (double)pos1.getZ() - radius, (double)pos1.getX() + radius, maxY, (double)pos1.getZ() + radius);
        } else {
            area = new AABB((double)Math.min(pos1.getX(), pos2.getX()), (double)Math.min(pos1.getY(), pos2.getY()), (double)Math.min(pos1.getZ(), pos2.getZ()), (double)(Math.max(pos1.getX(), pos2.getX()) + 1), (double)(Math.max(pos1.getY(), pos2.getY()) + 1), (double)(Math.max(pos1.getZ(), pos2.getZ()) + 1));
        }
        data.mainZones.removeIf(z -> z.team.equalsIgnoreCase(team));
        data.mainZones.add(new AASWorldData.MainProtectionZone(team.toUpperCase(), shape.toUpperCase(), area));
        data.setDirty();
        source.sendSuccess(() -> Component.literal((String)(team.toUpperCase() + " Main Protection Zone successfully added!")).withStyle(ChatFormatting.GREEN), true);
        return 1;
    }

    private static int removeMainZone(CommandSourceStack source, String team) {
        ServerLevel level = source.getLevel();
        AASWorldData data = AASWorldData.get(level);
        boolean removed = data.mainZones.removeIf(z -> z.team.equalsIgnoreCase(team));
        if (removed) {
            data.setDirty();
            source.sendSuccess(() -> Component.literal((String)(team.toUpperCase() + " Main Protection Zone removed!")).withStyle(ChatFormatting.GREEN), true);
        } else {
            source.sendFailure((Component)Component.literal((String)("No protection zone found for team: " + team.toUpperCase())));
        }
        return 1;
    }

    private static int clearSquads(CommandSourceStack source, String teamName) {
        ServerLevel level = source.getLevel();
        AASWorldData data = AASWorldData.get(level);
        String targetTeam = teamName.toUpperCase();
        for (ServerPlayer player : source.getServer().getPlayerList().getPlayers()) {
            boolean belongsToTeam;
            if (!player.getPersistentData().contains("AAS_SquadID") || !(belongsToTeam = data.squads.stream().anyMatch(s -> s.id == player.getPersistentData().getInt("AAS_SquadID") && s.team.equalsIgnoreCase(targetTeam)))) continue;
            player.getPersistentData().remove("AAS_SquadID");
            player.getPersistentData().remove("AAS_IsSquadLeader");
        }
        data.squads.removeIf(squad -> squad.team.equalsIgnoreCase(targetTeam));
        if (targetTeam.equals("BLUE")) {
            data.blueCMDId = -1;
        } else if (targetTeam.equals("RED")) {
            data.redCMDId = -1;
        }
        data.setDirty();
        PacketHandler.sendToAllClients(level, data);
        source.sendSuccess(() -> Component.literal((String)("Cleared squads and CMD for " + targetTeam)).withStyle(ChatFormatting.GREEN), true);
        return 1;
    }
}

