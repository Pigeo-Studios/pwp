/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.network.FriendlyByteBuf
 *  net.minecraft.network.chat.Component
 *  net.minecraft.server.level.ServerPlayer
 *  net.minecraft.world.Container
 *  net.minecraft.world.MenuProvider
 *  net.minecraft.world.SimpleContainer
 *  net.minecraft.world.entity.player.Inventory
 *  net.minecraft.world.entity.player.Player
 *  net.minecraft.world.inventory.AbstractContainerMenu
 *  net.minecraft.world.item.ItemStack
 *  net.minecraftforge.network.NetworkEvent$Context
 *  net.minecraftforge.network.NetworkHooks
 */
package com.example.aas.network;

import com.example.aas.menu.KitEditorMenu;
import com.example.aas.world.AASWorldData;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkHooks;

public class PacketOpenKitEditor {
    private final String team;
    private final String kitName;

    public PacketOpenKitEditor(String team, String kitName) {
        this.team = team;
        this.kitName = kitName;
    }

    public static void encode(PacketOpenKitEditor msg, FriendlyByteBuf buf) {
        buf.m_130070_(msg.team);
        buf.m_130070_(msg.kitName);
    }

    public static PacketOpenKitEditor decode(FriendlyByteBuf buf) {
        return new PacketOpenKitEditor(buf.m_130277_(), buf.m_130277_());
    }

    public static void handle(final PacketOpenKitEditor msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ((NetworkEvent.Context)ctx.get()).getSender();
            if (player != null && player.m_7500_()) {
                AASWorldData.KitInfo kit;
                AASWorldData data = AASWorldData.get(player.m_284548_());
                AASWorldData.KitInfo kitInfo = kit = msg.team.equals("BLUE") ? data.blueKits.get(msg.kitName) : data.redKits.get(msg.kitName);
                if (kit != null) {
                    NetworkHooks.openScreen((ServerPlayer)player, (MenuProvider)new MenuProvider(){

                        public Component m_5446_() {
                            return Component.m_237113_((String)("Edit Kit: " + kit.name));
                        }

                        public AbstractContainerMenu m_7208_(int id, Inventory inv, Player p) {
                            SimpleContainer container = new SimpleContainer(49);
                            for (int i = 0; i < 49; ++i) {
                                container.m_6836_(i, ((ItemStack)kit.inventory.get(i)).m_41777_());
                            }
                            return new KitEditorMenu(id, inv, (Container)container, msg.team, kit.name, kit.isLeaderOnly, kit.maxPerTeam, kit.maxPerSquad, kit.minSquadPlayers, kit.resupplyFlags, kit.saveNbtFlags);
                        }
                    }, buf -> {
                        buf.m_130070_(msg.team);
                        buf.m_130070_(msg.kitName);
                        buf.writeBoolean(kit.isLeaderOnly);
                        buf.writeInt(kit.maxPerTeam);
                        buf.writeInt(kit.maxPerSquad);
                        buf.writeInt(kit.minSquadPlayers);
                        for (boolean f : kit.resupplyFlags) {
                            buf.writeBoolean(f);
                        }
                        for (boolean f : kit.saveNbtFlags) {
                            buf.writeBoolean(f);
                        }
                    });
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }
}

