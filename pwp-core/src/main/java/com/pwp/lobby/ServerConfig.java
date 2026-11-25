package com.pwp.lobby;

import com.pwp.api.ServerRole;
import com.pwp.api.config.LobbySettings;
import com.pwp.api.config.ServerSettings;
import com.pwp.lobby.match.MatchAllocator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Настройки лобби-сервера (автостарт, лимит матчей, минимум игроков, предпрогрев) — секция {@code lobby}
 * единого файла {@code config/pwp-server.json} ({@link com.pwp.api.ServerRole}); до 25.11.2026 —
 * {@code config/pwp_lobby.json} (переносится один раз автоматически). ADMIN меняет значения до рестарта,
 * запись в файл («навсегда») — только OWNER (op 4 или роль owner): кнопка панели и команды от op 4.
 */
public final class ServerConfig {
    private static final Logger LOG = LoggerFactory.getLogger(ServerConfig.class);

    private static volatile LobbySettings settings = LobbySettings.DEFAULT;
    /** Что лежит в файле (после load/save). */
    private static volatile LobbySettings onDisk = LobbySettings.DEFAULT;
    /**
     * Предпрогрев матч-сервера (этап 5): поднимать сервер сразу после выбора карты. Отдельный ключ файла
     * (не поле LobbySettings — тот уходит пакетом админ-панели), по умолчанию ВЫКЛ до проверки в игре.
     */
    private static volatile boolean prewarm = false;

    private static volatile boolean prewarmOnDisk = false;

    public static boolean isPrewarm() {
        return prewarm;
    }

    public static void setPrewarm(boolean v, boolean persist) {
        prewarm = v;
        if (persist) save();
    }

    private ServerConfig() {}

    public static LobbySettings get() {
        return settings;
    }

    public static boolean isAutoStartEnabled() {
        return settings.autostart();
    }

    /** Команда {@code /pwp server autostart}: persist — сохранить на диск (op 4 = OWNER). */
    public static void setAutoStartEnabled(boolean v, boolean persist) {
        apply(settings.withAutostart(v), persist);
    }

    public static int getMaxMatches() {
        return settings.maxMatches();
    }

    public static void setMaxMatches(int n, boolean persist) {
        apply(settings.withMaxMatches(n), persist);
    }

    /** Текущие значения совпадают с файлом (нет изменений «до рестарта»). */
    public static boolean isPersisted() {
        return settings.equals(onDisk) && prewarm == prewarmOnDisk;
    }

    public static int getMinPlayers() {
        return settings.minPlayers();
    }

    /**
     * Применить (в т.ч. в MatchAllocator); {@code persist} — ещё и записать на диск (иначе до рестарта
     * лобби). false — запись не удалась (значения при этом применены).
     */
    public static boolean apply(LobbySettings next, boolean persist) {
        settings = next;
        MatchAllocator.configure(next.minPlayers());
        return !persist || save();
    }

    /** Чтение при старте лобби (файл создаёт {@link com.pwp.api.ServerRole} на выделенном сервере). */
    public static void load() {
        ServerSettings s = ServerRole.settings();
        LobbySettings loaded = s.lobby();
        prewarm = s.prewarm();
        settings = loaded;
        onDisk = loaded;
        prewarmOnDisk = prewarm;
        MatchAllocator.configure(loaded.minPlayers());
        LOG.info(
                "Настройки лобби: autostart={}, maxMatches={}, minPlayers={}, prewarm={}",
                loaded.autostart(),
                loaded.maxMatches(),
                loaded.minPlayers(),
                prewarm);
    }

    private static boolean save() {
        ServerSettings next = ServerRole.settings().withLobby(settings).withPrewarm(prewarm);
        if (!ServerRole.file().save(next, LOG::warn)) return false;
        onDisk = settings;
        prewarmOnDisk = prewarm;
        return true;
    }
}
