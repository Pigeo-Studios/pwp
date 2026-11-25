package com.pigeostudios.pwp.warfare.client.gui;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.pigeostudios.pwp.warfare.client.ClientData;
import com.pigeostudios.pwp.warfare.client.gui.admin.AdminFormScreen;
import com.pigeostudios.pwp.warfare.client.gui.ui.WDraw;
import com.pigeostudios.pwp.warfare.client.gui.ui.WarfareIcon;
import com.pigeostudios.pwp.warfare.network.PacketHandler;
import com.pigeostudios.pwp.warfare.network.PacketSaveFactionVehicle;
import com.pigeostudios.pwp.warfare.world.WarfareWorldData;
import com.pwp.api.util.Parse;
import com.pwp.coreclient.gui.theme.PWPTheme;
import com.pwp.coreclient.gui.theme.PWPTheme.Spacing;
import com.pwp.coreclient.network.data.DataRequest;
import com.pwp.coreclient.ui.UIButton;
import com.pwp.coreclient.ui.UIDraw;
import com.pwp.coreclient.ui.UIIcon;
import com.pwp.coreclient.ui.layout.Rect;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

/**
 * Админ: машина пула фракции. Форма на {@link AdminFormScreen}: «Машина» (имя, отображаемое
 * имя, ID сущности, категория), «Спавн» (yaw, респавн/первый спавн, с), «Инвентарь» —
 * слот модификатора + 32 слота машины и инвентарь игрока (ЛКМ — взять/положить, ПКМ — по
 * одному/половину, как раньше). Загрузка — DataRequest «factionVehicle», сохранение —
 * {@link PacketSaveFactionVehicle} с прежними аргументами.
 */
public class FactionVehicleEditorScreen extends AdminFormScreen {
    private static final org.slf4j.Logger PWP_LOG = org.slf4j.LoggerFactory.getLogger(FactionVehicleEditorScreen.class);
    private static final int SLOT = 18;
    private static final int VEH_COLS = 8;
    private static final int VEH_ROWS = 4;
    private static final int INV_COLS = 9;

    private final String faction;
    private final String editVehicleName;
    private final boolean isNew;
    private boolean loading;
    private boolean requested;

    private String vehicleName = "";
    private String displayName = "";
    private String vehicleId = "";
    private float yaw = 0;
    private int respawnTime = 60;
    private int initialTime = 60;
    private String category = "";
    private final ItemStack[] vehicleInv = new ItemStack[33];
    private final Inventory playerInv;
    /** Курсор, если игрока нет (тест-стенд вне мира). */
    private ItemStack localCursor = ItemStack.EMPTY;

    private EditBox nameBox, displayBox, idBox, categoryBox, yawBox, respawnBox, initialBox;
    private Rect modR = Rect.of(0, 0, 0, 0);
    private Rect vehR = modR;
    private Rect invR = modR;

    public FactionVehicleEditorScreen(String faction, String vehicleName) {
        super(Component.translatable("gui.pwpwarfare.faction_vehicle_editor.title", faction.toUpperCase()));
        this.faction = faction;
        this.editVehicleName = vehicleName;
        this.isNew = vehicleName == null || vehicleName.isEmpty();
        java.util.Arrays.fill(vehicleInv, ItemStack.EMPTY);
        var p = Minecraft.getInstance().player;
        this.playerInv = p != null ? p.getInventory() : null;
        if (!isNew) {
            loading = true;
            this.vehicleName = vehicleName;
        }
    }

    /** Тест-стенд UI: значения из фикстуры, без запроса и сохранения. */
    public static FactionVehicleEditorScreen preview(
            String faction, String name, String id, String cat, ItemStack... inv) {
        FactionVehicleEditorScreen s = new FactionVehicleEditorScreen(faction, null);
        s.asPreview();
        s.vehicleName = name;
        s.displayName = name.toUpperCase();
        s.vehicleId = id;
        s.category = cat;
        for (int i = 0; i < inv.length && i < 33; i++) s.vehicleInv[i] = inv[i];
        return s;
    }

    @Override
    protected WarfareIcon titleIcon() {
        return WarfareIcon.CRATE;
    }

    @Override
    protected Component badge() {
        return loading ? Component.translatable("gui.pwpwarfare.grid.loading") : null;
    }

    @Override
    protected boolean twoColumns() {
        return true;
    }

