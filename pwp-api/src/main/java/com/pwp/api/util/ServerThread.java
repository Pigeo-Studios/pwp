package com.pwp.api.util;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.server.ServerLifecycleHooks;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Возврат результатов асинхронных операций на главный поток сервера.
 *
 * <p>Правило проекта: никакого блокирующего ввода-вывода на главном потоке. HTTP идёт
 * в фоне, а работа с миром/игроками — только через {@link #thenOnServer}.
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

    /** true, если текущий поток — главный поток запущенного сервера. */
    public static boolean isServerThread() {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        return server != null && server.isSameThread();
    }
}
