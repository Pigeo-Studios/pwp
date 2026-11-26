package com.pigeostudios.pwp.warfare.command;

import static com.pigeostudios.pwp.warfare.command.MapCommands.*;
import static com.pigeostudios.pwp.warfare.command.MatchCommands.*;
import static com.pigeostudios.pwp.warfare.command.ModerationCommands.*;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.pigeostudios.pwp.warfare.config.WarfareConfig;
import com.pigeostudios.pwp.warfare.data.state.WarfareData;
import com.pigeostudios.pwp.warfare.data.state.WarfareKeys;
import com.pigeostudios.pwp.warfare.events.DownedHandler;
import com.pigeostudios.pwp.warfare.network.PacketHandler;
import com.pigeostudios.pwp.warfare.network.PacketOpenSkinInventory;
import com.pigeostudios.pwp.warfare.network.PacketVehicleDriveAnswer;
import com.pigeostudios.pwp.warfare.network.PacketVehicleDriveRequest;
import com.pigeostudios.pwp.warfare.world.WarfareWorldData;
import com.pwp.api.time.GameClock;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.network.PacketDistributor;

public class ModCommands {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register((LiteralArgumentBuilder) ((LiteralArgumentBuilder) ((LiteralArgumentBuilder)
                                ((LiteralArgumentBuilder) ((LiteralArgumentBuilder) ((LiteralArgumentBuilder)
                                                                ((LiteralArgumentBuilder)
                                                                                ((LiteralArgumentBuilder)
                                                                                                ((LiteralArgumentBuilder)
                                                                                                                ((LiteralArgumentBuilder)
                                                                                                                                ((LiteralArgumentBuilder)
                                                                                                                                                ((LiteralArgumentBuilder)
                                                                                                                                                                ((LiteralArgumentBuilder)
                                                                                                                                                                                ((LiteralArgumentBuilder)
                                                                                                                                                                                                ((LiteralArgumentBuilder)
                                                                                                                                                                                                                ((LiteralArgumentBuilder)
                                                                                                                                                                                                                                ((LiteralArgumentBuilder)
                                                                                                                                                                                                                                                ((LiteralArgumentBuilder)
                                                                                                                                                                                                                                                                ((LiteralArgumentBuilder)
                                                                                                                                                                                                                                                                                ((LiteralArgumentBuilder)
                                                                                                                                                                                                                                                                                                Commands
                                                                                                                                                                                                                                                                                                        .literal(
                                                                                                                                                                                                                                                                                                                "pwpwarfare")
                                                                                                                                                                                                                                                                                                        .requires(
                                                                                                                                                                                                                                                                                                                s ->
                                                                                                                                                                                                                                                                                                                        s
                                                                                                                                                                                                                                                                                                                                .hasPermission(
                                                                                                                                                                                                                                                                                                                                        2)))
                                                                                                                                                                                                                                                                                        .then(
                                                                                                                                                                                                                                                                                                Commands
                                                                                                                                                                                                                                                                                                        .literal(
                                                                                                                                                                                                                                                                                                                "gamestart")
                                                                                                                                                                                                                                                                                                        .then(
                                                                                                                                                                                                                                                                                                                Commands
                                                                                                                                                                                                                                                                                                                        .argument(
                                                                                                                                                                                                                                                                                                                                "active",
                                                                                                                                                                                                                                                                                                                                BoolArgumentType
                                                                                                                                                                                                                                                                                                                                        .bool())
                                                                                                                                                                                                                                                                                                                        .then(
                                                                                                                                                                                                                                                                                                                                Commands
                                                                                                                                                                                                                                                                                                                                        .literal(
                                                                                                                                                                                                                                                                                                                                                "force")
                                                                                                                                                                                                                                                                                                                                        .executes(
                                                                                                                                                                                                                                                                                                                                                ctx ->
                                                                                                                                                                                                                                                                                                                                                        setGameStart(
                                                                                                                                                                                                                                                                                                                                                                (CommandSourceStack)
                                                                                                                                                                                                                                                                                                                                                                        ctx
                                                                                                                                                                                                                                                                                                                                                                                .getSource(),
                                                                                                                                                                                                                                                                                                                                                                BoolArgumentType
                                                                                                                                                                                                                                                                                                                                                                        .getBool(
                                                                                                                                                                                                                                                                                                                                                                                ctx,
                                                                                                                                                                                                                                                                                                                                                                                "active"),
                                                                                                                                                                                                                                                                                                                                                                true)))
                                                                                                                                                                                                                                                                                                                        .executes(
                                                                                                                                                                                                                                                                                                                                ctx ->
                                                                                                                                                                                                                                                                                                                                        setGameStart(
                                                                                                                                                                                                                                                                                                                                                (CommandSourceStack)
                                                                                                                                                                                                                                                                                                                                                        ctx
                                                                                                                                                                                                                                                                                                                                                                .getSource(),
                                                                                                                                                                                                                                                                                                                                                BoolArgumentType
                                                                                                                                                                                                                                                                                                                                                        .getBool(
                                                                                                                                                                                                                                                                                                                                                                ctx,
                                                                                                                                                                                                                                                                                                                                                                "active"))))))
                                                                                                                                                                                                                                                                        .then(
                                                                                                                                                                                                                                                                                Commands
                                                                                                                                                                                                                                                                                        .literal(
                                                                                                                                                                                                                                                                                                "gamemode")
                                                                                                                                                                                                                                                                                        .then(
                                                                                                                                                                                                                                                                                                Commands
                                                                                                                                                                                                                                                                                                        .argument(
                                                                                                                                                                                                                                                                                                                "mode",
                                                                                                                                                                                                                                                                                                                StringArgumentType
                                                                                                                                                                                                                                                                                                                        .word())
                                                                                                                                                                                                                                                                                                        .suggests(
                                                                                                                                                                                                                                                                                                                (ctx,
                                                                                                                                                                                                                                                                                                                        builder) ->
                                                                                                                                                                                                                                                                                                                        SharedSuggestionProvider
                                                                                                                                                                                                                                                                                                                                .suggest(
                                                                                                                                                                                                                                                                                                                                        List
                                                                                                                                                                                                                                                                                                                                                .of(
                                                                                                                                                                                                                                                                                                                                                        "aas",
                                                                                                                                                                                                                                                                                                                                                        "invasion"),
                                                                                                                                                                                                                                                                                                                                        builder))
                                                                                                                                                                                                                                                                                                        .executes(
                                                                                                                                                                                                                                                                                                                ctx ->
                                                                                                                                                                                                                                                                                                                        setGameMode(
                                                                                                                                                                                                                                                                                                                                (CommandSourceStack)
                                                                                                                                                                                                                                                                                                                                        ctx
                                                                                                                                                                                                                                                                                                                                                .getSource(),
                                                                                                                                                                                                                                                                                                                                StringArgumentType
                                                                                                                                                                                                                                                                                                                                        .getString(
                                                                                                                                                                                                                                                                                                                                                ctx,
                                                                                                                                                                                                                                                                                                                                                "mode"))))))
                                                                                                                                                                                                                                                        .then(
                                                                                                                                                                                                                                                                Commands
                                                                                                                                                                                                                                                                        .literal(
                                                                                                                                                                                                                                                                                "invasiondefend")
                                                                                                                                                                                                                                                                        .then(
                                                                                                                                                                                                                                                                                Commands
                                                                                                                                                                                                                                                                                        .argument(
                                                                                                                                                                                                                                                                                                "team",
                                                                                                                                                                                                                                                                                                StringArgumentType
                                                                                                                                                                                                                                                                                                        .word())
                                                                                                                                                                                                                                                                                        .suggests(
                                                                                                                                                                                                                                                                                                (ctx,
                                                                                                                                                                                                                                                                                                        builder) ->
                                                                                                                                                                                                                                                                                                        SharedSuggestionProvider
                                                                                                                                                                                                                                                                                                                .suggest(
                                                                                                                                                                                                                                                                                                                        List
                                                                                                                                                                                                                                                                                                                                .of(
                                                                                                                                                                                                                                                                                                                                        "blue",
                                                                                                                                                                                                                                                                                                                                        "red"),
                                                                                                                                                                                                                                                                                                                        builder))
                                                                                                                                                                                                                                                                                        .executes(
                                                                                                                                                                                                                                                                                                ctx ->
                                                                                                                                                                                                                                                                                                        setInvasionDefender(
                                                                                                                                                                                                                                                                                                                (CommandSourceStack)
                                                                                                                                                                                                                                                                                                                        ctx
                                                                                                                                                                                                                                                                                                                                .getSource(),
                                                                                                                                                                                                                                                                                                                StringArgumentType
                                                                                                                                                                                                                                                                                                                        .getString(
                                                                                                                                                                                                                                                                                                                                ctx,
                                                                                                                                                                                                                                                                                                                                "team"))))))
                                                                                                                                                                                                                                        .then(
                                                                                                                                                                                                                                                Commands
                                                                                                                                                                                                                                                        .literal(
                                                                                                                                                                                                                                                                "deathtimer")
                                                                                                                                                                                                                                                        .then(
                                                                                                                                                                                                                                                                Commands
                                                                                                                                                                                                                                                                        .argument(
                                                                                                                                                                                                                                                                                "seconds",
                                                                                                                                                                                                                                                                                IntegerArgumentType
                                                                                                                                                                                                                                                                                        .integer(
                                                                                                                                                                                                                                                                                                0))
                                                                                                                                                                                                                                                                        .executes(
                                                                                                                                                                                                                                                                                ctx ->
                                                                                                                                                                                                                                                                                        setRespawnTime(
                                                                                                                                                                                                                                                                                                (CommandSourceStack)
                                                                                                                                                                                                                                                                                                        ctx
                                                                                                                                                                                                                                                                                                                .getSource(),
                                                                                                                                                                                                                                                                                                IntegerArgumentType
                                                                                                                                                                                                                                                                                                        .getInteger(
                                                                                                                                                                                                                                                                                                                ctx,
                                                                                                                                                                                                                                                                                                                "seconds"))))))
                                                                                                                                                                                                                        .then(
                                                                                                                                                                                                                                Commands
                                                                                                                                                                                                                                        .literal(
                                                                                                                                                                                                                                                "deathtickets")
                                                                                                                                                                                                                                        .then(
                                                                                                                                                                                                                                                Commands
                                                                                                                                                                                                                                                        .argument(
                                                                                                                                                                                                                                                                "amount",
                                                                                                                                                                                                                                                                IntegerArgumentType
                                                                                                                                                                                                                                                                        .integer(
                                                                                                                                                                                                                                                                                0))
                                                                                                                                                                                                                                                        .executes(
                                                                                                                                                                                                                                                                ctx ->
                                                                                                                                                                                                                                                                        setDeathTickets(
                                                                                                                                                                                                                                                                                (CommandSourceStack)
                                                                                                                                                                                                                                                                                        ctx
                                                                                                                                                                                                                                                                                                .getSource(),
                                                                                                                                                                                                                                                                                IntegerArgumentType
                                                                                                                                                                                                                                                                                        .getInteger(
                                                                                                                                                                                                                                                                                                ctx,
                                                                                                                                                                                                                                                                                                "amount"))))))
                                                                                                                                                                                                        .then(
                                                                                                                                                                                                                Commands
                                                                                                                                                                                                                        .literal(
                                                                                                                                                                                                                                "clearsquad")
                                                                                                                                                                                                                        .then(
                                                                                                                                                                                                                                Commands
                                                                                                                                                                                                                                        .argument(
                                                                                                                                                                                                                                                "team",
                                                                                                                                                                                                                                                StringArgumentType
                                                                                                                                                                                                                                                        .word())
                                                                                                                                                                                                                                        .suggests(
                                                                                                                                                                                                                                                (ctx,
                                                                                                                                                                                                                                                        builder) ->
                                                                                                                                                                                                                                                        SharedSuggestionProvider
                                                                                                                                                                                                                                                                .suggest(
                                                                                                                                                                                                                                                                        List
                                                                                                                                                                                                                                                                                .of(
                                                                                                                                                                                                                                                                                        "blue",
                                                                                                                                                                                                                                                                                        "red"),
                                                                                                                                                                                                                                                                        builder))
                                                                                                                                                                                                                                        .executes(
                                                                                                                                                                                                                                                ctx ->
                                                                                                                                                                                                                                                        clearSquads(
                                                                                                                                                                                                                                                                (CommandSourceStack)
                                                                                                                                                                                                                                                                        ctx
                                                                                                                                                                                                                                                                                .getSource(),
                                                                                                                                                                                                                                                                StringArgumentType
                                                                                                                                                                                                                                                                        .getString(
                                                                                                                                                                                                                                                                                ctx,
                                                                                                                                                                                                                                                                                "team"))))))
                                                                                                                                                                                        .then(
                                                                                                                                                                                                Commands
                                                                                                                                                                                                        .literal(
                                                                                                                                                                                                                "teamtickets")
                                                                                                                                                                                                        .then(
                                                                                                                                                                                                                Commands
                                                                                                                                                                                                                        .argument(
                                                                                                                                                                                                                                "team",
                                                                                                                                                                                                                                StringArgumentType
                                                                                                                                                                                                                                        .word())
                                                                                                                                                                                                                        .suggests(
                                                                                                                                                                                                                                (ctx,
                                                                                                                                                                                                                                        builder) ->
                                                                                                                                                                                                                                        SharedSuggestionProvider
                                                                                                                                                                                                                                                .suggest(
                                                                                                                                                                                                                                                        List
                                                                                                                                                                                                                                                                .of(
                                                                                                                                                                                                                                                                        "blue",
                                                                                                                                                                                                                                                                        "red"),
                                                                                                                                                                                                                                                        builder))
                                                                                                                                                                                                                        .then(
                                                                                                                                                                                                                                Commands
                                                                                                                                                                                                                                        .argument(
                                                                                                                                                                                                                                                "amount",
                                                                                                                                                                                                                                                IntegerArgumentType
                                                                                                                                                                                                                                                        .integer())
                                                                                                                                                                                                                                        .executes(
                                                                                                                                                                                                                                                ctx ->
                                                                                                                                                                                                                                                        setTickets(
                                                                                                                                                                                                                                                                (CommandSourceStack)
                                                                                                                                                                                                                                                                        ctx
                                                                                                                                                                                                                                                                                .getSource(),
                                                                                                                                                                                                                                                                StringArgumentType
                                                                                                                                                                                                                                                                        .getString(
                                                                                                                                                                                                                                                                                ctx,
                                                                                                                                                                                                                                                                                "team"),
                                                                                                                                                                                                                                                                IntegerArgumentType
                                                                                                                                                                                                                                                                        .getInteger(
                                                                                                                                                                                                                                                                                ctx,
                                                                                                                                                                                                                                                                                "amount")))))))
                                                                                                                                                                        .then(
                                                                                                                                                                                Commands
                                                                                                                                                                                        .literal(
                                                                                                                                                                                                "mainzone")
                                                                                                                                                                                        .then(
                                                                                                                                                                                                Commands
                                                                                                                                                                                                        .argument(
                                                                                                                                                                                                                "team",
                                                                                                                                                                                                                StringArgumentType
                                                                                                                                                                                                                        .word())
                                                                                                                                                                                                        .suggests(
                                                                                                                                                                                                                (ctx,
                                                                                                                                                                                                                        builder) ->
                                                                                                                                                                                                                        SharedSuggestionProvider
                                                                                                                                                                                                                                .suggest(
                                                                                                                                                                                                                                        List
                                                                                                                                                                                                                                                .of(
                                                                                                                                                                                                                                                        "blue",
                                                                                                                                                                                                                                                        "red"),
                                                                                                                                                                                                                                        builder))
                                                                                                                                                                                                        .then(
                                                                                                                                                                                                                Commands
                                                                                                                                                                                                                        .argument(
                                                                                                                                                                                                                                "shape",
                                                                                                                                                                                                                                StringArgumentType
                                                                                                                                                                                                                                        .word())
                                                                                                                                                                                                                        .suggests(
                                                                                                                                                                                                                                (ctx,
                                                                                                                                                                                                                                        builder) ->
                                                                                                                                                                                                                                        SharedSuggestionProvider
                                                                                                                                                                                                                                                .suggest(
                                                                                                                                                                                                                                                        List
                                                                                                                                                                                                                                                                .of(
                                                                                                                                                                                                                                                                        "cube",
                                                                                                                                                                                                                                                                        "cylinder"),
                                                                                                                                                                                                                                                        builder))
                                                                                                                                                                                                                        .then(
                                                                                                                                                                                                                                Commands
                                                                                                                                                                                                                                        .argument(
                                                                                                                                                                                                                                                "pos1",
                                                                                                                                                                                                                                                BlockPosArgument
                                                                                                                                                                                                                                                        .blockPos())
                                                                                                                                                                                                                                        .then(
                                                                                                                                                                                                                                                Commands
                                                                                                                                                                                                                                                        .argument(
                                                                                                                                                                                                                                                                "pos2",
                                                                                                                                                                                                                                                                BlockPosArgument
                                                                                                                                                                                                                                                                        .blockPos())
                                                                                                                                                                                                                                                        .executes(
                                                                                                                                                                                                                                                                ctx ->
                                                                                                                                                                                                                                                                        addMainZone(
                                                                                                                                                                                                                                                                                (CommandSourceStack)
                                                                                                                                                                                                                                                                                        ctx
                                                                                                                                                                                                                                                                                                .getSource(),
                                                                                                                                                                                                                                                                                StringArgumentType
                                                                                                                                                                                                                                                                                        .getString(
                                                                                                                                                                                                                                                                                                ctx,
                                                                                                                                                                                                                                                                                                "team"),
                                                                                                                                                                                                                                                                                StringArgumentType
                                                                                                                                                                                                                                                                                        .getString(
                                                                                                                                                                                                                                                                                                ctx,
                                                                                                                                                                                                                                                                                                "shape"),
                                                                                                                                                                                                                                                                                BlockPosArgument
                                                                                                                                                                                                                                                                                        .getSpawnablePos(
                                                                                                                                                                                                                                                                                                ctx,
                                                                                                                                                                                                                                                                                                "pos1"),
                                                                                                                                                                                                                                                                                BlockPosArgument
                                                                                                                                                                                                                                                                                        .getSpawnablePos(
                                                                                                                                                                                                                                                                                                ctx,
                                                                                                                                                                                                                                                                                                "pos2")))))))))
                                                                                                                                                        .then(
                                                                                                                                                                Commands
                                                                                                                                                                        .literal(
                                                                                                                                                                                "removemainzone")
                                                                                                                                                                        .then(
                                                                                                                                                                                Commands
                                                                                                                                                                                        .argument(
                                                                                                                                                                                                "team",
                                                                                                                                                                                                StringArgumentType
                                                                                                                                                                                                        .word())
                                                                                                                                                                                        .suggests(
                                                                                                                                                                                                (ctx,
                                                                                                                                                                                                        builder) ->
                                                                                                                                                                                                        SharedSuggestionProvider
                                                                                                                                                                                                                .suggest(
                                                                                                                                                                                                                        List
                                                                                                                                                                                                                                .of(
                                                                                                                                                                                                                                        "blue",
                                                                                                                                                                                                                                        "red"),
                                                                                                                                                                                                                        builder))
                                                                                                                                                                                        .executes(
                                                                                                                                                                                                ctx ->
                                                                                                                                                                                                        removeMainZone(
                                                                                                                                                                                                                (CommandSourceStack)
                                                                                                                                                                                                                        ctx
                                                                                                                                                                                                                                .getSource(),
                                                                                                                                                                                                                StringArgumentType
                                                                                                                                                                                                                        .getString(
                                                                                                                                                                                                                                ctx,
                                                                                                                                                                                                                                "team"))))))
                                                                                                                                        .then(
                                                                                                                                                Commands
                                                                                                                                                        .literal(
                                                                                                                                                                "teamjoin")
                                                                                                                                                        .then(
                                                                                                                                                                Commands
                                                                                                                                                                        .argument(
                                                                                                                                                                                "team",
                                                                                                                                                                                StringArgumentType
                                                                                                                                                                                        .word())
                                                                                                                                                                        .suggests(
                                                                                                                                                                                (ctx,
                                                                                                                                                                                        builder) ->
                                                                                                                                                                                        SharedSuggestionProvider
                                                                                                                                                                                                .suggest(
                                                                                                                                                                                                        List
                                                                                                                                                                                                                .of(
                                                                                                                                                                                                                        "blue",
                                                                                                                                                                                                                        "red"),
                                                                                                                                                                                                        builder))
                                                                                                                                                                        .then(
                                                                                                                                                                                Commands
                                                                                                                                                                                        .argument(
                                                                                                                                                                                                "player",
                                                                                                                                                                                                EntityArgument
                                                                                                                                                                                                        .player())
                                                                                                                                                                                        .executes(
                                                                                                                                                                                                ctx ->
                                                                                                                                                                                                        joinTeam(
                                                                                                                                                                                                                (CommandSourceStack)
                                                                                                                                                                                                                        ctx
                                                                                                                                                                                                                                .getSource(),
                                                                                                                                                                                                                StringArgumentType
                                                                                                                                                                                                                        .getString(
                                                                                                                                                                                                                                ctx,
                                                                                                                                                                                                                                "team"),
                                                                                                                                                                                                                EntityArgument
                                                                                                                                                                                                                        .getPlayer(
                                                                                                                                                                                                                                ctx,
                                                                                                                                                                                                                                "player")))))))
                                                                                                                        .then(
                                                                                                                                Commands
                                                                                                                                        .literal(
                                                                                                                                                "addpoint")
                                                                                                                                        .then(
                                                                                                                                                Commands
                                                                                                                                                        .argument(
                                                                                                                                                                "shape",
                                                                                                                                                                StringArgumentType
                                                                                                                                                                        .word())
                                                                                                                                                        .suggests(
                                                                                                                                                                (ctx,
                                                                                                                                                                        builder) ->
                                                                                                                                                                        SharedSuggestionProvider
                                                                                                                                                                                .suggest(
                                                                                                                                                                                        List
                                                                                                                                                                                                .of(
                                                                                                                                                                                                        "cube",
                                                                                                                                                                                                        "cylinder"),
                                                                                                                                                                                        builder))
                                                                                                                                                        .then(
                                                                                                                                                                Commands
                                                                                                                                                                        .argument(
                                                                                                                                                                                "pos1",
                                                                                                                                                                                BlockPosArgument
                                                                                                                                                                                        .blockPos())
                                                                                                                                                                        .then(
                                                                                                                                                                                Commands
                                                                                                                                                                                        .argument(
                                                                                                                                                                                                "pos2",
                                                                                                                                                                                                BlockPosArgument
                                                                                                                                                                                                        .blockPos())
                                                                                                                                                                                        .then(
                                                                                                                                                                                                Commands
                                                                                                                                                                                                        .argument(
                                                                                                                                                                                                                "name",
                                                                                                                                                                                                                StringArgumentType
                                                                                                                                                                                                                        .string())
                                                                                                                                                                                                        .then(
                                                                                                                                                                                                                Commands
                                                                                                                                                                                                                        .argument(
                                                                                                                                                                                                                                "bluePriority",
                                                                                                                                                                                                                                IntegerArgumentType
                                                                                                                                                                                                                                        .integer(
                                                                                                                                                                                                                                                0,
                                                                                                                                                                                                                                                999))
                                                                                                                                                                                                                        .then(
                                                                                                                                                                                                                                Commands
                                                                                                                                                                                                                                        .argument(
                                                                                                                                                                                                                                                "redPriority",
                                                                                                                                                                                                                                                IntegerArgumentType
                                                                                                                                                                                                                                                        .integer(
                                                                                                                                                                                                                                                                0,
                                                                                                                                                                                                                                                                999))
                                                                                                                                                                                                                                        .then(
                                                                                                                                                                                                                                                Commands
                                                                                                                                                                                                                                                        .argument(
                                                                                                                                                                                                                                                                "timeMin",
                                                                                                                                                                                                                                                                IntegerArgumentType
                                                                                                                                                                                                                                                                        .integer(
                                                                                                                                                                                                                                                                                1))
                                                                                                                                                                                                                                                        .then(
                                                                                                                                                                                                                                                                Commands
                                                                                                                                                                                                                                                                        .argument(
                                                                                                                                                                                                                                                                                "penalty",
                                                                                                                                                                                                                                                                                IntegerArgumentType
                                                                                                                                                                                                                                                                                        .integer(
                                                                                                                                                                                                                                                                                                0))
                                                                                                                                                                                                                                                                        .then(
                                                                                                                                                                                                                                                                                Commands
                                                                                                                                                                                                                                                                                        .argument(
                                                                                                                                                                                                                                                                                                "captureDeduct",
                                                                                                                                                                                                                                                                                                IntegerArgumentType
                                                                                                                                                                                                                                                                                                        .integer(
                                                                                                                                                                                                                                                                                                                0))
                                                                                                                                                                                                                                                                                        .then(
                                                                                                                                                                                                                                                                                                Commands
                                                                                                                                                                                                                                                                                                        .argument(
                                                                                                                                                                                                                                                                                                                "lockMinutes",
                                                                                                                                                                                                                                                                                                                IntegerArgumentType
                                                                                                                                                                                                                                                                                                                        .integer(
                                                                                                                                                                                                                                                                                                                                0))
                                                                                                                                                                                                                                                                                                        .then(
                                                                                                                                                                                                                                                                                                                Commands
                                                                                                                                                                                                                                                                                                                        .argument(
                                                                                                                                                                                                                                                                                                                                "gainNeutralize",
                                                                                                                                                                                                                                                                                                                                IntegerArgumentType
                                                                                                                                                                                                                                                                                                                                        .integer(
                                                                                                                                                                                                                                                                                                                                                0))
                                                                                                                                                                                                                                                                                                                        .then(
                                                                                                                                                                                                                                                                                                                                Commands
                                                                                                                                                                                                                                                                                                                                        .argument(
                                                                                                                                                                                                                                                                                                                                                "gainCapture",
                                                                                                                                                                                                                                                                                                                                                IntegerArgumentType
                                                                                                                                                                                                                                                                                                                                                        .integer(
                                                                                                                                                                                                                                                                                                                                                                0))
                                                                                                                                                                                                                                                                                                                                        .executes(
                                                                                                                                                                                                                                                                                                                                                ctx ->
                                                                                                                                                                                                                                                                                                                                                        addPoint(
                                                                                                                                                                                                                                                                                                                                                                (CommandSourceStack)
                                                                                                                                                                                                                                                                                                                                                                        ctx
                                                                                                                                                                                                                                                                                                                                                                                .getSource(),
                                                                                                                                                                                                                                                                                                                                                                StringArgumentType
                                                                                                                                                                                                                                                                                                                                                                        .getString(
                                                                                                                                                                                                                                                                                                                                                                                ctx,
                                                                                                                                                                                                                                                                                                                                                                                "shape"),
                                                                                                                                                                                                                                                                                                                                                                BlockPosArgument
                                                                                                                                                                                                                                                                                                                                                                        .getSpawnablePos(
                                                                                                                                                                                                                                                                                                                                                                                ctx,
                                                                                                                                                                                                                                                                                                                                                                                "pos1"),
                                                                                                                                                                                                                                                                                                                                                                BlockPosArgument
                                                                                                                                                                                                                                                                                                                                                                        .getSpawnablePos(
                                                                                                                                                                                                                                                                                                                                                                                ctx,
                                                                                                                                                                                                                                                                                                                                                                                "pos2"),
                                                                                                                                                                                                                                                                                                                                                                StringArgumentType
                                                                                                                                                                                                                                                                                                                                                                        .getString(
                                                                                                                                                                                                                                                                                                                                                                                ctx,
                                                                                                                                                                                                                                                                                                                                                                                "name"),
                                                                                                                                                                                                                                                                                                                                                                IntegerArgumentType
                                                                                                                                                                                                                                                                                                                                                                        .getInteger(
                                                                                                                                                                                                                                                                                                                                                                                ctx,
                                                                                                                                                                                                                                                                                                                                                                                "bluePriority"),
                                                                                                                                                                                                                                                                                                                                                                IntegerArgumentType
                                                                                                                                                                                                                                                                                                                                                                        .getInteger(
                                                                                                                                                                                                                                                                                                                                                                                ctx,
                                                                                                                                                                                                                                                                                                                                                                                "redPriority"),
                                                                                                                                                                                                                                                                                                                                                                IntegerArgumentType
                                                                                                                                                                                                                                                                                                                                                                        .getInteger(
                                                                                                                                                                                                                                                                                                                                                                                ctx,
                                                                                                                                                                                                                                                                                                                                                                                "timeMin"),
                                                                                                                                                                                                                                                                                                                                                                IntegerArgumentType
                                                                                                                                                                                                                                                                                                                                                                        .getInteger(
                                                                                                                                                                                                                                                                                                                                                                                ctx,
                                                                                                                                                                                                                                                                                                                                                                                "penalty"),
                                                                                                                                                                                                                                                                                                                                                                IntegerArgumentType
                                                                                                                                                                                                                                                                                                                                                                        .getInteger(
                                                                                                                                                                                                                                                                                                                                                                                ctx,
                                                                                                                                                                                                                                                                                                                                                                                "captureDeduct"),
                                                                                                                                                                                                                                                                                                                                                                IntegerArgumentType
                                                                                                                                                                                                                                                                                                                                                                        .getInteger(
                                                                                                                                                                                                                                                                                                                                                                                ctx,
                                                                                                                                                                                                                                                                                                                                                                                "lockMinutes"),
                                                                                                                                                                                                                                                                                                                                                                IntegerArgumentType
                                                                                                                                                                                                                                                                                                                                                                        .getInteger(
                                                                                                                                                                                                                                                                                                                                                                                ctx,
                                                                                                                                                                                                                                                                                                                                                                                "gainNeutralize"),
                                                                                                                                                                                                                                                                                                                                                                IntegerArgumentType
                                                                                                                                                                                                                                                                                                                                                                        .getInteger(
                                                                                                                                                                                                                                                                                                                                                                                ctx,
                                                                                                                                                                                                                                                                                                                                                                                "gainCapture")))))))))))))))))
                                                                                                        .then(
                                                                                                                Commands
                                                                                                                        .literal(
                                                                                                                                "warn")
                                                                                                                        .then(
                                                                                                                                Commands
                                                                                                                                        .argument(
                                                                                                                                                "target",
                                                                                                                                                EntityArgument
                                                                                                                                                        .player())
                                                                                                                                        .then(
                                                                                                                                                Commands
                                                                                                                                                        .argument(
                                                                                                                                                                "message",
                                                                                                                                                                StringArgumentType
                                                                                                                                                                        .greedyString())
                                                                                                                                                        .executes(
                                                                                                                                                                ctx ->
                                                                                                                                                                        issueWarning(
                                                                                                                                                                                (CommandSourceStack)
                                                                                                                                                                                        ctx
                                                                                                                                                                                                .getSource(),
                                                                                                                                                                                EntityArgument
                                                                                                                                                                                        .getPlayer(
                                                                                                                                                                                                ctx,
                                                                                                                                                                                                "target"),
                                                                                                                                                                                StringArgumentType
                                                                                                                                                                                        .getString(
                                                                                                                                                                                                ctx,
                                                                                                                                                                                                "message")))))))
                                                                                        .then(
                                                                                                Commands.literal(
                                                                                                                "removepoint")
                                                                                                        .then(
                                                                                                                Commands
                                                                                                                        .argument(
                                                                                                                                "name",
                                                                                                                                StringArgumentType
                                                                                                                                        .greedyString())
                                                                                                                        .suggests(
                                                                                                                                (ctx,
                                                                                                                                        builder) ->
                                                                                                                                        suggestLocalPoints(
                                                                                                                                                ctx,
                                                                                                                                                builder))
                                                                                                                        .executes(
                                                                                                                                ctx ->
                                                                                                                                        removePoint(
                                                                                                                                                (CommandSourceStack)
                                                                                                                                                        ctx
                                                                                                                                                                .getSource(),
                                                                                                                                                StringArgumentType
                                                                                                                                                        .getString(
                                                                                                                                                                ctx,
                                                                                                                                                                "name"))))))
                                                                        .then(
                                                                                Commands.literal("pointclear")
                                                                                        .then(
                                                                                                Commands.argument(
                                                                                                                "name",
                                                                                                                StringArgumentType
                                                                                                                        .greedyString())
                                                                                                        .suggests(
                                                                                                                (ctx,
                                                                                                                        builder) ->
                                                                                                                        suggestLocalPoints(
                                                                                                                                ctx,
                                                                                                                                builder))
                                                                                                        .executes(
                                                                                                                ctx ->
                                                                                                                        clearSpecificPoint(
                                                                                                                                (CommandSourceStack)
                                                                                                                                        ctx
                                                                                                                                                .getSource(),
                                                                                                                                StringArgumentType
                                                                                                                                        .getString(
                                                                                                                                                ctx,
                                                                                                                                                "name"))))))
                                                        .then(Commands.literal("pointcapture")
                                                                .then(Commands.argument(
                                                                                "team", StringArgumentType.word())
                                                                        .suggests((ctx, builder) ->
                                                                                SharedSuggestionProvider.suggest(
                                                                                        List.of("blue", "red"),
                                                                                        builder))
                                                                        .then(Commands.argument(
                                                                                        "name",
                                                                                        StringArgumentType
                                                                                                .greedyString())
                                                                                .suggests((ctx, builder) ->
                                                                                        suggestLocalPoints(
                                                                                                ctx, builder))
                                                                                .executes(ctx -> forceCapturePoint(
                                                                                        (CommandSourceStack)
                                                                                                ctx.getSource(),
                                                                                        StringArgumentType.getString(
                                                                                                ctx, "team"),
                                                                                        StringArgumentType.getString(
                                                                                                ctx, "name")))))))
                                                .then(Commands.literal("teamspawn")
                                                        .then(Commands.argument("team", StringArgumentType.word())
                                                                .suggests((ctx, builder) ->
                                                                        SharedSuggestionProvider.suggest(
                                                                                List.of("blue", "red", "none"),
                                                                                builder))
                                                                .then(Commands.argument(
                                                                                "pos", BlockPosArgument.blockPos())
                                                                        .executes(ctx -> setTeamSpawn(
                                                                                (CommandSourceStack) ctx.getSource(),
                                                                                StringArgumentType.getString(
                                                                                        ctx, "team"),
                                                                                BlockPosArgument.getSpawnablePos(
                                                                                        ctx, "pos")))))))
                                        .then(((LiteralArgumentBuilder) ((LiteralArgumentBuilder) Commands.literal(
                                                                        "map")
                                                                .then(Commands.literal("setimage")
                                                                        .then(Commands.argument(
                                                                                        "imagename",
                                                                                        StringArgumentType.word())
                                                                                .suggests((ctx, builder) -> {
                                                                                    var level = ((CommandSourceStack)
                                                                                                    ctx.getSource())
                                                                                            .getLevel();
                                                                                    var data =
                                                                                            WarfareWorldData.get(level);
                                                                                    List<String> images =
                                                                                            new ArrayList<>(
                                                                                                    data
                                                                                                            .getAvailableMapImages());
                                                                                    if (images.isEmpty())
                                                                                        images.add(
                                                                                                data.match
                                                                                                        .currentMapImage);
                                                                                    return SharedSuggestionProvider
                                                                                            .suggest(images, builder);
                                                                                })
                                                                                .executes(ctx -> {
                                                                                    String imgName =
                                                                                            StringArgumentType
                                                                                                    .getString(
                                                                                                            ctx,
                                                                                                            "imagename");
                                                                                    ServerLevel level =
                                                                                            ((CommandSourceStack)
                                                                                                            ctx
                                                                                                                    .getSource())
                                                                                                    .getLevel();
                                                                                    WarfareWorldData data =
                                                                                            WarfareWorldData.get(level);
                                                                                    data.match.currentMapImage =
                                                                                            imgName;
                                                                                    data.setDirty();
                                                                                    PacketHandler.sendToAllClients(
                                                                                            level, data);
                                                                                    ((CommandSourceStack)
                                                                                                    ctx.getSource())
                                                                                            .sendSuccess(
                                                                                                    () ->
                                                                                                            Component
                                                                                                                    .literal(
                                                                                                                            "Map image set to: "
                                                                                                                                    + imgName
                                                                                                                                    + ".png"),
                                                                                                    true);
                                                                                    return 1;
                                                                                }))))
                                                        .then(Commands.literal("setcenter")
                                                                .then(Commands.argument(
                                                                                "x", IntegerArgumentType.integer())
                                                                        .then(Commands.argument(
                                                                                        "z",
                                                                                        IntegerArgumentType.integer())
                                                                                .executes(ctx -> {
                                                                                    int x =
                                                                                            IntegerArgumentType
                                                                                                    .getInteger(
                                                                                                            ctx, "x");
                                                                                    int z =
                                                                                            IntegerArgumentType
                                                                                                    .getInteger(
                                                                                                            ctx, "z");
                                                                                    ServerLevel level =
                                                                                            ((CommandSourceStack)
                                                                                                            ctx
                                                                                                                    .getSource())
                                                                                                    .getLevel();
                                                                                    WarfareWorldData data =
                                                                                            WarfareWorldData.get(level);
                                                                                    data.match.mapCenterX = x;
                                                                                    data.match.mapCenterZ = z;
                                                                                    data.setDirty();
                                                                                    PacketHandler.sendToAllClients(
                                                                                            level, data);
                                                                                    ((CommandSourceStack)
                                                                                                    ctx.getSource())
                                                                                            .sendSuccess(
                                                                                                    () ->
                                                                                                            Component
                                                                                                                    .literal(
                                                                                                                            "Map center manually set to X: "
                                                                                                                                    + x
                                                                                                                                    + ", Z: "
                                                                                                                                    + z),
                                                                                                    true);
                                                                                    return 1;
                                                                                })))))
                                                .then(Commands.literal("setsize")
                                                        .then(Commands.argument(
                                                                        "blocks", IntegerArgumentType.integer(128))
                                                                .executes(ctx -> {
                                                                    int size = IntegerArgumentType.getInteger(
                                                                            ctx, "blocks");
                                                                    WarfareWorldData data = WarfareWorldData.get(
                                                                            ((CommandSourceStack) ctx.getSource())
                                                                                    .getLevel());
                                                                    data.match.mapSizeBlocks = size;
                                                                    data.setDirty();
                                                                    PacketHandler.sendToAllClients(
                                                                            ((CommandSourceStack) ctx.getSource())
                                                                                    .getLevel(),
                                                                            data);
                                                                    ((CommandSourceStack) ctx.getSource())
                                                                            .sendSuccess(
                                                                                    () -> Component.translatable(
                                                                                            "pwpwarfare.msg.admin_cmd.map_size",
                                                                                            size),
                                                                                    true);
                                                                    return 1;
                                                                })))))
                        .then(Commands.literal("fraction")
                                .then(Commands.argument("team", StringArgumentType.word())
                                        .suggests((ctx, builder) ->
                                                SharedSuggestionProvider.suggest(List.of("blue", "red"), builder))
                                        .then(Commands.argument("faction", StringArgumentType.word())
                                                .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(
                                                        List.of(
                                                                "ukraine",
                                                                "russia",
                                                                "usa",
                                                                "nato",
                                                                "bluefor",
                                                                "redfor",
                                                                "insurgency",
                                                                "pmc",
                                                                "clear"),
                                                        builder))
                                                .executes(ctx -> setFaction(
                                                        (CommandSourceStack) ctx.getSource(),
                                                        StringArgumentType.getString(ctx, "team"),
                                                        StringArgumentType.getString(ctx, "faction")))))))
                .then(Commands.literal("cleartenkill")
                        .then(Commands.argument("target", EntityArgument.player())
                                .executes(ctx -> clearTeamkill(
                                        (CommandSourceStack) ctx.getSource(),
                                        EntityArgument.getPlayer(ctx, "target")))))
                .then(Commands.literal("votestart")
                        .then(Commands.argument("active", BoolArgumentType.bool())
                                .executes(ctx -> {
                                    ServerLevel level = ((CommandSourceStack) ctx.getSource()).getLevel();
                                    WarfareWorldData data = WarfareWorldData.get(level);
                                    boolean active = BoolArgumentType.getBool(ctx, "active");
                                    data.match.voteActive = active;
                                    if (active) {
                                        data.match.voteTimer = (Integer) WarfareConfig.VOTE_AUTO_START_TIME.get() * 60;
                                        data.match.votes.clear();
                                        data.match.blueReady = false;
                                        data.match.redReady = false;
                                    }

                                    data.setDirty();
                                    PacketHandler.sendToAllClients(level, data);
                                    String status = active
                                            ? "pwpwarfare.msg.admin_cmd.vote_started"
                                            : "pwpwarfare.msg.admin_cmd.vote_stopped";
                                    ((CommandSourceStack) ctx.getSource())
                                            .sendSuccess(() -> Component.translatable(status), true);
                                    return 1;
                                })))
                .then(Commands.literal("voicemute")
                        .then(Commands.argument("target", EntityArgument.player())
                                .executes(ctx -> voiceMutePlayer(
                                        (CommandSourceStack) ctx.getSource(),
                                        EntityArgument.getPlayer(ctx, "target"),
                                        0,
                                        ""))
                                .then(Commands.argument("minutes", IntegerArgumentType.integer(1))
                                        .executes(ctx -> voiceMutePlayer(
                                                (CommandSourceStack) ctx.getSource(),
                                                EntityArgument.getPlayer(ctx, "target"),
                                                IntegerArgumentType.getInteger(ctx, "minutes"),
                                                ""))
                                        .then(Commands.argument("reason", StringArgumentType.greedyString())
                                                .executes(ctx -> voiceMutePlayer(
                                                        (CommandSourceStack) ctx.getSource(),
                                                        EntityArgument.getPlayer(ctx, "target"),
                                                        IntegerArgumentType.getInteger(ctx, "minutes"),
                                                        StringArgumentType.getString(ctx, "reason"))))))
                        .then(Commands.literal("list")
                                .executes(ctx -> voiceMuteList((CommandSourceStack) ctx.getSource()))))
                .then(Commands.literal("pause").executes(ctx -> togglePause((CommandSourceStack) ctx.getSource())))
                .then(Commands.literal("voiceunmute")
                        .then(Commands.argument("target", EntityArgument.player())
                                .executes(ctx -> voiceUnmutePlayer(
                                        (CommandSourceStack) ctx.getSource(),
                                        EntityArgument.getPlayer(ctx, "target"))))));

        dispatcher.register(Commands.literal("pwpwarfare")
                .requires(s -> s.hasPermission(2))
                .then(Commands.literal("makecmd")
                        .then(Commands.argument("squadId", IntegerArgumentType.integer())
                                .executes(ctx -> forceSetCMD(
                                        (CommandSourceStack) ctx.getSource(),
                                        IntegerArgumentType.getInteger(ctx, "squadId"))))));

        dispatcher.register(Commands.literal("pwp")
                .then(Commands.literal("inv").executes(ctx -> {
                    ServerPlayer player = ((CommandSourceStack) ctx.getSource()).getPlayerOrException();
                    PacketHandler.INSTANCE.send(
                            PacketDistributor.PLAYER.with(() -> player), new PacketOpenSkinInventory());
                    return 1;
                }))
                .then(Commands.literal("respawn").executes(ctx -> {
                    ServerPlayer player = ((CommandSourceStack) ctx.getSource()).getPlayerOrException();
                    ServerLevel level = player.serverLevel();
                    WarfareWorldData data = WarfareWorldData.get(level);

                    if (!data.match.isGameStarted) {
                        player.sendSystemMessage(Component.translatable("pwpwarfare.msg.admin_cmd.game_inactive")
                                .withStyle(ChatFormatting.RED));
                        return 0;
                    }

                    String team = player.getTeam() != null ? player.getTeam().getName() : "";
                    if (!team.equalsIgnoreCase("Blue") && !team.equalsIgnoreCase("Red")) {
                        player.sendSystemMessage(Component.translatable("pwpwarfare.msg.admin_cmd.need_team")
                                .withStyle(ChatFormatting.RED));
                        return 0;
                    }

                    long lastRespawn = WarfareData.of(player).getLong(WarfareKeys.LAST_RESPAWN_COMMAND);
                    int cooldownTicks = (Integer) WarfareConfig.RESPAWN_COMMAND_COOLDOWN_SECONDS.get() * 20;
                    long currentTick = GameClock.worldTicks(level);
                    long elapsed = currentTick - lastRespawn;

                    if (elapsed < cooldownTicks && !player.isCreative()) {
                        long remaining = (cooldownTicks - elapsed) / 20;
                        player.sendSystemMessage(
                                Component.translatable("pwpwarfare.msg.admin_cmd.respawn_cooldown", remaining)
                                        .withStyle(ChatFormatting.RED));
                        return 0;
                    }

                    WarfareData.of(player).putLong(WarfareKeys.LAST_RESPAWN_COMMAND, currentTick);

                    if (WarfareData.of(player).getBoolean(WarfareKeys.IS_DOWNED)) {
                        DownedHandler.forceGiveUp(player);
                    } else {
                        player.kill();
                    }

                    return 1;
                })));

        dispatcher.register(Commands.literal("ready").executes(ctx -> {
            ServerPlayer player = ((CommandSourceStack) ctx.getSource()).getPlayerOrException();
            ServerLevel level = player.serverLevel();
            WarfareWorldData data = WarfareWorldData.get(level);
            if (!data.match.voteActive || data.match.isGameStarted) {
                player.sendSystemMessage(Component.translatable("pwpwarfare.msg.admin_cmd.no_vote")
                        .withStyle(ChatFormatting.RED));
                return 0;
            }
            data.match.votes.put(player.getUUID(), true);
            player.sendSystemMessage(Component.translatable("pwpwarfare.msg.admin_cmd.voted_ready")
                    .withStyle(ChatFormatting.GREEN));
            PacketHandler.sendToAllClients(level, data);
            return 1;
        }));

        dispatcher.register(Commands.literal("vrequest")
                .executes(ctx -> {
                    ServerPlayer player = ((CommandSourceStack) ctx.getSource()).getPlayerOrException();
                    Entity vehicle = player.getVehicle();
                    if (vehicle != null && WarfareData.of(vehicle).contains(WarfareKeys.VEHICLE_TEAM)) {
                        PacketVehicleDriveRequest.handleRequest(player, vehicle.getId());
                    } else {
                        player.sendSystemMessage(Component.translatable("pwpwarfare.msg.admin_cmd.not_in_vehicle")
                                .withStyle(ChatFormatting.RED));
                    }
                    return 1;
                })
                .then(Commands.argument("vehicleId", IntegerArgumentType.integer())
                        .executes(ctx -> {
                            ServerPlayer player = ((CommandSourceStack) ctx.getSource()).getPlayerOrException();
                            int vehicleId = IntegerArgumentType.getInteger(ctx, "vehicleId");
                            PacketVehicleDriveRequest.handleRequest(player, vehicleId);
                            return 1;
                        })));

        dispatcher.register(Commands.literal("vanswer")
                .then(Commands.argument("action", StringArgumentType.word())
                        .then(Commands.argument("vehicleId", IntegerArgumentType.integer())
                                .then(Commands.argument("playerUUID", StringArgumentType.word())
                                        .executes(ctx -> {
                                            ServerPlayer slPlayer =
                                                    ((CommandSourceStack) ctx.getSource()).getPlayerOrException();
                                            String action = StringArgumentType.getString(ctx, "action");
                                            int vehicleId = IntegerArgumentType.getInteger(ctx, "vehicleId");
                                            UUID playerUUID =
                                                    UUID.fromString(StringArgumentType.getString(ctx, "playerUUID"));
                                            boolean accept = action.equalsIgnoreCase("approve");
                                            PacketVehicleDriveAnswer.handleAnswer(
                                                    slPlayer, accept, vehicleId, playerUUID);
                                            return 1;
                                        })))));
    }
}
