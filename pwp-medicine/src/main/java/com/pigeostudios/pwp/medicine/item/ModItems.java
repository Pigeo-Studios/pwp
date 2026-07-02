package com.pigeostudios.pwp.medicine.item;

import com.pigeostudios.pwp.medicine.item.BandageItem;
import com.pigeostudios.pwp.medicine.item.MedkitItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.IForgeRegistry;
import net.minecraftforge.registries.RegistryObject;

// Регистрация предметов мода PWP Medicine.
public class ModItems {
    // Регистратор предметов с modid pwp_medicine
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create((IForgeRegistry)ForgeRegistries.ITEMS, (String)"pwp_medicine");
    // Бинт: стак до 16, обычной редкости
    public static final RegistryObject<Item> BANDAGE = ITEMS.register("bandage", () -> new BandageItem(new Item.Properties().stacksTo(16).rarity(Rarity.COMMON)));
    // Аптечка: стак до 1
    public static final RegistryObject<Item> MEDKIT = ITEMS.register("medkit", () -> new MedkitItem(new Item.Properties().stacksTo(1)));

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
