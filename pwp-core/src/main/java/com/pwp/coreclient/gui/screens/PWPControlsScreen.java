package com.pwp.coreclient.gui.screens;

import com.pwp.coreclient.controls.ControlsGroup;
import com.pwp.coreclient.controls.ControlsList;
import com.pwp.coreclient.controls.ControlsRow;
import com.pwp.coreclient.controls.ControlsSearch;
import com.pwp.coreclient.controls.KeyCapture;
import com.pwp.coreclient.controls.KeyInfo;
import com.pwp.coreclient.controls.KeyInput;
import com.pwp.coreclient.gui.controls.ControlsSource;
import com.pwp.coreclient.gui.controls.ControlsWhitelist;
import com.pwp.coreclient.gui.controls.KeyAdapters;
import com.pwp.coreclient.gui.controls.LiveControlsSource;
import com.pwp.coreclient.gui.theme.PWPTheme;
import com.pwp.coreclient.gui.theme.PWPTheme.Colors;
import com.pwp.coreclient.gui.theme.PWPTheme.Spacing;
import com.pwp.coreclient.gui.theme.PWPTheme.Styles;
import com.pwp.coreclient.ui.UIButton;
import com.pwp.coreclient.ui.UIDraw;
import com.pwp.coreclient.ui.UIIcon;
import com.pwp.coreclient.ui.UILayer;
import com.pwp.coreclient.ui.UIModal;
import com.pwp.coreclient.ui.UIScrollArea;
import com.pwp.coreclient.ui.UITooltip;
import com.pwp.coreclient.ui.UIWidget;
import com.pwp.coreclient.ui.layout.Align;
import com.pwp.coreclient.ui.layout.Fit;
import com.pwp.coreclient.ui.layout.Flow;
import com.pwp.coreclient.ui.layout.Rect;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.controls.KeyBindsScreen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Экран «Управление» PWP: только клавиши из белого списка ({@link ControlsWhitelist}) по
 * категориям, у каждой категории раскрывающееся «Дополнительно». Поиск сверху (по названию,
 * категории, клавише, моду), счётчик найденного, подсветка совпадений. Клик по клавише — ожидание
 * ({@link KeyCapture}: клавиатура/мышь/модификаторы Forge, Esc — отмена, Backspace/Del — снять).
 * Конфликт (в т. ч. с клавишей вне белого списка) — красная подсветка и тултип «конфликтует с».
 * Внизу «Все клавиши…» (ванильный экран, возврат сюда), «Сбросить все» (с подтверждением) и
 * «Готово».
 *
 * <p>Данные — через {@link ControlsSource}; снимок и список пересчитываются при открытии, вводе в
 * поиск и после изменения назначения, НЕ в кадре.
 */
@OnlyIn(Dist.CLIENT)
public class PWPControlsScreen extends Screen {
    private static final Logger LOG = LoggerFactory.getLogger(PWPControlsScreen.class);

    private final Screen parent;
    private final ControlsSource source;
    private final UILayer layer = new UILayer();
    private final UIScrollArea list = new UIScrollArea();
    private final KeyCapture capture = new KeyCapture();
    private final Set<String> expanded = new HashSet<>();
    private final Map<String, KeyButton> keyButtons = new HashMap<>();

    private EditBox search;
    private String query = "";
    private ControlsSource.Snapshot snapshot = new ControlsSource.Snapshot(List.of(), Map.of());
    private Map<String, KeyInfo> byId = Map.of();
    private List<ControlsGroup> allGroups = List.of();
    private List<ControlsGroup> shown = List.of();
    private int found;
    private int visibleConflicts;

    private Rect panel = Rect.of(0, 0, 0, 0);
    private Rect header = panel;
    private Rect searchRect = panel;
    private Rect counterRect = panel;
    private Rect listRect = panel;
    private Fit.Density density = Fit.Density.NORMAL;

