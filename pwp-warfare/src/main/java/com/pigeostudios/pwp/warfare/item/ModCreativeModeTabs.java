package com.pigeostudios.pwp.warfare.item;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

// Р РµРµСЃС‚СЂ РєСЂРµР°С‚РёРІРЅС‹С… РІРєР»Р°РґРѕРє РјРѕРґР°
// РЎРѕРґРµСЂР¶РёС‚ РІСЃРµ РїСЂРµРґРјРµС‚С‹ РјРѕРґР° РІ РѕРґРЅРѕР№ РІРєР»Р°РґРєРµ В«Advance And SecureВ»
public class ModCreativeModeTabs {
   public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, "pwpwarfare");
   public static final RegistryObject<CreativeModeTab> WARFARE_TAB = CREATIVE_MODE_TABS.register(
      "warfare_tab",
      () -> CreativeModeTab.builder()
         .icon(() -> new ItemStack((ItemLike)ModItems.SQUAD_LEADER_RADIO.get()))
         .title(Component.translatable("itemGroup.pwpwarfare"))
         .displayItems((pParameters, pOutput) -> {
            pOutput.accept((ItemLike)ModItems.SQUAD_LEADER_RADIO.get());
            pOutput.accept((ItemLike)ModItems.ENTRENCHING_TOOL.get());
            pOutput.accept((ItemLike)ModItems.VEHICLE_SPAWNER_ITEM.get());
            pOutput.accept((ItemLike)ModItems.MAIN_SUPPLY_ITEM.get());
            pOutput.accept((ItemLike)ModItems.KIT_SETUP_ITEM.get());
            pOutput.accept((ItemLike)ModItems.FACTION_KIT_SETUP_ITEM.get());
            pOutput.accept((ItemLike)ModItems.FACTION_VEHICLE_SETUP_ITEM.get());
            pOutput.accept((ItemLike)ModItems.SKIN_SETUP_ITEM.get());
            pOutput.accept((ItemLike)ModItems.GAME_START_TRIGGER_ITEM.get());
            pOutput.accept((ItemLike)ModItems.M2_AMMO.get());
            pOutput.accept((ItemLike)ModItems.AGS_AMMO.get());
            pOutput.accept((ItemLike)ModItems.AMMO_BAG.get());
            pOutput.accept((ItemLike)ModItems.BLUE_APC.get());
            pOutput.accept((ItemLike)ModItems.BLUE_TANK.get());
            pOutput.accept((ItemLike)ModItems.BLUE_HELICOPTER.get());
            pOutput.accept((ItemLike)ModItems.BLUE_CAS_HELICOPTER.get());
            pOutput.accept((ItemLike)ModItems.BLUE_CAS_FIGHTER.get());
            pOutput.accept((ItemLike)ModItems.BLUE_COMBAT_VEHICLE.get());
            pOutput.accept((ItemLike)ModItems.BLUE_INFANTRY_VEHICLE.get());
            pOutput.accept((ItemLike)ModItems.BLUE_SUPPLY_MARKER.get());
            pOutput.accept((ItemLike)ModItems.BLUE_SUPPLY_HELICOPTER.get());
            pOutput.accept((ItemLike)ModItems.BLUE_STATIC_ZU.get());
            pOutput.accept((ItemLike)ModItems.BLUE_MOBILE_ZU.get());
            pOutput.accept((ItemLike)ModItems.BLUE_BOAT.get());
            pOutput.accept((ItemLike)ModItems.BLUE_MOTORCYCLE.get());
            pOutput.accept((ItemLike)ModItems.BLUE_LIGHT_SUPPLY.get());
            pOutput.accept((ItemLike)ModItems.RED_APC.get());
            pOutput.accept((ItemLike)ModItems.RED_TANK.get());
            pOutput.accept((ItemLike)ModItems.RED_HELICOPTER.get());
            pOutput.accept((ItemLike)ModItems.RED_CAS_HELICOPTER.get());
            pOutput.accept((ItemLike)ModItems.RED_CAS_FIGHTER.get());
            pOutput.accept((ItemLike)ModItems.RED_COMBAT_VEHICLE.get());
            pOutput.accept((ItemLike)ModItems.RED_INFANTRY_VEHICLE.get());
            pOutput.accept((ItemLike)ModItems.RED_SUPPLY_MARKER.get());
            pOutput.accept((ItemLike)ModItems.RED_SUPPLY_HELICOPTER.get());
            pOutput.accept((ItemLike)ModItems.RED_STATIC_ZU.get());
            pOutput.accept((ItemLike)ModItems.RED_MOBILE_ZU.get());
            pOutput.accept((ItemLike)ModItems.RED_BOAT.get());
            pOutput.accept((ItemLike)ModItems.RED_MOTORCYCLE.get());
            pOutput.accept((ItemLike)ModItems.RED_LIGHT_SUPPLY.get());
         })
         .build()
   );

   public static void register(IEventBus eventBus) {
      CREATIVE_MODE_TABS.register(eventBus);
   }
}
