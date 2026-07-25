package com.pigeostudios.pwp.warfare.client.gui;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.pigeostudios.pwp.warfare.menu.KitEditorMenu;
import com.pwp.coreclient.CoreAPI;
import com.pwp.coreclient.gui.components.PWPButton;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.*;
import java.util.stream.Collectors;
import com.pwp.coreclient.gui.theme.PWPTheme;

public class KitSkinSelectScreen extends Screen {

    private static final String[] CATEGORIES = {"PRIMARY", "SECONDARY", "KNIFE", "MELEE", "UNIFORM"};
    private static final String CAT_PREFIX = "__CAT__";

    private final KitEditorMenu menu;
    private final Screen parentScreen;
    private final Map<Integer, List<String>> allowedSkins;
    private final Map<Integer, String> slotCategories;
    private final List<Integer> weaponSlots = new ArrayList<>();
    private List<SkinOption> allSkins = new ArrayList<>();
    private int selectedSlot = -1;
    private int skinScroll = 0;

    private static class SkinOption {
        String skinId, name, weaponTag, rarity;
    }

    public KitSkinSelectScreen(KitEditorMenu menu, Screen parentScreen) {
        super(Component.literal("Allowed Skins per Slot"));
        this.menu = menu;
        this.parentScreen = parentScreen;
        this.allowedSkins = new HashMap<>();
        this.slotCategories = new HashMap<>();
        for (Map.Entry<Integer, List<String>> e : menu.slotSkins.entrySet()) {
            List<String> list = new ArrayList<>(e.getValue());
            if (!list.isEmpty() && list.get(0).startsWith(CAT_PREFIX)) {
                slotCategories.put(e.getKey(), list.get(0).substring(CAT_PREFIX.length()));
            } else {
                this.allowedSkins.put(e.getKey(), list);
            }
        }
        for (int i = 0; i < 9; i++) weaponSlots.add(i);
        for (int i = 41; i < 49; i++) weaponSlots.add(i);
    }

    @Override
    protected void init() {
        addRenderableWidget(new PWPButton(this.width - 100, 8, 80, 20, Component.literal("Done"), b -> saveAndClose(), PWPButton.Style.PRIMARY));
        loadSkins();
    }

    private void loadSkins() {
        new Thread(() -> {
            try {
                JsonObject r = CoreAPI.getSkins();
                if (r != null && r.has("data")) {
                    List<SkinOption> list = new ArrayList<>();
                    for (JsonElement e : r.get("data").getAsJsonArray()) {
                        JsonObject o = e.getAsJsonObject();
                        SkinOption s = new SkinOption();
                        s.skinId = o.get("skinId").getAsString();
                        s.name = o.has("name") ? o.get("name").getAsString() : s.skinId;
                        s.weaponTag = o.get("weaponTag").getAsString();
                        s.rarity = o.get("rarity").getAsString();
                        list.add(s);
                    }
                    Minecraft.getInstance().submit(() -> allSkins = list);
                }
            } catch (Exception ignored) {}
        }).start();
    }

    private void saveAndClose() {
        menu.slotSkins.clear();
        for (Map.Entry<Integer, String> e : slotCategories.entrySet()) {
            menu.slotSkins.put(e.getKey(), Collections.singletonList(CAT_PREFIX + e.getValue()));
        }
        for (Map.Entry<Integer, List<String>> e : allowedSkins.entrySet()) {
            menu.slotSkins.put(e.getKey(), new ArrayList<>(e.getValue()));
        }
        if (this.minecraft.player != null) {
            this.minecraft.player.displayClientMessage(
                Component.literal("В§aSkin settings saved! Click 'Save' in kit editor."), true);
        }
        if (parentScreen != null) {
            this.minecraft.setScreen(parentScreen);
        } else {
            this.minecraft.setScreen(null);
        }
    }

    private String getWeaponTagForSlot(int slot) {
        ItemStack stack = menu.kitInventory.getItem(slot);
        if (stack.isEmpty()) return "any";
        String id = ForgeRegistries.ITEMS.getKey(stack.getItem()).toString();
        if (id.contains("knife") || id.contains("bayonet") || id.contains("dagger") || id.contains("sword")) return "knife";
        if (id.contains("pistol") || id.contains("deagle") || id.contains("glock") || id.contains("revolver")) return "deagle";
        if (id.contains("ak")) return "ak47";
        if (id.contains("m4")) return "m4";
        if (id.contains("sniper") || id.contains("l96") || id.contains("svd")) return "sniper";
        if (id.contains("lmg") || id.contains("pk") || id.contains("rpk")) return "lmg";
        if (id.contains("shotgun")) return "shotgun";
        if (id.contains("smg") || id.contains("mp5") || id.contains("uzi")) return "smg";
        if (id.contains("rifle")) return "rifle";
        return "any";
    }

