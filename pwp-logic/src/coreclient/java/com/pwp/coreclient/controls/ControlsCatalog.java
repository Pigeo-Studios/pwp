package com.pwp.coreclient.controls;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/**
 * Белый список экрана «Управление» ({@code assets/pwp_core_client/controls/whitelist.json}):
 * категории и клавиши с порядком, флагом «Дополнительно» и подсказкой.
 *
 * <p>Формат: {@code {"version":1, "categories":[{id,titleKey,order}], "keys":[{id,category,order,
 * advanced?,hintKey?}]}}. Битые записи (нет id, неизвестная категория, дубль id) пропускаются и
 * попадают в {@link #issues()} — экран пишет их в лог, но работает. Порядок — по {@code order},
 * при равенстве — по позиции в файле (стабильно). Клавиши, которых нет в сборке, отсекает {@link
 * #retain(Set)} / {@link #build} молча.
 */
public final class ControlsCatalog {
    /** Категория экрана. */
    public record Category(String id, String titleKey, int order) {}

    /** Запись белого списка. */
    public record Entry(String id, String category, int order, boolean advanced, String hintKey) {}

    private final List<Category> categories;
    private final List<Entry> entries;
    private final List<String> issues;

    private ControlsCatalog(List<Category> categories, List<Entry> entries, List<String> issues) {
        this.categories = List.copyOf(categories);
        this.entries = List.copyOf(entries);
        this.issues = List.copyOf(issues);
    }

    /** Категории по порядку. */
    public List<Category> categories() {
        return categories;
    }

    /** Записи по порядку категорий, внутри категории — по {@code order}. */
    public List<Entry> entries() {
        return entries;
    }

    /** Замечания разбора (пропущенные записи). */
    public List<String> issues() {
        return issues;
    }

    /** Множество id белого списка. */
    public Set<String> ids() {
        Set<String> out = new HashSet<>();
        for (Entry e : entries) out.add(e.id());
        return out;
    }

    public static ControlsCatalog parse(String json) {
        return parse(new StringReader(json));
    }

    /**
     * Разбор файла.
     *
     * @throws IllegalArgumentException если это не JSON-объект
     */
    public static ControlsCatalog parse(Reader reader) {
        JsonElement root;
        try {
            root = JsonParser.parseReader(reader);
        } catch (JsonParseException e) {
            throw new IllegalArgumentException("whitelist: битый JSON: " + e.getMessage(), e);
        }
        if (root == null || !root.isJsonObject()) throw new IllegalArgumentException("whitelist: корень не объект");
        JsonObject obj = root.getAsJsonObject();
        List<String> issues = new ArrayList<>();

        // Категории
        List<Category> cats = new ArrayList<>();
        Map<String, Category> catById = new LinkedHashMap<>();
        for (JsonObject o : objects(obj, "categories", issues)) {
            String id = str(o, "id");
            if (id == null || id.isBlank()) {
                issues.add("категория без id");
                continue;
            }
            if (catById.containsKey(id)) {
                issues.add("дубль категории " + id);
                continue;
            }
            String titleKey = str(o, "titleKey");
            Category c = new Category(id, titleKey == null ? id : titleKey, intOr(o, "order", Integer.MAX_VALUE));
            catById.put(id, c);
            cats.add(c);
        }
        sortStable(cats, Comparator.comparingInt(Category::order));

        // Клавиши
        List<Entry> raw = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (JsonObject o : objects(obj, "keys", issues)) {
            String id = str(o, "id");
            if (id == null || id.isBlank()) {
                issues.add("клавиша без id");
                continue;
            }
            String cat = str(o, "category");
            if (cat == null || !catById.containsKey(cat)) {
                issues.add("клавиша " + id + ": неизвестная категория " + cat);
                continue;
            }
            if (!seen.add(id)) {
                issues.add("дубль клавиши " + id);
                continue;
            }
            boolean adv = o.has("advanced")
                    && o.get("advanced").isJsonPrimitive()
                    && o.get("advanced").getAsBoolean();
            raw.add(new Entry(id, cat, intOr(o, "order", Integer.MAX_VALUE), adv, str(o, "hintKey")));
        }
        return new ControlsCatalog(cats, ordered(cats, raw), issues);
    }

