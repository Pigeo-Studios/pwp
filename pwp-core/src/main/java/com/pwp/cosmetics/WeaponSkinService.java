package com.pwp.cosmetics;

import com.pwp.api.cosmetics.WeaponSkin;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;

/**
 * Скины стволов на сервере (этап 11): для каждого игрока — GunId → display надетых скинов. Раз в 2 с (и сразу
 * после экипировки) все стволы TaCZ в инвентаре получают {@code GunDisplayId} своего скина с меткой
 * {@link WeaponSkin#MARK_TAG}; снятый скин — метка и display убираются. Работает для любого источника ствола:
 * кит, пополнение, подобранный с земли — настраивать киты не нужно. Характеристики ({@code GunId}) не
 * меняются никогда — только вид.
 */
public final class WeaponSkinService {
    private static final Map<UUID, Map<String, String>> EQUIPPED = new ConcurrentHashMap<>();
    private static final int PERIOD_TICKS = 40;

    private WeaponSkinService() {}

    public static void init() {
        MinecraftForge.EVENT_BUS.addListener(WeaponSkinService::onTick);
        MinecraftForge.EVENT_BUS.addListener(WeaponSkinService::onLogout);
    }

    public static void put(UUID player, WeaponSkin skin) {
        EQUIPPED.computeIfAbsent(player, k -> new ConcurrentHashMap<>()).put(skin.gunId(), skin.displayId());
    }

    public static void remove(UUID player, String gunId) {
        Map<String, String> m = EQUIPPED.get(player);
        if (m != null) m.remove(gunId);
    }

    public static void clear(UUID player) {
        EQUIPPED.remove(player);
    }

    /** Надетые скины стволов (GunId → display) — для экрана арсенала и отладки. */
    public static Map<String, String> of(UUID player) {
        return EQUIPPED.getOrDefault(player, Map.of());
    }

    /** Применить к инвентарю сейчас (после экипировки — чтобы ствол в руках сменил вид сразу). */
    public static void apply(ServerPlayer p) {
        Map<String, String> eq = EQUIPPED.getOrDefault(p.getUUID(), Map.of());
        Inventory inv = p.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) applyTo(inv.getItem(i), eq);
    }

    /** Один предмет: не ствол TaCZ (нет GunId) — не трогаем. */
    public static boolean applyTo(ItemStack stack, Map<String, String> eq) {
        if (stack.isEmpty() || !stack.hasTag()) return false;
        CompoundTag tag = stack.getTag();
        if (!tag.contains("GunId", 8)) return false;
        String gun = tag.getString("GunId");
        String cur = tag.getString(WeaponSkin.DISPLAY_TAG);
        boolean marked = tag.getBoolean(WeaponSkin.MARK_TAG);
        switch (WeaponSkin.decide(eq, gun, cur, marked)) {
            case SET -> {
                tag.putString(WeaponSkin.DISPLAY_TAG, eq.get(gun));
                tag.putBoolean(WeaponSkin.MARK_TAG, true);
                return true;
            }
            case CLEAR -> {
                tag.remove(WeaponSkin.DISPLAY_TAG);
                tag.remove(WeaponSkin.MARK_TAG);
                return true;
            }
            default -> {
                return false;
            }
        }
    }

    private static void onTick(TickEvent.PlayerTickEvent e) {
        if (e.phase != TickEvent.Phase.END || !(e.player instanceof ServerPlayer p)) return;
        if ((com.pwp.api.time.GameClock.serverTicks(p.server) + p.getId()) % PERIOD_TICKS != 0) return;
        apply(p);
    }

    private static void onLogout(PlayerEvent.PlayerLoggedOutEvent e) {
        clear(e.getEntity().getUUID());
    }
}
