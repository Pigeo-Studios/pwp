package com.pwp.coreclient.ui;

import com.pwp.coreclient.gui.theme.PWPTheme.Spacing;
import com.pwp.coreclient.gui.theme.PWPTheme.Styles;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * Прокручиваемый список строк одинаковой высоты с обрезкой (scissor), плавной прокруткой и
 * скроллбаром. Как рисовать строку — решает {@link RowRenderer}.
 */
@OnlyIn(Dist.CLIENT)
public class UIScrollList<T> extends UIWidget {
    /** Рисует одну строку в прямоугольнике (x, y, w, h). */
    @FunctionalInterface
    public interface RowRenderer<T> {
        void render(GuiGraphics g, T item, int x, int y, int w, int h, boolean hovered, boolean selected);
    }

    private final List<T> items = new ArrayList<>();
    private final int rowHeight;
    private final RowRenderer<T> renderer;
    private Consumer<T> onSelect;
    private double scrollTarget;
    private final UIAnim scroll = new UIAnim(0F, 120, UIAnim.EASE_OUT_CUBIC);
    private int selectedIndex = -1;

    public UIScrollList(int x, int y, int width, int height, int rowHeight, RowRenderer<T> renderer) {
        super(x, y, width, height);
        this.rowHeight = Math.max(1, rowHeight);
        this.renderer = renderer;
    }

    public UIScrollList<T> onSelect(Consumer<T> onSelect) {
        this.onSelect = onSelect;
        return this;
    }

    /** Новые данные (из DTO сервера); прокрутка сохраняется в допустимых пределах. */
    public void setItems(List<T> newItems) {
        items.clear();
        items.addAll(newItems);
        if (selectedIndex >= items.size()) selectedIndex = -1;
        scrollTo(scrollTarget);
    }

    public T selected() {
        return selectedIndex >= 0 && selectedIndex < items.size() ? items.get(selectedIndex) : null;
    }

    private int contentHeight() {
        return items.size() * rowHeight;
    }

    private void scrollTo(double target) {
        scrollTarget = UIMath.clampScroll(target, contentHeight(), height);
        scroll.to((float) scrollTarget);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        int sbw = Spacing.SCROLLBAR_WIDTH;
        int listW = width - (contentHeight() > height ? sbw + Spacing.XXS : 0);
        int offset = Math.round(scroll.value());
        g.enableScissor(x, y, x + width, y + height);
        int first = Math.max(0, offset / rowHeight);
        int last = Math.min(items.size() - 1, (offset + height) / rowHeight);
        for (int i = first; i <= last; i++) {
            int ry = y + i * rowHeight - offset;
            boolean hovered = mouseX >= x
                    && mouseX < x + listW
                    && mouseY >= Math.max(y, ry)
                    && mouseY < Math.min(y + height, ry + rowHeight);
            renderer.render(g, items.get(i), x, ry, listW, rowHeight, hovered, i == selectedIndex);
        }
        g.disableScissor();
        if (contentHeight() > height) {
            int tx = x + width - sbw;
            g.fill(tx, y, tx + sbw, y + height, Styles.ScrollBar.TRACK);
            int thumb = UIMath.thumbHeight(contentHeight(), height, Spacing.LG);
            int ty = y + UIMath.thumbOffset(scroll.value(), contentHeight(), height, thumb);
            boolean over = mouseX >= tx && mouseX < tx + sbw && mouseY >= ty && mouseY < ty + thumb;
            g.fill(tx, ty, tx + sbw, ty + thumb, over ? Styles.ScrollBar.THUMB_HOVER : Styles.ScrollBar.THUMB);
        }
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        if (!isMouseOver(mx, my)) return false;
        scrollTo(scrollTarget - delta * rowHeight * 2);
        return true;
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (!enabled || button != 0 || !isMouseOver(mx, my)) return false;
        int index = (int) ((my - y + scroll.value()) / rowHeight);
        if (index >= 0 && index < items.size()) {
            selectedIndex = index;
            if (onSelect != null) onSelect.accept(items.get(index));
        }
        return true;
    }
}
