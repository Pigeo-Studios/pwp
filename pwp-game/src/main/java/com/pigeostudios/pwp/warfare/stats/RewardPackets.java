package com.pigeostudios.pwp.warfare.stats;

import com.pwp.api.net.Net;
import net.minecraft.network.FriendlyByteBuf;

/**
 * Награды за матч игроку (сеть v2). Раньше XP/Coins считались и начислялись в core, но игрок их
 * не видел: экран победы открывается до расчёта, а значений в нём не было. Сервер шлёт пакет каждому
 * игроку после расчёта (фоновый поток → главный), экран победы подхватывает значения на лету.
 */
public final class RewardPackets {
    private RewardPackets() {}

    public static void register(Net.Registrar r) {
        r.s2c(MatchRewards.class, MatchRewards::encode, MatchRewards::decode);
    }

    public record MatchRewards(long xp, long coins, int assists) implements Net.S2C {
        public static void encode(MatchRewards m, FriendlyByteBuf b) {
            b.writeVarLong(Math.max(0, m.xp));
            b.writeVarLong(Math.max(0, m.coins));
            b.writeVarInt(Math.max(0, m.assists));
        }

        public static MatchRewards decode(FriendlyByteBuf b) {
            return new MatchRewards(b.readVarLong(), b.readVarLong(), b.readVarInt());
        }

        @Override
        public void handleClient() {
            com.pigeostudios.pwp.warfare.client.ClientData.onMatchRewards(xp, coins, assists);
        }
    }
}
