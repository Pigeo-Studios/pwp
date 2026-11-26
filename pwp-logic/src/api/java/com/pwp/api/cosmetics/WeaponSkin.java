package com.pwp.api.cosmetics;

import java.util.Map;
import java.util.regex.Pattern;

/**
 * Скин ствола (этап 11 ROADMAP) — меняет ТОЛЬКО внешний вид, характеристики остаются: TaCZ рисует ствол по
 * NBT {@code GunDisplayId}, а стреляет по {@code GunId}. Поэтому скин = «для оружия X показывать display Y».
 *
 * <p>Определение в каталоге core ({@code skin_definitions}) без новых колонок: {@code slotType = WEAPON},
 * {@code weaponTag} = GunId ствола ({@code cib:ak74}), {@code modelPath = display:<id display>}
 * ({@code display:cib:ak74_gold_display}). Старые скины (modelPath = предмет/SNBT, предмет-замена в слоте
 * кита) по-прежнему работают как раньше — это {@link #parse} == null.
 *
 * <p>Надетый скин хранится в core как экипировка {@code (slotType=WEAPON, role=GunId)} — один скин на ствол,
 * на всех ролях и китах сразу, без настройки китов админом.
 */
public record WeaponSkin(String skinId, String gunId, String displayId) {
    public static final String SLOT = "WEAPON";
    public static final String DISPLAY_PREFIX = "display:";
    /** Метка «display поставил PWP» — чтобы снимать только свой, а не родной display пака. */
    public static final String MARK_TAG = "PwpSkin";

    public static final String DISPLAY_TAG = "GunDisplayId";
    public static final int MAX_ID = 96;
    private static final Pattern RL = Pattern.compile("[a-z0-9_.-]+:[a-z0-9_./-]+");

    /** Скин ствола из определения каталога или null (это не скин ствола / битые поля). */
    public static WeaponSkin parse(String skinId, String slotType, String weaponTag, String modelPath) {
        if (skinId == null || modelPath == null || !modelPath.startsWith(DISPLAY_PREFIX)) return null;
        String display = modelPath.substring(DISPLAY_PREFIX.length()).trim();
        String gun = weaponTag == null ? "" : weaponTag.trim();
        if (!validId(gun) || !validId(display)) return null;
        return new WeaponSkin(skinId, gun, display);
    }

    public static boolean validId(String id) {
        return id != null && id.length() <= MAX_ID && RL.matcher(id).matches();
    }

    /** Строка modelPath для сохранения скина в каталог. */
    public static String modelPath(String displayId) {
        return DISPLAY_PREFIX + displayId;
    }

    /** Что сделать с NBT ствола. */
    public enum Action {
        NONE,
        SET,
        CLEAR
    }

    /**
     * Решение для одного ствола инвентаря. {@code equipped}: GunId → display надетых скинов игрока.
     * {@code currentDisplay} — текущий GunDisplayId предмета (пусто — родной), {@code marked} — его ставил PWP.
     * Родной display пака (не помеченный) не трогаем, если скина нет.
     */
    public static Action decide(Map<String, String> equipped, String gunId, String currentDisplay, boolean marked) {
        if (gunId == null || gunId.isEmpty()) return Action.NONE;
        String want = equipped == null ? null : equipped.get(gunId);
        String cur = currentDisplay == null ? "" : currentDisplay;
        if (want != null) return want.equals(cur) && marked ? Action.NONE : Action.SET;
        return marked ? Action.CLEAR : Action.NONE;
    }
}
