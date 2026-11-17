package com.pigeostudios.pwp.warfare.map;

import java.util.ArrayList;
import java.util.List;

/**
 * Параметр запроса тайлов {@code "x,z,hash;x,z,hash"} (DataRequest "mapTile"): компактно, чтобы
 * уложиться в лимит параметров запроса. {@code hash} — версия тайла в кэше клиента (пусто — нет).
 * Разбор — клиентский ввод: битые записи пропускаются, координаты и длина ограничены.
 */
public final class MapTileQuery {
    private MapTileQuery() {}

    /** Тайлов в одном запросе. */
    public static final int MAX_TILES = 6;
    /** |tx|, |tz| — с запасом больше границы мира (30M блоков / 256). */
    public static final int MAX_COORD = 1 << 18;

    public record Ref(int x, int z, String hash) {}

    public static String format(List<Ref> refs) {
        StringBuilder sb = new StringBuilder();
        for (Ref r : refs) {
            if (sb.length() > 0) sb.append(';');
            sb.append(r.x()).append(',').append(r.z()).append(',').append(r.hash() == null ? "" : r.hash());
        }
        return sb.toString();
    }

    public static List<Ref> parse(String s) {
        List<Ref> out = new ArrayList<>();
        if (s == null || s.isEmpty()) return out;
        for (String part : s.split(";", MAX_TILES + 1)) {
            if (out.size() >= MAX_TILES) break;
            String[] f = part.split(",", -1);
            if (f.length < 2 || f.length > 3) continue;
            Integer x = coord(f[0]), z = coord(f[1]);
            if (x == null || z == null) continue;
            String h = f.length == 3 ? f[2] : "";
            if (!isHash(h)) h = "";
            boolean dup = false;
            for (Ref r : out) dup |= r.x() == x && r.z() == z;
            if (!dup) out.add(new Ref(x, z, h));
        }
        return out;
    }

    static boolean isHash(String h) {
        if (h.length() > 16) return false;
        for (int i = 0; i < h.length(); i++) {
            char c = h.charAt(i);
            if (!((c >= '0' && c <= '9') || (c >= 'a' && c <= 'f'))) return false;
        }
        return true;
    }

    private static Integer coord(String s) {
        if (s.isEmpty() || s.length() > 8) return null;
        int i = 0, sign = 1, v = 0;
        if (s.charAt(0) == '-') {
            sign = -1;
            i = 1;
            if (s.length() == 1) return null;
        }
        for (; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c < '0' || c > '9') return null;
            v = v * 10 + (c - '0');
        }
        v *= sign;
        return Math.abs(v) > MAX_COORD ? null : v;
    }
}
