package com.pwp.coreclient.gui.devtest;

import com.pwp.coreclient.gui.screens.PWPRotatingBackground;
import com.pwp.coreclient.ui.hud.HudInventory;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * Превью PWP-стиля инвентаря (этап 6 HUD) без мира: настоящие ванильные меню ({@link InventoryMenu},
 * {@link ChestMenu}) на фиктивном {@link Inventory} без игрока — позиции слотов те же, что в игре.
 * Рисуется тот же {@link HudInventory}, что и в миксинах, независимо от настройки. Куклы нет (нет
 * сущности). Пакетов не шлёт, контейнер не открывает.
 */
@OnlyIn(Dist.CLIENT)
public final class InventoryStylePreviewScreen extends Screen {

    /** Размеры ванильных экранов 1.20.1: InventoryScreen 176x166, ContainerScreen 114 + 18·ряды. */
    private static final int IMAGE_W = 176;

    private static final int PLAYER_IMAGE_H = 166;
    private static final int CHEST_BASE_H = 114;
    private static final int ROW_H = 18;
    private static final int CHEST_ROWS = 3;
    /** Ванильная подпись инвентаря игрока: x = 8, y = imageHeight − 94. */
    private static final int INV_LABEL_X = 8;

    private static final int INV_LABEL_FROM_BOTTOM = 94;

    private final boolean chest;
    private final AbstractContainerMenu menu;
    private final int imageH;
    private int left;
    private int top;

    private InventoryStylePreviewScreen(boolean chest) {
        super(Component.literal("Inventory style preview"));
        this.chest = chest;
        Inventory inv = new Inventory(null);
        this.menu = chest ? ChestMenu.threeRows(0, inv) : new InventoryMenu(inv, true, null);
        this.imageH = chest ? CHEST_BASE_H + CHEST_ROWS * ROW_H : PLAYER_IMAGE_H;
        if (!UiGallery.emptyData()) fill(inv);
    }

    public static InventoryStylePreviewScreen player() {
        return new InventoryStylePreviewScreen(false);
    }

    public static InventoryStylePreviewScreen chest() {
        return new InventoryStylePreviewScreen(true);
    }

    /** Несколько предметов для контраста слотов (хотбар, броня, основной ряд). */
    private static void fill(Inventory inv) {
        inv.setItem(0, new ItemStack(Items.IRON_SWORD));
        inv.setItem(1, new ItemStack(Items.BOW));
        inv.setItem(2, new ItemStack(Items.ARROW, 48));
        inv.setItem(8, new ItemStack(Items.COOKED_BEEF, 12));
        inv.setItem(9, new ItemStack(Items.OAK_PLANKS, 64));
        inv.setItem(20, new ItemStack(Items.TNT, 3));
        inv.setItem(39, new ItemStack(Items.IRON_HELMET));
        inv.setItem(38, new ItemStack(Items.IRON_CHESTPLATE));
        inv.setItem(40, new ItemStack(Items.SHIELD));
    }

    @Override
    protected void init() {
        left = (width - IMAGE_W) / 2;
        top = (height - imageH) / 2;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        PWPRotatingBackground.render(g, 0, 0, width, height);
        List<Slot> slots = menu.slots;
        if (chest) {
            Component title = UiGallery.longStrings()
                    ? Component.literal("Ящик снабжения отряда «Северный узел» — боеприпасы и медицина")
                    : Component.translatable("container.chest");
            HudInventory.renderContainer(
                    g,
                    font,
                    slots,
                    left,
                    top,
                    IMAGE_W,
                    imageH,
                    title,
                    Component.translatable("container.inventory"),
                    INV_LABEL_X,
                    imageH - INV_LABEL_FROM_BOTTOM);
        } else {
            HudInventory.renderPlayer(g, font, slots, left, top, IMAGE_W, imageH, mouseX, mouseY, null);
        }
        for (int i = 0, n = slots.size(); i < n; i++) {
            Slot s = slots.get(i);
            ItemStack stack = s.getItem();
            if (stack.isEmpty()) continue;
            g.renderItem(stack, left + s.x, top + s.y);
            g.renderItemDecorations(font, stack, left + s.x, top + s.y);
        }
        super.render(g, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
