package com.pwp.coreclient.gui.devtest;

import com.pwp.coreclient.gui.components.PWPButton;
import com.pwp.coreclient.gui.components.PWPCard;
import com.pwp.coreclient.gui.components.PWPContextMenu;
import com.pwp.coreclient.gui.components.PWPLayout;
import com.pwp.coreclient.gui.components.PWPMatchCard;
import com.pwp.coreclient.gui.components.PWPPanel;
import com.pwp.coreclient.gui.components.PWPProgressBar;
import com.pwp.coreclient.gui.components.PWPScrollPanel;
import com.pwp.coreclient.gui.components.PWPSkeleton;
import com.pwp.coreclient.gui.screens.PWPRotatingBackground;
import com.pwp.coreclient.gui.theme.PWPTheme;
import com.pwp.coreclient.gui.theme.PWPTheme.Spacing;
import com.pwp.coreclient.ui.UIButton;
import com.pwp.coreclient.ui.UICard;
import com.pwp.coreclient.ui.UIDraw;
import com.pwp.coreclient.ui.UIIcon;
import com.pwp.coreclient.ui.UILayer;
import com.pwp.coreclient.ui.UIProgressBar;
import com.pwp.coreclient.ui.layout.Flow;
import com.pwp.coreclient.ui.layout.Grid;
import com.pwp.coreclient.ui.layout.Rect;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Витрина компонентов для тест-стенда ({@code core.components}): легаси PWP* (их ещё рисуют
 * warfare-экраны) и новый UI-кит рядом, в адаптивной сетке ячеек. ПКМ — контекстное меню.
 */
public class ComponentsDemoScreen extends Screen {
    private final UILayer layer = new UILayer();
    private final PWPProgressBar legacyBar = new PWPProgressBar();
    private final PWPContextMenu menu = new PWPContextMenu();
    private final PWPScrollPanel scroll = new PWPScrollPanel(0, 0, 0, 0);
    private final long openMs = System.currentTimeMillis();
    private Rect[] cells = new Rect[0];

    public ComponentsDemoScreen() {
        super(Component.literal("components"));
        legacyBar.setProgress(0.65F);
    }

    private static String text(String normal) {
        return UiGallery.longStrings() ? "Очень длинная подпись компонента для проверки обрезки строк" : normal;
    }

