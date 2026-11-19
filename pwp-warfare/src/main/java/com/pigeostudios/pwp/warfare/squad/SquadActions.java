package com.pigeostudios.pwp.warfare.squad;

import com.pigeostudios.pwp.warfare.config.WarfareConfig;
import com.pigeostudios.pwp.warfare.data.state.WarfareData;
import com.pigeostudios.pwp.warfare.data.state.WarfareKeys;
import com.pigeostudios.pwp.warfare.item.ModItems;
import com.pigeostudios.pwp.warfare.network.PacketHandler;
import com.pigeostudios.pwp.warfare.network.PacketRequestKitMenu;
import com.pigeostudios.pwp.warfare.network.ResupplyHandler;
import com.pigeostudios.pwp.warfare.util.PlayerFeedback;
import com.pigeostudios.pwp.warfare.world.WarfareWorldData;
import java.util.ArrayList;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;

// Пакет управления отрядом: создание, вступление, выход, кик, повышение, блокировка
// Содержит всю логику работы с отделениями игроков
/**
 * Логика действий отряда (создать/вступить/выйти/кик/повысить/закрыть/огневые группы/распустить).
 * Сетевые пакеты — {@link SquadPackets} (по пакету на действие); общие помощники — ниже.
 */
public final class SquadActions {
    private static final String[] NATO_ALPHABET = new String[] {
        "Alpha",
        "Bravo",
        "Charlie",
        "Delta",
        "Echo",
        "Foxtrot",
        "Golf",
        "Hotel",
        "India",
        "Juliett",
        "Kilo",
        "Lima",
        "Mike",
        "November",
        "Oscar",
        "Papa",
        "Quebec",
        "Romeo",
        "Sierra",
        "Tango",
        "Uniform",
        "Victor",
        "Whiskey",
        "X-ray",
        "Yankee",
        "Zulu"
    };

    private SquadActions() {}

    /** Сигнатура действия: игрок, данные мира, ник, команда игрока. */
    @FunctionalInterface
    interface Action {
        void run(ServerPlayer player, WarfareWorldData data, String pName, String pTeam);
    }

    /**
     * Общая обвязка всех действий (как в старом обработчике): игрок должен быть в команде
     * (или в креативе), после действия — сохранение мира и синхронизация отрядов в измерении.
     */
    static void perform(ServerPlayer player, Action action) {
        WarfareWorldData data = WarfareWorldData.get(player.serverLevel());
        String pName = player.getScoreboardName();
        String pTeam = player.getTeam() != null ? player.getTeam().getName() : "NEUTRAL";
        if (pTeam.equals("NEUTRAL") && !player.isCreative()) {
            PlayerFeedback.deny(player, "pwpwarfare.msg.squad.choose_team_first");
            return;
        }
        action.run(player, data, pName, pTeam);
        data.setDirty();
        PacketHandler.syncSquadsAll(data.squadRegistry.squads);
    }

    /** Действие отряда #0 (бывший PacketSquadAction action=0). */
    static void create(
            ServerPlayer player, WarfareWorldData data, String pName, String pTeam, int squadId, String stringData) {
        leaveCurrentSquad(player, data);
        String rawName = stringData.trim();
        if (rawName.length() > 12) {
            rawName = rawName.substring(0, 12);
        }

        String finalName = rawName.isEmpty() ? getAvailableSquadName(data, pTeam) : rawName;
        int newId = 0;

        for (int i = 1; i < 1000; i++) {
            boolean idTaken = false;

            for (WarfareWorldData.Squad s : data.squadRegistry.squads) {
                if (s.id == i) {
                    idTaken = true;
                    break;
                }
            }

            if (!idTaken) {
                newId = i;
                break;
            }
        }

        String currentDim = player.level().dimension().location().toString();
        WarfareWorldData.Squad newSquad = new WarfareWorldData.Squad(newId, finalName, pTeam, pName, currentDim);
        newSquad.members.add(pName);
        data.squadRegistry.squads.add(newSquad);
        updatePlayerTags(player, newId, true);
        if ((Boolean) WarfareConfig.AUTO_GIVE_SL_RADIO.get()) {
            giveRadio(player);
        }

        player.sendSystemMessage(Component.translatable("pwpwarfare.msg.squad.created", finalName)
                .withStyle(ChatFormatting.GOLD));
        PacketRequestKitMenu.sendKitMenu(player, data);
    }

