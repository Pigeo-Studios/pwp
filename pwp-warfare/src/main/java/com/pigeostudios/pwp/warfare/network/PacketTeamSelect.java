package com.pigeostudios.pwp.warfare.network;

import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent.Context;

public class PacketTeamSelect {
    private final String teamName;

    public PacketTeamSelect(String teamName) {
        this.teamName = teamName;
    }

    public static void encode(PacketTeamSelect msg, FriendlyByteBuf buf) {
        buf.writeUtf(msg.teamName);
    }

    public static PacketTeamSelect decode(FriendlyByteBuf buf) {
        return new PacketTeamSelect(buf.readUtf(8));
    }

    /**
     * Выбор команды (экран команды, меню K, «АВТО»). Логика и ответ клиенту ({@code TeamResult}) —
     * {@link com.pigeostudios.pwp.warfare.staging.StagingService#selectTeam}.
     */
    public static void handle(PacketTeamSelect msg, Supplier<Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null || msg.teamName.length() > 8) return;
            com.pigeostudios.pwp.warfare.staging.StagingService.selectTeam(player, msg.teamName);
        });
        ctx.get().setPacketHandled(true);
    }
}
