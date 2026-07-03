package com.pigeostudios.pwp.warfare.client.gui;

import com.google.gson.JsonObject;
import com.pwp.coreclient.CoreAPI;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

public class SkinEditorScreen extends Screen {

    private final SkinListScreen parent;
    private EditBox nameBox;
    private String slotType = "PRIMARY";
    private String weaponTag = "any";
    private String rarity = "COMMON";
    private ItemStack previewItem = ItemStack.EMPTY;
    private String previewRegistryName = "";
    private String previewItemName = "";
    private String statusMsg = "";
    private long statusTime = 0;

    private String editSkinId = null;
    private String titleText = "Add Skin";
    private String pendingEditName = null;
    private String pendingEditRegistryName = null;

    private static final String[] SLOT_TYPES = {"PRIMARY", "SECONDARY", "KNIFE", "MELEE", "UNIFORM"};
    private static final String[] RARITIES = {"COMMON", "UNCOMMON", "RARE", "EPIC", "LEGENDARY", "MYTHIC"};
    private static final int[] RARITY_COLORS = {0xFF6A6D73, 0xFF3D6FA5, 0xFF7A4A8A, 0xFFC8812A, 0xFFA53D3D, 0xFFFFD700};
    private static final int INV_COLS = 9;
    private static final int INV_ROWS = 4;
    private static final int SLOT_SIZE = 18;

    public SkinEditorScreen(SkinListScreen parent) {
        super(Component.literal("Add Skin"));
        this.parent = parent;
    }

    public void editExisting(String skinId, String name, String st, String wt, String r, String registryName) {
        titleText = "Edit Skin";
        editSkinId = skinId;
        slotType = st;
        weaponTag = wt;
        rarity = r;
        pendingEditName = name;
        pendingEditRegistryName = registryName;
    }

    @Override
    protected void init() {
        int cx = this.width / 2;

        nameBox = new EditBox(this.font, cx - 70, 30, 140, 16, Component.empty());
        nameBox.setHint(Component.literal("e.g. Dragon Fang"));
        if (pendingEditName != null) {
            nameBox.setValue(pendingEditName);
            if (pendingEditRegistryName != null && pendingEditRegistryName.startsWith("{")) {
                previewRegistryName = pendingEditRegistryName;
                try {
                    CompoundTag loaded = TagParser.parseTag(pendingEditRegistryName);
                    previewItem = ItemStack.of(loaded);
                    ResourceLocation rl = ForgeRegistries.ITEMS.getKey(previewItem.getItem());
                    previewItemName = rl != null ? rl.toString() : "unknown";
                } catch (Exception ignored) {}
            }
        }
        addRenderableWidget(nameBox);

        int btnY = 56;
        for (int i = 0; i < SLOT_TYPES.length; i++) {
            int bx = cx - 160 + i * 64;
            final int fi = i;
            String label = SLOT_TYPES[i].charAt(0) + SLOT_TYPES[i].substring(1).toLowerCase();
            addRenderableWidget(Button.builder(Component.literal(label), b -> {
                slotType = SLOT_TYPES[fi];
            }).bounds(bx, btnY, 60, 18).build());
        }

        int rarityY = 78;
        for (int i = 0; i < RARITIES.length; i++) {
            int bx = cx - 170 + i * 57;
            final int fi = i;
            addRenderableWidget(Button.builder(Component.literal(RARITIES[i].substring(0, 3)), b -> {
                rarity = RARITIES[fi];
            }).bounds(bx, rarityY, 53, 16).build());
        }

        addRenderableWidget(Button.builder(Component.literal("Save"), b -> save())
            .bounds(cx - 60, 230, 50, 20).build());
        Button delBtn = Button.builder(Component.literal("Delete"), b -> delete())
            .bounds(cx - 5, 230, 50, 20).build();
        delBtn.active = editSkinId != null;
        addRenderableWidget(delBtn);
        addRenderableWidget(Button.builder(Component.literal("Back"), b -> close())
            .bounds(cx + 50, 230, 50, 20).build());
    }

    @Override
    public void render(GuiGraphics gui, int mx, int my, float pt) {
        renderBackground(gui);
        int cx = this.width / 2;

        gui.drawCenteredString(this.font, titleText, cx, 8, 0xFFC8CBCE);
        gui.drawString(this.font, "Name:", cx - 120, 33, 0xFF7A7D84, false);
        gui.drawString(this.font, "Type:", cx - 120, 59, 0xFF7A7D84, false);
        gui.drawString(this.font, "Rarity:", cx - 120, 81, 0xFF7A7D84, false);

        int previewX = cx - 120;
        int previewY = 100;
        gui.drawString(this.font, "Preview:", previewX, previewY, 0xFF7A7D84, false);
        gui.fill(previewX, previewY + 10, previewX + 50, previewY + 60, 0xCC12151A);
        if (!previewItem.isEmpty()) {
            gui.renderItem(previewItem, previewX + 9, previewY + 14);
            gui.renderItemDecorations(this.font, previewItem, previewX + 9, previewY + 14);
            String name = previewItemName.isEmpty() ? "unknown" : previewItemName;
            if (name.length() > 28) name = name.substring(0, 25) + "...";
            gui.drawString(this.font, name, previewX + 56, previewY + 14, 0xFF3D6FA5, false);
            gui.drawString(this.font, "Slot: " + slotType, previewX + 56, previewY + 26, 0xFFC8812A, false);
            gui.drawString(this.font, "Rarity: " + rarity, previewX + 56, previewY + 38, RARITY_COLORS[java.util.Arrays.asList(RARITIES).indexOf(rarity)], false);
        } else {
            gui.drawCenteredString(this.font, "?", previewX + 25, previewY + 30, 0xFF4A4D54);
        }

        int invY = 164;
        gui.drawString(this.font, "Click an item to set as preview:", cx - 120, invY - 12, 0xFF4A4D54, false);
        int gridWidth = INV_COLS * (SLOT_SIZE + 2);
        int invStartX = (this.width - gridWidth) / 2;

        if (mc.player != null) {
            for (int i = 0; i < 36; i++) {
                int col = i % INV_COLS;
                int row = i / INV_COLS;
                int ix = invStartX + col * (SLOT_SIZE + 2);
                int iy = invY + row * (SLOT_SIZE + 2);
                ItemStack stack = mc.player.getInventory().items.get(i);
                gui.fill(ix, iy, ix + SLOT_SIZE, iy + SLOT_SIZE, 0x2212151A);
                if (!stack.isEmpty()) {
                    gui.renderItem(stack, ix + 1, iy + 1);
                    if (mx >= ix && mx <= ix + SLOT_SIZE && my >= iy && my <= iy + SLOT_SIZE) {
                        gui.renderTooltip(this.font, stack, mx, my);
                    }
                }
            }
        }

        if (System.currentTimeMillis() - statusTime < 3000) {
            gui.drawCenteredString(this.font, statusMsg, cx, this.height - 30, 0xFFC8812A);
        }

        super.render(gui, mx, my, pt);
    }

