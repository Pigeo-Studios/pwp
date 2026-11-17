package com.pigeostudios.pwp.warfare.layout;

import java.util.List;

/**
 * Можно ли стартовать матч при текущих замечаниях валидатора раскладки. ERROR блокирует старт (все пути:
 * панель, команда, автостарт после ожидания/голосования), если блокировка включена в конфиге. Админ может
 * стартовать «всё равно» ({@code force}) — тогда старт разрешён, но помечен как принудительный (лог).
 */
public final class StartGate {
    private StartGate() {}

    /**
     * @param allowed старт разрешён
     * @param forced разрешён только благодаря force (ошибки были)
     * @param errors ERROR-замечания (пусто — раскладка без ошибок)
     */
    public record Decision(boolean allowed, boolean forced, List<Issue> errors) {
        public Decision {
            errors = List.copyOf(errors);
        }

        public boolean blocked() {
            return !allowed;
        }
    }

    /**
     * @param issues замечания валидатора (null — раскладка не загружалась: блокировать нечем)
     * @param blockEnabled флаг конфига «блокировать старт при ошибках»
     * @param force админ подтвердил «Стартовать всё равно»
     */
    public static Decision check(List<Issue> issues, boolean blockEnabled, boolean force) {
        List<Issue> errors = issues == null
                ? List.of()
                : issues.stream().filter(Issue::isError).toList();
        if (errors.isEmpty() || !blockEnabled) return new Decision(true, false, errors);
        if (force) return new Decision(true, true, errors);
        return new Decision(false, false, errors);
    }
}
