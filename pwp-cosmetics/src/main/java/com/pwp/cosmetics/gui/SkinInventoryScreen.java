package com.pwp.cosmetics.gui;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.blaze3d.systems.RenderSystem;
import com.pwp.coreclient.CoreAPI;
import com.pwp.coreclient.PlayerData;
import com.pwp.coreclient.PlayerData.CosmeticEntry;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
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
    private String currentFilter = "ALL";
    private int scrollOffset = 0;
    private int maxScroll = 0;
    private String statusMsg = "";
    private long statusTime = 0;

    private static final int[] RARITY_COLORS = {
            0xFF6A6D73, // COMMON
            0xFF3D6FA5, // UNCOMMON
            0xFF7A4A8A, // RARE
            0xFFC8812A, // EPIC
            0xFFA53D3D, // LEGENDARY
            0xFFFFD700  // MYTHIC
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
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        UUID uuid = mc.player.getUUID();
        PlayerData.CachedProfile profile = PlayerData.get(uuid);
        if (profile != null) {
            allCosmetics = profile.getCosmetics();
        }
        loadSkinDefinitions();
        filterCosmetics();
    }

    private void loadSkinDefinitions() {
        if (globalSkinItemCache != null && globalSkinEntries != null) {
            skinItemCache = globalSkinItemCache;
            skinNameCache = globalSkinNameCache;
            if (allCosmetics.isEmpty()) {
                allCosmetics = new ArrayList<>(globalSkinEntries);
                filterCosmetics();
            }
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
                                if (!is.isEmpty()) {
                                    cache.put(skinId, is);
                                }
                            } catch (Exception ignored) {}
                        } else if (!modelPath.isEmpty() && modelPath.contains(":")) {
                            Item item = ForgeRegistries.ITEMS.getValue(ResourceLocation.tryParse(modelPath));
                            if (item != null) {
                                cache.put(skinId, new ItemStack(item));
                            }
                        }
                        CosmeticEntry ce = new CosmeticEntry();
                        ce.itemUuid = skinId;
                        ce.skinId = skinId;
                        ce.slotType = slotType;
                        ce.rarity = rarity;
                        ce.source = "builtin";
                        ce.equipped = false;
                        allSkinEntries.add(ce);
                    }
                    globalSkinItemCache = cache;
                    globalSkinEntries = allSkinEntries;
                    globalSkinNameCache = nameCache;
                    Minecraft.getInstance().submit(() -> {
                        skinItemCache = cache;
                        skinNameCache = nameCache;
                        if (allCosmetics.isEmpty()) {
                            allCosmetics = allSkinEntries;
                            filterCosmetics();
                        }
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

        gui.drawCenteredString(this.font, this.title, cx, 36, 0xFFC8CBCE);

        if (filtered.isEmpty()) {
            gui.drawCenteredString(this.font, Component.translatable("gui.pwp_cosmetics.skin_inventory.empty"),
                    cx, this.height / 2, 0xFF7A7D84);
        } else {
            RenderSystem.enableBlend();
            for (int i = scrollOffset * ITEMS_PER_ROW; i < filtered.size(); i++) {
                int row = (i - scrollOffset * ITEMS_PER_ROW) / ITEMS_PER_ROW;
                int col = (i - scrollOffset * ITEMS_PER_ROW) % ITEMS_PER_ROW;
                int x = startX + col * (ITEM_SIZE + ITEM_GAP);
                int y = START_Y + row * (ITEM_SIZE + ITEM_GAP);

                if (y + ITEM_SIZE > this.height) break;

                CosmeticEntry entry = filtered.get(i);
                int rarityColor = getRarityColor(entry.rarity);

                gui.fill(x, y, x + ITEM_SIZE, y + ITEM_SIZE, 0xCC12151A);
                if (entry.equipped) {
                    gui.renderOutline(x - 1, y - 1, ITEM_SIZE + 2, ITEM_SIZE + 2, 0xFFFFFFFF);
                    gui.renderOutline(x, y, ITEM_SIZE, ITEM_SIZE, rarityColor);
                } else {
                    gui.renderOutline(x, y, ITEM_SIZE, ITEM_SIZE, rarityColor);
                }

                ItemStack stack = skinItemCache.get(entry.skinId);
                if (stack != null && !stack.isEmpty()) {
                    gui.renderItem(stack, x + 4, y + 2);
                }

                String label = entry.skinId.length() > 12 ? entry.skinId.substring(0, 11) + ".." : entry.skinId;
                gui.drawString(this.font, label, x + 2, y + ITEM_SIZE / 2 - 4, rarityColor, false);

                if (mx >= x && mx <= x + ITEM_SIZE && my >= y && my <= y + ITEM_SIZE) {
                    gui.renderOutline(x - 1, y - 1, ITEM_SIZE + 2, ITEM_SIZE + 2, 0xFFC8812A);

                    List<Component> tooltip = new ArrayList<>();
                    String displayName = skinNameCache.getOrDefault(entry.skinId, entry.skinId);
                    tooltip.add(Component.literal("§" + getRarityCode(entry.rarity) + displayName));
                    tooltip.add(Component.literal("§7" + entry.slotType + " §8| §7" + entry.rarity));
                    if (entry.equipped) {
                        tooltip.add(Component.literal("§a✔ Equipped"));
                    } else {
                        tooltip.add(Component.literal("§eClick to equip"));
                    }
                    gui.renderComponentTooltip(this.font, tooltip, mx, my);
                }
            }
            RenderSystem.disableBlend();
        }

        if (System.currentTimeMillis() - statusTime < 3000 && !statusMsg.isEmpty())
            gui.drawCenteredString(this.font, statusMsg, cx, this.height - 20, 0xFFC8812A);

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
                    toggleEquip(entry);
                    return true;
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
        statusMsg = "§eEquipping...";
        statusTime = System.currentTimeMillis();

        new Thread(() -> {
            try {
                com.pwp.coreclient.CoreAPI.grantItem(uuid, entry.skinId, "menu");
                Thread.sleep(200);
                JsonObject fresh = com.pwp.coreclient.CoreAPI.loadPlayer(uuid);
                if (fresh != null && fresh.has("data")) {
                    JsonObject data = fresh.getAsJsonObject("data");
                    PlayerData.put(mc.player.getUUID(), data);
                    if (data.has("cosmetics")) {
                        for (JsonElement e : data.get("cosmetics").getAsJsonArray()) {
                            JsonObject obj = e.getAsJsonObject();
                            if (obj.get("skinId").getAsString().equals(entry.skinId)) {
                                String realItemUuid = obj.get("itemUuid").getAsString();
                                String slotType = obj.get("slotType").getAsString();
                                com.pwp.coreclient.CoreAPI.equipItem(uuid, realItemUuid, slotType, role);
                                break;
                            }
                        }
                    }
                }
                Thread.sleep(200);
                JsonObject fresh2 = com.pwp.coreclient.CoreAPI.loadPlayer(uuid);
                if (fresh2 != null && fresh2.has("data")) {
                    PlayerData.put(mc.player.getUUID(), fresh2.getAsJsonObject("data"));
                }
                ItemStack equippedItem = skinItemCache.get(entry.skinId);
                String itemSnbt = "";
                if (equippedItem != null && !equippedItem.isEmpty()) {
                    CompoundTag tag = new CompoundTag();
                    equippedItem.save(tag);
                    itemSnbt = tag.toString();
                }
                CosmeticsMod.NETWORK.sendToServer(new PacketSyncCosmeticEquip(entry.slotType, "ALL", entry.skinId, itemSnbt));
                Minecraft.getInstance().submit(() -> {
                    statusMsg = "§a✔ Equipped!";
                    statusTime = System.currentTimeMillis();
                    loadCosmetics();
                });
            } catch (Exception e) {
                Minecraft.getInstance().submit(() -> {
                    statusMsg = "§cEquip failed!";
                    statusTime = System.currentTimeMillis();
                });
            }
        }, "PWP-Equip-Thread").start();
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
