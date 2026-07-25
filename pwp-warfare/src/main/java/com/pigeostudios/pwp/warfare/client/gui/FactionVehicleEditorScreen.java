package com.pigeostudios.pwp.warfare.client.gui;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.pigeostudios.pwp.warfare.network.PacketHandler;
import com.pigeostudios.pwp.warfare.network.PacketSaveFactionVehicle;
import com.pigeostudios.pwp.warfare.world.WarfareWorldData;
import com.pwp.coreclient.CoreAPI;
import com.pwp.coreclient.gui.theme.PWPTheme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import com.pwp.coreclient.gui.components.PWPButton;
import com.pwp.coreclient.gui.components.PWPPanel;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

public class FactionVehicleEditorScreen extends Screen {
    private final String faction;
    private final String editVehicleName;
    private boolean loading = true;
    private boolean isNew = true;

    private EditBox vehicleNameField;
    private EditBox displayNameField;
    private EditBox vehicleIdField;
    private EditBox yawField;
    private EditBox respawnTimeField;
    private EditBox initialTimeField;

    private String vehicleName = "";
    private String displayName = "";
    private String vehicleId = "";
    private float yaw = 0;
    private int respawnTime = 60;
    private int initialTime = 60;
    private ItemStack[] vehicleInv = new ItemStack[33];
    private Inventory playerInv;

    private static final int SLOT = 18;
    private static final int MOD_W = 22;
    private static final int VEH_COLS = 8;
    private static final int VEH_ROWS = 4;
    private static final int INV_COLS = 9;
    private static final int INV_ROWS = 3;

    private int panelW = 446;
    private int panelH;

    private int cx;
    private int modX, modY;
    private int vInvX, vInvY;
    private int pInvX, pInvY;
    private int hotY;

    public FactionVehicleEditorScreen(String faction, String vehicleName) {
        super(Component.translatable("gui.pwpwarfare.faction_vehicle_editor.title", faction.toUpperCase()));
        this.faction = faction;
        this.editVehicleName = vehicleName;
        this.isNew = (vehicleName == null || vehicleName.isEmpty());
        for (int i = 0; i < 33; i++) vehicleInv[i] = ItemStack.EMPTY;
        this.playerInv = Minecraft.getInstance().player != null ? Minecraft.getInstance().player.getInventory() : null;
    }