    /** Настоящие клавиши. */
    public PWPControlsScreen(Screen parent) {
        this(parent, new LiveControlsSource());
    }

    public PWPControlsScreen(Screen parent, ControlsSource source) {
        super(Component.translatable("pwp_core.controls.title"));
        this.parent = parent;
        this.source = source;
    }

    // ================================================================ лейаут

    @Override
    protected void init() {
        layer.clear();
        capture.stop();
        Font font = PWPTheme.Fonts.display();
        density = PWPChrome.density(width, height);
        boolean compact = density == Fit.Density.COMPACT;
        int m = compact ? Spacing.SM : PWPChrome.margin(width);
        int top = PWPChrome.topBarHeight(density);
        int footer = PWPChrome.footerHeight(density);
        int pw = Math.min(Spacing.SETTINGS_MAX_W, width - 2 * m);
        panel = Rect.of((width - pw) / 2, top + m / 2, pw, height - top - footer - m);
        int headerH = compact ? Spacing.PANEL_HEADER_COMPACT : Spacing.PANEL_HEADER;
        int pad = compact ? Spacing.XS : Spacing.SM;
        int actionH = compact ? Spacing.BUTTON_SMALL_HEIGHT : Spacing.BUTTON_HEIGHT;
        int searchH = compact ? Spacing.SETTING_ROW_H_COMPACT : Spacing.BUTTON_SMALL_HEIGHT;
        Rect[] v = Flow.column(panel, 0, Flow.Justify.START, headerH, Flow.flex(1), actionH + 2 * pad);
        header = v[0];
        Rect body = v[1].inset(pad);
        Rect actions = v[2].inset(pad);

        Rect[] b = Flow.column(body, pad, Flow.Justify.START, searchH, Flow.flex(1));
        int counterW = font.width(Component.translatable("pwp_core.controls.found", 999)) + Spacing.SM;
        Rect[] s = Flow.row(b[0], Spacing.SM, Flow.Justify.START, Flow.flex(1), counterW);
        searchRect = s[0];
        counterRect = s[1];
        listRect = b[1];

        Font mcFont = Minecraft.getInstance().font;
        int boxH = mcFont.lineHeight + 1;
        EditBox box = new EditBox(
                mcFont,
                searchRect.x() + Spacing.XS,
                searchRect.y() + (searchRect.h() - mcFont.lineHeight) / 2,
                searchRect.w() - 2 * Spacing.XS,
                boxH,
                Component.translatable("pwp_core.controls.search"));
        box.setBordered(false);
        box.setMaxLength(64);
        box.setTextColor(Styles.Input.TEXT);
        box.setValue(query);
        box.setResponder(this::onQuery);
        box.setFocused(search == null || search.isFocused());
        search = box;

        list.setBounds(listRect);
        layer.add(list);

        int aw = Math.min(Spacing.SETTING_ACTION_MAX_W, (actions.w() - 2 * pad) / 3);
        Rect[] a = Flow.row(actions, pad, Flow.Justify.START, aw, aw, Flow.flex(1), aw);
        UIButton all = layer.add(new UIButton(Component.translatable("pwp_core.controls.all_keys"), this::openVanilla)
                .style(UIButton.Style.DARK)
                .icon(UIIcon.KEYBOARD));
        all.setBounds(a[0]);
        if (source.preview()) {
            all.setEnabled(false);
            all.tooltip(UITooltip.Content.of(Component.translatable("pwp_core.controls.preview_only")));
        }
        layer.add(new UIButton(Component.translatable("pwp_core.controls.reset_all"), this::confirmResetAll)
                        .style(UIButton.Style.GHOST)
                        .icon(UIIcon.BACK))
                .setBounds(a[1]);
        layer.add(new UIButton(Component.translatable("pwp_core.ui_settings.done"), this::onClose)
                        .style(UIButton.Style.ACCENT)
                        .icon(UIIcon.CHECK))
                .setBounds(a[3]);

        reload();
    }

