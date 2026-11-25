package com.pwp.coreclient.gui.screens;

import com.pwp.coreclient.gui.theme.PWPTheme;
import com.pwp.coreclient.gui.theme.PWPTheme.Spacing;
import com.pwp.coreclient.gui.theme.PWPTheme.Styles;
import com.pwp.coreclient.settings.SettingsLayout;
import com.pwp.coreclient.settings.UiSettings;
import com.pwp.coreclient.settings.UiSettingsFile;
import com.pwp.coreclient.ui.UIButton;
import com.pwp.coreclient.ui.UIChoice;
import com.pwp.coreclient.ui.UIDraw;
import com.pwp.coreclient.ui.UIIcon;
import com.pwp.coreclient.ui.UILayer;
import com.pwp.coreclient.ui.UIModal;
import com.pwp.coreclient.ui.UIScrollArea;
import com.pwp.coreclient.ui.UISlider;
import com.pwp.coreclient.ui.UIToggle;
import com.pwp.coreclient.ui.UITooltip;
import com.pwp.coreclient.ui.UIWidget;
import com.pwp.coreclient.ui.layout.Align;
import com.pwp.coreclient.ui.layout.Fit;
import com.pwp.coreclient.ui.layout.Flow;
import com.pwp.coreclient.ui.layout.Rect;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * Настройки интерфейса PWP — собираются из реестра {@link UiSettings} автоматически: слева
 * категории (вкладки), справа ряды «подпись — контрол» (ползунок/тумблер/выбор) в прокрутке.
 * Изменения применяются сразу (их видно за меню — на карте/HUD), файл
 * {@code config/pwp_ui_client.json} пишется по «Готово»/ESC/закрытию. Подписи длиннее места
 * обрезаются «…», полный текст и описание — в тултипе.
 */
@OnlyIn(Dist.CLIENT)
public class PWPUiSettingsScreen extends Screen {
    private final Screen parent;
    private final UILayer layer = new UILayer();
    private final UIScrollArea rows = new UIScrollArea();
    private final List<UIButton> tabs = new ArrayList<>();
    private List<UiSettings.Category> categories = List.of();
    private int selected;
    private Rect panel = Rect.of(0, 0, 0, 0);
    private Rect header = panel;
    private Rect content = panel;

    /** id категории, которую открыть первой (null — первая по порядку). */
    private String initialCategory;

    public PWPUiSettingsScreen(Screen parent) {
        super(Component.translatable("pwp_core.ui_settings.title"));
        this.parent = parent;
    }

    /** Открыть сразу на категории {@code categoryId} (тест-стенд, ссылки из других экранов). */
    public PWPUiSettingsScreen(Screen parent, String categoryId) {
        this(parent);
        this.initialCategory = categoryId;
    }

