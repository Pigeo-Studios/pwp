package com.pigeostudios.pwp.warfare.data;

import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;

public class FactionVehicleData {
    public String faction;
    public String vehicleName;
    public String displayName;
    public String vehicleId;
    public float yaw;
    public int respawnTime = 60;
    public int initialTime = 60;
    public String category = "";
    public NonNullList<ItemStack> inventory = NonNullList.withSize(33, ItemStack.EMPTY);
}
