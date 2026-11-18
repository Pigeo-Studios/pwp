package com.pwp.coreclient.controls;

/**
 * Строка экрана «Управление».
 *
 * @param id имя {@code KeyMapping}
 * @param label переведённое название
 * @param mod имя мода-владельца
 * @param category id категории белого списка
 * @param categoryLabel переведённое название категории (участвует в поиске)
 * @param keyLabel имя назначенной клавиши
 * @param isDefault назначение совпадает с дефолтом
 * @param advanced строка под «Дополнительно»
 * @param hintKey ключ lang подсказки или {@code null}
 */
public record ControlsRow(
        String id,
        String label,
        String mod,
        String category,
        String categoryLabel,
        String keyLabel,
        boolean isDefault,
        boolean advanced,
        String hintKey) {}
