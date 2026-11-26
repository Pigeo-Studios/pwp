package com.pigeostudios.pwp.warfare.command;

import static com.pigeostudios.pwp.warfare.command.MapCommands.*;
import static com.pigeostudios.pwp.warfare.command.ModCommands.*;
import static com.pigeostudios.pwp.warfare.command.ModerationCommands.*;

import com.pigeostudios.pwp.warfare.data.state.WarfareData;
import com.pigeostudios.pwp.warfare.data.state.WarfareKeys;
import com.pigeostudios.pwp.warfare.network.PacketHandler;
import com.pigeostudios.pwp.warfare.server.MarkerManager;
import com.pigeostudios.pwp.warfare.server.PathManager;
import com.pigeostudios.pwp.warfare.squad.SquadActions;
import com.pigeostudios.pwp.warfare.world.WarfareWorldData;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;

/**
 * Команды матча /pwpwarfare: старт/пауза, режим, фракции, тикеты, респаун, команды игроков, отряды и командир.
 *
 * <p>Вынесено из {@link ModCommands} без изменения кода (этап 13 ROADMAP).
 */
public final class MatchCommands {
    private MatchCommands() {}

    static int setGameStart(CommandSourceStack source, boolean active) {
        return setGameStart(source, active, false);
    }

    /** Старт/стоп через MatchAdmin: ошибка (в т.ч. блок раскладки) — sendFailure, а не «успех». */
    static int setGameStart(CommandSourceStack source, boolean active, boolean force) {
        ServerLevel level = source.getLevel();
        if (active) {
            String err = com.pigeostudios.pwp.warfare.admin.MatchAdmin.start(level, force);
            if (com.pigeostudios.pwp.warfare.admin.MatchAdmin.ERR_LAYOUT_BLOCKED.equals(err)) {
                var d = com.pigeostudios.pwp.warfare.admin.MatchAdmin.gate(false);
                source.sendFailure(Component.translatable(
                        "pwpwarfare.admin.match.cmd_blocked", d.errors().size()));
                for (var i : d.errors()) {
                    source.sendFailure(Component.literal(" - ")
                            .append(Component.translatable(i.langKey()))
                            .append(i.subject().isEmpty() ? "" : " [" + i.subject() + "]"));
                }
                return 0;
            }
            return reply(source, err, "pwpwarfare.admin.match.started");
        }
        return reply(
                source, com.pigeostudios.pwp.warfare.admin.MatchAdmin.stop(level), "pwpwarfare.admin.match.stopped");
    }

    /** Ответ команды по результату MatchAdmin: null — успех (ключ ok), иначе — ошибка. */
    static int reply(CommandSourceStack source, String error, String okKey, Object... args) {
        if (error != null) {
            source.sendFailure(Component.translatable(error));
            return 0;
        }
        source.sendSuccess(() -> Component.translatable(okKey, args).withStyle(ChatFormatting.GREEN), true);
        return 1;
    }

    static int setGameMode(CommandSourceStack source, String mode) {
        return reply(
                source,
                com.pigeostudios.pwp.warfare.admin.MatchAdmin.setMode(source.getLevel(), mode),
                "pwpwarfare.admin.match.mode_set",
                mode.toLowerCase(java.util.Locale.ROOT));
    }

    static int setInvasionDefender(CommandSourceStack source, String team) {
        return reply(
                source,
                com.pigeostudios.pwp.warfare.admin.MatchAdmin.setDefender(source.getLevel(), team),
                "pwpwarfare.admin.match.defender_set",
                team.toUpperCase(java.util.Locale.ROOT));
    }

    static int setRespawnTime(CommandSourceStack source, int seconds) {
        ServerLevel level = source.getLevel();
        WarfareWorldData data = WarfareWorldData.get(level);
        data.match.respawnTimer = seconds;
        data.setDirty();
        syncDataToAll(level, data);
        source.sendSuccess(
                () -> Component.translatable("pwpwarfare.msg.admin_cmd.respawn_timer_set", seconds)
                        .withStyle(ChatFormatting.GREEN),
                true);
        return 1;
    }

    static int setFaction(CommandSourceStack source, String team, String faction) {
        return reply(
                source,
                com.pigeostudios.pwp.warfare.admin.MatchAdmin.setFaction(source.getLevel(), team, faction),
                "pwpwarfare.admin.match.faction_set",
                team.toUpperCase(java.util.Locale.ROOT),
                faction.toLowerCase(java.util.Locale.ROOT));
    }

    static int setTickets(CommandSourceStack source, String team, int amount) {
        return reply(
                source,
                com.pigeostudios.pwp.warfare.admin.MatchAdmin.setTickets(source.getLevel(), team, amount),
                "pwpwarfare.admin.match.tickets_set",
                team.toUpperCase(java.util.Locale.ROOT),
                amount);
    }

    static int setDeathTickets(CommandSourceStack source, int amount) {
        ServerLevel level = source.getLevel();
        WarfareWorldData data = WarfareWorldData.get(level);
        data.match.deathTicketCost = amount;
        data.setDirty();
        syncDataToAll(level, data);
        source.sendSuccess(
                () -> Component.translatable("pwpwarfare.msg.admin_cmd.death_cost_set", amount)
                        .withStyle(ChatFormatting.GOLD),
                true);
        return 1;
    }

