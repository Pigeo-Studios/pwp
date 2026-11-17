package com.pwp.coreclient.admin;

import com.pwp.coreclient.ui.UIModal;
import com.pwp.coreclient.ui.UIWidget;
import com.pwp.coreclient.ui.layout.Rect;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * Вкладка админ-панели. Экземпляр создаётся фабрикой при каждом открытии панели; {@link #init} зовётся
 * при каждом пересчёте лейаута (смена размера окна, {@link Host#relayout()}) — виджеты добавлять заново.
 */
@OnlyIn(Dist.CLIENT)
public interface AdminTab {
    /** Разложить виджеты в области {@code area} (координаты экрана) через {@code host.add}. */
    void init(Host host, Rect area);

    /** Фон/тексты вкладки под виджетами (виджеты рисует слой экрана). */
    default void render(GuiGraphics g, Font font, Rect area, int mouseX, int mouseY, float partialTick) {}

    /** Ответ сервера любого типа (в т.ч. {@code actionResult}); главный поток клиента. */
    default void onData(String type, String json) {}

    /** Раз в клиентский тик, пока вкладка открыта (опрос состояния). */
    default void tick() {}

    /** Вкладку открыли (выбрали или открыли панель на ней) — хорошее место для первого запроса. */
    default void onShown() {}

    /** Что экран панели даёт вкладке. */
    interface Host {
        <T extends UIWidget> T add(T widget);

        /** Пересобрать виджеты вкладки (после прихода данных, меняющих набор контролов). */
        void relayout();

        void showModal(UIModal modal);

        AdminBackend backend();

        default boolean preview() {
            return backend().preview();
        }
    }
}
