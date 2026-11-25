package com.pwp.coreclient.network.data;

import com.pwp.api.admin.LineRing;
import com.pwp.api.admin.LogScrub;
import java.util.List;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Property;

/**
 * Последние WARN/ERROR сервера для вкладки «Сервер» админ-панели. Аппендер log4j2 на корневом логгере
 * (тот же поток, что пишет в {@code latest.log}): сообщение + класс/текст исключения проходят
 * {@link LogScrub} (ключи, токены, пароли, IP игроков, длинные подписи — маскируются) и кладутся в кольцо
 * на {@value #KEEP} строк. Внутри {@code append} ничего не логируется (иначе рекурсия).
 */
public final class ServerLogTail {
    public static final int KEEP = 100;
    private static final LineRing RING = new LineRing(KEEP);
    private static volatile boolean installed;

    private ServerLogTail() {}

    /** Подключить один раз на сервере (ServerStartingEvent). */
    public static synchronized void install() {
        if (installed) return;
        if (!(LogManager.getRootLogger() instanceof Logger root)) return;
        Appender a = new Appender();
        a.start();
        root.addAppender(a);
        installed = true;
    }

    public static List<LineRing.Line> recent(int n) {
        return RING.recent(n);
    }

    private static final class Appender extends AbstractAppender {
        Appender() {
            super("PWPAdminLogTail", null, null, true, Property.EMPTY_ARRAY);
        }

        @Override
        public void append(LogEvent event) {
            if (!event.getLevel().isMoreSpecificThan(Level.WARN)) return;
            String msg = event.getMessage() == null ? "" : event.getMessage().getFormattedMessage();
            Throwable t = event.getThrown();
            if (t != null) msg = msg + " — " + t.getClass().getSimpleName() + ": " + t.getMessage();
            String logger = event.getLoggerName() == null ? "" : event.getLoggerName();
            int dot = logger.lastIndexOf('.');
            RING.add(
                    event.getTimeMillis(),
                    event.getLevel().name(),
                    dot >= 0 ? logger.substring(dot + 1) : logger,
                    LogScrub.scrub(msg));
        }
    }
}
