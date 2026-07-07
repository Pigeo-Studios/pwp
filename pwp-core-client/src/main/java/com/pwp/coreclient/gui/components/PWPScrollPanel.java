package com.pwp.coreclient.gui.components;

import com.pwp.coreclient.gui.theme.PWPTheme;
import net.minecraft.client.gui.GuiGraphics;

public class PWPScrollPanel {

    private double scrollOffset;
    private double scrollMax;
    private int contentHeight;
    private int viewportHeight;
    private int x, y, width, height;
    private boolean visible = true;
    private boolean dragging;
    private double dragStartY;
    private double dragStartScroll;

    private static final int SCROLLBAR_WIDTH = 5;
    private static final int SCROLLBAR_MARGIN = 2;

    public PWPScrollPanel(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.scrollOffset = 0;
        this.scrollMax = 0;
        this.contentHeight = 0;
        this.viewportHeight = height;
    }

    public void setBounds(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.viewportHeight = height;
        recalcScroll();
    }

    public void setContentHeight(int contentHeight) {
        this.contentHeight = contentHeight;
        recalcScroll();
    }

    public int getContentHeight() {
        return contentHeight;
    }

    private void recalcScroll() {
        int prevMax = (int) scrollMax;
        scrollMax = Math.max(0, contentHeight - viewportHeight);
        if (scrollOffset > scrollMax) scrollOffset = scrollMax;
    }

    public double getScrollOffset() {
        return scrollOffset;
    }

    public void setScrollOffset(double offset) {
        scrollOffset = Math.max(0, Math.min(offset, scrollMax));
    }

    public boolean hasScroll() {
        return scrollMax > 0;
    }

    public int getX() { return x; }
    public int getY() { return y; }
    public int getWidth() { return width; }
    public int getHeight() { return height; }

    public boolean isMouseOver(double mx, double my) {
        return mx >= x && mx <= x + width && my >= y && my <= y + height;
    }

    public void enableScissor(GuiGraphics gui) {
        gui.enableScissor(x, y, x + width, y + height);
    }

    public void disableScissor(GuiGraphics gui) {
        gui.disableScissor();
    }

    public double mouseScrolled(double mx, double my, double delta) {
        if (!isMouseOver(mx, my) || scrollMax <= 0) return 0;
        double prev = scrollOffset;
        scrollOffset -= delta * 12;
        scrollOffset = Math.max(0, Math.min(scrollOffset, scrollMax));
        return scrollOffset - prev;
    }

    public boolean mouseClicked(double mx, double my, int button) {
        if (button != 0 || scrollMax <= 0) return false;
        int sbX = x + width - SCROLLBAR_WIDTH - SCROLLBAR_MARGIN;
        int sbY = y + SCROLLBAR_MARGIN;
        int sbH = viewportHeight - SCROLLBAR_MARGIN * 2;
        if (mx >= sbX && mx <= sbX + SCROLLBAR_WIDTH && my >= sbY && my <= sbY + sbH) {
            dragging = true;
            dragStartY = my;
            dragStartScroll = scrollOffset;
            return true;
        }
        return false;
    }

    public boolean mouseDragged(double mx, double my, int button, double dragX, double dragY) {
        if (!dragging || scrollMax <= 0) return false;
        int sbH = viewportHeight - SCROLLBAR_MARGIN * 2;
        int thumbH = getThumbHeight(sbH);
        double scrollablePixels = sbH - thumbH;
        if (scrollablePixels <= 0) return true;
        double deltaY = my - dragStartY;
        scrollOffset = Math.max(0, Math.min(scrollMax, dragStartScroll + (deltaY / scrollablePixels) * scrollMax));
        return true;
    }

    public boolean mouseReleased(double mx, double my, int button) {
        if (dragging) {
            dragging = false;
            return true;
        }
        return false;
    }

    public void renderScrollbar(GuiGraphics gui) {
        if (scrollMax <= 0) return;
        int sbX = x + width - SCROLLBAR_WIDTH - SCROLLBAR_MARGIN;
        int sbY = y + SCROLLBAR_MARGIN;
        int sbH = viewportHeight - SCROLLBAR_MARGIN * 2;

        gui.fill(sbX, sbY, sbX + SCROLLBAR_WIDTH, sbY + sbH, PWPTheme.Colors.SURFACE_DIM);

        int thumbH = getThumbHeight(sbH);
        int thumbY = sbY + (int)((scrollOffset / scrollMax) * (sbH - thumbH));
        gui.fill(sbX, thumbY, sbX + SCROLLBAR_WIDTH, thumbY + thumbH, PWPTheme.Colors.ACCENT);
        gui.fill(sbX, thumbY, sbX + SCROLLBAR_WIDTH - 1, thumbY + thumbH - 1, PWPTheme.Colors.ACCENT_SOFT);
    }

    private int getThumbHeight(int trackHeight) {
        float ratio = (float) viewportHeight / Math.max(contentHeight, viewportHeight);
        return Math.max(10, (int)(trackHeight * ratio));
    }

    public boolean isVisible() { return visible; }
    public void setVisible(boolean v) { this.visible = v; }
}