    /** Новый снимок клавиш (открытие, возврат из ванильного экрана, после изменения). */
    private void reload() {
        snapshot = source.snapshot();
        Map<String, KeyInfo> map = new LinkedHashMap<>();
        for (KeyInfo k : snapshot.keys()) map.putIfAbsent(k.id(), k);
        byId = map;
        allGroups = ControlsWhitelist.get()
                .build(map, k -> Component.translatable(k).getString());
        refilter();
    }

    private void onQuery(String value) {
        if (value.equals(query)) return;
        query = value;
        refilter();
    }

    private void refilter() {
        shown = ControlsSearch.filter(allGroups, query);
        found = ControlsList.rowCount(shown);
        visibleConflicts = 0;
        for (ControlsGroup g : allGroups) {
            for (ControlsRow r : g.rows()) if (snapshot.conflicts().containsKey(r.id())) visibleConflicts++;
        }
        rebuildList();
    }

    private int rowH() {
        return density == Fit.Density.COMPACT ? Spacing.SETTING_ROW_H_COMPACT : Spacing.SETTING_ROW_H;
    }

    private int headerRowH(Font font) {
        return font.lineHeight + Spacing.SM;
    }

    private void rebuildList() {
        list.clear();
        keyButtons.clear();
        Font font = Minecraft.getInstance().font;
        boolean searching = !ControlsSearch.tokens(query).isEmpty();
        List<ControlsList.Item> items = ControlsList.flatten(shown, searching, expanded);
        int rowH = rowH();
        int gap = Spacing.XXS;
        int total = 0;
        for (int i = 0; i < items.size(); i++) total += height(items.get(i), i, font, rowH) + gap;
        int w = list.contentWidth(total);
        int keyW = Math.max(Spacing.SETTING_CONTROL_MIN_W, Math.min(Spacing.SETTING_CONTROL_MAX_W, w / 3));
        int y = 0;
        int stripe = 0;
        for (int i = 0; i < items.size(); i++) {
            ControlsList.Item item = items.get(i);
            int h = height(item, i, font, rowH);
            Rect r = Rect.of(0, y, w, h);
            if (item instanceof ControlsList.Header hd) {
                list.place(new GroupHeader(hd.group().title(), hd.count()), r);
                stripe = 0;
            } else if (item instanceof ControlsList.AdvancedToggle t) {
                String arrow = t.expanded() ? "▾ " : "▸ ";
                UIButton tb = new UIButton(
                                Component.literal(arrow)
                                        .append(Component.translatable("pwp_core.controls.advanced"))
                                        .append(" (" + t.count() + ")"),
                                () -> toggleAdvanced(t.groupId()))
                        .style(UIButton.Style.GHOST)
                        .align(Align.START);
                list.place(tb, r);
            } else if (item instanceof ControlsList.Row row) {
                placeRow(row.row(), r, keyW, stripe++ % 2 == 1);
            }
            y += h + gap;
        }
    }

    private int height(ControlsList.Item item, int index, Font font, int rowH) {
        if (item instanceof ControlsList.Header) return headerRowH(font) + (index == 0 ? 0 : Spacing.SM);
        return rowH;
    }

    private void placeRow(ControlsRow row, Rect r, int keyW, boolean striped) {
        KeyInfo info = byId.get(row.id());
        List<String> with = snapshot.conflicts().getOrDefault(row.id(), List.of());
        boolean conflict = !with.isEmpty();
        Rect[] parts = Flow.row(r, Spacing.XS, Flow.Justify.START, Flow.flex(1), keyW, r.h());
        List<Component> conflictLines = conflictLines(with);

        list.place(new RowLabel(row, query, conflict, striped, parts[0].w(), conflictLines), r);
        KeyButton kb = new KeyButton(row, conflict, () -> startCapture(row.id()));
        if (conflict)
            kb.tooltip(new UITooltip.Content(Component.translatable("pwp_core.controls.conflict"), conflictLines));
        keyButtons.put(row.id(), kb);
        list.place(kb, parts[1]);
        ResetButton rb = new ResetButton(() -> resetOne(row.id()));
        rb.setEnabled(info != null && !info.isDefault());
        rb.tooltip(UITooltip.Content.of(Component.translatable("pwp_core.controls.reset")));
        list.place(rb, parts[2]);
    }

