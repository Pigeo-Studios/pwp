package com.pigeostudios.pwp.warfare.data.state;

import net.minecraft.nbt.CompoundTag;

/** Миграция старого формата (ключи {@code WARFARE_*} в persistentData) — чистая логика, без Forge. */
public final class WarfareMigration {
    private WarfareMigration() {}

    /**
     * Перекладывает все ключи с префиксом {@code WARFARE_} (включая динамические, например
     * кулдауны вождения) из persistentData в тег capability и удаляет их из persistentData.
     * Значение, уже лежащее в capability, не перетирается. Остальные ключи не трогаются.
     *
     * @return сколько ключей перенесено
     */
    public static int migratePlayer(CompoundTag persistent, CompoundTag target) {
        int moved = 0;
        for (String key : persistent.getAllKeys().toArray(new String[0])) {
            if (!key.startsWith(WarfareKeys.PREFIX)) continue;
            if (!target.contains(key)) {
                target.put(key, persistent.get(key).copy());
                moved++;
            }
            persistent.remove(key);
        }
        return moved;
    }
}
