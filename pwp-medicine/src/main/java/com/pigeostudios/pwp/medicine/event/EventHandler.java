package com.pigeostudios.pwp.medicine.event;

import java.util.HashMap;
import java.util.UUID;
import com.pigeostudios.pwp.medicine.config.PWPConfig;
import com.pigeostudios.pwp.medicine.effect.ModEffects;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

// Обработчик событий мода: пассивные эффекты, наложение кровотечения, очистка при смерти.
@Mod.EventBusSubscriber(modid="pwp_medicine")
public class EventHandler {
    // Флаги для пассивных эффектов: flag7 — ХП <= 14, flag3 — ХП <= 6
    private static final HashMap<UUID, Boolean> flag7 = new HashMap();
    private static final HashMap<UUID, Boolean> flag3 = new HashMap();
    // Карта: UUID жертвы -> UUID атакующего (для отслеживания источника кровотечения)
    public static final HashMap<UUID, UUID> BLEEDING_SOURCES = new HashMap();
    public static final ResourceKey<DamageType> BLEEDING_DAMAGE_KEY = ResourceKey.create(Registries.DAMAGE_TYPE, new ResourceLocation("pwp_medicine", "bleeding"));

    // Пассивные эффекты в зависимости от уровня здоровья
    @SubscribeEvent
    public static void onPlayerTick(LivingEvent.LivingTickEvent event) {
        LivingEntity livingEntity = event.getEntity();
        if (livingEntity instanceof Player) {
            Player player = (Player)livingEntity;
            if (!player.level().isClientSide) { // Только на сервере
                float hp = player.getHealth();
                int ticks = player.tickCount;
                UUID uuid = player.getUUID();
                // При ХП <= 14 — тошнота I
                if (hp <= 14.0f) {
                    if (!flag7.getOrDefault(uuid, false)) {
                        player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 100, 0, false, false, true));
                        flag7.put(uuid, true);
                    }
                } else {
                    flag7.put(uuid, false);
                }
                // При ХП <= 6 — тошнота II и слепота
                if (hp <= 6.0f) {
                    if (!flag3.getOrDefault(uuid, false)) {
                        player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 200, 1, false, false, true));
                        player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 200, 0, false, false, true));
                        flag3.put(uuid, true);
                    }
                } else {
                    flag3.put(uuid, false);
                }
                // Слабость в зависимости от уровня ХП
                if (hp <= 6.0f) {
                    player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 40, 2, false, false, true));
                } else if (hp <= 10.0f) {
                    player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 40, 1, false, false, true));
                } else if (hp <= 14.0f) {
                    player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 40, 0, false, false, true));
                }
                // Периодическая тошнота на разных порогах
                if (hp > 10.0f && hp <= 14.0f) {
                    if (ticks % 300 == 0) {
                        player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 20, 0, false, false, true));
                    }
                } else if (hp > 6.0f && hp <= 10.0f) {
                    if (ticks % 200 == 0) {
                        player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 30, 0, false, false, true));
                    }
                } else if (hp > 0.0f && hp <= 6.0f && ticks % 100 == 0) {
                    player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 40, 0, false, false, true));
                }
            }
        }
    }

    // Наложение кровотечения при получении урона
    @SubscribeEvent
    public static void onLivingDamage(LivingDamageEvent event) {
        LivingEntity livingEntity = event.getEntity();
        if (livingEntity instanceof Player) {
            Player victim = (Player)livingEntity;
            if (!victim.level().isClientSide) { // Только на сервере
                // Не накладываем кровотечение если игрок верхом
                if (victim.isPassenger()) {
                    return;
                }
                // Не накладываем если игрок в состоянии "нокаут"
                if (victim.getPersistentData().getBoolean("PWP_IsDowned")) {
                    return;
                }
                // Игнорируем урон от самого кровотечения (чтобы избежать бесконечного цикла)
                if (event.getSource().is(BLEEDING_DAMAGE_KEY)) {
                    return;
                }
                // Проверяем порог урона
                if (event.getAmount() < PWPConfig.BLEEDING_DAMAGE_THRESHOLD.get().floatValue()) {
                    return;
                }
                // Роллим шанс кровотечения
                float bleedChance = PWPConfig.BLEEDING_CHANCE.get().floatValue();
                if (victim.getRandom().nextFloat() < bleedChance) {
                    victim.addEffect(new MobEffectInstance(ModEffects.BLEEDING.get(), 12000, 0, false, false, true));
                    // Запоминаем атакующего для сообщения о смерти
                    Entity attacker = event.getSource().getEntity();
                    if (attacker instanceof Player) {
                        Player playerAttacker = (Player)attacker;
                        BLEEDING_SOURCES.put(victim.getUUID(), playerAttacker.getUUID());
                    } else {
                        BLEEDING_SOURCES.remove(victim.getUUID());
                    }
                }
            }
        }
    }

    // Очистка при смерти игрока: снимаем кровотечение и удаляем из карты источников
    @SubscribeEvent
    public static void onPlayerDeath(LivingDeathEvent event) {
        LivingEntity livingEntity = event.getEntity();
        if (livingEntity instanceof Player) {
            Player victim = (Player)livingEntity;
            if (victim.hasEffect(ModEffects.BLEEDING.get())) {
                victim.removeEffect(ModEffects.BLEEDING.get());
            }
            BLEEDING_SOURCES.remove(victim.getUUID());
        }
    }
}
