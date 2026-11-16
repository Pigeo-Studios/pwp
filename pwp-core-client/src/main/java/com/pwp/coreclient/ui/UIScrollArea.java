package com.pwp.coreclient.ui;

import com.pwp.coreclient.gui.theme.PWPTheme.Spacing;
import com.pwp.coreclient.gui.theme.PWPTheme.Styles;
import com.pwp.coreclient.ui.layout.Rect;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * Прокручиваемая область с дочерними виджетами (сетка карточек): дети раскладываются в
 * координатах КОНТЕНТА ({@link #place}), область сама сдвигает их на прокрутку, обрезает
 * scissor'ом, колесо мыши прокручивает плавно, клики/тултипы получают только видимые дети
 * внутри окна. Если контент влезает — прокрутки и скроллбара нет.
 */
@OnlyIn(Dist.CLIENT)
public class UIScrollArea extends UIWidget {
    private final List<UIWidget> children = new ArrayList<>();
    private final List<Rect> local = new ArrayList<>();
    private final UIAnim scroll = new UIAnim(0F, 120, UIAnim.EASE_OUT_CUBIC);
    private double scrollTarget;
    private int contentHeight;
    private int lastMouseX = Integer.MIN_VALUE;
    private int lastMouseY = Integer.MIN_VALUE;

    public UIScrollArea() {
        super(0, 0, 0, 0);
    }

    /** Убрать всех детей (прокрутка сохраняется в допустимых пределах). */
    public void clear() {
        children.clear();
        local.clear();
        contentHeight = 0;
    }

    /** Добавить ребёнка в прямоугольник КОНТЕНТА (0,0 = левый верх области без прокрутки). */
    public <T extends UIWidget> T place(T child, Rect inContent) {
        children.add(child);
        local.add(inContent);
        contentHeight = Math.max(contentHeight, inContent.bottom());
        return child;
    }

    /** Переставить уже добавленного ребёнка (данные пришли — размер прежний, позиция новая). */
    public void move(UIWidget child, Rect inContent) {
        int i = children.indexOf(child);
        if (i < 0) return;
        local.set(i, inContent);
        contentHeight = 0;
        for (Rect r : local) contentHeight = Math.max(contentHeight, r.bottom());
    }

    public List<UIWidget> children() {
        return children;
    }

    /** Ширина под контент: без колонки скроллбара, если контент не влезает. */
    public int contentWidth(int forContentHeight) {
        return forContentHeight > height ? width - Spacing.SCROLLBAR_WIDTH - Spacing.XS : width;
    }

    private int offset() {
        scrollTarget = UIMath.clampScroll(scrollTarget, contentHeight, height);
        scroll.to((float) scrollTarget);
        return Math.round(scroll.value());
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        lastMouseX = mouseX;
        lastMouseY = mouseY;
        int off = offset();
        boolean inside = isMouseOver(mouseX, mouseY);
        g.enableScissor(x, y, x + width, y + height);
        for (int i = 0; i < children.size(); i++) {
            UIWidget c = children.get(i);
            Rect r = local.get(i).translate(x, y - off);
            c.setBounds(r);
            if (r.bottom() < y || r.y() > y + height || !c.isVisible()) continue;
            if (c.opacity() <= 0F) continue;
            boolean shifted = c.offsetX() != 0 || c.offsetY() != 0;
            if (shifted) {
                g.pose().pushPose();
                g.pose().translate(c.offsetX(), c.offsetY(), 0);
            }
            c.render(g, inside ? mouseX : Integer.MIN_VALUE, inside ? mouseY : Integer.MIN_VALUE, partialTick);
            if (shifted) g.pose().popPose();
        }
        g.disableScissor();
        if (contentHeight > height) {
            int sbw = Spacing.SCROLLBAR_WIDTH;
            int tx = x + width - sbw;
            UIDraw.rect(g, Rect.of(tx, y, sbw, height), UIDraw.fade(Styles.Loader.TRACK, opacity));
            int thumb = UIMath.thumbHeight(contentHeight, height, Spacing.LG);
            int ty = y + UIMath.thumbOffset(scroll.value(), contentHeight, height, thumb);
            UIDraw.rect(g, Rect.of(tx, ty, sbw, thumb), UIDraw.fade(Styles.ScrollBar.THUMB, opacity));
        }
    }

    private UIWidget childAt(double mx, double my) {
        if (!isMouseOver(mx, my)) return null;
        for (int i = children.size() - 1; i >= 0; i--) {
            UIWidget c = children.get(i);
            if (c.isVisible() && c.opacity() > 0F && c.isMouseOver(mx, my)) return c;
        }
        return null;
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        UIWidget c = childAt(mx, my);
        return c != null && c.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        boolean handled = false;
        for (UIWidget c : children) handled |= c.mouseReleased(mx, my, button);
        return handled;
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        if (!isMouseOver(mx, my) || contentHeight <= height) return false;
        scrollTarget = UIMath.clampScroll(scrollTarget - delta * Spacing.XXL, contentHeight, height);
        return true;
    }

    /** Тултип ребёнка под курсором (UILayer спрашивает у верхнего виджета). */
    @Override
    public UITooltip.Content tooltip() {
        UIWidget c = childAt(lastMouseX, lastMouseY);
        return c != null ? c.tooltip() : null;
    }
}