    /** Действие отряда #1 (бывший PacketSquadAction action=1). */
    static void join(
            ServerPlayer player, WarfareWorldData data, String pName, String pTeam, int squadId, String stringData) {
        for (WarfareWorldData.Squad s : data.squadRegistry.squads) {
            if (s.id == squadId && s.team.equalsIgnoreCase(pTeam)) {
                if (s.isLocked && !player.isCreative()) {
                    player.sendSystemMessage(Component.translatable("pwpwarfare.msg.squad.locked")
                            .withStyle(ChatFormatting.RED));
                    return;
                }

                if (s.members.size() >= 9) {
                    player.sendSystemMessage(
                            Component.translatable("pwpwarfare.msg.squad.full").withStyle(ChatFormatting.RED));
                    return;
                }

                leaveCurrentSquad(player, data);
                s.members.add(pName);
                updatePlayerTags(player, s.id, false);
                player.sendSystemMessage(Component.translatable("pwpwarfare.msg.squad.joined", s.name)
                        .withStyle(ChatFormatting.GREEN));
                PacketRequestKitMenu.sendKitMenu(player, data);
                break;
            }
        }
    }

    /**
     * «Быстро вступить»: первый открытый отряд своей команды в этом измерении со свободным местом
     * (по id), иначе — новый отряд с автоименем. Уже в отряде — ничего не меняется.
     */
    public static void quickJoin(ServerPlayer player) {
        perform(player, (pl, d, n, t) -> {
            if (getPlayerSquad(n, d) != null) {
                pl.sendSystemMessage(Component.translatable("pwpwarfare.squad.quick_join.already")
                        .withStyle(ChatFormatting.YELLOW));
                return;
            }
            String dim = pl.level().dimension().location().toString();
            WarfareWorldData.Squad best = null;
            for (WarfareWorldData.Squad s : d.squadRegistry.squads) {
                if (!s.team.equalsIgnoreCase(t) || s.isLocked || s.members.size() >= 9) continue;
                if (s.dimension != null && !s.dimension.equals(dim)) continue;
                if (best == null || s.id < best.id) best = s;
            }
            if (best != null) join(pl, d, n, t, best.id, "");
            else create(pl, d, n, t, 0, "");
        });
    }

    /** Действие отряда #2 (бывший PacketSquadAction action=2). */
    static void leave(
            ServerPlayer player, WarfareWorldData data, String pName, String pTeam, int squadId, String stringData) {
        leaveCurrentSquad(player, data);
        player.sendSystemMessage(
                Component.translatable("pwpwarfare.msg.squad.left").withStyle(ChatFormatting.YELLOW));
        PacketRequestKitMenu.sendKitMenu(player, data);
    }