    @Override
    protected void build() {
        if (loading && !requested && !preview) {
            requested = true;
            ClientData.factionVehicleDetail = null;
            com.pwp.coreclient.network.PacketHandler.INSTANCE.sendToServer(new DataRequest(
                    "factionVehicle", "{\"faction\":\"" + faction + "\",\"vehicle\":\"" + editVehicleName + "\"}"));
        }
        section(Component.translatable("gui.pwpwarfare.faction_vehicle_editor.section.vehicle"));
        nameBox = text(Component.translatable("gui.pwpwarfare.faction_vehicle_editor.name"), vehicleName, 64);
        nameBox.setResponder(v -> vehicleName = v);
        displayBox =
                text(Component.translatable("gui.pwpwarfare.faction_vehicle_editor.display_name"), displayName, 64);
        displayBox.setResponder(v -> displayName = v);
        idBox = text(Component.translatable("gui.pwpwarfare.vehicle_spawner.vehicle_id"), vehicleId, 128);
        idBox.setResponder(v -> vehicleId = v);
        categoryBox = text(Component.translatable("gui.pwpwarfare.faction_vehicle_editor.category"), category, 32);
        categoryBox.setResponder(v -> category = v);

        section(Component.translatable("gui.pwpwarfare.faction_vehicle_editor.section.spawn"));
        yawBox = number(Component.translatable("gui.pwpwarfare.vehicle_spawner.yaw"), (int) yaw, true);
        yawBox.setResponder(v -> {
            float f = Parse.floatOr(v, Float.NaN);
            if (!Float.isNaN(f)) yaw = ((f % 360F) + 360F) % 360F;
        });
        respawnBox = number(Component.translatable("gui.pwpwarfare.vehicle_spawner.respawn_time"), respawnTime, false);
        respawnBox.setResponder(v -> respawnTime = Math.max(0, Parse.intOr(v, respawnTime)));
        initialBox = number(Component.translatable("gui.pwpwarfare.vehicle_spawner.initial_time"), initialTime, false);
        initialBox.setResponder(v -> initialTime = Math.max(0, Parse.intOr(v, initialTime)));

        section(Component.translatable("gui.pwpwarfare.faction_vehicle_editor.section.inventory"));
        custom(VEH_ROWS * SLOT + Spacing.XS, this::renderVehicleInv);
        custom(4 * SLOT + Spacing.SM, this::renderPlayerInv);

        action(
                Component.translatable("gui.pwpwarfare.faction_vehicle_editor.back"),
                UIButton.Style.GHOST,
                UIIcon.BACK,
                this::back);
        action(
                Component.translatable("gui.pwpwarfare.faction_vehicle_editor.save"),
                UIButton.Style.ACCENT,
                UIIcon.CHECK,
                this::saveVehicle);
    }

    @Override
    public void tick() {
        super.tick();
        if (!requested || ClientData.factionVehicleDetail == null) return;
        requested = false;
        JsonObject data = ClientData.factionVehicleDetail;
        ClientData.factionVehicleDetail = null;
        if (data.has("vehicleName")) vehicleName = data.get("vehicleName").getAsString();
        if (data.has("displayName")) displayName = data.get("displayName").getAsString();
        if (data.has("vehicleId")) vehicleId = data.get("vehicleId").getAsString();
        if (data.has("yaw")) yaw = data.get("yaw").getAsFloat();
        if (data.has("respawnTime")) respawnTime = data.get("respawnTime").getAsInt();
        if (data.has("initialTime")) initialTime = data.get("initialTime").getAsInt();
        if (data.has("category")) category = data.get("category").getAsString();
        if (data.has("inventory")) readInventory(data.getAsJsonArray("inventory"));
        loading = false;
        nameBox.setValue(vehicleName);
        displayBox.setValue(displayName);
        idBox.setValue(vehicleId);
        categoryBox.setValue(category);
        yawBox.setValue(String.valueOf((int) yaw));
        respawnBox.setValue(String.valueOf(respawnTime));
        initialBox.setValue(String.valueOf(initialTime));
    }

    private void readInventory(JsonArray arr) {
        try {
            for (int i = 0; i < arr.size(); i++) {
                JsonObject itemJson = arr.get(i).getAsJsonObject();
                int slot = itemJson.get("slot").getAsInt();
                if (slot < 0 || slot >= 32 || !itemJson.has("item")) continue;
                JsonObject itemData = itemJson.getAsJsonObject("item");
                String id = itemData.has("id") ? itemData.get("id").getAsString() : "";
                int count = itemData.has("Count") ? itemData.get("Count").getAsInt() : 1;
                if (id.isEmpty() || id.equals("minecraft:air")) continue;
                var item = net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(
                        new net.minecraft.resources.ResourceLocation(id));
                if (item == null || item == net.minecraft.world.item.Items.AIR) continue;
                ItemStack stack = new ItemStack(item, count);
                if (itemData.has("tag") && itemData.get("tag").isJsonObject()) {
                    var tag = WarfareWorldData.KitInfo.jsonToCompound(itemData.getAsJsonObject("tag"));
                    if (!tag.isEmpty()) stack.setTag(tag);
                }
                vehicleInv[slot] = stack;
            }
        } catch (RuntimeException e) {
            PWP_LOG.warn("FactionVehicleEditorScreen: инвентарь машины не прочитан", e);
        }
    }

