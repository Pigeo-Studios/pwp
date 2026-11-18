package com.pigeostudios.pwp.warfare.staging;

import com.pigeostudios.pwp.warfare.data.state.WarfareData;
import com.pigeostudios.pwp.warfare.data.state.WarfareKeys;
import com.pigeostudios.pwp.warfare.match.TeamBalance;
import com.pigeostudios.pwp.warfare.network.PacketHandler;
import com.pigeostudios.pwp.warfare.network.ResupplyHandler;
import com.pigeostudios.pwp.warfare.server.FactionDataLoader;
import com.pigeostudios.pwp.warfare.squad.SquadActions;
import com.pigeostudios.pwp.warfare.world.WarfareWorldData;
import com.pwp.api.time.GameClock;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;
import net.minecraftforge.network.PacketDistributor;

/**
 * Стейджинг «как в Squad» (этап 4): фаза ожидания перед GO. Команда при входе (меньшая), выбор/смена
 * команды с ответом клиенту, кит сразу при выборе роли в мейне, «ВЗЯТЬ СНАРЯЖЕНИЕ» живым в своём
 * мейне, техника в мейне с начала ожидания, удержание техники с игроками в мейне до GO.
 * Состояние — только {@link WarfareWorldData}/{@link WarfareData}; время — {@link GameClock}.
 */
public final class StagingService {
    /** Кулдаун «взять снаряжение» = кулдаун главного склада (InteractionEvents). */
    public static final long TAKE_KIT_COOLDOWN_TICKS = 1200L;

    // ключи ответа на выбор команды (lang pwpwarfare.team.result.*)
    public static final String RESULT_JOINED = "pwpwarfare.team.result.joined";
    public static final String RESULT_SAME = "pwpwarfare.team.result.same";
    public static final String RESULT_FULL = "pwpwarfare.team.result.full";
    public static final String RESULT_BAD = "pwpwarfare.team.result.bad";

    /** Техника стейджинга выставлена в этом матче (память; на GO — выставить, если не успели). */
    private static boolean vehiclesFilled;

    /** Последняя позиция техники внутри своего мейна (для возврата при попытке выехать до GO). */
    private static final Map<UUID, Vec3> lastInside = new HashMap<>();

    private StagingService() {}

    /** Фаза до GO, когда игроки уже в мейне: ожидание или обратный отсчёт. */
    public static boolean isStaging(WarfareWorldData data) {
        return !data.match.isGameStarted && (data.match.waitingActive || data.match.countdownActive);
    }

    public static String teamOf(ServerPlayer p) {
        String t = p.getTeam() != null ? p.getTeam().getName() : "";
        return TeamBalance.BLUE.equalsIgnoreCase(t) || TeamBalance.RED.equalsIgnoreCase(t) ? t : "";
    }

    /** Игрок внутри мейн-зоны СВОЕЙ команды. */
    public static boolean inOwnMain(ServerPlayer p, WarfareWorldData data) {
        String team = teamOf(p);
        if (team.isEmpty()) return false;
        for (WarfareWorldData.MainProtectionZone z : data.structures.mainZones) {
            if (z.team.equalsIgnoreCase(team) && z.isInside(p.position())) return true;
        }
        return false;
    }

    private static boolean hasZoneFor(String team, WarfareWorldData data) {
        for (WarfareWorldData.MainProtectionZone z : data.structures.mainZones) {
            if (z.team.equalsIgnoreCase(team)) return true;
        }
        return false;
    }

    private static boolean downed(ServerPlayer p) {
        return WarfareData.of(p).getBoolean(WarfareKeys.IS_DOWNED);
    }

    // ───────────────────────────── команда ─────────────────────────────

    private static int[] counts(ServerPlayer except) {
        int blue = 0, red = 0;
        for (ServerPlayer p : except.server.getPlayerList().getPlayers()) {
            if (p == except) continue;
            String t = teamOf(p);
            if (TeamBalance.BLUE.equalsIgnoreCase(t)) blue++;
            else if (TeamBalance.RED.equalsIgnoreCase(t)) red++;
        }
        return new int[] {blue, red};
    }

    /** Матч на этом сервере настроен (есть спавн мейна) — иначе (лобби, пустой мир) команду не даём. */
    public static boolean matchConfigured(ServerLevel level, WarfareWorldData data) {
        String dim = level.dimension().location().toString();
        return data.match.blueSpawns.containsKey(dim) || data.match.redSpawns.containsKey(dim);
    }

    /** Вход без команды: автоматически в меньшую (решение 5). Креатив/наблюдатель — не трогаем. */
    public static void autoAssign(ServerPlayer player) {
        if (!teamOf(player).isEmpty() || player.isCreative() || player.isSpectator()) return;
        WarfareWorldData data = WarfareWorldData.get(player.serverLevel());
        if (!matchConfigured(player.serverLevel(), data)) return;
        int[] c = counts(player);
        String team = TeamBalance.smaller(c[0], c[1]);
        applyTeam(player, data, team);
        player.sendSystemMessage(
                Component.translatable("pwpwarfare.team.auto_assigned", teamLabel(team, data), PacketKeys.squadMenu())
                        .withStyle(ChatFormatting.GOLD));
    }

