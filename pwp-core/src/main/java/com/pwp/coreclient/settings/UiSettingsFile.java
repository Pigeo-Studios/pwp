package com.pwp.coreclient.settings;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.loading.FMLPaths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Файл настроек интерфейса {@code config/pwp_ui_client.json}: {@code {"version":1,"values":{id:
 * значение}}}. Разбор — вручную через JsonObject (без Gson-маппинга POJO — не ломается
 * обфускацией). Битый файл/значение — дефолт + warn, файл перезапишется при сохранении.
 */
@OnlyIn(Dist.CLIENT)
public final class UiSettingsFile {
    private static final Logger LOG = LoggerFactory.getLogger(UiSettingsFile.class);
    private static final String FILE = "pwp_ui_client.json";
    private static final int VERSION = 1;

    private UiSettingsFile() {}

    public static Path path() {
        return FMLPaths.CONFIGDIR.get().resolve(FILE);
    }

    /** Загрузка при старте клиента (настройки модов, зарегистрированные позже, подхватятся). */
    public static void load() {
        UiSettings.setWarnSink(LOG::warn);
        Path p = path();
        if (!Files.isRegularFile(p)) return;
        try {
            JsonElement root = JsonParser.parseString(Files.readString(p, StandardCharsets.UTF_8));
            if (!root.isJsonObject() || !root.getAsJsonObject().has("values")) {
                LOG.warn("UiSettingsFile: {} без объекта values — настройки по умолчанию", p);
                return;
            }
            JsonElement values = root.getAsJsonObject().get("values");
            if (!values.isJsonObject()) {
                LOG.warn("UiSettingsFile: {}: values не объект — настройки по умолчанию", p);
                return;
            }
            Map<String, Object> raw = new LinkedHashMap<>();
            for (Map.Entry<String, JsonElement> e : values.getAsJsonObject().entrySet()) {
                raw.put(e.getKey(), toRaw(e.getValue()));
            }
            UiSettings.load(raw);
            LOG.info("UiSettingsFile: загружено {} значений из {}", raw.size(), p);
        } catch (IOException | JsonParseException | IllegalStateException e) {
            LOG.warn("UiSettingsFile: не прочитан {} — настройки по умолчанию", p, e);
        }
    }

    /** Сохранить, если были изменения (атомарно: tmp + move). */
    public static void saveIfDirty() {
        if (UiSettings.isDirty()) save();
    }

    public static void save() {
        JsonObject values = new JsonObject();
        for (Map.Entry<String, Object> e : UiSettings.snapshot().entrySet()) {
            JsonElement v = toJson(e.getValue());
            if (v != null) values.add(e.getKey(), v);
        }
        JsonObject root = new JsonObject();
        root.addProperty("version", VERSION);
        root.add("values", values);
        Path p = path();
        Path tmp = p.resolveSibling(FILE + ".tmp");
        try {
            Files.createDirectories(p.getParent());
            Files.writeString(
                    tmp, new GsonBuilder().setPrettyPrinting().create().toJson(root), StandardCharsets.UTF_8);
            Files.move(tmp, p, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            UiSettings.markSaved();
        } catch (IOException e) {
            LOG.warn("UiSettingsFile: не сохранён {}", p, e);
        }
    }

    /** JSON -> Boolean/Double/String; прочее (массив/объект) — как есть (будет битым). */
    private static Object toRaw(JsonElement e) {
        if (e.isJsonPrimitive()) {
            JsonPrimitive p = e.getAsJsonPrimitive();
            if (p.isBoolean()) return p.getAsBoolean();
            if (p.isNumber()) return p.getAsDouble();
            return p.getAsString();
        }
        return e;
    }

    private static JsonElement toJson(Object v) {
        if (v instanceof Boolean b) return new JsonPrimitive(b);
        if (v instanceof Number n) return new JsonPrimitive(n);
        if (v instanceof String s) return new JsonPrimitive(s);
        if (v instanceof JsonElement j) return j;
        return null;
    }
}
