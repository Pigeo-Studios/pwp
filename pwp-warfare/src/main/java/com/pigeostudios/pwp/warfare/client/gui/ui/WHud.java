package com.pigeostudios.pwp.warfare.client.gui.ui;

import com.pwp.coreclient.gui.theme.PWPTheme.Colors;
import com.pwp.coreclient.ui.UIDraw;
import com.pwp.coreclient.ui.hud.HudZones;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * HUD-кит матча (этап 1 редизайна HUD): общий гейт видимости, зоны экрана и примитивы HUD. Всё
 * рисуется через GuiGraphics с int-координатами — БЕЗ аллокаций в кадре (никаких Rect/Component/
 * String внутри примитивов; числа — через {@link Num}). Цвета — только {@link WTheme}/PWPTheme.
 *
 * <p>Раскладка зон — чистый {@link HudZones} (pwp-logic), здесь — кэш под текущий экран.
 */
@OnlyIn(Dist.CLIENT)
public final class WHud {
    private WHud() {}

    private static final HudZones.Cache ZONES = new HudZones.Cache();
    private static final ResourceLocation SBW_MONITOR = new ResourceLocation("superbwarfare", "monitor");

    // ================================================================ гейт

    /**
     * Общий гейт HUD матча: есть игрок и мир, HUD не скрыт по F1 ({@code hideGui}). Его проверяют
     * ВСЕ HUD-элементы pwp-warfare/pwp-medicine (pwp-limits/pwp-movement не зависят от warfare —
     * у них та же проверка hideGui на месте). Полноэкранные эффекты (виньетка нока, шейдеры, прицел
     * АГС) гейт НЕ проверяют — они часть «картинки», как ванильная тыква/подзорная труба.
     */
    public static boolean hudVisible() {
        Minecraft mc = Minecraft.getInstance();
        return mc.player != null && mc.level != null && !mc.options.hideGui;
    }

