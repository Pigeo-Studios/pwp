package com.pwp.api;

import java.nio.file.Path;

/**
 * Роль выделенного сервера. Матч-серверы поднимаются лобби в папках {@code match_NN} (из {@code match_template}),
 * всё остальное — лобби. Единый признак для core-server, лобби и warfare.
 */
public final class ServerRole {
    private static final boolean MATCH = detect();

    private ServerRole() {}

    /** Сервер запущен из папки {@code match_*} (матч-сервер, поднятый лобби). */
    public static boolean isMatchServer() {
        return MATCH;
    }

    private static boolean detect() {
        Path dir = Path.of("").toAbsolutePath().getFileName();
        return dir != null && dir.toString().startsWith("match_");
    }
}
