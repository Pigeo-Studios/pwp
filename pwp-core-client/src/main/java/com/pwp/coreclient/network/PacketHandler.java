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
        INSTANCE.registerMessage(id++, VoteMapPacket.class,
                VoteMapPacket::encode,
                VoteMapPacket::decode,
                VoteMapPacket::handle);
        INSTANCE.registerMessage(id++, JoinMatchPacket.class,
                JoinMatchPacket::encode,
                JoinMatchPacket::decode,
                JoinMatchPacket::handle);
        INSTANCE.registerMessage(id++, JoinMatchServerPacket.class,
                JoinMatchServerPacket::encode,
                JoinMatchServerPacket::decode,
                JoinMatchServerPacket::handle);
        INSTANCE.registerMessage(id++, VoteModePacket.class,
                VoteModePacket::encode,
                VoteModePacket::decode,
                VoteModePacket::handle);
        INSTANCE.registerMessage(id++, PacketDataRequest.class,
                PacketDataRequest::encode,
                PacketDataRequest::decode,
                PacketDataRequest::handle);
        INSTANCE.registerMessage(id++, PacketDataResponse.class,
                PacketDataResponse::encode,
                PacketDataResponse::decode,
                PacketDataResponse::handle);
        INSTANCE.registerMessage(id++, PacketAction.class,
                PacketAction::encode,
                PacketAction::decode,
                PacketAction::handle);
        INSTANCE.registerMessage(id++, VoteFactionPacket.class,
                VoteFactionPacket::encode,
                VoteFactionPacket::decode,
                VoteFactionPacket::handle);
        INSTANCE.registerMessage(id++, LobbyStatePacket.class,
                LobbyStatePacket::encode,
                LobbyStatePacket::decode,
                LobbyStatePacket::handle);
        INSTANCE.registerMessage(id++, PacketDonatorTiers.class,
                PacketDonatorTiers::encode,
                PacketDonatorTiers::decode,
                PacketDonatorTiers::handle);

    }
}
