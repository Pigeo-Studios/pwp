package com.pwp.coreclient.network.data;

import com.pwp.api.admin.AuditTrail;
import com.pwp.api.security.PwpPermissions;
import com.pwp.api.util.LogOnce;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Аудит-лог админ-действий сервера: кто (ник + уровень), что, над кем, когда, итог. Пишется из единых
 * точек {@link AdminAction#runLocal} / {@code AdminAction.run} (все вкладки панели F6 и пакеты warfare идут
 * через них), отказы по правам — тоже. Файл {@code logs/pwp-admin-audit.log} дописывается в фоновом потоке
 * (главный поток не ждёт диск); последние {@value #KEEP} записей — в памяти для вкладки «Сервер».
 */
public final class AdminAudit {
    private static final Logger LOG = LoggerFactory.getLogger(AdminAudit.class);
    public static final int KEEP = 200;
    static final Path FILE = Path.of("logs", "pwp-admin-audit.log");

    private static final AuditTrail TRAIL = new AuditTrail(KEEP);
    private static final ExecutorService WRITER = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "PWP-AdminAudit");
        t.setDaemon(true);
        return t;
    });

    private AdminAudit() {}

    /** Записать действие {@code actor}. {@code error} — ключ/текст ошибки или null/"" при успехе. */
    public static void record(
            ServerPlayer actor, String action, String details, AuditTrail.Result result, String error) {
        AuditTrail.Entry e = TRAIL.add(
                System.currentTimeMillis(),
                actor.getGameProfile().getName(),
                PwpPermissions.level(actor).name(),
                action,
                details,
                result,
                error);
        String line = AuditTrail.format(e) + System.lineSeparator();
        WRITER.execute(() -> {
            try {
                Files.createDirectories(FILE.getParent());
                Files.writeString(
                        FILE, line, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            } catch (IOException ex) {
                LogOnce.warn(LOG, "admin_audit_write", "Аудит-лог не пишется в " + FILE, ex);
            }
        });
    }

    /** Последние {@code n} записей, новые первыми. */
    public static List<AuditTrail.Entry> recent(int n) {
        return TRAIL.recent(n);
    }
}
