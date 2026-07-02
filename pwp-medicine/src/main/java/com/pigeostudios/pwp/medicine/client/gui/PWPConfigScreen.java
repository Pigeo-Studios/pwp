package com.pigeostudios.pwp.medicine.client.gui;

import com.pigeostudios.pwp.medicine.config.PWPConfig;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

// Экран конфигурации PWP Medicine.
// Позволяет менять время применения аптечки, порог урона для кровотечения и шанс кровотечения.
public class PWPConfigScreen
extends Screen {
    private final Screen parent;
    // Поля ввода для трёх конфигурационных значений
    private EditBox medkitTimeBox;    // Время между применениями аптечки (в тиках)
    private EditBox thresholdBox;     // Минимальный урон для шанса кровотечения
    private EditBox chanceBox;        // Шанс получения кровотечения (0.0–1.0)

    public PWPConfigScreen(Screen parent) {
        super(Component.literal("Настройки PWP Medicine"));
        this.parent = parent;
    }

    // Инициализация виджетов: текстовые поля и кнопки "Сохранить"/"Отмена"
    protected void init() {
        super.init();
        int centerX = this.width / 2;
        // Поле для времени применения аптечки
        this.medkitTimeBox = new EditBox(this.font, centerX - 100, 60, 200, 20, Component.literal("Medkit Time"));
        this.medkitTimeBox.setValue(String.valueOf(PWPConfig.MEDKIT_APPLY_TIME.get()));
        this.addRenderableWidget(this.medkitTimeBox);
        // Поле для порога урона
        this.thresholdBox = new EditBox(this.font, centerX - 100, 110, 200, 20, Component.literal("Damage Threshold"));
        this.thresholdBox.setValue(String.valueOf(PWPConfig.BLEEDING_DAMAGE_THRESHOLD.get()));
        this.addRenderableWidget(this.thresholdBox);
        // Поле для шанса кровотечения
        this.chanceBox = new EditBox(this.font, centerX - 100, 160, 200, 20, Component.literal("Bleeding Chance"));
        this.chanceBox.setValue(String.valueOf(PWPConfig.BLEEDING_CHANCE.get()));
        this.addRenderableWidget(this.chanceBox);
        // Кнопка "Сохранить"
        this.addRenderableWidget(Button.builder(Component.literal("Сохранить"), b -> this.saveAndClose()).bounds(centerX - 105, 200, 100, 20).build());
        // Кнопка "Отмена"
        this.addRenderableWidget(Button.builder(Component.literal("Отмена"), b -> this.minecraft.setScreen(this.parent)).bounds(centerX + 5, 200, 100, 20).build());
    }

    // Сохраняет значения из полей в конфиг и возвращается к родительскому экрану
    private void saveAndClose() {
        try {
            int time = Integer.parseInt(this.medkitTimeBox.getValue());
            double threshold = Double.parseDouble(this.thresholdBox.getValue());
            double chance = Double.parseDouble(this.chanceBox.getValue());
            PWPConfig.MEDKIT_APPLY_TIME.set(Math.max(1, time));
            PWPConfig.BLEEDING_DAMAGE_THRESHOLD.set(Math.max(0.0, threshold));
            PWPConfig.BLEEDING_CHANCE.set(Math.max(0.0, Math.min(1.0, chance)));
        }
        catch (NumberFormatException numberFormatException) {
            // Игнорируем некорректный ввод
        }
        this.minecraft.setScreen(this.parent);
    }

    // Отрисовка экрана: фон, заголовок, подписи к полям
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        int centerX = this.width / 2;
        guiGraphics.drawString(this.font, this.title, centerX, 20, 0xFFFFFF);
        guiGraphics.drawString(this.font, "Скорость применения аптечки (тиков, 20 = 1 сек):", centerX - 100, 48, 0xA0A0A0, false);
        guiGraphics.drawString(this.font, "Порог урона для кровотечения (ХП):", centerX - 100, 98, 0xA0A0A0, false);
        guiGraphics.drawString(this.font, "Шанс кровотечения (от 0.0 до 1.0):", centerX - 100, 148, 0xA0A0A0, false);
    }
}
