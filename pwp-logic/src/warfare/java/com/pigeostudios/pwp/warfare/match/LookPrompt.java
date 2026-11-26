package com.pigeostudios.pwp.warfare.match;

/**
 * Подсказка при наведении, как в Squad (этап 17i): смотришь на объект — у прицела «что сделать и какой
 * клавишей». Правило отражает то, что реально делает код (ПКМ пустой рукой у ФОБа — снабжение, удержание ПКМ
 * лопатой — стройка/разборка/подкоп, медик с аптечкой — подъём и т.д.); клиент только показывает.
 *
 * <p>{@link Prompt#action}/{@link Prompt#altAction} — суффиксы ключей lang {@code gui.pwpwarfare.prompt.*};
 * {@link Key} — какую клавишу подписать (имя берётся из текущей раскладки).
 */
public final class LookPrompt {
    public enum Target {
        NONE,
        OWN_VEHICLE,
        OWN_FOB,
        ENEMY_FOB,
        OWN_BLUEPRINT,
        OWN_STRUCTURE,
        ENEMY_STRUCTURE,
        SUPPLY_CRATE,
        AMMO_BAG,
        DOWNED_ALLY
    }

    public enum Key {
        USE,
        HOLD_USE,
        SHIFT_USE,
        VEHICLE_MENU,
        TACTICAL
    }

    /**
     * @param shovel в руке лопата
     * @param emptyHand главная рука пуста
     * @param medic кит медика
     * @param reviveItem в руке предмет оживления
     */
    public record Context(boolean shovel, boolean emptyHand, boolean medic, boolean reviveItem) {}

    /** {@code key == null} — только текст (чего не хватает); {@code altAction == null} — второго действия нет. */
    public record Prompt(String action, Key key, String altAction, Key altKey) {
        static Prompt of(String action, Key key) {
            return new Prompt(action, key, null, null);
        }

        static Prompt info(String action) {
            return new Prompt(action, null, null, null);
        }
    }

    private LookPrompt() {}

    /** null — подсказки нет. */
    public static Prompt of(Target t, Context c) {
        return switch (t) {
            case OWN_VEHICLE -> new Prompt("enter", Key.USE, "vehicle_actions", Key.VEHICLE_MENU);
            case OWN_FOB -> c.shovel()
                    ? Prompt.of("dismantle", Key.HOLD_USE)
                    : c.emptyHand() ? Prompt.of("fob_supply", Key.USE) : Prompt.info("free_hand");
            case ENEMY_FOB -> c.shovel() ? Prompt.of("dig_fob", Key.HOLD_USE) : Prompt.info("need_shovel_dig");
            case OWN_BLUEPRINT -> c.shovel() ? Prompt.of("build", Key.HOLD_USE) : Prompt.info("need_shovel_build");
            case OWN_STRUCTURE -> c.shovel() ? Prompt.of("dismantle", Key.HOLD_USE) : null;
            case ENEMY_STRUCTURE -> c.shovel() ? Prompt.of("dig", Key.HOLD_USE) : null;
            case SUPPLY_CRATE -> Prompt.of("crate_supply", Key.USE);
            case AMMO_BAG -> new Prompt("take_ammo", Key.USE, "pick_up", Key.SHIFT_USE);
            case DOWNED_ALLY -> c.medic()
                    ? (c.reviveItem() ? Prompt.of("revive", Key.HOLD_USE) : Prompt.info("take_medkit"))
                    : new Prompt("downed", null, "call_medic", Key.TACTICAL);
            case NONE -> null;
        };
    }
}
