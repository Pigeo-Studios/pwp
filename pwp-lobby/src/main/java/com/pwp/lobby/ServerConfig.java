package com.pwp.lobby;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.pwp.api.config.LobbySettings;
import com.pwp.lobby.match.MatchAllocator;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Настройки лобби-сервера (автостарт, лимит матчей, минимум игроков). Раньше жили только в памяти и
 * сбрасывались при рестарте; теперь — {@code config/pwp_lobby.json} (относительно папки сервера),
 * загрузка в {@link #load()} при старте лобби. ADMIN меняет значения до рестарта, запись в файл
 * («навсегда») — только OWNER (op 4 или роль owner): кнопка панели и команды от op 4.
 */
public final class ServerConfig {
    private static final Logger LOG = LoggerFactory.getLogger(ServerConfig.class);
    private static final Path FILE = Paths.get("config", "pwp_lobby.json");

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

    /** Чтение файла при старте лобби; нет файла — дефолт и создание файла. */
    public static void load() {
        LobbySettings loaded = LobbySettings.DEFAULT;
        if (Files.isRegularFile(FILE)) {
            try {
                JsonObject o = JsonParser.parseString(Files.readString(FILE, StandardCharsets.UTF_8))
                        .getAsJsonObject();
                List<String> warnings = new ArrayList<>();
                loaded = LobbySettings.fromJson(o, warnings);
                if (o.has("prewarm")) {
                    if (o.get("prewarm").isJsonPrimitive()
                            && o.getAsJsonPrimitive("prewarm").isBoolean()) {
                        prewarm = o.get("prewarm").getAsBoolean();
                    } else {
                        warnings.add("prewarm: expected true/false, got " + o.get("prewarm"));
                    }
                }
                for (String w : warnings) LOG.warn("{}: {} — взят дефолт", FILE, w);
            } catch (IOException | JsonParseException | IllegalStateException e) {
                LOG.warn("{} не прочитан — настройки лобби по умолчанию", FILE, e);
            }
        }
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
        if (!Files.isRegularFile(FILE)) save();
    }

    private static boolean save() {
        try {
            Files.createDirectories(FILE.toAbsolutePath().getParent());
            Path tmp = FILE.resolveSibling(FILE.getFileName() + ".tmp");
            JsonObject o = settings.toJson();
            o.addProperty("prewarm", prewarm);
            Files.writeString(tmp, o.toString(), StandardCharsets.UTF_8);
            Files.move(tmp, FILE, StandardCopyOption.REPLACE_EXISTING);
            onDisk = settings;
            prewarmOnDisk = prewarm;
            return true;
        } catch (IOException e) {
            LOG.warn("Не удалось записать {}", FILE, e);
            return false;
        }
    }
}
