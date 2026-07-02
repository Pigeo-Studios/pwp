package com.pwp.coreclient;

import com.pwp.coreclient.network.ConnectToServerPacket;
import com.pwp.coreclient.network.OpenVotingScreenPacket;
import com.pwp.coreclient.network.PacketHandler;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(CoreClientMod.MODID)
public class CoreClientMod {

    public static final String MODID = "pwp_core_client";
    public static final String CORE_API_URL = "http://localhost:8080";
    public static final String CORE_API_KEY = "pwp_server_key_change_me";

    public CoreClientMod() {
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::commonSetup);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(PacketHandler::register);

        // Register client-only packets using DistExecutor
        event.enqueueWork(() ->
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                registerClientPackets()
            )
        );
    }

    private static void registerClientPackets() {
        int id = 10;
        PacketHandler.INSTANCE.registerMessage(id++, ConnectToServerPacket.class,
                ConnectToServerPacket::encode,
                ConnectToServerPacket::decode,
                ConnectToServerPacket::handle);
        PacketHandler.INSTANCE.registerMessage(id++, OpenVotingScreenPacket.class,
                OpenVotingScreenPacket::encode,
                OpenVotingScreenPacket::decode,
                OpenVotingScreenPacket::handle);
    }
}
