package com.pigeostudios.pwp.warfare.network;

import com.pigeostudios.pwp.warfare.config.WarfareConfig;
import com.pigeostudios.pwp.warfare.data.state.WarfareData;
import com.pigeostudios.pwp.warfare.data.state.WarfareKeys;
import com.pigeostudios.pwp.warfare.match.TeamVisibility;
import com.pigeostudios.pwp.warfare.world.WarfareWorldData;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.Packet;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.scores.Team;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;
import net.minecraftforge.server.ServerLifecycleHooks;

// Центральный регистратор сетевых пакетов мода
// Содержит канал связи и методы отправки данных всем клиентам
public class PacketHandler {
    /**
     * Версия протокола канала. Поднимать при любом изменении состава/порядка регистраций.
     * 2 — удалён PacketApiAction (выдача любого скина по запросу клиента).
     * 3 — этап 2: запросы данных — единая система core-client (удалены PacketRequestData/
     * PacketSendData), действия отряда — по пакету на действие (network/squad).
     * 4 — PacketRespawnRequest/PacketSelectKit переведены на сеть v2 (старые регистрации удалены,
     * новые — в конце списка).
     */
    private static final String PROTOCOL_VERSION = "8";

    public static final SimpleChannel INSTANCE = NetworkRegistry.newSimpleChannel(
            new ResourceLocation("pwpwarfare:main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals);

    // Регистрирует все пакеты мода с их кодеками и обработчиками
    public static void register() {
        int id = 0;
        INSTANCE.registerMessage(
                id++,
                PacketSyncGameData.class,
                PacketSyncGameData::encode,
                PacketSyncGameData::decode,
                PacketSyncGameData::handle);
        INSTANCE.registerMessage(
                id++, PacketSyncPoint.class, PacketSyncPoint::encode, PacketSyncPoint::decode, PacketSyncPoint::handle);
        INSTANCE.registerMessage(
                id++, PacketDebugFill.class, PacketDebugFill::encode, PacketDebugFill::decode, PacketDebugFill::handle);
        INSTANCE.registerMessage(
                id++,
                PacketApplyMarker.class,
                PacketApplyMarker::encode,
                PacketApplyMarker::decode,
                PacketApplyMarker::handle);
        INSTANCE.registerMessage(
                id++,
                PacketSpawnGhost.class,
                PacketSpawnGhost::encode,
                PacketSpawnGhost::decode,
                PacketSpawnGhost::handle);
        INSTANCE.registerMessage(
                id++,
                PacketBuildRequest.class,
                PacketBuildRequest::encode,
                PacketBuildRequest::decode,
                PacketBuildRequest::handle);
        INSTANCE.registerMessage(
                id++, PacketToggleAim.class, PacketToggleAim::encode, PacketToggleAim::decode, PacketToggleAim::handle);
        INSTANCE.registerMessage(
                id++,
                PacketVehicleShoot.class,
                PacketVehicleShoot::encode,
                PacketVehicleShoot::decode,
                PacketVehicleShoot::handle);
        INSTANCE.registerMessage(
                id++,
                PacketRequestAmmo.class,
                PacketRequestAmmo::encode,
                PacketRequestAmmo::decode,
                PacketRequestAmmo::handle);
        INSTANCE.registerMessage(
                id++, PacketDropCrate.class, PacketDropCrate::encode, PacketDropCrate::decode, PacketDropCrate::handle);
        INSTANCE.registerMessage(
                id++,
                PacketUpdateSpawner.class,
                PacketUpdateSpawner::encode,
                PacketUpdateSpawner::decode,
                PacketUpdateSpawner::handle);
        INSTANCE.registerMessage(
                id++, PacketRecoil.class, PacketRecoil::encode, PacketRecoil::decode, PacketRecoil::handle);
        INSTANCE.registerMessage(
                id++,
                PacketSyncSquads.class,
                PacketSyncSquads::encode,
                PacketSyncSquads::new,
                PacketSyncSquads::handle);
        INSTANCE.registerMessage(
                id++,
                PacketTeamSelect.class,
                PacketTeamSelect::encode,
                PacketTeamSelect::decode,
                PacketTeamSelect::handle);
        INSTANCE.registerMessage(
                id++, PacketSquadChat.class, PacketSquadChat::encode, PacketSquadChat::decode, PacketSquadChat::handle);
        INSTANCE.registerMessage(
                id++,
                PacketSyncMapPlayers.class,
                PacketSyncMapPlayers::encode,
                PacketSyncMapPlayers::decode,
                PacketSyncMapPlayers::handle);
        INSTANCE.registerMessage(
                id++,
                PacketOpenKitEditor.class,
                PacketOpenKitEditor::encode,
                PacketOpenKitEditor::decode,
                PacketOpenKitEditor::handle);
        INSTANCE.registerMessage(
                id++, PacketSaveKit.class, PacketSaveKit::encode, PacketSaveKit::decode, PacketSaveKit::handle);
        INSTANCE.registerMessage(
                id++,
                PacketRequestKitMenu.class,
                PacketRequestKitMenu::encode,
                PacketRequestKitMenu::decode,
                PacketRequestKitMenu::handle);
        INSTANCE.registerMessage(
                id++,
                PacketOpenPlayerKitMenu.class,
                PacketOpenPlayerKitMenu::encode,
                PacketOpenPlayerKitMenu::decode,
                PacketOpenPlayerKitMenu::handle);
        INSTANCE.registerMessage(
                id++,
                PacketRequestCrateAmmo.class,
                PacketRequestCrateAmmo::encode,
                PacketRequestCrateAmmo::decode,
                PacketRequestCrateAmmo::handle);
        INSTANCE.registerMessage(
                id++,
                PacketRequestKitData.class,
                PacketRequestKitData::encode,
                PacketRequestKitData::decode,
                PacketRequestKitData::handle);
        INSTANCE.registerMessage(
                id++,
                PacketSendKitData.class,
                PacketSendKitData::encode,
                PacketSendKitData::decode,
                PacketSendKitData::handle);
        INSTANCE.registerMessage(
                id++, PacketPasteKit.class, PacketPasteKit::encode, PacketPasteKit::decode, PacketPasteKit::handle);
        INSTANCE.registerMessage(
                id++, PacketPasteTeam.class, PacketPasteTeam::encode, PacketPasteTeam::decode, PacketPasteTeam::handle);
        INSTANCE.registerMessage(
                id++,
                PacketDownedAction.class,
                PacketDownedAction::encode,
                PacketDownedAction::decode,
                PacketDownedAction::handle);
        INSTANCE.registerMessage(
                id++,
                PacketSyncDownedState.class,
                PacketSyncDownedState::encode,
                PacketSyncDownedState::decode,
                PacketSyncDownedState::handle);
        INSTANCE.registerMessage(
                id++,
                PacketSquadMarker.class,
                PacketSquadMarker::encode,
                PacketSquadMarker::decode,
                PacketSquadMarker::handle);
        INSTANCE.registerMessage(
                id++,
                PacketPlaceMapMarker.class,
                PacketPlaceMapMarker::encode,
                PacketPlaceMapMarker::decode,
                PacketPlaceMapMarker::handle);
        INSTANCE.registerMessage(
                id++,
                PacketRadioAction.class,
                PacketRadioAction::encode,
                PacketRadioAction::decode,
                PacketRadioAction::handle);
        INSTANCE.registerMessage(
                id++,
                PacketVoteAction.class,
                PacketVoteAction::encode,
                PacketVoteAction::decode,
                PacketVoteAction::handle);
        INSTANCE.registerMessage(
                id++,
                PacketCaptureNotification.class,
                PacketCaptureNotification::encode,
                PacketCaptureNotification::decode,
                PacketCaptureNotification::handle);
        INSTANCE.registerMessage(
                id++, PacketPlacePing.class, PacketPlacePing::encode, PacketPlacePing::decode, PacketPlacePing::handle);
        INSTANCE.registerMessage(
                id++,
                PacketRequestCMD.class,
                PacketRequestCMD::encode,
                PacketRequestCMD::decode,
                PacketRequestCMD::handle);
        INSTANCE.registerMessage(
                id++, PacketCMDVote.class, PacketCMDVote::encode, PacketCMDVote::decode, PacketCMDVote::handle);
        INSTANCE.registerMessage(
                id++,
                PacketConfirmArtStrike.class,
                PacketConfirmArtStrike::encode,
                PacketConfirmArtStrike::decode,
                PacketConfirmArtStrike::handle);
        INSTANCE.registerMessage(
                id++, PacketSyncMyKit.class, PacketSyncMyKit::encode, PacketSyncMyKit::decode, PacketSyncMyKit::handle);
        INSTANCE.registerMessage(
                id++,
                PacketOpenFactionKitEditor.class,
                PacketOpenFactionKitEditor::encode,
                PacketOpenFactionKitEditor::decode,
                PacketOpenFactionKitEditor::handle);
        INSTANCE.registerMessage(
                id++,
                PacketSaveFactionKit.class,
                PacketSaveFactionKit::encode,
                PacketSaveFactionKit::decode,
                PacketSaveFactionKit::handle);
        INSTANCE.registerMessage(
                id++,
                PacketSaveFactionVehicle.class,
                PacketSaveFactionVehicle::encode,
                PacketSaveFactionVehicle::decode,
                PacketSaveFactionVehicle::handle);
        INSTANCE.registerMessage(
                id++,
                PacketVoiceActivity.class,
                PacketVoiceActivity::encode,
                PacketVoiceActivity::decode,
                PacketVoiceActivity::handle);
        INSTANCE.registerMessage(
                id++,
                PacketRadioVoiceActivity.class,
                PacketRadioVoiceActivity::encode,
                PacketRadioVoiceActivity::decode,
                PacketRadioVoiceActivity::handle);
        INSTANCE.registerMessage(
                id++,
                PacketVoiceChannelState.class,
                PacketVoiceChannelState::encode,
                PacketVoiceChannelState::decode,
                PacketVoiceChannelState::handle);
        INSTANCE.registerMessage(
                id++,
                PacketOpenVictoryScreen.class,
                PacketOpenVictoryScreen::encode,
                PacketOpenVictoryScreen::decode,
                PacketOpenVictoryScreen::handle);
        INSTANCE.registerMessage(
                id++,
                PacketOpenSkinInventory.class,
                PacketOpenSkinInventory::encode,
                PacketOpenSkinInventory::decode,
                PacketOpenSkinInventory::handle);
        INSTANCE.registerMessage(
                id++,
                PacketSyncPlayerSkin.class,
                PacketSyncPlayerSkin::encode,
                PacketSyncPlayerSkin::decode,
                PacketSyncPlayerSkin::handle);
        INSTANCE.registerMessage(
                id++,
                PacketVehicleDriveRequest.class,
                PacketVehicleDriveRequest::encode,
                PacketVehicleDriveRequest::decode,
                PacketVehicleDriveRequest::handle);
        INSTANCE.registerMessage(
                id++,
                PacketVehicleDriveAnswer.class,
                PacketVehicleDriveAnswer::encode,
                PacketVehicleDriveAnswer::decode,
                PacketVehicleDriveAnswer::handle);
        INSTANCE.registerMessage(
                id++,
                PacketPlaceMarker.class,
                PacketPlaceMarker::encode,
                PacketPlaceMarker::decode,
                PacketPlaceMarker::handle);
        INSTANCE.registerMessage(
                id++,
                PacketSyncMarker.class,
                PacketSyncMarker::encode,
                PacketSyncMarker::decode,
                PacketSyncMarker::handle);
        INSTANCE.registerMessage(
                id++,
                PacketRemoveMarker.class,
                PacketRemoveMarker::encode,
                PacketRemoveMarker::decode,
                PacketRemoveMarker::handle);
        INSTANCE.registerMessage(
                id++,
                PacketSyncSpawners.class,
                PacketSyncSpawners::encode,
                PacketSyncSpawners::decode,
                PacketSyncSpawners::handle);
        INSTANCE.registerMessage(
                id++, PacketPlacePath.class, PacketPlacePath::encode, PacketPlacePath::decode, PacketPlacePath::handle);
        INSTANCE.registerMessage(
                id++, PacketSyncPath.class, PacketSyncPath::encode, PacketSyncPath::decode, PacketSyncPath::handle);
        INSTANCE.registerMessage(
                id++,
                PacketRemovePath.class,
                PacketRemovePath::encode,
                PacketRemovePath::decode,
                PacketRemovePath::handle);
        INSTANCE.registerMessage(
                id++,
                PacketMainZoneState.class,
                PacketMainZoneState::encode,
                PacketMainZoneState::decode,
                PacketMainZoneState::handle);
        INSTANCE.registerMessage(
                id++,
                PacketNotification.class,
                PacketNotification::encode,
                PacketNotification::decode,
                PacketNotification::handle);
        // Регистрируется ПОСЛЕДНИМ, чтобы не сдвигать индексы существующих пакетов
        // (клиенты со старым jar остаются совместимы до обновления).
        INSTANCE.registerMessage(
                id++,
                PacketVehicleStatus.class,
                PacketVehicleStatus::encode,
                PacketVehicleStatus::decode,
                PacketVehicleStatus::handle);
        // Тоже в конец: деплой дронов (сервер -> клиент).
        INSTANCE.registerMessage(
                id++,
                PacketDroneDeployState.class,
                PacketDroneDeployState::encode,
                PacketDroneDeployState::decode,
                PacketDroneDeployState::handle);
        // Сеть v2 (record-DTO, проверка на сервере). Новые пакеты — только в конец.
        com.pwp.api.net.Net.Registrar v2 = new com.pwp.api.net.Net.Registrar(INSTANCE, id);
        com.pigeostudios.pwp.warfare.squad.SquadPackets.register(v2);
        v2.c2s(PacketRespawnRequest.class, PacketRespawnRequest::encode, PacketRespawnRequest::decode)
                .c2s(PacketSelectKit.class, PacketSelectKit::encode, PacketSelectKit::decode);
        // этап 2: админ-панель, вкладка «Матч» (только в конец)
        com.pigeostudios.pwp.warfare.admin.MatchAdminPackets.register(v2);
        // этап 3: редактор карты (только в конец)
        com.pigeostudios.pwp.warfare.admin.MapEditorPackets.register(v2);
        // этап 4: стейджинг — состояние ожидания, ответ выбора команды, «взять снаряжение», быстрый отряд
        com.pigeostudios.pwp.warfare.staging.StagingPackets.register(v2);
        // награды за матч на экран победы (только в конец)
        com.pigeostudios.pwp.warfare.stats.RewardPackets.register(v2);
        // 19.11.2026 (протокол 7): админ-панель — действия над игроком, матч (рестарт/победа/перезагрузка)
        com.pigeostudios.pwp.warfare.admin.PlayerAdminPackets.register(v2);
        com.pigeostudios.pwp.warfare.admin.MatchAdminPackets.registerV7(v2);
        // 8 (25.11.2026): дивизия команды в админ-панели
        com.pigeostudios.pwp.warfare.admin.MatchAdminPackets.registerV8(v2);
        // 8 (25.11.2026, pwp-game): лимиты (бывший канал pwplimit:network) — в конец
        id = com.pigeostudios.pwp.limit.network.NetworkHandler.register(INSTANCE, v2.nextId());
    }

    private static String getFactionName(String currentFaction, boolean isBlue) {
        if (currentFaction != null
                && !currentFaction.equals("none")
                && !currentFaction.equals("bluefor")
                && !currentFaction.equals("redfor")) {
            return currentFaction.toUpperCase();
        } else {
            return isBlue
                    ? (String) WarfareConfig.BLUE_TEAM_CUSTOM_NAME.get()
                    : (String) WarfareConfig.RED_TEAM_CUSTOM_NAME.get();
        }
    }

    private static Map<String, String> getPlayerKitsMap() {
        Map<String, String> pKits = new HashMap<>();
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server != null) {
            for (ServerPlayer p : server.getPlayerList().getPlayers()) {
                String current = WarfareData.of(p).getString(WarfareKeys.CURRENT_KIT);
                String pending = WarfareData.of(p).getString(WarfareKeys.PENDING_KIT);
                String displayKit = !pending.isEmpty() ? pending : current;
                pKits.put(
                        p.getScoreboardName(),
                        displayKit != null && !displayKit.isEmpty() && !displayKit.equals("Unassigned")
                                ? displayKit
                                : "Unassigned");
            }
        }

        return pKits;
    }

    // Отправляет полный пакет SyncGameData всем клиентам (перегрузка с прямыми параметрами)
    public static void sendToAllClients(
            int blue,
            int red,
            boolean hasBlue,
            boolean hasRed,
            boolean blueBleed,
            boolean redBleed,
            int respawnTime,
            boolean blueBlocked,
            boolean redBlocked,
            List<WarfareWorldData.HubInfo> hubs,
            String bFac,
            String rFac) {
        String bName = getFactionName(bFac, true);
        String rName = getFactionName(rFac, false);
        int blueCount = 0, redCount = 0;
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server != null) {
            for (ServerPlayer p : server.getPlayerList().getPlayers()) {
                String t = p.getTeam() != null ? p.getTeam().getName() : "";
                if (t.equalsIgnoreCase("Blue")) blueCount++;
                else if (t.equalsIgnoreCase("Red")) redCount++;
            }
        }
        INSTANCE.send(
                PacketDistributor.ALL.noArg(),
                new PacketSyncGameData(
                        blue,
                        red,
                        blueCount,
                        redCount,
                        hasBlue,
                        hasRed,
                        blueBleed,
                        redBleed,
                        respawnTime,
                        blueBlocked,
                        redBlocked,
                        (Boolean) WarfareConfig.HUB_SPAWN_COSTS_MATERIALS.get(),
                        (Integer) WarfareConfig.HUB_SPAWN_MATERIAL_COST.get(),
                        0,
                        0,
                        2048,
                        "map1",
                        new ArrayList<>(),
                        hubs,
                        new ArrayList<>(),
                        bFac,
                        rFac,
                        bName,
                        rName,
                        "aas",
                        false,
                        false,
                        0,
                        "RED",
                        new ArrayList<>(),
                        new HashMap<>(),
                        new HashMap<>(),
                        new HashMap<>(),
                        getPlayerKitsMap(),
                        new ArrayList<>(),
                        false,
                        0,
                        new HashMap<>(),
                        -1,
                        -1,
                        false,
                        "",
                        -1,
                        0,
                        new HashMap<>(),
                        false,
                        "",
                        -1,
                        0,
                        new HashMap<>(),
                        new ArrayList<>(),
                        BlockPos.ZERO,
                        BlockPos.ZERO,
                        0,
                        0,
                        "",
                        "",
                        false,
                        false));
    }

