package com.pigeostudios.pwp.warfare.data.state;

import com.pwp.api.util.LogOnce;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Capability с состоянием PWP игрока (бывшие ключи {@code WARFARE_*} в persistentData).
 * Сохраняется в player.dat (ForgeCaps → {@code pwpwarfare:player_data}).
 *
 * <p>Миграция: при первом обращении к данным игрока и при входе (HIGHEST) все ключи с префиксом
 * {@code WARFARE_} копируются из persistentData в capability ОДИН раз (маркер в capability).
 * Старые ключи остаются в persistentData ещё одну версию — для отката на старый мод.
 *
 * <p>Клон игрока: capability не копируется сама — правила в {@link PlayerCloneRules}
 * (смерть — отряд/киты/ресапплай, возврат из Энда — всё), вызов — {@link #copyOnClone}.
 */
public final class PlayerWarfareData extends WarfareData {
    private static final Logger LOG = LoggerFactory.getLogger(PlayerWarfareData.class);

    public static final ResourceLocation ID = new ResourceLocation("pwpwarfare", "player_data");
    public static final Capability<PlayerWarfareData> CAPABILITY = CapabilityManager.get(new CapabilityToken<>() {});

    private CompoundTag tag = new CompoundTag();
    /** Миграция persistentData уже выполнена для этого объекта (в NBT не пишется). */
    private boolean migrated;

    /** Данные игрока. Если capability нет (не должно случаться) — пустой отвязанный объект. */
    public static PlayerWarfareData of(Player player) {
        PlayerWarfareData data = player.getCapability(CAPABILITY).orElse(null);
        if (data == null) {
            LogOnce.warn(LOG, "no-cap", "У игрока нет capability PlayerWarfareData — данные не сохранятся", null);
            data = new PlayerWarfareData();
            data.migrated = true;
            return data;
        }
        if (!data.migrated) {
            data.migrated = true;
            int moved = WarfareMigration.migratePlayer(player.getPersistentData(), data.tag);
            // откат на старый мод больше не поддерживается (18.11.2026) — старые ключи удаляем
            int removed = WarfareMigration.removeLegacy(player.getPersistentData());
            if (moved > 0 || removed > 0)
                LOG.info(
                        "Миграция {}: {} ключей WARFARE_* перенесено в capability, {} удалено из persistentData",
                        player.getScoreboardName(),
                        moved,
                        removed);
        }
        return data;
    }

    @Override
    protected CompoundTag tag() {
        return tag;
    }

    /**
     * Копирование данных при {@code PlayerEvent.Clone} по {@link PlayerCloneRules}. Capability
     * старого игрока к этому моменту инвалидирована — вызывающий оборачивает в
     * {@code reviveCaps()/invalidateCaps()}.
     */
    public static void copyOnClone(Player oldPlayer, Player newPlayer, boolean wasDeath) {
        PlayerCloneRules.copy(of(oldPlayer).tag, of(newPlayer).tag, wasDeath);
    }

    /** Полная копия данных (для клона игрока при смене измерения и тестов). */
    public CompoundTag copyTag() {
        return tag.copy();
    }

    // ===== Регистрация и прикрепление =====

    @Mod.EventBusSubscriber(modid = "pwpwarfare", bus = Mod.EventBusSubscriber.Bus.MOD)
    public static final class ModEvents {
        private ModEvents() {}

        @SubscribeEvent
        public static void register(RegisterCapabilitiesEvent event) {
            event.register(PlayerWarfareData.class);
        }
    }

    @Mod.EventBusSubscriber(modid = "pwpwarfare", bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static final class ForgeEvents {
        private ForgeEvents() {}

        @SubscribeEvent
        public static void attach(AttachCapabilitiesEvent<Entity> event) {
            if (event.getObject() instanceof Player) {
                event.addCapability(ID, new Provider());
            }
        }

        /** Миграция раньше всех остальных обработчиков входа (они читают кит/отряд). */
        @SubscribeEvent(priority = EventPriority.HIGHEST)
        public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
            of(event.getEntity());
        }
    }

    private static final class Provider implements ICapabilitySerializable<CompoundTag> {
        private final PlayerWarfareData data = new PlayerWarfareData();
        private final LazyOptional<PlayerWarfareData> optional = LazyOptional.of(() -> data);

        @Override
        public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
            return CAPABILITY.orEmpty(cap, optional);
        }

        @Override
        public CompoundTag serializeNBT() {
            return data.tag.copy();
        }

        @Override
        public void deserializeNBT(CompoundTag nbt) {
            data.tag = nbt.copy();
        }
    }
}
