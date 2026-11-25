package com.pigeostudios.pwp.warfare.client;

import com.pigeostudios.pwp.warfare.client.gui.TeamSelectionScreen;
import com.pigeostudios.pwp.warfare.tutorial.Tutorial;
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
        registerTutorial();
        Hints.register(
                "match.team",
                90,
                false,
                () -> !TutorialClient.active() && Minecraft.getInstance().screen instanceof TeamSelectionScreen,
                () -> Component.translatable("gui.pwpwarfare.hint.team"));
        Hints.register(
                "staging.squad",
                80,
                false,
                () -> !TutorialClient.active()
                        && inWorldNoScreen()
                        && ClientStaging.isWaiting()
                        && ClientStaging.hasTeam()
                        && ClientStaging.mySquad() == null,
                () -> Component.translatable(
                        "gui.pwpwarfare.hint.staging.squad", k(ModKeyBindings.OPEN_SQUAD_MENU_KEY)));
        Hints.register(
                "staging.role",
                70,
                false,
                () -> !TutorialClient.active()
                        && inWorldNoScreen()
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

    /**
     * Обучение новичка (этап 5 ROADMAP): по подсказке на шаг, приоритет выше всех прочих (100+), «Обучение N/5»
     * в тексте. Шаг ведёт {@link TutorialClient}; подсказка повторяется, пока шаг не выполнен (как обычные).
     */
    private static void registerTutorial() {
        Hints.register(
                "tutorial.team",
                110,
                false,
                () -> TutorialClient.showing(Tutorial.Step.TEAM)
                        && (Minecraft.getInstance().screen instanceof TeamSelectionScreen || inWorldNoScreen()),
                () -> step(Tutorial.Step.TEAM));
        Hints.register(
                "tutorial.squad",
                109,
                false,
                () -> TutorialClient.showing(Tutorial.Step.SQUAD) && inWorldNoScreen(),
                () -> step(Tutorial.Step.SQUAD, k(ModKeyBindings.OPEN_SQUAD_MENU_KEY)));
        Hints.register(
                "tutorial.role",
                108,
                false,
                () -> TutorialClient.showing(Tutorial.Step.ROLE) && inWorldNoScreen(),
                () -> step(Tutorial.Step.ROLE, k(ModKeyBindings.OPEN_SQUAD_MENU_KEY)));
        Hints.register(
                "tutorial.deploy",
                107,
                false,
                () -> TutorialClient.showing(Tutorial.Step.DEPLOY) && inWorldNoScreen(),
                () -> step(Tutorial.Step.DEPLOY, k(ModKeyBindings.OPEN_SQUAD_MENU_KEY)));
        Hints.register(
                "tutorial.capture",
                106,
                false,
                () -> TutorialClient.showing(Tutorial.Step.CAPTURE) && inWorldNoScreen(),
                () -> step(Tutorial.Step.CAPTURE, k(ModKeyBindings.OPEN_MAP_KEY)));
        Hints.register(
                "tutorial.done",
                105,
                true,
                () -> TutorialClient.justFinished() && inWorldNoScreen(),
                () -> Component.translatable("gui.pwpwarfare.tutorial.done"));
    }

    /** «Обучение N/5 · текст шага». */
    private static Component step(Tutorial.Step s, Object... args) {
        return Component.translatable(
                "gui.pwpwarfare.tutorial.prefix",
                s.number(),
                Tutorial.TOTAL,
                Component.translatable(s.langKey(), args));
    }
}
