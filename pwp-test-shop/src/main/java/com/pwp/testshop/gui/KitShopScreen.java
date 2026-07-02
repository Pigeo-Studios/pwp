package com.pwp.testshop.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.List;

public class KitShopScreen extends Screen {

    private final long openTime;
    private final List<KitItem> kits;
    private int scrollOffset;

    private static final int COLS = 3;
    private static final int CARD_W = 150;
    private static final int CARD_H = 120;
    private static final int CARD_GAP = 14;
    private static final int PANEL_PAD = 30;

    public KitShopScreen() {
        super(Component.literal("\u00a7eМагазин китов"));
        this.openTime = System.currentTimeMillis();
        this.kits = createStubKits();
        this.scrollOffset = 0;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    protected void init() {
        super.init();
    }

    @Override
    public void render(GuiGraphics gui, int mx, int my, float pt) {
        long elapsed = System.currentTimeMillis() - openTime;

        float bgAlpha = Mth.clamp(elapsed / 1000f, 0f, 1f);
        float titleAlpha = Mth.clamp((elapsed - 200f) / 400f, 0f, 1f);
        float contentAlpha = Mth.clamp((elapsed - 400f) / 600f, 0f, 1f);

        renderBackground(gui);

        if (bgAlpha > 0.01f) {
            RenderSystem.enableBlend();
            renderOverlay(gui, bgAlpha);
            renderAnimatedParticles(gui, elapsed);
            renderTitle(gui, titleAlpha, elapsed);
            renderCards(gui, mx, my, contentAlpha, elapsed);
            renderCloseButton(gui, mx, my, contentAlpha);
            RenderSystem.disableBlend();
        }

        super.render(gui, mx, my, pt);
    }

    private void renderOverlay(GuiGraphics gui, float alpha) {
        int a = (int) (alpha * 180);
        int topColor = a << 24 | 0x0D1117;
        int bottomColor = a << 24 | 0x0A0E14;
        gui.fillGradient(0, 0, width, height, topColor, bottomColor);

        int borderA = (int) (alpha * 40);
        int borderColor = borderA << 24 | 0x2D5F8A;
        gui.renderOutline(4, 4, width - 8, height - 8, borderColor);
    }

    private void renderAnimatedParticles(GuiGraphics gui, long elapsed) {
        int particleCount = 6;
        for (int i = 0; i < particleCount; i++) {
            float phase = (elapsed / 3000f + (float) i / particleCount) % 1f;
            float x = width * 0.1f + (width * 0.8f) * ((float) i / particleCount + 0.1f * (float) Math.sin(elapsed / 2000f + i));
            float y = height * 0.3f + height * 0.4f * (float) Math.sin(phase * Math.PI * 2);
            float size = 1.5f + 1.5f * Easing.pulse((elapsed / 1500f + i * 0.7f) % 1f);
            float alpha = 0.15f + 0.1f * Easing.pulse((elapsed / 2000f + i * 0.5f) % 1f);
            int col = (int) (alpha * 255) << 24 | 0xF0C040;
            gui.fill((int) x, (int) y, (int) (x + size), (int) (y + size), col);
        }
    }

    private void renderTitle(GuiGraphics gui, float alpha, long elapsed) {
        int a = (int) (alpha * 255);
        int cx = width / 2;

        gui.pose().pushPose();
        float titleScale = 1.8f;
        float s = 1f + (1f - alpha) * 0.3f;
        gui.pose().translate(cx, 28, 0);
        gui.pose().scale(titleScale * s, titleScale * s, 1);
        int titleColor = a << 24 | 0xF0C040;
        gui.drawCenteredString(font, "\u2726 Магазин китов \u2726", 0, 0, titleColor);
        gui.pose().popPose();

        float lineProgress = Easing.easeOutCubic(Mth.clamp((elapsed - 600f) / 500f, 0f, 1f));
        int lineW = (int) (160 * lineProgress);
        int lineY = 48;
        int lineCol = a << 24 | 0x3A7BB5;
        gui.fill(cx - lineW / 2, lineY, cx + lineW / 2, lineY + 2, lineCol);

        gui.pose().pushPose();
        gui.pose().translate(cx, 0, 0);
        gui.pose().scale(0.85f, 0.85f, 1);
        int subColor = a << 24 | 0x9AA0A6;
        gui.drawCenteredString(font, "\u00a77Выберите набор экипировки", 0, 60, subColor);
        gui.pose().popPose();
    }

    private void renderCards(GuiGraphics gui, int mx, int my, float alpha, long elapsed) {
        if (alpha <= 0.01f) return;

        int a = (int) (alpha * 255);
        int totalW = COLS * CARD_W + (COLS - 1) * CARD_GAP;
        int startX = (width - totalW) / 2;
        int startY = 80;

        for (int i = 0; i < kits.size(); i++) {
            KitItem kit = kits.get(i);
            int col = i % COLS;
            int row = i / COLS;

            float staggerDelay = 100f + i * 60f;
            float cardProgress = Mth.clamp((elapsed - staggerDelay) / 500f, 0f, 1f);
            float cardT = Easing.easeOutBack(cardProgress);
            float cardAlpha = Mth.clamp((elapsed - staggerDelay) / 300f, 0f, 1f);

            if (cardT <= 0.01f) continue;

            int x = startX + col * (CARD_W + CARD_GAP);
            int y = startY + row * (CARD_H + CARD_GAP) + (int) ((1f - cardT) * 40);

            boolean hovered = mx >= x && mx < x + CARD_W && my >= y && my < y + CARD_H;

            renderCard(gui, x, y, kit, hovered, a, cardAlpha, elapsed, i);
        }
    }

    private void renderCard(GuiGraphics gui, int x, int y, KitItem kit, boolean hovered, int parentAlpha, float cardAlpha, long elapsed, int index) {
        int a = (int) (cardAlpha * parentAlpha);
        if (a <= 0) return;

        float hoverGlow = hovered ? 1f : 0f;
        float glow = Mth.lerp(0.1f * Minecraft.getInstance().getFrameTime(), lastGlow[index], hoverGlow);
        lastGlow[index] = glow;

        int bgColor = parentAlpha << 24 | 0x1A1F2E;
        int borderColor;
        if (glow > 0.01f) {
            int r = (int) Mth.lerp(glow, 0x2D, 0xF0);
            int g = (int) Mth.lerp(glow, 0x31, 0xC0);
            int b = (int) Mth.lerp(glow, 0x42, 0x40);
            borderColor = a << 24 | (r << 16 | g << 8 | b);
        } else {
            borderColor = a << 24 | 0x2D3142;
        }

        float rarityPulse = 0.9f + 0.1f * Easing.pulse((elapsed + index * 200f) / 2000f % 1f);
        int rarityAccent = withMultipliedBrightness(kit.rarityColor, rarityPulse);

        gui.fill(x, y, x + CARD_W, y + CARD_H, bgColor);
        gui.renderOutline(x, y, CARD_W, CARD_H, borderColor);

        gui.pose().pushPose();
        float scale = 1f + glow * 0.03f;
        float originX = x + CARD_W / 2f;
        float originY = y + 30f;
        gui.pose().translate(originX, originY, 0);
        gui.pose().scale(scale, scale, 1);
        gui.drawCenteredString(font, kit.icon, 0, -8, a << 24 | kit.rarityColor);
        gui.pose().popPose();

        int nameCol = a << 24 | (hovered ? 0xF0C040 : 0xE8EAED);
        gui.drawCenteredString(font, kit.name, x + CARD_W / 2, y + 52, nameCol);

        gui.pose().pushPose();
        gui.pose().scale(0.7f, 0.7f, 1);
        int descCol = a << 24 | 0x9AA0A6;
        String desc = kit.description.length() > 20 ? kit.description.substring(0, 19) + ".." : kit.description;
        gui.drawCenteredString(font, desc, (int) ((x + CARD_W / 2) / 0.7f), (int) ((y + 66) / 0.7f), descCol);
        gui.pose().popPose();

        int priceColor = a << 24 | 0xF0C040;
        gui.drawCenteredString(font, "\u25CB " + kit.price, x + CARD_W / 2, y + 82, priceColor);

        boolean buyHovered = hovered;
        int btnY = y + CARD_H - 18;
        int btnCol = buyHovered ? (a << 24 | 0xF0C040) : (a << 24 | 0x2D5F8A);
        gui.fill(x + 10, btnY, x + CARD_W - 10, btnY + 14, btnCol);
        gui.drawCenteredString(font, "\u2714 Купить", x + CARD_W / 2, btnY + 3, a << 24 | 0xFFFFFF);
    }

    private final float[] lastGlow = new float[32];

    private int withMultipliedBrightness(int color, float mult) {
        int r = (int) Mth.clamp(((color >> 16) & 0xFF) * mult, 0, 255);
        int g = (int) Mth.clamp(((color >> 8) & 0xFF) * mult, 0, 255);
        int b = (int) Mth.clamp((color & 0xFF) * mult, 0, 255);
        return (color & 0xFF000000) | (r << 16) | (g << 8) | b;
    }

    private void renderCloseButton(GuiGraphics gui, int mx, int my, float alpha) {
        int a = (int) (alpha * 255);
        int btnX = width - 36;
        int btnY = 10;
        int btnSize = 24;
        boolean hovered = mx >= btnX && mx < btnX + btnSize && my >= btnY && my < btnY + btnSize;

        int bg = hovered ? (a << 24 | 0xE53935) : (a << 24 | 0x1E2433);
        int border = hovered ? (a << 24 | 0xE53935) : (a << 24 | 0x2D3142);
        gui.fill(btnX, btnY, btnX + btnSize, btnY + btnSize, bg);
        gui.renderOutline(btnX, btnY, btnSize, btnSize, border);
        gui.drawCenteredString(font, "\u2716", btnX + btnSize / 2, btnY + 7, a << 24 | 0xE8EAED);

        if (hovered && alpha > 0.5f) {
            lastCloseHover = Math.min(lastCloseHover + 0.15f, 1f);
        } else {
            lastCloseHover = Math.max(lastCloseHover - 0.05f, 0f);
        }

        if (lastCloseHover > 0.01f) {
            int glowCol = (int) (lastCloseHover * 80) << 24 | 0xE53935;
            gui.fill(btnX - 2, btnY - 2, btnX + btnSize + 2, btnY + btnSize + 2, glowCol);
        }
    }

    private float lastCloseHover;

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button == 0) {
            int btnX = width - 36;
            int btnY = 10;
            int btnSize = 24;
            if (mx >= btnX && mx < btnX + btnSize && my >= btnY && my < btnY + btnSize) {
                onClose();
                return true;
            }

            int totalW = COLS * CARD_W + (COLS - 1) * CARD_GAP;
            int startX = (width - totalW) / 2;
            int startY = 80;

            for (int i = 0; i < kits.size(); i++) {
                int col = i % COLS;
                int row = i / COLS;
                int x = startX + col * (CARD_W + CARD_GAP);
                int y = startY + row * (CARD_H + CARD_GAP);

                if (mx >= x && mx < x + CARD_W && my >= y && my < y + CARD_H) {
                    KitItem kit = kits.get(i);
                    int btnY2 = y + CARD_H - 18;
                    if (my >= btnY2 && my < btnY2 + 14) {
                        onBuyKit(kit);
                        return true;
                    }
                }
            }
        }
        return super.mouseClicked(mx, my, button);
    }

    private void onBuyKit(KitItem kit) {
        assert minecraft != null;
        minecraft.player.displayClientMessage(
            Component.literal("\u00a7e\u2714 Вы купили набор \u00a76" + kit.name + "\u00a7e за \u00a76" + kit.price + " \u00a7eмонет!"),
            true
        );
    }

    @Override
    public void onClose() {
        assert minecraft != null;
        minecraft.setScreen(null);
    }

    private static List<KitItem> createStubKits() {
        List<KitItem> list = new ArrayList<>();
        list.add(new KitItem("\u2694", "Штурмовик", "5.56 винтовка, гранаты", 1000, Theme.Colors.RARITY_COMMON));
        list.add(new KitItem("\u2764", "Медик", "Аптечка, бинты, дефиб", 800, Theme.Colors.SUCCESS));
        list.add(new KitItem("\u2316", "Снайпер", "DMR, маскировка, рация", 1500, Theme.Colors.RARITY_RARE));
        list.add(new KitItem("\u2699", "Поддержка", "LMG, ящик боеприпасов", 1200, Theme.Colors.RARITY_RARE));
        list.add(new KitItem("\u2726", "Инженер", "Инструменты, мины", 900, Theme.Colors.RARITY_EPIC));
        list.add(new KitItem("\u2606", "Разведчик", "Дрон, ИК-визор", 1100, Theme.Colors.RARITY_EPIC));
        list.add(new KitItem("\u265B", "Тяжёлый", "Экзоскелет, миниган", 2000, Theme.Colors.RARITY_LEGENDARY));
        list.add(new KitItem("\u26A1", "Сапёр", "Взрывчатка, C4, детонатор", 1300, Theme.Colors.RARITY_EPIC));
        list.add(new KitItem("\u2620", "Наёмник", "Трофейное оружие, щит", 1800, Theme.Colors.RARITY_LEGENDARY));
        return list;
    }

    public static class KitItem {
        public final String icon;
        public final String name;
        public final String description;
        public final int price;
        public final int rarityColor;

        public KitItem(String icon, String name, String description, int price, int rarityColor) {
            this.icon = icon;
            this.name = name;
            this.description = description;
            this.price = price;
            this.rarityColor = rarityColor;
        }
    }
}
