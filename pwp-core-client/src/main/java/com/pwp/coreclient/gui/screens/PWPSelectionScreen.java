package com.pwp.coreclient.gui.screens;

import com.pwp.coreclient.gui.components.PWPCard;
import com.pwp.coreclient.gui.components.PWPLayout;
import com.pwp.coreclient.gui.components.PWPPanel;
import com.pwp.coreclient.gui.components.PWPButton;
import com.pwp.coreclient.gui.theme.PWPTheme;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class PWPSelectionScreen extends Screen {

    private final String title;
    private final String instruction;
    private final List<String> items;
    private final BiConsumer<GuiGraphics, SelectionItem> cardRenderer;
    private final Consumer<Integer> onConfirm;
    private final int autoSelectSeconds;

    private int selectedIndex = -1;
    private PWPButton confirmBtn;
    private long openTime;

    public static class SelectionItem {
        public final int index;
        public final String label;
        public final boolean selected;
        public final boolean hovered;
        public final int x, y, w, h;

        public SelectionItem(int index, String label, boolean selected, boolean hovered, int x, int y, int w, int h) {
            this.index = index;
            this.label = label;
            this.selected = selected;
            this.hovered = hovered;
            this.x = x;
            this.y = y;
            this.w = w;
            this.h = h;
        }
    }

    public PWPSelectionScreen(String title, String instruction, List<String> items,
                              BiConsumer<GuiGraphics, SelectionItem> cardRenderer,
                              Consumer<Integer> onConfirm) {
        this(title, instruction, items, cardRenderer, onConfirm, -1);
    }

    public PWPSelectionScreen(String title, String instruction, List<String> items,
                              BiConsumer<GuiGraphics, SelectionItem> cardRenderer,
                              Consumer<Integer> onConfirm, int autoSelectSeconds) {
        super(Component.literal(title));
        this.title = title;
        this.instruction = instruction;
        this.items = items;
        this.cardRenderer = cardRenderer;
        this.onConfirm = onConfirm;
        this.autoSelectSeconds = autoSelectSeconds;
        this.openTime = System.currentTimeMillis();
    }

    @Override
    protected void init() {
        super.init();

        int btnW = 180;
        int btnH = 28;

        confirmBtn = addRenderableWidget(new PWPButton(
            PWPLayout.centerX(width, btnW), height - 55, btnW, btnH,
            Component.literal("Подтвердить"),
            btn -> {
                if (selectedIndex >= 0 && onConfirm != null) {
                    onConfirm.accept(selectedIndex);
                }
            },
            PWPButton.Style.ACCENT
        ));
        confirmBtn.active = false;
    }

    @Override
    public void render(GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
        renderBackground(gui);

        PWPLayout.renderHeader(gui, title, width);

        var font = minecraft.font;
        if (instruction != null && !instruction.isEmpty()) {
            gui.drawCenteredString(font, Component.literal(instruction), width / 2, 28, PWPTheme.Colors.TEXT_SECONDARY);
        }

        int contentW = PWPLayout.contentWidth(width);
        int cx = PWPLayout.centerX(width, contentW);
        int cardW = 130;
        int cardH = 50;
        int gap = 8;

        int cols = PWPLayout.gridColumns(contentW, gap, cardW);
        int gridW = cols * cardW + (cols - 1) * gap;
        int gridX = PWPLayout.centerX(width, gridW);
        int gridY = 42;

        for (int i = 0; i < items.size(); i++) {
            int row = i / cols;
            int col = i % cols;
            int ix = gridX + col * (cardW + gap);
            int iy = gridY + row * (cardH + gap);

            boolean isSelected = (i == selectedIndex);
            boolean isHovered = PWPCard.isHovered(ix, iy, cardW, cardH, mouseX, mouseY);

            PWPCard.State state = PWPCard.getState(isSelected, isHovered);
            PWPCard.render(gui, ix, iy, cardW, cardH, state);

            SelectionItem si = new SelectionItem(i, items.get(i), isSelected, isHovered, ix, iy, cardW, cardH);
            cardRenderer.accept(gui, si);
        }

        // Auto-select timer
        if (autoSelectSeconds > 0 && selectedIndex < 0) {
            long elapsed = (System.currentTimeMillis() - openTime) / 1000;
            int remaining = Math.max(0, autoSelectSeconds - (int) elapsed);
            if (remaining <= 0 && !items.isEmpty()) {
                selectedIndex = 0;
                confirmBtn.active = true;
            }
            String timerText = "Автовыбор через " + remaining + "с";
            gui.drawCenteredString(font, Component.literal(timerText), width / 2, height - 85, PWPTheme.Colors.TEXT_DIM);
        }

        super.render(gui, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && selectedIndex >= 0 && confirmBtn.active) {
            if (PWPCard.isHovered(confirmBtn.getX(), confirmBtn.getY(), confirmBtn.getWidth(), confirmBtn.getHeight(),
                (int) mouseX, (int) mouseY)) {
                return super.mouseClicked(mouseX, mouseY, button);
            }
        }

        int contentW = PWPLayout.contentWidth(width);
        int cardW = 130;
        int cardH = 50;
        int gap = 8;
        int cols = PWPLayout.gridColumns(contentW, gap, cardW);
        int gridW = cols * cardW + (cols - 1) * gap;
        int gridX = PWPLayout.centerX(width, gridW);
        int gridY = 42;

        for (int i = 0; i < items.size(); i++) {
            int row = i / cols;
            int col = i % cols;
            int ix = gridX + col * (cardW + gap);
            int iy = gridY + row * (cardH + gap);

            if (PWPCard.isHovered(ix, iy, cardW, cardH, (int) mouseX, (int) mouseY)) {
                selectedIndex = i;
                confirmBtn.active = true;
                return true;
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
