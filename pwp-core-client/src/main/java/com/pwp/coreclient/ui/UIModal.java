package com.pwp.coreclient.ui;

import com.pwp.coreclient.gui.theme.PWPTheme.Animations;
import com.pwp.coreclient.gui.theme.PWPTheme.Spacing;
import com.pwp.coreclient.gui.theme.PWPTheme.Styles;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.lwjgl.glfw.GLFW;

/**
 * Модальное окно (подтверждение, ошибка): затемняет экран, перехватывает все события, кнопки
 * внизу. Размер — от размера экрана ({@link UIMath#scaled}), текст переносится.
 */
@OnlyIn(Dist.CLIENT)
public class UIModal extends UIWidget {
    private final Component title;
    private final Component message;
    private final List<UIButton> buttons = new ArrayList<>();
    private final UIAnim open = new UIAnim(0F, Animations.DURATION_NORMAL, UIAnim.EASE_OUT_CUBIC);
    private Runnable onClose;
    private int screenW;
    private int screenH;

    public UIModal(Component title, Component message) {
        super(0, 0, 0, 0);
        this.title = title;
        this.message = message;
        visible = false;
    }

    /** Добавить кнопку; после нажатия окно закрывается. */
    public UIModal button(Component label, UIButton.Style style, Runnable action) {
        buttons.add(new UIButton(0, 0, Spacing.BUTTON_WIDTH, label, () -> {
                    close();
                    if (action != null) action.run();
                })
                .style(style));
        return this;
    }

    public UIModal onClose(Runnable onClose) {
        this.onClose = onClose;
        return this;
    }

    public void show(int screenW, int screenH) {
        this.screenW = screenW;
        this.screenH = screenH;
        int w = UIMath.scaled(screenW, screenH, 0.6F, 200, 360);
        List<FormattedCharSequence> lines = font().split(message, w - 2 * Spacing.PANEL_PADDING);
        int h = Spacing.PANEL_PADDING * 3
                + font().lineHeight
                + Spacing.SM
                + lines.size() * (font().lineHeight + Spacing.XXS)
                + Spacing.BUTTON_HEIGHT;
        setBounds((screenW - w) / 2, (screenH - h) / 2, w, h);
        layoutButtons();
        open.snap(0F);
        open.to(1F);
        visible = true;
    }

    private void layoutButtons() {
        int gap = Spacing.SM;
        int total = 0;
        for (UIButton b : buttons) total += b.width() + gap;
        total -= gap;
        int bx = x + (width - total) / 2;
        int by = y + height - Spacing.PANEL_PADDING - Spacing.BUTTON_HEIGHT;
        for (UIButton b : buttons) {
            b.setBounds(bx, by, b.width(), Spacing.BUTTON_HEIGHT);
            bx += b.width() + gap;
        }
    }

    public void close() {
        if (!visible) return;
        visible = false;
        if (onClose != null) onClose.run();
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        float a = open.value();
        g.pose().pushPose();
        g.pose().translate(0, 0, 300);
        g.fill(0, 0, screenW, screenH, UIDraw.fade(Styles.Modal.OVERLAY, a));
        int slide = Math.round((1F - a) * Spacing.LG);
        g.pose().translate(0, slide, 0);
        UIDraw.box(g, x, y, width, height, Spacing.RADIUS_MEDIUM, Styles.Modal.BG, Styles.Modal.BORDER);
        int p = Spacing.PANEL_PADDING;
        g.drawString(font(), title, x + p, y + p, Styles.Modal.TITLE, true);
        int ty = y + p + font().lineHeight + Spacing.SM;
        for (FormattedCharSequence line : font().split(message, width - 2 * p)) {
            g.drawString(font(), line, x + p, ty, Styles.Modal.TEXT, false);
            ty += font().lineHeight + Spacing.XXS;
        }
        for (UIButton b : buttons) b.render(g, mouseX, mouseY - slide, partialTick);
        g.pose().popPose();
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (!visible) return false;
        for (UIButton b : new ArrayList<>(buttons)) {
            if (b.mouseClicked(mx, my, button)) return true;
        }
        return true; // модалка съедает клики мимо
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        return visible;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!visible) return false;
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) close();
        return true;
    }
}