    @Override
    protected void init() {
        cx = (width - panelW) / 2;
        int fieldLeft = cx + 12;
        int fieldWide = 160;
        int rightX = cx + panelW - 120;
        int labelGap = 10;

        modX = cx + 12;
        modY = 108;
        vInvX = cx + 40;
        vInvY = 108;
        int pX = cx + 12;
        pInvX = pX;
        pInvY = 192;
        hotY = pInvY + INV_ROWS * SLOT + 4;
        panelH = hotY + SLOT + 34;

        vehicleNameField = new EditBox(PWPTheme.Fonts.display(), fieldLeft + 50, 28, 120, 16, Component.literal(""));
        vehicleNameField.setMaxLength(64);
        vehicleNameField.setValue(vehicleName);
        vehicleNameField.setResponder(val -> vehicleName = val);
        addRenderableWidget(vehicleNameField);

        displayNameField = new EditBox(PWPTheme.Fonts.display(), fieldLeft + 220, 28, fieldWide, 16, Component.literal(""));
        displayNameField.setMaxLength(128);
        displayNameField.setValue(displayName);
        displayNameField.setResponder(val -> displayName = val);
        addRenderableWidget(displayNameField);

        vehicleIdField = new EditBox(PWPTheme.Fonts.display(), fieldLeft + 65, 50, 180, 16, Component.literal(""));
        vehicleIdField.setMaxLength(64);
        vehicleIdField.setValue(vehicleId);
        vehicleIdField.setResponder(val -> vehicleId = val);
        addRenderableWidget(vehicleIdField);

        yawField = new EditBox(PWPTheme.Fonts.display(), fieldLeft + 290, 50, 42, 16, Component.literal(""));
        yawField.setValue(String.valueOf((int) yaw));
        yawField.setFilter(s -> s.matches("-?\\d*"));
        yawField.setResponder(val -> { try { yaw = Float.parseFloat(val); } catch (Exception ignored) {} });
        addRenderableWidget(yawField);

        addRenderableWidget(new PWPButton(fieldLeft + 334, 50, 16, 16, Component.literal("\u21BA"), b -> adjustYaw(-45.0F), PWPButton.Style.PRIMARY));
        addRenderableWidget(new PWPButton(fieldLeft + 352, 50, 16, 16, Component.literal("\u21BB"), b -> adjustYaw(45.0F), PWPButton.Style.PRIMARY));

        addRenderableWidget(new PWPButton(fieldLeft + 62, 74, 16, 16, Component.literal("-"), b -> adjustTimer(false, -5), PWPButton.Style.PRIMARY));
        respawnTimeField = new EditBox(PWPTheme.Fonts.display(), fieldLeft + 80, 74, 50, 16, Component.literal(""));
        respawnTimeField.setValue(String.valueOf(respawnTime));
        respawnTimeField.setResponder(val -> { try { respawnTime = Math.max(0, Integer.parseInt(val)); } catch (Exception ignored) {} });
        addRenderableWidget(respawnTimeField);
        addRenderableWidget(new PWPButton(fieldLeft + 132, 74, 16, 16, Component.literal("+"), b -> adjustTimer(false, 5), PWPButton.Style.PRIMARY));

        addRenderableWidget(new PWPButton(fieldLeft + 202, 74, 16, 16, Component.literal("-"), b -> adjustTimer(true, -5), PWPButton.Style.PRIMARY));
        initialTimeField = new EditBox(PWPTheme.Fonts.display(), fieldLeft + 220, 74, 50, 16, Component.literal(""));
        initialTimeField.setValue(String.valueOf(initialTime));
        initialTimeField.setResponder(val -> { try { initialTime = Math.max(0, Integer.parseInt(val)); } catch (Exception ignored) {} });
        addRenderableWidget(initialTimeField);
        addRenderableWidget(new PWPButton(fieldLeft + 272, 74, 16, 16, Component.literal("+"), b -> adjustTimer(true, 5), PWPButton.Style.PRIMARY));

        addRenderableWidget(new PWPButton(cx + panelW - 120, panelH - 28, 100, 20,
            Component.translatable("gui.pwpwarfare.faction_vehicle_editor.save"),
            b -> saveVehicle(),
            PWPButton.Style.ACCENT
        ));

        addRenderableWidget(new PWPButton(cx + 12, panelH - 28, 100, 20,
            Component.literal("< " + Component.translatable("gui.pwpwarfare.faction_vehicle_editor.back").getString()),
            b -> minecraft.setScreen(new FactionVehicleListScreen(faction)),
            PWPButton.Style.PRIMARY
        ));

        if (!isNew) loadVehicle();
        else loading = false;
    }

