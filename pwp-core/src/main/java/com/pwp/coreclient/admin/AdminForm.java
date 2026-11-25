package com.pwp.coreclient.admin;

import com.pwp.coreclient.gui.theme.PWPTheme;
import com.pwp.coreclient.gui.theme.PWPTheme.Spacing;
import com.pwp.coreclient.gui.theme.PWPTheme.Styles;
import com.pwp.coreclient.ui.UIButton;
import com.pwp.coreclient.ui.UIDraw;
import com.pwp.coreclient.ui.UIScrollArea;
import com.pwp.coreclient.ui.UIWidget;
import com.pwp.coreclient.ui.layout.Flow;
import com.pwp.coreclient.ui.layout.Rect;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.IntSupplier;
import java.util.function.Supplier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * Построитель формы вкладки админ-панели: ряды фиксированной высоты в прокрутке (заголовок секции,
 * «ключ — значение», строка текста, подпись + контрол [+ «Применить»], ряд кнопок). Колонки считает
 * {@link Flow}, размеры/цвета — {@link PWPTheme}. Тексты-значения — поставщики, читаются покадрово.
 */
@OnlyIn(Dist.CLIENT)
public final class AdminForm {
    private record Row(int height, BiConsumer<UIScrollArea, Rect> place) {}

    private final List<Row> rows = new ArrayList<>();
    private final int rowH = Spacing.SETTING_ROW_H;

    /** Заголовок секции. */
    public AdminForm section(Component title) {
        rows.add(new Row(
                rowH,
                (area, r) -> area.place(
                        new Line((g, b) -> UIDraw.sectionTitle(g, font(), b, title, PWPTheme.Text.HEADING, 1F)), r)));
        return this;
    }

    /** «Ключ — значение» (значение живое). */
    public AdminForm value(Component key, Supplier<Component> value) {
        rows.add(new Row(
                rowH,
                (area, r) -> area.place(new Line((g, b) -> UIDraw.keyValue(g, font(), b, key, value.get(), 1F)), r)));
        return this;
    }

    /** Строка текста (обрезается «…»), цвет — поставщик (ошибка/предупреждение). */
    public AdminForm text(Supplier<Component> text, IntSupplier color) {
        rows.add(new Row(
                rowH, (area, r) -> area.place(new Line((g, b) -> label(g, b, text.get(), color.getAsInt())), r)));
        return this;
    }

    /** Подпись слева, контрол справа. */
    public AdminForm control(Component label, UIWidget control) {
        return control(label, control, null);
    }

    /** Подпись слева, контрол и (необязательно) кнопка «Применить» справа. */
    public AdminForm control(Component label, UIWidget control, UIButton apply) {
        rows.add(new Row(rowH, (area, r) -> {
            int ctrlW = Math.max(Spacing.SETTING_CONTROL_MIN_W, Math.min(Spacing.SETTING_CONTROL_MAX_W, r.w() * 2 / 5));
            int applyW = Math.min(Spacing.BUTTON_WIDTH, r.w() / 5);
            Rect[] p = apply == null
                    ? Flow.row(r, Spacing.SM, Flow.Justify.START, Flow.flex(1), ctrlW)
                    : Flow.row(r, Spacing.SM, Flow.Justify.START, Flow.flex(1), ctrlW, applyW);
            area.place(new Line((g, b) -> label(g, b, label, Styles.Setting.LABEL)), p[0]);
            area.place(control, p[1]);
            if (apply != null) area.place(apply, buttonBox(p[2]));
        }));
        return this;
    }

    /** Ряд кнопок равной ширины. */
    public AdminForm buttons(UIButton... buttons) {
        rows.add(new Row(rowH, (area, r) -> {
            int[] flex = new int[buttons.length];
            for (int i = 0; i < flex.length; i++) flex[i] = Flow.flex(1);
            Rect[] p = Flow.row(r, Spacing.SM, Flow.Justify.START, flex);
            for (int i = 0; i < buttons.length; i++) area.place(buttons[i], buttonBox(p[i]));
        }));
        return this;
    }

    /** Узкий разделитель. */
    public AdminForm gap() {
        rows.add(new Row(Spacing.SM, (area, r) -> {}));
        return this;
    }

    /** Разложить ряды в прокрутку на области {@code bounds} (добавить результат через {@code host.add}). */
    public UIScrollArea build(Rect bounds) {
        UIScrollArea area = new UIScrollArea();
        area.setBounds(bounds);
        int gap = Spacing.XXS;
        int total = 0;
        for (Row row : rows) total += row.height() + gap;
        int w = area.contentWidth(total);
        int y = 0;
        for (Row row : rows) {
            row.place().accept(area, Rect.of(0, y, w, row.height()));
            y += row.height() + gap;
        }
        return area;
    }

    private static Font font() {
        return Minecraft.getInstance().font;
    }

    private static void label(GuiGraphics g, Rect b, Component text, int color) {
        Font font = font();
        Component shown = UIDraw.ellipsize(font, text, b.w() - Spacing.SM);
        UIDraw.text(g, font, shown, b.x() + Spacing.XS, b.y() + (b.h() - font.lineHeight) / 2 + 1, color, false);
    }

    /** Кнопка по высоте малой кнопки, по центру ряда. */
    private static Rect buttonBox(Rect r) {
        int h = Math.min(r.h(), Spacing.BUTTON_SMALL_HEIGHT);
        return Rect.of(r.x(), r.y() + (r.h() - h) / 2, r.w(), h);
    }

    /** Текстовый ряд без реакции на мышь: рисует в своих текущих bounds. */
    private static final class Line extends UIWidget {
        private final BiConsumer<GuiGraphics, Rect> draw;

        Line(BiConsumer<GuiGraphics, Rect> draw) {
            super(0, 0, 0, 0);
            this.draw = draw;
        }

        @Override
        public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
            draw.accept(g, bounds());
        }
    }
}
