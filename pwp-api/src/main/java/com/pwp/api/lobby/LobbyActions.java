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
    /** Перенести игрока в матч с данным id сервера. */
    void joinMatchById(int matchId, ServerPlayer player);

    /*
     * Админ-панель (вкладка «Лобби»). Право op 2+ уже проверено пакетом. Возврат: null — успех,
     * иначе ключ перевода ошибки (уходит клиенту в actionResult).
     */

    /** Начать голосование за карту. */
    default String adminStartVote(ServerPlayer admin) {
        return "pwp_core.admin.error.no_lobby";
    }

    /** Остановить любое идущее голосование. */
    default String adminStopVote(ServerPlayer admin) {
        return "pwp_core.admin.error.no_lobby";
    }

    /** Применить настройки лобби; {@code persist} — ещё и записать на диск (право OWNER проверено пакетом). */
    default String adminSaveSettings(ServerPlayer admin, com.pwp.api.config.LobbySettings settings, boolean persist) {
        return "pwp_core.admin.error.no_lobby";
    }

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
