package com.pigeostudios.sbwchunkload.classifier;

import com.atsuishio.superbwarfare.entity.projectile.FastThrowableProjectile;
import com.atsuishio.superbwarfare.entity.projectile.ProjectileEntity;
import com.atsuishio.superbwarfare.entity.projectile.TaserBulletEntity;
import com.vicmatskiv.pointblank.entity.ProjectileLike;
import net.minecraft.world.entity.Entity;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Классификация снарядов по КЛАССУ, а не по каждой сущности.
 *
 * Первый спавн класса — короткая instanceof-цепочка (базовые классы SBW и
 * интерфейс pointblank). Результат фиксируется за классом в кэше — дальше
 * ни одного instanceof. Любой будущий аддон, наследующий базовые классы
 * SBW (FastThrowableProjectile/ProjectileEntity/TaserBulletEntity) или
 * реализующий pointblank ProjectileLike, подхватывается автоматически:
 * никакой явной поддержки VVP/FCP/DragonRise/прочих здесь нет.
 *
 * Пули и тазеры жёстко FAST_LINEAR; остальные наследуемые — по скорости
 * первого замера: медленные (< 1.5 бл/тик) — BALLISTIC (бомбы/мины),
 * до 4 бл/тик — SLOW_LINEAR (ракеты/ПТУРы/НУРС), быстрее — HEAVY
 * (снаряды автопушек). Класс, не прошедший ни одной ветки — STATIC,
 * не трекается вовсе.
 */
public final class ProjectileClassifier {

    /** Кэш: класс снаряда -> профиль. Вычисляется один раз на класс. */
    private static final ConcurrentHashMap<Class<?>, ProjectileProfile> CACHE = new ConcurrentHashMap<>();

    private ProjectileClassifier() {
    }

    /**
     * Определяет профиль снаряда. Сначала кэш, при промахе — instanceof.
     * Возвращает STATIC для неподдерживаемых сущностей (не трекаются).
     */
    public static ProjectileProfile classify(Entity entity) {
        if (entity == null) return ProjectileProfile.STATIC;
        return CACHE.computeIfAbsent(entity.getClass(), cls -> resolve(entity));
    }

    private static ProjectileProfile resolve(Entity entity) {
        // Пули SBW и тазеры — быстрые линейные, интервал 1 (иначе вылетают
        // за пределы прогретого коридора за 1 тик).
        if (entity instanceof ProjectileEntity || entity instanceof TaserBulletEntity) {
            return ProjectileProfile.FAST_LINEAR;
        }
        // pointblank/FCL-трубы: РПГ-7В2, AT4, M72, SMAW, Карл Густав и др.
        if (entity instanceof ProjectileLike) {
            return bySpeed(entity, 1.5, 4.0, ProjectileProfile.BALLISTIC,
                ProjectileProfile.SLOW_LINEAR, ProjectileProfile.SLOW_LINEAR);
        }
        // База SBW-снарядов (ПТУРы/НУРС/снаряды/бомбы + всё наследуемое аддонами)
        if (entity instanceof FastThrowableProjectile) {
            return bySpeed(entity, 1.5, 4.0, ProjectileProfile.BALLISTIC,
                ProjectileProfile.SLOW_LINEAR, ProjectileProfile.HEAVY);
        }
        return ProjectileProfile.STATIC;
    }

    private static ProjectileProfile bySpeed(Entity entity, double slowBound, double midBound,
                                             ProjectileProfile slow, ProjectileProfile mid,
                                             ProjectileProfile fast) {
        double speed = entity.getDeltaMovement().length();
        if (speed <= slowBound) return slow;
        if (speed < midBound) return mid;
        return fast;
    }

    /** Размер кэша (для debug). */
    public static int cacheSize() {
        return CACHE.size();
    }
}