    private final Minecraft mc = Minecraft.getInstance();

    @Override
    public boolean mouseClicked(double mx, double my, int btn) {
        int gridWidth = INV_COLS * (SLOT_SIZE + 2);
        int invStartX = (this.width - gridWidth) / 2;
        if (mc.player != null) {
            for (int i = 0; i < 36; i++) {
                int col = i % INV_COLS;
                int row = i / INV_COLS;
                int ix = invStartX + col * (SLOT_SIZE + 2);
                int iy = 164 + row * (SLOT_SIZE + 2);
                if (mx >= ix && mx <= ix + SLOT_SIZE && my >= iy && my <= iy + SLOT_SIZE) {
                    ItemStack stack = mc.player.getInventory().items.get(i);
                    if (!stack.isEmpty()) setPreviewItem(stack);
                    return true;
                }
            }
        }
        return super.mouseClicked(mx, my, btn);
    }

    private void setPreviewItem(ItemStack stack) {
        previewItem = stack.copy();
        previewItem.setCount(1);
        CompoundTag tag = new CompoundTag();
        stack.save(tag);
        previewRegistryName = tag.toString();
        ResourceLocation key = ForgeRegistries.ITEMS.getKey(stack.getItem());
        previewItemName = key != null ? key.toString() : "unknown";
        String id = key != null ? key.toString() : "";
        if (id.contains("knife") || id.contains("bayonet") || id.contains("dagger") || id.contains("sword")) {
            slotType = "KNIFE"; weaponTag = "knife";
        } else if (id.contains("pistol") || id.contains("deagle") || id.contains("glock") || id.contains("revolver")) {
            slotType = "SECONDARY"; weaponTag = id.contains("deagle") ? "deagle" : "pistol";
        } else {
            slotType = "PRIMARY";
            if (id.contains("ak")) weaponTag = "ak47";
            else if (id.contains("m4")) weaponTag = "m4";
            else if (id.contains("sniper") || id.contains("l96") || id.contains("svd")) weaponTag = "sniper";
            else if (id.contains("lmg") || id.contains("pk") || id.contains("rpk")) weaponTag = "lmg";
            else if (id.contains("shotgun") || id.contains("spas")) weaponTag = "shotgun";
            else if (id.contains("smg") || id.contains("mp5") || id.contains("uzi")) weaponTag = "smg";
            else if (id.contains("rifle") || id.contains("carbine")) weaponTag = "rifle";
            else weaponTag = "any";
        }
    }

    private void save() {
        String name = nameBox.getValue().trim();
        if (name.isEmpty()) { showMsg("§eEnter a name!"); return; }
        if (previewItem.isEmpty()) { showMsg("§eClick an item from inventory!"); return; }

        String skinId = editSkinId;
        if (skinId == null) {
            String base = name.toLowerCase().replaceAll("[^a-z0-9_]", "_").replaceAll("_+", "_").replaceAll("^_|_$", "");
            String itemHash = String.valueOf(Math.abs(previewRegistryName.hashCode()));
            skinId = itemHash + "_" + base;
        }

        showMsg("§eSaving...");
        new Thread(() -> {
            try {
                JsonObject result = CoreAPI.saveSkin(skinId, name, "", slotType, weaponTag, rarity, previewRegistryName);
                if (result != null && result.has("success") && result.get("success").getAsBoolean()) {
                    mc.submit(() -> { showMsg("§aSaved!"); close(); });
                } else {
                    String err = result != null && result.has("error") ? result.get("error").getAsString() : "API returned null";
                    mc.submit(() -> showMsg("§c" + err));
                }
            } catch (Exception e) {
                mc.submit(() -> showMsg("§cError: " + e.getMessage()));
            }
        }).start();
    }

    private void delete() {
        if (editSkinId == null) { showMsg("§eSave first, then delete"); return; }
        showMsg("§eDeleting...");
        new Thread(() -> {
            try {
                CoreAPI.deleteSkin(editSkinId);
                Thread.sleep(200);
                mc.submit(() -> { showMsg("§aDeleted!"); close(); });
            } catch (Exception e) {
                mc.submit(() -> showMsg("§cError: " + e.getMessage()));
            }
        }, "PWP-Skin-Delete").start();
    }

    private void showMsg(String msg) { statusMsg = msg; statusTime = System.currentTimeMillis(); }
    private void close() { mc.setScreen(new SkinListScreen()); }
    @Override public boolean isPauseScreen() { return false; }
}