    /** Копия только с клавишами, которые есть в сборке. */
    public ControlsCatalog retain(Set<String> present) {
        List<Entry> kept = new ArrayList<>();
        for (Entry e : entries) if (present.contains(e.id())) kept.add(e);
        return new ControlsCatalog(categories, kept, issues);
    }

    /**
     * Строки экрана: категории по порядку, пустые категории выкидываются, отсутствующие в сборке
     * клавиши пропускаются молча.
     *
     * @param keys все клавиши сборки (id → снимок); лишние (вне белого списка) игнорируются
     * @param translate ключ lang → текст (для названий категорий)
     */
    public List<ControlsGroup> build(Map<String, KeyInfo> keys, Function<String, String> translate) {
        List<ControlsGroup> out = new ArrayList<>();
        for (Category c : categories) {
            String title = translate == null ? c.titleKey() : translate.apply(c.titleKey());
            List<ControlsRow> rows = new ArrayList<>();
            for (Entry e : entries) {
                if (!e.category().equals(c.id())) continue;
                KeyInfo k = keys.get(e.id());
                if (k == null) continue;
                rows.add(new ControlsRow(
                        k.id(),
                        k.label(),
                        k.mod(),
                        c.id(),
                        title,
                        k.keyLabel(),
                        k.isDefault(),
                        e.advanced(),
                        e.hintKey()));
            }
            if (!rows.isEmpty()) out.add(new ControlsGroup(c, title, rows));
        }
        return out;
    }

    /** То же по списку клавиш (дубли id — берётся первая). */
    public List<ControlsGroup> build(Collection<KeyInfo> keys, Function<String, String> translate) {
        Map<String, KeyInfo> map = new LinkedHashMap<>();
        for (KeyInfo k : keys) map.putIfAbsent(k.id(), k);
        return build(map, translate);
    }

    // ---- служебное ----

    /** Записи сгруппированы по порядку категорий, внутри — стабильная сортировка по order. */
    private static List<Entry> ordered(List<Category> cats, List<Entry> raw) {
        List<Entry> out = new ArrayList<>();
        for (Category c : cats) {
            List<Entry> part = new ArrayList<>();
            for (Entry e : raw) if (e.category().equals(c.id())) part.add(e);
            sortStable(part, Comparator.comparingInt(Entry::order));
            out.addAll(part);
        }
        return out;
    }

    /** {@link List#sort} — стабильная сортировка (TimSort), равные order остаются в порядке файла. */
    private static <T> void sortStable(List<T> list, Comparator<T> cmp) {
        list.sort(cmp);
    }

    private static List<JsonObject> objects(JsonObject root, String key, List<String> issues) {
        List<JsonObject> out = new ArrayList<>();
        JsonElement el = root.get(key);
        if (el == null || !el.isJsonArray()) {
            issues.add("нет массива " + key);
            return out;
        }
        JsonArray arr = el.getAsJsonArray();
        for (int i = 0; i < arr.size(); i++) {
            JsonElement e = arr.get(i);
            if (e.isJsonObject()) out.add(e.getAsJsonObject());
            else issues.add(key + "[" + i + "]: не объект");
        }
        return out;
    }

    private static String str(JsonObject o, String key) {
        JsonElement e = o.get(key);
        return e != null && e.isJsonPrimitive() ? e.getAsString() : null;
    }

    private static int intOr(JsonObject o, String key, int def) {
        JsonElement e = o.get(key);
        if (e == null || !e.isJsonPrimitive() || !e.getAsJsonPrimitive().isNumber()) return def;
        return e.getAsInt();
    }
}