    /** Действие отряда #3 (бывший PacketSquadAction action=3). */
    static void kick(
            ServerPlayer player, WarfareWorldData data, String pName, String pTeam, int squadId, String stringData) {
        WarfareWorldData.Squad targetSquad = null;

        for (WarfareWorldData.Squad s : data.squadRegistry.squads) {
            if (s.id == squadId) {
                targetSquad = s;
                break;
            }
        }

        if (targetSquad != null) {
            String targetName = stringData;
            ServerPlayer targetEntity = player.server.getPlayerList().getPlayerByName(targetName);
            boolean isLeaderKicking = targetSquad.leader.equals(pName) && !targetName.equals(pName);
            boolean isKickingOfflineLeader = targetName.equals(targetSquad.leader)
                    && targetEntity == null
                    && targetSquad.members.contains(pName);
            if ((isLeaderKicking || isKickingOfflineLeader) && targetSquad.members.remove(targetName)) {
                if (targetName.equals(targetSquad.leader) && !targetSquad.members.isEmpty()) {
                    targetSquad.leader = targetSquad.members.get(0);
                    ServerPlayer newLeader = player.server.getPlayerList().getPlayerByName(targetSquad.leader);
                    if (newLeader != null) {
                        updatePlayerTags(newLeader, targetSquad.id, true);
                        if ((Boolean) WarfareConfig.AUTO_GIVE_SL_RADIO.get()) {
                            giveRadio(newLeader);
                        }

                        newLeader.sendSystemMessage(Component.translatable("pwpwarfare.msg.squad.new_leader_offline")
                                .withStyle(ChatFormatting.GOLD));
                        PacketRequestKitMenu.sendKitMenu(newLeader, data);
                    }
                }

                if (targetEntity != null) {
                    removePlayerTags(targetEntity);
                    removeRadio(targetEntity);
                    targetEntity.displayClientMessage(
                            Component.translatable("pwpwarfare.msg.squad.kicked")
                                    .withStyle(ChatFormatting.RED),
                            true);
                    PacketRequestKitMenu.sendKitMenu(targetEntity, data);
                }

                Component text = isKickingOfflineLeader
                        ? Component.translatable(
                                "pwpwarfare.msg.squad.offline_leader_removed", targetName, targetSquad.name)
                        : Component.translatable("pwpwarfare.msg.squad.member_kicked", targetName, targetSquad.name);
                player.server
                        .getPlayerList()
                        .broadcastSystemMessage(text.copy().withStyle(ChatFormatting.YELLOW), false);
            }
        }
    }

    /** Действие отряда #4 (бывший PacketSquadAction action=4). */
    static void promote(
            ServerPlayer player, WarfareWorldData data, String pName, String pTeam, int squadId, String stringData) {
        WarfareWorldData.Squad mySquad = getPlayerSquad(pName, data);
        if (mySquad != null && mySquad.leader.equals(pName)) {
            String targetName = stringData;
            if (mySquad.members.contains(targetName)) {
                removeRadio(player);
                if (!pTeam.equalsIgnoreCase("Blue")) {
                    ;
                }

                mySquad.leader = targetName;
                updatePlayerTags(player, mySquad.id, false);
                ServerPlayer target = player.server.getPlayerList().getPlayerByName(targetName);
                if (target != null) {
                    updatePlayerTags(target, mySquad.id, true);
                    if ((Boolean) WarfareConfig.AUTO_GIVE_SL_RADIO.get()) {
                        giveRadio(target);
                    }

                    mySquad.leader = targetName;
                    target.displayClientMessage(
                            Component.translatable("pwpwarfare.msg.squad.promoted_you")
                                    .withStyle(ChatFormatting.GOLD),
                            true);
                    PacketRequestKitMenu.sendKitMenu(target, data);
                }

                player.sendSystemMessage(Component.translatable("pwpwarfare.msg.squad.promoted", targetName)
                        .withStyle(ChatFormatting.GOLD));
                PacketRequestKitMenu.sendKitMenu(player, data);
            }
        }
    }

    /** Действие отряда #5 (бывший PacketSquadAction action=5). */
    static void toggleLock(
            ServerPlayer player, WarfareWorldData data, String pName, String pTeam, int squadId, String stringData) {
        WarfareWorldData.Squad mySquad = getPlayerSquad(pName, data);
        if (mySquad != null && mySquad.leader.equals(pName)) {
            mySquad.isLocked = !mySquad.isLocked;
            String status =
                    mySquad.isLocked ? "pwpwarfare.msg.squad.status_locked" : "pwpwarfare.msg.squad.status_open";
            ChatFormatting color = mySquad.isLocked ? ChatFormatting.RED : ChatFormatting.GREEN;
            player.sendSystemMessage(Component.translatable("pwpwarfare.msg.squad.now", Component.translatable(status))
                    .withStyle(color));
        }
    }

