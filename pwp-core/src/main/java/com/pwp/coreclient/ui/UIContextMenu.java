package com.pwp.coreclient.ui;

import com.pwp.coreclient.gui.theme.PWPTheme.Animations;
import com.pwp.coreclient.gui.theme.PWPTheme.Colors;
import com.pwp.coreclient.gui.theme.PWPTheme.Spacing;
import com.pwp.coreclient.gui.theme.PWPTheme.Styles;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * Контекстное меню (ПКМ по игроку в списке отряда, по метке на карте): вертикальный список
 * {@link UIAction} у курсора, не вылезает за экран, закрывается кликом мимо или после выбора.
 */
@OnlyIn(Dist.CLIENT)
public class UIContextMenu extends UIWidget {
    private final List<UIAction> actions = new ArrayList<>();
    private final UIAnim open = new UIAnim(0F, Animations.DURATION_FAST, UIAnim.EASE_OUT_CUBIC);

    public UIContextMenu() {
        super(0, 0, 0, 0);
        visible = false;
    }

    private static int itemHeight() {
        return font().lineHeight + 2 * Spacing.XS;
    }

    /** Открыть у точки (mx, my) в пределах экрана. */
    public void openAt(int mx, int my, int screenW, int screenH, List<UIAction> list) {
        actions.clear();
        actions.addAll(list);
        int w = Spacing.BUTTON_WIDTH;
        for (UIAction a : actions) w = Math.max(w, font().width(a.label()) + 2 * Spacing.SM);
        int h = actions.size() * itemHeight() + 2 * Spacing.XXS;
        setBounds(UIMath.keepInside(mx, w, screenW, Spacing.XS), UIMath.keepInside(my, h, screenH, Spacing.XS), w, h);
        open.snap(0F);
        open.to(1F);
        visible = !actions.isEmpty();
    }

    public void close() {
        visible = false;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        float a = open.value();
        int slide = Math.round((1F - a) * Spacing.XS);
        UIDraw.box(
                g,
                x,
                y - slide,
                width,
                height,
                Spacing.RADIUS_SMALL,
                UIDraw.fade(Styles.Modal.BG, a),
                UIDraw.fade(Styles.Modal.BORDER, a));
        int ih = itemHeight();
        for (int i = 0; i < actions.size(); i++) {
            UIAction act = actions.get(i);
            int iy = y - slide + Spacing.XXS + i * ih;
            boolean hovered = mouseX >= x && mouseX < x + width && mouseY >= iy && mouseY < iy + ih;
            if (hovered && act.enabled())
                g.fill(x + 1, iy, x + width - 1, iy + ih, UIDraw.fade(Styles.Modal.MENU_ITEM_HOVER, a));
            int color = act.enabled() ? Styles.Modal.TEXT : Colors.TEXT_DIM;
            g.drawString(font(), act.label(), x + Spacing.SM, iy + Spacing.XS, UIDraw.fade(color, a), false);
        }
    }

    /** Наведённый пункт (для тултипа причины недоступности) или null. */
    public UIAction hovered(double mx, double my) {
        if (!isMouseOver(mx, my)) return null;
        int i = (int) ((my - y - Spacing.XXS) / itemHeight());
        return i >= 0 && i < actions.size() ? actions.get(i) : null;
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (!visible) return false;
        UIAction act = hovered(mx, my);
        if (act == null) {
            close(); // клик мимо закрывает меню, но не съедает событие
            return false;
        }
        if (act.enabled()) {
            close();
            act.run().run();
        }
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (visible && keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE) {
            close();
            return true;
        }
        return false;
    }
}
