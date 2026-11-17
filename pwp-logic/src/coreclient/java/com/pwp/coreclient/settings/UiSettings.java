package com.pwp.coreclient.settings;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Модульный реестр клиентских настроек интерфейса PWP. Любой мод регистрирует свои настройки
 * (обычно статическими полями-хэндлами), экран настроек собирается из реестра автоматически.
 *
 * <pre>{@code
 * UiSettings.Category MAP = UiSettings.category("map", "gui.pwpwarfare.settings.map", 100);
 * UiSettings.Slider MARKER_OPACITY = UiSettings.slider("map.marker_opacity", MAP, "...label",
 *         0.2F, 1F, 0.05F, 1F, UiSettings.Format.PERCENT);
 * float a = MARKER_OPACITY.get(); // дёшево, можно звать покадрово
 * }</pre>
 *
 * <p>Хранение — словарь {@code id -> значение} (Double/Boolean/String). Чтение/запись файла —
 * у вызывающего (core-client {@code UiSettingsFile}); значения, пришедшие до регистрации
 * (мод зарегистрировался позже загрузки), применяются при регистрации. Неизвестные id
 * сохраняются как есть (мод мог быть временно выключен). Битое значение — дефолт +
 * предупреждение в {@link #setWarnSink}. Чистая логика — без Minecraft, покрыта тестами.
 */
public final class UiSettings {
    private UiSettings() {}

    /** Формат подписи значения слайдера. */
    public enum Format {
        /** Значение 0..1 показывается как 0..100%. */
        PERCENT(null),
        INTEGER(null),
        DECIMAL1(null),
        /** Множитель «1.25×». */
        MULTIPLIER(null),
        /** Секунды с одним знаком: ключ единицы с аргументом. */
        SECONDS("pwp_core.ui_settings.unit.seconds"),
        /** Миллисекунды (целое). */
        MILLIS("pwp_core.ui_settings.unit.millis");

        private final String unitKey;

        Format(String unitKey) {
            this.unitKey = unitKey;
        }

        /** Ключ перевода единицы ({@code %s с}) или null — число показывается как есть. */
        public String unitKey() {
            return unitKey;
        }

        /** Число для подписи (без единицы). */
        public String number(float v) {
            return switch (this) {
                case PERCENT -> Math.round(v * 100F) + "%";
                case INTEGER, MILLIS -> String.valueOf(Math.round(v));
                case DECIMAL1, SECONDS -> String.format(Locale.ROOT, "%.1f", v);
                case MULTIPLIER -> trim(String.format(Locale.ROOT, "%.2f", v)) + "×";
            };
        }

        private static String trim(String s) {
            if (!s.contains(".")) return s;
            s = s.replaceAll("0+$", "");
            return s.endsWith(".") ? s.substring(0, s.length() - 1) : s;
        }
    }

    /** Категория (вкладка экрана): ключ заголовка и порядок (меньше — выше). */
    public record Category(String id, String titleKey, int order) {}

    // ================================================================ настройки

    /** Общая часть настройки. */
    public abstract static sealed class Setting permits Slider, Toggle, Choice {
        private final String id;
        private final Category category;
        private final String labelKey;
        private String tooltipKey;
        private final List<Runnable> listeners = new CopyOnWriteArrayList<>();

        Setting(String id, Category category, String labelKey) {
            this.id = id;
            this.category = category;
            this.labelKey = labelKey;
        }

        public String id() {
            return id;
        }

        public Category category() {
            return category;
        }

        public String labelKey() {
            return labelKey;
        }

        /** Ключ тултипа (описание) или null. */
        public String tooltipKey() {
            return tooltipKey;
        }

        void setTooltipKey(String key) {
            this.tooltipKey = key;
        }

        /** Слушатель изменения значения (вызывается и при загрузке/сбросе). */
        public void onChange(Runnable r) {
            listeners.add(r);
            r.run();
        }

        protected final void changed() {
            dirty = true;
            for (Runnable r : listeners) r.run();
        }

        public abstract void reset();

        public abstract boolean isDefault();

        /** Значение для файла: Double / Boolean / String. */
        abstract Object encode();

        /** Применить значение из файла; false — значение битое (оставлен дефолт). */
        abstract boolean decode(Object raw);
    }

    /** Ползунок: значение в [min, max] с шагом step. */
    public static final class Slider extends Setting {
        private final float min;
        private final float max;
        private final float step;
        private final float def;
        private final Format format;
        private volatile float value;

        Slider(String id, Category c, String labelKey, float min, float max, float step, float def, Format format) {
            super(id, c, labelKey);
            if (!(max > min) || !(step > 0) || !Float.isFinite(min) || !Float.isFinite(max)) {
                throw new IllegalArgumentException("slider " + id + ": bad range");
            }
            this.min = min;
            this.max = max;
            this.step = step;
            this.def = snap(def, min, max, step);
            this.format = format;
            this.value = this.def;
        }

        public Slider tooltip(String key) {
            setTooltipKey(key);
            return this;
        }

        public float get() {
            return value;
        }

        public void set(float v) {
            if (!Float.isFinite(v)) return;
            float s = snap(v, min, max, step);
            if (s == value) return;
            value = s;
            changed();
        }

        /** Доля 0..1 текущего значения (позиция ползунка). */
        public float fraction() {
            return toFraction(value, min, max);
        }

        public void setFraction(float f) {
            set(fromFraction(f, min, max));
        }

        /** Сдвиг на n шагов (клавиатура/колесо). */
        public void nudge(int steps) {
            set(value + steps * step);
        }

        public float min() {
            return min;
        }

        public float max() {
            return max;
        }

        public float step() {
            return step;
        }

        public float defaultValue() {
            return def;
        }

        public Format format() {
            return format;
        }

        @Override
        public void reset() {
            if (value == def) return;
            value = def;
            changed();
        }

        @Override
        public boolean isDefault() {
            return value == def;
        }

        @Override
        Object encode() {
            return (double) value;
        }

        @Override
        boolean decode(Object raw) {
            if (!(raw instanceof Number n)) return false;
            double d = n.doubleValue();
            if (!Double.isFinite(d) || d < min - step * 0.01 || d > max + step * 0.01) return false;
            float s = snap((float) d, min, max, step);
            if (s != value) {
                value = s;
                changed();
            }
            return true;
        }
    }

    /** Тумблер вкл/выкл. */
    public static final class Toggle extends Setting {
        private final boolean def;
        private volatile boolean value;

        Toggle(String id, Category c, String labelKey, boolean def) {
            super(id, c, labelKey);
            this.def = def;
            this.value = def;
        }

        public Toggle tooltip(String key) {
            setTooltipKey(key);
            return this;
        }

        public boolean get() {
            return value;
        }

        public void set(boolean v) {
            if (v == value) return;
            value = v;
            changed();
        }

        public void toggle() {
            set(!value);
        }

        public boolean defaultValue() {
            return def;
        }

        @Override
        public void reset() {
            set(def);
        }

        @Override
        public boolean isDefault() {
            return value == def;
        }

        @Override
        Object encode() {
            return value;
        }

        @Override
        boolean decode(Object raw) {
            if (!(raw instanceof Boolean b)) return false;
            set(b);
            return true;
        }
    }

    /** Выбор одного варианта из списка (enum или строки); в файле — id варианта. */
    public static final class Choice<T> extends Setting {
        private final List<T> options;
        private final Function<T, String> idOf;
        private final Function<T, String> labelOf;
        private final int def;
        private volatile int index;

        Choice(
                String id,
                Category c,
                String labelKey,
                List<T> options,
                T def,
                Function<T, String> idOf,
                Function<T, String> labelOf) {
            super(id, c, labelKey);
            if (options.isEmpty()) throw new IllegalArgumentException("choice " + id + ": no options");
            this.options = List.copyOf(options);
            this.idOf = idOf;
            this.labelOf = labelOf;
            int d = this.options.indexOf(def);
            if (d < 0) throw new IllegalArgumentException("choice " + id + ": default not in options");
            this.def = d;
            this.index = d;
        }

        public Choice<T> tooltip(String key) {
            setTooltipKey(key);
            return this;
        }

        public T get() {
            return options.get(index);
        }

        public int index() {
            return index;
        }

        public List<T> options() {
            return options;
        }

        /** Ключ перевода подписи варианта. */
        public String optionLabelKey(T option) {
            return labelOf.apply(option);
        }

        public void set(T option) {
            int i = options.indexOf(option);
            if (i >= 0) setIndex(i);
        }

        public void setIndex(int i) {
            if (i < 0 || i >= options.size() || i == index) return;
            index = i;
            changed();
        }

        /** Следующий/предыдущий вариант по кругу (dir = +1 / -1). */
        public void cycle(int dir) {
            setIndex(Math.floorMod(index + dir, options.size()));
        }

        public T defaultValue() {
            return options.get(def);
        }

        @Override
        public void reset() {
            setIndex(def);
        }

        @Override
        public boolean isDefault() {
            return index == def;
        }

        @Override
        Object encode() {
            return idOf.apply(get());
        }

        @Override
        boolean decode(Object raw) {
            if (!(raw instanceof String s)) return false;
            for (int i = 0; i < options.size(); i++) {
                if (idOf.apply(options.get(i)).equals(s)) {
                    setIndex(i);
                    return true;
                }
            }
            return false;
        }
    }

    // ================================================================ реестр

    private static final Map<String, Category> CATEGORIES = new LinkedHashMap<>();
    private static final Map<String, Setting> SETTINGS = new LinkedHashMap<>();
    /** Значения из файла без зарегистрированной настройки (применятся при регистрации). */
    private static final Map<String, Object> PENDING = new LinkedHashMap<>();

    private static volatile Consumer<String> warnSink = s -> {};
    private static volatile boolean dirty;

    /** Куда писать предупреждения (core-client ставит LOG::warn). */
    public static void setWarnSink(Consumer<String> sink) {
        warnSink = sink == null ? s -> {} : sink;
    }

    /** Категория: повторная регистрация того же id возвращает существующую. */
    public static synchronized Category category(String id, String titleKey, int order) {
        return CATEGORIES.computeIfAbsent(id, k -> new Category(id, titleKey, order));
    }

    public static Slider slider(
            String id, Category c, String labelKey, float min, float max, float step, float def, Format format) {
        return register(new Slider(id, c, labelKey, min, max, step, def, format));
    }

    public static Toggle toggle(String id, Category c, String labelKey, boolean def) {
        return register(new Toggle(id, c, labelKey, def));
    }

    /** Выбор из enum: id варианта = {@code name().toLowerCase()}, подпись = labelPrefix + id. */
    public static <E extends Enum<E>> Choice<E> choice(
            String id, Category c, String labelKey, Class<E> type, E def, String labelPrefix) {
        Function<E, String> idOf = e -> e.name().toLowerCase(Locale.ROOT);
        return register(new Choice<>(
                id, c, labelKey, List.of(type.getEnumConstants()), def, idOf, e -> labelPrefix + idOf.apply(e)));
    }

    /** Выбор из списка строк-id: подпись = labelPrefix + id. */
    public static Choice<String> choice(
            String id, Category c, String labelKey, List<String> options, String def, String labelPrefix) {
        return register(new Choice<>(id, c, labelKey, options, def, s -> s, s -> labelPrefix + s));
    }

    private static synchronized <S extends Setting> S register(S s) {
        if (s.category() == null || !CATEGORIES.containsKey(s.category().id())) {
            throw new IllegalArgumentException("setting " + s.id() + ": unknown category");
        }
        if (SETTINGS.containsKey(s.id())) throw new IllegalArgumentException("duplicate setting id " + s.id());
        SETTINGS.put(s.id(), s);
        Object raw = PENDING.remove(s.id());
        if (raw != null && !s.decode(raw)) {
            warnSink.accept("UiSettings: bad value for " + s.id() + " = " + raw + ", using default");
        }
        return s;
    }

    /** Категории, в которых есть настройки, по порядку (при равном — по регистрации). */
    public static synchronized List<Category> categories() {
        List<Category> out = new ArrayList<>();
        for (Category c : CATEGORIES.values()) {
            if (!settings(c).isEmpty()) out.add(c);
        }
        out.sort(Comparator.comparingInt(Category::order));
        return out;
    }

    public static synchronized List<Setting> settings(Category c) {
        List<Setting> out = new ArrayList<>();
        for (Setting s : SETTINGS.values()) {
            if (s.category().id().equals(c.id())) out.add(s);
        }
        return out;
    }

    public static synchronized List<Setting> all() {
        return Collections.unmodifiableList(new ArrayList<>(SETTINGS.values()));
    }

    public static synchronized Setting find(String id) {
        return SETTINGS.get(id);
    }

    public static void resetCategory(Category c) {
        for (Setting s : settings(c)) s.reset();
    }

    public static void resetAll() {
        for (Setting s : all()) s.reset();
    }

    /**
     * Загрузить значения из файла (id -> Double/Boolean/String; прочее — битое). Неизвестные id
     * откладываются до регистрации. Сбрасывает признак изменений.
     */
    public static synchronized void load(Map<String, ?> raw) {
        if (raw == null) return;
        for (Map.Entry<String, ?> e : raw.entrySet()) {
            Setting s = SETTINGS.get(e.getKey());
            if (s == null) {
                PENDING.put(e.getKey(), e.getValue());
            } else if (!s.decode(e.getValue())) {
                warnSink.accept("UiSettings: bad value for " + e.getKey() + " = " + e.getValue() + ", using default");
                s.reset();
            }
        }
        dirty = false;
    }

    /** Все значения для файла (зарегистрированные + неизвестные, сохранённые как были). */
    public static synchronized Map<String, Object> snapshot() {
        Map<String, Object> out = new LinkedHashMap<>(PENDING);
        for (Setting s : SETTINGS.values()) out.put(s.id(), s.encode());
        return out;
    }

    /** Были ли изменения после последней загрузки/сохранения. */
    public static boolean isDirty() {
        return dirty;
    }

    public static void markSaved() {
        dirty = false;
    }

    /** Только для тестов: очистить реестр. */
    static synchronized void clearForTests() {
        CATEGORIES.clear();
        SETTINGS.clear();
        PENDING.clear();
        dirty = false;
    }

    // ================================================================ математика ползунка

    /** Привязка к шагу от min (или к max, если он ближе) с ограничением [min, max]. */
    public static float snap(float v, float min, float max, float step) {
        float c = Math.max(min, Math.min(max, v));
        double steps = Math.round((c - min) / (double) step);
        double s = min + steps * step;
        // max — тоже допустимая точка, даже если шаг не делит диапазон (ползунок в упоре)
        if (Math.abs(max - c) < Math.abs(s - c)) s = max;
        s = Math.round(s * 10000.0) / 10000.0;
        return (float) Math.max(min, Math.min(max, s));
    }

    public static float toFraction(float v, float min, float max) {
        if (max <= min) return 0F;
        return Math.max(0F, Math.min(1F, (v - min) / (max - min)));
    }

    public static float fromFraction(float f, float min, float max) {
        float c = Math.max(0F, Math.min(1F, f));
        return min + (max - min) * c;
    }
}
