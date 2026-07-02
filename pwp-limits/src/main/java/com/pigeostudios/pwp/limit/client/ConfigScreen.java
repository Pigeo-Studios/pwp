package com.pigeostudios.pwp.limit.client;

import com.pigeostudios.pwp.limit.ModConfig;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

// Экран настроек мода PWP: Limits
public class ConfigScreen
extends Screen {
    private final Screen lastScreen;
    private EditBox cooldownInput;

    public ConfigScreen(Screen lastScreen) {
        super((Component)Component.literal((String)"PWP: Limits Settings"));
        this.lastScreen = lastScreen;
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        // Кнопка включения/отключения кулдауна прыжка
        this.addRenderableWidget(CycleButton.onOffBuilder((Boolean)ModConfig.ENABLE_JUMP_COOLDOWN.get()).create(centerX - 100, 50, 200, 20, Component.literal("КД на прыжок"), (button, value) -> ModConfig.ENABLE_JUMP_COOLDOWN.set(value)));
        // Поле ввода секунд кулдауна
        this.cooldownInput = new EditBox(this.font, centerX - 100, 90, 200, 20, Component.literal("Секунды"));
        this.cooldownInput.setValue(String.valueOf(ModConfig.JUMP_COOLDOWN_SECONDS.get()));
        this.cooldownInput.setFilter(s -> s.matches("^[0-9]*\\.?[0-9]*$"));
        this.addRenderableWidget(this.cooldownInput);
        // Кнопка "Готово" — сохранить и закрыть
        this.addRenderableWidget(Button.builder(Component.translatable("gui.done"), button -> this.saveAndClose()).pos(centerX - 100, 150).size(200, 20).build());
    }

    private void saveAndClose() {
        try {
            double value = Double.parseDouble(this.cooldownInput.getValue());
            ModConfig.JUMP_COOLDOWN_SECONDS.set(value);
        }
        catch (Exception exception) {
            // empty catch block
        }
        ModConfig.SPEC.save();
        if (this.minecraft != null) {
            this.minecraft.setScreen(this.lastScreen);
        }
    }

    @Override
    public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics);
        guiGraphics.drawString(this.font, this.title, this.width / 2, 20, 0xFFFFFF);
        guiGraphics.drawString(this.font, "Секунды КД:", this.width / 2 - 100, 78, 0xAAAAAA);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }
}
