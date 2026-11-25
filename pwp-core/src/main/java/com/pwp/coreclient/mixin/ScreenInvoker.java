package com.pwp.coreclient.mixin;

import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Screen.class)
public interface ScreenInvoker {

    // Рефмап-запись для этого @Invoker Mixin AP генерирует нестабильно
    // (дубликаты в tsrg: addRenderableWidget наследуется всеми подклассами
    // Screen). Запись принудительно вписывается в рефмап в build.gradle
    // (compileJava.doLast) — без неё кнопка отмены в ConnectScreen не
    // зарегистрируется в проде (SRG).
    @Invoker("addRenderableWidget")
    <T extends GuiEventListener & Renderable & NarratableEntry> T pwpInvokeAddRenderableWidget(T widget);
}
