package com.pigeostudios.pwp.warfare.client.gui.ui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.pigeostudios.pwp.warfare.item.RallyItem;
import com.pwp.coreclient.gui.theme.PWPTheme;
import com.pwp.coreclient.gui.theme.PWPTheme.Colors;
import com.pwp.coreclient.gui.theme.PWPTheme.Spacing;
import com.pwp.coreclient.gui.theme.PWPTheme.Styles;
import com.pwp.coreclient.ui.UIAnim;
import com.pwp.coreclient.ui.UIDraw;
import com.pwp.coreclient.ui.layout.Rect;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Matrix4f;

/**
 * Общая база всех радиальных меню warfare (рация, оборона, стены, установки, снабжение FOB/ящика,
 * тактические маркеры). Наследник отдаёт только СПИСОК пунктов ({@link #entries()}, пересобирается
 * каждый кадр — кулдауны/материалы живые); геометрия, hit-test, анимации и рисование — здесь.
 *
 * <p>Раскладка: пункт 0 — вверху, далее по часовой стрелке; сектор i занимает
 * [i·step − step/2, i·step + step/2] от «вверх». Пункт выбирается при расстоянии от центра больше
 * мёртвой зоны (как в прежних радиалках — без внешнего предела). Опционально — действие центра
 * ({@link #centerAction()}), правый клик — {@link #onBack()}.
 */
@OnlyIn(Dist.CLIENT)
public abstract class WarfareRadialScreen extends Screen {
    private static final int ARC_STEPS = 32;

    /**
     * Пункт радиалки.
     *
     * @param detail вторая строка (стоимость, кулдаун) или null
     * @param accent цвет иконки/акцента пункта
     * @param warn красная подсветка (кулдаун/нет материалов), но клик проходит (как раньше)
     * @param action действие; null — пункт выключен (клик поглощается, меню не закрывается)
     */
    public record Entry(
            Component label, WarfareIcon icon, Component detail, int accent, boolean warn, Runnable action) {
        public static Entry of(Component label, WarfareIcon icon, Runnable action) {
            return new Entry(label, icon, null, Colors.ACCENT, false, action);
        }

        public Entry detail(Component d) {
            return new Entry(label, icon, d, accent, warn, action);
        }

        public Entry accent(int c) {
            return new Entry(label, icon, detail, c, warn, action);
        }

        public Entry warn(boolean w) {
            return new Entry(label, icon, detail, accent, w, action);
        }

        public boolean enabled() {
            return action != null;
        }
    }

    private final UIAnim open = new UIAnim(0F, PWPTheme.Animations.DURATION_FAST, UIAnim.EASE_OUT_CUBIC);
    private final List<UIAnim> hover = new ArrayList<>();
    private final UIAnim centerHover = new UIAnim(0F, PWPTheme.Animations.DURATION_FAST, UIAnim.EASE_OUT_CUBIC);
    /** Переход в другую радиалку — не проигрывать анимацию «закрыть» рации. */
    private boolean switching;
    /** Тест-стенд: клики по пунктам ничего не выполняют (никаких пакетов). */
    private boolean preview;

    protected WarfareRadialScreen(Component title) {
        super(title);
    }

    /** Тест-стенд UI: пункты рисуются, но клики не выполняют действий. */
    public WarfareRadialScreen asPreview() {
        this.preview = true;
        return this;
    }

    /** Пункты по часовой стрелке от верхнего. */
    protected abstract List<Entry> entries();

    /** Действие клика в центр (null — нет). */
    protected Runnable centerAction() {
        return null;
    }

    /** Подпись центра, когда ничего не наведено (по умолчанию — заголовок). */
    protected Component centerText() {
        return title;
    }

    /** Вторая строка центра (материалы и т.п.) или null. */
    protected Component centerInfo() {
        return null;
    }

    /** Правый клик (по умолчанию — ничего, как у радиалок без родителя). */
    protected boolean onBack() {
        return false;
    }

    /** Проигрывать анимацию рации (deploy/close) при открытии/закрытии. */
    protected boolean radioAnimations() {
        return false;
    }

