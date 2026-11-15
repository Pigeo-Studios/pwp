package com.pwp.cosmetics;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.pwp.api.core.CoreApi;
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

    private static volatile Map<String, String> modelPaths = Collections.emptyMap();
    private static volatile long loadedAt = 0;

    private SkinCatalog() {}

    /** Карта skinId → modelPath; из кэша, если он свежий, иначе с core-service. */
    public static CompletableFuture<Map<String, String>> modelPaths() {
        if (System.currentTimeMillis() - loadedAt < TTL_MS) {
            return CompletableFuture.completedFuture(modelPaths);
        }
        return CoreApi.getSkins().thenApply(resp -> {
            JsonArray arr = CoreApi.dataArray(resp);
            if (arr == null) return modelPaths; // core-service недоступен — последний известный каталог
            Map<String, String> map = new HashMap<>();
            for (JsonElement e : arr) {
                JsonObject s = e.getAsJsonObject();
                String mp = s.has("modelPath") && !s.get("modelPath").isJsonNull() ? s.get("modelPath").getAsString() : "";
                if (!mp.isEmpty()) map.put(s.get("skinId").getAsString(), mp);
            }
            modelPaths = Collections.unmodifiableMap(map);
            loadedAt = System.currentTimeMillis();
            return modelPaths;
        });
    }

    /** Предмет скина из modelPath: SNBT ({@code {...}}) или id предмета ({@code mod:item}). */
    public static ItemStack buildItem(String skinId, String modelPath) {
        if (modelPath == null || modelPath.isEmpty()) return ItemStack.EMPTY;
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
