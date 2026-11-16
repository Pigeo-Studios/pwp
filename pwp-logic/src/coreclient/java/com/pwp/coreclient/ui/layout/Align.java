package com.pwp.coreclient.ui.layout;

/** Выравнивание по одной оси. STRETCH — растянуть на всю ось. */
public enum Align {
    START,
    CENTER,
    END,
    STRETCH;

    /** Смещение блока размером {@code size} внутри оси длиной {@code space}. */
    public int offset(int space, int size) {
        int free = Math.max(0, space - size);
        return switch (this) {
            case START, STRETCH -> 0;
            case CENTER -> free / 2;
            case END -> free;
        };
    }
}
