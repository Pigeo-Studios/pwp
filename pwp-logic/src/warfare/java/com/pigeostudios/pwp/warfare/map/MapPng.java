package com.pigeostudios.pwp.warfare.map;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.zip.CRC32;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

/**
 * Минимальный PNG (RGBA 8 бит, без интерлейса) для тайлов карты: без java.desktop/ImageIO и без
 * LWJGL — работает одинаково на выделенном сервере и клиенте. Декодер понимает все 5 фильтров и
 * цветовые типы 2 (RGB) и 6 (RGBA); остальное — {@link IllegalArgumentException}.
 */
public final class MapPng {
    private MapPng() {}

    private static final byte[] SIGNATURE = {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n'};
    /** Защита декодера от огромных размеров из чужих/битых файлов. */
    public static final int MAX_SIDE = 4096;

    /** Результат декодирования: ARGB построчно. */
    public record Image(int width, int height, int[] argb) {}

    public static byte[] encode(int[] argb, int w, int h) {
        if (w <= 0 || h <= 0 || argb.length < w * h) throw new IllegalArgumentException("size");
        int stride = w * 4;
        byte[] raw = new byte[(stride + 1) * h];
        byte[] cur = new byte[stride], prev = new byte[stride];
        byte[] sub = new byte[stride], up = new byte[stride];
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int c = argb[y * w + x];
                int o = x * 4;
                cur[o] = (byte) (c >> 16);
                cur[o + 1] = (byte) (c >> 8);
                cur[o + 2] = (byte) c;
                cur[o + 3] = (byte) (c >>> 24);
            }
            long sNone = 0, sSub = 0, sUp = 0;
            for (int i = 0; i < stride; i++) {
                int left = i >= 4 ? cur[i - 4] & 0xFF : 0;
                sub[i] = (byte) ((cur[i] & 0xFF) - left);
                up[i] = (byte) ((cur[i] & 0xFF) - (prev[i] & 0xFF));
                sNone += Math.abs((int) cur[i]);
                sSub += Math.abs((int) sub[i]);
                sUp += Math.abs((int) up[i]);
            }
            int off = y * (stride + 1);
            byte[] pick;
            if (sSub <= sUp && sSub <= sNone) {
                raw[off] = 1;
                pick = sub;
            } else if (sUp <= sNone) {
                raw[off] = 2;
                pick = up;
            } else {
                raw[off] = 0;
                pick = cur;
            }
            System.arraycopy(pick, 0, raw, off + 1, stride);
            byte[] t = prev;
            prev = cur;
            cur = t;
        }
        Deflater d = new Deflater(Deflater.BEST_COMPRESSION);
        byte[] idat;
        try {
            d.setInput(raw);
            d.finish();
            ByteArrayOutputStream z = new ByteArrayOutputStream(raw.length / 4 + 64);
            byte[] buf = new byte[16384];
            while (!d.finished()) z.write(buf, 0, d.deflate(buf));
            idat = z.toByteArray();
        } finally {
            d.end();
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream(idat.length + 64);
        out.writeBytes(SIGNATURE);
        byte[] ihdr = new byte[13];
        putInt(ihdr, 0, w);
        putInt(ihdr, 4, h);
        ihdr[8] = 8; // бит на канал
        ihdr[9] = 6; // RGBA
        chunk(out, "IHDR", ihdr);
        chunk(out, "IDAT", idat);
        chunk(out, "IEND", new byte[0]);
        return out.toByteArray();
    }

