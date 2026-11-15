package com.pwp.coreclient.ui;

import com.pwp.coreclient.gui.theme.PWPTheme.Spacing;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * Квадратная кнопка-иконка: текстура (ResourceLocation, рисуется целиком в {@code iconSize}) или
 * символ из {@code PWPIcons}. Подпись — в тултипе.
 */
@OnlyIn(Dist.CLIENT)
public class UIIconButton extends UIButton {
    private final ResourceLocation texture;
    private final String glyph;
    private final int iconSize;

    private UIIconButton(int x, int y, int size, ResourceLocation texture, String glyph, Runnable onPress) {
        super(x, y, size, size, Component.empty(), onPress);
        this.texture = texture;
        this.glyph = glyph;
        this.iconSize = Math.max(8, size - 2 * Spacing.XS);
        style(Style.DARK);
    }

    public static UIIconButton texture(int x, int y, int size, ResourceLocation texture, Runnable onPress) {
        return new UIIconButton(x, y, size, texture, null, onPress);
    }

    public static UIIconButton glyph(int x, int y, int size, String glyph, Runnable onPress) {
        return new UIIconButton(x, y, size, null, glyph, onPress);
    }

    @Override
    protected void renderContent(GuiGraphics g, int textColor) {
        if (texture != null) {
            int ix = x + (width - iconSize) / 2;
            int iy = y + (height - iconSize) / 2;
            g.blit(texture, ix, iy, 0, 0, iconSize, iconSize, iconSize, iconSize);
        } else if (glyph != null) {
            UIDraw.centered(g, font(), Component.literal(glyph), x + width / 2, y + height / 2 + 1, textColor);
        }
    }
}
