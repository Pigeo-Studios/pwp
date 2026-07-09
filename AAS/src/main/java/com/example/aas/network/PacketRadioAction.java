/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.ChatFormatting
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.Vec3i
 *  net.minecraft.network.FriendlyByteBuf
 *  net.minecraft.network.chat.Component
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.server.level.ServerPlayer
 *  net.minecraft.world.item.ItemStack
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.level.block.Block
 *  net.minecraft.world.level.block.entity.BlockEntity
 *  net.minecraft.world.level.block.state.BlockState
 *  net.minecraft.world.level.block.state.properties.Property
 *  net.minecraft.world.phys.AABB
 *  net.minecraft.world.phys.Vec3
 *  net.minecraftforge.network.NetworkEvent$Context
 *  net.minecraftforge.network.PacketDistributor
 */
package com.example.aas.network;

import com.example.aas.block.AGSConstructionBlock;
import com.example.aas.block.AGSConstructionBlockEntity;
import com.example.aas.block.HubBlockEntity;
import com.example.aas.block.M2ConstructionBlock;
import com.example.aas.block.M2ConstructionBlockEntity;
import com.example.aas.block.ModBlocks;
import com.example.aas.block.MortarConstructionBlock;
import com.example.aas.block.MortarConstructionBlockEntity;
import com.example.aas.block.RallyPointBlockEntity;
import com.example.aas.block.TOWConstructionBlock;
import com.example.aas.block.TOWConstructionBlockEntity;
import com.example.aas.config.AASConfig;
import com.example.aas.entity.SupplyCrateEntity;
import com.example.aas.network.PacketHandler;
import com.example.aas.network.PacketSyncSquads;
import com.example.aas.world.AASWorldData;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

public class PacketRadioAction {
    private final int actionId;

    public PacketRadioAction(int actionId) {
        this.actionId = actionId;
    }

