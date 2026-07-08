/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.ChatFormatting
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.Direction
 *  net.minecraft.core.Vec3i
 *  net.minecraft.network.FriendlyByteBuf
 *  net.minecraft.network.chat.Component
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.server.level.ServerPlayer
 *  net.minecraft.world.level.block.Block
 *  net.minecraft.world.level.block.entity.BlockEntity
 *  net.minecraft.world.level.block.state.BlockState
 *  net.minecraft.world.level.block.state.properties.Property
 *  net.minecraft.world.phys.AABB
 *  net.minecraftforge.network.NetworkEvent$Context
 */
package com.example.aas.network;

import com.example.aas.block.AGSConstructionBlock;
import com.example.aas.block.AGSConstructionBlockEntity;
import com.example.aas.block.BarbedWireBlock;
import com.example.aas.block.BarbedWireBlockEntity;
import com.example.aas.block.HubBlockEntity;
import com.example.aas.block.M2ConstructionBlock;
import com.example.aas.block.M2ConstructionBlockEntity;
import com.example.aas.block.ModBlocks;
import com.example.aas.block.MortarConstructionBlock;
import com.example.aas.block.MortarConstructionBlockEntity;
import com.example.aas.block.TOWConstructionBlock;
import com.example.aas.block.TOWConstructionBlockEntity;
import com.example.aas.block.WallBlock;
import com.example.aas.block.WallBlockEntity;
import com.example.aas.config.AASConfig;
import com.example.aas.entity.SupplyCrateEntity;
import com.example.aas.item.ModItems;
import com.example.aas.world.AASWorldData;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.network.NetworkEvent;

public class PacketBuildRequest {
    private final int structureId;
    private final BlockPos pos;
    private final int rotation;

    public PacketBuildRequest(int structureId, BlockPos pos, int rotation) {
        this.structureId = structureId;
        this.pos = pos;
        this.rotation = rotation;
    }

