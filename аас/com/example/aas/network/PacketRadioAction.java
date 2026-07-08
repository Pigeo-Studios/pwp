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
            ServerLevel currentLevel = player.m_284548_();
            AASWorldData data = AASWorldData.get(currentLevel);
            String pName = player.m_6302_();
            AASWorldData.Squad playerSquad = null;
            boolean isLeader = false;
            for (AASWorldData.Squad s : data.squads) {
                if (!s.members.contains(pName)) continue;
                playerSquad = s;
                if (!s.leader.equals(pName)) break;
                isLeader = true;
                break;
            }
            if (!player.m_7500_()) {
                if (player.m_5647_() == null) {
                    player.m_213846_((Component)Component.m_237113_((String)"Access Denied: You must be in a TEAM!").m_130940_(ChatFormatting.RED));
                    return;
                }
                if (playerSquad == null) {
                    player.m_213846_((Component)Component.m_237113_((String)"Access Denied: You must be in a SQUAD in this world!").m_130940_(ChatFormatting.RED));
                    return;
                }
                if (!isLeader) {
                    player.m_213846_((Component)Component.m_237113_((String)"Access Denied: You must be a Squad Leader!").m_130940_(ChatFormatting.RED));
                    return;
                }
            }
            ItemStack stack = player.m_21205_();
            long currentGameTime = currentLevel.m_46467_();
            if (msg.actionId == 0) {
                long lastUse = playerSquad.nextRallyAvailableTick;
                if (currentGameTime < lastUse && !player.m_7500_()) {
                    long timeLeft = (lastUse - currentGameTime) / 20L;
                    player.m_213846_((Component)Component.m_237113_((String)("Rally Point Cooldown: " + timeLeft + "s")).m_130940_(ChatFormatting.RED));
                    return;
                }
                String team = "NEUTRAL";
                if (player.m_5647_() != null) {
                    String rawTeamName = player.m_5647_().m_5758_();
                    if (rawTeamName.equalsIgnoreCase("Blue")) {
                        team = "BLUE";
                    } else if (rawTeamName.equalsIgnoreCase("Red")) {
                        team = "RED";
                    }
                }
                if (team.equals("NEUTRAL") && player.m_7500_()) {
                    team = "BLUE";
                }
                if (PacketRadioAction.trySpawnRally(player, currentLevel, team, playerSquad, data)) {
                    playerSquad.nextRallyAvailableTick = currentGameTime + 6000L;
                    player.m_213846_((Component)Component.m_237113_((String)"Squad Rally Point Deployed!").m_130940_(ChatFormatting.GREEN));
                } else {
                    playerSquad.nextRallyAvailableTick = currentGameTime + 300L;
                }
                data.m_77762_();
                PacketHandler.INSTANCE.send(PacketDistributor.DIMENSION.with(() -> ((Level)player.m_9236_()).m_46472_()), (Object)new PacketSyncSquads(data.squads));
            } else {
                PacketRadioAction.handleConstructionLogic(msg.actionId, player, currentLevel, data);
            }
        });
        ctx.get().setPacketHandled(true);
    }

    private static void handleConstructionLogic(int actionId, ServerPlayer player, ServerLevel level, AASWorldData data) {
        BlockPos targetPos = player.m_20183_();
        if (!level.m_8055_(targetPos).m_247087_() && actionId != 14) {
            targetPos = targetPos.m_121945_(player.m_6350_());
        }
        if (actionId == 14) {
            String playerTeam = player.m_5647_() != null ? player.m_5647_().m_5758_().toUpperCase() : "NEUTRAL";
            long hubCount = data.hubs.stream().filter(h -> h.team.equalsIgnoreCase(playerTeam)).count();
            if (hubCount >= (long)((Integer)AASConfig.MAX_HUBS_PER_TEAM.get()).intValue() && !player.m_7500_()) {
                player.m_213846_((Component)Component.m_237113_((String)"FOB Limit Reached for this world!").m_130940_(ChatFormatting.RED));
                return;
            }
            for (AASWorldData.HubInfo existingHub : data.hubs) {
                if (!existingHub.team.equalsIgnoreCase(playerTeam) || !(existingHub.pos.m_123331_((Vec3i)targetPos) < (double)((Integer)AASConfig.MIN_HUB_DISTANCE.get() * (Integer)AASConfig.MIN_HUB_DISTANCE.get()))) continue;
                player.m_213846_((Component)Component.m_237113_((String)"Too close to friendly FOB!").m_130940_(ChatFormatting.RED));
                return;
            }
            if (((Boolean)AASConfig.HUB_PLACEMENT_REQUIRES_CRATE.get()).booleanValue() && !player.m_7500_()) {
                double crateCheckRad = 50.0;
                AABB area = new AABB(targetPos).m_82400_(crateCheckRad);
                List crates = level.m_45976_(SupplyCrateEntity.class, area);
                SupplyCrateEntity targetCrate = crates.stream().filter(c -> c.getTeamOwner().equals("NEUTRAL") || c.getTeamOwner().equalsIgnoreCase(playerTeam)).findFirst().orElse(null);
                if (targetCrate == null) {
                    player.m_213846_((Component)Component.m_237113_((String)"FOB placement requires a Supply Crate within 50 blocks!").m_130940_(ChatFormatting.RED));
                    return;
                }
                targetCrate.m_146870_();
                player.m_213846_((Component)Component.m_237113_((String)"Supply Crate consumed for FOB placement.").m_130940_(ChatFormatting.YELLOW));
            }
            level.m_7731_(targetPos, ((Block)ModBlocks.HUB_BLOCK.get()).m_49966_(), 3);
            BlockEntity be = level.m_7702_(targetPos);
            if (be instanceof HubBlockEntity) {
                HubBlockEntity hubEntity = (HubBlockEntity)be;
                hubEntity.setTeam(playerTeam);
                level.m_7260_(targetPos, level.m_8055_(targetPos), level.m_8055_(targetPos), 3);
            }
            data.hubs.add(new AASWorldData.HubInfo(targetPos, playerTeam, false, level.m_46472_().m_135782_().toString()));
            data.m_77762_();
            PacketHandler.sendToAllClients(level, data);
            player.m_213846_((Component)Component.m_237113_((String)"FOB Blueprint placed!").m_130940_(ChatFormatting.GREEN));
        } else if (actionId == 20) {
            PacketRadioAction.placeBlueprint(level, targetPos, player, (BlockState)((Block)ModBlocks.M2_CONSTRUCTION_BLOCK.get()).m_49966_().m_61124_((Property)M2ConstructionBlock.FACING, (Comparable)player.m_6350_().m_122424_()), "M2");
        } else if (actionId == 21) {
            PacketRadioAction.placeBlueprint(level, targetPos, player, (BlockState)((Block)ModBlocks.AGS_CONSTRUCTION_BLOCK.get()).m_49966_().m_61124_((Property)AGSConstructionBlock.FACING, (Comparable)player.m_6350_().m_122424_()), "AGS");
        } else if (actionId == 22) {
            PacketRadioAction.placeBlueprint(level, targetPos, player, (BlockState)((Block)ModBlocks.MORTAR_CONSTRUCTION_BLOCK.get()).m_49966_().m_61124_((Property)MortarConstructionBlock.FACING, (Comparable)player.m_6350_().m_122424_()), "Mortar");
        } else if (actionId == 23) {
            PacketRadioAction.placeBlueprint(level, targetPos, player, (BlockState)((Block)ModBlocks.TOW_CONSTRUCTION_BLOCK.get()).m_49966_().m_61124_((Property)TOWConstructionBlock.FACING, (Comparable)player.m_6350_().m_122424_()), "TOW");
        }
    }

    private static void placeBlueprint(ServerLevel level, BlockPos pos, ServerPlayer player, BlockState state, String name) {
        String pTeam;
        if (!level.m_8055_(pos).m_60795_() && !level.m_8055_(pos).m_247087_()) {
            return;
        }
        level.m_7731_(pos, state, 3);
        BlockEntity be = level.m_7702_(pos);
        String string = pTeam = player.m_5647_() != null ? player.m_5647_().m_5758_() : "NEUTRAL";
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
        player.m_213846_((Component)Component.m_237113_((String)(name + " Blueprint placed.")).m_130940_(ChatFormatting.GREEN));
    }

    private static boolean trySpawnRally(ServerPlayer player, ServerLevel level, String team, AASWorldData.Squad squad, AASWorldData data) {
        BlockPos pos = player.m_20183_();
        for (AASWorldData.CapturePoint point : data.capturePoints) {
            Vec3 center = point.area.m_82399_();
            if (!(pos.m_203198_(center.f_82479_, center.f_82480_, center.f_82481_) < (double)((Integer)AASConfig.MIN_RALLY_POINT_DISTANCE.get() * (Integer)AASConfig.MIN_RALLY_POINT_DISTANCE.get()))) continue;
            player.m_213846_((Component)Component.m_237113_((String)"Too close to Capture Point!").m_130940_(ChatFormatting.RED));
            return false;
        }
        int checkRadius = (Integer)AASConfig.RALLY_BLOCK_RADIUS.get();
        AABB enemyBox = new AABB(pos).m_82400_((double)checkRadius);
        for (ServerPlayer p : level.m_45976_(ServerPlayer.class, enemyBox)) {
            if (p.m_5833_() || p.m_5647_() == null || p.m_5647_().m_5758_().equalsIgnoreCase(team)) continue;
            player.m_213846_((Component)Component.m_237113_((String)"Enemies nearby! Cannot deploy Rally.").m_130940_(ChatFormatting.RED));
            return false;
        }
        int playerSquadId = player.getPersistentData().m_128451_("AAS_SquadID");
        AABB squadBox = new AABB(pos).m_82400_(5.0);
        int squadMatesNearby = 0;
        for (ServerPlayer p : level.m_45976_(ServerPlayer.class, squadBox)) {
            if (p == player || p.m_5833_()) continue;
            int otherSquadId = p.getPersistentData().m_128451_("AAS_SquadID");
            if (playerSquadId == 0 || otherSquadId != playerSquadId) continue;
            ++squadMatesNearby;
        }
        if (squadMatesNearby >= 1 || player.m_7500_()) {
            if (squad.rallyPos != null && level.m_46749_(squad.rallyPos)) {
                BlockEntity oldBe = level.m_7702_(squad.rallyPos);
                if (oldBe instanceof RallyPointBlockEntity) {
                    RallyPointBlockEntity rbe = (RallyPointBlockEntity)oldBe;
                    rbe.isDecay = true;
                }
                level.m_7471_(squad.rallyPos, false);
            }
            BlockState rallyState = team.equals("BLUE") ? ((Block)ModBlocks.BLUE_RALLY_BLOCK.get()).m_49966_() : ((Block)ModBlocks.RED_RALLY_BLOCK.get()).m_49966_();
            level.m_7731_(pos, rallyState, 3);
            BlockEntity be = level.m_7702_(pos);
            if (be instanceof RallyPointBlockEntity) {
                RallyPointBlockEntity rbe = (RallyPointBlockEntity)be;
                rbe.setSquadId(squad.id);
            }
            squad.rallyPos = pos;
            squad.rallyDimension = level.m_46472_().m_135782_().toString();
            squad.rallyExpiryTick = level.m_46467_() + 12000L;
            if (be instanceof RallyPointBlockEntity) {
                RallyPointBlockEntity rbe = (RallyPointBlockEntity)be;
                rbe.setExpiryTick(squad.rallyExpiryTick);
            }
            player.m_213846_((Component)Component.m_237113_((String)"Squad Rally Point Deployed!").m_130940_(ChatFormatting.GREEN));
            if (team.equals("BLUE")) {
                data.blueRallies.add(pos);
            } else {
                data.redRallies.add(pos);
            }
            data.m_77762_();
            PacketHandler.sendToAllClients(level, data);
            PacketHandler.INSTANCE.send(PacketDistributor.DIMENSION.with(() -> ((ServerLevel)level).m_46472_()), (Object)new PacketSyncSquads(data.squads));
            return true;
        }
        player.m_213846_((Component)Component.m_237113_((String)"Need at least 1 SQUAD MEMBER nearby!").m_130940_(ChatFormatting.RED));
        return false;
    }
}

