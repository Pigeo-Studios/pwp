package com.pwp.api.util;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.server.ServerLifecycleHooks;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Возврат результатов асинхронных операций на главный поток сервера.
 *
 * <p>Правило проекта: никакого блокирующего ввода-вывода на главном потоке. HTTP идёт
 * в фоне, а работа с миром/игроками — только через {@link #thenOnServer} / {@link #thenForPlayer}.
 *
 * <p>Если колбэк работает с игроком — только {@link #thenForPlayer}: пока шёл HTTP, игрок мог
 * выйти и зайти заново, захваченный {@code ServerPlayer} к этому моменту — мёртвый объект.
 */
public final class ServerThread {
    private static final Logger LOG = LoggerFactory.getLogger(ServerThread.class);

    private ServerThread() {}

    /**
     * Когда future завершится — выполняет consumer на главном потоке сервера.
     * Ошибки consumer'а логируются, а не теряются в future.
     */
    public static <T> void thenOnServer(CompletableFuture<T> future, MinecraftServer server, Consumer<T> consumer) {
        future.whenComplete((value, error) -> {
            if (error != null) {
                // CoreApi сам превращает ошибки в null — сюда попадают только сбои цепочек
                LOG.warn("Асинхронная операция завершилась ошибкой", error);
                return;
            }
            server.execute(() -> {
                try {
                    consumer.accept(value);
                } catch (RuntimeException e) {
                    LOG.error("Ошибка обработки результата на главном потоке", e);
                }
            });
        });
    }

    /**
     * Как {@link #thenOnServer}, но для колбэка, работающего с игроком: из захваченного игрока
     * берутся только UUID и сервер, на главном потоке игрок заново ищется в списке онлайн.
     * Игрок офлайн — колбэк не вызывается.
     */
    public static <T> void thenForPlayer(
            CompletableFuture<T> future, ServerPlayer player, BiConsumer<ServerPlayer, T> consumer) {
        UUID uuid = player.getUUID();
        MinecraftServer server = player.server;
        thenOnServer(future, server, value -> {
            ServerPlayer online = server.getPlayerList().getPlayer(uuid);
            if (online == null || online.hasDisconnected()) return;
            consumer.accept(online, value);
        });
    }

    /**
     * {@link #thenForPlayer} с защитой от спама: если у игрока уже идёт запрос с этим ключом
     * или не прошёл кулдаун — запрос не запускается и возвращается false. Слот освобождается,
     * когда future завершится (успехом или ошибкой).
     */
    public static <T> boolean guardedForPlayer(
            RequestGuard guard,
            String key,
            ServerPlayer player,
            java.util.function.Supplier<CompletableFuture<T>> call,
            BiConsumer<ServerPlayer, T> consumer) {
        UUID uuid = player.getUUID();
        if (!guard.tryAcquire(uuid, key)) return false;
        CompletableFuture<T> future;
        try {
            future = call.get();
        } catch (RuntimeException e) {
            guard.release(uuid, key);
            throw e;
        }
        future.whenComplete((v, e) -> guard.release(uuid, key));
        thenForPlayer(future, player, consumer);
        return true;
    }

    /** true, если текущий поток — главный поток запущенного сервера. */
    public static boolean isServerThread() {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        return server != null && server.isSameThread();
    }
}
