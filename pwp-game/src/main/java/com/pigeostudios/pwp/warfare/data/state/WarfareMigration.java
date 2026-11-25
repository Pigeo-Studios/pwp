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

    private WarfareMigration() {}

    /**
     * Копирует все ключи с префиксом {@code WARFARE_} (включая динамические, например кулдауны
     * вождения) из persistentData в тег capability — ОДИН раз на игрока (маркер
     * {@link #MIGRATED_MARKER}). Значение, уже лежащее в capability, не перетирается.
     *
     * <p>Сразу после переноса {@code PlayerWarfareData.of} удаляет старые ключи ({@link #removeLegacy}):
     * откат на старый мод не поддерживается с 18.11.2026. Маркер остаётся — повторный перенос не нужен.
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

    /** Удаляет старые ключи {@code WARFARE_*} из persistentData ИГРОКА (у прочих сущностей это живое хранилище!). */
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
