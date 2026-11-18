package com.pigeostudios.pwp.warfare.client;

import com.pigeostudios.pwp.warfare.block.ModBlocks;
import com.pigeostudios.pwp.warfare.client.gui.KitEditorScreen;
import com.pigeostudios.pwp.warfare.client.gui.VehicleSpawnerScreen;
import com.pigeostudios.pwp.warfare.client.renderer.AGS30Renderer;
import com.pigeostudios.pwp.warfare.client.renderer.M2BrowningRenderer;
import com.pigeostudios.pwp.warfare.client.renderer.M2BulletRenderer;
import com.pigeostudios.pwp.warfare.client.renderer.SupplyCrateRenderer;
import com.pigeostudios.pwp.warfare.client.renderer.VehicleSpawnerRenderer;
import com.pigeostudios.pwp.warfare.entity.ModEntities;
import com.pigeostudios.pwp.warfare.menu.ModMenuTypes;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent.RegisterLayerDefinitions;
import net.minecraftforge.client.event.EntityRenderersEvent.RegisterRenderers;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@EventBusSubscriber(modid = "pwpwarfare", bus = Bus.MOD, value = Dist.CLIENT)
// События клиентской инициализации мода: регистрация рендеров, меню
public class ClientModEvents {
    @SubscribeEvent
    public static void registerRenderers(RegisterRenderers event) {
        event.registerEntityRenderer((EntityType) ModEntities.M2_BROWNING.get(), M2BrowningRenderer::new);
        event.registerEntityRenderer((EntityType) ModEntities.M2_BULLET.get(), M2BulletRenderer::new);
        event.registerEntityRenderer((EntityType) ModEntities.AGS_30.get(), AGS30Renderer::new);
        event.registerBlockEntityRenderer(
                (BlockEntityType) ModBlocks.VEHICLE_SPAWNER_BE.get(), VehicleSpawnerRenderer::new);
        event.registerEntityRenderer(
                (EntityType) ModEntities.AGS_30_GRENADE.get(), context -> new ThrownItemRenderer(context, 0.5F, true));
        event.registerEntityRenderer((EntityType) ModEntities.SUPPLY_CRATE.get(), SupplyCrateRenderer::new);
    }

    @SubscribeEvent
    public static void registerLayerDefinitions(RegisterLayerDefinitions event) {}

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        // настройки карты/HUD в реестре интерфейса core-client (значения из файла применятся сами)
        WarfareUiSettings.init();
        // этап 4: подсказки «что дальше» матча (реестр core-client, выключатель — в настройках интерфейса)
        WarfareHints.register();
        // админ-панель core-client: вкладки «Матч» и «Карта»
        com.pigeostudios.pwp.warfare.client.admin.WarfareAdminTabs.register();
        // этап 3: редактор карты (кнопки в паузе и во вкладке «Карта»)
        com.pigeostudios.pwp.warfare.client.editor.MapEditorClient.init();
        com.pigeostudios.pwp.warfare.client.map.MapTileClient.init();
        // Ответы единой системы данных core-client → кэши warfare (ClientData)
        com.pwp.coreclient.network.data.ClientDataResponses.listen(
                com.pigeostudios.pwp.warfare.network.ClientDataHandler::handleData);
        // Тест-стенд UI (/pwpui): фикстуры экранов warfare — только в dev или с -Dpwp.uitest=true
        if (com.pwp.coreclient.gui.devtest.UiGallery.enabled()) {
            com.pigeostudios.pwp.warfare.client.gui.devtest.WarfareUiFixtures.registerAll();
            com.pigeostudios.pwp.warfare.client.gui.devtest.WarfareAdminFixtures.registerAll();
            com.pigeostudios.pwp.warfare.client.gui.devtest.KitSkinFixtures.registerAll();
        }
        event.enqueueWork(() -> {
            MenuScreens.register((MenuType) ModMenuTypes.VEHICLE_SPAWNER_MENU.get(), VehicleSpawnerScreen::new);
            MenuScreens.register((MenuType) ModMenuTypes.KIT_EDITOR_MENU.get(), KitEditorScreen::new);
        });
    }
}
