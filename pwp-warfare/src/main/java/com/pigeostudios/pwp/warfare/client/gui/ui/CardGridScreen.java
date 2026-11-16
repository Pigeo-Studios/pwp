package com.pigeostudios.pwp.warfare.client.gui.ui;

import com.pwp.coreclient.gui.theme.PWPTheme;
import com.pwp.coreclient.gui.theme.PWPTheme.Colors;
import com.pwp.coreclient.gui.theme.PWPTheme.Spacing;
import com.pwp.coreclient.ui.UIBackdrop;
import com.pwp.coreclient.ui.UIDraw;
import com.pwp.coreclient.ui.UIMath;
import com.pwp.coreclient.ui.UITooltip;
import com.pwp.coreclient.ui.layout.Align;
import com.pwp.coreclient.ui.layout.Grid;
import com.pwp.coreclient.ui.layout.Rect;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * База экранов «сетка карточек» (выбор кита, команды, фракции, списки китов/скинов): стеклянная
 * панель по центру поверх затемнённого мира, шапка с числом элементов, карточки через Grid
 * (колонки по ширине), прокрутка, состояния hover/selected/disabled, мелкие кнопки-действия в
 * углу карточки, тултип причины недоступности, заглушки «загрузка» и «пусто».
 *
 * <p>Наследник отдаёт список {@link Card} ({@link #cards()}, каждый кадр) — геометрия, ввод и
 * рисование здесь, без магических координат.
 */
@OnlyIn(Dist.CLIENT)
public abstract class CardGridScreen extends Screen {
    /** Мелкое действие в углу карточки (иконка + подсказка). */
    public record Action(WarfareIcon icon, Component tooltip, Runnable run) {}

    /**
     * Карточка.
     *
     * @param image текстура (иконка кита/флаг) или null — тогда {@code icon}
     * @param imageAspect ширина/высота картинки (флаг 64x36 = 16/9, иконка = 1)
     * @param reason причина недоступности (тултип), null — нет
     * @param onPress клик по карточке (null — не нажимается)
     */
    public record Card(
            Object key,
            Component title,
            Component subtitle,
            ResourceLocation image,
            float imageAspect,
            WarfareIcon icon,
            int accent,
            boolean selected,
            boolean enabled,
            Component reason,
            Runnable onPress,
            List<Action> actions) {
        public static Card of(Object key, Component title, Runnable onPress) {
            return new Card(key, title, null, null, 1F, null, Colors.ACCENT, false, true, null, onPress, List.of());
        }

        public Card subtitle(Component s) {
            return new Card(
                    key, title, s, image, imageAspect, icon, accent, selected, enabled, reason, onPress, actions);
        }

        public Card image(ResourceLocation tex, float aspect) {
            return new Card(
                    key, title, subtitle, tex, aspect, icon, accent, selected, enabled, reason, onPress, actions);
        }

        public Card icon(WarfareIcon i) {
            return new Card(
                    key, title, subtitle, image, imageAspect, i, accent, selected, enabled, reason, onPress, actions);
        }

        public Card accent(int c) {
            return new Card(
                    key, title, subtitle, image, imageAspect, icon, c, selected, enabled, reason, onPress, actions);
        }

        public Card selected(boolean s) {
            return new Card(
                    key, title, subtitle, image, imageAspect, icon, accent, s, enabled, reason, onPress, actions);
        }

        public Card disabled(Component why) {
            return new Card(
                    key, title, subtitle, image, imageAspect, icon, accent, selected, false, why, onPress, actions);
        }

        public Card actions(Action... a) {
            return new Card(
                    key,
                    title,
                    subtitle,
                    image,
                    imageAspect,
                    icon,
                    accent,
                    selected,
                    enabled,
                    reason,
                    onPress,
                    List.of(a));
        }
    }

    private final Scroll scroll = new Scroll();
    private final AnimMap hover = new AnimMap();
    private final AnimMap select = new AnimMap(PWPTheme.Animations.DURATION_NORMAL);
    private Object hoveredKey;
    private long hoverSince;

    protected CardGridScreen(Component title) {
        super(title);
    }

    protected abstract List<Card> cards();

    /** Данные ещё грузятся (заглушка вместо сетки). */
    protected boolean loading() {
        return false;
    }

    protected Component emptyTitle() {
        return Component.translatable("gui.pwpwarfare.grid.empty");
    }

    protected Component emptyHint() {
        return null;
    }

    /** Минимальная ширина и высота карточки. */
    protected int cardMinW() {
        return WTheme.CARD_MIN_W;
    }

    protected int cardH() {
        return WTheme.CARD_H;
    }

    protected int maxCols() {
        return WTheme.CARD_COLS_MAX;
    }

    protected WarfareIcon titleIcon() {
        return WarfareIcon.GRID;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    // ================================================================ геометрия

    private Rect panel(int count) {
        int maxW = Math.min(width - 2 * Spacing.SM, WTheme.CARD_PANEL_MAX_W);
        int cols = Grid.columnsFor(maxW - 2 * Spacing.SM, cardMinW(), Spacing.XS, maxCols());
        cols = Math.max(1, Math.min(cols, Math.max(1, count)));
        int natural = cols * WTheme.CARD_MAX_W + (cols - 1) * Spacing.XS + 2 * Spacing.SM + WTheme.SCROLLBAR_W;
        int w = Math.max(Math.min(WTheme.CARD_PANEL_MIN_W, maxW), Math.min(maxW, natural));
        int headerH = WDraw.headerHeight(height);
        int gridH = Grid.height(Math.max(1, count), cols, cardH(), Spacing.XS);
        int h = Math.min(height - 2 * Spacing.LG, headerH + 2 * Spacing.SM + Math.max(gridH, WTheme.CARD_EMPTY_H));
        return Rect.of((width - w) / 2, (height - h) / 2, w, Math.max(0, h));
    }

    private Rect content(Rect p) {
        return p.inset(Spacing.SM, WDraw.headerHeight(height) + Spacing.SM, Spacing.SM, Spacing.SM);
    }

    private Rect[] cells(Rect c, int count) {
        int cols = Grid.columnsFor(c.w() - WTheme.SCROLLBAR_W - Spacing.XS, cardMinW(), Spacing.XS, maxCols());
        cols = Math.max(1, Math.min(cols, Math.max(1, count)));
        Rect area = Rect.of(c.x(), c.y() - scroll.offset(), c.w() - WTheme.SCROLLBAR_W - Spacing.XS, 0);
        return Grid.cells(area, count, cols, cardH(), Spacing.XS);
    }

    private Rect[] actionRects(Rect card, int n) {
        Rect[] out = new Rect[n];
        int s = WTheme.CARD_ACTION;
        for (int i = 0; i < n; i++) {
            out[i] = Rect.of(card.right() - Spacing.XXS - (i + 1) * s - i * Spacing.XXS, card.y() + Spacing.XXS, s, s);
        }
        return out;
    }

    // ================================================================ рисование

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        UIBackdrop.overlay(g, width, height, UIBackdrop.Shade.NONE);
        List<Card> cards = loading() ? List.of() : cards();
        Rect p = panel(cards.size());
        Rect c = WDraw.panel(
                g,
                font,
                p,
                WDraw.headerHeight(height),
                titleIcon(),
                title,
                loading() ? null : Component.literal(String.valueOf(cards.size())),
                1F);
        if (loading()) {
            float t = UIMath.pingPong(System.currentTimeMillis(), PWPTheme.Animations.LOADER_PERIOD);
            WDraw.emptyState(
                    g,
                    font,
                    c,
                    WarfareIcon.CLOCK,
                    Component.translatable("gui.pwpwarfare.grid.loading"),
                    null,
                    0.5F + 0.5F * t);
        } else if (cards.isEmpty()) {
            WDraw.emptyState(g, font, c, WarfareIcon.GRID, emptyTitle(), emptyHint(), 1F);
        } else {
            renderCards(g, c, cards, mx, my);
        }
        super.render(g, mx, my, pt);
    }

    private void renderCards(GuiGraphics g, Rect c, List<Card> cards, int mx, int my) {
        Rect[] cells = cells(c, cards.size());
        int contentH = cells[cells.length - 1].bottom() + scroll.offset() - c.y();
        scroll.clamp(contentH, c.h());
        boolean inside = c.contains(mx, my);
        Object hov = null;
        UITooltip.Content tip = null;
        g.enableScissor(c.x(), c.y(), c.right(), c.bottom());
        for (int i = 0; i < cards.size(); i++) {
            Rect r = cells[i];
            if (r.bottom() < c.y() || r.y() > c.bottom()) continue;
            Card card = cards.get(i);
            boolean over = inside && r.contains(mx, my);
            if (over) {
                hov = card.key();
                if (!card.enabled() && card.reason() != null)
                    tip = new UITooltip.Content(card.title(), List.of(card.reason()));
            }
            Rect[] ar = actionRects(r, card.actions().size());
            for (int a = 0; a < ar.length; a++) {
                if (inside && ar[a].contains(mx, my))
                    tip = UITooltip.Content.of(card.actions().get(a).tooltip());
            }
            renderCard(g, r, card, over, ar, mx, my);
        }
        g.disableScissor();
        WDraw.scrollbar(g, c, contentH, scroll.offset(), 1F);
        if (hov == null || !hov.equals(hoveredKey)) hoverSince = System.currentTimeMillis();
        hoveredKey = hov;
        if (tip != null && System.currentTimeMillis() - hoverSince >= UITooltip.DELAY_MS) {
            UITooltip.render(g, tip, mx, my, width, height, 1F);
        }
    }

    private void renderCard(GuiGraphics g, Rect r, Card card, boolean over, Rect[] actions, int mx, int my) {
        float h = hover.get(card.key(), over && card.enabled() && card.onPress() != null);
        float s = select.get(card.key(), card.selected());
        WDraw.tile(g, r, h, s, card.enabled() ? 1F : 0F, 1F);
        // цветная засечка сверху (команда/категория)
        UIDraw.accentBarH(
                g, r.x() + 1, r.y() + 1, r.w() - 2, 1, Colors.withAlpha(card.accent(), card.enabled() ? 0xB0 : 0x50));

        int lh = font.lineHeight + 1;
        int textH = lh + (card.subtitle() != null ? lh : 0);
        Rect media = Rect.of(
                r.x() + Spacing.XS,
                r.y() + Spacing.XS,
                r.w() - 2 * Spacing.XS,
                Math.max(0, r.h() - textH - 2 * Spacing.XS - Spacing.XXS));
        if (card.image() != null && media.h() > 0) {
            int ih = Math.min(media.h(), Math.round(media.w() / Math.max(0.1F, card.imageAspect())));
            int iw = Math.round(ih * card.imageAspect());
            Rect img = media.centered(iw, ih);
            UIDraw.blitTinted(
                    g,
                    card.enabled() ? 0xFFFFFFFF : Colors.TEXT_SECONDARY,
                    () -> g.blit(card.image(), img.x(), img.y(), img.w(), img.h(), 0F, 0F, 1, 1, 1, 1));
            if (card.imageAspect() != 1F) UIDraw.outline(g, img, PWPTheme.Styles.Glass.BORDER);
        } else if (card.icon() != null && media.h() >= Spacing.ICON_SIZE) {
            int is = media.h() >= Spacing.ICON_SIZE_LG ? Spacing.ICON_SIZE_LG : Spacing.ICON_SIZE;
            WDraw.icon(
                    g,
                    card.icon(),
                    media.centerX() - is / 2,
                    media.centerY() - is / 2,
                    is,
                    card.enabled() ? Colors.lerp(card.accent(), PWPTheme.Text.HEADING, h * 0.3F) : Colors.TEXT_DIM);
        }
        int ty = r.bottom() - Spacing.XS - textH;
        int titleColor =
                !card.enabled() ? Colors.TEXT_DIM : Colors.lerp(PWPTheme.Text.HEADING, PWPTheme.Colors.TEXT_ACCENT, s);
        WDraw.text(
                g,
                font,
                card.title(),
                Rect.of(r.x() + Spacing.XS, ty, r.w() - 2 * Spacing.XS, lh),
                Align.CENTER,
                titleColor,
                true,
                1F);
        if (card.subtitle() != null) {
            WDraw.text(
                    g,
                    font,
                    card.subtitle(),
                    Rect.of(r.x() + Spacing.XS, ty + lh, r.w() - 2 * Spacing.XS, lh),
                    Align.CENTER,
                    PWPTheme.Text.MUTED,
                    false,
                    1F);
        }
        if (card.selected()) {
            WDraw.icon(
                    g,
                    WarfareIcon.STAR,
                    r.x() + Spacing.XXS + Spacing.ACCENT_BAR_SELECTED,
                    r.y() + Spacing.XXS,
                    WTheme.ICON_SMALL,
                    Colors.ACCENT);
        }
        if (!card.enabled()) {
            UIDraw.rect(g, r.inset(1), WTheme.DISABLED_VEIL);
            WDraw.icon(
                    g,
                    WarfareIcon.LOCK,
                    r.x() + Spacing.XXS + Spacing.ACCENT_BAR,
                    r.y() + Spacing.XXS,
                    WTheme.ICON_SMALL,
                    WTheme.STATUS_BAD);
        }
        for (int a = 0; a < actions.length; a++) {
            Rect ar = actions[a];
            float ah = hover.get(card.key() + "#" + a, ar.contains(mx, my));
            WDraw.tile(g, ar, ah, 0F, 1F, 1F);
            int is = Math.min(ar.w(), ar.h()) - 2 * Spacing.XXS;
            WDraw.icon(
                    g,
                    card.actions().get(a).icon(),
                    ar.centerX() - is / 2,
                    ar.centerY() - is / 2,
                    is,
                    Colors.lerp(PWPTheme.Text.MUTED, Colors.ACCENT, ah));
        }
    }

    // ================================================================ ввод

    @Override
    public boolean mouseClicked(double mx, double my, int btn) {
        if (btn == 0 && !loading()) {
            List<Card> cards = cards();
            Rect c = content(panel(cards.size()));
            if (c.contains(mx, my) && !cards.isEmpty()) {
                Rect[] cells = cells(c, cards.size());
                for (int i = 0; i < cards.size(); i++) {
                    Card card = cards.get(i);
                    Rect[] ar = actionRects(cells[i], card.actions().size());
                    for (int a = 0; a < ar.length; a++) {
                        if (ar[a].contains(mx, my)) {
                            card.actions().get(a).run().run();
                            return true;
                        }
                    }
                    if (cells[i].contains(mx, my)) {
                        if (card.enabled() && card.onPress() != null)
                            card.onPress().run();
                        return true;
                    }
                }
            }
        }
        return super.mouseClicked(mx, my, btn);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        scroll.wheel(delta);
        return true;
    }
}
