package com.pigeostudios.pwp.limit.client;

import com.pigeostudios.pwp.limit.ModConfig;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class ConfigScreen extends Screen {
    private static final double MIN_SECONDS = 0.1;
    private static final double MAX_SECONDS = 60.0;

    private final Screen lastScreen;
    private EditBox cooldownInput;
    private String errorMsg = null;

    public ConfigScreen(Screen lastScreen) {
        super(Component.literal("PWP: Настройки лимитов"));
        this.lastScreen = lastScreen;
    }

    @Override
    protected void init() {
        int cx = this.width / 2;
        this.addRenderableWidget(CycleButton.onOffBuilder((Boolean) ModConfig.ENABLE_JUMP_COOLDOWN.get())
            .create(cx - 100, 50, 200, 20, Component.literal("КД прыжка"), (button, value) -> ModConfig.ENABLE_JUMP_COOLDOWN.set(value)));

        this.cooldownInput = new EditBox(this.font, cx - 100, 90, 200, 20, Component.literal("Секунды"));
        this.cooldownInput.setValue(String.valueOf(ModConfig.JUMP_COOLDOWN_SECONDS.get()));
        this.cooldownInput.setFilter(s -> s.matches("^[0-9]*\\.?[0-9]*$"));
        this.addRenderableWidget(this.cooldownInput);

        this.addRenderableWidget(Button.builder(Component.translatable("gui.done"), button -> this.saveAndClose())
            .bounds(cx - 100, 150, 200, 22).build());
    }

    private void saveAndClose() {
        double value;
        try {
            value = Double.parseDouble(this.cooldownInput.getValue());
        } catch (NumberFormatException e) {
            this.errorMsg = "Введите число, например 3.0";
            return;
        }
        if (value < MIN_SECONDS || value > MAX_SECONDS) {
            this.errorMsg = "Значение должно быть от 0.1 до 60.0";
            return;
        }
        this.errorMsg = null;
        ModConfig.JUMP_COOLDOWN_SECONDS.set(value);
        ModConfig.SPEC.save();
        // Обновляем клиентский кэш сразу (в сингле пакет от сервера придёт со старыми значениями,
        // в мультиплеере серверные значения перекроют эти при следующем логине)
        LimitsConfigCache.apply(ModConfig.ENABLE_JUMP_COOLDOWN.get(), value);
        if (this.minecraft != null) {
            this.minecraft.setScreen(this.lastScreen);
        }
    }

    @Override
    public void render(GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(gui);
        int cx = this.width / 2;
        gui.drawCenteredString(this.font, "\u2699 " + this.title.getString(), cx, 20, 0xFFC8812A);
        gui.drawString(this.font, "КД в секундах:", cx - 100, 78, 0xFF7A7D84);
        if (this.errorMsg != null) {
            gui.drawString(this.font, this.errorMsg, cx - 100, 118, 0xFFA53D3D);
        }
        super.render(gui, mouseX, mouseY, partialTick);
    }
}