    /**
     * Выбор команды из экрана/меню K. Возвращает ключ результата ({@code RESULT_*}); игроку уходит
     * {@link StagingPackets.TeamResult}. Своя же команда — ничего не меняется (раньше чистило кит).
     */
    public static String selectTeam(ServerPlayer player, String requested) {
        WarfareWorldData data = WarfareWorldData.get(player.serverLevel());
        int[] c = counts(player);
        String target = TeamBalance.resolve(requested, c[0], c[1]);
        String result;
        if (target == null) {
            result = RESULT_BAD;
        } else if (target.equalsIgnoreCase(teamOf(player))) {
            result = RESULT_SAME;
        } else if (!TeamBalance.canJoin(target, c[0], c[1])) {
            result = RESULT_FULL;
        } else {
            FactionDataLoader.cancelDeploy(player, "Команда изменена — деплой отменён");
            SquadActions.leaveCurrentSquad(player, data);
            WarfareData.of(player).putString(WarfareKeys.CURRENT_KIT, "Unassigned");
            WarfareData.of(player).remove(WarfareKeys.PENDING_KIT);
            player.getInventory().clearContent();
            ResupplyHandler.clearCurios(player);
            player.inventoryMenu.broadcastChanges();
            applyTeam(player, data, target);
            result = RESULT_JOINED;
        }
        String shown = target != null ? target : teamOf(player);
        PacketHandler.INSTANCE.send(
                PacketDistributor.PLAYER.with(() -> player),
                new StagingPackets.TeamResult(
                        !RESULT_FULL.equals(result) && !RESULT_BAD.equals(result), result, shown));
        if (RESULT_JOINED.equals(result)) {
            player.sendSystemMessage(Component.translatable(result, teamLabel(target, data))
                    .withStyle(TeamBalance.BLUE.equals(target) ? ChatFormatting.BLUE : ChatFormatting.RED));
        }
        return result;
    }

    private static Component teamLabel(String team, WarfareWorldData data) {
        String faction = TeamBalance.BLUE.equalsIgnoreCase(team) ? data.match.blueFaction : data.match.redFaction;
        String name = faction == null || faction.isEmpty() || "none".equals(faction)
                ? team
                : faction.replace('_', ' ').toUpperCase();
        return Component.literal(name)
                .withStyle(TeamBalance.BLUE.equalsIgnoreCase(team) ? ChatFormatting.BLUE : ChatFormatting.RED);
    }

    /** Записать в команду scoreboard, режим, синхронизация и телепорт в мейн (в ожидании — тоже). */
    private static void applyTeam(ServerPlayer player, WarfareWorldData data, String target) {
        data.setDirty();
        Scoreboard scoreboard = player.server.getScoreboard();
        PlayerTeam team = scoreboard.getPlayerTeam(target);
        if (team == null) {
            team = scoreboard.addPlayerTeam(target);
            team.setColor(TeamBalance.BLUE.equals(target) ? ChatFormatting.BLUE : ChatFormatting.RED);
            team.setSeeFriendlyInvisibles(true);
        }
        scoreboard.addPlayerToTeam(player.getScoreboardName(), team);
        player.setGameMode(GameType.SURVIVAL);
        PacketHandler.broadcastPlayerSkin(player);
        PacketHandler.sendToAllClients(player.serverLevel(), data);
        PacketHandler.syncSquadsAll(data.squadRegistry.squads);
        String dim = player.level().dimension().location().toString();
        BlockPos spawn =
                TeamBalance.BLUE.equals(target) ? data.match.blueSpawns.get(dim) : data.match.redSpawns.get(dim);
        if (spawn == null) {
            player.sendSystemMessage(
                    Component.translatable("pwpwarfare.main.no_spawn").withStyle(ChatFormatting.RED));
            return;
        }
        if (data.match.isGameStarted || isStaging(data)) {
            player.stopRiding();
            player.teleportTo(spawn.getX() + 0.5, spawn.getY(), spawn.getZ() + 0.5);
            Component msg = isStaging(data) && data.match.waitingActive
                    ? Component.translatable("pwpwarfare.main.arrived_staging", formatTime(data.match.waitingTimer))
                    : Component.translatable("pwpwarfare.main.arrived");
            player.displayClientMessage(msg.copy().withStyle(ChatFormatting.YELLOW), true);
        }
        StagingPackets.sendState(player, data);
    }

    // ───────────────────────────── кит ─────────────────────────────

    /** Кит сразу при выборе роли (решение 1): в стейджинге, живой, в своём мейне. */
    public static boolean issueOnSelect(ServerPlayer player, WarfareWorldData data) {
        if (!isStaging(data) || player.isSpectator() || !player.isAlive() || downed(player)) return false;
        String team = teamOf(player);
        if (team.isEmpty() || (hasZoneFor(team, data) && !inOwnMain(player, data))) return false;
        ResupplyHandler.tryApplyPendingKit(player, data);
        return true;
    }