    private void loadVehicle() {
        new Thread(() -> {
            try {
                JsonObject result = CoreAPI.getFactionVehicle(faction, editVehicleName);
                if (result != null && result.has("data")) {
                    JsonObject data = result.getAsJsonObject("data");
                    Minecraft.getInstance().submit(() -> {
                        if (data.has("vehicleName")) vehicleName = data.get("vehicleName").getAsString();
                        if (data.has("displayName")) displayName = data.get("displayName").getAsString();
                        if (data.has("vehicleId")) vehicleId = data.get("vehicleId").getAsString();
                        if (data.has("yaw")) yaw = data.get("yaw").getAsFloat();
                        if (data.has("respawnTime")) respawnTime = data.get("respawnTime").getAsInt();
                        if (data.has("initialTime")) initialTime = data.get("initialTime").getAsInt();

                        if (data.has("inventory")) {
                            try {
                                JsonArray arr = data.getAsJsonArray("inventory");
                                for (int i = 0; i < arr.size(); i++) {
                                    JsonObject itemJson = arr.get(i).getAsJsonObject();
                                    int slot = itemJson.get("slot").getAsInt();
                                    if (slot >= 0 && slot < 32 && itemJson.has("item")) {
                                        JsonObject itemData = itemJson.getAsJsonObject("item");
                                        String id = itemData.has("id") ? itemData.get("id").getAsString() : "";
                                        int count = itemData.has("Count") ? itemData.get("Count").getAsInt() : 1;
                                        if (!id.isEmpty() && !id.equals("minecraft:air")) {
                                            var item = net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(new net.minecraft.resources.ResourceLocation(id));
                                            if (item != null && item != net.minecraft.world.item.Items.AIR) {
                                                ItemStack stack = new ItemStack(item, count);
                                                if (itemData.has("tag") && itemData.get("tag").isJsonObject()) {
                                                    var tag = WarfareWorldData.KitInfo.jsonToCompound(itemData.getAsJsonObject("tag"));
                                                    if (!tag.isEmpty()) stack.setTag(tag);
                                                }
                                                vehicleInv[slot] = stack;
                                            }
                                        }
                                    }
                                }
                            } catch (Exception ignored) {}
                        }
                        vehicleNameField.setValue(vehicleName);
                        displayNameField.setValue(displayName);
                        vehicleIdField.setValue(vehicleId);
                        yawField.setValue(String.valueOf((int) yaw));
                        respawnTimeField.setValue(String.valueOf(respawnTime));
                        initialTimeField.setValue(String.valueOf(initialTime));
                        loading = false;
                    });
                } else {
                    Minecraft.getInstance().submit(() -> loading = false);
                }
            } catch (Exception ex) {
                Minecraft.getInstance().submit(() -> loading = false);
            }
        }, "PWP-FactionVehicle-Load").start();
    }

    private void saveVehicle() {
        if (vehicleName.trim().isEmpty()) {
            minecraft.player.displayClientMessage(Component.literal("Vehicle name cannot be empty!"), true);
            return;
        }
        PacketHandler.INSTANCE.sendToServer(new PacketSaveFactionVehicle(
            faction, vehicleName.trim(), displayName, vehicleId, yaw, respawnTime, initialTime, vehicleInv
        ));
        minecraft.setScreen(new FactionVehicleListScreen(faction));
    }

    private void adjustTimer(boolean isInitial, int change) {
        if (isInitial) {
            initialTime = Math.max(0, initialTime + change);
            initialTimeField.setValue(String.valueOf(initialTime));
        } else {
            respawnTime = Math.max(0, respawnTime + change);
            respawnTimeField.setValue(String.valueOf(respawnTime));
        }
    }

    private void adjustYaw(float amount) {
        yaw = (yaw + amount) % 360.0F;
        if (yaw < 0) yaw += 360.0F;
        yawField.setValue(String.valueOf((int) yaw));
    }

    // --- Drawing ---