    /** Действие отряда #6 (бывший PacketSquadAction action=6). */
    static void setBravoLeader(
            ServerPlayer player, WarfareWorldData data, String pName, String pTeam, int squadId, String stringData) {
        WarfareWorldData.Squad mySquad = getPlayerSquad(pName, data);
        if (mySquad != null && (mySquad.leader.equals(pName) || mySquad.bravoLeader.equals(pName))) {
            String target = stringData;
            if (mySquad.members.contains(target)
                    && !mySquad.leader.equals(target)
                    && !mySquad.charlieLeader.equals(target)) {
                mySquad.removeFromFireteams(target);
                mySquad.bravoLeader = target;
                if (!mySquad.bravoMembers.contains(target)) {
                    mySquad.bravoMembers.add(target);
                }

                player.sendSystemMessage(Component.translatable("pwpwarfare.msg.squad.ftl_bravo", target)
                        .withStyle(ChatFormatting.GOLD));
            }
        }
    }

    /** Действие отряда #7 (бывший PacketSquadAction action=7). */
    static void setCharlieLeader(
            ServerPlayer player, WarfareWorldData data, String pName, String pTeam, int squadId, String stringData) {
        WarfareWorldData.Squad mySquad = getPlayerSquad(pName, data);
        if (mySquad != null && (mySquad.leader.equals(pName) || mySquad.charlieLeader.equals(pName))) {
            String target = stringData;
            if (mySquad.members.contains(target)
                    && !mySquad.leader.equals(target)
                    && !mySquad.bravoLeader.equals(target)) {
                mySquad.removeFromFireteams(target);
                mySquad.charlieLeader = target;
                if (!mySquad.charlieMembers.contains(target)) {
                    mySquad.charlieMembers.add(target);
                }

                player.sendSystemMessage(Component.translatable("pwpwarfare.msg.squad.ftl_charlie", target)
                        .withStyle(ChatFormatting.GOLD));
            }
        }
    }

    /** Действие отряда #8 (бывший PacketSquadAction action=8). */
    static void addToBravo(
            ServerPlayer player, WarfareWorldData data, String pName, String pTeam, int squadId, String stringData) {
        WarfareWorldData.Squad mySquad = getPlayerSquad(pName, data);
        if (mySquad != null && (mySquad.leader.equals(pName) || mySquad.bravoLeader.equals(pName))) {
            String target = stringData;
            if (mySquad.members.contains(target) && !mySquad.leader.equals(target)) {
                mySquad.removeFromFireteams(target);
                mySquad.bravoMembers.add(target);
            }
        }
    }

    /** Действие отряда #9 (бывший PacketSquadAction action=9). */
    static void addToCharlie(
            ServerPlayer player, WarfareWorldData data, String pName, String pTeam, int squadId, String stringData) {
        WarfareWorldData.Squad mySquad = getPlayerSquad(pName, data);
        if (mySquad != null && (mySquad.leader.equals(pName) || mySquad.charlieLeader.equals(pName))) {
            String target = stringData;
            if (mySquad.members.contains(target) && !mySquad.leader.equals(target)) {
                mySquad.removeFromFireteams(target);
                mySquad.charlieMembers.add(target);
            }
        }
    }

    /** Действие отряда #10 (бывший PacketSquadAction action=10). */
    static void removeFromFireteam(
            ServerPlayer player, WarfareWorldData data, String pName, String pTeam, int squadId, String stringData) {
        WarfareWorldData.Squad mySquad = getPlayerSquad(pName, data);
        if (mySquad != null) {
            String target = stringData;
            boolean canRemove = mySquad.leader.equals(pName)
                    || mySquad.bravoLeader.equals(pName) && mySquad.bravoMembers.contains(target)
                    || mySquad.charlieLeader.equals(pName) && mySquad.charlieMembers.contains(target);
            if (canRemove) {
                mySquad.removeFromFireteams(target);
            }
        }
    }

