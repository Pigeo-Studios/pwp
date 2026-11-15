package com.pigeostudios.pwp.warfare.network;

import com.pigeostudios.pwp.warfare.data.state.WarfareData;
import com.pigeostudios.pwp.warfare.data.state.WarfareKeys;
import com.pigeostudios.pwp.warfare.match.KitLimits;
import com.pigeostudios.pwp.warfare.world.WarfareWorldData;
import java.util.Map;
import net.minecraft.server.level.ServerPlayer;

/**
 * Серверная проверка доступности кита игроку: собирает счётчики из онлайна и передаёт их в
 * чистое правило {@link KitLimits#unavailableReason}. Единая точка для выбора кита
 * ({@link PacketSelectKit}).
 */
public final class KitAccess {
    public static final String UNASSIGNED = "Unassigned";

    private KitAccess() {}

    /** Киты команды игрока (BLUE/RED) или null, если игрок без команды. */
    public static Map<String, WarfareWorldData.KitInfo> teamKits(ServerPlayer player, WarfareWorldData data) {
        if (player.getTeam() == null) return null;
        String team = player.getTeam().getName().toUpperCase();
        if (team.equals("BLUE")) return data.blueKits;
        if (team.equals("RED")) return data.redKits;
        return null;
    }

    public static WarfareWorldData.Squad squadOf(String playerName, WarfareWorldData data) {
        for (WarfareWorldData.Squad s : data.squadRegistry.squads) {
            if (s.members.contains(playerName)) return s;
        }
        return null;
    }

    /** Лидер отряда (по реестру или флагу); креатив — тоже (как в меню деплоя). */
    public static boolean isLeader(ServerPlayer player, WarfareWorldData data) {
        if (player.isCreative()) return true;
        WarfareWorldData.Squad squad = squadOf(player.getScoreboardName(), data);
        if (squad != null && squad.leader.equals(player.getScoreboardName())) return true;
        return WarfareData.of(player).getBoolean(WarfareKeys.IS_SQUAD_LEADER);
    }

    /** null — кит доступен игроку, иначе причина. */
    public static String unavailableReason(
            ServerPlayer player, WarfareWorldData data, String kitName, WarfareWorldData.KitInfo kit) {
        if (UNASSIGNED.equals(kitName)) return null;
        if (kit.isLeaderOnly && !isLeader(player, data)) return "Squad Leader only";
        if (kit.maxPerTeam == 0) return "Role Disabled";
        String team = player.getTeam() != null ? player.getTeam().getName().toUpperCase() : "";
        WarfareWorldData.Squad mySquad = squadOf(player.getScoreboardName(), data);
        int tCount = 0, sCount = 0;
        for (ServerPlayer p : player.server.getPlayerList().getPlayers()) {
            if (p == player || p.getTeam() == null) continue;
            if (!p.getTeam().getName().toUpperCase().equals(team)) continue;
            String cKit = WarfareData.of(p).getString(WarfareKeys.CURRENT_KIT);
            String pKit = WarfareData.of(p).getString(WarfareKeys.PENDING_KIT);
            if (cKit.equals(kitName) || pKit.equals(kitName)) {
                tCount++;
                if (mySquad != null && mySquad.members.contains(p.getScoreboardName())) sCount++;
            }
        }
        return KitLimits.unavailableReason(new KitLimits.Input(
                kit.maxPerTeam,
                kit.maxPerSquad,
                kit.minSquadPlayers,
                kit.category,
                tCount,
                sCount,
                mySquad == null ? -1 : mySquad.members.size(),
                KitLimits.FIRE_SUPPORT.equals(kit.category)
                        ? ResupplyHandler.countFireSupportOthersInSquad(player, data)
                        : 0));
    }
}
