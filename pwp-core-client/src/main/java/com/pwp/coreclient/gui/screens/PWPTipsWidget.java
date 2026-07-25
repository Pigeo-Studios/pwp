package com.pwp.coreclient.gui.screens;

import com.pwp.coreclient.gui.animations.Easing;
import com.pwp.coreclient.gui.theme.PWPTheme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.Random;

public class PWPTipsWidget {

    private static final List<String> TIPS = List.of(
        "Используйте тактическое оборудование для победы",
        "Связь с отрядом — ключ к успеху",
        "Следите за уровнем брони и здоровья",
        "Захватывайте точки, чтобы получить преимущество",
        "Техника уязвима с тыла и флангов",
        "РЎРѕРІРµС‚: РёСЃРїРѕР»СЊР·СѓР№С‚Рµ СѓРєСЂС‹С‚РёСЏ С‡С‚РѕР±С‹ РёР·Р±РµР¶Р°С‚СЊ РѕРіРЅСЏ",
        "Аптечки восстанавливают здоровье",
        "Боеприпасы можно пополнить на точке",
        "РЎРѕРІРµС‚: РІРЅРёРјР°С‚РµР»СЊРЅРѕ СЃР»СѓС€Р°Р№С‚Рµ С€Р°РіРё РїСЂРѕС‚РёРІРЅРёРєР°",
        "Не забывайте перезаряжаться перед боем",
        "РЎРѕРІРµС‚: РёСЃРїРѕР»СЊР·СѓР№С‚Рµ РґС‹РјРѕРІС‹Рµ РіСЂР°РЅР°С‚С‹ РґР»СЏ РїСЂРёРєСЂС‹С‚РёСЏ",
        "РЎРѕРІРµС‚: РјР°СЂРєРµСЂС‹ РЅР° РєР°СЂС‚Рµ РїРѕРјРѕРіСѓС‚ РєРѕРѕСЂРґРёРЅР°С†РёРё",
        "РЎРѕРІРµС‚: СЌРєРёРїРёСЂРѕРІРєР° РІР»РёСЏРµС‚ РЅР° СЃРєРѕСЂРѕСЃС‚СЊ РїРµСЂРµРґРІРёР¶РµРЅРёСЏ",
        "РЎРѕРІРµС‚: РЅРѕС‡РЅРѕРµ РІСЂРµРјСЏ СЃРЅРёР¶Р°РµС‚ РІРёРґРёРјРѕСЃС‚СЊ вЂ” РёСЃРїРѕР»СЊР·СѓР№С‚Рµ РџРќР’",
        "РЎРѕРІРµС‚: РїРѕРґРґРµСЂР¶РёРІР°Р№С‚Рµ СЃРѕСЋР·РЅРёРєРѕРІ РѕРіРЅС‘Рј"
    );

    private static final long SHOW_MS = 4000;
    private static final long FADE_MS = 300;

    private final Random random = new Random();
    private final int maxTextWidth;

    private int currentIndex;
    private long stateStartTime;
    private boolean fading;

    public PWPTipsWidget(int maxTextWidth) {
        this.maxTextWidth = maxTextWidth;
        this.currentIndex = random.nextInt(TIPS.size());
        this.stateStartTime = System.currentTimeMillis();
        this.fading = false;
    }

    public void tick(long now) {
        long elapsed = now - stateStartTime;

        if (!fading && elapsed > SHOW_MS) {
            fading = true;
            stateStartTime = now;
        } else if (fading && elapsed > FADE_MS) {
            fading = false;
            int next;
            do {
                next = random.nextInt(TIPS.size());
            } while (next == currentIndex);
            currentIndex = next;
            stateStartTime = now;
        }
    }

    public void render(GuiGraphics gui, int cx, int y) {
        long now = System.currentTimeMillis();
        tick(now);

        float alpha;
        if (fading) {
            float fadeProgress = Math.min((now - stateStartTime) / (float) FADE_MS, 1);
            alpha = 1.0f - Easing.easeOutCubic(fadeProgress);
        } else {
            alpha = 1.0f;
        }

        var font = PWPTheme.Fonts.display();
        int color = PWPUtils.withAlpha(PWPTheme.Colors.TEXT_SECONDARY, (int) (alpha * 180));
        String text = TIPS.get(currentIndex);

        if (font.width(text) > maxTextWidth) {
            var lines = font.split(Component.literal(text), maxTextWidth);
            int lineCount = Math.min(lines.size(), 2);
            for (int i = 0; i < lineCount; i++) {
                gui.drawString(font, lines.get(i), cx - maxTextWidth / 2, y + i * 10, color, false);
            }
        } else {
            gui.drawString(font, Component.literal(text), cx - font.width(text) / 2, y, color, false);
        }
    }
}
