package com.pigeostudios.pwp.warfare.client.gui;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.blaze3d.systems.RenderSystem;
import com.pwp.coreclient.CoreAPI;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

public class SkinListScreen extends Screen {

    private List<SkinEntry> skins = new ArrayList<>();
    private int scrollOffset = 0;

    public static class SkinEntry {
        String skinId, name, slotType, weaponTag, rarity, modelPath;
    }

    public SkinListScreen() {
        super(Component.translatable("gui.pwpwarfare.skin_list.title"));
    }

    @Override
    protected void init() {
        int cx = this.width / 2;
        addRenderableWidget(Button.builder(Component.literal("+ Add Skin"), b -> {
            Minecraft.getInstance().setScreen(new SkinEditorScreen(this));
        }).bounds(cx - 40, 10, 90, 20).build());

        addRenderableWidget(Button.builder(Component.literal("Refresh"), b -> loadSkins())
                .bounds(cx + 55, 10, 55, 20).build());

        loadSkins();
    }

    public void loadSkins() {
        new Thread(() -> {
            try {
                JsonObject result = CoreAPI.getSkins();
                if (result != null && result.has("data")) {
                    List<SkinEntry> loaded = new ArrayList<>();
                    JsonArray arr = result.get("data").getAsJsonArray();
                    for (JsonElement e : arr) {
                        JsonObject obj = e.getAsJsonObject();
                        SkinEntry se = new SkinEntry();
                        se.skinId = obj.get("skinId").getAsString();
                        se.name = obj.has("name") ? obj.get("name").getAsString() : se.skinId;
                        se.slotType = obj.get("slotType").getAsString();
                        se.weaponTag = obj.get("weaponTag").getAsString();
                        se.rarity = obj.get("rarity").getAsString();
                        se.modelPath = obj.has("modelPath") && !obj.get("modelPath").isJsonNull() ? obj.get("modelPath").getAsString() : "";
                        loaded.add(se);
                    }
                    Minecraft.getInstance().tell(() -> {
                        skins = loaded;
                        scrollOffset = 0;
                    });
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }, "PWP-Skin-Load").start();
    }

    @Override
    public void render(GuiGraphics gui, int mx, int my, float pt) {
        renderBackground(gui);
        gui.drawCenteredString(this.font, this.title, this.width / 2, 36, 0xFFC8CBCE);

        int startX = (this.width - 440) / 2;
        int y = 50;

        if (skins.isEmpty()) {
            gui.drawCenteredString(this.font, "No skins defined. Click '+ Add Skin' to create one.", this.width / 2, this.height / 2, 0xFF7A7D84);
        }

        RenderSystem.enableBlend();
        if (!skins.isEmpty()) for (int i = Math.min(scrollOffset, skins.size() - 1); i < skins.size(); i++) {
            int row = i - scrollOffset;
            int ry = y + row * 28;
            if (ry + 26 > this.height) break;

            SkinEntry se = skins.get(i);
            int rarityColor = getRarityColor(se.rarity);

            gui.fill(startX, ry, startX + 440, ry + 24, 0xCC12151A);
            gui.renderOutline(startX, ry, 440, 24, 0xFF1E222A);

            gui.drawString(this.font, se.name, startX + 6, ry + 7, rarityColor, false);
            gui.drawString(this.font, se.slotType, startX + 160, ry + 7, 0xFF7A7D84, false);
            gui.drawString(this.font, se.weaponTag, startX + 260, ry + 7, 0xFF3D6FA5, false);
            gui.drawString(this.font, se.rarity, startX + 350, ry + 7, rarityColor, false);

            boolean hovered = mx >= startX && mx <= startX + 440 && my >= ry && my <= ry + 24;
            if (hovered) {
                if (mx >= startX + 380 && mx < startX + 410) {
                    gui.drawString(this.font, "Del", startX + 382, ry + 7, 0xFFA53D3D, false);
                } else if (mx >= startX + 410) {
                    gui.drawString(this.font, "Edit", startX + 412, ry + 7, 0xFFC8812A, false);
                }
            }
        }
        RenderSystem.disableBlend();

        super.render(gui, mx, my, pt);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        int startX = (this.width - 440) / 2;
        int y = 50;

        if (!skins.isEmpty()) for (int i = Math.min(scrollOffset, skins.size() - 1); i < skins.size(); i++) {
            int row = i - scrollOffset;
            int ry = y + row * 28;
            if (ry + 26 > this.height) break;

            if (mx >= startX && mx <= startX + 440 && my >= ry && my <= ry + 24) {
                SkinEntry se = skins.get(i);
                if (mx >= startX + 410) {
                    SkinEditorScreen s = new SkinEditorScreen(this);
                    s.editExisting(se.skinId, se.name, se.slotType, se.weaponTag, se.rarity, se.modelPath);
                    Minecraft.getInstance().setScreen(s);
                } else if (mx >= startX + 380 && mx < startX + 410) {
                    deleteSkin(se.skinId);
                }
                return true;
            }
        }
        return super.mouseClicked(mx, my, button);
    }

    private void deleteSkin(String skinId) {
        new Thread(() -> {
            try {
                CoreAPI.deleteSkin(skinId);
                Thread.sleep(200);
                Minecraft.getInstance().tell(this::loadSkins);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }, "PWP-Skin-Delete").start();
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        if (skins.isEmpty()) return true;
        int maxScroll = Math.max(0, skins.size() - 1);
        if (delta > 0) scrollOffset = Math.max(0, scrollOffset - 1);
        else scrollOffset = Math.min(maxScroll, scrollOffset + 1);
        return true;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private int getRarityColor(String rarity) {
        if (rarity == null) return 0xFF6A6D73;
        switch (rarity.toUpperCase()) {
            case "COMMON": return 0xFF6A6D73;
            case "UNCOMMON": return 0xFF3D6FA5;
            case "RARE": return 0xFF7A4A8A;
            case "EPIC": return 0xFFC8812A;
            case "LEGENDARY": return 0xFFA53D3D;
            case "MYTHIC": return 0xFFFFD700;
            default: return 0xFF6A6D73;
        }
    }
}
