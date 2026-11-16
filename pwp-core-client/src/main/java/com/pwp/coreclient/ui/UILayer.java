package com.pwp.coreclient.ui;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * Контейнер виджетов экрана: рисует их по порядку, события отдаёт сверху вниз (последний
 * добавленный — верхний), поверх всего — контекстное меню, модалка и тултип. Экран вызывает
 * методы слоя из своих render/mouseClicked/mouseScrolled/keyPressed.
 */
@OnlyIn(Dist.CLIENT)
public final class UILayer {
    private final List<UIWidget> widgets = new ArrayList<>();
    private final UIContextMenu contextMenu = new UIContextMenu();
    private UIModal modal;
    private UIWidget tooltipOwner;
    private long hoverStartMs;
    private final UIAnim tooltipAlpha = new UIAnim(0F, 120, UIAnim.EASE_OUT_CUBIC);

    public <T extends UIWidget> T add(T widget) {
        widgets.add(widget);
        return widget;
    }

    public void remove(UIWidget widget) {
        widgets.remove(widget);
    }

    public void clear() {
        widgets.clear();
        contextMenu.close();
        modal = null;
    }

    public UIContextMenu contextMenu() {
        return contextMenu;
    }

    public void showModal(UIModal modal, int screenW, int screenH) {
        this.modal = modal;
        modal.show(screenW, screenH);
    }

    public boolean hasModal() {
        return modal != null && modal.isVisible();
    }

    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick, int screenW, int screenH) {
        boolean blocked = hasModal() || contextMenu.isVisible();
        int mx = blocked ? Integer.MIN_VALUE : mouseX;
        int my = blocked ? Integer.MIN_VALUE : mouseY;
        for (UIWidget w : widgets) {
            if (!w.isVisible() || w.opacity() <= 0F) continue;
            boolean shifted = w.offsetX() != 0 || w.offsetY() != 0;
            if (shifted) {
                g.pose().pushPose();
                g.pose().translate(w.offsetX(), w.offsetY(), 0);
            }
            w.render(g, mx, my, partialTick);
            if (shifted) g.pose().popPose();
        }
        if (contextMenu.isVisible()) contextMenu.render(g, mouseX, mouseY, partialTick);
        renderTooltip(g, mouseX, mouseY, screenW, screenH, blocked);
        if (hasModal()) modal.render(g, mouseX, mouseY, partialTick);
    }

    private void renderTooltip(GuiGraphics g, int mouseX, int mouseY, int screenW, int screenH, boolean blocked) {
        UIWidget owner = null;
        if (!hasModal()) {
            if (contextMenu.isVisible()) {
                UIAction a = contextMenu.hovered(mouseX, mouseY);
                if (a != null && a.disabledReason() != null) {
                    UITooltip.render(
                            g,
                            UITooltip.Content.of(net.minecraft.network.chat.Component.literal(a.disabledReason())),
                            mouseX,
                            mouseY,
                            screenW,
                            screenH,
                            1F);
                }
                return;
            }
            for (int i = widgets.size() - 1; i >= 0 && !blocked; i--) {
                UIWidget w = widgets.get(i);
                if (w.isMouseOver(mouseX, mouseY)) {
                    owner = w.tooltip() != null ? w : null;
                    break;
                }
            }
        }
        long now = UIAnim.REAL_TIME.getAsLong();
        if (owner != tooltipOwner) {
            tooltipOwner = owner;
            hoverStartMs = now;
            tooltipAlpha.snap(0F);
        }
        if (owner != null && now - hoverStartMs >= UITooltip.DELAY_MS) tooltipAlpha.to(1F);
        if (owner != null) UITooltip.render(g, owner.tooltip(), mouseX, mouseY, screenW, screenH, tooltipAlpha.value());
    }

    public boolean mouseClicked(double mx, double my, int button) {
        if (hasModal()) return modal.mouseClicked(mx, my, button);
        if (contextMenu.isVisible() && contextMenu.mouseClicked(mx, my, button)) return true;
        for (int i = widgets.size() - 1; i >= 0; i--) {
            UIWidget w = widgets.get(i);
            // невидимые и ещё не проявившиеся (opacity 0) виджеты кликов не получают
            if (w.isVisible() && w.opacity() > 0F && w.mouseClicked(mx, my, button)) return true;
        }
        return false;
    }

    /** Отпускание мыши получают ВСЕ виджеты (сброс нажатия, даже если курсор ушёл). */
    public boolean mouseReleased(double mx, double my, int button) {
        boolean handled = false;
        for (int i = widgets.size() - 1; i >= 0; i--) {
            if (widgets.get(i).mouseReleased(mx, my, button)) handled = true;
        }
        return handled;
    }

    public List<UIWidget> widgets() {
        return java.util.Collections.unmodifiableList(widgets);
    }

    public boolean mouseScrolled(double mx, double my, double delta) {
        if (hasModal()) return true;
        for (int i = widgets.size() - 1; i >= 0; i--) {
            UIWidget w = widgets.get(i);
            if (w.isVisible() && w.mouseScrolled(mx, my, delta)) return true;
        }
        return false;
    }

    /** true — клавиша съедена (ESC закрыл модалку/меню — экран закрываться не должен). */
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (hasModal()) return modal.keyPressed(keyCode, scanCode, modifiers);
        if (contextMenu.keyPressed(keyCode, scanCode, modifiers)) return true;
        for (int i = widgets.size() - 1; i >= 0; i--) {
            UIWidget w = widgets.get(i);
            if (w.isVisible() && w.keyPressed(keyCode, scanCode, modifiers)) return true;
        }
        return false;
    }
}
