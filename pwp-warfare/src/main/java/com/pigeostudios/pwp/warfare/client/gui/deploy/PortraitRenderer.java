package com.pigeostudios.pwp.warfare.client.gui.deploy;

import com.pwp.coreclient.gui.components.RoundedRect;
import com.pwp.coreclient.gui.theme.PWPTheme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

public class PortraitRenderer {

    private final ItemStack[] saved = new ItemStack[2];
    private boolean inSwap;

    public void render(GuiGraphics gui, int x, int y, int w, int h, int mx, int my, String desc, ItemStack weapon) {
        var f = PWPTheme.Fonts.display();

        RoundedRect.fill(gui, x, y, w, h - 14, 6, 0xFF0E1117);
        RoundedRect.border(gui, x, y, w, h - 14, 6, 1, PWPTheme.Colors.BORDER);

        var p = Minecraft.getInstance().player;
        if (p != null) {
            boolean dead = p.isDeadOrDying();
            int savedDeathTime = p.deathTime;
            if (dead) p.deathTime = 0;
            swap(p, weapon);
            int cx = x + w / 2, cy = y + (h - 14) / 2 + 8;
            float lx = (float)(cx - mx) * 0.4f, ly = (float)(cy - 30 - my) * 0.4f;
            int scale = Math.min(64, Math.max(48, w * 16 / 100));
            gui.pose().pushPose();
            try {
                InventoryScreen.renderEntityInInventoryFollowsMouse(gui, x + w / 2, y + h - 14, scale, lx, ly, p);
            } catch (Exception ignored) {}
            gui.pose().popPose();
            restore(p);
            if (dead) p.deathTime = savedDeathTime;
        }

        gui.drawString(f, desc, x + 6, y + h - 14 + 2, PWPTheme.Colors.TEXT_SECONDARY, false);
    }

    private void swap(net.minecraft.world.entity.player.Player p, ItemStack weapon) {
        if (inSwap) return; inSwap = true;
        saved[0] = p.getItemBySlot(EquipmentSlot.MAINHAND);
        saved[1] = p.getItemBySlot(EquipmentSlot.OFFHAND);
        p.setItemSlot(EquipmentSlot.MAINHAND, weapon.isEmpty() ? saved[0] : weapon.copy());
        p.setItemSlot(EquipmentSlot.OFFHAND, ItemStack.EMPTY);
    }

    private void restore(net.minecraft.world.entity.player.Player p) {
        if (!inSwap) return; inSwap = false;
        p.setItemSlot(EquipmentSlot.MAINHAND, saved[0]);
        p.setItemSlot(EquipmentSlot.OFFHAND, saved[1]);
    }
}
