package com.pwp.coreclient.gui.screens;

import com.pwp.coreclient.gui.theme.PWPTheme;
import com.pwp.coreclient.gui.theme.PWPTheme.Spacing;
import com.pwp.coreclient.ui.UIButton;
import com.pwp.coreclient.ui.UIDraw;
import com.pwp.coreclient.ui.UIIcon;
import com.pwp.coreclient.ui.UILayer;
import com.pwp.coreclient.ui.layout.Fit;
import com.pwp.coreclient.ui.layout.Flow;
import com.pwp.coreclient.ui.layout.Rect;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * «Как играть» (этап 4): слева — клавиши PWP из РЕАЛЬНЫХ KeyMapping (категории {@code key.categories.pwp*},
 * подпись — {@code getTranslatedKeyMessage}, т.е. как переназначил игрок), справа — шесть карточек цикла
 * матча (лобби → ожидание → отряд и роль → деплой → точки и тикеты → связь). Открывается из главного
 * меню и паузы. Тест-стенд: {@code /pwpui open core.howto}.
 */
@OnlyIn(Dist.CLIENT)
public class PWPHowToPlayScreen extends Screen {
    private static final String[] CARDS = {"lobby", "staging", "squad", "deploy", "points", "comms"};
    private static final UIIcon[] CARD_ICONS = {
        UIIcon.SERVER, UIIcon.PLAY, UIIcon.INFO, UIIcon.PLAY, UIIcon.INFO, UIIcon.INFO
    };

    private final Screen parent;
    private final UILayer layer = new UILayer();
    private Rect keysPanel = Rect.of(0, 0, 0, 0);
    private Rect cardsPanel = keysPanel;
    private double keysScroll;
    private List<KeyMapping> keys = List.of();

