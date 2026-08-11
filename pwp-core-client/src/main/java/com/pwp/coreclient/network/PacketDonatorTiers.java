package com.pwp.coreclient.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Синхронизация донат-тиров и ролей игроков (сервер лобби -> клиент). Донатеры tier != NONE и роли ADMIN/MODERATOR. */
public class PacketDonatorTiers {

    public final String[] uuids;
    public final String[] tiers;
    public final String[] roles;

    public PacketDonatorTiers(String[] uuids, String[] tiers, String[] roles) {
        this.uuids = uuids;
        this.tiers = tiers;
        this.roles = roles;
    }

    public static void encode(PacketDonatorTiers msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.uuids.length);
        for (int i = 0; i < msg.uuids.length; i++) {
            buf.writeUtf(msg.uuids[i]);
            String t = msg.tiers[i];
            buf.writeUtf(t != null ? t : "NONE");
            String r = msg.roles[i];
            buf.writeUtf(r != null ? r : "");
        }
    }

    public static PacketDonatorTiers decode(FriendlyByteBuf buf) {
        int len = buf.readInt();
        String[] uuids = new String[len];
        String[] tiers = new String[len];
        String[] roles = new String[len];
        for (int i = 0; i < len; i++) {
            uuids[i] = buf.readUtf();
            tiers[i] = buf.readUtf();
            roles[i] = buf.readUtf();
        }
        return new PacketDonatorTiers(uuids, tiers, roles);
    }

    public static void handle(PacketDonatorTiers msg, Supplier<NetworkEvent.Context> ctx) {
        if (ctx.get().getDirection().getReceptionSide().isClient()) {
            ctx.get().enqueueWork(() -> com.pwp.coreclient.DonatorCache.apply(msg.uuids, msg.tiers, msg.roles));
        }
        ctx.get().setPacketHandled(true);
    }
}