    private void saveVehicle() {
        if (vehicleName.trim().isEmpty()) {
            status(Component.translatable("gui.pwpwarfare.faction_vehicle_editor.need_name"), true);
            return;
        }
        if (preview) {
            status(Component.translatable("gui.pwpwarfare.admin.preview_skip"), false);
            return;
        }
        PacketHandler.INSTANCE.sendToServer(new PacketSaveFactionVehicle(
                faction,
                vehicleName.trim(),
                displayName,
                vehicleId,
                yaw,
                respawnTime,
                initialTime,
                category,
                vehicleInv));
        Minecraft.getInstance().setScreen(new FactionVehicleListScreen(faction));
    }

    private void back() {
        if (preview) return;
        setCursor(ItemStack.EMPTY);
        Minecraft.getInstance().setScreen(new FactionVehicleListScreen(faction));
    }

    // ================================================================ инвентари

    private void renderVehicleInv(GuiGraphics g, Rect r) {
        modR = Rect.of(r.x(), r.y(), SLOT + Spacing.XS, SLOT + Spacing.XS);
        vehR = Rect.of(modR.right() + Spacing.SM, r.y(), VEH_COLS * SLOT, VEH_ROWS * SLOT);
        WDraw.well(g, modR, 1F);
        UIDraw.outline(g, modR, PWPTheme.Colors.INFO);
        drawItem(g, modR.x() + 3, modR.y() + 3, vehicleInv[0]);
        UIDraw.text(
                g,
                font,
                Component.translatable("gui.pwpwarfare.faction_vehicle_editor.mod"),
                modR.x(),
                modR.bottom() + Spacing.XXS,
                PWPTheme.Text.CAPTION,
                false);
        for (int i = 0; i < VEH_COLS * VEH_ROWS; i++) {
            Rect s = Rect.of(vehR.x() + (i % VEH_COLS) * SLOT, vehR.y() + (i / VEH_COLS) * SLOT, SLOT, SLOT);
            WDraw.well(g, s, 1F);
            drawItem(g, s.x() + 1, s.y() + 1, vehicleInv[1 + i]);
        }
    }

    private void renderPlayerInv(GuiGraphics g, Rect r) {
        invR = Rect.of(r.x(), r.y() + Spacing.XS, INV_COLS * SLOT, 4 * SLOT + Spacing.XS);
        for (int i = 0; i < 36; i++) {
            Rect s = playerSlotRect(i);
            WDraw.well(g, s, 1F);
            if (playerInv != null) drawItem(g, s.x() + 1, s.y() + 1, playerInv.getItem(i));
        }
    }

    /** Слот игрока: 9..35 — три ряда сверху, 0..8 — хотбар снизу с зазором. */
    private Rect playerSlotRect(int slot) {
        if (slot < 9) return Rect.of(invR.x() + slot * SLOT, invR.y() + 3 * SLOT + Spacing.XS, SLOT, SLOT);
        int i = slot - 9;
        return Rect.of(invR.x() + (i % INV_COLS) * SLOT, invR.y() + (i / INV_COLS) * SLOT, SLOT, SLOT);
    }

    private void drawItem(GuiGraphics g, int x, int y, ItemStack stack) {
        if (stack == null || stack.isEmpty()) return;
        g.renderItem(stack, x, y);
        g.renderItemDecorations(font, stack, x, y);
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        super.render(g, mx, my, pt);
        ItemStack carried = getCursor();
        if (!carried.isEmpty()) {
            g.pose().pushPose();
            g.pose().translate(0, 0, 300);
            drawItem(g, mx - 8, my - 8, carried);
            g.pose().popPose();
        }
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (loading || (button != 0 && button != 1)) return super.mouseClicked(mx, my, button);
        if (modR.contains(mx, my)) {
            handleVehicleSlot(0, button);
            return true;
        }
        if (vehR.contains(mx, my)) {
            int col = (int) ((mx - vehR.x()) / SLOT), row = (int) ((my - vehR.y()) / SLOT);
            handleVehicleSlot(1 + row * VEH_COLS + col, button);
            return true;
        }
        if (playerInv != null && invR.contains(mx, my)) {
            for (int i = 0; i < 36; i++) {
                if (playerSlotRect(i).contains(mx, my)) {
                    handlePlayerSlot(i, button);
                    return true;
                }
            }
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
        } else if (!cursor.isEmpty()) {
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

    private void handlePlayerSlot(int slot, int button) {
        if (preview) return; // тест-стенд: настоящий инвентарь игрока не трогаем
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
        } else if (!cursor.isEmpty()) {
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

    private ItemStack getCursor() {
        var p = Minecraft.getInstance().player;
        return p != null ? p.containerMenu.getCarried() : localCursor;
    }

    private void setCursor(ItemStack stack) {
        var p = Minecraft.getInstance().player;
        if (p != null) p.containerMenu.setCarried(stack);
        else localCursor = stack;
    }

    @Override
    public void onClose() {
        setCursor(ItemStack.EMPTY);
        super.onClose();
    }
}
