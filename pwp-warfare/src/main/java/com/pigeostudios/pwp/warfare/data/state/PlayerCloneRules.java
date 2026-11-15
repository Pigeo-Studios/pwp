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
 *   <li>Без смерти (возврат из Энда) — копируется ВСЁ: игрок не умирал, его состояние
 *       (кулдаун дрона, тимкиллы, ограничение базы, мейн-зона) не должно теряться. До этапа 2
 *       терялось — Forge копирует из persistentData только {@code PlayerPersisted}.
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

    private PlayerCloneRules() {}

    /** Копирует из тега старого игрока в тег нового по правилам выше. */
    public static void copy(CompoundTag from, CompoundTag to, boolean wasDeath) {
        if (!wasDeath) {
            for (String key : from.getAllKeys()) {
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
}