    /**
     * Игрок смотрит через монитор SBW на FPV-дрон (та же проверка, что {@code FPVState.isInFPV} в
     * pwp-drone; warfare от pwp-drone не зависит). Пока НЕ подключено к гейту: скрывать ли наш HUD
     * в FPV — решение этапов 2–5 после проверки в игре.
     */
    public static boolean droneFpvActive() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return false;
        ItemStack stack = mc.player.getMainHandItem();
        if (stack.isEmpty() || !SBW_MONITOR.equals(ForgeRegistries.ITEMS.getKey(stack.getItem()))) return false;
        CompoundTag tag = stack.getTag();
        return tag != null && tag.getBoolean("Using") && tag.getBoolean("Linked");
    }

    /** Зоны HUD текущего экрана (пересчёт только при смене размера окна/guiScale). */
    public static HudZones zones() {
        Minecraft mc = Minecraft.getInstance();
        return ZONES.get(mc.getWindow().getGuiScaledWidth(), mc.getWindow().getGuiScaledHeight(), WTheme.HUD_SCALE);
    }

    // ================================================================ текст

    /** Строка с защитой альфы (&lt; 4 у ванильного шрифта = непрозрачный текст — пропускаем). */
    public static int text(GuiGraphics g, Font font, String text, int x, int y, int color, boolean shadow) {
        if (text == null || Colors.alpha(color) < 4) return 0;
        return g.drawString(font, text, x, y, color, shadow);
    }

    /** Текст с обводкой в 1px (читается на любом фоне мира); {@code o} — общая прозрачность. */
    public static void outlined(GuiGraphics g, Font font, Component text, int x, int y, int color, float o) {
        int c = UIDraw.fade(color, o);
        if (Colors.alpha(c) < 4) return;
        int oc = UIDraw.fade(WTheme.HUD_TEXT_OUTLINE, o * Colors.alpha(color) / 255F);
        if (Colors.alpha(oc) >= 4) {
            g.drawString(font, text, x - 1, y, oc, false);
            g.drawString(font, text, x + 1, y, oc, false);
            g.drawString(font, text, x, y - 1, oc, false);
            g.drawString(font, text, x, y + 1, oc, false);
        }
        g.drawString(font, text, x, y, c, false);
    }

    // ================================================================ полоса

    /**
     * Полоса с делениями: трек, заливка доли {@code frac} (0..1) цветом {@code fill}, деления на
     * {@code segments} частей (≤1 — без делений), рамка 1px.
     */
    public static void segmentBar(
            GuiGraphics g, int x, int y, int w, int h, float frac, int segments, int fill, float o) {
        if (w <= 0 || h <= 0 || o <= 0F) return;
        g.fill(x, y, x + w, y + h, UIDraw.fade(WTheme.HUD_BAR_TRACK, o));
        float f = Float.isNaN(frac) ? 0F : Math.max(0F, Math.min(1F, frac));
        int fw = Math.round(w * f);
        if (fw > 0) g.fill(x, y, x + fw, y + h, UIDraw.fade(fill, o));
        if (segments > 1) {
            int tick = UIDraw.fade(WTheme.HUD_BAR_TICK, o);
            for (int i = 1; i < segments; i++) {
                int tx = x + (int) ((long) w * i / segments);
                g.fill(tx, y, tx + 1, y + h, tick);
            }
        }
        border(g, x, y, w, h, UIDraw.fade(WTheme.HUD_BAR_BORDER, o));
    }

    /** Рамка 1px снаружи области {@code x,y,w,h}. */
    public static void border(GuiGraphics g, int x, int y, int w, int h, int color) {
        if (Colors.alpha(color) == 0) return;
        g.fill(x - 1, y - 1, x + w + 1, y, color);
        g.fill(x - 1, y + h, x + w + 1, y + h + 1, color);
        g.fill(x - 1, y, x, y + h, color);
        g.fill(x + w, y, x + w + 1, y + h, color);
    }

    // ================================================================ плашка

    /** Высота плашки-значения. */
    public static int plateHeight(Font font) {
        return font.lineHeight + 2 * WTheme.HUD_PLATE_PAD_Y;
    }

    /** Ширина плашки: полоска + поле + [подпись + зазор] + значение + поле. */
    public static int plateWidth(Font font, Component label, String value) {
        int w = WTheme.HUD_PLATE_STRIPE + 2 * WTheme.HUD_PLATE_PAD_X;
        if (label != null) w += font.width(label) + WTheme.HUD_PLATE_PAD_X;
        if (value != null) w += font.width(value);
        return w;
    }

    /**
     * Плашка-значение: тёмная подложка, акцентная полоска слева, подпись (приглушённо) и значение.
     * {@code label} может быть null. Возвращает ширину.
     */
    public static int plate(
            GuiGraphics g, Font font, int x, int y, Component label, String value, int accent, float o) {
        if (o <= 0F) return 0;
        int w = plateWidth(font, label, value);
        int h = plateHeight(font);
        g.fill(x, y, x + w, y + h, UIDraw.fade(WTheme.HUD_PLATE_BG, o));
        g.fill(x, y, x + WTheme.HUD_PLATE_STRIPE, y + h, UIDraw.fade(accent, o));
        int tx = x + WTheme.HUD_PLATE_STRIPE + WTheme.HUD_PLATE_PAD_X;
        int ty = y + WTheme.HUD_PLATE_PAD_Y + 1;
        if (label != null) {
            int lc = UIDraw.fade(WTheme.HUD_LABEL, o);
            if (Colors.alpha(lc) >= 4) g.drawString(font, label, tx, ty, lc, false);
            tx += font.width(label) + WTheme.HUD_PLATE_PAD_X;
        }
        text(g, font, value, tx, ty, UIDraw.fade(WTheme.HUD_VALUE, o), false);
        return w;
    }

    // ================================================================ иконка + счётчик

    /** Ширина «иконка + число». */
    public static int iconCountWidth(Font font, String count) {
        return WTheme.HUD_ICON + WTheme.HUD_ICON_GAP + (count == null ? 0 : font.width(count));
    }

    /**
     * Иконка атласа warfare и число справа, оба цветом {@code color}; иконка по вертикали по центру
     * строки текста. Возвращает ширину.
     */
    public static int iconCount(
            GuiGraphics g, Font font, WarfareIcon icon, int x, int y, String count, int color, float o) {
        int c = UIDraw.fade(color, o);
        if (Colors.alpha(c) < 4) return iconCountWidth(font, count);
        int is = WTheme.HUD_ICON;
        WDraw.icon(g, icon, x, y + (font.lineHeight - is) / 2, is, c);
        text(g, font, count, x + is + WTheme.HUD_ICON_GAP, y, c, true);
        return iconCountWidth(font, count);
    }

    // ================================================================ кэш чисел

    /**
     * Кэш строки числа: строка создаётся только когда число изменилось (счётчики HUD меняются
     * редко, а рисуются каждый кадр).
     */
    public static final class Num {
        private int last = Integer.MIN_VALUE;
        private String text = "";

        public String of(int value) {
            if (value != last) {
                last = value;
                text = Integer.toString(value);
            }
            return text;
        }
    }

    /** Кэш строки таймера «мм:сс»: пересборка раз в секунду, а не каждый кадр. */
    public static final class Clock {
        private int last = Integer.MIN_VALUE;
        private String text = "";

        public String of(int seconds) {
            int s = Math.max(0, seconds);
            if (s != last) {
                last = s;
                int m = s / 60;
                int r = s % 60;
                text = (m < 10 ? "0" : "") + m + (r < 10 ? ":0" : ":") + r;
            }
            return text;
        }
    }

    // ================================================================ кэш переводов

    /**
     * Кэш {@code Component.translatable(key, args)}: компонент пересобирается, только когда
     * изменились аргументы (строки сравниваются по equals, числа — по значению, без боксинга в
     * кадре). Перевод разрешается при отрисовке — смена языка подхватывается сама; кэш строки
     * {@link #string()} обновляется вместе с компонентом.
     */
    public static final class Fmt {
        private final String key;
        private String s1;
        private String s2;
        private int i1 = Integer.MIN_VALUE;
        private int i2 = Integer.MIN_VALUE;
        private Component component;
        private String string;

        public Fmt(String key) {
            this.key = key;
        }

        public Component of(String a) {
            if (component == null || !eq(a, s1)) {
                s1 = a;
                set(Component.translatable(key, str(a)));
            }
            return component;
        }

        public Component of(String a, String b) {
            if (component == null || !eq(a, s1) || !eq(b, s2)) {
                s1 = a;
                s2 = b;
                set(Component.translatable(key, str(a), str(b)));
            }
            return component;
        }

        public Component of(int a) {
            if (component == null || a != i1) {
                i1 = a;
                set(Component.translatable(key, a));
            }
            return component;
        }

        public Component of(int a, int b) {
            if (component == null || a != i1 || b != i2) {
                i1 = a;
                i2 = b;
                set(Component.translatable(key, a, b));
            }
            return component;
        }

        public Component of(String a, int b) {
            if (component == null || !eq(a, s1) || b != i1) {
                s1 = a;
                i1 = b;
                set(Component.translatable(key, str(a), b));
            }
            return component;
        }

        /** Строка последнего компонента (для отрисовки String-хелперами); null до первого of. */
        public String string() {
            if (string == null && component != null) string = component.getString();
            return string;
        }

        private void set(Component c) {
            component = c;
            string = null;
        }

        private static boolean eq(String a, String b) {
            return a == null ? b == null : a.equals(b);
        }

        private static String str(String a) {
            return a == null ? "" : a;
        }
    }

    /**
     * Подсказка клавиш «{0}: ДА | {1}: НЕТ»: имена клавиш — из {@link KeyMapping}
     * ({@code getTranslatedKeyMessage}), компонент пересобирается только при переназначении.
     */
    public static final class KeyHint {
        private final String key;
        private final KeyMapping first;
        private final KeyMapping second;
        private Object k1;
        private Object k2;
        private Object m1;
        private Object m2;
        private Component component;
        private String string;

        public KeyHint(String key, KeyMapping first, KeyMapping second) {
            this.key = key;
            this.first = first;
            this.second = second;
        }

        public Component get() {
            if (component == null
                    || first.getKey() != k1
                    || second.getKey() != k2
                    || first.getKeyModifier() != m1
                    || second.getKeyModifier() != m2) {
                k1 = first.getKey();
                k2 = second.getKey();
                m1 = first.getKeyModifier();
                m2 = second.getKeyModifier();
                component =
                        Component.translatable(key, first.getTranslatedKeyMessage(), second.getTranslatedKeyMessage());
                string = null;
            }
            return component;
        }

        public String string() {
            Component c = get();
            if (string == null) string = c.getString();
            return string;
        }
    }
}
