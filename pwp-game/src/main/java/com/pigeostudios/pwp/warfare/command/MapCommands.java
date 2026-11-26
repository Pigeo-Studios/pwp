package com.pigeostudios.pwp.warfare.command;

import static com.pigeostudios.pwp.warfare.command.MatchCommands.*;
import static com.pigeostudios.pwp.warfare.command.ModCommands.*;
import static com.pigeostudios.pwp.warfare.command.ModerationCommands.*;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.pigeostudios.pwp.warfare.network.PacketHandler;
import com.pigeostudios.pwp.warfare.world.WarfareWorldData;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.AABB;

/**
 * Команды карты /pwpwarfare: точки захвата, спавны команд, мейн-зоны.
 *
 * <p>Вынесено из {@link ModCommands} без изменения кода (этап 13 ROADMAP).
 */
public final class MapCommands {
    private MapCommands() {}

    static CompletableFuture<Suggestions> suggestLocalPoints(
            CommandContext<CommandSourceStack> ctx, SuggestionsBuilder builder) {
        ServerLevel level = ((CommandSourceStack) ctx.getSource()).getLevel();
        WarfareWorldData data = WarfareWorldData.get(level);
        List<String> pointNames = new ArrayList<>();

        for (WarfareWorldData.CapturePoint p : data.match.capturePoints) {
            pointNames.add(p.name);
        }

        return SharedSuggestionProvider.suggest(pointNames, builder);
    }

    static int addPoint(
            CommandSourceStack source,
            String shape,
            BlockPos pos1,
            BlockPos pos2,
            String name,
            int bp,
            int rp,
            int time,
            int penalty,
            int captureDeduct,
            int lockMinutes,
            int gainNeut,
            int gainCap) {
        ServerLevel level = source.getLevel();
        WarfareWorldData data = WarfareWorldData.get(level);
        AABB area;
        if (shape.equalsIgnoreCase("cylinder")) {
            double radius = Math.sqrt(pos1.distSqr(new BlockPos(pos2.getX(), pos1.getY(), pos2.getZ())));
            double minY = Math.min(pos1.getY(), pos2.getY());
            double maxY = Math.max(pos1.getY(), pos2.getY()) + 1;
            area = new AABB(
                    pos1.getX() - radius, minY, pos1.getZ() - radius, pos1.getX() + radius, maxY, pos1.getZ() + radius);
        } else {
            double minX = Math.min(pos1.getX(), pos2.getX());
            double minY = Math.min(pos1.getY(), pos2.getY());
            double minZ = Math.min(pos1.getZ(), pos2.getZ());
            double maxX = Math.max(pos1.getX(), pos2.getX()) + 1;
            double maxY = Math.max(pos1.getY(), pos2.getY()) + 1;
            double maxZ = Math.max(pos1.getZ(), pos2.getZ()) + 1;
            area = new AABB(minX, minY, minZ, maxX, maxY, maxZ);
        }

        data.match.capturePoints.add(new WarfareWorldData.CapturePoint(
                name, area, bp, rp, time, penalty, captureDeduct, shape.toUpperCase(), lockMinutes, gainNeut, gainCap));
        data.setDirty();
        source.sendSuccess(
                () -> Component.translatable(
                        "pwpwarfare.msg.admin_cmd.point_added", name, shape, lockMinutes, gainNeut, gainCap),
                true);
        PacketHandler.sendToAllClients(level, data);
        return 1;
    }

    static int removePoint(CommandSourceStack source, String name) {
        ServerLevel level = source.getLevel();
        WarfareWorldData data = WarfareWorldData.get(level);
        boolean removed = data.match.capturePoints.removeIf(p -> p.name.equals(name));
        if (removed) {
            data.setDirty();
            syncDataToAll(level, data);
            source.sendSuccess(
                    () -> Component.translatable(
                                    "pwpwarfare.msg.admin_cmd.point_removed",
                                    name,
                                    level.dimension().location().toString())
                            .withStyle(ChatFormatting.RED),
                    true);
            return 1;
        } else {
            source.sendFailure(Component.translatable("pwpwarfare.msg.admin_cmd.point_not_found", name));
            return 0;
        }
    }

    static int clearSpecificPoint(CommandSourceStack source, String name) {
        ServerLevel level = source.getLevel();
        WarfareWorldData data = WarfareWorldData.get(level);

        for (WarfareWorldData.CapturePoint point : data.match.capturePoints) {
            if (point.name.equals(name)) {
                point.owner = "NEUTRAL";
                point.progress = 0.0F;
                point.capturingTeam = "NONE";
                data.setDirty();
                syncDataToAll(level, data);
                source.sendSuccess(
                        () -> Component.translatable("pwpwarfare.msg.admin_cmd.point_reset", name)
                                .withStyle(ChatFormatting.YELLOW),
                        true);
                return 1;
            }
        }

        source.sendFailure(Component.translatable("pwpwarfare.msg.admin_cmd.point_not_found", name));
        return 0;
    }

