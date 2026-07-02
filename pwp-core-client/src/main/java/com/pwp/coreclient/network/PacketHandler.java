package com.pwp.coreclient.network;

import com.pwp.coreclient.CoreClientMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public class PacketHandler {

    private static final String PROTOCOL_VERSION = "1";
    public static final SimpleChannel INSTANCE = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(CoreClientMod.MODID, "network"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
        );

    private static int id = 0;

    public static void register() {
        // Register everything on both sides
        INSTANCE.registerMessage(id++, ConnectToServerPacket.class,
                ConnectToServerPacket::encode,
                ConnectToServerPacket::decode,
                ConnectToServerPacket::handle);
        INSTANCE.registerMessage(id++, OpenVotingScreenPacket.class,
                OpenVotingScreenPacket::encode,
                OpenVotingScreenPacket::decode,
                OpenVotingScreenPacket::handle);
    }
}
