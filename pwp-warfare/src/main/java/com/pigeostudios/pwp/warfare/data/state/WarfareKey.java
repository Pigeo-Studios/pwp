package com.pigeostudios.pwp.warfare.data.state;

import java.util.Objects;
import java.util.function.BiFunction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

/**
 * Типизированный ключ состояния игрока/сущности. Имя ключа в NBT совпадает со старым строковым
 * ключом {@code WARFARE_*} — формат сохранений не меняется, миграция — простое перекладывание.
 *
 * <p>Тип и значение по умолчанию задаются один раз (в {@link WarfareKeys}), поэтому одно и то же
 * значение больше не читается в разных местах разными {@code getInt/getLong}.
 */
public final class WarfareKey<T> {
    private final String name;
    private final BiFunction<CompoundTag, String, T> reader;
    private final TriWriter<T> writer;

    private WarfareKey(String name, BiFunction<CompoundTag, String, T> reader, TriWriter<T> writer) {
        this.name = Objects.requireNonNull(name);
        this.reader = reader;
        this.writer = writer;
    }

    /** Имя ключа в NBT (совпадает со старым ключом persistentData). */
    public String name() {
        return name;
    }

    /** Значение из тега; при отсутствии — значение по умолчанию (как у CompoundTag.getX). */
    public T read(CompoundTag tag) {
        return reader.apply(tag, name);
    }

    public void write(CompoundTag tag, T value) {
        writer.write(tag, name, value);
    }

    @Override
    public String toString() {
        return name;
    }

    @FunctionalInterface
    interface TriWriter<T> {
        void write(CompoundTag tag, String key, T value);
    }

    static WarfareKey<String> string(String name) {
        return new WarfareKey<>(name, CompoundTag::getString, CompoundTag::putString);
    }

    static WarfareKey<Integer> integer(String name) {
        return new WarfareKey<>(name, CompoundTag::getInt, CompoundTag::putInt);
    }

    static WarfareKey<Long> longKey(String name) {
        return new WarfareKey<>(name, CompoundTag::getLong, CompoundTag::putLong);
    }

    static WarfareKey<Double> doubleKey(String name) {
        return new WarfareKey<>(name, CompoundTag::getDouble, CompoundTag::putDouble);
    }

    static WarfareKey<Float> floatKey(String name) {
        return new WarfareKey<>(name, CompoundTag::getFloat, CompoundTag::putFloat);
    }

    static WarfareKey<Boolean> bool(String name) {
        return new WarfareKey<>(name, CompoundTag::getBoolean, CompoundTag::putBoolean);
    }

    static WarfareKey<CompoundTag> compound(String name) {
        return new WarfareKey<>(name, CompoundTag::getCompound, CompoundTag::put);
    }

    /** Список составных тегов (как {@code getList(name, Tag.TAG_COMPOUND)}). */
    static WarfareKey<ListTag> compoundList(String name) {
        return new WarfareKey<>(name, (t, k) -> t.getList(k, Tag.TAG_COMPOUND), CompoundTag::put);
    }
}
