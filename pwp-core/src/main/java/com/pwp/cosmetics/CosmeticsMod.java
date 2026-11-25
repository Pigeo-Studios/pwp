package com.pwp.cosmetics;

import com.pwp.cosmetics.network.PacketCosmeticEquip;
import com.pwp.cosmetics.network.PacketCosmeticEquipResult;
import net.minecraftforge.network.simple.SimpleChannel;

/** Косметика (часть pwp-core). Пакеты — в канале core ({@link com.pwp.coreclient.network.PacketHandler}). */
public class CosmeticsMod {

    public static final String MODID = "pwp_cosmetics";
    public static final SimpleChannel NETWORK = com.pwp.coreclient.network.PacketHandler.INSTANCE;

    public CosmeticsMod() {
        SkinRegistry.initDefaults();
        if (net.minecraftforge.fml.loading.FMLEnvironment.dist == net.minecraftforge.api.distmarker.Dist.CLIENT) {
            // тест-стенд UI (/pwpui): регистрирует экраны только при включённом стенде
            com.pwp.cosmetics.gui.CosmeticsUiFixtures.register();
        }
    }

    /** Регистрирует пакеты косметики в канале core начиная с {@code id}; возвращает следующий свободный id. */
    public static int registerPackets(SimpleChannel ch, int id) {
        ch.registerMessage(
                id++,
                PacketCosmeticEquip.class,
                PacketCosmeticEquip::encode,
                PacketCosmeticEquip::decode,
                PacketCosmeticEquip::handle);
        ch.registerMessage(
                id++,
                PacketCosmeticEquipResult.class,
                PacketCosmeticEquipResult::encode,
                PacketCosmeticEquipResult::decode,
                PacketCosmeticEquipResult::handle);
        return id;
    }
}
