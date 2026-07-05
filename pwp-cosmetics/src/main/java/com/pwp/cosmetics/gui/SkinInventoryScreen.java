package com.pwp.cosmetics.gui;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.blaze3d.systems.RenderSystem;
import com.pwp.coreclient.CoreAPI;
import com.pwp.coreclient.PlayerData;
import com.pwp.coreclient.PlayerData.CosmeticEntry;
import com.pwp.coreclient.gui.theme.PWPTheme;
import com.pwp.cosmetics.CosmeticsMod;
import com.pwp.cosmetics.network.PacketSyncCosmeticEquip;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.*;
import java.util.stream.Collectors;

public class SkinInventoryScreen extends Screen {

    private static final int ITEMS_PER_ROW = 6;
    private static final int ITEM_SIZE = 24;
    private static final int ITEM_GAP = 6;
    private static final int START_Y = 50;

    private static Map<String, ItemStack> globalSkinItemCache = null;
    private static List<CosmeticEntry> globalSkinEntries = null;
    private static Map<String, String> globalSkinNameCache = null;

    private List<CosmeticEntry> allCosmetics = new ArrayList<>();
    private List<CosmeticEntry> filtered = new ArrayList<>();
    private Map<String, ItemStack> skinItemCache = new HashMap<>();
    private Map<String, String> skinNameCache = new HashMap<>();
    private Set<String> ownedSkinIds = new HashSet<>();
    private String currentFilter = "ALL";
    private int scrollOffset = 0;
    private int maxScroll = 0;
    private String statusMsg = "";
    private long statusTime = 0;

    private static final int[] RARITY_COLORS = {
            0xFF6A6D73, 0xFF3D6FA5, 0xFF7A4A8A, 0xFFC8812A, 0xFFA53D3D, 0xFFFFD700
    };

    public SkinInventoryScreen() {
        super(Component.translatable("gui.pwp_cosmetics.skin_inventory.title"));
    }