    public static void encode(PacketBuildRequest msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.structureId);
        buf.m_130064_(msg.pos);
        buf.writeInt(msg.rotation);
    }

    public static PacketBuildRequest decode(FriendlyByteBuf buf) {
        return new PacketBuildRequest(buf.readInt(), buf.m_130135_(), buf.readInt());
    }

    public static void handle(PacketBuildRequest msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            boolean hasRadio;
            ServerPlayer player = ((NetworkEvent.Context)ctx.get()).getSender();
            if (player == null) {
                return;
            }
            ServerLevel level = player.m_284548_();
            if (player.m_20275_((double)msg.pos.m_123341_(), (double)msg.pos.m_123342_(), (double)msg.pos.m_123343_()) > 64.0) {
                return;
            }
            boolean bl = hasRadio = player.m_21205_().m_41720_() == ModItems.SQUAD_LEADER_RADIO.get() || player.m_21206_().m_41720_() == ModItems.SQUAD_LEADER_RADIO.get();
            if (!hasRadio && !player.m_7500_()) {
                return;
            }
            int r = msg.rotation % 360;
            if (r < 0) {
                r += 360;
            }
            Direction facing = Direction.NORTH;
            if (r == 0) {
                facing = Direction.NORTH;
            } else if (r == 270) {
                facing = Direction.EAST;
            } else if (r == 180) {
                facing = Direction.SOUTH;
            } else if (r == 90) {
                facing = Direction.WEST;
            }
            String team = "NEUTRAL";
            if (player.m_5647_() != null) {
                team = player.m_5647_().m_5758_();
            }
            boolean isCreative = player.m_7500_();
            if (msg.structureId == 10) {
                int cost = 5;
                if (!PacketBuildRequest.canPlaceAt(level, msg.pos)) {
                    PacketBuildRequest.sendBlockedMessage(player);
                    return;
                }
                if (isCreative || PacketBuildRequest.hasMaterials(level, msg.pos, team, cost)) {
                    if (!isCreative) {
                        PacketBuildRequest.consumeMaterials(level, msg.pos, team, cost);
                    }
                    BlockState state = (BlockState)((BlockState)((BlockState)((Block)ModBlocks.WALL_BLOCK.get()).m_49966_().m_61124_((Property)WallBlock.FACING, (Comparable)facing)).m_61124_((Property)WallBlock.CONSTRUCTED, (Comparable)Boolean.valueOf(false))).m_61124_((Property)WallBlock.VALID, (Comparable)Boolean.valueOf(true));
                    level.m_7731_(msg.pos, state, 3);
                    PacketBuildRequest.setupWallEntity(level, msg.pos, team, false, null);
                } else {
                    PacketBuildRequest.sendNoMaterialsMessage(player, cost);
                }
            } else if (msg.structureId == 11) {
                int cost = 10;
                ArrayList<BlockPos> wallParts = new ArrayList<BlockPos>();
                wallParts.add(msg.pos);
                wallParts.add(msg.pos.m_7494_());
                BlockPos secondColPos = PacketBuildRequest.getRelativePos(msg.pos, facing);
                wallParts.add(secondColPos);
                wallParts.add(secondColPos.m_7494_());
                PacketBuildRequest.attemptBuildMulti(player, level, wallParts, facing, team, cost);
            } else if (msg.structureId == 12) {
                int cost = 15;
                ArrayList<BlockPos> wallParts = new ArrayList<BlockPos>();
                for (int h = 0; h < 3; ++h) {
                    BlockPos columnBase = PacketBuildRequest.getRelativePos(msg.pos, facing, h);
                    for (int v = 0; v < 3; ++v) {
                        wallParts.add(columnBase.m_6630_(v));
                    }
                }
                PacketBuildRequest.attemptBuildMulti(player, level, wallParts, facing, team, cost);
            } else if (msg.structureId == 13) {
                int cost = 25;
                ArrayList<BlockPos> parts = new ArrayList<BlockPos>();
                for (int h = 0; h < 3; ++h) {
                    parts.add(PacketBuildRequest.getRelativePos(msg.pos, facing, h));
                }
                boolean blocked = false;
                for (BlockPos p : parts) {
                    if (PacketBuildRequest.canPlaceAt(level, p)) continue;
                    blocked = true;
                    break;
                }
                if (blocked) {
                    PacketBuildRequest.sendBlockedMessage(player);
                    return;
                }
                if (isCreative || PacketBuildRequest.hasMaterials(level, (BlockPos)parts.get(0), team, cost)) {
                    if (!isCreative) {
                        PacketBuildRequest.consumeMaterials(level, (BlockPos)parts.get(0), team, cost);
                    }
                    for (BlockPos p : parts) {
                        BlockState state = (BlockState)((BlockState)((BlockState)((Block)ModBlocks.BARBED_WIRE_BLOCK.get()).m_49966_().m_61124_((Property)BarbedWireBlock.FACING, (Comparable)facing)).m_61124_((Property)BarbedWireBlock.CONSTRUCTED, (Comparable)Boolean.valueOf(false))).m_61124_((Property)BarbedWireBlock.VALID, (Comparable)Boolean.valueOf(true));
                        level.m_7731_(p, state, 3);
                    }
                    for (BlockPos p : parts) {
                        BlockEntity be = level.m_7702_(p);
                        if (!(be instanceof BarbedWireBlockEntity)) continue;
                        BarbedWireBlockEntity wire = (BarbedWireBlockEntity)be;
                        wire.setTeam(team);
                        wire.setLinkedWires(parts);
                    }
                } else {
                    PacketBuildRequest.sendNoMaterialsMessage(player, cost);
                }
            } else if (msg.structureId == 20) {
                int cost = 100;
                if (!PacketBuildRequest.canPlaceAt(level, msg.pos)) {
                    PacketBuildRequest.sendBlockedMessage(player);
                    return;
                }
                if (isCreative || PacketBuildRequest.hasMaterials(level, msg.pos, team, cost)) {
                    BlockState m2State;
                    if (!isCreative) {
                        PacketBuildRequest.consumeMaterials(level, msg.pos, team, cost);
                    }
                    if (level.m_7731_(msg.pos, m2State = (BlockState)((BlockState)((Block)ModBlocks.M2_CONSTRUCTION_BLOCK.get()).m_49966_().m_61124_((Property)M2ConstructionBlock.FACING, (Comparable)facing)).m_61124_((Property)M2ConstructionBlock.VALID, (Comparable)Boolean.valueOf(true)), 3)) {
                        BlockEntity be = level.m_7702_(msg.pos);
                        if (be instanceof M2ConstructionBlockEntity) {
                            M2ConstructionBlockEntity m2 = (M2ConstructionBlockEntity)be;
                            m2.setTeam(team);
                        }
                        player.m_213846_((Component)Component.m_237113_((String)"M2 Blueprint placed!").m_130940_(ChatFormatting.GREEN));
                    }
                } else {
                    PacketBuildRequest.sendNoMaterialsMessage(player, cost);
                }
            } else if (msg.structureId == 21) {
                int cost = 100;
                if (!PacketBuildRequest.canPlaceAt(level, msg.pos)) {
                    PacketBuildRequest.sendBlockedMessage(player);
                    return;
                }
                if (isCreative || PacketBuildRequest.hasMaterials(level, msg.pos, team, cost)) {
                    BlockState agsState;
                    if (!isCreative) {
                        PacketBuildRequest.consumeMaterials(level, msg.pos, team, cost);
                    }
                    if (level.m_7731_(msg.pos, agsState = (BlockState)((BlockState)((Block)ModBlocks.AGS_CONSTRUCTION_BLOCK.get()).m_49966_().m_61124_((Property)AGSConstructionBlock.FACING, (Comparable)facing)).m_61124_((Property)AGSConstructionBlock.VALID, (Comparable)Boolean.valueOf(true)), 3)) {
                        BlockEntity be = level.m_7702_(msg.pos);
                        if (be instanceof AGSConstructionBlockEntity) {
                            AGSConstructionBlockEntity ags = (AGSConstructionBlockEntity)be;
                            ags.setTeam(team);
                        }
                        player.m_213846_((Component)Component.m_237113_((String)"AGS-30 Blueprint placed!").m_130940_(ChatFormatting.GREEN));
                    }
                } else {
                    PacketBuildRequest.sendNoMaterialsMessage(player, cost);
                }
            } else if (msg.structureId == 22) {
                int cost = 300;
                if (!PacketBuildRequest.canPlaceAt(level, msg.pos)) {
                    PacketBuildRequest.sendBlockedMessage(player);
                    return;
                }
                if (isCreative || PacketBuildRequest.hasMaterials(level, msg.pos, team, cost)) {
                    BlockState state;
                    if (!isCreative) {
                        PacketBuildRequest.consumeMaterials(level, msg.pos, team, cost);
                    }
                    if (level.m_7731_(msg.pos, state = (BlockState)((BlockState)((Block)ModBlocks.MORTAR_CONSTRUCTION_BLOCK.get()).m_49966_().m_61124_((Property)MortarConstructionBlock.FACING, (Comparable)facing)).m_61124_((Property)MortarConstructionBlock.VALID, (Comparable)Boolean.valueOf(true)), 3)) {
                        BlockEntity be = level.m_7702_(msg.pos);
                        if (be instanceof MortarConstructionBlockEntity) {
                            MortarConstructionBlockEntity mortar = (MortarConstructionBlockEntity)be;
                            mortar.setTeam(team);
                        }
                        player.m_213846_((Component)Component.m_237113_((String)"Mortar Blueprint placed!").m_130940_(ChatFormatting.GREEN));
                    }
                } else {
                    PacketBuildRequest.sendNoMaterialsMessage(player, cost);
                }
            } else if (msg.structureId == 23) {
                int cost = 200;
                if (!PacketBuildRequest.canPlaceAt(level, msg.pos)) {
                    PacketBuildRequest.sendBlockedMessage(player);
                    return;
                }
                if (isCreative || PacketBuildRequest.hasMaterials(level, msg.pos, team, cost)) {
                    BlockState state;
                    if (!isCreative) {
                        PacketBuildRequest.consumeMaterials(level, msg.pos, team, cost);
                    }
                    if (level.m_7731_(msg.pos, state = (BlockState)((BlockState)((Block)ModBlocks.TOW_CONSTRUCTION_BLOCK.get()).m_49966_().m_61124_((Property)TOWConstructionBlock.FACING, (Comparable)facing)).m_61124_((Property)TOWConstructionBlock.VALID, (Comparable)Boolean.valueOf(true)), 3)) {
                        BlockEntity be = level.m_7702_(msg.pos);
                        if (be instanceof TOWConstructionBlockEntity) {
                            TOWConstructionBlockEntity tow = (TOWConstructionBlockEntity)be;
                            tow.setTeam(team);
                        }
                        player.m_213846_((Component)Component.m_237113_((String)"TOW Blueprint placed!").m_130940_(ChatFormatting.GREEN));
                    }
                } else {
                    PacketBuildRequest.sendNoMaterialsMessage(player, cost);
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }

    private static boolean canPlaceAt(ServerLevel level, BlockPos pos) {
        return level.m_8055_(pos).m_247087_();
    }

    private static void sendBlockedMessage(ServerPlayer player) {
        player.m_213846_((Component)Component.m_237113_((String)"Space is blocked!").m_130940_(ChatFormatting.RED));
    }

    private static void sendNoMaterialsMessage(ServerPlayer player, int cost) {
        player.m_213846_((Component)Component.m_237113_((String)("Need " + cost + " Materials!")).m_130940_(ChatFormatting.RED));
    }

    private static BlockPos getRelativePos(BlockPos start, Direction facing) {
        return PacketBuildRequest.getRelativePos(start, facing, 1);
    }

    private static BlockPos getRelativePos(BlockPos start, Direction facing, int offset) {
        if (facing == Direction.NORTH) {
            return start.m_122030_(offset);
        }
        if (facing == Direction.WEST) {
            return start.m_122013_(offset);
        }
        if (facing == Direction.SOUTH) {
            return start.m_122025_(offset);
        }
        if (facing == Direction.EAST) {
            return start.m_122020_(offset);
        }
        return start;
    }

    private static void attemptBuildMulti(ServerPlayer player, ServerLevel level, List<BlockPos> parts, Direction facing, String team, int cost) {
        for (BlockPos p : parts) {
            if (PacketBuildRequest.canPlaceAt(level, p)) continue;
            PacketBuildRequest.sendBlockedMessage(player);
            return;
        }
        if (player.m_7500_() || PacketBuildRequest.hasMaterials(level, parts.get(0), team, cost)) {
            if (!player.m_7500_()) {
                PacketBuildRequest.consumeMaterials(level, parts.get(0), team, cost);
            }
            for (BlockPos p : parts) {
                BlockState state = (BlockState)((BlockState)((BlockState)((Block)ModBlocks.WALL_BLOCK.get()).m_49966_().m_61124_((Property)WallBlock.FACING, (Comparable)facing)).m_61124_((Property)WallBlock.CONSTRUCTED, (Comparable)Boolean.valueOf(false))).m_61124_((Property)WallBlock.VALID, (Comparable)Boolean.valueOf(true));
                level.m_7731_(p, state, 3);
            }
            for (BlockPos p : parts) {
                PacketBuildRequest.setupWallEntity(level, p, team, true, parts);
            }
        } else {
            PacketBuildRequest.sendNoMaterialsMessage(player, cost);
        }
    }

    private static void setupWallEntity(ServerLevel level, BlockPos pos, String team, boolean multi, List<BlockPos> links) {
        BlockEntity be = level.m_7702_(pos);
        if (be instanceof WallBlockEntity) {
            WallBlockEntity wall = (WallBlockEntity)be;
            wall.setTeam(team);
            if (multi && links != null) {
                wall.setLinkedWalls(links);
            }
        }
    }

    private static boolean hasMaterials(ServerLevel level, BlockPos pos, String team, int cost) {
        int totalMaterials = 0;
        AASWorldData data = AASWorldData.get(level);
        String currentDim = level.m_46472_().m_135782_().toString();
        int hubRadius = (Integer)AASConfig.HUB_BUILD_RADIUS.get();
        double maxHubSq = hubRadius * hubRadius;
        for (AASWorldData.HubInfo hubInfo : data.hubs) {
            HubBlockEntity hub;
            BlockEntity be;
            if (hubInfo.dimension != null && !hubInfo.dimension.equals(currentDim) || !(hubInfo.pos.m_123331_((Vec3i)pos) <= maxHubSq) || !level.m_46749_(hubInfo.pos) || !((be = level.m_7702_(hubInfo.pos)) instanceof HubBlockEntity) || !(hub = (HubBlockEntity)be).getTeam().equalsIgnoreCase(team) && !hub.getTeam().equals("NEUTRAL")) continue;
            totalMaterials += hub.getMaterials();
        }
        int crateRadius = (Integer)AASConfig.CRATE_BUILD_RADIUS.get();
        double maxCrateSq = crateRadius * crateRadius;
        AABB searchArea = new AABB(pos).m_82400_((double)crateRadius);
        List crates = level.m_45976_(SupplyCrateEntity.class, searchArea);
        for (SupplyCrateEntity crate : crates) {
            if (!crate.getTeamOwner().equalsIgnoreCase(team) && !crate.getTeamOwner().equals("NEUTRAL") || !(crate.m_20275_((double)pos.m_123341_() + 0.5, (double)pos.m_123342_() + 0.5, (double)pos.m_123343_() + 0.5) <= maxCrateSq)) continue;
            totalMaterials += crate.getMaterials();
        }
        return totalMaterials >= cost;
    }

    private static void consumeMaterials(ServerLevel level, BlockPos pos, String team, int cost) {
        int remainingToDeduct = cost;
        AASWorldData data = AASWorldData.get(level);
        String currentDim = level.m_46472_().m_135782_().toString();
        int crateRadius = (Integer)AASConfig.CRATE_BUILD_RADIUS.get();
        double maxCrateSq = crateRadius * crateRadius;
        AABB searchArea = new AABB(pos).m_82400_((double)crateRadius);
        List crates = level.m_45976_(SupplyCrateEntity.class, searchArea);
        for (SupplyCrateEntity crate : crates) {
            if (remainingToDeduct <= 0) break;
            if (!crate.getTeamOwner().equalsIgnoreCase(team) && !crate.getTeamOwner().equals("NEUTRAL") || !(crate.m_20275_((double)pos.m_123341_() + 0.5, (double)pos.m_123342_() + 0.5, (double)pos.m_123343_() + 0.5) <= maxCrateSq)) continue;
            int cMats = crate.getMaterials();
            int take = Math.min(cMats, remainingToDeduct);
            crate.setMaterials(cMats - take);
            remainingToDeduct -= take;
        }
        if (remainingToDeduct > 0) {
            int hubRadius = (Integer)AASConfig.HUB_BUILD_RADIUS.get();
            double maxHubSq = hubRadius * hubRadius;
            for (AASWorldData.HubInfo hubInfo : data.hubs) {
                HubBlockEntity hub;
                BlockEntity be;
                if (remainingToDeduct <= 0) break;
                if (hubInfo.dimension != null && !hubInfo.dimension.equals(currentDim) || !(hubInfo.pos.m_123331_((Vec3i)pos) <= maxHubSq) || !level.m_46749_(hubInfo.pos) || !((be = level.m_7702_(hubInfo.pos)) instanceof HubBlockEntity) || !(hub = (HubBlockEntity)be).getTeam().equalsIgnoreCase(team) && !hub.getTeam().equals("NEUTRAL")) continue;
                int hMats = hub.getMaterials();
                int take = Math.min(hMats, remainingToDeduct);
                hub.consumeMaterials(take);
                remainingToDeduct -= take;
            }
        }
    }
}