    static int forceCapturePoint(CommandSourceStack source, String teamInput, String name) {
        ServerLevel level = source.getLevel();
        WarfareWorldData data = WarfareWorldData.get(level);
        String targetTeam = teamInput.toUpperCase();
        if (!targetTeam.equals("BLUE") && !targetTeam.equals("RED")) {
            source.sendFailure(Component.translatable("pwpwarfare.msg.admin_cmd.bad_team"));
            return 0;
        }

        for (WarfareWorldData.CapturePoint point : data.match.capturePoints) {
            if (point.name.equals(name)) {
                point.owner = targetTeam;
                point.progress = 1.0F;
                point.capturingTeam = "NONE";
                data.setDirty();
                syncDataToAll(level, data);
                ChatFormatting color = targetTeam.equals("BLUE") ? ChatFormatting.BLUE : ChatFormatting.RED;
                source.sendSuccess(
                        () -> Component.translatable("pwpwarfare.msg.admin_cmd.point_forced", name, targetTeam)
                                .withStyle(color),
                        true);
                level.getServer()
                        .getPlayerList()
                        .broadcastSystemMessage(
                                Component.translatable(
                                                "pwpwarfare.msg.admin_cmd.point_forced_broadcast", name, targetTeam)
                                        .withStyle(color),
                                false);
                return 1;
            }
        }

        source.sendFailure(Component.translatable("pwpwarfare.msg.admin_cmd.point_not_found", name));
        return 0;
    }

    static int setTeamSpawn(CommandSourceStack source, String teamName, BlockPos pos) {
        ServerLevel level = source.getLevel();
        WarfareWorldData data = WarfareWorldData.get(level);
        String currentDim = level.dimension().location().toString();
        if (teamName.equalsIgnoreCase("blue")) {
            data.match.blueSpawns.put(currentDim, pos);
        } else if (teamName.equalsIgnoreCase("red")) {
            data.match.redSpawns.put(currentDim, pos);
        } else if (teamName.equalsIgnoreCase("none")) {
            data.match.neutralSpawns.put(currentDim, pos);
        }

        data.setDirty();
        syncDataToAll(level, data);
        source.sendSuccess(
                () -> Component.translatable("pwpwarfare.msg.admin_cmd.spawn_set")
                        .withStyle(ChatFormatting.GREEN),
                true);
        return 1;
    }

    static int addMainZone(CommandSourceStack source, String team, String shape, BlockPos pos1, BlockPos pos2) {
        ServerLevel level = source.getLevel();
        WarfareWorldData data = WarfareWorldData.get(level);
        AABB area;
        if (shape.equalsIgnoreCase("cylinder")) {
            double radius = Math.sqrt(pos1.distSqr(new BlockPos(pos2.getX(), pos1.getY(), pos2.getZ())));
            double minY = Math.min(pos1.getY(), pos2.getY());
            double maxY = Math.max(pos1.getY(), pos2.getY()) + 1;
            area = new AABB(
                    pos1.getX() - radius, minY, pos1.getZ() - radius, pos1.getX() + radius, maxY, pos1.getZ() + radius);
        } else {
            area = new AABB(
                    Math.min(pos1.getX(), pos2.getX()),
                    Math.min(pos1.getY(), pos2.getY()),
                    Math.min(pos1.getZ(), pos2.getZ()),
                    Math.max(pos1.getX(), pos2.getX()) + 1,
                    Math.max(pos1.getY(), pos2.getY()) + 1,
                    Math.max(pos1.getZ(), pos2.getZ()) + 1);
        }

        data.structures.mainZones.removeIf(z -> z.team.equalsIgnoreCase(team));
        data.structures.mainZones.add(
                new WarfareWorldData.MainProtectionZone(team.toUpperCase(), shape.toUpperCase(), area));
        data.setDirty();
        source.sendSuccess(
                () -> Component.translatable("pwpwarfare.msg.admin_cmd.zone_added", team.toUpperCase())
                        .withStyle(ChatFormatting.GREEN),
                true);
        return 1;
    }

    static int removeMainZone(CommandSourceStack source, String team) {
        ServerLevel level = source.getLevel();
        WarfareWorldData data = WarfareWorldData.get(level);
        boolean removed = data.structures.mainZones.removeIf(z -> z.team.equalsIgnoreCase(team));
        if (removed) {
            data.setDirty();
            source.sendSuccess(
                    () -> Component.translatable("pwpwarfare.msg.admin_cmd.zone_removed", team.toUpperCase())
                            .withStyle(ChatFormatting.GREEN),
                    true);
        } else {
            source.sendFailure(Component.translatable("pwpwarfare.msg.admin_cmd.zone_not_found", team.toUpperCase()));
        }

        return 1;
    }

    static void syncDataToAll(ServerLevel level, WarfareWorldData data) {
        PacketHandler.sendToAllClients(level, data);
    }
}