    public static void encode(PacketRadioAction msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.actionId);
    }

    public static PacketRadioAction decode(FriendlyByteBuf buf) {
        return new PacketRadioAction(buf.readInt());
    }

    public static void handle(PacketRadioAction msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ((NetworkEvent.Context)ctx.get()).getSender();
            if (player == null) {
                return;
            }
            ServerLevel currentLevel = player.serverLevel();
            AASWorldData data = AASWorldData.get(currentLevel);
            String pName = player.getScoreboardName();
            AASWorldData.Squad playerSquad = null;
            boolean isLeader = false;
            for (AASWorldData.Squad s : data.squads) {
                if (!s.members.contains(pName)) continue;
                playerSquad = s;
                if (!s.leader.equals(pName)) break;
                isLeader = true;
                break;
            }
            if (!player.isCreative()) {
                if (player.getTeam() == null) {
                    player.sendSystemMessage((Component)Component.literal((String)"Access Denied: You must be in a TEAM!").withStyle(ChatFormatting.RED));
                    return;
                }
                if (playerSquad == null) {
                    player.sendSystemMessage((Component)Component.literal((String)"Access Denied: You must be in a SQUAD in this world!").withStyle(ChatFormatting.RED));
                    return;
                }
                if (!isLeader) {
                    player.sendSystemMessage((Component)Component.literal((String)"Access Denied: You must be a Squad Leader!").withStyle(ChatFormatting.RED));
                    return;
                }
            }
            ItemStack stack = player.getMainHandItem();
            long currentGameTime = currentLevel.getGameTime();
            if (msg.actionId == 0) {
                long lastUse = playerSquad.nextRallyAvailableTick;
                if (currentGameTime < lastUse && !player.isCreative()) {
                    long timeLeft = (lastUse - currentGameTime) / 20L;
                    player.sendSystemMessage((Component)Component.literal((String)("Rally Point Cooldown: " + timeLeft + "s")).withStyle(ChatFormatting.RED));
                    return;
                }
                String team = "NEUTRAL";
                if (player.getTeam() != null) {
                    String rawTeamName = player.getTeam().getName();
                    if (rawTeamName.equalsIgnoreCase("Blue")) {
                        team = "BLUE";
                    } else if (rawTeamName.equalsIgnoreCase("Red")) {
                        team = "RED";
                    }
                }
                if (team.equals("NEUTRAL") && player.isCreative()) {
                    team = "BLUE";
                }
                if (PacketRadioAction.trySpawnRally(player, currentLevel, team, playerSquad, data)) {
                    playerSquad.nextRallyAvailableTick = currentGameTime + 6000L;
                    player.sendSystemMessage((Component)Component.literal((String)"Squad Rally Point Deployed!").withStyle(ChatFormatting.GREEN));
                } else {
                    playerSquad.nextRallyAvailableTick = currentGameTime + 300L;
                }
                data.setDirty();
                PacketHandler.INSTANCE.send(PacketDistributor.DIMENSION.with(() -> ((Level)player.level()).dimension()), (Object)new PacketSyncSquads(data.squads));
            } else {
                PacketRadioAction.handleConstructionLogic(msg.actionId, player, currentLevel, data);
            }
        });
        ctx.get().setPacketHandled(true);
    }

    private static void handleConstructionLogic(int actionId, ServerPlayer player, ServerLevel level, AASWorldData data) {
        BlockPos targetPos = player.blockPosition();
        if (!level.getBlockState(targetPos).canBeReplaced() && actionId != 14) {
            targetPos = targetPos.relative(player.getDirection());
        }
        if (actionId == 14) {
            String playerTeam = player.getTeam() != null ? player.getTeam().getName().toUpperCase() : "NEUTRAL";
            long hubCount = data.hubs.stream().filter(h -> h.team.equalsIgnoreCase(playerTeam)).count();
            if (hubCount >= (long)((Integer)AASConfig.MAX_HUBS_PER_TEAM.get()).intValue() && !player.isCreative()) {
                player.sendSystemMessage((Component)Component.literal((String)"FOB Limit Reached for this world!").withStyle(ChatFormatting.RED));
                return;
            }
            for (AASWorldData.HubInfo existingHub : data.hubs) {
                if (!existingHub.team.equalsIgnoreCase(playerTeam) || !(existingHub.pos.distSqr((Vec3i)targetPos) < (double)((Integer)AASConfig.MIN_HUB_DISTANCE.get() * (Integer)AASConfig.MIN_HUB_DISTANCE.get()))) continue;
                player.sendSystemMessage((Component)Component.literal((String)"Too close to friendly FOB!").withStyle(ChatFormatting.RED));
                return;
            }
            if (((Boolean)AASConfig.HUB_PLACEMENT_REQUIRES_CRATE.get()).booleanValue() && !player.isCreative()) {
                double crateCheckRad = 50.0;
                AABB area = new AABB(targetPos).inflate(crateCheckRad);
                List crates = level.getEntitiesOfClass(SupplyCrateEntity.class, area);
                SupplyCrateEntity targetCrate = crates.stream().filter(c -> c.getTeamOwner().equals("NEUTRAL") || c.getTeamOwner().equalsIgnoreCase(playerTeam)).findFirst().orElse(null);
                if (targetCrate == null) {
                    player.sendSystemMessage((Component)Component.literal((String)"FOB placement requires a Supply Crate within 50 blocks!").withStyle(ChatFormatting.RED));
                    return;
                }
                targetCrate.discard();
                player.sendSystemMessage((Component)Component.literal((String)"Supply Crate consumed for FOB placement.").withStyle(ChatFormatting.YELLOW));
            }
            level.setBlock(targetPos, ((Block)ModBlocks.HUB_BLOCK.get()).defaultBlockState(), 3);
            BlockEntity be = level.getBlockEntity(targetPos);
            if (be instanceof HubBlockEntity) {
                HubBlockEntity hubEntity = (HubBlockEntity)be;
                hubEntity.setTeam(playerTeam);
                level.sendBlockUpdated(targetPos, level.getBlockState(targetPos), level.getBlockState(targetPos), 3);
            }
            data.hubs.add(new AASWorldData.HubInfo(targetPos, playerTeam, false, level.dimension().location().toString()));
            data.setDirty();
            PacketHandler.sendToAllClients(level, data);
            player.sendSystemMessage((Component)Component.literal((String)"FOB Blueprint placed!").withStyle(ChatFormatting.GREEN));
        } else if (actionId == 20) {
            PacketRadioAction.placeBlueprint(level, targetPos, player, (BlockState)((Block)ModBlocks.M2_CONSTRUCTION_BLOCK.get()).defaultBlockState().setValue((Property)M2ConstructionBlock.FACING, (Comparable)player.getDirection().getOpposite()), "M2");
        } else if (actionId == 21) {
            PacketRadioAction.placeBlueprint(level, targetPos, player, (BlockState)((Block)ModBlocks.AGS_CONSTRUCTION_BLOCK.get()).defaultBlockState().setValue((Property)AGSConstructionBlock.FACING, (Comparable)player.getDirection().getOpposite()), "AGS");
        } else if (actionId == 22) {
            PacketRadioAction.placeBlueprint(level, targetPos, player, (BlockState)((Block)ModBlocks.MORTAR_CONSTRUCTION_BLOCK.get()).defaultBlockState().setValue((Property)MortarConstructionBlock.FACING, (Comparable)player.getDirection().getOpposite()), "Mortar");
        } else if (actionId == 23) {
            PacketRadioAction.placeBlueprint(level, targetPos, player, (BlockState)((Block)ModBlocks.TOW_CONSTRUCTION_BLOCK.get()).defaultBlockState().setValue((Property)TOWConstructionBlock.FACING, (Comparable)player.getDirection().getOpposite()), "TOW");
        }
    }

    private static void placeBlueprint(ServerLevel level, BlockPos pos, ServerPlayer player, BlockState state, String name) {
        String pTeam;
        if (!level.getBlockState(pos).isAir() && !level.getBlockState(pos).canBeReplaced()) {
            return;
        }
        level.setBlock(pos, state, 3);
        BlockEntity be = level.getBlockEntity(pos);
        String string = pTeam = player.getTeam() != null ? player.getTeam().getName() : "NEUTRAL";
        if (be instanceof M2ConstructionBlockEntity) {
            M2ConstructionBlockEntity m2 = (M2ConstructionBlockEntity)be;
            m2.setTeam(pTeam);
        } else if (be instanceof AGSConstructionBlockEntity) {
            AGSConstructionBlockEntity ags = (AGSConstructionBlockEntity)be;
            ags.setTeam(pTeam);
        } else if (be instanceof MortarConstructionBlockEntity) {
            MortarConstructionBlockEntity mortar = (MortarConstructionBlockEntity)be;
            mortar.setTeam(pTeam);
        } else if (be instanceof TOWConstructionBlockEntity) {
            TOWConstructionBlockEntity tow = (TOWConstructionBlockEntity)be;
            tow.setTeam(pTeam);
        }
        player.sendSystemMessage((Component)Component.literal((String)(name + " Blueprint placed.")).withStyle(ChatFormatting.GREEN));
    }

    private static boolean trySpawnRally(ServerPlayer player, ServerLevel level, String team, AASWorldData.Squad squad, AASWorldData data) {
        BlockPos pos = player.blockPosition();
        for (AASWorldData.CapturePoint point : data.capturePoints) {
            Vec3 center = point.area.getCenter();
            if (!(pos.distToCenterSqr(center.x, center.y, center.z) < (double)((Integer)AASConfig.MIN_RALLY_POINT_DISTANCE.get() * (Integer)AASConfig.MIN_RALLY_POINT_DISTANCE.get()))) continue;
            player.sendSystemMessage((Component)Component.literal((String)"Too close to Capture Point!").withStyle(ChatFormatting.RED));
            return false;
        }
        int checkRadius = (Integer)AASConfig.RALLY_BLOCK_RADIUS.get();
        AABB enemyBox = new AABB(pos).inflate((double)checkRadius);
        for (ServerPlayer p : level.getEntitiesOfClass(ServerPlayer.class, enemyBox)) {
            if (p.isSpectator() || p.getTeam() == null || p.getTeam().getName().equalsIgnoreCase(team)) continue;
            player.sendSystemMessage((Component)Component.literal((String)"Enemies nearby! Cannot deploy Rally.").withStyle(ChatFormatting.RED));
            return false;
        }
        int playerSquadId = player.getPersistentData().getInt("AAS_SquadID");
        AABB squadBox = new AABB(pos).inflate(5.0);
        int squadMatesNearby = 0;
        for (ServerPlayer p : level.getEntitiesOfClass(ServerPlayer.class, squadBox)) {
            if (p == player || p.isSpectator()) continue;
            int otherSquadId = p.getPersistentData().getInt("AAS_SquadID");
            if (playerSquadId == 0 || otherSquadId != playerSquadId) continue;
            ++squadMatesNearby;
        }
        if (squadMatesNearby >= 1 || player.isCreative()) {
            if (squad.rallyPos != null && level.isLoaded(squad.rallyPos)) {
                BlockEntity oldBe = level.getBlockEntity(squad.rallyPos);
                if (oldBe instanceof RallyPointBlockEntity) {
                    RallyPointBlockEntity rbe = (RallyPointBlockEntity)oldBe;
                    rbe.isDecay = true;
                }
                level.removeBlock(squad.rallyPos, false);
            }
            BlockState rallyState = team.equals("BLUE") ? ((Block)ModBlocks.BLUE_RALLY_BLOCK.get()).defaultBlockState() : ((Block)ModBlocks.RED_RALLY_BLOCK.get()).defaultBlockState();
            level.setBlock(pos, rallyState, 3);
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof RallyPointBlockEntity) {
                RallyPointBlockEntity rbe = (RallyPointBlockEntity)be;
                rbe.setSquadId(squad.id);
            }
            squad.rallyPos = pos;
            squad.rallyDimension = level.dimension().location().toString();
            squad.rallyExpiryTick = level.getGameTime() + 12000L;
            if (be instanceof RallyPointBlockEntity) {
                RallyPointBlockEntity rbe = (RallyPointBlockEntity)be;
                rbe.setExpiryTick(squad.rallyExpiryTick);
            }
            player.sendSystemMessage((Component)Component.literal((String)"Squad Rally Point Deployed!").withStyle(ChatFormatting.GREEN));
            if (team.equals("BLUE")) {
                data.blueRallies.add(pos);
            } else {
                data.redRallies.add(pos);
            }
            data.setDirty();
            PacketHandler.sendToAllClients(level, data);
            PacketHandler.INSTANCE.send(PacketDistributor.DIMENSION.with(() -> ((ServerLevel)level).dimension()), (Object)new PacketSyncSquads(data.squads));
            return true;
        }
        player.sendSystemMessage((Component)Component.literal((String)"Need at least 1 SQUAD MEMBER nearby!").withStyle(ChatFormatting.RED));
        return false;
    }
}

