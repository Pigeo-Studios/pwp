package com.pigeostudios.pwp.warfare.client.gui;

import com.pigeostudios.pwp.warfare.client.WarfareClipboard;
import com.pigeostudios.pwp.warfare.network.PacketHandler;
import com.pigeostudios.pwp.warfare.network.PacketOpenKitEditor;
import com.pigeostudios.pwp.warfare.network.PacketPasteKit;
import com.pigeostudios.pwp.warfare.network.PacketPasteTeam;
import com.pigeostudios.pwp.warfare.network.PacketRequestKitData;
import com.pigeostudios.pwp.warfare.world.WarfareWorldData;
import com.mojang.blaze3d.systems.RenderSystem;
import com.pwp.coreclient.gui.theme.PWPTheme;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import com.pwp.coreclient.gui.components.PWPButton;
import com.pwp.coreclient.gui.components.PWPPanel;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class KitListScreen extends Screen {
    private final String team;
    private static final int CARD_W = 120;
    private static final int CARD_H = 64;
    private static final int GAP = 10;

    private int cols;
    private int panelW;
    private int panelH;
    private int contentH;
    private int scrollOff;
    private int maxScroll;
    private int cx;
    private final List<PWPButton> kitButtons = new ArrayList<>();

    public KitListScreen(String team) {
        super(Component.translatable("gui.pwpwarfare.kit_list.title_format", team));
        this.team = team;
    }

    @Override
    protected void init() {
        cols = Math.max(1, Math.min(4, (width - 40) / (CARD_W + GAP)));
        cols = Math.min(cols, WarfareWorldData.KIT_NAMES.length);
        panelW = cols * CARD_W + (cols - 1) * GAP + 20;
        panelW = Math.min(panelW, width - 20);
        cx = (width - panelW) / 2;

        int rows = (WarfareWorldData.KIT_NAMES.length + cols - 1) / cols;
        contentH = rows * CARD_H + (rows - 1) * GAP;
        panelH = Math.min(height - 60, contentH + 76);
        maxScroll = Math.max(0, contentH + 76 - panelH);
        if (maxScroll == 0) scrollOff = 0;
        if (scrollOff > maxScroll) scrollOff = maxScroll;

        clearWidgets();
        kitButtons.clear();

        addRenderableWidget(
            new PWPButton(cx + 6, 28, 80, 20,
                Component.translatable("gui.pwpwarfare.kit_list.copy_all"),
                b -> PacketHandler.INSTANCE.sendToServer(new PacketRequestKitData(team, "ALL")),
                PWPButton.Style.PRIMARY
            )
        );
        PWPButton pasteAllBtn = new PWPButton(cx + 92, 28, 80, 20,
            Component.translatable("gui.pwpwarfare.kit_list.paste_all"),
            b -> {
                if (WarfareClipboard.teamKitsData != null)
                    PacketHandler.INSTANCE.sendToServer(new PacketPasteTeam(team, WarfareClipboard.teamKitsData));
            },
            PWPButton.Style.PRIMARY
        );
        pasteAllBtn.active = WarfareClipboard.teamKitsData != null;
        addRenderableWidget(pasteAllBtn);

        for (int i = 0; i < WarfareWorldData.KIT_NAMES.length; i++) {
            String kitName = WarfareWorldData.KIT_NAMES[i];

            PWPButton editBtn = new PWPButton(0, 0, 0, 0,
                Component.literal(kitName),
                b -> PacketHandler.INSTANCE.sendToServer(new PacketOpenKitEditor(team, kitName)),
                PWPButton.Style.PRIMARY
            );
            addRenderableWidget(editBtn);
            kitButtons.add(editBtn);

            PWPButton copyBtn = new PWPButton(0, 0, 0, 0,
                Component.translatable("gui.pwpwarfare.kit_list.copy"),
                b -> PacketHandler.INSTANCE.sendToServer(new PacketRequestKitData(team, kitName)),
                PWPButton.Style.PRIMARY
            );
            addRenderableWidget(copyBtn);
            kitButtons.add(copyBtn);

            PWPButton pBtn = new PWPButton(0, 0, 0, 0,
                Component.translatable("gui.pwpwarfare.kit_list.paste"),
                b -> {
                    if (WarfareClipboard.kitData != null)
                        PacketHandler.INSTANCE.sendToServer(new PacketPasteKit(team, kitName, WarfareClipboard.kitData));
                },
                PWPButton.Style.PRIMARY
            );
            pBtn.active = WarfareClipboard.kitData != null;
            addRenderableWidget(pBtn);
            kitButtons.add(pBtn);
        }

        repositionKitButtons();
    }

    private void repositionKitButtons() {
        int baseY = 60 - scrollOff;

        for (int i = 0; i < WarfareWorldData.KIT_NAMES.length; i++) {
            int r = i / cols;
            int c = i % cols;
            int bx = cx + 10 + c * (CARD_W + GAP);
            int by = baseY + r * (CARD_H + GAP);
            int idx = i * 3;

            kitButtons.get(idx).setX(bx + 6);
            kitButtons.get(idx).setY(by + 30);
            kitButtons.get(idx).setWidth(70);
            kitButtons.get(idx).setHeight(20);

            kitButtons.get(idx + 1).setX(bx + 78);
            kitButtons.get(idx + 1).setY(by + 30);
            kitButtons.get(idx + 1).setWidth(18);
            kitButtons.get(idx + 1).setHeight(20);

            kitButtons.get(idx + 2).setX(bx + 98);
            kitButtons.get(idx + 2).setY(by + 30);
            kitButtons.get(idx + 2).setWidth(18);
            kitButtons.get(idx + 2).setHeight(20);
        }
    }

    @Override
    public void render(GuiGraphics gui, int mx, int my, float pt) {
        renderBackground(gui);

        int py = 10;
        PWPPanel.render(gui, cx, py, panelW, panelH);

        int titleColor = team.equals("BLUE") ? PWPTheme.Colors.TEAM_BLUE : PWPTheme.Colors.TEAM_RED;
        gui.drawCenteredString(PWPTheme.Fonts.display(), Component.literal(title.getString()), width / 2, py + 4, titleColor);
        gui.fill(cx + 6, py + 14, cx + panelW - 6, py + 15, PWPTheme.Colors.ACCENT);

        int clipY = py + 40;
        int clipH = panelH - 40;
        gui.enableScissor(cx, clipY, cx + panelW, clipY + clipH);

        int baseY = 60 - scrollOff;
        for (int i = 0; i < WarfareWorldData.KIT_NAMES.length; i++) {
            String kitName = WarfareWorldData.KIT_NAMES[i];
            int r = i / cols;
            int c = i % cols;
            int bx = cx + 10 + c * (CARD_W + GAP);
            int by = baseY + r * (CARD_H + GAP);

            gui.fill(bx, by, bx + CARD_W, by + CARD_H, PWPTheme.Colors.SURFACE_LIGHT);
            gui.renderOutline(bx, by, CARD_W, CARD_H, PWPTheme.Colors.BORDER);

            String iconPath = kitName.toLowerCase().replace(" ", "_").replace("-", "_");
            ResourceLocation iconLoc = new ResourceLocation("pwpwarfare", "textures/gui/kits/" + iconPath + ".png");
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            gui.blit(iconLoc, bx + (CARD_W - 24) / 2, by + 4, 0, 0, 24, 24, 24, 24);
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
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        if (maxScroll > 0 && mx >= cx && mx <= cx + panelW) {
            int prev = scrollOff;
            scrollOff = (int) Math.max(0, Math.min(maxScroll, scrollOff - delta * 20));
            if (prev != scrollOff) repositionKitButtons();
            return true;
        }
        return super.mouseScrolled(mx, my, delta);
    }
}