    /** Действие отряда #11 (бывший PacketSquadAction action=11). */
    static void disband(
            ServerPlayer player, WarfareWorldData data, String pName, String pTeam, int squadId, String stringData) {
        WarfareWorldData.Squad mySquad = getPlayerSquad(pName, data);
        if (mySquad != null && mySquad.id == squadId && mySquad.leader.equals(pName)) {
            String squadName = mySquad.name;
            for (String m : new ArrayList<>(mySquad.members)) {
                mySquad.members.remove(m);
                mySquad.removeFromFireteams(m);
                ServerPlayer mem = player.server.getPlayerList().getPlayerByName(m);
                if (mem != null) {
                    removePlayerTags(mem);
                    removeRadio(mem);
                    mem.displayClientMessage(
                            Component.translatable("pwpwarfare.msg.squad.disbanded_by_leader")
                                    .withStyle(ChatFormatting.RED),
                            true);
                    PacketRequestKitMenu.sendKitMenu(mem, data);
                }
            }
            if (mySquad.id == data.squadRegistry.blueCMDId) data.squadRegistry.blueCMDId = -1;
            if (mySquad.id == data.squadRegistry.redCMDId) data.squadRegistry.redCMDId = -1;
            data.squadRegistry.squads.remove(mySquad);
            player.sendSystemMessage(
                    Component.translatable("pwpwarfare.msg.squad.disbanded").withStyle(ChatFormatting.GOLD));
        }
    }

    public static String getAvailableSquadName(WarfareWorldData data, String teamName) {
        for (String natoName : NATO_ALPHABET) {
            boolean taken = false;

            for (WarfareWorldData.Squad s : data.squadRegistry.squads) {
                if (s.team.equalsIgnoreCase(teamName) && s.name.equalsIgnoreCase(natoName)) {
                    taken = true;
                    break;
                }
            }

            if (!taken) {
                return natoName;
            }
        }

        return "Squad " + (data.squadRegistry.squads.size() + 1);
    }

    public static WarfareWorldData.Squad getPlayerSquad(String playerName, WarfareWorldData data) {
        for (WarfareWorldData.Squad s : data.squadRegistry.squads) {
            if (s.members.contains(playerName)) {
                return s;
            }
        }

        return null;
    }

