/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.network.FriendlyByteBuf
 *  net.minecraft.server.level.ServerPlayer
 *  net.minecraft.world.item.ItemStack
 *  net.minecraftforge.network.NetworkEvent$Context
 *  net.minecraftforge.network.PacketDistributor
 */
package com.example.aas.network;

import com.example.aas.network.PacketHandler;
import com.example.aas.network.PacketOpenPlayerKitMenu;
import com.example.aas.world.AASWorldData;
import java.util.ArrayList;
import java.util.Collection;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

public class PacketRequestKitMenu {
    public static void encode(PacketRequestKitMenu msg, FriendlyByteBuf buf) {
    }

    public static PacketRequestKitMenu decode(FriendlyByteBuf buf) {
        return new PacketRequestKitMenu();
    }

    public static void handle(PacketRequestKitMenu msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ((NetworkEvent.Context)ctx.get()).getSender();
            if (player == null || player.m_5647_() == null) {
                return;
            }
            AASWorldData data = AASWorldData.get(player.m_284548_());
            String teamName = player.m_5647_().m_5758_().toUpperCase();
            String pName = player.m_6302_();
            AASWorldData.Squad mySquad = null;
            for (AASWorldData.Squad s : data.squads) {
                if (!s.members.contains(pName)) continue;
                mySquad = s;
                break;
            }
            boolean amILeader = mySquad != null && mySquad.leader.equals(pName);
            ArrayList<PacketOpenPlayerKitMenu.KitDTO> dtoList = new ArrayList<PacketOpenPlayerKitMenu.KitDTO>();
            for (String kitName : AASWorldData.KIT_NAMES) {
                AASWorldData.KitInfo kit;
                AASWorldData.KitInfo kitInfo = kit = teamName.equals("BLUE") ? data.blueKits.get(kitName) : data.redKits.get(kitName);
                if (kit == null || kit.isLeaderOnly && !amILeader && !player.m_7500_() || kit.maxPerTeam == 0 && !kitName.equals("Unassigned")) continue;
                int tCount = 0;
                int sCount = 0;
                for (ServerPlayer p : player.f_8924_.m_6846_().m_11314_()) {
                    if (p == player || p.m_5647_() == null || !p.m_5647_().m_5758_().toUpperCase().equals(teamName)) continue;
                    String cKit = p.getPersistentData().m_128461_("AAS_CurrentKit");
                    String pKit = p.getPersistentData().m_128461_("AAS_PendingKit");
                    if (!cKit.equals(kitName) && !pKit.equals(kitName)) continue;
                    ++tCount;
                    if (mySquad == null || !mySquad.members.contains(p.m_6302_())) continue;
                    ++sCount;
                }
                boolean available = true;
                Object reason = "";
                if (kit.maxPerTeam > 0 && tCount >= kit.maxPerTeam) {
                    available = false;
                    reason = "Team Full (" + tCount + "/" + kit.maxPerTeam + ")";
                } else if (kit.maxPerSquad > 0 && sCount >= kit.maxPerSquad) {
                    available = false;
                    reason = "Squad Full (" + sCount + "/" + kit.maxPerSquad + ")";
                } else if (kit.minSquadPlayers > 0 && (mySquad == null || mySquad.members.size() < kit.minSquadPlayers)) {
                    available = false;
                    reason = "Need " + kit.minSquadPlayers + " players in Squad";
                }
                ArrayList<ItemStack> kitPreviewItems = new ArrayList<ItemStack>((Collection<ItemStack>)kit.inventory);
                dtoList.add(new PacketOpenPlayerKitMenu.KitDTO(kitName, available, (String)reason, kitPreviewItems));
            }
            PacketHandler.INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), (Object)new PacketOpenPlayerKitMenu(dtoList));
        });
        ctx.get().setPacketHandled(true);
    }
}

