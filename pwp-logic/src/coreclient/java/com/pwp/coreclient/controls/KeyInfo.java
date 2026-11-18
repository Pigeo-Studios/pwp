package com.pwp.coreclient.controls;

/**
 * Одна клавиша сборки, как её видит экран (снимок {@code KeyMapping} без Minecraft). Сюда попадают
 * ВСЕ клавиши сборки — и из белого списка, и вне его (для поиска конфликтов).
 *
 * @param id имя {@code KeyMapping} ({@code key.forward})
 * @param label переведённое название
 * @param mod отображаемое имя мода-владельца
 * @param keyLabel переведённое имя назначенной клавиши («W», «ЛКМ», «Ctrl + R»)
 * @param isDefault назначение совпадает с дефолтом
 * @param key абстрактное назначение
 * @param context группа контекста Forge ({@link ControlsConflicts#IN_GAME}/{@code GUI}/{@code
 *     UNIVERSAL} или любая своя строка — пересечение решает {@link ControlsConflicts.ContextOverlap})
 */
public record KeyInfo(
        String id, String label, String mod, String keyLabel, boolean isDefault, KeyInput key, String context) {
    public KeyInfo {
        if (id == null) throw new IllegalArgumentException("id");
        if (label == null) label = id;
        if (mod == null) mod = "";
        if (keyLabel == null) keyLabel = "";
        if (key == null) key = KeyInput.UNBOUND;
        if (context == null) context = ControlsConflicts.IN_GAME;
    }
}
