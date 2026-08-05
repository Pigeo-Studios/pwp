package com.pigeostudios.pwp.warfare.client.gui;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.pigeostudios.pwp.warfare.client.ClientData;
import com.pigeostudios.pwp.warfare.client.gui.FactionVehicleEditorScreen;
import com.pigeostudios.pwp.warfare.network.PacketHandler;
import com.pigeostudios.pwp.warfare.network.PacketRequestData;
import com.pwp.coreclient.CoreAPI;
import com.pwp.coreclient.gui.theme.PWPTheme;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import com.pwp.coreclient.gui.components.PWPButton;
import com.pwp.coreclient.gui.components.PWPPanel;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class FactionVehicleListScreen extends Screen {
    private final String faction;
    private static final int CARD_W = 180;
    private static final int CARD_H = 36;
    private static final int GAP = 6;

    private List<VehicleEntry> vehicles = new ArrayList<>();
    private List<VehicleEntry> filtered = new ArrayList<>();
    private boolean loading = true;
    private boolean requested;
    private int panelW;
    private int panelH;
    private int contentH;
    private int scrollOff;
    private int maxScroll;
    private int cx;
    private EditBox searchField;
    private String searchText = "";

    private static class VehicleEntry {
        String vehicleName;
        String displayName;
        String vehicleId;
        String category;

        VehicleEntry(String vehicleName, String displayName, String vehicleId, String category) {
            this.vehicleName = vehicleName;
            this.displayName = displayName;
            this.vehicleId = vehicleId;
            this.category = category;
        }
    }

    public FactionVehicleListScreen(String faction) {
        super(Component.translatable("gui.pwpwarfare.faction_vehicle_list.title", faction.toUpperCase()));
        this.faction = faction;
    }

    @Override
    protected void init() {
        panelW = Math.min(width - 40, 400);
        cx = (width - panelW) / 2;
        panelH = Math.min(height - 60, 400);
        maxScroll = 0;
        scrollOff = 0;

        searchField = new EditBox(PWPTheme.Fonts.display(), cx + 10, 34, panelW - 20, 16, Component.literal(""));
        searchField.setMaxLength(64);
        searchField.setValue(searchText);
        searchField.setResponder(val -> {
            searchText = val.toLowerCase();
            updateFiltered();
            repositionButtons();
        });
        addRenderableWidget(searchField);

        loadVehicles();
    }

    private void loadVehicles() {
        loading = true;
        requested = true;
        ClientData.factionVehiclesData = null;
        PacketHandler.INSTANCE.sendToServer(new PacketRequestData("factionVehicles",
            "{\"faction\":\"" + faction + "\"}"));
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
                JsonObject obj = e.getAsJsonObject();
                String vn = obj.has("vehicleName") ? obj.get("vehicleName").getAsString() : "";
                String dn = obj.has("displayName") ? obj.get("displayName").getAsString() : "";
                String vi = obj.has("vehicleId") ? obj.get("vehicleId").getAsString() : "";
                String cat = obj.has("category") ? obj.get("category").getAsString() : "";
                loaded.add(new VehicleEntry(vn, dn, vi, cat));
            }
            vehicles = loaded;
            loading = false;
            updateFiltered();
            recreateWidgets();
        }
    }

    private void updateFiltered() {
        filtered = new ArrayList<>();
        for (VehicleEntry v : vehicles) {
            if (searchText.isEmpty() || v.vehicleName.toLowerCase().contains(searchText)
                || v.displayName.toLowerCase().contains(searchText)
                || v.vehicleId.toLowerCase().contains(searchText)
                || v.category.toLowerCase().contains(searchText)) {
                filtered.add(v);
            }
        }
    }

    private void recreateWidgets() {
        clearWidgets();
        addRenderableWidget(searchField);

        if (!loading) {
            int rows = filtered.size();
            contentH = rows * (CARD_H + GAP);
            int availH = panelH - 56;
            maxScroll = Math.max(0, contentH - availH);
            if (maxScroll == 0) scrollOff = 0;
            if (scrollOff > maxScroll) scrollOff = maxScroll;

            PWPButton addBtn = new PWPButton(cx + panelW - 80, 8, 70, 20,
                Component.translatable("gui.pwpwarfare.faction_vehicle_list.add"),
                b -> minecraft.setScreen(new FactionVehicleEditorScreen(faction, null)),
                PWPButton.Style.PRIMARY
            );
            addRenderableWidget(addBtn);

            repositionButtons();
        }
    }

    private void repositionButtons() {
        clearWidgets();
        addRenderableWidget(searchField);

        PWPButton addBtn = new PWPButton(cx + panelW - 80, 8, 70, 20,
            Component.translatable("gui.pwpwarfare.faction_vehicle_list.add"),
                b -> minecraft.setScreen(new FactionVehicleEditorScreen(faction, null)),
                PWPButton.Style.PRIMARY
        );
        addRenderableWidget(addBtn);

        for (int i = 0; i < filtered.size(); i++) {
            VehicleEntry entry = filtered.get(i);
            int by = 56 + i * (CARD_H + GAP) - scrollOff;

            PWPButton editBtn = new PWPButton(cx + panelW - 70, by + 6, 50, 22,
                Component.literal("Edit"),
                b -> minecraft.setScreen(new FactionVehicleEditorScreen(faction, entry.vehicleName)),
                PWPButton.Style.PRIMARY
            );
            addRenderableWidget(editBtn);

            PWPButton delBtn = new PWPButton(cx + panelW - 24, by + 6, 16, 22,
                Component.literal("X"),
                b -> deleteVehicle(entry.vehicleName),
                PWPButton.Style.DANGER
            );
            addRenderableWidget(delBtn);
        }
    }

    private void deleteVehicle(String vehicleName) {
        new Thread(() -> {
            try {
                CoreAPI.deleteFactionVehicle(faction, vehicleName);
            } catch (Exception ignored) {}
            Minecraft.getInstance().submit(() -> {
                vehicles.removeIf(v -> v.vehicleName.equals(vehicleName));
                updateFiltered();
                repositionButtons();
            });
        }, "PWP-FactionVehicle-Delete").start();
    }

    @Override
    public void render(GuiGraphics gui, int mx, int my, float pt) {
        renderBackground(gui);

        int py = 8;
        PWPPanel.render(gui, cx, py, panelW, panelH);
        gui.drawCenteredString(PWPTheme.Fonts.display(), title.getString(), width / 2, py + 10, PWPTheme.Colors.TEXT_ACCENT);

        int clipY = py + 52;
        int clipH = panelH - 54;
        gui.enableScissor(cx, clipY, cx + panelW, clipY + clipH);

        if (loading) {
            gui.drawCenteredString(PWPTheme.Fonts.display(), Component.translatable("gui.pwpwarfare.faction_select.loading"), width / 2, height / 2, PWPTheme.Colors.TEXT_DIM);
        } else if (filtered.isEmpty()) {
            gui.drawCenteredString(PWPTheme.Fonts.display(), Component.translatable("gui.pwpwarfare.faction_vehicle_list.empty"), width / 2, clipY + 20, PWPTheme.Colors.TEXT_DIM);
        } else {
            for (int i = 0; i < filtered.size(); i++) {
                VehicleEntry entry = filtered.get(i);
                int by = 56 + i * (CARD_H + GAP) - scrollOff;

                gui.fill(cx + 8, by, cx + panelW - 8, by + CARD_H, PWPTheme.Colors.SURFACE_LIGHT);
                gui.renderOutline(cx + 8, by, panelW - 16, CARD_H, PWPTheme.Colors.BORDER);
                gui.drawString(PWPTheme.Fonts.display(), entry.vehicleName, cx + 14, by + 4, PWPTheme.Colors.TEXT_ACCENT, false);
                String info = entry.displayName.isEmpty() ? entry.vehicleId : entry.displayName;
                gui.drawString(PWPTheme.Fonts.display(), info, cx + 14, by + 16, PWPTheme.Colors.TEXT_DIM, false);
                if (!entry.category.isEmpty()) {
                    gui.drawString(PWPTheme.Fonts.display(), entry.category, cx + panelW - 60, by + 16, PWPTheme.Colors.TEXT_SECONDARY, false);
                }
            }
        }

        gui.disableScissor();
        super.render(gui, mx, my, pt);

        if (maxScroll > 0) {
            if (scrollOff > 0)
                gui.drawCenteredString(PWPTheme.Fonts.display(), Component.literal("\u25B2"), width / 2, py + 2, PWPTheme.Colors.TEXT_DIM);
            if (scrollOff < maxScroll)
                gui.drawCenteredString(PWPTheme.Fonts.display(), Component.literal("\u25BC"), width / 2, py + panelH - 4, PWPTheme.Colors.TEXT_DIM);
        }
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        if (maxScroll > 0 && mx >= cx && mx <= cx + panelW) {
            int prev = scrollOff;
            scrollOff = (int) Math.max(0, Math.min(maxScroll, scrollOff - delta * 20));
            if (prev != scrollOff) repositionButtons();
            return true;
        }
        return super.mouseScrolled(mx, my, delta);
    }
}
