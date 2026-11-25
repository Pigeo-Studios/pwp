package com.pwp.api.time;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import net.minecraftforge.server.ServerLifecycleHooks;

/**
 * Единственный источник игрового времени на сервере. Прямые вызовы {@code getTickCount()} /
 * {@code getGameTime()} в серверной логике запрещены — только через этот класс.
 *
 * <p>Два разных времени, путать их нельзя:
 *
 * <ul>
 *   <li>{@link #serverTicks} — счётчик тиков сервера. Монотонный, но <b>сбрасывается при каждом
 *       рестарте</b>. Только для состояния в памяти, живущего в пределах одного запуска: каналы
 *       деплоя дронов, таймеры сообщений, анти-спам. Значения, посчитанные от него, НЕЛЬЗЯ
 *       сохранять в persistentData/capability/SavedData — после рестарта они окажутся «в будущем»
 *       (инцидент 06.08.2026: кулдаун дрона «висел бесконечно»).
 *   <li>{@link #worldTicks} — мировое время уровня ({@code level.dat}). Переживает рестарт.
 *       Для всего, что сохраняется: кулдауны в данных игрока, таймеры построек, ресапплай.
 * </ul>
 *
 * Внутри одного процесса не смешивать: elapsed = serverTicks − worldTicks — бессмыслица
 * (инцидент: деплой дрона зависал, elapsed вечно отрицательный).
 */
public final class GameClock {
    private GameClock() {}

    /** Тики сервера с запуска (сбрасывается при рестарте). */
    public static long serverTicks(MinecraftServer server) {
        return server.getTickCount();
    }

    /** Тики текущего сервера; 0, если сервер не запущен (клиент в меню). */
    public static long serverTicks() {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        return server == null ? 0 : server.getTickCount();
    }

    /** Мировое время уровня (сохраняется в level.dat, переживает рестарт). */
    public static long worldTicks(Level level) {
        return level.getGameTime();
    }

    /** Реальное время, мс — только для интервалов вне игрового времени (HTTP-кэши, анти-спам). */
    public static long realMillis() {
        return System.currentTimeMillis();
    }
}
