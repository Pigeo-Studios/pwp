/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.network.FriendlyByteBuf
 *  net.minecraft.server.level.ServerPlayer
 *  net.minecraft.world.inventory.AbstractContainerMenu
 *  net.minecraftforge.network.NetworkEvent$Context
 */
package com.example.aas.network;

import com.example.aas.menu.KitEditorMenu;
import com.example.aas.network.PacketHandler;
import com.example.aas.world.AASWorldData;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraftforge.network.NetworkEvent;

public class PacketSaveKit {
    private final String team;
    private final String kitName;
    private final boolean isLeader;
    private final int maxTeam;
    private final int maxSquad;
    private final int minSquadPlayers;
    private final boolean[] resupplyFlags;
    private final boolean[] nbtFlags;

    public PacketSaveKit(String team, String kitName, boolean isLeader, int maxTeam, int maxSquad, int minSquadPlayers, boolean[] resupplyFlags, boolean[] nbtFlags) {
        this.team = team;
        this.kitName = kitName;
        this.isLeader = isLeader;
        this.maxTeam = maxTeam;
        this.maxSquad = maxSquad;
        this.minSquadPlayers = minSquadPlayers;
        this.resupplyFlags = resupplyFlags;
        this.nbtFlags = nbtFlags;
    }

    public static void encode(PacketSaveKit msg, FriendlyByteBuf buf) {
        int i;
        buf.m_130070_(msg.team);
        buf.m_130070_(msg.kitName);
        buf.writeBoolean(msg.isLeader);
        buf.writeInt(msg.maxTeam);
        buf.writeInt(msg.maxSquad);
        buf.writeInt(msg.minSquadPlayers);
        for (i = 0; i < 49; ++i) {
            buf.writeBoolean(msg.resupplyFlags[i]);
        }
        for (i = 0; i < 49; ++i) {
            buf.writeBoolean(msg.nbtFlags[i]);
        }
    }

    public static PacketSaveKit decode(FriendlyByteBuf buf) {
        String t = buf.m_130277_();
        String k = buf.m_130277_();
        boolean l = buf.readBoolean();
        int mt = buf.readInt();
        int ms = buf.readInt();
        int minP = buf.readInt();
        boolean[] f1 = new boolean[49];
        for (int i = 0; i < 49; ++i) {
            f1[i] = buf.readBoolean();
        }
        boolean[] f2 = new boolean[49];
        for (int i = 0; i < 49; ++i) {
            f2[i] = buf.readBoolean();
        }
        return new PacketSaveKit(t, k, l, mt, ms, minP, f1, f2);
    }

    public static void handle(PacketSaveKit msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            AbstractContainerMenu patt2782$temp;
            ServerPlayer player = ((NetworkEvent.Context)ctx.get()).getSender();
            if (player != null && player.m_7500_() && (patt2782$temp = player.f_36096_) instanceof KitEditorMenu) {
                AASWorldData.KitInfo kit;
                KitEditorMenu menu = (KitEditorMenu)patt2782$temp;
                AASWorldData data = AASWorldData.get(player.m_284548_());
                AASWorldData.KitInfo kitInfo = kit = msg.team.equals("BLUE") ? data.blueKits.get(msg.kitName) : data.redKits.get(msg.kitName);
                if (kit != null) {
                    kit.isLeaderOnly = msg.isLeader;
                    kit.maxPerTeam = msg.maxTeam;
                    kit.maxPerSquad = msg.maxSquad;
                    kit.minSquadPlayers = msg.minSquadPlayers;
                    kit.resupplyFlags = msg.resupplyFlags;
                    kit.saveNbtFlags = msg.nbtFlags;
                    for (int i = 0; i < 49; ++i) {
                        kit.inventory.set(i, (Object)menu.kitInventory.m_8020_(i).m_41777_());
                    }
                    data.m_77762_();
                    PacketHandler.sendToAllClients(player.m_284548_(), data);
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }
}

