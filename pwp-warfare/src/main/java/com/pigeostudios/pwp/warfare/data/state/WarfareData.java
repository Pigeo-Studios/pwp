package com.pigeostudios.pwp.warfare.data.state;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

/**
 * Типизированное состояние PWP у сущности — единственная точка доступа вместо
 * {@code getPersistentData()} + строковых ключей {@code "WARFARE_*"}.
 *
 * <ul>
 *   <li>Игрок → capability {@link PlayerWarfareData} (старые ключи persistentData мигрируют туда
 *       при первом обращении/входе).
 *   <li>Любая другая сущность (техника, дроны) → persistentData сущности, формат не меняется.
 * </ul>
 *
 * Имена методов повторяют CompoundTag, но принимают только ключи нужного типа: читать
 * Long-ключ через {@code getInt} больше нельзя — не скомпилируется.
 */
public abstract class WarfareData {

    /** Состояние сущности: для игрока — capability, для прочих — persistentData. */
    public static WarfareData of(Entity entity) {
        if (entity instanceof Player player) return PlayerWarfareData.of(player);
        return new EntityTags(entity.getPersistentData());
    }

    /** Тег-хранилище (для игрока — тег capability). */
    protected abstract CompoundTag tag();

    public String getString(WarfareKey<String> key) {
        return key.read(tag());
    }

    public int getInt(WarfareKey<Integer> key) {
        return key.read(tag());
    }

    public long getLong(WarfareKey<Long> key) {
        return key.read(tag());
    }

    public boolean getBoolean(WarfareKey<Boolean> key) {
        return key.read(tag());
    }

    public double getDouble(WarfareKey<Double> key) {
        return key.read(tag());
    }

    public float getFloat(WarfareKey<Float> key) {
        return key.read(tag());
    }

    public CompoundTag getCompound(WarfareKey<CompoundTag> key) {
        return key.read(tag());
    }

    public ListTag getList(WarfareKey<ListTag> key) {
        return key.read(tag());
    }

    public void putString(WarfareKey<String> key, String value) {
        key.write(tag(), value);
    }

    public void putInt(WarfareKey<Integer> key, int value) {
        key.write(tag(), value);
    }

    public void putLong(WarfareKey<Long> key, long value) {
        key.write(tag(), value);
    }

    public void putBoolean(WarfareKey<Boolean> key, boolean value) {
        key.write(tag(), value);
    }

    public void putDouble(WarfareKey<Double> key, double value) {
        key.write(tag(), value);
    }

    public void putFloat(WarfareKey<Float> key, float value) {
        key.write(tag(), value);
    }

    public <T extends net.minecraft.nbt.Tag> void put(WarfareKey<T> key, T value) {
        key.write(tag(), value);
    }

    public boolean contains(WarfareKey<?> key) {
        return tag().contains(key.name());
    }

    public void remove(WarfareKey<?> key) {
        tag().remove(key.name());
    }

    /** Вид на persistentData не-игровой сущности (формат NBT прежний). */
    static final class EntityTags extends WarfareData {
        private final CompoundTag tag;

        EntityTags(CompoundTag tag) {
            this.tag = tag;
        }

        @Override
        protected CompoundTag tag() {
            return tag;
        }
    }
}
