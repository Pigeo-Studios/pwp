package com.pigeostudios.sbwchunkload.mixin;

import com.atsuishio.superbwarfare.entity.vehicle.DroneEntity;
import com.pigeostudios.sbwchunkload.api.ProjectileTracker;
import com.pigeostudios.sbwchunkload.drone.DroneTracker;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Единая точка входа: ловим спавн КАЖДОЙ сущности на сервере и регистрируем
 * снаряды в трекере (ProjectileLike у pointblank — все ракеты FCL-труб и др.,
 * FastThrowableProjectile/ProjectileEntity/TaserBulletEntity у SuperbWarfare —
 * НУРС, ПТУРы, автопушки, бомбы, 7.62-пули, а также все снаряды аддонов
 * FCP/VVP/DragonRise, наследующие SBW-классы).
 * Такой подход не зависит от конкретных классов снарядов: любой будущий
 * аддон покрывается автоматически, без новых миксинов.
 *
 * ВАЖНО: в 1.20.1 (official mappings) ServerLevel.addFreshEntity(Entity)
 * объявлен ТОЛЬКО на ServerLevel и возвращает boolean (SRG m_7967_(Entity)Z,
 * обёртка над entityManager.addNewEntity) — хендлер обязан использовать
 * CallbackInfoReturnable<Boolean>, иначе миксин падает при применении
 * ("CallbackInfoReturnable is required!") и сервер не стартует.
 */
@Mixin(ServerLevel.class)
public class LevelAddFreshEntityMixin {

    @Inject(method = "addFreshEntity", at = @At("TAIL"))
    private void sbwchunkload$onAddFreshEntity(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        ProjectileTracker.register(entity);
        // Дроны регистрируются СРАЗУ при спавне (а не через полный скан раз в
        // 100 тиков): дрон летит 20-40 блоков/сек и может вылететь за зону
        // прогруза быстрее, чем скан его найдёт.
        if (entity instanceof DroneEntity) {
            DroneTracker.register(entity);
        }
    }
}
