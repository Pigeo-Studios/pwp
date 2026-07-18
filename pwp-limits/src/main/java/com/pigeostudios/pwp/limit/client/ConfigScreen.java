package com.pigeostudios.pwp.limit.client;

import com.pigeostudios.pwp.limit.ModConfig;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class ConfigScreen extends Screen {
    private final Screen lastScreen;
    private EditBox cooldownInput;

    public ConfigScreen(Screen lastScreen) {
        super(Component.literal("PWP: Настройки лимитов"));
        this.lastScreen = lastScreen;
    }

    @Override
    protected void init() {
        int cx = this.width / 2;
        this.addRenderableWidget(CycleButton.onOffBuilder((Boolean)ModConfig.ENABLE_JUMP_COOLDOWN.get())
            .create(cx - 100, 50, 200, 20, Component.literal("КД прыжка"), (button, value) -> ModConfig.ENABLE_JUMP_COOLDOWN.set(value)));

        this.cooldownInput = new EditBox(this.font, cx - 100, 90, 200, 20, Component.literal("Секунды"));
        this.cooldownInput.setValue(String.valueOf(ModConfig.JUMP_COOLDOWN_SECONDS.get()));
        this.cooldownInput.setFilter(s -> s.matches("^[0-9]*\\.?[0-9]*$"));
        this.addRenderableWidget(this.cooldownInput);

        this.addRenderableWidget(Button.builder(Component.translatable("gui.done"), button -> this.saveAndClose())
            .bounds(cx - 100, 150, 200, 22).build());
    }

    private void saveAndClose() {
        try {
            double value = Double.parseDouble(this.cooldownInput.getValue());
            ModConfig.JUMP_COOLDOWN_SECONDS.set(value);
        } catch (Exception ignored) {}
        ModConfig.SPEC.save();
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
        super.render(gui, mouseX, mouseY, partialTick);
    }
}
