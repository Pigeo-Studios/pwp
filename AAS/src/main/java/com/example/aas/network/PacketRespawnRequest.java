/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.ChatFormatting
 *  net.minecraft.core.BlockPos
 *  net.minecraft.network.FriendlyByteBuf
 *  net.minecraft.network.chat.Component
 *  net.minecraft.resources.ResourceKey
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.server.level.ServerPlayer
 *  net.minecraft.tags.BlockTags
 *  net.minecraft.world.level.BlockGetter
 *  net.minecraft.world.level.GameType
 *  net.minecraft.world.level.block.Blocks
 *  net.minecraft.world.level.block.entity.BlockEntity
 *  net.minecraft.world.level.block.state.BlockState
 *  net.minecraftforge.network.NetworkEvent$Context
 */
package com.example.aas.network;

import com.example.aas.block.HubBlockEntity;
import com.example.aas.block.RallyPointBlock;
import com.example.aas.config.AASConfig;
import com.example.aas.events.GameLogicEvents;
import com.example.aas.network.PacketHandler;
import com.example.aas.network.ResupplyHandler;
import com.example.aas.world.AASWorldData;
import java.util.Map;
import java.util.Random;
import java.util.function.Supplier;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.network.NetworkEvent;

public class PacketRespawnRequest {
    private final String type;

    public PacketRespawnRequest(String type) {
        this.type = type;
    }

    public static void encode(PacketRespawnRequest msg, FriendlyByteBuf buf) {
        buf.writeUtf(msg.type);
    }

    public static PacketRespawnRequest decode(FriendlyByteBuf buf) {
        return new PacketRespawnRequest(buf.readUtf());
    }

