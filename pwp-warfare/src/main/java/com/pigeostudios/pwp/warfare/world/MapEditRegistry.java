package com.pigeostudios.pwp.warfare.world;

import net.minecraft.nbt.CompoundTag;

/**
 * Раскладка карты «только на этот матч» из GUI-редактора (этап 3). Часть {@link WarfareWorldData}.
 *
 * <p>Хранится JSON раскладки в формате {@code MapLayoutCodec} (только ключи раскладки, см. {@code
 * LayoutTransfer.strip}); переживает рестарт сервера в пределах матча и очищается с концом матча ({@code
 * MapLayoutService.onMatchEnded}). Ключ NBT {@code PwpMatchLayout} не переименовывать.
 */
public final class MapEditRegistry {
    static final String KEY = "PwpMatchLayout";
    static final String KEY_AUTHOR = "PwpMatchLayoutAuthor";

    /** null — правок матча нет. */
    public String matchLayoutJson;

    public String matchLayoutAuthor = "";

    public boolean hasMatchLayout() {
        return matchLayoutJson != null && !matchLayoutJson.isEmpty();
    }

    public void clear() {
        matchLayoutJson = null;
        matchLayoutAuthor = "";
    }

    public void save(CompoundTag tag) {
        if (hasMatchLayout()) {
            tag.putString(KEY, matchLayoutJson);
            tag.putString(KEY_AUTHOR, matchLayoutAuthor == null ? "" : matchLayoutAuthor);
        }
    }

    public void load(CompoundTag tag) {
        matchLayoutJson = tag.contains(KEY) ? tag.getString(KEY) : null;
        matchLayoutAuthor = tag.getString(KEY_AUTHOR);
    }
}
