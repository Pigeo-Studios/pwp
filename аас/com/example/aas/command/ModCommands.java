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
        dispatcher.register((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.m_82127_((String)"aas").requires(s -> s.m_6761_(2))).then(Commands.m_82127_((String)"gamestart").then(Commands.m_82129_((String)"active", (ArgumentType)BoolArgumentType.bool()).executes(ctx -> ModCommands.setGameStart((CommandSourceStack)ctx.getSource(), BoolArgumentType.getBool((CommandContext)ctx, (String)"active")))))).then(Commands.m_82127_((String)"deathtimer").then(Commands.m_82129_((String)"seconds", (ArgumentType)IntegerArgumentType.integer((int)0)).executes(ctx -> ModCommands.setRespawnTime((CommandSourceStack)ctx.getSource(), IntegerArgumentType.getInteger((CommandContext)ctx, (String)"seconds")))))).then(Commands.m_82127_((String)"deathtickets").then(Commands.m_82129_((String)"amount", (ArgumentType)IntegerArgumentType.integer((int)0)).executes(ctx -> ModCommands.setDeathTickets((CommandSourceStack)ctx.getSource(), IntegerArgumentType.getInteger((CommandContext)ctx, (String)"amount")))))).then(Commands.m_82127_((String)"clearsquad").then(Commands.m_82129_((String)"team", (ArgumentType)StringArgumentType.word()).suggests((ctx, builder) -> SharedSuggestionProvider.m_82970_(List.of("blue", "red"), (SuggestionsBuilder)builder)).executes(ctx -> ModCommands.clearSquads((CommandSourceStack)ctx.getSource(), StringArgumentType.getString((CommandContext)ctx, (String)"team")))))).then(Commands.m_82127_((String)"teamtickets").then(Commands.m_82129_((String)"team", (ArgumentType)StringArgumentType.word()).suggests((ctx, builder) -> SharedSuggestionProvider.m_82970_(List.of("blue", "red"), (SuggestionsBuilder)builder)).then(Commands.m_82129_((String)"amount", (ArgumentType)IntegerArgumentType.integer()).executes(ctx -> ModCommands.setTickets((CommandSourceStack)ctx.getSource(), StringArgumentType.getString((CommandContext)ctx, (String)"team"), IntegerArgumentType.getInteger((CommandContext)ctx, (String)"amount"))))))).then(Commands.m_82127_((String)"mainzone").then(Commands.m_82129_((String)"team", (ArgumentType)StringArgumentType.word()).suggests((ctx, builder) -> SharedSuggestionProvider.m_82970_(List.of("blue", "red"), (SuggestionsBuilder)builder)).then(Commands.m_82129_((String)"shape", (ArgumentType)StringArgumentType.word()).suggests((ctx, builder) -> SharedSuggestionProvider.m_82970_(List.of("cube", "cylinder"), (SuggestionsBuilder)builder)).then(Commands.m_82129_((String)"pos1", (ArgumentType)BlockPosArgument.m_118239_()).then(Commands.m_82129_((String)"pos2", (ArgumentType)BlockPosArgument.m_118239_()).executes(ctx -> ModCommands.addMainZone((CommandSourceStack)ctx.getSource(), StringArgumentType.getString((CommandContext)ctx, (String)"team"), StringArgumentType.getString((CommandContext)ctx, (String)"shape"), BlockPosArgument.m_174395_((CommandContext)ctx, (String)"pos1"), BlockPosArgument.m_174395_((CommandContext)ctx, (String)"pos2"))))))))).then(Commands.m_82127_((String)"removemainzone").then(Commands.m_82129_((String)"team", (ArgumentType)StringArgumentType.word()).suggests((ctx, builder) -> SharedSuggestionProvider.m_82970_(List.of("blue", "red"), (SuggestionsBuilder)builder)).executes(ctx -> ModCommands.removeMainZone((CommandSourceStack)ctx.getSource(), StringArgumentType.getString((CommandContext)ctx, (String)"team")))))).then(Commands.m_82127_((String)"removemainzone").then(Commands.m_82129_((String)"team", (ArgumentType)StringArgumentType.word()).suggests((ctx, builder) -> SharedSuggestionProvider.m_82970_(List.of("blue", "red"), (SuggestionsBuilder)builder)).executes(ctx -> ModCommands.removeMainZone((CommandSourceStack)ctx.getSource(), StringArgumentType.getString((CommandContext)ctx, (String)"team")))))).then(Commands.m_82127_((String)"teamjoin").then(Commands.m_82129_((String)"team", (ArgumentType)StringArgumentType.word()).suggests((ctx, builder) -> SharedSuggestionProvider.m_82970_(List.of("blue", "red"), (SuggestionsBuilder)builder)).then(Commands.m_82129_((String)"player", (ArgumentType)EntityArgument.m_91466_()).executes(ctx -> ModCommands.joinTeam((CommandSourceStack)ctx.getSource(), StringArgumentType.getString((CommandContext)ctx, (String)"team"), EntityArgument.m_91474_((CommandContext)ctx, (String)"player"))))))).then(Commands.m_82127_((String)"addpoint").then(Commands.m_82129_((String)"shape", (ArgumentType)StringArgumentType.word()).suggests((ctx, builder) -> SharedSuggestionProvider.m_82970_(List.of("cube", "cylinder"), (SuggestionsBuilder)builder)).then(Commands.m_82129_((String)"pos1", (ArgumentType)BlockPosArgument.m_118239_()).then(Commands.m_82129_((String)"pos2", (ArgumentType)BlockPosArgument.m_118239_()).then(Commands.m_82129_((String)"name", (ArgumentType)StringArgumentType.string()).then(Commands.m_82129_((String)"bluePriority", (ArgumentType)IntegerArgumentType.integer((int)0, (int)999)).then(Commands.m_82129_((String)"redPriority", (ArgumentType)IntegerArgumentType.integer((int)0, (int)999)).then(Commands.m_82129_((String)"timeMin", (ArgumentType)IntegerArgumentType.integer((int)1)).then(Commands.m_82129_((String)"penalty", (ArgumentType)IntegerArgumentType.integer((int)0)).then(Commands.m_82129_((String)"captureDeduct", (ArgumentType)IntegerArgumentType.integer((int)0)).then(Commands.m_82129_((String)"lockMinutes", (ArgumentType)IntegerArgumentType.integer((int)0)).executes(ctx -> ModCommands.addPoint((CommandSourceStack)ctx.getSource(), StringArgumentType.getString((CommandContext)ctx, (String)"shape"), BlockPosArgument.m_174395_((CommandContext)ctx, (String)"pos1"), BlockPosArgument.m_174395_((CommandContext)ctx, (String)"pos2"), StringArgumentType.getString((CommandContext)ctx, (String)"name"), IntegerArgumentType.getInteger((CommandContext)ctx, (String)"bluePriority"), IntegerArgumentType.getInteger((CommandContext)ctx, (String)"redPriority"), IntegerArgumentType.getInteger((CommandContext)ctx, (String)"timeMin"), IntegerArgumentType.getInteger((CommandContext)ctx, (String)"penalty"), IntegerArgumentType.getInteger((CommandContext)ctx, (String)"captureDeduct"), IntegerArgumentType.getInteger((CommandContext)ctx, (String)"lockMinutes"))))))))))))))).then(Commands.m_82127_((String)"warn").then(Commands.m_82129_((String)"target", (ArgumentType)EntityArgument.m_91466_()).then(Commands.m_82129_((String)"message", (ArgumentType)StringArgumentType.greedyString()).executes(ctx -> ModCommands.issueWarning((CommandSourceStack)ctx.getSource(), EntityArgument.m_91474_((CommandContext)ctx, (String)"target"), StringArgumentType.getString((CommandContext)ctx, (String)"message"))))))).then(Commands.m_82127_((String)"removepoint").then(Commands.m_82129_((String)"name", (ArgumentType)StringArgumentType.greedyString()).suggests((ctx, builder) -> ModCommands.suggestLocalPoints((CommandContext<CommandSourceStack>)ctx, builder)).executes(ctx -> ModCommands.removePoint((CommandSourceStack)ctx.getSource(), StringArgumentType.getString((CommandContext)ctx, (String)"name")))))).then(Commands.m_82127_((String)"pointclear").then(Commands.m_82129_((String)"name", (ArgumentType)StringArgumentType.greedyString()).suggests((ctx, builder) -> ModCommands.suggestLocalPoints((CommandContext<CommandSourceStack>)ctx, builder)).executes(ctx -> ModCommands.clearSpecificPoint((CommandSourceStack)ctx.getSource(), StringArgumentType.getString((CommandContext)ctx, (String)"name")))))).then(Commands.m_82127_((String)"pointcapture").then(Commands.m_82129_((String)"team", (ArgumentType)StringArgumentType.word()).suggests((ctx, builder) -> SharedSuggestionProvider.m_82970_(List.of("blue", "red"), (SuggestionsBuilder)builder)).then(Commands.m_82129_((String)"name", (ArgumentType)StringArgumentType.greedyString()).suggests((ctx, builder) -> ModCommands.suggestLocalPoints((CommandContext<CommandSourceStack>)ctx, builder)).executes(ctx -> ModCommands.forceCapturePoint((CommandSourceStack)ctx.getSource(), StringArgumentType.getString((CommandContext)ctx, (String)"team"), StringArgumentType.getString((CommandContext)ctx, (String)"name"))))))).then(Commands.m_82127_((String)"teamspawn").then(Commands.m_82129_((String)"team", (ArgumentType)StringArgumentType.word()).suggests((ctx, builder) -> SharedSuggestionProvider.m_82970_(List.of("blue", "red", "none"), (SuggestionsBuilder)builder)).then(Commands.m_82129_((String)"pos", (ArgumentType)BlockPosArgument.m_118239_()).executes(ctx -> ModCommands.setTeamSpawn((CommandSourceStack)ctx.getSource(), StringArgumentType.getString((CommandContext)ctx, (String)"team"), BlockPosArgument.m_174395_((CommandContext)ctx, (String)"pos"))))))).then(((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.m_82127_((String)"map").then(Commands.m_82127_((String)"setimage").then(Commands.m_82129_((String)"imagename", (ArgumentType)StringArgumentType.word()).suggests((ctx, builder) -> SharedSuggestionProvider.m_82970_(List.of("map1", "map2", "map3", "map4", "map5", "map6", "map7", "map8", "map9", "map10", "map11", "map12"), (SuggestionsBuilder)builder)).executes(ctx -> {
            String imgName = StringArgumentType.getString((CommandContext)ctx, (String)"imagename");
            ServerLevel level = ((CommandSourceStack)ctx.getSource()).m_81372_();
            AASWorldData data = AASWorldData.get(level);
            data.currentMapImage = imgName;
            data.m_77762_();
            PacketHandler.sendToAllClients(level, data);
            ((CommandSourceStack)ctx.getSource()).m_288197_(() -> Component.m_237113_((String)("Map image set to: " + imgName + ".png")), true);
            return 1;
        })))).then(Commands.m_82127_((String)"setcenter").then(Commands.m_82129_((String)"x", (ArgumentType)IntegerArgumentType.integer()).then(Commands.m_82129_((String)"z", (ArgumentType)IntegerArgumentType.integer()).executes(ctx -> {
            int x = IntegerArgumentType.getInteger((CommandContext)ctx, (String)"x");
            int z = IntegerArgumentType.getInteger((CommandContext)ctx, (String)"z");
            ServerLevel level = ((CommandSourceStack)ctx.getSource()).m_81372_();
            AASWorldData data = AASWorldData.get(level);
            data.mapCenterX = x;
            data.mapCenterZ = z;
            data.m_77762_();
            PacketHandler.sendToAllClients(level, data);
            ((CommandSourceStack)ctx.getSource()).m_288197_(() -> Component.m_237113_((String)("Map center manually set to X: " + x + ", Z: " + z)), true);
            return 1;
        }))))).then(Commands.m_82127_((String)"setsize").then(Commands.m_82129_((String)"blocks", (ArgumentType)IntegerArgumentType.integer((int)128)).executes(ctx -> {
            int size = IntegerArgumentType.getInteger((CommandContext)ctx, (String)"blocks");
            AASWorldData data = AASWorldData.get(((CommandSourceStack)ctx.getSource()).m_81372_());
            data.mapSizeBlocks = size;
            data.m_77762_();
            PacketHandler.sendToAllClients(((CommandSourceStack)ctx.getSource()).m_81372_(), data);
            ((CommandSourceStack)ctx.getSource()).m_288197_(() -> Component.m_237113_((String)("Map world size set to " + size + " blocks.")), true);
            return 1;
        }))))).then(Commands.m_82127_((String)"fraction").then(Commands.m_82129_((String)"team", (ArgumentType)StringArgumentType.word()).suggests((ctx, builder) -> SharedSuggestionProvider.m_82970_(List.of("blue", "red"), (SuggestionsBuilder)builder)).then(Commands.m_82129_((String)"faction", (ArgumentType)StringArgumentType.word()).suggests((ctx, builder) -> SharedSuggestionProvider.m_82970_(List.of("ukraine", "russia", "usa", "nato", "bluefor", "redfor", "insurgency", "pmc", "clear"), (SuggestionsBuilder)builder)).executes(ctx -> ModCommands.setFaction((CommandSourceStack)ctx.getSource(), StringArgumentType.getString((CommandContext)ctx, (String)"team"), StringArgumentType.getString((CommandContext)ctx, (String)"faction"))))))).then(Commands.m_82127_((String)"votestart").then(Commands.m_82129_((String)"active", (ArgumentType)BoolArgumentType.bool()).executes(ctx -> {
            boolean active;
            ServerLevel level = ((CommandSourceStack)ctx.getSource()).m_81372_();
            AASWorldData data = AASWorldData.get(level);
            data.voteActive = active = BoolArgumentType.getBool((CommandContext)ctx, (String)"active");
            if (active) {
                data.voteTimer = (Integer)AASConfig.VOTE_AUTO_START_TIME.get() * 60;
                data.votes.clear();
                data.blueReady = false;
                data.redReady = false;
            }
            data.m_77762_();
            PacketHandler.sendToAllClients(level, data);
            String status = active ? "started" : "stopped";
            ((CommandSourceStack)ctx.getSource()).m_288197_(() -> Component.m_237113_((String)("Voting process " + status)), true);
            return 1;
        }))));
    }

    private static CompletableFuture<Suggestions> suggestLocalPoints(CommandContext<CommandSourceStack> ctx, SuggestionsBuilder builder) {
        ServerLevel level = ((CommandSourceStack)ctx.getSource()).m_81372_();
        AASWorldData data = AASWorldData.get(level);
        ArrayList<String> pointNames = new ArrayList<String>();
        for (AASWorldData.CapturePoint p : data.capturePoints) {
            pointNames.add(p.name);
        }
        return SharedSuggestionProvider.m_82970_(pointNames, (SuggestionsBuilder)builder);
    }

    private static int setGameStart(CommandSourceStack source, boolean active) {
        ServerLevel level = source.m_81372_();
        AASWorldData data = AASWorldData.get(level);
        if (active) {
            data.playedBlueSiren = false;
            data.playedRedSiren = false;
            GameLogicEvents.startGameCountdown(level);
            source.m_288197_(() -> Component.m_237113_((String)"Countdown started in this world!").m_130940_(ChatFormatting.GREEN), true);
        } else {
            data.isGameStarted = false;
            GameLogicEvents.cancelCountdown(level);
            data.m_77762_();
            ModCommands.syncDataToAll(level, data);
            source.m_288197_(() -> Component.m_237113_((String)"Game Stopped in this world!").m_130940_(ChatFormatting.RED), true);
        }
        return 1;
    }

    private static int setRespawnTime(CommandSourceStack source, int seconds) {
        ServerLevel level = source.m_81372_();
        AASWorldData data = AASWorldData.get(level);
        data.respawnTimer = seconds;
        data.m_77762_();
        ModCommands.syncDataToAll(level, data);
        source.m_288197_(() -> Component.m_237113_((String)("Respawn timer set to " + seconds + "s for current world")).m_130940_(ChatFormatting.GREEN), true);
        return 1;
    }

    private static int setFaction(CommandSourceStack source, String team, String faction) {
        String valueToSave;
        ServerLevel level = source.m_81372_();
        AASWorldData data = AASWorldData.get(level);
        String cleanFaction = faction.toLowerCase();
        String string = valueToSave = cleanFaction.equals("clear") ? "none" : cleanFaction;
        if (team.equalsIgnoreCase("blue")) {
            data.blueFaction = valueToSave;
        } else if (team.equalsIgnoreCase("red")) {
            data.redFaction = valueToSave;
        }
        data.m_77762_();
        ModCommands.syncDataToAll(level, data);
        return 1;
    }

    private static int setTickets(CommandSourceStack source, String team, int amount) {
        ServerLevel level = source.m_81372_();
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
        data.m_77762_();
        ModCommands.syncDataToAll(level, data);
        source.m_288197_(() -> Component.m_237113_((String)(team.toUpperCase() + " tickets set to " + amount)).m_130940_(ChatFormatting.GOLD), true);
        return 1;
    }

    private static int setDeathTickets(CommandSourceStack source, int amount) {
        ServerLevel level = source.m_81372_();
        AASWorldData data = AASWorldData.get(level);
        data.deathTicketCost = amount;
        data.m_77762_();
        ModCommands.syncDataToAll(level, data);
        source.m_288197_(() -> Component.m_237113_((String)("Death cost set to " + amount)).m_130940_(ChatFormatting.GOLD), true);
        return 1;
    }

    private static int joinTeam(CommandSourceStack source, String teamName, ServerPlayer player) {
        ServerLevel level = source.m_81372_();
        AASWorldData data = AASWorldData.get(level);
        PacketSquadAction.leaveCurrentSquad(player, data);
        data.m_77762_();
        ModCommands.syncDataToAll(level, data);
        PacketHandler.INSTANCE.send(PacketDistributor.ALL.noArg(), (Object)new PacketSyncSquads(data.squads));
        ServerScoreboard scoreboard = source.m_81377_().m_129896_();
        String internalTeamName = teamName.equalsIgnoreCase("blue") ? "Blue" : "Red";
        ChatFormatting color = teamName.equalsIgnoreCase("blue") ? ChatFormatting.BLUE : ChatFormatting.RED;
        PlayerTeam team = scoreboard.m_83489_(internalTeamName);
        if (team == null) {
            team = scoreboard.m_83492_(internalTeamName);
        }
        team.m_83351_(color);
        scoreboard.m_6546_(player.m_6302_(), team);
        source.m_288197_(() -> Component.m_237113_((String)("Player joined " + internalTeamName)).m_130940_(color), true);
        return 1;
    }

    private static int addPoint(CommandSourceStack source, String shape, BlockPos pos1, BlockPos pos2, String name, int bp, int rp, int time, int penalty, int captureDeduct, int lockMinutes) {
        AABB area;
        ServerLevel level = source.m_81372_();
        AASWorldData data = AASWorldData.get(level);
        if (shape.equalsIgnoreCase("cylinder")) {
            double radius = Math.sqrt(pos1.m_123331_((Vec3i)new BlockPos(pos2.m_123341_(), pos1.m_123342_(), pos2.m_123343_())));
            double minY = Math.min(pos1.m_123342_(), pos2.m_123342_());
            double maxY = Math.max(pos1.m_123342_(), pos2.m_123342_()) + 1;
            area = new AABB((double)pos1.m_123341_() - radius, minY, (double)pos1.m_123343_() - radius, (double)pos1.m_123341_() + radius, maxY, (double)pos1.m_123343_() + radius);
        } else {
            double minX = Math.min(pos1.m_123341_(), pos2.m_123341_());
            double minY = Math.min(pos1.m_123342_(), pos2.m_123342_());
            double minZ = Math.min(pos1.m_123343_(), pos2.m_123343_());
            double maxX = Math.max(pos1.m_123341_(), pos2.m_123341_()) + 1;
            double maxY = Math.max(pos1.m_123342_(), pos2.m_123342_()) + 1;
            double maxZ = Math.max(pos1.m_123343_(), pos2.m_123343_()) + 1;
            area = new AABB(minX, minY, minZ, maxX, maxY, maxZ);
        }
        data.capturePoints.add(new AASWorldData.CapturePoint(name, area, bp, rp, time, penalty, captureDeduct, shape.toUpperCase(), lockMinutes));
        data.m_77762_();
        source.m_288197_(() -> Component.m_237113_((String)("Point '" + name + "' (" + shape + ") added! Lock: " + lockMinutes + " min.")), true);
        PacketHandler.sendToAllClients(level, data);
        return 1;
    }

    private static int removePoint(CommandSourceStack source, String name) {
        ServerLevel level = source.m_81372_();
        AASWorldData data = AASWorldData.get(level);
        boolean removed = data.capturePoints.removeIf(p -> p.name.equals(name));
        if (removed) {
            data.m_77762_();
            ModCommands.syncDataToAll(level, data);
            source.m_288197_(() -> Component.m_237113_((String)("Point '" + name + "' removed from " + String.valueOf(level.m_46472_().m_135782_()))).m_130940_(ChatFormatting.RED), true);
            return 1;
        }
        source.m_81352_((Component)Component.m_237113_((String)("Point '" + name + "' not found in THIS world!")));
        return 0;
    }

    private static int clearSpecificPoint(CommandSourceStack source, String name) {
        ServerLevel level = source.m_81372_();
        AASWorldData data = AASWorldData.get(level);
        for (AASWorldData.CapturePoint point : data.capturePoints) {
            if (!point.name.equals(name)) continue;
            point.owner = "NEUTRAL";
            point.progress = 0.0f;
            point.capturingTeam = "NONE";
            data.m_77762_();
            ModCommands.syncDataToAll(level, data);
            source.m_288197_(() -> Component.m_237113_((String)("Point '" + name + "' reset to NEUTRAL in this world!")).m_130940_(ChatFormatting.YELLOW), true);
            return 1;
        }
        source.m_81352_((Component)Component.m_237113_((String)("Point '" + name + "' not found in THIS world!")));
        return 0;
    }

    private static int forceCapturePoint(CommandSourceStack source, String teamInput, String name) {
        ServerLevel level = source.m_81372_();
        AASWorldData data = AASWorldData.get(level);
        String targetTeam = teamInput.toUpperCase();
        if (!targetTeam.equals("BLUE") && !targetTeam.equals("RED")) {
            source.m_81352_((Component)Component.m_237113_((String)"Invalid team! Please use 'blue' or 'red'."));
            return 0;
        }
        for (AASWorldData.CapturePoint point : data.capturePoints) {
            if (!point.name.equals(name)) continue;
            point.owner = targetTeam;
            point.progress = 1.0f;
            point.capturingTeam = "NONE";
            data.m_77762_();
            ModCommands.syncDataToAll(level, data);
            ChatFormatting color = targetTeam.equals("BLUE") ? ChatFormatting.BLUE : ChatFormatting.RED;
            source.m_288197_(() -> Component.m_237113_((String)("Point '" + name + "' forcefully captured by " + targetTeam + "!")).m_130940_(color), true);
            level.m_7654_().m_6846_().m_240416_((Component)Component.m_237113_((String)("[ADMIN] Point " + name + " forcefully captured by " + targetTeam)).m_130940_(color), false);
            return 1;
        }
        source.m_81352_((Component)Component.m_237113_((String)("Point '" + name + "' not found in THIS world!")));
        return 0;
    }

    private static int setTeamSpawn(CommandSourceStack source, String teamName, BlockPos pos) {
        ServerLevel level = source.m_81372_();
        AASWorldData data = AASWorldData.get(level);
        String currentDim = level.m_46472_().m_135782_().toString();
        if (teamName.equalsIgnoreCase("blue")) {
            data.blueSpawns.put(currentDim, pos);
        } else if (teamName.equalsIgnoreCase("red")) {
            data.redSpawns.put(currentDim, pos);
        } else if (teamName.equalsIgnoreCase("none")) {
            data.neutralSpawns.put(currentDim, pos);
        }
        data.m_77762_();
        ModCommands.syncDataToAll(level, data);
        source.m_288197_(() -> Component.m_237113_((String)"Spawn set for this dimension.").m_130940_(ChatFormatting.GREEN), true);
        return 1;
    }

    private static int issueWarning(CommandSourceStack source, ServerPlayer target, String message) {
        target.f_8906_.m_9829_((Packet)new ClientboundSetTitlesAnimationPacket(10, 140, 20));
        target.f_8906_.m_9829_((Packet)new ClientboundSetSubtitleTextPacket((Component)Component.m_237113_((String)message).m_130940_(ChatFormatting.YELLOW)));
        target.f_8906_.m_9829_((Packet)new ClientboundSetTitleTextPacket((Component)Component.m_237113_((String)"!WARNING!").m_130944_(new ChatFormatting[]{ChatFormatting.DARK_RED, ChatFormatting.BOLD})));
        target.m_6330_(SoundEvents.f_11668_, SoundSource.MASTER, 1.0f, 0.8f);
        target.m_213846_((Component)Component.m_237113_((String)("[ADMIN WARN] " + message)).m_130944_(new ChatFormatting[]{ChatFormatting.RED, ChatFormatting.BOLD}));
        source.m_288197_(() -> Component.m_237113_((String)("Successfully warned " + target.m_6302_() + "!")).m_130940_(ChatFormatting.GREEN), true);
        return 1;
    }

    private static void syncDataToAll(ServerLevel level, AASWorldData data) {
        PacketHandler.sendToAllClients(level, data);
    }

    private static int addMainZone(CommandSourceStack source, String team, String shape, BlockPos pos1, BlockPos pos2) {
        AABB area;
        ServerLevel level = source.m_81372_();
        AASWorldData data = AASWorldData.get(level);
        if (shape.equalsIgnoreCase("cylinder")) {
            double radius = Math.sqrt(pos1.m_123331_((Vec3i)new BlockPos(pos2.m_123341_(), pos1.m_123342_(), pos2.m_123343_())));
            double minY = Math.min(pos1.m_123342_(), pos2.m_123342_());
            double maxY = Math.max(pos1.m_123342_(), pos2.m_123342_()) + 1;
            area = new AABB((double)pos1.m_123341_() - radius, minY, (double)pos1.m_123343_() - radius, (double)pos1.m_123341_() + radius, maxY, (double)pos1.m_123343_() + radius);
        } else {
            area = new AABB((double)Math.min(pos1.m_123341_(), pos2.m_123341_()), (double)Math.min(pos1.m_123342_(), pos2.m_123342_()), (double)Math.min(pos1.m_123343_(), pos2.m_123343_()), (double)(Math.max(pos1.m_123341_(), pos2.m_123341_()) + 1), (double)(Math.max(pos1.m_123342_(), pos2.m_123342_()) + 1), (double)(Math.max(pos1.m_123343_(), pos2.m_123343_()) + 1));
        }
        data.mainZones.removeIf(z -> z.team.equalsIgnoreCase(team));
        data.mainZones.add(new AASWorldData.MainProtectionZone(team.toUpperCase(), shape.toUpperCase(), area));
        data.m_77762_();
        source.m_288197_(() -> Component.m_237113_((String)(team.toUpperCase() + " Main Protection Zone successfully added!")).m_130940_(ChatFormatting.GREEN), true);
        return 1;
    }

    private static int removeMainZone(CommandSourceStack source, String team) {
        ServerLevel level = source.m_81372_();
        AASWorldData data = AASWorldData.get(level);
        boolean removed = data.mainZones.removeIf(z -> z.team.equalsIgnoreCase(team));
        if (removed) {
            data.m_77762_();
            source.m_288197_(() -> Component.m_237113_((String)(team.toUpperCase() + " Main Protection Zone removed!")).m_130940_(ChatFormatting.GREEN), true);
        } else {
            source.m_81352_((Component)Component.m_237113_((String)("No protection zone found for team: " + team.toUpperCase())));
        }
        return 1;
    }

    private static int clearSquads(CommandSourceStack source, String teamName) {
        ServerLevel level = source.m_81372_();
        AASWorldData data = AASWorldData.get(level);
        String targetTeam = teamName.toUpperCase();
        for (ServerPlayer player : source.m_81377_().m_6846_().m_11314_()) {
            boolean belongsToTeam;
            if (!player.getPersistentData().m_128441_("AAS_SquadID") || !(belongsToTeam = data.squads.stream().anyMatch(s -> s.id == player.getPersistentData().m_128451_("AAS_SquadID") && s.team.equalsIgnoreCase(targetTeam)))) continue;
            player.getPersistentData().m_128473_("AAS_SquadID");
            player.getPersistentData().m_128473_("AAS_IsSquadLeader");
        }
        data.squads.removeIf(squad -> squad.team.equalsIgnoreCase(targetTeam));
        if (targetTeam.equals("BLUE")) {
            data.blueCMDId = -1;
        } else if (targetTeam.equals("RED")) {
            data.redCMDId = -1;
        }
        data.m_77762_();
        PacketHandler.sendToAllClients(level, data);
        source.m_288197_(() -> Component.m_237113_((String)("Cleared squads and CMD for " + targetTeam)).m_130940_(ChatFormatting.GREEN), true);
        return 1;
    }
}

