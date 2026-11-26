package com.pigeostudios.pwp.warfare.match;

/**
 * Подсказка при наведении, как в Squad (этапы 17i–17j): ЕДИНСТВЕННОЕ место, где решается, что показать у прицела
 * при взгляде на объект — своя/вражеская техника, ФОБ (с лопатой и без), каркас, постройка, ящик, мешок с
 * патронами, главный склад, раненый союзник. Правило повторяет то, что реально делает код (ПКМ пустой рукой у
 * ФОБа — снабжение, удержание ПКМ лопатой — стройка/разборка/подкоп, Shift+ПКМ у своей техники — пополнить БК,
 * медик с предметом оживления — подъём). Клиент только рисует одну плашку.
 *
 * <p>{@link Prompt#action}/{@code alt*} — суффиксы ключей lang {@code gui.pwpwarfare.prompt.*}; {@link Key} — какую
 * клавишу подписать (имя берётся из текущей раскладки).
 */
public final class LookPrompt {
    public enum Target {
        NONE,
        OWN_VEHICLE,
        ENEMY_VEHICLE,
        OWN_FOB,
        ENEMY_FOB,
        OWN_BLUEPRINT,
        OWN_STRUCTURE,
        ENEMY_STRUCTURE,
        SUPPLY_CRATE,
        AMMO_BAG,
        MAIN_SUPPLY,
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
     * @param crewKitMissing машина специалиста, а кит игрока не подходит (нужен механик/пилот)
     */
    public record Context(
            boolean shovel, boolean emptyHand, boolean medic, boolean reviveItem, boolean crewKitMissing) {
        public Context(boolean shovel, boolean emptyHand, boolean medic, boolean reviveItem) {
            this(shovel, emptyHand, medic, reviveItem, false);
        }
    }

    /**
     * Плашка. {@code key == null} — только текст (чего не хватает). До двух дополнительных действий. {@code warn} —
     * красная (враг, не тот кит). {@code materials} — дописать «Материалы: N» объекта.
     */
    public record Prompt(
            String action,
            Key key,
            String altAction,
            Key altKey,
            String alt2Action,
            Key alt2Key,
            boolean warn,
            boolean materials) {
        static Prompt of(String action, Key key) {
            return new Prompt(action, key, null, null, null, null, false, false);
        }

        static Prompt info(String action) {
            return of(action, null);
        }

        Prompt alt(String a, Key k) {
            return altAction == null
                    ? new Prompt(action, key, a, k, null, null, warn, materials)
                    : new Prompt(action, key, altAction, altKey, a, k, warn, materials);
        }

        Prompt asWarn() {
            return new Prompt(action, key, altAction, altKey, alt2Action, alt2Key, true, materials);
        }

        Prompt withMaterials() {
            return new Prompt(action, key, altAction, altKey, alt2Action, alt2Key, warn, true);
        }
    }

    private LookPrompt() {}

    /** null — подсказки нет. */
    public static Prompt of(Target t, Context c) {
        return switch (t) {
            case OWN_VEHICLE -> c.crewKitMissing()
                    ? Prompt.info("need_crew_kit").alt("rearm", Key.SHIFT_USE).asWarn()
                    : Prompt.of("enter", Key.USE).alt("rearm", Key.SHIFT_USE).alt("vehicle_actions", Key.VEHICLE_MENU);
            case ENEMY_VEHICLE -> Prompt.info("enemy_vehicle").asWarn();
            case OWN_FOB -> (c.shovel()
                            ? Prompt.of("dismantle", Key.HOLD_USE)
                            : c.emptyHand() ? Prompt.of("fob_supply", Key.USE) : Prompt.info("free_hand"))
                    .withMaterials();
            case ENEMY_FOB -> (c.shovel() ? Prompt.of("dig_fob", Key.HOLD_USE) : Prompt.info("need_shovel_dig"))
                    .asWarn();
            case OWN_BLUEPRINT -> c.shovel() ? Prompt.of("build", Key.HOLD_USE) : Prompt.info("need_shovel_build");
            case OWN_STRUCTURE -> c.shovel() ? Prompt.of("dismantle", Key.HOLD_USE) : null;
            case ENEMY_STRUCTURE -> c.shovel() ? Prompt.of("dig", Key.HOLD_USE).asWarn() : null;
            case SUPPLY_CRATE -> Prompt.of("crate_supply", Key.USE).withMaterials();
            case AMMO_BAG -> Prompt.of("take_ammo", Key.USE).alt("pick_up", Key.SHIFT_USE);
            case MAIN_SUPPLY -> Prompt.info("main_supply");
            case DOWNED_ALLY -> c.medic()
                    ? (c.reviveItem() ? Prompt.of("revive", Key.HOLD_USE) : Prompt.info("take_medkit"))
                    : Prompt.info("downed").alt("call_medic", Key.TACTICAL);
            case NONE -> null;
        };
    }
}
