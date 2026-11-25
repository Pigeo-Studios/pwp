package com.pwp.coreclient.ui;

import com.pwp.coreclient.gui.theme.PWPTheme.Animations;
import com.pwp.coreclient.gui.theme.PWPTheme.Colors;
import com.pwp.coreclient.gui.theme.PWPTheme.Spacing;
import com.pwp.coreclient.gui.theme.PWPTheme.Styles;
import com.pwp.coreclient.ui.layout.Rect;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * Выбираемая карточка (кит, режим, карта, матч): те же состояния и анимации, что у
 * {@link UIButton} (hover / pressed / selected / disabled, акцентная полоса), но контент —
 * картинка или иконка слева, заголовок, подзаголовок и бейдж справа. Всё обрезается «…» по
 * ширине карточки.
 *
 * <ul>
 *   <li>{@link #badgeWidth} — бейдж в колонке ФИКСИРОВАННОЙ ширины (счётчики голосов/игроков
 *       не дёргают заголовок при смене числа);
 *   <li>{@link #marked} — галочка у бейджа (например «ваш голос»);
 *   <li>{@link #share} — тонкая полоса доли снизу (доля голосов), плавно догоняет значение;
 *   <li>{@link #image} — картинка (превью карты, флаг) с сохранением пропорций.
 * </ul>
 */
@OnlyIn(Dist.CLIENT)
public class UICard extends UIButton {
    private Component subtitle;
    private Component badge;
    private int badgeWidth;
    private boolean marked;
    private int iconTint = Styles.Control.ICON;
    private boolean iconTintFixed;
    private ResourceLocation image;
    private int imageW;
    private int imageH;
    private boolean hasShare;
    private int shareColor = Styles.Loader.FILL;
    private final UIAnim share = new UIAnim(0F, Animations.DURATION_SLOW, UIAnim.EASE_OUT_CUBIC);

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

    /** Фиксированная ширина колонки бейджа (0 — по тексту). */
    public UICard badgeWidth(int width) {
        this.badgeWidth = Math.max(0, width);
        return this;
    }

    /** Галочка у бейджа. */
    public UICard marked(boolean marked) {
        this.marked = marked;
        return this;
    }

    /** Иконка с фиксированным цветом (не меняется при наведении). */
    public UICard icon(UIIcon icon, int tint) {
        this.icon = icon;
        this.iconTint = tint;
        this.iconTintFixed = true;
        return this;
    }

    /**
     * Картинка слева вместо иконки; {@code w:h} — пропорции (вписывается по высоте контента).
     * null — убрать.
     */
    public UICard image(ResourceLocation image, int w, int h) {
        this.image = image;
        this.imageW = Math.max(1, w);
        this.imageH = Math.max(1, h);
        return this;
    }

    /** Полоса доли 0..1 у нижней кромки (анимированно). */
    public UICard share(float value, int color) {
        this.hasShare = true;
        this.shareColor = color;
        share.to(Math.max(0F, Math.min(1F, value)));
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
        if (image != null && inner.h() >= iconSize) {
            int ih = inner.h();
            int iw = Math.max(1, ih * imageW / imageH);
            if (iw < inner.w() / 3) {
                Rect r = Rect.of(x, inner.y(), iw, ih);
                UIDraw.image(g, image, r, UIDraw.fade(enabled ? 0xFFFFFFFF : Styles.Control.TEXT_DISABLED, opacity));
                UIDraw.outline(g, r, UIDraw.fade(Styles.Control.BORDER, opacity));
                x += iw + Spacing.SM;
            }
        } else if (icon != null && inner.h() >= iconSize && inner.w() > 3 * iconSize) {
            int tint = iconTintFixed
                    ? UIDraw.fade(enabled ? iconTint : Styles.Control.TEXT_DISABLED, opacity)
                    : contentIconColor;
            UIDraw.icon(g, icon, x, inner.y() + (inner.h() - iconSize) / 2, iconSize, tint);
            x += iconSize + Spacing.SM;
        }
        int right = inner.right();
        if (badge != null || badgeWidth > 0) {
            int col = badgeWidth > 0 ? badgeWidth : font.width(badge);
            int markW = marked ? iconSize + Spacing.XXS : 0;
            if (col + markW < (right - x) / 2) {
                int by = inner.y() + (inner.h() - font.lineHeight) / 2 + 1;
                int badgeColor = selected || marked ? Styles.Control.ACCENT_BAR : Styles.Control.SUBTEXT;
                int color = UIDraw.fade(enabled ? badgeColor : textColor, opacity);
                if (badge != null) {
                    Component b = UIDraw.ellipsize(font, badge, col);
                    UIDraw.text(g, font, b, right - font.width(b), by, color, true); // по правому краю колонки
                }
                if (marked) {
                    UIDraw.icon(
                            g,
                            UIIcon.CHECK,
                            right - col - markW,
                            inner.y() + (inner.h() - iconSize) / 2,
                            iconSize,
                            UIDraw.fade(Styles.Control.ACCENT_BAR, opacity));
                }
                right -= col + markW + Spacing.SM;
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
        if (hasShare) {
            Rect track = bounds().inset(Spacing.HAIRLINE).bottomStrip(Spacing.SHARE_BAR_H);
            UIDraw.rect(g, track, UIDraw.fade(Styles.Loader.TRACK, opacity));
            int fw = Math.round(track.w() * share.value());
            UIDraw.rect(g, track.withWidth(fw), UIDraw.fade(shareColor, opacity));
        }
    }
}
