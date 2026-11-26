package com.pwp.coreclient.gui.screens;

import com.pwp.coreclient.gui.theme.PWPTheme;
import com.pwp.coreclient.gui.theme.PWPTheme.Spacing;
import com.pwp.coreclient.hub.PwpHub;
import com.pwp.coreclient.ui.UICard;
import com.pwp.coreclient.ui.UIDraw;
import com.pwp.coreclient.ui.UILayer;
import com.pwp.coreclient.ui.UIScrollArea;
import com.pwp.coreclient.ui.layout.Fit;
import com.pwp.coreclient.ui.layout.Grid;
import com.pwp.coreclient.ui.layout.Rect;
import java.util.List;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * «Меню PWP» (этап 11): все экраны одним списком по разделам — Игра, Профиль, Сообщество, Настройки, Админ.
 * Карточка: иконка, название, одна строка «что это», справа — клавиша, если есть. Раздел без видимых
 * карточек не рисуется. Прокрутка колесом, если не влезает. Открывается из паузы (вторая кнопка).
 * Тест-стенд: {@code /pwpui open core.hub}.
 */
@OnlyIn(Dist.CLIENT)
public class PWPHubScreen extends Screen {
    private static final Logger LOG = LoggerFactory.getLogger(PWPHubScreen.class);
    private static final int CARD_MIN_W = 190;
    private static final int MAX_COLS = 3;

    private final Screen parent;
    private final boolean preview;
    private final UILayer layer = new UILayer();
    private final UIScrollArea scroll = new UIScrollArea();
    private Rect body = Rect.of(0, 0, 0, 0);

    /** Заголовок раздела внутри прокрутки (едет вместе с карточками). */
    private static final class SectionLabel extends com.pwp.coreclient.ui.UIWidget {
        private final Component title;

        SectionLabel(Component title) {
            super(0, 0, 0, 0);
            this.title = title;
        }

        @Override
        public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
            UIDraw.sectionTitle(g, font(), bounds(), title, PWPTheme.Text.HEADING, opacity);
        }
    }

    public PWPHubScreen(Screen parent) {
        this(parent, false);
    }

    private PWPHubScreen(Screen parent, boolean preview) {
        super(Component.translatable("pwp_core.hub.title"));
        this.parent = parent;
        this.preview = preview;
    }

    public static PWPHubScreen preview() {
        return new PWPHubScreen(null, true);
    }

    @Override
    protected void init() {
        layer.clear();
        scroll.clear();
        Fit.Density d = PWPChrome.density(width, height);
        int m = d == Fit.Density.COMPACT ? Spacing.SM : PWPChrome.margin(width);
        int top = PWPChrome.topBarHeight(d) + Spacing.SM;
        body = Rect.of(m, top, width - 2 * m, height - top - PWPChrome.footerHeight(d) - Spacing.SM);
        scroll.setBounds(body);
        layer.add(scroll);

        Font font = PWPTheme.Fonts.display();
        int cardH = UICard.preferredHeight();
        int gap = Spacing.SM;
        int contentW = scroll.contentWidth(Integer.MAX_VALUE / 4);
        int cols = Grid.columnsFor(contentW, CARD_MIN_W, gap, MAX_COLS);
        int y = 0;
        for (PwpHub.Section s : PwpHub.Section.values()) {
            List<PwpHub.Entry> list = PwpHub.visible(s);
            if (list.isEmpty()) continue;
            int labelH = font.lineHeight + Spacing.XS;
            scroll.place(new SectionLabel(Component.translatable(s.titleKey())), Rect.of(0, y, contentW, labelH));
            y += labelH;
            int h = Grid.height(list.size(), cols, cardH, gap);
            Rect[] cells = Grid.cells(Rect.of(0, y, contentW, h), list.size(), cols, cardH, gap);
            for (int i = 0; i < list.size(); i++) {
                PwpHub.Entry e = list.get(i);
                UICard card = new UICard(Component.translatable(e.titleKey()), () -> open(e))
                        .subtitle(Component.translatable(e.descKey()))
                        .icon(e.icon(), PWPTheme.Colors.ACCENT);
                KeyMapping k = e.key().get();
                if (k != null && !k.isUnbound()) card.badge(k.getTranslatedKeyMessage());
                scroll.place(card, cells[i]);
            }
            y += h + Spacing.LG;
        }
    }

    private void open(PwpHub.Entry e) {
        if (preview) {
            LOG.info("[PWPUI] hub: {} (preview — не открываю)", e.id());
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        Screen next;
        try {
            next = e.open().apply(this);
        } catch (RuntimeException ex) {
            LOG.warn("Меню PWP: карточка {} не открылась", e.id(), ex);
            return;
        }
        if (next != null) mc.setScreen(next);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        PWPPauseScreen.renderMenuBackground(g, width, height);
        Font font = PWPTheme.Fonts.display();
        Fit.Density d = PWPChrome.density(width, height);
        PWPChrome.topBar(g, font, width, d, 1F, title);
        layer.render(g, mouseX, mouseY, partialTick, width, height);
        PWPChrome.footer(g, font, width, height, d, Component.translatable("pwp_core.hub.footer"), null);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (layer.mouseClicked(mx, my, button)) return true;
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        layer.mouseReleased(mx, my, button);
        return super.mouseReleased(mx, my, button);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        if (layer.mouseScrolled(mx, my, delta)) return true;
        return super.mouseScrolled(mx, my, delta);
    }

    @Override
    public boolean isPauseScreen() {
        return parent == null || parent.isPauseScreen();
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(parent);
    }
}
