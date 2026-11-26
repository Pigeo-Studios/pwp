package com.pigeostudios.pwp.warfare.server;

import com.pigeostudios.pwp.warfare.block.ModBlocks;
import com.pigeostudios.pwp.warfare.build.Structure;
import com.pigeostudios.pwp.warfare.build.StructureRules;
import com.pigeostudios.pwp.warfare.config.WarfareConfig;
import com.pigeostudios.pwp.warfare.world.WarfareWorldData;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Факты для {@link StructureRules} (этап 17c): стройка в мейне врага и сколько таких же построек уже у ФОБа.
 * Считается только по запросу стройки (не в тике): станции — по реестру, миномёт и ПТРК — каркасы
 * (блок-сущности в чанках радиуса) плюс готовые установки SBW в том же радиусе.
 */
public final class BuildConditions {
    private BuildConditions() {}

    public static StructureRules.Facts facts(
            ServerLevel level, BlockPos pos, String team, Structure s, boolean creative) {
        return new StructureRules.Facts(inEnemyMain(level, pos, team), countNearFob(level, pos, team, s), creative);
    }

    static boolean inEnemyMain(ServerLevel level, BlockPos pos, String team) {
        Vec3 c = Vec3.atCenterOf(pos);
        for (WarfareWorldData.MainProtectionZone z : WarfareWorldData.get(level).structures.mainZones) {
            if (!z.team.equalsIgnoreCase(team) && z.isInside(c)) return true;
        }
        return false;
    }

    /** Центр счёта — ближайший свой ФОБ в радиусе стройки, иначе сама точка (стройка от ящика). */
    static BlockPos fobCenter(ServerLevel level, BlockPos pos, String team, int radius) {
        BlockPos best = null;
        double bestD = (double) radius * radius;
        String dim = level.dimension().location().toString();
        for (WarfareWorldData.HubInfo h : WarfareWorldData.get(level).structures.hubs) {
            if (h.dimension != null && !h.dimension.equals(dim)) continue;
            if (!h.team.equalsIgnoreCase(team)) continue;
            double d = h.pos.distSqr(pos);
            if (d <= bestD) {
                bestD = d;
                best = h.pos;
            }
        }
        return best == null ? pos : best;
    }

    static int countNearFob(ServerLevel level, BlockPos pos, String team, Structure s) {
        if (s == null || s.perFob <= 0) return 0;
        int radius = WarfareConfig.HUB_BUILD_RADIUS.get();
        BlockPos center = fobCenter(level, pos, team, radius);
        double r2 = (double) radius * radius;
        return switch (s) {
            case VEHICLE_STATION -> (int) WarfareWorldData.get(level).structures.vehicleStations.stream()
                    .filter(v -> v.pos != null && v.pos.distSqr(center) <= r2)
                    .count();
            case MORTAR -> blocks(level, center, radius, ModBlocks.MORTAR_CONSTRUCTION_BE.get())
                    + entities(level, center, radius, "mortar");
            case TOW -> blocks(level, center, radius, ModBlocks.TOW_CONSTRUCTION_BE.get())
                    + entities(level, center, radius, "tow");
            default -> 0;
        };
    }

    private static int blocks(ServerLevel level, BlockPos center, int radius, BlockEntityType<?> type) {
        int count = 0;
        double r2 = (double) radius * radius;
        ChunkPos min = new ChunkPos(center.offset(-radius, 0, -radius));
        ChunkPos max = new ChunkPos(center.offset(radius, 0, radius));
        for (int cx = min.x; cx <= max.x; cx++) {
            for (int cz = min.z; cz <= max.z; cz++) {
                if (!level.hasChunk(cx, cz)) continue;
                for (var be : level.getChunk(cx, cz).getBlockEntities().values()) {
                    if (be.getType() == type && be.getBlockPos().distSqr(center) <= r2) count++;
                }
            }
        }
        return count;
    }

    private static int entities(ServerLevel level, BlockPos center, int radius, String sbwId) {
        EntityType<?> type = ForgeRegistries.ENTITY_TYPES.getValue(new ResourceLocation("superbwarfare", sbwId));
        if (type == null) return 0;
        double r2 = (double) radius * radius;
        AABB box = new AABB(center).inflate(radius);
        return level.getEntities(type, box, e -> e.blockPosition().distSqr(center) <= r2)
                .size();
    }
}
