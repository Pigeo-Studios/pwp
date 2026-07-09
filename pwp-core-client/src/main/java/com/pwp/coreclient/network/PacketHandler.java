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
        INSTANCE.registerMessage(id++, OpenMatchScreenPacket.class,
                OpenMatchScreenPacket::encode,
                OpenMatchScreenPacket::decode,
                OpenMatchScreenPacket::handle);
        INSTANCE.registerMessage(id++, VoteMapPacket.class,
                VoteMapPacket::encode,
                VoteMapPacket::decode,
                VoteMapPacket::handle);
        INSTANCE.registerMessage(id++, JoinMatchPacket.class,
                JoinMatchPacket::encode,
                JoinMatchPacket::decode,
                JoinMatchPacket::handle);
        INSTANCE.registerMessage(id++, OpenMatchListScreenPacket.class,
                OpenMatchListScreenPacket::encode,
                OpenMatchListScreenPacket::decode,
                OpenMatchListScreenPacket::handle);
        INSTANCE.registerMessage(id++, JoinMatchServerPacket.class,
                JoinMatchServerPacket::encode,
                JoinMatchServerPacket::decode,
                JoinMatchServerPacket::handle);
        INSTANCE.registerMessage(id++, VoteModePacket.class,
                VoteModePacket::encode,
                VoteModePacket::decode,
                VoteModePacket::handle);
        INSTANCE.registerMessage(id++, OpenModeVotePacket.class,
                OpenModeVotePacket::encode,
                OpenModeVotePacket::decode,
                OpenModeVotePacket::handle);
    }
}