    /** «Название · мод» каждой конфликтующей клавиши (и невидимой на экране). */
    private List<Component> conflictLines(List<String> ids) {
        List<Component> out = new ArrayList<>(ids.size());
        for (String id : ids) {
            KeyInfo k = byId.get(id);
            String text = k == null ? id : (k.mod().isEmpty() ? k.label() : k.label() + " · " + k.mod());
            out.add(Component.literal("• " + text));
        }
        return out;
    }

    private void toggleAdvanced(String groupId) {
        if (!expanded.remove(groupId)) expanded.add(groupId);
        rebuildList();
    }

    // ================================================================ действия

    private void startCapture(String id) {
        capture.start(id);
        if (search != null) search.setFocused(false);
        updateWaitingLabel();
    }

    private void updateWaitingLabel() {
        for (Map.Entry<String, KeyButton> e : keyButtons.entrySet()) {
            boolean waiting = capture.active() && e.getKey().equals(capture.target());
            e.getValue().waiting(waiting ? waitingLabel() : null);
        }
    }

    private Component waitingLabel() {
        KeyInput pending = capture.pendingModifier();
        String mid = pending == null ? "…" : KeyAdapters.label(pending).getString() + " + …";
        return Component.literal("> " + mid + " <");
    }

    /** Итог ожидания: применить и пересчитать снимок. */
    private void apply(String id, KeyCapture.Result r) {
        if (r instanceof KeyCapture.Waiting) {
            updateWaitingLabel();
            return;
        }
        if (r instanceof KeyCapture.Bind bind) source.bind(id, bind.key());
        else if (r instanceof KeyCapture.Unbind) source.unbind(id);
        reload();
    }

    private void resetOne(String id) {
        source.reset(id);
        reload();
    }

    private void confirmResetAll() {
        layer.showModal(
                new UIModal(
                                Component.translatable("pwp_core.controls.reset_all"),
                                Component.translatable("pwp_core.controls.reset_all.confirm"))
                        .button(Component.translatable("pwp_core.ui.cancel"), UIButton.Style.DARK, null)
                        .button(
                                Component.translatable("pwp_core.controls.reset_all"),
                                UIButton.Style.DANGER,
                                this::resetAll),
                width,
                height);
    }

    /** Сбросить все клавиши ЭТОГО экрана (белый список); остальные — через «Все клавиши…». */
    private void resetAll() {
        List<String> ids = new ArrayList<>();
        for (ControlsGroup g : allGroups) for (ControlsRow r : g.rows()) ids.add(r.id());
        source.resetAll(ids);
        reload();
    }

    private void openVanilla() {
        if (source.preview()) {
            LOG.info("[PWPUI] controls: «Все клавиши…» (превью — без действия)");
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        mc.setScreen(new KeyBindsScreen(this, mc.options));
    }

    // ================================================================ отрисовка

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderBackground(g);
        Font font = PWPTheme.Fonts.display();
        PWPChrome.topBar(g, font, width, density, 1F);
        UIDraw.glass(g, panel, 1F, true);
        UIDraw.header(g, font, header, UIIcon.KEYBOARD, title, PWPTheme.Text.HEADING, 1F);
        renderSearch(g, mouseX, mouseY, partialTick);

        if (allGroups.isEmpty()) {
            UIDraw.emptyState(
                    g,
                    font,
                    listRect,
                    UIIcon.INFO,
                    Component.translatable("pwp_core.controls.empty"),
                    Component.translatable("pwp_core.controls.empty.hint"),
                    1F);
        } else if (shown.isEmpty()) {
            UIDraw.emptyState(
                    g,
                    font,
                    listRect,
                    UIIcon.INFO,
                    Component.translatable("pwp_core.controls.not_found"),
                    Component.translatable("pwp_core.controls.not_found.hint"),
                    1F);
        }
        Component left = capture.active()
                ? Component.translatable("pwp_core.controls.footer.waiting")
                : Component.translatable("pwp_core.controls.footer");
        Component right = visibleConflicts > 0
                ? Component.translatable("pwp_core.controls.conflicts", visibleConflicts)
                : Component.empty();
        PWPChrome.footer(g, font, width, height, density, left, right);
        layer.render(g, mouseX, mouseY, partialTick, width, height);
    }

