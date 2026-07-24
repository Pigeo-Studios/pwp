package com.pigeostudios.pwp.warfare.client.gui;

import com.google.gson.JsonObject;
import com.pwp.coreclient.CoreAPI;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import com.pwp.coreclient.gui.components.PWPButton;
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
    private int previewSlotIndex = -1;

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

        for (int i = 0; i < SLOT_TYPES.length; i++) {
            int bx = cx - 160 + i * 64;
            final int fi = i;
            addRenderableWidget(new PWPButton(bx, 56, 60, 18, Component.literal(SLOT_TYPES[i]), b -> {
                slotType = SLOT_TYPES[fi];
                rebuildButtons();
            }, PWPButton.Style.PRIMARY));
        }

        for (int i = 0; i < RARITIES.length; i++) {
            int bx = cx - 170 + i * 57;
            final int fi = i;
            addRenderableWidget(new PWPButton(bx, 78, 53, 16, Component.literal(RARITIES[i].substring(0, 3)), b -> {
                rarity = RARITIES[fi];
                rebuildButtons();
            }, PWPButton.Style.PRIMARY));
        }

        addRenderableWidget(new PWPButton(cx - 60, 230, 50, 20, Component.literal("Save"), b -> save(), PWPButton.Style.ACCENT));
        PWPButton delBtn = new PWPButton(cx - 5, 230, 50, 20, Component.literal("Delete"), b -> delete(), PWPButton.Style.DANGER);
        delBtn.active = editSkinId != null;
        addRenderableWidget(delBtn);
        addRenderableWidget(new PWPButton(cx + 50, 230, 50, 20, Component.literal("Back"), b -> close(), PWPButton.Style.PRIMARY));

        rebuildButtons();
    }

    private void rebuildButtons() {
        int idx = 0;
        for (Object w : renderables) {
            if (w instanceof PWPButton btn) {
                if (idx < SLOT_TYPES.length) {
                    boolean sel = slotType.equals(SLOT_TYPES[idx]);
                    btn.setMessage(Component.literal(sel ? "\u25b6 " + SLOT_TYPES[idx] : "   " + SLOT_TYPES[idx]));
                } else if (idx - SLOT_TYPES.length < RARITIES.length) {
                    int ri = idx - SLOT_TYPES.length;
                    boolean sel = rarity.equals(RARITIES[ri]);
                    btn.setMessage(Component.literal(sel ? "\u25b6" + RARITIES[ri].substring(0, 3) : " " + RARITIES[ri].substring(0, 3)));
                }
                idx++;
            }
        }
    }

    @Override
    public void render(GuiGraphics gui, int mx, int my, float pt) {
        renderBackground(gui);
        int cx = this.width / 2;

        gui.drawCenteredString(this.font, titleText, cx, 8, 0xFFC8CBCE);
        gui.drawString(this.font, "Name:", cx - 120, 33, 0xFF7A7D84, false);
        gui.drawString(this.font, "Type:", cx - 120, 59, 0xFF7A7D84, false);
        gui.drawString(this.font, "Rarity:", cx - 120, 81, 0xFF7A7D84, false);

        int pX = cx - 120, pY = 102;
        gui.drawString(this.font, "Preview:", pX, pY, 0xFF7A7D84, false);
        gui.fill(pX, pY + 10, pX + 50, pY + 60, 0xCC12151A);
        if (!previewItem.isEmpty()) {
            gui.renderItem(previewItem, pX + 9, pY + 14);
            gui.renderItemDecorations(this.font, previewItem, pX + 9, pY + 14);
            gui.drawString(this.font, previewItemName.length() > 28 ? previewItemName.substring(0, 25) + "..." : previewItemName,
                pX + 56, pY + 14, 0xFF3D6FA5, false);
            gui.drawString(this.font, "Slot: " + slotType, pX + 56, pY + 26, 0xFFC8812A, false);
            int rColor = RARITY_COLORS[java.util.Arrays.asList(RARITIES).indexOf(rarity)];
            gui.drawString(this.font, rarity, pX + 56, pY + 38, rColor, false);
        } else {
            gui.drawCenteredString(this.font, "?", pX + 25, pY + 30, 0xFF4A4D54);
        }

        int invY = 166;
        gui.drawString(this.font, "Click an item to preview:", cx - 120, invY - 12, 0xFF4A4D54, false);
        int gridW = INV_COLS * (SLOT_SIZE + 2);
        int invSX = (this.width - gridW) / 2;

        if (mc.player != null) {
            for (int i = 0; i < 36; i++) {
                int col = i % INV_COLS, row = i / INV_COLS;
                int ix = invSX + col * (SLOT_SIZE + 2);
                int iy = invY + row * (SLOT_SIZE + 2);
                ItemStack stack = mc.player.getInventory().items.get(i);
                boolean isSelected = i == previewSlotIndex;
                gui.fill(ix, iy, ix + SLOT_SIZE, iy + SLOT_SIZE, isSelected ? 0x44C8812A : 0x2212151A);
                if (isSelected) gui.renderOutline(ix, iy, SLOT_SIZE, SLOT_SIZE, 0xFFC8812A);
                if (!stack.isEmpty()) {
                    gui.renderItem(stack, ix + 1, iy + 1);
                    if (mx >= ix && mx <= ix + SLOT_SIZE && my >= iy && my <= iy + SLOT_SIZE) {
                        gui.renderTooltip(this.font, stack, mx, my);
                    }
                }
            }
        }

        if (System.currentTimeMillis() - statusTime < 3000 && !statusMsg.isEmpty())
            gui.drawCenteredString(this.font, statusMsg, cx, this.height - 30, 0xFFC8812A);

        super.render(gui, mx, my, pt);
    }

    private final Minecraft mc = Minecraft.getInstance();

    @Override
    public boolean mouseClicked(double mx, double my, int btn) {
        int gridW = INV_COLS * (SLOT_SIZE + 2);
        int invSX = (this.width - gridW) / 2;
        if (mc.player != null) {
            for (int i = 0; i < 36; i++) {
                int col = i % INV_COLS, row = i / INV_COLS;
                int ix = invSX + col * (SLOT_SIZE + 2);
                int iy = 166 + row * (SLOT_SIZE + 2);
                if (mx >= ix && mx <= ix + SLOT_SIZE && my >= iy && my <= iy + SLOT_SIZE) {
                    ItemStack stack = mc.player.getInventory().items.get(i);
                    if (!stack.isEmpty()) { setPreviewItem(stack); previewSlotIndex = i; }
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
        rebuildButtons();
    }

    private void save() {
        String name = nameBox.getValue().trim();
        if (name.isEmpty()) { showMsg("§eEnter a name!"); return; }
        if (previewItem.isEmpty()) { showMsg("§eClick an item in the grid!"); return; }

        final String saveSkinId = editSkinId != null ? editSkinId :
            String.valueOf(Math.abs(previewRegistryName.hashCode())) + "_" +
            name.toLowerCase().replaceAll("[^a-z0-9_]", "_").replaceAll("_+", "_").replaceAll("^_|_$", "");
        final String saveName = name;
        final String saveSlot = slotType;
        final String saveWeapon = weaponTag;
        final String saveRarity = rarity;
        final String saveRegistry = previewRegistryName;

        showMsg("§eSaving...");
        new Thread(() -> {
            try {
                JsonObject result = CoreAPI.saveSkin(saveSkinId, saveName, "", saveSlot, saveWeapon, saveRarity, saveRegistry);
                if (result != null && result.has("success") && result.get("success").getAsBoolean())
                    mc.submit(() -> { showMsg("§aSaved!"); close(); });
                else
                    mc.submit(() -> showMsg("§c" + (result != null && result.has("error") ? result.get("error").getAsString() : "API error")));
            } catch (Exception e) {
                mc.submit(() -> showMsg("§c" + e.getMessage()));
            }
        }).start();
    }

    private void delete() {
        if (editSkinId == null) { showMsg("§eSave first!"); return; }
        showMsg("§eDeleting...");
        new Thread(() -> {
            try {
                CoreAPI.deleteSkin(editSkinId);
                Thread.sleep(200);
                mc.submit(() -> { showMsg("§aDeleted!"); close(); });
            } catch (Exception e) {
                mc.submit(() -> showMsg("§c" + e.getMessage()));
            }
        }, "PWP-Skin-Delete").start();
    }

    private void showMsg(String msg) { statusMsg = msg; statusTime = System.currentTimeMillis(); }
    private void close() { mc.setScreen(new SkinListScreen()); }
    @Override public boolean isPauseScreen() { return false; }
}
