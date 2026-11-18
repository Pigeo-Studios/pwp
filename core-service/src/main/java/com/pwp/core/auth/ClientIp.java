package com.pwp.core.auth;

import io.javalin.http.Context;

/**
 * Настоящий IP клиента. Core слушает только 127.0.0.1, снаружи все ходят через Caddy, и
 * {@code ctx.ip()} всегда был 127.0.0.1: лимиты логина/API были общими на всех, IP-баны и
 * trusted_ips не работали. Caddy кладёт адрес клиента в {@code X-Forwarded-For} — доверяем
 * заголовку ТОЛЬКО если запрос пришёл с loopback (то есть от Caddy или локального сервиса);
 * берём правый элемент — его дописал наш прокси, левые мог подставить сам клиент.
 */
public final class ClientIp {
    private ClientIp() {}

    public static String of(Context ctx) {
        String direct = ctx.ip();
        if (!isLoopback(direct)) return direct;
        String xff = ctx.header("X-Forwarded-For");
        if (xff == null || xff.isBlank()) return direct;
        String[] parts = xff.split(",");
        String last = parts[parts.length - 1].trim();
        return last.isEmpty() || last.length() > 64 ? direct : last;
    }

    /** true, если запрос пришёл напрямую с этой машины, а не через прокси (игровые серверы, боты). */
    public static boolean isLocalDirect(Context ctx) {
        return isLoopback(ctx.ip()) && (ctx.header("X-Forwarded-For") == null);
    }

    static boolean isLoopback(String ip) {
        if (ip == null) return false;
        return ip.equals("127.0.0.1")
                || ip.startsWith("127.")
                || ip.equals("::1")
                || ip.equals("0:0:0:0:0:0:0:1")
                || ip.equals("[::1]");
    }
}
