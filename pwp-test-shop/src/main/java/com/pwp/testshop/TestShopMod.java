package com.pwp.testshop;

import com.mojang.blaze3d.platform.InputConstants;
import com.pwp.testshop.gui.KitShopScreen;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.lwjgl.glfw.GLFW;

@Mod("pwptestshop")
public class TestShopMod {

    public static final String MOD_ID = "pwptestshop";
    public static KeyMapping OPEN_SHOP_KEY;

    public TestShopMod() {
        OPEN_SHOP_KEY = new KeyMapping(
            "key.pwptestshop.open_shop",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_U,
            "key.categories.pwptestshop"
        );
        FMLJavaModLoadingContext.get().getModEventBus().register(this);
        MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void onKeyRegister(RegisterKeyMappingsEvent event) {
        event.register(OPEN_SHOP_KEY);
    }

    @Mod.EventBusSubscriber(modid = MOD_ID, value = Dist.CLIENT)
    public static class ClientEvents {
        @SubscribeEvent
        public static void onKeyInput(InputEvent.Key event) {
            if (OPEN_SHOP_KEY.consumeClick()) {
                net.minecraft.client.Minecraft.getInstance().setScreen(
                    new KitShopScreen()
                );
            }
        }
    }
}
