package com.pigeostudios.pwp.warfare.client.gui;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.pigeostudios.pwp.warfare.client.ClientData;
import com.pigeostudios.pwp.warfare.client.gui.deploy.TeamRoster;
import com.pigeostudios.pwp.warfare.client.gui.ui.CardGridScreen;
import com.pigeostudios.pwp.warfare.client.gui.ui.WarfareIcon;
import com.pwp.coreclient.network.data.DataRequest;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

/**
 * Админ: выбор фракции для пулов техники. Список — DataRequest «factions» (пустой ответ —
 * фолбэк ukraine/russia/usa, как раньше); карточка — флаг + название, клик —
 * {@link FactionVehicleListScreen}.
 */
public class FactionVehicleSelectScreen extends CardGridScreen {
    private List<String> factions = new ArrayList<>();
    private boolean loading = true;
    private boolean requested;
    private boolean preview;

    public FactionVehicleSelectScreen() {
        super(Component.translatable("gui.pwpwarfare.faction_vehicle_select.title"));
    }

    /** Тест-стенд UI: список фракций из фикстуры, без запроса на сервер. */
    public static FactionVehicleSelectScreen preview(List<String> factions) {
        FactionVehicleSelectScreen s = new FactionVehicleSelectScreen();
        s.preview = true;
        s.factions = new ArrayList<>(factions);
        s.loading = false;
        return s;
    }

    @Override
    protected void init() {
        super.init();
        if (!preview && !requested && loading) {
            ClientData.factionsData = null;
            requested = true;
            com.pwp.coreclient.network.PacketHandler.INSTANCE.sendToServer(new DataRequest("factions", ""));
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (requested && ClientData.factionsData != null) {
            requested = false;
            JsonArray arr = ClientData.factionsData;
            ClientData.factionsData = null;
            List<String> loaded = new ArrayList<>();
            for (JsonElement e : arr) loaded.add(e.getAsString());
            if (loaded.isEmpty()) {
                loaded.add("ukraine");
                loaded.add("russia");
                loaded.add("usa");
            }
            factions = loaded;
            loading = false;
        }
    }

    @Override
    protected boolean loading() {
        return loading;
    }

    @Override
    protected WarfareIcon titleIcon() {
        return WarfareIcon.TEAM;
    }

    @Override
    protected List<Card> cards() {
        List<Card> out = new ArrayList<>();
        for (String faction : factions) {
            Card c = Card.of(faction, Component.literal(faction.toUpperCase()), () -> {
                        if (!preview) Minecraft.getInstance().setScreen(new FactionVehicleListScreen(faction));
                    })
                    .subtitle(Component.translatable("gui.pwpwarfare.faction_vehicle_select.hint"))
                    .icon(WarfareIcon.TEAM);
            var flag = TeamRoster.flag(faction);
            if (flag != null
                    && Minecraft.getInstance()
                            .getResourceManager()
                            .getResource(flag)
                            .isPresent()) {
                c = c.image(flag, 64F / 36F);
            }
            out.add(c);
        }
        return out;
    }
}
