package com.pwp.coreclient.gui.screens.tabs;

import com.pwp.coreclient.ui.layout.Rect;
import net.minecraft.client.gui.GuiGraphics;

/**
 * Вкладка лобби: получает свою область контента ({@link #layout}, при каждом изменении окна),
 * рисует себя и обрабатывает ввод. Данные — только из LobbyStatePacket / ответов DataRequest;
 * вкладка ничего не вычисляет из игрового состояния сама.
 */
public interface LobbyTabView {
    void layout(Rect area);

    void render(GuiGraphics g, int mouseX, int mouseY, float partialTick);

    boolean mouseClicked(double mx, double my, int button);

    default boolean mouseReleased(double mx, double my, int button) {
        return false;
    }

    default boolean mouseScrolled(double mx, double my, double delta) {
        return false;
    }

    /** Вкладку только что показали (сброс анимации входа). */
    default void onShow() {}
}
