package com.pwp.coreclient.controls;

import java.util.List;

/**
 * Категория экрана со строками в порядке белого списка (основные, затем «Дополнительно» —
 * порядок задаёт сам файл).
 */
public record ControlsGroup(ControlsCatalog.Category category, String title, List<ControlsRow> rows) {
    public ControlsGroup {
        rows = List.copyOf(rows);
    }

    /** Основные строки. */
    public List<ControlsRow> basic() {
        return rows.stream().filter(r -> !r.advanced()).toList();
    }

    /** Строки под «Дополнительно». */
    public List<ControlsRow> advanced() {
        return rows.stream().filter(ControlsRow::advanced).toList();
    }
}
