package com.pigeostudios.pwp.warfare.client.gui;

import com.pigeostudios.pwp.warfare.client.gui.ui.CardGridScreen;
import com.pigeostudios.pwp.warfare.client.gui.ui.WTheme;
import com.pigeostudios.pwp.warfare.client.gui.ui.WarfareIcon;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

/** Админ: выбор команды для редактирования китов (BLUE / RED → KitListScreen). */
public class KitTeamSelectScreen extends CardGridScreen {
    public KitTeamSelectScreen() {
        super(Component.translatable("gui.pwpwarfare.kit_team_select.title"));
    }

    @Override
    protected WarfareIcon titleIcon() {
        return WarfareIcon.TEAM;
    }

    @Override
    protected int maxCols() {
        return 2;
    }

    @Override
    protected List<Card> cards() {
        return List.of(
                Card.of(
                                "BLUE",
                                Component.translatable("gui.pwpwarfare.kit_team_select.blue"),
                                () -> Minecraft.getInstance().setScreen(new KitListScreen("BLUE")))
                        .icon(WarfareIcon.SHIELD)
                        .accent(WTheme.TEAM_BLUE),
                Card.of(
                                "RED",
                                Component.translatable("gui.pwpwarfare.kit_team_select.red"),
                                () -> Minecraft.getInstance().setScreen(new KitListScreen("RED")))
                        .icon(WarfareIcon.TARGET)
                        .accent(WTheme.TEAM_RED));
    }
}
