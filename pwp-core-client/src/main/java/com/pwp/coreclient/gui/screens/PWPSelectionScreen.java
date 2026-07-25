package com.pwp.coreclient.gui.screens;

import com.pwp.coreclient.gui.components.PWPCard;
import com.pwp.coreclient.gui.components.PWPLayout;
import com.pwp.coreclient.gui.components.PWPButton;
import com.pwp.coreclient.gui.theme.PWPTheme;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.IntPredicate;

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
    private PWPButton confirmBtn;
    private long openTime;

    public static class SelectionItem {
        public final int index;
        public final String label;
        public final boolean selected;
        public final boolean hovered;
        public final boolean disabled;
        public final int x, y, w, h;

        public SelectionItem(int index, String label, boolean selected, boolean hovered, boolean disabled,
                              int x, int y, int w, int h) {
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

    public PWPSelectionScreen(String title, String instruction, List<String> items,
                              BiConsumer<GuiGraphics, SelectionItem> cardRenderer,
                              Consumer<Integer> onConfirm) {
        this(title, instruction, items, cardRenderer, onConfirm, -1);
    }

    public PWPSelectionScreen(String title, String instruction, List<String> items,
                              BiConsumer<GuiGraphics, SelectionItem> cardRenderer,
                              Consumer<Integer> onConfirm, int autoSelectSeconds) {
        this(title, instruction, items, cardRenderer, onConfirm, autoSelectSeconds,
            130, 50, PWPTheme.Spacing.GRID_GAP, null, false);
    }

    public PWPSelectionScreen(String title, String instruction, List<String> items,
                              BiConsumer<GuiGraphics, SelectionItem> cardRenderer,
                              Consumer<Integer> onConfirm, int autoSelectSeconds,
                              int cardW, int cardH, int gap,
                              IntPredicate disabledPredicate, boolean instantSelect) {
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
    }

    @Override
    protected void init() {
        super.init();

        if (instantSelect) return;

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

        var font = PWPTheme.Fonts.display();
        if (instruction != null && !instruction.isEmpty()) {
            gui.drawString(font, Component.literal(instruction), width / 2 - font.width(instruction) / 2, 28, PWPTheme.Colors.TEXT_SECONDARY, false);
        }

        int contentW = PWPLayout.contentWidth(width);
        int cols = PWPLayout.gridColumns(contentW, gap, cardW);
        int gridW = cols * cardW + (cols - 1) * gap;
        int gridX = PWPLayout.centerX(width, gridW);
        int gridY = 42;

        for (int i = 0; i < items.size(); i++) {
            int row = i / cols;
            int col = i % cols;
            int ix = gridX + col * (cardW + gap);
            int iy = gridY + row * (cardH + gap);

            boolean isDisabled = disabledPredicate != null && disabledPredicate.test(i);
            boolean isSelected = (i == selectedIndex);
            boolean isHovered = !isDisabled && PWPCard.isHovered(ix, iy, cardW, cardH, mouseX, mouseY);

            PWPCard.State state = PWPCard.getState(isSelected, isHovered, isDisabled);
            PWPCard.render(gui, ix, iy, cardW, cardH, state);

            SelectionItem si = new SelectionItem(i, items.get(i), isSelected, isHovered, isDisabled, ix, iy, cardW, cardH);
            cardRenderer.accept(gui, si);
        }

        if (!instantSelect && autoSelectSeconds > 0 && selectedIndex < 0) {
            long elapsed = (System.currentTimeMillis() - openTime) / 1000;
            int remaining = Math.max(0, autoSelectSeconds - (int) elapsed);
            if (remaining <= 0 && !items.isEmpty()) {
                selectedIndex = 0;
                confirmBtn.active = true;
            }
            String timerText = "Автовыбор через " + remaining + "с";
            gui.drawString(font, Component.literal(timerText), width / 2 - font.width(timerText) / 2, height - 85, PWPTheme.Colors.TEXT_DIM, false);
        }

        super.render(gui, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!instantSelect && button == 0 && selectedIndex >= 0 && confirmBtn.active) {
            if (PWPCard.isHovered(confirmBtn.getX(), confirmBtn.getY(), confirmBtn.getWidth(), confirmBtn.getHeight(),
                (int) mouseX, (int) mouseY)) {
                return super.mouseClicked(mouseX, mouseY, button);
            }
        }

        int contentW = PWPLayout.contentWidth(width);
        int cols = PWPLayout.gridColumns(contentW, gap, cardW);
        int gridW = cols * cardW + (cols - 1) * gap;
        int gridX = PWPLayout.centerX(width, gridW);
        int gridY = 42;

        for (int i = 0; i < items.size(); i++) {
            int row = i / cols;
            int col = i % cols;
            int ix = gridX + col * (cardW + gap);
            int iy = gridY + row * (cardH + gap);

            boolean isDisabled = disabledPredicate != null && disabledPredicate.test(i);
            if (isDisabled) continue;

            if (PWPCard.isHovered(ix, iy, cardW, cardH, (int) mouseX, (int) mouseY)) {
                if (instantSelect) {
                    if (onConfirm != null) onConfirm.accept(i);
                } else {
                    selectedIndex = i;
                    confirmBtn.active = true;
                }
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