    @Override
    protected void init() {
        layer.clear();
        tabs.clear();
        categories = UiSettings.categories();
        if (initialCategory != null) {
            for (int i = 0; i < categories.size(); i++) {
                if (categories.get(i).id().equals(initialCategory)) selected = i;
            }
            initialCategory = null;
        }
        if (selected >= categories.size()) selected = 0;
        var font = PWPTheme.Fonts.display();
        Fit.Density d = PWPChrome.density(width, height);
        int m = d == Fit.Density.COMPACT ? Spacing.SM : PWPChrome.margin(width);
        int top = PWPChrome.topBarHeight(d);
        int footer = PWPChrome.footerHeight(d);
        int pw = Math.min(Spacing.SETTINGS_MAX_W, width - 2 * m);
        panel = Rect.of((width - pw) / 2, top + m / 2, pw, height - top - footer - m);
        int headerH = d == Fit.Density.COMPACT ? Spacing.PANEL_HEADER_COMPACT : Spacing.PANEL_HEADER;
        int pad = d == Fit.Density.COMPACT ? Spacing.XS : Spacing.SM;
        int actionH = d == Fit.Density.COMPACT ? Spacing.BUTTON_SMALL_HEIGHT : Spacing.BUTTON_HEIGHT;
        Rect[] v = Flow.column(panel, 0, Flow.Justify.START, headerH, Flow.flex(1), actionH + 2 * pad);
        header = v[0];
        Rect body = v[1].inset(pad);
        Rect actions = v[2].inset(pad);

        // колонки: категории | ряды
        int catW = SettingsLayout.categoryWidth(body.w(), Spacing.SETTING_CAT_MIN_W, Spacing.SETTING_CAT_MAX_W);
        Rect[] cols = Flow.row(body, pad, Flow.Justify.START, catW, Flow.flex(1));
        int tabH = d == Fit.Density.COMPACT ? Spacing.MENU_BUTTON_H_COMPACT : Spacing.MENU_BUTTON_H;
        for (int i = 0; i < categories.size(); i++) {
            final int idx = i;
            UiSettings.Category c = categories.get(i);
            Component title = Component.translatable(c.titleKey());
            UIButton b = new UIButton(title, () -> selectCategory(idx))
                    .style(UIButton.Style.GHOST)
                    .align(Align.START)
                    .selected(i == selected);
            if (font.width(title) > catW - 2 * Spacing.SM - Spacing.ACCENT_BAR_SELECTED) {
                b.tooltip(UITooltip.Content.of(title));
            }
            b.setBounds(Rect.of(cols[0].x(), cols[0].y() + i * (tabH + Spacing.XXS), catW, tabH));
            tabs.add(layer.add(b));
        }
        content = cols[1];
        rows.setBounds(content);
        layer.add(rows);
        buildRows(d);

        // действия снизу
        int aw = Math.min(Spacing.SETTING_ACTION_MAX_W, (actions.w() - 3 * pad) / 4);
        Rect[] a = Flow.row(actions, pad, Flow.Justify.START, aw, aw, Flow.flex(1), aw, aw);
        layer.add(new UIButton(Component.translatable("pwp_core.ui_settings.reset_category"), this::resetCategory)
                        .style(UIButton.Style.DARK)
                        .icon(UIIcon.BACK))
                .setBounds(a[0]);
        layer.add(new UIButton(Component.translatable("pwp_core.ui_settings.reset_all"), this::confirmResetAll)
                        .style(UIButton.Style.GHOST))
                .setBounds(a[1]);
        // вход в экран «Управление» (наши клавиши, белый список)
        layer.add(new UIButton(Component.translatable("pwp_core.ui_settings.controls"), () -> Minecraft.getInstance()
                                .setScreen(new PWPControlsScreen(this)))
                        .style(UIButton.Style.DARK)
                        .icon(UIIcon.KEYBOARD))
                .setBounds(a[3]);
        layer.add(new UIButton(Component.translatable("pwp_core.ui_settings.done"), this::onClose)
                        .style(UIButton.Style.ACCENT)
                        .icon(UIIcon.CHECK))
                .setBounds(a[4]);
    }

    private void selectCategory(int idx) {
        if (idx == selected) return;
        selected = idx;
        for (int i = 0; i < tabs.size(); i++) tabs.get(i).selected(i == selected);
        buildRows(PWPChrome.density(width, height));
    }

    /** Ряды текущей категории в прокрутке: подпись слева, контрол справа. */
    private void buildRows(Fit.Density d) {
        rows.clear();
        if (categories.isEmpty()) return;
        int rowH = d == Fit.Density.COMPACT ? Spacing.SETTING_ROW_H_COMPACT : Spacing.SETTING_ROW_H;
        int gap = Spacing.XXS;
        List<UiSettings.Setting> list = UiSettings.settings(categories.get(selected));
        int total = list.size() * (rowH + gap);
        int w = rows.contentWidth(total);
        int ctrlW = SettingsLayout.controlWidth(
                w,
                Spacing.SM,
                Spacing.SETTING_CONTROL_MIN_W,
                Spacing.SETTING_CONTROL_MAX_W,
                Spacing.SETTING_LABEL_MIN_W);
        for (int i = 0; i < list.size(); i++) {
            UiSettings.Setting s = list.get(i);
            Rect row = Rect.of(0, i * (rowH + gap), w, rowH);
            Rect[] parts = Flow.row(row, Spacing.SM, Flow.Justify.START, Flow.flex(1), ctrlW);
            rows.place(new SettingLabel(s, i % 2 == 1, parts[0].w()), row);
            rows.place(control(s), parts[1]);
        }
    }

    private static UIWidget control(UiSettings.Setting s) {
        if (s instanceof UiSettings.Slider sl) {
            return new UISlider(sl::fraction, f -> sl.setFraction((float) f), () -> sliderText(sl));
        }
        if (s instanceof UiSettings.Toggle t) {
            return new UIToggle(t::get, t::toggle);
        }
        UiSettings.Choice<?> c = (UiSettings.Choice<?>) s;
        return new UIChoice(() -> choiceText(c), c::cycle);
    }

    /** Подпись значения ползунка с единицей из перевода. */
    public static Component sliderText(UiSettings.Slider s) {
        String n = s.format().number(s.get());
        String unit = s.format().unitKey();
        return unit == null ? Component.literal(n) : Component.translatable(unit, n);
    }

