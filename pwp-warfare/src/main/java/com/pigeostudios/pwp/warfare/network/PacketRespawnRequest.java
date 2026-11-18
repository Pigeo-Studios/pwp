package com.pigeostudios.pwp.warfare.network;

import com.pigeostudios.pwp.warfare.block.HubBlockEntity;
import com.pigeostudios.pwp.warfare.block.RallyPointBlock;
import com.pigeostudios.pwp.warfare.config.WarfareConfig;
import com.pigeostudios.pwp.warfare.data.state.WarfareData;
import com.pigeostudios.pwp.warfare.data.state.WarfareKeys;
import com.pigeostudios.pwp.warfare.events.DownedHandler;
import com.pigeostudios.pwp.warfare.events.GameLogicEvents;
import com.pigeostudios.pwp.warfare.match.SpawnRules;
import com.pigeostudios.pwp.warfare.server.FactionDataLoader;
import com.pigeostudios.pwp.warfare.world.WarfareWorldData;
import com.pwp.api.net.Net;
import com.pwp.api.util.RequestGuard;
import java.util.Map;
import java.util.Random;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap.Types;

/**
 * Запрос деплоя (сеть v2): главная база, точка сбора или ФОБ. Тип проверяется в {@link
 * #validate()} ({@link SpawnRules#validateType}), состояние игрока — {@link SpawnRules#denyReason}:
 * живой игрок на поле не может телепортироваться и получить кит повторно. Антиспам — {@link
 * #GUARD} (один запрос в секунду на игрока).
 */
public record PacketRespawnRequest(String type) implements Net.C2S {
    /** Антиспам: не чаще одного запроса деплоя в секунду на игрока. */
    private static final RequestGuard GUARD = new RequestGuard(1000);

    private static final String GUARD_TYPE = "respawn";

    public static void encode(PacketRespawnRequest msg, FriendlyByteBuf buf) {
        buf.writeUtf(msg.type, SpawnRules.MAX_TYPE_LENGTH);
    }

    public static PacketRespawnRequest decode(FriendlyByteBuf buf) {
        return new PacketRespawnRequest(buf.readUtf(SpawnRules.MAX_TYPE_LENGTH));
    }

    @Override
    public String validate() {
        return SpawnRules.validateType(type);
    }

    @Override
    public void handle(ServerPlayer player) {
        if (!GUARD.tryAcquire(player.getUUID(), GUARD_TYPE)) return;
        GUARD.release(player.getUUID(), GUARD_TYPE);
        String denied = SpawnRules.denyReason(new SpawnRules.PlayerState(
                player.isDeadOrDying(),
                player.gameMode.getGameModeForPlayer() == GameType.SPECTATOR,
                WarfareData.of(player).getBoolean(WarfareKeys.IS_DOWNED),
                WarfareData.of(player).getBoolean(WarfareKeys.TEAM_KILL_SPECTATOR)));
        if (denied != null) {
            player.sendSystemMessage(Component.translatable(denied).withStyle(ChatFormatting.RED));
            return;
        }
        process(player, type);
    }

    /** Забыть игрока в антиспаме (выход с сервера). */
    public static void forget(java.util.UUID player) {
        GUARD.forget(player);
    }

