package com.pwp.api.lobby;

import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Действия лобби, вызываемые сетевыми пакетами pwp-core-client. Реализацию регистрирует
 * pwp-lobby при загрузке ({@link #install}); на матч-серверах лобби нет — вызовы игнорируются.
 *
 * <p>Заменяет рефлексию {@code Class.forName("com.pwp.lobby...")} из пакетов core-client:
 * зависимость явная и переживает обфускацию без keep-правил. Голосования за карту/режим/фракции
 * GUI отправляет командами ({@code /votefaction} и т.п.), отдельных пакетов для них нет.
 */
public interface LobbyActions {
    /** Перенести игрока в текущий активный матч. */
    void joinActiveMatch(ServerPlayer player);

    /** Перенести игрока в матч с данным id сервера. */
    void joinMatchById(int matchId, ServerPlayer player);

    /** Регистрация реализации (pwp-lobby). */
    static void install(LobbyActions impl) {
        Holder.impl = impl;
        Holder.LOG.info(
                "LobbyActions: реализация установлена ({})", impl.getClass().getSimpleName());
    }

    /** Реализация или null, если лобби на этом сервере нет. */
    static LobbyActions get() {
        return Holder.impl;
    }

    final class Holder {
        private static final Logger LOG = LoggerFactory.getLogger(LobbyActions.class);
        private static volatile LobbyActions impl;

        private Holder() {}
    }
}