    @Override
    protected void init() {
        clearWidgets();
        layer.clear();
        Rect area = Rect.of(Spacing.SM, 28, width - 2 * Spacing.SM, height - 28 - Spacing.LG);
        int cols = Grid.columnsFor(area.w(), 200, Spacing.SM, 4);
        int rows = (8 + cols - 1) / cols;
        int cellH = Math.max(80, (area.h() - (rows - 1) * Spacing.SM) / rows);
        cells = Grid.cells(area, 8, cols, cellH, Spacing.SM);

        // 0: легаси-кнопки (ванильные виджеты)
        Rect[] b = Flow.column(cells[0].inset(Spacing.XS), Spacing.XXS, Flow.Justify.START, 18, 18, 18, 18, 18);
        PWPButton.Style[] st = PWPButton.Style.values();
        for (int i = 0; i < st.length && i < b.length; i++) {
            PWPButton btn = new PWPButton(
                    b[i].x(), b[i].y(), b[i].w(), b[i].h(), Component.literal(text(st[i].name())), x -> {}, st[i]);
            if (i == 4) btn.active = false;
            if (i == 2) btn.setLoading(true);
            addRenderableWidget(btn);
        }
        // 1: новые кнопки
        Rect[] nb = Flow.column(cells[1].inset(Spacing.XS), Spacing.XXS, Flow.Justify.START, 20, 20, 20, 20);
        layer.add(new UIButton(Component.literal(text("ACCENT")), () -> {})
                        .style(UIButton.Style.ACCENT)
                        .icon(UIIcon.PLAY))
                .setBounds(nb[0]);
        layer.add(new UIButton(Component.literal(text("DARK")), () -> {})
                        .style(UIButton.Style.DARK)
                        .icon(UIIcon.SETTINGS))
                .setBounds(nb[1]);
        layer.add(new UIButton(Component.literal(text("SELECTED")), () -> {})
                        .style(UIButton.Style.DARK)
                        .selected(true))
                .setBounds(nb[2]);
        layer.add(new UIButton(Component.literal(text("DISABLED")), () -> {})
                        .style(UIButton.Style.DARK)
                        .setEnabled(false))
                .setBounds(nb[3]);
        // 2: UICard
        Rect[] cb = Flow.column(
                cells[2].inset(Spacing.XS),
                Spacing.XS,
                Flow.Justify.START,
                UICard.preferredHeight(),
                UICard.preferredHeight());
        layer.add(new UICard(Component.literal(text("Стрелок")), () -> {})
                        .subtitle(Component.literal(text("DIRECT_COMBAT · AK-74M")))
                        .badge(Component.literal("3/6"))
                        .badgeWidth(font.width("00/00"))
                        .icon(UIIcon.DIRECT_COMBAT, UIIcon.DIRECT_COMBAT.roleColor())
                        .marked(true)
                        .share(0.4F, PWPTheme.Styles.Loader.FILL))
                .setBounds(cb[0]);
        layer.add(new UICard(Component.literal(text("Медик")), () -> {})
                        .subtitle(Component.literal(text("SUPPORT")))
                        .badge(Component.literal("10/10"))
                        .icon(UIIcon.SUPPORT, UIIcon.SUPPORT.roleColor())
                        .selected(true))
                .setBounds(cb[1]);
        // 4: прогресс
        UIProgressBar bar = new UIProgressBar(0, 0, 0, 0).progress(0.4F, false);
        bar.setBounds(cells[4].inset(Spacing.XS).bottomStrip(Spacing.VOTE_BAR_H));
        layer.add(bar);
        // 7: скролл-панель
        scroll.setBounds(cells[7].x(), cells[7].y(), cells[7].w(), cells[7].h());
        scroll.setContentHeight(30 * 12);
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        PWPRotatingBackground.render(g, 0, 0, width, height);
        var font = PWPTheme.Fonts.display();
        long age = System.currentTimeMillis() - openMs;
        PWPLayout.renderHeader(g, text("COMPONENTS"), -1, width, PWPTheme.Spacing.SM);
        if (cells.length < 8) return;
        // 3: PWPCard 4 состояния
        Rect[] c = Flow.row(cells[3].inset(Spacing.XS), Spacing.XS, Flow.Justify.START, -1, -1, -1, -1);
        PWPCard.State[] states = PWPCard.State.values();
        for (int i = 0; i < 4; i++) PWPCard.render(g, c[i].x(), c[i].y(), c[i].w(), c[i].h(), states[i]);
        // 4: PWPPanel + прогресс
        Rect p = cells[4].inset(Spacing.XS);
        PWPPanel.renderWithTitle(
                g, p.x(), p.y(), p.w(), p.h() - Spacing.LG, text("PWPPanel"), PWPTheme.Colors.TEXT_ACCENT);
        legacyBar.render(g, p.x() + Spacing.SM, p.y() + 30, p.w() - 2 * Spacing.SM, 4, age);
        PWPProgressBar.renderPulse(g, p.x() + Spacing.SM, p.y() + 40, p.w() - 2 * Spacing.SM, 4, age);
        // 5: PWPMatchCard
        Rect m = cells[5].inset(Spacing.XS);
        PWPMatchCard.render(
                g,
                m.x(),
                m.y(),
                m.w(),
                Math.min(m.h(), PWPMatchCard.cardHeight()),
                text("Такмачка"),
                "AAS",
                "12:34",
                63,
                80,
                "UKRAINE",
                "RUSSIA",
                214,
                187,
                PWPMatchCard.Status.PLAYING,
                null,
                false,
                m.contains(mx, my));
        // 6: скелетон + легаси-панель с акцентом
        Rect s = cells[6].inset(Spacing.XS);
        PWPPanel.render(g, s.x(), s.y(), s.w(), s.h(), PWPPanel.Variant.ACCENT_BORDER, true);
        PWPSkeleton.renderCard(
                g,
                s.x() + Spacing.SM,
                s.y() + Spacing.SM,
                s.w() - 2 * Spacing.SM,
                Math.min(46, s.h() - 2 * Spacing.SM),
                age);
        // 7: скролл-панель со строками
        Rect sc = cells[7];
        PWPPanel.render(g, sc.x(), sc.y(), sc.w(), sc.h(), PWPPanel.Variant.SURFACE_DIM);
        scroll.enableScissor(g);
        int off = (int) scroll.getScrollOffset();
        for (int i = 0; i < 30; i++) {
            int y = sc.y() + i * 12 - off;
            UIDraw.text(
                    g,
                    font,
                    Component.literal(text("Строка") + " " + (i + 1)),
                    sc.x() + Spacing.SM,
                    y + 2,
                    PWPTheme.Text.BODY,
                    false);
        }
        scroll.disableScissor(g);
        scroll.renderScrollbar(g);
        PWPLayout.renderDivider(g, Spacing.SM, height - Spacing.LG, width - 2 * Spacing.SM);
        PWPLayout.renderFooter(g, text("ПКМ — контекстное меню, колесо — прокрутка"), width, height);

        super.render(g, mx, my, pt);
        layer.render(g, mx, my, pt, width, height);
        menu.render(g, mx, my);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (menu.isVisible()) {
            menu.mouseClicked(mx, my, button); // клик вне меню просто закрывает его
            return true;
        }
        if (button == 1) {
            menu.show((int) mx, (int) my, List.of(text("Открыть"), text("Переименовать"), text("Удалить")), i -> {});
            return true;
        }
        if (scroll.mouseClicked(mx, my, button)) return true;
        if (layer.mouseClicked(mx, my, button)) return true;
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (scroll.mouseDragged(mx, my, button, dx, dy)) return true;
        return super.mouseDragged(mx, my, button, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        scroll.mouseReleased(mx, my, button);
        layer.mouseReleased(mx, my, button);
        return super.mouseReleased(mx, my, button);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        if (scroll.mouseScrolled(mx, my, delta) != 0) return true;
        return super.mouseScrolled(mx, my, delta);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
