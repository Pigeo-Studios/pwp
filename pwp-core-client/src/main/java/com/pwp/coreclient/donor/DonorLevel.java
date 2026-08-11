package com.pwp.coreclient.donor;

import com.pwp.coreclient.CoreClientMod;
import com.pwp.coreclient.particles.DonorFxConfig;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.config.ModConfigEvent;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Единая модель донат-уровня (тиры + роли): один источник цвета для частиц (glow),
 * градиентов ников (stops) и ореолов. Роль приоритетнее тира (ADMIN > MODERATOR > тир).
 * Цвета по умолчанию — здесь; переопределяются строками RRGGBB в клиентском
 * donor_fx.toml (секция colors), кэш сбрасывается на событии перезагрузки конфига.
 */
public enum DonorLevel {

    SILVER("SILVER", new int[]{0xFF8E9AA6, 0xFFF3F6F9}, 0xFF9FB4C8, 0),
    GOLD("GOLD", new int[]{0xFFB8860B, 0xFFFFF3B0}, 0xFFFFC94D, 1),
    PLATINUM("PLATINUM", new int[]{0xFF6FB6D6, 0xFFF6FBFF}, 0xFFA8E4FF, 2),
    MODERATOR("MODERATOR", new int[]{0xFF2A5B8F, 0xFF9FD0FF}, 0xFF4A9BFF, 3),
    ADMIN("ADMIN", new int[]{0xFFD50000, 0xFFFF4D4D, 0xFFFFD980}, 0xFFFF3030, 4);

    private final String name;
    private final int[] defaultStops;
    private final int defaultGlow;
    private final int priority;

    private static final Map<String, int[]> STOPS_CACHE = new ConcurrentHashMap<>();
    private static final Map<String, Integer> GLOW_CACHE = new ConcurrentHashMap<>();

    DonorLevel(String name, int[] defaultStops, int defaultGlow, int priority) {
        this.name = name;
        this.defaultStops = defaultStops;
        this.defaultGlow = defaultGlow;
        this.priority = priority;
    }

    /** Ключ уровня (display-строка). */
    public String key() {
        return name;
    }

    public int priority() {
        return priority;
    }

    /** Стопы градиента ника (2-3 цвета ARGB) — с учётом оверрайда конфига. */
    public int[] stops() {
        return STOPS_CACHE.computeIfAbsent(name, n -> {
            int[] parsed = parseHexList(DonorFxConfig.stopsCfg(this));
            return parsed != null ? parsed : defaultStops;
        });
    }

    /** Цвет свечения: частицы + ореол ника — с учётом оверрайда конфига. */
    public int glowColor() {
        return GLOW_CACHE.computeIfAbsent(name, n -> {
            int parsed = parseHex(DonorFxConfig.glowCfg(this));
            return parsed >= 0 ? parsed : defaultGlow;
        });
    }

    /** glow как float RGB 0..1 для setColor частиц. */
    public float[] rgb() {
        int c = glowColor();
        return new float[]{(c >> 16 & 0xFF) / 255f, (c >> 8 & 0xFF) / 255f, (c & 0xFF) / 255f};
    }

    public static void clearCache() {
        STOPS_CACHE.clear();
        GLOW_CACHE.clear();
    }

    /** Сброс кэша цветов при (пере)загрузке клиентского конфига — оверрайды применяются на лету. */
    public static void onModConfig(ModConfigEvent event) {
        ModConfig cfg = event.getConfig();
        if (cfg != null && cfg.getType() == ModConfig.Type.CLIENT && CoreClientMod.MODID.equals(cfg.getModId())) {
            clearCache();
        }
    }

    // ====== Нормализация ролей / резолв уровня (единый источник) ======

    /** Нормализация роли из core-service (admin/owner/support/moderator) в display-уровень или null. */
    public static String normalizeRole(String role) {
        if (role == null) return null;
        return switch (role.trim().toLowerCase()) {
            case "admin", "owner" -> "ADMIN";
            case "support", "moderator" -> "MODERATOR";
            default -> null;
        };
    }

    /** Итоговый display-уровень (строка) для отображения: роль приоритетнее тира. */
    public static String resolve(String role, String tier) {
        String r = normalizeRole(role);
        if (r != null) return r;
        if (tier == null) return null;
        String t = tier.toUpperCase();
        return isSupported(t) ? t : null;
    }

    public static boolean isSupported(String level) {
        return byName(level) != null;
    }

    /** Уровень по display-строке (SILVER/GOLD/PLATINUM/MODERATOR/ADMIN) или null. */
    public static DonorLevel byName(String level) {
        if (level == null) return null;
        return switch (level.toUpperCase()) {
            case "SILVER" -> SILVER;
            case "GOLD" -> GOLD;
            case "PLATINUM" -> PLATINUM;
            case "MODERATOR" -> MODERATOR;
            case "ADMIN" -> ADMIN;
            default -> null;
        };
    }

    // ====== Парсинг hex ======

    /** Парсинг "RRGGBB[,RRGGBB...]" (2-3 стопа) → ARGB-массив, или null при невалидных данных. */
    private static int[] parseHexList(String cfg) {
        if (cfg == null || cfg.isBlank()) return null;
        String[] parts = cfg.split(",");
        if (parts.length < 2 || parts.length > 3) return null;
        int[] out = new int[parts.length];
        for (int i = 0; i < parts.length; i++) {
            int c = parseHex(parts[i]);
            if (c < 0) return null;
            out[i] = c;
        }
        return out;
    }

    /** Парсинг одного "RRGGBB" (с # или без) → ARGB или -1 при невалидном. */
    private static int parseHex(String s) {
        if (s == null) return -1;
        String h = s.trim();
        if (h.startsWith("#")) h = h.substring(1);
        if (h.length() != 6) return -1;
        try {
            return Integer.parseInt(h, 16) | 0xFF000000;
        } catch (NumberFormatException e) {
            return -1;
        }
    }
}
