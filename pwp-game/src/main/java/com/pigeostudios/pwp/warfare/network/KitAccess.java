package com.pigeostudios.pwp.warfare.network;

import com.pigeostudios.pwp.warfare.data.state.WarfareData;
import com.pigeostudios.pwp.warfare.data.state.WarfareKeys;
import com.pigeostudios.pwp.warfare.match.KitLimits;
import com.pigeostudios.pwp.warfare.world.WarfareWorldData;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.server.level.ServerPlayer;

/**
 * Серверная проверка доступности кита игроку — ЕДИНСТВЕННАЯ точка для меню деплоя
 * ({@link PacketRequestKitMenu}), выбора кита ({@link PacketSelectKit}) и применения кита при
 * спавне ({@link ResupplyHandler#tryApplyPendingKit}, {@link ResupplyHandler#applyCurrentKit}).
 * Собирает других игроков команды в {@link KitLimits.Holder} и передаёт счётчики в чистое
 * правило {@link KitLimits#unavailableReason}; ручных копий проверок быть не должно.
 */
public final class KitAccess {
    public static final String UNASSIGNED = KitLimits.UNASSIGNED;

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

    /** Лидер отряда (по реестру или флагу); креатив — тоже (как и раньше в меню деплоя). */
    public static boolean isLeader(ServerPlayer player, WarfareWorldData data) {
        if (player.isCreative()) return true;
        WarfareWorldData.Squad squad = squadOf(player.getScoreboardName(), data);
        if (squad != null && squad.leader.equals(player.getScoreboardName())) return true;
        return WarfareData.of(player).getBoolean(WarfareKeys.IS_SQUAD_LEADER);
    }

    /** Другие онлайн-игроки команды игрока с их CurrentKit/PendingKit. */
    public static List<KitLimits.Holder> others(ServerPlayer player, WarfareWorldData data) {
        List<KitLimits.Holder> out = new ArrayList<>();
        if (player.getTeam() == null) return out;
        String team = player.getTeam().getName().toUpperCase();
        WarfareWorldData.Squad mySquad = squadOf(player.getScoreboardName(), data);
        for (ServerPlayer p : player.server.getPlayerList().getPlayers()) {
            if (p == player || p.getUUID().equals(player.getUUID()) || p.getTeam() == null) continue;
            if (!p.getTeam().getName().toUpperCase().equals(team)) continue;
            out.add(new KitLimits.Holder(
                    WarfareData.of(p).getString(WarfareKeys.CURRENT_KIT),
                    WarfareData.of(p).getString(WarfareKeys.PENDING_KIT),
                    mySquad != null && mySquad.members.contains(p.getScoreboardName())));
        }
        return out;
    }

    /** null — кит доступен игроку, иначе причина в виде {@code key|a|b} ({@link KitReasonText#of}). */
    public static String unavailableReason(
            ServerPlayer player, WarfareWorldData data, String kitName, WarfareWorldData.KitInfo kit) {
        return unavailableReason(player, data, kitName, kit, others(player, data));
    }

    /** То же с уже собранным списком других игроков (меню проверяет все киты за один проход). */
    public static String unavailableReason(
            ServerPlayer player,
            WarfareWorldData data,
            String kitName,
            WarfareWorldData.KitInfo kit,
            List<KitLimits.Holder> others) {
        // «Стрельбище» (этап 15): любые роли без лимитов — тренироваться можно всем
        if (com.pwp.api.match.GameModes.isTraining(data.match.gameMode) && kit != null) return null;
        Map<String, WarfareWorldData.KitInfo> kits = teamKits(player, data);
        WarfareWorldData.Squad mySquad = squadOf(player.getScoreboardName(), data);
        KitLimits.Occupancy occ = KitLimits.occupancy(others, kitName);
        int fireSupport = KitLimits.FIRE_SUPPORT.equals(kit.category) && kits != null
                ? KitLimits.fireSupportInSquad(others, k -> {
                    WarfareWorldData.KitInfo info = kits.get(k);
                    return info != null ? info.category : "";
                })
                : 0;
        KitLimits.Reason r = KitLimits.reasonOf(new KitLimits.Input(
                kitName,
                kit.maxPerTeam,
                kit.maxPerSquad,
                kit.minSquadPlayers,
                kit.category,
                kit.isLeaderOnly,
                isLeader(player, data),
                occ.team(),
                occ.squad(),
                mySquad == null ? -1 : mySquad.members.size(),
                fireSupport));
        // Игроку уходит ключ lang + аргументы ({@code key|a|b}); текст — KitReasonText.of
        return r == null ? null : r.encoded();
    }
}