    /**
     * Возрождение игрока по типу спавна; вызывается из пакета (после проверки состояния) и из
     * отложенного деплоя (состояние проверено при откладывании и сверено со снимком).
     */
    public static void process(ServerPlayer player, String type) {
        {
            if (WarfareData.of(player).getBoolean(WarfareKeys.IS_DOWNED)) {
                DownedHandler.forceGiveUp(player);
                return;
            }
            if (!FactionDataLoader.kitsReady(FactionDataLoader.teamOf(player))) {
                // Киты команды ещё грузятся из core-service — деплой выполнится после загрузки
                FactionDataLoader.deferDeploy(player, type);
                return;
            }
            ServerLevel level = player.serverLevel();
            WarfareWorldData data = WarfareWorldData.get(level);
            BlockPos targetPos = null;
            ResourceKey<Level> targetDimension = level.dimension();
            String currentDim = level.dimension().location().toString();
            boolean useCustomSpawn = false;
            String team = "NEUTRAL";
            if (player.getTeam() != null) {
                team = player.getTeam().getName().equalsIgnoreCase("Blue") ? "BLUE" : "RED";
            }

            boolean isBlue = team.equals("BLUE");
            Map<String, BlockPos> mainSpawns = isBlue
                    ? data.match.blueSpawns
                    : (team.equals("RED") ? data.match.redSpawns : data.match.neutralSpawns);
            SpawnRules.SpawnType spawn = SpawnRules.parse(type);
            if (spawn == null) return;
            if (spawn.kind() == SpawnRules.SpawnType.Kind.MAIN) {
                if (mainSpawns.containsKey(currentDim)) {
                    targetPos = mainSpawns.get(currentDim);
                    useCustomSpawn = true;
                }
            } else if (spawn.kind() == SpawnRules.SpawnType.Kind.RALLY) {
                String pName = player.getScoreboardName();
                WarfareWorldData.Squad mySquad = getPlayerSquad(pName, data);
                if (mySquad != null && mySquad.rallyPos != null) {
                    if (mySquad.isRallyBlocked && !player.isCreative()) {
                        player.sendSystemMessage(Component.translatable("pwpwarfare.msg.spawn.rally_threat")
                                .withStyle(ChatFormatting.RED));
                        return;
                    }

                    String rallyDim = mySquad.rallyDimension != null ? mySquad.rallyDimension : "minecraft:overworld";
                    if (!rallyDim.equals(currentDim)) {
                        player.sendSystemMessage(Component.translatable("pwpwarfare.msg.spawn.rally_other_dim")
                                .withStyle(ChatFormatting.RED));
                        return;
                    }

                    if (!level.isLoaded(mySquad.rallyPos)
                            || !(level.getBlockState(mySquad.rallyPos).getBlock() instanceof RallyPointBlock)) {
                        player.sendSystemMessage(Component.translatable("pwpwarfare.msg.spawn.rally_destroyed")
                                .withStyle(ChatFormatting.RED));
                        return;
                    }

                    targetPos = findRandomSafeSpawn(level, mySquad.rallyPos, 10);
                    useCustomSpawn = true;
                    if (targetPos == null) {
                        targetPos = mainSpawns.get(currentDim);
                        player.sendSystemMessage(Component.translatable("pwpwarfare.msg.spawn.rally_blocked")
                                .withStyle(ChatFormatting.YELLOW));
                    }
                }
            } else if (spawn.kind() == SpawnRules.SpawnType.Kind.HUB) {
                BlockPos reqPos = new BlockPos(spawn.x(), spawn.y(), spawn.z());

                for (WarfareWorldData.HubInfo h : data.structures.hubs) {
                    if (h.pos.equals(reqPos) && h.team.equalsIgnoreCase(team) && h.constructed) {
                        int hubRadius = (Integer) WarfareConfig.HUB_BLOCK_RADIUS.get();
                        int hubEnemiesReq = (Integer) WarfareConfig.HUB_BLOCK_ENEMY_COUNT.get();
                        int enemyCount = GameLogicEvents.getEnemyCount(level, reqPos, team);
                        if ((h.isBlocked || enemyCount >= hubEnemiesReq) && !player.isCreative()) {
                            player.sendSystemMessage(Component.translatable("pwpwarfare.msg.spawn.fob_threat")
                                    .withStyle(ChatFormatting.RED));
                            return;
                        }

                        boolean spawnCosts = (Boolean) WarfareConfig.HUB_SPAWN_COSTS_MATERIALS.get();
                        int spawnCost = (Integer) WarfareConfig.HUB_SPAWN_MATERIAL_COST.get();
                        if (spawnCosts) {
                            level.getChunkSource().getChunk(reqPos.getX() >> 4, reqPos.getZ() >> 4, true);
                            if (!(level.getBlockEntity(reqPos) instanceof HubBlockEntity hubBe)) {
                                player.sendSystemMessage(Component.translatable("pwpwarfare.msg.spawn.fob_missing")
                                        .withStyle(ChatFormatting.RED));
                                return;
                            }

                            if (hubBe.getMaterials() < spawnCost) {
                                player.sendSystemMessage(Component.translatable(
                                                "pwpwarfare.msg.spawn.fob_no_mats", hubBe.getMaterials(), spawnCost)
                                        .withStyle(ChatFormatting.RED));
                                return;
                            }

                            hubBe.consumeMaterials(spawnCost);
                            h.materials = hubBe.getMaterials();
                            data.setDirty();
                            PacketHandler.sendToAllClients(level, data);
                        }

                        targetPos = findRandomSafeSpawn(level, reqPos, 10);
                        useCustomSpawn = true;
                        if (targetPos == null) {
                            targetPos = mainSpawns.get(currentDim);
                            player.sendSystemMessage(Component.translatable("pwpwarfare.msg.spawn.fob_blocked")
                                    .withStyle(ChatFormatting.YELLOW));
                        }
                        break;
                    }
                }
            }

            if (useCustomSpawn && targetPos != null) {
                player.setRespawnPosition(targetDimension, targetPos, 0.0F, true, false);
                player.teleportTo(
                        level,
                        targetPos.getX() + 0.5,
                        targetPos.getY(),
                        targetPos.getZ() + 0.5,
                        player.getYRot(),
                        0.0F);
                if (player.gameMode.getGameModeForPlayer() == GameType.SPECTATOR) {
                    player.setGameMode(GameType.SURVIVAL);
                }

                if (data.match.isGameStarted) {
                    String pTeam = player.getTeam() != null
                            ? player.getTeam().getName().toUpperCase()
                            : "";
                    boolean isSetupAttacker =
                            data.match.invasionSetupActive && !pTeam.equalsIgnoreCase(data.match.invasionDefender);
                    if (!isSetupAttacker) {
                        ResupplyHandler.applyKitOnSpawn(player, data);
                    }
                }
            } else {
                player.sendSystemMessage(Component.translatable("pwpwarfare.msg.spawn.unavailable")
                        .withStyle(ChatFormatting.RED));
            }
        }
    }