    public static void handle(PacketRespawnRequest msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            boolean useCustomSpawn;
            ResourceKey targetDimension;
            BlockPos targetPos;
            AASWorldData data;
            ServerLevel level;
            ServerPlayer player;
            block21: {
                String[] parts;
                Map<String, BlockPos> mainSpawns;
                String team;
                String currentDim;
                block23: {
                    Object rallyDim;
                    block22: {
                        boolean isBlue;
                        player = ((NetworkEvent.Context)ctx.get()).getSender();
                        if (player == null) {
                            return;
                        }
                        level = player.serverLevel();
                        data = AASWorldData.get(level);
                        targetPos = null;
                        targetDimension = level.dimension();
                        currentDim = level.dimension().location().toString();
                        useCustomSpawn = false;
                        team = "NEUTRAL";
                        if (player.getTeam() != null) {
                            String string = team = player.getTeam().getName().equalsIgnoreCase("Blue") ? "BLUE" : "RED";
                        }
                        Map<String, BlockPos> map = (isBlue = team.equals("BLUE")) ? data.blueSpawns : (mainSpawns = team.equals("RED") ? data.redSpawns : data.neutralSpawns);
                        if (!msg.type.equals("MAIN")) break block22;
                        if (mainSpawns.containsKey(currentDim)) {
                            targetPos = mainSpawns.get(currentDim);
                            useCustomSpawn = true;
                        }
                        break block21;
                    }
                    if (!msg.type.equals("RALLY")) break block23;
                    String pName = player.getScoreboardName();
                    AASWorldData.Squad mySquad = PacketRespawnRequest.getPlayerSquad(pName, data);
                    if (mySquad == null || mySquad.rallyPos == null) break block21;
                    if (mySquad.isRallyBlocked && !player.isCreative()) {
                        player.sendSystemMessage((Component)Component.literal((String)"Spawn Failed: Squad Rally is OVERRUN!").withStyle(ChatFormatting.RED));
                        return;
                    }
                    Object object = rallyDim = mySquad.rallyDimension != null ? mySquad.rallyDimension : "minecraft:overworld";
                    if (!((String)rallyDim).equals(currentDim)) {
                        player.sendSystemMessage((Component)Component.literal((String)"Rally Point is in another dimension!").withStyle(ChatFormatting.RED));
                        return;
                    }
                    if (level.isLoaded(mySquad.rallyPos) && level.getBlockState(mySquad.rallyPos).getBlock() instanceof RallyPointBlock) {
                        targetPos = PacketRespawnRequest.findRandomSafeSpawn(level, mySquad.rallyPos, 10);
                        useCustomSpawn = true;
                        if (targetPos == null) {
                            targetPos = mainSpawns.get(currentDim);
                            player.sendSystemMessage((Component)Component.literal((String)"Rally spawn blocked! Redirecting to Main Base.").withStyle(ChatFormatting.YELLOW));
                        }
                        break block21;
                    } else {
                        player.sendSystemMessage((Component)Component.literal((String)"Rally Point destroyed!").withStyle(ChatFormatting.RED));
                        return;
                    }
                }
                if (msg.type.startsWith("HUB") && (parts = msg.type.split(":")).length == 4) {
                    try {
                        BlockPos reqPos = new BlockPos(Integer.parseInt(parts[1]), Integer.parseInt(parts[2]), Integer.parseInt(parts[3]));
                        for (AASWorldData.HubInfo h : data.hubs) {
                            if (!h.pos.equals((Object)reqPos) || !h.team.equalsIgnoreCase(team) || !h.constructed) continue;
                            int hubRadius = (Integer)AASConfig.HUB_BLOCK_RADIUS.get();
                            int hubEnemiesReq = (Integer)AASConfig.HUB_BLOCK_ENEMY_COUNT.get();
                            int enemyCount = GameLogicEvents.getEnemyCount(level, reqPos, team);
                            if ((h.isBlocked || enemyCount >= hubEnemiesReq) && !player.isCreative()) {
                                player.sendSystemMessage((Component)Component.literal((String)"Spawn Failed: FOB is OVERRUN!").withStyle(ChatFormatting.RED));
                                return;
                            }
                            boolean spawnCosts = (Boolean)AASConfig.HUB_SPAWN_COSTS_MATERIALS.get();
                            int spawnCost = (Integer)AASConfig.HUB_SPAWN_MATERIAL_COST.get();
                            if (spawnCosts) {
                                level.getChunkSource().getChunk(reqPos.getX() >> 4, reqPos.getZ() >> 4, true);
                                BlockEntity be = level.getBlockEntity(reqPos);
                                if (!(be instanceof HubBlockEntity)) {
                                    player.sendSystemMessage((Component)Component.literal((String)"Spawn Failed: FOB block missing!").withStyle(ChatFormatting.RED));
                                    return;
                                }
                                HubBlockEntity hubBe = (HubBlockEntity)be;
                                if (hubBe.getMaterials() < spawnCost) {
                                    player.sendSystemMessage((Component)Component.literal((String)("Spawn Failed: Not enough materials! (" + hubBe.getMaterials() + "/" + spawnCost + ")")).withStyle(ChatFormatting.RED));
                                    return;
                                }
                                hubBe.consumeMaterials(spawnCost);
                                h.materials = hubBe.getMaterials();
                                data.setDirty();
                                PacketHandler.sendToAllClients(level, data);
                            }
                            targetPos = PacketRespawnRequest.findRandomSafeSpawn(level, reqPos, 10);
                            useCustomSpawn = true;
                            if (targetPos == null) {
                                targetPos = mainSpawns.get(currentDim);
                                player.sendSystemMessage((Component)Component.literal((String)"FOB spawn blocked! Redirecting to Main Base.").withStyle(ChatFormatting.YELLOW));
                            }
                            break;
                        }
                    }
                    catch (Exception reqPos) {
                        // empty catch block
                    }
                }
            }
            if (useCustomSpawn && targetPos != null) {
                player.setRespawnPosition(targetDimension, targetPos, 0.0f, true, false);
                player.teleportTo(level, (double)targetPos.getX() + 0.5, (double)targetPos.getY(), (double)targetPos.getZ() + 0.5, player.getYRot(), 0.0f);
                if (player.gameMode.getGameModeForPlayer() == GameType.SPECTATOR) {
                    player.setGameMode(GameType.SURVIVAL);
                }
                if (!data.isGameStarted) {
                    player.sendSystemMessage((Component)Component.literal((String)"\u041a\u0438\u0442 \u0437\u0430\u0431\u0440\u043e\u043d\u0438\u0440\u043e\u0432\u0430\u043d. \u041f\u0440\u0435\u0434\u043c\u0435\u0442\u044b \u0431\u0443\u0434\u0443\u0442 \u0432\u044b\u0434\u0430\u043d\u044b \u043f\u043e\u0441\u043b\u0435 \u043d\u0430\u0447\u0430\u043b\u0430 \u0438\u0433\u0440\u044b.").withStyle(ChatFormatting.YELLOW));
                    return;
                }
                if (player.getPersistentData().contains("AAS_PendingKit")) {
                    ResupplyHandler.tryApplyPendingKit(player, data);
                    return;
                }
                String currentKitName = player.getPersistentData().getString("AAS_CurrentKit");
                if (currentKitName.isEmpty()) return;
                if (currentKitName.equals("Unassigned")) return;
                String tName = player.getTeam() != null ? player.getTeam().getName().toUpperCase() : "NEUTRAL";
                AASWorldData.KitInfo kit = tName.equals("BLUE") ? data.blueKits.get(currentKitName) : data.redKits.get(currentKitName);
                if (kit == null) return;
                ResupplyHandler.applyKitToPlayer(player, kit);
                return;
            }
            player.sendSystemMessage((Component)Component.literal((String)"Spawn point is currently unavailable!").withStyle(ChatFormatting.RED));
        });
        ctx.get().setPacketHandled(true);
    }

    private static AASWorldData.Squad getPlayerSquad(String playerName, AASWorldData data) {
        for (AASWorldData.Squad s : data.squads) {
            if (!s.members.contains(playerName)) continue;
            return s;
        }
        return null;
    }

    private static BlockPos findRandomSafeSpawn(ServerLevel level, BlockPos center, int radius) {
        Random rand = new Random();
        for (int i = 0; i < 100; ++i) {
            int dx = rand.nextInt(radius * 2 + 1) - radius;
            int dz = rand.nextInt(radius * 2 + 1) - radius;
            for (int dy = -3; dy <= 3; ++dy) {
                BlockPos candidate = center.offset(dx, dy, dz);
                if (!PacketRespawnRequest.isValidSpawnSpot(level, candidate)) continue;
                return candidate;
            }
        }
        return null;
    }

    private static boolean isValidSpawnSpot(ServerLevel level, BlockPos pos) {
        BlockState feet = level.getBlockState(pos);
        BlockState head = level.getBlockState(pos.above());
        BlockState ground = level.getBlockState(pos.below());
        boolean spaceClear = feet.getCollisionShape((BlockGetter)level, pos).isEmpty() && head.getCollisionShape((BlockGetter)level, pos.above()).isEmpty();
        boolean groundSolid = !ground.getCollisionShape((BlockGetter)level, pos.below()).isEmpty();
        boolean notDangerous = !feet.is(Blocks.LAVA) && !feet.is(Blocks.FIRE) && !ground.is(Blocks.LAVA) && !feet.is(Blocks.WATER) && !head.is(Blocks.WATER) && !feet.is(BlockTags.LEAVES) && !head.is(BlockTags.LEAVES) && !feet.is(Blocks.COCOA) && !head.is(Blocks.COCOA);
        return spaceClear && groundSolid && notDangerous;
    }
}