    public PWPHowToPlayScreen(Screen parent) {
        super(Component.translatable("pwp_core.howto.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        layer.clear();
        Minecraft mc = Minecraft.getInstance();
        List<KeyMapping> list = new ArrayList<>();
        for (KeyMapping k : mc.options.keyMappings) {
            if (k.getCategory().startsWith("key.categories.pwp")) list.add(k);
        }
        list.sort(Comparator.comparing(KeyMapping::getCategory).thenComparing(KeyMapping::getName));
        keys = list;
        Fit.Density d = PWPChrome.density(width, height);
        int m = d == Fit.Density.COMPACT ? Spacing.SM : PWPChrome.margin(width);
        int top = PWPChrome.topBarHeight(d);
        int footer = PWPChrome.footerHeight(d);
        Rect body = Rect.of(m, top + m / 2, width - 2 * m, height - top - footer - m);
        int actionH = d == Fit.Density.COMPACT ? Spacing.BUTTON_SMALL_HEIGHT : Spacing.BUTTON_HEIGHT;
        Rect[] v = Flow.column(body, Spacing.SM, Flow.Justify.START, Flow.flex(1), actionH);
        int keysW = Math.min(Spacing.HOWTO_KEYS_W, v[0].w() * 2 / 5);
        Rect[] cols = Flow.row(v[0], Spacing.SM, Flow.Justify.START, keysW, Flow.flex(1));
        keysPanel = cols[0];
        cardsPanel = cols[1];
        int bw = Math.min(Spacing.BUTTON_WIDTH, v[1].w());
        layer.add(new UIButton(
                                Component.translatable("pwp_core.howto.controls"),
                                () -> mc.setScreen(
                                        new net.minecraft.client.gui.screens.controls.KeyBindsScreen(this, mc.options)))
                        .style(UIButton.Style.DARK)
                        .icon(UIIcon.KEYBOARD))
                .setBounds(Rect.of(v[1].x(), v[1].y(), bw, v[1].h()));
        layer.add(new UIButton(Component.translatable("pwp_core.ui_settings.done"), this::onClose)
                        .style(UIButton.Style.ACCENT)
                        .icon(UIIcon.CHECK))
                .setBounds(Rect.of(v[1].right() - bw, v[1].y(), bw, v[1].h()));
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        PWPPauseScreen.renderMenuBackground(g, width, height);
        Font font = PWPTheme.Fonts.display();
        Fit.Density d = PWPChrome.density(width, height);
        PWPChrome.topBar(g, font, width, d, 1F, title);
        int headerH = d == Fit.Density.COMPACT ? Spacing.PANEL_HEADER_COMPACT : Spacing.PANEL_HEADER;
        renderKeys(g, font, headerH);
        renderCards(g, font, headerH);
        layer.render(g, mouseX, mouseY, partialTick, width, height);
    }

    private void renderKeys(GuiGraphics g, Font font, int headerH) {
        UIDraw.glass(g, keysPanel, 1F, false);
        Rect[] v = Flow.column(keysPanel, 0, Flow.Justify.START, headerH, Flow.flex(1));
        UIDraw.header(
                g,
                font,
                v[0],
                UIIcon.KEYBOARD,
                Component.translatable("pwp_core.howto.keys"),
                PWPTheme.Text.HEADING,
                1F);
        Rect list = v[1].inset(Spacing.SM);
        int rowH = font.lineHeight + Spacing.XS;
        double max = Math.max(0, keys.size() * rowH - list.h());
        keysScroll = Math.max(0, Math.min(keysScroll, max));
        g.enableScissor(list.x(), list.y(), list.right(), list.bottom());
        int y = list.y() - (int) keysScroll;
        String lastCat = "";
        for (KeyMapping k : keys) {
            if (y + rowH >= list.y() && y <= list.bottom()) {
                Component key =
                        k.isUnbound() ? Component.translatable("pwp_core.howto.unbound") : k.getTranslatedKeyMessage();
                int kw = Math.min(font.width(key), list.w() / 3);
                Component name =
                        UIDraw.ellipsize(font, Component.translatable(k.getName()), list.w() - kw - Spacing.SM);
                UIDraw.text(g, font, name, list.x(), y, PWPTheme.Text.BODY, false);
                UIDraw.text(
                        g,
                        font,
                        UIDraw.ellipsize(font, key, kw),
                        list.right() - kw,
                        y,
                        k.isUnbound() ? PWPTheme.Text.MUTED : PWPTheme.Text.ACCENT,
                        false);
            }
            lastCat = k.getCategory();
            y += rowH;
        }
        g.disableScissor();
        if (keys.isEmpty() && lastCat.isEmpty()) {
            UIDraw.emptyState(
                    g,
                    font,
                    list,
                    UIIcon.KEYBOARD,
                    Component.translatable("pwp_core.howto.keys_empty"),
                    Component.empty(),
                    1F);
        }
    }

    private void renderCards(GuiGraphics g, Font font, int headerH) {
        UIDraw.glass(g, cardsPanel, 1F, false);
        Rect[] v = Flow.column(cardsPanel, 0, Flow.Justify.START, headerH, Flow.flex(1));
        UIDraw.header(
                g, font, v[0], UIIcon.PLAY, Component.translatable("pwp_core.howto.cycle"), PWPTheme.Text.HEADING, 1F);
        Rect area = v[1].inset(Spacing.SM);
        int cols = area.w() >= 2 * Spacing.HINT_MAX_W ? 2 : 1;
        int rows = (CARDS.length + cols - 1) / cols;
        int cardH = Math.max(Spacing.HOWTO_CARD_H, (area.h() - (rows - 1) * Spacing.SM) / rows);
        int cardW = (area.w() - (cols - 1) * Spacing.SM) / cols;
        for (int i = 0; i < CARDS.length; i++) {
            Rect c = Rect.of(
                    area.x() + (i % cols) * (cardW + Spacing.SM),
                    area.y() + (i / cols) * (cardH + Spacing.SM),
                    cardW,
                    cardH);
            if (c.bottom() > area.bottom() + Spacing.SM) break;
            UIDraw.rect(g, c, PWPTheme.Styles.Glass.HEADER_BOTTOM);
            UIDraw.accentBar(g, c.x(), c.y(), c.h(), Spacing.ACCENT_BAR, PWPTheme.Colors.ACCENT);
            int is = Spacing.ICON_SIZE;
            UIDraw.icon(g, CARD_ICONS[i], c.x() + Spacing.SM, c.y() + Spacing.XS, is, PWPTheme.Text.ACCENT);
            Component t = Component.literal((i + 1) + ". ")
                    .append(Component.translatable("pwp_core.howto.card." + CARDS[i] + ".title"));
            UIDraw.text(
                    g,
                    font,
                    UIDraw.ellipsize(font, t, c.w() - is - 3 * Spacing.SM),
                    c.x() + is + 2 * Spacing.SM,
                    c.y() + Spacing.XS + (is - font.lineHeight) / 2 + 1,
                    PWPTheme.Text.HEADING,
                    true);
            Rect body = Rect.of(
                    c.x() + Spacing.SM,
                    c.y() + is + Spacing.SM,
                    c.w() - 2 * Spacing.SM,
                    c.h() - is - Spacing.SM - Spacing.XS);
            int maxLines = Math.max(1, body.h() / (font.lineHeight + 1));
            List<FormattedCharSequence> lines = UIDraw.wrap(
                    font, Component.translatable("pwp_core.howto.card." + CARDS[i] + ".body"), body.w(), maxLines);
            UIDraw.lines(g, font, lines, body, false, PWPTheme.Text.BODY, false);
        }
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        if (keysPanel.contains((int) mx, (int) my)) {
            keysScroll -= delta * (font.lineHeight + Spacing.XS) * 2;
            return true;
        }
        return super.mouseScrolled(mx, my, delta);
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
    public boolean isPauseScreen() {
        return parent == null || parent.isPauseScreen();
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(parent);
    }
}
