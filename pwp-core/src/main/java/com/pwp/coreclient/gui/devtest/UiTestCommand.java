package com.pwp.coreclient.gui.devtest;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.common.MinecraftForge;

/**
 * Клиентская команда тест-стенда (регистрируется только при {@link UiGallery#enabled()}):
 * {@code /pwpui} — галерея, {@code /pwpui open <id>}, {@code /pwpui next}, {@code /pwpui list}.
 * Экран открывается на следующем кадре: чат закрывает себя ПОСЛЕ выполнения команды.
 */
@OnlyIn(Dist.CLIENT)
public final class UiTestCommand {
    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(UiTestCommand.class);
    private static boolean initialized;

    private UiTestCommand() {}

    /** Подключить стенд (из клиентской инициализации мода). В проде без флага — ничего. */
    public static void init() {
        if (initialized || !UiGallery.enabled()) return;
        initialized = true;
        CoreUiFixtures.registerAll();
        MinecraftForge.EVENT_BUS.addListener(UiTestCommand::onRegister);
        LOG.info("[PWPUI] UI test bench enabled ({} screens)", UiGallery.ids().size());
    }

    private static final SuggestionProvider<CommandSourceStack> IDS =
            (ctx, b) -> SharedSuggestionProvider.suggest(UiGallery.ids(), b);

    private static void onRegister(RegisterClientCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> d = event.getDispatcher();
        d.register(Commands.literal("pwpui")
                .executes(ctx -> later(UiGallery::openGallery))
                .then(Commands.literal("open")
                        .then(Commands.argument("id", StringArgumentType.greedyString())
                                .suggests(IDS)
                                .executes(ctx -> {
                                    String id = StringArgumentType.getString(ctx, "id")
                                            .trim();
                                    return later(() -> {
                                        if (!UiGallery.open(id)) {
                                            ctx.getSource()
                                                    .sendFailure(Component.translatable("pwp_core.uitest.unknown", id));
                                        }
                                    });
                                })))
                .then(Commands.literal("next").executes(ctx -> later(UiGallery::next)))
                .then(Commands.literal("list").executes(ctx -> {
                    for (String id : UiGallery.ids()) {
                        LOG.info("[PWPUI] id {}", id);
                        ctx.getSource().sendSuccess(() -> Component.literal(id), false);
                    }
                    return UiGallery.ids().size();
                })));
    }

    private static int later(Runnable r) {
        Minecraft.getInstance().tell(r);
        return 1;
    }
}
