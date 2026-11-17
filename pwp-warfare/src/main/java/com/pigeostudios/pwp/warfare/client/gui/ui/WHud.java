package com.pigeostudios.pwp.warfare.client.gui.ui;

import com.pwp.coreclient.gui.theme.PWPTheme;
import com.pwp.coreclient.gui.theme.PWPTheme.Colors;
import com.pwp.coreclient.ui.UIDraw;
import com.pwp.coreclient.ui.hud.Hud;
import com.pwp.coreclient.ui.hud.HudDraw;
import com.pwp.coreclient.ui.hud.HudTheme;
import com.pwp.coreclient.ui.hud.HudZones;
import com.pwp.coreclient.ui.layout.TextFit;
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

    /**
     * Зоны HUD текущего экрана — общие для всех модов ({@link Hud#zones()}: масштаб из настройки
     * «Масштаб HUD», сброс стеков раз в кадр в {@code RenderGuiEvent.Pre}).
     */
    public static HudZones zones() {
        return Hud.zones();
    }

    // ================================================================ контекстный ярус
    // Константы и обёртки — чтобы pwp-medicine не тянул классы core-client в свой classpath.

    public static final int CTX_DRONE = HudZones.ContextTier.DRONE_CHANNEL;
    public static final int CTX_CONSTRUCTION = HudZones.ContextTier.CONSTRUCTION;
    public static final int CTX_PATIENT = HudZones.ContextTier.PATIENT;

    /** Заявка в контекстный ярус на этот кадр (вызывать в {@code RenderGuiEvent.Pre}). */
    public static void contextOffer(int priority) {
        zones().contextTier().offer(priority);
    }

    /**
     * Старый элемент (look-at, Build Mode — пока на своих координатах) нарисован в этом кадре и
     * занимает низ-центр до Y {@code top}: победитель яруса поднимется над ним или скроется.
     */
    public static void contextReserve(int top) {
        zones().contextTier().reserve(top);
    }

    /**
     * Плашка контекстного яруса (стройка, канал дрона, пациент): иконка, заголовок, значение
     * справа, полоса прогресса снизу. Рисуется, только если {@code priority} — победитель яруса и
     * место есть; иначе false.
     */
    public static boolean contextPlate(
            GuiGraphics g,
            Font font,
            int priority,
            WarfareIcon icon,
            Component title,
            String value,
            int valueColor,
            float frac,
            int accent,
            float o) {
        HudZones z = zones();
        HudZones.ContextTier tier = z.contextTier();
        if (o <= 0F || !tier.shows(priority)) return false;
        int y = tier.place(z.u(HudTheme.CONTEXT_H));
        if (y == HudZones.ContextTier.NONE) return false;
        float s = z.scale();
        int w = Math.min(HudTheme.CONTEXT_W, Math.round(tier.area().w() / s));
        int h = HudTheme.CONTEXT_H;
        HudDraw.begin(g, tier.area().centerX() - w * s / 2F, y, s);
        HudDraw.plate(g, 0, 0, w, h, accent, o);
        int pad = HudTheme.PLATE_STRIPE + HudTheme.PAD_X;
        int ty = HudTheme.PAD_Y + 1;
        int tx = pad;
        if (icon != null) {
            icon(g, icon, tx, ty, HudTheme.ICON, UIDraw.fade(accent, o));
            tx += HudTheme.ICON + HudTheme.ICON_GAP;
        }
        int vw = value == null ? 0 : font.width(value);
        int vx = w - HudTheme.PAD_X - vw;
        if (title != null) {
            Component t = UIDraw.ellipsize(font, title, Math.max(0, vx - HudTheme.PAD_X - tx));
            UIDraw.text(g, font, t, tx, ty, UIDraw.fade(HudTheme.VALUE, o), true);
        }
        HudDraw.text(g, font, value, vx, ty, UIDraw.fade(valueColor, o), true);
        int by = ty + font.lineHeight + HudTheme.PAD_Y;
        HudDraw.bar(g, pad, by, w - pad - HudTheme.PAD_X, HudTheme.CONTEXT_BAR_H, frac, -1F, 10, accent, o);
        HudDraw.end(g);
        return true;
    }

    // ================================================================ боковые стеки (этап 4)
    // Обёртки без типов core-client в сигнатурах — pwp-medicine не тянет core-client в classpath.

    /** «Места нет» для {@link #leftClaim}. */
    public static final int SIDE_NONE = HudZones.Stack.NONE;

    /** Шрифт HUD матча (как у остальных элементов warfare). */
    public static Font font() {
        return PWPTheme.Fonts.display();
    }

    /** Высота боковой плашки-тревоги ({@link #sideAlert}) в базовых единицах. */
    public static int alertHeight(Font font) {
        return plateHeight(font) + WTheme.SIDE_ALERT_EXTRA_H;
    }

    /** Обрезка заголовка плашки-тревоги (без аллокаций, пока заголовок/ширина не менялись). */
    private static final Fit ALERT_FIT = new Fit();

    /**
     * Занять место под плашку-тревогу в начале левого стека (баннер кровотечения). Вызывать в
     * {@code RenderGuiEvent.Pre} (после сброса стеков, до отрисовки HUD warfare): так тревога всегда
     * выше голосований и голоса. Возвращает Y (GUI-пиксели) или {@link #SIDE_NONE}.
     */
    public static int leftClaim(Font font) {
        HudZones z = zones();
        return z.left().next(z.u(alertHeight(font)));
    }

    /**
     * Плашка-тревога левого стека в занятом {@link #leftClaim} месте: акцентная полоска (пульс
     * {@code pulse} 0..1 — сила подсветки), иконка-текстура {@code tex} (целиком, без тинта) или
     * иконка атласа {@code icon}, заголовок цветом акцента. {@code slide} 0..1 — выезд от левого края.
     */
    public static void sideAlert(
            GuiGraphics g,
            Font font,
            int y,
            ResourceLocation tex,
            WarfareIcon icon,
            Component title,
            int accent,
            float slide,
            float pulse) {
        if (y == SIDE_NONE || slide <= 0F) return;
        HudZones z = zones();
        HudZones.Stack st = z.left();
        float s = z.scale();
        int pad = HudTheme.PLATE_STRIPE + HudTheme.PAD_X;
        int is = WTheme.SIDE_ALERT_ICON;
        int maxW = Math.round(st.area().w() / s);
        int tx = pad + is + HudTheme.ICON_GAP + 1;
        if (maxW - HudTheme.PAD_X <= tx) return;
        String t = ALERT_FIT.of(font, title, maxW - HudTheme.PAD_X - tx);
        int w = tx + font.width(t) + HudTheme.PAD_X;
        int h = alertHeight(font);
        float x = st.area().x();
        float hidden = -(w * s) - 1F;
        HudDraw.begin(g, x + (hidden - x) * (1F - Math.min(1F, slide)), y, s);
        HudDraw.plate(g, 0, 0, w, h, accent, 1F);
        if (pulse > 0F) g.fill(0, 0, w, h, UIDraw.fade(accent, WTheme.SIDE_ALERT_PULSE * Math.min(1F, pulse)));
        int iy = (h - is) / 2;
        if (tex != null) {
            HudDraw.icon(g, tex, 0, 0, 1, 1, 1, pad, iy, is, HudTheme.VALUE);
        } else {
            icon(g, icon, pad, iy, is, accent);
        }
        HudDraw.text(g, font, t, tx, (h - font.lineHeight) / 2 + 1, accent, true);
        HudDraw.end(g);
    }

    // ================================================================ плашка-значение в стеке

    /**
     * Плашка-значение в стеке низ-лево/низ-право (БК, припасы): иконка цветом акцента, подпись
     * приглушённо, значение справа; {@code frac} ≥ 0 — тонкая полоса заполнения по низу. Занимает
     * место в стеке; false — не влезла.
     */
    public static boolean stackPlate(
            GuiGraphics g,
            Font font,
            HudZones.Stack stack,
            WarfareIcon icon,
            Component label,
            String value,
            int valueColor,
            float frac,
            int accent,
            float o) {
        HudZones z = zones();
        int h = plateHeight(font);
        int y = stack.next(z.u(h));
        if (y == HudZones.Stack.NONE || o <= 0F) return false;
        float s = z.scale();
        int w = Math.round(stack.area().w() / s);
        HudDraw.begin(g, stack.area().x(), y, s);
        HudDraw.plate(g, 0, 0, w, h, accent, o);
        int tx = HudTheme.PLATE_STRIPE + HudTheme.PAD_X;
        int ty = HudTheme.PAD_Y + 1;
        if (icon != null) {
            icon(g, icon, tx, ty, HudTheme.ICON, UIDraw.fade(accent, o));
            tx += HudTheme.ICON + HudTheme.ICON_GAP;
        }
        if (label != null) UIDraw.text(g, font, label, tx, ty, UIDraw.fade(HudTheme.LABEL, o), false);
        int vw = value == null ? 0 : font.width(value);
        HudDraw.text(g, font, value, w - HudTheme.PAD_X - vw, ty, UIDraw.fade(valueColor, o), true);
        if (frac >= 0F) {
            int fw = Math.round((w - 2) * Math.min(1F, frac));
            if (fw > 0) g.fill(1, h - 1 - HudTheme.VALUE_PLATE_BAR_H, 1 + fw, h - 1, UIDraw.fade(accent, o));
        }
        HudDraw.end(g);
        return true;
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
        HudDraw.bar(g, x, y, w, h, frac, -1F, segments, fill, o);
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
        HudDraw.plate(g, x, y, w, h, accent, o); // фон × настройка «Фон HUD-плашек»
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
        icon(g, icon, x, y + (font.lineHeight - is) / 2, is, c);
        text(g, font, count, x + is + WTheme.HUD_ICON_GAP, y, c, true);
        return iconCountWidth(font, count);
    }

    /** Иконка атласа warfare с тинтом — без лямбды ({@code WDraw.icon} аллоцирует на вызов). */
    public static void icon(GuiGraphics g, WarfareIcon icon, int x, int y, int size, int color) {
        if (icon == null) return;
        HudDraw.icon(
                g,
                WTheme.ICONS,
                icon.u(),
                icon.v(),
                WTheme.ICON_CELL,
                WTheme.ICONS_W,
                WTheme.ICONS_H,
                x,
                y,
                size,
                color);
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

    /**
     * Строка, обрезанная многоточием по ширине; пересчёт только при смене строки/компонента, ширины
     * или языка (обрезка аллоцирует — в кадре её нет, пока ничего не меняется).
     */
    public static final class Fit {
        private String src;
        private Component srcComponent;
        private String lang;
        private int width = -1;
        private String out = "";

        public String of(Font font, String s, int maxW) {
            if (s == null) return "";
            if (srcComponent != null || !s.equals(src) || maxW != width) {
                srcComponent = null;
                src = s;
                width = maxW;
                out = cut(font, s, maxW);
            }
            return out;
        }

        /** Переведённый компонент: строка разрешается заново только при смене языка. */
        public String of(Font font, Component c, int maxW) {
            if (c == null) return "";
            String l = Minecraft.getInstance().getLanguageManager().getSelected();
            if (c != srcComponent || maxW != width || !l.equals(lang)) {
                srcComponent = c;
                src = null;
                lang = l;
                width = maxW;
                out = cut(font, c.getString(), maxW);
            }
            return out;
        }

        private static String cut(Font font, String s, int maxW) {
            return font.width(s) <= maxW ? s : TextFit.ellipsize(s, maxW, font::width);
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
