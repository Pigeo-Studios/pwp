package com.pigeostudios.pwp.warfare.client.gui;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.pigeostudios.pwp.warfare.client.ClientData;
import com.pigeostudios.pwp.warfare.client.gui.deploy.DeployData;
import com.pigeostudios.pwp.warfare.client.gui.ui.CardGridScreen;
import com.pigeostudios.pwp.warfare.client.gui.ui.WarfareIcon;
import com.pigeostudios.pwp.warfare.network.PacketHandler;
import com.pigeostudios.pwp.warfare.network.PacketOpenFactionKitEditor;
import com.pwp.coreclient.network.data.DataRequest;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.chat.Component;

/**
 * Админ: киты фракции. Список — DataRequest «factionKits» (как раньше), карточка открывает
 * редактор кита фракции (PacketOpenFactionKitEditor). Копирование/вставка для фракций не
 * поддерживаются (кнопки раньше были выключены) — их нет.
 */
public class FactionKitListScreen extends CardGridScreen {
    private final String faction;
    private List<String> kitNames = new ArrayList<>();
    private boolean loading = true;
    private boolean requested;

    public FactionKitListScreen(String faction) {
        super(Component.translatable("gui.pwpwarfare.faction_kit_list.title_format", faction.toUpperCase()));
        this.faction = faction;
    }

    @Override
    protected void init() {
        super.init();
        ClientData.factionKitsData = null;
        requested = true;
        com.pwp.coreclient.network.PacketHandler.INSTANCE.sendToServer(
                new DataRequest("factionKits", "{\"faction\":\"" + faction + "\"}"));
    }

    @Override
    public void tick() {
        super.tick();
        if (requested && ClientData.factionKitsData != null) {
            requested = false;
            JsonArray arr = ClientData.factionKitsData;
            ClientData.factionKitsData = null;
            List<String> loaded = new ArrayList<>();
            for (JsonElement e : arr) {
                JsonObject obj = e.getAsJsonObject();
                String name = obj.has("kitName") ? obj.get("kitName").getAsString() : "";
                if (!name.isEmpty()) loaded.add(name);
            }
            kitNames = loaded;
            loading = false;
        }
    }

    @Override
    protected boolean loading() {
        return loading;
    }

    @Override
    protected WarfareIcon titleIcon() {
        return WarfareIcon.EDIT;
    }

    @Override
    protected Component emptyTitle() {
        return Component.translatable("gui.pwpwarfare.deploy.roles.empty");
    }

    @Override
    protected List<Card> cards() {
        List<Card> out = new ArrayList<>();
        for (String kitName : kitNames) {
            out.add(Card.of(
                            kitName,
                            Component.literal(DeployData.getDisplayName(kitName)),
                            () -> PacketHandler.INSTANCE.sendToServer(new PacketOpenFactionKitEditor(faction, kitName)))
                    .image(KitListScreen.kitIcon(kitName), 1F)
                    .icon(WarfareIcon.PERSON));
        }
        return out;
    }
}
