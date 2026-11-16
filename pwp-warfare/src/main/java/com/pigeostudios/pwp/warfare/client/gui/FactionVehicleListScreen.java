package com.pigeostudios.pwp.warfare.client.gui;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.pigeostudios.pwp.warfare.client.ClientData;
import com.pigeostudios.pwp.warfare.client.gui.ui.CardGridScreen;
import com.pigeostudios.pwp.warfare.client.gui.ui.WarfareIcon;
import com.pwp.coreclient.gui.theme.PWPTheme;
import com.pwp.coreclient.network.AdminActions;
import com.pwp.coreclient.network.data.DataRequest;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

/**
 * Админ: техника фракции. Карточки — имя машины, отображаемое имя/ID и категория; действия
 * в углу — «Изменить» ({@link FactionVehicleEditorScreen}) и «Удалить»
 * ({@link AdminActions#deleteFactionVehicle}, как раньше). Первая карточка — «Добавить».
 * Поиск — просто печатать на клавиатуре (строка поиска в заголовке, Backspace — стереть):
 * фильтр по имени, отображаемому имени, ID и категории, как в прежнем поле поиска.
 */
public class FactionVehicleListScreen extends CardGridScreen {
    private final String faction;
    private List<VehicleEntry> vehicles = new ArrayList<>();
    private boolean loading = true;
    private boolean requested;
    private boolean preview;
    private String search = "";

    /** Запись пула техники (поля ответа DataRequest «factionVehicles»). */
    public record VehicleEntry(String vehicleName, String displayName, String vehicleId, String category) {}

    public FactionVehicleListScreen(String faction) {
        super(Component.translatable("gui.pwpwarfare.faction_vehicle_list.title", faction.toUpperCase()));
        this.faction = faction;
    }

    /** Тест-стенд UI: список из фикстуры, без запросов и удаления на сервере. */
    public static FactionVehicleListScreen preview(String faction, List<VehicleEntry> list) {
        FactionVehicleListScreen s = new FactionVehicleListScreen(faction);
        s.preview = true;
        s.vehicles = new ArrayList<>(list);
        s.loading = false;
        return s;
    }

    @Override
    protected void init() {
        super.init();
        if (!preview && !requested && loading) {
            requested = true;
            ClientData.factionVehiclesData = null;
            com.pwp.coreclient.network.PacketHandler.INSTANCE.sendToServer(
                    new DataRequest("factionVehicles", "{\"faction\":\"" + faction + "\"}"));
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (requested && ClientData.factionVehiclesData != null) {
            requested = false;
            JsonArray arr = ClientData.factionVehiclesData;
            ClientData.factionVehiclesData = null;
            List<VehicleEntry> loaded = new ArrayList<>();
            for (JsonElement e : arr) {
                JsonObject o = e.getAsJsonObject();
                loaded.add(new VehicleEntry(
                        str(o, "vehicleName"), str(o, "displayName"), str(o, "vehicleId"), str(o, "category")));
            }
            vehicles = loaded;
            loading = false;
        }
    }

    private static String str(JsonObject o, String k) {
        return o.has(k) && !o.get(k).isJsonNull() ? o.get(k).getAsString() : "";
    }

    @Override
    protected boolean loading() {
        return loading;
    }

    @Override
    protected WarfareIcon titleIcon() {
        return WarfareIcon.CRATE;
    }

    @Override
    protected Component emptyTitle() {
        return Component.translatable("gui.pwpwarfare.faction_vehicle_list.empty");
    }

    @Override
    protected Component emptyHint() {
        return search.isEmpty() ? null : Component.translatable("gui.pwpwarfare.admin.search_hint", search);
    }

    private boolean matches(VehicleEntry v) {
        if (search.isEmpty()) return true;
        String q = search.toLowerCase();
        return v.vehicleName().toLowerCase().contains(q)
                || v.displayName().toLowerCase().contains(q)
                || v.vehicleId().toLowerCase().contains(q)
                || v.category().toLowerCase().contains(q);
    }

    @Override
    protected List<Card> cards() {
        List<Card> out = new ArrayList<>();
        Component addTitle = search.isEmpty()
                ? Component.translatable("gui.pwpwarfare.faction_vehicle_list.add")
                : Component.translatable("gui.pwpwarfare.admin.search", search);
        out.add(Card.of("__add__", addTitle, () -> open(null))
                .subtitle(Component.translatable("gui.pwpwarfare.admin.type_to_search"))
                .icon(WarfareIcon.PLUS));
        for (VehicleEntry v : vehicles) {
            if (!matches(v)) continue;
            Component sub = Component.literal(v.displayName().isEmpty() ? v.vehicleId() : v.displayName());
            if (!v.category().isEmpty())
                sub = Component.literal(v.category() + " · ").append(sub);
            out.add(Card.of(v.vehicleName(), Component.literal(v.vehicleName()), () -> open(v.vehicleName()))
                    .subtitle(sub)
                    .icon(WarfareIcon.CRATE)
                    .accent(PWPTheme.Colors.ACCENT)
                    .actions(
                            new Action(
                                    WarfareIcon.TRASH,
                                    Component.translatable("gui.pwpwarfare.admin.delete"),
                                    () -> delete(v.vehicleName())),
                            new Action(
                                    WarfareIcon.EDIT,
                                    Component.translatable("gui.pwpwarfare.skin_editor.edit"),
                                    () -> open(v.vehicleName()))));
        }
        return out;
    }

    private void open(String vehicleName) {
        if (preview) return;
        Minecraft.getInstance().setScreen(new FactionVehicleEditorScreen(faction, vehicleName));
    }

    private void delete(String vehicleName) {
        if (preview) {
            vehicles.removeIf(v -> v.vehicleName().equals(vehicleName));
            return;
        }
        AdminActions.deleteFactionVehicle(faction, vehicleName, r -> {
            if (r.success()) vehicles.removeIf(v -> v.vehicleName().equals(vehicleName));
        });
    }

    @Override
    public boolean charTyped(char c, int modifiers) {
        if (c >= ' ' && search.length() < 64) {
            search += c;
            return true;
        }
        return super.charTyped(c, modifiers);
    }

    @Override
    public boolean keyPressed(int key, int scan, int modifiers) {
        if (key == GLFW.GLFW_KEY_BACKSPACE && !search.isEmpty()) {
            search = search.substring(0, search.length() - 1);
            return true;
        }
        return super.keyPressed(key, scan, modifiers);
    }
}
