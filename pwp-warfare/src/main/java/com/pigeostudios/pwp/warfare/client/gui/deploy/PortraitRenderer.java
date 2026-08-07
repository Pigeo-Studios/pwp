package com.pigeostudios.pwp.warfare.client.gui.deploy;

import com.pwp.coreclient.gui.components.RoundedRect;
import com.pwp.coreclient.gui.theme.PWPTheme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public class PortraitRenderer {

    private final ItemStack[] saved = new ItemStack[6];
    private boolean inSwap;

    public void render(GuiGraphics gui, int x, int y, int w, int h, int mx, int my,
                       String desc, ItemStack weapon, List<ItemStack> armorPieces) {
        // Описание роли под портретом не рисуем (некуда — нет места), портрет занимает всю панель.
        // Текст описания живёт в БД (единый источник) и доступен через API китов.
        int panelH = h;

        RoundedRect.fill(gui, x, y, w, panelH, 6, 0xFF0E1117);
        RoundedRect.border(gui, x, y, w, panelH, 6, 1, PWPTheme.Colors.BORDER);

        var p = Minecraft.getInstance().player;
        if (p != null) {
            boolean dead = p.isDeadOrDying();
            int savedDeathTime = p.deathTime;
            if (dead) p.deathTime = 0;
            try {
                swap(p, weapon, armorPieces);
            } catch (Exception e) {
                restore(p);
            }

            int sc = Math.min(120, Math.max(70, Math.min(w, panelH) / 3));
            int entityX = x + w / 2;
            int entityY = y + panelH / 2 + panelH / 6;

            gui.enableScissor(x, y, x + w, y + panelH);
            float lx = (float)(entityX - mx) * 0.4f;
            float ly = (float)(entityY - panelH / 4 - my) * 0.4f;
            gui.pose().pushPose();
            try {
                InventoryScreen.renderEntityInInventoryFollowsMouse(gui, entityX, entityY, sc, lx, ly, p);
            } catch (Exception ignored) {}
            gui.pose().popPose();
            gui.disableScissor();

            restore(p);
            if (dead) p.deathTime = savedDeathTime;
        }
    }

    private void swap(net.minecraft.world.entity.player.Player p, ItemStack weapon, List<ItemStack> armor) {
        if (inSwap) { restore(p); return; }
        inSwap = true;
        saved[0] = p.getItemBySlot(EquipmentSlot.MAINHAND);
        saved[1] = p.getItemBySlot(EquipmentSlot.OFFHAND);
        saved[2] = p.getItemBySlot(EquipmentSlot.HEAD);
        saved[3] = p.getItemBySlot(EquipmentSlot.CHEST);
        saved[4] = p.getItemBySlot(EquipmentSlot.LEGS);
        saved[5] = p.getItemBySlot(EquipmentSlot.FEET);

        p.setItemSlot(EquipmentSlot.MAINHAND, weapon.isEmpty() ? saved[0] : weapon.copy());
        p.setItemSlot(EquipmentSlot.OFFHAND, ItemStack.EMPTY);

        if (armor != null) {
            for (ItemStack a : armor) {
                if (a.isEmpty()) continue;
                var item = a.getItem();
                if (item instanceof net.minecraft.world.item.ArmorItem ai) {
                    p.setItemSlot(ai.getEquipmentSlot(), a.copy());
                }
            }
        }
    }

    private void restore(net.minecraft.world.entity.player.Player p) {
        if (!inSwap) return; inSwap = false;
        p.setItemSlot(EquipmentSlot.MAINHAND, saved[0]);
        p.setItemSlot(EquipmentSlot.OFFHAND, saved[1]);
        p.setItemSlot(EquipmentSlot.HEAD, saved[2]);
        p.setItemSlot(EquipmentSlot.CHEST, saved[3]);
        p.setItemSlot(EquipmentSlot.LEGS, saved[4]);
        p.setItemSlot(EquipmentSlot.FEET, saved[5]);
    }
}
