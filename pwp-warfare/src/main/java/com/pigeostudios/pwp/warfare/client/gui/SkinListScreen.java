package com.pigeostudios.pwp.warfare.client.gui;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.pigeostudios.pwp.warfare.client.ClientData;
import com.pigeostudios.pwp.warfare.client.gui.ui.CardGridScreen;
import com.pigeostudios.pwp.warfare.client.gui.ui.WTheme;
import com.pigeostudios.pwp.warfare.client.gui.ui.WarfareIcon;
import com.pwp.coreclient.network.AdminActions;
import com.pwp.coreclient.network.data.DataRequest;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

/**
 * Админ: список скинов (DataRequest «skins», как раньше). Карточка — имя, слот/оружие, цвет
 * редкости; в углу — изменить (SkinEditorScreen.editExisting) и удалить (AdminActions.deleteSkin);
 * над сеткой — «добавить» и «обновить».
 */
public class SkinListScreen extends CardGridScreen {
    private List<SkinEntry> skins = new ArrayList<>();
    private boolean loading = true;
    private boolean requested;

    public static class SkinEntry {
        String skinId, name, slotType, weaponTag, rarity, modelPath;
    }

    public SkinListScreen() {
        super(Component.translatable("gui.pwpwarfare.skin_list.title"));
    }

    @Override
    protected void init() {
        super.init();
        if (!requested) loadSkins();
    }

    public void loadSkins() {
        loading = true;
        requested = true;
        ClientData.skinsData = null;
        com.pwp.coreclient.network.PacketHandler.INSTANCE.sendToServer(new DataRequest("skins", ""));
    }

    @Override
    public void tick() {
        super.tick();
        if (requested && ClientData.skinsData != null) {
            requested = false;
            JsonArray arr = ClientData.skinsData;
            ClientData.skinsData = null;
            List<SkinEntry> loaded = new ArrayList<>();
            for (JsonElement e : arr) {
                JsonObject obj = e.getAsJsonObject();
                SkinEntry se = new SkinEntry();
                se.skinId = obj.get("skinId").getAsString();
                se.name = obj.has("name") ? obj.get("name").getAsString() : se.skinId;
                se.slotType = obj.get("slotType").getAsString();
                se.weaponTag = obj.get("weaponTag").getAsString();
                se.rarity = obj.get("rarity").getAsString();
                se.modelPath = obj.has("modelPath") && !obj.get("modelPath").isJsonNull()
                        ? obj.get("modelPath").getAsString()
                        : "";
                loaded.add(se);
            }
            skins = loaded;
            loading = false;
        }
    }

    @Override
    protected boolean loading() {
        return loading;
    }

    @Override
    protected WarfareIcon titleIcon() {
        return WarfareIcon.LAYERS;
    }

    @Override
    protected Component emptyTitle() {
        return Component.translatable("gui.pwpwarfare.skin_list.empty");
    }

    @Override
    protected Component emptyHint() {
        return Component.translatable("gui.pwpwarfare.skin_list.empty_hint");
    }

    @Override
    protected List<Action> toolbar() {
        return List.of(
                new Action(
                        WarfareIcon.PLUS,
                        Component.translatable("gui.pwpwarfare.skin_list.add"),
                        () -> Minecraft.getInstance().setScreen(new SkinEditorScreen(this))),
                new Action(
                        WarfareIcon.CLOCK,
                        Component.translatable("gui.pwpwarfare.skin_list.refresh"),
                        this::loadSkins));
    }

    @Override
    protected List<Card> cards() {
        List<Card> out = new ArrayList<>();
        for (SkinEntry se : skins) {
            Runnable edit = () -> {
                SkinEditorScreen s = new SkinEditorScreen(this);
                s.editExisting(se.skinId, se.name, se.slotType, se.weaponTag, se.rarity, se.modelPath);
                Minecraft.getInstance().setScreen(s);
            };
            out.add(Card.of(se.skinId, Component.literal(se.name), edit)
                    .subtitle(Component.literal(se.slotType + " · " + se.weaponTag))
                    .icon(WarfareIcon.LAYERS)
                    .accent(rarityColor(se.rarity))
                    .actions(
                            new Action(
                                    WarfareIcon.TRASH,
                                    Component.translatable("gui.pwpwarfare.skin_list.delete"),
                                    () -> AdminActions.deleteSkin(se.skinId, r -> loadSkins())),
                            new Action(
                                    WarfareIcon.EDIT, Component.translatable("gui.pwpwarfare.skin_list.edit"), edit)));
        }
        return out;
    }

    private static int rarityColor(String rarity) {
        if (rarity == null) return WTheme.RARITY_COMMON;
        return switch (rarity.toUpperCase()) {
            case "UNCOMMON" -> WTheme.RARITY_UNCOMMON;
            case "RARE" -> WTheme.RARITY_RARE;
            case "EPIC" -> WTheme.RARITY_EPIC;
            case "LEGENDARY" -> WTheme.RARITY_LEGENDARY;
            case "MYTHIC" -> WTheme.RARITY_MYTHIC;
            default -> WTheme.RARITY_COMMON;
        };
    }
}
