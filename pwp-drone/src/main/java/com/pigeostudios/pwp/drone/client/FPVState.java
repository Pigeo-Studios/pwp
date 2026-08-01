package com.pigeostudios.pwp.drone.client;

import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

import net.minecraft.world.phys.AABB;
import java.util.List;

public class FPVState {
    public static boolean isInFPV() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return false;

        ItemStack stack = mc.player.getMainHandItem();
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(stack.getItem());
        if (id == null || !id.toString().equals("superbwarfare:monitor")) return false;

        CompoundTag tag = stack.getTag();
        return tag != null && tag.getBoolean("Using") && tag.getBoolean("Linked");
    }

    public static Entity getLinkedDrone() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return null;

        // Check if riding drone directly
        Entity vehicle = mc.player.getVehicle();
        if (vehicle != null) return vehicle;

        // Read UUID from monitor NBT
        ItemStack stack = mc.player.getMainHandItem();
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains("LinkedDrone")) return null;
        String linkedUuid = tag.getString("LinkedDrone");

        // Search through ALL entities in the level
        // ClientLevel has an internal entities() Iterable, use getAllEntities() approach
        List<Entity> allEntities = mc.level.getEntitiesOfClass(Entity.class, new AABB(Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY), e -> true);
        for (Entity e : allEntities) {
            if (e != null && e.isAlive() && e.getStringUUID().equals(linkedUuid)) {
                return e;
            }
        }
        return null;
    }
}
