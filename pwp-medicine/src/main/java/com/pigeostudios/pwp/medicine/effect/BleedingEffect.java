package com.pigeostudios.pwp.medicine.effect;

import java.util.UUID;
import com.pigeostudios.pwp.medicine.event.EventHandler;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

// Эффект кровотечения.
// Наносит периодический урон игроку, пока тот не умрёт или не исцелится.
public class BleedingEffect
extends MobEffect {
    public BleedingEffect() {
        super(MobEffectCategory.HARMFUL, 0xFF0000); // Красный цвет эффекта
    }

    // Вызывается каждый тик для применения эффекта
    public void applyEffectTick(LivingEntity entity, int amplifier) {
        if (entity instanceof Player) {
            Player victim = (Player)entity;
            if (!victim.level().isClientSide) { // Только на сервере
                // Если игрок в состоянии "нокаут" — снимаем кровотечение
                if (victim.getPersistentData().getBoolean("PWP_IsDowned")) {
                    victim.removeEffect(this);
                    EventHandler.BLEEDING_SOURCES.remove(victim.getUUID());
                    return;
                }
                float currentHealth = victim.getHealth();
                // Если ХП <= 1 — наносим большой урон (смертельный)
                if (currentHealth <= 1.0f) {
                    DamageSource source;
                    UUID attackerUUID = EventHandler.BLEEDING_SOURCES.get(victim.getUUID());
                    Player attacker = attackerUUID != null ? victim.level().getPlayerByUUID(attackerUUID) : null;
                    // Кастомный тип урона "bleeding" из pwp_medicine
                    Registry<DamageType> registry = victim.level().registryAccess().registry(Registries.DAMAGE_TYPE).orElse(null);
                    if (registry != null && registry.getHolderOrThrow(EventHandler.BLEEDING_DAMAGE_KEY) != null) {
                        Holder.Reference<DamageType> holder = registry.getHolderOrThrow(EventHandler.BLEEDING_DAMAGE_KEY);
                        source = new DamageSource(holder, (Entity)attacker, (Entity)attacker);
                    } else {
                        source = victim.damageSources().genericKill(); // Фолбэк: общий урон
                    }
                    victim.hurt(source, 10.0f);
                } else {
                    // Иначе просто уменьшаем ХП на 1
                    victim.setHealth(currentHealth - 1.0f);
                }
            }
        }
    }

    // Эффект срабатывает каждые 40 тиков (2 секунды)
    public boolean isDurationEffectTick(int duration, int amplifier) {
        return duration % 40 == 0;
    }
}
