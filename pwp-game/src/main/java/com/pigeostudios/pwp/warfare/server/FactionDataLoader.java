package com.pigeostudios.pwp.warfare.server;

import com.google.gson.JsonObject;
import com.pigeostudios.pwp.warfare.data.state.WarfareData;
import com.pigeostudios.pwp.warfare.data.state.WarfareKeys;
import com.pigeostudios.pwp.warfare.network.PacketHandler;
import com.pigeostudios.pwp.warfare.network.PacketRespawnRequest;
import com.pigeostudios.pwp.warfare.world.WarfareWorldData;
import com.pwp.api.core.CoreApi;
import com.pwp.api.time.GameClock;
import com.pwp.api.util.Backoff;
import com.pwp.api.util.ServerThread;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Асинхронная загрузка китов и пулов техники фракций из core-service на старте матча.
 *
 * <p>Пока киты команды не загружены, деплой игроков этой команды ждёт: запрос запоминается,
 * игрок видит «Загрузка китов…», после загрузки запрос выполняется автоматически. Ошибка
 * загрузки (core-service недоступен) логируется, попытка повторяется через {@link #RETRY_TICKS}.
 *
 * <p>Если загрузка не запускалась (лобби, мир без core-service) — киты считаются готовыми
 * (используются NBT-киты мира, как раньше).
 */
@Mod.EventBusSubscriber(modid = "pwpwarfare", bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class FactionDataLoader {
    private static final Logger LOG = LoggerFactory.getLogger(FactionDataLoader.class);

    /** Первая пауза между повторами загрузки: 10 с; дальше удваивается (Backoff) до {@link #RETRY_MAX_TICKS}. */
    static final int RETRY_TICKS = 200;
    /** Потолок паузы: 5 мин (core-service выключен на тест-стенде — не спамить лог и сеть). */
    static final int RETRY_MAX_TICKS = 6000;

    public enum Team {
        BLUE,
        RED
    }

    private enum Kind {
        KITS,
        VEHICLES
    }

    /** Состояние загрузки одного ресурса одной команды (главный поток). */
    private static final class Slot {
        final String faction;
        boolean ready;
        boolean inFlight;
        long nextAttemptTick;
        int attempts;

        Slot(String faction) {
            this.faction = faction;
        }
    }

    private static final Map<Team, Map<Kind, Slot>> SLOTS = new EnumMap<>(Team.class);
    /** Отложенные деплои: игрок → снимок состояния в момент отложения. */
    private static final Map<UUID, DeferredDeploy> PENDING_DEPLOYS = new LinkedHashMap<>();

    private static ServerLevel level;

    private FactionDataLoader() {}

    /** Запускает загрузку для обеих команд (вызывается после применения map_config). */
    public static void start(ServerLevel serverLevel, WarfareWorldData data) {
        level = serverLevel;
        SLOTS.clear();
        // Ожидавшие деплоя прошлого матча — деплой не выполнится сам, игрок подтверждает заново
        for (UUID id : PENDING_DEPLOYS.keySet()) {
            ServerPlayer p = serverLevel.getServer().getPlayerList().getPlayer(id);
            if (p != null && !p.hasDisconnected()) {
                p.displayClientMessage(
                        Component.translatable("pwpwarfare.msg.deploy.match_restarted")
                                .withStyle(ChatFormatting.YELLOW),
                        true);
            }
        }
        PENDING_DEPLOYS.clear();
        put(Team.BLUE, data.match.blueFaction);
        put(Team.RED, data.match.redFaction);
        long now = GameClock.serverTicks(serverLevel.getServer());
        SLOTS.values().forEach(m -> m.values().forEach(s -> s.nextAttemptTick = now));
        tryLoads(serverLevel.getServer());
    }

    private static void put(Team team, String faction) {
        if (faction == null || faction.isEmpty() || faction.equals("none")) return;
        Map<Kind, Slot> m = new EnumMap<>(Kind.class);
        m.put(Kind.KITS, new Slot(faction));
        m.put(Kind.VEHICLES, new Slot(faction));
        SLOTS.put(team, m);
    }

    /** Состояние загрузки для админ-панели: команда → kits/vehicles → {faction, ready, attempts, inFlight}. */
    public static com.google.gson.JsonObject statusJson() {
        com.google.gson.JsonObject o = new com.google.gson.JsonObject();
        SLOTS.forEach((team, m) -> {
            com.google.gson.JsonObject t = new com.google.gson.JsonObject();
            m.forEach((kind, s) -> {
                com.google.gson.JsonObject e = new com.google.gson.JsonObject();
                e.addProperty("faction", s.faction);
                e.addProperty("ready", s.ready);
                e.addProperty("attempts", s.attempts);
                e.addProperty("inFlight", s.inFlight);
                t.add(kind.name().toLowerCase(java.util.Locale.ROOT), e);
            });
            o.add(team.name(), t);
        });
        return o;
    }

    /** true — киты команды можно использовать (загружены или загрузка не требуется). */
    public static boolean kitsReady(String team) {
        Team t = parse(team);
        if (t == null) return true;
        Map<Kind, Slot> m = SLOTS.get(t);
        return m == null || m.get(Kind.KITS).ready;
    }

    /**
     * Ставит деплой в очередь до загрузки китов (повторный запрос перезаписывает). Снимок
     * команды/режима/кита сверяется при выполнении — см. {@link DeferredDeploy#cancelReason}.
     */
    public static void deferDeploy(ServerPlayer player, String spawnType) {
        PENDING_DEPLOYS.put(player.getUUID(), snapshot(player, spawnType));
        player.displayClientMessage(
                Component.translatable("pwpwarfare.msg.deploy.loading_kits").withStyle(ChatFormatting.YELLOW), true);
    }

    /** Отменить ожидание (смена кита/команды). Нет ожидания — ничего не делает. */
    public static void cancelDeploy(ServerPlayer player, String reason) {
        if (PENDING_DEPLOYS.remove(player.getUUID()) != null) {
            player.displayClientMessage(Component.translatable(reason).withStyle(ChatFormatting.YELLOW), true);
        }
    }

    private static DeferredDeploy snapshot(ServerPlayer player, String spawnType) {
        String kit = WarfareData.of(player).getString(WarfareKeys.PENDING_KIT);
        if (kit.isEmpty()) kit = WarfareData.of(player).getString(WarfareKeys.CURRENT_KIT);
        return new DeferredDeploy(
                spawnType, teamOf(player), player.gameMode.getGameModeForPlayer() == GameType.SPECTATOR, kit);
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || SLOTS.isEmpty()) return;
        MinecraftServer server = event.getServer();
        if (GameClock.serverTicks(server) % 20 == 0) tryLoads(server);
    }

    /**
     * Сервер остановлен (одиночная игра: выход в меню и повторное открытие мира — тот же JVM):
     * статическое состояние не должно пережить сервер — иначе ссылка на выгруженный мир и
     * «готовые» слоты прошлого мира.
     */
    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        level = null;
        SLOTS.clear();
        PENDING_DEPLOYS.clear();
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        PENDING_DEPLOYS.remove(event.getEntity().getUUID());
        PacketRespawnRequest.forget(event.getEntity().getUUID());
    }

    private static void tryLoads(MinecraftServer server) {
        if (level == null) return;
        long now = GameClock.serverTicks(server);
        for (Map.Entry<Team, Map<Kind, Slot>> e : SLOTS.entrySet()) {
            for (Map.Entry<Kind, Slot> ks : e.getValue().entrySet()) {
                Slot slot = ks.getValue();
                if (slot.ready || slot.inFlight || now < slot.nextAttemptTick) continue;
                load(server, e.getKey(), ks.getKey(), slot);
            }
        }
    }

    private static void load(MinecraftServer server, Team team, Kind kind, Slot slot) {
        slot.inFlight = true;
        slot.attempts++;
        // Пул техники и дивизии фракции (этап 6 ROADMAP) — одним результатом: техника выставляется сразу по
        // дивизии. Дивизии не загрузились (старый core-service, ошибка) — общий пул, как раньше.
        CompletableFuture<JsonObject> call = kind == Kind.KITS
                ? CoreApi.getFactionKits(slot.faction)
                : CoreApi.getFactionVehicles(slot.faction)
                        .thenCompose(veh -> veh == null
                                ? CompletableFuture.completedFuture(null)
                                : CoreApi.getFactionDivisions(slot.faction).handle((div, err) -> {
                                    if (div != null
                                            && div.has("data")
                                            && div.get("data").isJsonArray()) {
                                        veh.add("divisions", div.getAsJsonArray("data"));
                                    }
                                    return veh;
                                }));
        ServerLevel target = level;
        ServerThread.thenOnServer(call, server, response -> {
            slot.inFlight = false;
            // Матч перезапущен, пока шёл запрос — ответ устарел
            if (SLOTS.get(team) == null || SLOTS.get(team).get(kind) != slot) return;
            if (response == null
                    || !response.has("data")
                    || !response.get("data").isJsonArray()) {
                long delay = Backoff.delay(slot.attempts, RETRY_TICKS, RETRY_MAX_TICKS);
                slot.nextAttemptTick = GameClock.serverTicks(server) + delay;
                // громко — только на попытках 1, 2, 4, 8…; остальные — debug (раньше ERROR каждые 10 с)
                if (Backoff.loud(slot.attempts)) {
                    LOG.warn(
                            "{} фракции {} ({}) не загружены из core-service (попытка {}), повтор через {} с",
                            kind,
                            slot.faction,
                            team,
                            slot.attempts,
                            delay / 20);
                } else {
                    LOG.debug("{} фракции {} ({}): попытка {} неудачна", kind, slot.faction, team, slot.attempts);
                }
                return;
            }
            WarfareWorldData current = WarfareWorldData.get(target);
            if (kind == Kind.KITS) {
                current.loadKitsFromApi(team.name(), response);
            } else {
                current.loadFactionVehiclesFromApi(team.name(), response);
                current.applyDivisions(
                        team.name(),
                        response.has("divisions") && response.get("divisions").isJsonArray()
                                ? response.getAsJsonArray("divisions")
                                : new com.google.gson.JsonArray());
            }
            current.setDirty();
            PacketHandler.sendToAllClients(target, current);
            slot.ready = true;
            LOG.info(
                    "{} фракции {} загружены для команды {}: {} шт.",
                    kind,
                    slot.faction,
                    team,
                    response.getAsJsonArray("data").size());
            if (kind == Kind.KITS) flushDeploys(server, team);
        });
    }

    /**
     * Выполняет отложенные деплои игроков команды, чьи киты только что загрузились. Игрок
     * ищется заново по UUID (офлайн — пропуск), состояние сверяется со снимком; сам спавн идёт
     * обычным путём {@link PacketRespawnRequest#process} — лимиты ролей/FIRE_SUPPORT и нок
     * проверяются там заново, кит берётся текущий.
     */
    private static void flushDeploys(MinecraftServer server, Team team) {
        var it = PENDING_DEPLOYS.entrySet().iterator();
        while (it.hasNext()) {
            var e = it.next();
            ServerPlayer p = server.getPlayerList().getPlayer(e.getKey());
            if (p == null || p.hasDisconnected()) {
                it.remove();
                continue;
            }
            DeferredDeploy waiting = e.getValue();
            if (parse(waiting.team()) != team) continue;
            it.remove();
            String reason = waiting.cancelReason(snapshot(p, waiting.spawnType()));
            if (reason != null) {
                p.displayClientMessage(Component.translatable(reason).withStyle(ChatFormatting.YELLOW), true);
                continue;
            }
            PacketRespawnRequest.process(p, waiting.spawnType());
        }
    }

    /** Команда игрока в формате BLUE/RED/NEUTRAL (как в PacketRespawnRequest). */
    public static String teamOf(ServerPlayer player) {
        return com.pigeostudios.pwp.warfare.util.Teams.orNeutral(player);
    }

    private static Team parse(String team) {
        if ("BLUE".equalsIgnoreCase(team)) return Team.BLUE;
        if ("RED".equalsIgnoreCase(team)) return Team.RED;
        return null;
    }
}
