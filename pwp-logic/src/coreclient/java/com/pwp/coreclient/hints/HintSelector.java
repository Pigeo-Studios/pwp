package com.pwp.coreclient.hints;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Выбор подсказки «что делать дальше» (чистая логика, без Minecraft). Правила «ненавязчивости»:
 *
 * <ul>
 *   <li>одновременно видна одна подсказка; более важная НЕ перебивает уже показанную (без мигания);
 *   <li>подсказка висит не дольше {@link #SHOW_MS}; разовая ({@code once}) после этого считается
 *       показанной навсегда, обычная — «засыпает» на {@link #SNOOZE_MS};
 *   <li>условие пропало (игрок сделал, что просили) — подсказка гаснет; разовая, провисевшая хотя бы
 *       {@link #MIN_SEEN_MS}, считается показанной;
 *   <li>между подсказками — пауза {@link #GAP_MS};
 *   <li>из подходящих берётся с наибольшим приоритетом, при равенстве — раньше зарегистрированная.
 * </ul>
 */
public final class HintSelector {
    public static final long SHOW_MS = 9_000L;
    public static final long SNOOZE_MS = 90_000L;
    public static final long MIN_SEEN_MS = 1_500L;
    public static final long GAP_MS = 1_200L;

    /** Кандидат на этот кадр: активен ли (условие выполнено) — решает вызывающий. */
    public record Candidate(String id, int priority, boolean once, boolean active) {}

    /** Состояние между кадрами. {@link #seen} сохраняется в файл (разовые подсказки). */
    public static final class State {
        public final Set<String> seen = new HashSet<>();
        final Map<String, Long> snoozedUntil = new HashMap<>();
        String current;
        long shownAt;
        long quietUntil;
        /** true — {@link #seen} изменился, файл нужно переписать. */
        public boolean dirty;

        public String current() {
            return current;
        }

        public long shownAt() {
            return shownAt;
        }
    }

    private HintSelector() {}

    /** Подсказка на этот кадр (id) или null. */
    public static String update(List<Candidate> candidates, State st, long now) {
        if (st.current != null) {
            Candidate cur = find(candidates, st.current);
            boolean stillActive = cur != null && cur.active();
            long shown = now - st.shownAt;
            if (!stillActive) {
                if (cur != null && cur.once() && shown >= MIN_SEEN_MS) markSeen(st, cur.id());
                finish(st, now);
            } else if (shown >= SHOW_MS) {
                if (cur.once()) markSeen(st, cur.id());
                else st.snoozedUntil.put(cur.id(), now + SNOOZE_MS);
                finish(st, now);
            } else {
                return st.current;
            }
        }
        if (now < st.quietUntil) return null;
        Candidate best = null;
        for (Candidate c : candidates) {
            if (!eligible(c, st, now)) continue;
            if (best == null || c.priority() > best.priority()) best = c;
        }
        if (best == null) return null;
        st.current = best.id();
        st.shownAt = now;
        return st.current;
    }

    /** Скрыть все подсказки (настройка выключена / сброс по смене сервера) без пометки «показано». */
    public static void clear(State st) {
        st.current = null;
        st.snoozedUntil.clear();
        st.quietUntil = 0L;
    }

    private static boolean eligible(Candidate c, State st, long now) {
        if (!c.active()) return false;
        if (c.once() && st.seen.contains(c.id())) return false;
        Long snooze = st.snoozedUntil.get(c.id());
        return snooze == null || now >= snooze;
    }

    private static void finish(State st, long now) {
        st.current = null;
        st.quietUntil = now + GAP_MS;
    }

    private static void markSeen(State st, String id) {
        if (st.seen.add(id)) st.dirty = true;
    }

    private static Candidate find(List<Candidate> list, String id) {
        for (Candidate c : list) if (c.id().equals(id)) return c;
        return null;
    }
}
