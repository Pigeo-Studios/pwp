package com.pwp.coreclient.mixin;

import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ConnectScreen.class)
public interface ConnectScreenAccessor {

    // Доступ к приватному полю parent через @Accessor: в отличие от @Shadow,
    // у Accessor Mixin AP стабильно генерирует рефмап-запись (поле объявлено
    // прямо в целевом классе), поэтому доступ работает и в проде (SRG-имена).
    @Accessor("parent")
    Screen pwpGetParent();
}