    @Override
    public void render(GuiGraphics gui, int mx, int my, float pt) {
        renderBackground(gui);
        gui.drawCenteredString(PWPTheme.Fonts.display(), "Allowed Skins per Slot", this.width / 2, 8, 0xFFC8CBCE);

        int leftX = 8, leftY = 34;
        gui.fill(leftX - 2, leftY - 2, leftX + 220, this.height - 10, 0xFF1E222A);
        gui.fill(leftX, leftY, leftX + 220, this.height - 12, 0xCC0A0C0E);
        gui.drawCenteredString(PWPTheme.Fonts.display(), "Weapon Slots", leftX + 110, leftY + 2, 0xFFC8CBCE);

        int sy = leftY + 16;
        for (int i = 0; i < weaponSlots.size(); i++) {
            int slot = weaponSlots.get(i);
            int col = i % 2, row = i / 2;
            int sx = leftX + 4 + col * 106, sry = sy + row * 28;
            if (sry + 26 > this.height - 14) break;

            boolean hover = mx >= sx && mx <= sx + 102 && my >= sry && my <= sry + 24;
            boolean sel = selectedSlot == slot;
            boolean hasCategory = slotCategories.containsKey(slot);
            boolean hasSkins = allowedSkins.containsKey(slot) && !allowedSkins.get(slot).isEmpty();

            int borderColor = sel ? 0xFFC8812A : hasCategory ? 0xFF3D7A40 : hasSkins ? 0xFF3D6FA5 : 0xFF1E222A;
            gui.fill(sx, sry, sx + 102, sry + 24, sel ? 0xFF2A2F3A : hover ? 0x881A1E26 : 0x2212151A);
            gui.renderOutline(sx, sry, 102, 24, borderColor);

            ItemStack stack = menu.kitInventory.getItem(slot);
            if (!stack.isEmpty()) gui.renderItem(stack, sx + 3, sry + 4);

            String label = slot < 9 ? "S" + slot : "C" + (slot - 41);
            gui.drawString(PWPTheme.Fonts.display(), label, sx + 21, sry + 7, 0xFF7A7D84, false);
            if (hasCategory) {
                gui.drawString(PWPTheme.Fonts.display(), "CAT:" + slotCategories.get(slot), sx + 38, sry + 7, 0xFF3D7A40, false);
            } else {
                int count = allowedSkins.getOrDefault(slot, Collections.emptyList()).size();
                if (count > 0) gui.drawString(PWPTheme.Fonts.display(), count + " skins", sx + 38, sry + 7, 0xFF3D6FA5, false);
            }
            if (sel) gui.drawString(PWPTheme.Fonts.display(), "\u25B6", sx + 90, sry + 7, 0xFFC8812A, false);
        }

        int rightX = this.width - 240;
        int rightY = 34;
        int rightW = 230;
        int rightH = this.height - 50;

        if (selectedSlot >= 0) {
            gui.fill(rightX - 2, rightY - 2, rightX + rightW + 2, rightY + rightH + 2, 0xFF1E222A);
            gui.fill(rightX, rightY, rightX + rightW, rightY + rightH, 0xCC0A0C0E);

            boolean isCategory = slotCategories.containsKey(selectedSlot);
            String curCat = isCategory ? slotCategories.get(selectedSlot) : null;

            int btnY = rightY + 4;
            int btnW = (rightW - 8) / 5;
            for (int i = 0; i < CATEGORIES.length; i++) {
                int bx = rightX + 4 + i * (btnW + 1);
                int color = CATEGORIES[i].equals(curCat) ? 0xFF3D7A40 : 0xFF1E222A;
                gui.fill(bx, btnY, bx + btnW, btnY + 16, CATEGORIES[i].equals(curCat) ? 0xFF1A2E1A : 0x2212151A);
                gui.renderOutline(bx, btnY, btnW, 16, color);
                gui.drawCenteredString(PWPTheme.Fonts.display(), CATEGORIES[i].substring(0, Math.min(4, CATEGORIES[i].length())), bx + btnW / 2, btnY + 3, color);
            }

            if (isCategory) {
                gui.drawCenteredString(PWPTheme.Fonts.display(), "Any " + curCat + " skin equipped by player", rightX + rightW / 2, btnY + 24, 0xFF3D7A40);
                gui.drawCenteredString(PWPTheme.Fonts.display(), "will replace default item in this slot", rightX + rightW / 2, btnY + 36, 0xFF3D7A40);
            } else {
                List<String> allowed = allowedSkins.computeIfAbsent(selectedSlot, k -> new ArrayList<>());
                String tag = getWeaponTagForSlot(selectedSlot);
                int totalMatching = 0;
                for (SkinOption so : allSkins) {
                    if (so.weaponTag.equals(tag) || so.weaponTag.equals("any")) totalMatching++;
                }
                gui.drawCenteredString(PWPTheme.Fonts.display(), "Skins: " + allowed.size() + "/" + totalMatching, rightX + rightW / 2, btnY + 24, 0xFFC8CBCE);

                List<SkinOption> sortedSkins = new ArrayList<>(allSkins);
                sortedSkins.sort((a, b) -> {
                    boolean aA = allowed.contains(a.skinId);
                    boolean bA = allowed.contains(b.skinId);
                    if (aA != bA) return aA ? -1 : 1;
                    return 0;
                });
                int skinY = btnY + 36;
                for (int si = skinScroll; si < sortedSkins.size(); si++) {
                    SkinOption so = sortedSkins.get(si);
                    if (skinY + 18 > rightY + rightH - 4) break;
                    if (!so.weaponTag.equals(tag) && !so.weaponTag.equals("any")) continue;

                    boolean isAllowed = allowed.contains(so.skinId);
                    boolean isHover = mx >= rightX + 2 && mx <= rightX + rightW - 2 && my >= skinY && my <= skinY + 16;

                    gui.fill(rightX + 2, skinY, rightX + rightW - 2, skinY + 16, isAllowed ? 0xFF1A2E1A : isHover ? 0x881A1E26 : 0x2212151A);
                    gui.renderOutline(rightX + 2, skinY, rightW - 4, 16, isAllowed ? 0xFF3D7A40 : 0xFF1E222A);

                    String sn = so.name.length() > 20 ? so.name.substring(0, 19) + "." : so.name;
                    gui.drawString(PWPTheme.Fonts.display(), sn, rightX + 20, skinY + 3, isAllowed ? 0xFF3D7A40 : 0xFFC8CBCE, false);
                    gui.drawString(PWPTheme.Fonts.display(), isAllowed ? "\u2713" : "\u25CB", rightX + 6, skinY + 3, isAllowed ? 0xFF3D7A40 : 0xFF4A4D54, false);

                    skinY += 18;
                }
                if (totalMatching == 0) {
                    gui.drawCenteredString(PWPTheme.Fonts.display(), "No matching skins", rightX + rightW / 2, btnY + 60, 0xFF4A4D54);
                }
            }
        } else {
            gui.drawCenteredString(PWPTheme.Fonts.display(), "Click a weapon slot", this.width / 2, this.height / 2, 0xFF4A4D54);
            gui.drawCenteredString(PWPTheme.Fonts.display(), "to configure skins", this.width / 2, this.height / 2 + 14, 0xFF4A4D54);
        }

        super.render(gui, mx, my, pt);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int btn) {
        int leftX = 8, leftY = 34, sy = leftY + 16;
        for (int i = 0; i < weaponSlots.size(); i++) {
            int slot = weaponSlots.get(i);
            int col = i % 2, row = i / 2;
            int sx = leftX + 4 + col * 106, sry = sy + row * 28;
            if (sry + 26 > this.height - 14) break;
            if (mx >= sx && mx <= sx + 102 && my >= sry && my <= sry + 24) {
                selectedSlot = (selectedSlot == slot) ? -1 : slot;
                skinScroll = 0;
                return true;
            }
        }

        if (selectedSlot >= 0) {
            int rightX = this.width - 240, rightY = 34, rightW = 230, rightH = this.height - 50;
            int btnY = rightY + 4;
            int btnW = (rightW - 8) / 5;

            for (int i = 0; i < CATEGORIES.length; i++) {
                int bx = rightX + 4 + i * (btnW + 1);
                if (mx >= bx && mx <= bx + btnW && my >= btnY && my <= btnY + 16) {
                    String cat = CATEGORIES[i];
                    String current = slotCategories.get(selectedSlot);
                    if (cat.equals(current)) {
                        slotCategories.remove(selectedSlot);
                    } else {
                        slotCategories.put(selectedSlot, cat);
                        allowedSkins.remove(selectedSlot);
                    }
                    return true;
                }
            }

            if (!slotCategories.containsKey(selectedSlot)) {
                List<String> allowed = allowedSkins.computeIfAbsent(selectedSlot, k -> new ArrayList<>());
                String tag = getWeaponTagForSlot(selectedSlot);
                List<SkinOption> sortedSkins = new ArrayList<>(allSkins);
                sortedSkins.sort((a, b) -> {
                    boolean aA = allowed.contains(a.skinId);
                    boolean bA = allowed.contains(b.skinId);
                    if (aA != bA) return aA ? -1 : 1;
                    return 0;
                });
                int skinY = btnY + 36;
                for (int si = skinScroll; si < sortedSkins.size(); si++) {
                    SkinOption so = sortedSkins.get(si);
                    if (skinY + 18 > rightY + rightH - 4) break;
                    if (!so.weaponTag.equals(tag) && !so.weaponTag.equals("any")) continue;
                    if (mx >= rightX + 2 && mx <= rightX + rightW - 2 && my >= skinY && my <= skinY + 16) {
                        if (allowed.contains(so.skinId)) allowed.remove(so.skinId);
                        else allowed.add(so.skinId);
                        return true;
                    }
                    skinY += 18;
                }
            }
        }
        return super.mouseClicked(mx, my, btn);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        if (selectedSlot >= 0 && mx >= this.width - 240) {
            skinScroll = Math.max(0, Math.min(skinScroll - (int)delta * 3, allSkins.size() - 1));
            return true;
        }
        return super.mouseScrolled(mx, my, delta);
    }

    @Override public boolean isPauseScreen() { return false; }
}