    // Отправляет пакет SyncGameData всем клиентам (перегрузка с WarfareWorldData)
    public static void sendToAllClients(
            WarfareWorldData data, boolean blueBleed, boolean redBleed, boolean bBlocked, boolean rBlocked) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;
        sendSync(server.getPlayerList().getPlayers(), data, blueBleed, redBleed, bBlocked, rBlocked);
    }

    // Отправляет пакет SyncGameData всем игрокам в указанном измерении
    public static void sendToAllClients(ServerLevel level, WarfareWorldData data) {
        sendSync(level.players(), data, false, false, false, false);
    }

    // ------------------------------------------------------------ рассылка по командам (как в Squad)
    // Врагу не уходят ФОБы/хабы, маркеры, техника, станции, отряды и запросы арты (TeamVisibility).
    // На рассылку — не больше 3 сборок/кодирований пакета (BLUE, RED, наблюдатели/без команды),
    // готовый vanilla-пакет переиспользуется для всех игроков ключа (как PacketDistributor.ALL).

    /** Ключ видимости игрока: креатив/спектатор — наблюдатель (видит всё), иначе по scoreboard-команде. */
    public static String viewerKey(ServerPlayer p) {
        String team = p.getTeam() != null ? p.getTeam().getName() : null;
        // видеть всё — только модератор-наблюдатель (этап 12): наказанный спектатор видит свою команду
        return TeamVisibility.viewerKey(
                team,
                p.isSpectator() || p.isCreative(),
                com.pwp.api.security.PwpPermissions.has(p, com.pwp.api.security.AccessLevel.MODERATOR));
    }

    /** SyncGameData — каждому игроку версия его команды. */
    public static void sendSync(
            Collection<ServerPlayer> targets,
            WarfareWorldData data,
            boolean blueBleed,
            boolean redBleed,
            boolean bBlocked,
            boolean rBlocked) {
        sendByViewer(targets, key -> createSyncPacket(data, blueBleed, redBleed, bBlocked, rBlocked, key));
    }

    /** Отряды — только своей команды (наблюдателю — все). */
    public static void syncSquads(Collection<ServerPlayer> targets, List<WarfareWorldData.Squad> squads) {
        sendByViewer(targets, key -> new PacketSyncSquads(visibleTo(squads, s -> s.team, key)));
    }

    /** Отряды всем игрокам сервера (каждому — своей команды). */
    public static void syncSquadsAll(List<WarfareWorldData.Squad> squads) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server != null) syncSquads(server.getPlayerList().getPlayers(), squads);
    }

    public static void syncSquadsTo(ServerPlayer player, List<WarfareWorldData.Squad> squads) {
        if (player != null) syncSquads(List.of(player), squads);
    }

    /**
     * Состояние нока: своей команде и наблюдателям — всегда (компас медика, раненые на HUD),
     * врагам — только тем, кто видит сущность (поза лежащего); при входе в обзор досылает
     * {@code DownedHandler.onStartTracking}.
     */
    public static void syncDowned(ServerPlayer subject, boolean downed) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null || subject == null) return;
        PacketSyncDownedState msg = new PacketSyncDownedState(subject.getId(), downed);
        Packet<?> vp = INSTANCE.toVanillaPacket(msg, NetworkDirection.PLAY_TO_CLIENT);
        String team = subject.getTeam() != null ? subject.getTeam().getName() : null;
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            if (p.connection != null && (p == subject || TeamVisibility.visible(team, viewerKey(p)))) {
                p.connection.send(vp);
            }
        }
        // враги рядом: повтор своим (они тоже трекают) безвреден — состояние идемпотентно
        INSTANCE.send(PacketDistributor.TRACKING_ENTITY.with(() -> subject), msg);
    }

    private static void sendByViewer(Collection<ServerPlayer> targets, Function<String, Object> build) {
        Map<String, Packet<?>> byKey = new HashMap<>();
        for (ServerPlayer p : targets) {
            if (p.connection == null) continue;
            Packet<?> vp = byKey.computeIfAbsent(
                    viewerKey(p), key -> INSTANCE.toVanillaPacket(build.apply(key), NetworkDirection.PLAY_TO_CLIENT));
            p.connection.send(vp);
        }
    }

    private static <T> List<T> visibleTo(List<T> list, Function<T, String> teamOf, String key) {
        if (list == null) return new ArrayList<>();
        if (TeamVisibility.ALL.equals(key)) return list;
        List<T> out = new ArrayList<>(list.size());
        for (T t : list) {
            if (TeamVisibility.visible(teamOf.apply(t), key)) out.add(t);
        }
        return out;
    }

    public static void broadcastPlayerSkin(ServerPlayer targetPlayer) {
        if (targetPlayer == null) return;
        UUID uuid = targetPlayer.getUUID();
        String faction = "none";
        Team team = targetPlayer.getTeam();
        if (team != null) {
            String tName = team.getName();
            MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
            if (server != null) {
                for (ServerLevel level : server.getAllLevels()) {
                    WarfareWorldData data = WarfareWorldData.get(level);
                    if (tName.equalsIgnoreCase("Blue")) {
                        faction = data.match.blueFaction;
                        break;
                    } else if (tName.equalsIgnoreCase("Red")) {
                        faction = data.match.redFaction;
                        break;
                    }
                }
            }
        }
        String kit = WarfareData.of(targetPlayer).getString(WarfareKeys.CURRENT_KIT);
        if (kit.isEmpty()) kit = "Unassigned";
        PacketSyncPlayerSkin packet = new PacketSyncPlayerSkin(uuid, faction, kit);
        INSTANCE.send(PacketDistributor.ALL.noArg(), packet);
    }

    private static PacketSyncGameData createSyncPacket(
            WarfareWorldData data,
            boolean blueBleed,
            boolean redBleed,
            boolean bBlocked,
            boolean rBlocked,
            String viewer) {
        // запрос арты чужой команды врагу не показываем
        var blueArt = TeamVisibility.visible("BLUE", viewer) ? data.match.blueArtRequest : null;
        var redArt = TeamVisibility.visible("RED", viewer) ? data.match.redArtRequest : null;
        boolean hasBlue = !data.structures.blueRallies.isEmpty();
        boolean hasRed = !data.structures.redRallies.isEmpty();
        String bName = getFactionName(data.match.blueFaction, true);
        String rName = getFactionName(data.match.redFaction, false);
        int blueCount = 0, redCount = 0;
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server != null) {
            for (ServerPlayer p : server.getPlayerList().getPlayers()) {
                String t = p.getTeam() != null ? p.getTeam().getName() : "";
                if (t.equalsIgnoreCase("Blue")) blueCount++;
                else if (t.equalsIgnoreCase("Red")) redCount++;
            }
        }
        return new PacketSyncGameData(
                data.match.blueTickets,
                data.match.redTickets,
                blueCount,
                redCount,
                hasBlue,
                hasRed,
                blueBleed,
                redBleed,
                data.match.respawnTimer,
                bBlocked,
                rBlocked,
                (Boolean) WarfareConfig.HUB_SPAWN_COSTS_MATERIALS.get(),
                (Integer) WarfareConfig.HUB_SPAWN_MATERIAL_COST.get(),
                data.match.mapCenterX,
                data.match.mapCenterZ,
                data.match.mapSizeBlocks,
                data.match.currentMapImage,
                visibleTo(data.vehicles.markedVehicles, v -> v.team, viewer),
                visibleTo(data.structures.hubs, h -> h.team, viewer),
                visibleTo(data.structures.vehicleStations, st -> st.team, viewer),
                data.match.blueFaction,
                data.match.redFaction,
                bName,
                rName,
                data.match.gameMode != null ? data.match.gameMode : "aas",
                data.match.isGameStarted,
                data.match.invasionSetupActive,
                data.match.invasionSetupTimer,
                data.match.invasionDefender != null ? data.match.invasionDefender : "RED",
                data.match.capturePoints,
                data.match.blueSpawns,
                data.match.redSpawns,
                data.match.neutralSpawns,
                getPlayerKitsMap(),
                visibleTo(data.markers.activeMarkers, m -> m.team, viewer),
                data.match.voteActive,
                data.match.voteTimer,
                data.match.votes,
                data.squadRegistry.blueCMDId,
                data.squadRegistry.redCMDId,
                data.squadRegistry.blueCmdVoteActive,
                data.squadRegistry.blueCmdCandidateName,
                data.squadRegistry.blueCmdCandidateId,
                data.squadRegistry.blueCmdVoteTimer,
                data.squadRegistry.blueCmdVotes,
                data.squadRegistry.redCmdVoteActive,
                data.squadRegistry.redCmdCandidateName,
                data.squadRegistry.redCmdCandidateId,
                data.squadRegistry.redCmdVoteTimer,
                data.squadRegistry.redCmdVotes,
                visibleTo(data.match.activeStrikes, st -> st.team, viewer),
                blueArt != null ? blueArt.pos : BlockPos.ZERO,
                redArt != null ? redArt.pos : BlockPos.ZERO,
                blueArt != null ? blueArt.timer : 0,
                redArt != null ? redArt.timer : 0,
                blueArt != null ? blueArt.requesterName : "",
                redArt != null ? redArt.requesterName : "",
                data.match.blueReady,
                data.match.redReady);
    }
}