    private static WarfareWorldData.Squad getPlayerSquad(String playerName, WarfareWorldData data) {
        for (WarfareWorldData.Squad s : data.squadRegistry.squads) {
            if (s.members.contains(playerName)) {
                return s;
            }
        }

        return null;
    }

    // Ищет случайную безопасную точку для возрождения в радиусе от центра
    private static BlockPos findRandomSafeSpawn(ServerLevel level, BlockPos center, int radius) {
        Random rand = new Random();

        for (int i = 0; i < 100; i++) {
            int dx = rand.nextInt(radius * 2 + 1) - radius;
            int dz = rand.nextInt(radius * 2 + 1) - radius;
            BlockPos surface = level.getHeightmapPos(Types.MOTION_BLOCKING_NO_LEAVES, center.offset(dx, 0, dz));

            for (int dy = 1; dy >= -2; dy--) {
                BlockPos candidate = surface.offset(0, dy, 0);
                if (isValidSpawnSpot(level, candidate)) {
                    return candidate;
                }
            }
        }

        return null;
    }

    // Проверяет, безопасно ли место: свободное пространство, твёрдая земля, нет лавы/кактусов
    private static boolean isValidSpawnSpot(ServerLevel level, BlockPos pos) {
        BlockState feet = level.getBlockState(pos);
        BlockState head = level.getBlockState(pos.above());
        BlockState ground = level.getBlockState(pos.below());
        boolean spaceClear = feet.getCollisionShape(level, pos).isEmpty()
                && head.getCollisionShape(level, pos.above()).isEmpty();
        boolean groundSolid = ground.isFaceSturdy(level, pos.below(), Direction.UP);
        boolean notDangerous = !feet.is(Blocks.LAVA)
                && !feet.is(Blocks.FIRE)
                && !ground.is(Blocks.LAVA)
                && !feet.is(Blocks.WATER)
                && !head.is(Blocks.WATER);
        return spaceClear && groundSolid && notDangerous;
    }
}