    @Override
    public void render(GuiGraphics gui, int mx, int my, float pt) {
        renderBackground(gui);

        PWPPanel.render(gui, cx, 8, panelW, panelH);
        gui.drawCenteredString(PWPTheme.Fonts.display(), title.getString(), width / 2, 12, PWPTheme.Colors.TEXT_ACCENT);

        if (loading) {
            gui.drawCenteredString(PWPTheme.Fonts.display(), Component.translatable("gui.pwpwarfare.faction_select.loading"), width / 2, height / 2, PWPTheme.Colors.TEXT_DIM);
            super.render(gui, mx, my, pt);
            return;
        }

        int fl = cx + 12;
        gui.drawString(PWPTheme.Fonts.display(), "Name ID:", fl, 32, PWPTheme.Colors.TEXT_SECONDARY, false);
        gui.drawString(PWPTheme.Fonts.display(), "Display:", fl + 170, 32, PWPTheme.Colors.TEXT_SECONDARY, false);
        gui.drawString(PWPTheme.Fonts.display(), "Vehicle ID:", fl, 54, PWPTheme.Colors.TEXT_SECONDARY, false);
        gui.drawString(PWPTheme.Fonts.display(), "Yaw:", fl + 260, 54, PWPTheme.Colors.TEXT_SECONDARY, false);
        gui.drawString(PWPTheme.Fonts.display(), "Respawn:", fl, 78, PWPTheme.Colors.TEXT_SECONDARY, false);
        gui.drawString(PWPTheme.Fonts.display(), "Initial:", fl + 152, 78, PWPTheme.Colors.TEXT_SECONDARY, false);

        gui.drawString(PWPTheme.Fonts.display(), "Modifier", modX, modY - 10, PWPTheme.Colors.TEXT_ACCENT, false);
        gui.drawString(PWPTheme.Fonts.display(), "Items (32)", vInvX, vInvY - 10, PWPTheme.Colors.TEXT_ACCENT, false);

        // modifier slot (slot 0)
        gui.fill(modX - 2, modY - 2, modX + MOD_W + 2, modY + MOD_W + 2, PWPTheme.Colors.SURFACE_LIGHT);
        gui.renderOutline(modX - 2, modY - 2, MOD_W + 4, MOD_W + 4, PWPTheme.Colors.BORDER);
        drawSlot(gui, modX, modY, vehicleInv[0]);

        // vehicle inventory slots (1-32)
        int vw = VEH_COLS * SLOT + 4;
        int vh = VEH_ROWS * SLOT + 4;
        gui.fill(vInvX - 2, vInvY - 2, vInvX + vw, vInvY + vh, PWPTheme.Colors.SURFACE_LIGHT);
        gui.renderOutline(vInvX - 2, vInvY - 2, vw + 2, vh + 2, PWPTheme.Colors.BORDER);
        for (int r = 0; r < VEH_ROWS; r++) {
            for (int c = 0; c < VEH_COLS; c++) {
                int idx = 1 + r * VEH_COLS + c;
                int sx = vInvX + c * SLOT;
                int sy = vInvY + r * SLOT;
                drawSlot(gui, sx, sy, vehicleInv[idx]);
            }
        }

        // player inventory
        gui.drawString(PWPTheme.Fonts.display(), "Player Inventory", cx + 12, pInvY - 10, PWPTheme.Colors.TEXT_ACCENT, false);
        int pw = INV_COLS * SLOT + 4;
        int ph = INV_ROWS * SLOT + 4;
        gui.fill(pInvX - 2, pInvY - 2, pInvX + pw, pInvY + ph, PWPTheme.Colors.SURFACE_LIGHT);
        gui.renderOutline(pInvX - 2, pInvY - 2, pw + 2, ph + 2, PWPTheme.Colors.BORDER);
        if (playerInv != null) {
            for (int r = 0; r < INV_ROWS; r++) {
                for (int c = 0; c < INV_COLS; c++) {
                    int idx = 9 + r * INV_COLS + c;
                    int sx = pInvX + c * SLOT;
                    int sy = pInvY + r * SLOT;
                    drawSlot(gui, sx, sy, playerInv.getItem(idx));
                }
            }
        }

        // hotbar
        gui.fill(pInvX - 2, hotY - 2, pInvX + INV_COLS * SLOT + 2, hotY + SLOT + 2, PWPTheme.Colors.SURFACE_LIGHT);
        gui.renderOutline(pInvX - 2, hotY - 2, INV_COLS * SLOT + 4, SLOT + 4, PWPTheme.Colors.BORDER);
        if (playerInv != null) {
            for (int c = 0; c < INV_COLS; c++) {
                int sx = pInvX + c * SLOT;
                drawSlot(gui, sx, hotY, playerInv.getItem(c));
            }
        }

        super.render(gui, mx, my, pt);
    }

    private void drawSlot(GuiGraphics gui, int x, int y, ItemStack stack) {
        gui.fill(x, y, x + SLOT - 2, y + SLOT - 2, PWPTheme.Styles.Panel.BG);
        if (!stack.isEmpty()) {
            gui.renderItem(stack, x, y);
            gui.renderItemDecorations(PWPTheme.Fonts.display(), stack, x, y);
        }
    }

    // --- Mouse input ---

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (loading) return super.mouseClicked(mx, my, button);
        if (button != 0 && button != 1) return super.mouseClicked(mx, my, button);

        // modifier slot (slot 0)
        if (mx >= modX && mx < modX + MOD_W && my >= modY && my < modY + MOD_W) {
            handleVehicleSlot(0, button);
            return true;
        }

