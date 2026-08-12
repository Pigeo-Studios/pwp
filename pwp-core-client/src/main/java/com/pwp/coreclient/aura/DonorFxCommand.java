package com.pwp.coreclient.aura;

import com.pwp.coreclient.CoreClientMod;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Клиентская команда /pwpfx — включить/выключить донат-FX ауры локально
 * (своя настройка на устройстве, сохраняется в donor_fx.toml).
 * Без аргументов — toggle.
 */
@Mod.EventBusSubscriber(modid = CoreClientMod.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class DonorFxCommand {

    private DonorFxCommand() {}

    @SubscribeEvent
    public static void onRegisterClientCommands(RegisterClientCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("pwpfx")
                .executes(ctx -> set(!DonorFxConfig.ENABLED.get(), ctx.getSource()))
                .then(Commands.literal("on").executes(ctx -> set(true, ctx.getSource())))
                .then(Commands.literal("off").executes(ctx -> set(false, ctx.getSource())))
                .then(Commands.literal("toggle").executes(ctx -> set(!DonorFxConfig.ENABLED.get(), ctx.getSource()))));
    }

    private static int set(boolean enabled, CommandSourceStack src) {
        DonorFxConfig.ENABLED.set(enabled);
        try {
            DonorFxConfig.SPEC.save();
        } catch (Exception ignored) {
            // Сохранение опционально — настройка работает и на текущую сессию
        }
        src.sendSuccess(() -> Component.literal("§7Донат-FX ауры: " + (enabled ? "§aвключены" : "§cвыключены")), false);
        return 1;
    }
}
