package com.pigeostudios.pwp.warfare.client.gui.admin;

import com.pigeostudios.pwp.warfare.client.gui.ui.WDraw;
import com.pigeostudios.pwp.warfare.client.gui.ui.WarfareIcon;
import com.pwp.coreclient.gui.screens.PWPRotatingBackground;
import com.pwp.coreclient.gui.theme.PWPTheme;
import com.pwp.coreclient.gui.theme.PWPTheme.Spacing;
import com.pwp.coreclient.gui.theme.PWPTheme.Styles;
import com.pwp.coreclient.ui.UIBackdrop;
import com.pwp.coreclient.ui.UIButton;
import com.pwp.coreclient.ui.UIDraw;
import com.pwp.coreclient.ui.UIIcon;
import com.pwp.coreclient.ui.UILayer;
import com.pwp.coreclient.ui.UIMath;
import com.pwp.coreclient.ui.UIWidget;
import com.pwp.coreclient.ui.layout.Fit;
import com.pwp.coreclient.ui.layout.Flow;
import com.pwp.coreclient.ui.layout.Rect;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.function.Supplier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * База админ-редакторов: стеклянная панель по центру, форма собирается АВТО-ЛЕЙАУТОМ из
 * строк «подпись — поле» ({@link #text}, {@link #number}, {@link #toggle}, {@link #cycle},
 * {@link #info}), секций ({@link #section}) и произвольных блоков ({@link #custom}); на широком
 * экране — две колонки (разрез по секциям), на узком — одна. Тело прокручивается колесом,
 * кнопки действий ({@link #action}) — в футере справа, статус — слева. Наследник описывает
 * форму в {@link #build()} — никаких координат.
 *
 * <p>Поля — ванильные {@link EditBox} без рамки поверх «колодца» {@link WDraw#field}; позиции
 * пересчитываются каждый кадр (прокрутка), поле вне видимой области скрыто и не кликается.
 */
public abstract class AdminFormScreen extends Screen {
    private static final long STATUS_MS = 3500;

    /** Строка формы: высота, раскладка в прямоугольник (видима ли), рисование. */
    private abstract static class Row {
        int height() {
            return PWPTheme.Spacing.FORM_ROW_H;
        }

        boolean section() {
            return false;
        }

        void place(Rect r, boolean visible) {}

        abstract void render(GuiGraphics g, Rect r, int mx, int my);
    }

    private final List<Row> rows = new ArrayList<>();
    private final List<UIButton> actions = new ArrayList<>();
    private final UILayer layer = new UILayer();
    private final List<UIWidget> rowWidgets = new ArrayList<>();
    private double scrollTarget;
    private Rect panel = Rect.of(0, 0, 0, 0);
    private Rect body = panel;
    private Rect footer = panel;
    private Rect[] columns = new Rect[0];
    private int[] columnStart = new int[0];
    private int labelW;
    private int contentH;
    private Component status;
    private boolean statusError;
    private long statusAt;
    private final long openedAt = System.currentTimeMillis();
    /** Тест-стенд UI: действия с сетью/диском не выполняются (наследник проверяет). */
    protected boolean preview;

    protected AdminFormScreen(Component title) {
        super(title);
    }

    /** Описать форму: section/text/number/toggle/cycle/info/custom/action. */
    protected abstract void build();

    protected WarfareIcon titleIcon() {
        return WarfareIcon.EDIT;
    }

    /** Бейдж в шапке справа (например имя фракции). */
    protected Component badge() {
        return null;
    }

    /** Разрешить две колонки на широком экране. */
    protected boolean twoColumns() {
        return false;
    }

    /** Режим тест-стенда: наследник не шлёт пакеты и не пишет на диск. */
    public AdminFormScreen asPreview() {
        this.preview = true;
        return this;
    }

    // ================================================================ построитель

    protected final void section(Component title) {
        rows.add(new Row() {
            @Override
            int height() {
                return PWPTheme.Spacing.FORM_SECTION_H;
            }

            @Override
            boolean section() {
                return true;
            }

            @Override
            void render(GuiGraphics g, Rect r, int mx, int my) {
                WDraw.section(g, font, r.bottomStrip(font.lineHeight + Spacing.XXS), title, PWPTheme.Text.ACCENT, 1F);
            }
        });
    }

    /** Текстовое поле. */
    protected final EditBox text(Component label, String value, int maxLength) {
        EditBox box = new EditBox(font, 0, 0, 0, 0, label);
        box.setMaxLength(maxLength);
        box.setBordered(false);
        box.setValue(value != null ? value : "");
        addRenderableWidget(box);
        rows.add(new FieldRow(label, box));
        return box;
    }

    /** Числовое поле (только цифры, при {@code negative} — и «-»). */
    protected final EditBox number(Component label, int value, boolean negative) {
        EditBox box = text(label, String.valueOf(value), 9);
        box.setFilter(s -> s.matches(negative ? "-?\\d*" : "\\d*"));
        return box;
    }

    /** Переключатель ВКЛ/ВЫКЛ. */
    protected final UIButton toggle(Component label, boolean value, Consumer<Boolean> onChange) {
        boolean[] state = {value};
        UIButton btn = new UIButton(onOff(value), () -> {}) {
            @Override
            public boolean mouseClicked(double mx, double my, int button) {
                if (!super.mouseClicked(mx, my, button)) return false;
                state[0] = !state[0];
                selected(state[0]).label(onOff(state[0]));
                onChange.accept(state[0]);
                return true;
            }
        }.style(UIButton.Style.DARK).selected(value);
        rows.add(new WidgetRow(label, btn, PWPTheme.Spacing.FORM_TOGGLE_W));
        return btn;
    }

    private static Component onOff(boolean v) {
        return Component.translatable(v ? "gui.pwpwarfare.admin.on" : "gui.pwpwarfare.admin.off");
    }

    /** Выбор из списка по кругу: «‹ значение ›». */
    protected final UIButton cycle(Component label, List<Component> values, int index, IntConsumer onChange) {
        int[] idx = {Math.max(0, Math.min(values.size() - 1, index))};
        UIButton btn = new UIButton(values.isEmpty() ? Component.literal("—") : values.get(idx[0]), () -> {}) {
            @Override
            public boolean mouseClicked(double mx, double my, int button) {
                if (!super.mouseClicked(mx, my, button) || values.isEmpty()) return false;
                idx[0] = (idx[0] + 1) % values.size();
                label(values.get(idx[0]));
                onChange.accept(idx[0]);
                return true;
            }
        }.style(UIButton.Style.DARK);
        rows.add(new WidgetRow(label, btn, -1));
        return btn;
    }

    /** Строка «ключ … значение» только для чтения. */
    protected final void info(Component label, Supplier<Component> value) {
        rows.add(new Row() {
            @Override
            void render(GuiGraphics g, Rect r, int mx, int my) {
                UIDraw.keyValue(g, font, r, label, value.get(), 1F);
            }
        });
    }

    /** Произвольный блок высотой {@code height}; {@code height < 0} — занять остаток (одна колонка). */
    protected final void custom(int height, BiConsumer<GuiGraphics, Rect> renderer) {
        rows.add(new Row() {
            @Override
            int height() {
                return Math.max(0, height);
            }

            @Override
            void render(GuiGraphics g, Rect r, int mx, int my) {
                renderer.accept(g, r);
            }
        });
    }

    /** Виджет кита во всю ширину строки (например кнопка «Добавить»). */
    protected final <T extends UIWidget> T wide(T widget, int height) {
        rows.add(new Row() {
            @Override
            int height() {
                return height;
            }

            @Override
            void place(Rect r, boolean visible) {
                widget.setBounds(r);
                widget.setVisible(visible);
            }

            @Override
            void render(GuiGraphics g, Rect r, int mx, int my) {}
        });
        rowWidgets.add(widget);
        return widget;
    }

    /** Кнопка действия в футере (порядок слева направо; главная — последней). */
    protected final UIButton action(Component label, UIButton.Style style, UIIcon icon, Runnable run) {
        UIButton b = new UIButton(label, run).style(style);
        if (icon != null) b.icon(icon);
        actions.add(b);
        return b;
    }

    /** Статус в футере (после сохранения/ошибки), гаснет через несколько секунд. */
    protected final void status(Component text, boolean error) {
        status = text;
        statusError = error;
        statusAt = System.currentTimeMillis();
    }

    // ================================================================ строки с виджетами

    private final class FieldRow extends Row {
        final Component label;
        final EditBox box;

        FieldRow(Component label, EditBox box) {
            this.label = label;
            this.box = box;
        }

        @Override
        void place(Rect r, boolean visible) {
            Rect f = fieldRect(r);
            box.setX(f.x() + Spacing.XS);
            box.setY(f.y() + (f.h() - font.lineHeight) / 2 + 1);
            box.setWidth(Math.max(1, f.w() - 2 * Spacing.XS));
            box.setHeight(font.lineHeight);
            box.visible = visible;
            box.active = visible;
        }

        @Override
        void render(GuiGraphics g, Rect r, int mx, int my) {
            renderLabel(g, r, label);
            WDraw.field(g, fieldRect(r), box.isFocused(), 1F);
        }
    }

    private final class WidgetRow extends Row {
        final Component label;
        final UIWidget widget;
        final int fixedW;

        WidgetRow(Component label, UIWidget widget, int fixedW) {
            this.label = label;
            this.widget = widget;
            this.fixedW = fixedW;
            rowWidgets.add(widget);
        }

        @Override
        void place(Rect r, boolean visible) {
            Rect f = fieldRect(r);
            if (fixedW > 0) f = f.withWidth(Math.min(f.w(), fixedW));
            widget.setBounds(f);
            widget.setVisible(visible);
        }

        @Override
        void render(GuiGraphics g, Rect r, int mx, int my) {
            renderLabel(g, r, label);
        }
    }

    private Rect labelRect(Rect r) {
        return r.left(labelW);
    }

    private Rect fieldRect(Rect r) {
        return r.rightOf(labelW, Spacing.SM).inset(0, Spacing.XXS);
    }

    private void renderLabel(GuiGraphics g, Rect r, Component label) {
        Rect l = labelRect(r);
        Component t = UIDraw.ellipsize(font, label, l.w());
        UIDraw.text(g, font, t, l.x(), l.y() + (l.h() - font.lineHeight) / 2 + 1, PWPTheme.Text.MUTED, false);
    }

    // ================================================================ раскладка

    @Override
    protected void init() {
        clearWidgets();
        rows.clear();
        actions.clear();
        rowWidgets.clear();
        layer.clear();
        build();
        relayout();
    }

    private void relayout() {
        Fit.Density d = Fit.Density.of(width, height);
        int m = d == Fit.Density.COMPACT ? Spacing.XS : Spacing.LG;
        boolean two = twoColumns() && width - 2 * m >= 2 * Spacing.FORM_COL_MIN_W + Spacing.LG;
        int maxW = two ? Spacing.FORM_MAX_W_TWO : Spacing.FORM_MAX_W;
        int pw = Math.min(width - 2 * m, maxW);
        int headerH = WDraw.headerHeight(height);
        int footerH = PWPTheme.Spacing.BUTTON_HEIGHT + 2 * Spacing.XS;
        int natural = headerH + Spacing.SM + naturalHeight(two) + Spacing.SM + footerH;
        int ph = Math.min(height - 2 * m, natural);
        panel = Rect.of((width - pw) / 2, (height - ph) / 2, pw, ph);
        Rect inner = panel.inset(Spacing.SM, headerH + Spacing.SM, Spacing.SM, Spacing.XS);
        footer = inner.bottomStrip(footerH);
        body = inner.above(footerH, Spacing.XS);

        int colW = two ? (body.w() - Spacing.LG) / 2 : body.w();
        columns = two
                ? new Rect[] {body.withWidth(colW), Rect.of(body.x() + colW + Spacing.LG, body.y(), colW, body.h())}
                : new Rect[] {body};
        columnStart = two ? new int[] {0, splitIndex()} : new int[] {0};
        int maxLabel = 0;
        for (Row r : rows) {
            if (r instanceof FieldRow f) maxLabel = Math.max(maxLabel, font.width(f.label));
            if (r instanceof WidgetRow w) maxLabel = Math.max(maxLabel, font.width(w.label));
        }
        labelW = Math.min(maxLabel + Spacing.SM, (int) (colW * Spacing.FORM_LABEL_FRACTION));

        // футер: кнопки справа налево, ширина по подписи
        int[] widths = new int[actions.size()];
        for (int i = 0; i < widths.length; i++) {
            widths[i] = Math.min(
                    Spacing.FORM_ACTION_MAX_W,
                    Math.max(Spacing.BUTTON_WIDTH * 2 / 3, actions.get(i).preferredWidth()));
        }
        Rect btnRow = footer.inset(0, Spacing.XS, 0, 0);
        Rect[] cells = Flow.row(btnRow, Spacing.XS, Flow.Justify.END, widths);
        for (int i = 0; i < actions.size(); i++) {
            actions.get(i).setBounds(cells[i]);
            layer.add(actions.get(i));
        }
        for (UIWidget w : rowWidgets) layer.add(w);
        contentH = naturalHeight(two);
    }

    /** Индекс строки, с которой начинается вторая колонка: ближайшая к половине высоты секция. */
    private int splitIndex() {
        int total = 0;
        for (Row r : rows) total += r.height();
        int acc = 0, best = rows.size(), bestDiff = Integer.MAX_VALUE;
        for (int i = 0; i < rows.size(); i++) {
            if (rows.get(i).section() && i > 0) {
                int diff = Math.abs(total - 2 * acc);
                if (diff < bestDiff) {
                    bestDiff = diff;
                    best = i;
                }
            }
            acc += rows.get(i).height();
        }
        return best;
    }

    private int naturalHeight(boolean two) {
        if (!two) {
            int h = 0;
            for (Row r : rows) h += r.height();
            return h;
        }
        int split = splitIndex(), a = 0, b = 0;
        for (int i = 0; i < rows.size(); i++) {
            if (i < split) a += rows.get(i).height();
            else b += rows.get(i).height();
        }
        return Math.max(a, b);
    }

    private int scroll() {
        scrollTarget = UIMath.clampScroll(scrollTarget, contentH, body.h());
        return (int) Math.round(scrollTarget);
    }

    // ================================================================ рисование

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        if (Minecraft.getInstance().level != null) UIBackdrop.overlay(g, width, height, UIBackdrop.Shade.NONE);
        else PWPRotatingBackground.render(g, 0, 0, width, height);
        float o = UIMath.stagger(System.currentTimeMillis() - openedAt, 0, 0, 0, PWPTheme.Animations.ENTER);
        WDraw.panel(g, font, panel, WDraw.headerHeight(height), titleIcon(), getTitle(), badge(), Math.max(0.01F, o));

        int off = scroll();
        g.enableScissor(body.x(), body.y(), body.right(), body.bottom());
        for (int c = 0; c < columns.length; c++) {
            int end = c + 1 < columns.length ? columnStart[c + 1] : rows.size();
            int y = columns[c].y() - off;
            for (int i = columnStart[c]; i < end; i++) {
                Row row = rows.get(i);
                Rect r = Rect.of(columns[c].x(), y, columns[c].w(), row.height());
                boolean visible = r.y() >= body.y() && r.bottom() <= body.bottom();
                row.place(r, visible);
                if (r.bottom() > body.y() && r.y() < body.bottom()) row.render(g, r, mx, my);
                y += row.height();
            }
        }
        g.disableScissor();
        if (contentH > body.h()) WDraw.scrollbar(g, body, contentH, off, 1F);
        UIDraw.hline(g, footer.x(), footer.right(), footer.y(), Styles.Glass.HEADER_DIVIDER);
        renderStatus(g);

        super.render(g, mx, my, pt);
        layer.render(g, mx, my, pt, width, height);
    }

    private void renderStatus(GuiGraphics g) {
        if (status == null || System.currentTimeMillis() - statusAt > STATUS_MS) return;
        int buttonsW = 0;
        for (UIButton b : actions) buttonsW += b.width() + Spacing.XS;
        Rect r = footer.inset(0, Spacing.XS, 0, 0).withWidth(Math.max(0, footer.w() - buttonsW - Spacing.SM));
        int color = statusError ? PWPTheme.Text.DANGER : PWPTheme.Colors.SUCCESS_LIGHT;
        UIDraw.icon(
                g,
                statusError ? UIIcon.WARNING : UIIcon.CHECK,
                r.x(),
                r.y() + (r.h() - Spacing.ICON_SIZE) / 2,
                Spacing.ICON_SIZE,
                color);
        Component t = UIDraw.ellipsize(font, status, r.w() - Spacing.ICON_SIZE - Spacing.XS);
        UIDraw.text(
                g,
                font,
                t,
                r.x() + Spacing.ICON_SIZE + Spacing.XS,
                r.y() + (r.h() - font.lineHeight) / 2 + 1,
                color,
                false);
    }

    // ================================================================ ввод

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
        if (body.contains(mx, my) && contentH > body.h()) {
            scrollTarget = UIMath.clampScroll(scrollTarget - delta * Spacing.FORM_ROW_H * 2, contentH, body.h());
            return true;
        }
        return super.mouseScrolled(mx, my, delta);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
