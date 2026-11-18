package com.pigeostudios.pwp.warfare.staging;

import com.pigeostudios.pwp.warfare.network.PacketHandler;
import com.pigeostudios.pwp.warfare.squad.SquadActions;
import com.pigeostudios.pwp.warfare.world.WarfareWorldData;
import com.pwp.api.net.Net;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.PacketDistributor;

/**
 * Сеть v2 стейджинга (этап 4). Регистрация — в КОНЕЦ канала pwpwarfare (после редактора карты).
 * Клиентские обработчики только делегируют в {@code client.ClientStaging}.
 */
public final class StagingPackets {
    private static final int MAX_KEY = 96;
    private static final int MAX_TEAM = 8;

    private StagingPackets() {}

    public static void register(Net.Registrar r) {
        r.s2c(State.class, State::encode, State::decode)
                .s2c(TeamResult.class, TeamResult::encode, TeamResult::decode)
                .c2s(TakeKit.class, TakeKit::encode, TakeKit::decode)
                .c2s(QuickJoinSquad.class, QuickJoinSquad::encode, QuickJoinSquad::decode);
    }

    /** Состояние ожидания одному игроку (вход, смена команды). */
    public static void sendState(ServerPlayer p, WarfareWorldData data) {
        PacketHandler.INSTANCE.send(PacketDistributor.PLAYER.with(() -> p), State.of(data));
    }

    /** Всем в измерении матча (раз в секунду ожидания + на переходах). */
    public static void broadcastState(ServerLevel level, WarfareWorldData data) {
        PacketHandler.INSTANCE.send(PacketDistributor.DIMENSION.with(level::dimension), State.of(data));
    }

    /**
     * Фаза ожидания для клиента: идёт ли, сколько секунд осталось, идёт ли обратный отсчёт GO.
     * Клиент гасит плашку сам, если пакеты перестали приходить.
     */
    public record State(boolean waiting, int secondsLeft, boolean countdown) implements Net.S2C {
        static State of(WarfareWorldData data) {
            return new State(
                    data.match.waitingActive && !data.match.isGameStarted,
                    Math.max(0, data.match.waitingTimer),
                    data.match.countdownActive);
        }

        public static void encode(State m, FriendlyByteBuf b) {
            b.writeBoolean(m.waiting);
            b.writeVarInt(m.secondsLeft);
            b.writeBoolean(m.countdown);
        }

        public static State decode(FriendlyByteBuf b) {
            return new State(b.readBoolean(), b.readVarInt(), b.readBoolean());
        }

        @Override
        public void handleClient() {
            com.pigeostudios.pwp.warfare.client.ClientStaging.onState(waiting, secondsLeft, countdown);
        }
    }

    /** Ответ на выбор команды: экран закрывается только при ok, иначе показывает ошибку у себя. */
    public record TeamResult(boolean ok, String messageKey, String team) implements Net.S2C {
        public static void encode(TeamResult m, FriendlyByteBuf b) {
            b.writeBoolean(m.ok);
            b.writeUtf(m.messageKey, MAX_KEY);
            b.writeUtf(m.team, MAX_TEAM);
        }

        public static TeamResult decode(FriendlyByteBuf b) {
            return new TeamResult(b.readBoolean(), b.readUtf(MAX_KEY), b.readUtf(MAX_TEAM));
        }

        @Override
        public void handleClient() {
            com.pigeostudios.pwp.warfare.client.ClientStaging.onTeamResult(ok, messageKey, team);
        }
    }

    /** «ВЗЯТЬ СНАРЯЖЕНИЕ» в меню K: всё проверяет сервер ({@link StagingService#takeKit}). */
    public record TakeKit() implements Net.C2S {
        public static void encode(TakeKit m, FriendlyByteBuf b) {}

        public static TakeKit decode(FriendlyByteBuf b) {
            return new TakeKit();
        }

        @Override
        public void handle(ServerPlayer sender) {
            StagingService.takeKit(sender);
        }
    }

    /** «Быстро вступить» в отряд ({@link SquadActions#quickJoin}). */
    public record QuickJoinSquad() implements Net.C2S {
        public static void encode(QuickJoinSquad m, FriendlyByteBuf b) {}

        public static QuickJoinSquad decode(FriendlyByteBuf b) {
            return new QuickJoinSquad();
        }

        @Override
        public void handle(ServerPlayer sender) {
            SquadActions.quickJoin(sender);
        }
    }
}
