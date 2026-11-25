package com.pigeostudios.pwp.warfare.config;

import com.mojang.logging.LogUtils;
import com.pigeostudios.pwp.warfare.layout.Issue;
import com.pigeostudios.pwp.warfare.layout.MapLayout;
import java.util.List;
import org.slf4j.Logger;

/**
 * Раскладка карты, применённая из map_config на этом запуске сервера ({@code WarfareMod.applyMapConfig}), и
 * замечания валидатора. ERROR блокируют старт матча (этап 2, {@code GameLogicEvents.startGameCountdown},
 * флаг {@code WarfareConfig.BLOCK_START_ON_LAYOUT_ERRORS}); админ может стартовать принудительно (force).
 */
public final class MapLayoutState {
    private static final Logger LOGGER = LogUtils.getLogger();

    private static volatile MapLayout layout;
    private static volatile List<Issue> issues = List.of();
    private static volatile String source = "";
    /** Явная раскладка до авто-вывода (база для редактора и для слияния версий, этап 3); null — не загружалась. */
    private static volatile MapLayout explicit;

    private MapLayoutState() {}

    public static void set(MapLayout resolved, List<Issue> found, String from) {
        layout = resolved;
        issues = List.copyOf(found);
        source = from == null ? "" : from;
    }

    /** Явная раскладка (как в конфиге / последней применённой правке) — то, что правит редактор. */
    public static void setExplicit(MapLayout l) {
        explicit = l;
    }

    public static MapLayout explicit() {
        return explicit;
    }

    /** Откуда взята текущая раскладка (файл / БД / правка матча). */
    public static String source() {
        return source;
    }

    /** Разрешённая раскладка (после авто-вывода); null — map_config не загружался. */
    public static MapLayout layout() {
        return layout;
    }

    public static List<Issue> issues() {
        return issues;
    }

    public static boolean hasErrors() {
        return Issue.hasErrors(issues);
    }

    /** Лог замечаний: ERROR — error, WARN — warn. {@code phase} — когда проверяем (загрузка/старт матча). */
    public static void log(String phase) {
        if (layout == null) return;
        List<Issue> list = issues;
        long errors = list.stream().filter(Issue::isError).count();
        if (list.isEmpty()) {
            LOGGER.info("[MapLayout] {}: {} — OK, no issues", phase, source);
            return;
        }
        LOGGER.warn(
                "[MapLayout] {}: {} — {} error(s), {} warning(s){}",
                phase,
                source,
                errors,
                list.size() - errors,
                errors > 0
                        ? (WarfareConfig.BLOCK_START_ON_LAYOUT_ERRORS.get()
                                ? " (match start is BLOCKED; force from admin panel or /pwpwarfare gamestart true force)"
                                : " (blocking disabled: blockStartOnLayoutErrors=false)")
                        : "");
        for (Issue i : list) {
            if (i.isError()) LOGGER.error("[MapLayout]   {}", i.format());
            else LOGGER.warn("[MapLayout]   {}", i.format());
        }
    }
}
