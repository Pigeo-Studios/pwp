package com.pwp.coreclient.ui;

import com.pwp.coreclient.gui.theme.PWPTheme.Colors;
import com.pwp.coreclient.gui.theme.PWPTheme.Spacing;
import com.pwp.coreclient.gui.theme.PWPTheme.Styles;
import com.pwp.coreclient.ui.layout.Rect;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * Выбираемая карточка (кит, режим, карта): те же состояния и анимации, что у {@link UIButton}
 * (hover / pressed / selected / disabled, акцентная полоса), но контент — иконка слева
 * (цвет задаётся, например {@link UIIcon#roleColor()}), заголовок, подзаголовок и бейдж
 * справа. Всё обрезается «…» по ширине карточки. Высота для двух строк —
 * {@link #preferredHeight()}.
 */
@OnlyIn(Dist.CLIENT)
public class UICard extends UIButton {
    private Component subtitle;
    private Component badge;
    private int iconTint = Styles.Control.ICON;
    private boolean iconTintFixed;

    public UICard(Component title, Runnable onPress) {
        super(title, onPress);
        style(Style.PRIMARY);
    }

    public UICard subtitle(Component subtitle) {
        this.subtitle = subtitle;
        return this;
    }

    /** Короткая метка справа (например «2/6»). */
    public UICard badge(Component badge) {
        this.badge = badge;
        return this;
    }

    /** Иконка с фиксированным цветом (не меняется при наведении). */
    public UICard icon(UIIcon icon, int tint) {
        this.icon = icon;
        this.iconTint = tint;
        this.iconTintFixed = true;
        return this;
    }

    /** Высота карточки с заголовком и подзаголовком. */
    public static int preferredHeight() {
        var font = font();
        return 2 * Spacing.SM + 2 * font.lineHeight + Spacing.XXS;
    }

    @Override
    protected void renderContent(GuiGraphics g, int textColor) {
        var font = font();
        Rect inner = bounds().inset(Spacing.SM + Spacing.ACCENT_BAR_SELECTED, Spacing.XS, Spacing.SM, Spacing.XS)
                .translate(0, contentShift);
        int x = inner.x();
        int iconSize = Spacing.ICON_SIZE;
        if (icon != null && inner.h() >= iconSize && inner.w() > 3 * iconSize) {
            int tint = iconTintFixed
                    ? UIDraw.fade(enabled ? iconTint : Styles.Control.TEXT_DISABLED, opacity)
                    : contentIconColor;
            UIDraw.icon(g, icon, x, inner.y() + (inner.h() - iconSize) / 2, iconSize, tint);
            x += iconSize + Spacing.SM;
        }
        int right = inner.right();
        if (badge != null) {
            int bw = font.width(badge);
            if (bw < (right - x) / 2) {
                int by = inner.y() + (inner.h() - font.lineHeight) / 2 + 1;
                int badgeColor = selected ? Styles.Control.ACCENT_BAR : Styles.Control.SUBTEXT;
                UIDraw.text(
                        g, font, badge, right - bw, by, UIDraw.fade(enabled ? badgeColor : textColor, opacity), true);
                right -= bw + Spacing.SM;
            }
        }
        int maxW = Math.max(0, right - x);
        boolean two = subtitle != null && inner.h() >= 2 * font.lineHeight;
        int blockH = two ? 2 * font.lineHeight + Spacing.XXS : font.lineHeight;
        int ty = inner.y() + (inner.h() - blockH) / 2 + 1;
        UIDraw.text(g, font, UIDraw.ellipsize(font, label, maxW), x, ty, textColor, true);
        if (two) {
            int sub = Colors.lerp(Styles.Control.TEXT_DISABLED, Styles.Control.SUBTEXT, enabled ? 1F : 0F);
            UIDraw.text(
                    g,
                    font,
                    UIDraw.ellipsize(font, subtitle, maxW),
                    x,
                    ty + font.lineHeight + Spacing.XXS,
                    UIDraw.fade(sub, opacity),
                    false);
        }
    }
}
