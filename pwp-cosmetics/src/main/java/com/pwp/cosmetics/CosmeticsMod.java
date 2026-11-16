package com.pwp.cosmetics;

import com.pwp.cosmetics.network.PacketCosmeticEquip;
import com.pwp.cosmetics.network.PacketCosmeticEquipResult;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

@Mod("pwp_cosmetics")
public class CosmeticsMod {

    public static final String MODID = "pwp_cosmetics";
    public static SimpleChannel NETWORK;
    private static int packetId = 0;

    public CosmeticsMod() {
        SkinRegistry.initDefaults();
        initNetwork();
        if (net.minecraftforge.fml.loading.FMLEnvironment.dist == net.minecraftforge.api.distmarker.Dist.CLIENT) {
            // тест-стенд UI (/pwpui): регистрирует экраны только при включённом стенде
            com.pwp.cosmetics.gui.CosmeticsUiFixtures.register();
        }
    }

    /** Версия протокола канала. 2 — экипировка по skinId (PacketCosmeticEquip) вместо SNBT от клиента. */
    private static final String PROTOCOL_VERSION = "2";

    private void initNetwork() {
        NETWORK = NetworkRegistry.newSimpleChannel(
                new ResourceLocation(MODID, "main"),
                () -> PROTOCOL_VERSION,
                PROTOCOL_VERSION::equals,
                PROTOCOL_VERSION::equals);
        NETWORK.registerMessage(
                packetId++,
                PacketCosmeticEquip.class,
                PacketCosmeticEquip::encode,
                PacketCosmeticEquip::decode,
                PacketCosmeticEquip::handle);
        NETWORK.registerMessage(
                packetId++,
                PacketCosmeticEquipResult.class,
                PacketCosmeticEquipResult::encode,
                PacketCosmeticEquipResult::decode,
                PacketCosmeticEquipResult::handle);
    }
}
