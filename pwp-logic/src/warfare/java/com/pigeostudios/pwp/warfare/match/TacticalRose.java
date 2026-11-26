package com.pigeostudios.pwp.warfare.match;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Тактическая радиалка — «коммо-роза» как в Squad (этап 17d): одна клавиша, пункты по роли и ситуации.
 * Клиент строит меню по этому правилу; сервер всё равно проверяет каждое действие сам.
 *
 * <ul>
 *   <li>любой боец команды: {@link Item#SPOT} — «враг здесь» (метка команде), {@link Item#REQUESTS} — медик,
 *       патроны, транспорт (сообщение отряду);
 *   <li>лидер отряда или фаертима: {@link Item#ORDERS} — приказы отряду (двигаться/атаковать/оборонять/строить);
 *   <li>лидер отряда: {@link Item#BUILD} — ралли, ФОБ и стройка по разделам (радиалка рации);
 *   <li>рядом своя техника: {@link Item#VEHICLE} — действия с техникой;
 *   <li>командир: {@link Item#COMMANDER} — разведка, сброс, авиаудар.
 * </ul>
 */
public final class TacticalRose {
    public enum Item {
        SPOT,
        REQUESTS,
        ORDERS,
        BUILD,
        VEHICLE,
        COMMANDER;

        public String id() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    /** Запросы отряду. */
    public enum Request {
        MEDIC,
        AMMO,
        RIDE;

        public String id() {
            return name().toLowerCase(Locale.ROOT);
        }

        public static Request parse(String s) {
            if (s == null) return null;
            try {
                return valueOf(s.trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException e) {
                return null;
            }
        }
    }

    /** Кулдаун на метку врага и на каждый запрос, тиков (против спама). */
    public static final int SPOT_COOLDOWN_TICKS = 20 * 8;

    public static final int REQUEST_COOLDOWN_TICKS = 20 * 20;
    /** Дальность метки врага, блоков (как дальность пинга лидера). */
    public static final int SPOT_RANGE = 300;

    public record Context(
            boolean inTeam,
            boolean inSquad,
            boolean squadLeader,
            boolean fireteamLeader,
            boolean commander,
            boolean vehicleNearby) {}

    private TacticalRose() {}

    public static List<Item> items(Context c) {
        List<Item> out = new ArrayList<>();
        if (!c.inTeam()) return out;
        out.add(Item.SPOT);
        if (c.inSquad()) out.add(Item.REQUESTS);
        if (c.squadLeader() || c.fireteamLeader()) out.add(Item.ORDERS);
        if (c.squadLeader()) out.add(Item.BUILD);
        if (c.vehicleNearby()) out.add(Item.VEHICLE);
        if (c.commander()) out.add(Item.COMMANDER);
        return out;
    }
}