    @Override
    protected void init() {
        int cx = this.width / 2;
        int btnY = 15;

        addRenderableWidget(Button.builder(Component.literal("All"), b -> setFilter("ALL"))
                .bounds(cx - 160, btnY, 45, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Primary"), b -> setFilter("PRIMARY"))
                .bounds(cx - 110, btnY, 55, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Secondary"), b -> setFilter("SECONDARY"))
                .bounds(cx - 50, btnY, 60, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Knife"), b -> setFilter("KNIFE"))
                .bounds(cx + 15, btnY, 45, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Melee"), b -> setFilter("MELEE"))
                .bounds(cx + 65, btnY, 45, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Uniform"), b -> setFilter("UNIFORM"))
                .bounds(cx + 115, btnY, 50, 20).build());

        loadCosmetics();
    }

    private void setFilter(String filter) {
        this.currentFilter = filter;
        this.scrollOffset = 0;
        filterCosmetics();
    }

    private void loadCosmetics() {
        globalSkinItemCache = null;
        globalSkinEntries = null;
        globalSkinNameCache = null;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        UUID uuid = mc.player.getUUID();
        PlayerData.CachedProfile profile = PlayerData.get(uuid);
        Set<String> equippedSkinIds = new HashSet<>();
        ownedSkinIds.clear();
        if (profile != null) {
            for (CosmeticEntry ce : profile.getCosmetics()) {
                ownedSkinIds.add(ce.skinId);
                if (ce.equipped) equippedSkinIds.add(ce.skinId);
            }
        }
        loadSkinDefinitions(equippedSkinIds);
    }

    private boolean isAdmin() {
        Minecraft mc = Minecraft.getInstance();
        return mc.player != null && (mc.player.isCreative() || mc.player.hasPermissions(2));
    }

    private void loadSkinDefinitions(Set<String> equippedSkinIds) {
        if (globalSkinItemCache != null && globalSkinEntries != null) {
            skinItemCache = globalSkinItemCache;
            skinNameCache = globalSkinNameCache;
            allCosmetics = new ArrayList<>(globalSkinEntries);
            for (CosmeticEntry ce : allCosmetics) {
                ce.equipped = equippedSkinIds.contains(ce.skinId);
            }
            filterCosmetics();
            return;
        }
        new Thread(() -> {
            try {
                JsonObject result = CoreAPI.getSkins();
                if (result != null && result.has("data")) {
                    JsonArray arr = result.get("data").getAsJsonArray();
                    Map<String, ItemStack> cache = new HashMap<>();
                    Map<String, String> nameCache = new HashMap<>();
                    List<CosmeticEntry> allSkinEntries = new ArrayList<>();
                    for (JsonElement e : arr) {
                        JsonObject obj = e.getAsJsonObject();
                        String skinId = obj.get("skinId").getAsString();
                        String skinName = obj.has("name") ? obj.get("name").getAsString() : skinId;
                        nameCache.put(skinId, skinName);
                        String slotType = obj.get("slotType").getAsString();
                        String rarity = obj.get("rarity").getAsString();
                        String modelPath = obj.has("modelPath") && !obj.get("modelPath").isJsonNull()
                                ? obj.get("modelPath").getAsString() : "";
                        if (!modelPath.isEmpty() && modelPath.startsWith("{")) {
                            try {
                                CompoundTag loaded = TagParser.parseTag(modelPath);
                                ItemStack is = ItemStack.of(loaded);
                                if (!is.isEmpty()) cache.put(skinId, is);
                            } catch (Exception ignored) {}
                        } else if (!modelPath.isEmpty() && modelPath.contains(":")) {
                            Item item = ForgeRegistries.ITEMS.getValue(ResourceLocation.tryParse(modelPath));
                            if (item != null) cache.put(skinId, new ItemStack(item));
                        }
                        CosmeticEntry ce = new CosmeticEntry();
                        ce.itemUuid = skinId;
                        ce.skinId = skinId;
                        ce.slotType = slotType;
                        ce.rarity = rarity;
                        ce.source = "builtin";
                        ce.equipped = equippedSkinIds.contains(skinId);
                        allSkinEntries.add(ce);
                    }
                    globalSkinItemCache = cache;
                    globalSkinEntries = allSkinEntries;
                    globalSkinNameCache = nameCache;
                    Minecraft.getInstance().submit(() -> {
                        skinItemCache = cache;
                        skinNameCache = nameCache;
                        allCosmetics = new ArrayList<>(allSkinEntries);
                        filterCosmetics();
                    });
                }
            } catch (Exception ignored) {}
        }, "PWP-SkinDef-Loader").start();
    }

    private void filterCosmetics() {
        if (currentFilter.equals("ALL")) {
            filtered = new ArrayList<>(allCosmetics);
        } else {
            filtered = allCosmetics.stream()
                    .filter(c -> c.slotType.equals(currentFilter))
                    .collect(Collectors.toList());
        }

        filtered.sort((a, b) -> {
            boolean aOwned = ownedSkinIds.contains(a.skinId);
            boolean bOwned = ownedSkinIds.contains(b.skinId);
            if (a.equipped && !b.equipped) return -1;
            if (!a.equipped && b.equipped) return 1;
            if (aOwned && !bOwned) return -1;
            if (!aOwned && bOwned) return 1;
            return 0;
        });

        int rows = (filtered.size() + ITEMS_PER_ROW - 1) / ITEMS_PER_ROW;
        int visibleRows = (this.height - START_Y - 30) / (ITEM_SIZE + ITEM_GAP);
        maxScroll = Math.max(0, rows - visibleRows);
    }

    @Override
    public void render(GuiGraphics gui, int mx, int my, float pt) {
        renderBackground(gui);

        int cx = this.width / 2;
        int totalWidth = ITEMS_PER_ROW * (ITEM_SIZE + ITEM_GAP) - ITEM_GAP;
        int startX = cx - totalWidth / 2;

        gui.drawCenteredString(this.font, this.title, cx, 36, PWPTheme.Colors.TEXT_PRIMARY);
        gui.fill(cx - totalWidth / 2 - 8, 44, cx + totalWidth / 2 + 8, 45, PWPTheme.Colors.ACCENT);

        if (filtered.isEmpty()) {
            gui.drawCenteredString(this.font, Component.translatable("gui.pwp_cosmetics.skin_inventory.empty"),
                    cx, this.height / 2, PWPTheme.Colors.TEXT_DIM);
        } else {
            RenderSystem.enableBlend();
            for (int i = scrollOffset * ITEMS_PER_ROW; i < filtered.size(); i++) {
                int row = (i - scrollOffset * ITEMS_PER_ROW) / ITEMS_PER_ROW;
                int col = (i - scrollOffset * ITEMS_PER_ROW) % ITEMS_PER_ROW;
                int x = startX + col * (ITEM_SIZE + ITEM_GAP);
                int y = START_Y + row * (ITEM_SIZE + ITEM_GAP);

                if (y + ITEM_SIZE > this.height) break;

                CosmeticEntry entry = filtered.get(i);
                boolean canAccess = isAdmin() || ownedSkinIds.contains(entry.skinId);
                int rarityColor = getRarityColor(entry.rarity);

                gui.fill(x, y, x + ITEM_SIZE, y + ITEM_SIZE, PWPTheme.Colors.SURFACE);

                if (entry.equipped) {
                    gui.renderOutline(x - 2, y - 2, ITEM_SIZE + 4, ITEM_SIZE + 4, PWPTheme.Colors.SUCCESS);
                    gui.renderOutline(x - 1, y - 1, ITEM_SIZE + 2, ITEM_SIZE + 2, rarityColor);
                    gui.drawString(this.font, Component.literal("\u2714"), x + ITEM_SIZE - 9, y + 1, PWPTheme.Colors.SUCCESS, false);
                } else if (canAccess) {
                    gui.renderOutline(x, y, ITEM_SIZE, ITEM_SIZE, rarityColor);
                } else {
                    gui.renderOutline(x, y, ITEM_SIZE, ITEM_SIZE, 0xFF3A3D44);
                    gui.fill(x, y, x + ITEM_SIZE, y + ITEM_SIZE, 0x8812151A);
                }

                ItemStack stack = skinItemCache.get(entry.skinId);
                if (stack != null && !stack.isEmpty()) {
                    if (!canAccess) RenderSystem.setShaderColor(0.5F, 0.5F, 0.5F, 0.6F);
                    gui.renderItem(stack, x + 4, y + 4);
                    if (!canAccess) RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
                }

                if (!canAccess) {
                    gui.drawString(this.font, Component.literal("\uD83D\uDD12"), x + 2, y + ITEM_SIZE - 10, 0xFF7A7D84, false);
                }

                if (mx >= x && mx <= x + ITEM_SIZE && my >= y && my <= y + ITEM_SIZE) {
                    gui.renderOutline(x - 1, y - 1, ITEM_SIZE + 2, ITEM_SIZE + 2, PWPTheme.Colors.ACCENT);

                    List<Component> tooltip = new ArrayList<>();
                    String displayName = skinNameCache.getOrDefault(entry.skinId, entry.skinId);
                    tooltip.add(Component.literal("\u00a7" + getRarityCode(entry.rarity) + displayName));
                    tooltip.add(Component.literal("\u00a77" + entry.slotType + " \u00a78| \u00a77" + entry.rarity));
                    if (entry.equipped) {
                        tooltip.add(Component.literal("\u00a7a\u2714 Equipped - Click to unequip"));
                    } else if (canAccess) {
                        tooltip.add(Component.literal("\u00a7eClick to equip"));
                    } else {
                        tooltip.add(Component.literal("\u00a78\uD83D\uDD12 Locked - Not owned"));
                    }
                    gui.renderComponentTooltip(this.font, tooltip, mx, my);
                }
            }
            RenderSystem.disableBlend();
        }

        if (currentFilter.equals("PRIMARY")) {
            String hint = "\u00a77\u2714 Multiple PRIMARY skins can be selected";
            gui.drawCenteredString(this.font, Component.literal(hint), cx, this.height - 28, PWPTheme.Colors.TEXT_SECONDARY);
        }

        if (System.currentTimeMillis() - statusTime < 3000 && !statusMsg.isEmpty())
            gui.drawCenteredString(this.font, Component.literal(statusMsg), cx, this.height - 16, PWPTheme.Colors.ACCENT);

        super.render(gui, mx, my, pt);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button == 0) {
            int cx = this.width / 2;
            int totalWidth = ITEMS_PER_ROW * (ITEM_SIZE + ITEM_GAP) - ITEM_GAP;
            int startX = cx - totalWidth / 2;

            for (int i = scrollOffset * ITEMS_PER_ROW; i < filtered.size(); i++) {
                int row = (i - scrollOffset * ITEMS_PER_ROW) / ITEMS_PER_ROW;
                int col = (i - scrollOffset * ITEMS_PER_ROW) % ITEMS_PER_ROW;
                int x = startX + col * (ITEM_SIZE + ITEM_GAP);
                int y = START_Y + row * (ITEM_SIZE + ITEM_GAP);

                if (y + ITEM_SIZE > this.height) break;

                if (mx >= x && mx <= x + ITEM_SIZE && my >= y && my <= y + ITEM_SIZE) {
                    CosmeticEntry entry = filtered.get(i);
                    if (isAdmin() || ownedSkinIds.contains(entry.skinId)) {
                        toggleEquip(entry);
                        return true;
                    }
                }
            }
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        if (delta > 0) scrollOffset = Math.max(0, scrollOffset - 1);
        else if (delta < 0) scrollOffset = Math.min(maxScroll, scrollOffset + 1);
        return true;
    }

    private void toggleEquip(CosmeticEntry entry) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        String uuid = mc.player.getStringUUID();
        String role = "ALL";
        boolean isPrimary = entry.slotType.equals("PRIMARY");

        if (entry.equipped) {
            statusMsg = "\u00a76Unequipping...";
            statusTime = System.currentTimeMillis();
            new Thread(() -> {
                try {
                    CoreAPI.unequipItem(uuid, entry.slotType, role);
                    Thread.sleep(300);
                    JsonObject fresh = CoreAPI.loadPlayer(uuid);
                    if (fresh != null && fresh.has("data")) {
                        PlayerData.put(mc.player.getUUID(), fresh.getAsJsonObject("data"));
                    }
                    CosmeticsMod.NETWORK.sendToServer(new PacketSyncCosmeticEquip(entry.slotType, role, "", ""));
                    Minecraft.getInstance().submit(() -> {
                        statusMsg = "\u00a7a\u2714 Unequipped (default skin)";
                        statusTime = System.currentTimeMillis();
                        loadCosmetics();
                    });
                } catch (Exception e) {
                    Minecraft.getInstance().submit(() -> {
                        statusMsg = "\u00a7cUnequip failed: " + e.getMessage();
                        statusTime = System.currentTimeMillis();
                    });
                }
            }, "PWP-Unequip-Thread").start();
        } else {
            statusMsg = "\u00a76Equipping...";
            statusTime = System.currentTimeMillis();
            new Thread(() -> {
                try {
                    CoreAPI.grantItem(uuid, entry.skinId, "menu");
                    Thread.sleep(300);
                    JsonObject fresh = CoreAPI.loadPlayer(uuid);
                    if (fresh == null || !fresh.has("data")) {
                        Minecraft.getInstance().submit(() -> {
                            statusMsg = "\u00a7cAPI error (grant/load failed)";
                            statusTime = System.currentTimeMillis();
                        });
                        return;
                    }
                    JsonObject data = fresh.getAsJsonObject("data");
                    PlayerData.put(mc.player.getUUID(), data);
                    boolean equipped = false;
                    if (data.has("cosmetics")) {
                        for (JsonElement e : data.get("cosmetics").getAsJsonArray()) {
                            JsonObject obj = e.getAsJsonObject();
                            if (obj.get("skinId").getAsString().equals(entry.skinId)) {
                                String realItemUuid = obj.get("itemUuid").getAsString();
                                String slotType = obj.get("slotType").getAsString();
                                CoreAPI.equipItem(uuid, realItemUuid, slotType, role);
                                equipped = true;
                                break;
                            }
                        }
                    }
                    if (!equipped) {
                        Minecraft.getInstance().submit(() -> {
                            statusMsg = "\u00a7cSkin not found in profile!";
                            statusTime = System.currentTimeMillis();
                        });
                        return;
                    }
                    Thread.sleep(300);
                    JsonObject fresh2 = CoreAPI.loadPlayer(uuid);
                    if (fresh2 != null && fresh2.has("data")) {
                        PlayerData.put(mc.player.getUUID(), fresh2.getAsJsonObject("data"));
                    }
                    ItemStack eqItem = skinItemCache.get(entry.skinId);
                    String itemSnbt = "";
                    if (eqItem != null && !eqItem.isEmpty()) {
                        CompoundTag tag = new CompoundTag();
                        eqItem.save(tag);
                        itemSnbt = tag.toString();
                    }
                    CosmeticsMod.NETWORK.sendToServer(new PacketSyncCosmeticEquip(entry.slotType, role, entry.skinId, itemSnbt));
                    Minecraft.getInstance().submit(() -> {
                        statusMsg = "\u00a7a\u2714 Equipped!";
                        statusTime = System.currentTimeMillis();
                        loadCosmetics();
                    });
                } catch (Exception e) {
                    Minecraft.getInstance().submit(() -> {
                        statusMsg = "\u00a7cEquip failed: " + e.getMessage();
                        statusTime = System.currentTimeMillis();
                    });
                }
            }, "PWP-Equip-Thread").start();
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private int getRarityColor(String rarity) {
        if (rarity == null) return RARITY_COLORS[0];
        switch (rarity.toUpperCase()) {
            case "COMMON": return RARITY_COLORS[0];
            case "UNCOMMON": return RARITY_COLORS[1];
            case "RARE": return RARITY_COLORS[2];
            case "EPIC": return RARITY_COLORS[3];
            case "LEGENDARY": return RARITY_COLORS[4];
            case "MYTHIC": return RARITY_COLORS[5];
            default: return RARITY_COLORS[0];
        }
    }

    private String getRarityCode(String rarity) {
        if (rarity == null) return "7";
        switch (rarity.toUpperCase()) {
            case "COMMON": return "7";
            case "UNCOMMON": return "b";
            case "RARE": return "9";
            case "EPIC": return "5";
            case "LEGENDARY": return "6";
            case "MYTHIC": return "c";
            default: return "7";
        }
    }
}
