package com.pwp.coreclient.gui.screens;

import com.pwp.coreclient.gui.theme.PWPTheme;
import com.pwp.coreclient.gui.theme.PWPTheme.Animations;
import com.pwp.coreclient.gui.theme.PWPTheme.Colors;
import com.pwp.coreclient.gui.theme.PWPTheme.Spacing;
import com.pwp.coreclient.gui.theme.PWPTheme.Styles;
import com.pwp.coreclient.ui.UIAnim;
import com.pwp.coreclient.ui.UIBackdrop;
import com.pwp.coreclient.ui.UIButton;
import com.pwp.coreclient.ui.UIDraw;
import com.pwp.coreclient.ui.UIIcon;
import com.pwp.coreclient.ui.UILayer;
import com.pwp.coreclient.ui.UIMath;
import com.pwp.coreclient.ui.layout.Align;
import com.pwp.coreclient.ui.layout.Fit;
import com.pwp.coreclient.ui.layout.Flow;
import com.pwp.coreclient.ui.layout.Grid;
import com.pwp.coreclient.ui.layout.Rect;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.IntPredicate;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Универсальный экран выбора карточкой (наследник в pwp-warfare — TeamSelectionScreen).
 * API прежний: конструкторы, {@link SelectionItem} и колбэк рисования содержимого карточки
 * (карточка фиксированного размера cardW x cardH — содержимое наследника рассчитано на него).
 *
 * <p>Вид: в игре — мир за затемнением, вне мира — фон меню; заголовок с акцентной полосой и
 * инструкция с переносом; карточки — стеклянные с состояниями hover/selected/disabled
 * (плавные переходы, акцентная полоса), сетка адаптивная ({@link Grid#columnsFor}), по центру.
 * Кнопка «Подтвердить» (не для instantSelect) активна после выбора; автовыбор по таймеру.
 */
public class PWPSelectionScreen extends Screen {

    private final String title;
    private final String instruction;
    private final List<String> items;
    private final BiConsumer<GuiGraphics, SelectionItem> cardRenderer;
    private final Consumer<Integer> onConfirm;
    private final int autoSelectSeconds;

    private final int cardW;
    private final int cardH;
    private final int gap;

    private final IntPredicate disabledPredicate;

    private final boolean instantSelect;

    private int selectedIndex = -1;
    private final long openTime;
    private final UILayer layer = new UILayer();
    private UIButton confirmBtn;
    private final List<UIAnim> hoverAnims = new ArrayList<>();
    private final List<UIAnim> selectAnims = new ArrayList<>();

    // раскладка
    private Rect titleR = Rect.of(0, 0, 0, 0);
    private Rect instructionR = titleR;
    private Rect timerR = titleR;
    private Rect[] cells = new Rect[0];

    public static class SelectionItem {
        public final int index;
        public final String label;
        public final boolean selected;
        public final boolean hovered;
        public final boolean disabled;
        public final int x, y, w, h;

        public SelectionItem(
                int index,
                String label,
                boolean selected,
                boolean hovered,
                boolean disabled,
                int x,
                int y,
                int w,
                int h) {
            this.index = index;
            this.label = label;
            this.selected = selected;
            this.hovered = hovered;
            this.disabled = disabled;
            this.x = x;
            this.y = y;
            this.w = w;
            this.h = h;
        }
    }

    public PWPSelectionScreen(
            String title,
            String instruction,
            List<String> items,
            BiConsumer<GuiGraphics, SelectionItem> cardRenderer,
            Consumer<Integer> onConfirm) {
        this(title, instruction, items, cardRenderer, onConfirm, -1);
    }

    public PWPSelectionScreen(
            String title,
            String instruction,
            List<String> items,
            BiConsumer<GuiGraphics, SelectionItem> cardRenderer,
            Consumer<Integer> onConfirm,
            int autoSelectSeconds) {
        this(
                title,
                instruction,
                items,
                cardRenderer,
                onConfirm,
                autoSelectSeconds,
                130,
                50,
                PWPTheme.Spacing.GRID_GAP,
                null,
                false);
    }

    public PWPSelectionScreen(
            String title,
            String instruction,
            List<String> items,
            BiConsumer<GuiGraphics, SelectionItem> cardRenderer,
            Consumer<Integer> onConfirm,
            int autoSelectSeconds,
            int cardW,
            int cardH,
            int gap,
            IntPredicate disabledPredicate,
            boolean instantSelect) {
        super(Component.literal(title));
        this.title = title;
        this.instruction = instruction;
        this.items = items;
        this.cardRenderer = cardRenderer;
        this.onConfirm = onConfirm;
        this.autoSelectSeconds = autoSelectSeconds;
        this.cardW = cardW;
        this.cardH = cardH;
        this.gap = gap;
        this.disabledPredicate = disabledPredicate;
        this.instantSelect = instantSelect;
        this.openTime = System.currentTimeMillis();
        for (int i = 0; i < items.size(); i++) {
            hoverAnims.add(new UIAnim(0F, Animations.DURATION_FAST, UIAnim.EASE_OUT_CUBIC));
            selectAnims.add(new UIAnim(0F, Animations.DURATION_NORMAL, UIAnim.EASE_OUT_CUBIC));
        }
    }

    @Override
    protected void init() {
        super.init();
        layer.clear();
        var font = PWPTheme.Fonts.display();
        Fit.Density d = PWPChrome.density(width, height);
        int m = PWPChrome.margin(width);
        int vm = d == Fit.Density.COMPACT ? Spacing.SM : m;
        Rect area = Rect.of(m, vm, width - 2 * m, height - 2 * vm);
        int lh = font.lineHeight;
        int instrH = instruction == null || instruction.isEmpty()
                ? 0
                : UIDraw.linesHeight(
                        font,
                        UIDraw.wrap(font, Component.literal(instruction), area.w(), 2)
                                .size());
        int btnH = instantSelect ? 0 : PWPLoadingView.buttonHeight(d);
        int timerH = autoSelectSeconds > 0 && !instantSelect ? lh : 0;

        Rect[] col = Flow.column(
                area,
                0,
                Flow.Justify.START,
                lh,
                instrH > 0 ? Spacing.XS : 0,
                instrH,
                Spacing.MD,
                Flow.flex(1),
                timerH > 0 ? Spacing.SM : 0,
                timerH,
                btnH > 0 ? Spacing.SM : 0,
                btnH);
        titleR = col[0];
        instructionR = col[2];
        Rect body = col[4];
        timerR = col[6];

        // сетка карточек фиксированного размера (наследник рисует содержимое под него)
        int n = items.size();
        int w = Math.min(cardW, body.w());
        int cols = Math.min(Math.max(1, n), Grid.columnsFor(body.w(), w, gap, Spacing.SELECTION_MAX_COLS));
        int rows = n == 0 ? 0 : (n + cols - 1) / cols;
        int gridW = cols * w + (cols - 1) * gap;
        int gridH = Grid.height(n, cols, cardH, gap);
        Rect gridR = body.align(gridW, gridH, Align.CENTER, Align.CENTER);
        // если не влезло по высоте — от верха области, без наложения на заголовок
        if (gridH > body.h()) gridR = Rect.of(gridR.x(), body.y(), gridW, gridH);
        cells = new Rect[n];
        for (int i = 0; i < n; i++) {
            int c = i % cols, r = i / cols;
            cells[i] = Rect.of(gridR.x() + c * (w + gap), gridR.y() + r * (cardH + gap), w, cardH);
        }

        if (!instantSelect) {
            confirmBtn = new UIButton(Component.translatable("pwp_core.ui.confirm"), () -> {
                        if (selectedIndex >= 0 && onConfirm != null) onConfirm.accept(selectedIndex);
                    })
                    .style(UIButton.Style.ACCENT)
                    .icon(UIIcon.CHECK);
            int bw = Math.min(col[8].w(), Math.max(Spacing.BUTTON_WIDTH + Spacing.XXL, confirmBtn.preferredWidth()));
            confirmBtn.setBounds(col[8].align(bw, btnH, Align.CENTER, Align.STRETCH));
            confirmBtn.setEnabled(selectedIndex >= 0);
            layer.add(confirmBtn);
        }
    }

    private boolean isDisabled(int i) {
        return disabledPredicate != null && disabledPredicate.test(i);
    }

    @Override
    public void renderBackground(GuiGraphics gui) {
        if (Minecraft.getInstance().level != null) {
            // в игре: мир за атмосферным затемнением
            UIBackdrop.overlay(gui, width, height, UIBackdrop.Shade.NONE);
        } else {
            PWPRotatingBackground.render(gui, 0, 0, width, height);
        }
    }

    @Override
    public void render(GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
        renderBackground(gui);
        var font = PWPTheme.Fonts.display();
        long elapsed = System.currentTimeMillis() - openTime;
        float fade = UIMath.stagger(elapsed, 0, 0, 0, Animations.ENTER);

        // заголовок по центру с подчёркиванием
        Component t = UIDraw.ellipsize(font, Component.literal(title), titleR.w());
        int tw = font.width(t);
        UIDraw.text(
                gui, font, t, titleR.centerX() - tw / 2, titleR.y(), UIDraw.fade(PWPTheme.Text.HEADING, fade), true);
        UIDraw.accentBarH(
                gui,
                titleR.centerX() - tw / 2,
                titleR.bottom() + 1,
                tw,
                Spacing.ACCENT_BAR,
                UIDraw.fade(Styles.Control.ACCENT_BAR, fade));
        if (instructionR.h() > 0) {
            UIDraw.lines(
                    gui,
                    font,
                    UIDraw.wrap(font, Component.literal(instruction), instructionR.w(), 2),
                    instructionR,
                    true,
                    UIDraw.fade(PWPTheme.Text.MUTED, fade),
                    false);
        }

        for (int i = 0; i < cells.length; i++) {
            Rect r = cells[i];
            boolean disabled = isDisabled(i);
            boolean selected = i == selectedIndex;
            boolean hovered = !disabled && r.contains(mouseX, mouseY);
            float h = hoverAnims.get(i).to(hovered ? 1F : 0F).value();
            float s = selectAnims.get(i).to(selected ? 1F : 0F).value();
            float o =
                    UIMath.stagger(elapsed, i, Animations.STAGGER_NORMAL, Animations.STAGGER_NORMAL, Animations.ENTER);
            renderCardFrame(gui, r, h, s, disabled, o);
            if (o >= 1F) {
                cardRenderer.accept(
                        gui,
                        new SelectionItem(i, items.get(i), selected, hovered, disabled, r.x(), r.y(), r.w(), r.h()));
            }
        }

        if (!instantSelect && autoSelectSeconds > 0 && selectedIndex < 0) {
            int remaining = Math.max(0, autoSelectSeconds - (int) (elapsed / 1000));
            if (remaining <= 0 && !items.isEmpty()) select(0);
            Component timer = Component.translatable("pwp_core.selection.auto", remaining);
            Component shown = UIDraw.ellipsize(font, timer, timerR.w());
            UIDraw.text(
                    gui,
                    font,
                    shown,
                    timerR.centerX() - font.width(shown) / 2,
                    timerR.y(),
                    UIDraw.fade(PWPTheme.Text.CAPTION, fade),
                    false);
        }
        layer.render(gui, mouseX, mouseY, partialTick, width, height);
    }

    /** Стеклянная карточка с состояниями; содержимое рисует колбэк наследника поверх. */
    private static void renderCardFrame(GuiGraphics g, Rect r, float h, float s, boolean disabled, float o) {
        if (o <= 0F) return;
        int bg = Colors.lerp(Styles.Control.BG, Styles.Control.BG_HOVER, h);
        bg = Colors.lerp(bg, Styles.Control.BG_SELECTED, s);
        if (disabled) bg = Styles.Control.BG_DISABLED;
        int border = Colors.lerp(Styles.Control.BORDER, Styles.Control.BORDER_HOVER, h);
        border = Colors.lerp(border, Styles.Control.BORDER_SELECTED, s);
        if (disabled) border = Styles.Control.BORDER_DISABLED;
        UIDraw.shadow(g, r, o * 0.6F);
        UIDraw.rect(g, r, UIDraw.fade(bg, o));
        UIDraw.gradientV(g, r, UIDraw.fade(Styles.Control.SHEEN_TOP, o), UIDraw.fade(Styles.Control.SHEEN_BOTTOM, o));
        UIDraw.outline(g, r, UIDraw.fade(border, o));
        int bar = Math.round(Math.max(h * Spacing.ACCENT_BAR, s * Spacing.ACCENT_BAR_SELECTED));
        if (bar > 0 && !disabled)
            UIDraw.accentBar(g, r.x(), r.y(), r.h(), bar, UIDraw.fade(Styles.Control.ACCENT_BAR, o));
    }

    private void select(int i) {
        selectedIndex = i;
        if (confirmBtn != null) confirmBtn.setEnabled(true);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (layer.mouseClicked(mouseX, mouseY, button)) return true;
        if (button == 0) {
            for (int i = 0; i < cells.length; i++) {
                if (isDisabled(i) || !cells[i].contains(mouseX, mouseY)) continue;
                if (instantSelect) {
                    if (onConfirm != null) onConfirm.accept(i);
                } else {
                    select(i);
                }
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        layer.mouseReleased(mouseX, mouseY, button);
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
