package com.pwp.api.net;

import java.nio.charset.StandardCharsets;
import net.minecraft.network.FriendlyByteBuf;

/**
 * Запись/чтение больших строк (JSON-каталоги скинов/техники/китов) в пакетах.
 *
 * <p>{@code FriendlyByteBuf.writeUtf} ограничен 32767 символами — большой каталог не влезал и
 * загрузка экрана «висела». Здесь строка пишется массивом UTF-8 байт с явным лимитом
 * {@link #MAX_LARGE_BYTES} (ниже лимита S→C пакета Forge ~1 МиБ).
 */
public final class Payloads {
    /** Лимит большой строки S→C, байт. */
    public static final int MAX_LARGE_BYTES = 1_000_000;
    /** Лимит строк C→S (параметры запросов), символов. */
    public static final int MAX_REQUEST_CHARS = 2048;

    private Payloads() {}

    /** Пишет большую строку; если она больше лимита — бросает исключение (на сервере, до отправки). */
    public static void writeLarge(FriendlyByteBuf buf, String value) {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        if (bytes.length > MAX_LARGE_BYTES) {
            throw new IllegalArgumentException("Строка " + bytes.length + " байт больше лимита " + MAX_LARGE_BYTES);
        }
        buf.writeByteArray(bytes);
    }

    public static String readLarge(FriendlyByteBuf buf) {
        return new String(buf.readByteArray(MAX_LARGE_BYTES), StandardCharsets.UTF_8);
    }

    /** true — строка влезает в {@link #writeLarge} (для проверки до отправки). */
    public static boolean fits(String value) {
        return value.getBytes(StandardCharsets.UTF_8).length <= MAX_LARGE_BYTES;
    }
}