        // vehicle inventory slots (1-32)
        int col = (int) ((mx - vInvX) / SLOT);
        int row = (int) ((my - vInvY) / SLOT);
        if (col >= 0 && col < VEH_COLS && row >= 0 && row < VEH_ROWS) {
            handleVehicleSlot(1 + row * VEH_COLS + col, button);
            return true;
        }

        // player main inventory slots
        col = (int) ((mx - pInvX) / SLOT);
        row = (int) ((my - pInvY) / SLOT);
        if (col >= 0 && col < INV_COLS && row >= 0 && row < INV_ROWS && playerInv != null) {
            handlePlayerSlot(9 + row * INV_COLS + col, button);
            return true;
        }

        // player hotbar slots
        col = (int) ((mx - pInvX) / SLOT);
        if (col >= 0 && col < INV_COLS && my >= hotY && my < hotY + SLOT && playerInv != null) {
            handlePlayerSlot(col, button);
            return true;
        }

        return super.mouseClicked(mx, my, button);
    }

    private void handleVehicleSlot(int slot, int button) {
        ItemStack cursor = getCursor();
        ItemStack stack = vehicleInv[slot];

        if (button == 0) {
            if (!cursor.isEmpty()) {
                vehicleInv[slot] = cursor.copy();
                setCursor(stack.isEmpty() ? ItemStack.EMPTY : stack);
            } else if (!stack.isEmpty()) {
                setCursor(stack.copy());
                vehicleInv[slot] = ItemStack.EMPTY;
            }
        } else {
            if (!cursor.isEmpty()) {
                ItemStack one = cursor.copy();
                one.setCount(1);
                if (stack.isEmpty()) {
                    vehicleInv[slot] = one;
                    cursor.shrink(1);
                    setCursor(cursor.getCount() <= 0 ? ItemStack.EMPTY : cursor);
                } else if (ItemStack.isSameItemSameTags(stack, one) && stack.getCount() < stack.getMaxStackSize()) {
                    stack.grow(1);
                    cursor.shrink(1);
                    setCursor(cursor.getCount() <= 0 ? ItemStack.EMPTY : cursor);
                }
            } else if (!stack.isEmpty()) {
                int take = (stack.getCount() + 1) / 2;
                ItemStack half = stack.copy();
                half.setCount(take);
                stack.shrink(take);
                if (stack.getCount() <= 0) vehicleInv[slot] = ItemStack.EMPTY;
                setCursor(half);
            }
        }
    }

    private void handlePlayerSlot(int slot, int button) {
        ItemStack cursor = getCursor();
        ItemStack stack = playerInv.getItem(slot);

        if (button == 0) {
            if (!cursor.isEmpty()) {
                playerInv.setItem(slot, cursor.copy());
                setCursor(stack.isEmpty() ? ItemStack.EMPTY : stack);
            } else if (!stack.isEmpty()) {
                setCursor(stack.copy());
                playerInv.setItem(slot, ItemStack.EMPTY);
            }
        } else {
            if (!cursor.isEmpty()) {
                ItemStack one = cursor.copy();
                one.setCount(1);
                if (stack.isEmpty()) {
                    playerInv.setItem(slot, one);
                    cursor.shrink(1);
                    setCursor(cursor.getCount() <= 0 ? ItemStack.EMPTY : cursor);
                } else if (ItemStack.isSameItemSameTags(stack, one) && stack.getCount() < stack.getMaxStackSize()) {
                    stack.grow(1);
                    cursor.shrink(1);
                    setCursor(cursor.getCount() <= 0 ? ItemStack.EMPTY : cursor);
                }
            } else if (!stack.isEmpty()) {
                int take = (stack.getCount() + 1) / 2;
                ItemStack half = stack.copy();
                half.setCount(take);
                stack.shrink(take);
                if (stack.getCount() <= 0) playerInv.setItem(slot, ItemStack.EMPTY);
                setCursor(half);
            }
        }
    }

    private ItemStack getCursor() {
        return minecraft.player.containerMenu.getCarried();
    }

    private void setCursor(ItemStack stack) {
        minecraft.player.containerMenu.setCarried(stack);
    }

    @Override
    public void onClose() {
        setCursor(ItemStack.EMPTY);
        super.onClose();
    }
}
