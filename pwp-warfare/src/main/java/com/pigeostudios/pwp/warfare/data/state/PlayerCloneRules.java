package com.pigeostudios.pwp.warfare.data.state;

import java.util.List;
import net.minecraft.nbt.CompoundTag;

/**
 * Что из данных игрока переживает пересоздание сущности ({@code PlayerEvent.Clone}). Чистая
 * логика над тегами capability — без Forge, покрыта тестами.
 *
 * <ul>
 *   <li>Смерть ({@code isWasDeath=true}) — только {@link #SURVIVE_DEATH}: отряд, роль лидера,
 *       киты, кулдауны ресапплая/респавна. Всё остальное (нок, каналы дронов, флаги зоны)
 *       начинается заново — как и до capability.
 *   <li>Без смерти (возврат из Энда) — копируется всё, КРОМЕ {@link #RESET_ON_END_RETURN}:
 *       игрок не умирал, его состояние (кулдаун дрона, тимкиллы, ограничение базы) не должно
 *       теряться. До этапа 2 терялось — Forge копирует из persistentData только
 *       {@code PlayerPersisted}. Каналы установки/перезарядки дрона привязаны к сущности
 *       старого игрока и точке в старом измерении — они не переносятся (иначе «вечная»
 *       установка), флаг мейн-зоны пересчитывается тикером заново.
 * </ul>
 */
public final class PlayerCloneRules {
    /** Ключи, переживающие смерть. */
    public static final List<WarfareKey<?>> SURVIVE_DEATH = List.of(
            WarfareKeys.SQUAD_ID,
            WarfareKeys.IS_SQUAD_LEADER,
            WarfareKeys.CURRENT_KIT,
            WarfareKeys.PENDING_KIT,
            WarfareKeys.LAST_FOB_RESUPPLY,
            WarfareKeys.LAST_MAIN_RESUPPLY,
            WarfareKeys.LAST_RESPAWN_COMMAND);

    /**
     * Ключи, которые НЕ переносятся при возврате из Энда: каналы дронов (кроме кулдауна
     * {@link WarfareKeys#DRONE_DEPLOY_CD}) и флаг мейн-зоны.
     */
    public static final List<WarfareKey<?>> RESET_ON_END_RETURN = List.of(
            WarfareKeys.DRONE_DEPLOY_TYPE,
            WarfareKeys.DRONE_DEPLOY_START,
            WarfareKeys.DRONE_DEPLOY_X,
            WarfareKeys.DRONE_DEPLOY_Y,
            WarfareKeys.DRONE_DEPLOY_Z,
            WarfareKeys.DRONE_DEPLOY_ENTITY,
            WarfareKeys.DRONE_RELOAD_TYPE,
            WarfareKeys.DRONE_RELOAD_START,
            WarfareKeys.DRONE_RELOAD_X,
            WarfareKeys.DRONE_RELOAD_Y,
            WarfareKeys.DRONE_RELOAD_Z,
            WarfareKeys.DRONE_RELOAD_ENTITY,
            WarfareKeys.IN_MAIN_ZONE);

    private PlayerCloneRules() {}

    /** Копирует из тега старого игрока в тег нового по правилам выше. */
    public static void copy(CompoundTag from, CompoundTag to, boolean wasDeath) {
        if (!wasDeath) {
            for (String key : from.getAllKeys()) {
                if (isResetOnEndReturn(key)) continue;
                to.put(key, from.get(key).copy());
            }
            return;
        }
        for (WarfareKey<?> key : SURVIVE_DEATH) {
            if (from.contains(key.name()))
                to.put(key.name(), from.get(key.name()).copy());
        }
        // маркер миграции переживает смерть — иначе миграция persistentData пошла бы повторно
        if (from.contains(WarfareMigration.MIGRATED_MARKER)) {
            to.putInt(WarfareMigration.MIGRATED_MARKER, from.getInt(WarfareMigration.MIGRATED_MARKER));
        }
    }

    /**
     * Откат на старый мод после смерти: Forge не копирует persistentData при смерти (кроме
     * {@code PlayerPersisted}), и у нового игрока старых ключей нет — старый мод потерял бы
     * отряд и кит. Копирует ключи {@link #SURVIVE_DEATH} из тега capability в persistentData
     * нового игрока. Действует, пока {@link WarfareMigration#LEGACY_KEYS_KEPT}; УДАЛИТЬ вместе
     * с {@link WarfareMigration#removeLegacy}.
     */
    public static void mirrorToLegacy(CompoundTag capability, CompoundTag persistent) {
        if (!WarfareMigration.LEGACY_KEYS_KEPT) return;
        for (WarfareKey<?> key : SURVIVE_DEATH) {
            if (capability.contains(key.name())) {
                persistent.put(key.name(), capability.get(key.name()).copy());
            }
        }
    }

    private static boolean isResetOnEndReturn(String key) {
        for (WarfareKey<?> k : RESET_ON_END_RETURN) {
            if (k.name().equals(key)) return true;
        }
        return false;
    }
}
