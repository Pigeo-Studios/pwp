package com.pigeostudios.pwp.warfare.network;

import com.pigeostudios.pwp.warfare.block.BarbedWireBlock;
import com.pigeostudios.pwp.warfare.block.BarbedWireBlockEntity;
import com.pigeostudios.pwp.warfare.block.HubBlockEntity;
import com.pigeostudios.pwp.warfare.block.ModBlocks;
import com.pigeostudios.pwp.warfare.block.MortarConstructionBlock;
import com.pigeostudios.pwp.warfare.block.MortarConstructionBlockEntity;
import com.pigeostudios.pwp.warfare.block.RebConstructionBlock;
import com.pigeostudios.pwp.warfare.block.RebConstructionBlockEntity;
import com.pigeostudios.pwp.warfare.block.TOWConstructionBlock;
import com.pigeostudios.pwp.warfare.block.TOWConstructionBlockEntity;
import com.pigeostudios.pwp.warfare.block.VehicleStationBlock;
import com.pigeostudios.pwp.warfare.block.VehicleStationBlockEntity;
import com.pigeostudios.pwp.warfare.block.WallBlock;
import com.pigeostudios.pwp.warfare.block.WallBlockEntity;
import com.pigeostudios.pwp.warfare.build.Structure;
import com.pigeostudios.pwp.warfare.build.StructureRules;
import com.pigeostudios.pwp.warfare.config.WarfareConfig;
import com.pigeostudios.pwp.warfare.data.state.WarfareData;
import com.pigeostudios.pwp.warfare.data.state.WarfareKeys;
import com.pigeostudios.pwp.warfare.entity.SupplyCrateEntity;
import com.pigeostudios.pwp.warfare.item.ModItems;
import com.pigeostudios.pwp.warfare.util.PlayerFeedback;
import com.pigeostudios.pwp.warfare.world.WarfareWorldData;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.network.NetworkEvent.Context;
import net.minecraftforge.registries.ForgeRegistries;

