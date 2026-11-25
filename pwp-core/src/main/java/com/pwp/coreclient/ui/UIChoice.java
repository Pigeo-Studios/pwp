package com.pwp.coreclient.ui;

import com.pwp.coreclient.gui.theme.PWPTheme.Animations;
import com.pwp.coreclient.gui.theme.PWPTheme.Colors;
import com.pwp.coreclient.gui.theme.PWPTheme.Spacing;
import com.pwp.coreclient.gui.theme.PWPTheme.Styles;
import com.pwp.coreclient.ui.layout.Rect;
import java.util.function.IntConsumer;
import java.util.function.Supplier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * Выбор варианта «‹ значение ›»: клик по левой половине — предыдущий, по правой — следующий
 * (по кругу). Стекло-плашка как у кнопки, подсветка стрелки под курсором, подпись — «…» по
 * ширине. Значение хранит владелец ({@code current}), {@code cycle} получает -1/+1.
 */
@OnlyIn(Dist.CLIENT)
public class UIChoice extends UIWidget {
    private static final Component LEFT = Component.literal("‹");
    private static final Component RIGHT = Component.literal("›");
    private final Supplier<Component> current;
    private final IntConsumer cycle;
    private final UIAnim hover = new UIAnim(0F, Animations.DURATION_FAST, UIAnim.EASE_OUT_CUBIC);

    public UIChoice(Supplier<Component> current, IntConsumer cycle) {
        super(0, 0, 0, Spacing.SETTING_ROW_H);
        this.current = current;
        this.cycle = cycle;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        float o = opacity;
        boolean hovered = enabled && isMouseOver(mouseX, mouseY);
        float h = hover.to(hovered ? 1F : 0F).value();
        int bh = Math.min(height, Spacing.BUTTON_SMALL_HEIGHT);
        Rect r = Rect.of(x, y + (height - bh) / 2, width, bh);
        int bg = enabled ? Colors.lerp(Styles.Control.BG, Styles.Control.BG_HOVER, h) : Styles.Control.BG_DISABLED;
        UIDraw.rect(g, r, UIDraw.fade(bg, o));
        int border = enabled
                ? Colors.lerp(Styles.Control.BORDER, Styles.Control.BORDER_HOVER, h)
                : Styles.Control.BORDER_DISABLED;
        UIDraw.outline(g, r, UIDraw.fade(border, o));
        var font = font();
        int aw = Spacing.CHOICE_ARROW_W;
        boolean leftHover = hovered && mouseX < x + width / 2;
        int ty = r.y() + (r.h() - font.lineHeight) / 2 + 1;
        int arrowBase = enabled ? Styles.Setting.ARROW : Styles.Control.TEXT_DISABLED;
        int lc = hovered && leftHover ? Styles.Setting.ARROW_HOVER : arrowBase;
        int rc = hovered && !leftHover ? Styles.Setting.ARROW_HOVER : arrowBase;
        UIDraw.text(g, font, LEFT, r.x() + (aw - font.width(LEFT)) / 2, ty, UIDraw.fade(lc, o), false);
        UIDraw.text(g, font, RIGHT, r.right() - aw + (aw - font.width(RIGHT)) / 2, ty, UIDraw.fade(rc, o), false);
        Component shown = UIDraw.ellipsize(font, current.get(), Math.max(0, width - 2 * aw));
        int tc = enabled ? Styles.Setting.VALUE : Styles.Control.TEXT_DISABLED;
        UIDraw.text(g, font, shown, r.centerX() - font.width(shown) / 2, ty, UIDraw.fade(tc, o), false);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (!enabled || (button != 0 && button != 1) || !isMouseOver(mx, my)) return false;
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
        // ПКМ — всегда назад; ЛКМ — по половине
        cycle.accept(button == 1 || mx < x + width / 2.0 ? -1 : 1);
        return true;
    }
}
