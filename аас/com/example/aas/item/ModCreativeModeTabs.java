/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.core.registries.Registries
 *  net.minecraft.network.chat.Component
 *  net.minecraft.resources.ResourceKey
 *  net.minecraft.world.item.CreativeModeTab
 *  net.minecraft.world.item.ItemStack
 *  net.minecraft.world.level.ItemLike
 *  net.minecraftforge.eventbus.api.IEventBus
 *  net.minecraftforge.registries.DeferredRegister
 *  net.minecraftforge.registries.RegistryObject
 */
package com.example.aas.item;

import com.example.aas.item.ModItems;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public class ModCreativeModeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create((ResourceKey)Registries.f_279569_, (String)"aas");
    public static final RegistryObject<CreativeModeTab> AAS_TAB = CREATIVE_MODE_TABS.register("aas_tab", () -> CreativeModeTab.builder().m_257737_(() -> new ItemStack((ItemLike)ModItems.SQUAD_LEADER_RADIO.get())).m_257941_((Component)Component.m_237113_((String)"Advance And Secure")).m_257501_((pParameters, pOutput) -> {
        pOutput.m_246326_((ItemLike)ModItems.SQUAD_LEADER_RADIO.get());
        pOutput.m_246326_((ItemLike)ModItems.ENTRENCHING_TOOL.get());
        pOutput.m_246326_((ItemLike)ModItems.VEHICLE_SPAWNER_ITEM.get());
        pOutput.m_246326_((ItemLike)ModItems.MAIN_SUPPLY_ITEM.get());
        pOutput.m_246326_((ItemLike)ModItems.KIT_SETUP_ITEM.get());
        pOutput.m_246326_((ItemLike)ModItems.GAME_START_TRIGGER_ITEM.get());
        pOutput.m_246326_((ItemLike)ModItems.M2_AMMO.get());
        pOutput.m_246326_((ItemLike)ModItems.AGS_AMMO.get());
        pOutput.m_246326_((ItemLike)ModItems.AMMO_BAG.get());
        pOutput.m_246326_((ItemLike)ModItems.BLUE_APC.get());
        pOutput.m_246326_((ItemLike)ModItems.BLUE_TANK.get());
        pOutput.m_246326_((ItemLike)ModItems.BLUE_HELICOPTER.get());
        pOutput.m_246326_((ItemLike)ModItems.BLUE_CAS_HELICOPTER.get());
        pOutput.m_246326_((ItemLike)ModItems.BLUE_CAS_FIGHTER.get());
        pOutput.m_246326_((ItemLike)ModItems.BLUE_COMBAT_VEHICLE.get());
        pOutput.m_246326_((ItemLike)ModItems.BLUE_INFANTRY_VEHICLE.get());
        pOutput.m_246326_((ItemLike)ModItems.BLUE_SUPPLY_MARKER.get());
        pOutput.m_246326_((ItemLike)ModItems.BLUE_SUPPLY_HELICOPTER.get());
        pOutput.m_246326_((ItemLike)ModItems.BLUE_STATIC_ZU.get());
        pOutput.m_246326_((ItemLike)ModItems.BLUE_MOBILE_ZU.get());
        pOutput.m_246326_((ItemLike)ModItems.BLUE_BOAT.get());
        pOutput.m_246326_((ItemLike)ModItems.BLUE_MOTORCYCLE.get());
        pOutput.m_246326_((ItemLike)ModItems.BLUE_LIGHT_SUPPLY.get());
        pOutput.m_246326_((ItemLike)ModItems.RED_APC.get());
        pOutput.m_246326_((ItemLike)ModItems.RED_TANK.get());
        pOutput.m_246326_((ItemLike)ModItems.RED_HELICOPTER.get());
        pOutput.m_246326_((ItemLike)ModItems.RED_CAS_HELICOPTER.get());
        pOutput.m_246326_((ItemLike)ModItems.RED_CAS_FIGHTER.get());
        pOutput.m_246326_((ItemLike)ModItems.RED_COMBAT_VEHICLE.get());
        pOutput.m_246326_((ItemLike)ModItems.RED_INFANTRY_VEHICLE.get());
        pOutput.m_246326_((ItemLike)ModItems.RED_SUPPLY_MARKER.get());
        pOutput.m_246326_((ItemLike)ModItems.RED_SUPPLY_HELICOPTER.get());
        pOutput.m_246326_((ItemLike)ModItems.RED_STATIC_ZU.get());
        pOutput.m_246326_((ItemLike)ModItems.RED_MOBILE_ZU.get());
        pOutput.m_246326_((ItemLike)ModItems.RED_BOAT.get());
        pOutput.m_246326_((ItemLike)ModItems.RED_MOTORCYCLE.get());
        pOutput.m_246326_((ItemLike)ModItems.RED_LIGHT_SUPPLY.get());
    }).m_257652_());

    public static void register(IEventBus eventBus) {
        CREATIVE_MODE_TABS.register(eventBus);
    }
}

