package com.pigeostudios.pwp.warfare.client.gui;

import com.pwp.coreclient.gui.components.PWPButton;
import com.pigeostudios.pwp.warfare.network.PacketHandler;
import com.pigeostudios.pwp.warfare.network.PacketOpenPlayerKitMenu;
import com.pigeostudios.pwp.warfare.network.PacketSelectKit;
import com.mojang.blaze3d.systems.RenderSystem;
import com.pwp.coreclient.gui.theme.PWPTheme;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class PlayerKitSelectScreen extends Screen {
    private final List<PacketOpenPlayerKitMenu.KitDTO> kits;
    private static final int CARD_W = 100;
    private static final int CARD_H = 78;
    private static final int GAP = 8;
    private static final Component EYE = Component.literal("\uD83D\uDC41");

    private int cols;
    private int panelW;
    private int panelH;
    private int contentH;
    private int scrollOff;
    private int maxScroll;
    private int cx;
    private final List<PWPButton> kitButtons = new ArrayList<>();
    private final List<PWPButton> eyeButtons = new ArrayList<>();

    public PlayerKitSelectScreen(List<PacketOpenPlayerKitMenu.KitDTO> kits) {
        super(Component.translatable("gui.pwpwarfare.kit_select.title"));
        this.kits = kits;
    }

    @Override
    protected void init() {
        cols = Math.max(1, Math.min(4, (width - 40) / (CARD_W + GAP)));
        cols = Math.min(cols, kits.size());
        panelW = cols * CARD_W + (cols - 1) * GAP + 16;
        panelW = Math.min(panelW, width - 20);
        cx = (width - panelW) / 2;

        int rows = (kits.size() + cols - 1) / cols;
        contentH = rows * CARD_H + (rows - 1) * GAP;
        panelH = Math.min(height - 50, contentH + 50);
        maxScroll = Math.max(0, contentH + 50 - panelH);
        if (maxScroll == 0) scrollOff = 0;
        if (scrollOff > maxScroll) scrollOff = maxScroll;

        kitButtons.clear();
        eyeButtons.clear();

        for (int i = 0; i < kits.size(); i++) {
            PacketOpenPlayerKitMenu.KitDTO kit = kits.get(i);
            Component label = kit.isSelected
                ? Component.literal("вњ” " + kit.name)
                : Component.literal(kit.name);

            PWPButton btn = new PWPButton(0, 0, 0, 0, label, b -> {
                PacketHandler.INSTANCE.sendToServer(new PacketSelectKit(kit.name));
                onClose();
            }, PWPButton.Style.PRIMARY);
            btn.active = kit.available;
            addRenderableWidget(btn);
            kitButtons.add(btn);

            PWPButton eyeBtn = new PWPButton(0, 0, 0, 0, EYE, b ->
                minecraft.setScreen(new KitPreviewScreen(this, kit.name, kit.items))
            , PWPButton.Style.GHOST);
            addRenderableWidget(eyeBtn);
            eyeButtons.add(eyeBtn);
        }

        repositionButtons();
    }

    private void repositionButtons() {
        int baseY = 46 - scrollOff;

        for (int i = 0; i < kits.size(); i++) {
            int r = i / cols;
            int c = i % cols;
            int bx = cx + 8 + c * (CARD_W + GAP);
            int by = baseY + r * (CARD_H + GAP);

            kitButtons.get(i).setX(bx + 5);
            kitButtons.get(i).setY(by + 30);
            kitButtons.get(i).setWidth(CARD_W - 30);
            kitButtons.get(i).setHeight(18);

            eyeButtons.get(i).setX(bx + CARD_W - 22);
            eyeButtons.get(i).setY(by + 30);
            eyeButtons.get(i).setWidth(18);
            eyeButtons.get(i).setHeight(18);
        }
    }

    @Override
    public void render(GuiGraphics gui, int mx, int my, float pt) {
        renderBackground(gui);

        int py = 10;
        gui.fill(cx, py, cx + panelW, py + panelH, PWPTheme.Colors.SURFACE);
        gui.renderOutline(cx, py, panelW, panelH, PWPTheme.Colors.BORDER);

        gui.drawCenteredString(PWPTheme.Fonts.display(), title, width / 2, py + 4, PWPTheme.Colors.TEXT_PRIMARY);
        gui.fill(cx + 4, py + 14, cx + panelW - 4, py + 15, PWPTheme.Colors.ACCENT);

        int clipY = py + 16;
        int clipH = panelH - 16;
        gui.enableScissor(cx, clipY, cx + panelW, clipY + clipH);

        int baseY = 46 - scrollOff;
        for (int i = 0; i < kits.size(); i++) {
            int r = i / cols;
            int c = i % cols;
            int bx = cx + 8 + c * (CARD_W + GAP);
            int by = baseY + r * (CARD_H + GAP);

            PacketOpenPlayerKitMenu.KitDTO kit = kits.get(i);

            int cardBg = kit.available ? PWPTheme.Colors.SURFACE_LIGHT : 0x3312151A;
            int cardBorder = kit.isSelected ? PWPTheme.Colors.BORDER_ACCENT : PWPTheme.Colors.BORDER;
            gui.fill(bx, by, bx + CARD_W, by + CARD_H, cardBg);
            gui.renderOutline(bx, by, CARD_W, CARD_H, cardBorder);

            String iconName = kit.name.toLowerCase().replace(" ", "_").replace("-", "_");
            ResourceLocation iconLoc = new ResourceLocation("pwpwarfare", "textures/gui/kits/" + iconName + ".png");
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, kit.available ? 1.0F : 0.4F);
            gui.blit(iconLoc, bx + (CARD_W - 24) / 2, by + 3, 0, 0, 24, 24, 24, 24);

            if (kit.isSelected) {
                RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
                gui.renderOutline(bx + (CARD_W - 24) / 2 - 2, by + 1, 28, 28, PWPTheme.Colors.ACCENT);
            }
        }

        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        super.render(gui, mx, my, pt);

        gui.disableScissor();

        if (maxScroll > 0) {
            if (scrollOff > 0)
                gui.drawCenteredString(PWPTheme.Fonts.display(), Component.literal("\u25B2"), width / 2, py + 2, PWPTheme.Colors.TEXT_DIM);
            if (scrollOff < maxScroll)
                gui.drawCenteredString(PWPTheme.Fonts.display(), Component.literal("\u25BC"), width / 2, py + panelH - 4, PWPTheme.Colors.TEXT_DIM);
        }

        for (Renderable w : renderables) {
            if (w instanceof Button btn && btn.isHovered() && !btn.active) {
                for (PacketOpenPlayerKitMenu.KitDTO kit : kits) {
                    String msg = btn.getMessage().getString();
                    if (msg.equals(kit.name) || msg.equals("вњ” " + kit.name)) {
                        gui.renderTooltip(PWPTheme.Fonts.display(), Component.literal(kit.reason), mx, my);
                        break;
                    }
                }
            }
        }
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        if (maxScroll > 0 && mx >= cx && mx <= cx + panelW) {
            int prev = scrollOff;
            scrollOff = (int) Math.max(0, Math.min(maxScroll, scrollOff - delta * 20));
            if (prev != scrollOff) repositionButtons();
            return true;
        }
        return super.mouseScrolled(mx, my, delta);
    }
}
