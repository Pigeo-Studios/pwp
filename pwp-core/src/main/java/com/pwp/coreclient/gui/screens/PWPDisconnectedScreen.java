package com.pwp.coreclient.gui.screens;

import com.pwp.coreclient.gui.animations.Easing;
import com.pwp.coreclient.gui.theme.PWPTheme;
import com.pwp.coreclient.gui.theme.PWPTheme.Spacing;
import com.pwp.coreclient.ui.UIButton;
import com.pwp.coreclient.ui.UIDraw;
import com.pwp.coreclient.ui.UIIcon;
import com.pwp.coreclient.ui.UILayer;
import com.pwp.coreclient.ui.layout.Align;
import com.pwp.coreclient.ui.layout.Fit;
import com.pwp.coreclient.ui.layout.Rect;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

/**
 * Экран разрыва соединения (подменяет ванильный DisconnectedScreen): по центру стеклянный
 * диалог — шапка с предупреждением, причина (перенос по словам, лишнее — «…»), кнопка
 * «Вернуться в меню». Кнопка кликабельна после проявления диалога (как раньше: её не было до
 * конца фейда). ESC закрывает (ваниль уводит в главное меню).
 */
public class PWPDisconnectedScreen extends Screen {
    private static final long FADE_MS = 250;

    private final Component reason;
    private final UILayer layer = new UILayer();
    private long openTime;
    private UIButton backButton;

    // раскладка (пересчитывается в init — при каждом изменении размера окна)
    private Rect panel = Rect.of(0, 0, 0, 0);
    private Rect header = panel;
    private Rect body = panel;
    private List<FormattedCharSequence> reasonLines = List.of();

    public PWPDisconnectedScreen(Component reason) {
        super(Component.translatable("pwp_core.disconnect.title"));
        this.reason = reason;
    }

    @Override
    protected void init() {
        if (openTime == 0) openTime = System.currentTimeMillis();
        layer.clear();
        var font = PWPTheme.Fonts.display();
        Fit.Density d = PWPChrome.density(width, height);
        int m = PWPChrome.margin(width);
        int top = PWPChrome.topBarHeight(d);
        int headerH = d == Fit.Density.COMPACT ? Spacing.PANEL_HEADER_COMPACT : Spacing.PANEL_HEADER;
        int pad = d == Fit.Density.COMPACT ? Spacing.SM : Spacing.PANEL_PADDING;
        int btnH = PWPLoadingView.buttonHeight(d);

        Component backLabel = Component.translatable("pwp_core.disconnect.back");
        int panelW = PWPLoadingView.panelWidth(width);
        int contentW = panelW - 2 * pad;

        // сколько строк причины влезает по высоте экрана
        Rect area = Rect.of(m, top + m, width - 2 * m, height - top - 2 * m);
        int fixedH = headerH + 3 * pad + btnH;
        int maxLines = Math.max(
                1,
                Math.min(
                        Spacing.REASON_LINES_MAX,
                        (area.h() - fixedH + PWPTheme.Text.LINE_GAP) / (font.lineHeight + PWPTheme.Text.LINE_GAP)));
        Component shownReason = reason == null || reason.getString().isBlank()
                ? Component.translatable("pwp_core.disconnect.no_reason")
                : reason;
        reasonLines = UIDraw.wrap(font, shownReason, contentW, maxLines);

        int bodyH = UIDraw.linesHeight(font, reasonLines.size());
        int panelH = Math.min(area.h(), fixedH + bodyH);
        panel = area.centered(panelW, panelH);
        header = panel.top(headerH).inset(Spacing.HAIRLINE, Spacing.HAIRLINE, Spacing.HAIRLINE, 0);
        Rect inner = panel.below(headerH, 0).inset(pad);
        body = inner.above(btnH, pad);

        int bw = Math.min(inner.w(), PWPLoadingView.buttonWidth(font, backLabel));
        Rect btn = inner.bottomStrip(btnH).align(bw, btnH, Align.END, Align.STRETCH);
        backButton =
                layer.add(new UIButton(backLabel, () -> Minecraft.getInstance().setScreen(new PWPMainMenuScreen()))
                        .style(UIButton.Style.ACCENT)
                        .icon(UIIcon.BACK));
        backButton.setBounds(btn);
    }

    private float fade() {
        return Easing.easeOutCubic(Math.min((System.currentTimeMillis() - openTime) / (float) FADE_MS, 1F));
    }

    @Override
    public void renderBackground(GuiGraphics gui) {
        int w = Minecraft.getInstance().getWindow().getGuiScaledWidth();
        int h = Minecraft.getInstance().getWindow().getGuiScaledHeight();
        PWPRotatingBackground.render(gui, 0, 0, w, h);
    }

    @Override
    public void render(GuiGraphics gui, int mx, int my, float pt) {
        renderBackground(gui);
        var font = PWPTheme.Fonts.display();
        Fit.Density d = PWPChrome.density(width, height);
        PWPChrome.topBar(gui, font, width, d, 1F);

        float fade = fade();
        int slide = Math.round((1F - fade) * PWPTheme.Animations.ENTER_SLIDE);
        Rect p = panel.translate(0, slide);
        UIDraw.glass(gui, p, fade, true);
        UIDraw.header(
                gui,
                font,
                header.translate(0, slide),
                UIIcon.WARNING,
                Component.translatable("pwp_core.disconnect.title"),
                PWPTheme.Text.DANGER,
                fade);
        UIDraw.lines(
                gui, font, reasonLines, body.translate(0, slide), false, UIDraw.fade(PWPTheme.Text.BODY, fade), false);

        backButton.setAppearance(fade, 0, slide);
        layer.render(gui, mx, my, pt, width, height);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (fade() >= 1F && layer.mouseClicked(mx, my, button)) return true;
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        layer.mouseReleased(mx, my, button);
        return super.mouseReleased(mx, my, button);
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return true;
    }
}
