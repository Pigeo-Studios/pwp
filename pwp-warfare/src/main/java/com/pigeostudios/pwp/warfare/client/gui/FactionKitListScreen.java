package com.pigeostudios.pwp.warfare.client.gui;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.pigeostudios.pwp.warfare.client.ClientData;
import com.pigeostudios.pwp.warfare.client.gui.deploy.DeployData;
import com.pigeostudios.pwp.warfare.network.PacketHandler;
import com.pigeostudios.pwp.warfare.network.PacketOpenFactionKitEditor;
import com.pigeostudios.pwp.warfare.network.PacketRequestData;
import com.google.gson.Gson;
import com.mojang.blaze3d.systems.RenderSystem;
import com.pwp.coreclient.gui.theme.PWPTheme;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import com.pwp.coreclient.gui.components.PWPButton;
import com.pwp.coreclient.gui.components.PWPPanel;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class FactionKitListScreen extends Screen {
    private final String faction;
    private static final int CARD_W = 120;
    private static final int CARD_H = 64;
    private static final int GAP = 10;

    private List<String> kitNames = new ArrayList<>();
    private boolean loading = true;
    private boolean requested;
    private int cols;
    private int panelW;
    private int panelH;
    private int contentH;
    private int scrollOff;
    private int maxScroll;
    private int cx;
    private final List<PWPButton> kitButtons = new ArrayList<>();

    public FactionKitListScreen(String faction) {
        super(Component.translatable("gui.pwpwarfare.faction_kit_list.title_format", faction.toUpperCase()));
        this.faction = faction;
    }

    @Override
    protected void init() {
        cols = Math.max(1, Math.min(4, (width - 40) / (CARD_W + GAP)));
        panelW = Math.min(cols * CARD_W + (cols - 1) * GAP + 20, width - 20);
        cx = (width - panelW) / 2;
        panelH = Math.min(height - 60, 400);
        maxScroll = 0;
        scrollOff = 0;

        ClientData.factionKitsData = null;
        requested = true;
        PacketHandler.INSTANCE.sendToServer(new PacketRequestData("factionKits",
            "{\"faction\":\"" + faction + "\"}"));
    }

    @Override
    public void tick() {
        super.tick();
        if (requested && ClientData.factionKitsData != null) {
            requested = false;
            JsonArray arr = ClientData.factionKitsData;
            ClientData.factionKitsData = null;
            List<String> loaded = new ArrayList<>();
            for (JsonElement e : arr) {
                JsonObject obj = e.getAsJsonObject();
                String name = obj.has("kitName") ? obj.get("kitName").getAsString() : "";
                if (!name.isEmpty()) loaded.add(name);
            }
            kitNames = loaded;
            loading = false;
            recreateWidgets();
        }
    }

    private void recreateWidgets() {
        clearWidgets();
        kitButtons.clear();

        if (kitNames.isEmpty()) return;

        cols = Math.max(1, Math.min(4, (width - 40) / (CARD_W + GAP)));
        cols = Math.min(cols, kitNames.size());
        panelW = Math.min(cols * CARD_W + (cols - 1) * GAP + 20, width - 20);
        cx = (width - panelW) / 2;

        int rows = (kitNames.size() + cols - 1) / cols;
        contentH = rows * CARD_H + (rows - 1) * GAP;
        int availH = panelH - 76;
        maxScroll = Math.max(0, contentH - availH);
        if (maxScroll == 0) scrollOff = 0;
        if (scrollOff > maxScroll) scrollOff = maxScroll;

        for (int i = 0; i < kitNames.size(); i++) {
            String kitName = kitNames.get(i);
            PWPButton editBtn = new PWPButton(0, 0, 0, 0,
                Component.literal(DeployData.getDisplayName(kitName)),
                b -> PacketHandler.INSTANCE.sendToServer(new PacketOpenFactionKitEditor(faction, kitName)),
                PWPButton.Style.PRIMARY
            );
            addRenderableWidget(editBtn);
            kitButtons.add(editBtn);

            PWPButton copyBtn = new PWPButton(0, 0, 0, 0,
                Component.translatable("gui.pwpwarfare.kit_list.copy"),
                b -> {},
                PWPButton.Style.PRIMARY
            );
            copyBtn.active = false;
            addRenderableWidget(copyBtn);
            kitButtons.add(copyBtn);

            PWPButton pBtn = new PWPButton(0, 0, 0, 0,
                Component.translatable("gui.pwpwarfare.kit_list.paste"),
                b -> {},
                PWPButton.Style.PRIMARY
            );
            pBtn.active = false;
            addRenderableWidget(pBtn);
            kitButtons.add(pBtn);
        }

        repositionKitButtons();
    }

    private void repositionKitButtons() {
        int baseY = 52 - scrollOff;

        for (int i = 0; i < kitNames.size(); i++) {
            int r = i / cols;
            int c = i % cols;
            int bx = cx + 10 + c * (CARD_W + GAP);
            int by = baseY + r * (CARD_H + GAP);
            int idx = i * 3;

            kitButtons.get(idx).setX(bx + 6);
            kitButtons.get(idx).setY(by + 32);
            kitButtons.get(idx).setWidth(108);
            kitButtons.get(idx).setHeight(22);
            kitButtons.get(idx).active = true;

            kitButtons.get(idx + 1).visible = false;
            kitButtons.get(idx + 2).visible = false;
        }
    }

    @Override
    public void render(GuiGraphics gui, int mx, int my, float pt) {
        renderBackground(gui);

        int py = 10;
        PWPPanel.render(gui, cx, py, panelW, panelH);

        gui.drawCenteredString(PWPTheme.Fonts.display(), Component.literal(title.getString()), width / 2, py + 4, PWPTheme.Colors.ACCENT);
        gui.fill(cx + 6, py + 14, cx + panelW - 6, py + 15, PWPTheme.Colors.ACCENT);

        if (loading) {
            gui.drawCenteredString(PWPTheme.Fonts.display(), Component.translatable("gui.pwpwarfare.faction_select.loading"), width / 2, height / 2, PWPTheme.Colors.TEXT_DIM);
            super.render(gui, mx, my, pt);
            return;
        }

        if (kitNames.isEmpty()) {
            gui.drawCenteredString(PWPTheme.Fonts.display(), Component.literal("No kits found for " + faction.toUpperCase()), width / 2, height / 2, PWPTheme.Colors.TEXT_DIM);
            super.render(gui, mx, my, pt);
            return;
        }

        int clipY = py + 28;
        int clipH = panelH - 28;
        gui.enableScissor(cx, clipY, cx + panelW, clipY + clipH);

        int baseY = 52 - scrollOff;
        for (int i = 0; i < kitNames.size(); i++) {
            String kitName = kitNames.get(i);
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
