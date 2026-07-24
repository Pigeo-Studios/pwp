package com.pwp.coreclient.gui.components;

import com.pwp.coreclient.gui.theme.PWPTheme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class PWPTabs {

    private final List<TabWidget> tabWidgets = new ArrayList<>();
    private int selectedIndex;
    private float underlineX;
    private long lastRenderTime;
    private final Consumer<Integer> onChange;
    private int x, y, width;

    public PWPTabs(int x, int y, int width, int selectedIndex, Consumer<Integer> onChange) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.selectedIndex = selectedIndex;
        this.onChange = onChange;
        this.lastRenderTime = System.currentTimeMillis();
    }

    public void setBounds(int x, int y, int width) {
        this.x = x;
        this.y = y;
        this.width = width;
    }

    public void setTabs(List<String> labels) {
        tabWidgets.clear();
        int tabCount = labels.size();
        int tabW = Math.min(width / tabCount, 140);
        int startX = x + (width - tabW * tabCount) / 2;

        for (int i = 0; i < tabCount; i++) {
            int finalI = i;
            int tx = startX + i * tabW;
            TabWidget w = new TabWidget(tx, y, tabW, PWPTheme.Spacing.BUTTON_TAB_HEIGHT,
                Component.literal(labels.get(i)),
                () -> {
                    if (finalI != selectedIndex) {
                        selectedIndex = finalI;
                        if (onChange != null) onChange.accept(finalI);
                    }
                });
            tabWidgets.add(w);
        }
        updateUnderlineX();
    }

    public int getSelectedIndex() {
        return selectedIndex;
    }

    public void setSelectedIndex(int index) {
        if (index >= 0 && index < tabWidgets.size() && index != selectedIndex) {
            selectedIndex = index;
            if (onChange != null) onChange.accept(index);
        }
    }

    public List<? extends AbstractWidget> getWidgets() {
        return tabWidgets;
    }

    public void render(GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
        long now = System.currentTimeMillis();
        float dt = Math.min((now - lastRenderTime) / 50.0F, 4.0F);
        lastRenderTime = now;

        float targetUnderline = computeUnderlineTarget();
        float speed = 0.12F;
        underlineX += (targetUnderline - underlineX) * (1 - (float) Math.exp(-dt * speed * 50));

        for (TabWidget w : tabWidgets) {
            w.render(gui, mouseX, mouseY, partialTick);
        }

        gui.fill(x, y + PWPTheme.Spacing.BUTTON_TAB_HEIGHT, x + width, y + PWPTheme.Spacing.BUTTON_TAB_HEIGHT + 1,
            PWPTheme.Styles.Tab.BORDER);

        int underlineW = Math.min(width / Math.max(1, tabWidgets.size()), 60);
        gui.fill((int) underlineX - underlineW / 2, y + PWPTheme.Spacing.BUTTON_TAB_HEIGHT,
            (int) underlineX + underlineW / 2, y + PWPTheme.Spacing.BUTTON_TAB_HEIGHT + 2,
            PWPTheme.Styles.Tab.BORDER_ACTIVE);
    }

    private float computeUnderlineTarget() {
        if (tabWidgets.isEmpty()) return x + width / 2;
        TabWidget sel = tabWidgets.get(Math.min(selectedIndex, tabWidgets.size() - 1));
        return sel.getX() + sel.getWidth() / 2;
    }

    private void updateUnderlineX() {
        underlineX = computeUnderlineTarget();
    }

    private static class TabWidget extends AbstractWidget {
        private final Runnable onClick;

        TabWidget(int x, int y, int w, int h, Component msg, Runnable onClick) {
            super(x, y, w, h, msg);
            this.onClick = onClick;
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput output) {
        }

        @Override
        public boolean mouseClicked(double mx, double my, int button) {
            if (this.active && this.visible && clicked(mx, my)) {
                this.onClick.run();
                return true;
            }
            return false;
        }

        @Override
        protected void renderWidget(GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
            if (!this.visible) return;
            boolean hovered = this.isHovered();
            int bg = hovered ? PWPTheme.Styles.Tab.BG_HOVER : PWPTheme.Styles.Tab.BG;
            var font = Minecraft.getInstance().font;
            int textColor = PWPTheme.Styles.Tab.TEXT;

            int x = this.getX();
            int y = this.getY();
            int w = this.width;
            int h = this.height;

            gui.fill(x, y, x + w, y + h, bg);
            gui.drawCenteredString(font, this.getMessage(), x + w / 2, y + (h - 8) / 2, textColor);
        }
    }
}