    public static Image decode(byte[] png) {
        if (png == null || png.length < 8 + 25) throw new IllegalArgumentException("short");
        for (int i = 0; i < 8; i++) if (png[i] != SIGNATURE[i]) throw new IllegalArgumentException("signature");
        int p = 8, w = 0, h = 0, type = -1;
        ByteArrayOutputStream z = new ByteArrayOutputStream();
        while (p + 8 <= png.length) {
            int len = getInt(png, p);
            String name = new String(png, p + 4, 4, StandardCharsets.US_ASCII);
            int data = p + 8;
            if (len < 0 || data + len + 4 > png.length) throw new IllegalArgumentException("chunk");
            if (name.equals("IHDR")) {
                w = getInt(png, data);
                h = getInt(png, data + 4);
                if (png[data + 8] != 8 || png[data + 12] != 0) throw new IllegalArgumentException("format");
                type = png[data + 9];
            } else if (name.equals("IDAT")) {
                z.write(png, data, len);
            } else if (name.equals("IEND")) {
                break;
            }
            p = data + len + 4;
        }
        if (w <= 0 || h <= 0 || w > MAX_SIDE || h > MAX_SIDE) throw new IllegalArgumentException("size");
        int bpp;
        if (type == 6) bpp = 4;
        else if (type == 2) bpp = 3;
        else throw new IllegalArgumentException("type " + type);
        int stride = w * bpp;
        byte[] raw = new byte[(stride + 1) * h];
        Inflater inf = new Inflater();
        try {
            inf.setInput(z.toByteArray());
            int n = 0;
            while (n < raw.length) {
                int r = inf.inflate(raw, n, raw.length - n);
                if (r == 0 && (inf.finished() || inf.needsInput() || inf.needsDictionary())) break;
                n += r;
            }
            if (n < raw.length) throw new IllegalArgumentException("truncated");
        } catch (DataFormatException e) {
            throw new IllegalArgumentException("zlib", e);
        } finally {
            inf.end();
        }
        byte[] prev = new byte[stride], cur = new byte[stride];
        int[] out = new int[w * h];
        for (int y = 0; y < h; y++) {
            int off = y * (stride + 1);
            int f = raw[off];
            for (int i = 0; i < stride; i++) {
                int x = raw[off + 1 + i] & 0xFF;
                int a = i >= bpp ? cur[i - bpp] & 0xFF : 0;
                int b = prev[i] & 0xFF;
                int c = i >= bpp ? prev[i - bpp] & 0xFF : 0;
                int v =
                        switch (f) {
                            case 0 -> x;
                            case 1 -> x + a;
                            case 2 -> x + b;
                            case 3 -> x + ((a + b) >> 1);
                            case 4 -> x + paeth(a, b, c);
                            default -> throw new IllegalArgumentException("filter " + f);
                        };
                cur[i] = (byte) v;
            }
            for (int x = 0; x < w; x++) {
                int o = x * bpp;
                int alpha = bpp == 4 ? cur[o + 3] & 0xFF : 0xFF;
                out[y * w + x] =
                        (alpha << 24) | ((cur[o] & 0xFF) << 16) | ((cur[o + 1] & 0xFF) << 8) | (cur[o + 2] & 0xFF);
            }
            byte[] t = prev;
            prev = cur;
            cur = t;
        }
        return new Image(w, h, out);
    }

    /** Короткий хэш содержимого (16 hex-символов SHA-256) — версия тайла для кэша. */
    public static String hash(byte[] data) {
        try {
            byte[] d = MessageDigest.getInstance("SHA-256").digest(data);
            StringBuilder sb = new StringBuilder(16);
            for (int i = 0; i < 8; i++) sb.append(String.format("%02x", d[i] & 0xFF));
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 недоступен", e);
        }
    }

    private static int paeth(int a, int b, int c) {
        int p = a + b - c;
        int pa = Math.abs(p - a), pb = Math.abs(p - b), pc = Math.abs(p - c);
        if (pa <= pb && pa <= pc) return a;
        return pb <= pc ? b : c;
    }

    private static void chunk(ByteArrayOutputStream out, String name, byte[] data) {
        byte[] len = new byte[4];
        putInt(len, 0, data.length);
        out.writeBytes(len);
        byte[] n = name.getBytes(StandardCharsets.US_ASCII);
        out.writeBytes(n);
        out.writeBytes(data);
        CRC32 crc = new CRC32();
        crc.update(n);
        crc.update(data);
        byte[] c = new byte[4];
        putInt(c, 0, (int) crc.getValue());
        out.writeBytes(c);
    }

    private static void putInt(byte[] b, int o, int v) {
        b[o] = (byte) (v >>> 24);
        b[o + 1] = (byte) (v >>> 16);
        b[o + 2] = (byte) (v >>> 8);
        b[o + 3] = (byte) v;
    }

    private static int getInt(byte[] b, int o) {
        return ((b[o] & 0xFF) << 24) | ((b[o + 1] & 0xFF) << 16) | ((b[o + 2] & 0xFF) << 8) | (b[o + 3] & 0xFF);
    }
}
