package com.pwp.coreclient.gui.screens;

import com.pwp.coreclient.gui.theme.PWPTheme;
import com.pwp.coreclient.gui.theme.PWPTheme.Animations;
import com.pwp.coreclient.gui.theme.PWPTheme.Spacing;
import com.pwp.coreclient.gui.theme.PWPTheme.Styles;
import com.pwp.coreclient.ui.UIDraw;
import com.pwp.coreclient.ui.UIIcon;
import com.pwp.coreclient.ui.UIMath;
import com.pwp.coreclient.ui.layout.Align;
import com.pwp.coreclient.ui.layout.Fit;
import com.pwp.coreclient.ui.layout.Flow;
import com.pwp.coreclient.ui.layout.Rect;
import java.util.List;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

/**
 * Общий вид экранов загрузки (PWPLoadingScreen, ванильные ConnectScreen / LevelLoadingScreen
 * через миксины): фон мира, верхняя полоса с логотипом, внизу слева стеклянная панель —
 * шапка со статусом, индикатор (бегущий блок или прогресс) и совет. Под панелью — слот кнопки
 * (Отмена/Назад). Высота панели фиксирована под максимум строк совета — при смене совета
 * ничего не прыгает.
 */
public final class PWPLoadingView {

    /** Раскладка кадра: панель и слот кнопки под ней (пустой, если кнопка не нужна). */
    public record Layout(Rect panel, Rect button) {}

    private final long openMs = System.currentTimeMillis();
    private final PWPTipsWidget tips = new PWPTipsWidget();

    /** Ширина кнопки под панелью по её подписи. */
    public static int buttonWidth(Font font, Component label) {
        return Math.max(Spacing.BUTTON_WIDTH, font.width(label) + 2 * Spacing.LG);
    }

    public static int buttonHeight(Fit.Density d) {
        return d.pick(Spacing.MENU_BUTTON_H_COMPACT, Spacing.BUTTON_HEIGHT, Spacing.BUTTON_HEIGHT);
    }

    private static int headerHeight(Fit.Density d) {
        return d == Fit.Density.COMPACT ? Spacing.PANEL_HEADER_COMPACT : Spacing.PANEL_HEADER;
    }

    private static int padding(Fit.Density d) {
        return d == Fit.Density.COMPACT ? Spacing.SM : Spacing.PANEL_PADDING;
    }

    /** Ширина панели: доля экрана в пределах темы, но не шире экрана без полей. */
    public static int panelWidth(int w) {
        int m = PWPChrome.margin(w);
        return Fit.fraction(
                w,
                Spacing.DIALOG_WIDTH_FRACTION,
                Spacing.DIALOG_WIDTH_MIN,
                Math.min(Spacing.DIALOG_WIDTH_MAX, w - 2 * m));
    }

    /** Раскладка: панель с телом высотой bodyH, внизу слева, кнопка шириной buttonW под ней. */
    public static Layout layout(int w, int h, int bodyH, int buttonW) {
        Fit.Density d = PWPChrome.density(w, h);
        int m = PWPChrome.margin(w);
        int top = PWPChrome.topBarHeight(d) + m;
        Rect area = Rect.of(m, top, w - 2 * m, h - top - m);
        int panelH = headerHeight(d) + 2 * padding(d) + bodyH;
        int pw = panelWidth(w);
        if (buttonW <= 0) {
            Rect[] col = Flow.column(area, 0, Flow.Justify.END, panelH);
            return new Layout(col[0].align(pw, col[0].h(), Align.START, Align.STRETCH), Rect.of(0, 0, 0, 0));
        }
        int bh = buttonHeight(d);
        Rect[] col = Flow.column(area, Spacing.SM, Flow.Justify.END, panelH, bh);
        return new Layout(
                col[0].align(pw, col[0].h(), Align.START, Align.STRETCH),
                col[1].align(Math.min(buttonW, pw), col[1].h(), Align.START, Align.STRETCH));
    }

    /** Строк совета под плотность. */
    private static int tipLines(Fit.Density d) {
        return d == Fit.Density.COMPACT ? Spacing.TIP_LINES_COMPACT : Spacing.TIP_LINES;
    }

    /** Высота тела обычного состояния: индикатор + подпись «СОВЕТ» + строки совета. */
    private static int loadingBodyHeight(Font font, Fit.Density d) {
        return Spacing.LOADER_BAR_H
                + Spacing.SM
                + font.lineHeight
                + Spacing.XXS
                + UIDraw.linesHeight(font, tipLines(d));
    }

