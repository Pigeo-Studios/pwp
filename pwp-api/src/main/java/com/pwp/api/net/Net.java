package com.pwp.api.net;

import com.pwp.api.util.RateLimiter;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.simple.SimpleChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Сетевой слой v2: один пакет = одно действие, пакет — record-DTO, всё проверяет сервер.
 *
 * <p>Правила:
 *
 * <ul>
 *   <li>C→S пакет реализует {@link C2S}: {@link C2S#validate()} — проверка полей (длины, диапазоны,
 *       NaN) до любой логики; {@link C2S#handle} — выполняется на главном потоке сервера
 *       только для живого отправителя. Исключение в обработчике логируется и не роняет сервер.
 *   <li>S→C пакет реализует {@link S2C}; обработка — только на клиенте.
 *   <li>Направление фиксируется при регистрации: C2S, пришедший от сервера (и наоборот), отбрасывается.
 *   <li>Новые пакеты регистрируются в КОНЕЦ списка; удаление/перестановка — поднять версию канала.
 * </ul>
 */
public final class Net {
    private static final Logger LOG = LoggerFactory.getLogger(Net.class);

    /**
     * Отказы и ошибки — не чаще раза в 10 с на (игрок, пакет): злой клиент не забивает лог.
     * Отказ проверки — клиентский ввод, стектрейс не пишется.
     */
    private static final RateLimiter REJECT_LOG = new RateLimiter(10_000);

    private Net() {}

    /** Пакет клиент → сервер. */
    public interface C2S {
        /** null — пакет корректен, иначе причина отказа (пишется в лог, обработчик не вызывается). */
        default String validate() {
            return null;
        }

        /** Главный поток сервера, отправитель онлайн. */
        void handle(ServerPlayer sender);
    }

    /** Пакет сервер → клиент. Обработчик должен лишь делегировать в клиентский класс. */
    public interface S2C {
        void handleClient();
    }

    /** Счётчик id регистраций одного канала (новые — в конец). */
    public static final class Registrar {
        private final SimpleChannel channel;
        private int nextId;

        public Registrar(SimpleChannel channel, int firstId) {
            this.channel = channel;
            this.nextId = firstId;
        }

        public <T extends C2S> Registrar c2s(
                Class<T> type, BiConsumer<T, FriendlyByteBuf> encoder, Function<FriendlyByteBuf, T> decoder) {
            channel.messageBuilder(type, nextId++, NetworkDirection.PLAY_TO_SERVER)
                    .encoder(encoder)
                    .decoder(decoder)
                    .consumerMainThread(Net::handleC2S)
                    .add();
            return this;
        }

        public <T extends S2C> Registrar s2c(
                Class<T> type, BiConsumer<T, FriendlyByteBuf> encoder, Function<FriendlyByteBuf, T> decoder) {
            channel.messageBuilder(type, nextId++, NetworkDirection.PLAY_TO_CLIENT)
                    .encoder(encoder)
                    .decoder(decoder)
                    .consumerMainThread(Net::handleS2C)
                    .add();
            return this;
        }

        /** Следующий свободный id (для продолжения регистрации старыми registerMessage). */
        public int nextId() {
            return nextId;
        }
    }

    private static <T extends C2S> void handleC2S(T msg, Supplier<NetworkEvent.Context> ctx) {
        ServerPlayer sender = ctx.get().getSender();
        if (sender == null || sender.hasDisconnected()) return;
        String error;
        try {
            error = msg.validate();
        } catch (RuntimeException e) {
            error = "исключение при проверке: " + e;
        }
        String name = msg.getClass().getSimpleName();
        if (error != null) {
            if (REJECT_LOG.tryAcquire(sender.getStringUUID() + ":reject:" + name)) {
                LOG.warn(
                        "Пакет {} от {} отклонён: {} (повторы от игрока подавляются {} с)",
                        name,
                        sender.getGameProfile().getName(),
                        error,
                        REJECT_LOG.intervalMs() / 1000);
            }
            return;
        }
        try {
            msg.handle(sender);
        } catch (RuntimeException e) {
            // Ошибка обработчика — баг сервера: стектрейс нужен, но не чаще раза в интервал
            if (REJECT_LOG.tryAcquire(sender.getStringUUID() + ":error:" + name)) {
                LOG.error(
                        "Ошибка обработки пакета {} от {}",
                        name,
                        sender.getGameProfile().getName(),
                        e);
            }
        }
    }

    private static <T extends S2C> void handleS2C(T msg, Supplier<NetworkEvent.Context> ctx) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
            try {
                msg.handleClient();
            } catch (RuntimeException e) {
                LOG.error(
                        "Ошибка обработки пакета {} на клиенте", msg.getClass().getSimpleName(), e);
            }
        });
    }
}
