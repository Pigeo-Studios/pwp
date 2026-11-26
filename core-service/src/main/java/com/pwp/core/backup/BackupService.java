package com.pwp.core.backup;

import com.pwp.core.CoreApplication;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import java.util.zip.GZIPOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Ночной бэкап БД (этап 10a): раз в 10 минут проверяет {@link BackupPlan#isDue}, снимает дамп
 * {@code mysqldump --single-transaction} (без блокировки таблиц), сжимает в .sql.gz, затем ротация. Пароль —
 * только через переменную окружения процесса {@code MYSQL_PWD} (не в командной строке и не в логах). Неполный
 * файл при ошибке удаляется. Состояние — {@link #status()} для {@code GET /api/v1/admin/backup}.
 */
public final class BackupService {
    private static final Logger log = LoggerFactory.getLogger(BackupService.class);
    private static final String[] DUMP_CANDIDATES = {
        "C:/xampp/mysql/bin/mysqldump.exe", "mysqldump", "mariadb-dump"
    };

    private static CoreApplication.BackupConfig cfg;
    private static CoreApplication.DatabaseConfig db;
    private static volatile String lastFile = "";
    private static volatile String lastError = "";
    private static volatile long lastAt;
    private static volatile boolean running;

    private BackupService() {}

    public static void start(CoreApplication.BackupConfig backup, CoreApplication.DatabaseConfig database) {
        cfg = backup;
        db = database;
        if (cfg == null || !cfg.enabled) {
            log.info("Бэкап БД выключен (backup.enabled=false)");
            return;
        }
        ScheduledExecutorService ex = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "PWP-Backup");
            t.setDaemon(true);
            return t;
        });
        ex.scheduleWithFixedDelay(BackupService::tick, 1, 10, TimeUnit.MINUTES);
        log.info("Бэкап БД: каждый день после {}:00 в {} (хранить {} дн.)", cfg.hour, dir(), cfg.keepDays);
    }

    private static Path dir() {
        return Path.of(cfg.dir == null || cfg.dir.isBlank() ? "backups" : cfg.dir).toAbsolutePath();
    }

    private static void tick() {
        try {
            if (BackupPlan.isDue(LocalDateTime.now(), cfg.hour, list(), db.name)) runNow();
        } catch (RuntimeException | IOException e) {
            lastError = e.getMessage();
            log.warn("Бэкап БД: проверка расписания упала", e);
        }
    }

    private static List<String> list() throws IOException {
        Path d = dir();
        if (!Files.isDirectory(d)) return List.of();
        try (Stream<Path> s = Files.list(d)) {
            return s.map(p -> p.getFileName().toString()).toList();
        }
    }

    /** Снять бэкап сейчас (из расписания или по кнопке админа). false — уже идёт или ошибка. */
    public static synchronized boolean runNow() {
        if (cfg == null || db == null || running) return false;
        running = true;
        Path out = null;
        try {
            Files.createDirectories(dir());
            out = dir().resolve(BackupPlan.fileName(db.name, LocalDateTime.now()));
            String exe = findDump();
            if (exe == null) throw new IOException("mysqldump не найден — укажите backup.mysqldump в config.json");
            ProcessBuilder pb = new ProcessBuilder(
                    exe, "--single-transaction", "--routines", "--default-character-set=utf8mb4",
                    "-h", db.host, "-P", String.valueOf(db.port), "-u", db.user, db.name);
            pb.environment().put("MYSQL_PWD", db.password == null ? "" : db.password);
            pb.redirectError(ProcessBuilder.Redirect.DISCARD);
            Process p = pb.start();
            try (InputStream in = p.getInputStream(); OutputStream gz = new GZIPOutputStream(Files.newOutputStream(out))) {
                in.transferTo(gz);
            }
            if (!p.waitFor(30, TimeUnit.MINUTES) || p.exitValue() != 0) {
                p.destroyForcibly();
                throw new IOException("mysqldump завершился с ошибкой (код " + (p.isAlive() ? "timeout" : p.exitValue()) + ")");
            }
            if (Files.size(out) < 100) throw new IOException("пустой дамп");
            lastFile = out.getFileName().toString();
            lastAt = System.currentTimeMillis();
            lastError = "";
            log.info("Бэкап БД готов: {} ({} КБ)", out, Files.size(out) / 1024);
            for (String old : BackupPlan.toDelete(list(), db.name, LocalDate.now(), cfg.keepDays, cfg.keepMin)) {
                Files.deleteIfExists(dir().resolve(old));
                log.info("Бэкап БД: удалён старый {}", old);
            }
            return true;
        } catch (IOException | RuntimeException e) {
            lastError = e.getMessage();
            log.error("Бэкап БД не удался: {}", e.getMessage());
            deleteQuietly(out);
            return false;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            deleteQuietly(out);
            return false;
        } finally {
            running = false;
        }
    }

    private static void deleteQuietly(Path p) {
        if (p == null) return;
        try {
            Files.deleteIfExists(p);
        } catch (IOException e) {
            log.warn("Не удалось удалить неполный бэкап {}", p, e);
        }
    }

    private static String findDump() {
        List<String> c = new ArrayList<>();
        if (cfg.mysqldump != null && !cfg.mysqldump.isBlank()) c.add(cfg.mysqldump);
        c.addAll(List.of(DUMP_CANDIDATES));
        for (String exe : c) {
            if (exe.contains("/") || exe.contains("\\")) {
                if (Files.isRegularFile(Path.of(exe))) return exe;
                continue;
            }
            try {
                Process p = new ProcessBuilder(exe, "--version")
                        .redirectErrorStream(true)
                        .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                        .start();
                if (p.waitFor(10, TimeUnit.SECONDS) && p.exitValue() == 0) return exe;
            } catch (IOException e) {
                // нет в PATH — пробуем следующий
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return null;
            }
        }
        return null;
    }

    public static Map<String, Object> status() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("enabled", cfg != null && cfg.enabled);
        m.put("running", running);
        m.put("dir", cfg == null ? "" : dir().toString());
        m.put("lastFile", lastFile);
        m.put("lastAt", lastAt);
        m.put("lastError", lastError);
        try {
            m.put("files", cfg == null ? List.of() : list().stream().filter(n -> BackupPlan.dateOf(db.name, n) != null).sorted().toList());
        } catch (IOException e) {
            m.put("files", List.of());
        }
        return m;
    }
}
