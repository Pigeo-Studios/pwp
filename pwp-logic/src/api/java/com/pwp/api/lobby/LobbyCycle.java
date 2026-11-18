package com.pwp.api.lobby;

/**
 * Машина состояний подготовки матча в лобби (этап 5): «сервер поднимается параллельно с голосованиями →
 * настройки доставлены и применены → пререндер (с потолком ожидания) → переброс». Чистая: время и наблюдения
 * передаёт вызывающий (лобби раз в секунду), побочные эффекты (запись файла, переброс, остановка процесса)
 * — по возвращённому {@link Action}.
 *
 * <p>Сбой (процесс умер, мир не поднялся за {@link Config#bootTimeoutMs}, настройки не применились за
 * {@link Config#applyTimeoutMs}, матч-сервер сообщил ошибку) — {@link Action#RETRY}, пока не исчерпан
 * {@link Config#maxRetries}, затем {@link Action#ABORT}; причина — {@link #error()} (ключ перевода
 * {@code pwp_core.lobby.prep.error.<код>}). Тишины нет: каждый исход виден вызывающему.
 */
public final class LobbyCycle {
    public enum Phase {
        IDLE,
        /** Копия шаблона/мира, запуск, загрузка мира. */
        PREPARING,
        /** Мир загружен, ждём конца голосований. */
        WAITING_VOTES,
        /** Настройки доставлены, ждём применения/пререндера. */
        FINISHING,
        TRANSFERRING,
        FAILED
    }

    public enum Action {
        NONE,
        DELIVER_SETTINGS,
        TRANSFER,
        RETRY,
        ABORT
    }

    public record Config(long bootTimeoutMs, long applyTimeoutMs, long prerenderGraceMs, int maxRetries) {
        /** Загрузка до 5 мин (как прежний таймаут запуска), применение 60 с, пререндер ждём до 90 с, 1 повтор. */
        public static final Config DEFAULT = new Config(300_000, 60_000, 90_000, 1);
    }

    /** Что лобби видит у матч-сервера в этот момент. */
    public record Observed(
            boolean alive,
            boolean worldLoaded,
            boolean settingsApplied,
            int prerenderDone,
            int prerenderTotal,
            String serverError) {
        public boolean prerenderFinished() {
            return prerenderTotal <= 0 || prerenderDone >= prerenderTotal;
        }
    }

    public static final String ERR_DIED = "died";
    public static final String ERR_BOOT_TIMEOUT = "boot_timeout";
    public static final String ERR_APPLY_TIMEOUT = "apply_timeout";
    public static final String ERR_SERVER = "server";

    private final Config cfg;
    private Phase phase = Phase.IDLE;
    private long launchedAt;
    private long deliveredAt;
    private long loadedSeenAt;
    private long appliedSeenAt;
    private boolean delivered;
    private int retries;
    private String error;

    public LobbyCycle(Config cfg) {
        this.cfg = cfg;
    }

    public Phase phase() {
        return phase;
    }

    public String error() {
        return error;
    }

    public int retries() {
        return retries;
    }

    public boolean delivered() {
        return delivered;
    }

    public long launchedAt() {
        return launchedAt;
    }

    /** Процесс матч-сервера запущен (первый раз — счётчик повторов с нуля). */
    public void launched(long now, boolean isRetry) {
        if (!isRetry) retries = 0;
        phase = Phase.PREPARING;
        launchedAt = now;
        deliveredAt = 0;
        loadedSeenAt = 0;
        appliedSeenAt = 0;
        delivered = false;
        error = null;
    }

    /** Лобби записало файл настроек. */
    public void settingsDelivered(long now) {
        delivered = true;
        deliveredAt = now;
        if (phase == Phase.PREPARING || phase == Phase.WAITING_VOTES) phase = Phase.FINISHING;
    }

    public void reset() {
        phase = Phase.IDLE;
        delivered = false;
        error = null;
        retries = 0;
    }

    /**
     * Шаг раз в секунду. {@code settingsReady} — все голосования закончены и настройки собраны.
     */
    public Action step(long now, Observed o, boolean settingsReady) {
        if (phase == Phase.IDLE || phase == Phase.TRANSFERRING || phase == Phase.FAILED) return Action.NONE;
        if (!o.alive()) return fail(ERR_DIED);
        if (o.serverError() != null) return fail(ERR_SERVER);
        if (!o.worldLoaded() && now - launchedAt > cfg.bootTimeoutMs()) return fail(ERR_BOOT_TIMEOUT);
        if (o.worldLoaded() && loadedSeenAt == 0) loadedSeenAt = now;
        if (!delivered) {
            if (settingsReady) return Action.DELIVER_SETTINGS;
            phase = o.worldLoaded() ? Phase.WAITING_VOTES : Phase.PREPARING;
            return Action.NONE;
        }
        phase = Phase.FINISHING;
        if (!o.settingsApplied()) {
            // применение начинается только после загрузки мира: отсчёт от позднего из «доставлено»/«загружен»
            if (o.worldLoaded() && now - Math.max(deliveredAt, loadedSeenAt) > cfg.applyTimeoutMs()) {
                return fail(ERR_APPLY_TIMEOUT);
            }
            return Action.NONE;
        }
        if (appliedSeenAt == 0) appliedSeenAt = now;
        if (o.prerenderFinished() || now - appliedSeenAt >= cfg.prerenderGraceMs()) {
            phase = Phase.TRANSFERRING;
            return Action.TRANSFER;
        }
        return Action.NONE;
    }

    private Action fail(String code) {
        error = code;
        if (retries < cfg.maxRetries()) {
            retries++;
            return Action.RETRY;
        }
        phase = Phase.FAILED;
        return Action.ABORT;
    }

    /**
     * Можно ли авто-циклу начать новое голосование: автостарт включён, лобби свободно (нет матча/голосования),
     * игроков не меньше {@code minPlayers}, и прошла пауза после сбоя ({@code now >= notBefore}).
     */
    public static boolean autoVoteAllowed(
            boolean autostart, boolean busy, int players, int minPlayers, long now, long notBefore) {
        return autostart && !busy && players >= Math.max(1, minPlayers) && now >= notBefore;
    }

    /**
     * Прогресс подготовки 0..100 для GUI: копирование/запуск — по времени (оценка {@code bootEstimateMs}, не
     * выше 50 до загрузки мира), после загрузки — 50 + доля пререндера. Без пререндера мир = 100.
     */
    public static int percent(long elapsedMs, long bootEstimateMs, Observed o) {
        if (o == null || !o.worldLoaded()) {
            if (bootEstimateMs <= 0) return 0;
            long p = elapsedMs * 50 / bootEstimateMs;
            return (int) Math.max(0, Math.min(49, p));
        }
        if (o.prerenderFinished()) return 100;
        return 50 + (int) ((long) o.prerenderDone() * 50 / o.prerenderTotal());
    }
}