    /** Открыть другую радиалку без анимации «закрыть» рации. */
    protected void switchTo(Screen next) {
        switching = true;
        Minecraft.getInstance().setScreen(next);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    protected void init() {
        super.init();
        open.snap(0F);
        open.to(1F);
        if (radioAnimations()) triggerRadioAnim("deploy");
    }

    @Override
    public void onClose() {
        if (radioAnimations() && !switching) triggerRadioAnim("close");
        super.onClose();
    }

    private void triggerRadioAnim(String animName) {
        var mc = Minecraft.getInstance();
        if (mc.player != null) {
            ItemStack stack = mc.player.getMainHandItem();
            if (stack.getItem() instanceof RallyItem radio) {
                long instanceId = stack.getOrCreateTag().getLong("GeckoLibID");
                radio.triggerAnim(mc.player, instanceId, "RadioController", animName);
            }
        }
    }

    // ================================================================ геометрия

    private int cx() {
        return width / 2;
    }

    private int cy() {
        return height / 2;
    }

    private float outer() {
        float base = Math.max(
                WTheme.RADIAL_OUTER_MIN,
                Math.min(WTheme.RADIAL_OUTER_MAX, Math.min(width, height) * WTheme.RADIAL_OUTER_FRACTION));
        return Math.min(base, Math.min(width, height) / 2F - Spacing.SM);
    }

    private float inner() {
        return outer() * WTheme.RADIAL_INNER_RATIO;
    }

    /** Индекс пункта под курсором; -1 — в центре; -2 — центр с действием. */
    private int hovered(double mx, double my, int n) {
        double dx = mx - cx();
        double dy = my - cy();
        double dist = Math.sqrt(dx * dx + dy * dy);
        if (centerAction() != null && dist < inner()) return -2;
        if (dist <= WTheme.RADIAL_DEADZONE || n == 0) return -1;
        double angle = Math.toDegrees(Math.atan2(dx, -dy));
        if (angle < 0) angle += 360.0;
        double step = 360.0 / n;
        return (int) Math.floor((angle + step / 2) / step) % n;
    }

    // ================================================================ рисование

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        List<Entry> list = entries();
        int n = list.size();
        while (hover.size() < n) hover.add(new UIAnim(0F, PWPTheme.Animations.DURATION_FAST, UIAnim.EASE_OUT_CUBIC));
        float a = open.value();

        UIDraw.gradientV(
                g,
                Rect.screen(width, height),
                UIDraw.fade(WTheme.RADIAL_SCRIM_TOP, a),
                UIDraw.fade(WTheme.RADIAL_SCRIM_BOTTOM, a));
        UIDraw.vignette(g, width, height, UIDraw.fade(Styles.Backdrop.VIGNETTE, a), Styles.Backdrop.VIGNETTE_FRACTION);

        int hov = hovered(mx, my, n);
        float r1 = outer() * (0.85F + 0.15F * a);
        float r0 = inner();
        double step = n > 0 ? 360.0 / n : 360.0;
        double gap = n > 1 ? Math.min(WTheme.RADIAL_GAP_DEG, step * 0.08) : 0;

        // кольцо-подложка (тень) под секторами
        arc(g, cx(), cy(), r0 - 1, r1 + WTheme.RADIAL_GROW + 2, 0, 360, UIDraw.fade(WTheme.RADIAL_RING_SHADOW, a));
        for (int i = 0; i < n; i++) {
            Entry e = list.get(i);
            float h = hover.get(i).to(i == hov ? 1F : 0F).value();
            double c = i * step;
            double a0 = c - step / 2 + gap;
            double a1 = c + step / 2 - gap;
            float grow = r1 + h * WTheme.RADIAL_GROW;
            int base = !e.enabled()
                    ? Styles.Radial.SECTOR_DISABLED
                    : Colors.lerp(Styles.Radial.SECTOR, Styles.Radial.SECTOR_HOVER, h);
            if (e.warn()) base = Colors.lerp(base, Colors.withAlpha(WTheme.STATUS_BAD, 0x66), 0.5F);
            arc(g, cx(), cy(), r0, grow, a0, a1, UIDraw.fade(base, a));
            // внутренний блик к центру и тонкая кромка
            arc(g, cx(), cy(), grow - 1, grow, a0, a1, UIDraw.fade(Styles.Glass.BORDER, a));
            arc(g, cx(), cy(), r0, r0 + 1, a0, a1, UIDraw.fade(Styles.Glass.HIGHLIGHT, a));
            int edge = e.warn() ? WTheme.STATUS_BAD : (e.enabled() ? e.accent() : Colors.TEXT_DIM);
            if (h > 0.01F) arc(g, cx(), cy(), grow - WTheme.RADIAL_EDGE, grow, a0, a1, UIDraw.fade(edge, a * h));
            drawLabel(g, e, c, r0, grow, h, a);
        }
        drawCenter(g, list, hov, r0, a);
    }

