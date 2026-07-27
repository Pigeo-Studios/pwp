package com.pigeostudios.pwp.warfare.client.gui;

import com.pigeostudios.pwp.warfare.client.gui.deploy.DeployData;
import com.pwp.coreclient.gui.theme.PWPTheme;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import com.pwp.coreclient.gui.components.PWPButton;
import com.pwp.coreclient.gui.components.PWPPanel;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

public class KitPreviewScreen extends Screen {
    private final Screen parent;
    private final String kitName;
    private final List<ItemStack> items;
    private static final int SLOT_SIZE = 18;
    private static final int PANEL_PAD = 10;

    public KitPreviewScreen(Screen parent, String kitName, List<ItemStack> items) {
        super(Component.literal(DeployData.getDisplayName(kitName)));
        this.parent = parent;
        this.kitName = kitName;
        this.items = items;
    }

    @Override
    protected void init() {
        addRenderableWidget(
            new PWPButton(width / 2 - 40, height - 30, 80, 20,
                Component.translatable("gui.pwpwarfare.kit_preview.back"),
                b -> minecraft.setScreen(parent),
                PWPButton.Style.PRIMARY
            )
        );
    }

    @Override
    public void render(GuiGraphics gui, int mx, int my, float pt) {
        renderBackground(gui);

        int gridW = 9 * SLOT_SIZE;
        int gridH = 3 * SLOT_SIZE + 2 * SLOT_SIZE;
        int panelW = gridW + PANEL_PAD * 2;
        int panelH = gridH + PANEL_PAD * 2 + 30;
        int cx = (width - panelW) / 2;
        int cy = (height - panelH) / 2;

        PWPPanel.render(gui, cx, cy, panelW, panelH);
        gui.drawCenteredString(PWPTheme.Fonts.display(), title, width / 2, cy + 4, PWPTheme.Colors.TEXT_PRIMARY);
        gui.fill(cx + 4, cy + 14, cx + panelW - 4, cy + 15, PWPTheme.Colors.ACCENT);

        int startX = cx + PANEL_PAD;
        int startY = cy + 24;

        for (int i = 0; i < 27; i++) {
            int x = startX + i % 9 * SLOT_SIZE;
            int y = startY + i / 9 * SLOT_SIZE;
            drawSlot(gui, x, y, items.get(i + 9));
        }
        for (int i = 0; i < 9; i++) {
            int x = startX + i * SLOT_SIZE;
            int y = startY + 3 * SLOT_SIZE;
            drawSlot(gui, x, y, items.get(i));
        }
        for (int i = 0; i < 5; i++) {
            int x = startX + i * SLOT_SIZE;
            int y = startY + 4 * SLOT_SIZE + 4;
            drawSlot(gui, x, y, items.get(i + 36));
        }

        super.render(gui, mx, my, pt);
    }

    private void drawSlot(GuiGraphics gui, int x, int y, ItemStack stack) {
        gui.fill(x, y, x + 17, y + 17, PWPTheme.Colors.SURFACE_LIGHT);
        gui.renderOutline(x, y, 17, 17, PWPTheme.Colors.BORDER);
        gui.renderFakeItem(stack, x + 1, y + 1);
        gui.renderItemDecorations(PWPTheme.Fonts.display(), stack, x + 1, y + 1);
    }
}
