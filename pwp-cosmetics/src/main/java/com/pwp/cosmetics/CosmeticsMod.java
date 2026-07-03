package com.pwp.cosmetics;

import com.pwp.cosmetics.network.PacketSyncCosmeticEquip;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

@Mod("pwp_cosmetics")
public class CosmeticsMod {

    public static final String MODID = "pwp_cosmetics";
    public static SimpleChannel NETWORK;
    private static int packetId = 0;

    public CosmeticsMod() {
        SkinRegistry.initDefaults();
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        initNetwork();
    }

    private void initNetwork() {
        NETWORK = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(MODID, "main"),
            () -> "1", "1"::equals, "1"::equals
        );
        NETWORK.registerMessage(
            packetId++, PacketSyncCosmeticEquip.class,
            PacketSyncCosmeticEquip::encode,
            PacketSyncCosmeticEquip::decode,
            PacketSyncCosmeticEquip::handle
        );
    }

}