    // Выход игрока из текущего отряда: очистка данных, назначение нового лидера
    public static void leaveCurrentSquad(ServerPlayer player, WarfareWorldData data) {
        String pName = player.getScoreboardName();

        for (WarfareWorldData.Squad s : data.squadRegistry.squads) {
            if (s.members.contains(pName)) {
                s.members.remove(pName);
                s.removeFromFireteams(pName);
                String pTeam =
                        player.getTeam() != null ? player.getTeam().getName().toUpperCase() : "NEUTRAL";
                boolean voteCancelled = false;
                if (pTeam.equals("BLUE")
                        && data.squadRegistry.blueCmdVoteActive
                        && data.squadRegistry.blueCmdCandidateName.equals(pName)) {
                    data.squadRegistry.blueCmdVoteActive = false;
                    data.squadRegistry.blueCmdVotes.clear();
                    voteCancelled = true;
                } else if (pTeam.equals("RED")
                        && data.squadRegistry.redCmdVoteActive
                        && data.squadRegistry.redCmdCandidateName.equals(pName)) {
                    data.squadRegistry.redCmdVoteActive = false;
                    data.squadRegistry.redCmdVotes.clear();
                    voteCancelled = true;
                }

                if (voteCancelled) {
                    Component cancelMsg = Component.translatable("pwpwarfare.msg.cmd.cancelled_left", pName)
                            .withStyle(ChatFormatting.RED);
                    player.server.getPlayerList().broadcastSystemMessage(cancelMsg, false);
                }

                if (s.leader.equals(pName)) {
                    removeRadio(player);
                    if (!s.members.isEmpty()) {
                        s.leader = s.members.get(0);
                        ServerPlayer newLeader = player.server.getPlayerList().getPlayerByName(s.leader);
                        if (newLeader != null) {
                            updatePlayerTags(newLeader, s.id, true);
                            if ((Boolean) WarfareConfig.AUTO_GIVE_SL_RADIO.get()) {
                                giveRadio(newLeader);
                            }
                        }
                    } else {
                        if (s.id == data.squadRegistry.blueCMDId) {
                            data.squadRegistry.blueCMDId = -1;
                        }

                        if (s.id == data.squadRegistry.redCMDId) {
                            data.squadRegistry.redCMDId = -1;
                        }
                    }
                }
                break;
            }
        }

        data.squadRegistry.squads.removeIf(sx -> sx.members.isEmpty());
        WarfareData.of(player).putString(WarfareKeys.CURRENT_KIT, "Unassigned");
        WarfareData.of(player).putString(WarfareKeys.PENDING_KIT, "");
        player.getInventory().clearContent();
        ResupplyHandler.clearCurios(player);
        player.inventoryMenu.broadcastChanges();
        player.containerMenu.broadcastChanges();
        removePlayerTags(player);
        if (!data.match.isGameStarted) {
            player.displayClientMessage(
                    Component.translatable("pwpwarfare.msg.squad.left_pregame").withStyle(ChatFormatting.YELLOW), true);
        } else {
            player.displayClientMessage(
                    Component.translatable("pwpwarfare.msg.squad.left_gear_reset")
                            .withStyle(ChatFormatting.YELLOW),
                    true);
        }

        data.setDirty();
        PacketHandler.sendToAllClients(player.serverLevel(), data);
    }

    // Выдаёт игроку радиостанцию, если её ещё нет в инвентаре
    public static void giveRadio(ServerPlayer player) {
        if (player != null) {
            boolean hasRadio = false;

            for (ItemStack stack : player.getInventory().items) {
                if (stack.getItem() == ModItems.SQUAD_LEADER_RADIO.get()) {
                    hasRadio = true;
                    break;
                }
            }

            if (!hasRadio && player.getOffhandItem().getItem() == ModItems.SQUAD_LEADER_RADIO.get()) {
                hasRadio = true;
            }

            if (!hasRadio) {
                ItemStack radioStack = new ItemStack((ItemLike) ModItems.SQUAD_LEADER_RADIO.get());
                if (!player.getInventory().add(radioStack)) {
                    player.drop(radioStack, false);
                }
            }
        }
    }

    public static void removeRadio(ServerPlayer player) {
        if (player != null) {
            if ((Boolean) WarfareConfig.AUTO_GIVE_SL_RADIO.get()) {
                player.getInventory()
                        .clearOrCountMatchingItems(
                                p -> p.getItem() == ModItems.SQUAD_LEADER_RADIO.get(),
                                -1,
                                player.inventoryMenu.getCraftSlots());
                player.inventoryMenu.broadcastChanges();
            }
        }
    }

    public static void updatePlayerTags(ServerPlayer p, int squadId, boolean isLeader) {
        WarfareData.of(p).putInt(WarfareKeys.SQUAD_ID, squadId);
        WarfareData.of(p).putBoolean(WarfareKeys.IS_SQUAD_LEADER, isLeader);
    }

    public static void removePlayerTags(ServerPlayer p) {
        WarfareData.of(p).remove(WarfareKeys.SQUAD_ID);
        WarfareData.of(p).remove(WarfareKeys.IS_SQUAD_LEADER);
    }
}