    static int joinTeam(CommandSourceStack source, String teamName, ServerPlayer player) {
        ServerLevel level = source.getLevel();
        WarfareWorldData data = WarfareWorldData.get(level);
        SquadActions.leaveCurrentSquad(player, data);
        data.setDirty();
        Scoreboard scoreboard = source.getServer().getScoreboard();
        String internalTeamName = teamName.equalsIgnoreCase("blue") ? "Blue" : "Red";
        ChatFormatting color = teamName.equalsIgnoreCase("blue") ? ChatFormatting.BLUE : ChatFormatting.RED;
        PlayerTeam team = scoreboard.getPlayerTeam(internalTeamName);
        if (team == null) {
            team = scoreboard.addPlayerTeam(internalTeamName);
        }

        team.setColor(color);
        team.setSeeFriendlyInvisibles(true);
        if (team.getNameTagVisibility() == PlayerTeam.Visibility.ALWAYS) {
            team.setNameTagVisibility(PlayerTeam.Visibility.HIDE_FOR_OTHER_TEAMS);
        }
        scoreboard.addPlayerToTeam(player.getScoreboardName(), team);
        player.setGameMode(GameType.SURVIVAL);
        player.inventoryMenu.broadcastChanges();
        // Переотправляем фракцию/кит на все клиенты — иначе скины и иконки
        // остаются на старой фракции и мерцают (ClientSkinManager.applyAllSkins)
        PacketHandler.broadcastPlayerSkin(player);
        MarkerManager.syncToPlayer(player);
        PathManager.syncToPlayer(player);
        syncDataToAll(level, data);
        PacketHandler.syncSquadsAll(data.squadRegistry.squads);
        if (data.match.isGameStarted) {
            String currentDim = player.level().dimension().location().toString();
            BlockPos mainSpawn = internalTeamName.equals("Blue")
                    ? data.match.blueSpawns.get(currentDim)
                    : data.match.redSpawns.get(currentDim);
            if (mainSpawn != null) {
                player.teleportTo(mainSpawn.getX() + 0.5, mainSpawn.getY(), mainSpawn.getZ() + 0.5);
                player.sendSystemMessage(Component.translatable("pwpwarfare.msg.admin_cmd.tp_main")
                        .withStyle(ChatFormatting.YELLOW));
            } else {
                player.sendSystemMessage(Component.translatable("pwpwarfare.msg.admin_cmd.no_main_spawn")
                        .withStyle(ChatFormatting.RED));
            }
        }

        player.sendSystemMessage(Component.translatable("pwpwarfare.msg.admin_cmd.joined_team", internalTeamName)
                .withStyle(color));
        source.sendSuccess(
                () -> Component.translatable("pwpwarfare.msg.admin_cmd.player_joined_team", internalTeamName)
                        .withStyle(color),
                true);
        return 1;
    }

    static int clearSquads(CommandSourceStack source, String teamName) {
        ServerLevel level = source.getLevel();
        WarfareWorldData data = WarfareWorldData.get(level);
        String targetTeam = teamName.toUpperCase();

        for (ServerPlayer player : source.getServer().getPlayerList().getPlayers()) {
            if (WarfareData.of(player).contains(WarfareKeys.SQUAD_ID)) {
                boolean belongsToTeam = data.squadRegistry.squads.stream()
                        .anyMatch(s -> s.id == WarfareData.of(player).getInt(WarfareKeys.SQUAD_ID)
                                && s.team.equalsIgnoreCase(targetTeam));
                if (belongsToTeam) {
                    WarfareData.of(player).remove(WarfareKeys.SQUAD_ID);
                    WarfareData.of(player).remove(WarfareKeys.IS_SQUAD_LEADER);
                }
            }
        }

        data.squadRegistry.squads.removeIf(squad -> squad.team.equalsIgnoreCase(targetTeam));
        if (targetTeam.equals("BLUE")) {
            data.squadRegistry.blueCMDId = -1;
        } else if (targetTeam.equals("RED")) {
            data.squadRegistry.redCMDId = -1;
        }

        data.setDirty();
        PacketHandler.sendToAllClients(level, data);
        source.sendSuccess(
                () -> Component.translatable("pwpwarfare.msg.admin_cmd.squads_cleared", targetTeam)
                        .withStyle(ChatFormatting.GREEN),
                true);
        return 1;
    }

    static int forceSetCMD(CommandSourceStack source, int squadId) {
        ServerLevel level = source.getLevel();
        WarfareWorldData data = WarfareWorldData.get(level);
        if (data == null) return 0;
        String team = null;
        for (var s : data.squadRegistry.squads) {
            if (s.id == squadId) {
                team = s.team;
                break;
            }
        }
        if (team == null) {
            source.sendFailure(Component.translatable("pwpwarfare.msg.admin_cmd.squad_not_found"));
            return 0;
        }
        boolean isBlue = team.toUpperCase().contains("BLUE");
        if (isBlue) data.squadRegistry.blueCMDId = squadId;
        else data.squadRegistry.redCMDId = squadId;
        data.setDirty();
        PacketHandler.sendToAllClients(level, data);
        String finalTeam = team;
        source.sendSuccess(
                () -> Component.translatable("pwpwarfare.msg.admin_cmd.cmd_assigned", squadId, finalTeam)
                        .withStyle(ChatFormatting.GREEN),
                true);
        return 1;
    }

    static int togglePause(CommandSourceStack source) {
        boolean paused = com.pigeostudios.pwp.warfare.admin.MatchAdmin.togglePause(source.getLevel());
        source.sendSuccess(
                () -> Component.translatable(
                        paused ? "pwpwarfare.admin.match.paused_chat" : "pwpwarfare.admin.match.resumed_chat"),
                true);
        return 1;
    }
}
