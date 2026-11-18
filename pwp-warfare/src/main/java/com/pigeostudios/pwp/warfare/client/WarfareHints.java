package com.pigeostudios.pwp.warfare.client;

import com.pigeostudios.pwp.warfare.client.gui.TeamSelectionScreen;
import com.pwp.coreclient.hints.Hints;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * Подсказки матча (этап 4) в реестре core {@link Hints}: ожидание (команда → отряд → роль → готовность →
 * карта) и бой (взять снаряжение, рация, пинг). Клавиши — реальные KeyMapping. Условия только читают
 * клиентское состояние (ClientData/ClientStaging), ничего не шлют.
 */
@OnlyIn(Dist.CLIENT)
public final class WarfareHints {
    private WarfareHints() {}

    private static boolean inWorldNoScreen() {
        Minecraft mc = Minecraft.getInstance();
        return mc.player != null && mc.screen == null && !mc.player.isSpectator() && mc.player.isAlive();
    }

    private static Component k(net.minecraft.client.KeyMapping key) {
        return key.getTranslatedKeyMessage();
    }

    public static void register() {
        Hints.register(
                "match.team",
                90,
                false,
                () -> Minecraft.getInstance().screen instanceof TeamSelectionScreen,
                () -> Component.translatable("gui.pwpwarfare.hint.team"));
        Hints.register(
                "staging.squad",
                80,
                false,
                () -> inWorldNoScreen()
                        && ClientStaging.isWaiting()
                        && ClientStaging.hasTeam()
                        && ClientStaging.mySquad() == null,
                () -> Component.translatable(
                        "gui.pwpwarfare.hint.staging.squad", k(ModKeyBindings.OPEN_SQUAD_MENU_KEY)));
        Hints.register(
                "staging.role",
                70,
                false,
                () -> inWorldNoScreen()
                        && ClientStaging.isWaiting()
                        && ClientStaging.mySquad() != null
                        && !ClientStaging.hasKit(),
                () -> Component.translatable(
                        "gui.pwpwarfare.hint.staging.role", k(ModKeyBindings.OPEN_SQUAD_MENU_KEY)));
        Hints.register(
                "staging.ready",
                60,
                false,
                () -> {
                    var p = Minecraft.getInstance().player;
                    return inWorldNoScreen()
                            && ClientStaging.isWaiting()
                            && ClientData.voteActive
                            && ClientStaging.hasKit()
                            && p != null
                            && !ClientData.votes.containsKey(p.getUUID());
                },
                () -> Component.translatable(
                        "gui.pwpwarfare.hint.staging.ready",
                        k(ModKeyBindings.VOTE_START_YES_KEY),
                        k(ModKeyBindings.VOTE_START_NO_KEY),
                        k(ModKeyBindings.OPEN_SQUAD_MENU_KEY)));
        Hints.register(
                "staging.map",
                40,
                true,
                () -> inWorldNoScreen() && ClientStaging.isWaiting() && ClientStaging.isLeaderOrFireteamLeader(),
                () -> Component.translatable("gui.pwpwarfare.hint.staging.map", k(ModKeyBindings.OPEN_MAP_KEY)));
        Hints.register(
                "battle.take_kit",
                95,
                false,
                () -> inWorldNoScreen()
                        && ClientData.isGameStarted
                        && ClientStaging.hasTeam()
                        && ClientSafetyState.inMainZone
                        && !ClientStaging.hasKit(),
                () -> Component.translatable(
                        "gui.pwpwarfare.hint.battle.take_kit", k(ModKeyBindings.OPEN_SQUAD_MENU_KEY)));
        Hints.register(
                "battle.voice",
                20,
                true,
                () -> inWorldNoScreen() && ClientData.isGameStarted && ClientStaging.mySquad() != null,
                () -> Component.translatable(
                        "gui.pwpwarfare.hint.battle.voice",
                        k(ModKeyBindings.SQUAD_PTT_KEY),
                        k(ModKeyBindings.COMMAND_PTT_KEY)));
        Hints.register(
                "battle.ping",
                10,
                true,
                () -> inWorldNoScreen() && ClientData.isGameStarted && ClientStaging.hasKit(),
                () -> Component.translatable("gui.pwpwarfare.hint.battle.ping", k(ModKeyBindings.PLACE_PING_KEY)));
    }
}
