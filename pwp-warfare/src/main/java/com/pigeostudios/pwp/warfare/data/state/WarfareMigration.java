package com.pigeostudios.pwp.warfare.data.state;

import net.minecraft.nbt.CompoundTag;

/** Миграция старого формата (ключи {@code WARFARE_*} в persistentData) — чистая логика, без Forge. */
public final class WarfareMigration {
    /**
     * Маркер в теге capability: миграция из persistentData уже выполнена. Не начинается с
     * {@code WARFARE_}, чтобы никогда не попасть в выборку ключей. Значение — версия миграции.
     */
    public static final String MIGRATED_MARKER = "pwp_migrated_from_persistent";

    public static final int VERSION = 1;

    /**
     * Старые ключи в persistentData ещё поддерживаются для отката на старый мод (пока не
     * вызывается {@link #removeLegacy}). Пока true — при смерти ключи {@link
     * PlayerCloneRules#SURVIVE_DEATH} дублируются в persistentData нового игрока
     * ({@link PlayerCloneRules#mirrorToLegacy}). УДАЛИТЬ вместе с переходом на removeLegacy.
     */
    public static final boolean LEGACY_KEYS_KEPT = true;

    private WarfareMigration() {}

    /**
     * Копирует все ключи с префиксом {@code WARFARE_} (включая динамические, например кулдауны
     * вождения) из persistentData в тег capability — ОДИН раз на игрока (маркер
     * {@link #MIGRATED_MARKER}). Значение, уже лежащее в capability, не перетирается.
     *
     * <p>Старые ключи в persistentData НЕ удаляются ещё одну версию — для отката на старый мод
     * (он увидит снимок состояния на момент миграции). Маркер нужен, чтобы этот снимок не
     * вернулся в capability при следующем входе (например, удалённый PendingKit). Удаление —
     * {@link #removeLegacy} в следующей версии.
     *
     * @return сколько ключей перенесено (0 — миграция уже была)
     */
    public static int migratePlayer(CompoundTag persistent, CompoundTag target) {
        if (target.contains(MIGRATED_MARKER)) return 0;
        int moved = 0;
        for (String key : persistent.getAllKeys()) {
            if (!key.startsWith(WarfareKeys.PREFIX) || target.contains(key)) continue;
            target.put(key, persistent.get(key).copy());
            moved++;
        }
        target.putInt(MIGRATED_MARKER, VERSION);
        return moved;
    }

    /** Следующая версия: удалить старые ключи из persistentData (сейчас не вызывается). */
    public static int removeLegacy(CompoundTag persistent) {
        int removed = 0;
        for (String key : persistent.getAllKeys().toArray(new String[0])) {
            if (key.startsWith(WarfareKeys.PREFIX)) {
                persistent.remove(key);
                removed++;
            }
        }
        return removed;
    }
}
