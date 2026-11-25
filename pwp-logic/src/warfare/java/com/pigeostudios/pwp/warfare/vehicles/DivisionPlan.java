package com.pigeostudios.pwp.warfare.vehicles;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Выбор дивизии и раздача её техники по слотам карты (этап 6 ROADMAP). Чистая логика: вызывающий даёт слоты
 * (в порядке {@code vehicleSpawners} карты), пул фракции (имя машины → роль) и дивизии фракции.
 */
public final class DivisionPlan {
    private DivisionPlan() {}

    /** Слот карты: роль или явная машина (обратная совместимость — явная машина дивизией не меняется). */
    public record Slot(String role, String vehicleName) {}

    /** Назначение слота: машина (null — слот пуст в этой дивизии), время респавна (0 — из пула фракции). */
    public record Pick(String vehicleName, int respawnSeconds) {
        public static final Pick EMPTY = new Pick(null, 0);
    }

    /**
     * Дивизия для команды: {@code requestedId} (выбор лобби/админа), если есть у фракции; иначе случайная из
     * подходящих режиму и размеру карты; нет подходящих — случайная любая; дивизий нет — null (прежний пул).
     */
    public static Division choose(
            List<Division> divisions, String requestedId, String mode, Division.MapSize size, Random rng) {
        if (divisions == null || divisions.isEmpty()) return null;
        if (requestedId != null && !requestedId.isBlank()) {
            for (Division d : divisions) if (d.id().equalsIgnoreCase(requestedId.trim())) return d;
        }
        List<Division> fitting = new ArrayList<>();
        for (Division d : divisions) if (d.fits(mode, size)) fitting.add(d);
        List<Division> from = fitting.isEmpty() ? divisions : fitting;
        return from.get(rng.nextInt(from.size()));
    }

    /**
     * Раздать машины по слотам. {@code division == null} — прежнее поведение: слоту роли — первая по алфавиту
     * машина пула с этой ролью. С дивизией: записи роли разворачиваются по {@code count} в очередь, слоты роли
     * получают их по порядку; не хватило машин — слот пуст. Машины, которых нет в пуле, пропускаются
     * (их видно в {@link #problems}). Результат — по индексу слота.
     */
    public static List<Pick> assign(List<Slot> slots, Map<String, String> poolRoleByVehicle, Division division) {
        List<Pick> out = new ArrayList<>(slots.size());
        Map<String, Deque<Pick>> queues = division == null ? null : queues(division, poolRoleByVehicle);
        for (Slot s : slots) {
            if (s.vehicleName() != null && !s.vehicleName().isBlank()) {
                out.add(poolRoleByVehicle.containsKey(s.vehicleName()) ? new Pick(s.vehicleName(), 0) : Pick.EMPTY);
                continue;
            }
            String role = Division.norm(s.role());
            if (role.isEmpty()) {
                out.add(Pick.EMPTY);
                continue;
            }
            if (queues == null) {
                out.add(legacyPick(role, poolRoleByVehicle.entrySet()));
            } else {
                Deque<Pick> q = queues.get(role);
                out.add(q == null || q.isEmpty() ? Pick.EMPTY : q.pollFirst());
            }
        }
        return out;
    }

    private static Map<String, Deque<Pick>> queues(Division d, Map<String, String> pool) {
        Map<String, Deque<Pick>> q = new HashMap<>();
        for (Division.Entry e : d.vehicles()) {
            if (!pool.containsKey(e.vehicleName())) continue;
            Deque<Pick> dq = q.computeIfAbsent(e.role(), k -> new ArrayDeque<>());
            for (int i = 0; i < e.count(); i++) dq.addLast(new Pick(e.vehicleName(), e.respawnSeconds()));
        }
        return q;
    }

    private static Pick legacyPick(String role, Collection<Map.Entry<String, String>> pool) {
        String best = null;
        for (Map.Entry<String, String> e : pool) {
            if (role.equalsIgnoreCase(e.getValue())
                    && (best == null || e.getKey().compareTo(best) < 0)) {
                best = e.getKey();
            }
        }
        return best == null ? Pick.EMPTY : new Pick(best, 0);
    }

    /**
     * Замечания для админа: машина дивизии отсутствует в пуле фракции, роль записи не совпадает с ролью машины
     * в пуле, машин роли больше, чем слотов роли на карте (лишние не появятся).
     */
    public static List<String> problems(List<Slot> slots, Map<String, String> poolRoleByVehicle, Division d) {
        List<String> out = new ArrayList<>();
        if (d == null) return out;
        Map<String, Integer> slotsByRole = new HashMap<>();
        for (Slot s : slots) {
            if (s.vehicleName() == null || s.vehicleName().isBlank()) {
                slotsByRole.merge(Division.norm(s.role()), 1, Integer::sum);
            }
        }
        Map<String, Integer> wanted = new HashMap<>();
        for (Division.Entry e : d.vehicles()) {
            String poolRole = poolRoleByVehicle.get(e.vehicleName());
            if (poolRole == null) {
                out.add("missing:" + e.vehicleName());
                continue;
            }
            if (!Division.norm(poolRole).equals(e.role())) out.add("role:" + e.vehicleName() + ":" + poolRole);
            wanted.merge(e.role(), e.count(), Integer::sum);
        }
        for (Map.Entry<String, Integer> w : wanted.entrySet()) {
            int have = slotsByRole.getOrDefault(w.getKey(), 0);
            if (w.getValue() > have) out.add("slots:" + w.getKey() + ":" + w.getValue() + ">" + have);
        }
        return out;
    }
}
