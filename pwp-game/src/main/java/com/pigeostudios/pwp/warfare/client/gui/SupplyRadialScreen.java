package com.pigeostudios.pwp.warfare.client.gui;

import com.pigeostudios.pwp.warfare.client.ClientData;
import com.pigeostudios.pwp.warfare.client.gui.ui.WTheme;
import com.pigeostudios.pwp.warfare.client.gui.ui.WarfareIcon;
import com.pigeostudios.pwp.warfare.client.gui.ui.WarfareRadialScreen;
import com.pigeostudios.pwp.warfare.config.WarfareConfig;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * Снабжение (ФОБ и ящик — один набор пунктов, раньше скопированный в двух экранах): пополнить БК (кулдаун 60 с),
 * снаряды миномёта и ПТРК. Красным — кулдаун/нехватка материалов; клик уходит на сервер всегда (решает сервер).
 * Наследник даёт материалы, кулдауны установок и отправку запроса сектора (0 — БК, 3 — миномёт, 4 — ПТРК).
 */
@OnlyIn(Dist.CLIENT)
abstract class SupplyRadialScreen extends WarfareRadialScreen {
    static final int SECTOR_AMMO = 0;
    static final int SECTOR_MORTAR = 3;
    static final int SECTOR_TOW = 4;
    static final int MORTAR_COST = 20;
    static final int TOW_COST = 50;
    private static final long AMMO_COOLDOWN_MS = 60_000L;

    protected SupplyRadialScreen(Component title) {
        super(title);
    }

    /** Обновить данные источника перед кадром; false — источника больше нет (меню закрыть). */
    protected abstract boolean refresh();

    protected abstract int materials();

    protected int mortarCooldownTicks() {
        return 0;
    }

    protected int towCooldownTicks() {
        return 0;
    }

    protected abstract void send(int sector);

    private void request(int sector) {
        send(sector);
        onClose();
    }

    private static int ammoCooldownTicks() {
        var mc = Minecraft.getInstance();
        long elapsed = System.currentTimeMillis() - ClientData.lastFobResupplyTime;
        boolean creative = mc.player != null && mc.player.isCreative();
        return elapsed < AMMO_COOLDOWN_MS && !creative ? (int) ((AMMO_COOLDOWN_MS - elapsed) / 50L) : 0;
    }

    static Component cost(int c) {
        return Component.translatable("gui.pwpwarfare.radial.cost", c);
    }

    static Component cooldown(int ticks) {
        return Component.translatable("gui.pwpwarfare.radial.cooldown", ticks / 20);
    }

    private Entry gun(String item, int sector, int cost, int cd) {
        int m = materials();
        return Entry.of(
                        Component.translatable("gui.pwpwarfare.radial.item." + item),
                        WarfareIcon.TARGET,
                        () -> request(sector))
                .detail(cd > 0 ? cooldown(cd) : cost(cost))
                .warn(cd > 0 || m < cost);
    }

    @Override
    protected List<Entry> entries() {
        // источник исчез — закрыть (как раньше), пункты этого кадра всё равно отдать: пустой список радиалка не рисует
        if (!refresh()) onClose();
        int m = materials();
        int cost = WarfareConfig.HUB_RESUPPLY_COST.get();
        int cdAmmo = ammoCooldownTicks();
        boolean creative = Minecraft.getInstance().player != null
                && Minecraft.getInstance().player.isCreative();
        Component resupplyLabel = Component.translatable("gui.pwpwarfare.radial.item.resupply");
        Entry resupply = cdAmmo > 0
                ? new Entry(resupplyLabel, WarfareIcon.CRATE, cooldown(cdAmmo), WTheme.STATUS_BAD, true, null)
                : Entry.of(resupplyLabel, WarfareIcon.CRATE, () -> {
                            if (m >= cost || creative) ClientData.lastFobResupplyTime = System.currentTimeMillis();
                            request(SECTOR_AMMO);
                        })
                        .detail(cost(cost))
                        .warn(m < cost);
        return List.of(
                resupply,
                gun("mortar", SECTOR_MORTAR, MORTAR_COST, mortarCooldownTicks()),
                gun("tow", SECTOR_TOW, TOW_COST, towCooldownTicks()));
    }
}