    private static <T> Component choiceText(UiSettings.Choice<T> c) {
        return Component.translatable(c.optionLabelKey(c.get()));
    }

    private void resetCategory() {
        if (!categories.isEmpty()) UiSettings.resetCategory(categories.get(selected));
    }

    private void confirmResetAll() {
        layer.showModal(
                new UIModal(
                                Component.translatable("pwp_core.ui_settings.reset_all"),
                                Component.translatable("pwp_core.ui_settings.reset_all.confirm"))
                        .button(Component.translatable("pwp_core.ui.cancel"), UIButton.Style.DARK, null)
                        .button(
                                Component.translatable("pwp_core.ui_settings.reset_all"),
                                UIButton.Style.DANGER,
                                UiSettings::resetAll),
                width,
                height);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderBackground(g);
        var font = PWPTheme.Fonts.display();
        Fit.Density d = PWPChrome.density(width, height);
        PWPChrome.topBar(g, font, width, d, 1F);
        UIDraw.glass(g, panel, 1F, true);
        UIDraw.header(g, font, header, UIIcon.SETTINGS, title, PWPTheme.Text.HEADING, 1F);
        if (categories.isEmpty()) {
            UIDraw.emptyState(
                    g, font, content, UIIcon.INFO, Component.translatable("pwp_core.ui_settings.empty"), null, 1F);
        }
        PWPChrome.footer(
                g,
                font,
                width,
                height,
                d,
                Component.translatable("pwp_core.ui_settings.footer"),
                Component.literal(UiSettingsFile.path().getFileName().toString()));
        layer.render(g, mouseX, mouseY, partialTick, width, height);
    }

    @Override
    public void renderBackground(GuiGraphics g) {
        PWPPauseScreen.renderMenuBackground(g, width, height);
    }

    @Override
    public boolean isPauseScreen() {
        return parent == null || parent.isPauseScreen();
    }

    @Override
    public void onClose() {
        UiSettingsFile.saveIfDirty();
        Minecraft.getInstance().setScreen(parent);
    }

    @Override
    public void removed() {
        // закрытие любым путём (дисконнект, другой экран) — тоже сохраняем
        UiSettingsFile.saveIfDirty();
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (layer.keyPressed(keyCode, scanCode, modifiers)) return true;
        return super.keyPressed(keyCode, scanCode, modifiers);
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
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (layer.mouseDragged(mx, my, button, dx, dy)) return true;
        return super.mouseDragged(mx, my, button, dx, dy);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        if (layer.mouseScrolled(mx, my, delta)) return true;
        return super.mouseScrolled(mx, my, delta);
    }

    /** Подпись ряда: «…» по ширине, акцент — если значение не по умолчанию, тултип с описанием. */
    private static final class SettingLabel extends UIWidget {
        private final UiSettings.Setting setting;
        private final boolean striped;
        private final Component label;
        /** Ширина под текст (ряд целиком подсвечивается, текст — до контрола). */
        private final int textW;

        SettingLabel(UiSettings.Setting setting, boolean striped, int textW) {
            super(0, 0, 0, 0);
            this.setting = setting;
            this.striped = striped;
            this.textW = textW;
            this.label = Component.translatable(setting.labelKey());
            List<Component> lines =
                    setting.tooltipKey() == null ? List.of() : List.of(Component.translatable(setting.tooltipKey()));
            tooltip(new UITooltip.Content(label, lines));
        }

        @Override
        public UITooltip.Content tooltip() {
            // без описания тултип нужен только для обрезанной подписи
            if (setting.tooltipKey() == null && font().width(label) <= textW - Spacing.SM) return null;
            return super.tooltip();
        }

        @Override
        public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
            Rect r = bounds();
            if (striped) UIDraw.rect(g, r, UIDraw.fade(Styles.Setting.ROW_DIVIDER, opacity));
            if (isMouseOver(mouseX, mouseY)) UIDraw.rect(g, r, UIDraw.fade(Styles.Setting.ROW_HOVER, opacity));
            var font = font();
            Component shown = UIDraw.ellipsize(font, label, textW - Spacing.SM);
            int color = setting.isDefault() ? Styles.Setting.LABEL : Styles.Setting.LABEL_CHANGED;
            UIDraw.text(
                    g,
                    font,
                    shown,
                    r.x() + Spacing.XS,
                    r.y() + (r.h() - font.lineHeight) / 2 + 1,
                    UIDraw.fade(color, opacity),
                    false);
        }
    }
}