    /**
     * Обычное состояние загрузки.
     *
     * @param progress 0..1 — известный прогресс, &lt; 0 — неопределённый (бегущий блок)
     * @param buttonLabel подпись кнопки под панелью (для слота) или null
     */
    public Layout render(
            GuiGraphics g,
            Font font,
            int w,
            int h,
            UIIcon icon,
            Component status,
            float progress,
            Component buttonLabel) {
        Fit.Density d = PWPChrome.density(w, h);
        long elapsed = System.currentTimeMillis() - openMs;
        float enter = UIMath.stagger(elapsed, 0, 0, 0, Animations.ENTER);
        PWPChrome.topBar(g, font, w, d, 1F);

        int bw = buttonLabel == null ? 0 : buttonWidth(font, buttonLabel);
        Layout lay = layout(w, h, loadingBodyHeight(font, d), bw);
        Rect panel = lay.panel().translate(0, Math.round((1F - enter) * Animations.ENTER_SLIDE));
        UIDraw.glass(g, panel, enter, true);
        Rect header = panel.top(headerHeight(d)).inset(Spacing.HAIRLINE, Spacing.HAIRLINE, Spacing.HAIRLINE, 0);
        UIDraw.header(g, font, header, icon, status, PWPTheme.Text.HEADING, enter);

        Rect body = panel.below(headerHeight(d), 0).inset(padding(d));
        Rect bar = body.top(Spacing.LOADER_BAR_H);
        renderLoader(g, bar, progress, elapsed, enter);

        Rect tipArea = body.below(Spacing.LOADER_BAR_H, Spacing.SM);
        UIDraw.text(
                g,
                font,
                Component.translatable("pwp_core.loading.tip_label"),
                tipArea.x(),
                tipArea.y(),
                UIDraw.fade(PWPTheme.Text.ACCENT, enter),
                false);
        tips.tick();
        Rect tipText = tipArea.below(font.lineHeight, Spacing.XXS);
        List<FormattedCharSequence> lines = UIDraw.wrap(font, tips.text(), tipText.w(), tipLines(d));
        int slide = Math.round(tips.slide() * Spacing.XS);
        g.enableScissor(tipText.x(), tipText.y(), tipText.right(), tipText.bottom());
        UIDraw.lines(
                g,
                font,
                lines,
                tipText.translate(0, slide),
                false,
                UIDraw.fade(PWPTheme.Text.MUTED, enter * tips.alpha()),
                false);
        g.disableScissor();
        return lay;
    }

    /** Состояние ошибки: шапка с предупреждением, пояснение; слот кнопки «Назад». */
    public Layout renderError(
            GuiGraphics g,
            Font font,
            int w,
            int h,
            Component title,
            Component hint,
            float fade,
            Component buttonLabel) {
        Fit.Density d = PWPChrome.density(w, h);
        PWPChrome.topBar(g, font, w, d, 1F);
        int contentW = panelWidth(w) - 2 * padding(d);
        List<FormattedCharSequence> lines = UIDraw.wrap(font, hint, contentW, tipLines(d));
        int bw = buttonLabel == null ? 0 : buttonWidth(font, buttonLabel);
        Layout lay = layout(w, h, UIDraw.linesHeight(font, Math.max(1, lines.size())), bw);
        Rect panel = lay.panel();
        UIDraw.glass(g, panel, Math.max(fade, 0.01F), true);
        Rect header = panel.top(headerHeight(d)).inset(Spacing.HAIRLINE, Spacing.HAIRLINE, Spacing.HAIRLINE, 0);
        UIDraw.header(g, font, header, UIIcon.WARNING, title, PWPTheme.Text.DANGER, fade);
        Rect body = panel.below(headerHeight(d), 0).inset(padding(d));
        UIDraw.lines(g, font, lines, body, false, UIDraw.fade(PWPTheme.Text.MUTED, fade), false);
        return lay;
    }

    /** Индикатор: известный прогресс — заливка; иначе — бегущий туда-обратно блок. */
    private static void renderLoader(GuiGraphics g, Rect bar, float progress, long elapsed, float opacity) {
        UIDraw.rect(g, bar, UIDraw.fade(Styles.Loader.TRACK, opacity));
        if (progress >= 0F) {
            int fw = Math.round(bar.w() * Math.min(1F, progress));
            UIDraw.rect(g, bar.withWidth(fw), UIDraw.fade(Styles.Loader.FILL, opacity));
            return;
        }
        int block = Math.max(Spacing.LG, bar.w() / Spacing.LOADER_BLOCK_DIV);
        float t = UIMath.pingPong(elapsed, Animations.LOADER_PERIOD);
        int bx = bar.x() + Math.round((bar.w() - block) * t);
        Rect glow = Rect.of(bx - Spacing.SM, bar.y(), block + 2 * Spacing.SM, bar.h());
        g.enableScissor(bar.x(), bar.y(), bar.right(), bar.bottom()); // свечение не выходит за трек
        UIDraw.gradientH(
                g,
                glow.left(Spacing.SM),
                PWPTheme.Colors.withAlpha(Styles.Loader.FILL_GLOW, 0),
                UIDraw.fade(Styles.Loader.FILL_GLOW, opacity));
        UIDraw.gradientH(
                g,
                glow.rightStrip(Spacing.SM),
                UIDraw.fade(Styles.Loader.FILL_GLOW, opacity),
                PWPTheme.Colors.withAlpha(Styles.Loader.FILL_GLOW, 0));
        UIDraw.rect(g, Rect.of(bx, bar.y(), block, bar.h()), UIDraw.fade(Styles.Loader.FILL, opacity));
        g.disableScissor();
    }
}
