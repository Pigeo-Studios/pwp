package com.pigeostudios.pwp.warfare.layout;

import com.google.gson.JsonObject;
import java.nio.charset.StandardCharsets;

/**
 * Перенос раскладки между редактором, матчем и БД (этап 3).
 *
 * <p>Переносится ТОЛЬКО раскладка (точки, мейны, спавны мейна, слоты техники, граница) в формате
 * {@link MapLayoutCodec}: всё остальное в map_config матча (фракции, тикеты, режим, паттерн, защитник)
 * пишет лобби перед каждым матчем, и сохранённая «навсегда» версия не должна его перетирать.
 */
public final class LayoutTransfer {
    /** Предел JSON раскладки в C→S пакете (пакет клиента в 1.20.1 ≤ 32767 байт вместе с заголовком). */
    public static final int MAX_BYTES = 30_000;

    private LayoutTransfer() {}

    /** Только ключи раскладки (+ имя карты для справки) — то, что хранится в {@code map_layout_versions.json}. */
    public static JsonObject strip(MapLayout l, String mapName) {
        JsonObject root = new JsonObject();
        if (mapName != null && !mapName.isEmpty()) root.addProperty("name", mapName);
        MapLayout bare =
                new MapLayout(root, null, l.points(), -1, l.mainZones(), l.spawns(), l.vehicleSlots(), l.border());
        return MapLayoutCodec.write(bare);
    }

    /**
     * Раскладка {@code incoming} поверх {@code base}: из base — корень (фракции, тикеты…), режим и выбранный
     * паттерн точек; из incoming — точки, мейны, спавны, техника, граница.
     */
    public static MapLayout merge(MapLayout base, MapLayout incoming) {
        return new MapLayout(
                base.source(),
                base.mode(),
                incoming.points(),
                base.patternIndex(),
                incoming.mainZones(),
                incoming.spawns(),
                incoming.vehicleSlots(),
                incoming.border());
    }

    /** Размер в UTF-8 (кириллица в именах точек — 2 байта). */
    public static int utf8Length(String s) {
        return s == null ? 0 : s.getBytes(StandardCharsets.UTF_8).length;
    }

    /** null — JSON годится для пакета, иначе причина. */
    public static String checkSize(String json) {
        if (json == null || json.isEmpty()) return "empty layout";
        int n = utf8Length(json);
        return n > MAX_BYTES ? "layout too large: " + n + " > " + MAX_BYTES + " bytes" : null;
    }
}
