package com.pigeostudios.pwp.movement;

/**
 * Выносливость на сервере (этап 17f): клиенту не доверяем. Сервер ведёт ту же модель {@link Stamina} по реальному
 * спринту игрока; честный клиент на нуле сам гасит бег ({@code StaminaClient}), поэтому наказание — только если
 * игрок бежит при пустой выносливости дольше {@link #GRACE_TICKS} (запас на расхождение клиента и сервера).
 */
public final class StaminaGuard {
    /** Секунда на рассинхрон клиент/сервер. */
    public static final int GRACE_TICKS = 20;

    private final Stamina stamina = new Stamina();
    private int overrun;

    /** Тик игрока. true — бежит «сверх» выносливости: выключить бег и замедлить. */
    public boolean tick(boolean sprinting, boolean crouching, boolean jumped) {
        boolean allowed = stamina.canSprint();
        stamina.tick(sprinting, crouching, jumped);
        if (sprinting && !allowed) {
            overrun++;
        } else {
            overrun = 0;
        }
        return overrun > GRACE_TICKS;
    }

    public float value() {
        return stamina.value();
    }

    public void reset() {
        stamina.reset();
        overrun = 0;
    }
}