    private void renderSearch(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        Font font = Minecraft.getInstance().font;
        boolean focused = search.isFocused();
        UIDraw.rect(g, searchRect, focused ? Styles.Input.BG_FOCUS : Styles.Input.BG);
        UIDraw.outline(g, searchRect, focused ? Styles.Input.BORDER_FOCUS : Styles.Input.BORDER);
        if (query.isEmpty() && !focused) {
            UIDraw.text(
                    g,
                    font,
                    UIDraw.ellipsize(font, Component.translatable("pwp_core.controls.search"), search.getWidth()),
                    search.getX(),
                    search.getY(),
                    Styles.Input.PLACEHOLDER,
                    false);
        }
        search.render(g, mouseX, mouseY, partialTick);
        Component count = ControlsSearch.tokens(query).isEmpty()
                ? Component.translatable("pwp_core.controls.total", found)
                : Component.translatable("pwp_core.controls.found", found);
        int color = found == 0 ? PWPTheme.Text.DANGER : PWPTheme.Text.MUTED;
        Component c = UIDraw.ellipsize(font, count, counterRect.w());
        UIDraw.text(
                g,
                font,
                c,
                counterRect.right() - font.width(c),
                counterRect.y() + (counterRect.h() - font.lineHeight) / 2 + 1,
                color,
                false);
    }

    @Override
    public void renderBackground(GuiGraphics g) {
        PWPPauseScreen.renderMenuBackground(g, width, height);
    }

    @Override
    public void tick() {
        if (search != null) search.tick();
    }

    @Override
    public boolean isPauseScreen() {
        return parent == null || parent.isPauseScreen();
    }

    @Override
    public void onClose() {
        capture.stop();
        Minecraft.getInstance().setScreen(parent);
    }

    @Override
    public void removed() {
        capture.stop();
    }