// Пакет запроса на строительство сооружения (стены, M2, АГС, миномёт, TOW)
// Отправляется клиентом при размещении чертежа конструкции
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

    // Обрабатывает запрос строительства: проверяет наличие радио, материалы,
    // размещает соответствующий блок конструкции в зависимости от structureId
    public static void handle(PacketBuildRequest msg, Supplier<Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player != null) {
                ServerLevel level = player.serverLevel();
                // раньше оба отказа были молчаливыми — игрок не понимал, почему стройка не ставится
                if (player.distanceToSqr(msg.pos.getX(), msg.pos.getY(), msg.pos.getZ()) > 64.0) {
                    PlayerFeedback.deny(player, "pwpwarfare.msg.build.too_far");
                    return;
                }
                if (!player.isCreative()
                        && player.getMainHandItem().getItem() != ModItems.SQUAD_LEADER_RADIO.get()
                        && player.getOffhandItem().getItem() != ModItems.SQUAD_LEADER_RADIO.get()) {
                    PlayerFeedback.deny(player, "pwpwarfare.msg.build.need_radio");
                    return;
                }
                if (!(player.distanceToSqr(msg.pos.getX(), msg.pos.getY(), msg.pos.getZ()) > 64.0)) {
                    boolean hasRadio = player.getMainHandItem().getItem() == ModItems.SQUAD_LEADER_RADIO.get()
                            || player.getOffhandItem().getItem() == ModItems.SQUAD_LEADER_RADIO.get();
                    if (hasRadio || player.isCreative()) {
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
                        // каталог и условия (этап 17c): не в мейне врага, лимит таких построек у одного ФОБа
                        Structure structure = Structure.byWireId(msg.structureId);
                        String ruleDeny = StructureRules.check(
                                structure,
                                com.pigeostudios.pwp.warfare.server.BuildConditions.facts(
                                        level, msg.pos, team, structure, isCreative));
                        if (ruleDeny != null) {
                            if (StructureRules.FOB_LIMIT.equals(ruleDeny)) {
                                PlayerFeedback.deny(
                                        player,
                                        ruleDeny,
                                        Component.translatable("gui.pwpwarfare.radial.item." + structure.id()),
                                        structure.perFob);
                            } else {
                                PlayerFeedback.deny(player, ruleDeny);
                            }
                            return;
                        }
                        if (msg.structureId == 10) {
                            int cost = Structure.WALL_1X1.cost;
                            if (!canPlaceAt(level, msg.pos)) {
                                sendBlockedMessage(player);
                                return;
                            }

                            if (!isCreative && !hasMaterials(level, msg.pos, team, cost)) {
                                sendNoMaterialsMessage(player, cost);
                            } else {
                                if (!isCreative) {
                                    consumeMaterials(level, msg.pos, team, cost);
                                }

                                BlockState state =
                                        (BlockState) ((BlockState) ((BlockState) ((Block) ModBlocks.WALL_BLOCK.get())
                                                                .defaultBlockState()
                                                                .setValue(WallBlock.FACING, facing))
                                                        .setValue(WallBlock.CONSTRUCTED, false))
                                                .setValue(WallBlock.VALID, true);
                                level.setBlock(msg.pos, state, 3);
                                setupWallEntity(level, msg.pos, team, false, null);
                            }
                        } else if (msg.structureId == 11) {
                            int cost = Structure.WALL_2X2.cost;
                            List<BlockPos> wallParts = new ArrayList<>();
                            wallParts.add(msg.pos);
                            wallParts.add(msg.pos.above());
                            BlockPos secondColPos = getRelativePos(msg.pos, facing);
                            wallParts.add(secondColPos);
                            wallParts.add(secondColPos.above());
                            attemptBuildMulti(player, level, wallParts, facing, team, cost);
                        } else if (msg.structureId == 12) {
                            int cost = Structure.WALL_3X3.cost;
                            List<BlockPos> wallParts = new ArrayList<>();

                            for (int h = 0; h < 3; h++) {
                                BlockPos columnBase = getRelativePos(msg.pos, facing, h);

                                for (int v = 0; v < 3; v++) {
                                    wallParts.add(columnBase.above(v));
                                }
                            }

                            attemptBuildMulti(player, level, wallParts, facing, team, cost);
                        } else if (msg.structureId == 13) {
                            int cost = Structure.WIRE.cost;
                            List<BlockPos> parts = new ArrayList<>();

                            for (int h = 0; h < 3; h++) {
                                parts.add(getRelativePos(msg.pos, facing, h));
                            }

                            boolean blocked = false;

                            for (BlockPos p : parts) {
                                if (!canPlaceAt(level, p)) {
                                    blocked = true;
                                    break;
                                }
                            }

                            if (blocked) {
                                sendBlockedMessage(player);
                                return;
                            }

                            if (!isCreative && !hasMaterials(level, parts.get(0), team, cost)) {
                                sendNoMaterialsMessage(player, cost);
                            } else {
                                if (!isCreative) {
                                    consumeMaterials(level, parts.get(0), team, cost);
                                }

                                for (BlockPos p : parts) {
                                    BlockState state = (BlockState)
                                            ((BlockState) ((BlockState) ((Block) ModBlocks.BARBED_WIRE_BLOCK.get())
                                                                    .defaultBlockState()
                                                                    .setValue(BarbedWireBlock.FACING, facing))
                                                            .setValue(BarbedWireBlock.CONSTRUCTED, false))
                                                    .setValue(BarbedWireBlock.VALID, true);
                                    level.setBlock(p, state, 3);
                                }

                                for (BlockPos p : parts) {
                                    if (level.getBlockEntity(p) instanceof BarbedWireBlockEntity wire) {
                                        wire.setTeam(team);
                                        wire.setLinkedWires(parts);
                                    }
                                }
                            }
                        } else if (msg.structureId == 15) {
                            int cost = Structure.WALL_WIRE.cost;
                            List<BlockPos> wallParts = new ArrayList<>();
                            List<BlockPos> wireParts = new ArrayList<>();
                            boolean blocked = false;

                            for (int x = -1; x <= 1; x++) {
                                BlockPos pStep = getRelativePos(msg.pos, msg.rotation, x, 0, 0);
                                BlockPos pWall1 = getRelativePos(msg.pos, msg.rotation, x, 0, -1);
                                BlockPos pWall2 = getRelativePos(msg.pos, msg.rotation, x, 1, -1);
                                BlockPos pWire = getRelativePos(msg.pos, msg.rotation, x, 0, -2);
                                wallParts.add(pStep);
                                wallParts.add(pWall1);
                                wallParts.add(pWall2);
                                wireParts.add(pWire);
                            }

                            for (BlockPos p : wallParts) {
                                if (!canPlaceAt(level, p)) {
                                    blocked = true;
                                    break;
                                }
                            }

                            for (BlockPos p : wireParts) {
                                if (!canPlaceAt(level, p)) {
                                    blocked = true;
                                    break;
                                }
                            }

                            if (blocked) {
                                sendBlockedMessage(player);
                                return;
                            }

                            if (!isCreative && !hasMaterials(level, msg.pos, team, cost)) {
                                sendNoMaterialsMessage(player, cost);
                                return;
                            }

                            if (!isCreative) {
                                consumeMaterials(level, msg.pos, team, cost);
                            }

                            BlockState wallState =
                                    (BlockState) ((BlockState) ((BlockState) ((Block) ModBlocks.WALL_BLOCK.get())
                                                            .defaultBlockState()
                                                            .setValue(WallBlock.FACING, facing))
                                                    .setValue(WallBlock.CONSTRUCTED, false))
                                            .setValue(WallBlock.VALID, true);
                            BlockState wireState =
                                    (BlockState) ((BlockState) ((BlockState) ((Block) ModBlocks.BARBED_WIRE_BLOCK.get())
                                                            .defaultBlockState()
                                                            .setValue(BarbedWireBlock.FACING, facing))
                                                    .setValue(BarbedWireBlock.CONSTRUCTED, false))
                                            .setValue(BarbedWireBlock.VALID, true);

                            for (BlockPos p : wallParts) {
                                level.setBlock(p, wallState, 3);
                            }

                            for (BlockPos p : wireParts) {
                                level.setBlock(p, wireState, 3);
                            }

                            for (BlockPos p : wallParts) {
                                setupWallEntity(level, p, team, true, wallParts);
                            }

                            for (BlockPos p : wireParts) {
                                if (level.getBlockEntity(p) instanceof BarbedWireBlockEntity wire) {
                                    wire.setTeam(team);
                                    wire.setLinkedWires(wireParts);
                                }
                            }
                        } else if (msg.structureId == 16) {
                            int cost = Structure.EMBRASURE.cost;
                            List<BlockPos> wallParts = new ArrayList<>();
                            List<BlockPos> slabParts = new ArrayList<>();
                            boolean blocked = false;

                            for (int x = -1; x <= 1; x++) {
                                for (int y = 0; y < 3; y++) {
                                    BlockPos p = getRelativePos(msg.pos, msg.rotation, x, y, 0);
                                    if (x == 0 && y == 1) {
                                        slabParts.add(p);
                                    } else {
                                        wallParts.add(p);
                                    }
                                }
                            }

                            for (BlockPos p : wallParts) {
                                if (!canPlaceAt(level, p)) {
                                    blocked = true;
                                    break;
                                }
                            }

                            for (BlockPos p : slabParts) {
                                if (!canPlaceAt(level, p)) {
                                    blocked = true;
                                    break;
                                }
                            }

                            if (blocked) {
                                sendBlockedMessage(player);
                                return;
                            }

                            if (!isCreative && !hasMaterials(level, msg.pos, team, cost)) {
                                sendNoMaterialsMessage(player, cost);
                                return;
                            }

                            if (!isCreative) {
                                consumeMaterials(level, msg.pos, team, cost);
                            }

                            BlockState wallState =
                                    (BlockState) ((BlockState) ((BlockState) ((Block) ModBlocks.WALL_BLOCK.get())
                                                            .defaultBlockState()
                                                            .setValue(WallBlock.FACING, facing))
                                                    .setValue(WallBlock.CONSTRUCTED, false))
                                            .setValue(WallBlock.VALID, true);
                            BlockState slabState =
                                    (BlockState) ((BlockState) ((BlockState) ((Block) ModBlocks.WALL_SLAB_BLOCK.get())
                                                            .defaultBlockState()
                                                            .setValue(WallBlock.FACING, facing))
                                                    .setValue(WallBlock.CONSTRUCTED, false))
                                            .setValue(WallBlock.VALID, true);

                            for (BlockPos p : wallParts) {
                                level.setBlock(p, wallState, 3);
                            }

                            for (BlockPos p : slabParts) {
                                level.setBlock(p, slabState, 3);
                            }

                            List<BlockPos> allParts = new ArrayList<>(wallParts);
                            allParts.addAll(slabParts);
                            for (BlockPos p : allParts) {
                                setupWallEntity(level, p, team, true, allParts);
                            }
                        } else if (msg.structureId == 17) {
                            int cost = Structure.BUNKER.cost;
                            List<BlockPos> wallParts = new ArrayList<>();
                            boolean blocked = false;

                            for (int x = -1; x <= 1; x++) {
                                for (int y = 0; y <= 2; y++) {
                                    for (int z = -1; z <= 1; z++) {
                                        if (x == 0 && z == 0 && y < 2) {
                                            continue;
                                        }
                                        BlockPos p = getRelativePos(msg.pos, msg.rotation, x, y, z);
                                        if (!canPlaceAt(level, p)) {
                                            blocked = true;
                                            break;
                                        }
                                        wallParts.add(p);
                                    }
                                    if (blocked) break;
                                }
                                if (blocked) break;
                            }

                            if (blocked) {
                                sendBlockedMessage(player);
                                return;
                            }

                            if (!isCreative && !hasMaterials(level, msg.pos, team, cost)) {
                                sendNoMaterialsMessage(player, cost);
                                return;
                            }

                            if (!isCreative) {
                                consumeMaterials(level, msg.pos, team, cost);
                            }

                            BlockState wallState =
                                    (BlockState) ((BlockState) ((BlockState) ((Block) ModBlocks.WALL_BLOCK.get())
                                                            .defaultBlockState()
                                                            .setValue(WallBlock.FACING, facing))
                                                    .setValue(WallBlock.CONSTRUCTED, false))
                                            .setValue(WallBlock.VALID, true);
                            BlockState slabState =
                                    (BlockState) ((BlockState) ((BlockState) ((Block) ModBlocks.WALL_SLAB_BLOCK.get())
                                                            .defaultBlockState()
                                                            .setValue(WallBlock.FACING, facing))
                                                    .setValue(WallBlock.CONSTRUCTED, false))
                                            .setValue(WallBlock.VALID, true);
                            int steps = ((-msg.rotation) / 90 % 4 + 4) % 4;

                            for (BlockPos p : wallParts) {
                                BlockPos rel = p.subtract(msg.pos);
                                boolean isNet = false;
                                Direction outwardFacing = Direction.NORTH;
                                if (rel.getX() == 0 && rel.getZ() == -1 && rel.getY() < 2) {
                                    isNet = true;
                                    outwardFacing = Direction.NORTH;
                                } else if (rel.getX() == 0 && rel.getZ() == 1 && rel.getY() == 1) {
                                    isNet = true;
                                    outwardFacing = Direction.SOUTH;
                                } else if (rel.getX() == -1 && rel.getZ() == 0 && rel.getY() == 1) {
                                    isNet = true;
                                    outwardFacing = Direction.WEST;
                                } else if (rel.getX() == 1 && rel.getZ() == 0 && rel.getY() == 1) {
                                    isNet = true;
                                    outwardFacing = Direction.EAST;
                                }

                                boolean isRoof = rel.getY() == 2;
                                level.setBlock(p, isRoof ? slabState : wallState, 3);

                                if (level.getBlockEntity(p) instanceof WallBlockEntity wall) {
                                    wall.setTeam(team);
                                    if (isNet) {
                                        Direction finalDir = outwardFacing;
                                        for (int i = 0; i < steps; i++) {
                                            finalDir = finalDir.getClockWise();
                                        }
                                        wall.setTransformTo("pwpwarfare:camo_net", finalDir.get2DDataValue());
                                    }
                                }
                            }

                            for (BlockPos p : wallParts) {
                                setupWallEntity(level, p, team, true, wallParts);
                            }
                        } else if (msg.structureId == 18) {
                            int cost = Structure.VEHICLE_STATION.cost;
                            if (!canPlaceAt(level, msg.pos)) {
                                sendBlockedMessage(player);
                                return;
                            }

                            if (!isCreative && !hasMaterials(level, msg.pos, team, cost)) {
                                sendNoMaterialsMessage(player, cost);
                                return;
                            }

                            if (!isCreative) {
                                consumeMaterials(level, msg.pos, team, cost);
                            }

                            BlockState state = (BlockState) ((Block) ModBlocks.VEHICLE_STATION_BLOCK.get())
                                    .defaultBlockState()
                                    .setValue(VehicleStationBlock.FACING, facing);
                            level.setBlock(msg.pos, state, 3);
                            WarfareWorldData data = WarfareWorldData.get(level);
                            data.structures.vehicleStations.removeIf(s -> s.pos.equals(msg.pos));
                            data.structures.vehicleStations.add(new WarfareWorldData.VehicleStationInfo(
                                    msg.pos, team, level.dimension().location().toString()));
                            data.setDirty();
                            PacketHandler.sendToAllClients(level, data);
                            if (level.getBlockEntity(msg.pos) instanceof VehicleStationBlockEntity supply) {
                                supply.setTeam(team);
                            }
                        } else if (msg.structureId == 22) {
                            int cost = Structure.MORTAR.cost;
                            if (!canPlaceAt(level, msg.pos)) {
                                sendBlockedMessage(player);
                                return;
                            }

                            if (!isCreative && !hasMaterials(level, msg.pos, team, cost)) {
                                sendNoMaterialsMessage(player, cost);
                            } else {
                                if (!isCreative) {
                                    consumeMaterials(level, msg.pos, team, cost);
                                }

                                BlockState state =
                                        (BlockState) ((BlockState) ((Block) ModBlocks.MORTAR_CONSTRUCTION_BLOCK.get())
                                                        .defaultBlockState()
                                                        .setValue(MortarConstructionBlock.FACING, facing))
                                                .setValue(MortarConstructionBlock.VALID, true);
                                if (level.setBlock(msg.pos, state, 3)) {
                                    if (level.getBlockEntity(msg.pos) instanceof MortarConstructionBlockEntity mortar) {
                                        mortar.setTeam(team);
                                    }

                                    player.sendSystemMessage(
                                            Component.translatable("pwpwarfare.msg.build.mortar_placed")
                                                    .withStyle(ChatFormatting.GREEN));
                                }
                            }
                        } else if (msg.structureId == 23) {
                            int cost = Structure.TOW.cost;
                            if (!canPlaceAt(level, msg.pos)) {
                                sendBlockedMessage(player);
                                return;
                            }

                            if (!isCreative && !hasMaterials(level, msg.pos, team, cost)) {
                                sendNoMaterialsMessage(player, cost);
                            } else {
                                if (!isCreative) {
                                    consumeMaterials(level, msg.pos, team, cost);
                                }

                                BlockState state =
                                        (BlockState) ((BlockState) ((Block) ModBlocks.TOW_CONSTRUCTION_BLOCK.get())
                                                        .defaultBlockState()
                                                        .setValue(TOWConstructionBlock.FACING, facing))
                                                .setValue(TOWConstructionBlock.VALID, true);
                                if (level.setBlock(msg.pos, state, 3)) {
                                    if (level.getBlockEntity(msg.pos) instanceof TOWConstructionBlockEntity tow) {
                                        tow.setTeam(team);
                                    }

                                    player.sendSystemMessage(Component.translatable("pwpwarfare.msg.build.tow_placed")
                                            .withStyle(ChatFormatting.GREEN));
                                }
                            }
                        } else if (msg.structureId == 24 || msg.structureId == 25) {
                            boolean mini = msg.structureId == 25;
                            int cost = mini
                                    ? (Integer) WarfareConfig.REB_MINI_BUILD_COST.get()
                                    : (Integer) WarfareConfig.REB_BUILD_COST.get();
                            if (!canPlaceAt(level, msg.pos)) {
                                sendBlockedMessage(player);
                                return;
                            }

                            // Нельзя строить РЭБ внутри уже стоящей сущности РЭБ
                            // (визуал большой — сущность 1x2 хитбоксом не перекрывает место)
                            if (!rebAreaFree(level, msg.pos, mini)) {
                                player.sendSystemMessage(Component.translatable("pwpwarfare.msg.build.reb_exists")
                                        .withStyle(ChatFormatting.RED));
                                return;
                            }

                            if (!isCreative
                                    && countTeamRebs(level, team) >= (Integer) WarfareConfig.REB_MAX_PER_TEAM.get()) {
                                player.sendSystemMessage(Component.translatable("pwpwarfare.msg.build.reb_limit")
                                        .withStyle(ChatFormatting.RED));
                                return;
                            }

                            if (!isCreative && !hasMaterials(level, msg.pos, team, cost)) {
                                sendNoMaterialsMessage(player, cost);
                            } else {
                                if (!isCreative) {
                                    consumeMaterials(level, msg.pos, team, cost);
                                }

                                BlockState state = (BlockState)
                                        ((BlockState) ((BlockState) ((Block) ModBlocks.REB_CONSTRUCTION_BLOCK.get())
                                                                .defaultBlockState()
                                                                .setValue(RebConstructionBlock.FACING, facing))
                                                        .setValue(RebConstructionBlock.VALID, true))
                                                .setValue(RebConstructionBlock.MINI, mini);
                                if (level.setBlock(msg.pos, state, 3)) {
                                    if (level.getBlockEntity(msg.pos) instanceof RebConstructionBlockEntity reb) {
                                        reb.setTeam(team);
                                    }

                                    player.sendSystemMessage(Component.translatable("pwpwarfare.msg.build.reb_placed")
                                            .withStyle(ChatFormatting.GREEN));
                                }
                            }
                        }
                    }
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }

    // Проверяет, можно ли разместить блок в указанной позиции
    private static boolean canPlaceAt(ServerLevel level, BlockPos pos) {
        return level.getBlockState(pos).canBeReplaced();
    }

    // Свободна ли зона для нового РЭБ: не пересекается ли с уже стоящей сущностью
    // reb/reb_mini (у сущностей маленький хитбокс 1x2, но визуал большой —
    // проверяем весь объём модели, иначе можно построить РЭБ внутри РЭБ).
    private static boolean rebAreaFree(ServerLevel level, BlockPos pos, boolean mini) {
        double half = mini ? 1.5 : 2.5;
        double height = mini ? 2.5 : 5.0;
        AABB area = new AABB(
                pos.getX() + 0.5 - half,
                pos.getY(),
                pos.getZ() + 0.5 - half,
                pos.getX() + 0.5 + half,
                pos.getY() + height,
                pos.getZ() + 0.5 + half);
        for (net.minecraft.world.entity.Entity e : level.getEntities((net.minecraft.world.entity.Entity) null, area)) {
            ResourceLocation eid = ForgeRegistries.ENTITY_TYPES.getKey(e.getType());
            if (eid != null
                    && (eid.toString().equals("uncomplicatedfpv:reb")
                            || eid.toString().equals("uncomplicatedfpv:reb_mini"))) {
                return false;
            }
        }
        return true;
    }

    // Считает построенные РЭБ/мини-РЭБ команды (лимит на команду).
    private static int countTeamRebs(ServerLevel level, String team) {
        int count = 0;
        for (net.minecraft.world.entity.Entity e : level.getEntities().getAll()) {
            String t = WarfareData.of(e).getString(WarfareKeys.VEHICLE_TEAM);
            if (t == null || t.isEmpty() || !t.equalsIgnoreCase(team)) continue;
            ResourceLocation eid = ForgeRegistries.ENTITY_TYPES.getKey(e.getType());
            if (eid != null
                    && (eid.toString().equals("uncomplicatedfpv:reb")
                            || eid.toString().equals("uncomplicatedfpv:reb_mini"))) {
                count++;
            }
        }
        return count;
    }

    private static void sendBlockedMessage(ServerPlayer player) {
        player.sendSystemMessage(
                Component.translatable("pwpwarfare.msg.build.blocked").withStyle(ChatFormatting.RED));
    }

    private static void sendNoMaterialsMessage(ServerPlayer player, int cost) {
        player.sendSystemMessage(
                Component.translatable("pwpwarfare.msg.build.need_mats", cost).withStyle(ChatFormatting.RED));
    }

    private static BlockPos getRelativePos(BlockPos start, Direction facing) {
        return getRelativePos(start, facing, 1);
    }

    private static BlockPos getRelativePos(BlockPos start, Direction facing, int offset) {
        if (facing == Direction.NORTH) {
            return start.east(offset);
        } else if (facing == Direction.WEST) {
            return start.north(offset);
        } else if (facing == Direction.SOUTH) {
            return start.west(offset);
        } else {
            return facing == Direction.EAST ? start.south(offset) : start;
        }
    }

    // Смещение с учётом поворота структуры (x - вправо, y - вверх, z - вперёд от направления взгляда)
    private static BlockPos getRelativePos(BlockPos base, int rotation, int xOff, int yOff, int zOff) {
        int rot = (rotation % 360 + 360) % 360;
        if (rot == 90) {
            return base.offset(zOff, yOff, -xOff);
        } else if (rot == 180) {
            return base.offset(-xOff, yOff, -zOff);
        } else if (rot == 270) {
            return base.offset(-zOff, yOff, xOff);
        } else {
            return base.offset(xOff, yOff, zOff);
        }
    }

    private static void attemptBuildMulti(
            ServerPlayer player, ServerLevel level, List<BlockPos> parts, Direction facing, String team, int cost) {
        for (BlockPos p : parts) {
            if (!canPlaceAt(level, p)) {
                sendBlockedMessage(player);
                return;
            }
        }

        if (!player.isCreative() && !hasMaterials(level, parts.get(0), team, cost)) {
            sendNoMaterialsMessage(player, cost);
        } else {
            if (!player.isCreative()) {
                consumeMaterials(level, parts.get(0), team, cost);
            }

            for (BlockPos p : parts) {
                BlockState state = (BlockState) ((BlockState) ((BlockState) ((Block) ModBlocks.WALL_BLOCK.get())
                                        .defaultBlockState()
                                        .setValue(WallBlock.FACING, facing))
                                .setValue(WallBlock.CONSTRUCTED, false))
                        .setValue(WallBlock.VALID, true);
                level.setBlock(p, state, 3);
            }

            for (BlockPos p : parts) {
                setupWallEntity(level, p, team, true, parts);
            }
        }
    }

    private static void setupWallEntity(
            ServerLevel level, BlockPos pos, String team, boolean multi, List<BlockPos> links) {
        if (level.getBlockEntity(pos) instanceof WallBlockEntity wall) {
            wall.setTeam(team);
            if (multi && links != null) {
                wall.setLinkedWalls(links);
            }
        }
    }

    // Проверяет, достаточно ли материалов у команды в радиусе строительства
    private static boolean hasMaterials(ServerLevel level, BlockPos pos, String team, int cost) {
        int totalMaterials = 0;
        WarfareWorldData data = WarfareWorldData.get(level);
        String currentDim = level.dimension().location().toString();
        int hubRadius = (Integer) WarfareConfig.HUB_BUILD_RADIUS.get();
        double maxHubSq = hubRadius * hubRadius;

        for (WarfareWorldData.HubInfo hubInfo : data.structures.hubs) {
            if ((hubInfo.dimension == null || hubInfo.dimension.equals(currentDim))
                    && hubInfo.pos.distSqr(pos) <= maxHubSq
                    && level.isLoaded(hubInfo.pos)
                    && level.getBlockEntity(hubInfo.pos) instanceof HubBlockEntity hub
                    && (hub.getTeam().equalsIgnoreCase(team) || hub.getTeam().equals("NEUTRAL"))) {
                totalMaterials += hub.getMaterials();
            }
        }

        int crateRadius = (Integer) WarfareConfig.CRATE_BUILD_RADIUS.get();
        double maxCrateSq = crateRadius * crateRadius;
        AABB searchArea = new AABB(pos).inflate(crateRadius);

        for (SupplyCrateEntity crate : level.getEntitiesOfClass(SupplyCrateEntity.class, searchArea)) {
            if ((crate.getTeamOwner().equalsIgnoreCase(team)
                            || crate.getTeamOwner().equals("NEUTRAL"))
                    && crate.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= maxCrateSq) {
                totalMaterials += crate.getMaterials();
            }
        }

        return totalMaterials >= cost;
    }

    // Списание материалов из ящиков снабжения и хабов для оплаты строительства
    private static void consumeMaterials(ServerLevel level, BlockPos pos, String team, int cost) {
        int remainingToDeduct = cost;
        WarfareWorldData data = WarfareWorldData.get(level);
        String currentDim = level.dimension().location().toString();
        int crateRadius = (Integer) WarfareConfig.CRATE_BUILD_RADIUS.get();
        double maxCrateSq = crateRadius * crateRadius;
        AABB searchArea = new AABB(pos).inflate(crateRadius);

        for (SupplyCrateEntity crate : level.getEntitiesOfClass(SupplyCrateEntity.class, searchArea)) {
            if (remainingToDeduct <= 0) {
                break;
            }

            if ((crate.getTeamOwner().equalsIgnoreCase(team)
                            || crate.getTeamOwner().equals("NEUTRAL"))
                    && crate.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= maxCrateSq) {
                int cMats = crate.getMaterials();
                int take = Math.min(cMats, remainingToDeduct);
                crate.setMaterials(cMats - take);
                remainingToDeduct -= take;
            }
        }

        if (remainingToDeduct > 0) {
            int hubRadius = (Integer) WarfareConfig.HUB_BUILD_RADIUS.get();
            double maxHubSq = hubRadius * hubRadius;

            for (WarfareWorldData.HubInfo hubInfo : data.structures.hubs) {
                if (remainingToDeduct <= 0) {
                    break;
                }

                if ((hubInfo.dimension == null || hubInfo.dimension.equals(currentDim))
                        && hubInfo.pos.distSqr(pos) <= maxHubSq
                        && level.isLoaded(hubInfo.pos)
                        && level.getBlockEntity(hubInfo.pos) instanceof HubBlockEntity hub
                        && (hub.getTeam().equalsIgnoreCase(team)
                                || hub.getTeam().equals("NEUTRAL"))) {
                    int hMats = hub.getMaterials();
                    int take = Math.min(hMats, remainingToDeduct);
                    hub.consumeMaterials(take);
                    hubInfo.materials = hub.getMaterials();
                    data.setDirty();
                    remainingToDeduct -= take;
                }
            }
        }
    }
}
