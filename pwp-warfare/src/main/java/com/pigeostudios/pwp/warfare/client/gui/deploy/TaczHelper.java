package com.pigeostudios.pwp.warfare.client.gui.deploy;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

public class TaczHelper {

    private static Boolean taczLoaded;

    public static boolean isTaczLoaded() {
        if (taczLoaded == null) {
            taczLoaded = false;
            try {
                Class.forName("com.tacz.guns.GunMod");
                taczLoaded = true;
            } catch (ClassNotFoundException ignored) {}
        }
        return taczLoaded;
    }

    public static ItemStack getGunItem(String gunId, ItemStack fallback) {
        if (!isTaczLoaded()) return fallback;

        ResourceLocation itemKey = new ResourceLocation("tacz", "modern_kinetic_gun");
        Item gunItem = ForgeRegistries.ITEMS.getValue(itemKey);
        if (gunItem == null) {
            gunItem = ForgeRegistries.ITEMS.getValue(new ResourceLocation("tacz", "gun_item"));
        }
        if (gunItem == null) return fallback;

        ItemStack stack = new ItemStack(gunItem);
        CompoundTag tag = stack.getOrCreateTag();
        tag.putString("GunId", "tacz:" + gunId);
        tag.putString("gun_id", "tacz:" + gunId);
        return stack;
    }
}
