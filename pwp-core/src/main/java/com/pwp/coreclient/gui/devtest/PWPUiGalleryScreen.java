package com.pwp.coreclient.gui.devtest;

import com.pwp.coreclient.gui.screens.PWPChrome;
import com.pwp.coreclient.gui.screens.PWPRotatingBackground;
import com.pwp.coreclient.gui.theme.PWPTheme;
import com.pwp.coreclient.gui.theme.PWPTheme.Spacing;
import com.pwp.coreclient.ui.UIButton;
import com.pwp.coreclient.ui.UIDraw;
import com.pwp.coreclient.ui.UIIcon;
import com.pwp.coreclient.ui.UILayer;
import com.pwp.coreclient.ui.UIScrollArea;
import com.pwp.coreclient.ui.layout.Align;
import com.pwp.coreclient.ui.layout.Fit;
import com.pwp.coreclient.ui.layout.Flow;
import com.pwp.coreclient.ui.layout.Grid;
import com.pwp.coreclient.ui.layout.Rect;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Галерея тест-стенда UI: переключатели (язык en/ru/uk, «длинные строки», «пустые данные») и
 * сетка кнопок всех зарегистрированных экранов ({@link UiGallery#ids()}). Клик — открыть
 * экран на фикстурах. Смена языка перезагружает ресурсы (как в настройках игры).
 */
public class PWPUiGalleryScreen extends Screen {
    private static final String[] LANGS = {"en_us", "ru_ru", "uk_ua"};

    private final UILayer layer = new UILayer();
    private final UIScrollArea list = new UIScrollArea();
    private Rect titleR = Rect.of(0, 0, 0, 0);

    public PWPUiGalleryScreen() {
        super(Component.translatable("pwp_core.uitest.title"));
    }

    @Override
    protected void init() {
        layer.clear();
        list.clear();
        var font = PWPTheme.Fonts.display();
        Fit.Density d = PWPChrome.density(width, height);
        int m = PWPChrome.margin(width);
        int top = PWPChrome.topBarHeight(d) + Spacing.SM;
        int btnH = d == Fit.Density.COMPACT ? Spacing.MENU_BUTTON_H_COMPACT : Spacing.BUTTON_SMALL_HEIGHT + Spacing.XS;
        Rect area = Rect.of(m, top, width - 2 * m, height - top - PWPChrome.footerHeight(d) - Spacing.SM);
        Rect[] col =
                Flow.column(area, 0, Flow.Justify.START, font.lineHeight, Spacing.SM, btnH, Spacing.SM, Flow.flex(1));
        titleR = col[0];

        // переключатели: EN RU UK | длинные строки | пустые данные
        String current = Minecraft.getInstance().getLanguageManager().getSelected();
        UIButton[] opts = new UIButton[LANGS.length + 2];
        for (int i = 0; i < LANGS.length; i++) {
            String code = LANGS[i];
            opts[i] = new UIButton(Component.literal(code.substring(0, 2).toUpperCase()), () -> setLanguage(code))
                    .style(UIButton.Style.DARK)
                    .selected(code.equals(current));
        }
        opts[LANGS.length] = new UIButton(Component.translatable("pwp_core.uitest.long_strings"), () -> {
                    UiGallery.setLongStrings(!UiGallery.longStrings());
                    init(minecraft, width, height);
                })
                .style(UIButton.Style.DARK)
                .selected(UiGallery.longStrings());
        opts[LANGS.length + 1] = new UIButton(Component.translatable("pwp_core.uitest.empty_data"), () -> {
                    UiGallery.setEmptyData(!UiGallery.emptyData());
                    init(minecraft, width, height);
                })
                .style(UIButton.Style.DARK)
                .selected(UiGallery.emptyData());
        int[] widths = new int[opts.length];
        for (int i = 0; i < opts.length; i++)
            widths[i] = i < LANGS.length ? font.width("WW") + 2 * Spacing.MD : opts[i].preferredWidth();
        Rect[] cells = Flow.row(col[2], Spacing.XS, Flow.Justify.START, widths);
        for (int i = 0; i < opts.length; i++) {
            opts[i].setBounds(cells[i]);
            layer.add(opts[i]);
        }

        // сетка экранов
        list.setBounds(col[4]);
        layer.add(list);
        List<String> ids = UiGallery.ids();
        int gap = Spacing.XS;
        int cols = Grid.columnsFor(col[4].w(), Spacing.GALLERY_ITEM_MIN_W, gap, 4);
        int contentW = list.contentWidth(Grid.height(ids.size(), cols, btnH, gap));
        Rect[] items = Grid.cells(Rect.of(0, 0, contentW, 0), ids.size(), cols, btnH, gap);
        for (int i = 0; i < ids.size(); i++) {
            String id = ids.get(i);
            UIButton b = new UIButton(Component.literal(id), () -> UiGallery.open(id))
                    .style(UIButton.Style.PRIMARY)
                    .align(Align.START)
                    .selected(id.equals(UiGallery.lastOpened()));
            list.place(b, items[i]);
        }
    }

    private void setLanguage(String code) {
        Minecraft mc = Minecraft.getInstance();
        if (code.equals(mc.getLanguageManager().getSelected())) return;
        mc.getLanguageManager().setSelected(code);
        mc.options.languageCode = code;
        mc.reloadResourcePacks().thenRun(() -> mc.tell(() -> init(mc, width, height)));
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        PWPRotatingBackground.render(g, 0, 0, width, height);
        var font = PWPTheme.Fonts.display();
        Fit.Density d = PWPChrome.density(width, height);
        PWPChrome.topBar(
                g,
                font,
                width,
                d,
                1F,
                Component.translatable("pwp_core.uitest.count", UiGallery.ids().size()));
        UIDraw.sectionTitle(
                g, font, titleR, Component.translatable("pwp_core.uitest.title"), PWPTheme.Text.HEADING, 1F);
        layer.render(g, mouseX, mouseY, partialTick, width, height);
        if (UiGallery.ids().isEmpty()) {
            UIDraw.emptyState(
                    g, font, list.bounds(), UIIcon.INFO, Component.translatable("pwp_core.uitest.none"), null, 1F);
        }
        PWPChrome.footer(g, font, width, height, d, Component.translatable("pwp_core.uitest.hint"), null);
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
        return false;
    }
}
