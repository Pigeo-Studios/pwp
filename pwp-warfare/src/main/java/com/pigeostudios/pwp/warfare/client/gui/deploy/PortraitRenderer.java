package com.pigeostudios.pwp.warfare.client.gui.deploy;

import com.pigeostudios.pwp.warfare.client.gui.ui.WDraw;
import com.pigeostudios.pwp.warfare.client.gui.ui.WTheme;
import com.pwp.coreclient.gui.theme.PWPTheme;
import com.pwp.coreclient.ui.UIDraw;
import com.pwp.coreclient.ui.layout.Rect;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.joml.Quaternionf;

public class PortraitRenderer {
    private static final org.slf4j.Logger PWP_LOG = org.slf4j.LoggerFactory.getLogger(PortraitRenderer.class);

    private final ItemStack[] saved = new ItemStack[6];
    private boolean inSwap;

    /**
     * Портрет бойца с выбранным оружием и бронёй кита в области {@code area}: углублённая
     * подложка с мягким градиентом «пола»; модель вращается перетаскиванием ({@link #drag}).
     */
    public void render(
            GuiGraphics gui, Rect area, int mx, int my, ItemStack weapon, List<ItemStack> armorPieces, float o) {
        int x = area.x(), y = area.y(), w = area.w(), panelH = area.h();
        WDraw.well(gui, area, o);
        UIDraw.gradientV(gui, area.bottomStrip(panelH / 3), 0, UIDraw.fade(PWPTheme.Styles.Glass.HEADER_TOP, o));
        UIDraw.frame(gui, area, UIDraw.fade(PWPTheme.Styles.Glass.FRAME_TINT, o));

        var p = Minecraft.getInstance().player;
        if (p != null) {
            boolean dead = p.isDeadOrDying();
            int savedDeathTime = p.deathTime;
            if (dead) p.deathTime = 0;
            try {
                swap(p, weapon, armorPieces);
            } catch (Exception e) {
                com.pwp.api.util.LogOnce.warn(
                        PWP_LOG, "PortraitRenderer.swap", "PortraitRenderer: подмена экипировки", e);
                restore(p);
            }

            int sc = Math.min(WTheme.PORTRAIT_SCALE_MAX, Math.max(WTheme.PORTRAIT_SCALE_MIN, Math.min(w, panelH) / 3));
            int entityX = x + w / 2;
            int entityY = y + panelH / 2 + panelH / 6;

            gui.enableScissor(x, y, x + w, y + panelH);
            gui.pose().pushPose();
            try {
                renderRotated(gui, entityX, entityY, sc, p);
            } catch (RuntimeException e) {
                com.pwp.api.util.LogOnce.warn(
                        PWP_LOG, "PortraitRenderer:render", "PortraitRenderer.render: исключение", e);
            }
            gui.pose().popPose();
            gui.disableScissor();

            restore(p);
            if (dead) p.deathTime = savedDeathTime;
        }
    }

    /** Поворот куклы (градусы вокруг вертикали), меняется перетаскиванием мышью. */
    private float yaw = WTheme.PORTRAIT_DEFAULT_YAW;

    private boolean dragging;

    /** ЛКМ по кукле — начать вращение. */
    public boolean startDrag(Rect area, double mx, double my) {
        dragging = area.contains(mx, my);
        return dragging;
    }

    public boolean drag(double dx) {
        if (!dragging) return false;
        yaw = (yaw + (float) dx * WTheme.PORTRAIT_DRAG_DEG_PER_PX) % 360F;
        return true;
    }

    public void stopDrag() {
        dragging = false;
    }

    /**
     * Модель целиком повёрнута на {@link #yaw} (как в Squad: вращение мышью, а не «смотрит на
     * курсор»). Повороты сущности подменяются только на время отрисовки.
     */
    private void renderRotated(GuiGraphics gui, int x, int y, int scale, LivingEntity e) {
        float bodyRot = e.yBodyRot, yRot = e.getYRot(), xRot = e.getXRot();
        float headO = e.yHeadRotO, head = e.yHeadRot, bodyO = e.yBodyRotO;
        Quaternionf pose = new Quaternionf().rotateZ((float) Math.PI);
        Quaternionf cam = new Quaternionf().rotateX((float) Math.toRadians(-8.0));
        pose.mul(cam);
        float r = 180F + yaw;
        e.yBodyRot = r;
        e.yBodyRotO = r;
        e.setYRot(r);
        e.setXRot(0F);
        e.yHeadRot = r;
        e.yHeadRotO = r;
        try {
            InventoryScreen.renderEntityInInventory(gui, x, y, scale, pose, cam, e);
        } finally {
            e.yBodyRot = bodyRot;
            e.yBodyRotO = bodyO;
            e.setYRot(yRot);
            e.setXRot(xRot);
            e.yHeadRotO = headO;
            e.yHeadRot = head;
        }
    }

    private void swap(net.minecraft.world.entity.player.Player p, ItemStack weapon, List<ItemStack> armor) {
        if (inSwap) {
            restore(p);
            return;
        }
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
        if (!inSwap) return;
        inSwap = false;
        p.setItemSlot(EquipmentSlot.MAINHAND, saved[0]);
        p.setItemSlot(EquipmentSlot.OFFHAND, saved[1]);
        p.setItemSlot(EquipmentSlot.HEAD, saved[2]);
        p.setItemSlot(EquipmentSlot.CHEST, saved[3]);
        p.setItemSlot(EquipmentSlot.LEGS, saved[4]);
        p.setItemSlot(EquipmentSlot.FEET, saved[5]);
    }
}
