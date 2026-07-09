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
        buf.writeBlockPos(msg.pos);
        buf.writeInt(msg.rotation);
    }

    public static PacketBuildRequest decode(FriendlyByteBuf buf) {
        return new PacketBuildRequest(buf.readInt(), buf.readBlockPos(), buf.readInt());
    }

    public static void handle(PacketBuildRequest msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            boolean hasRadio;
            ServerPlayer player = ((NetworkEvent.Context)ctx.get()).getSender();
            if (player == null) {
                return;
            }
            ServerLevel level = player.serverLevel();
            if (player.distanceToSqr((double)msg.pos.getX(), (double)msg.pos.getY(), (double)msg.pos.getZ()) > 64.0) {
                return;
            }
            boolean bl = hasRadio = player.getMainHandItem().getItem() == ModItems.SQUAD_LEADER_RADIO.get() || player.getOffhandItem().getItem() == ModItems.SQUAD_LEADER_RADIO.get();
            if (!hasRadio && !player.isCreative()) {
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
            if (player.getTeam() != null) {
                team = player.getTeam().getName();
            }
            boolean isCreative = player.isCreative();
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
                    BlockState state = (BlockState)((BlockState)((BlockState)((Block)ModBlocks.WALL_BLOCK.get()).defaultBlockState().setValue((Property)WallBlock.FACING, (Comparable)facing)).setValue((Property)WallBlock.CONSTRUCTED, (Comparable)Boolean.valueOf(false))).setValue((Property)WallBlock.VALID, (Comparable)Boolean.valueOf(true));
                    level.setBlock(msg.pos, state, 3);
                    PacketBuildRequest.setupWallEntity(level, msg.pos, team, false, null);
                } else {
                    PacketBuildRequest.sendNoMaterialsMessage(player, cost);
                }
            } else if (msg.structureId == 11) {
                int cost = 10;
                ArrayList<BlockPos> wallParts = new ArrayList<BlockPos>();
                wallParts.add(msg.pos);
                wallParts.add(msg.pos.above());
                BlockPos secondColPos = PacketBuildRequest.getRelativePos(msg.pos, facing);
                wallParts.add(secondColPos);
                wallParts.add(secondColPos.above());
                PacketBuildRequest.attemptBuildMulti(player, level, wallParts, facing, team, cost);
            } else if (msg.structureId == 12) {
                int cost = 15;
                ArrayList<BlockPos> wallParts = new ArrayList<BlockPos>();
                for (int h = 0; h < 3; ++h) {
                    BlockPos columnBase = PacketBuildRequest.getRelativePos(msg.pos, facing, h);
                    for (int v = 0; v < 3; ++v) {
                        wallParts.add(columnBase.above(v));
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
                        BlockState state = (BlockState)((BlockState)((BlockState)((Block)ModBlocks.BARBED_WIRE_BLOCK.get()).defaultBlockState().setValue((Property)BarbedWireBlock.FACING, (Comparable)facing)).setValue((Property)BarbedWireBlock.CONSTRUCTED, (Comparable)Boolean.valueOf(false))).setValue((Property)BarbedWireBlock.VALID, (Comparable)Boolean.valueOf(true));
                        level.setBlock(p, state, 3);
                    }
                    for (BlockPos p : parts) {
                        BlockEntity be = level.getBlockEntity(p);
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
                    if (level.setBlock(msg.pos, m2State = (BlockState)((BlockState)((Block)ModBlocks.M2_CONSTRUCTION_BLOCK.get()).defaultBlockState().setValue((Property)M2ConstructionBlock.FACING, (Comparable)facing)).setValue((Property)M2ConstructionBlock.VALID, (Comparable)Boolean.valueOf(true)), 3)) {
                        BlockEntity be = level.getBlockEntity(msg.pos);
                        if (be instanceof M2ConstructionBlockEntity) {
                            M2ConstructionBlockEntity m2 = (M2ConstructionBlockEntity)be;
                            m2.setTeam(team);
                        }
                        player.sendSystemMessage((Component)Component.literal((String)"M2 Blueprint placed!").withStyle(ChatFormatting.GREEN));
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
                    if (level.setBlock(msg.pos, agsState = (BlockState)((BlockState)((Block)ModBlocks.AGS_CONSTRUCTION_BLOCK.get()).defaultBlockState().setValue((Property)AGSConstructionBlock.FACING, (Comparable)facing)).setValue((Property)AGSConstructionBlock.VALID, (Comparable)Boolean.valueOf(true)), 3)) {
                        BlockEntity be = level.getBlockEntity(msg.pos);
                        if (be instanceof AGSConstructionBlockEntity) {
                            AGSConstructionBlockEntity ags = (AGSConstructionBlockEntity)be;
                            ags.setTeam(team);
                        }
                        player.sendSystemMessage((Component)Component.literal((String)"AGS-30 Blueprint placed!").withStyle(ChatFormatting.GREEN));
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
                    if (level.setBlock(msg.pos, state = (BlockState)((BlockState)((Block)ModBlocks.MORTAR_CONSTRUCTION_BLOCK.get()).defaultBlockState().setValue((Property)MortarConstructionBlock.FACING, (Comparable)facing)).setValue((Property)MortarConstructionBlock.VALID, (Comparable)Boolean.valueOf(true)), 3)) {
                        BlockEntity be = level.getBlockEntity(msg.pos);
                        if (be instanceof MortarConstructionBlockEntity) {
                            MortarConstructionBlockEntity mortar = (MortarConstructionBlockEntity)be;
                            mortar.setTeam(team);
                        }
                        player.sendSystemMessage((Component)Component.literal((String)"Mortar Blueprint placed!").withStyle(ChatFormatting.GREEN));
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
                    if (level.setBlock(msg.pos, state = (BlockState)((BlockState)((Block)ModBlocks.TOW_CONSTRUCTION_BLOCK.get()).defaultBlockState().setValue((Property)TOWConstructionBlock.FACING, (Comparable)facing)).setValue((Property)TOWConstructionBlock.VALID, (Comparable)Boolean.valueOf(true)), 3)) {
                        BlockEntity be = level.getBlockEntity(msg.pos);
                        if (be instanceof TOWConstructionBlockEntity) {
                            TOWConstructionBlockEntity tow = (TOWConstructionBlockEntity)be;
                            tow.setTeam(team);
                        }
                        player.sendSystemMessage((Component)Component.literal((String)"TOW Blueprint placed!").withStyle(ChatFormatting.GREEN));
                    }
                } else {
                    PacketBuildRequest.sendNoMaterialsMessage(player, cost);
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }

    private static boolean canPlaceAt(ServerLevel level, BlockPos pos) {
        return level.getBlockState(pos).canBeReplaced();
    }

    private static void sendBlockedMessage(ServerPlayer player) {
        player.sendSystemMessage((Component)Component.literal((String)"Space is blocked!").withStyle(ChatFormatting.RED));
    }

    private static void sendNoMaterialsMessage(ServerPlayer player, int cost) {
        player.sendSystemMessage((Component)Component.literal((String)("Need " + cost + " Materials!")).withStyle(ChatFormatting.RED));
    }

    private static BlockPos getRelativePos(BlockPos start, Direction facing) {
        return PacketBuildRequest.getRelativePos(start, facing, 1);
    }

    private static BlockPos getRelativePos(BlockPos start, Direction facing, int offset) {
        if (facing == Direction.NORTH) {
            return start.east(offset);
        }
        if (facing == Direction.WEST) {
            return start.north(offset);
        }
        if (facing == Direction.SOUTH) {
            return start.west(offset);
        }
        if (facing == Direction.EAST) {
            return start.south(offset);
        }
        return start;
    }

    private static void attemptBuildMulti(ServerPlayer player, ServerLevel level, List<BlockPos> parts, Direction facing, String team, int cost) {
        for (BlockPos p : parts) {
            if (PacketBuildRequest.canPlaceAt(level, p)) continue;
            PacketBuildRequest.sendBlockedMessage(player);
            return;
        }
        if (player.isCreative() || PacketBuildRequest.hasMaterials(level, parts.get(0), team, cost)) {
            if (!player.isCreative()) {
                PacketBuildRequest.consumeMaterials(level, parts.get(0), team, cost);
            }
            for (BlockPos p : parts) {
                BlockState state = (BlockState)((BlockState)((BlockState)((Block)ModBlocks.WALL_BLOCK.get()).defaultBlockState().setValue((Property)WallBlock.FACING, (Comparable)facing)).setValue((Property)WallBlock.CONSTRUCTED, (Comparable)Boolean.valueOf(false))).setValue((Property)WallBlock.VALID, (Comparable)Boolean.valueOf(true));
                level.setBlock(p, state, 3);
            }
            for (BlockPos p : parts) {
                PacketBuildRequest.setupWallEntity(level, p, team, true, parts);
            }
        } else {
            PacketBuildRequest.sendNoMaterialsMessage(player, cost);
        }
    }

    private static void setupWallEntity(ServerLevel level, BlockPos pos, String team, boolean multi, List<BlockPos> links) {
        BlockEntity be = level.getBlockEntity(pos);
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
        String currentDim = level.dimension().location().toString();
        int hubRadius = (Integer)AASConfig.HUB_BUILD_RADIUS.get();
        double maxHubSq = hubRadius * hubRadius;
        for (AASWorldData.HubInfo hubInfo : data.hubs) {
            HubBlockEntity hub;
            BlockEntity be;
            if (hubInfo.dimension != null && !hubInfo.dimension.equals(currentDim) || !(hubInfo.pos.distSqr((Vec3i)pos) <= maxHubSq) || !level.isLoaded(hubInfo.pos) || !((be = level.getBlockEntity(hubInfo.pos)) instanceof HubBlockEntity) || !(hub = (HubBlockEntity)be).getTeam().equalsIgnoreCase(team) && !hub.getTeam().equals("NEUTRAL")) continue;
            totalMaterials += hub.getMaterials();
        }
        int crateRadius = (Integer)AASConfig.CRATE_BUILD_RADIUS.get();
        double maxCrateSq = crateRadius * crateRadius;
        AABB searchArea = new AABB(pos).inflate((double)crateRadius);
        List crates = level.getEntitiesOfClass(SupplyCrateEntity.class, searchArea);
        for (SupplyCrateEntity crate : crates) {
            if (!crate.getTeamOwner().equalsIgnoreCase(team) && !crate.getTeamOwner().equals("NEUTRAL") || !(crate.distanceToSqr((double)pos.getX() + 0.5, (double)pos.getY() + 0.5, (double)pos.getZ() + 0.5) <= maxCrateSq)) continue;
            totalMaterials += crate.getMaterials();
        }
        return totalMaterials >= cost;
    }

    private static void consumeMaterials(ServerLevel level, BlockPos pos, String team, int cost) {
        int remainingToDeduct = cost;
        AASWorldData data = AASWorldData.get(level);
        String currentDim = level.dimension().location().toString();
        int crateRadius = (Integer)AASConfig.CRATE_BUILD_RADIUS.get();
        double maxCrateSq = crateRadius * crateRadius;
        AABB searchArea = new AABB(pos).inflate((double)crateRadius);
        List crates = level.getEntitiesOfClass(SupplyCrateEntity.class, searchArea);
        for (SupplyCrateEntity crate : crates) {
            if (remainingToDeduct <= 0) break;
            if (!crate.getTeamOwner().equalsIgnoreCase(team) && !crate.getTeamOwner().equals("NEUTRAL") || !(crate.distanceToSqr((double)pos.getX() + 0.5, (double)pos.getY() + 0.5, (double)pos.getZ() + 0.5) <= maxCrateSq)) continue;
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
                if (hubInfo.dimension != null && !hubInfo.dimension.equals(currentDim) || !(hubInfo.pos.distSqr((Vec3i)pos) <= maxHubSq) || !level.isLoaded(hubInfo.pos) || !((be = level.getBlockEntity(hubInfo.pos)) instanceof HubBlockEntity) || !(hub = (HubBlockEntity)be).getTeam().equalsIgnoreCase(team) && !hub.getTeam().equals("NEUTRAL")) continue;
                int hMats = hub.getMaterials();
                int take = Math.min(hMats, remainingToDeduct);
                hub.consumeMaterials(take);
                remainingToDeduct -= take;
            }
        }
    }
}

