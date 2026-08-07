package com.pigeostudios.pwp.medicine.client.gui;

import com.pigeostudios.pwp.medicine.config.PWPConfig;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class PWPConfigScreen extends Screen {
    private final Screen parent;
    private EditBox medkitTimeBox;
    private EditBox thresholdBox;
    private EditBox chanceBox;

    public PWPConfigScreen(Screen parent) {
        super(Component.literal("PWP: Настройки медицины"));
        this.parent = parent;
    }

    protected void init() {
        super.init();
        int cx = this.width / 2;

        this.medkitTimeBox = new EditBox(this.font, cx - 100, 60, 200, 20, Component.literal("Время аптечки"));
        this.medkitTimeBox.setValue(String.valueOf(PWPConfig.MEDKIT_APPLY_TIME.get()));
        this.addRenderableWidget(this.medkitTimeBox);

        this.thresholdBox = new EditBox(this.font, cx - 100, 110, 200, 20, Component.literal("Порог урона"));
        this.thresholdBox.setValue(String.valueOf(PWPConfig.BLEEDING_DAMAGE_THRESHOLD.get()));
        this.addRenderableWidget(this.thresholdBox);

        this.chanceBox = new EditBox(this.font, cx - 100, 160, 200, 20, Component.literal("Шанс кровотечения"));
        this.chanceBox.setValue(String.valueOf(PWPConfig.BLEEDING_CHANCE.get()));
        this.addRenderableWidget(this.chanceBox);

        this.addRenderableWidget(Button.builder(Component.literal("Сохранить"), b -> this.saveAndClose())
            .bounds(cx - 105, 200, 100, 22).build());
        this.addRenderableWidget(Button.builder(Component.literal("Отмена"), b -> this.minecraft.setScreen(this.parent))
            .bounds(cx + 5, 200, 100, 22).build());
    }

    private void saveAndClose() {
        try {
            int time = Integer.parseInt(this.medkitTimeBox.getValue());
            double threshold = Double.parseDouble(this.thresholdBox.getValue());
            double chance = Double.parseDouble(this.chanceBox.getValue());
            PWPConfig.MEDKIT_APPLY_TIME.set(Math.max(1, time));
            PWPConfig.BLEEDING_DAMAGE_THRESHOLD.set(Math.max(0.0, threshold));
            PWPConfig.BLEEDING_CHANCE.set(Math.max(0.0, Math.min(1.0, chance)));
        } catch (NumberFormatException ignored) {}
        this.minecraft.setScreen(this.parent);
    }

    public void render(GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(gui);
        super.render(gui, mouseX, mouseY, partialTick);
        int cx = this.width / 2;
        gui.drawCenteredString(this.font, "\u2699 " + this.title.getString(), cx, 20, 0xFFC8812A);
        gui.drawString(this.font, "Время применения аптечки (тики, 20 = 1с):", cx - 100, 48, 0xFF7A7D84, false);
        gui.drawString(this.font, "Порог урона для кровотечения (HP):", cx - 100, 98, 0xFF7A7D84, false);
        gui.drawString(this.font, "Шанс кровотечения (0.0 до 1.0):", cx - 100, 148, 0xFF7A7D84, false);
    }
}
