package com.pigeostudios.pwp.warfare.client.gui;

import com.pigeostudios.pwp.warfare.block.HubBlockEntity;
import com.pigeostudios.pwp.warfare.client.ClientData;
import com.pigeostudios.pwp.warfare.client.gui.ui.WTheme;
import com.pigeostudios.pwp.warfare.client.gui.ui.WarfareIcon;
import com.pigeostudios.pwp.warfare.client.gui.ui.WarfareRadialScreen;
import com.pigeostudios.pwp.warfare.config.WarfareConfig;
import com.pigeostudios.pwp.warfare.network.PacketHandler;
import com.pigeostudios.pwp.warfare.network.PacketRequestAmmo;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

/**
 * Снабжение FOB: пополнение БК (кулдаун 60 с), AGS-30/M2 (выключены), миномёт, ПТРК. Красным —
 * кулдаун/нехватка материалов; клик по миномёту/ПТРК уходит на сервер всегда (сервер проверяет).
 */
public class HubRadialScreen extends WarfareRadialScreen {
    private final BlockPos hubPos;
    private int cdAmmo;
    private int cdAGS;
    private int cdM2;
    private int cdMortar;
    private int cdTOW;
    private int materials;

    public HubRadialScreen(BlockPos pos) {
        super(Component.translatable("gui.pwpwarfare.radial.hub_supply"));
        this.hubPos = pos;
    }

    private void updateData() {
        var mc = Minecraft.getInstance();
        long elapsed = System.currentTimeMillis() - ClientData.lastFobResupplyTime;
        cdAmmo =
                elapsed < 60000L && mc.player != null && !mc.player.isCreative() ? (int) ((60000L - elapsed) / 50L) : 0;
        if (mc.level != null && mc.level.getBlockEntity(hubPos) instanceof HubBlockEntity hub) {
            cdAGS = hub.cooldownAGS;
            cdM2 = hub.cooldownM2;
            cdMortar = hub.cooldownMortar;
            cdTOW = hub.cooldownTOW;
            materials = hub.getMaterials();
        }
    }

    private void request(int sector) {
        PacketHandler.INSTANCE.sendToServer(new PacketRequestAmmo(hubPos, sector));
        onClose();
    }

    @Override
    protected List<Entry> entries() {
        updateData();
        int cost = WarfareConfig.HUB_RESUPPLY_COST.get();
        boolean creative = Minecraft.getInstance().player != null
                && Minecraft.getInstance().player.isCreative();
        Entry resupply = cdAmmo > 0
                ? new Entry(
                        Component.translatable("gui.pwpwarfare.radial.item.resupply"),
                        WarfareIcon.CRATE,
                        cooldown(cdAmmo),
                        WTheme.STATUS_BAD,
                        true,
                        null)
                : Entry.of(Component.translatable("gui.pwpwarfare.radial.item.resupply"), WarfareIcon.CRATE, () -> {
                            if (materials >= cost || creative) {
                                ClientData.lastFobResupplyTime = System.currentTimeMillis();
                            }
                            request(0);
                        })
                        .detail(cost(cost))
                        .warn(materials < cost);
        return List.of(
                resupply,
                new Entry(
                        Component.translatable("gui.pwpwarfare.radial.item.ags"),
                        WarfareIcon.GUN,
                        cdAGS > 0 ? cooldown(cdAGS) : cost(20),
                        WTheme.STATUS_BAD,
                        true,
                        null),
                new Entry(
                        Component.translatable("gui.pwpwarfare.radial.item.m2"),
                        WarfareIcon.GUN,
                        cdM2 > 0 ? cooldown(cdM2) : cost(15),
                        WTheme.STATUS_BAD,
                        true,
                        null),
                Entry.of(
                                Component.translatable("gui.pwpwarfare.radial.item.mortar"),
                                WarfareIcon.TARGET,
                                () -> request(3))
                        .detail(cdMortar > 0 ? cooldown(cdMortar) : cost(20))
                        .warn(cdMortar > 0 || materials < 20),
                Entry.of(Component.translatable("gui.pwpwarfare.radial.item.tow"), WarfareIcon.TARGET, () -> request(4))
                        .detail(cdTOW > 0 ? cooldown(cdTOW) : cost(50))
                        .warn(cdTOW > 0 || materials < 50));
    }

    private static Component cost(int c) {
        return Component.translatable("gui.pwpwarfare.radial.cost", c);
    }

    private static Component cooldown(int ticks) {
        return Component.translatable("gui.pwpwarfare.radial.cooldown", ticks / 20);
    }

    @Override
    protected Component centerInfo() {
        return Component.translatable("gui.pwpwarfare.radial.materials", materials);
    }
}
