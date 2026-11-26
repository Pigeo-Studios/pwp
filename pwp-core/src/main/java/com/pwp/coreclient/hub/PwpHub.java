package com.pwp.coreclient.hub;

import com.pwp.coreclient.ui.UIIcon;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BooleanSupplier;
import java.util.function.Function;
import java.util.function.Supplier;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.screens.Screen;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * «Меню PWP» — ОДНО место, откуда открывается всё (этап 11 ROADMAP). Раньше экраны жили по разным углам:
 * скины — только командой {@code /pwp inv}, редакторы китов/техники/скинов — особыми предметами из креатива,
 * статистика — командой, настройки — четырьмя кнопками паузы. Теперь каждый экран — карточка в своём
 * разделе; моды (warfare) добавляют свои карточки через {@link #register}. Карточка видна, только если
 * {@code visible} истинно (уровень доступа, лобби/матч, одиночка).
 *
 * <p>Правило: новый экран для игрока или админа — карточка здесь, а не новая кнопка паузы или предмет.
 */
@OnlyIn(Dist.CLIENT)
public final class PwpHub {

    /** Разделы по порядку на экране. */
    public enum Section {
        PLAY,
        PROFILE,
        COMMUNITY,
        SETTINGS,
        ADMIN;

        public String titleKey() {
            return "pwp_core.hub.section." + name().toLowerCase(java.util.Locale.ROOT);
        }
    }

    /**
     * Карточка. {@code open} получает экран хаба (родитель — чтобы «Назад» вела в меню) и возвращает экран
     * или null, если карточка — действие (закрыть меню и что-то сделать). {@code key} — клавиша-подсказка.
     */
    public record Entry(
            Section section,
            String id,
            int order,
            String titleKey,
            String descKey,
            UIIcon icon,
            Function<Screen, Screen> open,
            BooleanSupplier visible,
            Supplier<KeyMapping> key) {}

    private static final List<Entry> ENTRIES = new CopyOnWriteArrayList<>();

    private PwpHub() {}

    /** Добавить или заменить (по id) карточку. */
    public static void register(Entry e) {
        ENTRIES.removeIf(x -> x.id().equals(e.id()));
        ENTRIES.add(e);
    }

    /** Короткая форма: экран без клавиши, видна всегда, если {@code visible} null. */
    public static void register(
            Section section,
            String id,
            int order,
            UIIcon icon,
            Function<Screen, Screen> open,
            BooleanSupplier visible,
            Supplier<KeyMapping> key) {
        register(new Entry(
                section,
                id,
                order,
                "pwp_core.hub." + id + ".title",
                "pwp_core.hub." + id + ".desc",
                icon,
                open,
                visible == null ? () -> true : visible,
                key == null ? () -> null : key));
    }

    /** Видимые карточки раздела по порядку. */
    public static List<Entry> visible(Section s) {
        List<Entry> out = new ArrayList<>();
        for (Entry e : ENTRIES) {
            boolean show;
            try {
                show = e.section() == s && e.visible().getAsBoolean();
            } catch (RuntimeException ex) {
                show = false;
            }
            if (show) out.add(e);
        }
        out.sort(Comparator.comparingInt(Entry::order).thenComparing(Entry::id));
        return out;
    }
}
