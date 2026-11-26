package com.pwp.cosmetics;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.pwp.api.core.CoreApi;
import com.pwp.api.time.GameClock;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Серверный каталог определений скинов (core-service {@code /skins}) и сборка предмета скина.
 *
 * <p>Предмет скина собирает ТОЛЬКО сервер из {@code modelPath} определения. Раньше клиент
 * присылал SNBT предмета в пакете экипировки, и сервер выдавал его при пополнении кита —
 * любой мог получить произвольный предмет.
 */
public final class SkinCatalog {
    private static final Logger LOG = LoggerFactory.getLogger(SkinCatalog.class);
    private static final long TTL_MS = 60_000;

    /** Определение скина из каталога core. */
    public record Def(String skinId, String slotType, String weaponTag, String modelPath) {
        /** Скин ствола (display TaCZ) или null — старый скин-предмет. */
        public com.pwp.api.cosmetics.WeaponSkin weapon() {
            return com.pwp.api.cosmetics.WeaponSkin.parse(skinId, slotType, weaponTag, modelPath);
        }
    }

    private static volatile Map<String, Def> defs = Collections.emptyMap();
    private static volatile Map<String, String> modelPaths = Collections.emptyMap();
    private static volatile long loadedAt = 0;

    private SkinCatalog() {}

    /** Карта skinId → modelPath; из кэша, если он свежий, иначе с core-service. */
    public static CompletableFuture<Map<String, String>> modelPaths() {
        return defs().thenApply(d -> modelPaths);
    }

    /** Все определения skinId → {@link Def} (кэш 60 с; core недоступен — последний известный каталог). */
    public static CompletableFuture<Map<String, Def>> defs() {
        if (GameClock.realMillis() - loadedAt < TTL_MS) {
            return CompletableFuture.completedFuture(defs);
        }
        return CoreApi.getSkins().thenApply(resp -> {
            JsonArray arr = CoreApi.dataArray(resp);
            if (arr == null) return defs;
            Map<String, Def> d = new HashMap<>();
            Map<String, String> map = new HashMap<>();
            for (JsonElement e : arr) {
                JsonObject s = e.getAsJsonObject();
                String id = str(s, "skinId");
                if (id.isEmpty()) continue;
                String mp = str(s, "modelPath");
                d.put(id, new Def(id, str(s, "slotType"), str(s, "weaponTag"), mp));
                if (!mp.isEmpty()) map.put(id, mp);
            }
            defs = Collections.unmodifiableMap(d);
            modelPaths = Collections.unmodifiableMap(map);
            loadedAt = GameClock.realMillis();
            return defs;
        });
    }

    /** Сбросить кэш (админ сохранил/удалил скин). */
    public static void invalidate() {
        loadedAt = 0;
    }

    private static String str(JsonObject o, String k) {
        return o.has(k) && !o.get(k).isJsonNull() ? o.get(k).getAsString() : "";
    }

    /** Предмет скина из modelPath: SNBT ({@code {...}}) или id предмета ({@code mod:item}). */
    public static ItemStack buildItem(String skinId, String modelPath) {
        if (modelPath == null || modelPath.isEmpty()) return ItemStack.EMPTY;
        // скин ствола (display TaCZ) — не предмет; иконку рисует экран арсенала сам
        if (modelPath.startsWith(com.pwp.api.cosmetics.WeaponSkin.DISPLAY_PREFIX)) return ItemStack.EMPTY;
        if (modelPath.startsWith("{")) {
            try {
                CompoundTag tag = TagParser.parseTag(modelPath);
                return ItemStack.of(tag);
            } catch (com.mojang.brigadier.exceptions.CommandSyntaxException e) {
                LOG.warn("Скин {}: некорректный SNBT в modelPath", skinId, e);
                return ItemStack.EMPTY;
            }
        }
        if (modelPath.contains(":")) {
            ResourceLocation id = ResourceLocation.tryParse(modelPath);
            Item item = id != null ? ForgeRegistries.ITEMS.getValue(id) : null;
            if (item != null) return new ItemStack(item);
        }
        return ItemStack.EMPTY;
    }
}
