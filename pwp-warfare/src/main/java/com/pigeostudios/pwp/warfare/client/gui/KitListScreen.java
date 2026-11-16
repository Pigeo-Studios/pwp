package com.pigeostudios.pwp.warfare.client.gui;

import com.pigeostudios.pwp.warfare.client.WarfareClipboard;
import com.pigeostudios.pwp.warfare.client.gui.deploy.DeployData;
import com.pigeostudios.pwp.warfare.client.gui.ui.CardGridScreen;
import com.pigeostudios.pwp.warfare.client.gui.ui.WTheme;
import com.pigeostudios.pwp.warfare.client.gui.ui.WarfareIcon;
import com.pigeostudios.pwp.warfare.network.PacketHandler;
import com.pigeostudios.pwp.warfare.network.PacketOpenKitEditor;
import com.pigeostudios.pwp.warfare.network.PacketPasteKit;
import com.pigeostudios.pwp.warfare.network.PacketPasteTeam;
import com.pigeostudios.pwp.warfare.network.PacketRequestKitData;
import com.pigeostudios.pwp.warfare.world.WarfareWorldData;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/**
 * Админ: киты команды (BLUE/RED). Карточка — редактировать кит (PacketOpenKitEditor), в углу —
 * копировать/вставить; над сеткой — копировать/вставить все. Пакеты те же, что раньше;
 * «вставить» недоступно, пока буфер пуст.
 */
public class KitListScreen extends CardGridScreen {
    private final String team;

    public KitListScreen(String team) {
        super(Component.translatable("gui.pwpwarfare.kit_list.title_format", team));
        this.team = team;
    }

    @Override
    protected WarfareIcon titleIcon() {
        return WarfareIcon.EDIT;
    }

    @Override
    protected List<Action> toolbar() {
        return List.of(
                new Action(
                        WarfareIcon.LAYERS,
                        Component.translatable("gui.pwpwarfare.kit_list.copy_all"),
                        () -> PacketHandler.INSTANCE.sendToServer(new PacketRequestKitData(team, "ALL"))),
                new Action(
                        WarfareIcon.PLUS,
                        Component.translatable("gui.pwpwarfare.kit_list.paste_all"),
                        WarfareClipboard.teamKitsData == null
                                ? null
                                : () -> PacketHandler.INSTANCE.sendToServer(
                                        new PacketPasteTeam(team, WarfareClipboard.teamKitsData))));
    }

    @Override
    protected List<Card> cards() {
        List<Card> out = new ArrayList<>();
        int accent = team.equals("BLUE") ? WTheme.TEAM_BLUE : WTheme.TEAM_RED;
        for (String kitName : WarfareWorldData.KIT_NAMES) {
            out.add(Card.of(
                            kitName,
                            Component.literal(DeployData.getDisplayName(kitName)),
                            () -> PacketHandler.INSTANCE.sendToServer(new PacketOpenKitEditor(team, kitName)))
                    .image(kitIcon(kitName), 1F)
                    .icon(WarfareIcon.PERSON)
                    .accent(accent)
                    .actions(
                            new Action(
                                    WarfareIcon.LAYERS,
                                    Component.translatable("gui.pwpwarfare.kit_list.copy"),
                                    () -> PacketHandler.INSTANCE.sendToServer(new PacketRequestKitData(team, kitName))),
                            new Action(
                                    WarfareIcon.PLUS,
                                    Component.translatable("gui.pwpwarfare.kit_list.paste"),
                                    WarfareClipboard.kitData == null
                                            ? null
                                            : () -> PacketHandler.INSTANCE.sendToServer(
                                                    new PacketPasteKit(team, kitName, WarfareClipboard.kitData)))));
        }
        return out;
    }

    static ResourceLocation kitIcon(String name) {
        ResourceLocation rl = ResourceLocation.tryBuild(
                "pwpwarfare", "textures/gui/kits/" + DeployData.kitIconFileName(name) + ".png");
        if (rl == null) return null;
        return Minecraft.getInstance().getResourceManager().getResource(rl).isPresent() ? rl : null;
    }
}
