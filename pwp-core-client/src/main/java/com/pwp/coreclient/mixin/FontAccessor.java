package com.pwp.coreclient.mixin;

import java.util.function.Function;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.font.FontSet;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Доступ к провайдеру наборов глифов шрифта (поле {@code fonts}). Нужен
 * {@code PWPTheme.Fonts.display()}: раньше туда ходили рефлексией по mojmap-имени
 * {@code Minecraft.fontManager} — в проде (SRG) поля с таким именем нет, исключение
 * летело каждый кадр и забивало лог (инцидент 16.11.2026). У @Accessor рефмап-запись
 * генерируется стабильно.
 */
@Mixin(Font.class)
public interface FontAccessor {

    @Accessor("fonts")
    Function<ResourceLocation, FontSet> pwpGetFonts();
}