    /**
     * «ВЗЯТЬ СНАРЯЖЕНИЕ» (решение 3): живой в своём мейне получает выбранную роль через
     * {@link ResupplyHandler#tryApplyPendingKit}/{@link ResupplyHandler#applyCurrentKit} (проверка —
     * KitAccess внутри). Кулдаун как у склада. Это не деплой — SpawnRules не участвуют.
     */
    public static void takeKit(ServerPlayer player) {
        WarfareWorldData data = WarfareWorldData.get(player.serverLevel());
        String err = takeKitDenied(player, data);
        if (err == null) {
            long now = GameClock.worldTicks(player.level());
            long last = WarfareData.of(player).getLong(WarfareKeys.LAST_MAIN_RESUPPLY);
            if (!player.isCreative() && !isStaging(data) && now < last + TAKE_KIT_COOLDOWN_TICKS) {
                player.displayClientMessage(
                        Component.translatable(
                                        "pwpwarfare.take_kit.cooldown", (last + TAKE_KIT_COOLDOWN_TICKS - now) / 20L)
                                .withStyle(ChatFormatting.RED),
                        true);
                return;
            }
            if (WarfareData.of(player).contains(WarfareKeys.PENDING_KIT)) {
                ResupplyHandler.tryApplyPendingKit(player, data);
            } else {
                ResupplyHandler.applyCurrentKit(player, data);
            }
            WarfareData.of(player).putLong(WarfareKeys.LAST_MAIN_RESUPPLY, now);
            player.displayClientMessage(
                    Component.translatable("pwpwarfare.take_kit.done").withStyle(ChatFormatting.GREEN), true);
            return;
        }
        player.displayClientMessage(Component.translatable(err).withStyle(ChatFormatting.RED), true);
    }

    /** null — можно взять кит, иначе lang-ключ причины. */
    static String takeKitDenied(ServerPlayer player, WarfareWorldData data) {
        String team = teamOf(player);
        if (team.isEmpty()) return "pwpwarfare.take_kit.no_team";
        if (player.isSpectator() || !player.isAlive() || downed(player)) return "pwpwarfare.take_kit.dead";
        if (!inOwnMain(player, data)) return "pwpwarfare.take_kit.not_in_main";
        if (data.match.invasionSetupActive && !team.equalsIgnoreCase(data.match.invasionDefender)) {
            return "pwpwarfare.take_kit.setup";
        }
        String pending = WarfareData.of(player).getString(WarfareKeys.PENDING_KIT);
        String current = WarfareData.of(player).getString(WarfareKeys.CURRENT_KIT);
        boolean hasRole = !pending.isEmpty() || (!current.isEmpty() && !"Unassigned".equals(current));
        return hasRole ? null : "pwpwarfare.take_kit.no_role";
    }

    // ───────────────────────────── техника ─────────────────────────────

    /** Начало ожидания: сбросить флаг техники этого матча. */
    public static void onWaitingStart(ServerLevel level, WarfareWorldData data) {
        vehiclesFilled = false;
        lastInside.clear();
        tickVehicles(level, data);
    }

    /** Раз в секунду ожидания: выставить технику, как только пулы фракций загружены (БД асинхронно). */
    public static void tickVehicles(ServerLevel level, WarfareWorldData data) {
        if (vehiclesFilled) return;
        if (data.vehicles.blueFactionVehicles.isEmpty() && data.vehicles.redFactionVehicles.isEmpty()) return;
        data.fillVehicleSpawnersFromFactionDefaults(level);
        data.setDirty();
        vehiclesFilled = true;
    }

    /** GO: техника уже стоит с ожидания — повторно не заполняем (только догоняем незагруженные чанки). */
    public static void onGo(ServerLevel level, WarfareWorldData data) {
        if (vehiclesFilled) data.retryPendingVehicleSpawners(level);
        else data.fillVehicleSpawnersFromFactionDefaults(level);
        vehiclesFilled = false;
        lastInside.clear();
    }

    /** Игрок внутри своего мейна в технике — запомнить её позицию (точка возврата). */
    public static void rememberInside(ServerPlayer player) {
        Entity root = player.getRootVehicle();
        if (root != player) lastInside.put(root.getUUID(), root.position());
    }

    /**
     * Игрок в технике выехал из мейна до GO: всех высадить, технику вернуть на последнюю позицию
     * внутри мейна (если известна). Игроков дальше телепортирует удержание мейна.
     */
    public static void holdVehicle(ServerPlayer player) {
        Entity root = player.getRootVehicle();
        if (root == player) return;
        root.ejectPassengers();
        player.stopRiding();
        Vec3 back = lastInside.get(root.getUUID());
        if (back != null) {
            root.setDeltaMovement(Vec3.ZERO);
            root.teleportTo(back.x, back.y, back.z);
        }
    }

    public static String formatTime(int secs) {
        int s = Math.max(0, secs);
        return String.format("%d:%02d", s / 60, s % 60);
    }

    /** Клавиши для текста сервера: клиент подставит свои через keybind-компонент. */
    static final class PacketKeys {
        private PacketKeys() {}

        static Component squadMenu() {
            return Component.keybind("key.pwpwarfare.open_squad_menu");
        }
    }
}
