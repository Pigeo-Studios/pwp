package com.pwp.coreclient.ui;

import com.pwp.coreclient.gui.theme.PWPTheme.Animations;
import com.pwp.coreclient.gui.theme.PWPTheme.Colors;
import com.pwp.coreclient.gui.theme.PWPTheme.Spacing;
import com.pwp.coreclient.gui.theme.PWPTheme.Styles;
import com.pwp.coreclient.ui.layout.Align;
import com.pwp.coreclient.ui.layout.Rect;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * Кнопка UI-кита в тактическом стиле: полупрозрачная заливка + вертикальный блик + 1px
 * обводка, акцентная полоса слева растёт при наведении/выборе. Состояния hover / pressed /
 * selected / disabled с плавными переходами {@link UIAnim} (реальное время).
 *
 * <p>Стили: ACCENT — главная (градиент акцента), DANGER — опасное действие, PRIMARY/DARK —
 * обычные, GHOST — без фона до наведения. Иконка ({@link #icon}) и выравнивание подписи
 * ({@link #align}) — опционально. Клик срабатывает на нажатии (как раньше).
 */
@OnlyIn(Dist.CLIENT)
public class UIButton extends UIWidget {
    public enum Style {
        PRIMARY,
        ACCENT,
        DANGER,
        DARK,
        GHOST
    }

    protected Component label;
    protected Style style = Style.PRIMARY;
    protected Runnable onPress;
    protected UIIcon icon;
    protected Align align = Align.CENTER;
    protected boolean selected;
    protected boolean pressed;

    protected final UIAnim hover = new UIAnim(0F, Animations.DURATION_FAST, UIAnim.EASE_OUT_CUBIC);
    protected final UIAnim press = new UIAnim(0F, Animations.DURATION_PRESS, UIAnim.EASE_OUT_CUBIC);
    protected final UIAnim select = new UIAnim(0F, Animations.DURATION_NORMAL, UIAnim.EASE_OUT_CUBIC);
    protected final UIAnim enable = new UIAnim(1F, Animations.DURATION_NORMAL, UIAnim.EASE_OUT_CUBIC);

    /** Цвет иконки и сдвиг контента текущего кадра — для {@link #renderContent}. */
    protected int contentIconColor;

    protected int contentShift;

    public UIButton(int x, int y, int width, int height, Component label, Runnable onPress) {
        super(x, y, width, height);
        this.label = label;
        this.onPress = onPress;
    }

    /** Кнопка стандартной высоты {@link Spacing#BUTTON_HEIGHT}. */
    public UIButton(int x, int y, int width, Component label, Runnable onPress) {
        this(x, y, width, Spacing.BUTTON_HEIGHT, label, onPress);
    }

    /** Кнопка без позиции — положение задаёт лейаут через {@link #setBounds(Rect)}. */
    public UIButton(Component label, Runnable onPress) {
        this(0, 0, 0, Spacing.BUTTON_HEIGHT, label, onPress);
    }

    public UIButton style(Style style) {
        this.style = style;
        return this;
    }

    public UIButton label(Component label) {
        this.label = label;
        return this;
    }

    public Component label() {
        return label;
    }

    public UIButton icon(UIIcon icon) {
        this.icon = icon;
        return this;
    }

    /** START — подпись слева (меню), CENTER — по центру. */
    public UIButton align(Align align) {
        this.align = align == Align.END ? Align.END : (align == Align.START ? Align.START : Align.CENTER);
        return this;
    }

    public UIButton selected(boolean selected) {
        this.selected = selected;
        return this;
    }

    public boolean isSelected() {
        return selected;
    }

    public boolean isPressed() {
        return pressed;
    }

    /** Ширина «по контенту»: иконка + подпись + поля. */
    public int preferredWidth() {
        int w = font().width(label) + 2 * Spacing.MD + Spacing.ACCENT_BAR_SELECTED;
        if (icon != null) w += Spacing.ICON_SIZE + Spacing.SM;
        return w;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        boolean hovered = enabled && isMouseOver(mouseX, mouseY);
        if (!enabled) pressed = false;
        float h = hover.to(hovered ? 1F : 0F).value();
        float p = press.to(pressed && hovered ? 1F : 0F).value();
        float s = select.to(selected ? 1F : 0F).value();
        float e = enable.to(enabled ? 1F : 0F).value();
        float o = opacity;
        Rect r = bounds();

        int text;
        switch (style) {
            case ACCENT -> text = renderFilled(
                    g,
                    r,
                    h,
                    p,
                    e,
                    Styles.Control.PRIMARY_TOP,
                    Styles.Control.PRIMARY_BOTTOM,
                    Styles.Control.PRIMARY_TOP_HOVER,
                    Styles.Control.PRIMARY_BOTTOM_HOVER,
                    Styles.Control.PRIMARY_BORDER,
                    Styles.Control.PRIMARY_TEXT);
            case DANGER -> text = renderFilled(
                    g,
                    r,
                    h,
                    p,
                    e,
                    Styles.Control.DANGER_TOP,
                    Styles.Control.DANGER_BOTTOM,
                    Styles.Button.DANGER_HOVER,
                    Styles.Button.DANGER_PRESSED,
                    Styles.Control.DANGER_BORDER,
                    Styles.Control.TEXT_HOVER);
            default -> text = renderGlass(g, r, h, p, s, e);
        }
        contentShift = Math.round(Spacing.PRESS_SHIFT * p);
        renderContent(g, UIDraw.fade(text, o));
    }

    /** Залитая кнопка (ACCENT/DANGER): вертикальный градиент, светлая кромка сверху. */
    private int renderFilled(
            GuiGraphics g,
            Rect r,
            float h,
            float p,
            float e,
            int top,
            int bottom,
            int topHover,
            int bottomHover,
            int border,
            int text) {
        float o = opacity;
        int t = Colors.lerp(top, topHover, h);
        int b = Colors.lerp(bottom, bottomHover, h);
        // нажатие — темнее, выключено — серое
        t = Colors.lerp(t, Styles.Control.BG_PRESSED, p * 0.35F);
        b = Colors.lerp(b, Styles.Control.BG_PRESSED, p * 0.35F);
        t = Colors.lerp(Styles.Control.BG_DISABLED, t, e);
        b = Colors.lerp(Styles.Control.BG_DISABLED, b, e);
        int br = Colors.lerp(Styles.Control.BORDER_DISABLED, Colors.lerp(Colors.withAlpha(border, 0x99), border, h), e);
        UIDraw.gradientV(g, r, UIDraw.fade(t, o), UIDraw.fade(b, o));
        UIDraw.hline(g, r.x() + 1, r.right() - 1, r.y() + 1, UIDraw.fade(Styles.Glass.HIGHLIGHT, o * e));
        UIDraw.outline(g, r, UIDraw.fade(br, o));
        contentIconColor = UIDraw.fade(Colors.lerp(Styles.Control.TEXT_DISABLED, text, e), o);
        return Colors.lerp(Styles.Control.TEXT_DISABLED, text, e);
    }

    /** Стеклянная кнопка (PRIMARY/DARK/GHOST) с акцентной полосой. */
    private int renderGlass(GuiGraphics g, Rect r, float h, float p, float s, float e) {
        float o = opacity;
        boolean ghost = style == Style.GHOST;
        int base = ghost ? Colors.withAlpha(Styles.Control.BG, 0) : Styles.Control.BG;
        int bg = Colors.lerp(base, Styles.Control.BG_HOVER, h);
        bg = Colors.lerp(bg, Styles.Control.BG_SELECTED, s);
        bg = Colors.lerp(bg, Styles.Control.BG_PRESSED, p);
        bg = Colors.lerp(Styles.Control.BG_DISABLED, bg, e);
        int borderBase = ghost ? Colors.withAlpha(Styles.Control.BORDER, 0) : Styles.Control.BORDER;
        int border = Colors.lerp(borderBase, Styles.Control.BORDER_HOVER, h);
        border = Colors.lerp(border, Styles.Control.BORDER_SELECTED, s);
        border = Colors.lerp(Styles.Control.BORDER_DISABLED, border, e);

        UIDraw.rect(g, r, UIDraw.fade(bg, o));
        float sheen = ghost ? Math.max(h, s) : 1F;
        UIDraw.gradientV(
                g,
                r,
                UIDraw.fade(Styles.Control.SHEEN_TOP, o * sheen * e),
                UIDraw.fade(Styles.Control.SHEEN_BOTTOM, o));
        UIDraw.outline(g, r, UIDraw.fade(border, o));

        float bar = Math.max(h * Spacing.ACCENT_BAR, s * Spacing.ACCENT_BAR_SELECTED) * e;
        int barW = Math.round(bar);
        if (barW > 0) {
            UIDraw.accentBar(g, r.x(), r.y(), r.h(), barW, UIDraw.fade(Styles.Control.ACCENT_BAR, o));
        }

        int textBase = ghost ? Styles.Control.SUBTEXT : Styles.Control.TEXT;
        int text = Colors.lerp(textBase, Styles.Control.TEXT_HOVER, Math.max(h, s));
        text = Colors.lerp(Styles.Control.TEXT_DISABLED, text, e);
        int iconColor = Colors.lerp(Styles.Control.ICON, Styles.Control.ICON_HOVER, Math.max(h, s));
        contentIconColor = UIDraw.fade(Colors.lerp(Styles.Control.TEXT_DISABLED, iconColor, e), o);
        return text;
    }

    /** Содержимое кнопки: иконка (если влезает) + подпись с «…» по ширине. */
    protected void renderContent(GuiGraphics g, int textColor) {
        var font = font();
        int pad = Spacing.SM + (align == Align.CENTER ? 0 : Spacing.ACCENT_BAR_SELECTED);
        Rect inner = bounds().inset(pad, 0, Spacing.SM, 0).translate(0, contentShift);
        int iconSize = Spacing.ICON_SIZE;
        boolean showIcon = icon != null && height >= iconSize + 2 * Spacing.XXS && inner.w() > iconSize * 2;
        int iconBlock = showIcon ? iconSize + Spacing.SM : 0;
        Component shown = UIDraw.ellipsize(font, label, inner.w() - iconBlock);
        int contentW = iconBlock + font.width(shown);
        int startX = inner.x() + align.offset(inner.w(), contentW);
        int textY = inner.y() + (height - font.lineHeight) / 2 + 1;
        if (showIcon) {
            UIDraw.icon(g, icon, startX, inner.y() + (height - iconSize) / 2, iconSize, contentIconColor);
        }
        UIDraw.text(g, font, shown, startX + iconBlock, textY, textColor, style != Style.ACCENT);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (!enabled || button != 0 || !isMouseOver(mx, my)) return false;
        pressed = true;
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
        if (onPress != null) onPress.run();
        return true;
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        boolean was = pressed;
        if (button == 0) pressed = false;
        return was && button == 0;
    }
}