    // ================================================================ ввод

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (layer.hasModal()) return layer.keyPressed(keyCode, scanCode, modifiers);
        if (capture.active()) {
            String id = capture.target();
            KeyInput key = KeyAdapters.fromEvent(keyCode, scanCode);
            apply(id, capture.press(key, KeyAdapters.isModifierKey(key), KeyAdapters.heldModifier()));
            return true;
        }
        // Esc — ДО поля поиска: EditBox.keyPressed на Esc вернёт false и экран бы не закрылся
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            search.setFocused(false);
            onClose();
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_F && Screen.hasControlDown()) {
            search.setFocused(true);
            return true;
        }
        if (search.isFocused()) {
            if (search.keyPressed(keyCode, scanCode, modifiers)) return true;
            if (search.canConsumeInput()) return true;
        }
        if (layer.keyPressed(keyCode, scanCode, modifiers)) return true;
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        if (capture.active()) {
            String id = capture.target();
            apply(id, capture.release(KeyAdapters.fromEvent(keyCode, scanCode)));
            return true;
        }
        return super.keyReleased(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char c, int modifiers) {
        if (capture.active() || layer.hasModal()) return true;
        if (search.isFocused()) return search.charTyped(c, modifiers);
        return super.charTyped(c, modifiers);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (layer.hasModal()) return layer.mouseClicked(mx, my, button);
        if (capture.active()) {
            String id = capture.target();
            apply(id, capture.mouse(button, KeyAdapters.heldModifier()));
            return true;
        }
        boolean inSearch = searchRect.contains(mx, my);
        search.setFocused(inSearch);
        if (inSearch) {
            search.mouseClicked(mx, my, button);
            return true;
        }
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
        if (capture.active()) return true;
        if (layer.mouseScrolled(mx, my, delta)) return true;
        return super.mouseScrolled(mx, my, delta);
    }

    // ================================================================ виджеты

    /** Заголовок категории: акцентная полоса + название, справа число клавиш. */
    private static final class GroupHeader extends UIWidget {
        private final Component title;
        private final Component count;

        GroupHeader(String title, int count) {
            super(0, 0, 0, 0);
            this.title = Component.literal(title);
            this.count = Component.literal(String.valueOf(count));
        }

        @Override
        public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
            Font font = font();
            Rect r = bounds();
            int ty = r.bottom() - font.lineHeight - Spacing.XXS;
            int cw = font.width(count);
            UIDraw.sectionTitle(
                    g,
                    font,
                    Rect.of(r.x(), ty, r.w() - cw - Spacing.SM, font.lineHeight),
                    title,
                    PWPTheme.Text.HEADING,
                    opacity);
            UIDraw.text(
                    g,
                    font,
                    count,
                    r.right() - cw - Spacing.XS,
                    ty,
                    UIDraw.fade(PWPTheme.Text.CAPTION, opacity),
                    false);
            UIDraw.hline(g, r.x(), r.right(), r.bottom() - 1, UIDraw.fade(Styles.Setting.ROW_DIVIDER, opacity));
        }
    }

    /** Подпись строки: название с подсветкой совпадений поиска + мод мелким приглушённым текстом. */
    private static final class RowLabel extends UIWidget {
        private final boolean conflict;
        private final boolean striped;
        private final boolean changed;
        private final String shownLabel;
        private final List<int[]> ranges;
        private final Component mod;
        /** Ширина под текст: ряд подсвечивается целиком, текст — до кнопок справа. */
        private final int textW;

        RowLabel(
                ControlsRow row,
                String query,
                boolean conflict,
                boolean striped,
                int textW,
                List<Component> conflictLines) {
            super(0, 0, 0, 0);
            this.conflict = conflict;
            this.striped = striped;
            this.changed = !row.isDefault();
            this.textW = textW;
            Font font = font();
            int avail = Math.max(0, textW - 2 * Spacing.XS);
            int modW = row.mod().isEmpty() ? 0 : Math.min(font.width(row.mod()), avail / 3);
            int labelW = avail - (modW > 0 ? modW + Spacing.SM : 0);
            Component label = UIDraw.ellipsize(font, Component.literal(row.label()), labelW);
            this.shownLabel = label.getString();
            this.ranges = ControlsSearch.highlight(shownLabel, query);
            this.mod = modW > 0 ? UIDraw.ellipsize(font, Component.literal(row.mod()), modW) : Component.empty();

            List<Component> lines = new ArrayList<>();
            if (row.hintKey() != null) lines.add(Component.translatable(row.hintKey()));
            if (!row.mod().isEmpty()) lines.add(Component.translatable("pwp_core.controls.mod", row.mod()));
            if (conflict) {
                lines.add(Component.translatable("pwp_core.controls.conflict"));
                lines.addAll(conflictLines);
            }
            tooltip(new UITooltip.Content(Component.literal(row.label()), lines));
        }

        @Override
        public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
            Rect r = bounds();
            if (conflict) UIDraw.rect(g, r, UIDraw.fade(Colors.DANGER_DIM, opacity));
            else if (striped) UIDraw.rect(g, r, UIDraw.fade(Styles.Setting.ROW_DIVIDER, opacity));
            if (isMouseOver(mouseX, mouseY)) UIDraw.rect(g, r, UIDraw.fade(Styles.Setting.ROW_HOVER, opacity));
            if (conflict)
                UIDraw.accentBar(g, r.x(), r.y(), r.h(), Spacing.ACCENT_BAR, UIDraw.fade(Colors.DANGER, opacity));
            Font font = font();
            int ty = r.y() + (r.h() - font.lineHeight) / 2 + 1;
            int base = conflict ? PWPTheme.Text.DANGER : changed ? Styles.Setting.LABEL_CHANGED : Styles.Setting.LABEL;
            int x = r.x() + Spacing.XS + (conflict ? Spacing.ACCENT_BAR : 0);
            int pos = 0;
            for (int[] span : ranges) {
                int s = Math.min(span[0], shownLabel.length());
                int e = Math.min(span[1], shownLabel.length());
                if (s > pos) x = segment(g, font, shownLabel.substring(pos, s), x, ty, base, false);
                if (e > s) x = segment(g, font, shownLabel.substring(s, e), x, ty, PWPTheme.Text.ACCENT, true);
                pos = Math.max(pos, e);
            }
            if (pos < shownLabel.length()) segment(g, font, shownLabel.substring(pos), x, ty, base, false);
            if (!mod.getString().isEmpty()) {
                int mw = font.width(mod);
                UIDraw.text(
                        g,
                        font,
                        mod,
                        r.x() + textW - mw - Spacing.XS,
                        ty,
                        UIDraw.fade(PWPTheme.Text.CAPTION, opacity),
                        false);
            }
        }

        private int segment(GuiGraphics g, Font font, String text, int x, int y, int color, boolean mark) {
            Component c = Component.literal(text);
            int w = font.width(c);
            if (mark)
                UIDraw.rect(g, Rect.of(x, y - 1, w, font.lineHeight), UIDraw.fade(Styles.Control.BG_SELECTED, opacity));
            UIDraw.text(g, font, c, x, y, UIDraw.fade(color, opacity), false);
            return x + w;
        }
    }

    /** Кнопка назначения: имя клавиши; ожидание — «> … <»; конфликт — красная рамка и текст. */
    private static final class KeyButton extends UIButton {
        private final boolean conflict;
        private final Component normal;

        KeyButton(ControlsRow row, boolean conflict, Runnable onPress) {
            super(keyText(row, conflict), onPress);
            this.conflict = conflict;
            this.normal = this.label;
            style(Style.DARK);
        }

        private static Component keyText(ControlsRow row, boolean conflict) {
            String text = row.keyLabel().isEmpty() ? "—" : row.keyLabel();
            return conflict
                    ? Component.literal(text).withStyle(s -> s.withColor(PWPTheme.Text.DANGER & 0xFFFFFF))
                    : Component.literal(text);
        }

        /** {@code null} — обычное состояние. */
        void waiting(Component waitingLabel) {
            selected(waitingLabel != null);
            label(
                    waitingLabel == null
                            ? normal
                            : waitingLabel.copy().withStyle(s -> s.withColor(PWPTheme.Text.ACCENT & 0xFFFFFF)));
        }

        @Override
        public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
            super.render(g, mouseX, mouseY, partialTick);
            if (conflict && !isSelected()) UIDraw.outline(g, bounds(), UIDraw.fade(Colors.DANGER, opacity));
        }
    }

    /** Квадратная кнопка «к умолчанию» (только иконка; выключена, если уже по умолчанию). */
    private static final class ResetButton extends UIButton {
        ResetButton(Runnable onPress) {
            super(Component.empty(), onPress);
            style(Style.GHOST);
        }

        @Override
        protected void renderContent(GuiGraphics g, int textColor) {
            int size = Math.min(Spacing.ICON_SIZE, Math.max(0, height - 2 * Spacing.XXS));
            if (size <= 0) return;
            UIDraw.icon(
                    g,
                    UIIcon.BACK,
                    x + (width - size) / 2,
                    y + (height - size) / 2 + contentShift,
                    size,
                    contentIconColor);
        }
    }
}
