package com.pigeostudios.pwp.warfare.client.gui;

import com.pigeostudios.pwp.warfare.client.ClientData;
import com.pigeostudios.pwp.warfare.client.gui.ui.WTheme;
import com.pigeostudios.pwp.warfare.client.gui.ui.WarfareIcon;
import com.pigeostudios.pwp.warfare.client.gui.ui.WarfareRadialScreen;
import com.pigeostudios.pwp.warfare.config.WarfareConfig;
import com.pigeostudios.pwp.warfare.entity.SupplyCrateEntity;
import com.pigeostudios.pwp.warfare.network.PacketHandler;
import com.pigeostudios.pwp.warfare.network.PacketRequestCrateAmmo;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

/**
 * Снабжение из ящика: те же пункты, что у FOB (AGS-30/M2 выключены). Ящик исчез — меню
 * закрывается (как раньше).
 */
public class CrateRadialScreen extends WarfareRadialScreen {
    private final int entityId;
    private int cdAmmo;
    private int materials;

    public CrateRadialScreen(int entityId) {
        super(Component.translatable("gui.pwpwarfare.radial.crate_supply"));
        this.entityId = entityId;
    }

    private void updateData() {
        var mc = Minecraft.getInstance();
        long elapsed = System.currentTimeMillis() - ClientData.lastFobResupplyTime;
        cdAmmo =
                elapsed < 60000L && mc.player != null && !mc.player.isCreative() ? (int) ((60000L - elapsed) / 50L) : 0;
        if (mc.level != null) {
            if (mc.level.getEntity(entityId) instanceof SupplyCrateEntity crate) materials = crate.getMaterials();
            else onClose();
        }
    }

    private void request(int sector) {
        PacketHandler.INSTANCE.sendToServer(new PacketRequestCrateAmmo(entityId, sector));
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
                        cost(20),
                        WTheme.STATUS_BAD,
                        true,
                        null),
                new Entry(
                        Component.translatable("gui.pwpwarfare.radial.item.m2"),
                        WarfareIcon.GUN,
                        cost(15),
                        WTheme.STATUS_BAD,
                        true,
                        null),
                Entry.of(
                                Component.translatable("gui.pwpwarfare.radial.item.mortar"),
                                WarfareIcon.TARGET,
                                () -> request(3))
                        .detail(cost(20))
                        .warn(materials < 20),
                Entry.of(Component.translatable("gui.pwpwarfare.radial.item.tow"), WarfareIcon.TARGET, () -> request(4))
                        .detail(cost(50))
                        .warn(materials < 50));
    }

    private static Component cost(int c) {
        return Component.translatable("gui.pwpwarfare.radial.cost", c);
    }

    private static Component cooldown(int ticks) {
        return Component.translatable("gui.pwpwarfare.radial.cooldown", ticks / 20);
    }

    @Override
    protected Component centerInfo() {
        return Component.translatable(
                "gui.pwpwarfare.radial.materials_of", materials, WarfareConfig.SUPPLY_CRATE_MATERIALS.get());
    }
}