    private void drawLabel(GuiGraphics g, Entry e, double centerDeg, float r0, float r1, float h, float a) {
        double rad = Math.toRadians(centerDeg);
        float mid = (r0 + r1) / 2F;
        int lx = cx() + (int) Math.round(Math.sin(rad) * mid);
        int ly = cy() - (int) Math.round(Math.cos(rad) * mid);
        int maxW = (int) Math.max(Spacing.XXL, (r1 - r0) * 1.5F);
        boolean big = r1 - r0 >= WTheme.RADIAL_BIG_ICON_MIN;
        int is = big ? Spacing.ICON_SIZE_MED : Spacing.ICON_SIZE;
        int lines = 1 + (e.detail() != null ? 1 : 0);
        int blockH = (e.icon() != null ? is + Spacing.XXS : 0) + lines * (font.lineHeight + 1);
        int y = ly - blockH / 2;
        int iconColor = !e.enabled()
                ? Colors.TEXT_DIM
                : (e.warn() ? WTheme.STATUS_BAD : Colors.lerp(e.accent(), PWPTheme.Text.HEADING, h * 0.4F));
        if (e.icon() != null) {
            WDraw.icon(g, e.icon(), lx - is / 2, y, is, UIDraw.fade(iconColor, a));
            y += is + Spacing.XXS;
        }
        int text = e.enabled()
                ? Colors.lerp(Styles.Radial.TEXT, Styles.Control.TEXT_HOVER, h)
                : Styles.Radial.TEXT_DISABLED;
        Component label = UIDraw.ellipsize(font, e.label(), maxW);
        UIDraw.text(g, font, label, lx - font.width(label) / 2, y, UIDraw.fade(text, a), true);
        if (e.detail() != null) {
            y += font.lineHeight + 1;
            Component d = UIDraw.ellipsize(font, e.detail(), maxW);
            UIDraw.text(
                    g,
                    font,
                    d,
                    lx - font.width(d) / 2,
                    y,
                    UIDraw.fade(e.warn() ? WTheme.STATUS_BAD : PWPTheme.Text.MUTED, a),
                    true);
        }
    }

    private void drawCenter(GuiGraphics g, List<Entry> list, int hov, float r0, float a) {
        float ch = centerHover.to(hov == -2 ? 1F : 0F).value();
        float cr = r0 - WTheme.RADIAL_CENTER_INSET;
        arc(
                g,
                cx(),
                cy(),
                0,
                cr,
                0,
                360,
                UIDraw.fade(Colors.lerp(Styles.Radial.CENTER, Styles.Radial.SECTOR_HOVER, ch), a));
        arc(g, cx(), cy(), cr - 1, cr, 0, 360, UIDraw.fade(Colors.lerp(Styles.Glass.BORDER, Colors.ACCENT, ch), a));
        Component main;
        Component sub = centerInfo();
        int mainColor = Styles.Radial.TEXT;
        if (hov >= 0 && hov < list.size()) {
            Entry e = list.get(hov);
            main = e.label();
            if (!e.enabled() && e.detail() != null) {
                sub = e.detail();
                mainColor = Styles.Radial.TEXT_DISABLED;
            }
        } else {
            main = centerText();
        }
        int maxW = (int) (cr * 2) - Spacing.XS;
        Component m = UIDraw.ellipsize(font, main, maxW);
        int y = cy() - (sub != null ? font.lineHeight : font.lineHeight / 2);
        UIDraw.text(g, font, m, cx() - font.width(m) / 2, y, UIDraw.fade(mainColor, a), true);
        if (sub != null) {
            Component s = UIDraw.ellipsize(font, sub, maxW);
            UIDraw.text(
                    g,
                    font,
                    s,
                    cx() - font.width(s) / 2,
                    y + font.lineHeight + 1,
                    UIDraw.fade(PWPTheme.Colors.TEXT_ACCENT, a),
                    true);
        }
    }

    /** Кольцевой сектор [a0, a1] (градусы от «вверх» по часовой) — triangle strip. */
    private static void arc(GuiGraphics g, int cx, int cy, float r0, float r1, double a0, double a1, int argb) {
        if (Colors.alpha(argb) == 0 || r1 <= r0) return;
        Matrix4f m = g.pose().last().pose();
        float al = Colors.alpha(argb) / 255F;
        float r = Colors.red(argb) / 255F;
        float gr = Colors.green(argb) / 255F;
        float b = Colors.blue(argb) / 255F;
        int steps = Math.max(4, (int) Math.ceil(ARC_STEPS * (a1 - a0) / 90.0));
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        BufferBuilder buf = Tesselator.getInstance().getBuilder();
        buf.begin(VertexFormat.Mode.TRIANGLE_STRIP, DefaultVertexFormat.POSITION_COLOR);
        for (int i = 0; i <= steps; i++) {
            double ang = Math.toRadians(a0 + (a1 - a0) * i / steps);
            float sx = (float) Math.sin(ang);
            float sy = (float) -Math.cos(ang);
            buf.vertex(m, cx + sx * r1, cy + sy * r1, 0).color(r, gr, b, al).endVertex();
            buf.vertex(m, cx + sx * r0, cy + sy * r0, 0).color(r, gr, b, al).endVertex();
        }
        Tesselator.getInstance().end();
        RenderSystem.disableBlend();
    }

    // ================================================================ ввод

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button == 0) {
            List<Entry> list = entries();
            int h = hovered(mx, my, list.size());
            if (preview && h != -1) return true;
            if (h == -2) {
                centerAction().run();
                return true;
            }
            if (h >= 0) {
                Entry e = list.get(h);
                if (e.action() != null) e.action().run();
                return true;
            }
        }
        if (button == 1 && onBack()) return true;
        return super.mouseClicked(mx, my, button);
    }
}
