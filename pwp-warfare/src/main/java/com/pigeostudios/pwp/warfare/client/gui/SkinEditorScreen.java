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

        nameBox = new EditBox(this.font, cx - 80, 30, 160, 18, Component.empty());
        nameBox.setHint(Component.literal("e.g. Dragon Fang"));
        if (pendingEditName != null) {
            nameBox.setValue(pendingEditName);
            if (pendingEditRegistryName != null && !pendingEditRegistryName.isEmpty()) {
                previewRegistryName = pendingEditRegistryName;
                if (pendingEditRegistryName.startsWith("{")) {
                    try {
                        CompoundTag loaded = TagParser.parseTag(pendingEditRegistryName);
                        previewItem = ItemStack.of(loaded);
                        ResourceLocation rl = ForgeRegistries.ITEMS.getKey(previewItem.getItem());
                        previewItemName = rl != null ? rl.toString() : "unknown";
                    } catch (Exception ignored) {}
                }
            }
        }
        addRenderableWidget(nameBox);

        for (int i = 0; i < SLOT_TYPES.length; i++) {
            int bx = cx - 165 + i * 65;
            boolean sel = slotType.equals(SLOT_TYPES[i]);
            int fi = i;
            String label = sel ? "> " + SLOT_TYPES[i] + " <" : SLOT_TYPES[i];
            addRenderableWidget(Button.builder(Component.literal(label), b -> {
                slotType = SLOT_TYPES[fi];
                String text = nameBox.getValue();
                int cursor = nameBox.getCursorPosition();
                init();
                nameBox.setValue(text);
                nameBox.setCursorPosition(cursor);
                setFocused(nameBox);
            }).bounds(bx, 56, 70, 20).build());
        }

        for (int i = 0; i < RARITIES.length; i++) {
            int bx = cx - 180 + i * 60;
            boolean sel = rarity.equals(RARITIES[i]);
            int fi = i;
            String label = sel ? "> " + RARITIES[i] + " <" : RARITIES[i];
            addRenderableWidget(Button.builder(Component.literal(label), b -> {
                rarity = RARITIES[fi];
                String text = nameBox.getValue();
                int cursor = nameBox.getCursorPosition();
                init();
                nameBox.setValue(text);
                nameBox.setCursorPosition(cursor);
                setFocused(nameBox);
            }).bounds(bx, 84, 55, 18).build());
        }

        addRenderableWidget(Button.builder(Component.literal("Save"), b -> save())
            .bounds(cx - 80, 220, 50, 20).build());
        Button delBtn = Button.builder(Component.literal("Delete"), b -> delete())
            .bounds(cx - 25, 220, 50, 20).build();
        delBtn.active = editSkinId != null;
        addRenderableWidget(delBtn);
        addRenderableWidget(Button.builder(Component.literal("Back"), b -> close())
            .bounds(cx + 30, 220, 50, 20).build());
    }

    @Override
    public void render(GuiGraphics gui, int mx, int my, float pt) {
        renderBackground(gui);
        int cx = this.width / 2;

        gui.drawCenteredString(this.font, titleText, cx, 8, 0xFFC8CBCE);

        gui.drawString(this.font, "Name:", cx - 130, 33, 0xFF7A7D84, false);
        gui.drawString(this.font, "Type:", cx - 130, 60, 0xFF7A7D84, false);
        gui.drawString(this.font, "Rarity:", cx - 130, 88, 0xFF7A7D84, false);

        gui.drawString(this.font, "Preview:", cx - 130, 112, 0xFF7A7D84, false);
        gui.fill(cx - 60, 122, cx + 20, 182, 0xCC12151A);
        if (!previewItem.isEmpty()) {
            gui.renderItem(previewItem, cx - 38, 134);
            gui.renderItemDecorations(this.font, previewItem, cx - 38, 134);
            String display = previewItemName.isEmpty() ? "unknown" : previewItemName;
            if (display.length() > 25) display = display.substring(0, 22) + "...";
            gui.drawString(this.font, display, cx - 55, 186, 0xFF3D6FA5, false);
            gui.drawString(this.font, slotType + " | " + rarity, cx - 55, 196, 0xFFC8812A, false);
        }

        gui.drawString(this.font, "Click item below to select:", cx - 130, 210, 0xFF4A4D54, false);
        int invStartX = (this.width - 9 * 20) / 2;
        if (mc.player != null) {
            for (int i = 0; i < 36; i++) {
                int col = i % 9, row = i / 9;
                int ix = invStartX + col * 20;
                int iy = 224 + row * 20;
                ItemStack stack = mc.player.getInventory().items.get(i);
                gui.fill(ix, iy, ix + 18, iy + 18, 0x2212151A);
                if (!stack.isEmpty()) {
                    gui.renderItem(stack, ix + 1, iy + 1);
                    if (mx >= ix && mx <= ix + 18 && my >= iy && my <= iy + 18) {
                        gui.renderTooltip(this.font, stack, mx, my);
                    }
                }
            }
        }

        if (System.currentTimeMillis() - statusTime < 3000 && !statusMsg.isEmpty())
            gui.drawCenteredString(this.font, statusMsg, cx, 256, 0xFFC8812A);

        super.render(gui, mx, my, pt);
    }

    private final Minecraft mc = Minecraft.getInstance();

    @Override
    public boolean mouseClicked(double mx, double my, int btn) {
        int invStartX = (this.width - 9 * 20) / 2;
        if (mc.player != null) {
            for (int i = 0; i < 36; i++) {
                int col = i % 9, row = i / 9;
                int ix = invStartX + col * 20;
                int iy = 210 + row * 20;
                if (mx >= ix && mx <= ix + 18 && my >= iy && my <= iy + 18) {
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
        if (name.isEmpty()) { showMsg("Enter a name!"); return; }
        if (previewItem.isEmpty()) { showMsg("Select an item from inventory!"); return; }

        final String skinId;
        if (editSkinId != null) {
            skinId = editSkinId;
        } else {
            String base = name.toLowerCase().replaceAll("[^a-z0-9_]", "_").replaceAll("_+", "_").replaceAll("^_|_$", "");
            String itemHash = String.valueOf(Math.abs(previewRegistryName.hashCode()));
            skinId = itemHash + "_" + base;
        }

        showMsg("Saving...");
        new Thread(() -> {
            try {
                JsonObject result = CoreAPI.saveSkin(skinId, name, "", slotType, weaponTag, rarity, previewRegistryName);
                if (result != null && result.has("success") && result.get("success").getAsBoolean()) {
                    mc.tell(() -> { showMsg("Saved!"); close(); });
                } else {
                    String err = result != null && result.has("error") ? result.get("error").getAsString() : "API returned null";
                    mc.tell(() -> showMsg("Failed: " + err));
                }
            } catch (Exception e) {
                mc.tell(() -> showMsg("Error: " + e.getMessage()));
            }
        }).start();
    }

    private void delete() {
        if (editSkinId == null) { showMsg("Save first, then delete"); return; }
        showMsg("Deleting...");
        new Thread(() -> {
            try {
                CoreAPI.deleteSkin(editSkinId);
                Thread.sleep(200);
                mc.tell(() -> { showMsg("Deleted!"); close(); });
            } catch (Exception e) {
                mc.tell(() -> showMsg("Error: " + e.getMessage()));
            }
        }, "PWP-Skin-Delete").start();
    }

    private void showMsg(String msg) { statusMsg = msg; statusTime = System.currentTimeMillis(); }
    private void close() { mc.setScreen(new SkinListScreen()); }
    @Override public boolean isPauseScreen() { return false; }
}
