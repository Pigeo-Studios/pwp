package com.pwp.api.config;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Файл {@link ServerSettings}: {@code <configDir>/pwp-server.json}. Нет файла — миграция из старого
 * {@code pwp_lobby.json} (если есть; сам старый файл не трогаем) или дефолт, и файл создаётся — админ сразу
 * видит все ключи. Битый файл не перезаписывается (правка админа не теряется), берутся дефолты.
 */
public final class ServerSettingsFile {
    public static final String FILE_NAME = "pwp-server.json";
    public static final String LEGACY_LOBBY = "pwp_lobby.json";

    private final Path file;
    private final Path legacy;
    private JsonObject raw;
    private ServerSettings current = ServerSettings.DEFAULT;

    public ServerSettingsFile(Path configDir) {
        this.file = configDir.resolve(FILE_NAME);
        this.legacy = configDir.resolve(LEGACY_LOBBY);
    }

    public Path path() {
        return file;
    }

    public ServerSettings get() {
        return current;
    }

    /**
     * Загрузить. {@code log} получает замечания (битые поля, миграция). {@code create} — создать файл, если
     * его нет (на выделенном сервере; клиенту файл не нужен).
     */
    public ServerSettings load(boolean create, Consumer<String> log) {
        List<String> warnings = new ArrayList<>();
        if (Files.isRegularFile(file)) {
            try {
                raw = JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8))
                        .getAsJsonObject();
                current = ServerSettings.fromJson(raw, warnings);
                // файл старше этапа 10: дописать секцию community, чтобы админ видел ключи
                if (create && !raw.has("community")) save(current, log);
            } catch (IOException | JsonParseException | IllegalStateException e) {
                log.accept(file + " не прочитан (" + e.getMessage() + ") — настройки по умолчанию, файл не трогаю");
                current = ServerSettings.DEFAULT;
                return current;
            }
        } else {
            if (Files.isRegularFile(legacy)) {
                try {
                    JsonObject old = JsonParser.parseString(Files.readString(legacy, StandardCharsets.UTF_8))
                            .getAsJsonObject();
                    current = ServerSettings.fromLegacyLobby(old, warnings);
                    log.accept("Настройки лобби перенесены из " + legacy + " в " + file
                            + " (старый файл больше не читается)");
                } catch (IOException | JsonParseException | IllegalStateException e) {
                    log.accept(legacy + " не прочитан (" + e.getMessage() + ") — настройки по умолчанию");
                    current = ServerSettings.DEFAULT;
                }
            } else {
                current = ServerSettings.DEFAULT;
            }
            if (create) save(current, log);
        }
        for (String w : warnings) log.accept(file + ": " + w + " — взят дефолт");
        return current;
    }

    /** Атомарная запись (tmp + move); неизвестные ключи файла сохраняются. false — не удалось. */
    public boolean save(ServerSettings next, Consumer<String> log) {
        try {
            Files.createDirectories(file.toAbsolutePath().getParent());
            JsonObject o = next.toJson(raw);
            Path tmp = file.resolveSibling(FILE_NAME + ".tmp");
            Files.writeString(
                    tmp, new GsonBuilder().setPrettyPrinting().create().toJson(o), StandardCharsets.UTF_8);
            Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING);
            raw = o;
            current = next;
            return true;
        } catch (IOException e) {
            log.accept("Не удалось записать " + file + ": " + e.getMessage());
            return false;
        }
    }
}
